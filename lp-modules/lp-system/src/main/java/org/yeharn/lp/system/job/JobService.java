/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 定时任务业务服务
 */

package org.yeharn.lp.system.job;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysJob;
import org.yeharn.lp.system.mapper.SysJobMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 内置定时任务调度：基于 ThreadPoolTaskScheduler + cron
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final SysJobMapper jobMapper;
    private final ApplicationContext applicationContext;

    @Value("${lp.job.pool-size:4}")
    private int poolSize;

    private ThreadPoolTaskScheduler scheduler;
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, Long> lastResult = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("lp-job-");
        scheduler.initialize();
        for (SysJob job : jobMapper.selectListByQuery(QueryWrapper.create().eq("status", "0"))) {
            try {
                schedule(job);
            } catch (Exception e) {
                log.error("定时任务加载失败 {}: {}", job.getJobName(), e.getMessage());
            }
        }
        log.info("内置定时任务调度初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private Runnable wrap(SysJob job) {
        return () -> {
            try {
                LpJob handler = applicationContext.getBean(job.getJobBean(), LpJob.class);
                String result = handler.execute(job.getJobParam());
                lastResult.put(job.getJobBean(), System.currentTimeMillis());
                log.info("定时任务[{}]执行完成: {}", job.getJobName(), result);
            } catch (Exception e) {
                log.error("定时任务[{}]执行失败: {}", job.getJobName(), e.getMessage(), e);
            }
        };
    }

    private void validate(SysJob job) {
        if (StrUtil.isBlank(job.getJobBean())) {
            throw new ServiceException("处理器 Bean 名称不能为空");
        }
        LpJob handler = applicationContext.getBeansOfType(LpJob.class).get(job.getJobBean());
        if (handler == null) {
            throw new ServiceException("未找到任务处理器：" + job.getJobBean());
        }
        try {
            new CronTrigger(job.getCronExpression());
        } catch (Exception e) {
            throw new ServiceException("cron 表达式无效：" + job.getCronExpression());
        }
    }

    private synchronized void schedule(SysJob job) {
        ScheduledFuture<?> old = futures.remove(job.getJobId());
        if (old != null) {
            old.cancel(false);
        }
        if (!"0".equals(job.getStatus())) {
            return;
        }
        futures.put(job.getJobId(), scheduler.schedule(wrap(job), new CronTrigger(job.getCronExpression())));
    }

    public PageResult<SysJob> page(String jobName, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create().like("job_name", jobName, StrUtil.isNotBlank(jobName))
                .orderBy("job_id", true);
        Page<SysJob> page = jobMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public List<String> handlers() {
        return new ArrayList<>(applicationContext.getBeansOfType(LpJob.class).keySet());
    }

    public void add(SysJob job) {
        validate(job);
        job.setCreateBy(SecurityUtils.getUsername());
        job.setCreateTime(LocalDateTime.now());
        jobMapper.insertSelective(job);
        schedule(job);
    }

    public void update(SysJob job) {
        validate(job);
        job.setUpdateBy(SecurityUtils.getUsername());
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.update(job);
        schedule(job);
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ScheduledFuture<?> future = futures.remove(id);
            if (future != null) {
                future.cancel(false);
            }
            jobMapper.deleteById(id);
        }
    }

    public void changeStatus(SysJob job) {
        SysJob db = jobMapper.selectOneById(job.getJobId());
        if (db == null) {
            throw new ServiceException("任务不存在");
        }
        db.setStatus(job.getStatus());
        db.setUpdateBy(SecurityUtils.getUsername());
        db.setUpdateTime(LocalDateTime.now());
        jobMapper.update(db);
        schedule(db);
    }

    public void runOnce(Long jobId) {
        SysJob job = jobMapper.selectOneById(jobId);
        if (job == null) {
            throw new ServiceException("任务不存在");
        }
        LpJob handler = applicationContext.getBean(job.getJobBean(), LpJob.class);
        scheduler.execute(() -> {
            try {
                String result = handler.execute(job.getJobParam());
                log.info("手动执行任务[{}]完成: {}", job.getJobName(), result);
            } catch (Exception e) {
                log.error("手动执行任务[{}]失败: {}", job.getJobName(), e.getMessage(), e);
            }
        });
    }
}
