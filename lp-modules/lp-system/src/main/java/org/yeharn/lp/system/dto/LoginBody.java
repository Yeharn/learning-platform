/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 登录请求体
 */

package org.yeharn.lp.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginBody {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String code;
    private String uuid;
    private Boolean rememberMe;
}
