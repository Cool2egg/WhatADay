import ReactECharts from 'echarts-for-react'
import type { ActivitySummary, ActivityType } from '@/types'
import { ACTIVITY_COLORS, ACTIVITY_LABELS } from '@/constants/activity'
import { INK, MUTED } from '@/theme'

interface Props {
  summary: ActivitySummary | null
  height?: number
}

/** 当日活动类型占比（环图）。数量为 0 的类型不画进图里，避免图例出现无意义的项。 */
export default function ActivityStatistics({ summary, height = 240 }: Props) {
  const countByType = summary?.countByType
  const entries = countByType
    ? (Object.keys(countByType) as ActivityType[])
        .filter((type) => countByType[type] > 0)
        .map((type) => ({
          name: ACTIVITY_LABELS[type],
          value: countByType[type],
          itemStyle: { color: ACTIVITY_COLORS[type] },
        }))
    : []

  if (entries.length === 0) {
    return (
      <div style={{ height, display: 'grid', placeItems: 'center', color: '#9aa3ad', fontSize: 13 }}>
        还没有可用于统计的活动
      </div>
    )
  }

  const total = entries.reduce((sum, entry) => sum + entry.value, 0)

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: MUTED, fontSize: 12 },
    },
    title: {
      text: String(total),
      subtext: '条活动',
      left: 'center',
      top: '30%',
      textStyle: { fontSize: 26, fontWeight: 600, color: INK },
      subtextStyle: { fontSize: 12, color: MUTED },
    },
    series: [
      {
        type: 'pie',
        radius: ['58%', '78%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        data: entries,
      },
    ],
  }

  return <ReactECharts option={option} style={{ height }} notMerge />
}
