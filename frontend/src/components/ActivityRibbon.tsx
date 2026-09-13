import { Tooltip } from 'antd'
import type { ActivityEvent, ActivityType } from '@/types'
import { ACTIVITY_COLORS, ACTIVITY_LABELS } from '@/constants/activity'
import { formatDuration, formatRange } from '@/utils/format'

const MINUTES_IN_DAY = 24 * 60
/** 极短活动的可见宽度（分钟）：避免 1 分钟的活动在 24 小时轴上完全看不见。 */
const MIN_VISIBLE_MINUTES = 4

interface Props {
  events: ActivityEvent[]
  /** 点击某个活动时回调，用于联动其它区域。 */
  onSelect?: (event: ActivityEvent) => void
}

/**
 * 一天的时间条——工作台的主角。
 *
 * 把 00:00-24:00 铺成一条横向轴，每段活动按类型着色、按时长占宽。
 * 选它当主角，是因为这个产品的主题就是「一天的时间去哪了」：
 * 一眼看出时间分布，比一排大数字更贴近主题本身，也更有信息量。
 */
export default function ActivityRibbon({ events, onSelect }: Props) {
  const blocks = buildBlocks(events)

  if (blocks.length === 0) {
    return (
      <div className="ribbon-track" style={{ display: 'grid', placeItems: 'center' }}>
        <span style={{ color: '#9aa3ad', fontSize: 13 }}>今天还没有采集到活动</span>
      </div>
    )
  }

  const coveredMinutes = blocks.reduce((sum, block) => sum + (block.end - block.start), 0)

  return (
    <div>
      <div className="ribbon-track">
        {blocks.map((block) => (
          <Tooltip
            key={block.event.id}
            title={
              <div>
                <div>{formatRange(block.event.startTime, block.event.endTime)}</div>
                <div>{block.event.description ?? block.event.windowTitle ?? '未识别活动'}</div>
                <div style={{ opacity: 0.75 }}>
                  {ACTIVITY_LABELS[block.event.type]} · {formatDuration(block.end - block.start)}
                </div>
              </div>
            }
          >
            <div
              className="ribbon-block"
              style={{
                left: `${(block.start / MINUTES_IN_DAY) * 100}%`,
                width: `${((block.end - block.start) / MINUTES_IN_DAY) * 100}%`,
                background: ACTIVITY_COLORS[block.event.type],
                cursor: onSelect ? 'pointer' : 'default',
              }}
              onClick={() => onSelect?.(block.event)}
            />
          </Tooltip>
        ))}
      </div>

      <div className="ribbon-axis">
        {[0, 6, 12, 18, 24].map((hour) => (
          <span key={hour}>{String(hour).padStart(2, '0')}:00</span>
        ))}
      </div>

      <div
        style={{
          marginTop: 12,
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          gap: '6px 16px',
        }}
      >
        {usedTypes(events).map((type) => (
          <span
            key={type}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#6b7480' }}
          >
            <i
              style={{
                width: 8,
                height: 8,
                borderRadius: 2,
                background: ACTIVITY_COLORS[type],
                display: 'inline-block',
              }}
            />
            {ACTIVITY_LABELS[type]}
          </span>
        ))}
        <span style={{ marginLeft: 'auto', fontSize: 12, color: '#6b7480' }}>
          时间条覆盖 {formatDuration(coveredMinutes)}
        </span>
      </div>
    </div>
  )
}

interface Block {
  event: ActivityEvent
  start: number
  end: number
}

function buildBlocks(events: ActivityEvent[]): Block[] {
  return events
    .filter((event) => event.startTime)
    .map((event) => {
      const start = minutesOfDay(event.startTime)
      const rawEnd = event.endTime ? minutesOfDay(event.endTime) : start
      const end = Math.min(Math.max(rawEnd, start + MIN_VISIBLE_MINUTES), MINUTES_IN_DAY)
      return { event, start, end }
    })
    .filter((block) => block.start < MINUTES_IN_DAY)
    .sort((a, b) => a.start - b.start)
}

/** 从 ISO 时间串取当天第几分钟，例如 `2026-09-13T09:30:00` → 570。 */
function minutesOfDay(iso: string): number {
  const hour = Number(iso.slice(11, 13))
  const minute = Number(iso.slice(14, 16))
  return hour * 60 + minute
}

/** 只列出当天真正出现过的类型，避免图例里堆一排没用的项。 */
function usedTypes(events: ActivityEvent[]): ActivityType[] {
  return Array.from(new Set(events.map((event) => event.type)))
}
