/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 Sa-Token 权限数据源
 */

package org.yeharn.lp.monitor.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.security.LoginUser;

import java.util.ArrayList;
import java.util.List;

/**
 * 监控服务的权限来源：登录会话中的 LoginUser
 */
@Slf4j
@Component
public class MonitorStpInterface implements StpInterface {

    private LoginUser loginUser() {
        try {
            Object obj = StpUtil.getSession().get(Constants.TOKEN_SESSION_USER);
            return obj instanceof LoginUser user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser user = loginUser();
        if (user == null) {
            return List.of();
        }
        if (user.isAdmin()) {
            return List.of(Constants.ALL_PERMISSION);
        }
        return new ArrayList<>(user.getPermissions());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser user = loginUser();
        return user == null ? List.of() : new ArrayList<>(user.getRoles());
    }
}
