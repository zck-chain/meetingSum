import { useCallback, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { Progress, Typography, message } from 'antd'
import { InboxOutlined, CloudUploadOutlined } from '@ant-design/icons'
import { validateFile } from '@/utils/validators'
import { useMeetingStore } from '@/stores/meetingStore'
import { useTaskStore } from '@/stores/taskStore'

const { Text } = Typography

interface Props {
  onUploadComplete?: (meetingId: string) => void
}

export default function FileUploader({ onUploadComplete }: Props) {
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const uploadMeeting = useMeetingStore((s) => s.uploadMeeting)
  const startPolling = useTaskStore((s) => s.startPolling)

  const onDrop = useCallback(
    async (accepted: File[]) => {
      if (accepted.length === 0) return
      const file = accepted[0]
      const result = validateFile(file)
      if (!result.valid) {
        message.error(result.error)
        return
      }
      setUploading(true)
      setUploadProgress(0)
      const taskId = await uploadMeeting(file, setUploadProgress)
      setUploading(false)
      if (taskId) {
        startPolling(taskId)
        message.success('文件上传成功，正在处理中...')
        onUploadComplete?.(taskId)
      }
    },
    [uploadMeeting, startPolling, onUploadComplete],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: false,
    accept: {
      'video/*': ['.mp4', '.mov', '.avi', '.mkv', '.webm'],
      'audio/*': ['.mp3', '.wav', '.m4a'],
    },
  })

  return (
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
        </div>
      )}
    </div>
  )
}
