import { Layout, Menu, Typography } from 'antd'
import {
  ClockCircleOutlined,
  DashboardOutlined,
  EditOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { Link, Outlet, useLocation } from 'react-router-dom'

const { Header, Sider, Content } = Layout

const NAV_ITEMS = [
  { key: '/', icon: <DashboardOutlined />, label: <Link to="/">今日工作台</Link> },
  { key: '/timeline', icon: <ClockCircleOutlined />, label: <Link to="/timeline">时间线</Link> },
  { key: '/reports', icon: <FileTextOutlined />, label: <Link to="/reports">日报</Link> },
  { key: '/notes', icon: <EditOutlined />, label: <Link to="/notes">手动记录</Link> },
]

export default function App() {
  const location = useLocation()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          WhatADay
        </Typography.Title>
        <Typography.Text style={{ color: 'rgba(255,255,255,0.65)', marginLeft: 12 }}>
          工作复盘 Agent
        </Typography.Text>
      </Header>
      <Layout>
        <Sider width={200} theme="light">
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={NAV_ITEMS}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
