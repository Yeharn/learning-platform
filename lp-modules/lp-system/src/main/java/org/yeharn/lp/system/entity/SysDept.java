/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 部门
 */

package org.yeharn.lp.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class SysDept extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long deptId;

    private Long parentId;
    private String ancestors;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private String status;

    @JsonIgnore
    @Column(value = "del_flag", isLogicDelete = true)
    private String delFlag;

    @Column(ignore = true)
    private List<SysDept> children = new ArrayList<>();

    @Column(ignore = true)
    private String parentName;
}
