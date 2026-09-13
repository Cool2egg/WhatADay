import { useEffect, useState } from 'react'
import { Alert, Card, DatePicker, Select, Space, Spin, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import ActivityTimeline from '@/components/ActivityTimeline'
import { fetchSummary, fetchTimeline } from '@/api/timeline'
import { ACTIVITY_OPTIONS } from '@/constants/activity'
import { formatDuration } from '@/utils/format'
import type { ActivityEvent, ActivitySummary, ActivityType } from '@/types'

export default function Timeline() {
  const [date, setDate] = useState<Dayjs>(dayjs())
  const [type, setType] = useState<ActivityType | undefined>(undefined)
  const [events, setEvents] = useState<ActivityEvent[]>([])
  const [summary, setSummary] = useState<ActivitySummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    const day = date.format('YYYY-MM-DD')

    Promise.all([fetchTimeline(day, type), fetchSummary(day, type)])
      .then(([list, current]) => {
        if (!active) return
        setEvents(list)
        setSummary(current)
        setError(null)
      })
      .catch((e: Error) => {
        if (active) setError(e.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [date, type])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {error && <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} />}

      <Card>
        <Space size={12} wrap>
          <DatePicker
            value={date}
            onChange={(value) => value && setDate(value)}
            allowClear={false}
            placeholder="选择日期"
          />
          <Select<ActivityType>
            value={type}
            onChange={setType}
            options={ACTIVITY_OPTIONS}
            placeholder="全部类型"
            allowClear
            style={{ width: 140 }}
          />
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            {summary ? `${summary.eventCount} 条活动 · 累计 ${formatDuration(summary.activeMinutes)}` : ''}
          </Typography.Text>
        </Space>
      </Card>

      <Card title={`${date.format('YYYY-MM-DD')} 的时间线`}>
        <Spin spinning={loading}>
          <ActivityTimeline
            events={events}
            emptyText={type ? '这一天没有该类型的活动' : '这一天没有活动记录'}
          />
        </Spin>
      </Card>
    </div>
  )
}
