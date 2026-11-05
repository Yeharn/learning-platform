/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - XXL-JOB 执行器配置
 */

package org.yeharn.lp.system.job;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器（可选）：部署 xxl-job-admin 后设置 lp.xxl-job.enabled=true 开启
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "lp.xxl-job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${lp.xxl-job.admin-addresses}")
    private String adminAddresses;

    @Value("${lp.xxl-job.appname:lp-system}")
    private String appname;

    @Value("${lp.xxl-job.port:9999}")
    private int port;

    @Value("${lp.xxl-job.access-token:default_token}")
    private String accessToken;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("初始化 XXL-JOB 执行器 admin={} appname={}", adminAddresses, appname);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath("./logs/xxl-job");
        executor.setLogRetentionDays(7);
        return executor;
    }
}
