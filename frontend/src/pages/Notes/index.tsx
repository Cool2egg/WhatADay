import { useEffect, useState } from 'react'
import { Alert, Button, Card, Col, DatePicker, Empty, Input, List, Row, Select, Space, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { createNote, fetchNotes } from '@/api/notes'
import { clockOf } from '@/utils/format'
import type { UserNote } from '@/types'

export default function Notes() {
  const [content, setContent] = useState('')
  const [noteTime, setNoteTime] = useState<Dayjs>(dayjs())
  const [tags, setTags] = useState<string[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [listDate, setListDate] = useState<Dayjs>(dayjs())
  const [notes, setNotes] = useState<UserNote[]>([])
  const [loading, setLoading] = useState(true)

  const loadNotes = async (date: Dayjs) => {
    setLoading(true)
    try {
      setNotes(await fetchNotes(date.format('YYYY-MM-DD')))
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadNotes(listDate)
  }, [listDate])

  const save = async () => {
    if (!content.trim()) {
      setError('记录内容不能为空')
      return
    }
    setSaving(true)
    setNotice(null)
    try {
      await createNote({
        content: content.trim(),
        noteTime: noteTime.format('YYYY-MM-DDTHH:mm:ss'),
        tags,
      })
      setContent('')
      setTags([])
      setNotice('已保存')
      // 保存后把列表切到这条记录所在的日期，让用户立刻看到结果
      setListDate(noteTime)
      await loadNotes(noteTime)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {error && <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} />}
      {notice && <Alert type="success" showIcon message={notice} closable onClose={() => setNotice(null)} />}

      <Row gutter={16}>
        <Col xs={24} lg={12}>
          <Card title="写一条记录">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Input.TextArea
                rows={4}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="活动之外的信息，比如会议结论、临时想法。这些内容会参与日报生成。"
                maxLength={2000}
                showCount
              />
              <Space wrap>
                <DatePicker
                  showTime={{ format: 'HH:mm' }}
                  format="YYYY-MM-DD HH:mm"
                  value={noteTime}
                  onChange={(value) => value && setNoteTime(value)}
                  allowClear={false}
                />
                <Select
                  mode="tags"
                  value={tags}
                  onChange={setTags}
                  placeholder="标签，回车确认（如「完成」「下一步」）"
                  style={{ minWidth: 260 }}
                  tokenSeparators={[',']}
                />
              </Space>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                打上「下一步」标签的记录，会被日报的「下一步行动」采纳。
              </Typography.Text>
              <Button type="primary" icon={<PlusOutlined />} loading={saving} onClick={save}>
                保存记录
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title="当天记录"
            extra={
              <DatePicker
                value={listDate}
                onChange={(value) => value && setListDate(value)}
                allowClear={false}
                size="small"
              />
            }
          >
            {loading ? (
              <Empty description="加载中" />
            ) : notes.length === 0 ? (
              <Empty description="这一天还没有手动记录" />
            ) : (
              <List
                dataSource={notes}
                renderItem={(note) => (
                  <List.Item>
                    <List.Item.Meta
                      title={
                        <Space>
                          <span style={{ color: '#6b7480', fontSize: 13, fontVariantNumeric: 'tabular-nums' }}>
                            {clockOf(note.noteTime)}
                          </span>
                          <span>{note.content}</span>
                        </Space>
                      }
                      description={
                        note.tags.length > 0 ? (
                          <Space size={4} wrap>
                            {note.tags.map((tag) => (
                              <Tag key={tag} bordered={false}>
                                {tag}
                              </Tag>
                            ))}
                          </Space>
                        ) : null
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
