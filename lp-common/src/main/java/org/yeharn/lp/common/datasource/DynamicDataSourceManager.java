/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 动态数据源注册与连接池管理
 */

package org.yeharn.lp.common.datasource;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态数据源管理器
 */
@Slf4j
@Component
public class DynamicDataSourceManager {

    private final DynamicDataSource dataSource;
    private final int maximumPoolSize;

    public DynamicDataSourceManager(DynamicDataSource dataSource,
                                    @Value("${lp.datasource.max-pool-size:10}") int maximumPoolSize) {
        this.dataSource = dataSource;
        this.maximumPoolSize = maximumPoolSize;
    }

    public DynamicDataSource getDataSource() {
        return dataSource;
    }

    public HikariDataSource build(DataSourceInfo info) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("lp-" + info.getDsKey());
        config.setJdbcUrl(info.getUrl());
        config.setUsername(info.getUsername());
        config.setPassword(info.getPassword());
        if (StrUtil.isNotBlank(info.getDriverClass())) {
            config.setDriverClassName(info.getDriverClass());
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    public synchronized void register(DataSourceInfo info) {
        dataSource.addPool(info.getDsKey(), build(info));
        log.info("动态数据源已加载: {} -> {}", info.getDsKey(), info.getUrl());
    }

    public synchronized void remove(String key) {
        dataSource.removePool(key);
        log.info("动态数据源已移除: {}", key);
    }

    public boolean exists(String key) {
        return dataSource.getPool(key) != null;
    }

    /**
     * 测试连接，成功返回数据库产品信息
     */
    public String test(DataSourceInfo info) throws Exception {
        try (HikariDataSource ds = build(info); Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        }
    }

    public Map<String, Object> stats(String key) {
        HikariDataSource pool = dataSource.getPool(key);
        Map<String, Object> map = new LinkedHashMap<>();
        if (pool == null) {
            return map;
        }
        map.put("poolName", pool.getPoolName());
        map.put("jdbcUrl", pool.getJdbcUrl());
        map.put("active", pool.getHikariPoolMXBean() == null ? 0 : pool.getHikariPoolMXBean().getActiveConnections());
        map.put("idle", pool.getHikariPoolMXBean() == null ? 0 : pool.getHikariPoolMXBean().getIdleConnections());
        map.put("total", pool.getHikariPoolMXBean() == null ? 0 : pool.getHikariPoolMXBean().getTotalConnections());
        map.put("waiting", pool.getHikariPoolMXBean() == null ? 0 : pool.getHikariPoolMXBean().getThreadsAwaitingConnection());
        map.put("max", pool.getMaximumPoolSize());
        return map;
    }
}
