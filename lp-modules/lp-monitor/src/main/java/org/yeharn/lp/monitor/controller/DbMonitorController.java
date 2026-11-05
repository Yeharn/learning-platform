/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - 数据库监控接口控制器
 */

package org.yeharn.lp.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.monitor.service.DbMonitorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/mysql")
@RequiredArgsConstructor
public class DbMonitorController {

    private final DbMonitorService dbMonitorService;

    @SaCheckPermission("monitor:mysql:list")
    @GetMapping
    public R<Map<String, Object>> info() {
        return R.ok(dbMonitorService.info());
    }

    @SaCheckPermission("monitor:mysql:list")
    @GetMapping("/slow")
    public R<List<Map<String, Object>>> slowSqls(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(dbMonitorService.slowSqls(limit));
    }

    @SaCheckPermission("monitor:mysql:list")
    @GetMapping("/long")
    public R<Map<String, Object>> longQueries(@RequestParam(defaultValue = "3") int threshold) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("longQueries", dbMonitorService.longQueries(threshold));
        result.put("transactions", dbMonitorService.transactions());
        return R.ok(result);
    }

    @SaCheckPermission("monitor:mysql:kill")
    @OperLog(title = "MySQL监控-终止会话", businessType = BusinessType.FORCE_LOGOUT)
    @PostMapping("/kill")
    public R<String> kill(@RequestParam long threadId) {
        return R.ok(dbMonitorService.kill(threadId));
    }
}
