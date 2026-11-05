/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: MinIO / RustFS 对象存储实现（S3 协议）
 */

package org.yeharn.lp.file.storage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.core.ServiceException;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * S3 协议对象存储（MinIO / RustFS，均为 S3 兼容实现）
 * <p>
 * 由 sys_config: sys.file.mode 控制启用哪个模式，配置项 lp.file.s3.*
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lp.file.s3", name = "access-key")
public class S3FileStorage implements FileStorage {

    @Value("${lp.file.mode:minio}")
    private String configuredMode;

    @Value("${lp.file.s3.endpoint:http://127.0.0.1:9000}")
    private String endpoint;

    @Value("${lp.file.s3.access-key:}")
    private String accessKey;

    @Value("${lp.file.s3.secret-key:}")
    private String secretKey;

    @Value("${lp.file.s3.bucket:lp-files}")
    private String bucket;

    @Getter
    private volatile MinioClient client;

    private final Object initLock = new Object();

    private MinioClient obtain() {
        if (client == null) {
            synchronized (initLock) {
                if (client == null) {
                    client = MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(accessKey, secretKey)
                            .build();
                }
            }
        }
        return client;
    }

    @Override
    public String type() {
        return StrUtil.blankToDefault(configuredMode, "minio");
    }

    @Override
    public StorageResult store(String category, String originalName, String contentType,
                               InputStream inputStream, long size) throws Exception {
        MinioClient mc = obtain();
        ensureBucket(mc);
        String ext = StrUtil.subAfter(originalName, ".", true);
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = category + "/" + month + "/" + IdUtil.simpleUUID() + (StrUtil.isBlank(ext) ? "" : "." + ext);
        try (InputStream in = inputStream) {
            mc.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, size, -1)
                    .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream"))
                    .build());
        }
        return new StorageResult(type(), key, resolveUrl(key));
    }

    @Override
    public void remove(String path) throws Exception {
        obtain().removeObject(RemoveObjectArgs.builder().bucket(bucket).object(path).build());
    }

    @Override
    public String resolveUrl(String path) {
        String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return base + bucket + "/" + path;
    }

    /**
     * 读取对象内容（用于代理下载）
     */
    public InputStream getObject(String path) throws Exception {
        return obtain().getObject(GetObjectArgs.builder().bucket(bucket).object(path).build());
    }

    private void ensureBucket(MinioClient mc) {
        try {
            if (!mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                mc.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new ServiceException("对象存储初始化失败：" + e.getMessage());
        }
    }
}
