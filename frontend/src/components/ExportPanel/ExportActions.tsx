import { useState } from 'react'
import { Radio, Button, Space, message } from 'antd'
import {
  DownloadOutlined,
  CopyOutlined,
  FileMarkdownOutlined,
  FileWordOutlined,
  FilePdfOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { downloadExport } from '@/api/export'
import type { ExportFormat } from '@/types/settings'

interface Props {
  meetingId: string
  /** 会议标题，用于生成默认文件名 */
  meetingTitle?: string
  onCopyMarkdown?: () => string
}

const formatOptions: { value: ExportFormat; label: string; icon: React.ReactNode; ext: string }[] = [
  { value: 'md', label: 'Markdown', icon: <FileMarkdownOutlined />, ext: 'md' },
  { value: 'docx', label: 'Word', icon: <FileWordOutlined />, ext: 'docx' },
  { value: 'pdf', label: 'PDF', icon: <FilePdfOutlined />, ext: 'pdf' },
  { value: 'txt', label: '纯文本', icon: <FileTextOutlined />, ext: 'txt' },
]

export default function ExportActions({ meetingId, onCopyMarkdown }: Props) {
  const [format, setFormat] = useState<ExportFormat>('md')
  const [downloading, setDownloading] = useState(false)

  const handleDownload = async () => {
    setDownloading(true)
    try {
      const { blob, filename } = await downloadExport(meetingId, format)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
      message.success(`已下载：${filename}`)
    } catch {
      message.error('下载失败，请稍后重试')
    } finally {
      setDownloading(false)
    }
  }

  const handleCopy = () => {
    if (onCopyMarkdown) {
      const text = onCopyMarkdown()
      navigator.clipboard.writeText(text).then(
        () => message.success('已复制到剪贴板'),
        () => message.error('复制失败'),
      )
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 bg-gray-50 rounded-lg">
      <div>
        <div className="mb-2 text-sm text-gray-500">导出格式</div>
        <Radio.Group
          value={format}
          onChange={(e) => setFormat(e.target.value)}
          optionType="button"
          buttonStyle="solid"
        >
          {formatOptions.map((opt) => (
            <Radio.Button key={opt.value} value={opt.value}>
              <Space>
                {opt.icon}
                {opt.label}
              </Space>
            </Radio.Button>
          ))}
        </Radio.Group>
      </div>
      <Space>
        <Button
          type="primary"
          icon={<DownloadOutlined />}
          onClick={handleDownload}
          loading={downloading}
        >
          下载导出
        </Button>
        {onCopyMarkdown && (
          <Button icon={<CopyOutlined />} onClick={handleCopy}>
            复制 Markdown
          </Button>
        )}
      </Space>
    </div>
  )
}
