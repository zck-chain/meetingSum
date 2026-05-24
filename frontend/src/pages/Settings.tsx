import { Card, Radio, Typography, Descriptions, Divider, Space, Tag } from 'antd'
import {
  AudioOutlined,
  RobotOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { useSettingsStore } from '@/stores/settingsStore'
import type { AsrProvider, LlmProvider } from '@/types/settings'

const { Title, Text } = Typography

export default function Settings() {
  const { settings, setAsrProvider, setLlmProvider } = useSettingsStore()

  return (
    <div>
      <Title level={4} className="mb-6">
        设置
      </Title>

      <Card className="mb-4">
        <div className="flex items-center gap-2 mb-4">
          <AudioOutlined className="text-lg" />
          <Title level={5} className="!mb-0">
            语音识别 (ASR)
          </Title>
        </div>
        <Radio.Group
          value={settings.asrProvider}
          onChange={(e) => setAsrProvider(e.target.value as AsrProvider)}
        >
          <Space direction="vertical">
            <Radio value="whisper_local">
              <span>
                Whisper 本地模型
                <Tag color="green" className="ml-2 text-xs">
                  推荐
                </Tag>
              </span>
              <div className="text-xs text-gray-400 ml-6">
                离线可用，无需 API Key，支持中英文
              </div>
            </Radio>
            <Radio value="aliyun">
              <span>阿里云 ASR</span>
              <div className="text-xs text-gray-400 ml-6">
                识别率高，需要阿里云账号和 API Key
              </div>
            </Radio>
            <Radio value="xunfei">
              <span>讯飞 ASR</span>
              <div className="text-xs text-gray-400 ml-6">
                中文识别率高，需要讯飞开放平台账号
              </div>
            </Radio>
          </Space>
        </Radio.Group>
      </Card>

      <Card className="mb-4">
        <div className="flex items-center gap-2 mb-4">
          <RobotOutlined className="text-lg" />
          <Title level={5} className="!mb-0">
            摘要生成 (LLM)
          </Title>
        </div>
        <Radio.Group
          value={settings.llmProvider}
          onChange={(e) => setLlmProvider(e.target.value as LlmProvider)}
        >
          <Space direction="vertical">
            <Radio value="claude">
              <span>
                Claude API
                <Tag color="green" className="ml-2 text-xs">
                  推荐
                </Tag>
              </span>
              <div className="text-xs text-gray-400 ml-6">
                Anthropic Claude 系列模型，摘要质量高，支持长上下文
              </div>
            </Radio>
            <Radio value="openai">
              <span>OpenAI API</span>
              <div className="text-xs text-gray-400 ml-6">
                GPT-4o 等模型，需要 OpenAI API Key
              </div>
            </Radio>
          </Space>
        </Radio.Group>
      </Card>

      <Card className="mb-4">
        <div className="flex items-center gap-2 mb-4">
          <FileTextOutlined className="text-lg" />
          <Title level={5} className="!mb-0">
            摘要模板
          </Title>
        </div>
        <Text type="secondary">
          自定义摘要输出模板功能即将上线，敬请期待。
        </Text>
      </Card>

      <Divider />

      <Card title="系统信息">
        <Descriptions column={1} size="small">
          <Descriptions.Item label="应用版本">0.1.0</Descriptions.Item>
          <Descriptions.Item label="最大文件大小">2 GB</Descriptions.Item>
          <Descriptions.Item label="最长视频时长">4 小时</Descriptions.Item>
          <Descriptions.Item label="支持格式">
            MP4, MOV, AVI, MKV, WebM, MP3, WAV, M4A
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
