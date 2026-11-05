/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件上传、下载与元数据服务
 */

package org.yeharn.lp.file.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.file.entity.SysFile;
import org.yeharn.lp.file.mapper.SysFileMapper;
import org.yeharn.lp.file.storage.FileStorage;
import org.yeharn.lp.file.storage.LocalFileStorage;
import org.yeharn.lp.file.storage.S3FileStorage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件服务：本地 / MinIO / RustFS（S3 协议），模式存于 sys_config: sys.file.mode
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final List<String> MODES = Arrays.asList("local", "minio", "rustfs");
    private static final List<String> CATEGORIES = Arrays.asList("image", "video", "file");
    private static final Map<String, Long> SIZE_LIMIT = Map.of("image", 10L, "video", 100L, "file", 50L);

    private final SysFileMapper fileMapper;
    private final FileModeService fileModeService;
    private final LocalFileStorage localFileStorage;
    private final ObjectProvider<S3FileStorage> s3StorageProvider;

    public FileStorage currentStorage() {
        String mode = currentMode();
        if ("minio".equals(mode) || "rustfs".equals(mode)) {
            S3FileStorage s3 = s3StorageProvider.getIfAvailable();
            if (s3 == null) {
                throw new ServiceException("对象存储未配置，请先设置 lp.file.s3.* 参数后重启服务");
            }
            return s3;
        }
        return localFileStorage;
    }

    public String currentMode() {
        return fileModeService.current();
    }

    public Map<String, Object> modeInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("mode", currentMode());
        info.put("modes", MODES);
        info.put("categories", CATEGORIES);
        info.put("localPath", localFileStorage.rootPath());
        return info;
    }

    public void switchMode(String mode) {
        if (!MODES.contains(mode)) {
            throw new ServiceException("不支持的存储模式：" + mode);
        }
        if ("minio".equals(mode) || "rustfs".equals(mode)) {
            currentStorage();
        }
        fileModeService.save(mode);
    }

    public PageResult<SysFile> page(String category, String originalName, String storageType, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("category", category, StrUtil.isNotBlank(category))
                .like("original_name", originalName, StrUtil.isNotBlank(originalName))
                .eq("storage_type", storageType, StrUtil.isNotBlank(storageType))
                .orderBy("file_id", false);
        Page<SysFile> page = fileMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public SysFile getById(Long id) {
        SysFile file = fileMapper.selectOneById(id);
        if (file == null) {
            throw new ServiceException("文件不存在");
        }
        return file;
    }

    public List<SysFile> upload(String category, List<MultipartFile> files) throws Exception {
        if (!CATEGORIES.contains(category)) {
            throw new ServiceException("不支持的文件分类：" + category);
        }
        FileStorage storage = currentStorage();
        long limitBytes = SIZE_LIMIT.get(category) * 1024 * 1024;
        List<SysFile> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ServiceException("文件为空");
            }
            if (file.getSize() > limitBytes) {
                throw new ServiceException("文件超过大小限制 " + SIZE_LIMIT.get(category) + "MB：" + file.getOriginalFilename());
            }
            var result = storage.store(category, file.getOriginalFilename(), file.getContentType(),
                    file.getInputStream(), file.getSize());
            SysFile entity = new SysFile();
            entity.setCategory(category);
            entity.setStorageType(result.getStorageType());
            entity.setOriginalName(file.getOriginalFilename());
            entity.setFileName(StrUtil.subAfter(result.getPath(), "/", true));
            entity.setFilePath(result.getPath());
            entity.setUrl(result.getUrl());
            entity.setFileSize(file.getSize());
            entity.setFileType(file.getContentType());
            entity.setCreateBy(SecurityUtils.getUsername());
            entity.setCreateTime(LocalDateTime.now());
            fileMapper.insertSelective(entity);
            results.add(entity);
        }
        return results;
    }

    public void delete(List<Long> ids) {
        FileStorage storage = currentStorage();
        for (Long id : ids) {
            SysFile file = fileMapper.selectOneById(id);
            if (file == null) {
                continue;
            }
            try {
                storage.remove(file.getFilePath());
            } catch (Exception ignored) {
                // 存储端删除失败不阻断记录删除
            }
            fileMapper.deleteById(id);
        }
    }
}
