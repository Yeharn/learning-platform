/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色接口控制器
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
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.dto.RoleQuery;
import org.yeharn.lp.system.entity.SysRole;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.service.SysRoleService;

import java.util.List;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public R<PageResult<SysRole>> list(RoleQuery query) {
        return R.ok(roleService.page(query));
    }

    @GetMapping("/optionselect")
    public R<List<SysRole>> optionSelect() {
        return R.ok(roleService.listAll());
    }

    @SaCheckPermission("system:role:query")
    @GetMapping("/{roleId}")
    public R<SysRole> getInfo(@PathVariable Long roleId) {
        return R.ok(roleService.getById(roleId));
    }

    @SaCheckPermission("system:role:add")
    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysRole role) {
        roleService.add(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:edit")
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysRole role) {
        roleService.update(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:remove")
    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public R<Void> remove(@PathVariable List<Long> roleIds) {
        roleService.delete(roleIds);
        return R.ok();
    }

    @SaCheckPermission("system:role:auth")
    @OperLog(title = "角色管理-分配权限", businessType = BusinessType.GRANT)
    @PutMapping("/auth")
    public R<Void> auth(@RequestBody SysRole role) {
        roleService.saveAuth(role.getRoleId(), role.getMenuIds(), role.getDeptIds());
        return R.ok();
    }

    @SaCheckPermission("system:role:edit")
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysRole role) {
        roleService.changeStatus(role.getRoleId(), role.getStatus());
        return R.ok();
    }

    @SaCheckPermission("system:role:query")
    @GetMapping("/users/{roleId}")
    public R<List<SysUser>> users(@PathVariable Long roleId) {
        return R.ok(roleService.listUsers(roleId));
    }

    @GetMapping("/checkKey")
    public R<Boolean> checkKey(@RequestParam String roleKey) {
        return R.ok(roleService.listAll().stream().noneMatch(r -> r.getRoleKey().equals(roleKey)));
    }
}
