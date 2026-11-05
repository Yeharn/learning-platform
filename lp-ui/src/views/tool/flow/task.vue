<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 我的任务：待办与已办分页、办理、任意节点驳回、加签、转办、委托、拿回、撤销、已阅
-->

<template>
  <div class="app-container">
    <el-form :inline="true" class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="keyword" placeholder="流程名称 / 实例号 / 任务名称" clearable style="width: 260px"
          @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="loadAll">刷新</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <el-tabs v-model="activeTab" @tab-change="loadAll">
        <el-tab-pane label="我的待办" name="todo">
          <el-table v-loading="todoLoading" :data="todoList" border>
            <el-table-column prop="processName" label="流程名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="name" label="任务节点" min-width="130" show-overflow-tooltip />
            <el-table-column label="实例号" min-width="170">
              <template #default="{ row }">{{ row.instanceNo || row.instanceId }}</template>
            </el-table-column>
            <el-table-column prop="createBy" label="发起人" width="110" />
            <el-table-column prop="createTime" label="到达时间" width="170" />
            <el-table-column label="操作" width="290" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" icon="Finished" v-hasPerm="['tool:flow:execute']" @click="openComplete(row)">
                  办理
                </el-button>
                <el-button link type="danger" icon="RefreshLeft" v-hasPerm="['tool:flow:execute']" @click="openReject(row)">
                  驳回
                </el-button>
                <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
                  <el-button link type="primary" icon="ArrowDown">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="sign" icon="Plus">加签</el-dropdown-item>
                      <el-dropdown-item command="transfer" icon="Switch">转办</el-dropdown-item>
                      <el-dropdown-item command="delegate" icon="Connection">委托</el-dropdown-item>
                      <el-dropdown-item command="reclaim" icon="RefreshLeft">拿回</el-dropdown-item>
                      <el-dropdown-item command="withdraw" icon="CircleClose">撤销</el-dropdown-item>
                      <el-dropdown-item command="read" icon="View">已阅</el-dropdown-item>
                      <el-dropdown-item command="detail" icon="DataLine">流程图</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination v-model:current-page="todoQuery.pageNum" v-model:page-size="todoQuery.pageSize"
              :total="todoTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="() => { todoQuery.pageNum = 1; loadTodo() }" @current-change="loadTodo" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的已办" name="done">
          <el-table v-loading="doneLoading" :data="doneList" border>
            <el-table-column prop="processName" label="流程名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="name" label="任务节点" min-width="130" show-overflow-tooltip />
            <el-table-column label="实例号" min-width="170">
              <template #default="{ row }">{{ row.instanceNo || row.instanceId }}</template>
            </el-table-column>
            <el-table-column prop="comment" label="处理意见" min-width="160" show-overflow-tooltip />
            <el-table-column label="结果" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.state === 3 ? 'danger' : 'success'" size="small" effect="light">
                  {{ row.state === 3 ? '驳回' : '通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="finishTime" label="处理时间" width="170" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" icon="DataLine" @click="openDetail(row.instanceId)">流程图</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination v-model:current-page="doneQuery.pageNum" v-model:page-size="doneQuery.pageSize"
              :total="doneTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="() => { doneQuery.pageNum = 1; loadDone() }" @current-change="loadDone" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="completeVisible" title="办理任务" width="480px">
      <el-form label-width="80px">
        <el-form-item label="任务">
          <el-input :model-value="currentRow?.name" disabled />
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="actionForm.comment" type="textarea" :rows="3" placeholder="请输入处理意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleComplete">通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回任务" width="500px">
      <el-form label-width="90px">
        <el-form-item label="驳回节点">
          <el-select v-model="actionForm.nodeKey" placeholder="默认退回上一节点" clearable style="width: 100%">
            <el-option v-for="node in rejectNodes" :key="node.nodeKey" :label="node.nodeName" :value="node.nodeKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="驳回意见">
          <el-input v-model="actionForm.comment" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="signVisible" title="加签" width="480px">
      <el-form label-width="80px">
        <el-form-item label="加签人员">
          <el-select v-model="actionForm.userIds" multiple filterable placeholder="选择加签人员" style="width: 100%">
            <el-option v-for="user in candidates" :key="user.id" :label="user.name + ' (' + user.username + ')'"
              :value="String(user.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="actionForm.comment" type="textarea" :rows="2" placeholder="加签说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleSign">确认加签</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" :title="assignTitle" width="480px">
      <el-form label-width="80px">
        <el-form-item :label="assignTitle + '人员'">
          <el-select v-model="actionForm.userId" filterable placeholder="选择人员" style="width: 100%">
            <el-option v-for="user in candidates" :key="user.id" :label="user.name + ' (' + user.username + ')'"
              :value="String(user.id)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAssign">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="流程图" size="620px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="流程名称">{{ detail.instance?.processName }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.instance?.currentNodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.instance?.createBy }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.instance?.createTime }}</el-descriptions-item>
        </el-descriptions>
        <FlowViewer :nodes="detail.nodes || []" :current-key="detail.instance?.currentNodeKey" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlowViewer from '@/components/FlowViewer/index.vue'
import {
  addSignTask,
  completeTask,
  delegateTask,
  flowCandidates,
  flowDonePage,
  flowInstanceDetail,
  flowRejectNodes,
  flowTaskPage,
  readTask,
  reclaimTask,
  rejectToTask,
  transferTask,
  withdrawTask
} from '@/api/flow'

const activeTab = ref('todo')
const keyword = ref('')

const todoLoading = ref(false)
const todoList = ref<any[]>([])
const todoTotal = ref(0)
const todoQuery = reactive({ pageNum: 1, pageSize: 10 })

const doneLoading = ref(false)
const doneList = ref<any[]>([])
const doneTotal = ref(0)
const doneQuery = reactive({ pageNum: 1, pageSize: 10 })

const currentRow = ref<any>(null)
const candidates = ref<any[]>([])
const rejectNodes = ref<any[]>([])

const completeVisible = ref(false)
const rejectVisible = ref(false)
const signVisible = ref(false)
const assignVisible = ref(false)
const assignTitle = ref('转办')
const actionLoading = ref(false)
const actionForm = reactive<{ comment: string; nodeKey: string; userIds: string[]; userId: string }>({
  comment: '',
  nodeKey: '',
  userIds: [],
  userId: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({ instance: null, nodes: [] })

async function loadTodo() {
  todoLoading.value = true
  try {
    const res: any = await flowTaskPage({ keyword: keyword.value, ...todoQuery })
    todoList.value = res.rows || []
    todoTotal.value = Number(res.total) || 0
  } finally {
    todoLoading.value = false
  }
}

async function loadDone() {
  doneLoading.value = true
  try {
    const res: any = await flowDonePage({ keyword: keyword.value, ...doneQuery })
    doneList.value = res.rows || []
    doneTotal.value = Number(res.total) || 0
  } finally {
    doneLoading.value = false
  }
}

function loadAll() {
  loadTodo()
  loadDone()
}

function handleSearch() {
  todoQuery.pageNum = 1
  doneQuery.pageNum = 1
  loadAll()
}

async function ensureCandidates() {
  if (candidates.value.length === 0) {
    candidates.value = (await flowCandidates()) as any
  }
}

function resetForm() {
  actionForm.comment = ''
  actionForm.nodeKey = ''
  actionForm.userIds = []
  actionForm.userId = ''
}

function openComplete(row: any) {
  currentRow.value = row
  resetForm()
  completeVisible.value = true
}

async function handleComplete() {
  actionLoading.value = true
  try {
    await completeTask(currentRow.value.id, { comment: actionForm.comment })
    ElMessage.success('办理成功')
    completeVisible.value = false
    loadAll()
  } finally {
    actionLoading.value = false
  }
}

async function openReject(row: any) {
  currentRow.value = row
  resetForm()
  try {
    rejectNodes.value = (await flowRejectNodes(row.instanceId)) as any
  } catch (e) {
    rejectNodes.value = []
  }
  rejectVisible.value = true
}

async function handleReject() {
  actionLoading.value = true
  try {
    await rejectToTask(currentRow.value.id, { nodeKey: actionForm.nodeKey, comment: actionForm.comment })
    ElMessage.success('驳回成功')
    rejectVisible.value = false
    loadAll()
  } finally {
    actionLoading.value = false
  }
}

async function handleCommand(command: string, row: any) {
  currentRow.value = row
  resetForm()
  if (command === 'sign') {
    await ensureCandidates()
    signVisible.value = true
    return
  }
  if (command === 'transfer' || command === 'delegate') {
    await ensureCandidates()
    assignTitle.value = command === 'transfer' ? '转办' : '委托'
    assignVisible.value = true
    return
  }
  if (command === 'reclaim') {
    await ElMessageBox.confirm('确认拿回该任务?', '提示', { type: 'warning' })
    await reclaimTask(row.id)
    ElMessage.success('拿回成功')
    loadAll()
    return
  }
  if (command === 'withdraw') {
    await ElMessageBox.confirm('确认撤销该任务?', '提示', { type: 'warning' })
    await withdrawTask(row.id)
    ElMessage.success('撤销成功')
    loadAll()
    return
  }
  if (command === 'read') {
    await readTask(row.id)
    ElMessage.success('已标记为已阅')
    loadTodo()
    return
  }
  if (command === 'detail') {
    openDetail(row.instanceId)
  }
}

async function handleSign() {
  if (!actionForm.userIds.length) {
    ElMessage.warning('请选择加签人员')
    return
  }
  actionLoading.value = true
  try {
    await addSignTask(currentRow.value.id, { userIds: actionForm.userIds, comment: actionForm.comment })
    ElMessage.success('加签成功')
    signVisible.value = false
    loadTodo()
  } finally {
    actionLoading.value = false
  }
}

async function handleAssign() {
  if (!actionForm.userId) {
    ElMessage.warning('请选择人员')
    return
  }
  actionLoading.value = true
  try {
    if (assignTitle.value === '转办') {
      await transferTask(currentRow.value.id, { userId: actionForm.userId })
    } else {
      await delegateTask(currentRow.value.id, { userId: actionForm.userId })
    }
    ElMessage.success(assignTitle.value + '成功')
    assignVisible.value = false
    loadTodo()
  } finally {
    actionLoading.value = false
  }
}

async function openDetail(instanceId: string | number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = (await flowInstanceDetail(instanceId)) as any
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadAll)
</script>
