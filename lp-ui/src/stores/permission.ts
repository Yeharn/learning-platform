/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 权限状态：动态路由与菜单数据
 */

import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import { getRouters } from '@/api/auth'
import type { RouterItem } from '@/types'
import Layout from '@/layout/index.vue'

const viewModules = import.meta.glob('../views/**/*.vue')

function loadView(component: string) {
  const key = `../views/${component}.vue`
  const loader = viewModules[key]
  if (!loader) {
    console.warn('未找到视图组件:', key)
    return () => import('@/views/error/404.vue')
  }
  return loader
}

function isExternal(path: string) {
  return /^(https?:)?\/\//.test(path)
}

function buildRoutes(items: RouterItem[], parentPath = ''): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const item of items) {
    // 外链菜单不注册路由，由侧边栏直接新窗口打开
    if (isExternal(item.path)) continue
    const fullPath = item.path.startsWith('/')
      ? item.path
      : `${parentPath}/${item.path}`.replace(/\/+/g, '/')
    if (item.children && item.children.length > 0 && item.component === 'Layout') {
      routes.push({
        path: item.path.startsWith('/') ? item.path : `/${item.path}`,
        component: Layout,
        name: item.name,
        redirect: item.redirect === 'noRedirect' ? undefined : item.redirect,
        meta: item.meta,
        children: buildRoutes(item.children, fullPath)
      } as RouteRecordRaw)
    } else if (item.children && item.children.length > 0) {
      routes.push({
        path: item.path,
        name: item.name,
        component: item.component ? loadView(item.component) : undefined,
        meta: item.meta,
        children: buildRoutes(item.children, fullPath)
      } as RouteRecordRaw)
    } else {
      routes.push({
        path: item.path.startsWith('/') ? item.path : item.path,
        name: item.name,
        component: item.component ? loadView(item.component) : undefined,
        meta: item.meta
      } as RouteRecordRaw)
    }
  }
  return routes
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    menus: [] as RouterItem[],
    routes: [] as RouteRecordRaw[],
    loaded: false
  }),
  actions: {
    async generateRoutes() {
      const data = await getRouters()
      this.menus = data || []
      this.routes = buildRoutes(this.menus)
      this.loaded = true
      return this.routes
    },
    reset() {
      this.menus = []
      this.routes = []
      this.loaded = false
    }
  }
})
