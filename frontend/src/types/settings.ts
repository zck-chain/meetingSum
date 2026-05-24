export type AsrProvider = 'whisper_local' | 'aliyun' | 'xunfei'
export type LlmProvider = 'claude' | 'openai'
export type ExportFormat = 'md' | 'docx' | 'pdf' | 'txt'

export interface Settings {
  asrProvider: AsrProvider
  llmProvider: LlmProvider
}
