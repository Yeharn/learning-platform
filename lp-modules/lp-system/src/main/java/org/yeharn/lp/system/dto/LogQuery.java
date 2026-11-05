/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 日志查询条件
 */

package org.yeharn.lp.system.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogQuery extends PageQuery {

    private String username;
    private String title;
    private String status;
    private Integer businessType;
    private String beginTime;
    private String endTime;
    /** 日志月份 yyyyMM，为空取当前月 */
    private String month;
}
