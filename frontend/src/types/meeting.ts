export type MeetingStatus =
  | 'pending'
  | 'processing'
  | 'completed'
  | 'failed'
  | 'cancelled'

export type Importance = 'high' | 'medium' | 'low'

export interface KeyPoint {
  topic: string
  content: string
  importance: Importance
}

export interface Decision {
  content: string
  proposer: string
}

export interface ActionItem {
  content: string
  assignee: string
  deadline: string | null
}

export interface SummaryData {
  title: string
  summary: string
  key_points: KeyPoint[]
  decisions: Decision[]
  action_items: ActionItem[]
  tags: string[]
}

export interface Meeting {
  id: string
  title: string
  original_file: string
  original_format: string
  duration_seconds: number
  file_size_bytes: number
  status: MeetingStatus
  transcript_text: string | null
  summary_json: SummaryData | null
  error_message: string | null
  created_at: string
  updated_at: string
}

export interface MeetingListResponse {
  items: Meeting[]
  total: number
  page: number
  page_size: number
}
