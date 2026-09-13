import { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Card, Col, Empty, List, Row, Space, Spin, Typography } from 'antd'
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ReportCard from '@/components/ReportCard'
import { fetchReports, generateReport, generateTodayReport } from '@/api/reports'
import { today } from '@/utils/format'
import type { DailyReport } from '@/types'

export default function Reports() {
  const [reports, setReports] = useState<DailyReport[]>([])
  const [selected, setSelected] = useState<DailyReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  /** 拉取列表，并尽量保持当前选中的日期；preferDate 用于生成后自动跳到该日期。 */
  const loadReports = useCallback(async (preferDate?: string) => {
    try {
      const list = await fetchReports()
      setReports(list)
      setSelected((previous) => {
        const wanted = preferDate ?? previous?.reportDate ?? list[0]?.reportDate
        return list.find((item) => item.reportDate === wanted) ?? list[0] ?? null
      })
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadReports()
  }, [loadReports])

  const runAction = async (action: () => Promise<DailyReport>, successText: string) => {
    setBusy(true)
    setNotice(null)
    try {
      const report = await action()
      await loadReports(report.reportDate)
      setNotice(successText)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const hasToday = reports.some((item) => item.reportDate === today())

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {error && <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} />}
      {notice && <Alert type="success" showIcon message={notice} closable onClose={() => setNotice(null)} />}

      <Card>
        <Space wrap>
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={busy}
            onClick={() => runAction(generateTodayReport, hasToday ? '已重新生成今日日报' : '已生成今日日报')}
          >
            {hasToday ? '重新生成今日日报' : '生成今日日报'}
          </Button>
          <Button
            icon={<ReloadOutlined />}
            disabled={!selected || busy}
            onClick={() =>
              selected && runAction(() => generateReport(selected.reportDate), `已重新生成 ${selected.reportDate} 的日报`)
            }
          >
            重新生成选中日期
          </Button>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            同一天重复生成只会更新同一条记录
          </Typography.Text>
        </Space>
      </Card>

      <Spin spinning={loading}>
        {reports.length === 0 ? (
          <Card>
            <Empty description="还没有日报。点上面的「生成今日日报」试试。" />
          </Card>
        ) : (
          <Row gutter={16}>
            <Col xs={24} lg={7}>
              <Card title="历史日报" styles={{ body: { padding: 0 } }}>
                <List
                  dataSource={reports}
                  renderItem={(item) => (
                    <List.Item
                      onClick={() => setSelected(item)}
                      style={{
                        padding: '12px 16px',
                        cursor: 'pointer',
                        background: selected?.reportDate === item.reportDate ? '#eef4f3' : undefined,
                      }}
                    >
                      <List.Item.Meta
                        title={item.reportDate}
                        description={`${item.achievements.length} 项成果 · ${item.nextActions.length} 项待办`}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
            <Col xs={24} lg={17}>
              <Card title={selected ? `${selected.reportDate} 的日报` : '日报'}>
                {selected ? <ReportCard report={selected} /> : <Empty description="选择左侧日期查看日报" />}
              </Card>
            </Col>
          </Row>
        )}
      </Spin>
    </div>
  )
}
