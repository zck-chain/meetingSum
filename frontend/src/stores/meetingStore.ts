import { create } from 'zustand'
import type { Meeting, MeetingListResponse } from '@/types/meeting'
import * as meetingsApi from '@/api/meetings'

interface MeetingState {
  meetings: Meeting[]
  total: number
  currentMeeting: Meeting | null
  loading: boolean
  error: string | null

  fetchMeetings: (params?: {
    page?: number
    page_size?: number
    search?: string
  }) => Promise<void>
  fetchMeeting: (id: string) => Promise<void>
  uploadMeeting: (
    file: File,
    onProgress?: (pct: number) => void,
  ) => Promise<string | null>
  deleteMeeting: (id: string) => Promise<boolean>
  clearCurrent: () => void
}

export const useMeetingStore = create<MeetingState>((set, get) => ({
  meetings: [],
  total: 0,
  currentMeeting: null,
  loading: false,
  error: null,

  fetchMeetings: async (params) => {
    set({ loading: true, error: null })
    try {
      const data: MeetingListResponse =
        await meetingsApi.fetchMeetings(params || {})
      set({ meetings: data.items, total: data.total, loading: false })
    } catch {
      set({ error: '获取会议列表失败', loading: false })
    }
  },

  fetchMeeting: async (id) => {
    set({ loading: true, error: null })
    try {
      const meeting = await meetingsApi.fetchMeeting(id)
      set({ currentMeeting: meeting, loading: false })
    } catch {
      set({ error: '获取会议详情失败', loading: false })
    }
  },

  uploadMeeting: async (file, onProgress) => {
    set({ loading: true, error: null })
    try {
      const { task_id } = await meetingsApi.uploadMeeting(file, onProgress)
      await get().fetchMeetings()
      set({ loading: false })
      return task_id
    } catch {
      set({ error: '上传文件失败', loading: false })
      return null
    }
  },

  deleteMeeting: async (id) => {
    try {
      await meetingsApi.deleteMeeting(id)
      await get().fetchMeetings()
      return true
    } catch {
      set({ error: '删除会议失败' })
      return false
    }
  },

  clearCurrent: () => set({ currentMeeting: null }),
}))
