<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 消息中心页面
-->

<template>
  <div class="app-container">
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="box" @change="load">
          <el-radio-button value="inbox">收件箱</el-radio-button>
          <el-radio-button value="sent">已发送</el-radio-button>
        </el-radio-group>
        <div class="toolbar-right">
          <el-dropdown trigger="click" @command="handleMode">
            <el-tag type="success" effect="plain" style="cursor: pointer">
              推送模式：{{ modeText }}<el-icon><ArrowDown /></el-icon>
            </el-tag>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="inner">站内信（默认）</el-dropdown-item>
                <el-dropdown-item command="email">邮件</el-dropdown-item>
                <el-dropdown-item command="websocket">WebSocket 实时推送</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="box === 'inbox'" icon="Check" @click="handleReadAll">全部已读</el-button>
          <el-button type="primary" icon="Promotion" v-hasPerm="['system:message:send']" @click="sendVisible = true">发送消息</el-button>
          <el-button type="danger" icon="Delete" :disabled="!selection.length" v-hasPerm="['system:message:remove']" @click="handleDelete">删除</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" @selection-change="(s: any) => (selection = s)">
        <el-table-column type="selection" width="46" />
        <el-table-column label="状态" width="80" v-if="box === 'inbox'">
          <template #default="{ row }">
            <el-badge is-dot :hidden="row.readFlag === '1'" class="msg-dot" />
            <el-tag :type="row.readFlag === '1' ? 'info' : 'danger'" effect="light">
              {{ row.readFlag === '1' ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="msgType" label="类型" width="110">
          <template #default="{ row }">{{ typeText(row.msgType) }}</template>
        </el-table-column>
        <el-table-column :prop="box === 'sent' ? 'receiverName' : 'senderName'" :label="box === 'sent' ? '接收人' : '发送人'" width="130" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="show(row)">查看</el-button>
            <el-button link type="danger" icon="Delete" v-hasPerm="['system:message:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
    </el-card>

    <el-dialog v-model="detailVisible" :title="detail.title" width="560px">
      <div class="msg-meta">
        <el-tag size="small">{{ typeText(detail.msgType) }}</el-tag>
        <span>{{ detail.senderName }}</span>
        <span>{{ detail.createTime }}</span>
      </div>
      <div class="msg-content">{{ detail.content }}</div>
    </el-dialog>

    <el-dialog v-model="sendVisible" title="发送消息" width="620px">
      <el-form label-width="80px">
        <el-form-item label="接收人">
          <el-select v-model="receiverIds" multiple filterable placeholder="选择用户" style="width: 100%">
            <el-option v-for="u in users" :key="u.userId" :label="u.nickname || u.username" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.msgType">
            <el-radio value="notice">系统通知</el-radio>
            <el-radio value="announce">公告</el-radio>
            <el-radio value="task">任务提醒</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  delMessage, listMessage, messageModeInfo, readAllMessages,
  readMessages, sendMessage, switchMessageMode
} from '@/api/extras'
import { listUser } from '@/api/system'

const box = ref('inbox')
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const modeInfo = ref<any>({})
const detail = ref<any>({})
const detailVisible = ref(false)
const sendVisible = ref(false)
const sending = ref(false)
const users = ref<any[]>([])
const receiverIds = ref<number[]>([])
const form = reactive({ title: '', content: '', msgType: 'notice' })
const query = reactive({ pageNum: 1, pageSize: 10 })

const modeText = computed(() => ({ inner: '站内信', email: '邮件', websocket: '实时推送' } as any)[modeInfo.value.mode] || '站内信')

function typeText(t: string) {
  return ({ notice: '系统通知', announce: '公告', task: '任务提醒' } as any)[t] || t
}

async function load() {
  loading.value = true
  try {
    const res: any = await listMessage({ ...query, box })
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMode() {
  modeInfo.value = await messageModeInfo()
}

async function show(row: any) {
  detail.value = row
  detailVisible.value = true
  if (box.value === 'inbox' && row.readFlag === '0') {
    await readMessages([row.messageId])
    row.readFlag = '1'
  }
}

async function handleReadAll() {
  await readAllMessages()
  ElMessage.success('已全部标记为已读')
  load()
}

async function handleDelete(row?: any) {
  const ids = row ? [row.messageId] : selection.value.map((s) => s.messageId)
  if (!ids.length) return
  await ElMessageBox.confirm('确认删除所选消息？', '提示', { type: 'warning' })
  await delMessage(ids)
  ElMessage.success('删除成功')
  load()
}

async function openSend() {
  sendVisible.value = true
  if (!users.value.length) {
    const res: any = await listUser({ pageNum: 1, pageSize: 200 })
    users.value = res.rows || []
  }
}

async function handleMode(mode: string) {
  await switchMessageMode(mode)
  ElMessage.success('推送模式已切换')
  loadMode()
}

async function handleSend() {
  if (!receiverIds.value.length) {
    ElMessage.warning('请选择接收人')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  sending.value = true
  try {
    await sendMessage({ receiverIds: receiverIds.value, ...form })
    ElMessage.success('发送成功')
    sendVisible.value = false
    receiverIds.value = []
    form.title = ''
    form.content = ''
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  load()
  loadMode()
  requestUsers()
})

async function requestUsers() {
  try {
    const res: any = await listUser({ pageNum: 1, pageSize: 200 })
    users.value = res.rows || []
  } catch {
    // 无 system:user:list 权限时忽略
  }
}
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.msg-dot {
  display: inline-block;
  margin-right: 4px;
}

.msg-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.msg-content {
  white-space: pre-wrap;
  line-height: 1.8;
  background: var(--lp-card-bg, #f8fafc);
  border-radius: 12px;
  padding: 16px;
  min-height: 120px;
}
</style>
