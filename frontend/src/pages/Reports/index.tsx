import { Card, Typography } from 'antd'

/** 日报：查看今日及历史日报，生成 / 重新生成。 */
export default function Reports() {
  return (
    <Card>
      <Typography.Title level={3}>日报</Typography.Title>
      <Typography.Paragraph type="secondary">
        查看今日及历史日报，支持生成与重新生成，展示时间线、成果、学习、分心事项与下一步行动。
      </Typography.Paragraph>
    </Card>
  )
}
