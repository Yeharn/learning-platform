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
        <div class="stat-card" :class="card.cls">
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
            <el-icon color="var(--lp-primary)"><Bell /></el-icon>
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { dashboard } from '@/api/system'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

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
  { label: '用户总数', value: num(data.stats.userCount), icon: 'User', cls: 'sc-primary' },
  { label: '角色数量', value: num(data.stats.roleCount), icon: 'UserFilled', cls: 'sc-info' },
  { label: '在线用户', value: num(data.stats.onlineCount), icon: 'Connection', cls: 'sc-success' },
  { label: '今日登录', value: num(data.stats.todayLogin), icon: 'Key', cls: 'sc-warning' },
  { label: '菜单权限', value: num(data.stats.menuCount), icon: 'Menu', cls: 'sc-danger' },
  { label: '部门数量', value: num(data.stats.deptCount), icon: 'OfficeBuilding', cls: 'sc-violet' },
  { label: '今日操作', value: num(data.stats.todayOper), icon: 'Document', cls: 'sc-cyan' },
  { label: '日志总量', value: num(data.stats.operCount), icon: 'DataLine', cls: 'sc-slate' }
])

/** 读取主题变量，图表颜色随主题联动 */
function cssVar(name: string, fallback: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

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
  const primary = cssVar('--lp-primary', '#3b82f6')
  const primaryLight = cssVar('--lp-primary-light', '#60a5fa')
  trendChart.setOption(
    {
      tooltip: { trigger: 'axis' },
      grid: { left: 44, right: 20, top: 30, bottom: 30 },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.loginTrend.map((i: any) => i.day),
        axisLine: { lineStyle: { color: cssVar('--lp-border-strong', '#e5e7eb') } },
        axisLabel: { color: cssVar('--lp-text-muted', '#6b7280') }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: cssVar('--lp-border', '#f3f4f6'), type: 'dashed' } },
        axisLabel: { color: cssVar('--lp-text-muted', '#6b7280') }
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbolSize: 7,
          data: data.loginTrend.map((i: any) => Number(i.total)),
          itemStyle: { color: primary },
          lineStyle: { width: 3, color: primary },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: primaryLight },
              { offset: 1, color: 'rgba(255,255,255,0)' }
            ]),
            opacity: 0.35
          }
        }
      ]
    },
    true
  )
}

function renderDept() {
  if (!deptRef.value) return
  deptChart = deptChart || echarts.init(deptRef.value)
  deptChart.setOption(
    {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { fontSize: 11, color: cssVar('--lp-text-muted', '#6b7280') } },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '44%'],
          itemStyle: { borderRadius: 6, borderColor: cssVar('--lp-surface', '#fff'), borderWidth: 2 },
          label: { show: false },
          data: data.deptStat.map((i: any) => ({ name: i.name, value: Number(i.value) }))
        }
      ],
      color: [
        cssVar('--lp-primary', '#3b82f6'),
        cssVar('--lp-info', '#0ea5e9'),
        cssVar('--lp-success', '#10b981'),
        cssVar('--lp-warning', '#f59e0b'),
        cssVar('--lp-danger', '#ef4444'),
        cssVar('--lp-primary-light', '#60a5fa')
      ]
    },
    true
  )
}

function resize() {
  trendChart?.resize()
  deptChart?.resize()
}

/* 主题切换后重绘图表，保证配色同步 */
watch(
  () => appStore.theme,
  async () => {
    await nextTick()
    renderTrend()
    renderDept()
  }
)

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

/* 统计卡配色全部由主题变量派生，切换主题即时生效 */
.stat-card {
  margin-bottom: 16px;
  box-shadow: 0 8px 22px color-mix(in srgb, var(--lp-primary) 18%, transparent);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 28px color-mix(in srgb, var(--lp-primary) 26%, transparent);
  }

  &.sc-primary {
    background: linear-gradient(135deg, var(--lp-primary-dark), var(--lp-primary));
  }

  &.sc-info {
    background: linear-gradient(135deg, color-mix(in srgb, var(--lp-info) 78%, #000), var(--lp-info));
  }

  &.sc-success {
    background: linear-gradient(135deg, color-mix(in srgb, var(--lp-success) 78%, #000), var(--lp-success));
  }

  &.sc-warning {
    background: linear-gradient(135deg, color-mix(in srgb, var(--lp-warning) 78%, #000), var(--lp-warning));
  }

  &.sc-danger {
    background: linear-gradient(135deg, color-mix(in srgb, var(--lp-danger) 78%, #000), var(--lp-danger));
  }

  &.sc-violet {
    background: linear-gradient(135deg, var(--lp-primary), var(--lp-primary-light));
  }

  &.sc-cyan {
    background: linear-gradient(135deg, color-mix(in srgb, var(--lp-info) 70%, var(--lp-primary)), var(--lp-info));
  }

  &.sc-slate {
    background: linear-gradient(135deg, #475569, #64748b);
  }
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--lp-divider);

  &:last-child {
    border-bottom: none;
  }

  .notice-body {
    flex: 1;
    overflow: hidden;
  }

  .notice-title {
    font-size: 13px;
    color: var(--lp-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

@media (max-width: 768px) {
  .chart {
    height: 220px;
  }

  .stat-card {
    padding: 14px;

    .stat-value {
      font-size: 22px;
    }
  }
}
</style>
