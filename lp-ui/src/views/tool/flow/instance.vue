<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 流程实例：分页查询、流程图查看、审批记录、沟通与实例管控
-->

<template>
  <div class="app-container">
    <el-form :inline="true" class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="流程名称 / 实例号 / 发起人" clearable style="width: 240px"
          @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.state" style="width: 140px" @change="handleSearch">
          <el-option label="运行中" value="running" />
          <el-option label="已结束" value="finished" />
        </el-select>
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
        <el-table-column prop="currentNodeName" label="当前节点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="createBy" label="发起人" width="110" />
        <el-table-column prop="createTime" label="发起时间" width="170" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.finished ? 'info' : 'success'" effect="light">
              {{ row.finished ? '已结束' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
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

    <el-drawer v-model="detailVisible" title="流程实例详情" size="720px" destroy-on-close>
      <div v-loading="detailLoading" class="instance-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="流程名称">{{ detail.instance?.processName }}</el-descriptions-item>
          <el-descriptions-item label="实例号">{{ detail.instance?.instanceNo || detail.instance?.id }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.instance?.createBy }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.instance?.currentNodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.instance?.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.instance?.finished ? 'info' : 'success'" size="small">
              {{ detail.instance?.finished ? '已结束' : '运行中' }}
            </el-tag>
          </el-descriptions-item>
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

        <div v-if="!detail.instance?.finished" class="instance-actions">
          <el-button size="small" icon="VideoPause" v-hasPerm="['tool:flow:execute']" @click="handleSuspend">挂起</el-button>
          <el-button size="small" icon="VideoPlay" v-hasPerm="['tool:flow:execute']" @click="handleActivate">激活</el-button>
          <el-button size="small" type="danger" plain icon="CircleClose" v-hasPerm="['tool:flow:execute']"
            @click="handleTerminate">终止</el-button>
        </div>

        <div class="detail-section">
          <div class="section-title">流程图</div>
          <FlowViewer :nodes="detail.nodes || []" :current-key="detail.instance?.currentNodeKey" />
        </div>

        <div class="detail-section">
          <div class="section-title">审批记录</div>
          <el-timeline v-if="detail.records && detail.records.length">
            <el-timeline-item v-for="item in detail.records" :key="item.id" :timestamp="item.finishTime || item.createTime"
              :type="stateType(item.state)">
              <div class="record-title">
                {{ item.name }}
                <el-tag size="small" effect="plain">{{ item.actors || item.createBy }}</el-tag>
                <el-tag size="small" :type="stateType(item.state)" effect="light">{{ stateText(item.state) }}</el-tag>
              </div>
              <div v-if="item.comment" class="record-comment">{{ item.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </div>

        <div class="detail-section">
          <div class="section-title">沟通</div>
          <div class="comment-list">
            <div v-for="item in detail.comments || []" :key="item.id" class="comment-item">
              <span class="comment-user">{{ item.userName }}</span>
              <span class="comment-time">{{ item.createTime }}</span>
              <div class="comment-content">{{ item.content }}</div>
            </div>
            <el-empty v-if="!(detail.comments || []).length" description="暂无沟通记录" :image-size="50" />
          </div>
          <div class="comment-input">
            <el-input v-model="commentText" placeholder="输入沟通内容，@相关同事参与讨论" @keyup.enter="handleComment" />
            <el-button type="primary" :loading="commentLoading" @click="handleComment">发送</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlowViewer from '@/components/FlowViewer/index.vue'
import {
  activateInstance,
  addFlowComment,
  flowInstanceDetail,
  flowInstancePage,
  suspendInstance,
  terminateInstance
} from '@/api/flow'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ keyword: '', state: 'running', pageNum: 1, pageSize: 10 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({ instance: null, nodes: [], records: [], comments: [], form: null })
const currentInstanceId = ref<string | number>('')
const commentText = ref('')
const commentLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await flowInstancePage({ ...query })
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
  currentInstanceId.value = row.id
  detailVisible.value = true
  await loadDetail()
}

async function loadDetail() {
  detailLoading.value = true
  try {
    detail.value = (await flowInstanceDetail(currentInstanceId.value)) as any
  } finally {
    detailLoading.value = false
  }
}

async function handleSuspend() {
  await suspendInstance(currentInstanceId.value)
  ElMessage.success('流程已挂起')
  loadDetail()
}

async function handleActivate() {
  await activateInstance(currentInstanceId.value)
  ElMessage.success('流程已激活')
  loadDetail()
}

async function handleTerminate() {
  await ElMessageBox.confirm('终止后流程不可恢复，确认终止?', '提示', { type: 'warning' })
  await terminateInstance(currentInstanceId.value)
  ElMessage.success('流程已终止')
  loadDetail()
  load()
}

async function handleComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入沟通内容')
    return
  }
  commentLoading.value = true
  try {
    await addFlowComment(currentInstanceId.value, { content: commentText.value })
    commentText.value = ''
    await loadDetail()
  } finally {
    commentLoading.value = false
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

function stateType(state: number) {
  if (state === 2) return 'success'
  if (state === 3) return 'danger'
  return 'primary'
}

function stateText(state: number) {
  const map: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已完成', 3: '已驳回', 4: '已撤销', 5: '已超时' }
  return map[state] || '已处理'
}

onMounted(load)
</script>

<style scoped>
.instance-detail {
  padding-right: 6px;
}

.instance-actions {
  margin: 12px 0;
  display: flex;
  gap: 8px;
}

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

.comment-list {
  max-height: 220px;
  overflow-y: auto;
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

.comment-input {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
