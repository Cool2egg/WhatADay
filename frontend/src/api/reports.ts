import { get, post } from './client'
import type { DailyReport } from '@/types'

/** 历史日报列表（按日期倒序）。 */
export const fetchReports = () => get<DailyReport[]>('/reports')

/** 查看指定日期的日报；不存在时后端返回 404。 */
export const fetchReport = (date: string) => get<DailyReport>(`/reports/${date}`)

/** 生成今日日报（幂等）。 */
export const generateTodayReport = () => post<DailyReport>('/reports/today')

/** 生成指定日期的日报；重新生成同一日期只会更新同一条记录。 */
export const generateReport = (date: string) => post<DailyReport>(`/reports/${date}/generate`)
