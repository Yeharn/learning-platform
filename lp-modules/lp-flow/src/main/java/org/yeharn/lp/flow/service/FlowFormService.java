/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程表单模板服务
 */

package org.yeharn.lp.flow.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.utils.IdUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.flow.support.FlowUserSupport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单模板：流程定义的表单路径（form_path）指向 form_code，发起流程时按字段渲染，
 * 提交内容作为流程变量（variable）随实例流转。
 */
@Slf4j
@Service("lpFlowFormService")
@RequiredArgsConstructor
public class FlowFormService {

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> list(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT id, form_code, form_name, remark, version, create_by, create_time ")
                .append("FROM lp_flow_form WHERE del_flag = '0'");
        List<Object> args = new ArrayList<>();
        if (StrUtil.isNotBlank(keyword)) {
            sql.append(" AND (form_name LIKE ? OR form_code LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY update_time DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("formCode", rs.getString("form_code"));
            item.put("formName", rs.getString("form_name"));
            item.put("remark", rs.getString("remark"));
            item.put("version", rs.getInt("version"));
            item.put("createBy", rs.getString("create_by"));
            item.put("createTime", time(rs, "create_time"));
            return item;
        }, args.toArray());
    }

    public Map<String, Object> get(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT * FROM lp_flow_form WHERE id = ? AND del_flag = '0'", (rs, rowNum) -> map(rs), id);
        if (rows.isEmpty()) {
            throw new ServiceException("表单模板不存在");
        }
        return rows.get(0);
    }

    /**
     * 按表单编码取表单定义（流程定义 form_path 引用）
     */
    public Map<String, Object> detailByCode(String formCode) {
        if (StrUtil.isBlank(formCode)) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT * FROM lp_flow_form WHERE form_code = ? AND del_flag = '0'", (rs, rowNum) -> map(rs), formCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(Map<String, Object> body) {
        String formCode = str(body.get("formCode"));
        String formName = str(body.get("formName"));
        if (StrUtil.isBlank(formCode)) {
            throw new ServiceException("表单编码不能为空");
        }
        if (StrUtil.isBlank(formName)) {
            throw new ServiceException("表单名称不能为空");
        }
        String fieldsJson = toJson(body.get("fields"));
        String extJson = toJson(body.get("ext"));
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        String userId = FlowUserSupport.currentUserId();
        if (id == null) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lp_flow_form WHERE form_code = ? AND del_flag = '0'", Integer.class, formCode);
            if (exists != null && exists > 0) {
                throw new ServiceException("表单编码已存在：" + formCode);
            }
            id = IdUtils.nextId();
            jdbcTemplate.update("INSERT INTO lp_flow_form (id, form_code, form_name, fields_json, ext_json, remark, version, "
                            + "create_by, create_time, update_by, update_time, del_flag) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 1, ?, NOW(), ?, NOW(), '0')",
                    id, formCode, formName, fieldsJson, extJson, str(body.get("remark")), userId, userId);
        } else {
            jdbcTemplate.update("UPDATE lp_flow_form SET form_code = ?, form_name = ?, fields_json = ?, ext_json = ?, "
                            + "remark = ?, version = version + 1, update_by = ?, update_time = NOW() WHERE id = ?",
                    formCode, formName, fieldsJson, extJson, str(body.get("remark")), userId, id);
        }
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        jdbcTemplate.update("UPDATE lp_flow_form SET del_flag = '2' WHERE id = ?", id);
    }

    private Map<String, Object> map(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("formCode", rs.getString("form_code"));
        item.put("formName", rs.getString("form_name"));
        item.put("fields", readJson(rs.getString("fields_json")));
        item.put("ext", readJson(rs.getString("ext_json")));
        item.put("remark", rs.getString("remark"));
        item.put("version", rs.getInt("version"));
        item.put("createBy", rs.getString("create_by"));
        item.put("createTime", time(rs, "create_time"));
        item.put("updateTime", time(rs, "update_time"));
        return item;
    }

    private List<Map<String, Object>> readJson(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ServiceException("表单字段格式不正确");
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
