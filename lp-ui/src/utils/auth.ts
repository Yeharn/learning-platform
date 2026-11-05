/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 本地 token 读写
 */

const TOKEN_KEY = 'lp-token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
