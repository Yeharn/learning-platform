<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - 操作日志页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="模块">
        <el-input v-model="query.title" placeholder="请输入模块标题" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="请输入操作人" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.businessType" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="(label, value) in businessTypes" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="异常" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="月份">
        <el-select v-model="query.month" style="width: 130px" @change="getList">
          <el-option v-for="m in months" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 340px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button
          v-hasPerm="'monitor:operlog:remove'"
          type="danger"
          plain
          icon="Delete"
          :disabled="!selection.length"
          @click="handleDelete"
        >
          删除
        </el-button>
        <el-button v-hasPerm="'monitor:operlog:remove'" type="danger" plain icon="Delete" @click="handleClean">
          清空
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border @selection-change="(rows: any[]) => (selection = rows)">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="operId" label="ID" width="80" />
        <el-table-column prop="title" label="模块" width="140" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ businessTypes[row.businessType] || '其它' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column prop="requestMethod" label="方式" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="operTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-hasPerm="'monitor:operlog:remove'" link type="danger" @click="handleDeleteOne(row)">
              删除
            </el-button>
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

    <el-drawer v-model="drawer" title="操作日志详情" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ detail.requestMethod }} {{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item label="参数">
          <pre class="pre">{{ detail.operParam }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="异常">
          <pre class="pre error">{{ detail.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanOperLog, delOperLog, getOperLog, listOperLog } from '@/api/system'
import { logMonths } from '@/api/extras'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const selection = ref<any[]>([])
const drawer = ref(false)
const detail = ref<any>({})
const dateRange = ref<string[]>([])
const months = ref<string[]>([])

const businessTypes: Record<number, string> = {
  0: '其它',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '授权',
  5: '导出',
  6: '导入',
  7: '强退',
  8: '生成代码',
  9: '清空'
}

const query = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  title: '',
  username: '',
  status: '',
  businessType: undefined,
  month: ''
})

watch(dateRange, (val) => {
  query.beginTime = val?.[0] || ''
  query.endTime = val?.[1] || ''
})

async function getList() {
  loading.value = true
  try {
    const res = await listOperLog(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.title = ''
  query.username = ''
  query.status = ''
  query.businessType = undefined
  query.pageNum = 1
  dateRange.value = []
  getList()
}

async function handleView(row: any) {
  detail.value = await getOperLog(row.operId, query.month)
  drawer.value = true
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 条日志?`, '提示', { type: 'warning' })
  await delOperLog(selection.value.map((i) => i.operId), query.month)
  ElMessage.success('删除成功')
  getList()
}

async function handleDeleteOne(row: any) {
  await delOperLog([row.operId], query.month)
  ElMessage.success('删除成功')
  getList()
}

async function handleClean() {
  await ElMessageBox.confirm(`确认清空 ${query.month || '当前月'} 的操作日志?`, '提示', { type: 'warning' })
  await cleanOperLog(query.month)
  ElMessage.success('已清空')
  getList()
}

async function loadMonths() {
  months.value = (await logMonths('oper')) as string[]
  if (!query.month && months.value.length) {
    query.month = months.value[0]
  }
}

onMounted(() => {
  loadMonths()
  getList()
})
</script>

<style scoped>
.pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  margin: 0;
  max-height: 220px;
  overflow: auto;
}
.error {
  color: #dc2626;
}
</style>
