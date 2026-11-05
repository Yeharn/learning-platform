/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.dto.RoleQuery;
import org.yeharn.lp.system.entity.SysRole;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysRoleDeptMapper;
import org.yeharn.lp.system.mapper.SysRoleMapper;
import org.yeharn.lp.system.mapper.SysRoleMenuMapper;
import org.yeharn.lp.system.mapper.SysUserMapper;
import org.yeharn.lp.system.mapper.SysUserRoleMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    public PageResult<SysRole> page(RoleQuery query) {
        QueryWrapper qw = QueryWrapper.create()
                .like("role_name", query.getRoleName(), StrUtil.isNotBlank(query.getRoleName()))
                .like("role_key", query.getRoleKey(), StrUtil.isNotBlank(query.getRoleKey()))
                .eq("status", query.getStatus(), StrUtil.isNotBlank(query.getStatus()))
                .orderBy("role_sort", true);
        Page<SysRole> page = roleMapper.paginate(Page.of(query.getPageNumSafe(), query.getPageSizeSafe()), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), query.getPageNumSafe(), query.getPageSizeSafe());
    }

    public List<SysRole> listAll() {
        return roleMapper.selectListByQuery(QueryWrapper.create().eq("status", "0").orderBy("role_sort", true));
    }

    public List<SysRole> listByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    public SysRole getById(Long roleId) {
        SysRole role = roleMapper.selectOneById(roleId);
        if (role != null) {
            role.setMenuIds(roleMenuMapper.selectMenuIdsByRoleId(roleId));
            role.setDeptIds(roleDeptMapper.selectDeptIdsByRoleId(roleId));
        }
        return role;
    }

    public void add(SysRole role) {
        checkUnique(role);
        role.setCreateBy(SecurityUtils.getUsername());
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insertSelective(role);
    }

    public void update(SysRole role) {
        checkUnique(role);
        role.setUpdateBy(SecurityUtils.getUsername());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.update(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            if (roleId == 1L) {
                throw new ServiceException("超级管理员角色不允许删除");
            }
            if (userRoleMapper.countUserByRoleId(roleId) > 0) {
                throw new ServiceException("角色已分配用户，不允许删除");
            }
            roleMenuMapper.deleteByRoleId(roleId);
            roleDeptMapper.deleteByRoleId(roleId);
            roleMapper.deleteById(roleId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAuth(Long roleId, List<Long> menuIds, List<Long> deptIds) {
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleId, menuIds);
        }
        roleDeptMapper.deleteByRoleId(roleId);
        if (deptIds != null && !deptIds.isEmpty()) {
            roleDeptMapper.batchInsert(roleId, deptIds);
        }
    }

    public void changeStatus(Long roleId, String status) {
        if (roleId == 1L) {
            throw new ServiceException("超级管理员角色不允许停用");
        }
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setStatus(status);
        role.setUpdateBy(SecurityUtils.getUsername());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.update(role);
    }

    public List<SysUser> listUsers(Long roleId) {
        return userRoleMapper.selectUsersByRoleId(roleId);
    }

    private void checkUnique(SysRole role) {
        QueryWrapper qw = QueryWrapper.create().eq("role_key", role.getRoleKey());
        if (role.getRoleId() != null) {
            qw.ne("role_id", role.getRoleId());
        }
        if (roleMapper.selectCountByQuery(qw) > 0) {
            throw new ServiceException("角色权限字符已存在：" + role.getRoleKey());
        }
    }
}
