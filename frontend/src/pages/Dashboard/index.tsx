import { Card, Typography } from 'antd'

/** 今日工作台：采集状态、今日时长/活动数、类型占比、最近活动。 */
export default function Dashboard() {
  return (
    <Card>
      <Typography.Title level={3}>今日工作台</Typography.Title>
      <Typography.Paragraph type="secondary">
        采集状态、今日采集时长、活动数量、活动类型占比与最近活动将在此展示。
      </Typography.Paragraph>
    </Card>
  )
}
