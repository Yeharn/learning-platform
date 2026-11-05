/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 代码生成业务表实体
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("gen_table")
public class GenTable extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long tableId;

    private String tableName;
    private String tableComment;
    private String className;
    private String tplCategory;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String functionAuthor;
    private String genType;
    private String genPath;
    private String options;
    private String dsKey;
    private String remark;

    @Column(ignore = true)
    private List<GenTableColumn> columns;
}
