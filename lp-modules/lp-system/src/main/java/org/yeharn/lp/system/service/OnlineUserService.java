/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 在线用户业务服务（Redis 会话登记、分页查询与强制下线）
 */

package org.yeharn.lp.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.redis.RedisUtils;
import org.yeharn.lp.common.security.LoginUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 在线用户（基于 Redis 维护）
 */
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String ONLINE_KEY = "lp:online:";
    private static final int SCAN_COUNT = 1000;

    private final RedisUtils redisUtils;

    public void save(String token, LoginUser loginUser, long timeoutSeconds) {
        redisUtils.set(ONLINE_KEY + token, loginUser, timeoutSeconds);
    }

    public void remove(String token) {
        if (token != null) {
            redisUtils.delete(ONLINE_KEY + token);
        }
    }

    /**
     * 在线会话列表，按登录时间倒序
     */
    public List<LoginUser> list(String username) {
        List<LoginUser> users = new ArrayList<>();
        if (username != null && !username.isBlank()) {
            username = username.trim();
        }
        Set<String> keys = redisUtils.scan(ONLINE_KEY + "*", SCAN_COUNT);
        if (keys == null || keys.isEmpty()) {
            return users;
        }
        for (String key : keys) {
            Object value = redisUtils.get(key);
            if (value instanceof LoginUser user) {
                if (username == null || username.isEmpty() || username.equals(user.getUsername())) {
                    user.setToken(key.substring(ONLINE_KEY.length()));
                    users.add(user);
                }
            }
        }
        users.sort(Comparator.comparing((LoginUser u) -> u.getLoginTime() == null ? 0L : u.getLoginTime()).reversed());
        return users;
    }

    /**
     * 分页查询在线会话
     */
    public PageResult<LoginUser> page(String username, long pageNum, long pageSize) {
        List<LoginUser> all = list(username);
        long num = pageNum <= 0 ? 1 : pageNum;
        long size = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        int total = all.size();
        int from = (int) Math.min((num - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        return new PageResult<>(total, new ArrayList<>(all.subList(from, to)), num, size);
    }

    /**
     * 在线会话总数（顶部徽标与工作台统计，仅统计 key 数量，不反序列化会话）
     */
    public long count() {
        Set<String> keys = redisUtils.scan(ONLINE_KEY + "*", SCAN_COUNT);
        return keys == null ? 0 : keys.size();
    }

    public void kick(String token) {
        redisUtils.delete(ONLINE_KEY + token);
    }
}
