<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - Redis 监控页面
-->

<template>
  <div class="tech-page">
    <div class="tech-header">
      <div class="tech-title">
        <span class="tech-pulse" />
        <span>Redis 监控</span>
        <small>CACHE CLUSTER INSIGHT</small>
      </div>
      <div class="tech-tools">
        <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="tech-metrics">
      <div class="tech-metric">
        <div class="metric-label">Key 总数</div>
        <div class="metric-value">{{ info.dbSize ?? 0 }}</div>
        <div class="metric-sub">过期 {{ info.basic?.expired_keys ?? 0 }} · 淘汰 {{ info.basic?.evicted_keys ?? 0 }}</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">客户端连接</div>
        <div class="metric-value">{{ info.basic?.connected_clients ?? 0 }}</div>
        <div class="metric-sub">阻塞 {{ info.basic?.blocked_clients ?? 0 }}</div>
      </div>
      <div class="tech-metric" :class="Number(info.hitRate || 0) < 90 ? 'is-warn' : ''">
        <div class="metric-label">命中率</div>
        <div class="metric-value">{{ info.hitRate ?? 0 }}<small>%</small></div>
        <div class="metric-sub">命中 {{ info.basic?.keyspace_hits ?? 0 }}</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">实时 OPS</div>
        <div class="metric-value">{{ info.basic?.instantaneous_ops_per_sec ?? 0 }}</div>
        <div class="metric-sub">累计命令 {{ info.basic?.total_commands_processed ?? 0 }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>基础信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本">{{ info.basic?.redis_version }}</el-descriptions-item>
            <el-descriptions-item label="运行模式">{{ info.basic?.redis_mode }}</el-descriptions-item>
            <el-descriptions-item label="操作系统">{{ info.basic?.os }}</el-descriptions-item>
            <el-descriptions-item label="运行天数">{{ info.basic?.uptime_in_days }}</el-descriptions-item>
            <el-descriptions-item label="使用内存">{{ info.basic?.used_memory_human }}</el-descriptions-item>
            <el-descriptions-item label="内存峰值">{{ info.basic?.used_memory_peak_human }}</el-descriptions-item>
            <el-descriptions-item label="最大内存">{{ info.basic?.maxmemory_human }}</el-descriptions-item>
            <el-descriptions-item label="淘汰策略">{{ info.basic?.maxmemory_policy }}</el-descriptions-item>
            <el-descriptions-item label="持久化">
               RDB: {{ info.basic?.rdb_last_bgsave_status }} / AOF: {{ info.basic?.aof_enabled }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">{{ info.basic?.role }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>内存与命中</template>
          <div class="kv"><span>内存使用率</span><b>{{ info.memoryUsage ?? 0 }}%</b></div>
          <el-progress :percentage="Number(info.memoryUsage || 0)" :stroke-width="14" :color="memoryColor" />
          <div class="kv mt16"><span>命中次数</span><b>{{ info.basic?.keyspace_hits }}</b></div>
          <div class="kv"><span>未命中次数</span><b>{{ info.basic?.keyspace_misses }}</b></div>
          <div class="kv"><span>累计命令数</span><b>{{ info.basic?.total_commands_processed }}</b></div>
          <div class="kv"><span>累计连接数</span><b>{{ info.basic?.total_connections_received }}</b></div>
          <div class="kv"><span>过期 Key</span><b>{{ info.basic?.expired_keys }}</b></div>
          <div class="kv"><span>淘汰 Key</span><b>{{ info.basic?.evicted_keys }}</b></div>
          <div class="kv"><span>阻塞客户端</span><b>{{ info.basic?.blocked_clients }}</b></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>数据库</template>
          <el-table :data="info.databases || []" border size="small">
            <el-table-column prop="name" label="库" width="90" />
            <el-table-column prop="keys" label="Keyspace" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>命令统计</template>
          <el-table :data="info.commandStats || []" border size="small" max-height="320">
            <el-table-column prop="name" label="命令" width="160" />
            <el-table-column prop="value" label="统计" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16">
      <template #header>完整 INFO</template>
      <el-collapse class="tech-collapse">
        <el-collapse-item name="info">
          <template #title>
            <span class="tech-collapse-label">
              <i class="led" />
              <span>展开查看</span>
              <small>RAW DATA STREAM</small>
            </span>
          </template>
          <div class="tech-info-grid">
            <div v-for="(value, key) in info.info || {}" :key="key" class="tech-info-item">
              <span class="key">{{ key }}</span><span class="value">{{ value }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { redisInfo } from '@/api/monitor'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const info = ref<any>({})

const memoryColor = computed(() => {
  void appStore.theme
  const root = getComputedStyle(document.documentElement)
  const usage = Number(info.value.memoryUsage || 0)
  if (usage > 85) return root.getPropertyValue('--lp-danger').trim() || '#ef4444'
  if (usage > 65) return root.getPropertyValue('--lp-warning').trim() || '#f59e0b'
  return root.getPropertyValue('--tech-cyan').trim() || '#22d3ee'
})

async function load() {
  loading.value = true
  try {
    info.value = await redisInfo()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  padding: 8px 0;
  border-bottom: 1px dashed color-mix(in srgb, var(--tech-blue) 16%, transparent);

  span {
    color: var(--tech-text-dim);
  }

  b {
    color: var(--tech-text);
    font-weight: 500;
    text-align: right;
    word-break: break-all;
  }
}
</style>
