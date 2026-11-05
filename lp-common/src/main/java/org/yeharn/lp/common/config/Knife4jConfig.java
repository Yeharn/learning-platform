/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Knife4j / OpenAPI 文档配置
 */

package org.yeharn.lp.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置
 * <p>
 * 文档地址：/doc.html；OpenAPI 描述：/v3/api-docs
 */
@Configuration
public class Knife4jConfig {

    public static final String SECURITY_SCHEME = "Authorization";

    @Bean
    public OpenAPI lpOpenAPI(@Value("${spring.application.name:learning-platform}") String applicationName,
                             @Value("${lp.api-docs.title:Learning Platform 接口文档}") String title,
                             @Value("${lp.api-docs.description:基于 Spring Boot 3 + Sa-Token + MyBatis-Flex 的微服务权限管理系统接口文档}") String description,
                             @Value("${lp.api-docs.version:1.0.0}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title(title + " - " + applicationName)
                        .description(description)
                        .version(version)
                        .contact(new Contact().name("yeharn").url("https://www.yeharn.org"))
                        .license(new License().name("Apache License 2.0")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Bearer")
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_SCHEME)
                                .description("登录接口返回的 token，格式：Bearer {token}")));
    }
}
