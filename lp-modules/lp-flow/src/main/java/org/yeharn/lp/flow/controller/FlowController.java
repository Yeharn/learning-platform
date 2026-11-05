/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程引擎接口
 */

package org.yeharn.lp.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.flow.service.FlowActionService;
import org.yeharn.lp.flow.service.FlowDesignService;
import org.yeharn.lp.flow.service.FlowFormService;
import org.yeharn.lp.flow.service.FlowModelBuilder;
import org.yeharn.lp.flow.service.FlowQueryService;
import org.yeharn.lp.flow.service.FlowService;

import java.util.List;
import java.util.Map;

/**
 * 流程引擎（FlowLong 飞龙）
 */
@RestController
@RequestMapping("/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;
    private final FlowModelBuilder modelBuilder;
    private final FlowActionService flowActionService;
    private final FlowQueryService flowQueryService;
    private final FlowFormService flowFormService;
    private final FlowDesignService flowDesignService;

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/definitions")
    public R<Object> definitions(@RequestParam(required = false) String keyword) {
        return R.ok(flowService.definitions(keyword));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/definitions/{id}/model")
    public R<String> model(@PathVariable Long id) {
        return R.ok(flowService.model(id));
    }

    /**
     * 表单化建模：审批节点 + 抄送节点自动生成 FlowLong JSON 模型并部署
     */
    @SaCheckPermission("tool:flow:deploy")
    @OperLog(title = "流程建模部署", businessType = BusinessType.IMPORT, saveParam = false)
    @SuppressWarnings("unchecked")
    @PostMapping("/model")
    public R<Long> deployModel(@RequestBody Map<String, Object> body) {
        String key = String.valueOf(body.get("key"));
        String name = String.valueOf(body.get("name"));
        List<Map<String, String>> nodes = (List<Map<String, String>>) body.get("nodes");
        List<Map<String, String>> cc = (List<Map<String, String>>) body.getOrDefault("cc", List.of());
        String json = modelBuilder.build(key, name, nodes, cc);
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        return R.ok("部署成功", flowService.deploy(id, json, (String) body.get("remark")));
    }

    /**
     * 直接提交 JSON 模型（设计器导出或手工编写）
     */
    @SaCheckPermission("tool:flow:deploy")
    @OperLog(title = "流程 JSON 部署", businessType = BusinessType.IMPORT, saveParam = false)
    @PostMapping("/deploy")
    public R<Long> deploy(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        return R.ok("部署成功", flowService.deploy(id, (String) body.get("model"), (String) body.get("remark")));
    }

    @SaCheckPermission("tool:flow:remove")
    @OperLog(title = "流程删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/definitions/{id}")
    public R<Void> remove(@PathVariable Long id) {
        flowService.remove(id);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:start")
    @OperLog(title = "流程发起", businessType = BusinessType.INSERT, saveParam = false)
    @SuppressWarnings("unchecked")
    @PostMapping("/start")
    public R<Long> start(@RequestBody Map<String, Object> body) {
        Long processId = body.get("processId") == null ? null : Long.valueOf(String.valueOf(body.get("processId")));
        String processKey = body.get("key") == null ? null : String.valueOf(body.get("key"));
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        return R.ok("流程已发起", flowService.start(processId, processKey, variables));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/tasks")
    public R<Object> myTasks() {
        return R.ok(flowService.myTasks(String.valueOf(SecurityUtils.getUserId())));
    }

    @SaCheckPermission("tool:flow:execute")
    @GetMapping("/all-tasks")
    public R<Object> allTasks() {
        return R.ok(flowService.allTasks());
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程办理", businessType = BusinessType.UPDATE, saveParam = false)
    @SuppressWarnings("unchecked")
    @PostMapping("/tasks/{taskId}/complete")
    public R<Void> complete(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> variables = body == null ? null : (Map<String, Object>) body.get("variables");
        String comment = body == null ? null : (String) body.get("comment");
        flowService.complete(taskId, SecurityUtils.getUsername(), comment, variables);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程驳回", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/reject")
    public R<Void> reject(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        flowService.reject(taskId, SecurityUtils.getUsername(), body == null ? null : (String) body.get("comment"));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances")
    public R<Object> instances() {
        return R.ok(flowService.instances());
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/history")
    public R<Object> history(@RequestParam(defaultValue = "false") boolean onlyMine) {
        return R.ok(flowService.history(onlyMine, String.valueOf(SecurityUtils.getUserId())));
    }

    /* ---------------- 分页查询 ---------------- */

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/tasks/page")
    public R<PageResult<Map<String, Object>>> pageTasks(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(flowQueryService.pageMyTasks(keyword, pageNum, pageSize));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/tasks/done")
    public R<PageResult<Map<String, Object>>> doneTasks(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(flowQueryService.pageDoneTasks(keyword, pageNum, pageSize));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/page")
    public R<PageResult<Map<String, Object>>> pageInstances(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String state,
                                                            @RequestParam(defaultValue = "1") long pageNum,
                                                            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(flowQueryService.pageInstances(keyword, state, pageNum, pageSize));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/detail")
    public R<Map<String, Object>> instanceDetail(@PathVariable Long id) {
        return R.ok(flowQueryService.instanceDetail(id));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/records")
    public R<List<Map<String, Object>>> records(@PathVariable Long id) {
        return R.ok(flowQueryService.records(id));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/reject-nodes")
    public R<List<Map<String, Object>>> rejectNodes(@PathVariable Long id) {
        return R.ok(flowQueryService.rejectNodes(id));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/candidates")
    public R<List<Map<String, Object>>> candidates(@RequestParam(required = false) String keyword) {
        return R.ok(flowQueryService.candidates(keyword));
    }

    /* ---------------- 任务动作：加签 / 减签 / 转办 / 委托 / 拿回 / 撤销 / 已阅 / 任意驳回 ---------------- */

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程加签", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/add-sign")
    public R<Void> addSign(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        flowActionService.addSign(taskId, toLongList(body.get("userIds")), (String) body.get("comment"));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程减签", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/reduce-sign")
    public R<Void> reduceSign(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        flowActionService.reduceSign(taskId, (String) body.get("actorId"), (String) body.get("comment"));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程转办", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/transfer")
    public R<Void> transfer(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        flowActionService.transfer(taskId, Long.valueOf(String.valueOf(body.get("userId"))));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程委托", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/delegate")
    public R<Void> delegate(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        flowActionService.delegate(taskId, Long.valueOf(String.valueOf(body.get("userId"))));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程拿回", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/reclaim")
    public R<Void> reclaim(@PathVariable Long taskId) {
        flowActionService.reclaim(taskId);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程撤销", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/withdraw")
    public R<Void> withdraw(@PathVariable Long taskId) {
        flowActionService.withdraw(taskId);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:list")
    @PostMapping("/tasks/{taskId}/read")
    public R<Void> read(@PathVariable Long taskId) {
        flowActionService.read(taskId);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程驳回指定节点", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/reject-to")
    public R<Void> rejectTo(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        String nodeKey = body == null ? null : (String) body.get("nodeKey");
        String comment = body == null ? null : (String) body.get("comment");
        flowActionService.rejectTo(taskId, nodeKey, comment);
        return R.ok();
    }

    /* ---------------- 实例管控 ---------------- */

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程挂起", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{id}/suspend")
    public R<Void> suspend(@PathVariable Long id) {
        flowActionService.suspend(id);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程激活", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{id}/activate")
    public R<Void> activate(@PathVariable Long id) {
        flowActionService.activate(id);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:execute")
    @OperLog(title = "流程终止", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{id}/terminate")
    public R<Void> terminate(@PathVariable Long id) {
        flowActionService.terminate(id);
        return R.ok();
    }

    @SaCheckPermission("tool:flow:remove")
    @OperLog(title = "流程实例删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/instances/{id}")
    public R<Void> removeInstance(@PathVariable Long id) {
        flowActionService.removeInstance(id);
        return R.ok();
    }

    /* ---------------- 沟通 ---------------- */

    @SaCheckPermission("tool:flow:list")
    @PostMapping("/instances/{id}/comments")
    public R<Void> comment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long taskId = body.get("taskId") == null ? null : Long.valueOf(String.valueOf(body.get("taskId")));
        flowQueryService.comment(id, taskId, (String) body.get("content"));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/comments")
    public R<List<Map<String, Object>>> comments(@PathVariable Long id) {
        return R.ok(flowQueryService.comments(id));
    }

    /* ---------------- 表单模板 ---------------- */

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/forms")
    public R<List<Map<String, Object>>> forms(@RequestParam(required = false) String keyword) {
        return R.ok(flowFormService.list(keyword));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/forms/{id}")
    public R<Map<String, Object>> form(@PathVariable Long id) {
        return R.ok(flowFormService.get(id));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/forms/by-code")
    public R<Map<String, Object>> formByCode(@RequestParam String formCode) {
        return R.ok(flowFormService.detailByCode(formCode));
    }

    @SaCheckPermission("tool:flow:form")
    @OperLog(title = "流程表单保存", businessType = BusinessType.UPDATE)
    @PostMapping("/forms")
    public R<Long> saveForm(@RequestBody Map<String, Object> body) {
        return R.ok("保存成功", flowFormService.save(body));
    }

    @SaCheckPermission("tool:flow:form")
    @OperLog(title = "流程表单删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/forms/{id}")
    public R<Void> removeForm(@PathVariable Long id) {
        flowFormService.remove(id);
        return R.ok();
    }

    /* ---------------- 设计器校验 / 预览 ---------------- */

    @SaCheckPermission("tool:flow:list")
    @PostMapping("/design/preview")
    public R<Map<String, Object>> previewDesign(@RequestBody Map<String, Object> body) {
        return R.ok(flowDesignService.previewJson((String) body.get("model")));
    }

    /* ---------------- 组织数据（办理人选择） ---------------- */

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/roles")
    public R<List<Map<String, Object>>> roles() {
        return R.ok(flowQueryService.roles());
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/depts")
    public R<List<Map<String, Object>>> depts() {
        return R.ok(flowQueryService.depts());
    }

    /* ---------------- 任意跳转 / 流程变量 / 已阅 ---------------- */

    @SaCheckPermission("tool:flow:monitor")
    @OperLog(title = "流程任意跳转", businessType = BusinessType.UPDATE, saveParam = false)
    @PostMapping("/tasks/{taskId}/jump")
    public R<Void> jump(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        flowActionService.jumpTo(taskId, (String) body.get("nodeKey"), (String) body.get("comment"));
        return R.ok();
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/variables")
    public R<Map<String, Object>> variables(@PathVariable Long id) {
        return R.ok(flowActionService.variables(id));
    }

    @SaCheckPermission("tool:flow:list")
    @GetMapping("/instances/{id}/reads")
    public R<List<Map<String, Object>>> reads(@PathVariable Long id) {
        return R.ok(flowQueryService.readRecords(id));
    }

    private List<Long> toLongList(Object value) {
        List<Long> ids = new java.util.ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    ids.add(Long.valueOf(String.valueOf(item)));
                }
            }
        } else if (value != null) {
            ids.add(Long.valueOf(String.valueOf(value)));
        }
        return ids;
    }
}
