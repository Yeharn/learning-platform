/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - Redis监控接口控制器
 */

package org.yeharn.lp.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.monitor.service.RedisMonitorService;

import java.util.Map;

@RestController
@RequestMapping("/monitor/redis")
@RequiredArgsConstructor
public class RedisMonitorController {

    private final RedisMonitorService redisMonitorService;

    @SaCheckPermission("monitor:redis:list")
    @GetMapping
    public R<Map<String, Object>> info() {
        return R.ok(redisMonitorService.info());
    }
}
