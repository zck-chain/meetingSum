import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Typography } from 'antd'
import {
  UploadOutlined,
  HistoryOutlined,
  SettingOutlined,
  AudioOutlined,
} from '@ant-design/icons'

const { Sider, Content, Header } = Layout

const menuItems = [
  {
    key: '/',
    icon: <UploadOutlined />,
    label: '上传',
  },
  {
    key: '/history',
    icon: <HistoryOutlined />,
    label: '历史记录',
  },
  {
    key: '/settings',
    icon: <SettingOutlined />,
    label: '设置',
  },
]

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const selectedKey =
    menuItems.find((item) => location.pathname.startsWith(item.key))?.key || '/'

  return (
    <Layout className="min-h-screen">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="light"
        width={200}
        className="border-r border-gray-100"
      >
        <div className="flex items-center gap-2 h-16 px-4">
          <AudioOutlined className="text-lg text-blue-500" />
          {!collapsed && (
            <Typography.Title level={5} className="!mb-0 whitespace-nowrap">
              MeetingSum
            </Typography.Title>
          )}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="bg-white px-6 flex items-center border-b border-gray-100">
          <Typography.Text type="secondary" className="text-sm">
            MeetingSum — 会议学习视频智能总结
          </Typography.Text>
        </Header>
        <Content className="m-6 p-6 bg-white rounded-lg min-h-[calc(100vh-120px)]">
          <Outlet />
        </Content>
        <Layout.Footer className="text-center text-gray-400 text-xs">
          MeetingSum v0.1.0
        </Layout.Footer>
      </Layout>
    </Layout>
  )
}
