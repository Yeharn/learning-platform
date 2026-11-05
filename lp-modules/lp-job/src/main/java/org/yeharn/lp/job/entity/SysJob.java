/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 定时任务实体
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
 * 定时任务
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

    /** 上次运行时间（每次执行后回写） */
    private LocalDateTime lastRunTime;

    /** 上次运行状态：0 成功 / 1 失败 */
    private String lastStatus;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    /** 下次运行时间：按 cron 实时计算，不落库 */
    @Column(ignore = true)
    private LocalDateTime nextRunTime;
}
