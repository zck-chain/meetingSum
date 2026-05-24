import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Table, Tag, Input, Space, Modal, Button } from 'antd'
import {
  SearchOutlined,
  DeleteOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import type { Meeting, MeetingStatus } from '@/types/meeting'
import { useMeetingStore } from '@/stores/meetingStore'
import { formatDuration, formatFileSize } from '@/utils/format'

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
      render: (status: MeetingStatus) => {
        const cfg = statusConfig[status]
        return <Tag color={cfg.color}>{cfg.label}</Tag>
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
      width: 120,
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
    </div>
  )
}
