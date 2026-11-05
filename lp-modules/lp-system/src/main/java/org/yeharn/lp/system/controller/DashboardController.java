/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 工作台统计接口
 */

package org.yeharn.lp.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.system.service.DashboardService;

import java.util.Map;

/**
 * 控制台
 */
@RestController
@RequestMapping("/system/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public R<Map<String, Object>> dashboard() {
        return R.ok(dashboardService.all());
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(dashboardService.stats());
    }
}
