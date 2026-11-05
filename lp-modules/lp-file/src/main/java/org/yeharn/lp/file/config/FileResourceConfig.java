/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件访问静态资源映射与鉴权放行
 */

package org.yeharn.lp.file.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.yeharn.lp.file.storage.LocalFileStorage;

/**
 * 本地文件静态资源映射：/files/** -> 本地存储目录
 */
@Configuration
@RequiredArgsConstructor
public class FileResourceConfig implements WebMvcConfigurer {

    private final LocalFileStorage localFileStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = localFileStorage.rootPath();
        if (!root.endsWith("/")) {
            root = root + "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + root)
                .setCachePeriod(3600);
    }
}
