import type { Meeting, SummaryData } from '@/types/meeting'
import type { Task } from '@/types/task'

function makeSummary(overrides: Partial<SummaryData> = {}): SummaryData {
  return {
    title: '未命名会议',
    summary:
      '本次会议围绕项目进展情况进行了深入讨论，各团队汇报了上周工作成果，并就下一阶段的关键里程碑达成了一致意见。会议明确了资源调配方案，并对潜在风险制定了应对措施。',
    key_points: [
      {
        topic: 'Q2 产品迭代计划',
        content:
          '产品团队汇报了 Q2 迭代计划，确定了三个核心功能的上线时间节点。移动端适配工作将于下月启动。',
        importance: 'high',
      },
      {
        topic: '技术架构升级',
        content:
          '后端团队提议将现有服务逐步迁移至微服务架构。经讨论决定先从用户服务模块开始试点，预计需要 4 周时间完成。',
        importance: 'high',
      },
      {
        topic: '用户反馈汇总',
        content:
          '近两周收集到的主要反馈集中在搜索性能和导出功能的完善度上，已安排专人跟进。',
        importance: 'medium',
      },
      {
        topic: '团队建设活动',
        content: '下周五下午组织团建活动，地点待定，由行政部协调安排。',
        importance: 'low',
      },
    ],
    decisions: [
      {
        content: 'Q2 产品迭代计划按原定时间表执行，移动端适配优先级提升至 P0',
        proposer: '张三',
      },
      {
        content: '微服务迁移从用户服务模块开始试点，技术方案于下周三前完成评审',
        proposer: '李四',
      },
      {
        content: '搜索性能优化纳入当前迭代，目标是将搜索响应时间降低 50%',
        proposer: '王五',
      },
    ],
    action_items: [
      {
        content: '完成微服务迁移技术方案文档',
        assignee: '李四',
        deadline: '2026-05-28',
      },
      {
        content: '制定搜索性能优化方案并启动开发',
        assignee: '王五',
        deadline: '2026-06-01',
      },
      {
        content: '确认团建活动场地并发送通知',
        assignee: '赵六',
        deadline: '2026-05-30',
      },
      {
        content: '输出 Q2 移动端适配需求文档',
        assignee: '张三',
        deadline: '2026-06-05',
      },
    ],
    tags: ['产品迭代', '技术架构', '团队管理'],
    ...overrides,
  }
}

export const mockMeetings: Meeting[] = [
  {
    id: 'meeting-001',
    title: 'Q2 产品迭代规划会',
    original_file: 'meeting-2026-05-20.mp4',
    original_format: 'mp4',
    duration_seconds: 5430,
    file_size_bytes: 524288000,
    status: 'completed',
    transcript_text: null,
    summary_json: makeSummary({ title: 'Q2 产品迭代规划会' }),
    error_message: null,
    created_at: '2026-05-20T09:00:00Z',
    updated_at: '2026-05-20T10:45:00Z',
  },
  {
    id: 'meeting-002',
    title: '技术架构评审会议',
    original_file: 'arch-review-2026-05-22.mp4',
    original_format: 'mp4',
    duration_seconds: 3600,
    file_size_bytes: 380000000,
    status: 'completed',
    transcript_text: null,
    summary_json: makeSummary({
      title: '技术架构评审会议',
      summary:
        '本次技术架构评审重点讨论了微服务迁移方案、数据库选型和API网关设计。经充分讨论，团队确定了基于Kong的API网关方案，并选择PostgreSQL作为主数据库。',
      tags: ['技术架构', '微服务', '数据库'],
    }),
    error_message: null,
    created_at: '2026-05-22T14:00:00Z',
    updated_at: '2026-05-22T15:45:00Z',
  },
  {
    id: 'meeting-003',
    title: '周度站会 - 5月23日',
    original_file: 'standup-2026-05-23.mp4',
    original_format: 'mp4',
    duration_seconds: 900,
    file_size_bytes: 95000000,
    status: 'completed',
    transcript_text: null,
    summary_json: makeSummary({
      title: '周度站会 - 5月23日',
      summary:
        '各团队成员汇报了本周工作进展。前端团队完成了登录模块重构，后端团队修复了数据库连接池泄漏问题。整体进度正常，无明显阻塞项。',
      tags: ['站会', '周报'],
    }),
    error_message: null,
    created_at: '2026-05-23T09:00:00Z',
    updated_at: '2026-05-23T09:45:00Z',
  },
  {
    id: 'meeting-004',
    title: '客户需求评审会',
    original_file: 'customer-review-2026-05-22.mov',
    original_format: 'mov',
    duration_seconds: 7200,
    file_size_bytes: 820000000,
    status: 'processing',
    transcript_text: null,
    summary_json: null,
    error_message: null,
    created_at: '2026-05-24T08:30:00Z',
    updated_at: '2026-05-24T08:30:00Z',
  },
  {
    id: 'meeting-005',
    title: '数据安全培训',
    original_file: 'security-training.mp4',
    original_format: 'mp4',
    duration_seconds: 2400,
    file_size_bytes: 260000000,
    status: 'completed',
    transcript_text: null,
    summary_json: makeSummary({
      title: '数据安全培训',
      summary:
        '本次培训涵盖了数据分级分类标准、敏感数据处理规范、安全编码实践等内容。全员通过了培训后的安全知识测试。强调了GDPR和国内数据安全法的合规要求。',
      tags: ['安全', '培训', '合规'],
    }),
    error_message: null,
    created_at: '2026-05-18T13:00:00Z',
    updated_at: '2026-05-18T15:30:00Z',
  },
  {
    id: 'meeting-006',
    title: '设计评审 - 新功能原型',
    original_file: 'design-review.mkv',
    original_format: 'mkv',
    duration_seconds: 1800,
    file_size_bytes: 420000000,
    status: 'failed',
    transcript_text: null,
    summary_json: null,
    error_message: '音频提取失败：文件编码格式不支持',
    created_at: '2026-05-23T16:00:00Z',
    updated_at: '2026-05-23T16:15:00Z',
  },
  {
    id: 'meeting-007',
    title: '季度总结大会',
    original_file: 'quarterly-summary.mov',
    original_format: 'mov',
    duration_seconds: 5400,
    file_size_bytes: 680000000,
    status: 'pending',
    transcript_text: null,
    summary_json: null,
    error_message: null,
    created_at: '2026-05-24T07:00:00Z',
    updated_at: '2026-05-24T07:00:00Z',
  },
]

export function createMockTask(meetingId: string): Task {
  return {
    id: `task-${meetingId}`,
    meeting_id: meetingId,
    stage: 'extracting_audio',
    progress: 0,
    status: 'pending',
    result_path: null,
    created_at: new Date().toISOString(),
    completed_at: null,
  }
}

const stageTransitions: { stage: Task['stage']; duration: number }[] = [
  { stage: 'extracting_audio', duration: 30 },
  { stage: 'transcribing', duration: 50 },
  { stage: 'summarizing', duration: 15 },
  { stage: 'exporting', duration: 5 },
]

export function simulateProgress(elapsedSeconds: number): {
  stage: Task['stage']
  progress: number
  status: Task['status']
} {
  let cumulative = 0
  for (const { stage, duration } of stageTransitions) {
    if (elapsedSeconds < cumulative + duration) {
      const stageElapsed = elapsedSeconds - cumulative
      const percent = Math.min(100, Math.round((stageElapsed / duration) * 100))
      return { stage, progress: percent, status: 'processing' }
    }
    cumulative += duration
  }
  return { stage: 'exporting', progress: 100, status: 'completed' }
}
