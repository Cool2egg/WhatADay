import { get, post } from './client'
import type { CollectorStatus } from '@/types'

/** 采集状态；前端每 5 秒轮询。 */
export const fetchCollectorStatus = () => get<CollectorStatus>('/collector/status')

/** 开始采集（幂等）。 */
export const startCollector = () => post<CollectorStatus>('/collector/start')

/** 停止采集（幂等）。 */
export const stopCollector = () => post<CollectorStatus>('/collector/stop')

/** 立即采集一次，返回本次新增的活动数。 */
export const captureNow = () => post<number>('/collector/capture-now')
