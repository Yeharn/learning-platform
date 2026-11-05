/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户查询条件
 */

package org.yeharn.lp.system.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    private String username;
    private String nickname;
    private String phone;
    private String status;
    private Long deptId;
    private String beginTime;
    private String endTime;
}
