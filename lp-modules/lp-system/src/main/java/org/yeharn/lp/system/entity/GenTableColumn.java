/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 代码生成业务表字段实体
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
@Table("gen_table_column")
public class GenTableColumn extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long columnId;

    private Long tableId;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    private String isPk;
    private String isIncrement;
    private String isRequired;
    private String isInsert;
    private String isEdit;
    private String isList;
    private String isQuery;
    private String queryType;
    private String htmlType;
    private String dictType;
    private Integer sort;
}
