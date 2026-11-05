<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 顶部导航栏：面包屑、消息徽标、主题切换、用户菜单
-->

<template>
  <div class="navbar" :class="{ compact }">
    <div class="left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Expand v-if="appStore.sidebarCollapsed || compact" />
        <Fold v-else />
      </el-icon>
      <el-breadcrumb separator="/" class="crumbs">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.meta?.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <el-tooltip content="刷新页面" placement="bottom">
        <el-icon class="nav-icon" @click="refresh"><Refresh /></el-icon>
      </el-tooltip>
      <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
        <el-icon class="nav-icon" @click="toggleFullscreen">
          <Aim />
        </el-icon>
      </el-tooltip>
      <el-tooltip :content="`在线用户 ${onlineStore.count}`" placement="bottom">
        <el-badge :value="onlineStore.count" :hidden="onlineStore.count === 0" :max="99" class="nav-icon"
          @click="goOnline">
          <el-icon><Connection /></el-icon>
        </el-badge>
      </el-tooltip>
      <el-tooltip content="未读消息" placement="bottom">
        <el-badge :value="unread" :hidden="unread === 0" :max="99" class="nav-icon" @click="goMessage">
          <el-icon><ChatDotRound /></el-icon>
        </el-badge>
      </el-tooltip>

      <el-dropdown trigger="click" @command="handleTheme">
        <el-icon class="nav-icon theme-dot" title="主题设置">
          <Brush />
          <span class="dot" :style="{ background: currentTheme.color }" />
        </el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="t in THEMES" :key="t.key" :command="t.key"
              :class="{ active: t.key === appStore.theme }">
              <span class="theme-swatch" :style="{ background: t.color }" />{{ t.name }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="compact ? 26 : 30" :src="userStore.avatar">
            {{ (userStore.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <span v-if="!compact" class="name">{{ userStore.nickname }}</span>
          <el-icon class="caret"><CaretBottom /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" icon="User">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useAppStore, THEMES } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useOnlineStore } from '@/stores/online'
import { unreadCount } from '@/api/extras'
import { connectWebSocket, disconnectWebSocket } from '@/utils/websocket'

const props = withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })
const compact = computed(() => props.compact)

const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()
const isFullscreen = ref(false)
const onlineStore = useOnlineStore()
const unread = ref(0)
let unreadTimer: number | undefined

const currentTheme = computed(() => THEMES.find((t) => t.key === appStore.theme) || THEMES[0])

const breadcrumbs = computed(() =>
  route.matched.filter((r) => r.meta?.title && r.name !== 'Dashboard' && r.path !== '/')
)

function handleTheme(key: string) {
  appStore.applyTheme(key)
}

function refresh() {
  window.location.reload()
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

async function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录?', '提示', { type: 'warning' })
    await userStore.logout()
    permissionStore.reset()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

async function loadUnread() {
  try {
    unread.value = Number(await unreadCount()) || 0
  } catch {
    // 忽略
  }
}

function goMessage() {
  router.push('/system/message')
}

function goOnline() {
  router.push('/monitor/online')
}

onMounted(async () => {
  appStore.applyTheme()
  onlineStore.startPolling()
  loadUnread()
  unreadTimer = window.setInterval(loadUnread, 30000)
  connectWebSocket((payload) => {
    if (payload?.type === 'message') {
      unread.value += 1
      ElNotification({
        title: payload.title || '新消息',
        message: payload.content || '',
        type: 'info',
        duration: 6000
      })
    }
  })
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
  }
  onlineStore.stopPolling()
  disconnectWebSocket()
})
</script>

<style scoped lang="scss">
.navbar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;

  &.compact {
    .crumbs {
      display: none;
    }
  }
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;

  .crumbs {
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    :deep(.el-breadcrumb__inner) {
      color: var(--lp-text-muted);

      &.is-link:hover {
        color: var(--lp-primary);
      }
    }

    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: var(--lp-text);
      font-weight: 500;
    }
  }
}

.collapse-btn {
  font-size: 19px;
  cursor: pointer;
  color: var(--lp-text-secondary);
  padding: 6px;
  border-radius: var(--lp-radius-sm);
  transition: background 0.18s ease, color 0.18s ease;

  &:hover {
    color: var(--lp-primary);
    background: color-mix(in srgb, var(--lp-primary) 10%, transparent);
  }
}

.right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.nav-icon {
  font-size: 17px;
  color: var(--lp-text-secondary);
  cursor: pointer;
  padding: 5px;
  border-radius: var(--lp-radius-sm);
  transition: color 0.18s ease, background 0.18s ease;
  display: inline-flex;
  align-items: center;

  &:hover {
    color: var(--lp-primary);
    background: color-mix(in srgb, var(--lp-primary) 10%, transparent);
  }
}

.theme-dot {
  position: relative;

  .dot {
    position: absolute;
    right: -4px;
    bottom: -2px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    border: 1.5px solid var(--lp-surface, #fff);
  }
}

.theme-swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: -1px;
}

:global(.el-dropdown-menu__item.active) {
  color: var(--lp-primary);
  background: color-mix(in srgb, var(--lp-primary) 10%, transparent);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--lp-radius-sm);
  transition: background 0.18s ease;
  max-width: 180px;

  &:hover {
    background: color-mix(in srgb, var(--lp-primary) 10%, transparent);
  }

  .name {
    font-size: 13px;
    color: var(--lp-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .caret {
    font-size: 12px;
    color: var(--lp-text-muted);
  }
}

@media (max-width: 992px) {
  .right {
    gap: 8px;
  }

  .nav-icon {
    font-size: 16px;
    padding: 4px;
  }
}
</style>
