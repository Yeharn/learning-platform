/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程变量序列化支持
 */

package org.yeharn.lp.flow.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * flow_instance.variable / flow_his_task.variable 均为 JSON 字符串，这里统一读写
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowVariableSupport {

    private final ObjectMapper objectMapper;

    public Map<String, Object> read(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (Exception e) {
            log.warn("流程变量解析失败：{}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public String write(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            log.warn("流程变量序列化失败：{}", e.getMessage());
            return "{}";
        }
    }
}
