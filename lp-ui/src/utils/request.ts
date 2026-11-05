/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Axios 实例：请求拦截、统一响应与异常处理
 */

import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, removeToken } from './auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000
})

let relogining = false

/** 请求路径 → 目标服务名称（用于后端不可达时的友好提示） */
const SERVICE_ROUTES: Array<[RegExp, string]> = [
  [/^\/(auth|system|message|notice|workbench)/, '系统服务（lp-system）'],
  [/^\/monitor/, '监控服务（lp-monitor）'],
  [/^\/file/, '文件服务（lp-file）'],
  [/^\/flow/, '流程服务（lp-flow）']
]

function resolveServiceName(url?: string): string {
  const path = (url || '').replace(import.meta.env.VITE_APP_BASE_API, '')
  for (const [pattern, name] of SERVICE_ROUTES) {
    if (pattern.test(path)) return name
  }
  return '后端服务'
}

/** 后端不可达判定：连接拒绝/断网、网关 5xx、开发代理转发失败（500 + 非业务 JSON） */
function isBackendDown(error: any): boolean {
  const status = error.response?.status
  if (!error.response) {
    return error.code !== 'ECONNABORTED'
  }
  if (status === 502 || status === 503 || status === 504) {
    return true
  }
  return status === 500 && typeof error.response.data === 'string'
}

/** 网络类错误提示去重：并发请求失败时 3 秒内只弹一次 */
let lastNetTipAt = 0
function tipBackendDown(error: any) {
  const now = Date.now()
  if (now - lastNetTipAt < 3000) return
  lastNetTipAt = now
  const name = resolveServiceName(error.config?.url)
  ElMessage.error(`${name}未启动或不可达，请检查后端服务`)
}

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const config = response.config as AxiosRequestConfig & { responseType?: string }
    if (config.responseType === 'blob') {
      return response.data
    }
    const data = response.data
    if (data.code === 200) {
      return data.data
    }
    if (data.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(data.msg || '登录已过期'))
    }
    ElMessage.error(data.msg || '操作失败')
    return Promise.reject(new Error(data.msg || '操作失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    // 后端未启动 / 不可达：友好提示，不弹原始错误码
    if (isBackendDown(error)) {
      tipBackendDown(error)
      return Promise.reject(error)
    }
    // 请求超时
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (relogining) return
  relogining = true
  ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      removeToken()
      window.location.href = '/login'
    })
    .finally(() => {
      relogining = false
    })
}

const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config) as unknown as Promise<T>
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  },
  download(url: string, params?: any) {
    return service.get(url, { params, responseType: 'blob' }) as unknown as Promise<Blob>
  }
}

export default request
