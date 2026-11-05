/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 任务管理与运行日志接口
 */

package org.yeharn.lp.job.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.job.core.CronSupport;
import org.yeharn.lp.job.entity.SysJob;
import org.yeharn.lp.job.entity.SysJobLog;
import org.yeharn.lp.job.service.JobService;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理：任务 CRUD、启停、执行一次、运行批次查询
 * <p>
 * 任务操作与执行记录统一记录在任务运行日志中，不再写入系统操作日志。
 */
@RestController
@RequestMapping("/tool/job")
@RequiredArgsConstructor
public class JobController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobService jobService;

    /* ---------------- 任务 ---------------- */

    @SaCheckPermission("tool:job:list")
    @GetMapping("/list")
    public R<PageResult<SysJob>> list(@RequestParam(required = false) String jobName,
                                      @RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(jobService.page(jobName, pageNum, pageSize));
    }

    @SaCheckPermission("tool:job:list")
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("handlers", jobService.handlers());
        info.put("scheduler", "lp-job ThreadPoolTaskScheduler + Quartz Cron");
        return R.ok(info);
    }

    /** cron 预览：返回未来 N 次执行时间（默认 6 条），供前端可视化选择同步展示 */
    @SaCheckPermission("tool:job:list")
    @GetMapping("/preview")
    public R<List<String>> preview(@RequestParam String cron,
                                   @RequestParam(defaultValue = "6") int count) {
        return R.ok(CronSupport.preview(cron, count).stream().map(FMT::format).toList());
    }

    @SaCheckPermission("tool:job:add")
    @PostMapping
    public R<Void> add(@RequestBody SysJob job) {
        jobService.add(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysJob job) {
        jobService.update(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        jobService.delete(ids);
        return R.ok();
    }

    @SaCheckPermission("tool:job:changeStatus")
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysJob job) {
        jobService.changeStatus(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:run")
    @PostMapping("/run/{jobId}")
    public R<Void> run(@PathVariable Long jobId) {
        jobService.runOnce(jobId);
        return R.ok();
    }

    /* ---------------- 运行日志（批次） ---------------- */

    @SaCheckPermission("tool:job:list")
    @GetMapping("/log/list")
    public R<PageResult<SysJobLog>> logList(@RequestParam(required = false) Long jobId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(jobService.logPage(jobId, status, pageNum, pageSize));
    }

    @SaCheckPermission("tool:job:list")
    @GetMapping("/log/{logId}")
    public R<SysJobLog> logDetail(@PathVariable Long logId) {
        return R.ok(jobService.logDetail(logId));
    }
}
