/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - Sa-Token 权限数据源实现
 */

package org.yeharn.lp.system.service;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yeharn.lp.system.mapper.SysRoleMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据源：从数据库实时读取，角色/菜单调整后立即生效
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService userService;
    private final SysRoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(String.valueOf(loginId));
        return new ArrayList<>(userService.getPermissions(userId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(String.valueOf(loginId));
        List<String> roles = new ArrayList<>();
        roleMapper.selectRolesByUserId(userId).forEach(r -> roles.add(r.getRoleKey()));
        return roles;
    }
}
