/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 邮件发送器（消息中心邮件通道）
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.yeharn.lp.system.entity.SysMessage;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysUserMapper;

import java.nio.charset.StandardCharsets;

/**
 * 邮件通道：消息中心模式为 email 时投递邮件
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SysUserMapper userMapper;

    @Value("${lp.mail.enabled:false}")
    private boolean enabled;

    @Value("${lp.mail.from:}")
    private String from;

    public boolean isEnabled() {
        return enabled && mailSenderProvider.getIfAvailable() != null;
    }

    public void send(SysMessage message) {
        if (!isEnabled()) {
            log.warn("邮件通道未启用（lp.mail.enabled=false 或未配置 spring.mail.host），跳过邮件投递");
            return;
        }
        SysUser user = userMapper.selectOneById(message.getReceiverId());
        if (user == null || StrUtil.isBlank(user.getEmail())) {
            log.warn("接收人不存在或未配置邮箱，跳过邮件投递 receiverId={}", message.getReceiverId());
            return;
        }
        try {
            var mime = mailSenderProvider.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(StrUtil.blankToDefault(from, "noreply@learning.local"));
            helper.setTo(user.getEmail());
            helper.setSubject(message.getTitle());
            helper.setText(StrUtil.blankToDefault(message.getContent(), ""), true);
            mailSenderProvider.getObject().send(mime);
        } catch (Exception e) {
            log.error("邮件发送失败 receiver={}: {}", user.getEmail(), e.getMessage());
        }
    }
}
