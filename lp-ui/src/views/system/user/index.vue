<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 用户管理页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="账号">
        <el-input v-model="query.username" placeholder="请输入账号" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="请输入手机号" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-tree-select
          v-model="query.deptId"
          :data="depts"
          :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
          check-strictly
          clearable
          placeholder="全部部门"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button v-hasPerm="'system:user:add'" type="primary" icon="Plus" @click="handleAdd">新增</el-button>
        <el-button v-hasPerm="'system:user:export'" icon="Download" @click="handleExport">导出</el-button>
        <el-button
          v-hasPerm="'system:user:remove'"
          type="danger"
          plain
          icon="Delete"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >
          删除
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="username" label="账号" min-width="110" />
        <el-table-column prop="nickname" label="昵称" min-width="110" />
        <el-table-column prop="deptName" label="部门" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="角色" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.roleNames || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:user:edit'" link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:user:resetPwd'" link type="warning" @click="handleResetPwd(row)">
              重置密码
            </el-button>
            <el-button v-hasPerm="'system:user:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <el-dialog v-model="open" :title="form.userId ? '修改用户' : '新增用户'" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" :disabled="!!form.userId" placeholder="请输入登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.userId" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="默认 admin123" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-tree-select
                v-model="form.deptId"
                :data="depts"
                :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.sex" style="width: 100%">
                <el-option label="男" value="0" />
                <el-option label="女" value="1" />
                <el-option label="未知" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="请选择角色">
                <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addUser,
  changeUserStatus,
  delUser,
  deptTreeSelect,
  exportUser,
  getUser,
  listUser,
  resetUserPwd,
  roleOptions,
  updateUser
} from '@/api/system'
import type { SysDept, SysRole, SysUser } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const open = ref(false)
const list = ref<SysUser[]>([])
const total = ref(0)
const selection = ref<SysUser[]>([])
const depts = ref<SysDept[]>([])
const roles = ref<SysRole[]>([])
const formRef = ref<FormInstance>()

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  phone: '',
  status: '',
  deptId: undefined as any
})

const form = ref<SysUser>({})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const res = await listUser(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.phone = ''
  query.status = ''
  query.deptId = undefined
  query.pageNum = 1
  getList()
}

function onSelectionChange(rows: SysUser[]) {
  selection.value = rows
}

function handleAdd() {
  form.value = { status: '0', sex: '2', roleIds: [], deptId: undefined }
  open.value = true
}

async function handleEdit(row: SysUser) {
  form.value = await getUser(row.userId as any)
  form.value.roleIds = form.value.roleIds || []
  open.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.value.userId) {
      await updateUser(form.value)
    } else {
      await addUser(form.value)
    }
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysUser) {
  await ElMessageBox.confirm(`确认删除用户【${row.username}】?`, '提示', { type: 'warning' })
  await delUser([row.userId as any])
  ElMessage.success('删除成功')
  getList()
}

async function handleBatchDelete() {
  const ids = selection.value.map((i) => i.userId as any)
  await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个用户?`, '提示', { type: 'warning' })
  await delUser(ids)
  ElMessage.success('删除成功')
  getList()
}

async function handleStatusChange(row: SysUser) {
  try {
    await changeUserStatus(row.userId, row.status as string)
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = row.status === '0' ? '1' : '0'
  }
}

async function handleResetPwd(row: SysUser) {
  const { value } = await ElMessageBox.prompt(`请输入用户【${row.username}】的新密码`, '重置密码', {
    inputType: 'password',
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度需 6-20 位',
    inputValue: 'admin123'
  })
  await resetUserPwd(row.userId, value)
  ElMessage.success('密码重置成功')
}

async function handleExport() {
  const blob = await exportUser(query)
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'users.csv'
  link.click()
  window.URL.revokeObjectURL(url)
}

async function loadOptions() {
  const [deptData, roleData] = await Promise.all([deptTreeSelect(), roleOptions()])
  depts.value = deptData
  roles.value = roleData
}

onMounted(() => {
  getList()
  loadOptions()
})
</script>
