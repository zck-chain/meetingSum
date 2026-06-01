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

/** 说话人分段 — 转录文本按说话人切分 */
export interface SpeakerSegment {
  speaker: string
  speaker_label: string // e.g. "Speaker_A", "Speaker_B", or identified name
  text: string
  start_time: number // seconds from start
  end_time: number   // seconds from start
}

/** 转录数据结构 — 支持纯文本或说话人分段两种格式 */
export interface TranscriptData {
  full_text: string | null
  segments: SpeakerSegment[] | null
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
  transcript_data: TranscriptData | null
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
