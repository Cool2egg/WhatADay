import { Typography } from 'antd'
import type { DailyReport } from '@/types'

const SECTIONS = [
  { key: 'timeline', title: '时间线' },
  { key: 'achievements', title: '成果' },
  { key: 'learning', title: '学习' },
  { key: 'distractions', title: '分心事项' },
  { key: 'nextActions', title: '下一步行动' },
] as const

interface Props {
  report: DailyReport
}

/** 日报正文。五个段落固定顺序展示，空段落明确写「暂无」，不留空白让人误以为没生成。 */
export default function ReportCard({ report }: Props) {
  const regenerated = report.updatedAt !== report.createdAt

  return (
    <div>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        生成于 {report.createdAt}
        {regenerated && <> · 最后重新生成 {report.updatedAt}</>}
      </Typography.Text>

      {SECTIONS.map(({ key, title }) => {
        const items = report[key]
        return (
          <div className="report-section" key={key}>
            <h4>{title}</h4>
            {items.length === 0 ? (
              <div className="report-empty">暂无</div>
            ) : (
              <ul className="report-list">
                {items.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            )}
          </div>
        )
      })}
    </div>
  )
}
