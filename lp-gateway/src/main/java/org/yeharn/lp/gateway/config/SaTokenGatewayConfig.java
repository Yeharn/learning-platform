/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 网关全局鉴权过滤器与路由白名单
 */

package org.yeharn.lp.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关统一鉴权：白名单放行，其余请求必须携带有效 token
 */
@Configuration
public class SaTokenGatewayConfig {

    public static final String[] WHITE_LIST = {
            "/auth/login",
            "/auth/captcha",
            "/auth/register",
            "/error",
            "/favicon.ico",
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            // Knife4j 网关聚合后各服务的文档地址：/{serviceId}/v3/api-docs
            "/*/v3/api-docs/**",
            // 本地文件静态资源（图片 / 视频预览，无需携带 token）
            "/files/**",
            // WebSocket 握手无法携带自定义请求头，token 由 /ws/message 处理器自行校验
            "/ws/**"
    };

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude(WHITE_LIST)
                .setAuth(obj -> SaRouter.match("/**")
                        .notMatch(WHITE_LIST)
                        .check(r -> StpUtil.checkLogin()))
                .setError(e -> {
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("code", 401);
                    body.put("msg", "登录状态无效，请重新登录");
                    body.put("data", null);
                    body.put("timestamp", System.currentTimeMillis());
                    return JSONUtil.toJsonStr(body);
                });
    }
}
