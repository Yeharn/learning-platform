/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 代码生成接口
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.yeharn.lp.system.dto.GenQuery;
import org.yeharn.lp.system.entity.GenTable;
import org.yeharn.lp.system.service.GenService;

import java.util.List;
import java.util.Map;

/**
 * 代码生成
 */
@RestController
@RequestMapping("/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenService genService;

    @SaCheckPermission("tool:gen:list")
    @GetMapping("/list")
    public R<PageResult<GenTable>> list(GenQuery query) {
        return R.ok(genService.page(query));
    }

    @SaCheckPermission("tool:gen:import")
    @GetMapping("/db/list")
    public R<List<Map<String, Object>>> dbTables(@RequestParam(required = false) String dsKey) {
        return R.ok(genService.listDbTables(dsKey));
    }

    @SaCheckPermission("tool:gen:import")
    @GetMapping("/db/columns")
    public R<List<Map<String, Object>>> dbColumns(@RequestParam(required = false) String dsKey,
                                                  @RequestParam String tableName) {
        return R.ok(genService.listDbColumns(dsKey, tableName));
    }

    @SaCheckPermission("tool:gen:import")
    @OperLog(title = "代码生成-导入表", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public R<Integer> importTables(@RequestParam List<String> tables,
                                   @RequestParam(required = false, defaultValue = "master") String dsKey) {
        return R.ok("成功导入 " + genService.importTables(tables, dsKey) + " 张表", null);
    }

    @SaCheckPermission("tool:gen:query")
    @GetMapping("/{tableId}")
    public R<GenTable> getInfo(@PathVariable Long tableId) {
        return R.ok(genService.getInfo(tableId));
    }

    @SaCheckPermission("tool:gen:edit")
    @OperLog(title = "代码生成-修改配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody GenTable table) {
        genService.update(table);
        return R.ok();
    }

    @SaCheckPermission("tool:gen:remove")
    @OperLog(title = "代码生成-删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public R<Void> remove(@PathVariable List<Long> tableIds) {
        genService.delete(tableIds);
        return R.ok();
    }

    @SaCheckPermission("tool:gen:preview")
    @GetMapping("/preview/{tableId}")
    public R<Map<String, String>> preview(@PathVariable Long tableId) {
        return R.ok(genService.preview(tableId));
    }

    @SaCheckPermission("tool:gen:download")
    @OperLog(title = "代码生成-下载", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableId}")
    public void download(@PathVariable Long tableId, HttpServletResponse response) throws Exception {
        GenTable table = genService.getInfo(tableId);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=" + table.getClassName() + ".zip");
        genService.download(tableId, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
