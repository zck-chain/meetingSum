import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Spin, Result } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import SummaryContent from '@/components/SummaryViewer/SummaryContent'
import ExportActions from '@/components/ExportPanel/ExportActions'
import { useMeetingStore } from '@/stores/meetingStore'
import type { SummaryData } from '@/types/meeting'

function generateMarkdown(summary: SummaryData): string {
  return `# ${summary.title}

## 基本信息

## 会议摘要
${summary.summary}

## 关键讨论点
${summary.key_points
  .map(
    (kp, i) =>
      `${i + 1}. **${kp.topic}**${kp.importance === 'high' ? ' [重要]' : ''}\n   ${kp.content}`,
  )
  .join('\n')}

## 决策记录
${summary.decisions.map((d) => `- [ ] ${d.content} — ${d.proposer}`).join('\n')}

## 行动项
${summary.action_items
  .map(
    (a) =>
      `- [ ] ${a.content} — 负责人：${a.assignee}${a.deadline ? ` — 截止日期：${a.deadline}` : ''}`,
  )
  .join('\n')}

${summary.tags.length > 0 ? `\n标签：${summary.tags.join(', ')}` : ''}
`
}

export default function Summary() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { currentMeeting, loading, error, fetchMeeting, clearCurrent } =
    useMeetingStore()

  useEffect(() => {
    if (id) fetchMeeting(id)
    return () => clearCurrent()
  }, [id, fetchMeeting, clearCurrent])

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Spin size="large" />
      </div>
    )
  }

  if (error || !currentMeeting) {
    return (
      <Result
        status="404"
        title="未找到会议"
        subTitle={error || '该会议不存在或已被删除'}
        extra={
          <Button onClick={() => navigate('/history')}>返回历史记录</Button>
        }
      />
    )
  }

  if (currentMeeting.status !== 'completed' || !currentMeeting.summary_json) {
    return (
      <Result
        status="warning"
        title="摘要未就绪"
        subTitle={
          currentMeeting.error_message ||
          '该会议尚未处理完成，请等待处理完毕后再查看摘要'
        }
        extra={
          <Button onClick={() => navigate('/history')}>返回历史记录</Button>
        }
      />
    )
  }

  return (
    <div>
      <Button
        type="text"
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate(-1)}
        className="mb-4"
      >
        返回
      </Button>

      <SummaryContent
        summary={currentMeeting.summary_json}
        fileName={currentMeeting.original_file}
        durationSeconds={currentMeeting.duration_seconds}
        createdAt={currentMeeting.created_at}
        transcriptText={currentMeeting.transcript_text}
        transcriptSegments={currentMeeting.transcript_data?.segments ?? null}
      />

      <div className="mt-8">
        <ExportActions
          meetingId={currentMeeting.id}
          onCopyMarkdown={() =>
            generateMarkdown(currentMeeting.summary_json!)
          }
        />
      </div>
    </div>
  )
}
