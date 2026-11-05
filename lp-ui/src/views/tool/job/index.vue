<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统工具 - 定时任务页面
-->

<template>
  <div class="app-container">
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="query.jobName" placeholder="任务名称" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" icon="Search" @click="load">搜索</el-button>
        <el-button type="success" icon="Plus" v-hasPerm="['tool:job:add']" @click="openAdd">新增任务</el-button>
      </div>

      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        调度器：{{ info.scheduler || '内置 ThreadPoolTaskScheduler' }}；
        XXL-JOB 执行器：{{ info.xxlJobEnabled ? '已启用' : '未启用（部署 xxl-job-admin 后设置 lp.xxl-job.enabled=true）' }}
      </el-alert>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="jobId" label="ID" width="60" />
        <el-table-column prop="jobName" label="任务名称" min-width="160" />
        <el-table-column prop="jobBean" label="处理器" min-width="160" />
        <el-table-column prop="cronExpression" label="cron" width="140" />
        <el-table-column prop="jobParam" label="参数" min-width="100" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === '0'" v-hasPerm="['tool:job:changeStatus']"
              @change="(v: any) => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="VideoPlay" v-hasPerm="['tool:job:run']" @click="run(row)">执行一次</el-button>
            <el-button link type="primary" icon="Edit" v-hasPerm="['tool:job:edit']" @click="openEdit(row)">修改</el-button>
            <el-button link type="danger" icon="Delete" v-hasPerm="['tool:job:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.jobId ? '修改任务' : '新增任务'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="任务名称"><el-input v-model="form.jobName" /></el-form-item>
        <el-form-item label="处理器">
          <el-select v-model="form.jobBean" style="width: 100%">
            <el-option v-for="h in info.handlers || []" :key="h" :label="h" :value="h" />
          </el-select>
        </el-form-item>
        <el-form-item label="cron"><el-input v-model="form.cronExpression" placeholder="0 0 1 25 * ?" /></el-form-item>
        <el-form-item label="参数"><el-input v-model="form.jobParam" placeholder="可选" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addJob, changeJobStatus, delJob, jobInfo, listJob, runJob, updateJob } from '@/api/extras'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const info = ref<any>({})
const dialogVisible = ref(false)
const query = reactive({ jobName: '', pageNum: 1, pageSize: 10 })
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const res: any = await listJob(query)
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadInfo() {
  info.value = await jobInfo()
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { jobName: '', jobBean: '', cronExpression: '', jobParam: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  if (form.jobId) {
    await updateJob(form)
  } else {
    await addJob(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggle(row: any, value: boolean) {
  await changeJobStatus({ jobId: row.jobId, status: value ? '0' : '1' })
  ElMessage.success(value ? '已启用' : '已暂停')
  load()
}

async function run(row: any) {
  await runJob(row.jobId)
  ElMessage.success('已提交执行，请查看日志')
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除任务「${row.jobName}」？`, '提示', { type: 'warning' })
  await delJob([row.jobId])
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadInfo()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
</style>
