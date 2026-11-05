/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 用户状态：token、用户信息、角色与权限集合
 */

import { defineStore } from 'pinia'
import { getInfo, login as loginApi, logout as logoutApi, type LoginBody } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

interface UserState {
  token: string
  userInfo: any
  roles: string[]
  permissions: string[]
  isAdmin: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    userInfo: null,
    roles: [],
    permissions: [],
    isAdmin: false
  }),
  getters: {
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    avatar: (state) => state.userInfo?.avatar || ''
  },
  actions: {
    async login(data: LoginBody) {
      const res = await loginApi(data)
      this.token = res.token
      setToken(res.token)
      return res
    },
    async fetchInfo() {
      const res = await getInfo()
      this.userInfo = res.user
      this.roles = res.roles || []
      this.permissions = res.permissions || []
      this.isAdmin = !!res.admin
      return res
    },
    hasPermission(perm: string) {
      if (!perm) return true
      if (this.isAdmin || this.permissions.includes('*:*:*')) return true
      return this.permissions.includes(perm)
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略
      } finally {
        this.reset()
      }
    },
    reset() {
      removeToken()
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.permissions = []
      this.isAdmin = false
    }
  }
})
