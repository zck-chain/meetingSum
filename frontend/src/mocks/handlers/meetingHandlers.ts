import { http, HttpResponse, delay } from 'msw'
import { mockMeetings, createMockTask, simulateProgress } from '../data'
import type { Meeting } from '@/types/meeting'
import type { Task } from '@/types/task'

const taskStore = new Map<string, Task & { startedAt: number }>()

export const meetingHandlers = [
  http.get('/api/v1/meetings', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const pageSize = parseInt(url.searchParams.get('page_size') || '10')
    const search = url.searchParams.get('search') || ''

    let filtered = [...mockMeetings]
    if (search) {
      const q = search.toLowerCase()
      filtered = filtered.filter(
        (m) =>
          m.title.toLowerCase().includes(q) ||
          m.original_file.toLowerCase().includes(q),
      )
    }

    filtered.sort(
      (a, b) =>
        new Date(b.created_at).getTime() - new Date(a.created_at).getTime(),
    )

    const start = (page - 1) * pageSize
    return HttpResponse.json({
      items: filtered.slice(start, start + pageSize),
      total: filtered.length,
      page,
      page_size: pageSize,
    })
  }),

  http.get('/api/v1/meetings/:id', async ({ params }) => {
    await delay(200)
    const meeting = mockMeetings.find((m) => m.id === params.id)
    if (!meeting) {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json(meeting)
  }),

  http.post('/api/v1/meetings/upload', async () => {
    await delay(500)
    const newMeeting: Meeting = {
      id: `meeting-${Date.now()}`,
      title: '',
      original_file: 'uploaded-file.mp4',
      original_format: 'mp4',
      duration_seconds: 0,
      file_size_bytes: 0,
      status: 'pending',
      transcript_text: null,
      summary_json: null,
      error_message: null,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    }
    mockMeetings.unshift(newMeeting)

    const task = createMockTask(newMeeting.id)
    taskStore.set(task.id, { ...task, startedAt: Date.now() })

    return HttpResponse.json(
      { meeting: newMeeting, task_id: task.id },
      { status: 201 },
    )
  }),

  http.delete('/api/v1/meetings/:id', async ({ params }) => {
    await delay(200)
    const idx = mockMeetings.findIndex((m) => m.id === params.id)
    if (idx === -1) {
      return new HttpResponse(null, { status: 404 })
    }
    mockMeetings.splice(idx, 1)
    return HttpResponse.json({ success: true })
  }),

  http.get('/api/v1/meetings/:id/export', async ({ params, request }) => {
    await delay(400)
    const meeting = mockMeetings.find((m) => m.id === params.id)
    if (!meeting) {
      return new HttpResponse(null, { status: 404 })
    }
    const url = new URL(request.url)
    const format = url.searchParams.get('format') || 'md'
    const content =
      format === 'md'
        ? `# ${meeting.title || '会议纪要'}\n\n## 摘要\n\n示例内容...`
        : `示例 Word 内容`

    return new HttpResponse(content, {
      headers: {
        'Content-Type':
          format === 'md' ? 'text/markdown' : 'application/octet-stream',
        'Content-Disposition': `attachment; filename="meeting.${format}"`,
      },
    })
  }),

  http.get('/api/v1/tasks/:id/status', async ({ params }) => {
    await delay(200)
    const stored = taskStore.get(params.id as string)
    if (!stored) {
      return new HttpResponse(null, { status: 404 })
    }

    const elapsed = (Date.now() - stored.startedAt) / 1000
    const { stage, progress, status } = simulateProgress(elapsed)

    const updated: Task = {
      ...stored,
      stage,
      progress,
      status,
      completed_at: status === 'completed' ? new Date().toISOString() : null,
    }

    if (status === 'completed') {
      const meeting = mockMeetings.find((m) => m.id === stored.meeting_id)
      if (meeting) {
        meeting.status = 'completed'
      }
    }

    return HttpResponse.json(updated)
  }),
]
