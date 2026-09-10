import { Card, Typography } from 'antd'

/** 手动记录：补充活动之外的用户笔记，参与日报生成。 */
export default function Notes() {
  return (
    <Card>
      <Typography.Title level={3}>手动记录</Typography.Title>
      <Typography.Paragraph type="secondary">
        输入记录内容、选择记录时间、添加标签，保存后参与日报生成。
      </Typography.Paragraph>
    </Card>
  )
}
