/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 本地磁盘存储实现
 */

package org.yeharn.lp.file.storage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.core.ServiceException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘存储
 * <p>
 * Windows 默认 C:/lp/upload，其他系统默认 /data/lp/upload，可通过 lp.file.storage-path 覆盖
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    @Value("${lp.file.storage-path:}")
    private String configuredPath;

    public String rootPath() {
        if (StrUtil.isNotBlank(configuredPath)) {
            return configuredPath;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "C:/lp/upload" : "/data/lp/upload";
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public StorageResult store(String category, String originalName, String contentType,
                               InputStream inputStream, long size) throws Exception {
        String ext = StrUtil.subAfter(originalName, ".", true);
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = IdUtil.simpleUUID() + (StrUtil.isBlank(ext) ? "" : "." + ext);
        String key = category + "/" + month + "/" + fileName;

        Path root = Paths.get(rootPath()).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new ServiceException("非法的文件路径");
        }
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target);
        return new StorageResult("local", key, resolveUrl(key));
    }

    @Override
    public void remove(String path) throws Exception {
        Path root = Paths.get(rootPath()).toAbsolutePath().normalize();
        Path target = root.resolve(path).normalize();
        if (target.startsWith(root)) {
            Files.deleteIfExists(target);
        }
    }

    @Override
    public String resolveUrl(String path) {
        return "/files/" + path;
    }
}
