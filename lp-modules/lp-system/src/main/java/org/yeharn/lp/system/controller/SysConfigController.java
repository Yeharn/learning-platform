/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 参数接口控制器
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
import org.yeharn.lp.system.entity.SysConfig;
import org.yeharn.lp.system.service.SysConfigService;

import java.util.List;

/**
 * 参数配置
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public R<PageResult<SysConfig>> list(@RequestParam(required = false) String configName,
                                         @RequestParam(required = false) String configKey,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(configService.page(configName, configKey, pageNum, pageSize));
    }

    @SaCheckPermission("system:config:query")
    @GetMapping("/{configId}")
    public R<SysConfig> getInfo(@PathVariable Long configId) {
        return R.ok(configService.getById(configId));
    }

    @SaCheckPermission("system:config:add")
    @OperLog(title = "参数管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysConfig config) {
        configService.add(config);
        return R.ok();
    }

    @SaCheckPermission("system:config:edit")
    @OperLog(title = "参数管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysConfig config) {
        configService.update(config);
        return R.ok();
    }

    @SaCheckPermission("system:config:remove")
    @OperLog(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable List<Long> configIds) {
        configService.delete(configIds);
        return R.ok();
    }

    @SaCheckPermission("system:config:edit")
    @DeleteMapping("/refreshCache")
    public R<Void> refreshCache() {
        configService.refreshCache();
        return R.ok();
    }

    @GetMapping("/value/{configKey}")
    public R<String> value(@PathVariable String configKey) {
        return R.ok(configService.getValue(configKey));
    }
}
