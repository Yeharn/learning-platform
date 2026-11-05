/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 数据源管理接口控制器
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
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.entity.SysDatasource;
import org.yeharn.lp.system.service.SysDatasourceService;

import java.util.List;
import java.util.Map;

/**
 * 多数据源管理
 */
@RestController
@RequestMapping("/system/datasource")
@RequiredArgsConstructor
public class SysDatasourceController {

    private final SysDatasourceService datasourceService;

    @SaCheckPermission("system:datasource:list")
    @GetMapping("/list")
    public R<List<SysDatasource>> list() {
        return R.ok(datasourceService.list());
    }

    @SaCheckPermission("system:datasource:query")
    @GetMapping("/{id}")
    public R<SysDatasource> getInfo(@PathVariable Long id) {
        return R.ok(datasourceService.getById(id));
    }

    @SaCheckPermission("system:datasource:add")
    @OperLog(title = "数据源管理", businessType = BusinessType.INSERT, saveParam = false)
    @PostMapping
    public R<Void> add(@RequestBody SysDatasource datasource) {
        datasourceService.add(datasource);
        return R.ok();
    }

    @SaCheckPermission("system:datasource:edit")
    @OperLog(title = "数据源管理", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping
    public R<Void> edit(@RequestBody SysDatasource datasource) {
        datasourceService.update(datasource);
        return R.ok();
    }

    @SaCheckPermission("system:datasource:remove")
    @OperLog(title = "数据源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        datasourceService.delete(id);
        return R.ok();
    }

    @SaCheckPermission("system:datasource:test")
    @OperLog(title = "数据源管理-连接测试", businessType = BusinessType.OTHER, saveParam = false)
    @PostMapping("/test")
    public R<String> test(@RequestBody SysDatasource datasource) {
        return R.ok(datasourceService.test(datasource));
    }

    @GetMapping("/pool/{dsKey}")
    public R<Map<String, Object>> pool(@PathVariable String dsKey) {
        return R.ok(datasourceService.poolStats(dsKey));
    }
}
