import { useEffect } from 'react'
import { Typography } from 'antd'
import HistoryTable from '@/components/HistoryList/HistoryTable'
import { useMeetingStore } from '@/stores/meetingStore'

const { Title } = Typography

export default function History() {
  const fetchMeetings = useMeetingStore((s) => s.fetchMeetings)

  useEffect(() => {
    fetchMeetings({ page: 1, page_size: 10 })
  }, [fetchMeetings])

  return (
    <div>
      <Title level={4} className="mb-6">
        历史记录
      </Title>
      <HistoryTable />
    </div>
  )
}
