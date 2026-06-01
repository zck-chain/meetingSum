export type AsrProvider = 'whisper_local' | 'aliyun' | 'xunfei'
export type LlmProvider = 'claude' | 'openai'
export type ExportFormat = 'md' | 'docx' | 'pdf' | 'txt'

/** 摘要模板可用变量 */
export const TEMPLATE_VARIABLES = [
  { key: '{{title}}', label: '会议标题', description: '会议名称或文件名' },
  { key: '{{summary}}', label: '摘要正文', description: 'AI 生成的摘要内容' },
  { key: '{{key_points}}', label: '关键讨论点', description: '里程碑/讨论点列表' },
  { key: '{{decisions}}', label: '决策记录', description: '会议中做出的决定' },
  { key: '{{action_items}}', label: '行动项', description: '待办事项与责任人' },
  { key: '{{tags}}', label: '标签', description: '自动/手动标注的标签' },
  { key: '{{date}}', label: '会议日期', description: '会议创建日期' },
  { key: '{{duration}}', label: '会议时长', description: '视频/音频时长' },
] as const

export interface Settings {
  asrProvider: AsrProvider
  llmProvider: LlmProvider
  /** 自定义摘要模板 — 空字符串表示使用默认模板 */
  summaryTemplate: string
}
