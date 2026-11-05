/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统管理接口：用户、角色、菜单、部门、字典、参数、公告、数据源、日志、在线用户
 */

import request from '@/utils/request'
import type {
  PageResult,
  SysConfig,
  SysDatasource,
  SysDept,
  SysDictData,
  SysDictType,
  SysMenu,
  SysNotice,
  SysRole,
  SysUser
} from '@/types'

/* ---------------- 用户 ---------------- */
export function listUser(params: any) {
  return request.get<PageResult<SysUser>>('/system/user/list', { params })
}
export function getUser(id: string | number) {
  return request.get<SysUser>(`/system/user/${id}`)
}
export function addUser(data: SysUser) {
  return request.post('/system/user', data)
}
export function updateUser(data: SysUser) {
  return request.put('/system/user', data)
}
export function delUser(ids: Array<string | number>) {
  return request.delete(`/system/user/${ids.join(',')}`)
}
export function resetUserPwd(userId: any, password: string) {
  return request.put('/system/user/resetPwd', { userId, password })
}
export function changeUserStatus(userId: any, status: string) {
  return request.put('/system/user/changeStatus', { userId, status })
}
export function exportUser(params: any) {
  return request.download('/system/user/export', params)
}

/* ---------------- 角色 ---------------- */
export function listRole(params: any) {
  return request.get<PageResult<SysRole>>('/system/role/list', { params })
}
export function roleOptions() {
  return request.get<SysRole[]>('/system/role/optionselect')
}
export function getRole(id: any) {
  return request.get<SysRole>(`/system/role/${id}`)
}
export function addRole(data: SysRole) {
  return request.post('/system/role', data)
}
export function updateRole(data: SysRole) {
  return request.put('/system/role', data)
}
export function delRole(ids: Array<string | number>) {
  return request.delete(`/system/role/${ids.join(',')}`)
}
export function authRole(data: { roleId: any; menuIds: any[]; deptIds: any[] }) {
  return request.put('/system/role/auth', data)
}
export function changeRoleStatus(roleId: any, status: string) {
  return request.put('/system/role/changeStatus', { roleId, status })
}
export function roleUsers(roleId: any) {
  return request.get<SysUser[]>(`/system/role/users/${roleId}`)
}

/* ---------------- 菜单 ---------------- */
export function listMenu(params?: any) {
  return request.get<SysMenu[]>('/system/menu/list', { params })
}
export function menuTreeSelect() {
  return request.get<SysMenu[]>('/system/menu/treeselect')
}
export function getMenu(id: any) {
  return request.get<SysMenu>(`/system/menu/${id}`)
}
export function addMenu(data: SysMenu) {
  return request.post('/system/menu', data)
}
export function updateMenu(data: SysMenu) {
  return request.put('/system/menu', data)
}
export function delMenu(id: any) {
  return request.delete(`/system/menu/${id}`)
}
export function roleMenuTree(roleId: any) {
  return request.get<{ menus: SysMenu[]; checkedKeys: any[] }>(`/system/menu/roleMenuTree/${roleId}`)
}

/* ---------------- 部门 ---------------- */
export function listDept(params?: any) {
  return request.get<SysDept[]>('/system/dept/list', { params })
}
export function deptTreeSelect() {
  return request.get<SysDept[]>('/system/dept/treeselect')
}
export function getDept(id: any) {
  return request.get<SysDept>(`/system/dept/${id}`)
}
export function addDept(data: SysDept) {
  return request.post('/system/dept', data)
}
export function updateDept(data: SysDept) {
  return request.put('/system/dept', data)
}
export function delDept(id: any) {
  return request.delete(`/system/dept/${id}`)
}

/* ---------------- 字典 ---------------- */
export function listDictType(params: any) {
  return request.get<PageResult<SysDictType>>('/system/dict/type/list', { params })
}
export function allDictTypes() {
  return request.get<SysDictType[]>('/system/dict/type/all')
}
export function addDictType(data: SysDictType) {
  return request.post('/system/dict/type', data)
}
export function updateDictType(data: SysDictType) {
  return request.put('/system/dict/type', data)
}
export function delDictType(ids: any[]) {
  return request.delete(`/system/dict/type/${ids.join(',')}`)
}
export function listDictData(params: any) {
  return request.get<PageResult<SysDictData>>('/system/dict/data/list', { params })
}
export function dictDataByType(dictType: string) {
  return request.get<SysDictData[]>(`/system/dict/data/type/${dictType}`)
}
export function addDictData(data: SysDictData) {
  return request.post('/system/dict/data', data)
}
export function updateDictData(data: SysDictData) {
  return request.put('/system/dict/data', data)
}
export function delDictData(ids: any[]) {
  return request.delete(`/system/dict/data/${ids.join(',')}`)
}

/* ---------------- 参数 ---------------- */
export function listConfig(params: any) {
  return request.get<PageResult<SysConfig>>('/system/config/list', { params })
}
export function getConfig(id: any) {
  return request.get<SysConfig>(`/system/config/${id}`)
}
export function addConfig(data: SysConfig) {
  return request.post('/system/config', data)
}
export function updateConfig(data: SysConfig) {
  return request.put('/system/config', data)
}
export function delConfig(ids: any[]) {
  return request.delete(`/system/config/${ids.join(',')}`)
}
export function refreshConfigCache() {
  return request.delete('/system/config/refreshCache')
}

/* ---------------- 公告 ---------------- */
export function listNotice(params: any) {
  return request.get<PageResult<SysNotice>>('/system/notice/list', { params })
}
export function latestNotice() {
  return request.get<SysNotice[]>('/system/notice/latest')
}
export function addNotice(data: SysNotice) {
  return request.post('/system/notice', data)
}
export function updateNotice(data: SysNotice) {
  return request.put('/system/notice', data)
}
export function delNotice(ids: any[]) {
  return request.delete(`/system/notice/${ids.join(',')}`)
}

/* ---------------- 数据源 ---------------- */
export function listDatasource() {
  return request.get<SysDatasource[]>('/system/datasource/list')
}
export function addDatasource(data: SysDatasource) {
  return request.post('/system/datasource', data)
}
export function updateDatasource(data: SysDatasource) {
  return request.put('/system/datasource', data)
}
export function delDatasource(id: any) {
  return request.delete(`/system/datasource/${id}`)
}
export function testDatasource(data: SysDatasource) {
  return request.post<string>('/system/datasource/test', data)
}
export function datasourcePool(dsKey: string) {
  return request.get<any>(`/system/datasource/pool/${dsKey}`)
}

/* ---------------- 日志 / 在线 ---------------- */
export function listLoginLog(params: any) {
  return request.get<PageResult<any>>('/log/login/list', { params })
}
export function delLoginLog(ids: any[], month?: string) {
  return request.delete(`/log/login/${ids.join(',')}`, { params: { month } })
}
export function cleanLoginLog(month?: string) {
  return request.delete('/log/login/clean', { params: { month } })
}
export function listOperLog(params: any) {
  return request.get<PageResult<any>>('/log/oper/list', { params })
}
export function getOperLog(id: any, month?: string) {
  return request.get<any>(`/log/oper/${id}`, { params: { month } })
}
export function delOperLog(ids: any[], month?: string) {
  return request.delete(`/log/oper/${ids.join(',')}`, { params: { month } })
}
export function cleanOperLog(month?: string) {
  return request.delete('/log/oper/clean', { params: { month } })
}
export function listOnline(username?: string) {
  return request.get<any[]>('/online/list', { params: { username } })
}
export function onlinePage(params: { username?: string; pageNum: number; pageSize: number }) {
  return request.get<PageResult<any>>('/online/page', { params })
}
export function onlineCount() {
  return request.get<number>('/online/count')
}
export function kickOnline(token: string) {
  return request.delete(`/online/${token}`)
}

/* ---------------- 控制台 ---------------- */
export function dashboard() {
  return request.get<any>('/system/dashboard')
}
