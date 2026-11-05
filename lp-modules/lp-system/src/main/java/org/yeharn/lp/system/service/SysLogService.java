/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 日志业务服务（按月分表路由）
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.system.dto.LogQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 登录日志 / 操作日志（按月分表）
 */
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final JdbcTemplate jdbcTemplate;
    private final LogTableService logTableService;

    public PageResult<Map<String, Object>> pageLoginLog(LogQuery query) {
        String table = logTableService.resolveTable(LogTableService.LOGIN, query.getMonth());
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(query.getUsername())) {
            where.append(" and username like ?");
            args.add("%" + query.getUsername() + "%");
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            where.append(" and status = ?");
            args.add(query.getStatus());
        }
        appendTimeRange(where, args, query.getBeginTime(), query.getEndTime(), "login_time");
        return page(table, where, args, "login_time", query.getPageNumSafe(), query.getPageSizeSafe());
    }

    public PageResult<Map<String, Object>> pageOperLog(LogQuery query) {
        String table = logTableService.resolveTable(LogTableService.OPER, query.getMonth());
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(query.getTitle())) {
            where.append(" and title like ?");
            args.add("%" + query.getTitle() + "%");
        }
        if (StrUtil.isNotBlank(query.getUsername())) {
            where.append(" and oper_name like ?");
            args.add("%" + query.getUsername() + "%");
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            where.append(" and status = ?");
            args.add(Integer.valueOf(query.getStatus()));
        }
        if (query.getBusinessType() != null) {
            where.append(" and business_type = ?");
            args.add(query.getBusinessType());
        }
        appendTimeRange(where, args, query.getBeginTime(), query.getEndTime(), "oper_time");
        return page(table, where, args, "oper_time", query.getPageNumSafe(), query.getPageSizeSafe());
    }

    public Map<String, Object> getOperLog(Long operId, String month) {
        String table = logTableService.resolveTable(LogTableService.OPER, month);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select * from `" + table + "` where oper_id = ?", operId);
        return rows.isEmpty() ? null : logTableService.toCamel(rows.get(0));
    }

    public void deleteLoginLog(List<Long> ids, String month) {
        deleteByIds(logTableService.resolveTable(LogTableService.LOGIN, month), "info_id", ids);
    }

    public void cleanLoginLog(String month) {
        jdbcTemplate.update("delete from `" + logTableService.resolveTable(LogTableService.LOGIN, month) + "`");
    }

    public void deleteOperLog(List<Long> ids, String month) {
        deleteByIds(logTableService.resolveTable(LogTableService.OPER, month), "oper_id", ids);
    }

    public void cleanOperLog(String month) {
        jdbcTemplate.update("delete from `" + logTableService.resolveTable(LogTableService.OPER, month) + "`");
    }

    public long countLoginLog() {
        return logTableService.countAll(LogTableService.LOGIN);
    }

    public long countOperLog() {
        return logTableService.countAll(LogTableService.OPER);
    }

    public List<String> months(String type) {
        return logTableService.months("login".equals(type) ? LogTableService.LOGIN : LogTableService.OPER);
    }

    /** 控制台最近记录（跨基础表与月表取最新） */
    public List<Map<String, Object>> recent(String type, String timeColumn, int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select * from " + logTableService.unionOf(type) + " order by `" + timeColumn + "` desc limit " + limit);
        rows.replaceAll(logTableService::toCamel);
        return rows;
    }

    private void appendTimeRange(StringBuilder where, List<Object> args, String beginTime, String endTime, String column) {
        if (StrUtil.isNotBlank(beginTime)) {
            where.append(" and ").append(column).append(" >= ?");
            args.add(beginTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            where.append(" and ").append(column).append(" <= ?");
            args.add(endTime.length() > 10 ? endTime : endTime + " 23:59:59");
        }
    }

    private PageResult<Map<String, Object>> page(String table, StringBuilder where, List<Object> args,
                                                 String orderColumn, int pageNum, int pageSize) {
        Long total = jdbcTemplate.queryForObject("select count(*) from `" + table + "`" + where, Long.class, args.toArray());
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select * from `" + table + "`" + where + " order by `" + orderColumn + "` desc limit ?, ?",
                queryArgs.toArray());
        rows.replaceAll(logTableService::toCamel);
        return new PageResult<>(total == null ? 0 : total, rows, pageNum, pageSize);
    }

    private void deleteByIds(String table, String idColumn, List<Long> ids) {
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            in.append(i > 0 ? "," : "").append("?");
        }
        jdbcTemplate.update("delete from `" + table + "` where `" + idColumn + "` in (" + in + ")", ids.toArray());
    }
}
