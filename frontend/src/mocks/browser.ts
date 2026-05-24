import { setupWorker } from 'msw/browser'
import { meetingHandlers } from './handlers/meetingHandlers'

export const worker = setupWorker(...meetingHandlers)
