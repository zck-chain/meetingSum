/**
 * WebSocket 客户端 — 任务进度实时推送
 *
 * 策略：优先尝试 WebSocket 连接，失败/超时自动降级为轮询。
 * 后端尚未实现 WebSocket 时，直接使用轮询模式。
 */

import type { Task, TaskStage } from '@/types/task'

export type WsEventType = 'progress' | 'completed' | 'failed' | 'error'

export interface WsProgressEvent {
  type: WsEventType
  stage?: TaskStage
  progress?: number
  task?: Task
  error?: string
  error_code?: string
}

type WsCallback = (event: WsProgressEvent) => void

const WS_CONNECT_TIMEOUT = 3000 // 3 秒内连不上就降级

function getWsUrl(taskId: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host
  return `${protocol}://${host}/ws/tasks/${taskId}`
}

/**
 * 尝试建立 WebSocket 连接，超时或失败返回 null（调用方降级为轮询）
 */
function connectWebSocket(taskId: string, onEvent: WsCallback): {
  close: () => void
} | null {
  let ws: WebSocket | null = null
  let settled = false

  try {
    ws = new WebSocket(getWsUrl(taskId))
  } catch {
    return null // 浏览器不支持或 URL 非法
  }

  const timeout = setTimeout(() => {
    if (!settled) {
      settled = true
      ws?.close()
    }
  }, WS_CONNECT_TIMEOUT)

  ws.onopen = () => {
    clearTimeout(timeout)
    settled = true
  }

  ws.onmessage = (msg) => {
    try {
      const event: WsProgressEvent = JSON.parse(msg.data)
      onEvent(event)
    } catch {
      // 忽略无法解析的消息
    }
  }

  ws.onerror = () => {
    if (!settled) {
      clearTimeout(timeout)
      settled = true
      // 连接失败，调用方会走降级逻辑
    }
  }

  ws.onclose = () => {
    clearTimeout(timeout)
  }

  return {
    close: () => {
      clearTimeout(timeout)
      settled = true
      ws?.close()
    },
  }
}

/**
 * 订阅任务进度 — 优先 WebSocket，降级返回 false
 *
 * 返回 true 表示已建立 WebSocket 连接，false 表示调用方应使用轮询。
 * 返回的 cleanup 函数用于取消订阅。
 */
export function subscribeTaskProgress(
  taskId: string,
  onEvent: WsCallback,
): { cleanup: () => void; usingWebSocket: boolean } {
  const ws = connectWebSocket(taskId, onEvent)

  if (ws) {
    return {
      usingWebSocket: true,
      cleanup: () => ws.close(),
    }
  }

  return {
    usingWebSocket: false,
    cleanup: () => {},
  }
}
