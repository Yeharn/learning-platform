<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 角色管理页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="角色名称">
        <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="权限字符">
        <el-input v-model="query.roleKey" placeholder="请输入权限字符" clearable @keyup.enter="getList" />
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
        <el-button v-hasPerm="'system:role:add'" type="primary" icon="Plus" @click="handleAdd">新增</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="roleId" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="roleKey" label="权限字符" min-width="120" />
        <el-table-column prop="roleSort" label="排序" width="80" align="center" />
        <el-table-column label="数据权限" width="140">
          <template #default="{ row }">{{ scopeLabel(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" active-value="0" inactive-value="1" @change="handleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:role:edit'" link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:role:auth'" link type="warning" @click="handleAuth(row)">分配权限</el-button>
            <el-button v-hasPerm="'system:role:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <!-- 新增/修改 -->
    <el-dialog v-model="open" :title="form.roleId ? '修改角色' : '新增角色'" width="580px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 system:admin" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="数据权限">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="item in scopes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="authOpen" title="分配权限" width="680px" append-to-body>
      <div class="auth-body">
        <div class="auth-header">
          <span>角色：{{ currentRole?.roleName }}</span>
          <div>
            <el-checkbox v-model="expandAll" @change="toggleExpand">展开/折叠</el-checkbox>
            <el-checkbox v-model="checkAll" @change="toggleCheckAll">全选/全不选</el-checkbox>
            <el-checkbox v-model="strictly">父子联动</el-checkbox>
          </div>
        </div>

        <el-scrollbar height="420px" class="tree-scroll">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="menuId"
            show-checkbox
            :check-strictly="!strictly"
            :default-expand-all="expandAll"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <el-tag size="small" :type="tagType(data.menuType)" effect="plain">
                  {{ typeLabel(data.menuType) }}
                </el-tag>
                <span>{{ data.menuName }}</span>
                <span v-if="data.perms" class="perms">{{ data.perms }}</span>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>

        <div v-if="currentRole?.dataScope === '2'" class="dept-scope">
          <div class="auth-header"><span>自定义数据权限部门</span></div>
          <el-tree
            ref="deptTreeRef"
            :data="deptTree"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="deptId"
            show-checkbox
            default-expand-all
            check-strictly
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="authOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAuth">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addRole,
  authRole,
  changeRoleStatus,
  delRole,
  deptTreeSelect,
  getRole,
  listRole,
  menuTreeSelect,
  updateRole
} from '@/api/system'
import type { SysDept, SysMenu, SysRole } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const open = ref(false)
const authOpen = ref(false)
const list = ref<SysRole[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const menuTreeRef = ref<any>()
const deptTreeRef = ref<any>()
const menuTree = ref<SysMenu[]>([])
const deptTree = ref<SysDept[]>([])
const currentRole = ref<SysRole>()
const expandAll = ref(true)
const checkAll = ref(false)
const strictly = ref(true)

const scopes = [
  { label: '全部数据', value: '1' },
  { label: '自定义部门', value: '2' },
  { label: '本部门', value: '3' },
  { label: '本部门及以下', value: '4' },
  { label: '仅本人', value: '5' }
]

const query = reactive({ pageNum: 1, pageSize: 10, roleName: '', roleKey: '', status: '' })
const form = ref<SysRole>({})

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

function scopeLabel(value?: string) {
  return scopes.find((s) => s.value === value)?.label || '-'
}

function typeLabel(type?: string) {
  return type === 'M' ? '目录' : type === 'C' ? '菜单' : '按钮'
}

function tagType(type?: string) {
  return type === 'M' ? 'warning' : type === 'C' ? 'primary' : 'info'
}

async function getList() {
  loading.value = true
  try {
    const res = await listRole(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.roleName = ''
  query.roleKey = ''
  query.status = ''
  query.pageNum = 1
  getList()
}

function handleAdd() {
  form.value = { roleSort: 1, status: '0', dataScope: '1' }
  open.value = true
}

async function handleEdit(row: SysRole) {
  form.value = await getRole(row.roleId)
  open.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.value.roleId) await updateRole(form.value)
    else await addRole(form.value)
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysRole) {
  await ElMessageBox.confirm(`确认删除角色【${row.roleName}】?`, '提示', { type: 'warning' })
  await delRole([row.roleId as any])
  ElMessage.success('删除成功')
  getList()
}

async function handleStatus(row: SysRole) {
  try {
    await changeRoleStatus(row.roleId, row.status as string)
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = row.status === '0' ? '1' : '0'
  }
}

async function handleAuth(row: SysRole) {
  currentRole.value = await getRole(row.roleId)
  const checked = currentRole.value.menuIds || []
  authOpen.value = true
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(strictly.value ? checked : checked)
  await nextTick()
  deptTreeRef.value?.setCheckedKeys(currentRole.value.deptIds || [])
}

function toggleExpand(val: boolean) {
  const nodes = menuTreeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((node: any) => {
    node.expanded = val
  })
}

function toggleCheckAll(val: boolean) {
  menuTreeRef.value?.setCheckedKeys(val ? getAllKeys(menuTree.value) : [])
}

function getAllKeys(nodes: SysMenu[]): any[] {
  const keys: any[] = []
  const walk = (items: SysMenu[]) => {
    items.forEach((i) => {
      keys.push(i.menuId)
      if (i.children?.length) walk(i.children)
    })
  }
  walk(nodes)
  return keys
}

async function submitAuth() {
  submitting.value = true
  try {
    const menuIds = [...(menuTreeRef.value?.getCheckedKeys(false) || []), ...(menuTreeRef.value?.getHalfCheckedKeys() || [])]
    const deptIds = deptTreeRef.value?.getCheckedKeys(false) || []
    await authRole({ roleId: currentRole.value?.roleId, menuIds, deptIds })
    ElMessage.success('权限已保存，相关用户重新登录后生效')
    authOpen.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  getList()
  const [menus, depts] = await Promise.all([menuTreeSelect(), deptTreeSelect()])
  menuTree.value = menus
  deptTree.value = depts
})
</script>

<style scoped lang="scss">
.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #4b5563;
  padding: 8px 0;
}

.tree-scroll {
  border: 1px solid #eef2f7;
  border-radius: 6px;
  padding: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;

  .perms {
    color: #9ca3af;
    font-size: 12px;
  }
}

.dept-scope {
  margin-top: 12px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 8px;
  max-height: 260px;
  overflow: auto;
}
</style>
