import {useEffect, useState} from 'react'
import {
    Button,
    Card,
    Input,
    message,
    Space,
    Table,
    Tabs,
    Tag,
    Typography
} from 'antd'
import {
    ApiOutlined,
    PlayCircleOutlined,
    ReloadOutlined
} from '@ant-design/icons'
import {mcpApi} from '../../api'
import {EmptyState, PageHeader} from '@/common/components/ui'

const {TextArea} = Input
const {Text} = Typography

/**
 * MCP 状态视图。
 *
 * <p>上游由配置项 z.mcp.servers[*] 声明，z-mcp 没有 server 表，所以本页只读：
 * 列表来自 ExternalServerManager.state() + 本机注册表，工具来自 McpRegistry.listTools()，
 * 调用走 McpRegistry.call()。新增/编辑/删除/测试连接这四个写动作随 z-agent-mcp 一族退场。</p>
 */
export default function McpPage() {
    const [activeTab, setActiveTab] = useState('servers')
    const [servers, setServers] = useState([])
    const [loading, setLoading] = useState(false)

    // Tool test state
    const [selectedServer, setSelectedServer] = useState(null)
    const [tools, setTools] = useState([])
    const [selectedTool, setSelectedTool] = useState(null)
    const [toolArgs, setToolArgs] = useState('{}')
    const [toolResult, setToolResult] = useState('')
    const [toolRunning, setToolRunning] = useState(false)

    useEffect(() => {
        loadServers()
    }, [])

    const loadServers = async () => {
        setLoading(true)
        try {
            const res = await mcpApi.list()
            setServers(Array.isArray(res) ? res : (res?.data || res || []))
        } catch (e) {
            console.warn('MCP API 不可用:', e?.message)
            setServers([])
        } finally {
            setLoading(false)
        }
    }

    const handleSelectServer = async (record) => {
        setSelectedServer(record)
        setSelectedTool(null)
        setToolResult('')
        try {
            const res = await mcpApi.listTools({serverName: record.serverName})
            const data = res?.tools || res?.data?.tools || []
            setTools(Array.isArray(data) ? data : [])
        } catch (e) {
            setTools([])
            message.error('获取工具列表失败')
        }
    }

    const handleSelectTool = (tool) => {
        setSelectedTool(tool)
        // Build default args from schema
        const schema = tool.inputSchema
        if (schema?.properties) {
            const defaults = {}
            Object.keys(schema.properties).forEach(k => {
                defaults[k] = ''
            })
            setToolArgs(JSON.stringify(defaults, null, 2))
        } else {
            setToolArgs('{}')
        }
        setToolResult('')
    }

    const handleRunTool = async () => {
        if (!selectedTool) return
        setToolRunning(true)
        setToolResult('Running...')
        try {
            let args = {}
            try {
                args = JSON.parse(toolArgs)
            } catch (e) {
            }
            const res = await mcpApi.callTool({toolName: selectedTool.name, arguments: args})
            const data = res?.data || res
            if (data?.content && data.content.length > 0) {
                setToolResult(data.content[0].text || JSON.stringify(data.content, null, 2))
            } else {
                setToolResult(JSON.stringify(data, null, 2))
            }
        } catch (e) {
            setToolResult('Error: ' + e.message)
        } finally {
            setToolRunning(false)
        }
    }

    // ===== Server columns =====
    const columns = [
        {title: '名称', dataIndex: 'serverName', key: 'serverName', width: 160},
        {
            title: '类型', dataIndex: 'transportType', key: 'transportType', width: 80,
            render: (v) => <Tag color={v === 'LOCAL' ? 'green' : 'blue'}>{v || 'HTTP'}</Tag>
        },
        {title: 'URL', dataIndex: 'url', key: 'url', ellipsis: true},
        {
            title: '状态', dataIndex: 'status', key: 'status', width: 80,
            render: (v) => <Tag color={v === 'active' ? 'green' : 'default'}>{v}</Tag>
        },
        {title: '工具', dataIndex: 'toolCount', key: 'toolCount', width: 60},
        {
            title: '操作', key: 'actions', width: 90, render: (_, r) => (
                <Space size="small">
                    <Button size="small" icon={<ApiOutlined/>} onClick={() => handleSelectServer(r)}>工具</Button>
                </Space>
            )
        },
    ]

    return (
        <div style={{height: 'calc(100vh - 180px)', display: 'flex', flexDirection: 'column'}}>
            <PageHeader
                title="MCP 服务管理"
                subtitle="上游由配置项 z.mcp.servers 声明，本页只读：浏览注册表工具并试调用"
            />
            {/* 内容在 Tabs 之外按 activeTab 分支渲染，所以 items 只给 label，不带 children */}
            <Tabs activeKey={activeTab} onChange={setActiveTab} tabBarExtraContent={
                <Button icon={<ReloadOutlined/>} onClick={loadServers}>刷新</Button>
            } items={[
                {key: 'servers', label: '服务管理'},
                {key: 'test', label: '工具测试', disabled: !selectedServer},
            ]}/>

            {activeTab === 'servers' && (
                <div style={{flex: 1, display: 'flex', gap: 16, overflow: 'hidden'}}>
                    {/* Server List */}
                    <div style={{flex: selectedServer ? '0 0 50%' : 1, overflow: 'auto'}}>
                        {servers.length === 0 && !loading ? (
                            <EmptyState title="暂无 MCP server"
                                        description="z.mcp.enabled=false，或还没配 z.mcp.servers[*] 上游"/>
                        ) : (
                            <Table columns={columns} dataSource={servers} rowKey="serverName" loading={loading}
                                   size="small" pagination={false}
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
                            flex: '0 0 50%',
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
                                    <Card key={t.name} size="small" hoverable
                                          style={{
                                              marginBottom: 8,
                                              borderColor: selectedTool?.name === t.name ? '#1890ff' : undefined
                                          }}
                                          onClick={() => {
                                              handleSelectTool(t);
                                              setActiveTab('test')
                                          }}>
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center'
                                        }}>
                                            <div>
                                                <Text strong>{t.name}</Text>
                                                <br/><Text type="secondary"
                                                           style={{fontSize: 12}}>{t.description}</Text>
                                            </div>
                                            <Tag color="blue" style={{cursor: 'pointer'}}
                                                 onClick={(e) => {
                                                     e.stopPropagation();
                                                     handleSelectTool(t);
                                                     setActiveTab('test')
                                                 }}>
                                                测试 →
                                            </Tag>
                                        </div>
                                    </Card>
                                ))
                            )}
                        </div>
                    )}
                </div>
            )}

            {activeTab === 'test' && selectedTool && (
                <div style={{flex: 1, display: 'flex', gap: 16, overflow: 'hidden'}}>
                    {/* Input */}
                    <div style={{flex: '0 0 40%', display: 'flex', flexDirection: 'column'}}>
                        <div style={{marginBottom: 8}}>
                            <Text strong>{selectedTool.name}</Text>
                            <Text type="secondary" style={{marginLeft: 8}}>{selectedTool.description}</Text>
                        </div>
                        <Text type="secondary" style={{marginBottom: 4}}>参数 (JSON):</Text>
                        <TextArea rows={8} value={toolArgs} onChange={e => setToolArgs(e.target.value)}
                                  style={{fontFamily: 'monospace', fontSize: 13}}/>
                        <Button type="primary" icon={<PlayCircleOutlined/>} onClick={handleRunTool}
                                loading={toolRunning} style={{marginTop: 12}}>执行</Button>
                    </div>
                    {/* Output */}
                    <div style={{flex: '0 0 60%', display: 'flex', flexDirection: 'column'}}>
                        <Text type="secondary" style={{marginBottom: 4}}>结果:</Text>
                        <pre style={{
                            flex: 1, overflow: 'auto', background: '#f5f5f5', padding: 12, borderRadius: 4,
                            fontFamily: 'monospace', fontSize: 13, margin: 0, whiteSpace: 'pre-wrap'
                        }}>
              {toolResult || '点击"执行"查看结果'}
            </pre>
                    </div>
                </div>
            )}
        </div>
    )
}
