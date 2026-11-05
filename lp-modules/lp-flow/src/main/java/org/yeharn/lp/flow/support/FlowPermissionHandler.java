/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: warm-flow 办理人权限处理器
 */

package org.yeharn.lp.flow.support;

import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对接系统登录会话：
 * <ul>
 *     <li>{@link #getHandler()} 返回当前登录用户 id，引擎写入 create_by / approver</li>
 *     <li>{@link #permissions()} 返回用户 id、user:id、role:x、dept:x，用于办理权限校验</li>
 *     <li>{@link #convertPermissions(List)} 把节点上配置的角色、部门展开为具体用户 id</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FlowPermissionHandler implements PermissionHandler {

    private final FlowUserDirectory userDirectory;

    @Override
    public String getHandler() {
        return FlowUserSupport.currentUserId();
    }

    @Override
    public List<String> permissions() {
        return userDirectory.permissionFlags(FlowUserSupport.currentUserId());
    }

    @Override
    public List<String> convertPermissions(List<String> permissions) {
        return userDirectory.expand(permissions);
    }
}
