/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 字典类型实体
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
@Table("sys_dict_type")
public class SysDictType extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long dictId;

    private String dictName;
    private String dictType;
    private String status;
    private String remark;
}
