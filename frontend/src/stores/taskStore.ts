import { create } from 'zustand'
import type { Task, TaskStage } from '@/types/task'
import { fetchTaskStatus } from '@/api/tasks'

interface TaskState {
  taskId: string | null
  stage: TaskStage | null
  progress: number
  status: Task['status'] | null
  polling: boolean
  pollTimer: ReturnType<typeof setInterval> | null

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
  pollTimer: null,

  startPolling: (taskId: string) => {
    get().stopPolling()
    set({ taskId, polling: true, progress: 0, status: 'pending' })

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
        get().stopPolling()
      }
    }, 1000)

    set({ pollTimer: timer })
  },

  stopPolling: () => {
    const { pollTimer } = get()
    if (pollTimer) {
      clearInterval(pollTimer)
      set({ pollTimer: null, polling: false })
    }
  },

  clear: () => {
    get().stopPolling()
    set({
      taskId: null,
      stage: null,
      progress: 0,
      status: null,
      polling: false,
    })
  },
}))
