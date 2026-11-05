<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 侧边栏菜单项递归渲染（支持外链）
-->

<template>
  <el-sub-menu v-if="visibleChildren.length > 0" :index="fullPath">
    <template #title>
      <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
      <span>{{ item.meta?.title }}</span>
    </template>
    <SidebarItem
      v-for="child in visibleChildren"
      :key="child.path"
      :item="child"
      :base-path="fullPath"
    />
  </el-sub-menu>
  <a
    v-else-if="!item.hidden && isExternal"
    :href="fullPath"
    target="_blank"
    rel="noopener noreferrer"
    class="external-link"
    :class="{ 'is-collapsed': appStore.sidebarCollapsed }"
    :title="item.meta?.title"
  >
    <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
    <span v-show="!appStore.sidebarCollapsed">{{ item.meta?.title }}</span>
    <el-icon v-show="!appStore.sidebarCollapsed" class="external-icon"><TopRight /></el-icon>
  </a>
  <el-menu-item v-else-if="!item.hidden" :index="fullPath">
    <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
    <template #title>{{ item.meta?.title }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouterItem } from '@/types'
import { useAppStore } from '@/stores/app'

const props = defineProps<{ item: RouterItem; basePath: string }>()

const appStore = useAppStore()

const isExternal = computed(() => /^(https?:)?\/\//.test(props.item.path))

const fullPath = computed(() => {
  if (isExternal.value) return props.item.path
  if (props.item.path.startsWith('/')) return props.item.path
  const base = props.basePath.endsWith('/') ? props.basePath.slice(0, -1) : props.basePath
  return `${base}/${props.item.path}`.replace(/\/+/g, '/')
})

const visibleChildren = computed(() => (props.item.children || []).filter((c) => !c.hidden))
</script>

<style scoped lang="scss">
.external-link {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 50px;
  padding: 0 20px;
  color: #cbd5e1;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;

  &:hover {
    background-color: #1f2937;
    color: #fff;
  }

  &.is-collapsed {
    justify-content: center;
    padding: 0;
  }

  .external-icon {
    margin-left: auto;
    font-size: 12px;
    opacity: 0.6;
  }
}
</style>
