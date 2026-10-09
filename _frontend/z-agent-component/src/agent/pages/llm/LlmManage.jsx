import {useState} from 'react'
import {Tabs} from 'antd'
import {ApiOutlined, BarChartOutlined, RobotOutlined} from '@ant-design/icons'
import LlmProvider from './provider/index.jsx'
import LlmModel from './model/index.jsx'
import UsageDashboard from '../usage/index.jsx'
import {PageHeader} from '../../ui'

/**
 * FEATURE050: 模型与凭证 — 3 个独立菜单合并为单页 + 3 个 tab.
 *
 * 合并前:
 *   - /ai/llm/provider  LLM 提供商
 *   - /ai/llm/model     LLM 模型
 *   - /ai/usage         用量统计
 *
 * 合并后:
 *   - /ai/llm           模型与凭证 (单页, 内部 3 tab)
 *
 * tab 内容直接复用已有组件 (LlmProvider / LlmModel / UsageDashboard),
 * 各组件自管 state/请求, 不在此处抽公共.
 *
 * 模型为默认 tab — 是用户日常维护最高频的页面.
 */
const LlmManage = () => {
    const [activeTab, setActiveTab] = useState('model')

    return (
        <>
            <PageHeader
                title="模型与凭证"
                subtitle="LLM 提供商、模型配置、用量统计 (FEATURE050 合并单页)"
                breadcrumb={[{label: '智能中心'}, {label: '模型管理'}]}
            />
            <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                size="large"
                style={{flex: 1, display: 'flex', flexDirection: 'column'}}
                items={[
                    {
                        key: 'model',
                        label: <span><RobotOutlined style={{color: '#1677ff'}}/> 模型</span>,
                        children: <div style={{paddingTop: 8}}><LlmModel/></div>,
                    },
                    {
                        key: 'provider',
                        label: <span><ApiOutlined style={{color: '#722ed1'}}/> 供应商</span>,
                        children: <div style={{paddingTop: 8}}><LlmProvider/></div>,
                    },
                    {
                        key: 'usage',
                        label: <span><BarChartOutlined style={{color: '#13c2c2'}}/> 用量统计</span>,
                        children: <div style={{paddingTop: 8}}><UsageDashboard/></div>,
                    },
                ]}
            />
        </>
    )
}

export default LlmManage