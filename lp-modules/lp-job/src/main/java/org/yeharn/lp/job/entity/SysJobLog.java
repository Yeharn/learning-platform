/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 任务运行批次日志实体
 */

package org.yeharn.lp.job.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务运行日志（每执行一次 = 一个批次）
 */
@Data
@Table("sys_job_log")
public class SysJobLog implements Serializable {

    /** 批次号 */
    @Id(keyType = KeyType.Auto)
    private Long logId;

    private Long jobId;

    private String jobName;

    private String jobBean;

    private String jobParam;

    private String cronExpression;

    /** 触发方式：0 自动调度 / 1 手动执行 */
    private String triggerType;

    /** 运行状态：0 成功 / 1 失败 */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 执行结果（成功） */
    private String result;

    /** 异常信息（失败） */
    private String errorMsg;

    /** 触发人：手动执行时记录，自动调度为 system */
    @Column("trigger_by")
    private String triggerBy;

    private LocalDateTime createTime;
}
