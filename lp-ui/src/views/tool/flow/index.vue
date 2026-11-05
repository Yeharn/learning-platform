<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统工具 - 流程管理页面
-->

<template>
  <div class="app-container">
    <el-card class="table-card" shadow="never">
      <el-tabs v-model="tab" @tab-change="loadTab">
        <el-tab-pane label="流程定义" name="defs">
          <div class="toolbar">
            <el-input v-model="keyword" placeholder="流程名称 / KEY" clearable style="width: 220px" @keyup.enter="loadTab" />
            <el-button icon="Search" @click="loadTab">查询</el-button>
            <div class="grow" />
            <el-button type="primary" icon="Plus" v-hasPerm="['tool:flow:deploy']" @click="openCreate">新建流程</el-button>
            <el-button icon="Document" v-hasPerm="['tool:flow:deploy']" @click="importVisible = true">导入 JSON</el-button>
          </div>
          <el-table :data="defs" v-loading="loading">
            <el-table-column prop="key" label="流程KEY" min-width="130" />
            <el-table-column prop="name" label="流程名称" min-width="160" />
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="createBy" label="创建人" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" icon="Promotion" v-hasPerm="['tool:flow:start']" @click="openStart(row)">发起</el-button>
                <el-button link type="primary" icon="View" @click="openModel(row)">模型</el-button>
                <el-button link type="danger" icon="Delete" v-hasPerm="['tool:flow:remove']" @click="handleRemove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="我的待办" name="my">
          <el-table :data="tasks" v-loading="loading">
            <el-table-column prop="name" label="任务名称" min-width="140" />
            <el-table-column prop="processName" label="流程" min-width="140" />
            <el-table-column prop="currentNodeName" label="当前节点" min-width="130" />
            <el-table-column prop="actors" label="办理人" min-width="130" />
            <el-table-column prop="createBy" label="发起人" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" icon="Finished" v-hasPerm="['tool:flow:execute']" @click="openComplete(row)">办理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="全部任务" name="all">
          <el-table :data="tasks" v-loading="loading">
            <el-table-column prop="name" label="任务名称" min-width="140" />
            <el-table-column prop="processName" label="流程" min-width="140" />
            <el-table-column prop="currentNodeName" label="当前节点" min-width="130" />
            <el-table-column prop="actors" label="办理人" min-width="130" />
            <el-table-column prop="createBy" label="发起人" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="运行实例" name="instances">
          <el-table :data="instances" v-loading="loading">
            <el-table-column prop="instanceNo" label="实例编号" min-width="170" />
            <el-table-column prop="processName" label="流程" min-width="150" />
            <el-table-column prop="currentNodeName" label="当前节点" min-width="130" />
            <el-table-column prop="createBy" label="发起人" width="120" />
            <el-table-column prop="createTime" label="开始时间" width="170" />
            <el-table-column prop="lastUpdateTime" label="更新时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="流程历史" name="history">
          <div class="toolbar">
            <el-switch v-model="onlyMine" active-text="仅我发起" @change="loadTab" />
          </div>
          <el-table :data="history" v-loading="loading">
            <el-table-column prop="processName" label="流程" min-width="150" />
            <el-table-column prop="instanceNo" label="实例编号" min-width="170" />
            <el-table-column prop="startUserName" label="发起人" width="120" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column prop="endTime" label="结束时间" width="170" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="stateType(row.state)">{{ stateText(row.state) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建流程 -->
    <el-dialog v-model="createVisible" title="新建流程（FlowLong）" width="640px">
      <el-form label-width="90px">
        <el-form-item label="流程名称"><el-input v-model="form.name" placeholder="如：请假审批" /></el-form-item>
        <el-form-item label="流程KEY"><el-input v-model="form.key" placeholder="如：leave" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="审批节点">
          <div class="node-list">
            <div v-for="(node, index) in form.nodes" :key="index" class="node-row">
              <span class="node-index">{{ index + 1 }}</span>
              <el-input v-model="node.name" placeholder="节点名称" style="width: 200px" />
              <el-select v-model="node.assigneeId" filterable placeholder="审批人" style="width: 200px">
                <el-option v-for="u in users" :key="u.userId" :label="u.nickName || u.username" :value="String(u.userId)" />
              </el-select>
              <el-button link type="danger" icon="Delete" @click="form.nodes.splice(index, 1)" />
            </div>
            <el-button link type="primary" icon="Plus" @click="addNode">添加审批节点</el-button>
          </div>
        </el-form-item>
        <el-form-item label="抄送人">
          <el-select v-model="form.ccId" filterable clearable placeholder="可选" style="width: 200px">
            <el-option v-for="u in users" :key="u.userId" :label="u.nickName || u.username" :value="String(u.userId)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">部署</el-button>
      </template>
    </el-dialog>

    <!-- 导入 JSON -->
    <el-dialog v-model="importVisible" title="导入 FlowLong JSON 模型" width="640px">
      <el-input v-model="jsonModel" type="textarea" :rows="14" placeholder='{"key":"leave","name":"请假审批","nodeConfig":{...}}' />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport">部署</el-button>
      </template>
    </el-dialog>

    <!-- 查看模型 -->
    <el-dialog v-model="modelVisible" title="流程模型 JSON" width="640px">
      <el-input v-model="modelContent" type="textarea" :rows="16" readonly />
    </el-dialog>

    <!-- 发起流程 -->
    <el-dialog v-model="startVisible" :title="`发起流程：${current.name || current.key}`" width="480px">
      <el-form label-width="90px">
        <el-form-item label="事由">
          <el-input v-model="startForm.reason" type="textarea" placeholder="描述本次申请事由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStart">发起</el-button>
      </template>
    </el-dialog>

    <!-- 办理任务 -->
    <el-dialog v-model="completeVisible" title="办理任务" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审批意见"><el-input v-model="completeForm.comment" type="textarea" /></el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="completeForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回（退回发起人）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleComplete">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  allTasks,
  completeTask,
  deployFlowJson,
  deployFlowModel,
  flowDefinitions,
  flowHistory,
  flowInstances,
  flowModel,
  flowRemove,
  flowStart,
  myTasks,
  rejectTask
} from '@/api/extras'
import { listUser } from '@/api/system'

const tab = ref('defs')
const loading = ref(false)
const keyword = ref('')
const defs = ref<any[]>([])
const tasks = ref<any[]>([])
const instances = ref<any[]>([])
const history = ref<any[]>([])
const users = ref<any[]>([])
const onlyMine = ref(false)

const current = ref<any>({})
const createVisible = ref(false)
const importVisible = ref(false)
const modelVisible = ref(false)
const startVisible = ref(false)
const completeVisible = ref(false)
const modelContent = ref('')
const jsonModel = ref('')

const form = reactive({
  name: '',
  key: '',
  remark: '',
  ccId: '',
  nodes: [{ name: '主管审批', assigneeId: '' }] as Array<{ name: string; assigneeId: string }>
})
const startForm = reactive({ reason: '' })
const completeForm = reactive({ comment: '', approved: true })

async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'defs') {
      defs.value = (await flowDefinitions(keyword.value)) as any[]
    } else if (tab.value === 'my') {
      tasks.value = (await myTasks()) as any[]
    } else if (tab.value === 'all') {
      tasks.value = (await allTasks()) as any[]
    } else if (tab.value === 'instances') {
      instances.value = (await flowInstances()) as any[]
    } else if (tab.value === 'history') {
      history.value = (await flowHistory(onlyMine.value)) as any[]
    }
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  if (users.value.length) {
    return
  }
  try {
    const res: any = await listUser({ pageNum: 1, pageSize: 200 })
    users.value = res.rows || []
  } catch {
    // 无用户查询权限时允许直接填写用户ID
  }
}

function addNode() {
  form.nodes.push({ name: '审批节点', assigneeId: '' })
}

async function openCreate() {
  await loadUsers()
  form.name = ''
  form.key = ''
  form.remark = ''
  form.ccId = ''
  form.nodes = [{ name: '主管审批', assigneeId: '' }]
  createVisible.value = true
}

async function handleCreate() {
  if (!form.name || !form.key) {
    ElMessage.warning('请填写流程名称与 KEY')
    return
  }
  const nodes = form.nodes.filter((n) => n.assigneeId)
  if (!nodes.length) {
    ElMessage.warning('请至少配置一个审批人')
    return
  }
  const userName = (id: string) => {
    const user = users.value.find((u) => String(u.userId) === id)
    return user ? user.nickName || user.username : id
  }
  await deployFlowModel({
    key: form.key,
    name: form.name,
    remark: form.remark,
    nodes: nodes.map((n) => ({ name: n.name, assigneeId: n.assigneeId, assigneeName: userName(n.assigneeId) })),
    cc: form.ccId ? [{ assigneeId: form.ccId, assigneeName: userName(form.ccId) }] : []
  })
  ElMessage.success('流程已部署')
  createVisible.value = false
  tab.value = 'defs'
  loadTab()
}

async function handleImport() {
  if (!jsonModel.value.trim()) {
    ElMessage.warning('请粘贴流程模型 JSON')
    return
  }
  await deployFlowJson({ model: jsonModel.value })
  ElMessage.success('流程已部署')
  importVisible.value = false
  jsonModel.value = ''
  loadTab()
}

async function openModel(row: any) {
  modelContent.value = ((await flowModel(row.id)) as unknown as string) || ''
  modelVisible.value = true
}

function openStart(row: any) {
  current.value = row
  startForm.reason = ''
  startVisible.value = true
}

async function handleStart() {
  await flowStart({ processId: current.value.id, variables: { reason: startForm.reason } })
  ElMessage.success('流程已发起')
  startVisible.value = false
  tab.value = 'my'
  loadTab()
}

function openComplete(row: any) {
  current.value = row
  completeForm.comment = ''
  completeForm.approved = true
  completeVisible.value = true
}

async function handleComplete() {
  if (completeForm.approved) {
    await completeTask(current.value.id, { comment: completeForm.comment, variables: { approved: true } })
  } else {
    await rejectTask(current.value.id, { comment: completeForm.comment })
  }
  ElMessage.success(completeForm.approved ? '审批通过' : '已驳回')
  completeVisible.value = false
  loadTab()
}

async function handleRemove(row: any) {
  await ElMessageBox.confirm('确认删除该流程（含运行中实例与历史）？', '提示', { type: 'warning' })
  await flowRemove(row.id)
  ElMessage.success('删除成功')
  loadTab()
}

function stateText(state: number) {
  const map: Record<number, string> = {
    [-2]: '已暂停',
    [-1]: '暂存待审',
    0: '审批中',
    1: '审批通过',
    2: '审批拒绝',
    3: '撤销',
    4: '超时结束',
    5: '强制终止',
    6: '自动通过',
    7: '自动拒绝'
  }
  return map[state] || '未知'
}

function stateType(state: number) {
  if (state === 1 || state === 6) return 'success'
  if (state === 2 || state === 7) return 'danger'
  if (state === 3 || state === 4 || state === 5) return 'info'
  return 'warning'
}

onMounted(loadTab)
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.grow {
  flex: 1;
}

.node-list {
  width: 100%;
}

.node-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.node-index {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: var(--lp-primary, #3b82f6);
  color: #fff;
  font-size: 12px;
}
</style>
