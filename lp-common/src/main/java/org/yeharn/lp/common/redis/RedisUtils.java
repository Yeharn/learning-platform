/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Redis 操作工具
 */

package org.yeharn.lp.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作封装
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, Object value, long seconds) {
        set(key, value, seconds, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public String getString(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : String.valueOf(value);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long seconds) {
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 游标扫描匹配的 key，避免 KEYS 命令阻塞 Redis
     */
    public Set<String> scan(String pattern, int count) {
        Set<String> keys = new LinkedHashSet<>();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
                // 游标关闭异常忽略
            }
            return null;
        });
        return keys;
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void hDelete(String key, Object... fields) {
        redisTemplate.opsForHash().delete(key, fields);
    }

    public void leftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public List<Object> range(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }
}
