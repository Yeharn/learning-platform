/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 公告
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_notice")
public class SysNotice extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long noticeId;

    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String status;
    private String remark;
}
