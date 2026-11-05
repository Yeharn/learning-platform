/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 多数据源配置属性
 */

package org.yeharn.lp.common.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 主数据源配置（复用 spring.datasource 前缀）
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class LpDataSourceProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    private Hikari hikari = new Hikari();

    @Data
    public static class Hikari {
        private int maximumPoolSize = 20;
        private int minimumIdle = 2;
        private long connectionTimeout = 10000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private String connectionTestQuery = "SELECT 1";
    }
}
