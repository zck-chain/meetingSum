import { Progress, Tag, Typography, Collapse } from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  InboxOutlined,
} from '@ant-design/icons'

const { Text } = Typography

export type BatchFileStatus = 'waiting' | 'uploading' | 'processing' | 'completed' | 'failed'

export interface BatchFileItem {
  id: string
  name: string
  size: number
  status: BatchFileStatus
  progress: number     // 0-100, upload + process combined
  taskStage?: string
  error?: string
}

const statusConfig: Record<BatchFileStatus, { color: string; icon: React.ReactNode; label: string }> = {
  waiting: { color: 'default', icon: <ClockCircleOutlined />, label: '等待中' },
  uploading: { color: 'processing', icon: <SyncOutlined spin />, label: '上传中' },
  processing: { color: 'processing', icon: <SyncOutlined spin />, label: '处理中' },
  completed: { color: 'success', icon: <CheckCircleOutlined />, label: '完成' },
  failed: { color: 'error', icon: <CloseCircleOutlined />, label: '失败' },
}

interface Props {
  files: BatchFileItem[]
  /** 是否有正在进行的任务 */
  active: boolean
}

export default function BatchProgress({ files, active }: Props) {
  if (files.length === 0) return null

  const completedCount = files.filter((f) => f.status === 'completed').length
  const failedCount = files.filter((f) => f.status === 'failed').length
  const totalCount = files.length
  const overallPercent = totalCount > 0
    ? Math.round(((completedCount + failedCount) / totalCount) * 100)
    : 0

  return (
    <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <InboxOutlined className="text-blue-500" />
          <Text strong>
            批量处理进度
          </Text>
        </div>
        <Text type="secondary" className="text-sm">
          {completedCount}/{totalCount} 完成
          {failedCount > 0 && (
            <Text type="danger" className="ml-2">
              {failedCount} 失败
            </Text>
          )}
        </Text>
      </div>

      <Progress
        percent={overallPercent}
        status={active ? 'active' : failedCount > 0 ? 'exception' : 'success'}
        size="small"
        className="mb-3"
      />

      <Collapse
        size="small"
        ghost
        items={[
          {
            key: 'detail',
            label: `查看详情（${files.length} 个文件）`,
            children: (
              <div className="flex flex-col gap-1 max-h-64 overflow-y-auto">
                {files.map((file) => {
                  const cfg = statusConfig[file.status]
                  return (
                    <div
                      key={file.id}
                      className="flex items-center gap-3 py-1 px-2 rounded hover:bg-gray-100"
                    >
                      <span>{cfg.icon}</span>
                      <Text
                        ellipsis
                        className="flex-1 text-sm"
                        delete={file.status === 'failed'}
                      >
                        {file.name}
                      </Text>
                      <Tag color={cfg.color} className="text-xs">
                        {cfg.label}
                      </Tag>
                      {(file.status === 'uploading' || file.status === 'processing') && (
                        <Progress
                          percent={file.progress}
                          size="small"
                          className="w-20"
                          showInfo={false}
                        />
                      )}
                      {file.error && (
                        <Text type="danger" className="text-xs" ellipsis>
                          {file.error}
                        </Text>
                      )}
                    </div>
                  )
                })}
              </div>
            ),
          },
        ]}
      />
    </div>
  )
}
