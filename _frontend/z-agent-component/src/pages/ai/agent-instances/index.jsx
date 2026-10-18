import {useEffect, useRef, useState} from 'react'
import {
    Alert,
    Badge,
    Button,
    Card,
    Checkbox,
    Drawer,
    Empty,
    Form,
    Input,
    List,
    message,
    Modal,
    Popconfirm,
    Select,
    Skeleton,
    Space,
    Table,
    Tabs,
    Tag,
    Tooltip,
    Typography
} from 'antd'
import {
    CheckCircleOutlined,
    CloseCircleOutlined,
    CodeOutlined,
    ContainerOutlined,
    DesktopOutlined,
    LinkOutlined,
    PlusOutlined,
    ReloadOutlined,
    RocketOutlined,
    ThunderboltOutlined
} from '@ant-design/icons'
import {agentInstanceApi, agentSkillBindApi, skillMarketApi} from '../../../services/agentInstance'
import {PageHeader, SearchInput} from '@yuku123/z-frontend-common/ui'

const {Text, Title} = Typography

const AGENT_TYPE_ICONS = {
    ZBOT: <RocketOutlined/>,
    HERMES_CONTAINER: <ContainerOutlined/>,
    HERMES_LOCAL: <DesktopOutlined/>,
    CLAUDE_CODE: <CodeOutlined/>,
    OPENCODE: <CodeOutlined/>,
    CUSTOM: <ThunderboltOutlined/>,
}

const AGENT_TYPE_DESCRIPTIONS = {
    ZBOT: 'z-opc 自家 z-agent-bot，JVM 内 ReAct 循环',
    HERMES_CONTAINER: 'Docker 容器里跑的 Hermes（兼容旧 CONTAINER 模式）',
    HERMES_LOCAL: '本机 CLI 启动 hermes serve（需要本机装了 hermes）',
    CLAUDE_CODE: '本机 Claude Code CLI（需要 `claude` 在 PATH 中）',
    OPENCODE: '本机 OpenCode CLI（需要 `opencode` 在 PATH 中）',
    CUSTOM: '用户自定义命令（bash -c "{customCommand} --port {port}"）',
}

const AgentInstanceManager = () => {
    const [agentTypes, setAgentTypes] = useState({})
    const [instances, setInstances] = useState([])
    const [loading, setLoading] = useState(false)
    const [createOpen, setCreateOpen] = useState(false)
    const [creating, setCreating] = useState(false)
    const [selectedAgentType, setSelectedAgentType] = useState('ZBOT')
    const [bindDrawerOpen, setBindDrawerOpen] = useState(false)
    const [bindDrawerInstance, setBindDrawerInstance] = useState(null)
    const [bindings, setBindings] = useState([])
    const [syncLogs, setSyncLogs] = useState([])
    const [healthMap, setHealthMap] = useState({})
    // Marketplace picker state (Wishlist 7)
    const [marketSkills, setMarketSkills] = useState([])
    const [marketLoading, setMarketLoading] = useState(false)
    const [marketKeyword, setMarketKeyword] = useState('')
    const [selectedMarketSkills, setSelectedMarketSkills] = useState([])
    const [binding, setBinding] = useState(false)
    const [form] = Form.useForm()
    // 防止 React StrictMode + 重复 useEffect 导致同一接口被并发调用多次, 错误弹窗堆叠
    const listInflightRef = useRef(null)
    const typesInflightRef = useRef(null)

    useEffect(() => {
        refreshAgentTypes()
    }, [])

    const refreshAgentTypes = async () => {
        if (typesInflightRef.current) return typesInflightRef.current
        const p = (async () => {
            try {
                const res = await agentInstanceApi.listAgentTypes()
                const data = res?.data ?? res
                setAgentTypes(data || {})
            } catch (e) {
                const isNotFound = e?.message?.includes('404')
                const tip = isNotFound
                    ? '加载 Agent 类型失败：后端接口不存在 (404), 请确认本地后端 (localhost:8888) 已启动且包含 z-agent-center-web 模块'
                    : '加载 Agent 类型失败：' + (e?.message || '未知错误')
                message.error(tip)
            } finally {
                typesInflightRef.current = null
            }
        })()
        typesInflightRef.current = p
        return p
    }

    const refreshInstances = async () => {
        // 取当前用户 ownerId — 简化：从 localStorage / store 取
        const ownerId = localStorage.getItem('userId') || 'admin'
        if (listInflightRef.current) return listInflightRef.current
        setLoading(true)
        const p = (async () => {
            try {
                const res = await agentInstanceApi.listByOwner(ownerId)
                setInstances(res?.data ?? res ?? [])
            } catch (e) {
                const isNotFound = e?.message?.includes('404')
                const tip = isNotFound
                    ? '加载实例失败：后端接口不存在 (404), 请确认本地后端 (localhost:8888) 已启动且包含 z-agent-center-web 模块'
                    : '加载实例失败：' + (e?.message || '未知错误')
                message.error(tip)
            } finally {
                setLoading(false)
                listInflightRef.current = null
            }
        })()
        listInflightRef.current = p
        return p
    }

    useEffect(() => {
        refreshInstances()
    }, [])

    const handleCreate = async () => {
        try {
            const values = await form.validateFields()
            setCreating(true)
            const agentType = values.agentType
            const payload = {
                appCode: values.appCode,
                userId: localStorage.getItem('userId') || 'admin',
                userName: localStorage.getItem('userName') || 'admin',
                agentType,
                callbackUrl: values.callbackUrl || null,
                customCommand: values.customCommand || null,
            }
            const res = await agentInstanceApi.createExternal(payload)
            if (res?.code !== undefined && res.code !== 0) {
                throw new Error(res.msg || '创建失败')
            }
            message.success(`实例已创建（${agentType}）`)
            setCreateOpen(false)
            form.resetFields()
            refreshInstances()
        } catch (e) {
            if (!e.errorFields) {
                const isNotFound = e?.message?.includes('404')
                const tip = isNotFound
                    ? '添加 Agent 实例失败：后端接口不存在 (404), 请确认本地后端 (localhost:8888) 已启动且包含 z-agent-center-web 模块 (含 /api/agent/instance/create-external 端点)'
                    : '添加 Agent 实例失败：' + (e?.message || '未知错误')
                message.error(tip)
            }
        } finally {
            setCreating(false)
        }
    }

    const handleHealthCheck = async (instanceCode) => {
        try {
            const res = await agentInstanceApi.health(instanceCode)
            const data = res?.data ?? res
            setHealthMap(prev => ({...prev, [instanceCode]: data}))
        } catch (e) {
            message.error('健康检查失败：' + (e?.message || '未知错误'))
        }
    }

    const handleHealthAll = async () => {
        for (const inst of instances) {
            await handleHealthCheck(inst.instanceCode)
        }
    }

    const handleDestroy = async (inst) => {
        try {
            if (inst.agentType === 'HERMES_CONTAINER') {
                await agentInstanceApi.destroyContainer(inst.instanceCode)
            } else if (['HERMES_LOCAL', 'CLAUDE_CODE', 'OPENCODE', 'CUSTOM'].includes(inst.agentType)) {
                await agentInstanceApi.destroyLocalProcess(inst.instanceCode)
            }
            message.success('实例已销毁')
            refreshInstances()
        } catch (e) {
            message.error('销毁失败：' + (e?.message || '未知错误'))
        }
    }

    const openBindDrawer = async (inst) => {
        setBindDrawerInstance(inst)
        setBindDrawerOpen(true)
        setSelectedMarketSkills([])
        setMarketKeyword('')
        try {
            const [bRes, lRes, mRes] = await Promise.all([
                agentSkillBindApi.listBindings(inst.instanceCode),
                agentSkillBindApi.syncLogs(inst.instanceCode, 30),
                skillMarketApi.page({pageNum: 1, pageSize: 50}),
            ])
            setBindings(bRes?.data ?? bRes ?? [])
            setSyncLogs(lRes?.data ?? lRes ?? [])
            const market = mRes?.data ?? mRes
            // market 可能是 {records: [...], total: ...} 或裸数组
            const records = Array.isArray(market) ? market : (market?.records || market?.list || [])
            setMarketSkills(records)
        } catch (e) {
            message.error('加载绑定信息失败：' + (e?.message || '未知错误'))
        }
    }

    const refreshMarketplace = async (keyword) => {
        setMarketLoading(true)
        try {
            const res = await skillMarketApi.page({pageNum: 1, pageSize: 50, keyword: keyword || ''})
            const market = res?.data ?? res
            const records = Array.isArray(market) ? market : (market?.records || market?.list || [])
            setMarketSkills(records)
        } catch (e) {
            message.error('加载技能市场失败：' + (e?.message || '未知错误'))
        } finally {
            setMarketLoading(false)
        }
    }

    const handleBindFromMarket = async () => {
        if (!bindDrawerInstance || selectedMarketSkills.length === 0) {
            message.warning('请先勾选要绑定的 Skill')
            return
        }
        setBinding(true)
        try {
            await agentSkillBindApi.bind(
                bindDrawerInstance.instanceCode,
                selectedMarketSkills,
                'PUSH'
            )
            message.success(`已绑定 ${selectedMarketSkills.length} 个 Skill`)
            setSelectedMarketSkills([])
            // 刷新 bindings
            const bRes = await agentSkillBindApi.listBindings(bindDrawerInstance.instanceCode)
            setBindings(bRes?.data ?? bRes ?? [])
        } catch (e) {
            message.error('绑定失败：' + (e?.message || '未知错误'))
        } finally {
            setBinding(false)
        }
    }

    const columns = [
        {
            title: '实例编码',
            dataIndex: 'instanceCode',
            key: 'instanceCode',
            width: 220,
            render: (text, record) => (
                <Space orientation="vertical" size={0}>
                    <Text code>{text}</Text>
                    {record.skillRootPath && (
                        <Text type="secondary" style={{fontSize: 11}}>
                            <LinkOutlined/> {record.skillRootPath}
                        </Text>
                    )}
                </Space>
            ),
        },
        {
            title: '应用',
            dataIndex: 'appCode',
            key: 'appCode',
            width: 120,
        },
        {
            title: 'Agent 类型',
            dataIndex: 'agentType',
            key: 'agentType',
            width: 200,
            render: (type) => {
                const t = type || 'ZBOT'
                return (
                    <Tag color={t === 'ZBOT' ? 'blue' : 'purple'} icon={AGENT_TYPE_ICONS[t]}>
                        {agentTypes[t] || t}
                    </Tag>
                )
            },
        },
        {
            title: '运行模式',
            dataIndex: 'runMode',
            key: 'runMode',
            width: 130,
            render: (mode) => <Tag>{mode || '—'}</Tag>,
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 130,
            render: (status, record) => {
                const health = healthMap[record.instanceCode]
                if (health) {
                    return health.healthy
                        ? <Badge status="success" text="healthy"/>
                        : <Badge status="error" text="unhealthy"/>
                }
                const color = status === 'ACTIVE' ? 'green' : status === 'ERROR' ? 'red' : 'default'
                return <Tag color={color}>{status || '—'}</Tag>
            },
        },
        {
            title: '最后心跳',
            dataIndex: 'lastHeartbeat',
            key: 'lastHeartbeat',
            width: 170,
            render: (t) => t ? <Text type="secondary" style={{fontSize: 12}}>{t}</Text> :
                <Text type="secondary">—</Text>,
        },
        {
            title: '操作',
            key: 'actions',
            width: 280,
            render: (_, record) => (
                <Space>
                    <Tooltip title="健康检查">
                        <Button size="small" icon={<CheckCircleOutlined/>}
                                onClick={() => handleHealthCheck(record.instanceCode)}/>
                    </Tooltip>
                    <Button size="small" onClick={() => openBindDrawer(record)}>
                        Skill 绑定
                    </Button>
                    <Popconfirm
                        title="销毁此实例？"
                        description="本地进程 / Docker 容器都将被清理"
                        onConfirm={() => handleDestroy(record)}
                        okText="销毁"
                        cancelText="取消"
                    >
                        <Button size="small" danger icon={<CloseCircleOutlined/>}>销毁</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <div>
            <PageHeader
                title="多 Agent 实例管理"
                subtitle="支持 ZBOT / Hermes / Claude Code / OpenCode 等多种 Agent 类型"
            />
            <Card extra={
                <Space>
                    <Tag color="purple">FEATURE014</Tag>
                    <Button icon={<ReloadOutlined/>} onClick={refreshInstances}>刷新</Button>
                    <Button icon={<CheckCircleOutlined/>} onClick={handleHealthAll}>批量健康检查</Button>
                    <Button type="primary" icon={<PlusOutlined/>}
                            onClick={() => setCreateOpen(true)}>
                        创建实例
                    </Button>
                </Space>
            }>
                <Alert
                    type="info" showIcon
                    message="多 Agent 实例"
                    description="支持 ZBOT / Hermes (Docker+本地) / Claude Code / OpenCode / 自定义进程。创建后会自动拉起对应进程 / 容器，并在心跳中跟踪状态。"
                    style={{marginBottom: 16}}
                />
                <Table
                    rowKey="instanceCode"
                    columns={columns}
                    dataSource={instances}
                    loading={loading}
                    size="middle"
                    pagination={{pageSize: 10}}
                />
            </Card>

            {/* 创建实例 Modal */}
            <Modal
                title="创建 Agent 实例"
                open={createOpen}
                onCancel={() => setCreateOpen(false)}
                onOk={handleCreate}
                confirmLoading={creating}
                okText="创建"
                cancelText="取消"
                size="large"
            >
                <Form form={form} layout="vertical" initialValues={{agentType: 'ZBOT'}}>
                    <Form.Item label="应用编码 (appCode)" name="appCode"
                               rules={[{required: true, message: '请输入应用编码'}]}>
                        <Input placeholder="例如 default-chat"/>
                    </Form.Item>

                    <Form.Item label="Agent 类型" name="agentType"
                               rules={[{required: true}]}>
                        <Select onChange={setSelectedAgentType}>
                            {Object.entries(agentTypes).map(([code, label]) => (
                                <Select.Option key={code} value={code}>
                                    <Space>
                                        {AGENT_TYPE_ICONS[code]}
                                        <span>{label}</span>
                                        <Text type="secondary" style={{fontSize: 12}}>
                                            ({AGENT_TYPE_DESCRIPTIONS[code]})
                                        </Text>
                                    </Space>
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    {selectedAgentType !== 'ZBOT' && (
                        <Form.Item
                            label="回调地址 (callbackUrl)"
                            name="callbackUrl"
                            extra="外部 Agent 的 HTTP 地址（含端口），center 会用它推送 Skill 和发心跳"
                        >
                            <Input placeholder="http://192.168.1.10:9100"/>
                        </Form.Item>
                    )}

                    {selectedAgentType === 'CUSTOM' && (
                        <Form.Item
                            label="自定义启动命令 (customCommand)"
                            name="customCommand"
                            extra="用 bash -c 执行。{port} 会被替换为分配的端口号"
                            rules={[{required: true}]}
                        >
                            <Input.TextArea
                                rows={3}
                                placeholder='my-agent serve --addr 0.0.0.0 --port {port} --config /etc/my-agent.yaml'
                            />
                        </Form.Item>
                    )}

                    <Alert
                        type="warning" showIcon
                        title="提示"
                        description={AGENT_TYPE_DESCRIPTIONS[selectedAgentType]}
                        style={{marginTop: 8}}
                    />
                </Form>
            </Modal>

            {/* Skill 绑定 Drawer */}
            <Drawer
                title={bindDrawerInstance ? `Skill 绑定 — ${bindDrawerInstance.instanceCode}` : ''}
                open={bindDrawerOpen}
                onClose={() => setBindDrawerOpen(false)}
                size="large"
            >
                <Tabs
                    items={[
                        {
                            key: 'bindings',
                            label: `已绑定 (${bindings.length})`,
                            children: (
                                <Table
                                    rowKey="id"
                                    size="small"
                                    pagination={false}
                                    dataSource={bindings}
                                    columns={[
                                        {title: 'Skill 编码', dataIndex: 'skillCode'},
                                        {
                                            title: '模式', dataIndex: 'bindingType', width: 80,
                                            render: (t) => <Tag>{t}</Tag>
                                        },
                                        {
                                            title: '状态', dataIndex: 'status', width: 100,
                                            render: (s) => <Tag
                                                color={s === 'BOUND' ? 'green' : s === 'ERROR' ? 'red' : 'default'}>{s}</Tag>
                                        },
                                        {title: '绑定时间', dataIndex: 'gmtBind', width: 170},
                                    ]}
                                />
                            ),
                        },
                        {
                            key: 'market',
                            label: '从市场绑定',
                            children: (
                                <div>
                                    <Space style={{marginBottom: 12, width: '100%'}}>
                                        <SearchInput
                                            placeholder="按名称/编码搜索 Skill"
                                            value={marketKeyword}
                                            onChange={setMarketKeyword}
                                            onSearch={(kw) => refreshMarketplace(kw)}
                                            size="large"
                                            allowClear
                                        />
                                        <Button onClick={() => refreshMarketplace(marketKeyword)}
                                                icon={<ReloadOutlined/>}>刷新</Button>
                                        <div style={{flex: 1}}/>
                                        <Tag color="blue">已选 {selectedMarketSkills.length} 个</Tag>
                                        <Button type="primary" icon={<RocketOutlined/>}
                                                loading={binding}
                                                disabled={selectedMarketSkills.length === 0}
                                                onClick={handleBindFromMarket}>
                                            绑定到该实例
                                        </Button>
                                    </Space>
                                    {marketLoading ? (
                                        <Skeleton active paragraph={{rows: 6}}/>
                                    ) : marketSkills.length === 0 ? (
                                        <Empty description="技能市场暂无 PUBLISHED Skill"/>
                                    ) : (
                                        <List
                                            size="small"
                                            bordered
                                            dataSource={marketSkills}
                                            rowKey={(s) => s.skillCode || s.id}
                                            renderItem={(s) => {
                                                const code = s.skillCode || s.code
                                                const isAlreadyBound = bindings.some(b => b.skillCode === code)
                                                return (
                                                    <List.Item
                                                        actions={[
                                                            isAlreadyBound
                                                                ? <Tag color="green" key="b">已绑定</Tag>
                                                                : <Checkbox
                                                                    key="cb"
                                                                    checked={selectedMarketSkills.includes(code)}
                                                                    onChange={e => {
                                                                        if (e.target.checked) {
                                                                            setSelectedMarketSkills(prev => [...prev, code])
                                                                        } else {
                                                                            setSelectedMarketSkills(prev => prev.filter(c => c !== code))
                                                                        }
                                                                    }}/>
                                                        ]}
                                                    >
                                                        <List.Item.Meta
                                                            title={
                                                                <Space>
                                                                    <Text strong>{s.skillName || code}</Text>
                                                                    <Text type="secondary" code style={{fontSize: 12}}>
                                                                        {code}
                                                                    </Text>
                                                                    {s.version && <Tag>v{s.version}</Tag>}
                                                                    {s.categoryCode &&
                                                                        <Tag color="cyan">{s.categoryCode}</Tag>}
                                                                </Space>
                                                            }
                                                            description={s.description || '(无描述)'}
                                                        />
                                                    </List.Item>
                                                )
                                            }}
                                        />
                                    )}
                                </div>
                            ),
                        },
                        {
                            key: 'logs',
                            label: `同步日志 (${syncLogs.length})`,
                            children: (
                                <Table
                                    rowKey="id"
                                    size="small"
                                    pagination={{pageSize: 10}}
                                    dataSource={syncLogs}
                                    columns={[
                                        {title: '时间', dataIndex: 'gmtSync', width: 170},
                                        {title: 'Skill', dataIndex: 'skillCode'},
                                        {
                                            title: '操作', dataIndex: 'action', width: 90,
                                            render: (t) => <Tag>{t}</Tag>
                                        },
                                        {
                                            title: '结果', dataIndex: 'result', width: 90,
                                            render: (t) => <Tag color={t === 'SUCCESS' ? 'green' : 'red'}>{t}</Tag>
                                        },
                                        {title: '耗时(ms)', dataIndex: 'durationMs', width: 100},
                                        {
                                            title: '错误', dataIndex: 'errorMsg',
                                            render: (t) => t ? <Text type="danger">{t}</Text> : '—'
                                        },
                                    ]}
                                />
                            ),
                        },
                    ]}
                />
            </Drawer>
        </div>
    )
}

export default AgentInstanceManager
