/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 动态数据源路由实现
 */

package org.yeharn.lp.common.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源：根据上下文路由到不同的 HikariCP 连接池
 */
@Slf4j
public class DynamicDataSource extends AbstractDataSource {

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private String primaryKey = "master";

    public void addPool(String key, HikariDataSource dataSource) {
        HikariDataSource old = pools.put(key, dataSource);
        if (old != null && old != dataSource) {
            old.close();
        }
    }

    public void removePool(String key) {
        if (primaryKey.equals(key)) {
            return;
        }
        HikariDataSource ds = pools.remove(key);
        if (ds != null) {
            ds.close();
        }
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(pools.keySet());
    }

    public HikariDataSource getPool(String key) {
        return pools.get(key);
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    private HikariDataSource current() {
        String key = DataSourceContextHolder.getOrDefault();
        HikariDataSource ds = pools.get(key);
        if (ds == null) {
            ds = pools.get(primaryKey);
        }
        if (ds == null) {
            throw new IllegalStateException("未找到可用的数据源：" + key);
        }
        return ds;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return current().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return current().getConnection(username, password);
    }
}
