/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - 数据库监控业务服务
 */

package org.yeharn.lp.monitor.service;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.datasource.DynamicDataSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 运行状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbMonitorService {

    private final JdbcTemplate jdbcTemplate;
    private final DynamicDataSource dynamicDataSource;

    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> status = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("show global status")) {
            status.put(String.valueOf(row.get("Variable_name")), String.valueOf(row.get("Value")));
        }
        Map<String, String> variables = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("show global variables")) {
            variables.put(String.valueOf(row.get("Variable_name")), String.valueOf(row.get("Value")));
        }

        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("version", variables.get("version"));
        basic.put("versionComment", variables.get("version_comment"));
        basic.put("hostname", variables.get("hostname"));
        basic.put("port", variables.get("port"));
        basic.put("datadir", variables.get("datadir"));
        basic.put("uptime", status.get("Uptime"));
        basic.put("maxConnections", variables.get("max_connections"));
        basic.put("threadsConnected", status.get("Threads_connected"));
        basic.put("threadsRunning", status.get("Threads_running"));
        basic.put("questions", status.get("Questions"));
        basic.put("slowQueries", status.get("Slow_queries"));
        basic.put("abortedConnects", status.get("Aborted_connects"));
        basic.put("bytesReceived", status.get("Bytes_received"));
        basic.put("bytesSent", status.get("Bytes_sent"));
        basic.put("connections", status.get("Connections"));
        basic.put("openTables", status.get("Open_tables"));
        basic.put("tableLocksWaited", status.get("Table_locks_waited"));
        basic.put("innodbBufferPoolSize", variables.get("innodb_buffer_pool_size"));

        long uptime = parseLong(status.get("Uptime"));
        long questions = parseLong(status.get("Questions"));
        basic.put("qps", uptime == 0 ? 0 : Math.round(questions * 100.0 / uptime) / 100.0);
        long maxConn = parseLong(variables.get("max_connections"));
        basic.put("connectionUsage", maxConn == 0 ? 0
                : Math.round(parseLong(status.get("Threads_connected")) * 10000.0 / maxConn) / 100.0);
        result.put("basic", basic);

        result.put("status", status);

        HikariDataSource pool = dynamicDataSource.getPool("master");
        if (pool != null && pool.getHikariPoolMXBean() != null) {
            Map<String, Object> poolInfo = new LinkedHashMap<>();
            poolInfo.put("poolName", pool.getPoolName());
            poolInfo.put("active", pool.getHikariPoolMXBean().getActiveConnections());
            poolInfo.put("idle", pool.getHikariPoolMXBean().getIdleConnections());
            poolInfo.put("total", pool.getHikariPoolMXBean().getTotalConnections());
            poolInfo.put("waiting", pool.getHikariPoolMXBean().getThreadsAwaitingConnection());
            poolInfo.put("max", pool.getMaximumPoolSize());
            result.put("pool", poolInfo);
        }

        try {
            result.put("databases", jdbcTemplate.queryForList(
                    "select table_schema as name, round(sum(data_length + index_length) / 1024 / 1024, 2) as size, " +
                            "count(*) as tables from information_schema.tables group by table_schema order by size desc"));
        } catch (Exception e) {
            result.put("databases", new ArrayList<>());
        }

        try {
            List<Map<String, Object>> processes = new ArrayList<>();
            for (Map<String, Object> row : jdbcTemplate.queryForList("show full processlist")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", row.get("Id"));
                item.put("user", row.get("User"));
                item.put("host", row.get("Host"));
                item.put("db", row.get("db"));
                item.put("command", row.get("Command"));
                item.put("time", row.get("Time"));
                item.put("state", row.get("State"));
                item.put("info", row.get("Info"));
                processes.add(item);
            }
            result.put("processes", processes);
        } catch (Exception e) {
            result.put("processes", new ArrayList<>());
        }

        try {
            result.put("topTables", jdbcTemplate.queryForList(
                    "select table_name as name, table_rows as rowsCount, " +
                            "round((data_length + index_length) / 1024, 2) as sizeKb " +
                            "from information_schema.tables where table_schema = database() " +
                            "order by (data_length + index_length) desc limit 10"));
        } catch (Exception e) {
            result.put("topTables", new ArrayList<>());
        }
        return result;
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 慢 SQL 审计：基于 performance_schema 按总耗时排序的语句摘要
     */
    public List<Map<String, Object>> slowSqls(int limit) {
        try {
            return jdbcTemplate.queryForList(
                    "select schema_name as schemaName, digest_text as digestText, count_star as execCount," +
                            " round(sum_timer_wait/1000000000000, 3) as totalTimeSec," +
                            " round(avg_timer_wait/1000000000, 1) as avgTimeMs," +
                            " sum_rows_examined as rowsExamined, sum_rows_sent as rowsReturned," +
                            " round(sum_rows_examined / nullif(count_star, 0)) as avgScanRows," +
                            " last_seen as lastSeen" +
                            " from performance_schema.events_statements_summary_by_digest" +
                            " order by sum_timer_wait desc limit " + Math.min(Math.max(limit, 1), 100));
        } catch (Exception e) {
            log.warn("查询慢SQL审计失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 当前运行的长 SQL（可 KILL）
     */
    public List<Map<String, Object>> longQueries(int thresholdSeconds) {
        try {
            return jdbcTemplate.queryForList(
                    "select id, user, host, db, time, state, info from information_schema.processlist" +
                            " where command = 'Query' and time >= ? and info not like 'show%'" +
                            " and info not like 'select id, user, host%' order by time desc",
                    Math.max(thresholdSeconds, 0));
        } catch (Exception e) {
            log.warn("查询长SQL失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 当前事务（含锁等待 / 锁表事务，可 KILL trx_mysql_thread_id）
     */
    public List<Map<String, Object>> transactions() {
        try {
            return jdbcTemplate.queryForList(
                    "select trx_id as trxId, trx_state as trxState, trx_started as trxStarted," +
                            " timestampdiff(second, trx_started, now()) as runSeconds," +
                            " trx_mysql_thread_id as threadId, trx_query as trxQuery," +
                            " trx_rows_locked as rowsLocked, trx_rows_modified as rowsModified" +
                            " from information_schema.innodb_trx order by trx_started");
        } catch (Exception e) {
            log.warn("查询事务列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 终止指定线程（KILL），仅允许数字 ID
     */
    public String kill(long threadId) {
        if (threadId <= 0) {
            throw new IllegalArgumentException("非法线程ID");
        }
        jdbcTemplate.execute("KILL " + threadId);
        return "已发送 KILL " + threadId;
    }
}
