/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Warm-Flow 流程业务服务
 */

package org.yeharn.lp.flow.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.utils.IdUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.flow.support.FlowUserDirectory;
import org.yeharn.lp.flow.support.FlowUserSupport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程引擎服务（Warm-Flow 1.8.9）：流程定义 / 发起 / 待办 / 办理 / 退回 / 历史
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private final DefService defService;

    private final InsService insService;

    private final TaskService taskService;

    private final NodeService nodeService;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private final FlowUserDirectory userDirectory;

    private final FlowDesignService designService;

    /* ---------------- 流程定义 ---------------- */

    public List<Map<String, Object>> definitions(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT d.id, d.flow_code, d.flow_name, d.model_value, d.category, ")
                .append("d.version, d.is_publish, d.create_by, d.create_time FROM flow_definition d ")
                .append("JOIN (SELECT flow_code, MAX(id) AS max_id FROM flow_definition WHERE del_flag = '0' GROUP BY flow_code) t ")
                .append("ON t.max_id = d.id WHERE d.del_flag = '0'");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword)) {
            sql.append(" AND (d.flow_name LIKE ? OR d.flow_code LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY d.create_time DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("key", rs.getString("flow_code"));
            item.put("name", rs.getString("flow_name"));
            item.put("type", rs.getString("model_value"));
            item.put("version", rs.getString("version"));
            item.put("state", rs.getInt("is_publish"));
            item.put("useScope", 1);
            item.put("createBy", rs.getString("create_by"));
            item.put("createTime", time(rs, "create_time"));
            item.put("remark", rs.getString("category"));
            return item;
        }, args.toArray());
    }

    /**
     * 设计器数据（DefJson）：节点 + 连线 + 表单路径
     */
    public String model(Long id) {
        DefJson defJson = defService.queryDesign(id);
        if (defJson == null) {
            throw new ServiceException("流程定义不存在");
        }
        try {
            return objectMapper.writeValueAsString(defJson);
        } catch (Exception e) {
            throw new ServiceException("流程模型读取失败：" + e.getMessage());
        }
    }

    /**
     * 保存并发布流程定义
     *
     * @param id   已有定义 id，为空则新建
     * @param json DefJson 结构
     */
    @Transactional(rollbackFor = Exception.class)
    public Long deploy(Long id, String json, String remark) {
        if (StrUtil.isBlank(json)) {
            throw new ServiceException("流程模型不能为空");
        }
        try {
            DefJson defJson = objectMapper.readValue(json, DefJson.class);
            if (StrUtil.isBlank(defJson.getFlowCode())) {
                throw new ServiceException("流程编码不能为空");
            }
            if (StrUtil.isBlank(defJson.getModelValue())) {
                defJson.setModelValue("MIMIC");
            }
            if (StrUtil.isNotBlank(remark)) {
                defJson.setCategory(remark);
            }
            if (defJson.getIsPublish() == null) {
                defJson.setIsPublish(PublishStatus.UNPUBLISHED.getKey());
            }
            designService.normalize(defJson);
            Long targetId = id != null ? id : defJson.getId();
            if (targetId != null) {
                // 引擎 saveDef 更新场景不回填节点版本号，这里统一采用「新增版本」方式：
                // 旧版本保留，运行中实例不受影响，符合流程版本管理语义
                Definition old = defService.getById(targetId);
                if (old != null) {
                    defJson.setFlowCode(old.getFlowCode());
                    if (StrUtil.isBlank(defJson.getCategory())) {
                        defJson.setCategory(old.getCategory());
                    }
                }
                defJson.setId(null);
            }
            defJson.setIsPublish(PublishStatus.UNPUBLISHED.getKey());
            Definition created = defService.importDef(defJson);
            defService.publish(created.getId());
            return created.getId();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("流程部署失败", e);
            throw new ServiceException("流程部署失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        defService.removeDef(List.of(id));
    }

    /* ---------------- 流程实例 ---------------- */

    /**
     * 发起流程
     *
     * @param processId  流程定义 id
     * @param processKey 流程编码（processId 为空时使用）
     * @param variables  流程变量（含表单数据、附加参数）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long start(Long processId, String processKey, Map<String, Object> variables) {
        Definition definition = resolveDefinition(processId, processKey);
        if (definition == null) {
            throw new ServiceException("流程定义不存在或未发布");
        }
        if (!PublishStatus.PUBLISHED.getKey().equals(definition.getIsPublish())) {
            defService.publish(definition.getId());
        }
        String userId = FlowUserSupport.currentUserId();
        Map<String, Object> args = variables == null ? new HashMap<>() : new HashMap<>(variables);
        // 内置变量 handler：办理人表达式 ${handler} 取值来源
        args.putIfAbsent("handler", userId);
        args.putIfAbsent("initiator", userId);
        args.putIfAbsent("initiatorName", FlowUserSupport.currentUserName());

        try {
            FlowParams params = FlowParams.build()
                    .flowCode(definition.getFlowCode())
                    .handler(userId)
                    .permissionFlag(userDirectory.permissionFlags(userId))
                    .variable(args);
            Instance instance = insService.start(String.valueOf(IdUtils.nextId()), params);
            return instance.getId();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("流程发起失败", e);
            throw new ServiceException("流程发起失败：" + e.getMessage());
        }
    }

    /**
     * 办理通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long taskId, String username, String comment, Map<String, Object> variables) {
        Task task = requireTask(taskId);
        String userId = FlowUserSupport.currentUserId();
        Map<String, Object> args = variables == null ? new HashMap<>() : new HashMap<>(variables);
        if (StrUtil.isNotBlank(comment)) {
            args.put("approveComment", comment);
        }
        FlowParams params = FlowParams.build()
                .skipType(org.dromara.warm.flow.core.enums.SkipType.PASS.getKey())
                .handler(userId)
                .permissionFlag(userDirectory.permissionFlags(userId))
                .message(comment)
                .variable(args);
        taskService.skip(task.getId(), params);
    }

    /**
     * 退回：默认退回上一节点（发起节点）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String username, String comment) {
        Task task = requireTask(taskId);
        String userId = FlowUserSupport.currentUserId();
        FlowParams params = FlowParams.build()
                .handler(userId)
                .permissionFlag(userDirectory.permissionFlags(userId))
                .message(comment);
        String startNodeCode = startNodeCode(task.getInstanceId());
        if (StrUtil.isNotBlank(startNodeCode)) {
            taskService.rejectAtWill(task.getId(), startNodeCode, comment, null);
        } else {
            taskService.rejectLast(task.getId(), params);
        }
    }

    private Definition resolveDefinition(Long processId, String processKey) {
        if (processId != null) {
            return defService.getById(processId);
        }
        if (StrUtil.isNotBlank(processKey)) {
            return defService.getPublishByFlowCode(processKey);
        }
        return null;
    }

    /**
     * 退回目标：开始节点的下一个业务节点（引擎不允许直接退回开始节点）
     */
    private String startNodeCode(Long instanceId) {
        Instance instance = insService.getById(instanceId);
        if (instance == null) {
            return null;
        }
        List<String> next = jdbcTemplate.queryForList(
                "SELECT next_node_code FROM flow_skip WHERE definition_id = ? AND now_node_code = "
                        + "(SELECT node_code FROM flow_node WHERE definition_id = ? AND node_type = 0 AND del_flag = '0') "
                        + "AND del_flag = '0' LIMIT 1", String.class,
                instance.getDefinitionId(), instance.getDefinitionId());
        return next.isEmpty() ? null : next.get(0);
    }

    private Task requireTask(Long taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已办理");
        }
        return task;
    }

    /* ---------------- 任务 / 历史查询 ---------------- */

    public List<Map<String, Object>> myTasks(String userId) {
        String sql = "SELECT t.id, t.node_name, t.node_code, t.node_type, t.instance_id, t.create_time, "
                + "i.business_id, i.node_name AS current_node_name, i.create_by, d.flow_code, d.flow_name, "
                + "(SELECT GROUP_CONCAT(u2.processed_by) FROM flow_user u2 WHERE u2.associated = t.id AND u2.type = '1') AS actors "
                + "FROM flow_task t "
                + "JOIN flow_user u ON u.associated = t.id AND u.type = '1' AND u.processed_by = ? "
                + "JOIN flow_instance i ON i.id = t.instance_id "
                + "JOIN flow_definition d ON d.id = t.definition_id "
                + "WHERE t.del_flag = '0' ORDER BY t.create_time DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> taskMap(rs), userId);
    }

    public List<Map<String, Object>> allTasks() {
        String sql = "SELECT t.id, t.node_name, t.node_code, t.node_type, t.instance_id, t.create_time, "
                + "i.business_id, i.node_name AS current_node_name, i.create_by, d.flow_code, d.flow_name, "
                + "(SELECT GROUP_CONCAT(u2.processed_by) FROM flow_user u2 WHERE u2.associated = t.id AND u2.type = '1') AS actors "
                + "FROM flow_task t "
                + "JOIN flow_instance i ON i.id = t.instance_id "
                + "JOIN flow_definition d ON d.id = t.definition_id "
                + "WHERE t.del_flag = '0' ORDER BY t.create_time DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> taskMap(rs));
    }

    public List<Map<String, Object>> instances() {
        String sql = "SELECT i.id, i.business_id, i.node_name, i.node_code, i.flow_status, i.create_by, i.create_time, "
                + "i.update_time, d.flow_name, d.flow_code FROM flow_instance i "
                + "JOIN flow_definition d ON d.id = i.definition_id WHERE i.del_flag = '0' "
                + "ORDER BY i.create_time DESC LIMIT 100";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("instanceNo", rs.getString("business_id"));
            item.put("businessKey", rs.getString("business_id"));
            item.put("currentNodeName", rs.getString("node_name"));
            item.put("currentNodeKey", rs.getString("node_code"));
            item.put("state", rs.getString("flow_status"));
            item.put("createBy", displayName(rs.getString("create_by")));
            item.put("createTime", time(rs, "create_time"));
            item.put("lastUpdateTime", time(rs, "update_time"));
            item.put("processName", rs.getString("flow_name"));
            item.put("processKey", rs.getString("flow_code"));
            item.put("finished", isFinished(rs.getString("flow_status")));
            return item;
        });
    }

    public List<Map<String, Object>> history(boolean onlyMine, String userId) {
        StringBuilder sql = new StringBuilder("SELECT i.id, i.business_id, i.node_name, i.flow_status, i.create_by, ")
                .append("i.create_time, i.update_time, d.flow_name, d.flow_code FROM flow_instance i ")
                .append("JOIN flow_definition d ON d.id = i.definition_id ")
                .append("WHERE i.del_flag = '0' AND i.flow_status IN ('2','4','5','6','8','9','10')");
        List<Object> args = new ArrayList<>();
        if (onlyMine) {
            sql.append(" AND i.create_by = ?");
            args.add(userId);
        }
        sql.append(" ORDER BY i.update_time DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("instanceNo", rs.getString("business_id"));
            item.put("businessKey", rs.getString("business_id"));
            item.put("currentNodeName", rs.getString("node_name"));
            item.put("startUserName", displayName(rs.getString("create_by")));
            item.put("startTime", time(rs, "create_time"));
            item.put("endTime", time(rs, "update_time"));
            Timestamp start = rs.getTimestamp("create_time");
            Timestamp end = rs.getTimestamp("update_time");
            item.put("duration", start == null || end == null ? null : (end.getTime() - start.getTime()) / 1000);
            item.put("state", rs.getString("flow_status"));
            item.put("finished", isFinished(rs.getString("flow_status")));
            item.put("processName", rs.getString("flow_name"));
            item.put("processKey", rs.getString("flow_code"));
            return item;
        }, args.toArray());
    }

    private Map<String, Object> taskMap(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("name", rs.getString("node_name"));
        item.put("taskKey", rs.getString("node_code"));
        item.put("taskType", rs.getInt("node_type"));
        item.put("instanceId", rs.getLong("instance_id"));
        item.put("instanceNo", rs.getString("business_id"));
        item.put("businessKey", rs.getString("business_id"));
        item.put("currentNodeName", rs.getString("current_node_name"));
        item.put("processName", rs.getString("flow_name"));
        item.put("processKey", rs.getString("flow_code"));
        item.put("createBy", displayName(rs.getString("create_by")));
        item.put("createTime", time(rs, "create_time"));
        item.put("actors", actorNames(rs.getString("actors")));
        return item;
    }

    private String actorNames(String actors) {
        if (StrUtil.isBlank(actors)) {
            return "";
        }
        List<String> ids = List.of(actors.split(","));
        Map<String, String> names = userDirectory.displayNames(ids);
        return String.join(",", ids.stream().map(id -> names.getOrDefault(id, id)).toList());
    }

    private String displayName(String userId) {
        return StrUtil.isBlank(userId) ? "" : userDirectory.displayName(userId);
    }

    private boolean isFinished(String flowStatus) {
        return flowStatus != null && List.of("2", "4", "5", "6", "8", "10").contains(flowStatus);
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
