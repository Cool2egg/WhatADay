import type { ActivityType } from '@/types'

/** 活动类型的中文名。 */
export const ACTIVITY_LABELS: Record<ActivityType, string> = {
  CODING: '编码',
  LEARNING: '学习',
  MEETING: '会议',
  BROWSING: '浏览',
  ENTERTAINMENT: '娱乐',
  COMMUNICATION: '沟通',
  OTHER: '其他',
}

/**
 * 活动类型配色。
 *
 * 刻意避开组件库的默认分类色板：这一组整体偏灰调、明度接近，既能在时间条上互相区分，
 * 又不会喧宾夺主——时间条上叠了十几段颜色时，高饱和色板会变得刺眼且难以分辨。
 */
export const ACTIVITY_COLORS: Record<ActivityType, string> = {
  CODING: '#1f6f6b',
  LEARNING: '#4b5fd0',
  MEETING: '#c9862b',
  BROWSING: '#6b7a8f',
  ENTERTAINMENT: '#c25e73',
  COMMUNICATION: '#4e8c5a',
  OTHER: '#9aa3ad',
}

/** 类型筛选下拉框的选项，固定顺序，便于前端各处保持一致。 */
export const ACTIVITY_OPTIONS = (Object.keys(ACTIVITY_LABELS) as ActivityType[]).map((value) => ({
  value,
  label: ACTIVITY_LABELS[value],
}))
