import { create } from 'zustand'
import type { Task, TaskStage } from '@/types/task'
import { fetchTaskStatus } from '@/api/tasks'
import { subscribeTaskProgress } from '@/api/ws'
import type { WsProgressEvent } from '@/api/ws'

interface TaskState {
  taskId: string | null
  stage: TaskStage | null
  progress: number
  status: Task['status'] | null
  polling: boolean
  usingWebSocket: boolean
  pollTimer: ReturnType<typeof setInterval> | null
  wsCleanup: (() => void) | null

  startPolling: (taskId: string) => void
  stopPolling: () => void
  clear: () => void
}

export const useTaskStore = create<TaskState>((set, get) => ({
  taskId: null,
  stage: null,
  progress: 0,
  status: null,
  polling: false,
  usingWebSocket: false,
  pollTimer: null,
  wsCleanup: null,

  startPolling: (taskId: string) => {
    get().stopPolling()
    set({ taskId, polling: true, progress: 0, status: 'pending', stage: null })

    // 优先尝试 WebSocket
    const handleWsEvent = (event: WsProgressEvent) => {
      if (event.type === 'progress') {
        set({
          stage: event.stage || get().stage,
          progress: event.progress ?? get().progress,
          status: 'processing',
        })
      } else if (event.type === 'completed') {
        set({ status: 'completed', progress: 100 })
        get().stopPolling()
      } else if (event.type === 'failed') {
        set({ status: 'failed' })
        get().stopPolling()
      }
    }

    const { cleanup, usingWebSocket } = subscribeTaskProgress(taskId, handleWsEvent)

    if (usingWebSocket) {
      set({ usingWebSocket: true, wsCleanup: cleanup })
      return
    }

    // WebSocket 不可用，降级为轮询
    set({ usingWebSocket: false })

    const timer = setInterval(async () => {
      try {
        const task: Task = await fetchTaskStatus(taskId)
        set({
          stage: task.stage,
          progress: task.progress,
          status: task.status,
        })
        if (task.status === 'completed' || task.status === 'failed') {
          get().stopPolling()
        }
      } catch {
        // 轮询出错，继续重试（不立即停止）
      }
    }, 1000)

    set({ pollTimer: timer })
  },

  stopPolling: () => {
    const { pollTimer, wsCleanup } = get()
    if (pollTimer) {
      clearInterval(pollTimer)
      set({ pollTimer: null })
    }
    if (wsCleanup) {
      wsCleanup()
      set({ wsCleanup: null })
    }
    set({ polling: false, usingWebSocket: false })
  },

  clear: () => {
    get().stopPolling()
    set({
      taskId: null,
      stage: null,
      progress: 0,
      status: null,
      polling: false,
      usingWebSocket: false,
    })
  },
}))
