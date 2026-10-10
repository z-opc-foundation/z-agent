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
    Statistic,
    Table,
    Tabs,
    Tag,
    Tooltip
} from 'antd'
import {
    CheckCircleOutlined,
    CloudUploadOutlined,
    CopyOutlined,
    DeleteOutlined,
    EditOutlined,
    ExclamationCircleOutlined,
    EyeOutlined,
    FileTextOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    ShopOutlined,
    StopOutlined,
    TagOutlined
} from '@ant-design/icons'
import {productApi} from '../../api'
import {PageHeader, SearchInput} from '@/common/components/ui'

const {TextArea} = Input
const {Option} = Select
const {TabPane} = Tabs

// ===== 产品列表页 =====
const ProductListPage = () => {
    const [activeTab, setActiveTab] = useState('list')
    return (
        <div>
            <PageHeader
                title="订阅中心"
                subtitle="管理产品配置、发布和场景绑定"
            />
            <Card>
                <Tabs activeKey={activeTab} onChange={setActiveTab}>
                    <TabPane tab={<span><ShopOutlined/> 产品管理</span>} key="list">
                        <ProductTablePage/>
                    </TabPane>
                    <TabPane tab={<span><FileTextOutlined/> 产品配置</span>} key="config">
                        <ProductConfigPage/>
                    </TabPane>
                </Tabs>
            </Card>
        </div>
    )
}

// ===== 产品表格 =====
const ProductTablePage = () => {
    const [dataSource, setDataSource] = useState([])
    const [loading, setLoading] = useState(false)
    const [searchKey, setSearchKey] = useState('')
    const [statusFilter, setStatusFilter] = useState('')
    const [pageNum, setPageNum] = useState(1)
    const [pageSize] = useState(12)
    const [total, setTotal] = useState(0)
    const [modalVisible, setModalVisible] = useState(false)
    const [form] = Form.useForm()
    const [editingProduct, setEditingProduct] = useState(null)
    const [createLoading, setCreateLoading] = useState(false)
    const [detailVisible, setDetailVisible] = useState(false)
    const [detailProduct, setDetailProduct] = useState(null)

    useEffect(() => {
        fetchData()
    }, [pageNum, searchKey, statusFilter])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await productApi.page({productName: searchKey, status: statusFilter, id: pageNum})
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
        setEditingProduct(null)
        form.resetFields()
        form.setFieldsValue({
            status: 'DRAFT',
        })
        setModalVisible(true)
    }

    const handleEdit = (record) => {
        setEditingProduct(record)
        form.setFieldsValue({
            productCode: record.productCode,
            productName: record.productName,
            description: record.description,
            category: record.category,
            version: record.version || '1.0.0',
            iconUrl: record.iconUrl,
            status: record.status,
            tags: record.tags,
        })
        setModalVisible(true)
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            setCreateLoading(true)
            if (editingProduct) {
                await productApi.update({data: values, params: {id: editingProduct.id}})
                message.success('更新成功')
            } else {
                await productApi.create(values)
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
            await productApi.delete({params: {id}})
            message.success('删除成功')
            fetchData()
        } catch (e) {
            message.error('删除失败')
        }
    }

    const handlePublish = async (productCode) => {
        try {
            await productApi.publish({params: {productCode}})
            message.success('发布成功')
            fetchData()
        } catch (e) {
            message.error('发布失败')
        }
    }

    const handleOffline = async (productCode) => {
        try {
            await productApi.offline({params: {productCode}})
            message.success('下架成功')
            fetchData()
        } catch (e) {
            message.error('下架失败')
        }
    }

    const handleViewDetail = async (record) => {
        setDetailProduct(record)
        setDetailVisible(true)
    }

    const copyProductCode = (code) => {
        navigator.clipboard.writeText(code).then(() => message.success('产品编码已复制')).catch(() => {
        })
    }

    const columns = [
        {
            title: '产品',
            key: 'product',
            width: 280,
            render: (_, record) => (
                <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <div style={{
                        width: 40, height: 40, borderRadius: 8,
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center'
                    }}>
                        <ShopOutlined style={{fontSize: 18, color: '#fff'}}/>
                    </div>
                    <div>
                        <div style={{fontWeight: 500}}>{record.productName}</div>
                        <div style={{fontSize: 12, color: '#999'}}>
                            <span style={{fontFamily: 'monospace'}}>{record.productCode}</span>
                            <Tooltip title="复制编码">
                                <Button size="small" type="text" icon={<CopyOutlined/>}
                                        onClick={() => copyProductCode(record.productCode)}
                                        style={{marginLeft: 4, padding: 0}}/>
                            </Tooltip>
                        </div>
                    </div>
                </div>
            )
        },
        {
            title: '分类',
            dataIndex: 'category',
            width: 120,
            render: (val) => val ? <Tag icon={<TagOutlined/>}>{val}</Tag> : <span style={{color: '#ccc'}}>未分类</span>
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
            width: 320,
            render: (_, record) => (
                <Space size="small" wrap>
                    <Tooltip title="查看详情"><Button size="small" icon={<EyeOutlined/>}
                                                      onClick={() => handleViewDetail(record)}>详情</Button></Tooltip>
                    <Tooltip title="编辑配置"><Button size="small" icon={<EditOutlined/>}
                                                      onClick={() => handleEdit(record)}>编辑</Button></Tooltip>
                    {record.status === 'DRAFT' && (
                        <Tooltip title="发布产品"><Button size="small" type="primary" icon={<CheckCircleOutlined/>}
                                                          onClick={() => handlePublish(record.productCode)}>发布</Button></Tooltip>
                    )}
                    {record.status === 'PUBLISHED' && (
                        <Tooltip title="下架产品"><Button size="small" danger icon={<StopOutlined/>}
                                                          onClick={() => handleOffline(record.productCode)}>下架</Button></Tooltip>
                    )}
                    <Tooltip title="执行场景"><Button size="small" type="primary" icon={<PlayCircleOutlined/>}
                                                      onClick={() => window.open(`/ai/product/execute?productCode=${record.productCode}`, '_blank')}>执行</Button></Tooltip>
                    <Popconfirm title="确定删除此产品?" onConfirm={() => handleDelete(record.id)} okText="删除"
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
                    <SearchInput placeholder="搜索产品名称/编码" onSearch={v => {
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
                        <Option value="ARCHIVED">已归档</Option>
                    </Select>
                </Space>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新建产品</Button>
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
                    showTotal: t => `共 ${t} 个产品`,
                    onChange: p => setPageNum(p),
                }}
                locale={{
                    emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有产品，点击新建开始创建"/>
                }}
            />

            {/* 创建/编辑 Modal */}
            <Modal
                title={editingProduct ? `编辑产品: ${editingProduct.productName}` : '新建产品'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                size="large"
                confirmLoading={createLoading}
                okText={editingProduct ? '保存' : '创建'}
            >
                <Form form={form} layout="vertical" size="middle">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="productName" label="产品名称"
                                       rules={[{required: true, message: '请输入产品名称'}]}>
                                <Input placeholder="如：智能客服助手"/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="productCode" label="产品编码"
                                       rules={[{required: true, message: '请输入产品编码'}]}>
                                <Input placeholder="如 product-ai-assistant" disabled={!!editingProduct}/>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="category" label="产品分类">
                                <Input placeholder="如 AI助手, 图像生成"/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="version" label="版本号" initialValue="1.0.0">
                                <Input placeholder="1.0.0"/>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Form.Item name="description" label="产品描述">
                        <TextArea rows={2} placeholder="简短描述这个产品的能力和使用场景"/>
                    </Form.Item>

                    <Form.Item name="tags" label="产品标签">
                        <Input placeholder="用逗号分隔，如 AI,客服,对话"/>
                    </Form.Item>

                    <Form.Item name="iconUrl" label="图标URL">
                        <Input placeholder="可选，图标图片URL"/>
                    </Form.Item>

                    {!editingProduct && (
                        <Form.Item name="status" label="创建后状态" initialValue="DRAFT">
                            <Select>
                                <Option value="DRAFT">草稿（仅自己可见）</Option>
                                <Option value="PUBLISHED">直接发布</Option>
                            </Select>
                        </Form.Item>
                    )}
                </Form>
            </Modal>

            {/* 详情 Drawer */}
            <Drawer
                title="产品详情"
                open={detailVisible}
                onClose={() => setDetailVisible(false)}
                size="large"
            >
                {detailProduct && (
                    <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                        <div style={{display: 'flex', alignItems: 'center', gap: 12}}>
                            <div style={{
                                width: 48, height: 48, borderRadius: 10,
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                display: 'flex', alignItems: 'center', justifyContent: 'center'
                            }}>
                                <ShopOutlined style={{fontSize: 22, color: '#fff'}}/>
                            </div>
                            <div>
                                <div style={{fontSize: 16, fontWeight: 600}}>{detailProduct.productName}</div>
                                <div style={{fontSize: 12, color: '#999'}}>{detailProduct.productCode}</div>
                            </div>
                        </div>

                        <Divider style={{margin: '8px 0'}}/>

                        <Row gutter={16}>
                            <Col span={12}><Statistic title="分类" value={detailProduct.category || '未分类'}
                                                      styles={{content: {fontSize: 14}}}/></Col>
                            <Col span={12}><Statistic title="版本" value={detailProduct.version || '1.0.0'}
                                                      styles={{content: {fontSize: 14}}}/></Col>
                        </Row>

                        <div>
                            <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>状态</div>
                            <Badge status={
                                detailProduct.status === 'PUBLISHED' ? 'success' :
                                    detailProduct.status === 'DRAFT' ? 'warning' : 'error'
                            } text={
                                detailProduct.status === 'PUBLISHED' ? '已发布' :
                                    detailProduct.status === 'DRAFT' ? '草稿' :
                                        detailProduct.status === 'OFFLINE' ? '已下架' : '已归档'
                            }/>
                        </div>

                        {detailProduct.tags && (
                            <div>
                                <div style={{fontSize: 12, color: '#999', marginBottom: 6}}>标签</div>
                                <Space wrap>
                                    {(detailProduct.tags || '').split(',').filter(Boolean).map((t, i) => (
                                        <Tag key={i} color="blue">{t}</Tag>
                                    ))}
                                </Space>
                            </div>
                        )}

                        <div>
                            <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>描述</div>
                            <div
                                style={{color: '#333', lineHeight: 1.6}}>{detailProduct.description || '暂无描述'}</div>
                        </div>
                    </div>
                )}
            </Drawer>
        </div>
    )
}

// ===== 产品配置页 =====
const ProductConfigPage = () => {
    const [products, setProducts] = useState([])
    const [selectedProduct, setSelectedProduct] = useState(null)
    const [configData, setConfigData] = useState({scenes: [], params: []})
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [sceneModalVisible, setSceneModalVisible] = useState(false)
    const [form] = Form.useForm()

    useEffect(() => {
        productApi.page({id: 1, pageSize: 100}).then(res => {
            setProducts(res.data?.records || [])
        })
    }, [])

    useEffect(() => {
        if (!selectedProduct) return
        loadConfig(selectedProduct.productCode)
    }, [selectedProduct])

    const loadConfig = async (productCode) => {
        setLoading(true)
        try {
            const res = await productApi.configGet({params: {productCode}})
            setConfigData(res.data || {scenes: [], params: []})
        } catch (e) {
            setConfigData({scenes: [], params: []})
        }
        setLoading(false)
    }

    const handleSave = async () => {
        if (!selectedProduct) return
        setSaving(true)
        try {
            await productApi.configSave({productCode: selectedProduct.productCode, ...configData})
            message.success('配置已保存')
        } catch (e) {
            message.error('保存失败')
        } finally {
            setSaving(false)
        }
    }

    const handleAddScene = () => {
        form.resetFields()
        setSceneModalVisible(true)
    }

    const handleSubmitScene = async () => {
        try {
            const values = await form.validateFields()
            setConfigData(prev => ({
                ...prev,
                scenes: [...(prev.scenes || []), {...values, id: Date.now()}]
            }))
            setSceneModalVisible(false)
            message.success('场景已添加')
        } catch (e) {
        }
    }

    const handleRemoveScene = (id) => {
        setConfigData(prev => ({
            ...prev,
            scenes: (prev.scenes || []).filter(s => s.id !== id)
        }))
    }

    return (
        <div style={{display: 'flex', gap: 16}}>
            {/* 左侧：产品选择 */}
            <Card
                size="small"
                title={<span style={{fontWeight: 600}}><ShopOutlined style={{color: '#667eea', marginRight: 6}}/>选择产品</span>}
                style={{width: 260, flexShrink: 0, borderRadius: 10}}
                styles={{header: {borderBottom: '1px solid #f0f0f0', padding: '12px 16px'}, body: {padding: 8}}}
            >
                {(products || []).map(p => (
                    <div
                        key={p.id}
                        onClick={() => setSelectedProduct(p)}
                        style={{
                            padding: '10px 12px',
                            borderRadius: 8,
                            cursor: 'pointer',
                            marginBottom: 4,
                            background: selectedProduct?.id === p.id ? 'linear-gradient(135deg, #667eea22, #764ba222)' : 'transparent',
                            border: selectedProduct?.id === p.id ? '1px solid #667eea44' : '1px solid transparent',
                            transition: 'all 0.2s',
                        }}
                    >
                        <div style={{fontSize: 13, fontWeight: 500}}>{p.productName}</div>
                        <div style={{fontSize: 11, color: '#999'}}>{p.productCode}</div>
                    </div>
                ))}
                {products.length === 0 && <Empty description="暂无产品" style={{padding: 20}}/>}
            </Card>

            {/* 右侧：配置面板 */}
            <Card
                title={<span
                    style={{fontWeight: 600}}>产品配置 {selectedProduct ? `- ${selectedProduct.productName}` : ''}</span>}
                extra={<Button type="primary" icon={<CloudUploadOutlined/>} onClick={handleSave}
                               disabled={!selectedProduct} loading={saving}>保存配置</Button>}
                style={{flex: 1, borderRadius: 10}}
            >
                {!selectedProduct ? (
                    <Empty description="请先选择产品" style={{padding: 60}} image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                ) : loading ? (
                    <div style={{textAlign: 'center', padding: 40}}>加载中...</div>
                ) : (
                    <div style={{display: 'flex', flexDirection: 'column', gap: 20}}>
                        {/* 场景列表 */}
                        <div>
                            <div style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                marginBottom: 12
                            }}>
                                <span style={{fontWeight: 500}}>关联场景</span>
                                <Button size="small" icon={<PlusOutlined/>} onClick={handleAddScene}>添加场景</Button>
                            </div>
                            {(configData.scenes || []).length === 0 ? (
                                <div style={{
                                    textAlign: 'center',
                                    padding: '20px 0',
                                    color: '#999',
                                    background: '#fafafa',
                                    borderRadius: 8
                                }}>
                                    <ExclamationCircleOutlined style={{marginRight: 6}}/>暂无关联场景
                                </div>
                            ) : (
                                <div style={{display: 'flex', flexDirection: 'column', gap: 8}}>
                                    {(configData.scenes || []).map((scene, idx) => (
                                        <Card key={scene.id || idx} size="small"
                                              style={{background: '#fafafa', border: 'none'}}>
                                            <div style={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center'
                                            }}>
                                                <div>
                                                    <Tag color="purple">{scene.sceneName}</Tag>
                                                    <span style={{
                                                        fontSize: 12,
                                                        color: '#666',
                                                        marginLeft: 8
                                                    }}>{scene.sceneCode}</span>
                                                </div>
                                                <Button size="small" danger icon={<DeleteOutlined/>}
                                                        onClick={() => handleRemoveScene(scene.id)}/>
                                            </div>
                                        </Card>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </Card>

            {/* 添加场景 Modal */}
            <Modal
                title="添加场景"
                open={sceneModalVisible}
                onOk={handleSubmitScene}
                onCancel={() => setSceneModalVisible(false)}
                okText="添加"
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="sceneName" label="场景名称" rules={[{required: true, message: '请输入场景名称'}]}>
                        <Input placeholder="如 智能问答"/>
                    </Form.Item>
                    <Form.Item name="sceneCode" label="场景编码" rules={[{required: true, message: '请输入场景编码'}]}>
                        <Input placeholder="如 chat_scene"/>
                    </Form.Item>
                    <Form.Item name="sceneType" label="场景类型" initialValue="CONVERSATION">
                        <Select>
                            <Option value="CONVERSATION">对话</Option>
                            <Option value="TASK">任务</Option>
                            <Option value="WORKFLOW">工作流</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <TextArea rows={2} placeholder="场景描述"/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default ProductListPage
