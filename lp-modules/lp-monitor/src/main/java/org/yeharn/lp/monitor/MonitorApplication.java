/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务启动类
 */

package org.yeharn.lp.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 监控服务：服务器 / Redis / MySQL
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "org.yeharn.lp")
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
