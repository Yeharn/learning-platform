/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程办理人上下文（当前登录人）
 */

package org.yeharn.lp.flow.support;

import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.common.security.SecurityUtils;

/**
 * 统一从登录会话中取流程办理人信息，未登录场景（如定时任务、内置流程初始化）回退到系统用户
 */
public final class FlowUserSupport {

    /** 系统兜底用户，未登录时使用 */
    public static final String SYSTEM_USER_ID = "1";

    public static final String SYSTEM_USER_NAME = "admin";

    private FlowUserSupport() {
    }

    public static LoginUser currentUser() {
        return SecurityUtils.getLoginUserOrNull();
    }

    public static String currentUserId() {
        LoginUser user = currentUser();
        return user == null || user.getUserId() == null ? SYSTEM_USER_ID : String.valueOf(user.getUserId());
    }

    public static String currentUserName() {
        LoginUser user = currentUser();
        if (user == null) {
            return SYSTEM_USER_NAME;
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    public static String currentLoginName() {
        LoginUser user = currentUser();
        return user == null ? SYSTEM_USER_NAME : user.getUsername();
    }
}
