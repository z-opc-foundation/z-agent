import {useEffect, useState} from 'react'
import {
    Button,
    Card,
    Col,
    Descriptions,
    Drawer,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Row,
    Select,
    Space,
    Statistic,
    Table,
    Tag,
    Tooltip,
    Typography
} from 'antd'
import {
    CloudDownloadOutlined,
    CloudServerOutlined,
    CloudUploadOutlined,
    CodeOutlined,
    DeleteOutlined,
    EditOutlined,
    ImportOutlined,
    PlayCircleOutlined,
    PlusOutlined
} from '@ant-design/icons'
import {scriptApi} from '../../api'
import CurlImportModal from './CurlImportModal'
import OpenApiImportModal from './OpenApiImportModal'
import ApiBridgeEditor from './ApiBridgeEditor'
import {PageHeader} from '@/common/components/ui'

const {TextArea} = Input
const {Text, Paragraph} = Typography

const DSL_TYPES = [
    {value: 'EL', label: 'EL (SpEL)'},
    {value: 'GROOVY', label: 'Groovy'},
    {value: 'LUA', label: 'Lua'},
    {value: 'SQL', label: 'SQL'},
    {value: 'MOCK', label: 'Mock Template'},
    {value: 'API_BRIDGE', label: 'API Bridge'},
]

const EXPOSE_OPTIONS = [
    {value: 'NONE', label: 'None'},
    {value: 'HTTP', label: 'HTTP'},
    {value: 'MCP', label: 'MCP'},
    {value: 'BOTH', label: 'HTTP + MCP'},
]

const STATUS_MAP = {
    0: {text: 'Draft', color: 'default'},
    1: {text: 'Active', color: 'green'},
    2: {text: 'Disabled', color: 'red'},
}

const EXPOSE_COLOR = {
    NONE: 'default',
    HTTP: 'blue',
    MCP: 'purple',
    BOTH: 'cyan',
}

export default function ScriptCenterPage() {
    const [scripts, setScripts] = useState([])
    const [loading, setLoading] = useState(false)
    const [loadError, setLoadError] = useState(null)
    const [modalOpen, setModalOpen] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()
    const [runDrawerOpen, setRunDrawerOpen] = useState(false)
    const [runScript, setRunScript] = useState(null)
    const [runParams, setRunParams] = useState('{}')
    const [runResult, setRunResult] = useState('')
    const [runLoading, setRunLoading] = useState(false)
    const [curlModalOpen, setCurlModalOpen] = useState(false)
    const [openApiModalOpen, setOpenApiModalOpen] = useState(false)
    const [bridgeSourceCode, setBridgeSourceCode] = useState(null)
    const watchedDslType = Form.useWatch('dslType', form)
    const isApiBridge = watchedDslType === 'API_BRIDGE'

    useEffect(() => {
        loadScripts()
    }, [])

    const loadScripts = async () => {
        setLoading(true)
        try {
            const res = await scriptApi.list()
            setScripts(Array.isArray(res) ? res : (res?.data || res || []))
            setLoadError(null)
        } catch (e) {
            // 这里原来是 `setScripts(MOCK_SCRIPTS)` —— 6 条手写脚本（含"Mock Users API"、"总脚本数 6"的统计卡）
            // 在后端不可达时把假数据渲染成真实台账。数据源 /api/script/list 被 z-script 自己的
            // ApiKeyAuthInterceptor 拦着（带平台会话仍 401），所以每次进这页走的都是假数据分支。
            // 见 TASK-20260925-032：宁空表+说明，不接假数据。
            setScripts([])
            setLoadError(e?.message || 'HTTP 请求失败')
        } finally {
            setLoading(false)
        }
    }

    const handleCreate = () => {
        setEditing(null)
        form.resetFields()
        form.setFieldsValue({dslType: 'EL', exposeAs: 'NONE', status: 0})
        setBridgeSourceCode(null)
        setModalOpen(true)
    }

    const handleDslTypeChange = (val) => {
        if (val === 'API_BRIDGE') {
            // Initialize empty bridge definition
            setBridgeSourceCode(JSON.stringify({
                request: {
                    httpRequestLine: {url: '', requestMethod: 'GET'},
                    httpRequestHeader: {headers: {}},
                    httpRequestBody: {body: null}
                },
                inputParams: [],
                outputMapping: [],
            }))
        } else {
            setBridgeSourceCode(null)
        }
    }

    const handleEdit = (record) => {
        setEditing(record)
        form.setFieldsValue(record)
        if (record.dslType === 'API_BRIDGE') {
            setBridgeSourceCode(record.sourceCode)
        } else {
            setBridgeSourceCode(null)
        }
        setModalOpen(true)
    }

    const handleSave = async () => {
        try {
            const values = await form.validateFields()
            // For API_BRIDGE, serialize the structured editor back to sourceCode
            if (values.dslType === 'API_BRIDGE' && bridgeSourceCode != null) {
                values.sourceCode = bridgeSourceCode
            }
            if (editing) {
                await scriptApi.update({data: values, params: {scriptCode: editing.scriptCode}})
                message.success('更新成功')
            } else {
                await scriptApi.create(values)
                message.success('创建成功')
            }
            setModalOpen(false)
            setBridgeSourceCode(null)
            loadScripts()
        } catch (e) {
            if (e?.errorFields) return
            message.error('保存失败: ' + (e?.message || '未知错误'))
        }
    }

    const handleDelete = async (scriptCode) => {
        try {
            await scriptApi.delete({params: {scriptCode}})
            message.success('删除成功')
            loadScripts()
        } catch (e) {
            message.error('删除失败')
        }
    }

    const handleRun = (record) => {
        setRunScript(record)
        setRunParams('{}')
        setRunResult('')
        setRunDrawerOpen(true)
    }

    const executeRun = async () => {
        if (!runScript) return
        setRunLoading(true)
        try {
            const params = JSON.parse(runParams)
            const res = await scriptApi.run(runScript.scriptCode, params)
            setRunResult(JSON.stringify(res, null, 2))
        } catch (e) {
            setRunResult('Error: ' + (e?.message || '执行失败'))
        } finally {
            setRunLoading(false)
        }
    }

    const handlePublish = async (record, exposeAs) => {
        try {
            await scriptApi.publish(record.scriptCode, exposeAs || 'HTTP')
            message.success('发布成功')
            loadScripts()
        } catch (e) {
            message.error('发布失败')
        }
    }

    const handleUnpublish = async (record) => {
        try {
            await scriptApi.unpublish(record.scriptCode)
            message.success('已取消发布')
            loadScripts()
        } catch (e) {
            message.error('操作失败')
        }
    }

    const columns = [
        {
            title: '名称', dataIndex: 'scriptName', key: 'scriptName', width: 180,
            render: (text, r) => <Space><CodeOutlined/><Text strong>{text}</Text></Space>
        },
        {
            title: '编码', dataIndex: 'scriptCode', key: 'scriptCode', width: 150,
            render: (text) => <Text code copyable={{text}}>{text}</Text>
        },
        {
            title: 'DSL类型', dataIndex: 'dslType', key: 'dslType', width: 120,
            render: (v) => <Tag>{v}</Tag>
        },
        {
            title: '暴露方式', dataIndex: 'exposeAs', key: 'exposeAs', width: 120,
            render: (v) => <Tag color={EXPOSE_COLOR[v] || 'default'}>{v}</Tag>
        },
        {
            title: '状态', dataIndex: 'status', key: 'status', width: 100,
            render: (v) => {
                const s = STATUS_MAP[v] || STATUS_MAP[0];
                return <Tag color={s.color}>{s.text}</Tag>
            }
        },
        {title: '版本', dataIndex: 'version', key: 'version', width: 80},
        {
            title: 'HTTP路径', dataIndex: 'httpPath', key: 'httpPath', width: 180,
            render: (v) => v ? <Text type="secondary" copyable={{text: v}}>{v}</Text> : '-'
        },
        {
            title: 'MCP工具名', dataIndex: 'mcpToolName', key: 'mcpToolName', width: 160,
            render: (v) => v ? <Tag color="purple">{v}</Tag> : '-'
        },
        {
            title: '操作', key: 'action', width: 260, fixed: 'right',
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="运行测试">
                        <Button size="small" type="link" icon={<PlayCircleOutlined/>}
                                onClick={() => handleRun(record)}>运行</Button>
                    </Tooltip>
                    <Tooltip title="编辑">
                        <Button size="small" type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)}/>
                    </Tooltip>
                    {record.exposeAs === 'NONE' ? (
                        <Tooltip title="发布为HTTP">
                            <Button size="small" type="link" icon={<CloudUploadOutlined/>}
                                    onClick={() => handlePublish(record, 'HTTP')}>发布</Button>
                        </Tooltip>
                    ) : (
                        <Tooltip title="取消发布">
                            <Button size="small" type="link" icon={<CloudDownloadOutlined/>}
                                    onClick={() => handleUnpublish(record)}>取消</Button>
                        </Tooltip>
                    )}
                    <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.scriptCode)}>
                        <Button size="small" type="link" danger icon={<DeleteOutlined/>}/>
                    </Popconfirm>
                </Space>
            )
        },
    ]

    return (
        <div>
            {loadError && (
                <div style={{
                    padding: '8px 16px',
                    marginBottom: 16,
                    background: '#fff1f0',
                    border: '1px solid #ffa39e',
                    borderRadius: 4
                }}>
                    <Tag color="red">数据源不可达</Tag> {String(loadError)} —— 本表不接假数据，
                    修复见 TASK-20260925-032
                </div>
            )}

            <PageHeader
                title="脚本中心"
                subtitle="创建和管理 DSL 脚本，支持 EL/Groovy/Lua/SQL/Mock/API Bridge"
            />

            <Card extra={
                <Space>
                    <Button icon={<ImportOutlined/>} onClick={() => setCurlModalOpen(true)}>导入 Curl</Button>
                    <Button icon={<CloudServerOutlined/>} onClick={() => setOpenApiModalOpen(true)}>导入
                        OpenAPI</Button>
                    <Button icon={<PlayCircleOutlined/>} onClick={loadScripts}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新建脚本</Button>
                </Space>
            }>
                <Row gutter={16} style={{marginBottom: 16}}>
                    <Col span={6}><Statistic title="总脚本数" value={scripts.length}/></Col>
                    <Col span={6}><Statistic title="已发布" value={scripts.filter(s => s.exposeAs !== 'NONE').length}/></Col>
                    <Col span={6}><Statistic title="HTTP"
                                             value={scripts.filter(s => s.exposeAs === 'HTTP' || s.exposeAs === 'BOTH').length}/></Col>
                    <Col span={6}><Statistic title="MCP"
                                             value={scripts.filter(s => s.exposeAs === 'MCP' || s.exposeAs === 'BOTH').length}/></Col>
                </Row>

                <Table
                    dataSource={scripts}
                    columns={columns}
                    rowKey="id"
                    loading={loading}
                    pagination={{pageSize: 10}}
                    scroll={{x: 1300}}
                    size="small"
                />
            </Card>

            {/* Create/Edit Modal */}
            <Modal
                title={editing ? '编辑脚本' : '新建脚本'}
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                onOk={handleSave}
                size="large"
                okText="保存"
                cancelText="取消"
            >
                <Form form={form} layout="vertical" style={{marginTop: 16}}>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="scriptName" label="脚本名称"
                                       rules={[{required: true, message: '请输入名称'}]}>
                                <Input placeholder="e.g. Hello World"/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="scriptCode" label="脚本编码"
                                       rules={[{required: true, message: '请输入编码'}]}>
                                <Input placeholder="e.g. hello-world" disabled={!!editing}/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={8}>
                            <Form.Item name="dslType" label="DSL类型">
                                <Select options={DSL_TYPES} onChange={handleDslTypeChange}/>
                            </Form.Item>
                        </Col>
                        <Col span={8}>
                            <Form.Item name="exposeAs" label="暴露方式">
                                <Select options={EXPOSE_OPTIONS}/>
                            </Form.Item>
                        </Col>
                        <Col span={8}>
                            <Form.Item name="version" label="版本">
                                <Input placeholder="1.0.0"/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="description" label="描述">
                        <Input placeholder="脚本描述"/>
                    </Form.Item>
                    {isApiBridge ? (
                        <Form.Item label="API Bridge 配置">
                            <ApiBridgeEditor
                                value={bridgeSourceCode}
                                onChange={setBridgeSourceCode}
                            />
                        </Form.Item>
                    ) : (
                        <>
                            <Form.Item name="inputSchema" label="输入 Schema (JSON)">
                                <TextArea rows={3}
                                          placeholder='{"type": "object", "properties": {"name": {"type": "string"}}}'/>
                            </Form.Item>
                            <Form.Item name="sourceCode" label="源代码"
                                       rules={[{required: true, message: '请输入源代码'}]}>
                                <TextArea
                                    rows={12}
                                    placeholder="输入DSL脚本代码..."
                                    style={{fontFamily: 'monospace', fontSize: 13}}
                                />
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Modal>

            {/* Run Test Drawer */}
            <Drawer
                title={<Space><PlayCircleOutlined/>运行测试: {runScript?.scriptName}</Space>}
                open={runDrawerOpen}
                onClose={() => setRunDrawerOpen(false)}
                size="large"
            >
                {runScript && (
                    <div>
                        <Descriptions column={1} size="small" bordered style={{marginBottom: 16}}>
                            <Descriptions.Item label="编码">{runScript.scriptCode}</Descriptions.Item>
                            <Descriptions.Item label="DSL类型">{runScript.dslType}</Descriptions.Item>
                            <Descriptions.Item label="暴露方式">{runScript.exposeAs}</Descriptions.Item>
                        </Descriptions>

                        <Paragraph strong>输入参数 (JSON):</Paragraph>
                        <TextArea
                            rows={5}
                            value={runParams}
                            onChange={(e) => setRunParams(e.target.value)}
                            style={{fontFamily: 'monospace', fontSize: 13, marginBottom: 12}}
                        />

                        <Button
                            type="primary"
                            icon={<PlayCircleOutlined/>}
                            loading={runLoading}
                            onClick={executeRun}
                            block
                        >
                            执行
                        </Button>

                        {runResult && (
                            <div style={{marginTop: 16}}>
                                <Paragraph strong>执行结果:</Paragraph>
                                <pre style={{
                                    background: '#f5f5f5',
                                    padding: 12,
                                    borderRadius: 4,
                                    maxHeight: 400,
                                    overflow: 'auto',
                                    fontSize: 13,
                                    fontFamily: 'monospace',
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-all',
                                }}>
                  {runResult}
                </pre>
                            </div>
                        )}
                    </div>
                )}
            </Drawer>

            {/* 导入 Curl */}
            <CurlImportModal
                open={curlModalOpen}
                onClose={() => setCurlModalOpen(false)}
                onSuccess={loadScripts}
            />

            {/* 导入 OpenAPI */}
            <OpenApiImportModal
                open={openApiModalOpen}
                onClose={() => setOpenApiModalOpen(false)}
                onSuccess={loadScripts}
            />
        </div>
    )
}
