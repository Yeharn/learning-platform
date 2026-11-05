/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色查询条件
 */

package org.yeharn.lp.system.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery {

    private String roleName;
    private String roleKey;
    private String status;
}
