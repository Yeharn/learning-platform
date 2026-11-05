/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 字典数据
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
@Table("sys_dict_data")
public class SysDictData extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long dictCode;

    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String status;
    private String remark;
}
