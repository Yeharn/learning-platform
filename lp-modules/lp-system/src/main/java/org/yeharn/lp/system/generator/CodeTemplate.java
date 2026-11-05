/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 代码生成模板（Java / TS / Vue / SQL）
 */

package org.yeharn.lp.system.generator;

import cn.hutool.core.util.StrUtil;
import org.yeharn.lp.system.entity.GenTable;
import org.yeharn.lp.system.entity.GenTableColumn;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成模板（零模板引擎，直接拼装，便于二次修改）
 */
public final class CodeTemplate {

    private static final String AUTHOR = "Yeharn";
    private static final String VERSION = "1.0.0";

    private CodeTemplate() {
    }

    /**
     * 生成文件头注释，保证产出的 Java / TS / Vue / SQL 文件风格统一
     */
    private static String header(String description, String open, String prefix, String close) {
        return open + "\n"
                + prefix + " @Author: " + AUTHOR + "\n"
                + prefix + " @Date:" + LocalDate.now() + "\n"
                + prefix + " @Version: " + VERSION + "\n"
                + prefix + " @Description: " + description + "\n"
                + close + "\n\n";
    }

    private static String javaHeader(String description) {
        return header(description, "/**", " *", " */");
    }

    private static String tsHeader(String description) {
        return header(description, "/**", " *", " */");
    }

    private static String vueHeader(String description) {
        return header(description, "<!--", "  ", "-->");
    }

    private static String sqlHeader(String description) {
        return header(description, "/*", " *", " */");
    }

    public static Map<String, String> render(GenTable table, List<GenTableColumn> columns) {
        Map<String, String> files = new LinkedHashMap<>();
        String className = table.getClassName();
        String basePath = "main/java/" + table.getPackageName().replace('.', '/');
        files.put(basePath + "/entity/" + className + ".java", entity(table, columns));
        files.put(basePath + "/mapper/" + className + "Mapper.java", mapper(table));
        files.put(basePath + "/service/" + className + "Service.java", service(table, columns));
        files.put(basePath + "/controller/" + className + "Controller.java", controller(table, columns));
        files.put("vue/api/" + table.getBusinessName() + ".ts", api(table));
        files.put("vue/views/" + table.getModuleName() + "/" + table.getBusinessName() + "/index.vue", vue(table, columns));
        files.put("sql/" + table.getBusinessName() + "_menu.sql", menuSql(table));
        return files;
    }

    private static String entity(GenTable table, List<GenTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append(javaHeader(table.getFunctionName() + " 实体类（代码生成）"));
        sb.append("package ").append(table.getPackageName()).append(".entity;\n\n");
        sb.append("import com.mybatisflex.annotation.Id;\n");
        sb.append("import com.mybatisflex.annotation.KeyType;\n");
        sb.append("import com.mybatisflex.annotation.Table;\n");
        sb.append("import lombok.Data;\n\n");
        sb.append("import java.io.Serializable;\n");
        if (columns.stream().anyMatch(c -> "BigDecimal".equals(c.getJavaType()))) {
            sb.append("import java.math.BigDecimal;\n");
        }
        if (columns.stream().anyMatch(c -> "LocalDateTime".equals(c.getJavaType()))) {
            sb.append("import java.time.LocalDateTime;\n");
        }
        if (columns.stream().anyMatch(c -> "LocalDate".equals(c.getJavaType()))) {
            sb.append("import java.time.LocalDate;\n");
        }
        if (columns.stream().anyMatch(c -> "LocalTime".equals(c.getJavaType()))) {
            sb.append("import java.time.LocalTime;\n");
        }
        sb.append("\n/**\n * ").append(table.getTableComment()).append(" 实体\n */\n");
        sb.append("@Data\n@Table(\"").append(table.getTableName()).append("\")\n");
        sb.append("public class ").append(table.getClassName()).append(" implements Serializable {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");
        for (GenTableColumn c : columns) {
            if (StrUtil.isNotBlank(c.getColumnComment())) {
                sb.append("    /** ").append(c.getColumnComment()).append(" */\n");
            }
            if ("1".equals(c.getIsPk())) {
                sb.append("    @Id(keyType = KeyType.Auto)\n");
            }
            sb.append("    private ").append(c.getJavaType()).append(" ").append(c.getJavaField()).append(";\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String mapper(GenTable table) {
        return javaHeader(table.getFunctionName() + " 数据访问接口（代码生成）")
                + "package " + table.getPackageName() + ".mapper;\n\n"
                + "import com.mybatisflex.core.BaseMapper;\n"
                + "import org.apache.ibatis.annotations.Mapper;\n"
                + "import " + table.getPackageName() + ".entity." + table.getClassName() + ";\n\n"
                + "@Mapper\n"
                + "public interface " + table.getClassName() + "Mapper extends BaseMapper<" + table.getClassName() + "> {\n"
                + "}\n";
    }

    private static String service(GenTable table, List<GenTableColumn> columns) {
        String cls = table.getClassName();
        GenTableColumn pk = columns.stream().filter(c -> "1".equals(c.getIsPk())).findFirst().orElse(columns.get(0));
        StringBuilder sb = new StringBuilder();
        sb.append(javaHeader(table.getFunctionName() + " 业务服务（代码生成）"));
        sb.append("package ").append(table.getPackageName()).append(".service;\n\n");
        sb.append("import java.util.List;\n");
        sb.append("import com.mybatisflex.core.paginate.Page;\n");
        sb.append("import com.mybatisflex.core.query.QueryWrapper;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.yeharn.lp.common.core.PageResult;\n");
        sb.append("import org.yeharn.lp.common.core.PageQuery;\n");
        sb.append("import ").append(table.getPackageName()).append(".entity.").append(cls).append(";\n");
        sb.append("import ").append(table.getPackageName()).append(".mapper.").append(cls).append("Mapper;\n\n");
        sb.append("/**\n * ").append(table.getFunctionName()).append(" 服务\n */\n");
        sb.append("@Service\n@RequiredArgsConstructor\n");
        sb.append("public class ").append(cls).append("Service {\n\n");
        sb.append("    private final ").append(cls).append("Mapper mapper;\n\n");
        sb.append("    public PageResult<").append(cls).append("> page(PageQuery query) {\n");
        sb.append("        QueryWrapper qw = QueryWrapper.create();\n");
        sb.append("        qw.orderBy(\"").append(pk.getColumnName()).append("\", false);\n");
        sb.append("        Page<").append(cls).append("> page = mapper.paginate(Page.of(query.getPageNumSafe(), query.getPageSizeSafe()), qw);\n");
        sb.append("        return new PageResult<>(page.getTotalRow(), page.getRecords(), query.getPageNumSafe(), query.getPageSizeSafe());\n");
        sb.append("    }\n\n");
        sb.append("    public ").append(cls).append(" getById(").append(pk.getJavaType()).append(" id) {\n");
        sb.append("        return mapper.selectOneById(id);\n    }\n\n");
        sb.append("    public void save(").append(cls).append(" entity) {\n        mapper.insertSelective(entity);\n    }\n\n");
        sb.append("    public void update(").append(cls).append(" entity) {\n        mapper.update(entity);\n    }\n\n");
        sb.append("    public void delete(List<").append(pk.getJavaType()).append("> ids) {\n        ids.forEach(mapper::deleteById);\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String controller(GenTable table, List<GenTableColumn> columns) {
        String cls = table.getClassName();
        String perm = table.getModuleName() + ":" + table.getBusinessName();
        GenTableColumn pk = columns.stream().filter(c -> "1".equals(c.getIsPk())).findFirst().orElse(columns.get(0));
        return javaHeader(table.getFunctionName() + " 接口控制器（代码生成）")
                + "package " + table.getPackageName() + ".controller;\n\n"
                + "import cn.dev33.satoken.annotation.SaCheckPermission;\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import org.yeharn.lp.common.core.PageQuery;\n"
                + "import org.yeharn.lp.common.core.PageResult;\n"
                + "import org.yeharn.lp.common.core.R;\n"
                + "import org.yeharn.lp.common.log.BusinessType;\n"
                + "import org.yeharn.lp.common.log.OperLog;\n"
                + "import " + table.getPackageName() + ".entity." + cls + ";\n"
                + "import " + table.getPackageName() + ".service." + cls + "Service;\n\n"
                + "import java.util.List;\n\n"
                + "/**\n * " + table.getFunctionName() + " 接口\n */\n"
                + "@RestController\n"
                + "@RequestMapping(\"/" + table.getModuleName() + "/" + table.getBusinessName() + "\")\n"
                + "@RequiredArgsConstructor\n"
                + "public class " + cls + "Controller {\n\n"
                + "    private final " + cls + "Service service;\n\n"
                + "    @SaCheckPermission(\"" + perm + ":list\")\n"
                + "    @GetMapping(\"/list\")\n"
                + "    public R<PageResult<" + cls + ">> list(PageQuery query) {\n"
                + "        return R.ok(service.page(query));\n    }\n\n"
                + "    @SaCheckPermission(\"" + perm + ":query\")\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public R<" + cls + "> getInfo(@PathVariable " + pk.getJavaType() + " id) {\n"
                + "        return R.ok(service.getById(id));\n    }\n\n"
                + "    @SaCheckPermission(\"" + perm + ":add\")\n"
                + "    @OperLog(title = \"" + table.getFunctionName() + "\", businessType = BusinessType.INSERT)\n"
                + "    @PostMapping\n"
                + "    public R<Void> add(@RequestBody " + cls + " entity) {\n"
                + "        service.save(entity);\n        return R.ok();\n    }\n\n"
                + "    @SaCheckPermission(\"" + perm + ":edit\")\n"
                + "    @OperLog(title = \"" + table.getFunctionName() + "\", businessType = BusinessType.UPDATE)\n"
                + "    @PutMapping\n"
                + "    public R<Void> edit(@RequestBody " + cls + " entity) {\n"
                + "        service.update(entity);\n        return R.ok();\n    }\n\n"
                + "    @SaCheckPermission(\"" + perm + ":remove\")\n"
                + "    @OperLog(title = \"" + table.getFunctionName() + "\", businessType = BusinessType.DELETE)\n"
                + "    @DeleteMapping(\"/{ids}\")\n"
                + "    public R<Void> remove(@PathVariable List<" + pk.getJavaType() + "> ids) {\n"
                + "        service.delete(ids);\n        return R.ok();\n    }\n"
                + "}\n";
    }

    private static String api(GenTable table) {
        String cls = table.getClassName();
        String url = "/" + table.getModuleName() + "/" + table.getBusinessName();
        return tsHeader(table.getFunctionName() + " 接口定义（代码生成）")
                + "import request from '@/utils/request'\nimport type { PageResult } from '@/types'\n\n"
                + "export interface " + cls + " {\n  [key: string]: any\n}\n\n"
                + "export function list" + cls + "(params: any) {\n"
                + "  return request.get<PageResult<" + cls + ">>('" + url + "/list', { params })\n}\n\n"
                + "export function get" + cls + "(id: string | number) {\n"
                + "  return request.get<" + cls + ">('" + url + "/' + id)\n}\n\n"
                + "export function add" + cls + "(data: " + cls + ") {\n"
                + "  return request.post('" + url + "', data)\n}\n\n"
                + "export function update" + cls + "(data: " + cls + ") {\n"
                + "  return request.put('" + url + "', data)\n}\n\n"
                + "export function del" + cls + "(ids: Array<string | number>) {\n"
                + "  return request.delete('" + url + "/' + ids.join(','))\n}\n";
    }

    private static String vue(GenTable table, List<GenTableColumn> columns) {
        String cls = table.getClassName();
        String title = table.getFunctionName();
        StringBuilder search = new StringBuilder();
        StringBuilder tableCols = new StringBuilder();
        StringBuilder form = new StringBuilder();
        for (GenTableColumn c : columns) {
            if ("1".equals(c.getIsQuery())) {
                search.append("        <el-form-item label=\"").append(c.getColumnComment()).append("\">\n")
                        .append("          <el-input v-model=\"query.").append(c.getJavaField())
                        .append("\" placeholder=\"请输入").append(c.getColumnComment()).append("\" clearable @keyup.enter=\"getList\" />\n")
                        .append("        </el-form-item>\n");
            }
            if ("1".equals(c.getIsList())) {
                tableCols.append("      <el-table-column label=\"").append(c.getColumnComment())
                        .append("\" prop=\"").append(c.getJavaField()).append("\" show-overflow-tooltip />\n");
            }
            if ("1".equals(c.getIsEdit())) {
                form.append("        <el-form-item label=\"").append(c.getColumnComment()).append("\" prop=\"").append(c.getJavaField()).append("\">\n");
                if ("datetime".equals(c.getHtmlType())) {
                    form.append("          <el-date-picker v-model=\"form.").append(c.getJavaField())
                            .append("\" type=\"datetime\" value-format=\"YYYY-MM-DD HH:mm:ss\" placeholder=\"请选择\" style=\"width: 100%\" />\n");
                } else if ("textarea".equals(c.getHtmlType())) {
                    form.append("          <el-input v-model=\"form.").append(c.getJavaField()).append("\" type=\"textarea\" :rows=\"3\" />\n");
                } else {
                    form.append("          <el-input v-model=\"form.").append(c.getJavaField()).append("\" placeholder=\"请输入\" />\n");
                }
                form.append("        </el-form-item>\n");
            }
        }
        String pkField = columns.stream().filter(c -> "1".equals(c.getIsPk()))
                .map(GenTableColumn::getJavaField).findFirst().orElse("id");
        String apiName = table.getBusinessName();
        return vueHeader(table.getFunctionName() + " 列表页（代码生成）")
                + "<template>\n"
                + "  <div class=\"app-container\">\n"
                + "    <el-form :inline=\"true\" :model=\"query\" class=\"search-form\">\n"
                + search
                + "      <el-form-item>\n"
                + "        <el-button type=\"primary\" icon=\"Search\" @click=\"getList\">搜索</el-button>\n"
                + "        <el-button icon=\"Refresh\" @click=\"resetQuery\">重置</el-button>\n"
                + "      </el-form-item>\n"
                + "    </el-form>\n\n"
                + "    <el-row :gutter=\"10\" class=\"mb8\">\n"
                + "      <el-col :span=\"24\"><el-button type=\"primary\" plain icon=\"Plus\" @click=\"handleAdd\">新增</el-button></el-col>\n"
                + "    </el-row>\n\n"
                + "    <el-table v-loading=\"loading\" :data=\"list\" border>\n"
                + tableCols
                + "      <el-table-column label=\"操作\" width=\"150\" fixed=\"right\">\n"
                + "        <template #default=\"scope\">\n"
                + "          <el-button link type=\"primary\" @click=\"handleUpdate(scope.row)\">修改</el-button>\n"
                + "          <el-button link type=\"danger\" @click=\"handleDelete(scope.row)\">删除</el-button>\n"
                + "        </template>\n"
                + "      </el-table-column>\n"
                + "    </el-table>\n\n"
                + "    <el-pagination v-model:current-page=\"query.pageNum\" v-model:page-size=\"query.pageSize\"\n"
                + "      :total=\"total\" :page-sizes=\"[10, 20, 50]\" layout=\"total, sizes, prev, pager, next\"\n"
                + "      class=\"pagination\" @size-change=\"getList\" @current-change=\"getList\" />\n\n"
                + "    <el-dialog v-model=\"open\" :title=\"form." + pkField + " ? '修改" + title + "' : '新增" + title + "'\" width=\"600px\" append-to-body>\n"
                + "      <el-form ref=\"formRef\" :model=\"form\" :rules=\"rules\" label-width=\"100px\">\n"
                + form
                + "      </el-form>\n"
                + "      <template #footer>\n"
                + "        <el-button @click=\"open = false\">取消</el-button>\n"
                + "        <el-button type=\"primary\" @click=\"submitForm\">确定</el-button>\n"
                + "      </template>\n"
                + "    </el-dialog>\n"
                + "  </div>\n"
                + "</template>\n\n"
                + "<script setup lang=\"ts\">\n"
                + "import { ref, reactive, onMounted } from 'vue'\n"
                + "import { ElMessage, ElMessageBox } from 'element-plus'\n"
                + "import { list" + cls + ", get" + cls + ", add" + cls + ", update" + cls + ", del" + cls + " } from '@/api/" + apiName + "'\n\n"
                + "const loading = ref(false)\nconst list = ref<any[]>([])\nconst total = ref(0)\nconst open = ref(false)\nconst formRef = ref()\n"
                + "const query = reactive({ pageNum: 1, pageSize: 10 })\nconst form = ref<any>({})\n"
                + "const rules = {}\n\n"
                + "const getList = async () => {\n"
                + "  loading.value = true\n  try {\n"
                + "    const res = await list" + cls + "(query)\n"
                + "    list.value = res.rows\n    total.value = res.total\n"
                + "  } finally { loading.value = false }\n}\n\n"
                + "const resetQuery = () => { Object.keys(query).forEach(k => { if (k !== 'pageNum' && k !== 'pageSize') (query as any)[k] = undefined }); getList() }\n"
                + "const handleAdd = () => { form.value = {}; open.value = true }\n"
                + "const handleUpdate = async (row: any) => { form.value = await get" + cls + "(row." + pkField + "); open.value = true }\n"
                + "const handleDelete = async (row: any) => {\n"
                + "  await ElMessageBox.confirm('确认删除该数据?', '提示', { type: 'warning' })\n"
                + "  await del" + cls + "([row." + pkField + "])\n"
                + "  ElMessage.success('删除成功')\n  getList()\n}\n"
                + "const submitForm = async () => {\n"
                + "  await formRef.value?.validate()\n"
                + "  if (form.value." + pkField + ") { await update" + cls + "(form.value) } else { await add" + cls + "(form.value) }\n"
                + "  ElMessage.success('保存成功')\n  open.value = false\n  getList()\n}\n\n"
                + "onMounted(getList)\n"
                + "</script>\n\n"
                + "<style scoped>\n.pagination { margin-top: 16px; justify-content: flex-end; }\n</style>\n";
    }

    private static String menuSql(GenTable table) {
        String perm = table.getModuleName() + ":" + table.getBusinessName();
        return sqlHeader(table.getFunctionName() + " 菜单脚本（代码生成）")
                + "-- 菜单 SQL（父级菜单ID 请按实际情况调整）\n"
                + "INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)\n"
                + "VALUES ('" + table.getFunctionName() + "', 0, 10, '" + table.getBusinessName() + "', '"
                + table.getModuleName() + "/" + table.getBusinessName() + "/index', 'C', '0', '0', '" + perm + ":list', 'Document', 'admin', NOW());\n"
                + "-- 按钮权限\n"
                + "INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)\n"
                + "SELECT '查询', menu_id, 1, '', NULL, 'F', '0', '0', '" + perm + ":query', 'admin', NOW() FROM sys_menu WHERE perms = '" + perm + ":list';\n";
    }
}
