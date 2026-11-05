/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 工作台业务服务
 */

package org.yeharn.lp.system.service;

import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.yeharn.lp.system.entity.SysNotice;
import org.yeharn.lp.system.mapper.SysDeptMapper;
import org.yeharn.lp.system.mapper.SysMenuMapper;
import org.yeharn.lp.system.mapper.SysNoticeMapper;
import org.yeharn.lp.system.mapper.SysRoleMapper;
import org.yeharn.lp.system.mapper.SysUserMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页控制台数据
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysNoticeMapper noticeMapper;
    private final OnlineUserService onlineUserService;
    private final JdbcTemplate jdbcTemplate;
    private final LogTableService logTableService;
    private final SysLogService logService;

    public Map<String, Object> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userCount", userMapper.selectCountByQuery(QueryWrapper.create()));
        map.put("roleCount", roleMapper.selectCountByQuery(QueryWrapper.create()));
        map.put("menuCount", menuMapper.selectCountByQuery(QueryWrapper.create()));
        map.put("deptCount", deptMapper.selectCountByQuery(QueryWrapper.create()));
        map.put("onlineCount", onlineUserService.count());
        map.put("loginCount", logTableService.countAll(LogTableService.LOGIN));
        map.put("operCount", logTableService.countAll(LogTableService.OPER));

        Long todayLogin = jdbcTemplate.queryForObject(
                "select count(*) from " + logTableService.unionOf(LogTableService.LOGIN)
                        + " where date(login_time) = curdate()", Long.class);
        Long todayOper = jdbcTemplate.queryForObject(
                "select count(*) from " + logTableService.unionOf(LogTableService.OPER)
                        + " where date(oper_time) = curdate()", Long.class);
        map.put("todayLogin", todayLogin == null ? 0 : todayLogin);
        map.put("todayOper", todayOper == null ? 0 : todayOper);
        return map;
    }

    public List<Map<String, Object>> loginTrend() {
        return jdbcTemplate.queryForList(
                "select date_format(login_time, '%m-%d') as day, count(*) as total from "
                        + logTableService.unionOf(LogTableService.LOGIN)
                        + " where login_time >= date_sub(curdate(), interval 6 day) "
                        + "group by date_format(login_time, '%m-%d'), date(login_time) order by date(login_time)");
    }

    public List<Map<String, Object>> userDeptStat() {
        return jdbcTemplate.queryForList(
                "select d.dept_name as name, count(u.user_id) as value from sys_dept d " +
                        "left join sys_user u on u.dept_id = d.dept_id and u.del_flag = '0' " +
                        "where d.parent_id <> 0 group by d.dept_id, d.dept_name order by value desc limit 8");
    }

    public List<Map<String, Object>> operTypeStat() {
        return jdbcTemplate.queryForList(
                "select business_type as type, count(*) as total from "
                        + logTableService.unionOf(LogTableService.OPER) + " group by business_type");
    }

    public List<Map<String, Object>> recentLoginLogs() {
        return logService.recent(LogTableService.LOGIN, "login_time", 8);
    }

    public List<Map<String, Object>> recentOperLogs() {
        return logService.recent(LogTableService.OPER, "oper_time", 8);
    }

    public List<SysNotice> notices() {
        return noticeMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "0").orderBy("notice_id", false).limit(5));
    }

    public Map<String, Object> all() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stats", stats());
        map.put("loginTrend", loginTrend());
        map.put("deptStat", userDeptStat());
        map.put("operStat", operTypeStat());
        map.put("loginLogs", recentLoginLogs());
        map.put("operLogs", recentOperLogs());
        map.put("notices", notices());
        return map;
    }

    public List<Map<String, Object>> loginMap() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> log : recentLoginLogs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", log.get("username"));
            item.put("ipaddr", log.get("ipaddr"));
            item.put("status", log.get("status"));
            item.put("loginTime", log.get("loginTime"));
            list.add(item);
        }
        return list;
    }
}
