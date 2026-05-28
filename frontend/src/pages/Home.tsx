import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Tag, Spin, Typography, Row, Col, Button } from 'antd'
import {
  CheckCircleOutlined,
  SyncOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import FileUploader from '@/components/Uploader/FileUploader'
import { useMeetingStore } from '@/stores/meetingStore'
import { useTaskStore } from '@/stores/taskStore'
import { formatDuration } from '@/utils/format'
import type { MeetingStatus } from '@/types/meeting'

const { Title, Text } = Typography

const statusConfig: Record<
  MeetingStatus,
  { color: string; icon: React.ReactNode; label: string }
> = {
  pending: {
    color: 'default',
    icon: <ClockCircleOutlined />,
    label: '等待中',
  },
  processing: {
    color: 'processing',
    icon: <SyncOutlined spin />,
    label: '处理中',
  },
  completed: {
    color: 'success',
    icon: <CheckCircleOutlined />,
    label: '已完成',
  },
  failed: {
    color: 'error',
    icon: <CloseCircleOutlined />,
    label: '失败',
  },
  cancelled: { color: 'warning', icon: <CloseCircleOutlined />, label: '已取消' },
}

export default function Home() {
  const navigate = useNavigate()
  const { meetings, loading, fetchMeetings } = useMeetingStore()
  const { stage, progress, status: taskStatus, polling } = useTaskStore()

  useEffect(() => {
    fetchMeetings({ page: 1, page_size: 6 })
  }, [fetchMeetings])

  // 任务完成后自动刷新会议列表
  useEffect(() => {
    if (!polling && (taskStatus === 'completed' || taskStatus === 'failed')) {
      fetchMeetings({ page: 1, page_size: 6 })
    }
  }, [polling, taskStatus, fetchMeetings])

  const stageLabels: Record<string, string> = {
    extracting_audio: '提取音频',
    transcribing: '语音转文字',
    summarizing: '生成摘要',
    exporting: '导出文档',
  }

  return (
    <div>
      <Title level={4} className="mb-6">
        上传视频/音频文件
      </Title>

      <FileUploader
        onUploadComplete={() => {
          fetchMeetings({ page: 1, page_size: 6 })
        }}
      />

      {polling && taskStatus === 'completed' && (
        <Card className="mt-4 border-green-200 bg-green-50">
          <div className="flex items-center gap-2">
            <CheckCircleOutlined className="text-green-500 text-lg" />
            <Text strong className="text-green-700">
              处理完成！
            </Text>
            <Button
              type="link"
              onClick={() => {
                const latest = meetings[0]
                if (latest?.status === 'completed') {
                  navigate(`/summary/${latest.id}`)
                }
              }}
            >
              查看摘要
            </Button>
          </div>
        </Card>
      )}

      {polling && taskStatus === 'processing' && (
        <Card className="mt-4">
          <div className="flex items-center gap-4">
            <Spin />
            <div>
              <Text strong>正在处理中...</Text>
              <div className="flex items-center gap-2 mt-1">
                <Tag color="processing">
                  {stageLabels[stage || ''] || '处理中'}
                </Tag>
                <Text type="secondary">进度 {progress}%</Text>
              </div>
            </div>
          </div>
        </Card>
      )}

      <Title level={5} className="mt-8 mb-4">
        最近会议
      </Title>

      {loading ? (
        <div className="text-center py-8">
          <Spin />
        </div>
      ) : (
        <Row gutter={[16, 16]}>
          {meetings.slice(0, 6).map((m) => {
            const cfg = statusConfig[m.status]
            return (
              <Col key={m.id} xs={24} sm={12} lg={8}>
                <Card
                  hoverable
                  onClick={() => {
                    if (m.status === 'completed') {
                      navigate(`/summary/${m.id}`)
                    }
                  }}
                  className="h-full"
                >
                  <div className="flex flex-col gap-2">
                    <Text strong ellipsis>
                      {m.title || m.original_file}
                    </Text>
                    <div className="flex items-center gap-2">
                      <Tag color={cfg.color} icon={cfg.icon}>
                        {cfg.label}
                      </Tag>
                      {m.duration_seconds > 0 && (
                        <Text type="secondary" className="text-xs">
                          {formatDuration(m.duration_seconds)}
                        </Text>
                      )}
                    </div>
                    {m.error_message && (
                      <Text type="danger" className="text-xs" ellipsis>
                        {m.error_message}
                      </Text>
                    )}
                    <Text type="secondary" className="text-xs">
                      {new Date(m.created_at).toLocaleString('zh-CN')}
                    </Text>
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      )}

      {meetings.length === 0 && !loading && (
        <div className="text-center py-8 text-gray-400">
          暂无会议记录，上传一个视频开始吧
        </div>
      )}

      {meetings.length > 0 && (
        <div className="text-center mt-4">
          <Button type="link" onClick={() => navigate('/history')}>
            查看全部历史记录
          </Button>
        </div>
      )}
    </div>
  )
}
