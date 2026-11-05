/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 日志表业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.yeharn.lp.system.entity.SysLoginLog;
import org.yeharn.lp.system.entity.SysOperLog;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 日志按月分表：sys_oper_log_yyyyMM / sys_login_log_yyyyMM
 * 写入时月表不存在则按基础表结构自动创建；查询历史月份无月表时回落基础表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogTableService {

    public static final String OPER = "sys_oper_log";
    public static final String LOGIN = "sys_login_log";

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{6}$");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> CREATED = ConcurrentHashMap.newKeySet();

    private final JdbcTemplate jdbcTemplate;

    public String currentMonth() {
        return LocalDate.now().format(MONTH_FMT);
    }

    public String nextMonth() {
        return LocalDate.now().plusMonths(1).format(MONTH_FMT);
    }

    public String normalizeMonth(String month) {
        return StrUtil.isBlank(month) ? currentMonth() : month;
    }

    /** 确保月表存在并返回完整表名 */
    public String ensureTable(String type, String month) {
        if (!MONTH_PATTERN.matcher(month).matches()) {
            throw new IllegalArgumentException("非法的月份格式: " + month);
        }
        String table = type + "_" + month;
        if (CREATED.contains(table)) {
            return table;
        }
        if (!exists(type, month)) {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + table + "` LIKE `" + type + "`");
            log.info("已创建日志分表: {}", table);
        }
        CREATED.add(table);
        return table;
    }

    public boolean exists(String type, String month) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class, type + "_" + month);
        return count != null && count > 0;
    }

    /** 查询用表名：月表存在用月表，否则回落基础表（历史数据） */
    public String resolveTable(String type, String month) {
        month = normalizeMonth(month);
        return exists(type, month) ? ensureTable(type, month) : type;
    }

    /** 全部数据表（基础表 + 所有月表，月表在前） */
    public List<String> allTables(String type) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() and table_name like ?",
                type + "\\_%");
        List<String> tables = new ArrayList<>();
        tables.add(type);
        rows.stream().map(r -> String.valueOf(r.get("table_name")))
                .filter(t -> !t.equals(type))
                .sorted((a, b) -> b.compareTo(a))
                .forEach(tables::add);
        return tables;
    }

    /** 可查询的月份列表（月表），新到旧 */
    public List<String> months(String type) {
        List<String> months = new ArrayList<>();
        for (String table : allTables(type)) {
            if (table.length() > type.length() + 1) {
                months.add(table.substring(type.length() + 1));
            }
        }
        return months;
    }

    public void insertOperLog(SysOperLog log0) {
        String tb = ensureTable(OPER, currentMonth());
        jdbcTemplate.update("insert into `" + tb + "`"
                        + " (title,business_type,method,request_method,operator_type,oper_name,dept_name,"
                        + "oper_url,oper_ip,oper_location,oper_param,json_result,status,error_msg,oper_time,cost_time)"
                        + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                log0.getTitle(), log0.getBusinessType(), log0.getMethod(), log0.getRequestMethod(),
                log0.getOperatorType(), log0.getOperName(), log0.getDeptName(),
                log0.getOperUrl(), log0.getOperIp(), log0.getOperLocation(),
                log0.getOperParam(), log0.getJsonResult(), log0.getStatus(), log0.getErrorMsg(),
                Timestamp.valueOf(log0.getOperTime() == null ? LocalDateTime.now() : log0.getOperTime()),
                log0.getCostTime());
    }

    public void insertLoginLog(SysLoginLog log0) {
        String tb = ensureTable(LOGIN, currentMonth());
        jdbcTemplate.update("insert into `" + tb + "`"
                        + " (username,ipaddr,login_location,browser,os,status,msg,login_time) values (?,?,?,?,?,?,?,?)",
                log0.getUsername(), log0.getIpaddr(), log0.getLoginLocation(),
                log0.getBrowser(), log0.getOs(), log0.getStatus(), log0.getMsg(),
                Timestamp.valueOf(log0.getLoginTime() == null ? LocalDateTime.now() : log0.getLoginTime()));
    }

    /** 基础表 + 全部月表 union 子查询（必须带别名，否则 MySQL 报 Every derived table must have its own alias） */
    public String unionOf(String type) {
        StringBuilder sb = new StringBuilder("( ");
        List<String> tables = allTables(type);
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0) {
                sb.append(" union all ");
            }
            sb.append("select * from `").append(tables.get(i)).append("`");
        }
        return sb.append(" ) lp_union").toString();
    }

    /** 全量总数（基础表 + 全部月表） */
    public long countAll(String type) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + unionOf(type), Long.class);
        return count == null ? 0 : count;
    }

    /** 驼峰转换：snake_case -> camelCase（用于 JdbcTemplate 查询结果） */
    public Map<String, Object> toCamel(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((k, v) -> {
            String key = k == null ? null : StrUtil.toCamelCase(k.toLowerCase());
            result.put(key, v);
        });
        return result;
    }
}
