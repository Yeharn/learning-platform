<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - 服务器监控页面
-->

<template>
  <div class="app-container">
    <div class="flex-between mb8">
      <span class="page-title">服务器监控</span>
      <div>
        <el-switch v-model="autoRefresh" active-text="自动刷新" style="margin-right: 12px" />
        <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="card">
          <template #header>CPU</template>
          <el-progress type="dashboard" :percentage="Number(info.cpu?.usage || 0)" :color="progressColor" />
          <div class="kv"><span>型号</span><b>{{ info.cpu?.name }}</b></div>
          <div class="kv"><span>物理核心</span><b>{{ info.cpu?.physicalCores }}</b></div>
          <div class="kv"><span>逻辑核心</span><b>{{ info.cpu?.logicalCores }}</b></div>
          <div class="kv"><span>主频</span><b>{{ info.cpu?.frequency }} MHz</b></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="card">
          <template #header>内存</template>
          <el-progress type="dashboard" :percentage="Number(info.mem?.usage || 0)" :color="progressColor" />
          <div class="kv"><span>总内存</span><b>{{ fmt(info.mem?.total) }}</b></div>
          <div class="kv"><span>已使用</span><b>{{ fmt(info.mem?.used) }}</b></div>
          <div class="kv"><span>可用</span><b>{{ fmt(info.mem?.free) }}</b></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="card">
          <template #header>JVM</template>
          <el-progress type="dashboard" :percentage="Number(info.jvm?.usage || 0)" :color="progressColor" />
          <div class="kv"><span>版本</span><b>{{ info.jvm?.version }}</b></div>
          <div class="kv"><span>最大内存</span><b>{{ fmt(info.jvm?.max) }}</b></div>
          <div class="kv"><span>已使用</span><b>{{ fmt(info.jvm?.used) }}</b></div>
          <div class="kv"><span>运行时长</span><b>{{ info.jvm?.runTime }}</b></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>系统信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="主机名">{{ info.sys?.computerName }}</el-descriptions-item>
            <el-descriptions-item label="操作系统">{{ info.sys?.osName }}</el-descriptions-item>
            <el-descriptions-item label="系统架构">{{ info.sys?.osArch }}</el-descriptions-item>
            <el-descriptions-item label="本机IP">{{ info.sys?.ip }}</el-descriptions-item>
            <el-descriptions-item label="启动时间">{{ info.sys?.bootTime }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ info.sys?.runTime }}</el-descriptions-item>
            <el-descriptions-item label="进程数">{{ info.sys?.processCount }}</el-descriptions-item>
            <el-descriptions-item label="线程数">{{ info.sys?.threadCount }}</el-descriptions-item>
            <el-descriptions-item label="工作目录">{{ info.sys?.userDir }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>磁盘</template>
          <el-table :data="info.disks || []" size="small" border>
            <el-table-column prop="name" label="磁盘" min-width="100" />
            <el-table-column prop="mount" label="挂载点" min-width="100" />
            <el-table-column label="总量" width="100">
              <template #default="{ row }">{{ fmt(row.total) }}</template>
            </el-table-column>
            <el-table-column label="使用率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.usage || 0)" :stroke-width="8" :color="progressColor" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="flex-between">
          <span>网络接口</span>
          <span class="text-muted">
            累计接收 {{ fmt(info.net?.totalRecv) }} / 累计发送 {{ fmt(info.net?.totalSent) }}
          </span>
        </div>
      </template>
      <el-table :data="info.net?.interfaces || []" size="small" border>
        <el-table-column prop="name" label="接口" min-width="110" />
        <el-table-column prop="ipv4" label="IPv4" min-width="130" />
        <el-table-column prop="mac" label="MAC" min-width="150" />
        <el-table-column prop="speed" label="速率(bps)" width="110" />
        <el-table-column label="接收" width="110">
          <template #default="{ row }">{{ fmt(row.recv) }}</template>
        </el-table-column>
        <el-table-column label="发送" width="110">
          <template #default="{ row }">{{ fmt(row.sent) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { serverInfo } from '@/api/monitor'

const loading = ref(false)
const autoRefresh = ref(true)
const info = ref<any>({})
let timer: ReturnType<typeof setInterval> | null = null

function progressColor(percentage: number) {
  if (percentage > 85) return '#ef4444'
  if (percentage > 65) return '#f59e0b'
  return '#6366f1'
}

function fmt(bytes: any) {
  const value = Number(bytes || 0)
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let size = value
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }
  return `${size.toFixed(2)} ${units[index]}`
}

async function load() {
  loading.value = true
  try {
    info.value = await serverInfo()
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(load, 10000)
}

function stopTimer() {
  if (timer) clearInterval(timer)
  timer = null
}

watch(autoRefresh, (val) => (val ? startTimer() : stopTimer()))

onMounted(() => {
  load()
  startTimer()
})

onBeforeUnmount(stopTimer)
</script>

<style scoped lang="scss">
.card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
}

.mt16 {
  margin-top: 16px;
}

.kv {
  width: 100%;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 1px dashed #f1f5f9;

  span {
    color: #6b7280;
  }

  b {
    color: #111827;
    font-weight: 500;
    max-width: 65%;
    text-align: right;
    word-break: break-all;
  }
}
</style>
