import {useEffect, useState} from 'react'
import {
    Badge,
    Button,
    Card,
    Col,
    Divider,
    Drawer,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Row,
    Select,
    Space,
    Switch,
    Table,
    Tabs,
    Tag,
    Tooltip
} from 'antd'
import {
    ApiOutlined,
    CheckCircleOutlined,
    CopyOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    NodeIndexOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    SettingOutlined,
    StopOutlined,
    ThunderboltOutlined
} from '@ant-design/icons'
import {productApi, sceneApi} from '@/agent/api'
import {SearchInput} from '@/common/components/ui'

const {TextArea} = Input
const {Option} = Select
const {TabPane} = Tabs

// ===== 场景管理页 =====
const SceneManagementPage = () => {
    const [activeTab, setActiveTab] = useState('list')
    return (
        <Card style={{minHeight: 'calc(100vh - 140px)'}}>
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
                <TabPane tab={<span><NodeIndexOutlined/> 场景管理</span>} key="list">
                    <SceneTablePage/>
                </TabPane>
                <TabPane tab={<span><SettingOutlined/> 节点配置</span>} key="nodes">
                    <NodeConfigPage/>
                </TabPane>
                <TabPane tab={<span><ApiOutlined/> 场景编排</span>} key="canvas">
                    <SceneCanvasPage/>
                </TabPane>
            </Tabs>
        </Card>
    )
}

// ===== 场景列表 =====
const SceneTablePage = () => {
    const [dataSource, setDataSource] = useState([])
    const [loading, setLoading] = useState(false)
    const [searchKey, setSearchKey] = useState('')
    const [statusFilter, setStatusFilter] = useState('')
    const [pageNum, setPageNum] = useState(1)
    const [pageSize] = useState(12)
    const [total, setTotal] = useState(0)
    const [modalVisible, setModalVisible] = useState(false)
    const [form] = Form.useForm()
    const [editingScene, setEditingScene] = useState(null)
    const [createLoading, setCreateLoading] = useState(false)
    const [detailVisible, setDetailVisible] = useState(false)
    const [detailScene, setDetailScene] = useState(null)
    const [productOptions, setProductOptions] = useState([])

    useEffect(() => {
        productApi.page({id: 1, pageSize: 100}).then(res => {
            setProductOptions(res.data?.records || [])
        })
    }, [])

    useEffect(() => {
        fetchData()
    }, [pageNum, searchKey, statusFilter])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await sceneApi.page({sceneName: searchKey, status: statusFilter, id: pageNum})
            if (res.data) {
                setDataSource(res.data.records || [])
                setTotal(res.data.total || 0)
            }
        } catch (e) {
            message.error('加载失败: ' + (e.message || ''))
        }
        setLoading(false)
    }

    const handleCreate = () => {
        setEditingScene(null)
        form.resetFields()
        form.setFieldsValue({status: 'DRAFT', sceneType: 'CONVERSATION'})
        setModalVisible(true)
    }

    const handleEdit = (record) => {
        setEditingScene(record)
        form.setFieldsValue({
            sceneCode: record.sceneCode,
            sceneName: record.sceneName,
            description: record.description,
            productCode: record.productCode,
            sceneType: record.sceneType || 'CONVERSATION',
            version: record.version || '1.0.0',
            status: record.status,
            iconUrl: record.iconUrl,
        })
        setModalVisible(true)
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            setCreateLoading(true)
            if (editingScene) {
                await sceneApi.update({data: values, params: {id: editingScene.id}})
                message.success('更新成功')
            } else {
                await sceneApi.create(values)
                message.success('创建成功')
            }
            setModalVisible(false)
            fetchData()
        } catch (e) {
            if (!e.errorFields) message.error('操作失败')
        } finally {
            setCreateLoading(false)
        }
    }

    const handleDelete = async (id) => {
        try {
            await sceneApi.delete({params: {id}})
            message.success('删除成功')
            fetchData()
        } catch (e) {
            message.error('删除失败')
        }
    }

    const handlePublish = async (sceneCode) => {
        try {
            await sceneApi.publish({params: {sceneCode}})
            message.success('发布成功')
            fetchData()
        } catch (e) {
            message.error('发布失败')
        }
    }

    const handleOffline = async (sceneCode) => {
        try {
            await sceneApi.offline({params: {sceneCode}})
            message.success('下架成功')
            fetchData()
        } catch (e) {
            message.error('下架失败')
        }
    }

    const handleViewDetail = (record) => {
        setDetailScene(record)
        setDetailVisible(true)
    }

    const handleDuplicate = async (record) => {
        try {
            await sceneApi.duplicate({params: {id: record.id}})
            message.success('复制成功')
            fetchData()
        } catch (e) {
            message.error('复制失败')
        }
    }

    const copySceneCode = (code) => {
        navigator.clipboard.writeText(code).then(() => message.success('场景编码已复制')).catch(() => {
        })
    }

    const columns = [
        {
            title: '场景',
            key: 'scene',
            width: 280,
            render: (_, record) => (
                <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <div style={{
                        width: 40, height: 40, borderRadius: 8,
                        background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center'
                    }}>
                        <NodeIndexOutlined style={{fontSize: 18, color: '#fff'}}/>
                    </div>
                    <div>
                        <div style={{fontWeight: 500}}>{record.sceneName}</div>
                        <div style={{fontSize: 12, color: '#999'}}>
                            <span style={{fontFamily: 'monospace'}}>{record.sceneCode}</span>
                            <Tooltip title="复制编码">
                                <Button size="small" type="text" icon={<CopyOutlined/>}
                                        onClick={() => copySceneCode(record.sceneCode)}
                                        style={{marginLeft: 4, padding: 0}}/>
                            </Tooltip>
                        </div>
                    </div>
                </div>
            )
        },
        {
            title: '所属产品',
            dataIndex: 'productCode',
            width: 160,
            render: (val) => val ? <Tag color="blue">{val}</Tag> : <span style={{color: '#ccc'}}>未关联</span>
        },
        {
            title: '类型',
            dataIndex: 'sceneType',
            width: 100,
            render: (val) => {
                const map = {CONVERSATION: '对话', TASK: '任务', WORKFLOW: '工作流'}
                return <Tag icon={<NodeIndexOutlined/>}>{map[val] || val}</Tag>
            }
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (val) => {
                const map = {PUBLISHED: 'success', DRAFT: 'warning', OFFLINE: 'error', ARCHIVED: 'default'}
                const label = {PUBLISHED: '已发布', DRAFT: '草稿', OFFLINE: '已下架', ARCHIVED: '已归档'}
                return <Badge status={map[val] || 'default'} text={label[val] || val}/>
            }
        },
        {
            title: '版本',
            dataIndex: 'version',
            width: 90,
            render: (v) => <span style={{fontFamily: 'monospace'}}>v{v || '1.0.0'}</span>
        },
        {
            title: '描述',
            dataIndex: 'description',
            ellipsis: true,
        },
        {
            title: '操作',
            width: 340,
            render: (_, record) => (
                <Space size="small" wrap>
                    <Tooltip title="查看详情"><Button size="small" icon={<EyeOutlined/>}
                                                      onClick={() => handleViewDetail(record)}>详情</Button></Tooltip>
                    <Tooltip title="编辑配置"><Button size="small" icon={<EditOutlined/>}
                                                      onClick={() => handleEdit(record)}>编辑</Button></Tooltip>
                    <Tooltip title="复制场景"><Button size="small" icon={<CopyOutlined/>}
                                                      onClick={() => handleDuplicate(record)}>复制</Button></Tooltip>
                    {record.status === 'DRAFT' && (
                        <Tooltip title="发布场景"><Button size="small" type="primary" icon={<CheckCircleOutlined/>}
                                                          onClick={() => handlePublish(record.sceneCode)}>发布</Button></Tooltip>
                    )}
                    {record.status === 'PUBLISHED' && (
                        <Tooltip title="下架场景"><Button size="small" danger icon={<StopOutlined/>}
                                                          onClick={() => handleOffline(record.sceneCode)}>下架</Button></Tooltip>
                    )}
                    <Tooltip title="执行场景">
                        <Button size="small" type="primary" icon={<PlayCircleOutlined/>}
                                onClick={() => window.open(`/ai/product/execute?sceneCode=${record.sceneCode}`, '_blank')}>执行</Button>
                    </Tooltip>
                    <Popconfirm title="确定删除此场景?" onConfirm={() => handleDelete(record.id)} okText="删除"
                                okButtonProps={{danger: true}}>
                        <Button size="small" danger icon={<DeleteOutlined/>}/>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    return (
        <div>
            <div
                style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16, gap: 12, flexWrap: 'wrap'}}>
                <Space>
                    <SearchInput placeholder="搜索场景名称/编码" onSearch={v => {
                        setSearchKey(v);
                        setPageNum(1)
                    }} size="large" allowClear/>
                    <Select placeholder="状态筛选" allowClear style={{width: 120}} onChange={v => {
                        setStatusFilter(v);
                        setPageNum(1)
                    }}>
                        <Option value="DRAFT">草稿</Option>
                        <Option value="PUBLISHED">已发布</Option>
                        <Option value="OFFLINE">已下架</Option>
                    </Select>
                    <Select placeholder="类型筛选" allowClear style={{width: 120}} onChange={v => {
                        setStatusFilter(v);
                        setPageNum(1)
                    }}>
                        <Option value="CONVERSATION">对话</Option>
                        <Option value="TASK">任务</Option>
                        <Option value="WORKFLOW">工作流</Option>
                    </Select>
                </Space>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新建场景</Button>
            </div>

            <Table
                dataSource={dataSource}
                columns={columns}
                loading={loading}
                rowKey="id"
                pagination={{
                    current: pageNum,
                    pageSize,
                    total,
                    showSizeChanger: false,
                    showTotal: t => `共 ${t} 个场景`,
                    onChange: p => setPageNum(p),
                }}
                locale={{
                    emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有场景，点击新建开始创建"/>
                }}
            />

            {/* 创建/编辑 Modal */}
            <Modal
                title={editingScene ? `编辑场景: ${editingScene.sceneName}` : '新建场景'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                size="large"
                confirmLoading={createLoading}
                okText={editingScene ? '保存' : '创建'}
            >
                <Form form={form} layout="vertical" size="middle">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="sceneName" label="场景名称"
                                       rules={[{required: true, message: '请输入场景名称'}]}>
                                <Input placeholder="如 智能问答助手"/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="sceneCode" label="场景编码"
                                       rules={[{required: true, message: '请输入场景编码'}]}>
                                <Input placeholder="如 chat_scene" disabled={!!editingScene}/>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="sceneType" label="场景类型" initialValue="CONVERSATION">
                                <Select>
                                    <Option value="CONVERSATION">对话场景</Option>
                                    <Option value="TASK">任务场景</Option>
                                    <Option value="WORKFLOW">工作流场景</Option>
                                </Select>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="productCode" label="所属产品">
                                <Select allowClear placeholder="选择产品">
                                    {productOptions.map(p => <Option key={p.productCode}
                                                                     value={p.productCode}>{p.productName}</Option>)}
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Form.Item name="description" label="场景描述">
                        <TextArea rows={2} placeholder="简短描述这个场景的能力和使用方式"/>
                    </Form.Item>

                    <Form.Item name="version" label="版本号" initialValue="1.0.0">
                        <Input placeholder="1.0.0"/>
                    </Form.Item>

                    {!editingScene && (
                        <Form.Item name="status" label="创建后状态" initialValue="DRAFT">
                            <Select>
                                <Option value="DRAFT">草稿</Option>
                                <Option value="PUBLISHED">直接发布</Option>
                            </Select>
                        </Form.Item>
                    )}
                </Form>
            </Modal>

            {/* 详情 Drawer */}
            <Drawer
                title="场景详情"
                open={detailVisible}
                onClose={() => setDetailVisible(false)}
                size="large"
            >
                {detailScene && (
                    <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                        <div style={{display: 'flex', alignItems: 'center', gap: 12}}>
                            <div style={{
                                width: 48, height: 48, borderRadius: 10,
                                background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                                display: 'flex', alignItems: 'center', justifyContent: 'center'
                            }}>
                                <NodeIndexOutlined style={{fontSize: 22, color: '#fff'}}/>
                            </div>
                            <div>
                                <div style={{fontSize: 16, fontWeight: 600}}>{detailScene.sceneName}</div>
                                <div style={{fontSize: 12, color: '#999'}}>{detailScene.sceneCode}</div>
                            </div>
                        </div>

                        <Divider style={{margin: '8px 0'}}/>

                        <Row gutter={16}>
                            <Col span={12}>
                                <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>类型</div>
                                <Tag>
                                    {detailScene.sceneType === 'CONVERSATION' ? '对话' :
                                        detailScene.sceneType === 'TASK' ? '任务' : '工作流'}
                                </Tag>
                            </Col>
                            <Col span={12}>
                                <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>版本</div>
                                <span style={{fontFamily: 'monospace'}}>{detailScene.version || '1.0.0'}</span>
                            </Col>
                        </Row>

                        <div>
                            <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>状态</div>
                            <Badge status={
                                detailScene.status === 'PUBLISHED' ? 'success' :
                                    detailScene.status === 'DRAFT' ? 'warning' : 'error'
                            } text={
                                detailScene.status === 'PUBLISHED' ? '已发布' :
                                    detailScene.status === 'DRAFT' ? '草稿' :
                                        detailScene.status === 'OFFLINE' ? '已下架' : '已归档'
                            }/>
                        </div>

                        <div>
                            <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>描述</div>
                            <div style={{color: '#333', lineHeight: 1.6}}>{detailScene.description || '暂无描述'}</div>
                        </div>
                    </div>
                )}
            </Drawer>
        </div>
    )
}

// ===== 节点配置页 =====
const NodeConfigPage = () => {
    const [scenes, setScenes] = useState([])
    const [selectedScene, setSelectedScene] = useState(null)
    const [nodes, setNodes] = useState([])
    const [loading, setLoading] = useState(false)
    const [nodeModalVisible, setNodeModalVisible] = useState(false)
    const [form] = Form.useForm()

    useEffect(() => {
        sceneApi.page({id: 1, pageSize: 100}).then(res => {
            setScenes(res.data?.records || [])
        })
    }, [])

    useEffect(() => {
        if (!selectedScene) return
        loadNodes(selectedScene.sceneCode)
    }, [selectedScene])

    const loadNodes = async (sceneCode) => {
        setLoading(true)
        try {
            const res = await sceneApi.nodes({params: {sceneCode}})
            setNodes(res.data || [])
        } catch (e) {
            setNodes([])
        }
        setLoading(false)
    }

    const handleAddNode = () => {
        form.resetFields()
        form.setFieldsValue({nodeType: 'LLM', isEnabled: true})
        setNodeModalVisible(true)
    }

    const handleSubmitNode = async () => {
        try {
            const values = await form.validateFields()
            setNodes(prev => [...(prev || []), {...values, id: Date.now()}])
            setNodeModalVisible(false)
            message.success('节点已添加')
        } catch (e) {
        }
    }

    const handleRemoveNode = (id) => {
        setNodes(prev => (prev || []).filter(n => n.id !== id))
    }

    const handleToggleNode = (id) => {
        setNodes(prev => prev.map(n => n.id === id ? {...n, isEnabled: !n.isEnabled} : n))
    }

    const nodeColumns = [
        {title: '节点名称', dataIndex: 'nodeName', key: 'nodeName'},
        {title: '节点类型', dataIndex: 'nodeType', key: 'nodeType', render: (v) => <Tag color="blue">{v}</Tag>},
        {title: '配置', dataIndex: 'config', key: 'config', ellipsis: true},
        {
            title: '启用',
            dataIndex: 'isEnabled',
            key: 'isEnabled',
            render: (v, record) => <Switch checked={v} onChange={() => handleToggleNode(record.id)}/>
        },
        {
            title: '操作',
            width: 100,
            render: (_, record) => (
                <Button size="small" danger icon={<DeleteOutlined/>} onClick={() => handleRemoveNode(record.id)}/>
            )
        }
    ]

    return (
        <div style={{display: 'flex', gap: 16}}>
            {/* 左侧场景列表 */}
            <Card
                size="small"
                title={<span style={{fontWeight: 600}}><NodeIndexOutlined style={{color: '#4facfe', marginRight: 6}}/>选择场景</span>}
                style={{width: 260, flexShrink: 0, borderRadius: 10}}
                styles={{header: {borderBottom: '1px solid #f0f0f0', padding: '12px 16px'}, body: {padding: 8}}}
            >
                {(scenes || []).map(s => (
                    <div
                        key={s.id}
                        onClick={() => setSelectedScene(s)}
                        style={{
                            padding: '10px 12px',
                            borderRadius: 8,
                            cursor: 'pointer',
                            marginBottom: 4,
                            background: selectedScene?.id === s.id ? 'linear-gradient(135deg, #4facfe22, #00f2fe22)' : 'transparent',
                            border: selectedScene?.id === s.id ? '1px solid #4facfe44' : '1px solid transparent',
                        }}
                    >
                        <div style={{fontSize: 13, fontWeight: 500}}>{s.sceneName}</div>
                        <div style={{fontSize: 11, color: '#999'}}>{s.sceneCode}</div>
                    </div>
                ))}
                {scenes.length === 0 && <Empty description="暂无场景" style={{padding: 20}}/>}
            </Card>

            {/* 右侧节点配置 */}
            <Card
                title={<span
                    style={{fontWeight: 600}}>节点配置 {selectedScene ? `- ${selectedScene.sceneName}` : ''}</span>}
                extra={<Button size="small" icon={<PlusOutlined/>} onClick={handleAddNode}
                               disabled={!selectedScene}>添加节点</Button>}
                style={{flex: 1, borderRadius: 10}}
            >
                {!selectedScene ? (
                    <Empty description="请先选择场景" style={{padding: 60}} image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                ) : (
                    <Table
                        dataSource={nodes}
                        columns={nodeColumns}
                        rowKey="id"
                        pagination={false}
                        locale={{
                            emptyText: <Empty description="暂无节点，点击添加开始配置"
                                              image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                        }}
                    />
                )}
            </Card>

            {/* 添加节点 Modal */}
            <Modal
                title="添加节点"
                open={nodeModalVisible}
                onOk={handleSubmitNode}
                onCancel={() => setNodeModalVisible(false)}
                okText="添加"
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="nodeName" label="节点名称" rules={[{required: true, message: '请输入节点名称'}]}>
                        <Input placeholder="如 LLM调用节点"/>
                    </Form.Item>
                    <Form.Item name="nodeType" label="节点类型" initialValue="LLM">
                        <Select>
                            <Option value="LLM">LLM调用</Option>
                            <Option value="TOOL">工具调用</Option>
                            <Option value="CONDITION">条件分支</Option>
                            <Option value="TRANSFORM">数据转换</Option>
                            <Option value="HTTP">HTTP请求</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="config" label="节点配置">
                        <TextArea rows={3} placeholder='JSON格式配置，如 {"model":"gpt-4"}'/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

// ===== 场景编排页 =====
const SceneCanvasPage = () => {
    const [scenes, setScenes] = useState([])
    const [selectedScene, setSelectedScene] = useState(null)
    const [canvasNodes, setCanvasNodes] = useState([])
    const [canvasEdges, setCanvasEdges] = useState([])

    useEffect(() => {
        sceneApi.page({id: 1, pageSize: 100}).then(res => {
            setScenes(res.data?.records || [])
        })
    }, [])

    const handleSceneSelect = async (scene) => {
        setSelectedScene(scene)
        try {
            const res = await sceneApi.canvasGet({params: {sceneCode: scene.sceneCode}})
            if (res.data) {
                setCanvasNodes(res.data.nodes || [])
                setCanvasEdges(res.data.edges || [])
            }
        } catch (e) {
            setCanvasNodes([])
            setCanvasEdges([])
        }
    }

    const canvasColumns = [
        {title: '节点', dataIndex: 'label', key: 'label', width: 200},
        {title: '类型', dataIndex: 'type', key: 'type', render: (v) => <Tag color="purple">{v}</Tag>},
        {
            title: '位置',
            dataIndex: 'position',
            key: 'position',
            render: (p) => <span style={{fontSize: 11, color: '#999'}}>x:{p?.x || 0} y:{p?.y || 0}</span>
        },
    ]

    return (
        <div style={{display: 'flex', gap: 16, minHeight: 500}}>
            {/* 左侧场景选择 */}
            <Card
                size="small"
                title={<span style={{fontWeight: 600}}><NodeIndexOutlined style={{color: '#4facfe', marginRight: 6}}/>场景列表</span>}
                style={{width: 240, flexShrink: 0, borderRadius: 10}}
                styles={{header: {borderBottom: '1px solid #f0f0f0', padding: '12px 16px'}, body: {padding: 8}}}
            >
                {(scenes || []).map(s => (
                    <div
                        key={s.id}
                        onClick={() => handleSceneSelect(s)}
                        style={{
                            padding: '10px 12px',
                            borderRadius: 8,
                            cursor: 'pointer',
                            marginBottom: 4,
                            background: selectedScene?.id === s.id ? 'linear-gradient(135deg, #4facfe22, #00f2fe22)' : 'transparent',
                            border: selectedScene?.id === s.id ? '1px solid #4facfe44' : '1px solid transparent',
                        }}
                    >
                        <div style={{fontSize: 13, fontWeight: 500}}>{s.sceneName}</div>
                        <Tag style={{fontSize: 10, marginTop: 2}}>{s.sceneType}</Tag>
                    </div>
                ))}
            </Card>

            {/* 右侧画布 */}
            <Card
                title={<span
                    style={{fontWeight: 600}}>场景编排画布 {selectedScene ? `- ${selectedScene.sceneName}` : ''}</span>}
                style={{flex: 1, borderRadius: 10}}
                styles={{body: {padding: 16, display: 'flex', flexDirection: 'column', gap: 12}}}
            >
                {!selectedScene ? (
                    <Empty description="请选择场景进行编排" style={{padding: 60}} image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                ) : canvasNodes.length === 0 ? (
                    <Empty description="暂无节点配置，请先在节点配置中添加节点" style={{padding: 40}}
                           image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                ) : (
                    <>
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 8,
                            padding: '8px 0',
                            borderBottom: '1px solid #f0f0f0'
                        }}>
                            <ThunderboltOutlined style={{color: '#4facfe'}}/>
                            <span style={{
                                fontSize: 13,
                                color: '#666'
                            }}>编排节点 {canvasNodes.length} 个，连接 {canvasEdges.length} 条</span>
                        </div>
                        <Table
                            dataSource={canvasNodes}
                            columns={canvasColumns}
                            rowKey="id"
                            pagination={false}
                            size="small"
                        />
                        {canvasEdges.length > 0 && (
                            <>
                                <div style={{fontSize: 13, fontWeight: 500, marginTop: 8}}>连线</div>
                                <div style={{display: 'flex', flexDirection: 'column', gap: 4}}>
                                    {canvasEdges.map((edge, idx) => (
                                        <Card key={idx} size="small" style={{background: '#fafafa', border: 'none'}}>
                      <span style={{fontFamily: 'monospace', fontSize: 12}}>
                        {edge.source} → {edge.target}
                      </span>
                                        </Card>
                                    ))}
                                </div>
                            </>
                        )}
                    </>
                )}
            </Card>
        </div>
    )
}

export default SceneManagementPage
