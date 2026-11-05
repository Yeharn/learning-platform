/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 代码生成业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.datasource.DataSourceContextHolder;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.dto.GenQuery;
import org.yeharn.lp.system.entity.GenTable;
import org.yeharn.lp.system.entity.GenTableColumn;
import org.yeharn.lp.system.generator.CodeTemplate;
import org.yeharn.lp.system.mapper.GenTableColumnMapper;
import org.yeharn.lp.system.mapper.GenTableMapper;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenService {

    private final GenTableMapper tableMapper;
    private final GenTableColumnMapper columnMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<GenTable> page(GenQuery query) {
        QueryWrapper qw = QueryWrapper.create()
                .like("table_name", query.getTableName(), StrUtil.isNotBlank(query.getTableName()))
                .like("table_comment", query.getTableComment(), StrUtil.isNotBlank(query.getTableComment()))
                .orderBy("table_id", false);
        Page<GenTable> page = tableMapper.paginate(Page.of(query.getPageNumSafe(), query.getPageSizeSafe()), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), query.getPageNumSafe(), query.getPageSizeSafe());
    }

    /**
     * 查询数据库中的物理表
     */
    public List<Map<String, Object>> listDbTables(String dsKey) {
        String key = StrUtil.isBlank(dsKey) ? Constants.DS_MASTER : dsKey;
        return DataSourceContextHolder.execute(key, () -> jdbcTemplate.queryForList(
                "select table_name as tableName, table_comment as tableComment, create_time as createTime " +
                        "from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE' " +
                        "order by create_time desc"));
    }

    public List<Map<String, Object>> listDbColumns(String dsKey, String tableName) {
        String key = StrUtil.isBlank(dsKey) ? Constants.DS_MASTER : dsKey;
        return DataSourceContextHolder.execute(key, () -> jdbcTemplate.queryForList(
                "select column_name as columnName, column_comment as columnComment, column_type as columnType, " +
                        "column_key as columnKey, extra as extra, is_nullable as isNullable " +
                        "from information_schema.columns where table_schema = database() and table_name = ? " +
                        "order by ordinal_position", tableName));
    }

    @Transactional(rollbackFor = Exception.class)
    public int importTables(List<String> tableNames, String dsKey) {
        String key = StrUtil.isBlank(dsKey) ? Constants.DS_MASTER : dsKey;
        int count = 0;
        for (String tableName : tableNames) {
            if (tableMapper.selectCountByQuery(QueryWrapper.create().eq("table_name", tableName)) > 0) {
                continue;
            }
            List<Map<String, Object>> columns = listDbColumns(key, tableName);
            if (columns.isEmpty()) {
                continue;
            }
            String tableComment = "";
            List<Map<String, Object>> tables = listDbTables(key);
            for (Map<String, Object> t : tables) {
                if (tableName.equalsIgnoreCase(String.valueOf(t.get("tableName")))) {
                    tableComment = String.valueOf(t.get("tableComment"));
                }
            }

            GenTable table = new GenTable();
            table.setTableName(tableName);
            table.setTableComment(tableComment);
            table.setClassName(StrUtil.upperFirst(toCamel(tableName)));
            table.setTplCategory("crud");
            table.setPackageName("org.yeharn.lp.system");
            table.setModuleName("system");
            table.setBusinessName(toCamel(stripPrefix(tableName)));
            table.setFunctionName(StrUtil.isBlank(tableComment) ? table.getClassName() : tableComment);
            table.setFunctionAuthor("yeharn");
            table.setGenType("0");
            table.setGenPath("/");
            table.setDsKey(key);
            table.setCreateBy(SecurityUtils.getUsername());
            table.setCreateTime(LocalDateTime.now());
            tableMapper.insertSelective(table);

            int sort = 0;
            for (Map<String, Object> col : columns) {
                GenTableColumn entity = buildColumn(table.getTableId(), col);
                entity.setSort(sort++);
                entity.setCreateBy(SecurityUtils.getUsername());
                entity.setCreateTime(LocalDateTime.now());
                columnMapper.insertSelective(entity);
            }
            count++;
        }
        return count;
    }

    public GenTable getInfo(Long tableId) {
        GenTable table = tableMapper.selectOneById(tableId);
        if (table == null) {
            throw new ServiceException("生成配置不存在");
        }
        table.setColumns(columnMapper.selectListByQuery(
                QueryWrapper.create().eq("table_id", tableId).orderBy("sort", true)));
        return table;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(GenTable table) {
        table.setUpdateBy(SecurityUtils.getUsername());
        table.setUpdateTime(LocalDateTime.now());
        tableMapper.update(table);
        if (table.getColumns() != null) {
            for (GenTableColumn column : table.getColumns()) {
                column.setTableId(table.getTableId());
                column.setUpdateBy(SecurityUtils.getUsername());
                column.setUpdateTime(LocalDateTime.now());
                columnMapper.update(column);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> tableIds) {
        for (Long id : tableIds) {
            columnMapper.deleteByTableId(id);
            tableMapper.deleteById(id);
        }
    }

    public Map<String, String> preview(Long tableId) {
        GenTable table = getInfo(tableId);
        return CodeTemplate.render(table, table.getColumns());
    }

    public void download(Long tableId, OutputStream outputStream) {
        GenTable table = getInfo(tableId);
        Map<String, String> files = CodeTemplate.render(table, table.getColumns());
        try (ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
        } catch (Exception e) {
            throw new ServiceException("生成代码失败：" + e.getMessage());
        }
    }

    public Map<String, String> renderBatch(List<Long> tableIds) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Long id : tableIds) {
            result.putAll(preview(id));
        }
        return result;
    }

    private GenTableColumn buildColumn(Long tableId, Map<String, Object> col) {
        String columnName = String.valueOf(col.get("columnName"));
        String columnType = String.valueOf(col.get("columnType"));
        String comment = col.get("columnComment") == null ? "" : String.valueOf(col.get("columnComment"));
        String columnKey = String.valueOf(col.get("columnKey"));
        String extra = String.valueOf(col.get("extra"));

        GenTableColumn entity = new GenTableColumn();
        entity.setTableId(tableId);
        entity.setColumnName(columnName);
        entity.setColumnComment(comment);
        entity.setColumnType(columnType);
        entity.setJavaType(javaType(columnType));
        entity.setJavaField(toCamel(columnName));
        entity.setIsPk("PRI".equalsIgnoreCase(columnKey) ? "1" : "0");
        entity.setIsIncrement(extra.toLowerCase().contains("auto_increment") ? "1" : "0");
        entity.setIsRequired("NO".equalsIgnoreCase(String.valueOf(col.get("isNullable"))) ? "1" : "0");
        entity.setIsInsert("1");
        entity.setIsEdit("1");
        entity.setIsList("1");
        entity.setIsQuery(isBaseColumn(columnName) ? "0" : "1");
        entity.setQueryType("String".equals(entity.getJavaType()) ? "LIKE" : "EQ");
        entity.setHtmlType(htmlType(entity.getJavaType(), columnName));
        entity.setDictType("");
        return entity;
    }

    private boolean isBaseColumn(String name) {
        return List.of("create_by", "create_time", "update_by", "update_time", "del_flag", "remark").contains(name);
    }

    private String javaType(String columnType) {
        String type = columnType.toLowerCase();
        if (type.contains("bigint")) {
            return "Long";
        }
        if (type.contains("int") || type.contains("smallint") || type.contains("tinyint")) {
            return "Integer";
        }
        if (type.contains("decimal") || type.contains("numeric")) {
            return "BigDecimal";
        }
        if (type.contains("double") || type.contains("float")) {
            return "Double";
        }
        if (type.contains("datetime") || type.contains("timestamp")) {
            return "LocalDateTime";
        }
        if (type.contains("date")) {
            return "LocalDate";
        }
        if (type.contains("time")) {
            return "LocalTime";
        }
        if (type.contains("blob") || type.contains("binary")) {
            return "byte[]";
        }
        if (type.contains("text")) {
            return "String";
        }
        return "String";
    }

    private String htmlType(String javaType, String columnName) {
        if (javaType.endsWith("LocalDateTime")) {
            return "datetime";
        }
        if (javaType.endsWith("LocalDate")) {
            return "date";
        }
        if (columnName.contains("status") || columnName.endsWith("type") || columnName.endsWith("sex")) {
            return "select";
        }
        if (javaType.equals("String")) {
            return "input";
        }
        return "input";
    }

    private String stripPrefix(String tableName) {
        int index = tableName.indexOf('_');
        return index > 0 ? tableName.substring(index + 1) : tableName;
    }

    public static String toCamel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public List<GenTableColumn> listColumns(Long tableId) {
        return new ArrayList<>(columnMapper.selectListByQuery(QueryWrapper.create().eq("table_id", tableId)));
    }
}
