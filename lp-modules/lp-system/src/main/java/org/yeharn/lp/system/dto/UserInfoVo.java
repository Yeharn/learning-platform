/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户信息视图对象
 */

package org.yeharn.lp.system.dto;

import lombok.Data;
import org.yeharn.lp.system.entity.SysUser;

import java.util.Set;

@Data
public class UserInfoVo {

    private SysUser user;
    private Set<String> roles;
    private Set<String> permissions;
    private boolean admin;
}
