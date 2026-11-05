/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 调度核心（执行、记录批次日志、回写运行状态）
 */

package org.yeharn.lp.job.core;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.job.entity.SysJob;
import org.yeharn.lp.job.entity.SysJobLog;
import org.yeharn.lp.job.mapper.SysJobLogMapper;
import org.yeharn.lp.job.mapper.SysJobMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 调度核心：ThreadPoolTaskScheduler + Quartz CronExpression 动态排程
 * <p>
 * 每次触发执行后按 cron 重新计算下一次执行时间，天然支持含「年」字段的 7 段表达式；
 * 同一任务串行执行，上一批次未结束时跳过本次触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    /** 触发方式：自动调度 */
    public static final String TRIGGER_AUTO = "0";
    /** 触发方式：手动执行 */
    public static final String TRIGGER_MANUAL = "1";

    private static final int MAX_TEXT = 2000;

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final ApplicationContext applicationContext;

    @Value("${lp.job.pool-size:8}")
    private int poolSize;

    private ThreadPoolTaskScheduler scheduler;
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("lp-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        loadAll();
        log.info("[lp-job] 调度器初始化完成，线程池大小 {}", poolSize);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            futures.values().forEach(f -> f.cancel(false));
            futures.clear();
            scheduler.shutdown();
        }
    }

    /** 启动时加载全部启用任务 */
    public void loadAll() {
        List<SysJob> jobs = jobMapper.selectListByQuery(QueryWrapper.create().eq("status", "0"));
        for (SysJob job : jobs) {
            try {
                schedule(job);
            } catch (Exception e) {
                log.error("[lp-job] 任务[{}]加载失败: {}", job.getJobName(), e.getMessage());
            }
        }
    }

    /** 排程（幂等：先取消旧计划） */
    public synchronized void schedule(SysJob job) {
        cancel(job.getJobId());
        if (!"0".equals(job.getStatus())) {
            return;
        }
        try {
            CronExpression expression = CronSupport.parse(job.getCronExpression());
            Date next = expression.getNextValidTimeAfter(new Date());
            if (next == null) {
                log.warn("[lp-job] 任务[{}]cron 无未来触发时间: {}", job.getJobName(), job.getCronExpression());
                return;
            }
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                try {
                    execute(job, TRIGGER_AUTO, "system");
                } finally {
                    reschedule(job.getJobId());
                }
            }, next.toInstant());
            futures.put(job.getJobId(), future);
            log.info("[lp-job] 任务[{}]已排程，下次执行 {}", job.getJobName(),
                    LocalDateTime.ofInstant(next.toInstant(), CronSupport.ZONE));
        } catch (Exception e) {
            log.error("[lp-job] 任务[{}]排程失败: {}", job.getJobName(), e.getMessage());
        }
    }

    /** 取消排程 */
    public synchronized void cancel(Long jobId) {
        ScheduledFuture<?> future = futures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** 执行结束后按最新配置重新排程 */
    private void reschedule(Long jobId) {
        SysJob latest = jobMapper.selectOneById(jobId);
        if (latest == null) {
            cancel(jobId);
            return;
        }
        schedule(latest);
    }

    /** 手动执行一次（异步，不改动排程） */
    public void runNow(Long jobId, String operator) {
        SysJob job = jobMapper.selectOneById(jobId);
        if (job == null) {
            throw new ServiceException("任务不存在");
        }
        if (!applicationContext.containsBean(job.getJobBean())) {
            throw new ServiceException("未找到任务处理器：" + job.getJobBean());
        }
        scheduler.execute(() -> execute(job, TRIGGER_MANUAL, operator));
    }

    /** 执行任务并记录批次日志 */
    private void execute(SysJob job, String triggerType, String operator) {
        if (!running.add(job.getJobId())) {
            log.warn("[lp-job] 任务[{}]上一批次未结束，跳过本次触发", job.getJobName());
            return;
        }
        LocalDateTime start = LocalDateTime.now();
        SysJobLog row = new SysJobLog();
        row.setJobId(job.getJobId());
        row.setJobName(job.getJobName());
        row.setJobBean(job.getJobBean());
        row.setJobParam(job.getJobParam());
        row.setCronExpression(job.getCronExpression());
        row.setTriggerType(triggerType);
        row.setTriggerBy(operator);
        row.setStartTime(start);
        row.setCreateTime(start);

        String status = "0";
        String result = null;
        String error = null;
        try {
            LpJob handler = applicationContext.getBean(job.getJobBean(), LpJob.class);
            result = handler.execute(job.getJobParam());
            log.info("[lp-job] 任务[{}]执行成功: {}", job.getJobName(), result);
        } catch (Exception e) {
            status = "1";
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("[lp-job] 任务[{}]执行失败: {}", job.getJobName(), error, e);
        } finally {
            LocalDateTime end = LocalDateTime.now();
            row.setStatus(status);
            row.setEndTime(end);
            row.setCostTime(Duration.between(start, end).toMillis());
            row.setResult(StrUtil.sub(StrUtil.nullToEmpty(result), 0, MAX_TEXT));
            row.setErrorMsg(StrUtil.sub(StrUtil.nullToEmpty(error), 0, MAX_TEXT));
            try {
                jobLogMapper.insertSelective(row);
            } catch (Exception e) {
                log.error("[lp-job] 运行日志写入失败", e);
            }
            try {
                SysJob update = new SysJob();
                update.setJobId(job.getJobId());
                update.setLastRunTime(start);
                update.setLastStatus(status);
                jobMapper.update(update);
            } catch (Exception e) {
                log.error("[lp-job] 任务运行状态回写失败", e);
            }
            running.remove(job.getJobId());
        }
    }
}
