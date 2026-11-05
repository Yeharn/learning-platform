/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 动态数据源装配配置
 */

package org.yeharn.lp.common.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 主数据源装配
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LpDataSourceProperties.class)
public class DataSourceConfig {

    private final LpDataSourceProperties properties;

    @Bean
    @Primary
    public DynamicDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("lp-master");
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        LpDataSourceProperties.Hikari hikari = properties.getHikari();
        config.setMaximumPoolSize(hikari.getMaximumPoolSize());
        config.setMinimumIdle(hikari.getMinimumIdle());
        config.setConnectionTimeout(hikari.getConnectionTimeout());
        config.setIdleTimeout(hikari.getIdleTimeout());
        config.setMaxLifetime(hikari.getMaxLifetime());
        config.setConnectionTestQuery(hikari.getConnectionTestQuery());

        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.addPool("master", new HikariDataSource(config));
        log.info("主数据源初始化完成: {}", properties.getUrl());
        return dynamicDataSource;
    }
}
