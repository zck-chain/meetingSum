import { apiClient } from './client'
import type { Task } from '@/types/task'

export async function fetchTaskStatus(id: string): Promise<Task> {
  const { data } = await apiClient.get<Task>(`/tasks/${id}/status`)
  return data
}
