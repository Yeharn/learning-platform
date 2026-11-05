/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 扩展接口：文件、消息、定时任务、流程引擎、日志月份
 */

import request from '@/utils/request'

/* ---------------- 文件服务（lp-file） ---------------- */
export function listFile(params: any) {
  return request.get<any>('/file/list', { params })
}
export function fileModeInfo() {
  return request.get<any>('/file/mode')
}
export function switchFileMode(mode: string) {
  return request.put(`/file/mode/${mode}`)
}
export function uploadFile(category: string, files: File[]) {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return request.post('/file/upload', form, { params: { category } })
}
export function delFile(ids: Array<string | number>) {
  return request.delete(`/file/${ids.join(',')}`)
}
export function fileUrl(url: string) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  return base + url
}

/* ---------------- 消息中心 ---------------- */
export function listMessage(params: any) {
  return request.get<any>('/system/message/list', { params })
}
export function messageModeInfo() {
  return request.get<any>('/system/message/mode')
}
export function switchMessageMode(mode: string) {
  return request.put(`/system/message/mode/${mode}`)
}
export function unreadCount() {
  return request.get<number>('/system/message/unread-count')
}
export function sendMessage(data: { receiverIds: number[]; title: string; content?: string; msgType?: string }) {
  return request.post('/system/message/send', data)
}
export function readMessages(ids: Array<string | number>) {
  return request.put('/system/message/read', ids)
}
export function readAllMessages() {
  return request.put('/system/message/read-all')
}
export function delMessage(ids: Array<string | number>) {
  return request.delete(`/system/message/${ids.join(',')}`)
}

/* ---------------- 定时任务 ---------------- */
export function listJob(params: any) {
  return request.get<any>('/tool/job/list', { params })
}
export function jobInfo() {
  return request.get<any>('/tool/job/info')
}
export function addJob(data: any) {
  return request.post('/tool/job', data)
}
export function updateJob(data: any) {
  return request.put('/tool/job', data)
}
export function delJob(ids: Array<string | number>) {
  return request.delete(`/tool/job/${ids.join(',')}`)
}
export function changeJobStatus(data: any) {
  return request.put('/tool/job/changeStatus', data)
}
export function runJob(jobId: number | string) {
  return request.post(`/tool/job/run/${jobId}`)
}

/* ---------------- 流程引擎（lp-flow / FlowLong） ---------------- */
export function flowDefinitions(keyword?: string) {
  return request.get<any>('/flow/definitions', { params: { keyword } })
}
export function flowModel(id: string | number) {
  return request.get<string>(`/flow/definitions/${id}/model`)
}
export function deployFlowModel(data: {
  id?: number | string
  key: string
  name: string
  nodes: Array<{ name: string; assigneeId: string; assigneeName?: string }>
  cc?: Array<{ assigneeId: string; assigneeName?: string }>
  remark?: string
}) {
  return request.post('/flow/model', data)
}
export function deployFlowJson(data: { id?: number | string; model: string; remark?: string }) {
  return request.post('/flow/deploy', data)
}
export function flowRemove(id: string | number) {
  return request.delete(`/flow/definitions/${id}`)
}
export function flowStart(data: { processId?: number | string; key?: string; variables?: Record<string, any> }) {
  return request.post('/flow/start', data)
}
export function myTasks() {
  return request.get<any>('/flow/tasks')
}
export function allTasks() {
  return request.get<any>('/flow/all-tasks')
}
export function completeTask(taskId: string | number, data: { variables?: Record<string, any>; comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/complete`, data)
}
export function rejectTask(taskId: string | number, data: { comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/reject`, data)
}
export function flowInstances() {
  return request.get<any>('/flow/instances')
}
export function flowHistory(onlyMine = false) {
  return request.get<any>('/flow/history', { params: { onlyMine } })
}

/* ---------------- 日志月份 ---------------- */
export function logMonths(type = 'oper') {
  return request.get<string[]>('/log/months', { params: { type } })
}
