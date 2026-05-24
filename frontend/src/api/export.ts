import { apiClient } from './client'
import type { ExportFormat } from '@/types/settings'

export async function downloadExport(
  meetingId: string,
  format: ExportFormat,
): Promise<Blob> {
  const { data } = await apiClient.get(
    `/meetings/${meetingId}/export`,
    {
      params: { format },
      responseType: 'blob',
    },
  )
  return data
}
