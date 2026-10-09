import {useEffect, useState} from 'react'
import {
    Alert,
    Button,
    Card,
    Col,
    Form,
    Input,
    InputNumber,
    message,
    Row,
    Select,
    Space,
    Switch,
    Tabs,
    Tag,
    Typography
} from 'antd'
import {
    ApartmentOutlined,
    ArrowLeftOutlined,
    BookOutlined,
    CalendarOutlined,
    DatabaseOutlined,
    DiffOutlined,
    EyeOutlined,
    FileTextOutlined,
    MessageOutlined,
    PlusOutlined,
    RobotOutlined,
    RocketOutlined,
    SaveOutlined,
    ThunderboltOutlined
} from '@ant-design/icons'
import WorkflowEditor from './WorkflowEditor'
import {ErrorBoundary} from '../../../eb/ErrorBoundary'
import {agentApi} from '../../../api'

const {TextArea} = Input
const {Text, Title} = Typography

const MODELS_BY_PROVIDER = {
    ollama: [
        {code: 'qwen2.5:7b-instruct-q4_K_M', name: 'Qwen 2.5 7B'},
        {code: 'qwen3:8b', name: 'Qwen3 8B'},
        {code: 'llama3:latest', name: 'Llama 3'},
        {code: 'qwen2.5:14b-instruct-q4_K_M', name: 'Qwen 2.5 14B'},
    ],
    openai: [
        {code: 'gpt-4o', name: 'GPT-4o'},
        {code: 'gpt-4o-mini', name: 'GPT-4o Mini'},
        {code: 'gpt-4-turbo', name: 'GPT-4 Turbo'},
    ],
    dashscope: [
        {code: 'qwen-max', name: 'Qwen Max'},
        {code: 'qwen-plus', name: 'Qwen Plus'},
        {code: 'qwen-turbo', name: 'Qwen Turbo'},
    ],
}

// 三种应用类型：不同类型开放不同能力 tab
// FEATURE049 follow-up: 能力集市 5 项已下掉菜单, 全部内化进对应 Agent 类型 tab.
// - tools/skills/memory/knowledge/variables: agent 类型
// - textgen: chat/agent 通用 (批量任务)
// - trigger: agent/workflow 通用 (定时唤醒)
// - workflow: 仅 workflow 类型
const MODE_CONFIG = {
    chat: {
        label: '对话', icon: <MessageOutlined/>, color: '#1677ff',
        desc: '纯文本对话，配置 Prompt + 模型即可',
        tabs: ['basic', 'prompt', 'modelParams', 'memory', 'textgen'],
        tabLabels: {
            basic: '基本信息', prompt: '系统提示词', modelParams: '模型参数',
            memory: '记忆配置', textgen: '批量生成'
        },
    },
    agent: {
        label: '智能体', icon: <RobotOutlined/>, color: '#722ed1',
        desc: '工具调用 + 技能 + 知识库，具备自主决策能力',
        tabs: ['basic', 'prompt', 'modelParams', 'memory', 'tools', 'skills',
            'variables', 'knowledge', 'trigger', 'textgen'],
        tabLabels: {
            basic: '基本信息', prompt: '系统提示词', modelParams: '模型参数', memory: '记忆配置',
            tools: '工具配置', skills: '技能配置', variables: '变量配置', knowledge: '知识库',
            trigger: '定时触发', textgen: '批量生成'
        },
    },
    workflow: {
        label: '工作流', icon: <ApartmentOutlined/>, color: '#13c2c2',
        desc: '可视化多步骤流程编排',
        tabs: ['basic', 'modelParams', 'workflow', 'trigger'],
        tabLabels: {
            basic: '基本信息', modelParams: '模型参数', workflow: '流程编排',
            trigger: '定时触发'
        },
    },
}

const AgentAppEditor = ({app, onBack, onSaved}) => {
    const [localApp, setLocalApp] = useState(app)
    const isPublished = localApp.status === 'PUBLISHED'
    // 默认 chat，老数据无 agentMode 字段也走 chat
    const appMode = localApp.agentMode || 'chat'
    const modeDef = MODE_CONFIG[appMode] || MODE_CONFIG.chat
    const [saving, setSaving] = useState(false)
    const [upgrading, setUpgrading] = useState(false)
    const [activeTab, setActiveTab] = useState(modeDef.tabs[0])
    const [form] = Form.useForm()
    const [workflowData, setWorkflowData] = useState(null)

    // 当切换应用类型时，切换到对应类型的第一个 tab
    useEffect(() => {
        if (activeTab && !modeDef.tabs.includes(activeTab)) {
            setActiveTab(modeDef.tabs[0])
        }
    }, [appMode])

    // 应用类型变更：保存到 localApp 状态（不立刻持久化，由用户点保存统一提交）
    const handleModeChange = (newMode) => {
        setLocalApp(prev => ({...prev, agentMode: newMode}))
        form.setFieldsValue({agentMode: newMode})
    }

    const handleSave = async () => {
        try {
            const values = await form.validateFields()
            setSaving(true)
            await agentApi.appUpdate({id: localApp.id, agentMode: appMode, ...values, workflowData})
            message.success('保存成功')
            onSaved?.()
        } catch (e) {
            if (!e.errorFields) message.error('保存失败')
        } finally {
            setSaving(false)
        }
    }

    const handlePublish = async () => {
        try {
            await agentApi.appPublish(localApp.appCode)
            message.success('发布成功')
            onSaved?.()
        } catch (e) {
            message.error('发布失败')
        }
    }

    const handleUpgrade = async () => {
        try {
            setUpgrading(true)
            const updated = await agentApi.appUpgrade(localApp.appCode)
            const appData = updated?.data || updated
            if (appData) {
                setLocalApp(appData)
                message.success(`已升级到 ${appData.version || '新'} 版本，可编辑后发布`)
            }
        } catch (e) {
            message.error('升级失败：' + (e?.message || '未知错误'))
        } finally {
            setUpgrading(false)
        }
    }

    return (
        <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
            <div style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '12px 16px', borderBottom: '1px solid #f0f0f0',
                background: '#fff', flexShrink: 0,
            }}>
                <Button type="text" icon={<ArrowLeftOutlined/>} onClick={onBack}/>
                <Title level={5} style={{margin: 0, flex: 1, minWidth: 0}}>
                    <span style={{marginRight: 8}}>{localApp.appName}</span>
                    <Tag color={modeDef.color} style={{borderRadius: 20, marginRight: 4}}>
                        {modeDef.icon} {modeDef.label}
                    </Tag>
                    <Tag color={isPublished ? 'green' : 'default'} style={{borderRadius: 20}}>
                        {isPublished ? '已发布' : '草稿'}
                    </Tag>
                </Title>
                <div style={{flex: 1}}/>
                {isPublished ? (
                    <Button icon={<DiffOutlined/>} loading={upgrading} onClick={handleUpgrade}>
                        升级到新草稿
                    </Button>
                ) : (
                    <Space>
                        <Button icon={<SaveOutlined/>} type="primary" ghost onClick={handleSave}
                                loading={saving}>保存</Button>
                        <Button icon={<RocketOutlined/>} type="primary" onClick={handlePublish}>发布</Button>
                    </Space>
                )}
            </div>

            <div style={{flex: 1, overflow: 'auto', padding: 16, background: '#f5f5f5'}}>
                {isPublished && (
                    <Alert
                        type="warning"
                        showIcon
                        icon={<EyeOutlined/>}
                        message="查看模式：当前为已发布版本，所有字段仅可浏览不可修改"
                        description="如需调整配置，请点击右上角「升级到新草稿」按钮，系统会基于当前版本创建一份可编辑的草稿。"
                        style={{marginBottom: 16, borderRadius: 8}}
                    />
                )}

                {/* 应用类型选择（发布后只读） */}
                <Card size="small" style={{marginBottom: 16, borderLeft: `3px solid ${modeDef.color}`}}
                      styles={{body: {padding: '12px 16px'}}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap'}}>
                        <div style={{display: 'flex', alignItems: 'center', gap: 8, minWidth: 120}}>
                            <span style={{fontSize: 18, color: modeDef.color}}>{modeDef.icon}</span>
                            <div>
                                <Text strong style={{fontSize: 13}}>应用类型</Text>
                                <div>
                                    <Text type="secondary" style={{fontSize: 12}}>{modeDef.label}模式</Text>
                                </div>
                            </div>
                        </div>
                        <Select
                            value={appMode}
                            disabled={isPublished}
                            onChange={handleModeChange}
                            style={{flex: 1, maxWidth: 400}}
                            options={[
                                {
                                    value: 'chat',
                                    label: <span><MessageOutlined style={{color: '#1677ff'}}/> 对话 - 纯文本对话</span>
                                },
                                {
                                    value: 'agent',
                                    label: <span><RobotOutlined style={{color: '#722ed1'}}/> 智能体 - 工具调用 + 知识库</span>
                                },
                                {
                                    value: 'workflow',
                                    label: <span><ApartmentOutlined style={{color: '#13c2c2'}}/> 工作流 - 多步骤流程编排</span>
                                },
                            ]}
                        />
                        <Text type="secondary" style={{fontSize: 12}}>{modeDef.desc}</Text>
                    </div>
                </Card>

                <Card size="small">
                    <Tabs activeKey={activeTab} onChange={setActiveTab} style={{minHeight: 300}}>
                        {modeDef.tabs.map(tabKey => (
                            <Tabs.TabPane key={tabKey} tab={modeDef.tabLabels[tabKey] || tabKey}>
                                <Form form={form} layout="vertical"
                                      initialValues={{
                                          appName: localApp.appName,
                                          description: localApp.description,
                                          prompt: localApp.prompt,
                                          modelProvider: localApp.modelProvider || 'ollama',
                                          modelName: localApp.modelName || 'qwen2.5:7b-instruct-q4_K_M',
                                          iconUrl: localApp.iconUrl,
                                          tools: localApp.tools,
                                          skillCodes: localApp.skillCodes,
                                          variables: localApp.variables,
                                          knowledgeIds: localApp.knowledgeIds,
                                          temperature: localApp.temperature ?? 0.7,
                                          topP: localApp.topP ?? 0.9,
                                          maxTokens: localApp.maxTokens ?? 4096,
                                          historyWindowSize: localApp.historyWindowSize ?? 50,
                                          enableLongMemory: localApp.enableLongMemory ?? false,
                                          memoryPrompt: localApp.memoryPrompt || '',
                                      }}>
                                    {/* 基本信息 */}
                                    {tabKey === 'basic' && (
                                        <Row gutter={16}>
                                            <Col span={16}>
                                                <Form.Item name="appName" label="应用名称" rules={[{required: true}]}>
                                                    <Input disabled={isPublished}/>
                                                </Form.Item>
                                            </Col>
                                            <Col span={8}>
                                                <Form.Item name="iconUrl" label="图标 URL">
                                                    <Input disabled={isPublished} placeholder="图片URL"/>
                                                </Form.Item>
                                            </Col>
                                            <Col span={24}>
                                                <Form.Item name="description" label="应用描述">
                                                    <TextArea rows={2} disabled={isPublished}/>
                                                </Form.Item>
                                            </Col>
                                            <Col span={8}>
                                                <Form.Item name="modelProvider" label="供应商"
                                                           rules={[{required: true}]}>
                                                    <Select disabled={isPublished} onChange={(val) => {
                                                        const models = MODELS_BY_PROVIDER[val] || []
                                                        if (models[0]) form.setFieldsValue({modelName: models[0].code})
                                                    }}>
                                                        <Select.Option value="ollama">Ollama</Select.Option>
                                                        <Select.Option value="openai">OpenAI</Select.Option>
                                                        <Select.Option value="dashscope">阿里通义</Select.Option>
                                                    </Select>
                                                </Form.Item>
                                            </Col>
                                            <Col span={16}>
                                                <Form.Item name="modelName" label="模型" rules={[{required: true}]}>
                                                    <Select disabled={isPublished}>
                                                        {(MODELS_BY_PROVIDER[form.getFieldValue?.('modelProvider') || localApp.modelProvider || 'ollama'] || []).map(m => (
                                                            <Select.Option key={m.code}
                                                                           value={m.code}>{m.name}</Select.Option>
                                                        ))}
                                                    </Select>
                                                </Form.Item>
                                            </Col>
                                        </Row>
                                    )}

                                    {/* 系统提示词 */}
                                    {tabKey === 'prompt' && (
                                        <Form.Item name="prompt" label="系统提示词"
                                                   extra="定义 Agent 的身份、能力、行为规范和约束条件">
                                            <TextArea rows={12} disabled={isPublished}
                                                      placeholder="你是智能助手，负责回答用户问题..."/>
                                        </Form.Item>
                                    )}

                                    {/* 模型参数 */}
                                    {tabKey === 'modelParams' && (
                                        <div>
                                            <Alert type="info" showIcon
                                                   title="模型参数控制生成行为。不同模型支持的参数范围略有差异。"
                                                   style={{marginBottom: 16, border: 'none', background: '#e6f4ff'}}/>
                                            <Card size="small" style={{background: '#fafafa', marginBottom: 16}}>
                                                <div style={{marginBottom: 12}}>
                                                    <Text strong><ThunderboltOutlined/> 采样参数</Text>
                                                    <Text type="secondary" style={{marginLeft: 8, fontSize: 12}}>
                                                        控制生成多样性和确定性
                                                    </Text>
                                                </div>
                                                <Row gutter={16}>
                                                    <Col span={8}>
                                                        <Form.Item name="temperature" label="Temperature 温度"
                                                                   extra="0=确定性，2=高随机">
                                                            <InputNumber min={0} max={2} step={0.1}
                                                                         disabled={isPublished}
                                                                         style={{width: '100%'}}/>
                                                        </Form.Item>
                                                    </Col>
                                                    <Col span={8}>
                                                        <Form.Item name="topP" label="Top P 核采样"
                                                                   extra="0-1，建议 0.9">
                                                            <InputNumber min={0} max={1} step={0.05}
                                                                         disabled={isPublished}
                                                                         style={{width: '100%'}}/>
                                                        </Form.Item>
                                                    </Col>
                                                    <Col span={8}>
                                                        <Form.Item name="maxTokens" label="Max Tokens 最大输出"
                                                                   extra="单次响应最大 token 数">
                                                            <InputNumber min={100} max={128000} step={100}
                                                                         disabled={isPublished}
                                                                         style={{width: '100%'}}/>
                                                        </Form.Item>
                                                    </Col>
                                                </Row>
                                            </Card>
                                        </div>
                                    )}

                                    {/* 记忆配置 */}
                                    {tabKey === 'memory' && (
                                        <div>
                                            <Alert type="info" showIcon
                                                   title="记忆配置控制对话上下文管理"
                                                   style={{marginBottom: 16, border: 'none', background: '#e6f4ff'}}/>
                                            <Card size="small" style={{background: '#fafafa', marginBottom: 16}}>
                                                <div style={{marginBottom: 12}}>
                                                    <Text strong><DatabaseOutlined/> 历史窗口</Text>
                                                    <Text type="secondary" style={{marginLeft: 8, fontSize: 12}}>
                                                        保留最近 N 条对话消息作为上下文
                                                    </Text>
                                                </div>
                                                <Form.Item name="historyWindowSize" label="历史窗口大小">
                                                    <InputNumber min={0} max={100} disabled={isPublished}
                                                                 style={{width: 200}}/>
                                                </Form.Item>
                                            </Card>
                                            <Card size="small" style={{background: '#fafafa', marginBottom: 16}}>
                                                <div style={{marginBottom: 12}}>
                                                    <Text strong><BookOutlined/> 长期记忆</Text>
                                                    <Text type="secondary" style={{marginLeft: 8, fontSize: 12}}>
                                                        跨会话持久化关键信息
                                                    </Text>
                                                </div>
                                                <Form.Item name="enableLongMemory" label="启用长期记忆"
                                                           valuePropName="checked">
                                                    <Switch disabled={isPublished}/>
                                                </Form.Item>
                                                <Form.Item name="memoryPrompt" label="记忆提示词"
                                                           extra="定义如何总结和存储对话内容">
                                                    <TextArea rows={3} disabled={isPublished}
                                                              placeholder="例：提取用户的偏好和关键事实，以 JSON 结构存储..."/>
                                                </Form.Item>
                                            </Card>
                                        </div>
                                    )}

                                    {/* 工具配置 */}
                                    {tabKey === 'tools' && (
                                        <Form.Item name="tools" label="工具配置" extra="JSON 数组格式">
                                            <TextArea rows={8} disabled={isPublished}
                                                      placeholder='[{"toolCode":"web-search","toolName":"网页搜索"}]'/>
                                        </Form.Item>
                                    )}

                                    {/* 技能配置 */}
                                    {tabKey === 'skills' && (
                                        <Form.Item name="skillCodes" label="技能编码" extra="JSON 数组格式">
                                            <TextArea rows={8} disabled={isPublished}
                                                      placeholder='["code-review", "data-analyzer"]'/>
                                        </Form.Item>
                                    )}

                                    {/* 变量配置 */}
                                    {tabKey === 'variables' && (
                                        <Form.Item name="variables" label="变量配置" extra="JSON 数组格式">
                                            <TextArea rows={8} disabled={isPublished}
                                                      placeholder='[{"name":"userName","label":"用户名","type":"string"}]'/>
                                        </Form.Item>
                                    )}

                                    {/* 知识库 */}
                                    {tabKey === 'knowledge' && (
                                        <Form.Item name="knowledgeIds" label="知识库" extra="JSON 数组格式">
                                            <TextArea rows={8} disabled={isPublished}
                                                      placeholder='[1, 2, 3]'/>
                                        </Form.Item>
                                    )}

                                    {/* 定时触发 — FEATURE013 T3.1 内化 */}
                                    {tabKey === 'trigger' && (
                                        <div>
                                            <Alert type="info" showIcon
                                                   title={`为「${localApp.appName}」配置定时唤醒任务`}
                                                   description="到点自动唤起该 Agent 执行。可设置 cron 表达式 / 输入 prompt。"
                                                   style={{marginBottom: 16, border: 'none', background: '#e6f4ff'}}/>
                                            <Card size="small" style={{marginBottom: 12, background: '#fafafa'}}>
                                                <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                                                    <CalendarOutlined style={{fontSize: 18, color: '#722ed1'}}/>
                                                    <Text strong>定时触发 (Agent Trigger)</Text>
                                                </div>
                                                <ul style={{
                                                    marginTop: 8,
                                                    paddingLeft: 20,
                                                    color: '#666',
                                                    fontSize: 12
                                                }}>
                                                    <li>已支持后端能力：z_agent_trigger 表 + TriggerScheduler (cron
                                                        解析)
                                                    </li>
                                                    <li>归属即新建/编辑触发器时，绑定 appCode = 当前应用</li>
                                                    <li>本期为骨架版：完整 CRUD 列表与表单将通过 triggerApi 内联接入
                                                        (FEATURE053 follow-up)
                                                    </li>
                                                </ul>
                                            </Card>
                                            <Space>
                                                <Button type="primary" icon={<PlusOutlined/>} disabled={isPublished}>
                                                    新建触发任务 (待 FEATURE053 内嵌)
                                                </Button>
                                            </Space>
                                        </div>
                                    )}

                                    {/* 批量文本生成 — FEATURE013 T3.4 内化 */}
                                    {tabKey === 'textgen' && (
                                        <div>
                                            <Alert type="info" showIcon
                                                   title={`为「${localApp.appName}」配置批量文本生成任务`}
                                                   description="上传数据行，AI 批量产出结果。常用于：批量改写、批量分类、批量提取。"
                                                   style={{marginBottom: 16, border: 'none', background: '#e6f4ff'}}/>
                                            <Card size="small" style={{marginBottom: 12, background: '#fafafa'}}>
                                                <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                                                    <FileTextOutlined style={{fontSize: 18, color: '#722ed1'}}/>
                                                    <Text strong>批量文本生成 (Text Generator)</Text>
                                                </div>
                                                <ul style={{
                                                    marginTop: 8,
                                                    paddingLeft: 20,
                                                    color: '#666',
                                                    fontSize: 12
                                                }}>
                                                    <li>已支持后端能力：z_agent_text_gen / z_agent_text_gen_result 表
                                                    </li>
                                                    <li>归属由 task.ownerId 或 prompt 模板中显式引用 appCode 决定</li>
                                                    <li>本期为骨架版：完整任务列表与 CSV 上传将通过 textGenApi 内联接入
                                                        (FEATURE053 follow-up)
                                                    </li>
                                                </ul>
                                            </Card>
                                            <Space>
                                                <Button type="primary" icon={<PlusOutlined/>} disabled={isPublished}>
                                                    新建批量生成 (待 FEATURE053 内嵌)
                                                </Button>
                                            </Space>
                                        </div>
                                    )}

                                    {/* 工作流 */}
                                    {tabKey === 'workflow' && (
                                        <div>
                                            <Alert type="info" showIcon
                                                   title="工作流模式使用轻量引擎，支持中断恢复。节点定义存储在应用配置中。"
                                                   style={{marginBottom: 16, border: 'none', background: '#e6f4ff'}}/>
                                            <ErrorBoundary
                                                fallbackTitle="流程编辑器加载失败"
                                                fallbackDescription="LogicFlow 初始化异常。请重试，或切换到「对话」/「智能体」模式后重新切回工作流。"
                                                onReset={() => setWorkflowData(null)}
                                            >
                                                <WorkflowEditor
                                                    readOnly={isPublished}
                                                    value={workflowData}
                                                    onChange={setWorkflowData}
                                                    // FEATURE013 A5: 把 appCode / appName 传给 WorkflowEditor,
                                                    // 让用户在画布上能直接保存到后端 z_agent_flow_definition
                                                    appCode={localApp.appCode}
                                                    flowName={localApp.appName}
                                                />
                                            </ErrorBoundary>
                                        </div>
                                    )}
                                </Form>
                            </Tabs.TabPane>
                        ))}
                    </Tabs>
                </Card>
            </div>
        </div>
    )
}


export default AgentAppEditor