/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务启动类
 */

package org.yeharn.lp.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 系统服务：认证、用户、角色、菜单、字典、参数、日志、代码生成、数据源
 */
@EnableAsync
@EnableDiscoveryClient
@MapperScan("org.yeharn.lp.system.mapper")
@SpringBootApplication(scanBasePackages = "org.yeharn.lp")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
