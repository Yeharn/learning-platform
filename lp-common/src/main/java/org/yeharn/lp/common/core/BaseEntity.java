/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 实体公共字段基类
 */

package org.yeharn.lp.common.core;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计字段基类
 */
@Data
public class BaseEntity implements Serializable {

    @Column("create_by")
    private String createBy;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_by")
    private String updateBy;

    @Column("update_time")
    private LocalDateTime updateTime;
}
