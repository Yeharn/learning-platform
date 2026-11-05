<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 控制台首页：统计概览与图表
-->

<template>
  <div class="app-container dashboard">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="12" :md="6">
        <div class="stat-card" :style="{ background: card.bg }">
          <div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
          <el-icon class="stat-icon"><component :is="card.icon" /></el-icon>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span>近 7 天登录趋势</span>
              <el-button link type="primary" @click="load">刷新</el-button>
            </div>
          </template>
          <div ref="trendRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>部门人员分布</template>
          <div ref="deptRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>最近登录记录</template>
          <el-table :data="data.loginLogs" size="small">
            <el-table-column prop="username" label="账号" width="110" />
            <el-table-column prop="ipaddr" label="IP" width="130" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="loginTime" label="时间" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>系统公告</template>
          <el-empty v-if="!data.notices?.length" description="暂无公告" :image-size="70" />
          <div v-for="notice in data.notices" :key="notice.noticeId" class="notice-item">
            <el-icon color="#6366f1"><Bell /></el-icon>
            <div class="notice-body">
              <div class="notice-title">{{ notice.noticeTitle }}</div>
              <div class="text-muted">{{ notice.createTime }}</div>
            </div>
            <el-tag size="small" :type="notice.noticeType === '2' ? 'success' : 'warning'">
              {{ notice.noticeType === '2' ? '公告' : '通知' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboard } from '@/api/system'

const data = reactive<any>({
  stats: {},
  loginTrend: [],
  deptStat: [],
  loginLogs: [],
  notices: []
})

const trendRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let deptChart: echarts.ECharts | null = null

const cards = computed(() => [
  { label: '用户总数', value: num(data.stats.userCount), icon: 'User', bg: 'linear-gradient(135deg,#4f46e5,#7c3aed)' },
  { label: '角色数量', value: num(data.stats.roleCount), icon: 'UserFilled', bg: 'linear-gradient(135deg,#0ea5e9,#06b6d4)' },
  { label: '在线用户', value: num(data.stats.onlineCount), icon: 'Connection', bg: 'linear-gradient(135deg,#10b981,#22c55e)' },
  { label: '今日登录', value: num(data.stats.todayLogin), icon: 'Key', bg: 'linear-gradient(135deg,#f59e0b,#f97316)' },
  { label: '菜单权限', value: num(data.stats.menuCount), icon: 'Menu', bg: 'linear-gradient(135deg,#ec4899,#f43f5e)' },
  { label: '部门数量', value: num(data.stats.deptCount), icon: 'OfficeBuilding', bg: 'linear-gradient(135deg,#8b5cf6,#a855f7)' },
  { label: '今日操作', value: num(data.stats.todayOper), icon: 'Document', bg: 'linear-gradient(135deg,#0891b2,#0284c7)' },
  { label: '日志总量', value: num(data.stats.operCount), icon: 'DataLine', bg: 'linear-gradient(135deg,#475569,#64748b)' }
])

function num(value: any) {
  return Number(value || 0)
}

async function load() {
  const res = await dashboard()
  Object.assign(data, res)
  await nextTick()
  renderTrend()
  renderDept()
}

function renderTrend() {
  if (!trendRef.value) return
  trendChart = trendChart || echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.loginTrend.map((i: any) => i.day),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#6b7280' }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: data.loginTrend.map((i: any) => Number(i.total)),
        itemStyle: { color: '#6366f1' },
        lineStyle: { width: 3, color: '#6366f1' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99,102,241,0.35)' },
            { offset: 1, color: 'rgba(99,102,241,0.02)' }
          ])
        }
      }
    ]
  })
}

function renderDept() {
  if (!deptRef.value) return
  deptChart = deptChart || echarts.init(deptRef.value)
  deptChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#6b7280' } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: data.deptStat.map((i: any) => ({ name: i.name, value: Number(i.value) }))
      }
    ],
    color: ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#14b8a6', '#f43f5e']
  })
}

function resize() {
  trendChart?.resize()
  deptChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  deptChart?.dispose()
})
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}

.chart {
  height: 280px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #eef2f7;

  &:last-child {
    border-bottom: none;
  }

  .notice-body {
    flex: 1;
    overflow: hidden;
  }

  .notice-title {
    font-size: 13px;
    color: #374151;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
