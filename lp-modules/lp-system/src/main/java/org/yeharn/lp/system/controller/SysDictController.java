/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 字典接口控制器
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
import org.yeharn.lp.system.entity.SysDictData;
import org.yeharn.lp.system.entity.SysDictType;
import org.yeharn.lp.system.service.SysDictService;

import java.util.List;

/**
 * 字典管理
 */
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @SaCheckPermission("system:dict:list")
    @GetMapping("/type/list")
    public R<PageResult<SysDictType>> typeList(@RequestParam(required = false) String dictName,
                                               @RequestParam(required = false) String dictType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(dictService.pageType(dictName, dictType, status, pageNum, pageSize));
    }

    @GetMapping("/type/all")
    public R<List<SysDictType>> typeAll() {
        return R.ok(dictService.listTypes());
    }

    @SaCheckPermission("system:dict:add")
    @OperLog(title = "字典类型", businessType = BusinessType.INSERT)
    @PostMapping("/type")
    public R<Void> addType(@RequestBody SysDictType type) {
        dictService.addType(type);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典类型", businessType = BusinessType.UPDATE)
    @PutMapping("/type")
    public R<Void> editType(@RequestBody SysDictType type) {
        dictService.updateType(type);
        return R.ok();
    }

    @SaCheckPermission("system:dict:remove")
    @OperLog(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/type/{dictIds}")
    public R<Void> removeType(@PathVariable List<Long> dictIds) {
        dictService.deleteType(dictIds);
        return R.ok();
    }

    @GetMapping("/data/list")
    public R<PageResult<SysDictData>> dataList(@RequestParam(required = false) String dictType,
                                               @RequestParam(required = false) String dictLabel,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(dictService.pageData(dictType, dictLabel, status, pageNum, pageSize));
    }

    @GetMapping("/data/type/{dictType}")
    public R<List<SysDictData>> dataByType(@PathVariable String dictType) {
        return R.ok(dictService.listDataByType(dictType));
    }

    @SaCheckPermission("system:dict:add")
    @OperLog(title = "字典数据", businessType = BusinessType.INSERT)
    @PostMapping("/data")
    public R<Void> addData(@RequestBody SysDictData data) {
        dictService.addData(data);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典数据", businessType = BusinessType.UPDATE)
    @PutMapping("/data")
    public R<Void> editData(@RequestBody SysDictData data) {
        dictService.updateData(data);
        return R.ok();
    }

    @SaCheckPermission("system:dict:remove")
    @OperLog(title = "字典数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/data/{dictCodes}")
    public R<Void> removeData(@PathVariable List<Long> dictCodes) {
        dictService.deleteData(dictCodes);
        return R.ok();
    }
}
