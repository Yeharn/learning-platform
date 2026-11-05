/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 监控服务 - 服务注册监控业务服务（基于 Spring Cloud Netflix Eureka）
 */

package org.yeharn.lp.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 服务注册监控：从注册中心枚举全部服务与实例，并行探测各实例 /actuator/health，
 * 聚合注册信息（IP / 端口 / 注册时间 / 心跳 / 元数据）与健康明细
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceMonitorService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper;

    /** 共享 HTTP 客户端：2s 连接 / 3s 读超时，避免单个失联实例拖垮整体响应 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public Map<String, Object> overview() {
        List<Map<String, Object>> services = new ArrayList<>();
        int totalInstances = 0;
        int upInstances = 0;
        long responseSum = 0;
        int responseCount = 0;

        for (String serviceName : discoveryClient.getServices()) {
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (ServiceInstance instance : discoveryClient.getInstances(serviceName)) {
                futures.add(probe(instance));
            }

            List<Map<String, Object>> instances = new ArrayList<>();
            long upCount = 0;
            for (CompletableFuture<Map<String, Object>> future : futures) {
                Map<String, Object> item = future.join();
                instances.add(item);
                totalInstances++;
                if ("UP".equals(item.get("health"))) {
                    upInstances++;
                    upCount++;
                }
                Object ms = item.get("responseMs");
                if (ms instanceof Number number) {
                    responseSum += number.longValue();
                    responseCount++;
                }
            }
            instances.sort(Comparator.comparing(i -> String.valueOf(i.get("instanceId"))));

            Map<String, Object> service = new LinkedHashMap<>();
            service.put("name", String.valueOf(serviceName).toLowerCase());
            service.put("instances", instances);
            service.put("upCount", upCount);
            services.add(service);
        }
        services.sort(Comparator.comparing(s -> String.valueOf(s.get("name"))));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("services", services.size());
        summary.put("instances", totalInstances);
        summary.put("up", upInstances);
        summary.put("down", totalInstances - upInstances);
        summary.put("avgResponseMs", responseCount == 0 ? 0 : Math.round(responseSum / (double) responseCount));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("services", services);
        return result;
    }

    /**
     * 单实例探测：聚合 Eureka 注册信息，并行请求 /actuator/health，
     * 任何异常都降级为 DOWN / UNKNOWN，保证监控页可用
     */
    private CompletableFuture<Map<String, Object>> probe(ServiceInstance instance) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("instanceId", instance.getInstanceId());
            item.put("serviceId", instance.getServiceId().toLowerCase());
            item.put("host", instance.getHost());
            item.put("port", instance.getPort());
            item.put("scheme", String.valueOf(instance.getScheme()));

            if (instance instanceof EurekaServiceInstance eurekaInstance) {
                InstanceInfo info = eurekaInstance.getInstanceInfo();
                item.put("eurekaStatus", info.getStatus() == null ? "UNKNOWN" : info.getStatus().name());
                LeaseInfo lease = info.getLeaseInfo();
                if (lease != null) {
                    item.put("registrationTime", formatTime(lease.getRegistrationTimestamp()));
                    item.put("renewalTime", formatTime(lease.getRenewalTimestamp()));
                    item.put("leaseDurationSecs", lease.getDurationInSecs());
                }
            }
            Map<String, String> metadata = instance.getMetadata();
            item.put("metadata", metadata == null ? Map.of() : metadata);

            long start = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder(
                                URI.create(instance.getUri() + "/actuator/health"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long cost = System.currentTimeMillis() - start;
                item.put("responseMs", cost);

                if (response.statusCode() == 200) {
                    JsonNode body = objectMapper.readTree(response.body());
                    item.put("health", body.path("status").asText("UNKNOWN"));

                    Map<String, String> detail = new LinkedHashMap<>();
                    JsonNode components = body.path("components");
                    if (components.isObject()) {
                        components.properties().forEach(entry ->
                                detail.put(entry.getKey(), entry.getValue().path("status").asText("UNKNOWN")));
                    }
                    item.put("healthDetail", detail);
                } else {
                    item.put("health", "HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                item.put("health", "DOWN");
                item.put("responseMs", System.currentTimeMillis() - start);
                item.put("healthError", e.getClass().getSimpleName());
            }
            return item;
        });
    }

    private String formatTime(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return TIME_FMT.format(Instant.ofEpochMilli(epochMillis));
    }
}
