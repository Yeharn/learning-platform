/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 在线人数状态：顶部徽标与在线用户列表页共用，保证数字联动
 */

import { defineStore } from 'pinia'
import { onlineCount } from '@/api/system'

export const useOnlineStore = defineStore('online', {
  state: () => ({
    count: 0,
    timer: 0
  }),
  actions: {
    async refresh() {
      try {
        this.count = Number(await onlineCount()) || 0
      } catch {
        this.count = 0
      }
      return this.count
    },
    startPolling(interval = 60000) {
      this.refresh()
      if (this.timer) return
      this.timer = window.setInterval(() => this.refresh(), interval)
    },
    stopPolling() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = 0
      }
    }
  }
})
