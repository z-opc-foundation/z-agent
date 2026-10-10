import {useEffect, useState} from 'react'
import {
    Button,
    Card,
    Col,
    Divider,
    Empty,
    Form,
    Input,
    InputNumber,
    message,
    Modal,
    Row,
    Select,
    Space,
    Spin,
    Tag,
    Tooltip
} from 'antd'
import {
    ApiOutlined,
    AudioOutlined,
    BranchesOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    GlobalOutlined,
    NodeIndexOutlined,
    PictureOutlined,
    PlusOutlined,
    ReloadOutlined,
    ThunderboltOutlined
} from '@ant-design/icons'
import request from '../../api'
import {PageHeader} from '@/common/components/ui'

const {confirm} = Modal

// 示例数据
const mockProviders = [
    {
        providerCode: 'ollama',
        providerName: 'Ollama',
        providerType: 'ollama',
        baseUrl: 'http://localhost:11434',
        enabled: 1,
        priority: 100
    },
    {
        providerCode: 'openai',
        providerName: 'OpenAI',
        providerType: 'openai',
        baseUrl: 'https://api.openai.com',
        enabled: 1,
        priority: 90
    },
    {
        providerCode: 'dashscope',
        providerName: '阿里通义',
        providerType: 'dashscope',
        baseUrl: 'https://dashscope.aliyuncs.com',
        enabled: 1,
        priority: 80
    },
]

const mockModels = [
    {
        id: 1,
        modelCode: 'qwen2.5:7b-instruct-q4_K_M',
        modelName: 'Qwen 2.5 7B',
        providerCode: 'ollama',
        modelType: 'chat',
        contextWindow: 32768,
        status: 'ACTIVE',
        description: '通义千问 2.5 轻量级文本生成模型',
        inputPrice: 0,
        outputPrice: 0
    },
    {
        id: 2,
        modelCode: 'qwen3:8b',
        modelName: 'Qwen3 8B',
        providerCode: 'ollama',
        modelType: 'chat',
        contextWindow: 40960,
        status: 'ACTIVE',
        description: '通义千问 3 最新一代模型',
        inputPrice: 0,
        outputPrice: 0
    },
    {
        id: 3,
        modelCode: 'gpt-4o',
        modelName: 'GPT-4o',
        providerCode: 'openai',
        modelType: 'chat',
        contextWindow: 128000,
        status: 'ACTIVE',
        description: 'OpenAI 旗舰多模态模型',
        inputPrice: 0.0025,
        outputPrice: 0.01
    },
    {
        id: 4,
        modelCode: 'gpt-4o-mini',
        modelName: 'GPT-4o Mini',
        providerCode: 'openai',
        modelType: 'chat',
        contextWindow: 128000,
        status: 'ACTIVE',
        description: '轻量快速，性价比高',
        inputPrice: 0.00015,
        outputPrice: 0.0006
    },
    {
        id: 5,
        modelCode: 'qwen-vl-max',
        modelName: 'Qwen VL Max',
        providerCode: 'dashscope',
        modelType: 'vision',
        contextWindow: 8192,
        status: 'ACTIVE',
        description: '通义千问视觉模型，支持图像理解',
        inputPrice: 0.002,
        outputPrice: 0.008
    },
    {
        id: 6,
        modelCode: 'qwen2-audio',
        modelName: 'Qwen2 Audio',
        providerCode: 'dashscope',
        modelType: 'audio',
        contextWindow: 8192,
        status: 'ACTIVE',
        description: '通义千问语音模型',
        inputPrice: 0.001,
        outputPrice: 0.004
    },
    {
        id: 7,
        modelCode: 'text-embedding-v3',
        modelName: 'Text Embedding V3',
        providerCode: 'openai',
        modelType: 'embedding',
        contextWindow: 8191,
        status: 'ACTIVE',
        description: 'OpenAI 文本嵌入模型',
        inputPrice: 0.00002,
        outputPrice: 0
    },
    {
        id: 8,
        modelCode: 'codestral',
        modelName: 'Codestral',
        providerCode: 'ollama',
        modelType: 'code',
        contextWindow: 32768,
        status: 'ACTIVE',
        description: '代码生成专用模型，支持多种编程语言',
        inputPrice: 0,
        outputPrice: 0
    },
    {
        id: 9,
        modelCode: 'dall-e-3',
        modelName: 'DALL·E 3',
        providerCode: 'openai',
        modelType: 'image',
        contextWindow: 0,
        status: 'ACTIVE',
        description: 'OpenAI 图像生成模型',
        inputPrice: 0.04,
        outputPrice: 0
    },
]

// ===== 能力分类定义（阿里云风格） =====
const CATEGORY_DEFS = [
    {key: 'chat', title: '文本生成', icon: <BranchesOutlined/>, color: '#1677ff', bg: '#e6f4ff', border: '#1677ff'},
    {key: 'code', title: '代码生成', icon: <ThunderboltOutlined/>, color: '#722ed1', bg: '#f9f0ff', border: '#722ed1'},
    {key: 'image', title: '图像生成', icon: <PictureOutlined/>, color: '#13c2c2', bg: '#e6fffb', border: '#13c2c2'},
    {key: 'vision', title: '视觉理解', icon: <EyeOutlined/>, color: '#fa8c16', bg: '#fff7e6', border: '#fa8c16'},
    {key: 'audio', title: '语音', icon: <AudioOutlined/>, color: '#eb2f96', bg: '#fff0f6', border: '#eb2f96'},
    {
        key: 'embedding',
        title: '嵌入/向量',
        icon: <NodeIndexOutlined/>,
        color: '#52c41a',
        bg: '#f6ffed',
        border: '#52c41a'
    },
]

// 标准供应商 baseUrl 自动填充
const STANDARD_BASE_URLS = {
    openai: 'https://api.openai.com',
    dashscope: 'https://dashscope.aliyuncs.com',
    azure: 'https://{resource}.openai.azure.com',
    anthropic: 'https://api.anthropic.com',
}

const providerTypeList = [
    {value: 'ollama', label: 'Ollama'},
    {value: 'openai', label: 'OpenAI'},
    {value: 'dashscope', label: '阿里通义'},
    {value: 'azure', label: 'Azure OpenAI'},
    {value: 'anthropic', label: 'Anthropic'},
    {value: 'custom', label: '自定义'},
]

const ModelManage = () => {
    const [models, setModels] = useState([])
    const [providers, setProviders] = useState([])
    const [loading, setLoading] = useState(false)
    const [selectedCategory, setSelectedCategory] = useState('chat')
    const [selectedProvider, setSelectedProvider] = useState(null)
    const [modalVisible, setModalVisible] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()
    const [providerModalVisible, setProviderModalVisible] = useState(false)
    const [editingProvider, setEditingProvider] = useState(null)
    const [providerForm] = Form.useForm()

    const fetchData = async () => {
        setLoading(true)
        try {
            const [mRes, pRes] = await Promise.all([
                request('/llm-center/model/list', {method: 'GET'}).catch(() => null),
                request('/llm-center/provider/list', {method: 'GET'}).catch(() => null),
            ])
            const modelsData = Array.isArray(mRes) ? mRes : (mRes?.data || [])
            const providersData = Array.isArray(pRes) ? pRes : (pRes?.data || [])
            if (modelsData.length === 0 && providersData.length === 0) {
                setProviders(mockProviders)
                setModels(mockModels)
            } else {
                setModels(modelsData)
                setProviders(providersData)
            }
        } catch (e) {
            setProviders(mockProviders)
            setModels(mockModels)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchData()
    }, [])

    // 各分类模型数
    const categoryCounts = {}
    CATEGORY_DEFS.forEach(c => {
        categoryCounts[c.key] = models.filter(m => m.modelType === c.key).length
    })

    // 筛选模型：按 modelType 精确匹配分类 + 供应商
    const filteredModels = models.filter(m => {
        const catMatch = !selectedCategory || m.modelType === selectedCategory
        const provMatch = !selectedProvider || m.providerCode === selectedProvider
        return catMatch && provMatch
    })

    const providerTypeColor = {
        ollama: 'blue', openai: 'green', dashscope: 'cyan', azure: 'purple', anthropic: 'orange', custom: 'default',
    }

    // ===== 模型 CRUD =====
    const handleAddModel = () => {
        setEditing(null)
        form.resetFields()
        form.setFieldsValue({status: 'ACTIVE', contextWindow: 4096})
        setModalVisible(true)
    }

    const handleEditModel = (record) => {
        setEditing(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDeleteModel = (record) => {
        confirm({
            title: '确认删除',
            content: `删除模型 ${record.modelName} ？`,
            onOk: async () => {
                if (models === mockModels) {
                    setModels(models.filter(m => m.id !== record.id))
                    message.success('删除成功')
                    return
                }
                await request('/llm-center/model/delete', {method: 'POST', data: {id: record.id}})
                message.success('删除成功')
                fetchData()
            }
        })
    }

    const handleModelSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (models === mockModels) {
                if (editing) {
                    setModels(models.map(m => m.id === editing.id ? {...m, ...values} : m))
                } else {
                    setModels([...models, {...values, id: Date.now()}])
                }
                message.success(editing ? '更新成功' : '创建成功')
                setModalVisible(false)
                return
            }
            await request(editing ? '/llm-center/model/update' : '/llm-center/model/create', {
                method: 'POST', data: values
            })
            message.success(editing ? '更新成功' : '创建成功')
            setModalVisible(false)
            fetchData()
        } catch (e) {
        }
    }

    // ===== 供应商 CRUD =====
    const handleAddProvider = () => {
        setEditingProvider(null)
        providerForm.resetFields()
        providerForm.setFieldsValue({status: 'ACTIVE'})
        setProviderModalVisible(true)
    }

    const handleEditProvider = (record) => {
        setEditingProvider(record)
        providerForm.setFieldsValue({
            ...record,
            apiKey: record.apiKey ? '••••••••' : '',
        })
        setProviderModalVisible(true)
    }

    const handleDeleteProvider = (record) => {
        confirm({
            title: '确认删除',
            content: `删除供应商 ${record.providerName} ？`,
            onOk: async () => {
                if (providers === mockProviders) {
                    setProviders(providers.filter(p => p.providerCode !== record.providerCode))
                    message.success('删除成功')
                    return
                }
                await request('/llm-center/provider/delete', {method: 'POST', data: {id: record.id}})
                message.success('删除成功')
                fetchData()
            }
        })
    }

    const handleProviderSubmit = async () => {
        try {
            const values = await providerForm.validateFields()
            if (providers === mockProviders) {
                if (editingProvider) {
                    setProviders(providers.map(p => p.providerCode === editingProvider.providerCode ? {...p, ...values} : p))
                } else {
                    setProviders([...providers, {...values}])
                }
                message.success(editingProvider ? '更新成功' : '创建成功')
                setProviderModalVisible(false)
                return
            }
            await request(editingProvider ? '/llm-center/provider/update' : '/llm-center/provider/create', {
                method: 'POST', data: values
            })
            message.success(editingProvider ? '更新成功' : '创建成功')
            setProviderModalVisible(false)
            fetchData()
        } catch (e) {
        }
    }

    // 阿里云风格分类树渲染
    const renderCategoryItem = (cat) => {
        const selected = selectedCategory === cat.key
        const count = categoryCounts[cat.key] || 0
        return (
            <div
                key={cat.key}
                onClick={() => setSelectedCategory(cat.key)}
                style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '10px 14px',
                    marginBottom: 2,
                    cursor: 'pointer',
                    borderRadius: 8,
                    borderLeft: selected ? `3px solid ${cat.color}` : '3px solid transparent',
                    background: selected ? cat.bg : 'transparent',
                    transition: 'all 0.2s',
                }}
                onMouseEnter={(e) => {
                    if (!selected) e.currentTarget.style.background = '#f5f5f5'
                }}
                onMouseLeave={(e) => {
                    if (!selected) e.currentTarget.style.background = 'transparent'
                }}
            >
                <div style={{
                    width: 32, height: 32, borderRadius: 8,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: selected ? cat.color : '#f0f0f0',
                    color: selected ? '#fff' : '#8c8c8c',
                    fontSize: 16,
                    flexShrink: 0,
                    transition: 'all 0.2s',
                }}>
                    {cat.icon}
                </div>
                <div style={{flex: 1, minWidth: 0}}>
                    <div style={{
                        fontSize: 13, fontWeight: selected ? 600 : 400,
                        color: selected ? '#262626' : '#595959',
                        lineHeight: 1.4,
                    }}>
                        {cat.title}
                    </div>
                </div>
                <span style={{
                    fontSize: 11, color: selected ? cat.color : '#bababa',
                    fontWeight: selected ? 600 : 400,
                    background: selected ? '#fff' : 'transparent',
                    padding: '1px 7px',
                    borderRadius: 10,
                }}>
          {count}
        </span>
            </div>
        )
    }

    const currentCat = CATEGORY_DEFS.find(c => c.key === selectedCategory) || CATEGORY_DEFS[0]

    return (
        <div>
            <PageHeader
                title="LLM 管理"
                subtitle="管理大语言模型供应商和模型配置"
            />
            <div style={{display: 'flex', gap: 16, height: 'calc(100vh - 260px)', padding: '0 16px 16px'}}>
                {/* 左侧：能力分类 — 阿里云风格 */}
                <Card
                    size="small"
                    style={{
                        width: 210,
                        overflow: 'auto',
                        flexShrink: 0,
                        border: 'none',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.06)'
                    }}
                    styles={{body: {padding: '8px 0'}}}
                >
                    <div style={{padding: '8px 14px 4px', fontSize: 12, color: '#8c8c8c', fontWeight: 500}}>
                        能力分类
                    </div>
                    {CATEGORY_DEFS.map(renderCategoryItem)}
                </Card>

                {/* 右侧：供应商标签 + 模型卡片 */}
                <div style={{flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', gap: 12}}>
                    {/* 供应商标签栏 */}
                    <Card size="small" style={{flexShrink: 0}} styles={{body: {padding: '10px 16px'}}}>
                        <div style={{display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap'}}>
                        <span
                            style={{fontSize: 12, color: '#8c8c8c', marginRight: 4, whiteSpace: 'nowrap'}}>供应商</span>
                            <Tag
                                style={{
                                    cursor: 'pointer',
                                    padding: '2px 10px',
                                    fontSize: 12,
                                    borderRadius: 16,
                                    margin: 0
                                }}
                                color={!selectedProvider ? 'blue' : 'default'}
                                onClick={() => setSelectedProvider(null)}
                            >全部</Tag>
                            {providers.map(p => (
                                <Tag
                                    key={p.providerCode}
                                    style={{
                                        cursor: 'pointer',
                                        padding: '2px 10px',
                                        fontSize: 12,
                                        borderRadius: 16,
                                        margin: 0
                                    }}
                                    color={selectedProvider === p.providerCode ? (providerTypeColor[p.providerType] || 'blue') : 'default'}
                                    onClick={() => setSelectedProvider(p.providerCode)}
                                >
                                    {p.providerName}
                                </Tag>
                            ))}
                            <div style={{flex: 1}}/>
                            <Button size="small" type="primary" ghost icon={<PlusOutlined/>}
                                    onClick={handleAddModel}>新增模型</Button>
                            <Button size="small" icon={<ApiOutlined/>} onClick={handleAddProvider}>新增供应商</Button>
                            <Button size="small" icon={<ReloadOutlined/>} onClick={fetchData}/>
                        </div>
                    </Card>

                    {/* 模型卡片 */}
                    <div style={{flex: 1, overflow: 'auto'}}>
                        {loading ? (
                            <div style={{textAlign: 'center', padding: 80}}><Spin size="large"/></div>
                        ) : filteredModels.length === 0 ? (
                            <div style={{textAlign: 'center', padding: 80}}><Empty description="暂无匹配的模型"/></div>
                        ) : (
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
                                gap: 12
                            }}>
                                {filteredModels.map(model => {
                                    const provider = providers.find(p => p.providerCode === model.providerCode)
                                    const catDef = CATEGORY_DEFS.find(c => c.key === model.modelType)
                                    return (
                                        <Card
                                            key={model.id || model.modelCode}
                                            size="small"
                                            hoverable
                                            style={{
                                                borderRadius: 8,
                                                borderTop: `3px solid ${catDef?.color || '#d9d9d9'}`
                                            }}
                                            styles={{body: {padding: 14}}}
                                            actions={[
                                                <Tooltip key="edit" title="编辑模型"><EditOutlined
                                                    onClick={() => handleEditModel(model)}/></Tooltip>,
                                                <Tooltip key="delete" title="删除模型"><DeleteOutlined
                                                    style={{color: '#ff4d4f'}}
                                                    onClick={() => handleDeleteModel(model)}/></Tooltip>,
                                            ]}
                                        >
                                            <div style={{display: 'flex', gap: 12}}>
                                                <div style={{
                                                    width: 44,
                                                    height: 44,
                                                    borderRadius: 10,
                                                    background: catDef ? `linear-gradient(135deg, ${catDef.color}, ${catDef.color}dd)` : '#f0f0f0',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    flexShrink: 0,
                                                }}>
                                                    {catDef ? (
                                                        <span style={{color: '#fff', fontSize: 20}}>{catDef.icon}</span>
                                                    ) : (
                                                        <GlobalOutlined style={{fontSize: 20, color: '#fff'}}/>
                                                    )}
                                                </div>
                                                <div style={{flex: 1, minWidth: 0}}>
                                                    <div style={{
                                                        display: 'flex',
                                                        justifyContent: 'space-between',
                                                        alignItems: 'flex-start'
                                                    }}>
                                                        <div>
                                                            <div style={{
                                                                fontWeight: 600,
                                                                fontSize: 14,
                                                                color: '#262626'
                                                            }}>{model.modelName || model.modelCode}</div>
                                                            <div style={{
                                                                fontSize: 12,
                                                                color: '#8c8c8c',
                                                                fontFamily: 'monospace'
                                                            }}>{model.modelCode}</div>
                                                        </div>
                                                        <Tag color={model.status === 'ACTIVE' ? 'green' : 'red'}
                                                             style={{
                                                                 borderRadius: 20,
                                                                 fontSize: 11,
                                                                 lineHeight: '20px',
                                                                 margin: 0
                                                             }}>
                                                            {model.status === 'ACTIVE' ? '正常' : '禁用'}
                                                        </Tag>
                                                    </div>
                                                    <div style={{
                                                        marginTop: 8,
                                                        display: 'flex',
                                                        gap: 6,
                                                        flexWrap: 'wrap'
                                                    }}>
                                                        <Tag
                                                            color={providerTypeColor[provider?.providerType] || 'default'}
                                                            style={{fontSize: 11, borderRadius: 4}}>
                                                            <GlobalOutlined style={{marginRight: 4}}/>
                                                            {provider?.providerName || model.providerCode}
                                                        </Tag>
                                                        <Tag style={{fontSize: 11, borderRadius: 4}}>
                                                            {CATEGORY_DEFS.find(c => c.key === model.modelType)?.title || model.modelType}
                                                        </Tag>
                                                        {model.contextWindow > 0 && (
                                                            <Tag style={{fontSize: 11, borderRadius: 4}}>
                                                                {(model.contextWindow / 1000).toFixed(0)}K CTX
                                                            </Tag>
                                                        )}
                                                    </div>
                                                    {model.description && (
                                                        <div style={{
                                                            marginTop: 6,
                                                            fontSize: 12,
                                                            color: '#8c8c8c',
                                                            lineHeight: 1.5
                                                        }}>
                                                            {model.description}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </Card>
                                    )
                                })}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 模型编辑弹窗 */}
            <Modal title={editing ? '编辑模型' : '新增模型'} open={modalVisible}
                   onOk={handleModelSubmit} onCancel={() => setModalVisible(false)} size="large">
                <Form form={form} layout="vertical">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="modelCode" label="模型编码" rules={[{required: true}]}>
                                <Input placeholder="如: qwen2.5:72b, gpt-4o" disabled={!!editing}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="modelName" label="模型名称" rules={[{required: true}]}>
                                <Input placeholder="如: Qwen 2.5 72B"/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="providerCode" label="所属供应商" rules={[{required: true}]}>
                                <Select placeholder="选择供应商">
                                    {providers.map(p => (
                                        <Select.Option key={p.providerCode}
                                                       value={p.providerCode}>{p.providerName}</Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="modelType" label="能力类型" rules={[{required: true}]}>
                                <Select placeholder="选择能力分类">
                                    {CATEGORY_DEFS.map(c => (
                                        <Select.Option key={c.key} value={c.key}>
                                            <Space><span style={{color: c.color}}>{c.icon}</span>{c.title}</Space>
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="contextWindow" label="上下文窗口 (tokens)">
                                <InputNumber style={{width: '100%'}} placeholder="如: 4096" min={0}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="status" label="状态" initialValue="ACTIVE">
                                <Select>
                                    <Select.Option value="ACTIVE">正常</Select.Option>
                                    <Select.Option value="INACTIVE">禁用</Select.Option>
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="inputPrice" label="输入价格 (元/千token)">
                                <InputNumber style={{width: '100%'}} min={0} precision={6}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="outputPrice" label="输出价格 (元/千token)">
                                <InputNumber style={{width: '100%'}} min={0} precision={6}/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="模型描述..."/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 供应商编辑弹窗 */}
            <Modal title={editingProvider ? '编辑供应商' : '新增供应商'} open={providerModalVisible}
                   onOk={handleProviderSubmit} onCancel={() => setProviderModalVisible(false)} size="large">
                <Form form={providerForm} layout="vertical">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="providerCode" label="供应商编码" rules={[{required: true}]}>
                                <Input placeholder="如: ollama, openai" disabled={!!editingProvider}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="providerName" label="供应商名称" rules={[{required: true}]}>
                                <Input placeholder="如: Ollama, OpenAI"/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="providerType" label="类型" rules={[{required: true}]}>
                                <Select placeholder="选择类型">
                                    {providerTypeList.map(t => (
                                        <Select.Option key={t.value} value={t.value}>{t.label}</Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="status" label="状态" initialValue="ACTIVE">
                                <Select>
                                    <Select.Option value="ACTIVE">正常</Select.Option>
                                    <Select.Option value="INACTIVE">禁用</Select.Option>
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Divider style={{margin: '8px 0', fontSize: 12, color: '#8c8c8c'}}>连接配置</Divider>

                    <Form.Item
                        name="baseUrl"
                        label="Base URL"
                        rules={[{required: true}]}
                        tooldescription="标准供应商自动填充地址；自定义/Ollama 需手动填写"
                    >
                        <Input placeholder="如: http://localhost:11434"/>
                    </Form.Item>
                    <Form.Item
                        name="apiKey"
                        label="API Key"
                        tooldescription="系统级凭据，可在系统配置中统一管理。此字段仅用于展示，实际运行时从环境变量读取"
                    >
                        <Input.Password placeholder="可选，建议通过系统配置管理"/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default ModelManage
