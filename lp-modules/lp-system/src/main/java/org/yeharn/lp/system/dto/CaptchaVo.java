/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 验证码视图对象
 */

package org.yeharn.lp.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaVo {

    private String uuid;
    private String img;
    private boolean enabled;
}
