/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 部门接口控制器
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.entity.SysDept;
import org.yeharn.lp.system.service.SysDeptService;

import java.util.List;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @SaCheckPermission("system:dept:list")
    @GetMapping("/list")
    public R<List<SysDept>> list(@RequestParam(required = false) String deptName,
                                 @RequestParam(required = false) String status) {
        return R.ok(deptService.list(deptName, status));
    }

    @GetMapping("/treeselect")
    public R<List<SysDept>> treeSelect() {
        return R.ok(deptService.buildTree(deptService.listAll(), 0L));
    }

    @SaCheckPermission("system:dept:query")
    @GetMapping("/{deptId}")
    public R<SysDept> getInfo(@PathVariable Long deptId) {
        return R.ok(deptService.getById(deptId));
    }

    @SaCheckPermission("system:dept:add")
    @OperLog(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysDept dept) {
        deptService.add(dept);
        return R.ok();
    }

    @SaCheckPermission("system:dept:edit")
    @OperLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysDept dept) {
        deptService.update(dept);
        return R.ok();
    }

    @SaCheckPermission("system:dept:remove")
    @OperLog(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public R<Void> remove(@PathVariable Long deptId) {
        deptService.delete(deptId);
        return R.ok();
    }
}
