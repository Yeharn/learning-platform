<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 通知公告页面
-->

<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="公告标题">
        <el-input v-model="query.noticeTitle" placeholder="请输入标题" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 120px">
          <el-option label="通知" value="1" />
          <el-option label="公告" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <div class="mb8">
        <el-button v-hasPerm="'system:notice:add'" type="primary" icon="Plus" @click="handleAdd">新增</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="noticeId" label="ID" width="70" />
        <el-table-column prop="noticeTitle" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.noticeType === '2' ? 'success' : 'warning'" size="small">
              {{ row.noticeType === '2' ? '公告' : '通知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '正常' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建人" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button v-hasPerm="'system:notice:edit'" link type="warning" @click="handleEdit(row)">修改</el-button>
            <el-button v-hasPerm="'system:notice:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="getList"
      />
    </div>

    <el-dialog v-model="open" :title="form.noticeId ? '修改公告' : '新增公告'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.noticeType">
            <el-radio value="1">通知</el-radio>
            <el-radio value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.noticeContent" type="textarea" :rows="8" placeholder="支持 HTML 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewOpen" :title="viewData.noticeTitle" width="640px">
      <div class="notice-content" v-html="viewData.noticeContent" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addNotice, delNotice, listNotice, updateNotice } from '@/api/system'
import type { SysNotice } from '@/types'

const loading = ref(false)
const open = ref(false)
const viewOpen = ref(false)
const list = ref<SysNotice[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const viewData = ref<SysNotice>({})

const query = reactive({ pageNum: 1, pageSize: 10, noticeTitle: '', noticeType: '' })
const form = ref<SysNotice>({})

const rules: FormRules = {
  noticeTitle: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const res = await listNotice(query)
    list.value = res.rows
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.noticeTitle = ''
  query.noticeType = ''
  query.pageNum = 1
  getList()
}

function handleAdd() {
  form.value = { noticeType: '1', status: '0' }
  open.value = true
}

function handleEdit(row: SysNotice) {
  form.value = { ...row }
  open.value = true
}

function handleView(row: SysNotice) {
  viewData.value = row
  viewOpen.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.value.noticeId) await updateNotice(form.value)
  else await addNotice(form.value)
  ElMessage.success('保存成功')
  open.value = false
  getList()
}

async function handleDelete(row: SysNotice) {
  await ElMessageBox.confirm(`确认删除公告【${row.noticeTitle}】?`, '提示', { type: 'warning' })
  await delNotice([row.noticeId as any])
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>

<style scoped>
.notice-content {
  line-height: 1.8;
  color: var(--lp-text);
  max-height: 60vh;
  overflow: auto;
}
</style>
