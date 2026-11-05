/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 数据权限范围业务服务
 */

package org.yeharn.lp.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysRole;
import org.yeharn.lp.system.mapper.SysRoleDeptMapper;
import org.yeharn.lp.system.mapper.SysRoleMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限（行级）计算
 * 1 全部数据  2 自定义部门  3 本部门  4 本部门及以下  5 仅本人
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptService deptService;

    /**
     * @return null 表示不限制部门；空集合表示无可见数据
     */
    public List<Long> getDeptIds() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null || loginUser.isAdmin()) {
            return null;
        }
        String scope = loginUser.getDataScope();
        if (scope == null || "1".equals(scope)) {
            return null;
        }
        Set<Long> ids = new LinkedHashSet<>();
        switch (scope) {
            case "2" -> {
                for (SysRole role : roleMapper.selectRolesByUserId(loginUser.getUserId())) {
                    ids.addAll(roleDeptMapper.selectDeptIdsByRoleId(role.getRoleId()));
                }
            }
            case "3" -> {
                if (loginUser.getDeptId() != null) {
                    ids.add(loginUser.getDeptId());
                }
            }
            case "4" -> {
                if (loginUser.getDeptId() != null) {
                    ids.addAll(deptService.listChildrenIds(loginUser.getDeptId()));
                }
            }
            default -> {
                return List.of();
            }
        }
        return new ArrayList<>(ids);
    }

    public boolean onlySelf() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        return loginUser != null && !loginUser.isAdmin() && "5".equals(loginUser.getDataScope());
    }
}
