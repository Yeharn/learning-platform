<!--
  @Author: Yeharn
  @Date:2026-11-05
  @Version: 1.0.0
  @Description: 系统监控 - 服务注册监控页面（基于 Spring Cloud Netflix Eureka）
-->

<template>
  <div class="tech-page">
    <div class="tech-header">
      <div class="tech-title">
        <span class="tech-pulse" />
        <span>服务注册监控</span>
        <small>SERVICE REGISTRY INSIGHT</small>
      </div>
      <div class="tech-tools">
        <el-switch v-model="autoRefresh" active-text="30s 自动刷新" />
        <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="tech-metrics">
      <div class="tech-metric">
        <div class="metric-label">注册服务</div>
        <div class="metric-value">{{ summary.services ?? 0 }}</div>
        <div class="metric-sub">注册中心 EUREKA</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">实例总数</div>
        <div class="metric-value">{{ summary.instances ?? 0 }}</div>
        <div class="metric-sub">平均响应 {{ summary.avgResponseMs ?? 0 }} ms</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">在线实例</div>
        <div class="metric-value">{{ summary.up ?? 0 }}</div>
        <div class="metric-sub">健康探测 UP</div>
      </div>
      <div class="tech-metric" :class="Number(summary.down || 0) > 0 ? 'is-danger' : ''">
        <div class="metric-label">异常实例</div>
        <div class="metric-value">{{ summary.down ?? 0 }}</div>
        <div class="metric-sub">DOWN / 失联 / 探测失败</div>
      </div>
    </div>

    <div class="svc-grid">
      <el-card v-for="svc in services" :key="svc.name" shadow="never" class="svc-card">
        <template #header>
          <div class="svc-head">
            <span class="svc-name">
              <i class="svc-led" :class="Number(svc.upCount) === svc.instances.length ? 'is-up' : 'is-down'" />
              {{ svc.name }}
            </span>
            <el-tag size="small" :type="Number(svc.upCount) === svc.instances.length ? 'success' : 'danger'"
              effect="dark" class="svc-tag">
              {{ svc.upCount }}/{{ svc.instances.length }} UP
            </el-tag>
          </div>
        </template>

        <el-table :data="svc.instances" size="small">
          <el-table-column label="实例" min-width="170">
            <template #default="{ row }">
              <span class="mono">{{ row.instanceId || (row.host + ':' + row.port) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="地址" min-width="130">
            <template #default="{ row }">
              <span class="mono">{{ row.scheme }}://{{ row.host }}:{{ row.port }}</span>
            </template>
          </el-table-column>
          <el-table-column label="健康探测" width="100" align="center">
            <template #default="{ row }">
              <span class="health-badge" :class="healthClass(row.health)">
                <i class="dot" />{{ healthText(row.health) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="组件" width="130">
            <template #default="{ row }">
              <el-tooltip placement="top" :disabled="!hasDetail(row)">
                <template #content>
                  <div v-for="(v, k) in row.healthDetail || {}" :key="k" class="mono">{{ k }}: {{ v }}</div>
                </template>
                <span v-if="hasDetail(row)" class="components">
                  <i v-for="(v, k) in row.healthDetail || {}" :key="k"
                    class="comp-dot" :class="v === 'UP' ? 'is-up' : 'is-down'" :title="k + ': ' + v" />
                  <span class="comp-count">{{ upDetailCount(row) }}/{{ detailCount(row) }}</span>
                </span>
                <span v-else class="text-dim">-</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="响应" width="76" align="center">
            <template #default="{ row }">
              <span class="mono" :class="Number(row.responseMs) > 1000 ? 'is-slow' : ''">
                {{ row.responseMs ?? '-' }} ms
              </span>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="145">
            <template #default="{ row }">
              <span class="mono">{{ row.registrationTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="租约" width="70" align="center">
            <template #default="{ row }">
              <span class="mono">{{ row.leaseDurationSecs ? (row.leaseDurationSecs + 's') : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="62" align="center">
            <template #default="{ row }">
              <span class="mono">{{ row.metadata?.version || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-empty v-if="!loading && services.length === 0" description="注册中心暂无服务实例" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { serviceOverview } from '@/api/monitor'

const loading = ref(false)
const autoRefresh = ref(true)
const data = ref<any>({})
let timer: number | undefined

const summary = computed(() => data.value.summary || {})
const services = computed<any[]>(() => data.value.services || [])

function healthClass(health: string) {
  if (health === 'UP') return 'is-up'
  if (!health || health === 'UNKNOWN') return 'is-unknown'
  return 'is-down'
}

function healthText(health: string) {
  if (health === 'UP') return 'UP'
  if (!health || health === 'UNKNOWN') return '未知'
  return health === 'DOWN' ? 'DOWN' : health.startsWith('HTTP') ? '异常' : 'DOWN'
}

function hasDetail(row: any) {
  return row.healthDetail && Object.keys(row.healthDetail).length > 0
}

function detailCount(row: any) {
  return Object.keys(row.healthDetail || {}).length
}

function upDetailCount(row: any) {
  return Object.values(row.healthDetail || {}).filter((v) => v === 'UP').length
}

async function load() {
  loading.value = true
  try {
    data.value = await serviceOverview()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  timer = window.setInterval(() => {
    if (autoRefresh.value) load()
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
<style scoped lang="scss">
.svc-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(740px, 1fr));

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.svc-card {
  .svc-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .svc-name {
    display: inline-flex;
    align-items: center;
    gap: 9px;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 1px;
    color: #fff;
    font-family: 'Bahnschrift', Consolas, monospace;
    text-shadow: 0 0 14px color-mix(in srgb, var(--tech-cyan) 40%, transparent);
  }
}

.svc-led {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;

  &.is-up {
    background: var(--lp-success);
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--lp-success) 70%, transparent);
    animation: svc-pulse 1.8s ease-out infinite;
  }

  &.is-down {
    background: var(--lp-danger);
    box-shadow: 0 0 8px color-mix(in srgb, var(--lp-danger) 80%, transparent);
  }
}

@keyframes svc-pulse {
  0% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--lp-success) 65%, transparent);
  }

  70% {
    box-shadow: 0 0 0 10px transparent;
  }

  100% {
    box-shadow: 0 0 0 0 transparent;
  }
}

.mono {
  font-family: 'Bahnschrift', Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: var(--tech-text);
  letter-spacing: 0.3px;
}

.is-slow {
  color: var(--lp-warning);
}

.text-dim {
  color: var(--tech-text-dim);
}

.health-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  font-family: 'Bahnschrift', Consolas, monospace;
  padding: 2px 9px;
  border-radius: 10px;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &.is-up {
    color: var(--lp-success);
    background: color-mix(in srgb, var(--lp-success) 14%, transparent);
    border: 1px solid color-mix(in srgb, var(--lp-success) 40%, transparent);

    .dot {
      background: var(--lp-success);
      box-shadow: 0 0 7px var(--lp-success);
    }
  }

  &.is-down {
    color: var(--lp-danger);
    background: color-mix(in srgb, var(--lp-danger) 14%, transparent);
    border: 1px solid color-mix(in srgb, var(--lp-danger) 40%, transparent);

    .dot {
      background: var(--lp-danger);
      box-shadow: 0 0 7px var(--lp-danger);
      animation: svc-pulse 1s ease-out infinite;
    }
  }

  &.is-unknown {
    color: var(--tech-text-dim);
    background: color-mix(in srgb, var(--tech-blue) 10%, transparent);
    border: 1px solid color-mix(in srgb, var(--tech-blue) 26%, transparent);

    .dot {
      background: var(--tech-text-dim);
    }
  }
}

.components {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: default;

  .comp-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;

    &.is-up {
      background: var(--lp-success);
      box-shadow: 0 0 6px color-mix(in srgb, var(--lp-success) 70%, transparent);
    }

    &.is-down {
      background: var(--lp-danger);
      box-shadow: 0 0 6px color-mix(in srgb, var(--lp-danger) 70%, transparent);
    }
  }

  .comp-count {
    margin-left: 4px;
    font-size: 11px;
    font-family: 'Bahnschrift', Consolas, monospace;
    color: var(--tech-text-dim);
  }
}
</style>