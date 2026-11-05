/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 用户目录：办理人展开、候选人查询、名称回显
 */

package org.yeharn.lp.flow.support;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于系统用户表（sys_user / sys_user_role / sys_role / sys_dept）实现办理人目录能力，
 * 支撑「角色」「部门」「用户」三类办理人表达式的展开与展示。
 */
@Component
@RequiredArgsConstructor
public class FlowUserDirectory {

    private static final String ROLE_PREFIX = "role:";

    private static final String DEPT_PREFIX = "dept:";

    private static final String USER_PREFIX = "user:";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 当前用户的权限标识：用户id、user:id、role:x、dept:x
     * <p>
     * 用于引擎「办理权限」校验，保证节点上直接写角色/部门标识时也能匹配。
     */
    public List<String> permissionFlags(String userId) {
        if (StrUtil.isBlank(userId)) {
            return List.of();
        }
        Set<String> flags = new LinkedHashSet<>();
        flags.add(userId);
        flags.add(USER_PREFIX + userId);
        List<Map<String, Object>> user = jdbcTemplate.queryForList(
                "SELECT dept_id FROM sys_user WHERE user_id = ? AND del_flag = '0'", userId);
        if (!user.isEmpty() && user.get(0).get("dept_id") != null) {
            flags.add(DEPT_PREFIX + user.get(0).get("dept_id"));
        }
        for (Long roleId : roleIds(Long.valueOf(userId))) {
            flags.add(ROLE_PREFIX + roleId);
        }
        return new ArrayList<>(flags);
    }

    /**
     * 把办理人标识展开为具体用户 id 列表：
     * role:x / dept:x 展开为成员，其余（用户id、${} 表达式结果）原样保留
     */
    public List<String> expand(Collection<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return List.of();
        }
        Set<String> userIds = new LinkedHashSet<>();
        for (String flag : flags) {
            if (StrUtil.isBlank(flag)) {
                continue;
            }
            String value = flag.trim();
            if (value.startsWith(ROLE_PREFIX)) {
                userIds.addAll(userIdsByRole(parseId(value.substring(ROLE_PREFIX.length()))));
            } else if (value.startsWith(DEPT_PREFIX)) {
                userIds.addAll(userIdsByDept(parseId(value.substring(DEPT_PREFIX.length()))));
            } else if (value.startsWith(USER_PREFIX)) {
                userIds.add(value.substring(USER_PREFIX.length()));
            } else {
                userIds.add(value);
            }
        }
        userIds.removeIf(StrUtil::isBlank);
        return new ArrayList<>(userIds);
    }

    public List<Long> roleIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, userId);
    }

    public List<String> userIdsByRole(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM sys_user_role WHERE role_id = ?", String.class, roleId);
    }

    public List<String> userIdsByDept(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM sys_user WHERE dept_id = ? AND del_flag = '0' AND status = '0'",
                String.class, deptId);
    }

    public String displayName(String userId) {
        if (StrUtil.isBlank(userId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT COALESCE(NULLIF(nickname, ''), username) FROM sys_user WHERE user_id = ?",
                String.class, userId);
        return names.isEmpty() ? userId : names.get(0);
    }

    public Map<String, String> displayNames(Collection<String> userIds) {
        Map<String, String> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<String> ids = userIds.stream().filter(StrUtil::isNotBlank).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        jdbcTemplate.query("SELECT user_id, COALESCE(NULLIF(nickname, ''), username) AS name FROM sys_user WHERE user_id IN ("
                        + placeholders + ")", rs -> {
                    result.put(rs.getString("user_id"), rs.getString("name"));
                }, ids.toArray());
        ids.forEach(id -> result.putIfAbsent(id, id));
        return result;
    }

    /**
     * 办理人候选（供加签、转办、委派选人）
     */
    public List<Map<String, Object>> candidates(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT u.user_id, u.username, u.nickname, u.dept_id, d.dept_name ")
                .append("FROM sys_user u LEFT JOIN sys_dept d ON d.dept_id = u.dept_id ")
                .append("WHERE u.del_flag = '0' AND u.status = '0'");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword)) {
            sql.append(" AND (u.username LIKE ? OR u.nickname LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY u.user_id LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("user_id"));
            item.put("username", rs.getString("username"));
            item.put("name", StrUtil.blankToDefault(rs.getString("nickname"), rs.getString("username")));
            item.put("deptId", rs.getObject("dept_id"));
            item.put("deptName", rs.getString("dept_name"));
            return item;
        }, args.toArray());
    }

    /**
     * 角色列表（设计器配置办理人时使用）
     */
    public List<Map<String, Object>> roles() {
        return jdbcTemplate.query("SELECT role_id, role_name, role_key FROM sys_role WHERE del_flag = '0' ORDER BY role_id",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("role_id"));
                    item.put("name", rs.getString("role_name"));
                    item.put("key", rs.getString("role_key"));
                    return item;
                });
    }

    /**
     * 部门列表（设计器配置办理人时使用）
     */
    public List<Map<String, Object>> depts() {
        return jdbcTemplate.query("SELECT dept_id, dept_name, parent_id FROM sys_dept WHERE del_flag = '0' ORDER BY order_num",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("dept_id"));
                    item.put("name", rs.getString("dept_name"));
                    item.put("parentId", rs.getObject("parent_id"));
                    return item;
                });
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
