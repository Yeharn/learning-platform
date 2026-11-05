/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程任务动作服务
 */

package org.yeharn.lp.flow.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.flow.support.FlowUserDirectory;
import org.yeharn.lp.flow.support.FlowUserSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务动作：加签、减签、转办、委派、拿回、撤销、任意跳转、任意节点退回、终止与实例管控
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowActionService {

    private final TaskService taskService;

    private final InsService insService;

    private final JdbcTemplate jdbcTemplate;

    private final FlowUserDirectory userDirectory;

    private final FlowQueryService flowQueryService;

    /* ---------------- 加签 / 减签 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    public void addSign(Long taskId, List<Long> userIds, String comment) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException("请选择加签人员");
        }
        FlowParams params = baseParams().addHandlers(userIds.stream().map(String::valueOf).toList());
        if (StrUtil.isNotBlank(comment)) {
            params.message(comment);
        }
        requireTrue(taskService.addSignature(taskId, params), "加签失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(Long taskId, String actorId, String comment) {
        if (StrUtil.isBlank(actorId)) {
            throw new ServiceException("请选择减签人员");
        }
        FlowParams params = baseParams().reductionHandlers(List.of(actorId));
        if (StrUtil.isNotBlank(comment)) {
            params.message(comment);
        }
        requireTrue(taskService.reductionSignature(taskId, params), "减签失败");
    }

    /* ---------------- 转办 / 委派 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请选择转办人员");
        }
        FlowParams params = baseParams().addHandlers(List.of(String.valueOf(userId)));
        requireTrue(taskService.transfer(taskId, params), "转办失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delegate(Long taskId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请选择委派人员");
        }
        FlowParams params = baseParams().addHandlers(List.of(String.valueOf(userId)));
        requireTrue(taskService.depute(taskId, params), "委派失败");
    }

    /* ---------------- 拿回 / 撤销 / 已阅 ---------------- */

    /**
     * 拿回：当前待办退回给上一办理人（需在下一节点未办理前）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reclaim(Long taskId) {
        taskService.taskBack(taskId, baseParams().message("拿回任务"));
    }

    /**
     * 撤销：发起人撤销整个流程实例
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long taskId) {
        Task task = requireTask(taskId);
        taskService.revoke(task.getInstanceId(), baseParams().message("撤销流程"));
    }

    public void read(Long taskId) {
        Task task = requireTask(taskId);
        flowQueryService.markRead(task.getInstanceId(), taskId);
    }

    /* ---------------- 任意节点退回 / 任意跳转 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    public void rejectTo(Long taskId, String nodeKey, String comment) {
        FlowParams params = baseParams();
        if (StrUtil.isNotBlank(comment)) {
            params.message(comment);
        }
        if (StrUtil.isBlank(nodeKey)) {
            taskService.rejectLast(taskId, params);
        } else {
            taskService.rejectAtWill(taskId, resolveRejectNode(taskId, nodeKey), comment, null);
        }
    }

    /**
     * 退回目标兜底：开始节点与网关节点（互斥/并行/包容）不允许作为退回目标，
     * 自动沿连线向下找到第一个可退回的业务节点
     */
    private String resolveRejectNode(Long taskId, String nodeKey) {
        Task task = requireTask(taskId);
        var instance = insService.getById(task.getInstanceId());
        if (instance == null) {
            return nodeKey;
        }
        Long definitionId = instance.getDefinitionId();
        String current = nodeKey;
        for (int i = 0; i < 10; i++) {
            Integer nodeType = jdbcTemplate.queryForObject(
                    "SELECT node_type FROM flow_node WHERE definition_id = ? AND node_code = ? AND del_flag = '0'",
                    Integer.class, definitionId, current);
            // 开始节点与网关节点不能作为退回目标，继续向下找业务节点
            boolean needProbe = nodeType == null || nodeType == 0 || (nodeType >= 3 && nodeType <= 5);
            if (!needProbe) {
                break;
            }
            List<String> next = jdbcTemplate.queryForList(
                    "SELECT next_node_code FROM flow_skip WHERE definition_id = ? AND now_node_code = ? AND del_flag = '0' LIMIT 1",
                    String.class, definitionId, current);
            if (next.isEmpty()) {
                break;
            }
            current = next.get(0);
        }
        return current;
    }

    /**
     * 任意跳转（流程监控/管理员干预，忽略办理权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void jumpTo(Long taskId, String nodeKey, String comment) {
        if (StrUtil.isBlank(nodeKey)) {
            throw new ServiceException("请选择目标节点");
        }
        FlowParams params = baseParams().ignore(true);
        if (StrUtil.isNotBlank(comment)) {
            params.message(comment);
        }
        taskService.passAtWill(taskId, nodeKey, comment, null);
    }

    /* ---------------- 实例管控 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    public void suspend(Long instanceId) {
        insService.unActive(instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activate(Long instanceId) {
        insService.active(instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long instanceId) {
        List<Task> tasks = taskService.getByInsId(instanceId);
        if (tasks.isEmpty()) {
            throw new ServiceException("实例已无待办任务，无法终止");
        }
        taskService.termination(tasks.get(0).getId(), baseParams().ignore(true).message("流程终止"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeInstance(Long instanceId) {
        insService.remove(List.of(instanceId));
    }

    /* ---------------- 流程变量 ---------------- */

    public Map<String, Object> variables(Long instanceId) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT variable FROM flow_instance WHERE id = ?", (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("variable", rs.getString("variable"));
                    return item;
                }, instanceId);
        if (rows.isEmpty()) {
            throw new ServiceException("流程实例不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instanceId", instanceId);
        result.put("variable", rows.get(0).get("variable"));
        return result;
    }

    private FlowParams baseParams() {
        String userId = FlowUserSupport.currentUserId();
        return FlowParams.build()
                .skipType(org.dromara.warm.flow.core.enums.SkipType.REJECT.getKey())
                .handler(userId)
                .permissionFlag(userDirectory.permissionFlags(userId));
    }

    private Task requireTask(Long taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已办理");
        }
        return task;
    }

    private void requireTrue(boolean result, String message) {
        if (!result) {
            throw new ServiceException(message);
        }
    }
}
