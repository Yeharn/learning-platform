/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 全局 TypeScript 类型定义
 */

export interface PageResult<T> {
  total: number
  rows: T[]
  pageNum?: number
  pageSize?: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  [key: string]: any
}

export interface SysUser {
  userId?: string | number
  deptId?: string | number | null
  username?: string
  nickname?: string
  password?: string
  email?: string
  phone?: string
  sex?: string
  avatar?: string
  status?: string
  deptName?: string
  roleIds?: Array<string | number>
  roleNames?: string
  remark?: string
  loginIp?: string
  loginDate?: string
  createTime?: string
}

export interface SysRole {
  roleId?: string | number
  roleName?: string
  roleKey?: string
  roleSort?: number
  dataScope?: string
  status?: string
  remark?: string
  menuIds?: Array<string | number>
  deptIds?: Array<string | number>
  createTime?: string
}

export interface SysMenu {
  menuId?: string | number
  menuName?: string
  parentId?: string | number
  orderNum?: number
  path?: string
  component?: string
  queryParam?: string
  isFrame?: number
  isCache?: number
  menuType?: string
  visible?: string
  status?: string
  perms?: string
  icon?: string
  remark?: string
  children?: SysMenu[]
}

export interface SysDept {
  deptId?: string | number
  parentId?: string | number
  deptName?: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: string
  ancestors?: string
  children?: SysDept[]
}

export interface SysConfig {
  configId?: string | number
  configName?: string
  configKey?: string
  configValue?: string
  configType?: string
  remark?: string
  createTime?: string
}

export interface SysDictType {
  dictId?: string | number
  dictName?: string
  dictType?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface SysDictData {
  dictCode?: string | number
  dictSort?: number
  dictLabel?: string
  dictValue?: string
  dictType?: string
  listClass?: string
  isDefault?: string
  status?: string
  remark?: string
}

export interface SysNotice {
  noticeId?: string | number
  noticeTitle?: string
  noticeType?: string
  noticeContent?: string
  status?: string
  createBy?: string
  createTime?: string
}

export interface SysDatasource {
  id?: string | number
  name?: string
  dsKey?: string
  dbType?: string
  driverClass?: string
  url?: string
  username?: string
  password?: string
  passwordMask?: string
  status?: string
  isDefault?: string
  remark?: string
  connected?: boolean
}

export interface RouterItem {
  name: string
  path: string
  hidden?: boolean
  component?: string
  redirect?: string
  query?: string
  alwaysShow?: boolean
  meta?: { title: string; icon?: string; noCache?: boolean; link?: string }
  children?: RouterItem[]
}
