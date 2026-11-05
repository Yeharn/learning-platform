/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 消息
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

/**
 * 站内消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_message")
public class SysMessage extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long messageId;

    private String title;
    private String content;
    /** notice / announce / task */
    private String msgType;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    /** 0 未读 / 1 已读 */
    private String readFlag;
    private java.time.LocalDateTime readTime;
    private String delFlag;
}
