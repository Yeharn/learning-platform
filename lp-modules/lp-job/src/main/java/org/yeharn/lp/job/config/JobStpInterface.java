/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - Sa-Token 权限数据源
 */

package org.yeharn.lp.job.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.security.LoginUser;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限来源：登录会话（Redis）中的 LoginUser，跨服务复用系统服务签发的权限
 */
@Component
public class JobStpInterface implements StpInterface {

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
