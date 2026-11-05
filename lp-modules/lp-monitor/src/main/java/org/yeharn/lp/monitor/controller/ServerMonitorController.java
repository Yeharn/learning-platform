/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - 服务器监控接口控制器
 */

package org.yeharn.lp.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.monitor.service.ServerMonitorService;

import java.util.Map;

@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class ServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    @SaCheckPermission("monitor:server:list")
    @GetMapping
    public R<Map<String, Object>> info() {
        return R.ok(serverMonitorService.info());
    }
}
