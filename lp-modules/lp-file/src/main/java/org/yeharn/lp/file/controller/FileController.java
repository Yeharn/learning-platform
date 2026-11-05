/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件服务接口
 */

package org.yeharn.lp.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.file.entity.SysFile;
import org.yeharn.lp.file.service.FileService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文件服务（独立服务 lp-file）
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @SaCheckPermission("system:file:list")
    @GetMapping("/list")
    public R<PageResult<SysFile>> list(@RequestParam(required = false) String category,
                                       @RequestParam(required = false) String originalName,
                                       @RequestParam(required = false) String storageType,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(fileService.page(category, originalName, storageType, pageNum, pageSize));
    }

    @SaCheckPermission("system:file:list")
    @GetMapping("/mode")
    public R<Map<String, Object>> modeInfo() {
        return R.ok(fileService.modeInfo());
    }

    @SaCheckPermission("system:file:mode")
    @OperLog(title = "文件服务-切换存储模式", businessType = BusinessType.UPDATE)
    @PutMapping("/mode/{mode}")
    public R<Void> switchMode(@PathVariable String mode) {
        fileService.switchMode(mode);
        return R.ok();
    }

    @SaCheckPermission("system:file:upload")
    @OperLog(title = "文件服务-上传", businessType = BusinessType.IMPORT)
    @PostMapping("/upload")
    public R<List<SysFile>> upload(@RequestParam String category,
                                   @RequestParam("files") List<MultipartFile> files) throws Exception {
        return R.ok(fileService.upload(category, files));
    }

    @SaCheckPermission("system:file:remove")
    @OperLog(title = "文件服务-删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        fileService.delete(ids);
        return R.ok();
    }

    @SaCheckPermission("system:file:list")
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile file = fileService.getById(id);
        String url = file.getUrl();
        if (url != null && url.startsWith("/files/")) {
            response.sendRedirect(url);
        } else {
            response.sendRedirect(fileService.currentStorage().resolveUrl(file.getFilePath()));
        }
    }
}
