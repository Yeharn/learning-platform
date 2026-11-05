<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统工具 - 代码生成页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="表名称">
        <el-input v-model="query.tableName" placeholder="请输入表名" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="表描述">
        <el-input v-model="query.tableComment" placeholder="请输入表描述" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button v-hasPerm="'tool:gen:import'" type="primary" icon="Download" @click="openImport">导入表</el-button>
        <el-button
          v-hasPerm="'tool:gen:remove'"
          type="danger"
          plain
          icon="Delete"
          :disabled="!selection.length"
          @click="handleBatchDelete"
        >
          删除
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border @selection-change="(rows: any[]) => (selection = rows)">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="tableName" label="表名称" min-width="160" />
        <el-table-column prop="tableComment" label="表描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="实体类" width="150" />
        <el-table-column prop="moduleName" label="模块" width="100" />
        <el-table-column prop="businessName" label="业务名" width="120" />
        <el-table-column prop="dsKey" label="数据源" width="110" />
        <el-table-column prop="createTime" label="导入时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'tool:gen:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-hasPerm="'tool:gen:preview'" link type="success" @click="handlePreview(row)">预览</el-button>
            <el-button v-hasPerm="'tool:gen:download'" link type="warning" @click="handleDownload(row)">下载</el-button>
            <el-button v-hasPerm="'tool:gen:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <!-- 导入表 -->
    <el-dialog v-model="importOpen" title="导入数据库表" width="820px">
      <div class="mb8 import-bar">
        <el-select v-model="dsKey" style="width: 220px" placeholder="选择数据源" @change="loadDbTables">
          <el-option v-for="ds in datasources" :key="ds.dsKey" :label="`${ds.name} (${ds.dsKey})`" :value="ds.dsKey" />
        </el-select>
        <el-input v-model="tableFilter" placeholder="搜索表名" clearable style="width: 220px" />
        <span class="text-muted">共 {{ filteredTables.length }} 张表</span>
      </div>

      <el-table
        v-loading="dbLoading"
        :data="filteredTables"
        height="420"
        border
        @selection-change="(rows: any[]) => (dbSelection = rows)"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="tableName" label="表名" min-width="220" />
        <el-table-column prop="tableComment" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>

      <template #footer>
        <el-button @click="importOpen = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- 编辑生成配置 -->
    <el-dialog v-model="editOpen" title="编辑生成配置" width="1000px" top="5vh">
      <el-tabs v-model="editTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="genForm" label-width="100px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="表名称">
                  <el-input v-model="genForm.tableName" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="表描述">
                  <el-input v-model="genForm.tableComment" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实体类名">
                  <el-input v-model="genForm.className" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="作者">
                  <el-input v-model="genForm.functionAuthor" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="包路径">
                  <el-input v-model="genForm.packageName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模块名">
                  <el-input v-model="genForm.moduleName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="业务名">
                  <el-input v-model="genForm.businessName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="功能名">
                  <el-input v-model="genForm.functionName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模板类型">
                  <el-select v-model="genForm.tplCategory" style="width: 100%">
                    <el-option label="单表 CRUD" value="crud" />
                    <el-option label="树表" value="tree" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="数据源">
                  <el-input v-model="genForm.dsKey" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注">
                  <el-input v-model="genForm.remark" type="textarea" :rows="2" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="字段配置" name="columns">
          <el-table :data="genForm.columns || []" border size="small" max-height="520">
            <el-table-column prop="columnName" label="列名" width="150" />
            <el-table-column prop="columnComment" label="描述" width="150">
              <template #default="{ row }"><el-input v-model="row.columnComment" size="small" /></template>
            </el-table-column>
            <el-table-column prop="javaType" label="Java类型" width="130">
              <template #default="{ row }">
                <el-select v-model="row.javaType" size="small">
                  <el-option v-for="t in javaTypes" :key="t" :label="t" :value="t" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="javaField" label="属性名" width="130">
              <template #default="{ row }"><el-input v-model="row.javaField" size="small" /></template>
            </el-table-column>
            <el-table-column label="插入" width="60" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isInsert" true-value="1" false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="编辑" width="60" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isEdit" true-value="1" false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="列表" width="60" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isList" true-value="1" false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="查询" width="60" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isQuery" true-value="1" false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="查询方式" width="110">
              <template #default="{ row }">
                <el-select v-model="row.queryType" size="small">
                  <el-option label="EQ" value="EQ" />
                  <el-option label="LIKE" value="LIKE" />
                  <el-option label="GT" value="GT" />
                  <el-option label="LT" value="LT" />
                  <el-option label="BETWEEN" value="BETWEEN" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="显示类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.htmlType" size="small">
                  <el-option label="input" value="input" />
                  <el-option label="textarea" value="textarea" />
                  <el-option label="select" value="select" />
                  <el-option label="radio" value="radio" />
                  <el-option label="datetime" value="datetime" />
                  <el-option label="image" value="image" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="dictType" label="字典类型" width="130">
              <template #default="{ row }"><el-input v-model="row.dictType" size="small" /></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewOpen" title="代码预览" width="1080px" top="5vh">
      <div class="preview">
        <div class="file-list">
          <div
            v-for="(_, file) in previewData"
            :key="file"
            class="file-item"
            :class="{ active: currentFile === file }"
            @click="currentFile = file"
          >
            <el-icon><Document /></el-icon>
            <span>{{ file }}</span>
          </div>
        </div>
        <div class="code-area">
          <el-scrollbar height="62vh">
            <pre class="code-block">{{ previewData[currentFile] || '请选择文件' }}</pre>
          </el-scrollbar>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  delGenTable,
  downloadGen,
  getGenTable,
  importTables,
  listDbTables,
  listGenTable,
  previewGen,
  updateGenTable,
  type GenTable
} from '@/api/gen'
import { listDatasource } from '@/api/system'
import type { SysDatasource } from '@/types'

const loading = ref(false)
const dbLoading = ref(false)
const importing = ref(false)
const saving = ref(false)
const importOpen = ref(false)
const editOpen = ref(false)
const previewOpen = ref(false)
const editTab = ref('basic')
const list = ref<GenTable[]>([])
const total = ref(0)
const selection = ref<any[]>([])
const dbSelection = ref<any[]>([])
const dbTables = ref<any[]>([])
const datasources = ref<SysDatasource[]>([])
const dsKey = ref('master')
const tableFilter = ref('')
const genForm = ref<GenTable>({})
const previewData = ref<Record<string, string>>({})
const currentFile = ref('')

const query = reactive({ pageNum: 1, pageSize: 10, tableName: '', tableComment: '' })
const javaTypes = ['String', 'Integer', 'Long', 'Double', 'BigDecimal', 'LocalDateTime', 'LocalDate', 'Boolean']

const filteredTables = computed(() =>
  dbTables.value.filter((t) => !tableFilter.value || String(t.tableName).includes(tableFilter.value))
)

async function getList() {
  loading.value = true
  try {
    const res = await listGenTable(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.tableName = ''
  query.tableComment = ''
  query.pageNum = 1
  getList()
}

async function openImport() {
  importOpen.value = true
  if (!datasources.value.length) {
    datasources.value = await listDatasource()
  }
  await loadDbTables()
}

async function loadDbTables() {
  dbLoading.value = true
  try {
    dbTables.value = await listDbTables(dsKey.value)
  } finally {
    dbLoading.value = false
  }
}

async function handleImport() {
  if (!dbSelection.value.length) {
    ElMessage.warning('请选择要导入的表')
    return
  }
  importing.value = true
  try {
    await importTables(
      dbSelection.value.map((t) => t.tableName),
      dsKey.value
    )
    ElMessage.success('导入成功')
    importOpen.value = false
    getList()
  } finally {
    importing.value = false
  }
}

async function openEdit(row: GenTable) {
  genForm.value = await getGenTable(row.tableId)
  editTab.value = 'basic'
  editOpen.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await updateGenTable(genForm.value)
    ElMessage.success('保存成功')
    editOpen.value = false
    getList()
  } finally {
    saving.value = false
  }
}

async function handlePreview(row: GenTable) {
  previewData.value = await previewGen(row.tableId)
  currentFile.value = Object.keys(previewData.value)[0] || ''
  previewOpen.value = true
}

async function handleDownload(row: GenTable) {
  const blob = await downloadGen(row.tableId)
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.className || 'code'}.zip`
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleDelete(row: GenTable) {
  await ElMessageBox.confirm(`确认删除表【${row.tableName}】的生成配置?`, '提示', { type: 'warning' })
  await delGenTable([row.tableId as any])
  ElMessage.success('删除成功')
  getList()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 项配置?`, '提示', { type: 'warning' })
  await delGenTable(selection.value.map((i) => i.tableId))
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>

<style scoped lang="scss">
.import-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview {
  display: flex;
  gap: 12px;
  height: 62vh;
}

.file-list {
  width: 300px;
  flex-shrink: 0;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  overflow: auto;
  padding: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  color: #4b5563;
  word-break: break-all;

  &:hover {
    background: #f8fafc;
  }

  &.active {
    background: #eef2ff;
    color: #4f46e5;
  }
}

.code-area {
  flex: 1;
  min-width: 0;
}
</style>
