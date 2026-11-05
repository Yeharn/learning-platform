<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 菜单管理页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="菜单名称">
        <el-input v-model="query.menuName" placeholder="请输入菜单名称" clearable @keyup.enter="getList" />
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
        <el-button v-hasPerm="'system:menu:add'" type="primary" icon="Plus" @click="handleAdd()">新增</el-button>
        <el-button icon="Sort" @click="toggleExpand">展开/折叠</el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="list"
        row-key="menuId"
        border
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 'M' ? 'warning' : row.menuType === 'C' ? 'primary' : 'info'">
              {{ row.menuType === 'M' ? '目录' : row.menuType === 'C' ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="70" align="center" />
        <el-table-column prop="path" label="路由地址" min-width="120" />
        <el-table-column prop="component" label="组件路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-icon v-if="isValidIcon(row.icon)"><component :is="row.icon" /></el-icon>
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
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:menu:edit'" link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:menu:add'" link type="success" @click="handleAdd(row)">新增</el-button>
            <el-button v-hasPerm="'system:menu:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="open" :title="form.menuId ? '修改菜单' : '新增菜单'" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ label: 'menuName', value: 'menuId', children: 'children' }"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型">
              <el-radio-group v-model="form.menuType">
                <el-radio value="M">目录</el-radio>
                <el-radio value="C">菜单</el-radio>
                <el-radio value="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="如 user；外链填 http(s):// 完整地址" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType === 'C'" :span="12">
            <el-form-item label="组件路径">
              <el-input v-model="form.component" placeholder="如 system/user/index" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'M'" :span="12">
            <el-form-item label="权限标识">
              <el-input v-model="form.perms" placeholder="如 system:user:list" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-select v-model="form.icon" filterable clearable placeholder="选择图标" style="width: 100%">
                <el-option v-for="icon in iconList" :key="icon" :label="icon" :value="icon">
                  <span style="display: flex; align-items: center; gap: 8px">
                    <el-icon><component :is="icon" /></el-icon>{{ icon }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import * as ElIcons from '@element-plus/icons-vue'
import { addMenu, delMenu, getMenu, listMenu, updateMenu } from '@/api/system'
import type { SysMenu } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const open = ref(false)
const list = ref<SysMenu[]>([])
const formRef = ref<FormInstance>()
const tableRef = ref<TableInstance>()
const expandAll = ref(true)

const icons = [
  'Setting', 'Monitor', 'Tools', 'User', 'UserFilled', 'Menu', 'OfficeBuilding', 'Collection',
  'Edit', 'Bell', 'Coin', 'Connection', 'Key', 'Document', 'Cpu', 'DataLine', 'MagicStick',
  'Odometer', 'Grid', 'List', 'Tickets', 'Files', 'Folder', 'FolderOpened', 'Star', 'Promotion',
  'ChatDotRound', 'DataAnalysis', 'Database', 'Link', 'Timer', 'Guide', 'Finished', 'Clock', 'Histogram'
]
const iconList = icons

/** 仅当图标名对应真实存在的 Element Plus 图标组件时才渲染，避免非法值（如 "#"）导致 createElement 抛错 */
const iconCache = new Map<string, any>()
function iconComp(name?: string) {
  if (!name) return undefined
  if (iconCache.has(name)) return iconCache.get(name)
  const comp = (ElIcons as Record<string, any>)[name]
  const valid = typeof comp === 'object' || typeof comp === 'function' ? comp : undefined
  iconCache.set(name, valid)
  return valid
}
const isValidIcon = (name?: string) => !!iconComp(name)

const query = reactive({ menuName: '', status: '' })
const form = ref<SysMenu>({})

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

const parentOptions = computed(() => [{ menuId: 0, menuName: '主目录' } as SysMenu, ...list.value])

async function getList() {
  loading.value = true
  try {
    list.value = await listMenu(query)
    // 数据刷新后表格重新初始化，按当前展开状态同步一次
    await nextTick()
    if (!expandAll.value) setAllExpanded(false)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.menuName = ''
  query.status = ''
  getList()
}

function handleAdd(row?: SysMenu) {
  form.value = {
    parentId: row?.menuId || 0,
    menuType: row?.menuType === 'C' ? 'F' : 'C',
    orderNum: 1,
    visible: '0',
    status: '0',
    isFrame: 0,
    isCache: 1
  }
  open.value = true
}

async function handleEdit(row: SysMenu) {
  form.value = await getMenu(row.menuId)
  open.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.value.menuId) await updateMenu(form.value)
    else await addMenu(form.value)
    ElMessage.success('保存成功')
    open.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysMenu) {
  await ElMessageBox.confirm(`确认删除菜单【${row.menuName}】?`, '提示', { type: 'warning' })
  await delMenu(row.menuId)
  ElMessage.success('删除成功')
  getList()
}

/** 递归收集所有含子节点的行，供展开/折叠使用 */
function collectParentRows(rows: SysMenu[], acc: SysMenu[] = []): SysMenu[] {
  for (const row of rows) {
    const children = (row as any).children as SysMenu[] | undefined
    if (children && children.length) {
      acc.push(row)
      collectParentRows(children, acc)
    }
  }
  return acc
}

function setAllExpanded(expanded: boolean) {
  collectParentRows(list.value).forEach((row) => tableRef.value?.toggleRowExpansion(row, expanded))
}

function toggleExpand() {
  expandAll.value = !expandAll.value
  setAllExpanded(expandAll.value)
}

onMounted(getList)
</script>
