/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务启动类
 */

package org.yeharn.lp.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 定时任务服务：任务调度、cron 校验与下次执行时间计算、运行批次日志
 */
@EnableDiscoveryClient
@MapperScan("org.yeharn.lp.job.mapper")
@SpringBootApplication(scanBasePackages = "org.yeharn.lp")
public class JobApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobApplication.class, args);
    }
}
