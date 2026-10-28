import { Axios } from './Axios'
import { settings } from '@/config/settings'
import { getToken } from '@/utils/auth'

const data = (request) => request.then((response) => response.data?.data ?? response.data)

function streamUrl() {
  const baseURL = (settings.baseURL || '').replace(/\/$/, '')
  return `${baseURL}/api/v1/notifications/stream`
}

function emitNotificationFrame(frame, onNotification) {
  let eventName = 'message'
  const dataLines = []
  for (const line of frame.replace(/\r/g, '').split('\n')) {
    if (line.startsWith('event:')) eventName = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
  }
  if (eventName !== 'notification' || dataLines.length === 0) return
  try {
    onNotification(JSON.parse(dataLines.join('\n')))
  } catch {
    // Ignore malformed event frames; the persisted inbox remains authoritative.
  }
}

async function listen(onNotification, signal) {
  const token = getToken()
  if (!token) throw new Error('Authentication required')

  const response = await fetch(streamUrl(), {
    method: 'GET',
    headers: { Accept: 'text/event-stream', Authorization: `Bearer ${token}` },
    cache: 'no-store',
    signal,
  })
  if (!response.ok || !response.body) throw new Error(`Notification stream unavailable (${response.status})`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    while (!signal.aborted) {
      const { value, done } = await reader.read()
      if (done) return
      buffer = (buffer + decoder.decode(value, { stream: true })).replace(/\r\n/g, '\n')
      let boundary
      while ((boundary = buffer.indexOf('\n\n')) !== -1) {
        emitNotificationFrame(buffer.slice(0, boundary), onNotification)
        buffer = buffer.slice(boundary + 2)
      }
    }
  } finally {
    reader.releaseLock()
  }
}

export const notificationService = {
  inbox: () => data(Axios.get('/api/v1/notifications')),
  markRead: (id) => data(Axios.patch(`/api/v1/notifications/${id}/read`)),
  listen,
}
