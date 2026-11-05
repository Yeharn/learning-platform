/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 代码生成器接口：表导入、字段编辑、预览与下载
 */

import request from '@/utils/request'
import type { PageResult } from '@/types'

export interface GenTable {
  tableId?: string | number
  tableName?: string
  tableComment?: string
  className?: string
  tplCategory?: string
  packageName?: string
  moduleName?: string
  businessName?: string
  functionName?: string
  functionAuthor?: string
  genType?: string
  genPath?: string
  dsKey?: string
  remark?: string
  createTime?: string
  columns?: any[]
}

export function listGenTable(params: any) {
  return request.get<PageResult<GenTable>>('/tool/gen/list', { params })
}

export function listDbTables(dsKey?: string) {
  return request.get<any[]>('/tool/gen/db/list', { params: { dsKey } })
}

export function listDbColumns(dsKey: string | undefined, tableName: string) {
  return request.get<any[]>('/tool/gen/db/columns', { params: { dsKey, tableName } })
}

export function importTables(tables: string[], dsKey: string) {
  return request.post('/tool/gen/import', null, { params: { tables: tables.join(','), dsKey } })
}

export function getGenTable(tableId: any) {
  return request.get<GenTable>(`/tool/gen/${tableId}`)
}

export function updateGenTable(data: GenTable) {
  return request.put('/tool/gen', data)
}

export function delGenTable(ids: any[]) {
  return request.delete(`/tool/gen/${ids.join(',')}`)
}

export function previewGen(tableId: any) {
  return request.get<Record<string, string>>(`/tool/gen/preview/${tableId}`)
}

export function downloadGen(tableId: any) {
  return request.download(`/tool/gen/download/${tableId}`)
}
