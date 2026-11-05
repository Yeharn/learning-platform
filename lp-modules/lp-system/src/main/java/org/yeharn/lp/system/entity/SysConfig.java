/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 系统参数实体
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_config")
public class SysConfig extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long configId;

    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
}
