/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色
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

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long roleId;

    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private Boolean menuCheckStrictly;
    private Boolean deptCheckStrictly;
    private String status;

    @JsonIgnore
    @Column(value = "del_flag", isLogicDelete = true)
    private String delFlag;

    private String remark;

    @Column(ignore = true)
    private List<Long> menuIds;

    @Column(ignore = true)
    private List<Long> deptIds;

    @Column(ignore = true)
    private List<Long> userIds;

    @Column(ignore = true)
    private Boolean flag;
}
