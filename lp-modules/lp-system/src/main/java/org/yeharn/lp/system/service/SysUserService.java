/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.dto.UserQuery;
import org.yeharn.lp.system.entity.SysRole;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysMenuMapper;
import org.yeharn.lp.system.mapper.SysRoleMapper;
import org.yeharn.lp.system.mapper.SysUserMapper;
import org.yeharn.lp.system.mapper.SysUserRoleMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户管理
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptService deptService;
    private final SysConfigService configService;
    private final DataScopeService dataScopeService;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUser> page(UserQuery query) {
        QueryWrapper qw = QueryWrapper.create()
                .like("username", query.getUsername(), StrUtil.isNotBlank(query.getUsername()))
                .like("nickname", query.getNickname(), StrUtil.isNotBlank(query.getNickname()))
                .like("phone", query.getPhone(), StrUtil.isNotBlank(query.getPhone()))
                .eq("status", query.getStatus(), StrUtil.isNotBlank(query.getStatus()))
                .ge("create_time", query.getBeginTime(), StrUtil.isNotBlank(query.getBeginTime()))
                .le("create_time", query.getEndTime(), StrUtil.isNotBlank(query.getEndTime()));

        if (query.getDeptId() != null) {
            List<Long> deptIds = deptService.listChildrenIds(query.getDeptId());
            qw.in("dept_id", deptIds);
        }

        if (dataScopeService.onlySelf()) {
            qw.eq("user_id", SecurityUtils.getUserId());
        } else {
            List<Long> scopeDeptIds = dataScopeService.getDeptIds();
            if (scopeDeptIds != null) {
                if (scopeDeptIds.isEmpty()) {
                    qw.eq("user_id", -1L);
                } else {
                    qw.in("dept_id", scopeDeptIds);
                }
            }
        }
        qw.orderBy("user_id", true);

        Page<SysUser> page = userMapper.paginate(Page.of(query.getPageNumSafe(), query.getPageSizeSafe()), qw);
        page.getRecords().forEach(this::fillDeptAndRoles);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), query.getPageNumSafe(), query.getPageSizeSafe());
    }

    public SysUser getById(Long userId) {
        SysUser user = userMapper.selectOneById(userId);
        if (user != null) {
            fillDeptAndRoles(user);
            user.setRoleIds(userRoleMapper.selectRoleIdsByUserId(userId));
        }
        return user;
    }

    public SysUser getByUsername(String username) {
        SysUser user = userMapper.selectOneByQuery(QueryWrapper.create().eq("username", username));
        if (user != null) {
            fillDeptAndRoles(user);
        }
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysUser user) {
        if (userMapper.selectCountByQuery(QueryWrapper.create().eq("username", user.getUsername())) > 0) {
            throw new ServiceException("登录账号已存在：" + user.getUsername());
        }
        String initPassword = configService.getValue("sys.user.initPassword", "admin123");
        String rawPassword = StrUtil.isBlank(user.getPassword()) ? initPassword : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDelFlag("0");
        user.setCreateBy(SecurityUtils.getUsername());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insertSelective(user);
        saveUserRoles(user.getUserId(), user.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        SysUser old = userMapper.selectOneById(user.getUserId());
        if (old == null) {
            throw new ServiceException("用户不存在");
        }
        QueryWrapper qw = QueryWrapper.create().eq("username", user.getUsername()).ne("user_id", user.getUserId());
        if (userMapper.selectCountByQuery(qw) > 0) {
            throw new ServiceException("登录账号已存在：" + user.getUsername());
        }
        user.setPassword(null);
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        saveUserRoles(user.getUserId(), user.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> userIds) {
        for (Long userId : userIds) {
            if (userId == 1L) {
                throw new ServiceException("超级管理员不允许删除");
            }
            if (userId.equals(SecurityUtils.getUserId())) {
                throw new ServiceException("不能删除当前登录用户");
            }
            userRoleMapper.deleteByUserId(userId);
            userMapper.deleteById(userId);
        }
    }

    public void resetPassword(Long userId, String password) {
        if (StrUtil.isBlank(password) || password.length() < 6) {
            throw new ServiceException("密码长度不能小于6位");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    public void changeStatus(Long userId, String status) {
        if (userId == 1L && "1".equals(status)) {
            throw new ServiceException("超级管理员不允许停用");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    public void updateLoginInfo(Long userId, String ip, LocalDateTime time) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setLoginIp(ip);
        user.setLoginDate(time);
        userMapper.update(user);
    }

    public void updateProfile(SysUser user) {
        SysUser patch = new SysUser();
        patch.setUserId(SecurityUtils.getUserId());
        patch.setNickname(user.getNickname());
        patch.setEmail(user.getEmail());
        patch.setPhone(user.getPhone());
        patch.setSex(user.getSex());
        patch.setAvatar(user.getAvatar());
        patch.setUpdateBy(SecurityUtils.getUsername());
        patch.setUpdateTime(LocalDateTime.now());
        userMapper.update(patch);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        loginUser.setNickname(user.getNickname());
        SecurityUtils.refresh(loginUser);
    }

    public void changePassword(String oldPassword, String newPassword) {
        SysUser user = userMapper.selectOneById(SecurityUtils.getUserId());
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ServiceException("新密码长度不能小于6位");
        }
        resetPassword(user.getUserId(), newPassword);
    }

    public List<SysUser> listByRoleId(Long roleId) {
        return userRoleMapper.selectUsersByRoleId(roleId);
    }

    public List<SysUser> listAll() {
        return userMapper.selectListByQuery(QueryWrapper.create().eq("status", "0"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.batchInsert(userId, roleIds);
        }
    }

    public Set<String> getPermissions(Long userId) {
        Set<String> perms = new HashSet<>();
        SysUser user = userMapper.selectOneById(userId);
        if (user == null) {
            return perms;
        }
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        boolean admin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey()));
        if (admin) {
            perms.add("*:*:*");
            return perms;
        }
        for (SysRole role : roles) {
            perms.addAll(menuPerms(role.getRoleId()));
        }
        return perms;
    }

    private Set<String> menuPerms(Long roleId) {
        Set<String> perms = new HashSet<>();
        for (String perm : menuMapper.selectPermsByRoleId(roleId)) {
            if (StrUtil.isNotBlank(perm)) {
                for (String p : perm.split(",")) {
                    if (StrUtil.isNotBlank(p)) {
                        perms.add(p.trim());
                    }
                }
            }
        }
        return perms;
    }

    private void fillDeptAndRoles(SysUser user) {
        user.setDeptName(deptService.getDeptName(user.getDeptId()));
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getUserId());
        user.setRoles(roles);
        user.setRoleNames(roles.stream().map(SysRole::getRoleName).reduce((a, b) -> a + "、" + b).orElse(""));
    }
}
