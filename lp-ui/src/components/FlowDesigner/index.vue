<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 仿钉钉流程设计器（Dromara Warm-Flow 1.8.9）：节点面板 + 主干分支画布 + 属性面板，
                支持审批（会签/或签/票签）、抄送、自动化（自动审批/远程调用/脚本）、
                互斥/并行/包容网关、条件表达式、办理人表达式（${handler} / SpEL / 角色 / 部门）、表单绑定
-->

<template>
  <div class="flow-designer">
    <div class="designer-main">
      <div class="canvas-toolbar">
        <div class="toolbar-head">
          <div class="toolbar-title">
            <span class="toolbar-badge">⚙</span>
            <span>流程设计</span>
            <span class="toolbar-tip">点击节点配置属性，悬浮 "+" 插入节点</span>
          </div>
        </div>
        <el-form :inline="true" class="toolbar-form">
          <el-form-item label="流程编码">
            <el-input v-model="def.flowCode" placeholder="如 expense" style="width: 180px" :disabled="hasId" />
          </el-form-item>
          <el-form-item label="流程名称">
            <el-input v-model="def.flowName" placeholder="请输入流程名称" style="width: 200px" />
          </el-form-item>
          <el-form-item label="表单模板">
            <el-select v-model="def.formPath" clearable placeholder="绑定表单" style="width: 180px">
              <el-option v-for="form in forms" :key="form.formCode" :label="form.formName" :value="form.formCode" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button-group>
              <el-button icon="Upload" @click="exportJson">导出</el-button>
              <el-button icon="View" @click="preview">预览</el-button>
              <el-button type="primary" icon="Promotion" :loading="saving" @click="save">保存并部署</el-button>
            </el-button-group>
          </el-form-item>
        </el-form>
      </div>

      <div class="canvas-scroll">
        <div class="start-node">
          <div class="pill-badge start">▶</div>
          <div class="pill-text">
            <span>流程开始</span>
            <small>发起人自动提交</small>
          </div>
        </div>
        <div class="connect-line" />
        <render-chain :head="def.root" @edit="openProps" />
        <div class="connect-line" />
        <div class="end-node">
          <div class="pill-badge end">✓</div>
          <div class="pill-text">
            <span>流程结束</span>
            <small>归档并生成历史</small>
          </div>
        </div>
      </div>
    </div>

    <div class="designer-props" v-if="current">
      <div class="props-header">
        <span class="props-title">{{ propTitle }}</span>
        <el-button text icon="Close" @click="current = null" />
      </div>
      <el-scrollbar class="props-body">
        <el-form label-width="86px" size="default">
          <el-form-item label="节点名称">
            <el-input v-model="current.name" maxlength="30" />
          </el-form-item>

          <template v-if="current.kind === 'gateway'">
            <el-form-item label="网关类型">
              <el-radio-group v-model="current.gatewayKind">
                <el-radio :value="3">互斥</el-radio>
                <el-radio :value="4">并行</el-radio>
                <el-radio :value="5">包容</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-divider content-position="left">分支</el-divider>
            <div v-for="(branch, index) in current.branches" :key="branch.id" class="branch-editor">
              <el-form-item label="分支名称">
                <el-input v-model="branch.name" maxlength="20" style="width: 140px" />
              </el-form-item>
              <el-form-item label="条件表达式">
                <div class="condition-row">
                  <el-select v-model="branch.conditionMode" style="width: 110px" @change="onConditionModeChange(branch)">
                    <el-option label="内置运算符" value="builtin" />
                    <el-option label="SpEL" value="spel" />
                  </el-select>
                  <template v-if="branch.conditionMode === 'builtin'">
                    <el-select v-model="branch.op" style="width: 92px">
                      <el-option v-for="op in operators" :key="op.value" :label="op.label" :value="op.value" />
                    </el-select>
                    <el-input v-model="branch.field" placeholder="变量名" style="width: 120px" />
                    <el-input v-model="branch.value" placeholder="比较值" style="width: 120px" />
                  </template>
                  <el-input v-else v-model="branch.expression" :placeholder="'spel@@${amount > 1000}'" style="width: 330px" />
                  <el-button text type="danger" icon="Delete" :disabled="current.branches.length <= 2"
                             @click="removeBranch(index)" />
                </div>
                <div class="condition-tip">内置：gt@@field|value，SpEL：spel@@${expression}；默认分支使用 default@@${1==1}</div>
              </el-form-item>
            </div>
            <el-form-item>
              <el-button plain icon="Plus" @click="addBranch">添加分支</el-button>
            </el-form-item>
          </template>

          <template v-else-if="current.kind !== 'start' && current.kind !== 'end'">
            <el-form-item v-if="current.kind === 'approval'" label="审批方式">
              <el-select v-model="current.ratio">
                <el-option label="或签（任一人通过）" value="0" />
                <el-option label="会签（全部通过）" value="100" />
                <el-option label="票签（按比例）" value="50" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="current.kind === 'approval'" label="办理人">
              <el-select v-model="current.assigneeMode" @change="onModeChange">
                <el-option label="指定用户" value="user" />
                <el-option label="按角色" value="role" />
                <el-option label="按部门" value="dept" />
                <el-option label="发起人 ${handler}" value="handler" />
                <el-option label="SpEL 表达式" value="spel" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="current.kind === 'approval' && current.assigneeMode === 'user'" label="选择用户">
              <el-select v-model="current.assignees" multiple filterable style="width: 100%">
                <el-option v-for="c in candidates" :key="c.id" :label="c.name" :value="String(c.id)" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="current.kind === 'approval' && current.assigneeMode === 'role'" label="选择角色">
              <el-select v-model="current.assigneeExpr" style="width: 100%">
                <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="'role:' + r.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="current.kind === 'approval' && current.assigneeMode === 'dept'" label="选择部门">
              <el-select v-model="current.assigneeExpr" style="width: 100%">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="'dept:' + d.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="current.kind === 'approval' && current.assigneeMode === 'spel'" label="SpEL 表达式">
              <el-input v-model="current.assigneeExpr" placeholder="#{@userService.getUserIds()}" />
            </el-form-item>
            <div v-if="current.kind === 'approval'" class="assignee-tip">
              发起人使用内置变量 ${handler}；角色/部门在办理时自动展开为成员用户
            </div>

            <el-divider content-position="left">自动化扩展</el-divider>
            <el-form-item label="自动审批">
              <el-switch v-model="current.ext.autoApprove" />
            </el-form-item>
            <el-form-item v-if="current.ext.autoApprove" label="审批意见">
              <el-input v-model="current.ext.autoComment" placeholder="系统自动审批通过" />
            </el-form-item>
            <el-form-item label="抄送">
              <el-switch v-model="current.ext.cc" />
            </el-form-item>
            <el-form-item v-if="current.ext.cc" label="抄送人">
              <el-select v-model="current.ext.ccUsers" multiple filterable style="width: 100%">
                <el-option v-for="c in candidates" :key="c.id" :label="c.name" :value="String(c.id)" />
              </el-select>
            </el-form-item>
            <el-form-item label="远程调用">
              <el-switch v-model="current.remoteEnabled" />
            </el-form-item>
            <template v-if="current.remoteEnabled">
              <el-form-item label="调用地址">
                <el-input v-model="current.ext.remote.url" placeholder="http://..." />
              </el-form-item>
              <el-form-item label="请求方式">
                <el-select v-model="current.ext.remote.method" style="width: 120px">
                  <el-option label="POST" value="POST" />
                  <el-option label="GET" value="GET" />
                </el-select>
              </el-form-item>
            </template>
            <el-form-item label="脚本执行">
              <el-switch v-model="current.scriptEnabled" />
            </el-form-item>
            <template v-if="current.scriptEnabled">
              <el-form-item label="SpEL 脚本">
                <el-input v-model="current.ext.script.expression" placeholder="例：#amount > 1000" />
              </el-form-item>
              <el-form-item label="结果变量">
                <el-input v-model="current.ext.script.resultKey" placeholder="写入流程变量的变量名" />
              </el-form-item>
            </template>
          </template>

          <template v-else-if="current.kind === 'start'">
            <el-alert type="info" :closable="false" title="开始节点代表流程发起人，发起后直接进入下一节点" />
          </template>
        </el-form>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { flowCandidates, flowDepts, flowDesignPreview, flowForms, flowRoles } from '@/api/flow'

interface DesignBranch {
  id: string
  name: string
  conditionMode: 'builtin' | 'spel'
  op: string
  field: string
  value: string
  expression: string
  child: DesignNode | null
}

interface DesignNode {
  id: string
  kind: 'start' | 'approval' | 'gateway' | 'end'
  name: string
  gatewayKind: 3 | 4 | 5
  ratio: string
  assigneeMode: 'user' | 'role' | 'dept' | 'handler' | 'spel'
  assignees: string[]
  assigneeExpr: string
  formPath: string
  remoteEnabled: boolean
  scriptEnabled: boolean
  ext: {
    autoApprove?: boolean
    autoComment?: string
    cc?: boolean
    ccUsers?: string[]
    remote: { url?: string; method?: string }
    script: { expression?: string; resultKey?: string }
  }
  branches: DesignBranch[]
  child: DesignNode | null
}

const operators = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'ge' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'le' },
  { label: '包含', value: 'like' }
]

const props = defineProps<{ model?: string; saving?: boolean }>()
const emit = defineEmits<{ (e: 'save', payload: { model: string; key: string; name: string }): void }>()

const hasId = ref(false)
const candidates = ref<any[]>([])
const roles = ref<any[]>([])
const depts = ref<any[]>([])
const forms = ref<any[]>([])
const current = ref<DesignNode | null>(null)

let seq = 1
function nextId(prefix: string) {
  return prefix + '_' + Date.now().toString(36) + '_' + seq++
}

function defaultNode(kind: DesignNode['kind']): DesignNode {
  return {
    id: nextId(kind),
    kind,
    name: kindName(kind),
    gatewayKind: 3,
    ratio: '0',
    assigneeMode: 'user',
    assignees: [],
    assigneeExpr: '',
    formPath: '',
    remoteEnabled: false,
    scriptEnabled: false,
    ext: { autoApprove: false, autoComment: '', cc: false, ccUsers: [], remote: { url: '', method: 'POST' }, script: { expression: '', resultKey: '' } },
    branches: [],
    child: null
  }
}

function kindName(kind: DesignNode['kind']) {
  switch (kind) {
    case 'start':
      return '发起人'
    case 'approval':
      return '审批节点'
    case 'gateway':
      return '条件分支'
    case 'end':
      return '结束'
    default:
      return '节点'
  }
}

function newNodeChain() {
  const start = defaultNode('start')
  start.name = '发起人'
  return start
}

const def = reactive({
  flowCode: '',
  flowName: '',
  formPath: '',
  root: newNodeChain()
})

const propTitle = computed(() => (current.value ? '节点属性 - ' + (current.value.name || kindName(current.value.kind)) : ''))

/* ---------------- 画布渲染组件 ---------------- */

const RenderChain = defineComponent({
  name: 'RenderChain',
  props: { head: { type: Object as () => DesignNode | null, required: true } },
  emits: ['edit'],
  setup(chainProps, { emit: chainEmit }) {
    return () => {
      const children: any[] = []
      let node = chainProps.head && chainProps.head.kind === 'start' ? chainProps.head.child : chainProps.head
      let guard = 0
      while (node && guard++ < 100) {
        if (node.kind === 'gateway') {
          children.push(h(RenderGateway, { node, onEdit: (n: DesignNode) => chainEmit('edit', n) }))
        } else {
          children.push(
            h('div', { class: 'node-wrap' }, [
              h(NodeCard, { node, onEdit: () => chainEmit('edit', node!) }),
              h(AddButton, { onAdd: (kind: string) => addAfter(node!, kind) })
            ])
          )
        }
        node = node.child
      }
      children.push(h(AddButton, { onAdd: (kind: string) => addAtTail(kind) }))
      return h('div', { class: 'chain' }, children)
    }
  }
})

const NodeCard = defineComponent({
  props: { node: { type: Object as () => DesignNode, required: true } },
  emits: ['edit'],
  setup(cardProps, { emit: cardEmit }) {
    const icon = computed(() => {
      if (cardProps.node.kind === 'approval') return '✓'
      if (cardProps.node.kind === 'gateway') return '⚡'
      return '•'
    })
    const summary = computed(() => {
      const node = cardProps.node
      if (node.kind === 'approval') {
        if (node.assigneeMode === 'handler') return '发起人'
        if (node.assigneeMode === 'spel') return node.assigneeExpr || 'SpEL 表达式'
        if (node.assigneeMode === 'role') return '角色：' + (node.assigneeExpr || '未选择')
        if (node.assigneeMode === 'dept') return '部门：' + (node.assigneeExpr || '未选择')
        return node.assignees.length ? `指定 ${node.assignees.length} 人` : '未配置办理人'
      }
      return ''
    })
    const extra = computed(() => {
      const node = cardProps.node
      const tags: string[] = []
      if (node.ext.autoApprove) tags.push('自动审批')
      if (node.ext.cc) tags.push('抄送')
      if (node.remoteEnabled) tags.push('远程调用')
      if (node.scriptEnabled) tags.push('脚本')
      return tags.join(' / ')
    })
    return () =>
      h('div', { class: ['canvas-node', cardProps.node.kind, cardProps.node.ext.autoApprove ? 'auto' : ''], onClick: () => cardEmit('edit') }, [
        h('div', { class: ['canvas-node-icon', cardProps.node.kind] }, icon.value),
        h('div', { class: 'canvas-node-main' }, [
          h('div', { class: 'canvas-node-name' }, cardProps.node.name),
          h('div', { class: 'canvas-node-sub' }, summary.value || ''),
          extra.value ? h('div', { class: 'canvas-node-ext' }, extra.value) : null
        ]),
        h('div', { class: 'canvas-node-edit' }, '✎')
      ])
  }
})

const RenderGateway = defineComponent({
  props: { node: { type: Object as () => DesignNode, required: true } },
  emits: ['edit'],
  setup(gwProps, { emit: gwEmit }) {
    return () => {
      const branchColumns = gwProps.node.branches.map((branch) =>
        h('div', { class: 'branch-column' }, [
          h('div', { class: 'branch-title' }, [
            h('span', {}, branch.name || '分支'),
            h('span', { class: 'branch-cond' }, branchConditionText(branch))
          ]),
          h('div', { class: 'connect-line' }, ''),
          branch.child
            ? h('div', { class: 'node-wrap' }, [
                h(NodeCard, { node: branch.child, onEdit: () => gwEmit('edit', branch.child!) }),
                h(AddButton, { onAdd: (kind: string) => addAfter(branch.child!, kind) })
              ])
            : h('div', { class: 'branch-empty' }, '空分支'),
          h('div', { class: 'connect-line' }, ''),
          h(AddButton, { onAdd: (kind: string) => addInBranch(branch, kind) })
        ])
      )
      return h('div', { class: 'gateway-wrap' }, [
        h('div', { class: 'node-wrap gateway-head' }, [
          h(NodeCard, { node: gwProps.node, onEdit: () => gwEmit('edit', gwProps.node) }),
          h(AddButton, { onAdd: (kind: string) => addAfter(gwProps.node, kind) })
        ]),
        h('div', { class: 'branch-container' }, branchColumns)
      ])
    }
  }
})

const AddButton = defineComponent({
  props: {},
  emits: ['add'],
  setup(addProps, { emit: addEmit }) {
    const visible = ref(false)
    const items = [
      { kind: 'approval', label: '审批节点', icon: '✓' },
      { kind: 'gateway', label: '条件/并行分支', icon: '⚡' },
      { kind: 'end', label: '结束节点', icon: '■' }
    ]
    return () =>
      h('div', { class: 'add-wrap' }, [
        h('button', {
          class: 'add-btn',
          onClick: (event: Event) => {
            event.stopPropagation()
            visible.value = !visible.value
          }
        }, '+'),
        visible.value
          ? h('div', { class: 'add-menu' }, items.map((item) =>
              h('div', {
                class: 'add-item',
                onClick: (event: Event) => {
                  event.stopPropagation()
                  visible.value = false
                  addEmit('add', item.kind)
                }
              }, [h('span', { class: 'add-item-icon' }, item.icon), item.label])
            ))
          : null
      ])
  }
})

/* ---------------- 树结构操作 ---------------- */

function addAfter(node: DesignNode, kind: string) {
  const created = defaultNode(kind as DesignNode['kind'])
  created.child = node.child
  node.child = created
  openProps(created)
}

function addAtTail(kind: string) {
  let tail = def.root
  while (tail.child) tail = tail.child
  addAfter(tail, kind)
}

function addInBranch(branch: DesignBranch, kind: string) {
  if (!branch.child) {
    branch.child = defaultNode(kind as DesignNode['kind'])
    openProps(branch.child)
    return
  }
  addAfter(branch.child, kind)
}

function addBranch() {
  if (!current.value) return
  current.value.branches.push({
    id: nextId('branch'),
    name: '分支' + (current.value.branches.length + 1),
    conditionMode: 'builtin',
    op: 'gt',
    field: '',
    value: '',
    expression: 'spel@@${}',
    child: null
  })
}

function removeBranch(index: number) {
  current.value?.branches.splice(index, 1)
}

function onModeChange() {
  if (!current.value) return
  current.value.assignees = []
  current.value.assigneeExpr = ''
}

function onConditionModeChange(branch: DesignBranch) {
  if (branch.conditionMode === 'builtin') {
    branch.field = ''
    branch.value = ''
    branch.op = 'gt'
  } else {
    branch.expression = 'spel@@${}'
  }
}

function openProps(node: DesignNode) {
  current.value = node
  if (node.kind === 'gateway' && node.branches.length === 0) {
    addBranch()
    addBranch()
  }
}

function branchConditionText(branch: DesignBranch) {
  if (branch.conditionMode === 'spel') {
    return branch.expression && branch.expression !== 'spel@@${}' ? 'SpEL' : '未配置'
  }
  return branch.field ? `${branch.field} ${branch.op} ${branch.value}` : '未配置'
}

/* ---------------- DefJson 编译 / 解析 ---------------- */

function builtinCondition(branch: DesignBranch) {
  if (branch.conditionMode === 'spel') {
    const expr = (branch.expression || 'spel@@${}').trim()
    return expr.startsWith('spel@@') ? expr : 'spel@@' + expr
  }
  return `${branch.op}@@${branch.field}|${branch.value}`
}

function compile(): any {
  const nodeList: any[] = []
  const skipOf = (code: string, targets: { code: string; condition?: string; name?: string; type?: string }[]) => {
    const skipList = targets.map((target) => ({
      nowNodeCode: code,
      nextNodeCode: target.code,
      skipType: target.type || 'PASS',
      skipName: target.name,
      skipCondition: target.condition
    }))
    return skipList
  }

  const emitBusiness = (node: DesignNode, nextCode: string | null, nextCondition?: string) => {
    const nodeJson: any = {
      nodeType: 1,
      nodeCode: node.id,
      nodeName: node.name,
      nodeRatio: node.kind === 'approval' ? node.ratio : '0'
    }
    if (node.kind === 'approval') {
      const flags: string[] = []
      if (node.assigneeMode === 'handler') {
        flags.push('${handler}')
      } else if (node.assigneeMode === 'spel') {
        flags.push(node.assigneeExpr || '#{1}')
      } else if (node.assigneeMode === 'role') {
        if (node.assigneeExpr) flags.push(node.assigneeExpr)
      } else if (node.assigneeMode === 'dept') {
        if (node.assigneeExpr) flags.push(node.assigneeExpr)
      } else {
        flags.push(...node.assignees)
      }
      nodeJson.permissionFlag = flags.join('@@')
    }
    const ext: any = { ...node.ext }
    if (node.remoteEnabled && node.ext.remote?.url) {
      ext.remote = node.ext.remote
    } else {
      delete ext.remote
    }
    if (node.scriptEnabled && node.ext.script?.expression) {
      ext.script = node.ext.script
    } else {
      delete ext.script
    }
    if (Object.keys(ext).length) {
      nodeJson.ext = JSON.stringify(ext)
    }
    nodeJson.skipList = nextCode ? skipOf(node.id, [{ code: nextCode, condition: nextCondition }]) : []
    nodeList.push(nodeJson)
    return nodeJson
  }

  const emitGateway = (node: DesignNode, mergeTarget: string) => {
    const gatewayJson: any = {
      nodeType: node.gatewayKind,
      nodeCode: node.id,
      nodeName: node.name,
      nodeRatio: '0'
    }
    const branchCodes: { code: string; condition?: string; name?: string }[] = []
    node.branches.forEach((branch) => {
      if (!branch.child) {
        throw new Error(`分支「${branch.name}」为空，请为每个分支添加节点`)
      }
      const branchHeadCode = emitChain(branch.child, mergeTarget)
      branchCodes.push({ code: branchHeadCode!, condition: builtinCondition(branch), name: branch.name })
    })
    gatewayJson.skipList = skipOf(node.id, branchCodes)
    nodeList.push(gatewayJson)
    return gatewayJson
  }

  /** 线性链：内部节点依次相连，链尾连接到 tailCode；返回链头 code */
  const emitChain = (head: DesignNode, tailCode: string): string => {
    const items: DesignNode[] = []
    let cursor: DesignNode | null = head
    let guard = 0
    while (cursor && guard++ < 200) {
      items.push(cursor)
      cursor = cursor.child
    }
    items.forEach((item, index) => {
      const target = index === items.length - 1 ? tailCode : items[index + 1].id
      if (item.kind === 'gateway') {
        emitGateway(item, target)
      } else {
        emitBusiness(item, target)
      }
    })
    return items[0].id
  }

  // 主干
  const head = def.root.child
  if (!head) {
    throw new Error('请至少添加一个节点')
  }
  const startJson: any = { nodeType: 0, nodeCode: 'start', nodeName: '发起人', nodeRatio: '0' }
  const headCode = emitChain(head, 'end')
  startJson.skipList = skipOf('start', [{ code: headCode }])
  nodeList.push(startJson)
  const endJson: any = { nodeType: 2, nodeCode: 'end', nodeName: '结束', nodeRatio: '0', skipList: [] }
  nodeList.push(endJson)

  // 空连线兜底连接结束节点
  nodeList.forEach((node) => {
    if (node.nodeType !== 2 && (!node.skipList || node.skipList.length === 0)) {
      node.skipList = skipOf(node.nodeCode, [{ code: 'end' }])
    }
  })

  return {
    flowCode: def.flowCode,
    flowName: def.flowName,
    modelValue: 'MIMIC',
    formPath: def.formPath || undefined,
    formCustom: def.formPath ? 'Y' : undefined,
    nodeList
  }
}

function parse(modelJson: string) {
  try {
    const data = JSON.parse(modelJson)
    def.flowCode = data.flowCode || ''
    def.flowName = data.flowName || ''
    def.formPath = data.formPath || ''
    if (!Array.isArray(data.nodeList) || !data.nodeList.length) {
      def.root = newNodeChain()
      return
    }
    const nodes = new Map<string, any>()
    data.nodeList.forEach((node: any) => nodes.set(node.nodeCode, node))
    const nextOf = (code: string, type: string) =>
      (nodes.get(code)?.skipList || []).filter((s: any) => (type ? s.skipType === type : true))
    const build = (code: string, visited: Set<string>): DesignNode | null => {
      const raw = nodes.get(code)
      if (!raw || visited.has(code)) return null
      visited.add(code)
      if (raw.nodeType === 2) return null
      if (raw.nodeType === 0) {
        const nextCode = nextOf(code, 'PASS')[0]?.nextNodeCode
        const node = defaultNode('start')
        node.name = '发起人'
        node.child = nextCode ? build(nextCode, visited) : null
        return node
      }
      if (raw.nodeType === 3 || raw.nodeType === 4 || raw.nodeType === 5) {
        const node = defaultNode('gateway')
        node.kind = 'gateway'
        node.name = raw.nodeName || '条件分支'
        node.gatewayKind = raw.nodeType
        node.branches = (raw.skipList || []).map((skip: any, index: number) => {
          const condition = skip.skipCondition || ''
          const isSpel = condition.startsWith('spel@@')
          const builtin = condition.split('@@')
          const opEntry = operators.find((o) => o.value === builtin[0])
          return {
            id: nextId('branch'),
            name: skip.skipName || '分支' + (index + 1),
            conditionMode: isSpel ? 'spel' : 'builtin',
            op: opEntry?.value || 'gt',
            field: builtin[1]?.split('|')[0] || '',
            value: builtin[1]?.split('|')[1] || '',
            expression: condition,
            child: build(skip.nextNodeCode, visited)
          } as DesignBranch
        })
        return node
      }
      const node = defaultNode('approval')
      node.name = raw.nodeName || '审批节点'
      node.ratio = raw.nodeRatio || '0'
      const flags: string[] = String(raw.permissionFlag || '').split('@@').filter(Boolean)
      if (flags.length === 1 && flags[0] === '${handler}') {
        node.assigneeMode = 'handler'
      } else if (flags.length === 1 && flags[0].startsWith('#')) {
        node.assigneeMode = 'spel'
        node.assigneeExpr = flags[0]
      } else if (flags.length === 1 && flags[0].startsWith('role:')) {
        node.assigneeMode = 'role'
        node.assigneeExpr = flags[0]
      } else if (flags.length === 1 && flags[0].startsWith('dept:')) {
        node.assigneeMode = 'dept'
        node.assigneeExpr = flags[0]
      } else {
        node.assigneeMode = 'user'
        node.assignees = flags
      }
      if (raw.ext) {
        try {
          const ext = typeof raw.ext === 'string' ? JSON.parse(raw.ext) : raw.ext
          node.ext = { ...node.ext, ...ext }
          node.remoteEnabled = !!ext.remote
          node.scriptEnabled = !!ext.script
        } catch (e) {
          /* 忽略非 JSON 扩展 */
        }
      }
      const nextCode = nextOf(code, 'PASS')[0]?.nextNodeCode
      node.child = nextCode ? build(nextCode, visited) : null
      return node
    }
    const startCode = data.nodeList.find((n: any) => n.nodeType === 0)?.nodeCode
    const visited = new Set<string>()
    const built = startCode ? build(startCode, visited) : null
    def.root = built || newNodeChain()
  } catch (e) {
    ElMessage.warning('模型解析失败，已重置为空白流程')
    def.root = newNodeChain()
  }
}

/* ---------------- 对外动作 ---------------- */

function exportJson() {
  try {
    const model = JSON.stringify(compile(), null, 2)
    navigator.clipboard.writeText(model).then(() => ElMessage.success('模型 JSON 已复制到剪贴板'))
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function preview() {
  try {
    await flowDesignPreview(JSON.stringify(compile()))
    ElMessage.success('流程结构校验通过')
  } catch (e: any) {
    ElMessage.error(e.message || '流程结构校验失败')
  }
}

function save() {
  if (!def.flowCode.trim()) {
    ElMessage.warning('请填写流程编码')
    return
  }
  if (!def.flowName.trim()) {
    ElMessage.warning('请填写流程名称')
    return
  }
  try {
    const model = JSON.stringify(compile())
    emit('save', { model, key: def.flowCode, name: def.flowName })
  } catch (e: any) {
    ElMessage.error(e.message || '流程构建失败')
  }
}

onMounted(async () => {
  try {
    const [users, roleList, deptList, formList] = await Promise.all([
      flowCandidates(),
      flowRoles(),
      flowDepts(),
      flowForms()
    ])
    candidates.value = users || []
    roles.value = roleList || []
    depts.value = deptList || []
    forms.value = formList || []
  } catch (e) {
    /* 候选数据加载失败不阻塞设计 */
  }
  if (props.model) {
    hasId.value = true
    parse(props.model)
  }
})
</script>

<style scoped lang="scss">
.flow-designer {
  display: flex;
  gap: 14px;
  height: calc(100vh - 140px);
  min-height: 520px;

  .designer-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #fff;
    border-radius: 14px;
    padding: 0 12px 0;
    box-shadow: 0 2px 12px rgb(31 56 88 / 8%);
    overflow: hidden;

    .canvas-toolbar {
      padding: 12px 8px 12px;
      border-bottom: 1px solid var(--el-border-color-lighter);
      margin-bottom: 0;

      .toolbar-head {
        margin-bottom: 10px;

        .toolbar-title {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          font-weight: 700;
          font-size: 15px;
          color: var(--el-text-color-primary);

          .toolbar-badge {
            width: 26px;
            height: 26px;
            border-radius: 8px;
            background: linear-gradient(135deg, #4f7cff, #7c5cff);
            color: #fff;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
          }

          .toolbar-tip {
            font-weight: 400;
            font-size: 12px;
            color: var(--el-text-color-placeholder);
            margin-left: 4px;
          }
        }
      }
    }

    .canvas-scroll {
      flex: 1;
      overflow: auto;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px 0 48px;
      background-image: radial-gradient(circle, #dfe4ec 1px, transparent 1px);
      background-size: 22px 22px;
      background-position: -1px -1px;
    }
  }

  .start-node,
  .end-node {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 22px 9px 12px;
    border-radius: 24px;
    background: #fff;
    box-shadow: 0 2px 10px rgb(31 56 88 / 12%);
    border: 1px solid var(--el-border-color-lighter);

    .pill-badge {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      color: #fff;
      font-size: 13px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.start {
        background: linear-gradient(135deg, #34c38f, #23b27e);
        box-shadow: 0 2px 6px rgb(52 195 143 / 40%);
      }

      &.end {
        background: linear-gradient(135deg, #ff8f6b, #f56c6c);
        box-shadow: 0 2px 6px rgb(245 108 108 / 40%);
      }
    }

    .pill-text {
      display: flex;
      flex-direction: column;
      line-height: 1.3;

      span {
        font-size: 13px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      small {
        font-size: 11px;
        color: var(--el-text-color-placeholder);
      }
    }
  }

  .designer-props {
    width: 410px;
    flex-shrink: 0;
    background: #fff;
    border-radius: 14px;
    box-shadow: 0 2px 12px rgb(31 56 88 / 8%);
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .props-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 13px 16px;
      background: linear-gradient(135deg, #f2f5ff, #f8f2ff);
      border-bottom: 1px solid var(--el-border-color-lighter);

      .props-title {
        font-weight: 700;
        font-size: 14px;
        color: var(--el-text-color-primary);
        display: inline-flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          border-radius: 2px;
          background: linear-gradient(#4f7cff, #7c5cff);
        }
      }
    }

    .props-body {
      flex: 1;
      padding: 16px 16px 24px;

      :deep(.el-divider__text) {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .assignee-tip,
    .condition-tip {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
      line-height: 1.6;
      padding: 6px 8px;
      background: var(--el-fill-color-lighter);
      border-radius: 6px;
    }

    .condition-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: center;
    }

    .branch-editor {
      padding: 12px 10px 4px;
      background: var(--el-fill-color-lighter);
      border-radius: 10px;
      margin-bottom: 10px;
      border: 1px solid var(--el-border-color-lighter);
    }
  }
}
</style>

<!-- 画布动态节点由 h() 渲染函数生成，无法获得 scoped 属性，此处使用全局样式并以 .flow-designer 前缀隔离 -->
<style lang="scss">
@keyframes lp-flow-menu-pop {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-6px) scale(0.96);
  }

  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}

.flow-designer {
  .connect-line {
    width: 2px;
    height: 28px;
    background: linear-gradient(#c3c9d4, #a9b1c0);
    border-radius: 1px;
  }

  .chain {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .node-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
  }

  .canvas-node {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    width: 262px;
    flex-shrink: 0;
    border-radius: 12px;
    padding: 12px 12px 12px 10px;
    background: #fff;
    border: 1px solid var(--el-border-color-lighter);
    border-left: 4px solid var(--el-color-primary);
    box-shadow: 0 2px 8px rgb(31 56 88 / 8%);
    cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgb(31 56 88 / 16%);

      .canvas-node-edit {
        opacity: 1;
      }
    }

    &.auto {
      border-left-color: var(--el-color-warning);
    }

    &.gateway {
      border-left-color: #7c5cff;
    }

    .canvas-node-icon {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      color: #fff;
      font-size: 16px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.approval {
        background: linear-gradient(135deg, #4f7cff, #6a9bff);
        box-shadow: 0 3px 8px rgb(79 124 255 / 35%);
      }

      &.gateway {
        background: linear-gradient(135deg, #8a63ff, #a78bff);
        box-shadow: 0 3px 8px rgb(138 99 255 / 35%);
      }

      &.auto {
        background: linear-gradient(135deg, #ffb54f, #ff9a3d);
        box-shadow: 0 3px 8px rgb(255 154 61 / 35%);
      }
    }

    .canvas-node-main {
      flex: 1;
      min-width: 0;

      .canvas-node-name {
        font-weight: 600;
        font-size: 13px;
        color: var(--el-text-color-primary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .canvas-node-sub {
        margin-top: 3px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .canvas-node-ext {
        margin-top: 3px;
        font-size: 11px;
        color: var(--el-color-warning);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .canvas-node-edit {
      opacity: 0;
      transition: opacity 0.18s;
      color: var(--el-text-color-placeholder);
      font-size: 13px;
      padding-top: 2px;
    }
  }

  .add-wrap {
    position: relative;
    display: flex;
    justify-content: center;
    margin: 4px 0;

    .add-btn {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      border: 1px dashed #b3bac7;
      background: #fff;
      color: #7a8699;
      font-size: 15px;
      line-height: 1;
      cursor: pointer;
      transition: all 0.18s ease;

      &:hover {
        border: none;
        background: linear-gradient(135deg, #4f7cff, #7c5cff);
        color: #fff;
        transform: scale(1.15);
        box-shadow: 0 3px 10px rgb(79 124 255 / 40%);
      }
    }

    .add-menu {
      position: absolute;
      z-index: 20;
      top: 32px;
      left: 50%;
      transform: translateX(-50%);
      background: #fff;
      border-radius: 10px;
      box-shadow: 0 8px 24px rgb(31 56 88 / 20%);
      border: 1px solid var(--el-border-color-lighter);
      padding: 6px;
      animation: lp-flow-menu-pop 0.16s ease-out;
      transform-origin: top center;

      .add-item {
        display: flex;
        align-items: center;
        gap: 9px;
        padding: 8px 16px;
        border-radius: 7px;
        cursor: pointer;
        white-space: nowrap;
        font-size: 13px;
        color: var(--el-text-color-primary);
        transition: background 0.12s;

        &:hover {
          background: var(--el-color-primary-light-9);
          color: var(--el-color-primary);
        }

        .add-item-icon {
          width: 22px;
          height: 22px;
          border-radius: 6px;
          background: var(--el-color-primary-light-9);
          color: var(--el-color-primary);
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
        }
      }
    }
  }

  .gateway-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;

    .gateway-head {
      position: relative;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: -18px;
        width: 2px;
        height: 16px;
        transform: translateX(-50%);
        background: #b9c0cd;
      }
    }

    .branch-container {
      display: flex;
      align-items: stretch;
      margin-top: 18px;
      padding: 0 14px 4px;
    }

    .branch-column {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 262px;
      padding: 22px 12px 24px;
      position: relative;
      // 顶部与底部的中央竖线，衔接扇形横线与分支内容
      background-image: linear-gradient(#b9c0cd, #b9c0cd), linear-gradient(#b9c0cd, #b9c0cd);
      background-size: 2px 14px, 2px 14px;
      background-position: 50% 2px, 50% calc(100% - 16px);
      background-repeat: no-repeat;

      // 扇形连线：列间分隔线 + 首尾圆角横线
      &::before,
      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        height: 2px;
        background: #b9c0cd;
      }

      &::before {
        top: 0;
      }

      &::after {
        bottom: 0;
      }

      &:first-child::before {
        left: auto;
        right: 50%;
        width: 50%;
        border-top-left-radius: 16px;
        border-left: 2px solid #b9c0cd;
        height: 18px;
        top: -18px;
        background: none;
      }

      &:first-child::after {
        left: auto;
        right: 50%;
        width: 50%;
        border-bottom-left-radius: 16px;
        border-left: 2px solid #b9c0cd;
        height: 18px;
        bottom: -18px;
        background: none;
      }

      &:last-child::before {
        left: 50%;
        right: auto;
        width: 50%;
        border-top-right-radius: 16px;
        border-right: 2px solid #b9c0cd;
        height: 18px;
        top: -18px;
        background: none;
      }

      &:last-child::after {
        left: 50%;
        right: auto;
        width: 50%;
        border-bottom-right-radius: 16px;
        border-right: 2px solid #b9c0cd;
        height: 18px;
        bottom: -18px;
        background: none;
      }

      & + .branch-column {
        border-left: 2px dashed #dfe4ec;
      }

      .branch-title {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 5px 14px;
        border-radius: 14px;
        background: linear-gradient(135deg, #f2f5ff, #f8f2ff);
        border: 1px solid #d9dcff;
        font-size: 12px;
        font-weight: 600;
        color: #5b64c7;
        margin-bottom: 12px;
        max-width: 90%;

        .branch-cond {
          font-weight: 400;
          color: var(--el-text-color-secondary);
          font-size: 11px;
        }
      }

      .branch-empty {
        color: var(--el-text-color-placeholder);
        font-size: 12px;
        padding: 12px 0;
        background: var(--el-fill-color-lighter);
        border: 1px dashed var(--el-border-color);
        border-radius: 10px;
      }
    }
  }
}
</style>
