/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 菜单接口控制器
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
import org.yeharn.lp.system.entity.SysMenu;
import org.yeharn.lp.system.service.SysMenuService;

import java.util.List;

/**
 * 菜单权限管理
 */
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<SysMenu>> list(@RequestParam(required = false) String menuName,
                                 @RequestParam(required = false) String status) {
        return R.ok(menuService.list(menuName, status));
    }

    @GetMapping("/treeselect")
    public R<List<SysMenu>> treeSelect() {
        return R.ok(menuService.buildTree(menuService.listAll(), 0L));
    }

    @SaCheckPermission("system:menu:query")
    @GetMapping("/{menuId}")
    public R<SysMenu> getInfo(@PathVariable Long menuId) {
        return R.ok(menuService.getById(menuId));
    }

    @SaCheckPermission("system:menu:add")
    @OperLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysMenu menu) {
        menuService.add(menu);
        return R.ok();
    }

    @SaCheckPermission("system:menu:edit")
    @OperLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return R.ok();
    }

    @SaCheckPermission("system:menu:remove")
    @OperLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public R<Void> remove(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return R.ok();
    }

    @GetMapping("/roleMenuTree/{roleId}")
    public R<java.util.Map<String, Object>> roleMenuTree(@PathVariable Long roleId) {
        return R.ok(java.util.Map.of(
                "menus", menuService.buildTree(menuService.listAll(), 0L),
                "checkedKeys", menuService.listMenuIdsByRole(roleId)));
    }
}
