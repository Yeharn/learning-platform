/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件记录实体
 */

package org.yeharn.lp.file.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

/**
 * 文件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_file")
public class SysFile extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long fileId;

    /** 分类：image / video / file */
    private String category;
    /** 存储类型：local / minio / rustfs */
    private String storageType;
    private String originalName;
    private String fileName;
    private String filePath;
    private String url;
    private Long fileSize;
    private String fileType;
    private String delFlag;
}
