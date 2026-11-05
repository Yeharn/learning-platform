/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 定时任务接口
 */

package org.yeharn.lp.system.controller;

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
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.entity.SysJob;
import org.yeharn.lp.system.job.JobService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理
 */
@RestController
@RequestMapping("/tool/job")
@RequiredArgsConstructor
public class SysJobController {

    private final JobService jobService;

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
        info.put("scheduler", "内置 ThreadPoolTaskScheduler");
        info.put("xxlJobEnabled", Boolean.getBoolean("lp.xxl-job.enabled") || isActive());
        return R.ok(info);
    }

    private boolean isActive() {
        try {
            return Boolean.parseBoolean(System.getProperty("lp.xxl-job.enabled", "false"));
        } catch (Exception e) {
            return false;
        }
    }

    @SaCheckPermission("tool:job:add")
    @OperLog(title = "定时任务-新增", businessType = BusinessType.INSERT, saveParam = false)
    @PostMapping
    public R<Void> add(@RequestBody SysJob job) {
        jobService.add(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:edit")
    @OperLog(title = "定时任务-修改", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping
    public R<Void> update(@RequestBody SysJob job) {
        jobService.update(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:remove")
    @OperLog(title = "定时任务-删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        jobService.delete(ids);
        return R.ok();
    }

    @SaCheckPermission("tool:job:changeStatus")
    @OperLog(title = "定时任务-启停", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysJob job) {
        jobService.changeStatus(job);
        return R.ok();
    }

    @SaCheckPermission("tool:job:run")
    @OperLog(title = "定时任务-执行一次", businessType = BusinessType.OTHER)
    @PostMapping("/run/{jobId}")
    public R<Void> run(@PathVariable Long jobId) {
        jobService.runOnce(jobId);
        return R.ok();
    }
}
