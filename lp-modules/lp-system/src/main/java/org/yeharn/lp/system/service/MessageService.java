/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 消息业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysConfig;
import org.yeharn.lp.system.entity.SysMessage;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysMessageMapper;
import org.yeharn.lp.system.mapper.SysUserMapper;
import org.yeharn.lp.system.websocket.MessageWebSocketHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心：站内信（默认）/ 邮件 / WebSocket 实时推送
 * 站内信始终落库，邮件与 WebSocket 为追加投递，模式存于 sys_config: sys.message.mode
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    public static final String MODE_KEY = "sys.message.mode";
    private static final List<String> MODES = List.of("inner", "email", "websocket");
    private static final List<String> TYPES = List.of("notice", "announce", "task");

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final SysConfigService configService;
    private final ObjectProvider<EmailSender> emailSenderProvider;

    public Map<String, Object> modeInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("mode", currentMode());
        info.put("modes", MODES);
        info.put("types", TYPES);
        info.put("websocketOnline", MessageWebSocketHandler.onlineCount());
        return info;
    }

    public String currentMode() {
        return configService.getValue(MODE_KEY, "inner");
    }

    public void switchMode(String mode) {
        if (!MODES.contains(mode)) {
            throw new ServiceException("不支持的消息推送模式：" + mode);
        }
        if ("email".equals(mode)) {
            EmailSender sender = emailSenderProvider.getIfAvailable();
            if (sender == null || !sender.isEnabled()) {
                throw new ServiceException("邮件通道未启用，请配置 spring.mail.host 并设置 lp.mail.enabled=true");
            }
        }
        SysConfig config = configService.listAll().stream()
                .filter(c -> MODE_KEY.equals(c.getConfigKey())).findFirst().orElse(null);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(MODE_KEY);
            config.setConfigName("消息中心-推送模式");
            config.setConfigType("Y");
            config.setConfigValue(mode);
            configService.add(config);
        } else {
            config.setConfigValue(mode);
            configService.update(config);
        }
    }

    /** 发送消息（可多个接收人），始终写入站内信 */
    public int send(Long senderId, String senderName, List<Long> receiverIds, String title,
                    String content, String msgType) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            throw new ServiceException("请选择接收人");
        }
        String type = StrUtil.blankToDefault(msgType, "notice");
        String mode = currentMode();
        for (Long receiverId : receiverIds) {
            SysUser receiver = userMapper.selectOneById(receiverId);
            if (receiver == null) {
                continue;
            }
            SysMessage msg = new SysMessage();
            msg.setTitle(title);
            msg.setContent(content);
            msg.setMsgType(type);
            msg.setSenderId(senderId);
            msg.setSenderName(senderName);
            msg.setReceiverId(receiverId);
            msg.setReceiverName(receiver.getNickname());
            msg.setReadFlag("0");
            msg.setCreateTime(LocalDateTime.now());
            messageMapper.insertSelective(msg);
            deliver(msg, mode);
        }
        return receiverIds.size();
    }

    private void deliver(SysMessage msg, String mode) {
        switch (mode) {
            case "email" -> {
                EmailSender sender = emailSenderProvider.getIfAvailable();
                if (sender != null) {
                    sender.send(msg);
                }
            }
            case "websocket" -> MessageWebSocketHandler.push(msg.getReceiverId(), Map.of(
                    "type", "message", "title", StrUtil.nullToDefault(msg.getTitle(), ""),
                    "content", StrUtil.nullToDefault(msg.getContent(), ""),
                    "msgType", StrUtil.nullToDefault(msg.getMsgType(), "notice"),
                    "messageId", msg.getMessageId()));
            default -> {
            }
        }
    }

    public PageResult<SysMessage> page(String box, String title, String msgType, int pageNum, int pageSize) {
        Long userId = SecurityUtils.getUserId();
        QueryWrapper qw = QueryWrapper.create();
        if ("sent".equals(box)) {
            qw.eq("sender_id", userId);
        } else {
            qw.eq("receiver_id", userId);
        }
        qw.like("title", title, StrUtil.isNotBlank(title))
                .eq("msg_type", msgType, StrUtil.isNotBlank(msgType))
                .orderBy("message_id", false);
        Page<SysMessage> page = messageMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public long unreadCount() {
        Long userId = SecurityUtils.getUserId();
        return messageMapper.selectCountByQuery(QueryWrapper.create()
                .eq("receiver_id", userId).eq("read_flag", "0"));
    }

    public void markRead(List<Long> ids) {
        Long userId = SecurityUtils.getUserId();
        for (Long id : ids) {
            SysMessage msg = messageMapper.selectOneById(id);
            if (msg != null && userId.equals(msg.getReceiverId()) && !"1".equals(msg.getReadFlag())) {
                SysMessage update = new SysMessage();
                update.setMessageId(id);
                update.setReadFlag("1");
                update.setReadTime(LocalDateTime.now());
                messageMapper.update(update);
            }
        }
    }

    public void markAllRead() {
        Long userId = SecurityUtils.getUserId();
        SysMessage update = new SysMessage();
        update.setReadFlag("1");
        update.setReadTime(LocalDateTime.now());
        messageMapper.updateByQuery(update, QueryWrapper.create()
                .eq("receiver_id", userId).eq("read_flag", "0"));
    }

    public void delete(List<Long> ids) {
        boolean admin = SecurityUtils.isAdmin();
        Long userId = SecurityUtils.getUserId();
        for (Long id : ids) {
            SysMessage msg = messageMapper.selectOneById(id);
            if (msg == null) {
                continue;
            }
            if (!admin && !userId.equals(msg.getReceiverId()) && !userId.equals(msg.getSenderId())) {
                throw new ServiceException("无权删除该消息");
            }
            messageMapper.deleteById(id);
        }
    }
}
