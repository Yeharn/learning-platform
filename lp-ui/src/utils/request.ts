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
