<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统监控 - MySQL 监控页面
-->

<template>
  <div class="tech-page">
    <div class="tech-header">
      <div class="tech-title">
        <span class="tech-pulse" />
        <span>MySQL 监控</span>
        <small>DATABASE INSIGHT</small>
      </div>
      <div class="tech-tools">
        <el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="tech-metrics">
      <div class="tech-metric">
        <div class="metric-label">平均 QPS</div>
        <div class="metric-value">{{ info.basic?.qps ?? 0 }}</div>
        <div class="metric-sub">累计查询 {{ info.basic?.questions ?? '-' }}</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">当前连接</div>
        <div class="metric-value">{{ info.basic?.threadsConnected ?? 0 }}</div>
        <div class="metric-sub">连接使用率 {{ info.basic?.connectionUsage ?? 0 }}%</div>
      </div>
      <div class="tech-metric" :class="Number(info.basic?.slowQueries || 0) > 0 ? 'is-warn' : ''">
        <div class="metric-label">慢查询</div>
        <div class="metric-value">{{ info.basic?.slowQueries ?? 0 }}</div>
        <div class="metric-sub">阈值 {{ threshold }} 秒</div>
      </div>
      <div class="tech-metric">
        <div class="metric-label">运行天数</div>
        <div class="metric-value">{{ days(info.basic?.uptime) }}<small>天</small></div>
        <div class="metric-sub">版本 {{ info.basic?.version || '-' }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>基础信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本">{{ info.basic?.version }}</el-descriptions-item>
            <el-descriptions-item label="主机">{{ info.basic?.hostname }}:{{ info.basic?.port }}</el-descriptions-item>
            <el-descriptions-item label="数据目录">{{ info.basic?.datadir }}</el-descriptions-item>
            <el-descriptions-item label="最大连接">{{ info.basic?.maxConnections }}</el-descriptions-item>
            <el-descriptions-item label="连接使用率">{{ info.basic?.connectionUsage }}%</el-descriptions-item>
            <el-descriptions-item label="累计查询">{{ info.basic?.questions }}</el-descriptions-item>
            <el-descriptions-item label="接收流量">{{ fmt(info.basic?.bytesReceived) }}</el-descriptions-item>
            <el-descriptions-item label="发送流量">{{ fmt(info.basic?.bytesSent) }}</el-descriptions-item>
            <el-descriptions-item label="打开表数">{{ info.basic?.openTables }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>连接池 (HikariCP)</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="活跃">{{ info.pool?.active ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="空闲">{{ info.pool?.idle ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="总数">{{ info.pool?.total ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="最大">{{ info.pool?.max ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="等待线程">{{ info.pool?.waiting ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="池名称">{{ info.pool?.poolName ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>基础信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本">{{ info.basic?.version }}</el-descriptions-item>
            <el-descriptions-item label="主机">{{ info.basic?.hostname }}:{{ info.basic?.port }}</el-descriptions-item>
            <el-descriptions-item label="数据目录">{{ info.basic?.datadir }}</el-descriptions-item>
            <el-descriptions-item label="最大连接">{{ info.basic?.maxConnections }}</el-descriptions-item>
            <el-descriptions-item label="连接使用率">{{ info.basic?.connectionUsage }}%</el-descriptions-item>
            <el-descriptions-item label="累计查询">{{ info.basic?.questions }}</el-descriptions-item>
            <el-descriptions-item label="接收流量">{{ fmt(info.basic?.bytesReceived) }}</el-descriptions-item>
            <el-descriptions-item label="发送流量">{{ fmt(info.basic?.bytesSent) }}</el-descriptions-item>
            <el-descriptions-item label="打开表数">{{ info.basic?.openTables }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>连接池 (HikariCP)</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="活跃">{{ info.pool?.active ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="空闲">{{ info.pool?.idle ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="总数">{{ info.pool?.total ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="最大">{{ info.pool?.max ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="等待线程">{{ info.pool?.waiting ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="池名称">{{ info.pool?.poolName ?? '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="mt16">
            <div class="page-title">表空间 Top 10</div>
            <el-table :data="info.topTables || []" size="small" border>
              <el-table-column prop="name" label="表名" min-width="140" />
              <el-table-column prop="rowsCount" label="行数" width="90" />
              <el-table-column prop="sizeKb" label="大小(KB)" width="100" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>数据库大小</template>
          <el-table :data="info.databases || []" size="small" border max-height="320">
            <el-table-column prop="name" label="数据库" min-width="140" />
            <el-table-column prop="size" label="大小(MB)" width="100" />
            <el-table-column prop="tables" label="表数" width="80" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>当前连接</template>
          <el-table :data="info.processes || []" size="small" border max-height="320">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="user" label="用户" width="90" />
            <el-table-column prop="host" label="来源" min-width="120" show-overflow-tooltip />
            <el-table-column prop="db" label="库" width="110" show-overflow-tooltip />
            <el-table-column prop="command" label="命令" width="90" />
            <el-table-column prop="time" label="时间" width="70" />
            <el-table-column prop="state" label="状态" min-width="100" show-overflow-tooltip />
            <el-table-column prop="info" label="SQL" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="flex-between">
          <span>全局状态</span>
          <el-button size="small" icon="Refresh" @click="loadAudit">刷新审计</el-button>
        </div>
      </template>
      <el-tabs v-model="auditTab" @tab-change="loadAudit">
        <el-tab-pane label="慢 SQL 审计" name="slow">
          <el-table :data="slowSqls" size="small" border max-height="360">
            <el-table-column prop="digestText" label="语句摘要" min-width="300" show-overflow-tooltip />
            <el-table-column prop="schemaName" label="库" width="140" show-overflow-tooltip />
            <el-table-column prop="execCount" label="执行次数" width="90" />
            <el-table-column prop="totalTimeSec" label="总耗时(s)" width="100" sortable />
            <el-table-column prop="avgTimeMs" label="平均(ms)" width="100" sortable />
            <el-table-column prop="avgScanRows" label="平均扫描行" width="110" />
            <el-table-column prop="rowsReturned" label="返回行" width="90" />
            <el-table-column prop="lastSeen" label="最后执行" width="160" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`长 SQL 与事务 (${longCount})`" name="long">
          <div class="page-title">运行中的长 SQL（超过 {{ threshold }} 秒，可终止）</div>
          <el-table :data="longQueries" size="small" border max-height="260">
            <el-table-column prop="id" label="线程ID" width="80" />
            <el-table-column prop="user" label="用户" width="90" />
            <el-table-column prop="host" label="来源" min-width="120" show-overflow-tooltip />
            <el-table-column prop="db" label="库" width="110" show-overflow-tooltip />
            <el-table-column prop="time" label="耗时(s)" width="90" sortable />
            <el-table-column prop="state" label="状态" min-width="100" show-overflow-tooltip />
            <el-table-column prop="info" label="SQL" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" icon="CircleClose" v-hasPerm="['monitor:mysql:kill']" @click="kill(row.id)">终止</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="page-title mt16">运行中的事务（含锁等待，可终止）</div>
          <el-table :data="transactions" size="small" border max-height="240">
            <el-table-column prop="threadId" label="线程ID" width="90" />
            <el-table-column prop="trxState" label="状态" width="110" />
            <el-table-column prop="runSeconds" label="持续(s)" width="90" sortable />
            <el-table-column prop="rowsLocked" label="锁定行" width="90" />
            <el-table-column prop="rowsModified" label="修改行" width="90" />
            <el-table-column prop="trxQuery" label="SQL" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" icon="CircleClose" v-hasPerm="['monitor:mysql:kill']" @click="kill(row.threadId)">终止</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>全局状态</template>
      <el-collapse>
        <el-collapse-item title="展开查看" name="status">
          <div class="status-grid">
            <div v-for="(value, key) in info.status || {}" :key="key" class="status-item">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { mysqlInfo, mysqlSlowSqls, mysqlLongQueries, mysqlKill } from '@/api/monitor'

const loading = ref(false)
const info = ref<any>({})
const auditTab = ref('slow')
const slowSqls = ref<any[]>([])
const longQueries = ref<any[]>([])
const transactions = ref<any[]>([])
const threshold = ref(3)

const longCount = ref(0)

function days(uptime: any) {
  return Math.floor(Number(uptime || 0) / 86400)
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
    info.value = await mysqlInfo()
  } finally {
    loading.value = false
  }
}

async function loadAudit() {
  if (auditTab.value === 'slow') {
    slowSqls.value = (await mysqlSlowSqls(30)) as any[]
  } else {
    const res: any = await mysqlLongQueries(threshold.value)
    longQueries.value = res.longQueries || []
    transactions.value = res.transactions || []
    longCount.value = longQueries.value.length + transactions.value.length
  }
}

async function kill(threadId: any) {
  await ElMessageBox.confirm(`确认终止线程 ${threadId}？该操作将中断其执行的 SQL。`, '高危操作', { type: 'warning' })
  await mysqlKill(threadId)
  ElMessage.success('已发送 KILL')
  loadAudit()
  load()
}

onMounted(() => {
  load()
  loadAudit()
})
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}

.status-grid {
  max-height: 420px;
  overflow: auto;
}

.status-item {
  display: flex;
  font-size: 12px;
  border-bottom: 1px solid color-mix(in srgb, var(--tech-blue) 14%, transparent);

  .key {
    width: 280px;
    padding: 5px 10px;
    color: var(--tech-text-dim);
    background: color-mix(in srgb, var(--tech-blue) 8%, transparent);
    flex-shrink: 0;
    word-break: break-all;
  }

  .value {
    padding: 5px 10px;
    color: var(--tech-text);
    word-break: break-all;
  }
}

@media (max-width: 992px) {
  .status-item .key {
    width: 150px;
  }
}
</style>
