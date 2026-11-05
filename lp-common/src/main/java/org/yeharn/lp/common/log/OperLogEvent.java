/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 操作日志事件
 */

package org.yeharn.lp.common.log;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志事件
 */
@Data
@ToString
public class OperLogEvent implements Serializable {

    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private String operName;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private LocalDateTime operTime;
    private Long costTime;
}
