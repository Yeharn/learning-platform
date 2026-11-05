/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 监控服务 - 服务注册监控接口控制器（基于 Eureka 注册中心）
 */

package org.yeharn.lp.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.monitor.service.ServiceMonitorService;

import java.util.Map;

@RestController
@RequestMapping("/monitor/service")
@RequiredArgsConstructor
public class ServiceMonitorController {

    private final ServiceMonitorService serviceMonitorService;

    @SaCheckPermission("monitor:service:list")
    @GetMapping
    public R<Map<String, Object>> overview() {
        return R.ok(serviceMonitorService.overview());
    }
}
