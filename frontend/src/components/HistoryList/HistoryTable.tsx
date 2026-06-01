import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Table, Tag, Input, Space, Modal, Button, Typography } from 'antd'
import {
  SearchOutlined,
  DeleteOutlined,
  EyeOutlined,
  RedoOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import type { Meeting, MeetingStatus } from '@/types/meeting'
import { useMeetingStore } from '@/stores/meetingStore'
import { formatDuration, formatFileSize } from '@/utils/format'

const { Text } = Typography

const statusConfig: Record<
  MeetingStatus,
  { color: string; label: string }
> = {
  pending: { color: 'default', label: '等待中' },
  processing: { color: 'processing', label: '处理中' },
  completed: { color: 'success', label: '已完成' },
  failed: { color: 'error', label: '失败' },
  cancelled: { color: 'warning', label: '已取消' },
}

export default function HistoryTable() {
  const navigate = useNavigate()
  const { meetings, total, loading, fetchMeetings, deleteMeeting } =
    useMeetingStore()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [errorModal, setErrorModal] = useState<{
    open: boolean
    title: string
    message: string
  }>({ open: false, title: '', message: '' })

  const handleSearch = (value: string) => {
    setSearch(value)
    setPage(1)
    fetchMeetings({ page: 1, page_size: 10, search: value })
  }

  const handleDelete = (id: string, title: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除"${title}"吗？此操作无法撤销。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deleteMeeting(id),
    })
  }

  const handleRetry = (record: Meeting) => {
    Modal.confirm({
      title: '重新处理',
      icon: <ExclamationCircleOutlined />,
      content: `将重新上传并处理"${record.title || record.original_file}"。是否继续？`,
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        // 跳转回首页触发重新上传流程
        navigate('/')
      },
    })
  }

  const handleShowError = (record: Meeting) => {
    setErrorModal({
      open: true,
      title: record.title || record.original_file,
      message: record.error_message || '处理过程中发生未知错误，请检查文件格式是否正确或稍后重试。',
    })
  }

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: Meeting) =>
        title || record.original_file,
    },
    {
      title: '格式',
      dataIndex: 'original_format',
      key: 'format',
      width: 80,
      render: (fmt: string) => (
        <Tag>{fmt?.toUpperCase()}</Tag>
      ),
    },
    {
      title: '时长',
      dataIndex: 'duration_seconds',
      key: 'duration',
      width: 100,
      render: (d: number) => (d > 0 ? formatDuration(d) : '-'),
    },
    {
      title: '大小',
      dataIndex: 'file_size_bytes',
      key: 'size',
      width: 100,
      render: (s: number) => (s > 0 ? formatFileSize(s) : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: MeetingStatus, record: Meeting) => {
        const cfg = statusConfig[status]
        const tag = <Tag color={cfg.color}>{cfg.label}</Tag>
        // 失败状态可点击查看错误详情
        if (status === 'failed' && record.error_message) {
          return (
            <span
              className="cursor-pointer"
              onClick={() => handleShowError(record)}
              title="点击查看错误详情"
            >
              {tag}
            </span>
          )
        }
        return tag
      },
    },
    {
      title: '上传时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (d: string) => new Date(d).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      render: (_: unknown, record: Meeting) => (
        <Space>
          {record.status === 'completed' && (
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/summary/${record.id}`)}
            >
              查看
            </Button>
          )}
          {record.status === 'failed' && (
            <Button
              type="link"
              size="small"
              icon={<RedoOutlined />}
              onClick={() => handleRetry(record)}
            >
              重试
            </Button>
          )}
          <Button
            type="link"
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={() =>
              handleDelete(record.id, record.title || record.original_file)
            }
          />
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div className="mb-4">
        <Input.Search
          placeholder="搜索会议标题或文件名..."
          allowClear
          enterButton={<SearchOutlined />}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onSearch={handleSearch}
          className="max-w-md"
        />
      </div>
      <Table
        dataSource={meetings}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize: 10,
          total,
          showTotal: (t) => `共 ${t} 条记录`,
          onChange: (p) => {
            setPage(p)
            fetchMeetings({ page: p, page_size: 10, search })
          },
        }}
      />

      {/* 错误详情弹窗 */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined className="text-red-500" />
            <span>处理失败详情</span>
          </Space>
        }
        open={errorModal.open}
        onCancel={() => setErrorModal({ open: false, title: '', message: '' })}
        footer={[
          <Button
            key="close"
            onClick={() => setErrorModal({ open: false, title: '', message: '' })}
          >
            关闭
          </Button>,
        ]}
        width={520}
      >
        <div className="mb-3">
          <Text strong>会议：</Text>
          <Text>{errorModal.title}</Text>
        </div>
        <div>
          <Text strong>错误原因：</Text>
          <div className="mt-2 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700 whitespace-pre-wrap">
            {errorModal.message}
          </div>
        </div>
      </Modal>
    </div>
  )
}
