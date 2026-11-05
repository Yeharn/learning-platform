<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 侧边栏菜单容器
-->

<template>
  <div class="sidebar">
    <div class="logo">
      <img src="/logo.svg" alt="logo" />
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">Learning Platform</span>
    </div>
    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#cbd5e1"
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
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const activeMenu = computed(() => route.path)
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
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #4f46e5, #6366f1) !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #1f2937 !important;
}
</style>
