import {useEffect, useMemo, useState} from 'react'
import {
    Alert,
    Button,
    Card,
    Form,
    Input,
    InputNumber,
    message,
    Space,
    Switch,
    Table,
    Tabs,
    Tag,
    Tooltip,
    Typography
} from 'antd'
import {
    ApiOutlined,
    CheckCircleOutlined,
    CloudServerOutlined,
    FunctionOutlined,
    PlayCircleOutlined,
    ReloadOutlined,
    WarningOutlined
} from '@ant-design/icons'
import {mcpApi} from '../../api'
import {EmptyState, ErrorState} from '@yuku123/z-frontend-common/ui'

const {TextArea} = Input
const {Text} = Typography

/** schema.properties → 表单初值（枚举/标量给空串，复杂类型给空 JSON 模板） */
const defaultArgs = (schema) => {
    const out = {}
    const props = schema?.properties || {}
    for (const [name, prop] of Object.entries(props)) {
        const type = Array.isArray(prop?.type) ? prop.type[0] : prop?.type
        if (prop?.default != null) out[name] = prop.default
        else if (type === 'array') out[name] = '[]'
        else if (type === 'object') out[name] = '{}'
        else out[name] = undefined
    }
    return out
}

/** 按钮以 name 聚合的 KPI 卡 */
const KpiCard = ({icon, label, value, sub}) => (
    <Card size="small" styles={{body: {padding: '12px 16px'}}}>
        <Space size={10} align="center">
            <span style={{fontSize: 20, color: '#1890ff'}}>{icon}</span>
            <div>
                <div style={{fontSize: 20, fontWeight: 600, lineHeight: 1.2}}>{value}</div>
                <Text type="secondary" style={{fontSize: 12}}>{label}{sub ? ` · ${sub}` : ''}</Text>
            </div>
        </Space>
    </Card>
)

/** inputSchema.properties → 表单控件；unknown 类型回退 JSON 文本域 */
const SchemaValueInput = ({name, prop, required}) => {
    const type = Array.isArray(prop?.type) ? prop.type[0] : prop?.type
    const label = (
        <Space size={6}>
            <code style={{fontSize: 12}}>{name}</code>
            {required && <Text type="danger">*</Text>}
            {prop?.description && <Text type="secondary" style={{fontSize: 12, fontWeight: 400}}>{prop.description}</Text>}
        </Space>
    )
    return (
        <Form.Item
            name={name}
            label={label}
            rules={required ? [{required: true, message: `请填写 ${name}`}] : undefined}
        >
            {type === 'boolean' ? <Switch/> :
                type === 'number' || type === 'integer' ? <InputNumber style={{width: '100%'}}/> :
                    type === 'string' && !prop?.enum ? <Input placeholder={prop?.default != null ? String(prop.default) : ''}/> :
                        <TextArea
                            rows={2}
                            placeholder={type === 'array' ? '["a","b"]' : type === 'object' ? '{"k":"v"}' : 'JSON 值'}
                            style={{fontFamily: 'monospace', fontSize: 13}}
                        />}
        </Form.Item>
    )
}

/**
 * MCP 服务中心。
 *
 * <p>上游由配置项 z.mcp.servers[*] 声明，z-mcp 没有 server 表，所以本页只读。
 * 控制面（总览/健康/上游真实状态）来自 /api/mcp/admin/* 投影，需要 z.mcp.expose-admin=true；
 * 工具目录与调用走 McpRegistry（/api/mcp/server/*）。工具测试按 inputSchema 生成表单，
 * 不再让用户手写 JSON。</p>
 */
export default function McpPage() {
    const [activeTab, setActiveTab] = useState('servers')
    const [servers, setServers] = useState([])
    const [serverStates, setServerStates] = useState([])
    const [overview, setOverview] = useState(null)
    const [health, setHealth] = useState(null)
    const [adminWarning, setAdminWarning] = useState('')
    const [loading, setLoading] = useState(false)
    const [loadError, setLoadError] = useState(null)

    // Tool test state
    const [selectedServer, setSelectedServer] = useState(null)
    const [tools, setTools] = useState([])
    const [testTool, setTestTool] = useState(null)
    const [toolRunning, setToolRunning] = useState(false)
    const [toolResult, setToolResult] = useState(null)
    const [form] = Form.useForm()

    useEffect(() => {
        loadAll()
    }, [])

    const loadAll = async () => {
        setLoading(true)
        setLoadError(null)
        try {
            const res = await mcpApi.list()
            setServers(Array.isArray(res) ? res : (res?.data || res || []))
        } catch (e) {
            setLoadError(e?.message || 'MCP 上游列表请求失败')
            setServers([])
        } finally {
            setLoading(false)
        }
        // 控制面三条：挂了不影响 servers/tools 主功能，但要点名而不是装没事
        try {
            const res = await mcpApi.adminOverview()
            if (res && res.success === false) setAdminWarning(res.message || '控制面未装配')
            else setOverview(res?.data || res)
        } catch (e) {
            setAdminWarning(e?.message || '控制面请求失败')
        }
        try {
            const res = await mcpApi.serverState()
            if (res && res.success === false) setAdminWarning(res.message || '控制面未装配')
            else setServerStates(res?.data || res || [])
        } catch (e) {
            setAdminWarning(e?.message || '控制面请求失败')
        }
        try {
            const res = await mcpApi.health()
            if (res && res.success === false) setAdminWarning(res.message || '控制面未装配')
            else setHealth(res?.data || res)
        } catch (e) {
            /* health 失败由 overview/state 的警示覆盖 */
        }
    }

    const handleSelectServer = async (record) => {
        setSelectedServer(record)
        setTools([])
        try {
            const res = await mcpApi.listTools({serverName: record.serverName})
            const data = res?.tools || res?.data?.tools || []
            setTools(Array.isArray(data) ? data : [])
        } catch (e) {
            setTools([])
            message.error('获取工具列表失败：' + (e?.message || e))
        }
    }

    const openTest = (tool) => {
        setTestTool(tool)
        setToolResult(null)
        form.setFieldsValue(defaultArgs(tool?.inputSchema))
    }

    const handleRunTool = async () => {
        if (!testTool) return
        let values
        try {
            values = await form.validateFields()
        } catch {
            return
        }
        const schema = testTool.inputSchema
        const props = schema?.properties || {}
        const args = {}
        for (const [k, v] of Object.entries(values)) {
            if (v === undefined || v === null || v === '') continue
            const t = Array.isArray(props[k]?.type) ? props[k]?.type[0] : props[k]?.type
            if (typeof v === 'string' && (t === 'array' || t === 'object')) {
                try {
                    args[k] = JSON.parse(v)
                } catch {
                    args[k] = v
                }
            } else {
                args[k] = v
            }
        }
        setToolRunning(true)
        try {
            const res = await mcpApi.callTool({toolName: testTool.name, arguments: args})
            setToolResult(res?.data || res)
        } catch (e) {
            setToolResult({isError: true, content: [{type: 'text', text: e?.message || '调用失败'}]})
        } finally {
            setToolRunning(false)
        }
    }

    /** 上游真实状态按 name 并进目录行（connected/lastError） */
    const stateByName = useMemo(() => {
        const m = {}
        for (const s of serverStates) m[s?.name] = s
        return m
    }, [serverStates])

    const enrichedServers = useMemo(() => servers.map((s) => {
        const st = stateByName[s.serverName]
        return st ? {...s, lastError: st.lastError ?? null, stateConnected: st.connected} : s
    }), [servers, stateByName])

    // ===== Server columns =====
    const columns = [
        {title: '名称', dataIndex: 'serverName', key: 'serverName', width: 160},
        {
            title: '类型', dataIndex: 'transportType', key: 'transportType', width: 80,
            render: (v) => <Tag color={v === 'LOCAL' ? 'green' : 'blue'}>{v || 'HTTP'}</Tag>
        },
        {title: 'URL', dataIndex: 'url', ellipsis: true},
        {
            title: '状态', key: 'status', width: 110,
            render: (_, r) => {
                const ok = (r.stateConnected ?? (r.status === 'active'))
                return (
                    <Tooltip title={r.lastError || (ok ? '已连接' : '未连接')}>
                        <Tag color={ok ? 'green' : 'red'} icon={ok ? <CheckCircleOutlined/> : <WarningOutlined/>}>
                            {ok ? '在线' : '离线'}
                        </Tag>
                    </Tooltip>
                )
            }
        },
        {title: '工具', dataIndex: 'toolCount', key: 'toolCount', width: 60},
        {
            title: '操作', key: 'actions', width: 90, render: (_, r) => (
                <Space size="small">
                    <Button size="small" icon={<ApiOutlined/>} onClick={(e) => {
                        e.stopPropagation()
                        handleSelectServer(r)
                    }}>工具</Button>
                </Space>
            )
        },
    ]

    const renderOverview = () => {
        if (adminWarning) {
            return (
                <Alert
                    type="warning" showIcon style={{marginBottom: 16}}
                    message="控制面未接入"
                    description={`${adminWarning}。总览/健康/上游真实状态不可用；工具目录与调用不受影响。开启方式：z.mcp.expose-admin=true。`}
                />
            )
        }
        if (!overview) return null
        const healthStatus = health?.status
        return (
            <div style={{marginBottom: 16}}>
                <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: 12}}>
                    {/* 计数口径必须与下方表格一致 = /api/mcp/server/list 的行数（外部上游 + 本机 LOCAL）。
                        overview.serverCount 是「注册表里已注册的外部 server 数」，不含本机那一行，
                        单独拿来当标题数会与表格行数对不上（表里 1 行、标题写 0）。 */}
                    <KpiCard icon={<CloudServerOutlined/>} label="MCP Server" value={servers.length}
                             sub={overview.platform ? `注册 ${overview.serverCount ?? 0} · v${overview.version ?? '?'}` : ''}/>
                    <KpiCard icon={<FunctionOutlined/>} label="工具总数" value={overview.toolCount ?? '-'}
                             sub={`内建 ${overview.builtinToolCount ?? 0} / 外部 ${overview.externalToolCount ?? 0}`}/>
                    <KpiCard icon={<ApiOutlined/>} label="资源" value={overview.resourceCount ?? 0}
                             sub={`提示词 ${overview.promptCount ?? 0}`}/>
                    <KpiCard
                        icon={healthStatus === 'UP' ? <CheckCircleOutlined/> : <WarningOutlined/>}
                        label="健康状态"
                        value={healthStatus || '-'}
                        sub={health?.unreachableServers?.length ? `失联: ${health.unreachableServers.join('、')}` : (overview.protocol?.servedAs ? `协议 ${overview.protocol.servedAs}` : '')}
                    />
                </div>
            </div>
        )
    }

    return (
        <div style={{height: 'calc(100vh - 180px)', display: 'flex', flexDirection: 'column'}}>
            {renderOverview()}
            <Tabs activeKey={activeTab} onChange={setActiveTab} tabBarExtraContent={
                <Button icon={<ReloadOutlined/>} onClick={loadAll} loading={loading}>刷新</Button>
            } items={[
                {key: 'servers', label: '服务与工具'},
                {key: 'test', label: '工具测试', disabled: !testTool},
            ]}/>

            {loadError && (
                <ErrorState
                    error={loadError}
                    title="MCP 上游列表加载失败"
                    description="接口 /api/mcp/server/list 没有返回可用数据。请确认 z.mcp.enabled=true 且 z-mcp 已装配。"
                    onRetry={loadAll}
                    style={{marginBottom: 16}}
                />
            )}

            {activeTab === 'servers' && !loadError && (
                <div style={{flex: 1, display: 'flex', gap: 16, overflow: 'hidden'}}>
                    {/* Server List */}
                    <div style={{flex: selectedServer ? '0 0 46%' : 1, overflow: 'auto'}}>
                        {servers.length === 0 && !loading ? (
                            <EmptyState title="暂无 MCP server"
                                        description="z.mcp.enabled=false，或还没配 z.mcp.servers[*] 上游"/>
                        ) : (
                            <Table columns={columns} dataSource={enrichedServers} rowKey="serverName"
                                   loading={loading}
                                   size="small"
                                   // pagination 豁免：server 清单来自配置项 z.mcp.servers，条数由部署固定，不随运行数据增长
                                   pagination={false}
                                   onRow={(r) => ({
                                       onClick: () => handleSelectServer(r),
                                       style: {
                                           background: selectedServer?.serverName === r.serverName
                                               ? '#e6f7ff' : undefined,
                                           cursor: 'pointer'
                                       }
                                   })}/>
                        )}
                    </div>

                    {/* Tool Explorer */}
                    {selectedServer && (
                        <div style={{
                            flex: '0 0 54%',
                            borderLeft: '1px solid #f0f0f0',
                            paddingLeft: 16,
                            overflow: 'auto'
                        }}>
                            <div style={{
                                marginBottom: 12,
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center'
                            }}>
                                <Text strong style={{fontSize: 15}}>{selectedServer.serverName} — 工具列表</Text>
                                <Button size="small" icon={<ReloadOutlined/>}
                                        onClick={() => handleSelectServer(selectedServer)}>刷新</Button>
                            </div>
                            {tools.length === 0 ? (
                                <EmptyState title="暂无工具" description="该服务未暴露可用工具" size="sm"/>
                            ) : (
                                tools.map(t => (
                                    <Card key={t.name} size="small" style={{marginBottom: 8}}>
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            gap: 12
                                        }}>
                                            <div style={{minWidth: 0}}>
                                                <Text strong>{t.name}</Text>
                                                <div>
                                                    <Text type="secondary"
                                                          style={{fontSize: 12}}>{t.description}</Text>
                                                </div>
                                            </div>
                                            <Button size="small" type="primary" ghost icon={<PlayCircleOutlined/>}
                                                    onClick={() => {
                                                        openTest(t)
                                                        setActiveTab('test')
                                                    }}>
                                                测试
                                            </Button>
                                        </div>
                                    </Card>
                                ))
                            )}
                        </div>
                    )}
                </div>
            )}

            {activeTab === 'test' && (
                <div style={{flex: 1, display: 'flex', gap: 16, overflow: 'hidden'}}>
                    {/* Input */}
                    <div style={{flex: '0 0 44%', display: 'flex', flexDirection: 'column', overflow: 'auto'}}>
                        <div style={{marginBottom: 12}}>
                            <Text strong>{testTool?.name}</Text>
                            <div>
                                <Text type="secondary" style={{fontSize: 12}}>{testTool?.description}</Text>
                            </div>
                        </div>
                        {Object.keys(testTool?.inputSchema?.properties || {}).length === 0 ? (
                            <Alert type="info" showIcon message="该工具没有声明参数，直接执行即可"/>
                        ) : (
                            <Form form={form} layout="vertical" style={{maxWidth: 520}}>
                                {Object.entries(testTool?.inputSchema?.properties || {}).map(([name, prop]) => (
                                    <SchemaValueInput
                                        key={name}
                                        name={name}
                                        prop={prop}
                                        required={(testTool?.inputSchema?.required || []).includes(name)}
                                    />
                                ))}
                            </Form>
                        )}
                        <Button type="primary" icon={<PlayCircleOutlined/>} onClick={handleRunTool}
                                loading={toolRunning} style={{marginTop: 12, width: 120}}>执行</Button>
                    </div>
                    {/* Output */}
                    <div style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                        <Text type="secondary" style={{marginBottom: 4}}>结果:</Text>
                        <pre style={{
                            flex: 1, overflow: 'auto', background: '#f5f5f5', padding: 12, borderRadius: 4,
                            fontFamily: 'monospace', fontSize: 13, margin: 0, whiteSpace: 'pre-wrap'
                        }}>
              {toolResult == null ? '点击"执行"查看结果' : JSON.stringify(toolResult, null, 2)}
            </pre>
                    </div>
                </div>
            )}
        </div>
    )
}
