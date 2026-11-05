/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 数据源配置实体
 */

package org.yeharn.lp.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_datasource")
public class SysDatasource extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;
    private String dsKey;
    private String dbType;
    private String driverClass;
    private String url;
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String status;
    private String isDefault;
    private String remark;

    @Column(ignore = true)
    private String passwordMask;

    @Column(ignore = true)
    private Boolean connected;
}
