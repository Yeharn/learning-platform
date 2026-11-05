/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Sa-Token 权限数据源注册
 */

package org.yeharn.lp.flow.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yeharn.lp.common.security.SessionStpInterface;

/**
 * 流程服务权限校验数据源（读取登录会话中的权限集合）
 */
@Configuration
public class StpInterfaceConfig {

    @Bean
    public StpInterface stpInterface() {
        return new SessionStpInterface();
    }
}
