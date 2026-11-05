<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.1.0
  @Description: 后台整体布局容器：多分辨率自适应（窄屏自动折叠侧栏、移动端紧凑头部）
-->

<template>
  <el-container class="layout">
    <el-aside :width="asideWidth" class="layout-aside">
      <Sidebar :collapsed="effectiveCollapsed" />
    </el-aside>
    <el-container class="layout-body">
      <el-header :height="headerHeight" class="layout-header">
        <Navbar :compact="isMobile" />
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

const viewportWidth = ref(window.innerWidth)

/** 平板及以下自动折叠侧边栏（不覆盖用户偏好） */
const isNarrow = computed(() => viewportWidth.value < 992)
const isMobile = computed(() => viewportWidth.value < 768)
const effectiveCollapsed = computed(() => appStore.sidebarCollapsed || isNarrow.value)
const asideWidth = computed(() => (effectiveCollapsed.value ? '64px' : '220px'))
const headerHeight = computed(() => (isMobile.value ? '52px' : '56px'))

function onResize() {
  viewportWidth.value = window.innerWidth
}

onMounted(() => window.addEventListener('resize', onResize, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
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
  transition: height 0.2s ease;
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

@media (max-width: 768px) {
  .layout-header {
    padding: 0 10px;
  }
}
</style>
