<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - Redis 监控页面
-->

<template>
  <div class="app-container">
    <div class="flex-between mb8">
      <span class="page-title">Redis 监控</span>
      <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :xs="12" :md="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #dc2626, #f87171)">
          <div>
            <div class="stat-value">{{ info.dbSize ?? 0 }}</div>
            <div class="stat-label">Key 总数</div>
          </div>
          <el-icon class="stat-icon"><Coin /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #0f766e, #14b8a6)">
          <div>
            <div class="stat-value">{{ info.basic?.connected_clients ?? 0 }}</div>
            <div class="stat-label">客户端连接</div>
          </div>
          <el-icon class="stat-icon"><Connection /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4f46e5, #818cf8)">
          <div>
            <div class="stat-value">{{ info.hitRate ?? 0 }}%</div>
            <div class="stat-label">命中率</div>
          </div>
          <el-icon class="stat-icon"><DataLine /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #b45309, #f59e0b)">
          <div>
            <div class="stat-value">{{ info.basic?.instantaneous_ops_per_sec ?? 0 }}</div>
            <div class="stat-label">OPS</div>
          </div>
          <el-icon class="stat-icon"><Odometer /></el-icon>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="12">
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
          <el-progress :percentage="Number(info.memoryUsage || 0)" :stroke-width="14" :color="'#dc2626'" />
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
      <el-collapse>
        <el-collapse-item title="展开查看" name="info">
          <div class="info-grid">
            <div v-for="(value, key) in info.info || {}" :key="key" class="info-item">
              <span class="key">{{ key }}</span><span class="value">{{ value }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { redisInfo } from '@/api/monitor'

const loading = ref(false)
const info = ref<any>({})

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
  font-size: 13px;
  padding: 8px 0;
  border-bottom: 1px dashed #f1f5f9;

  span {
    color: #6b7280;
  }

  b {
    color: #111827;
  }
}

.info-grid {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #eef2f7;
  border-radius: 6px;
}

.info-item {
  display: flex;
  font-size: 12px;
  border-bottom: 1px solid #f8fafc;

  .key {
    width: 260px;
    padding: 6px 10px;
    color: #64748b;
    background: #f8fafc;
    flex-shrink: 0;
    word-break: break-all;
  }

  .value {
    padding: 6px 10px;
    color: #111827;
    word-break: break-all;
  }
}
</style>
