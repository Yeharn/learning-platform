/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: WebSocket 客户端：心跳保活、断线重连、消息推送
 */

/**
 * 消息推送 WebSocket 客户端
 * 连接 /ws/message?token=xxx，收到消息与心跳由回调处理
 */
type MessageHandler = (payload: any) => void

let socket: WebSocket | null = null
let heartbeatTimer: number | undefined
let reconnectTimer: number | undefined
let handler: MessageHandler | null = null

export function connectWebSocket(onMessage: MessageHandler) {
  handler = onMessage
  if (socket && socket.readyState <= WebSocket.OPEN) {
    return
  }
  const token = localStorage.getItem('lp-token')
  if (!token) {
    return
  }
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const base = import.meta.env.VITE_APP_BASE_API || ''
  socket = new WebSocket(`${proto}://${location.host}${base}/ws/message?token=${token}`)

  socket.onopen = () => {
    heartbeatTimer = window.setInterval(() => {
      if (socket?.readyState === WebSocket.OPEN) {
        socket.send('ping')
      }
    }, 30000)
  }

  socket.onmessage = (event) => {
    if (event.data === 'pong') {
      return
    }
    try {
      handler?.(JSON.parse(event.data))
    } catch {
      // 忽略非 JSON 消息
    }
  }

  socket.onclose = () => {
    stopHeartbeat()
    reconnectTimer = window.setTimeout(() => connectWebSocket(onMessage), 10000)
  }
}

export function disconnectWebSocket() {
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  handler = null
  socket?.close()
  socket = null
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = undefined
  }
}
