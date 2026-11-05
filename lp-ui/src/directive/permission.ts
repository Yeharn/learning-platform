/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 按钮级权限指令 v-hasPerm
 */

import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

const hasPerm: Directive = {
  mounted(el: HTMLElement, binding) {
    const userStore = useUserStore()
    const value = binding.value
    const perms: string[] = Array.isArray(value) ? value : [value]
    const pass = perms.some((p) => userStore.hasPermission(p))
    if (!pass) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupDirectives(app: App) {
  app.directive('hasPerm', hasPerm)
}
