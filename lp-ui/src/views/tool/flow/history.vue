<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 流程历史：已结束实例分页查询、流程图与审批记录回溯
-->

<template>
  <div class="app-container">
    <el-form :inline="true" class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="流程名称 / 实例号 / 发起人" clearable style="width: 260px"
          @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="实例号" min-width="180">
          <template #default="{ row }">{{ row.instanceNo || row.id }}</template>
        </el-table-column>
        <el-table-column prop="processName" label="流程名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="currentNodeName" label="结束节点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="createBy" label="发起人" width="110" />
        <el-table-column prop="createTime" label="发起时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ durationText(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.state === 3 ? 'danger' : row.state === 4 ? 'info' : 'success'" size="small">
              {{ stateText(row.state) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="handleSizeChange" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="流程历史详情" size="680px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="流程名称">{{ detail.instance?.processName }}</el-descriptions-item>
          <el-descriptions-item label="实例号">{{ detail.instance?.instanceNo || detail.instance?.id }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.instance?.createBy }}</el-descriptions-item>
          <el-descriptions-item label="结束节点">{{ detail.instance?.currentNodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.instance?.createTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.instance?.endTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="formEntries.length" class="detail-section">
          <div class="section-title">表单信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-for="item in formEntries" :key="item.key" :label="item.label">
              <template v-if="item.type === 'files'">
                <div v-for="(file, fi) in item.value" :key="fi">
                  <el-link type="primary" :href="file.url" target="_blank">{{ file.name }}</el-link>
                </div>
              </template>
              <template v-else-if="Array.isArray(item.value)">{{ item.value.join('、') }}</template>
              <template v-else>{{ item.value }}</template>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="section-title">流程图</div>
          <FlowViewer :nodes="detail.nodes || []" :current-key="detail.instance?.currentNodeKey" />
        </div>

        <div class="detail-section">
          <div class="section-title">审批记录</div>
          <el-timeline v-if="detail.records && detail.records.length">
            <el-timeline-item v-for="item in detail.records" :key="item.id" :timestamp="item.finishTime || item.createTime">
              <div class="record-title">
                {{ item.name }}
                <el-tag size="small" effect="plain">{{ item.actors || item.createBy }}</el-tag>
              </div>
              <div v-if="item.comment" class="record-comment">{{ item.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </div>

        <div v-if="(detail.comments || []).length" class="detail-section">
          <div class="section-title">沟通</div>
          <div v-for="item in detail.comments" :key="item.id" class="comment-item">
            <span class="comment-user">{{ item.userName }}</span>
            <span class="comment-time">{{ item.createTime }}</span>
            <div class="comment-content">{{ item.content }}</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import FlowViewer from '@/components/FlowViewer/index.vue'
import { flowInstanceDetail, flowInstancePage } from '@/api/flow'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ keyword: '', state: 'finished', pageNum: 1, pageSize: 10 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({ instance: null, nodes: [], records: [], comments: [], form: null })

async function load() {
  loading.value = true
  try {
    const res: any = await flowInstancePage({ ...query, state: 'finished' })
    list.value = res.rows || []
    total.value = Number(res.total) || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleSizeChange() {
  query.pageNum = 1
  load()
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = (await flowInstanceDetail(row.id)) as any
  } finally {
    detailLoading.value = false
  }
}

const formEntries = computed(() => {
  const fields: any[] = detail.value.form?.fields || []
  const vars: Record<string, any> = detail.value.instance?.variables || {}
  if (!fields.length) {
    return Object.entries(vars)
      .filter(([key, value]) => key !== 'approveComment' && typeof value !== 'object')
      .map(([key, value]) => ({ key, label: key, value, type: 'text' }))
  }
  return fields
    .filter((field: any) => field.type !== 'desc')
    .map((field: any) => ({
      key: field.key,
      label: field.label,
      value: vars[field.key],
      type: field.type === 'attachment' ? 'files' : 'text'
    }))
    .filter((item: any) => item.value !== undefined && item.value !== null && item.value !== '')
})

function durationText(duration: any) {
  const value = Number(duration)
  if (!value) return '-'
  if (value < 1000) return value + ' 毫秒'
  if (value < 60000) return Math.round(value / 1000) + ' 秒'
  if (value < 3600000) return Math.round(value / 60000) + ' 分钟'
  return (value / 3600000).toFixed(1) + ' 小时'
}

function stateText(state: number) {
  const map: Record<number, string> = { 2: '已完成', 3: '已驳回', 4: '已撤销', 5: '已超时', 7: '已终止' }
  return map[state] || '已结束'
}

onMounted(load)
</script>

<style scoped>
.detail-section {
  margin-top: 18px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.record-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.record-comment {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.comment-item {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  margin-bottom: 8px;
}

.comment-user {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.comment-time {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.comment-content {
  margin-top: 4px;
  font-size: 13px;
}
</style>
