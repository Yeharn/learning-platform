/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 启动时部署内置示例流程
 */

package org.yeharn.lp.flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.yeharn.lp.flow.service.FlowFormService;
import org.yeharn.lp.flow.service.FlowModelBuilder;
import org.yeharn.lp.flow.service.FlowService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动自动部署内置示例流程（请假审批），并初始化演示表单模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowModelInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private final FlowService flowService;

    private final FlowModelBuilder modelBuilder;

    private final FlowFormService formService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flow_definition WHERE flow_code = 'leave' AND del_flag = '0'", Integer.class);
            if (count != null && count > 0) {
                return;
            }
            initForm();
            Map<String, String> first = node("部门审批", "1");
            Map<String, String> second = node("主管审批", "1");
            String json = modelBuilder.build("leave", "请假审批", List.of(first, second), List.of());
            flowService.deploy(null, json, "内置示例：请假审批");
            log.info("已自动部署内置流程模型：leave");
        } catch (Exception e) {
            // 表未初始化或用户上下文缺失时不影响服务启动
            log.warn("内置流程模型初始化跳过：{}", e.getMessage());
        }
    }

    private Map<String, String> node(String name, String assigneeId) {
        Map<String, String> node = new LinkedHashMap<>();
        node.put("name", name);
        node.put("assigneeId", assigneeId);
        return node;
    }

    private void initForm() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lp_flow_form WHERE form_code = 'leave_form' AND del_flag = '0'", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("formCode", "leave_form");
        form.put("formName", "请假申请单");
        form.put("remark", "内置示例表单");
        form.put("fields", List.of(
                field("days", "请假天数", "number", true, 12),
                field("reason", "请假事由", "textarea", true, 24),
                field("attachment", "附件", "attachment", false, 24)
        ));
        form.put("ext", List.of(
                field("urgent", "是否紧急", "switch", false, 12)
        ));
        formService.save(form);
    }

    private Map<String, Object> field(String key, String label, String type, boolean required, int span) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", key);
        field.put("label", label);
        field.put("type", type);
        field.put("required", required);
        field.put("span", span);
        field.put("options", List.of());
        return field;
    }
}
