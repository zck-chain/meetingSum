export type TaskStage =
  | 'extracting_audio'
  | 'transcribing'
  | 'summarizing'
  | 'exporting'

export type TaskStatus = 'pending' | 'processing' | 'completed' | 'failed'

export interface Task {
  id: string
  meeting_id: string
  stage: TaskStage
  progress: number
  status: TaskStatus
  result_path: string | null
  created_at: string
  completed_at: string | null
  error_code?: string
  error_detail?: string
}

export interface ProgressEvent {
  stage: TaskStage
  percent: number
}
