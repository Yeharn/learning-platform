/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户
 */

package org.yeharn.lp.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long userId;

    private Long deptId;
    private String username;
    private String nickname;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String userType;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    private String status;
    private String loginIp;
    private LocalDateTime loginDate;
    private String remark;

    @JsonIgnore
    @Column(value = "del_flag", isLogicDelete = true)
    private String delFlag;

    @Column(ignore = true)
    private String deptName;

    @Column(ignore = true)
    private List<Long> roleIds;

    @Column(ignore = true)
    private List<SysRole> roles;

    @Column(ignore = true)
    private String roleNames;
}
