/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件存储结果
 */

package org.yeharn.lp.file.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {

    /** 存储类型：local / minio / rustfs */
    private String storageType;
    /** 存储路径 / 对象 Key（相对路径，如 image/202611/xxx.png） */
    private String path;
    /** 访问地址（本地为 /files/xxx，对象存储为完整 URL） */
    private String url;
}
