/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 认证接口：验证码、登录、用户信息、动态路由、退出
 */

import request from '@/utils/request'
import type { RouterItem } from '@/types'

export interface LoginBody {
  username: string
  password: string
  code?: string
  uuid?: string
  rememberMe?: boolean
}

export function getCaptcha() {
  return request.get<{ uuid: string; img: string; enabled: boolean }>('/auth/captcha')
}

export function login(data: LoginBody) {
  return request.post<{ token: string; tokenName: string; expiresIn: number }>('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getInfo() {
  return request.get<any>('/auth/info')
}

export function getRouters() {
  return request.get<RouterItem[]>('/auth/routers')
}

export function updateProfile(data: any) {
  return request.put('/profile', data)
}

export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/profile/password', data)
}
