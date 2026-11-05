/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 内置任务处理器
 */

package org.yeharn.lp.job.jobs;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.yeharn.lp.job.core.LpJob;
import org.yeharn.lp.job.service.JobService;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 内置任务处理器：运行日志清理、注册中心服务健康巡检
 */
public class BuiltinJobs {

    /** 清理 N 天前的任务运行日志，参数为天数，默认 30 */
    @Slf4j
    @Component("jobLogCleanupJob")
    public static class JobLogCleanupJob implements LpJob {

        private final JobService jobService;

        public JobLogCleanupJob(JobService jobService) {
            this.jobService = jobService;
        }

        @Override
        public String execute(String param) {
            int days = 30;
            if (StrUtil.isNotBlank(param)) {
                try {
                    days = Integer.parseInt(param.trim());
                } catch (NumberFormatException ignored) {
                    // 参数非法时使用默认保留天数
                }
            }
            int deleted = jobService.cleanLogs(days);
            return "清理 " + days + " 天前的运行日志 " + deleted + " 条";
        }
    }

    /** 巡检注册中心内全部实例的 /actuator/health，返回健康摘要 */
    @Slf4j
    @Component("serviceHealthCheckJob")
    public static class ServiceHealthCheckJob implements LpJob {

        private final DiscoveryClient discoveryClient;

        public ServiceHealthCheckJob(DiscoveryClient discoveryClient) {
            this.discoveryClient = discoveryClient;
        }

        @Override
        public String execute(String param) {
            List<String> services = discoveryClient.getServices();
            int up = 0;
            int down = 0;
            List<String> details = new ArrayList<>();
            for (String service : services) {
                for (ServiceInstance instance : discoveryClient.getInstances(service)) {
                    String status = probe(instance);
                    if ("UP".equals(status)) {
                        up++;
                    } else {
                        down++;
                    }
                    details.add(service + "@" + instance.getHost() + ":" + instance.getPort() + "=" + status);
                }
            }
            return "巡检 " + (up + down) + " 个实例，UP " + up + "，异常 " + down
                    + (details.isEmpty() ? "" : "；" + String.join(", ", details));
        }

        /** 探测实例健康端点，2 秒连接超时，避免阻塞调度线程 */
        private String probe(ServiceInstance instance) {
            String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/actuator/health";
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                conn.disconnect();
                return code == 200 ? "UP" : "HTTP " + code;
            } catch (Exception e) {
                return "UNREACHABLE";
            }
        }
    }
}
