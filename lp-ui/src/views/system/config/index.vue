<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 参数设置页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="参数名称">
        <el-input v-model="query.configName" placeholder="请输入参数名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="参数键名">
        <el-input v-model="query.configKey" placeholder="请输入参数键名" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button v-hasPerm="'system:config:add'" type="primary" icon="Plus" @click="handleAdd">新增</el-button>
        <el-button v-hasPerm="'system:config:edit'" type="warning" plain icon="RefreshRight" @click="handleRefreshCache">
          刷新缓存
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="configId" label="ID" width="70" />
        <el-table-column prop="configName" label="参数名称" min-width="160" />
        <el-table-column prop="configKey" label="参数键名" min-width="180" />
        <el-table-column prop="configValue" label="参数键值" min-width="160" show-overflow-tooltip />
        <el-table-column label="内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.configType === 'Y'" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:config:edit'" link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:config:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="open" :title="form.configId ? '修改参数' : '新增参数'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 sys.user.initPassword" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="系统内置">
          <el-radio-group v-model="form.configType">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addConfig, delConfig, getConfig, listConfig, refreshConfigCache, updateConfig } from '@/api/system'
import type { SysConfig } from '@/types'

const loading = ref(false)
const open = ref(false)
const list = ref<SysConfig[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

const query = reactive({ pageNum: 1, pageSize: 10, configName: '', configKey: '' })
const form = ref<SysConfig>({})

const rules: FormRules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const res = await listConfig(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.configName = ''
  query.configKey = ''
  query.pageNum = 1
  getList()
}

function handleAdd() {
  form.value = { configType: 'N' }
  open.value = true
}

async function handleEdit(row: SysConfig) {
  form.value = await getConfig(row.configId)
  open.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.value.configId) await updateConfig(form.value)
  else await addConfig(form.value)
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

async function handleDelete(row: SysConfig) {
  await ElMessageBox.confirm(`确认删除参数【${row.configName}】?`, '提示', { type: 'warning' })
  await delConfig([row.configId as any])
  ElMessage.success('删除成功')
  getList()
}

async function handleRefreshCache() {
  await refreshConfigCache()
  ElMessage.success('缓存已刷新')
}

onMounted(getList)
</script>
