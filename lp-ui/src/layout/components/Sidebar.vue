<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.1.0
  @Description: 侧边栏菜单容器：颜色跟随主题、支持受控折叠
-->

<template>
  <div class="sidebar">
    <div class="logo" :class="{ mini: collapsed }">
      <img src="/logo.svg" alt="logo" />
      <span v-show="!collapsed" class="logo-text">Learning Platform</span>
    </div>
    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        :text-color="menuTextColor"
        active-text-color="#ffffff"
        router
        unique-opened
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>
        <SidebarItem v-for="item in permissionStore.menus" :key="item.path" :item="item" base-path="" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SidebarItem from './SidebarItem.vue'
import { usePermissionStore } from '@/stores/permission'

const props = withDefaults(defineProps<{ collapsed?: boolean }>(), { collapsed: false })

const route = useRoute()
const permissionStore = usePermissionStore()

const activeMenu = computed(() => route.path)
const collapsed = computed(() => props.collapsed)
const menuTextColor = 'var(--lp-sidebar-text, #cbd5e1)'
</script>

<style scoped lang="scss">
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  flex-shrink: 0;

  &.mini {
    justify-content: center;
    padding: 0;
  }

  img {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .logo-text {
    color: #fff;
    font-weight: 600;
    font-size: 15px;
    white-space: nowrap;
  }
}

.menu-scroll {
  flex: 1;
  overflow-x: hidden;
}

:deep(.el-menu) {
  border-right: none;
  padding: 8px 8px 16px;
}

:deep(.el-menu--collapse) {
  padding: 8px 4px 16px;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  margin-bottom: 4px;
  border-radius: 10px;
  font-size: 13.5px;
  color: var(--lp-sidebar-text, #cbd5e1);
  transition: background 0.18s ease, color 0.18s ease;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--lp-primary) 92%, #000),
    color-mix(in srgb, var(--lp-primary-light) 80%, transparent)
  ) !important;
  color: #fff !important;
  box-shadow: 0 6px 18px color-mix(in srgb, var(--lp-primary) 35%, transparent);
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: color-mix(in srgb, var(--lp-primary) 18%, transparent) !important;
  color: #fff;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  justify-content: center;
  padding: 0 !important;
}
</style>
