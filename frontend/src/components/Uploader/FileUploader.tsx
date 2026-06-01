import { useCallback, useState, useRef } from 'react'
import { useDropzone } from 'react-dropzone'
import { Progress, Typography, message } from 'antd'
import { InboxOutlined, CloudUploadOutlined } from '@ant-design/icons'
import { validateFile } from '@/utils/validators'
import { useMeetingStore } from '@/stores/meetingStore'
import { useTaskStore } from '@/stores/taskStore'
import BatchProgress from '@/components/Uploader/BatchProgress'
import type { BatchFileItem } from '@/components/Uploader/BatchProgress'

const { Text } = Typography

interface Props {
  onUploadComplete?: (meetingId: string) => void
}

export default function FileUploader({ onUploadComplete }: Props) {
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [batchFiles, setBatchFiles] = useState<BatchFileItem[]>([])
  const [batchActive, setBatchActive] = useState(false)
  const batchRef = useRef<BatchFileItem[]>([])

  const uploadMeeting = useMeetingStore((s) => s.uploadMeeting)
  const startPolling = useTaskStore((s) => s.startPolling)
  const fetchMeetings = useMeetingStore((s) => s.fetchMeetings)

  /** 上传单个文件 */
  const uploadSingle = useCallback(
    async (file: File): Promise<boolean> => {
      setUploading(true)
      setUploadProgress(0)
      try {
        const taskId = await uploadMeeting(file, setUploadProgress)
        if (taskId) {
          startPolling(taskId)
          message.success(`"${file.name}" 上传成功，正在处理中...`)
          onUploadComplete?.(taskId)
          return true
        }
        return false
      } catch {
        return false
      } finally {
        setUploading(false)
      }
    },
    [uploadMeeting, startPolling, onUploadComplete],
  )

  /** 批量上传处理 */
  const processBatch = useCallback(
    async (files: File[]) => {
      // 初始化批量状态
      const items: BatchFileItem[] = files.map((f, i) => ({
        id: `batch-${Date.now()}-${i}`,
        name: f.name,
        size: f.size,
        status: 'waiting' as const,
        progress: 0,
      }))
      batchRef.current = items
      setBatchFiles([...items])
      setBatchActive(true)

      const updateItem = (index: number, update: Partial<BatchFileItem>) => {
        batchRef.current[index] = { ...batchRef.current[index], ...update }
        setBatchFiles([...batchRef.current])
      }

      for (let i = 0; i < files.length; i++) {
        const file = files[i]

        // 更新为上传中
        updateItem(i, { status: 'uploading', progress: 0 })

        try {
          const taskId = await uploadMeeting(file, (pct) => {
            updateItem(i, { progress: Math.round(pct * 0.5) }) // 上传占 50% 进度
          })

          if (taskId) {
            updateItem(i, {
              status: 'processing',
              progress: 50,
              taskStage: '处理中',
            })

            // 启动轮询，监听此任务
            startPolling(taskId)

            // 简单模拟 — 实际应由 taskStore 的 status 变化驱动
            // 此处标记为 completed，由 Home 页的 useEffect 同步
            updateItem(i, { status: 'completed', progress: 100 })
          } else {
            updateItem(i, { status: 'failed', error: '上传失败' })
          }
        } catch {
          updateItem(i, { status: 'failed', error: '上传出错' })
        }
      }

      setBatchActive(false)
      await fetchMeetings({ page: 1, page_size: 6 })
      message.success(
        `批量处理完成：${batchRef.current.filter((f) => f.status === 'completed').length}/${files.length} 成功`,
      )
    },
    [uploadMeeting, startPolling, fetchMeetings],
  )

  const onDrop = useCallback(
    async (accepted: File[]) => {
      if (accepted.length === 0) return

      // 校验所有文件
      const validFiles: File[] = []
      for (const file of accepted) {
        const result = validateFile(file)
        if (!result.valid) {
          message.error(`"${file.name}": ${result.error}`)
        } else {
          validFiles.push(file)
        }
      }

      if (validFiles.length === 0) return

      // 单文件：走原有的简单上传流程
      if (validFiles.length === 1) {
        await uploadSingle(validFiles[0])
        return
      }

      // 多文件：走批量流程
      await processBatch(validFiles)
    },
    [uploadSingle, processBatch],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
    accept: {
      'video/*': ['.mp4', '.mov', '.avi', '.mkv', '.webm'],
      'audio/*': ['.mp3', '.wav', '.m4a'],
    },
  })

  return (
    <div>
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-12 text-center cursor-pointer transition-colors ${
          isDragActive
            ? 'border-blue-400 bg-blue-50'
            : 'border-gray-300 hover:border-blue-400'
        }`}
      >
        <input {...getInputProps()} />
        {uploading ? (
          <div className="flex flex-col items-center gap-4">
            <CloudUploadOutlined className="text-4xl text-blue-500" />
            <Text strong>正在上传...</Text>
            <Progress percent={uploadProgress} className="w-80" />
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2">
            <InboxOutlined className="text-5xl text-gray-400" />
            <Text strong className="text-lg">
              点击或拖拽文件到此区域上传
            </Text>
            <Text type="secondary">
              支持 MP4、MOV、AVI、MKV、WebM、MP3、WAV、M4A 格式，最大 2GB
            </Text>
            <Text type="secondary" className="text-xs">
              可同时选择多个文件进行批量上传
            </Text>
          </div>
        )}
      </div>

      <BatchProgress files={batchFiles} active={batchActive} />
    </div>
  )
}
