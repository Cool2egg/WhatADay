import { Button, Space, Tag, Tooltip, Typography } from 'antd'
import {
  PauseCircleOutlined,
  PlayCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import type { CollectorStatus } from '@/types'
import { clockOf, formatDuration } from '@/utils/format'

interface Props {
  status: CollectorStatus | null
  /** 有请求在飞时禁用按钮，避免连点。 */
  busy: boolean
  onStart: () => void
  onStop: () => void
  onCaptureNow: () => void
}

/** 采集器状态与控制条。 */
export default function CollectorStatusPanel({ status, busy, onStart, onStop, onCaptureNow }: Props) {
  const running = status?.running ?? false

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        flexWrap: 'wrap',
      }}
    >
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
        <i
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: running ? '#4e8c5a' : '#9aa3ad',
            display: 'inline-block',
          }}
        />
        <Typography.Text strong>{running ? '正在采集' : '已停止'}</Typography.Text>
      </span>

      <Tag bordered={false} style={{ marginInlineEnd: 0 }}>
        模式 {status?.mode ?? '—'}
      </Tag>

      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
        {running && status?.startedAt ? `本次自 ${clockOf(status.startedAt)} 开始` : '点击「开始采集」启动'}
      </Typography.Text>

      <Space style={{ marginLeft: 'auto' }}>
        <Tooltip title="无需等待定时任务，立刻采集一次">
          <Button icon={<ThunderboltOutlined />} onClick={onCaptureNow} disabled={busy}>
            立即采集
          </Button>
        </Tooltip>
        {running ? (
          <Button icon={<PauseCircleOutlined />} onClick={onStop} loading={busy}>
            停止采集
          </Button>
        ) : (
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={onStart} loading={busy}>
            开始采集
          </Button>
        )}
      </Space>
    </div>
  )
}

/** 采集器的三个关键数字，单独抽出来便于在卡片里排成一行。 */
export function CollectorMetrics({ status }: { status: CollectorStatus | null }) {
  const items = [
    { label: '今日活跃时长', value: formatDuration(status?.todayActiveMinutes ?? 0) },
    { label: '今日活动', value: `${status?.todayActivityCount ?? 0} 条` },
    { label: '今日采集', value: `${status?.todayObservationCount ?? 0} 次` },
  ]

  return (
    <div style={{ display: 'flex', gap: 40, flexWrap: 'wrap' }}>
      {items.map((item) => (
        <div key={item.label}>
          <div style={{ fontSize: 22, fontWeight: 600, color: '#1c1f23', fontVariantNumeric: 'tabular-nums' }}>
            {item.value}
          </div>
          <div style={{ fontSize: 12, color: '#6b7480', marginTop: 2 }}>{item.label}</div>
        </div>
      ))}
    </div>
  )
}
