<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - 在线用户页面（分页查询、强制下线、与顶部在线徽标联动）
-->

<template>
  <div class="tech-page">
    <div class="tech-header">
      <div class="tech-title">
        <span class="tech-pulse" />
        <span>在线用户</span>
        <small>ONLINE SESSIONS</small>
      </div>
      <div class="tech-tools">
        <el-tag type="success" effect="dark">当前在线 {{ onlineStore.count }} 人</el-tag>
      </div>
    </div>

    <el-form :inline="true" class="search-form">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item class="search-actions">
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="nickname" label="昵称" width="130" />
        <el-table-column prop="deptName" label="部门" width="130" />
        <el-table-column prop="loginIp" label="登录IP" width="140" />
        <el-table-column prop="browser" label="浏览器" width="120" />
        <el-table-column prop="os" label="操作系统" width="120" />
        <el-table-column label="登录时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="['monitor:online:kick']" link type="danger" @click="handleKick(row)">
              强制下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="handleSizeChange" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { kickOnline, onlinePage } from '@/api/system'
import { useOnlineStore } from '@/stores/online'

const onlineStore = useOnlineStore()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ username: '', pageNum: 1, pageSize: 10 })

function formatTime(value: any) {
  if (!value) return '-'
  return new Date(Number(value)).toLocaleString('zh-CN')
}

async function load() {
  loading.value = true
  try {
    const res: any = await onlinePage({ ...query })
    list.value = res.rows || []
    total.value = Number(res.total) || 0
    await onlineStore.refresh()
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleSizeChange() {
  query.pageNum = 1
  load()
}

async function handleKick(row: any) {
  await ElMessageBox.confirm(`确认将用户【${row.username}】强制下线?`, '提示', { type: 'warning' })
  await kickOnline(row.token || '')
  ElMessage.success('操作成功')
  if (list.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  await load()
}

onMounted(load)
</script>
