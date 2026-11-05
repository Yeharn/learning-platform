/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 日志接口（按月分表）
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.dto.LogQuery;
import org.yeharn.lp.system.service.SysLogService;

import java.util.List;
import java.util.Map;

/**
 * 日志查询
 */
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @SaCheckPermission("monitor:loginlog:list")
    @GetMapping("/login/list")
    public R<PageResult<Map<String, Object>>> loginList(LogQuery query) {
        return R.ok(logService.pageLoginLog(query));
    }

    @SaCheckPermission("monitor:loginlog:remove")
    @OperLog(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/login/{infoIds}")
    public R<Void> deleteLoginLog(@PathVariable List<Long> infoIds, @RequestParam(required = false) String month) {
        logService.deleteLoginLog(infoIds, month);
        return R.ok();
    }

    @SaCheckPermission("monitor:loginlog:remove")
    @DeleteMapping("/login/clean")
    public R<Void> cleanLoginLog(@RequestParam(required = false) String month) {
        logService.cleanLoginLog(month);
        return R.ok();
    }

    @SaCheckPermission("monitor:operlog:list")
    @GetMapping("/oper/list")
    public R<PageResult<Map<String, Object>>> operList(LogQuery query) {
        return R.ok(logService.pageOperLog(query));
    }

    @SaCheckPermission("monitor:operlog:query")
    @GetMapping("/oper/{operId}")
    public R<Map<String, Object>> operInfo(@PathVariable Long operId, @RequestParam(required = false) String month) {
        return R.ok(logService.getOperLog(operId, month));
    }

    @SaCheckPermission("monitor:operlog:remove")
    @OperLog(title = "操作日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/oper/{operIds}")
    public R<Void> deleteOperLog(@PathVariable List<Long> operIds, @RequestParam(required = false) String month) {
        logService.deleteOperLog(operIds, month);
        return R.ok();
    }

    @SaCheckPermission("monitor:operlog:remove")
    @DeleteMapping("/oper/clean")
    public R<Void> cleanOperLog(@RequestParam(required = false) String month) {
        logService.cleanOperLog(month);
        return R.ok();
    }

    @SaCheckPermission("monitor:operlog:list")
    @GetMapping("/months")
    public R<List<String>> months(@RequestParam(defaultValue = "oper") String type) {
        return R.ok(logService.months(type));
    }
}
