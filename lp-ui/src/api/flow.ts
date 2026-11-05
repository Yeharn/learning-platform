/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程中心接口：定义、设计器、实例、任务动作、审批记录与沟通
 */

import request from '@/utils/request'

export interface FlowCandidates {
  id: number | string
  username: string
  name: string
  deptId?: number | string
}

/* ---------------- 流程定义 ---------------- */

export function flowDefinitions(keyword?: string) {
  return request.get<any[]>('/flow/definitions', { params: { keyword } })
}

export function flowModel(id: string | number) {
  return request.get<string>(`/flow/definitions/${id}/model`)
}

export function deployFlowJson(data: { id?: number | string; model: string; remark?: string }) {
  return request.post<number>('/flow/deploy', data)
}

export function flowRemove(id: string | number) {
  return request.delete(`/flow/definitions/${id}`)
}

export function flowStart(data: { processId?: number | string; key?: string; variables?: Record<string, any> }) {
  return request.post<number>('/flow/start', data)
}

/* ---------------- 分页查询 ---------------- */

export function flowTaskPage(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request.get<any>('/flow/tasks/page', { params })
}

export function flowDonePage(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request.get<any>('/flow/tasks/done', { params })
}

export function flowInstancePage(params: {
  keyword?: string
  state?: string
  pageNum: number
  pageSize: number
}) {
  return request.get<any>('/flow/instances/page', { params })
}

export function flowInstanceDetail(id: string | number) {
  return request.get<any>(`/flow/instances/${id}/detail`)
}

export function flowRecords(id: string | number) {
  return request.get<any[]>(`/flow/instances/${id}/records`)
}

export function flowRejectNodes(id: string | number) {
  return request.get<any[]>(`/flow/instances/${id}/reject-nodes`)
}

export function flowCandidates(keyword?: string) {
  return request.get<FlowCandidates[]>('/flow/candidates', { params: { keyword } })
}

/* ---------------- 任务动作 ---------------- */

export function completeTask(taskId: string | number, data: { comment?: string; variables?: Record<string, any> }) {
  return request.post(`/flow/tasks/${taskId}/complete`, data)
}

export function rejectTask(taskId: string | number, data: { comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/reject`, data)
}

export function rejectToTask(taskId: string | number, data: { nodeKey?: string; comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/reject-to`, data)
}

export function addSignTask(taskId: string | number, data: { userIds: (number | string)[]; comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/add-sign`, data)
}

export function reduceSignTask(taskId: string | number, data: { actorId: string; comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/reduce-sign`, data)
}

export function transferTask(taskId: string | number, data: { userId: number | string }) {
  return request.post(`/flow/tasks/${taskId}/transfer`, data)
}

export function delegateTask(taskId: string | number, data: { userId: number | string }) {
  return request.post(`/flow/tasks/${taskId}/delegate`, data)
}

export function reclaimTask(taskId: string | number) {
  return request.post(`/flow/tasks/${taskId}/reclaim`)
}

export function withdrawTask(taskId: string | number) {
  return request.post(`/flow/tasks/${taskId}/withdraw`)
}

export function readTask(taskId: string | number) {
  return request.post(`/flow/tasks/${taskId}/read`)
}

/* ---------------- 实例管控 ---------------- */

export function suspendInstance(id: string | number) {
  return request.post(`/flow/instances/${id}/suspend`)
}

export function activateInstance(id: string | number) {
  return request.post(`/flow/instances/${id}/activate`)
}

export function terminateInstance(id: string | number) {
  return request.post(`/flow/instances/${id}/terminate`)
}

export function removeInstance(id: string | number) {
  return request.delete(`/flow/instances/${id}`)
}

/* ---------------- 沟通 ---------------- */

export function flowComments(id: string | number) {
  return request.get<any[]>(`/flow/instances/${id}/comments`)
}

export function addFlowComment(id: string | number, data: { taskId?: string | number; content: string }) {
  return request.post(`/flow/instances/${id}/comments`, data)
}

/* ---------------- 表单模板 ---------------- */

export function flowForms(keyword?: string) {
  return request.get<any[]>('/flow/forms', { params: { keyword } })
}

export function flowForm(id: string | number) {
  return request.get<any>(`/flow/forms/${id}`)
}

export function flowFormByCode(formCode: string) {
  return request.get<any>('/flow/forms/by-code', { params: { formCode } })
}

export function saveFlowForm(data: any) {
  return request.post<number>('/flow/forms', data)
}

export function removeFlowForm(id: string | number) {
  return request.delete(`/flow/forms/${id}`)
}

/* ---------------- 设计器预览校验 / 组织数据 ---------------- */

export function flowDesignPreview(model: string) {
  return request.post<any>('/flow/design/preview', { model })
}

export function flowRoles() {
  return request.get<any[]>('/flow/roles')
}

export function flowDepts() {
  return request.get<any[]>('/flow/depts')
}

/* ---------------- 任意跳转 / 流程变量 / 已阅 ---------------- */

export function jumpTask(taskId: string | number, data: { nodeKey: string; comment?: string }) {
  return request.post(`/flow/tasks/${taskId}/jump`, data)
}

export function flowVariables(id: string | number) {
  return request.get<any>(`/flow/instances/${id}/variables`)
}

export function flowReads(id: string | number) {
  return request.get<any[]>(`/flow/instances/${id}/reads`)
}

