<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 流程定义：列表查询、可视化设计器建模、部署、发起与删除
-->

<template>
  <div class="app-container">
    <el-form :inline="true" class="search-form">
      <el-form-item label="流程名称">
        <el-input v-model="keyword" placeholder="流程名称 / KEY" clearable style="width: 220px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="load">搜索</el-button>
        <el-button icon="Refresh" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button type="primary" icon="Plus" v-hasPerm="['tool:flow:deploy']" @click="openCreate">新建流程</el-button>
        <el-button icon="Document" v-hasPerm="['tool:flow:form']" @click="openForms">表单设计</el-button>
        <el-button icon="Upload" v-hasPerm="['tool:flow:deploy']" @click="openImport">导入 JSON</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="name" label="流程名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="key" label="流程 KEY" min-width="140" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" v-hasPerm="['tool:flow:deploy']" @click="openDesign(row)">
              设计
            </el-button>
            <el-button link type="primary" icon="Promotion" v-hasPerm="['tool:flow:start']" @click="openStart(row)">
              发起
            </el-button>
            <el-button link type="danger" icon="Delete" v-hasPerm="['tool:flow:remove']" @click="handleRemove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="designerVisible" :title="designerTitle" size="82%" destroy-on-close>
      <FlowDesigner :model="designerModel" :saving="saving" @save="handleDesignerSave" />
    </el-drawer>

    <el-dialog v-model="startVisible" title="发起流程" width="700px">
      <el-form label-width="100px">
        <el-form-item label="流程名称">
          <el-input :model-value="startRow?.name" disabled />
        </el-form-item>
      </el-form>
      <template v-if="startFields.length">
        <el-divider content-position="left">表单信息</el-divider>
        <FlowForm :fields="startFields" :model="startModel" />
      </template>
      <el-form label-width="100px">
        <el-form-item label="申请说明">
          <el-input v-model="startRemark" type="textarea" :rows="2" placeholder="请输入申请说明" />
        </el-form-item>
        <el-form-item label="附加参数">
          <el-input v-model="startExtra" type="textarea" :rows="2" placeholder='可选 JSON，如 {"deptId":100}，将随流程变量传递' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" :loading="startLoading" @click="handleStart">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入流程模型 JSON" width="620px">
      <el-input v-model="importJson" type="textarea" :rows="12" placeholder="粘贴 Warm-Flow DefJson 模型 JSON" />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport">开始设计</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="formsVisible" title="流程表单模板" size="70%" destroy-on-close>
      <div class="mb8">
        <el-button type="primary" icon="Plus" @click="openFormEditor()">新建表单</el-button>
      </div>
      <el-table :data="formList" v-loading="formLoading" border>
        <el-table-column prop="formCode" label="表单编码" min-width="140" />
        <el-table-column prop="formName" label="表单名称" min-width="140" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column label="字段数" width="80" align="center">
          <template #default="{ row }">{{ (row.fields || []).length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="openFormEditor(row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleRemoveForm(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="formEditorVisible" :title="formModel.id ? '编辑表单' : '新建表单'" width="760px" append-to-body>
        <el-form label-width="90px">
          <el-form-item label="表单编码">
            <el-input v-model="formModel.formCode" :disabled="!!formModel.id" placeholder="如 expense_form" />
          </el-form-item>
          <el-form-item label="表单名称">
            <el-input v-model="formModel.formName" placeholder="如 费用报销单" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="formModel.remark" />
          </el-form-item>
          <el-form-item label="表单字段">
            <el-table :data="formModel.fields" border size="small">
              <el-table-column label="字段标识" width="150">
                <template #default="{ row }">
                  <el-input v-model="row.key" placeholder="key" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="标签" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.label" placeholder="label" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="类型" width="140">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small">
                    <el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="70" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.required" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="栅格" width="90" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.span" :min="6" :max="24" :step="6" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" icon="Delete" size="small" @click="formModel.fields.splice($index, 1)" />
                </template>
              </el-table-column>
            </el-table>
            <el-button plain icon="Plus" size="small" class="mt8" @click="addField">添加字段</el-button>
          </el-form-item>
          <el-form-item label="附加字段">
            <el-input v-model="formExtJson" type="textarea" :rows="3" placeholder='可选 JSON 数组，发起页固定展示的附加参数定义，如 [{"key":"deptId","label":"部门","type":"number","required":false,"span":12,"options":[]}]' />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="formEditorVisible = false">取消</el-button>
          <el-button type="primary" :loading="formSaving" @click="handleSaveForm">保存</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlowDesigner from '@/components/FlowDesigner/index.vue'
import FlowForm from '@/components/FlowForm/index.vue'
import {
  deployFlowJson,
  flowDefinitions,
  flowFormByCode,
  flowForms,
  flowModel,
  flowRemove,
  flowStart,
  removeFlowForm,
  saveFlowForm
} from '@/api/flow'

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')

const designerVisible = ref(false)
const designerModel = ref('')
const currentId = ref<string | number | undefined>(undefined)
const currentName = ref('')
const saving = ref(false)

const startVisible = ref(false)
const startRow = ref<any>(null)
const startRemark = ref('')
const startLoading = ref(false)
const startFields = ref<any[]>([])
const startModel = ref<Record<string, any>>({})
const startExtra = ref('')

const importVisible = ref(false)
const importJson = ref('')

const formsVisible = ref(false)
const formList = ref<any[]>([])
const formLoading = ref(false)
const formEditorVisible = ref(false)
const formSaving = ref(false)
const formModel = ref<{ id?: number; formCode: string; formName: string; remark?: string; fields: any[] }>({
  formCode: '',
  formName: '',
  remark: '',
  fields: []
})
const formExtJson = ref('')
const fieldTypes = ['input', 'textarea', 'number', 'amount', 'select', 'radio', 'checkbox', 'date', 'switch', 'attachment']

const designerTitle = computed(() => (currentId.value ? '流程设计 - ' + currentName.value : '新建流程'))

async function load() {
  loading.value = true
  try {
    list.value = (await flowDefinitions(keyword.value)) as any
  } finally {
    loading.value = false
  }
}

function openCreate() {
  currentId.value = undefined
  currentName.value = ''
  designerModel.value = ''
  designerVisible.value = true
}

async function openDesign(row: any) {
  currentId.value = row.id
  currentName.value = row.name
  designerModel.value = (await flowModel(row.id)) as any
  designerVisible.value = true
}

async function handleDesignerSave(payload: { model: string; key: string; name: string }) {
  saving.value = true
  try {
    await deployFlowJson({ id: currentId.value, model: payload.model, remark: payload.name })
    ElMessage.success('流程部署成功')
    designerVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openImport() {
  importJson.value = ''
  importVisible.value = true
}

function handleImport() {
  if (!importJson.value) {
    ElMessage.warning('请粘贴流程模型 JSON')
    return
  }
  try {
    JSON.parse(importJson.value)
  } catch (e) {
    ElMessage.error('JSON 格式不正确')
    return
  }
  currentId.value = undefined
  currentName.value = ''
  designerModel.value = importJson.value
  importVisible.value = false
  designerVisible.value = true
}

async function openStart(row: any) {
  startRow.value = row
  startRemark.value = ''
  startExtra.value = ''
  startFields.value = []
  startModel.value = {}
  startVisible.value = true
  try {
    const json = await flowModel(row.id)
    const data = JSON.parse(String(json))
    const formCode = data?.formPath
    if (formCode) {
      const form = await flowFormByCode(formCode)
      const fields = form?.fields || []
      if (fields.length) {
        startFields.value = fields
        const model: Record<string, any> = {}
        fields.forEach((field: any) => {
          if (field.type === 'checkbox' || field.type === 'attachment') {
            model[field.key] = []
          } else if (field.type === 'number' || field.type === 'amount') {
            model[field.key] = undefined
          } else if (field.type === 'switch') {
            model[field.key] = false
          } else {
            model[field.key] = ''
          }
        })
        startModel.value = model
      }
    }
  } catch (e) {
    startFields.value = []
  }
}

async function handleStart() {
  const missing = startFields.value.find((field) => {
    if (!field.required) {
      return false
    }
    const value = startModel.value[field.key]
    return value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)
  })
  if (missing) {
    ElMessage.warning(`请填写「${missing.label}」`)
    return
  }
  const variables: Record<string, any> = { remark: startRemark.value, ...startModel.value }
  if (startExtra.value.trim()) {
    try {
      Object.assign(variables, JSON.parse(startExtra.value))
    } catch (e) {
      ElMessage.error('附加参数必须是合法 JSON')
      return
    }
  }
  startLoading.value = true
  try {
    await flowStart({ processId: startRow.value.id, variables })
    ElMessage.success('流程已发起')
    startVisible.value = false
  } finally {
    startLoading.value = false
  }
}

async function handleRemove(row: any) {
  await ElMessageBox.confirm(`确认删除流程【${row.name}】?`, '提示', { type: 'warning' })
  await flowRemove(row.id)
  ElMessage.success('删除成功')
  load()
}

/* ---------------- 表单模板管理 ---------------- */

async function loadForms() {
  formLoading.value = true
  try {
    formList.value = (await flowForms()) as any
  } finally {
    formLoading.value = false
  }
}

function openForms() {
  formsVisible.value = true
  loadForms()
}

function openFormEditor(row?: any) {
  if (row) {
    formModel.value = {
      id: row.id,
      formCode: row.formCode,
      formName: row.formName,
      remark: row.remark,
      fields: JSON.parse(JSON.stringify(row.fields || []))
    }
    formExtJson.value = row.ext && row.ext.length ? JSON.stringify(row.ext, null, 2) : ''
  } else {
    formModel.value = { formCode: '', formName: '', remark: '', fields: [] }
    formExtJson.value = ''
  }
  formEditorVisible.value = true
}

function addField() {
  formModel.value.fields.push({ key: '', label: '', type: 'input', required: false, span: 12, options: [] })
}

async function handleSaveForm() {
  if (!formModel.value.formCode.trim()) {
    ElMessage.warning('请填写表单编码')
    return
  }
  if (!formModel.value.formName.trim()) {
    ElMessage.warning('请填写表单名称')
    return
  }
  let ext: any = []
  if (formExtJson.value.trim()) {
    try {
      ext = JSON.parse(formExtJson.value)
    } catch (e) {
      ElMessage.error('附加字段必须是合法 JSON 数组')
      return
    }
  }
  formSaving.value = true
  try {
    await saveFlowForm({ ...formModel.value, ext })
    ElMessage.success('表单保存成功')
    formEditorVisible.value = false
    loadForms()
  } finally {
    formSaving.value = false
  }
}

async function handleRemoveForm(row: any) {
  await ElMessageBox.confirm(`确认删除表单【${row.formName}】?`, '提示', { type: 'warning' })
  await removeFlowForm(row.id)
  ElMessage.success('删除成功')
  loadForms()
}

onMounted(load)
</script>
