/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 登录用户上下文工具
 */

package org.yeharn.lp.common.security;

import cn.dev33.satoken.stp.StpUtil;
import org.yeharn.lp.common.core.Constants;

/**
 * 当前登录用户工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUserOrNull() {
        try {
            Object obj = StpUtil.getSession().get(Constants.TOKEN_SESSION_USER);
            return obj instanceof LoginUser user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static LoginUser getLoginUser() {
        LoginUser user = getLoginUserOrNull();
        if (user == null) {
            throw new org.yeharn.lp.common.core.ServiceException(401, "未登录，请先登录");
        }
        return user;
    }

    public static Long getUserId() {
        LoginUser user = getLoginUserOrNull();
        return user == null ? null : user.getUserId();
    }

    public static Long getUserIdOrNull() {
        return getUserId();
    }

    public static String getUsername() {
        LoginUser user = getLoginUserOrNull();
        return user == null ? "anonymous" : user.getUsername();
    }

    public static boolean isAdmin() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isAdmin();
    }

    public static void setLoginUser(LoginUser user) {
        StpUtil.getSession().set(Constants.TOKEN_SESSION_USER, user);
    }

    public static void refresh(LoginUser user) {
        StpUtil.getSession().set(Constants.TOKEN_SESSION_USER, user);
    }
}
