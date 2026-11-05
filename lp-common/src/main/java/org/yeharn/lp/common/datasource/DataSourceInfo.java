/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 数据源配置信息
 */

package org.yeharn.lp.common.datasource;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据源连接信息
 */
@Data
public class DataSourceInfo implements Serializable {

    private String dsKey;
    private String name;
    private String dbType = "mysql";
    private String driverClass;
    private String url;
    private String username;
    private String password;
}
