import { apiClient } from './client'
import type { ExportFormat } from '@/types/settings'

export interface ExportResult {
  blob: Blob
  filename: string
}

/**
 * 从 Content-Disposition 响应头中解析文件名
 * 支持 filename= 和 filename*= (RFC 5987) 两种格式
 */
function parseFilename(header: string | undefined, fallback: string): string {
  if (!header) return fallback

  // 优先解析 filename*= (RFC 5987, UTF-8 编码)
  const rfc5987 = header.match(/filename\*=UTF-8''(.+?)(?:;|$)/i)
  if (rfc5987) {
    return decodeURIComponent(rfc5987[1])
  }

  // 降级解析 filename=
  const standard = header.match(/filename="?(.+?)"?(?:;|$)/i)
  if (standard) {
    return standard[1]
  }

  return fallback
}

/**
 * 下载会议导出文件
 *
 * 解析响应头中的 Content-Disposition 获取服务器建议的文件名，
 * 若未提供则根据 meetingId + format 生成默认文件名。
 */
export async function downloadExport(
  meetingId: string,
  format: ExportFormat,
): Promise<ExportResult> {
  const { data, headers } = await apiClient.get(
    `/meetings/${meetingId}/export`,
    {
      params: { format },
      responseType: 'blob',
    },
  )

  const contentDisposition = headers['content-disposition'] as string | undefined
  const filename = parseFilename(
    contentDisposition,
    `meeting_${meetingId}.${format}`,
  )

  return { blob: data as Blob, filename }
}
