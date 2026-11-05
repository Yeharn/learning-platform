/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Sa-Token 鉴权与接口白名单配置
 */

package org.yeharn.lp.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录校验 + 注解鉴权
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    public static final String[] EXCLUDE_PATHS = {
            "/auth/login", "/auth/captcha", "/auth/register", "/auth/forgot",
            "/error", "/favicon.ico",
            "/actuator/**", "/doc.html", "/webjars/**", "/v3/api-docs/**",
            "/swagger-ui/**", "/swagger-resources/**",
            "/files/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        SaRouter.match("/**")
                                .notMatch(EXCLUDE_PATHS)
                                .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
