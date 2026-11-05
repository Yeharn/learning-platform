<!--
  @Author: Yeharn
  @Date:2026-11-05
  @Version: 1.0.0
  @Description: 系统工具 - 定时任务运行批次日志
-->

<template>
  <div class="app-container">
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-button icon="ArrowLeft" @click="goBack">返回任务列表</el-button>
        <el-tag v-if="filter.jobName" type="info" effect="plain" closable @close="clearJob">
          任务：{{ filter.jobName }}
        </el-tag>
        <el-select v-model="query.status" placeholder="运行状态" clearable style="width: 130px" @change="reload">
          <el-option label="成功" value="0" />
          <el-option label="失败" value="1" />
        </el-select>
        <el-button type="primary" icon="Search" @click="reload">查询</el-button>
        <el-button icon="Refresh" @click="reload">刷新</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" @row-dblclick="openDetail">
        <el-table-column prop="logId" label="批次号" width="90" class-name="mono" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" />
        <el-table-column prop="jobBean" label="处理器" min-width="170" />
        <el-table-column label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.triggerType === '1' ? 'warning' : 'info'" effect="plain">
              {{ row.triggerType === '1' ? '手动' : '自动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行状态" width="100">
          <template #default="{ row }">
            <span class="status-cell" :class="row.status === '0' ? 'ok' : 'fail'">
              <i class="led" />{{ row.status === '0' ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" class-name="mono" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">{{ formatCost(row.costTime) }}</template>
        </el-table-column>
        <el-table-column prop="jobParam" label="运行参数" min-width="120" show-overflow-tooltip />
        <el-table-column prop="triggerBy" label="触发人" width="100" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="运行批次详情" size="520px">
      <div v-if="detail" class="detail">
        <div class="detail-head">
          <span class="batch">BATCH #{{ detail.logId }}</span>
          <el-tag :type="detail.status === '0' ? 'success' : 'danger'" effect="dark" size="small">
            {{ detail.status === '0' ? '运行成功' : '运行失败' }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务名称">{{ detail.jobName }}</el-descriptions-item>
          <el-descriptions-item label="处理器">{{ detail.jobBean }}</el-descriptions-item>
          <el-descriptions-item label="cron">{{ detail.cronExpression }}</el-descriptions-item>
          <el-descriptions-item label="触发方式">
            {{ detail.triggerType === '1' ? '手动执行' : '自动调度' }}（{{ detail.triggerBy || 'system' }}）
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.endTime }}</el-descriptions-item>
          <el-descriptions-item label="运行耗时">{{ formatCost(detail.costTime) }}</el-descriptions-item>
          <el-descriptions-item label="运行参数">
            <span class="mono">{{ detail.jobParam || '—' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === '0'" class="result-block success">
          <div class="block-title">执行结果</div>
          <div class="block-body">{{ detail.result || '—' }}</div>
        </div>
        <div v-else class="result-block error">
          <div class="block-title">异常信息</div>
          <div class="block-body">{{ detail.errorMsg || '—' }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { jobLogDetail, listJobLog } from '@/api/extras'

const route = useRoute()
const router = useRouter()

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)
const detail = ref<any>(null)

const filter = reactive<{ jobId?: string; jobName?: string }>({
  jobId: route.query.jobId ? String(route.query.jobId) : undefined,
  jobName: route.query.jobName ? String(route.query.jobName) : undefined
})

const query = reactive({ status: '', pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const res: any = await listJobLog({ jobId: filter.jobId, status: query.status, pageNum: query.pageNum, pageSize: query.pageSize })
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function clearJob() {
  filter.jobId = undefined
  filter.jobName = undefined
  reload()
}

async function openDetail(row: any) {
  detail.value = await jobLogDetail(row.logId)
  drawerVisible.value = true
}

function formatCost(cost?: number | string) {
  const ms = Number(cost || 0)
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

function goBack() {
  router.push('/tool/job')
}

onMounted(load)
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;

  .led {
    width: 7px;
    height: 7px;
    border-radius: 50%;
  }

  &.ok {
    color: var(--lp-success);

    .led {
      background: var(--lp-success);
      box-shadow: 0 0 6px var(--lp-success);
    }
  }

  &.fail {
    color: var(--lp-danger);

    .led {
      background: var(--lp-danger);
      box-shadow: 0 0 6px var(--lp-danger);
    }
  }
}

:deep(.mono .cell) {
  font-family: Consolas, monospace;
  font-size: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;

  .batch {
    font-family: Consolas, monospace;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 1px;
    color: var(--lp-primary);
  }
}

.result-block {
  margin-top: 16px;
  border-radius: var(--lp-radius-sm);
  overflow: hidden;
  border: 1px solid transparent;

  .block-title {
    padding: 8px 12px;
    font-size: 13px;
    font-weight: 600;
  }

  .block-body {
    padding: 12px;
    font-family: Consolas, monospace;
    font-size: 12px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-all;
    background: color-mix(in srgb, var(--lp-bg-card) 88%, transparent);
  }

  &.success {
    border-color: color-mix(in srgb, var(--lp-success) 45%, transparent);

    .block-title {
      background: color-mix(in srgb, var(--lp-success) 14%, transparent);
      color: var(--lp-success);
    }
  }

  &.error {
    border-color: color-mix(in srgb, var(--lp-danger) 45%, transparent);

    .block-title {
      background: color-mix(in srgb, var(--lp-danger) 14%, transparent);
      color: var(--lp-danger);
    }
  }
}

.mono {
  font-family: Consolas, monospace;
  font-size: 12px;
}
</style>
