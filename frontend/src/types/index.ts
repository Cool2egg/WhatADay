/** 与后端 DTO 一一对应的类型定义。 */

export type ActivityType =
  | 'CODING'
  | 'LEARNING'
  | 'MEETING'
  | 'BROWSING'
  | 'ENTERTAINMENT'
  | 'COMMUNICATION'
  | 'OTHER'

export type ActivitySource = 'VISION' | 'WINDOW_FALLBACK'

export interface ActivityEvent {
  id: number
  observationId: number | null
  startTime: string
  endTime: string | null
  appName: string | null
  windowTitle: string | null
  type: ActivityType
  description: string | null
  keywords: string[]
  confidence: number | null
  source: ActivitySource
  createdAt: string
}

export interface CollectorStatus {
  running: boolean
  mode: string
  startedAt: string | null
  lastCaptureAt: string | null
  todayObservationCount: number
  todayActivityCount: number
  todayActiveMinutes: number
}

export interface ActivitySummary {
  date: string
  eventCount: number
  activeMinutes: number
  countByType: Record<ActivityType, number>
}

export interface UserNote {
  id: number
  noteTime: string
  content: string
  tags: string[]
  createdAt: string
}

export interface DailyReport {
  reportDate: string
  timeline: string[]
  achievements: string[]
  learning: string[]
  distractions: string[]
  nextActions: string[]
  createdAt: string
  updatedAt: string
}

export interface CreateNoteRequest {
  noteTime?: string
  content: string
  tags?: string[]
}
