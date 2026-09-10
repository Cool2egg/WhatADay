import { Card, Typography } from 'antd'

/** 时间线：按日期与活动类型查询活动事件。 */
export default function Timeline() {
  return (
    <Card>
      <Typography.Title level={3}>时间线</Typography.Title>
      <Typography.Paragraph type="secondary">
        按日期查询、按活动类型筛选，展示开始/结束时间、持续时长、应用、窗口、描述、关键词与置信度。
      </Typography.Paragraph>
    </Card>
  )
}
