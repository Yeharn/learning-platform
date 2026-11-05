/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 定时任务
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内置定时任务
 */
@Data
@Table("sys_job")
public class SysJob implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long jobId;

    private String jobName;
    /** 处理器 Bean 名称（实现 LpJob 接口） */
    private String jobBean;
    private String jobParam;
    private String cronExpression;
    /** 0 正常 / 1 暂停 */
    private String status;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
