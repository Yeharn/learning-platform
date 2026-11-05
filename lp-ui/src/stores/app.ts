/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 应用状态：侧边栏折叠、主题切换与持久化
 */

import { defineStore } from 'pinia'

export interface ThemeOption {
  key: string
  name: string
  color: string
}

export const THEMES: ThemeOption[] = [
  { key: 'aurora', name: '极光蓝', color: '#3b82f6' },
  { key: 'violet', name: '暮光紫', color: '#8b5cf6' },
  { key: 'emerald', name: '青碧', color: '#10b981' },
  { key: 'rose', name: '玫瑰', color: '#f43f5e' },
  { key: 'midnight', name: '暗夜', color: '#6366f1' }
]

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('lp-sidebar') === '1',
    theme: localStorage.getItem('lp-theme') || 'aurora'
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('lp-sidebar', this.sidebarCollapsed ? '1' : '0')
    },
    applyTheme(theme?: string) {
      if (theme) {
        this.theme = theme
      }
      localStorage.setItem('lp-theme', this.theme)
      const root = document.documentElement
      root.dataset.theme = this.theme
      root.classList.toggle('lp-dark', this.theme === 'midnight')
    }
  }
})
