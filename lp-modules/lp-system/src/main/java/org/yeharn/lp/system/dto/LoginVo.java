/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 登录视图对象
 */

package org.yeharn.lp.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVo {

    private String token;
    private String tokenName;
    private long expiresIn;
}
