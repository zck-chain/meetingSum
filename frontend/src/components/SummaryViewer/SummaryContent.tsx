import { Descriptions, Typography, Timeline, Checkbox, Table, Collapse, Tag, Avatar } from 'antd'
import { ClockCircleOutlined, UserOutlined } from '@ant-design/icons'
import type { SummaryData, SpeakerSegment } from '@/types/meeting'
import type { Importance } from '@/types/meeting'
import { formatDuration } from '@/utils/format'

const { Title, Paragraph, Text } = Typography

const importanceColors: Record<Importance, string> = {
  high: 'red',
  medium: 'orange',
  low: 'green',
}

const importanceLabels: Record<Importance, string> = {
  high: '重要',
  medium: '一般',
  low: '参考',
}

/** 为每个说话人分配固定颜色 */
const SPEAKER_COLORS = [
  '#1677ff', '#52c41a', '#fa8c16', '#eb2f96',
  '#722ed1', '#13c2c2', '#f5222d', '#2f54eb',
]

function getSpeakerColor(speaker: string): string {
  let hash = 0
  for (let i = 0; i < speaker.length; i++) {
    hash = speaker.charCodeAt(i) + ((hash << 5) - hash)
  }
  return SPEAKER_COLORS[Math.abs(hash) % SPEAKER_COLORS.length]
}

function formatSpeakerLabel(label: string): string {
  // 将 Speaker_A → 发言人 A，或直接返回已识别的名字
  if (label.startsWith('Speaker_')) {
    return `发言人 ${label.replace('Speaker_', '')}`
  }
  return label
}

interface Props {
  summary: SummaryData
  fileName: string
  durationSeconds: number
  createdAt: string
  transcriptText?: string | null
  transcriptSegments?: SpeakerSegment[] | null
}

export default function SummaryContent({
  summary,
  fileName,
  durationSeconds,
  createdAt,
  transcriptText,
  transcriptSegments,
}: Props) {
  const hasSegments = transcriptSegments && transcriptSegments.length > 0
  const hasText = !!transcriptText

  return (
    <div>
      <Title level={3}>{summary.title}</Title>

      <Descriptions bordered size="small" column={3} className="mb-6">
        <Descriptions.Item label="日期">
          {new Date(createdAt).toLocaleDateString('zh-CN')}
        </Descriptions.Item>
        <Descriptions.Item label="时长">
          <ClockCircleOutlined className="mr-1" />
          {formatDuration(durationSeconds)}
        </Descriptions.Item>
        <Descriptions.Item label="源文件">{fileName}</Descriptions.Item>
      </Descriptions>

      <Title level={5}>会议摘要</Title>
      <Paragraph className="text-gray-700 leading-relaxed">
        {summary.summary}
      </Paragraph>

      <Title level={5} className="mt-6">
        关键讨论点
      </Title>
      <Timeline
        items={summary.key_points.map((kp, i) => ({
          color: kp.importance === 'high' ? 'red' : kp.importance === 'medium' ? 'blue' : 'gray',
          children: (
            <div key={i}>
              <div className="flex items-center gap-2">
                <Text strong>{kp.topic}</Text>
                <Tag color={importanceColors[kp.importance]}>
                  {importanceLabels[kp.importance]}
                </Tag>
              </div>
              <Paragraph
                type="secondary"
                className="mt-1 mb-0"
              >
                {kp.content}
              </Paragraph>
            </div>
          ),
        }))}
      />

      <Title level={5} className="mt-6">
        决策记录
      </Title>
      <div className="flex flex-col gap-2">
        {summary.decisions.map((d, i) => (
          <Checkbox key={i} checked disabled={false}>
            <Text>{d.content}</Text>
            {d.proposer && (
              <Text type="secondary" className="ml-2">
                — {d.proposer}
              </Text>
            )}
          </Checkbox>
        ))}
      </div>

      <Title level={5} className="mt-6">
        行动项
      </Title>
      <Table
        dataSource={summary.action_items}
        rowKey="content"
        pagination={false}
        size="small"
        columns={[
          { title: '内容', dataIndex: 'content', key: 'content' },
          {
            title: '负责人',
            dataIndex: 'assignee',
            key: 'assignee',
            width: 120,
          },
          {
            title: '截止日期',
            dataIndex: 'deadline',
            key: 'deadline',
            width: 140,
            render: (d: string | null) =>
              d || <Text type="secondary">未指定</Text>,
          },
        ]}
      />

      {summary.tags.length > 0 && (
        <div className="mt-4 flex items-center gap-2">
          <Text type="secondary">标签：</Text>
          {summary.tags.map((t) => (
            <Tag key={t}>{t}</Tag>
          ))}
        </div>
      )}

      {/* 说话人分离的转录文本 */}
      <Collapse
        className="mt-6"
        items={[
          {
            key: 'transcript',
            label: hasSegments
              ? `完整转录文本（${transcriptSegments!.length} 个分段，${new Set(transcriptSegments!.map(s => s.speaker)).size} 位发言人）`
              : '完整转录文本',
            children: hasSegments ? (
              <div className="flex flex-col gap-3 max-h-96 overflow-y-auto">
                {transcriptSegments!.map((seg, i) => {
                  const color = getSpeakerColor(seg.speaker)
                  return (
                    <div key={i} className="flex gap-3 items-start">
                      <Avatar
                        size="small"
                        style={{ backgroundColor: color, flexShrink: 0 }}
                        icon={<UserOutlined />}
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <Text
                            strong
                            className="text-xs"
                            style={{ color }}
                          >
                            {formatSpeakerLabel(seg.speaker_label || seg.speaker)}
                          </Text>
                          <Text type="secondary" className="text-xs">
                            {formatDuration(Math.floor(seg.start_time))}
                            {' — '}
                            {formatDuration(Math.floor(seg.end_time))}
                          </Text>
                        </div>
                        <Paragraph className="text-sm mb-0 whitespace-pre-wrap">
                          {seg.text}
                        </Paragraph>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : hasText ? (
              <Paragraph className="text-xs whitespace-pre-wrap">
                {transcriptText}
              </Paragraph>
            ) : (
              <Text type="secondary">转录文本暂未存储或已过期</Text>
            ),
          },
        ]}
      />
    </div>
  )
}
