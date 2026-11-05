/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 站内消息 WebSocket 推送处理器
 */

package org.yeharn.lp.system.websocket;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 消息实时推送 WebSocket 处理器
 * 连接地址：/ws/message?token={sa-token}，认证通过后按用户维护会话
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUser(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket 连接成功 userId={}，在线={}", userId, SESSIONS.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUser(session);
        if (userId != null) {
            Set<WebSocketSession> set = SESSIONS.get(userId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    SESSIONS.remove(userId);
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 心跳
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException ignored) {
            }
        }
    }

    private Long resolveUser(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (StrUtil.isBlank(query)) {
            return null;
        }
        String token = null;
        for (String pair : query.split("&")) {
            if (pair.startsWith("token=")) {
                token = StrUtil.subAfter(pair, "=", false);
            }
        }
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (Exception e) {
            return null;
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (IOException ignored) {
        }
    }

    /**
     * 向指定用户推送消息（JSON 文本）
     */
    public static void push(Long userId, Object payload) {
        Set<WebSocketSession> set = SESSIONS.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String text;
        try {
            text = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession session : set) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(text));
                    }
                } catch (IOException e) {
                    log.warn("WebSocket 推送失败 userId={}: {}", userId, e.getMessage());
                }
            }
        }
    }

    public static int onlineCount() {
        return SESSIONS.size();
    }
}
