/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统监控接口：服务器、Redis、MySQL、慢 SQL 审计与 KILL
 */

import request from '@/utils/request'

export function serverInfo() {
  return request.get<any>('/monitor/server')
}

export function redisInfo() {
  return request.get<any>('/monitor/redis')
}

export function mysqlInfo() {
  return request.get<any>('/monitor/mysql')
}

export function mysqlSlowSqls(limit = 20) {
  return request.get<any>('/monitor/mysql/slow', { params: { limit } })
}

export function mysqlLongQueries(threshold = 3) {
  return request.get<any>('/monitor/mysql/long', { params: { threshold } })
}

export function mysqlKill(threadId: number | string) {
  return request.post<any>('/monitor/mysql/kill', null, { params: { threadId } })
}

export function serviceOverview() {
  return request.get<any>('/monitor/service')
}
