<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 后台整体布局容器
-->

<template>
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="layout-aside">
      <Sidebar />
    </el-aside>
    <el-container class="layout-body">
      <el-header height="56px" class="layout-header">
        <Navbar />
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
  width: 100%;
}

.layout-aside {
  transition: width 0.25s ease;
  background: var(--lp-sidebar-bg, rgba(15, 23, 42, 0.94));
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.layout-body {
  height: 100%;
  overflow: hidden;
}

.layout-header {
  background: var(--lp-card-bg, rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(18px) saturate(1.4);
  border-bottom: 1px solid var(--lp-card-border, rgba(255, 255, 255, 0.6));
  padding: 0 16px;
  display: flex;
  align-items: center;
}

.layout-main {
  padding: 0;
  overflow-y: auto;
  background: transparent;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>
