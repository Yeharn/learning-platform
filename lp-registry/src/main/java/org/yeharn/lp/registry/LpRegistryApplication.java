package org.yeharn.lp.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 注册中心启动类（Spring Cloud Netflix Eureka Server）
 *
 * @author Yeharn
 */
@EnableEurekaServer
@SpringBootApplication
public class LpRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LpRegistryApplication.class, args);
    }
}
