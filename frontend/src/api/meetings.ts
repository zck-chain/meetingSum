import { apiClient } from './client'
import type { Meeting, MeetingListResponse } from '@/types/meeting'

export async function fetchMeetings(params: {
  page?: number
  page_size?: number
  search?: string
}): Promise<MeetingListResponse> {
  const { data } = await apiClient.get<MeetingListResponse>('/meetings', {
    params,
  })
  return data
}

export async function fetchMeeting(id: string): Promise<Meeting> {
  const { data } = await apiClient.get<Meeting>(`/meetings/${id}`)
  return data
}

export async function uploadMeeting(
  file: File,
  onProgress?: (percent: number) => void,
): Promise<{ meeting: Meeting; task_id: string }> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await apiClient.post<{
    meeting: Meeting
    task_id: string
  }>('/meetings/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (e.total && onProgress) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    },
  })
  return data
}

export async function deleteMeeting(
  id: string,
): Promise<{ success: boolean }> {
  const { data } = await apiClient.delete<{ success: boolean }>(
    `/meetings/${id}`,
  )
  return data
}
