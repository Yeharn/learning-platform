/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - Redis监控业务服务
 */

package org.yeharn.lp.monitor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Redis 运行状态
 */
@Service
@RequiredArgsConstructor
public class RedisMonitorService {

    private final StringRedisTemplate stringRedisTemplate;

    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        Properties properties = stringRedisTemplate.execute((RedisConnection conn) -> conn.serverCommands().info());
        if (properties == null) {
            return result;
        }
        Map<String, String> info = new LinkedHashMap<>();
        properties.stringPropertyNames().stream().sorted().forEach(k -> info.put(k, properties.getProperty(k)));
        result.put("info", info);

        Map<String, String> basic = new LinkedHashMap<>();
        for (String key : List.of("redis_version", "redis_mode", "os", "arch_bits", "process_id",
                "uptime_in_days", "connected_clients", "blocked_clients", "used_memory_human",
                "used_memory_peak_human", "maxmemory_human", "maxmemory_policy", "total_connections_received",
                "total_commands_processed", "instantaneous_ops_per_sec", "keyspace_hits", "keyspace_misses",
                "expired_keys", "evicted_keys", "rdb_last_bgsave_status", "aof_enabled", "role")) {
            basic.put(key, info.getOrDefault(key, "-"));
        }
        result.put("basic", basic);

        long hits = parseLong(info.get("keyspace_hits"));
        long misses = parseLong(info.get("keyspace_misses"));
        double hitRate = hits + misses == 0 ? 0 : Math.round(hits * 10000.0 / (hits + misses)) / 100.0;
        result.put("hitRate", hitRate);

        long usedMemory = parseLong(info.get("used_memory"));
        long maxMemory = parseLong(info.get("maxmemory"));
        result.put("memoryUsage", maxMemory <= 0 ? 0 : Math.round(usedMemory * 10000.0 / maxMemory) / 100.0);

        List<Map<String, String>> databases = new ArrayList<>();
        for (String key : info.keySet()) {
            if (key.startsWith("db")) {
                Map<String, String> db = new LinkedHashMap<>();
                db.put("name", key);
                db.put("keys", info.get(key));
                databases.add(db);
            }
        }
        result.put("databases", databases);
        result.put("dbSize", stringRedisTemplate.execute((RedisConnection conn) -> conn.serverCommands().dbSize()));

        Properties commandStats = stringRedisTemplate.execute((RedisConnection conn) ->
                conn.serverCommands().info("commandstats"));
        List<Map<String, String>> commands = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().stream().limit(30).forEach(k -> {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", k.replace("cmdstat_", ""));
                item.put("value", commandStats.getProperty(k));
                commands.add(item);
            });
        }
        result.put("commandStats", commands);
        return result;
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
