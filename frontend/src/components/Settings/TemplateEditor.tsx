import { useState } from 'react'
import { Input, Typography, Tag, Button, Space, Collapse, Alert } from 'antd'
import {
  EditOutlined,
  UndoOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { TEMPLATE_VARIABLES } from '@/types/settings'
import { useSettingsStore } from '@/stores/settingsStore'

const { Text } = Typography
const { TextArea } = Input

/** 默认模板预览 */
const DEFAULT_TEMPLATE = `# {{title}}

## 基本信息
- 日期：{{date}}
- 时长：{{duration}}

## 会议摘要
{{summary}}

## 关键讨论点
{{key_points}}

## 决策记录
{{decisions}}

## 行动项
{{action_items}}

{{tags}}`

export default function TemplateEditor() {
  const { settings, setSummaryTemplate, resetTemplate } = useSettingsStore()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(settings.summaryTemplate)

  const hasCustomTemplate = settings.summaryTemplate.length > 0

  const handleSave = () => {
    setSummaryTemplate(draft)
    setEditing(false)
  }

  const handleStartEdit = () => {
    setDraft(settings.summaryTemplate)
    setEditing(true)
  }

  const handleCancel = () => {
    setDraft(settings.summaryTemplate)
    setEditing(false)
  }

  const handleReset = () => {
    resetTemplate()
    setDraft('')
    setEditing(false)
  }

  const insertVariable = (variable: string) => {
    setDraft((prev) => prev + variable)
  }

  return (
    <div>
      {!editing && (
        <div>
          {hasCustomTemplate ? (
            <div>
              <Tag color="blue">已自定义</Tag>
              <Text type="secondary" className="ml-2">
                当前使用自定义摘要模板
              </Text>
              <div className="mt-3 flex gap-2">
                <Button size="small" icon={<EditOutlined />} onClick={handleStartEdit}>
                  编辑模板
                </Button>
                <Button size="small" icon={<UndoOutlined />} onClick={handleReset} danger>
                  恢复默认
                </Button>
              </div>
            </div>
          ) : (
            <div>
              <Text type="secondary">使用默认摘要模板，点击下方按钮自定义</Text>
              <div className="mt-3">
                <Button size="small" icon={<EditOutlined />} onClick={handleStartEdit}>
                  自定义模板
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="flex flex-col gap-3">
          <Alert
            type="info"
            icon={<InfoCircleOutlined />}
            message="使用 {{变量名}} 语法插入动态内容，处理时将自动替换为实际数据"
            className="text-xs"
            showIcon
          />

          <div>
            <Text type="secondary" className="text-xs">
              可用变量（点击插入）：
            </Text>
            <div className="flex flex-wrap gap-1 mt-1">
              {TEMPLATE_VARIABLES.map((v) => (
                <Tag
                  key={v.key}
                  color="blue"
                  className="cursor-pointer"
                  onClick={() => insertVariable(v.key)}
                  title={v.description}
                >
                  {v.key}
                </Tag>
              ))}
            </div>
          </div>

          <TextArea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={16}
            placeholder="输入自定义 Markdown 模板..."
            className="font-mono text-sm"
          />

          <Space>
            <Button type="primary" size="small" onClick={handleSave}>
              保存模板
            </Button>
            <Button size="small" onClick={handleCancel}>
              取消
            </Button>
          </Space>
        </div>
      )}

      <Collapse
        className="mt-4"
        size="small"
        items={[
          {
            key: 'preview',
            label: '默认模板预览（点击展开）',
            children: (
              <pre className="text-xs bg-gray-50 p-2 rounded whitespace-pre-wrap">
                {DEFAULT_TEMPLATE}
              </pre>
            ),
          },
        ]}
      />
    </div>
  )
}
