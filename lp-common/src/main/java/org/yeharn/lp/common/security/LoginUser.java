/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 登录用户信息
 */

package org.yeharn.lp.common.security;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录用户上下文
 */
@Data
public class LoginUser implements Serializable {

    private Long userId;
    private Long deptId;
    private String deptName;
    private String username;
    private String nickname;
    private String avatar;
    /** 角色标识集合 */
    private Set<String> roles = new HashSet<>();
    /** 权限标识集合 */
    private Set<String> permissions = new HashSet<>();
    /** 数据权限范围: 1全部 2自定义 3本部门 4本部门及以下 5仅本人 */
    private String dataScope = "1";
    private Long loginTime;
    private Long expireTime;
    private String loginIp;
    private String browser;
    private String os;
    /** 在线列表使用的当前 token */
    private String token;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAdmin() {
        return roles.contains("admin");
    }
}
