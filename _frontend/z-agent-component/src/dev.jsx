/**
 * Agent 组件库 dev playground
 * 运行: npm run dev (在 z-opc-frontend/packages/z-agent-frontend-component 目录下)
 *
 * 列出本包所有导出页面的占位卡片，方便独立 dev 调试。
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import {Card, ConfigProvider, Space, Tag, Typography} from 'antd'
import zhCN from 'antd/locale/zh_CN'
import {
    AgentAppList,
    AkManage,
    BucketList,
    McpPage,
    ModelManage,
    ProductList,
    ScriptCenterPage,
    SkillMarket,
    UsageDashboard,
} from './index.js'

const {Title, Text} = Typography

const App = () => {
    return (
        <ConfigProvider locale={zhCN}>
            <div style={{padding: 24, background: '#f5f5f5', minHeight: '100vh'}}>
                <Title level={2} style={{marginTop: 0}}>
                    @yuku123/z-agent-frontend-component
                    <Tag color="blue" style={{marginLeft: 12, verticalAlign: 'middle'}}>
                        Dev Playground
                    </Tag>
                </Title>
                <Text type="secondary">
                    本页是 z-agent-frontend-component 的独立 dev 入口，列出包内所有导出的页面。
                    真实数据走 <code>/api</code>，需要本地有 z-opc 后端在 <code>localhost:8888</code> 运行。
                </Text>

                <Space orientation="vertical" size="large" style={{width: '100%', marginTop: 24}}>
                    <Section title="Agent 应用">
                        <AgentAppList/>
                    </Section>
                    <Section title="LLM 模型管理">
                        <ModelManage/>
                    </Section>
                    <Section title="AK 管理">
                        <AkManage/>
                    </Section>
                    <Section title="MCP 中心">
                        <McpPage/>
                    </Section>
                    <Section title="OSS Bucket 列表">
                        <BucketList/>
                    </Section>
                    <Section title="脚本中心">
                        <ScriptCenterPage/>
                    </Section>
                    <Section title="技能市场">
                        <SkillMarket/>
                    </Section>
                    <Section title="产品中心">
                        <ProductList/>
                    </Section>
                    <Section title="用量统计">
                        <UsageDashboard/>
                    </Section>
                </Space>
            </div>
        </ConfigProvider>
    )
}

const Section = ({title, children}) => (
    <Card size="small" title={title} style={{borderRadius: 8}}>
        {children}
    </Card>
)

ReactDOM.createRoot(document.getElementById('root')).render(<App/>)
