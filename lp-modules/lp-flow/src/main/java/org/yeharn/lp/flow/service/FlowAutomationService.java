/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程自动化能力：自动审批、远程访问、脚本执行
 */

package org.yeharn.lp.flow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.yeharn.lp.flow.support.FlowVariableSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 节点级自动化扩展，配置在节点 ext（JSON）中：
 * <pre>
 * {
 *   "autoApprove": true,                     // 自动审批通过
 *   "autoComment": "系统自动通过",
 *   "remote": {"url": "http://...", "method": "POST", "body": {"id": 1}},
 *   "script": {"expression": "#amount > 1000", "resultKey": "bigAmount"}
 * }
 * </pre>
 * 全部在事务提交后异步执行，失败只记录日志，不影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowAutomationService {

    private final TaskService taskService;

    private final ObjectMapper objectMapper;

    private final FlowVariableSupport variableSupport;

    private final BeanFactory beanFactory;

    private final JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "flow-automation");
        thread.setDaemon(true);
        return thread;
    });

    public void onTaskCreated(Task task, Node node, Instance instance) {
        if (task == null || node == null || instance == null) {
            return;
        }
        Map<String, Object> ext = readExt(node.getExt());
        if (ext.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(ext.get("cc"))) {
            runAfterCommit(() -> recordCc(instance, ext));
        }
        if (Boolean.TRUE.equals(ext.get("autoApprove"))) {
            runAfterCommit(() -> autoApprove(task, instance, ext));
        }
        Map<String, Object> remote = asMap(ext.get("remote"));
        if (!remote.isEmpty()) {
            runAfterCommit(() -> callRemote(task, instance, remote));
        }
        Map<String, Object> script = asMap(ext.get("script"));
        if (!script.isEmpty()) {
            runAfterCommit(() -> runScript(instance, script));
        }
    }

    @SuppressWarnings("unchecked")
    private void recordCc(Instance instance, Map<String, Object> ext) {
        Object users = ext.get("ccUsers");
        if (!(users instanceof List<?> list)) {
            return;
        }
        for (Object user : list) {
            if (user == null || String.valueOf(user).isBlank()) {
                continue;
            }
            jdbcTemplate.update("INSERT IGNORE INTO lp_flow_read (id, instance_id, task_id, user_id, user_name, create_time) "
                            + "VALUES (?, ?, NULL, ?, ?, NOW())",
                    org.dromara.warm.flow.core.utils.IdUtils.nextId(), instance.getId(),
                    String.valueOf(user), String.valueOf(user));
        }
    }

    private void autoApprove(Task task, Instance instance, Map<String, Object> ext) {
        if (taskService.getById(task.getId()) == null) {
            return;
        }
        String handler = instance.getCreateBy() == null ? "1" : instance.getCreateBy();
        String comment = String.valueOf(ext.getOrDefault("autoComment", "系统自动审批通过"));
        FlowParams params = FlowParams.build()
                .handler(handler)
                .permissionFlag(List.of(handler))
                .message(comment)
                .ignore(true);
        taskService.skip(task.getId(), params);
        log.info("节点[{}]自动审批通过，任务：{}", task.getNodeName(), task.getId());
    }

    private void callRemote(Task task, Instance instance, Map<String, Object> remote) {
        String url = String.valueOf(remote.get("url"));
        if (url.isBlank() || "null".equals(url)) {
            return;
        }
        String method = String.valueOf(remote.getOrDefault("method", "POST")).toUpperCase();
        Object body = remote.get("body");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instanceId", instance.getId());
        payload.put("taskId", task.getId());
        payload.put("nodeCode", task.getNodeCode());
        payload.put("businessId", instance.getBusinessId());
        payload.put("variables", variableSupport.read(instance.getVariable()));
        if (body instanceof Map<?, ?> map) {
            map.forEach((key, value) -> payload.put(String.valueOf(key), value));
        }
        try {
            String response = restTemplate.exchange(url, HttpMethod.valueOf(method),
                    new HttpEntity<>(payload, headers), String.class).getBody();
            log.info("节点远程调用完成：{} -> {}", url, response);
        } catch (Exception e) {
            log.warn("节点远程调用失败：{}，{}", url, e.getMessage());
        }
    }

    private void runScript(Instance instance, Map<String, Object> script) {
        String expression = String.valueOf(script.get("expression"));
        if (expression.isBlank() || "null".equals(expression)) {
            return;
        }
        try {
            org.springframework.expression.spel.standard.SpelExpressionParser parser =
                    new org.springframework.expression.spel.standard.SpelExpressionParser();
            org.springframework.expression.spel.support.StandardEvaluationContext context =
                    new org.springframework.expression.spel.support.StandardEvaluationContext();
            context.setBeanResolver(new org.springframework.context.expression.BeanFactoryResolver(beanFactory));
            context.setVariables(variableSupport.read(instance.getVariable()));
            Object value = parser.parseExpression(expression).getValue(context);
            Object resultKey = script.get("resultKey");
            if (resultKey != null && value != null) {
                Map<String, Object> variables = variableSupport.read(instance.getVariable());
                variables.put(String.valueOf(resultKey), value);
                taskService.mergeVariable(instance, variables);
            }
            log.info("节点脚本执行完成：{} = {}", expression, value);
        } catch (Exception e) {
            log.warn("节点脚本执行失败：{}，{}", expression, e.getMessage());
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(action);
                }
            });
        } else {
            submit(action);
        }
    }

    private void submit(Runnable action) {
        executor.submit(() -> {
            try {
                action.run();
            } catch (Exception e) {
                log.warn("流程自动化执行失败：{}", e.getMessage());
            }
        });
    }

    private Map<String, Object> readExt(String ext) {
        if (ext == null || ext.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(ext, new TypeReference<>() {
            });
            return map == null ? Map.of() : map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
