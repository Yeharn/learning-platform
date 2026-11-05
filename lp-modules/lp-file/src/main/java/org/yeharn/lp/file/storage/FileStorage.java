/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件存储接口（策略模式）
 */

package org.yeharn.lp.file.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地 / MinIO / RustFS（S3 协议）
 */
public interface FileStorage {

    /**
     * 存储类型标识：local / minio / rustfs
     */
    String type();

    /**
     * 保存文件
     *
     * @param category 分类目录：image / video / file
     */
    StorageResult store(String category, String originalName, String contentType,
                        InputStream inputStream, long size) throws Exception;

    /**
     * 删除文件
     */
    void remove(String path) throws Exception;

    /**
     * 访问地址
     */
    String resolveUrl(String path);
}
