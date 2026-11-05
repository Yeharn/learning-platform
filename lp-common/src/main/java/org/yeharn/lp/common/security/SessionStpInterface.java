/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Sa-Token 会话权限数据源（多服务共用）
 */

package org.yeharn.lp.common.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.yeharn.lp.common.core.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于登录会话（Redis）的权限来源，供未直连系统库的服务复用
 * <p>
 * 不做成 Spring Bean，避免与各服务自有实现冲突，由需要它的服务通过 @Bean 注册。
 */
public class SessionStpInterface implements StpInterface {

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

    private LoginUser loginUser() {
        try {
            Object obj = StpUtil.getSession().get(Constants.TOKEN_SESSION_USER);
            return obj instanceof LoginUser user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }
}
