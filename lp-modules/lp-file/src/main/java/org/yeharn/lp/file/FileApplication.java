/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件服务启动类
 */

package org.yeharn.lp.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文件服务：本地 / MinIO / RustFS，独立部署
 */
@EnableDiscoveryClient
@MapperScan("org.yeharn.lp.file.mapper")
@SpringBootApplication(scanBasePackages = "org.yeharn.lp")
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
