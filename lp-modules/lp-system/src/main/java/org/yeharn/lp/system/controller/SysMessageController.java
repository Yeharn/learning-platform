/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 消息中心接口
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysMessage;
import org.yeharn.lp.system.service.MessageService;

import java.util.List;
import java.util.Map;

/**
 * 消息中心（站内信 / 邮件 / WebSocket）
 */
@RestController
@RequestMapping("/system/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final MessageService messageService;

    @SaCheckPermission("system:message:list")
    @GetMapping("/list")
    public R<PageResult<SysMessage>> list(@RequestParam(defaultValue = "inbox") String box,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String msgType,
                                          @RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(messageService.page(box, title, msgType, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    @SaCheckPermission("system:message:list")
    @GetMapping("/mode")
    public R<Map<String, Object>> modeInfo() {
        return R.ok(messageService.modeInfo());
    }

    @SaCheckPermission("system:message:send")
    @OperLog(title = "消息中心-切换推送模式", businessType = BusinessType.UPDATE)
    @PutMapping("/mode/{mode}")
    public R<Void> switchMode(@PathVariable String mode) {
        messageService.switchMode(mode);
        return R.ok();
    }

    @SaCheckPermission("system:message:send")
    @OperLog(title = "消息中心-发送", businessType = BusinessType.INSERT, saveParam = false)
    @PostMapping("/send")
    public R<Integer> send(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> receivers = (List<Number>) body.get("receiverIds");
        List<Long> ids = receivers.stream().map(Number::longValue).toList();
        int count = messageService.send(SecurityUtils.getUserId(), SecurityUtils.getUsername(), ids,
                String.valueOf(body.get("title")), String.valueOf(body.getOrDefault("content", "")),
                String.valueOf(body.getOrDefault("msgType", "notice")));
        return R.ok("已发送给 " + count + " 位用户", null);
    }

    @PutMapping("/read")
    public R<Void> read(@RequestBody List<Long> ids) {
        messageService.markRead(ids);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> readAll() {
        messageService.markAllRead();
        return R.ok();
    }

    @SaCheckPermission("system:message:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        messageService.delete(ids);
        return R.ok();
    }
}
