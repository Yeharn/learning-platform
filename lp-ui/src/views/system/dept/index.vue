<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 部门管理页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="部门名称">
        <el-input v-model="query.deptName" placeholder="请输入部门名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button v-hasPerm="'system:dept:add'" type="primary" icon="Plus" @click="handleAdd()">新增</el-button>
      </div>

      <el-table v-loading="loading" :data="list" row-key="deptId" border default-expand-all>
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:dept:edit'" link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:dept:add'" link type="success" @click="handleAdd(row)">新增</el-button>
            <el-button v-hasPerm="'system:dept:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="open" :title="form.deptId ? '修改部门' : '新增部门'" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="显示排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addDept, delDept, getDept, listDept, updateDept } from '@/api/system'
import type { SysDept } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const open = ref(false)
const list = ref<SysDept[]>([])
const formRef = ref<FormInstance>()

const query = reactive({ deptName: '', status: '' })
const form = ref<SysDept>({})

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

const parentOptions = computed(() => [{ deptId: 0, deptName: '顶级部门' } as SysDept, ...list.value])

async function getList() {
  loading.value = true
  try {
    list.value = await listDept(query)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.deptName = ''
  query.status = ''
  getList()
}

function handleAdd(row?: SysDept) {
  form.value = { parentId: row?.deptId || 0, orderNum: 1, status: '0' }
  open.value = true
}

async function handleEdit(row: SysDept) {
  form.value = await getDept(row.deptId)
  open.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.value.deptId) await updateDept(form.value)
    else await addDept(form.value)
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysDept) {
  await ElMessageBox.confirm(`确认删除部门【${row.deptName}】?`, '提示', { type: 'warning' })
  await delDept(row.deptId)
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>
