import { get } from './client'
import type { ActivityEvent, ActivitySummary, ActivityType } from '@/types'

/** 今日活动时间线。 */
export const fetchToday = () => get<ActivityEvent[]>('/timeline/today')

/** 按日期与类型查询时间线。 */
export const fetchTimeline = (date: string, type?: ActivityType) =>
  get<ActivityEvent[]>('/timeline', type ? { date, type } : { date })

/** 当日活动统计。 */
export const fetchSummary = (date?: string, type?: ActivityType) =>
  get<ActivitySummary>('/timeline/summary', {
    ...(date ? { date } : {}),
    ...(type ? { type } : {}),
  })
