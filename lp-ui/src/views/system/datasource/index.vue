<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 数据源管理页面
-->

<template>
  <div class="app-container">
    <div class="table-card">
      <div class="mb8 flex-between">
        <div>
          <el-button v-hasPerm="'system:datasource:add'" type="primary" icon="Plus" @click="handleAdd">新增数据源</el-button>
          <el-button icon="Refresh" @click="getList">刷新</el-button>
        </div>
        <span class="text-muted">动态数据源用于代码生成、外部库查询，可在 Service 上使用 @DS 注解切换</span>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="dsKey" label="标识" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.dsKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dbType" label="类型" width="100" />
        <el-table-column prop="url" label="连接地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="passwordMask" label="密码" width="100" />
        <el-table-column label="连接池" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.connected ? 'success' : 'info'" size="small">
              {{ row.connected ? '已加载' : '未加载' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="'system:datasource:test'" link type="success" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" @click="showPool(row)">连接池</el-button>
            <el-button
              v-hasPerm="'system:datasource:edit'"
              link
              type="warning"
              :disabled="row.dsKey === 'master'"
              @click="handleEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-hasPerm="'system:datasource:remove'"
              link
              type="danger"
              :disabled="row.dsKey === 'master'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="open" :title="form.id ? '修改数据源' : '新增数据源'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="如 业务库" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标识" prop="dsKey">
              <el-input v-model="form.dsKey" :disabled="!!form.id" placeholder="如 biz" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据库类型">
              <el-select v-model="form.dbType" style="width: 100%" @change="onDbTypeChange">
                <el-option label="MySQL" value="mysql" />
                <el-option label="PostgreSQL" value="postgresql" />
                <el-option label="Oracle" value="oracle" />
                <el-option label="SQL Server" value="sqlserver" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="驱动类">
              <el-input v-model="form.driverClass" placeholder="驱动类全名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="连接地址" prop="url">
              <el-input v-model="form.url" placeholder="jdbc:mysql://127.0.0.1:3306/db?useUnicode=true" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="数据库用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password placeholder="不填则不修改" />
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
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="success" :loading="testing" @click="testForm">测试连接</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="poolOpen" title="连接池状态" size="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="连接池">{{ pool.poolName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ pool.jdbcUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="活跃连接">{{ pool.active ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="空闲连接">{{ pool.idle ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="总连接">{{ pool.total ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="等待线程">{{ pool.waiting ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="最大连接">{{ pool.max ?? 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-if="!pool.poolName" description="该数据源未加载连接池" :image-size="70" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addDatasource,
  datasourcePool,
  delDatasource,
  listDatasource,
  testDatasource,
  updateDatasource
} from '@/api/system'
import type { SysDatasource } from '@/types'

const loading = ref(false)
const testing = ref(false)
const open = ref(false)
const poolOpen = ref(false)
const list = ref<SysDatasource[]>([])
const formRef = ref<FormInstance>()
const form = ref<SysDatasource>({})
const pool = ref<any>({})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dsKey: [{ required: true, message: '请输入标识', trigger: 'blur' }],
  url: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const drivers: Record<string, string> = {
  mysql: 'com.mysql.cj.jdbc.Driver',
  postgresql: 'org.postgresql.Driver',
  oracle: 'oracle.jdbc.OracleDriver',
  sqlserver: 'com.microsoft.sqlserver.jdbc.SQLServerDriver'
}

async function getList() {
  loading.value = true
  try {
    list.value = await listDatasource()
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  form.value = { dbType: 'mysql', driverClass: drivers.mysql, status: '0' }
  open.value = true
}

function handleEdit(row: SysDatasource) {
  form.value = { ...row, password: '' }
  open.value = true
}

function onDbTypeChange(val: string) {
  form.value.driverClass = drivers[val]
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.value.id) await updateDatasource(form.value)
  else await addDatasource(form.value)
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

async function testForm() {
  testing.value = true
  try {
    const msg = await testDatasource(form.value)
    ElMessage.success(msg)
  } finally {
    testing.value = false
  }
}

async function handleTest(row: SysDatasource) {
  const msg = await testDatasource({ ...row, password: '' })
  ElMessage.success(msg)
}

async function handleDelete(row: SysDatasource) {
  await ElMessageBox.confirm(`确认删除数据源【${row.name}】?`, '提示', { type: 'warning' })
  await delDatasource(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function showPool(row: SysDatasource) {
  pool.value = (await datasourcePool(row.dsKey as string)) || {}
  poolOpen.value = true
}

onMounted(getList)
</script>
