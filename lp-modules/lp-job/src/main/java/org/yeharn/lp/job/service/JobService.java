/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 任务与运行日志业务服务
 */

package org.yeharn.lp.job.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.job.core.CronSupport;
import org.yeharn.lp.job.core.JobScheduler;
import org.yeharn.lp.job.core.LpJob;
import org.yeharn.lp.job.entity.SysJob;
import org.yeharn.lp.job.entity.SysJobLog;
import org.yeharn.lp.job.mapper.SysJobLogMapper;
import org.yeharn.lp.job.mapper.SysJobMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务业务：CRUD、启停、手动执行、运行批次查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final JobScheduler jobScheduler;
    private final ApplicationContext applicationContext;

    /* ---------------- 任务 ---------------- */

    public PageResult<SysJob> page(String jobName, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .like("job_name", jobName, StrUtil.isNotBlank(jobName))
                .orderBy("job_id", true);
        Page<SysJob> page = jobMapper.paginate(Page.of(pageNum, pageSize), qw);
        page.getRecords().forEach(this::fillNextRunTime);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    /** 补充下次运行时间（按 cron 实时计算，暂停的任务不计算） */
    private void fillNextRunTime(SysJob job) {
        if (!"0".equals(job.getStatus()) || StrUtil.isBlank(job.getCronExpression())) {
            job.setNextRunTime(null);
            return;
        }
        try {
            job.setNextRunTime(CronSupport.nextRunTime(job.getCronExpression()));
        } catch (Exception e) {
            job.setNextRunTime(null);
        }
    }

    public List<String> handlers() {
        return new ArrayList<>(applicationContext.getBeansOfType(LpJob.class).keySet());
    }

    /** 校验处理器与 cron */
    private void validate(SysJob job) {
        if (StrUtil.isBlank(job.getJobName())) {
            throw new ServiceException("任务名称不能为空");
        }
        if (StrUtil.isBlank(job.getJobBean()) || !applicationContext.containsBean(job.getJobBean())) {
            throw new ServiceException("未找到任务处理器：" + job.getJobBean());
        }
        CronSupport.validate(job.getCronExpression());
    }

    public void add(SysJob job) {
        validate(job);
        job.setJobId(null);
        job.setStatus(StrUtil.blankToDefault(job.getStatus(), "0"));
        job.setCreateBy(SecurityUtils.getUsername());
        job.setCreateTime(LocalDateTime.now());
        jobMapper.insertSelective(job);
        jobScheduler.schedule(job);
    }

    public void update(SysJob job) {
        SysJob db = jobMapper.selectOneById(job.getJobId());
        if (db == null) {
            throw new ServiceException("任务不存在");
        }
        validate(job);
        job.setUpdateBy(SecurityUtils.getUsername());
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.update(job);
        jobScheduler.schedule(jobMapper.selectOneById(job.getJobId()));
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            jobScheduler.cancel(id);
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
        jobScheduler.schedule(db);
    }

    public void runOnce(Long jobId) {
        jobScheduler.runNow(jobId, SecurityUtils.getUsername());
    }

    /* ---------------- 运行日志 ---------------- */

    public PageResult<SysJobLog> logPage(Long jobId, String status, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("job_id", jobId, jobId != null)
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("log_id", false);
        Page<SysJobLog> page = jobLogMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public SysJobLog logDetail(Long logId) {
        SysJobLog log = jobLogMapper.selectOneById(logId);
        if (log == null) {
            throw new ServiceException("运行批次不存在");
        }
        return log;
    }

    /** 清理 N 天前的运行日志（供内置清理任务调用） */
    public int cleanLogs(int days) {
        int keep = days <= 0 ? 30 : days;
        LocalDateTime before = LocalDateTime.now().minusDays(keep);
        return jobLogMapper.deleteByQuery(QueryWrapper.create().lt("create_time", before));
    }
}
