/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 路由表与全局守卫：登录校验、动态路由注入
 */

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '403' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '控制台', icon: 'Odometer', affix: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

const whiteList = ['/login', '/404', '/403']

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (whiteList.includes(to.path)) {
    if (to.path === '/login' && userStore.token) {
      next('/')
      return
    }
    next()
    return
  }

  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (!permissionStore.loaded) {
    try {
      await userStore.fetchInfo()
      const routes = await permissionStore.generateRoutes()
      routes.forEach((route) => router.addRoute(route))
      router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } })
      next({ ...to, replace: true })
    } catch (e) {
      userStore.reset()
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }
  next()
})

router.afterEach((to) => {
  const title = to.meta?.title ? `${to.meta.title} - ` : ''
  document.title = `${title}Learning Platform`
})

export default router
