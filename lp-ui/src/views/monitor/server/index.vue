<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.1.0
  @Description: 系统监控 - 服务器监控页面（科技风：霓虹指标卡 + 环形仪表 + 资源面板）
-->

<template>
  <div class="tech-page">
    <div class="tech-header">
      <div class="tech-title">
        <span class="tech-pulse" />
        <span>服务器监控</span>
        <small>SERVER TELEMETRY · 10s 轮询</small>
      </div>
      <div class="tech-tools">
        <el-switch v-model="autoRefresh" active-text="自动刷新" />
        <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 概览指标 -->
    <div class="tech-metrics">
      <div class="tech-metric" :class="levelClass(cpuUsage)">
        <div class="metric-label">CPU 使用率</div>
        <div class="metric-value">{{ cpuUsage }}<small>%</small></div>
        <div class="metric-sub">{{ info.cpu?.logicalCores || '-' }} 逻辑核心 · {{ info.cpu?.physicalCores || '-' }} 物理核心</div>
      </div>
      <div class="tech-metric" :class="levelClass(memUsage)">
        <div class="metric-label">内存使用率</div>
        <div class="metric-value">{{ memUsage }}<small>%</small></div>
        <div class="metric-sub">{{ fmt(info.mem?.used) }} / {{ fmt(info.mem?.total) }}</div>
      </div>
      <div class="tech-metric" :class="levelClass(jvmUsage)">
        <div class="metric-label">JVM 堆使用率</div>
        <div class="metric-value">{{ jvmUsage }}<small>%</small></div>
        <div class="metric-sub">{{ fmt(info.jvm?.used) }} / {{ fmt(info.jvm?.max) }}</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">系统运行时长</div>
        <div class="metric-value uptime">{{ info.sys?.runTime || '-' }}</div>
        <div class="metric-sub">进程 {{ info.sys?.processCount ?? '-' }} · 线程 {{ info.sys?.threadCount ?? '-' }}</div>
      </div>
    </div>

    <!-- 资源仪表盘 -->
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never">
          <template #header>CPU 负载</template>
          <div class="tech-gauge">
            <el-progress type="dashboard" :percentage="Number(info.cpu?.usage || 0)" :width="150" :stroke-width="10"
              :color="progressColor" />
            <div class="gauge-caption">{{ info.cpu?.name || '未知处理器' }}</div>
          </div>
          <div class="kv-list">
            <div class="tech-kv"><span>物理核心</span><b>{{ info.cpu?.physicalCores ?? '-' }}</b></div>
            <div class="tech-kv"><span>逻辑核心</span><b>{{ info.cpu?.logicalCores ?? '-' }}</b></div>
            <div class="tech-kv"><span>主频</span><b>{{ info.cpu?.frequency ?? '-' }} MHz</b></div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never">
          <template #header>内存占用</template>
          <div class="tech-gauge">
            <el-progress type="dashboard" :percentage="Number(info.mem?.usage || 0)" :width="150" :stroke-width="10"
              :color="progressColor" />
            <div class="gauge-caption">{{ fmt(info.mem?.total) }} 物理内存</div>
          </div>
          <div class="kv-list">
            <div class="tech-kv"><span>总内存</span><b>{{ fmt(info.mem?.total) }}</b></div>
            <div class="tech-kv"><span>已使用</span><b>{{ fmt(info.mem?.used) }}</b></div>
            <div class="tech-kv"><span>可用</span><b>{{ fmt(info.mem?.free) }}</b></div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="8">
        <el-card shadow="never">
          <template #header>JVM 运行状态</template>
          <div class="tech-gauge">
            <el-progress type="dashboard" :percentage="Number(info.jvm?.usage || 0)" :width="150" :stroke-width="10"
              :color="progressColor" />
            <div class="gauge-caption">JDK {{ info.jvm?.version || '-' }}</div>
          </div>
          <div class="kv-list">
            <div class="tech-kv"><span>最大内存</span><b>{{ fmt(info.jvm?.max) }}</b></div>
            <div class="tech-kv"><span>已使用</span><b>{{ fmt(info.jvm?.used) }}</b></div>
            <div class="tech-kv"><span>运行时长</span><b>{{ info.jvm?.runTime || '-' }}</b></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>系统信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="主机名">{{ info.sys?.computerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作系统">{{ info.sys?.osName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="系统架构">{{ info.sys?.osArch || '-' }}</el-descriptions-item>
            <el-descriptions-item label="本机 IP">{{ info.sys?.ip || '-' }}</el-descriptions-item>
            <el-descriptions-item label="启动时间">{{ info.sys?.bootTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ info.sys?.runTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作目录">{{ info.sys?.userDir || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>磁盘容量</template>
          <el-table :data="info.disks || []" size="small" border>
            <el-table-column prop="name" label="磁盘" min-width="90" show-overflow-tooltip />
            <el-table-column prop="mount" label="挂载点" min-width="100" show-overflow-tooltip />
            <el-table-column label="总量" width="100">
              <template #default="{ row }">{{ fmt(row.total) }}</template>
            </el-table-column>
            <el-table-column label="使用率" min-width="140">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.usage || 0)" :stroke-width="8" :color="progressColor" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span>网络接口流量</span>
          <span class="text-muted">
            累计接收 {{ fmt(info.net?.totalRecv) }} · 累计发送 {{ fmt(info.net?.totalSent) }}
          </span>
        </div>
      </template>
      <el-table :data="info.net?.interfaces || []" size="small" border>
        <el-table-column prop="name" label="接口" min-width="110" show-overflow-tooltip />
        <el-table-column prop="ipv4" label="IPv4" min-width="130" show-overflow-tooltip />
        <el-table-column prop="mac" label="MAC" min-width="150" show-overflow-tooltip />
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { serverInfo } from '@/api/monitor'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const autoRefresh = ref(true)
const info = ref<any>({})
let timer: ReturnType<typeof setInterval> | null = null

const cpuUsage = computed(() => Number(info.value.cpu?.usage || 0))
const memUsage = computed(() => Number(info.value.mem?.usage || 0))
const jvmUsage = computed(() => Number(info.value.jvm?.usage || 0))

function levelClass(usage: number) {
  if (usage > 85) return 'is-danger'
  if (usage > 65) return 'is-warn'
  return ''
}

/** 读取主题变量，主题切换后（自动刷新触发重渲染）颜色同步更新 */
function cssVar(name: string, fallback: string) {
  void appStore.theme
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function progressColor(percentage: number) {
  if (percentage > 85) return cssVar('--lp-danger', '#ef4444')
  if (percentage > 65) return cssVar('--lp-warning', '#f59e0b')
  return cssVar('--tech-cyan', '#22d3ee')
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
.tech-metric .metric-value.uptime {
  font-size: 18px;
}

.tech-gauge {
  padding: 4px 0 8px;

  .gauge-caption {
    font-size: 12px;
    color: var(--tech-text-dim);
    text-align: center;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.kv-list {
  margin-top: 6px;
}
</style>
