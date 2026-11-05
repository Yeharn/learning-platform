<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 流程图查看器：按实例活动节点渲染纵向流程图，高亮当前节点
-->

<template>
  <div class="flow-viewer">
    <div v-for="(node, index) in nodes" :key="node.nodeKey + '-' + index" class="flow-node"
      :class="{ active: node.nodeKey === currentKey, branch: (node.level || 0) > 0 }"
      :style="{ marginLeft: (node.level || 0) * 32 + 'px' }">
      <div v-if="index > 0" class="flow-line" />
      <el-icon class="flow-node-icon" :class="'type-' + (node.type ?? 0)">
        <component :is="iconOf(node)" />
      </el-icon>
      <div class="flow-node-main">
        <div class="flow-node-name">{{ node.nodeName || '未命名节点' }}</div>
        <div class="flow-node-sub">
          <span>{{ typeText(node.type) }}</span>
          <span v-if="node.assignees && node.assignees.length"> · {{ node.assignees.join('、') }}</span>
        </div>
      </div>
      <el-tag v-if="node.nodeKey === currentKey" type="warning" size="small" effect="dark">进行中</el-tag>
    </div>
    <el-empty v-if="!nodes || nodes.length === 0" description="暂无流程节点" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  nodes: any[]
  currentKey?: string
}>()

function iconOf(node: any) {
  const type = node.type ?? 0
  if (type === 0) return 'Promotion'
  if (type === 2) return 'CircleClose'
  if (type === 3 || type === 4 || type === 5) return 'Switch'
  return 'UserFilled'
}

function typeText(type: number) {
  // Warm-Flow 节点类型：0 开始 1 审批 2 结束 3 互斥网关 4 并行网关 5 包容网关
  const map: Record<number, string> = {
    0: '发起',
    1: '审批',
    2: '结束',
    3: '互斥网关',
    4: '并行网关',
    5: '包容网关'
  }
  return map[type ?? 1] || '审批'
}
</script>

<style scoped>
.flow-viewer {
  padding: 8px 4px;
  max-height: 420px;
  overflow-y: auto;
}

.flow-node {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--lp-card-bg, rgba(255, 255, 255, 0.7));
  border: 1px solid var(--el-border-color-lighter);
  margin-bottom: 28px;
  transition: all 0.2s;
}

.flow-node.active {
  border-color: var(--el-color-warning);
  box-shadow: 0 0 0 2px var(--el-color-warning-light-7);
}

.flow-node.branch {
  border-style: dashed;
}

.flow-line {
  position: absolute;
  left: 26px;
  top: -29px;
  height: 28px;
  border-left: 2px dashed var(--el-border-color);
}

.flow-node-icon {
  color: var(--el-color-primary);
  font-size: 18px;
}

.flow-node-icon.type-0 {
  color: var(--el-color-success);
}

.flow-node-icon.type-2 {
  color: var(--el-color-info);
}

.flow-node-main {
  flex: 1;
  min-width: 0;
}

.flow-node-name {
  font-size: 14px;
  font-weight: 600;
}

.flow-node-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
