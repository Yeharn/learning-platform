<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 字典管理页面
-->

<template>
  <div class="app-container">
    <el-card shadow="never" class="block">
      <template #header>
        <div class="flex-between">
          <span>字典类型</span>
          <div>
            <el-input
              v-model="typeQuery.dictType"
              placeholder="字典类型"
              clearable
              style="width: 160px; margin-right: 8px"
              @keyup.enter="getTypes"
            />
            <el-button type="primary" icon="Search" @click="getTypes">搜索</el-button>
            <el-button v-hasPerm="'system:dict:add'" type="primary" plain icon="Plus" @click="handleAddType">新增</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="typeLoading" :data="types" border @row-click="selectType">
        <el-table-column prop="dictName" label="字典名称" min-width="140" />
        <el-table-column prop="dictType" label="字典类型" min-width="160" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="selectType(row)">字典数据</el-button>
            <el-button v-hasPerm="'system:dict:edit'" link type="warning" @click.stop="handleEditType(row)">修改</el-button>
            <el-button v-hasPerm="'system:dict:remove'" link type="danger" @click.stop="handleDeleteType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="typeQuery.pageNum"
        v-model:page-size="typeQuery.pageSize"
        :total="typeTotal"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="getTypes"
      />
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="flex-between">
          <span>
            字典数据
            <el-tag v-if="currentType" size="small" type="primary" style="margin-left: 8px">
              {{ currentType.dictName }} / {{ currentType.dictType }}
            </el-tag>
          </span>
          <el-button
            v-hasPerm="'system:dict:add'"
            type="primary"
            plain
            icon="Plus"
            :disabled="!currentType"
            @click="handleAddData"
          >
            新增数据
          </el-button>
        </div>
      </template>

      <el-table v-loading="dataLoading" :data="dictData" border>
        <el-table-column prop="dictSort" label="排序" width="70" align="center" />
        <el-table-column prop="dictLabel" label="字典标签" min-width="140" />
        <el-table-column prop="dictValue" label="字典键值" min-width="120" />
        <el-table-column label="默认" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 'Y'" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:dict:edit'" link type="primary" @click="handleEditData(row)">修改</el-button>
            <el-button v-hasPerm="'system:dict:remove'" link type="danger" @click="handleDeleteData(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeOpen" :title="typeForm.dictId ? '修改字典类型' : '新增字典类型'" width="520px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如 sys_user_sex" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataOpen" :title="dataForm.dictCode ? '修改字典数据' : '新增字典数据'" width="520px">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型">
          <el-input :value="dataForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="数据标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入键值" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="dataForm.isDefault">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataOpen = false">取消</el-button>
        <el-button type="primary" @click="submitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addDictData,
  addDictType,
  delDictData,
  delDictType,
  listDictData,
  listDictType,
  updateDictData,
  updateDictType
} from '@/api/system'
import type { SysDictData, SysDictType } from '@/types'

const typeLoading = ref(false)
const dataLoading = ref(false)
const types = ref<SysDictType[]>([])
const dictData = ref<SysDictData[]>([])
const typeTotal = ref(0)
const currentType = ref<SysDictType>()
const typeOpen = ref(false)
const dataOpen = ref(false)
const typeFormRef = ref<FormInstance>()
const dataFormRef = ref<FormInstance>()

const typeQuery = reactive({ pageNum: 1, pageSize: 10, dictName: '', dictType: '' })
const typeForm = ref<SysDictType>({})
const dataForm = ref<SysDictData>({})

const typeRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入数据标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入数据键值', trigger: 'blur' }]
}

async function getTypes() {
  typeLoading.value = true
  try {
    const res = await listDictType(typeQuery)
    types.value = res.rows
    typeTotal.value = Number(res.total)
    if (!currentType.value && res.rows.length) selectType(res.rows[0])
  } finally {
    typeLoading.value = false
  }
}

async function selectType(row: SysDictType) {
  currentType.value = row
  await getData()
}

async function getData() {
  if (!currentType.value) return
  dataLoading.value = true
  try {
    const res = await listDictData({ pageNum: 1, pageSize: 100, dictType: currentType.value.dictType })
    dictData.value = res.rows
  } finally {
    dataLoading.value = false
  }
}

function handleAddType() {
  typeForm.value = { status: '0' }
  typeOpen.value = true
}

function handleEditType(row: SysDictType) {
  typeForm.value = { ...row }
  typeOpen.value = true
}

async function submitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (typeForm.value.dictId) await updateDictType(typeForm.value)
  else await addDictType(typeForm.value)
  ElMessage.success('保存成功')
  typeOpen.value = false
  getTypes()
}

async function handleDeleteType(row: SysDictType) {
  await ElMessageBox.confirm(`确认删除字典类型【${row.dictName}】及其数据?`, '提示', { type: 'warning' })
  await delDictType([row.dictId as any])
  ElMessage.success('删除成功')
  if (currentType.value?.dictId === row.dictId) {
    currentType.value = undefined
    dictData.value = []
  }
  getTypes()
}

function handleAddData() {
  dataForm.value = { dictType: currentType.value?.dictType, dictSort: 1, status: '0', isDefault: 'N' }
  dataOpen.value = true
}

function handleEditData(row: SysDictData) {
  dataForm.value = { ...row }
  dataOpen.value = true
}

async function submitData() {
  const valid = await dataFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (dataForm.value.dictCode) await updateDictData(dataForm.value)
  else await addDictData(dataForm.value)
  ElMessage.success('保存成功')
  dataOpen.value = false
  getData()
}

async function handleDeleteData(row: SysDictData) {
  await ElMessageBox.confirm(`确认删除字典数据【${row.dictLabel}】?`, '提示', { type: 'warning' })
  await delDictData([row.dictCode as any])
  ElMessage.success('删除成功')
  getData()
}

onMounted(getTypes)
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
</style>
