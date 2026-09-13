import { useCallback, useState } from 'react'
import { Alert, Card, Col, Divider, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import CollectorStatusPanel, { CollectorMetrics } from '@/components/CollectorStatus'
import ActivityRibbon from '@/components/ActivityRibbon'
import ActivityStatistics from '@/components/ActivityStatistics'
import ActivityTimeline from '@/components/ActivityTimeline'
import { captureNow, fetchCollectorStatus, startCollector, stopCollector } from '@/api/collector'
import { fetchSummary, fetchToday } from '@/api/timeline'
import { usePolling } from '@/hooks/usePolling'
import type { ActivityEvent, ActivitySummary, CollectorStatus } from '@/types'

/** 最近活动列表只展示这么多条，完整内容去时间线页看。 */
const RECENT_LIMIT = 6

export default function Dashboard() {
  const [status, setStatus] = useState<CollectorStatus | null>(null)
  const [events, setEvents] = useState<ActivityEvent[]>([])
  const [summary, setSummary] = useState<ActivitySummary | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const loadStatus = useCallback(async () => {
    try {
      setStatus(await fetchCollectorStatus())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [])

  const loadActivity = useCallback(async () => {
    try {
      const [list, current] = await Promise.all([fetchToday(), fetchSummary()])
      setEvents(list)
      setSummary(current)
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [])

  // 采集状态 5 秒刷新，最近活动 10 秒刷新（与 PROJECT_PLAN 第 5.4 节一致）
  usePolling(loadStatus, 5000)
  usePolling(loadActivity, 10000)

  const runAction = async (action: () => Promise<unknown>, successText: string) => {
    setBusy(true)
    setNotice(null)
    try {
      await action()
      setNotice(successText)
      await Promise.all([loadStatus(), loadActivity()])
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const recent = [...events]
    .sort((a, b) => b.startTime.localeCompare(a.startTime))
    .slice(0, RECENT_LIMIT)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {error && (
        <Alert
          type="error"
          showIcon
          message={error}
          closable
          onClose={() => setError(null)}
        />
      )}
      {notice && (
        <Alert
          type="success"
          showIcon
          message={notice}
          closable
          onClose={() => setNotice(null)}
        />
      )}

      <Card>
        <CollectorStatusPanel
          status={status}
          busy={busy}
          onStart={() => runAction(startCollector, '已开始采集')}
          onStop={() => runAction(stopCollector, '已停止采集')}
          onCaptureNow={() =>
            runAction(async () => {
              const inserted = await captureNow()
              return inserted
            }, '已立即采集一次')
          }
        />
        <Divider style={{ margin: '18px 0' }} />
        <CollectorMetrics status={status} />
      </Card>

      <Card
        title="今天的时间去哪了"
        extra={<Typography.Text type="secondary" style={{ fontSize: 12 }}>鼠标悬停查看详情</Typography.Text>}
      >
        <ActivityRibbon events={events} />
      </Card>

      <Row gutter={16}>
        <Col xs={24} lg={9}>
          <Card title="活动类型占比" style={{ height: '100%' }}>
            <ActivityStatistics summary={summary} />
          </Card>
        </Col>
        <Col xs={24} lg={15}>
          <Card
            title="最近活动"
            extra={<Link to="/timeline">查看完整时间线</Link>}
            style={{ height: '100%' }}
          >
            <ActivityTimeline events={recent} emptyText="今天还没有采集到活动，点击上方「开始采集」试试" />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
