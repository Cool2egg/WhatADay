import { Tag } from 'antd'
import type { ActivityEvent } from '@/types'
import { ACTIVITY_COLORS, ACTIVITY_LABELS } from '@/constants/activity'
import { formatDuration, formatRange } from '@/utils/format'

interface Props {
  events: ActivityEvent[]
  emptyText?: string
}

/** 活动列表：左侧色条区分类型，右侧是时间、描述、应用/窗口、关键词与置信度。 */
export default function ActivityTimeline({ events, emptyText = '这一天还没有活动记录' }: Props) {
  if (events.length === 0) {
    return <div style={{ color: '#9aa3ad', fontSize: 13, padding: '24px 0' }}>{emptyText}</div>
  }

  return (
    <div>
      {events.map((event) => {
        const minutes = durationMinutes(event)
        return (
          <div className="timeline-row" key={event.id}>
            <span className="timeline-marker" style={{ background: ACTIVITY_COLORS[event.type] }} />
            <div className="timeline-main">
              <div className="timeline-head">
                <Tag
                  bordered={false}
                  style={{
                    color: ACTIVITY_COLORS[event.type],
                    background: `${ACTIVITY_COLORS[event.type]}14`,
                    marginInlineEnd: 0,
                  }}
                >
                  {ACTIVITY_LABELS[event.type]}
                </Tag>
                <span className="timeline-time">{formatRange(event.startTime, event.endTime)}</span>
                {minutes > 0 && <span className="timeline-time">{formatDuration(minutes)}</span>}
              </div>

              <p className="timeline-desc">{event.description ?? '未识别活动'}</p>

              <div className="timeline-meta">
                {[event.appName, event.windowTitle].filter(Boolean).join(' · ')}
                {event.keywords.length > 0 && <> · {event.keywords.join(' / ')}</>}
                {event.confidence != null && <> · 置信度 {Math.round(event.confidence * 100)}%</>}
                {event.source === 'WINDOW_FALLBACK' && <> · 窗口降级</>}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

/** 事件时长（分钟）；缺少任一端时返回 0。 */
function durationMinutes(event: ActivityEvent): number {
  if (!event.startTime || !event.endTime) return 0
  return Math.max(0, minutesOfDay(event.endTime) - minutesOfDay(event.startTime))
}

function minutesOfDay(iso: string): number {
  return Number(iso.slice(11, 13)) * 60 + Number(iso.slice(14, 16))
}
