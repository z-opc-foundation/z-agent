import {useEffect, useState} from 'react'
import {
    Alert,
    Button,
    Card,
    Col,
    Form,
    Input,
    message,
    Modal,
    Radio,
    Row,
    Select,
    Space,
    Spin,
    Statistic,
    Table,
    Tabs,
    Tag,
    Tooltip
} from 'antd'
import {
    ApiOutlined,
    AppstoreOutlined,
    CheckCircleOutlined,
    CopyOutlined,
    DeleteOutlined,
    KeyOutlined,
    PlusOutlined,
    ReloadOutlined,
    StopOutlined,
    ThunderboltOutlined,
    UserOutlined
} from '@ant-design/icons'
import request from '../../api'
import AkUsageDrawer from './AkUsageDrawer'
import {PageHeader} from '@/common/components/ui'

const {confirm} = Modal

const API_BASE_URL = 'http://localhost:8888'

const formatTokens = (n) => {
    if (!n) return '0'
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
    return Number(n).toLocaleString()
}

const AkManage = () => {
    const [activeTab, setActiveTab] = useState('PERSONAL')  // PERSONAL / APP
    const [keys, setKeys] = useState([])
    const [loading, setLoading] = useState(false)
    const [visible, setVisible] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()
    const [showSkModal, setShowSkModal] = useState(null)
    const [drawerAk, setDrawerAk] = useState(null)
    const [appOptions, setAppOptions] = useState([])

    const fetchList = async () => {
        setLoading(true)
        try {
            const res = await request('/ak/list', {method: 'GET', params: {akType: activeTab}})
            setKeys(Array.isArray(res) ? res : (res?.data || []))
        } catch (e) {
            message.error('加载失败')
        } finally {
            setLoading(false)
        }
    }

    const fetchApps = async () => {
        try {
            const res = await request('/agent/app/page', {
                method: 'POST',
                body: JSON.stringify({current: 1, size: 100})
            })
            const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
            setAppOptions(list.map(a => ({value: a.appCode, label: a.appName || a.appCode})))
        } catch (e) {
            // 静默失败
        }
    }

    useEffect(() => {
        fetchList()
    }, [activeTab])
    useEffect(() => {
        fetchApps()
    }, [])

    const handleAdd = () => {
        setEditing(null)
        form.resetFields()
        form.setFieldsValue({akType: activeTab})  // 按当前 Tab 预填类型
        setVisible(true)
    }

    const handleEdit = (record) => {
        setEditing(record)
        form.setFieldsValue({
            akName: record.akName,
            description: record.description,
            akType: record.akType || 'PERSONAL',
            appCode: record.appCode,
            appName: record.appName,
        })
        setVisible(true)
    }

    const handleDelete = (record) => {
        confirm({
            title: '确认删除',
            content: `删除 AK「${record.akName}」？此操作不可恢复。`,
            okText: '删除',
            okType: 'danger',
            onOk: async () => {
                await request('/ak/delete', {method: 'POST', params: {id: record.id}})
                message.success('删除成功')
                fetchList()
            },
        })
    }

    const handleToggle = async (record) => {
        const newStatus = record.status === 1 ? 0 : 1
        await request('/ak/toggle', {method: 'POST', params: {id: record.id, status: newStatus}})
        message.success(newStatus === 1 ? '已启用' : '已禁用')
        fetchList()
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (editing) {
                await request('/ak/update', {method: 'POST', data: {...values, id: editing.id}})
                message.success('更新成功')
                setVisible(false)
                fetchList()
            } else {
                const res = await request('/ak/create', {method: 'POST', data: values})
                const data = res?.accessKey ? res : (res?.data || {})
                setVisible(false)
                fetchList()
                setShowSkModal({
                    akName: values.akName,
                    accessKey: data.accessKey,
                    secretKey: data.secretKey,
                })
            }
        } catch (e) {
            // 校验失败不弹错误（form 自己展示）
        }
    }

    const handleCopy = (text, label) => {
        navigator.clipboard.writeText(text)
        message.success(`${label} 已复制`)
    }

    const columns = [
        {
            title: '名称',
            dataIndex: 'akName',
            key: 'akName',
            width: 160,
            render: (text, record) => (
                <Space>
                    <KeyOutlined style={{color: '#1677ff'}}/>
                    <span style={{fontWeight: 500}}>{text}</span>
                    {record.akType === 'APP' && record.appName && (
                        <Tag color="purple" style={{borderRadius: 10, fontSize: 11}}>{record.appName}</Tag>
                    )}
                </Space>
            ),
        },
        {
            title: 'AccessKey',
            dataIndex: 'accessKey',
            key: 'accessKey',
            width: 200,
            render: (text) => (
                <Space>
                    <code style={{
                        fontFamily: 'monospace', fontSize: 12, color: '#595959',
                        background: '#f5f5f5', padding: '3px 8px', borderRadius: 4,
                    }}>
                        {text}
                    </code>
                    <Tooltip title="复制 AccessKey">
                        <Button size="small" type="text" icon={<CopyOutlined/>} onClick={(e) => {
                            e.stopPropagation();
                            handleCopy(text, 'AccessKey')
                        }}/>
                    </Tooltip>
                </Space>
            ),
        },
        {
            title: 'Token 消耗',
            dataIndex: 'totalTokens',
            key: 'totalTokens',
            width: 110,
            sorter: (a, b) => (a.totalTokens || 0) - (b.totalTokens || 0),
            render: (t) => (
                <Space>
                    <ThunderboltOutlined style={{color: '#faad14', fontSize: 12}}/>
                    <span style={{fontWeight: 500}}>{formatTokens(t)}</span>
                </Space>
            ),
        },
        {
            title: '最后使用',
            dataIndex: 'lastUsedTime',
            key: 'lastUsedTime',
            width: 160,
            render: (t) => (
                <Space size={4}>
                    <span style={{color: t ? '#595959' : '#bfbfbf', fontSize: 13}}>{t || '从未使用'}</span>
                </Space>
            ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 70,
            render: (s) => (
                <Tag color={s === 1 ? 'green' : 'red'} style={{borderRadius: 20}}>
                    {s === 1 ? '正常' : '禁用'}
                </Tag>
            ),
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true,
        },
        {
            title: '创建时间',
            dataIndex: 'createdTime',
            key: 'createdTime',
            width: 160,
        },
        {
            title: '操作',
            key: 'action',
            width: 130,
            render: (_, record) => (
                <Space size="small" onClick={(e) => e.stopPropagation()}>
                    <Tooltip title={record.status === 1 ? '禁用' : '启用'}>
                        <Button
                            size="small"
                            type="text"
                            icon={record.status === 1 ? <StopOutlined style={{color: '#faad14'}}/> :
                                <CheckCircleOutlined style={{color: '#52c41a'}}/>}
                            onClick={() => handleToggle(record)}
                        />
                    </Tooltip>
                    <Tooltip title="编辑">
                        <Button size="small" type="text" icon={<KeyOutlined/>} onClick={() => handleEdit(record)}/>
                    </Tooltip>
                    <Tooltip title="删除">
                        <Button size="small" type="text" icon={<DeleteOutlined style={{color: '#ff4d4f'}}/>}
                                onClick={() => handleDelete(record)}/>
                    </Tooltip>
                </Space>
            ),
        },
    ]

    const totalTokensAll = keys.reduce((s, k) => s + (k.totalTokens || 0), 0)
    const activeCount = keys.filter(k => k.status === 1).length

    return (
        <div style={{padding: 16, display: 'flex', flexDirection: 'column', gap: 12}}>
            <PageHeader
                title="API Key 管理"
                subtitle="创建和管理 AccessKey，控制 API 访问权限"
            />
            <Alert
                type="info"
                showIcon
                icon={<ApiOutlined/>}
                message={
                    <Space>
                        <span style={{fontWeight: 500}}>API 接入地址：</span>
                        <code style={{
                            fontFamily: 'monospace', fontSize: 13, background: '#e6f4ff',
                            padding: '2px 8px', borderRadius: 4,
                        }}>{API_BASE_URL}</code>
                        <Button size="small" type="text" icon={<CopyOutlined/>}
                                onClick={() => handleCopy(API_BASE_URL, 'API 地址')}/>
                        <span style={{color: '#8c8c8c', fontSize: 12}}>使用此地址 + AccessKey 调用平台所有模型</span>
                    </Space>
                }
                style={{border: 'none', background: '#e6f4ff'}}
            />

            <Row gutter={12}>
                <Col span={6}>
                    <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                        <Statistic title="AK 总数" value={keys.length} prefix={<KeyOutlined/>}
                                   styles={{content: {fontSize: 18}}}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                        <Statistic title="累计 Token" value={formatTokens(totalTokensAll)}
                                   prefix={<ThunderboltOutlined/>} styles={{content: {fontSize: 18}}}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                        <Statistic title="有效 AK" value={activeCount}
                                   prefix={<CheckCircleOutlined style={{color: '#52c41a'}}/>}
                                   styles={{content: {fontSize: 18}}}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                        <Statistic title="所属应用" value={new Set(keys.map(k => k.appCode).filter(Boolean)).size}
                                   prefix={<AppstoreOutlined/>} styles={{content: {fontSize: 18}}}/>
                    </Card>
                </Col>
            </Row>

            <Card size="small" styles={{body: {padding: 0}}}>
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    size="small"
                    tabBarStyle={{margin: 0, padding: '0 16px'}}
                    tabBarExtraContent={
                        <Space style={{padding: '8px 0'}}>
                            <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd} size="small">
                                {activeTab === 'PERSONAL' ? '申请个人 AK' : '申请应用 AK'}
                            </Button>
                            <Button icon={<ReloadOutlined/>} onClick={fetchList} size="small"/>
                        </Space>
                    }
                    items={[
                        {
                            key: 'PERSONAL',
                            label: <Space><UserOutlined/>个人 AK</Space>,
                            children: (
                                <Spin spinning={loading}>
                                    <Table
                                        dataSource={keys}
                                        columns={columns}
                                        rowKey="id"
                                        pagination={false}
                                        size="small"
                                        onRow={(record) => ({
                                            onClick: () => setDrawerAk(record),
                                            style: {cursor: 'pointer'},
                                        })}
                                        locale={{emptyText: '暂无个人 AK，点右上角"申请个人 AK"创建'}}
                                    />
                                </Spin>
                            ),
                        },
                        {
                            key: 'APP',
                            label: <Space><AppstoreOutlined/>应用 AK</Space>,
                            children: (
                                <Spin spinning={loading}>
                                    <Table
                                        dataSource={keys}
                                        columns={columns}
                                        rowKey="id"
                                        pagination={false}
                                        size="small"
                                        onRow={(record) => ({
                                            onClick: () => setDrawerAk(record),
                                            style: {cursor: 'pointer'},
                                        })}
                                        locale={{emptyText: '暂无应用 AK，点右上角"申请应用 AK"并绑定一个应用'}}
                                    />
                                </Spin>
                            ),
                        },
                    ]}
                />
            </Card>

            {/* 创建/编辑弹窗 */}
            <Modal title={editing ? '编辑 AK' : (activeTab === 'APP' ? '申请应用 AK' : '申请个人 AK')}
                   open={visible}
                   onOk={handleSubmit}
                   onCancel={() => setVisible(false)}
                   size="large"
                   okText={editing ? '保存' : '申请'}
                   destroyOnHidden>
                <Form form={form} layout="vertical" preserve={false}>
                    <Form.Item name="akType" label="AK 类型" rules={[{required: true}]}>
                        <Radio.Group disabled={!!editing}>
                            <Radio.Button value="PERSONAL">个人 AK</Radio.Button>
                            <Radio.Button value="APP">应用 AK</Radio.Button>
                        </Radio.Group>
                    </Form.Item>
                    <Form.Item shouldUpdate={(prev, curr) => prev.akType !== curr.akType} noStyle>
                        {() => form.getFieldValue('akType') === 'APP' && (
                            <Form.Item name="appCode" label="所属应用"
                                       rules={[{required: true, message: '应用 AK 必须绑定一个应用'}]}>
                                <Select
                                    showSearch
                                    optionFilterProp="label"
                                    placeholder="选择应用"
                                    options={appOptions}
                                />
                            </Form.Item>
                        )}
                    </Form.Item>
                    <Form.Item name="akName" label="名称" rules={[{required: true, message: '请输入 AK 名称'}]}>
                        <Input placeholder={activeTab === 'APP' ? '如：生产环境-订单服务' : '如：本地调试'}/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="密钥用途..."/>
                    </Form.Item>
                    {!editing && (
                        <Alert
                            type="info"
                            showIcon
                            title="AccessKey 和 SecretKey 将由系统自动生成。SecretKey 仅在创建时展示一次，请妥善保管。"
                            style={{background: '#e6f4ff', border: 'none'}}
                        />
                    )}
                </Form>
            </Modal>

            {/* SecretKey 一次性展示弹窗 */}
            <Modal
                title="AK 创建成功"
                open={!!showSkModal}
                onCancel={() => setShowSkModal(null)}
                footer={
                    <Button type="primary" onClick={() => setShowSkModal(null)}>
                        我已保存，关闭
                    </Button>
                }
                size="large"
            >
                {showSkModal && (
                    <div>
                        <Alert
                            type="warning"
                            showIcon
                            title="SecretKey 仅在此展示一次，关闭后将无法再次查看。请立即复制并妥善保管。"
                            style={{marginBottom: 16}}
                        />

                        <div style={{marginBottom: 12}}>
                            <div style={{fontSize: 12, color: '#8c8c8c', marginBottom: 4}}>AccessKey</div>
                            <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                                <code style={{
                                    flex: 1, fontFamily: 'monospace', fontSize: 14, padding: '8px 12px',
                                    background: '#f5f5f5', borderRadius: 6, border: '1px solid #d9d9d9',
                                }}>
                                    {showSkModal.accessKey}
                                </code>
                                <Button icon={<CopyOutlined/>}
                                        onClick={() => handleCopy(showSkModal.accessKey, 'AccessKey')}>复制</Button>
                            </div>
                        </div>

                        <div style={{marginBottom: 12}}>
                            <div style={{fontSize: 12, color: '#8c8c8c', marginBottom: 4}}>SecretKey</div>
                            <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                                <code style={{
                                    flex: 1, fontFamily: 'monospace', fontSize: 14, padding: '8px 12px',
                                    background: '#fff7e6', borderRadius: 6, border: '1px solid #ffd591',
                                    color: '#d46b08',
                                }}>
                                    {showSkModal.secretKey}
                                </code>
                                <Button type="primary" icon={<CopyOutlined/>}
                                        onClick={() => handleCopy(showSkModal.secretKey, 'SecretKey')}>复制</Button>
                            </div>
                        </div>

                        <div style={{fontSize: 12, color: '#ff4d4f', marginTop: 8}}>
                            ⚠ 请立即复制 SecretKey，此弹窗关闭后将无法再次获取。如遗失，请删除后重新申请。
                        </div>
                    </div>
                )}
            </Modal>

            {/* 用量详情抽屉 */}
            <AkUsageDrawer
                open={!!drawerAk}
                ak={drawerAk}
                onClose={() => setDrawerAk(null)}
            />
        </div>
    )
}

export default AkManage