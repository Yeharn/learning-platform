/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程查询服务：分页、实例详情、审批记录、沟通评论、候选人
 */

package org.yeharn.lp.flow.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.utils.IdUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.flow.support.FlowUserDirectory;
import org.yeharn.lp.flow.support.FlowUserSupport;
import org.yeharn.lp.flow.support.FlowVariableSupport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一封装流程读模型，SQL 直接面向 flow_* 表，便于与业务系统字段对齐
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowQueryService {

    private final JdbcTemplate jdbcTemplate;

    private final DefService defService;

    private final InsService insService;

    private final FlowUserDirectory userDirectory;

    private final FlowVariableSupport variableSupport;

    private final FlowFormService formService;

    private final ObjectMapper objectMapper;

    /* ---------------- 待办 / 已办 ---------------- */

    public PageResult<Map<String, Object>> pageMyTasks(String keyword, long pageNum, long pageSize) {
        String userId = FlowUserSupport.currentUserId();
        StringBuilder where = new StringBuilder(" FROM flow_task t ")
                .append("JOIN flow_user u ON u.associated = t.id AND u.type = '1' AND u.processed_by = ? ")
                .append("JOIN flow_instance i ON i.id = t.instance_id ")
                .append("JOIN flow_definition d ON d.id = t.definition_id ")
                .append("WHERE t.del_flag = '0'");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (StrUtil.isNotBlank(keyword)) {
            where.append(" AND (d.flow_name LIKE ? OR t.node_name LIKE ? OR i.business_id LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        long total = queryCount("SELECT COUNT(*)" + where, args.toArray());
        String sql = "SELECT t.id, t.node_name, t.node_code, t.instance_id, t.create_time, i.business_id, "
                + "d.flow_name, d.flow_code, i.create_by" + where + " ORDER BY t.create_time DESC LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("name", rs.getString("node_name"));
            item.put("nodeCode", rs.getString("node_code"));
            item.put("instanceId", rs.getLong("instance_id"));
            item.put("instanceNo", rs.getString("business_id"));
            item.put("processName", rs.getString("flow_name"));
            item.put("processKey", rs.getString("flow_code"));
            item.put("createBy", userName(rs.getString("create_by")));
            item.put("createTime", time(rs, "create_time"));
            return item;
        }, pageArgs.toArray());
        return new PageResult<>(total, rows, pageNum, pageSize);
    }

    public PageResult<Map<String, Object>> pageDoneTasks(String keyword, long pageNum, long pageSize) {
        String userId = FlowUserSupport.currentUserId();
        StringBuilder where = new StringBuilder(" FROM flow_his_task h ")
                .append("JOIN flow_instance i ON i.id = h.instance_id ")
                .append("JOIN flow_definition d ON d.id = h.definition_id ")
                .append("WHERE h.del_flag = '0' AND h.approver = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (StrUtil.isNotBlank(keyword)) {
            where.append(" AND (d.flow_name LIKE ? OR h.node_name LIKE ? OR i.business_id LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        long total = queryCount("SELECT COUNT(*)" + where, args.toArray());
        String sql = "SELECT h.id, h.node_name, h.node_code, h.instance_id, h.message, h.skip_type, h.update_time, "
                + "i.business_id, d.flow_name, d.flow_code" + where + " ORDER BY h.update_time DESC LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("name", rs.getString("node_name"));
            item.put("nodeCode", rs.getString("node_code"));
            item.put("instanceId", rs.getLong("instance_id"));
            item.put("instanceNo", rs.getString("business_id"));
            item.put("processName", rs.getString("flow_name"));
            item.put("processKey", rs.getString("flow_code"));
            item.put("comment", rs.getString("message"));
            item.put("state", "REJECT".equals(rs.getString("skip_type")) ? 3 : 2);
            item.put("finishTime", time(rs, "update_time"));
            return item;
        }, pageArgs.toArray());
        return new PageResult<>(total, rows, pageNum, pageSize);
    }

    /* ---------------- 实例 ---------------- */

    public PageResult<Map<String, Object>> pageInstances(String keyword, String state, long pageNum, long pageSize) {
        StringBuilder where = new StringBuilder(" FROM flow_instance i JOIN flow_definition d ON d.id = i.definition_id ")
                .append("WHERE i.del_flag = '0'");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword)) {
            where.append(" AND (d.flow_name LIKE ? OR i.business_id LIKE ? OR i.node_name LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        if (StrUtil.isNotBlank(state)) {
            if ("running".equals(state)) {
                where.append(" AND i.flow_status IN ('0','1')");
            } else if ("finished".equals(state)) {
                where.append(" AND i.flow_status IN ('2','8')");
            } else if ("abort".equals(state)) {
                where.append(" AND i.flow_status IN ('4','5','6','9','10','11')");
            }
        }
        long total = queryCount("SELECT COUNT(*)" + where, args.toArray());
        String sql = "SELECT i.id, i.business_id, i.node_name, i.node_code, i.flow_status, i.create_by, i.create_time, "
                + "i.update_time, d.flow_name, d.flow_code" + where
                + " ORDER BY i.create_time DESC LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("instanceNo", rs.getString("business_id"));
            item.put("businessKey", rs.getString("business_id"));
            item.put("processName", rs.getString("flow_name"));
            item.put("processKey", rs.getString("flow_code"));
            item.put("currentNodeName", rs.getString("node_name"));
            item.put("currentNodeKey", rs.getString("node_code"));
            item.put("flowStatus", rs.getString("flow_status"));
            item.put("state", rs.getString("flow_status"));
            item.put("createBy", userName(rs.getString("create_by")));
            item.put("createTime", time(rs, "create_time"));
            item.put("lastUpdateTime", time(rs, "update_time"));
            item.put("finished", FlowStatus.isFinished(rs.getString("flow_status")));
            return item;
        }, pageArgs.toArray());
        return new PageResult<>(total, rows, pageNum, pageSize);
    }

    public Map<String, Object> instanceDetail(Long instanceId) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT i.*, d.flow_name, d.flow_code, d.form_path FROM flow_instance i "
                        + "JOIN flow_definition d ON d.id = i.definition_id WHERE i.id = ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("definitionId", rs.getLong("definition_id"));
                    item.put("instanceNo", rs.getString("business_id"));
                    item.put("businessKey", rs.getString("business_id"));
                    item.put("processName", rs.getString("flow_name"));
                    item.put("processKey", rs.getString("flow_code"));
                    item.put("currentNodeName", rs.getString("node_name"));
                    item.put("currentNodeKey", rs.getString("node_code"));
                    item.put("flowStatus", rs.getString("flow_status"));
                    item.put("createBy", userName(rs.getString("create_by")));
                    item.put("createTime", time(rs, "create_time"));
                    item.put("lastUpdateTime", time(rs, "update_time"));
                    item.put("finished", FlowStatus.isFinished(rs.getString("flow_status")));
                    item.put("variables", variableSupport.read(rs.getString("variable")));
                    item.put("formCode", rs.getString("form_path"));
                    return item;
                }, instanceId);
        if (rows.isEmpty()) {
            throw new ServiceException("流程实例不存在");
        }
        Map<String, Object> instance = rows.get(0);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("instance", instance);
        detail.put("form", formService.detailByCode((String) instance.get("formCode")));
        detail.put("nodes", nodes((Long) instance.get("definitionId"), (String) instance.get("currentNodeKey")));
        detail.put("records", records(instanceId));
        detail.put("comments", comments(instanceId));
        return detail;
    }

    /**
     * 流程图节点：按连线 BFS 计算层级，并标记节点状态（2 已完成 / 1 进行中 / 0 未开始）
     */
    private List<Map<String, Object>> nodes(Long definitionId, String currentNodeKey) {
        List<Map<String, Object>> nodeList = jdbcTemplate.query(
                "SELECT node_code, node_name, node_type, permission_flag FROM flow_node "
                        + "WHERE definition_id = ? AND del_flag = '0' ORDER BY id", (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", rs.getString("node_code"));
                    item.put("name", rs.getString("node_name"));
                    item.put("type", rs.getInt("node_type"));
                    item.put("permissionFlag", rs.getString("permission_flag"));
                    return item;
                }, definitionId);
        List<Map<String, Object>> skips = jdbcTemplate.query(
                "SELECT now_node_code, next_node_code FROM flow_skip WHERE definition_id = ? AND del_flag = '0'",
                (rs, rowNum) -> Map.of("now", rs.getString("now_node_code"), "next", rs.getString("next_node_code")),
                definitionId);
        Map<String, List<String>> nextMap = new HashMap<>();
        for (Map<String, Object> skip : skips) {
            nextMap.computeIfAbsent(String.valueOf(skip.get("now")), key -> new ArrayList<>())
                    .add(String.valueOf(skip.get("next")));
        }
        Map<String, Integer> levelMap = new HashMap<>();
        Map<String, Object> start = nodeList.stream()
                .filter(n -> NodeType.isStart((Integer) n.get("type"))).findFirst().orElse(null);
        if (start != null) {
            List<String> queue = new ArrayList<>();
            String startCode = String.valueOf(start.get("code"));
            queue.add(startCode);
            levelMap.put(startCode, 0);
            for (int i = 0; i < queue.size() && i < 500; i++) {
                String code = queue.get(i);
                int level = levelMap.getOrDefault(code, 0);
                for (String next : nextMap.getOrDefault(code, List.of())) {
                    if (!levelMap.containsKey(next)) {
                        levelMap.put(next, level + 1);
                        queue.add(next);
                    }
                }
            }
        }
        Set<String> finishedNodes = new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT DISTINCT h.node_code FROM flow_his_task h WHERE h.instance_id IN "
                        + "(SELECT id FROM flow_instance WHERE definition_id = ?) AND h.skip_type = 'PASS'",
                String.class, definitionId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : nodeList) {
            String code = String.valueOf(node.get("code"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeKey", code);
            item.put("nodeName", node.get("name"));
            item.put("type", node.get("type"));
            item.put("level", levelMap.getOrDefault(code, 0));
            item.put("nodeState", code.equals(currentNodeKey) ? 1 : (finishedNodes.contains(code) ? 2 : 0));
            item.put("assignees", assigneeNames((String) node.get("permissionFlag")));
            result.add(item);
        }
        result.sort((a, b) -> Integer.compare((Integer) a.get("level"), (Integer) b.get("level")));
        return result;
    }

    private List<String> assigneeNames(String permissionFlag) {
        if (StrUtil.isBlank(permissionFlag)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String part : permissionFlag.split("@@")) {
            if (part.startsWith("role:") || part.startsWith("dept:")) {
                ids.add(part);
            } else if (part.startsWith("user:")) {
                ids.add(part.substring(5));
            } else if (part.contains("${") || part.contains("#{")) {
                ids.add(part);
            } else {
                ids.add(part);
            }
        }
        Map<String, String> names = userDirectory.displayNames(ids.stream()
                .filter(id -> !id.startsWith("role:") && !id.startsWith("dept:") && !id.contains("${") && !id.contains("#{"))
                .toList());
        List<String> result = new ArrayList<>();
        for (String id : ids) {
            if (id.startsWith("role:")) {
                result.add("角色" + id.substring(5));
            } else if (id.startsWith("dept:")) {
                result.add("部门" + id.substring(5));
            } else if (id.contains("${") || id.contains("#{")) {
                result.add(id);
            } else {
                result.add(names.getOrDefault(id, id));
            }
        }
        return result;
    }

    /* ---------------- 审批记录 ---------------- */

    public List<Map<String, Object>> records(Long instanceId) {
        return jdbcTemplate.query(
                "SELECT h.id, h.node_name, h.node_code, h.approver, h.collaborator, h.cooperate_type, h.skip_type, "
                        + "h.message, h.create_time, h.update_time FROM flow_his_task h "
                        + "WHERE h.instance_id = ? AND h.del_flag = '0' ORDER BY h.update_time ASC, h.id ASC",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("name", rs.getString("node_name"));
                    item.put("nodeCode", rs.getString("node_code"));
                    item.put("actors", userName(rs.getString("approver")));
                    item.put("cooperateType", rs.getObject("cooperate_type"));
                    item.put("createBy", userName(rs.getString("approver")));
                    item.put("state", "REJECT".equals(rs.getString("skip_type")) ? 3 : 2);
                    item.put("skipType", rs.getString("skip_type"));
                    item.put("comment", rs.getString("message"));
                    item.put("finishTime", time(rs, "update_time"));
                    item.put("createTime", time(rs, "create_time"));
                    return item;
                }, instanceId);
    }

    /**
     * 可退回节点：已流转过的中间节点
     */
    public List<Map<String, Object>> rejectNodes(Long instanceId) {
        return jdbcTemplate.query(
                "SELECT DISTINCT h.node_code, h.node_name FROM flow_his_task h "
                        + "WHERE h.instance_id = ? AND h.del_flag = '0' AND h.node_code IS NOT NULL "
                        + "AND h.node_code <> '' ORDER BY h.update_time ASC",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("nodeKey", rs.getString("node_code"));
                    item.put("nodeName", rs.getString("node_name"));
                    return item;
                }, instanceId);
    }

    /* ---------------- 沟通评论 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    public void comment(Long instanceId, Long taskId, String content) {
        if (StrUtil.isBlank(content)) {
            throw new ServiceException("评论内容不能为空");
        }
        String userId = FlowUserSupport.currentUserId();
        jdbcTemplate.update("INSERT INTO lp_flow_comment (id, instance_id, task_id, user_id, user_name, content, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                IdUtils.nextId(), instanceId, taskId, userId, FlowUserSupport.currentUserName(), content);
        // 沟通即视为已阅
        markRead(instanceId, taskId);
    }

    public List<Map<String, Object>> comments(Long instanceId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, user_name, content, create_time FROM lp_flow_comment "
                        + "WHERE instance_id = ? ORDER BY create_time ASC",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("userId", rs.getString("user_id"));
                    item.put("userName", rs.getString("user_name"));
                    item.put("content", rs.getString("content"));
                    item.put("createTime", time(rs, "create_time"));
                    return item;
                }, instanceId);
    }

    public void markRead(Long instanceId, Long taskId) {
        String userId = FlowUserSupport.currentUserId();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lp_flow_read WHERE instance_id = ? AND user_id = ?",
                Integer.class, instanceId, userId);
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO lp_flow_read (id, instance_id, task_id, user_id, user_name, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, NOW())",
                IdUtils.nextId(), instanceId, taskId, userId, FlowUserSupport.currentUserName());
    }

    public List<Map<String, Object>> readRecords(Long instanceId) {
        return jdbcTemplate.query("SELECT user_id, user_name, create_time FROM lp_flow_read WHERE instance_id = ? ORDER BY create_time",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", rs.getString("user_id"));
                    item.put("userName", rs.getString("user_name"));
                    item.put("createTime", time(rs, "create_time"));
                    return item;
                }, instanceId);
    }

    /* ---------------- 候选人 / 组织 ---------------- */

    public List<Map<String, Object>> candidates(String keyword) {
        return userDirectory.candidates(keyword);
    }

    public List<Map<String, Object>> roles() {
        return userDirectory.roles();
    }

    public List<Map<String, Object>> depts() {
        return userDirectory.depts();
    }

    /* ---------------- 工具 ---------------- */

    private long queryCount(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private String userName(String userId) {
        return StrUtil.isBlank(userId) ? "" : userDirectory.displayName(userId);
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 表单定义（供实例详情渲染）
     */
    public JsonNode formFields(String formCode) {
        if (StrUtil.isBlank(formCode)) {
            return null;
        }
        try {
            return objectMapper.readTree(formService.detailByCode(formCode).get("fields").toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 流程定义详情（含节点树）
     */
    public Map<String, Object> definitionDetail(Long definitionId) {
        Definition definition = defService.getById(definitionId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("definition", definition);
        detail.put("form", formService.detailByCode(definition == null ? null : definition.getFormPath()));
        return detail;
    }

    /**
     * 判断实例是否处于挂起状态
     */
    public boolean isSuspended(Long instanceId) {
        var instance = insService.getById(instanceId);
        return instance != null && instance.getActivityStatus() != null && instance.getActivityStatus() == 0;
    }
}
