import {useCallback, useEffect, useState} from 'react'
import {
    Button,
    Card,
    Col,
    Dropdown,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Row,
    Select,
    Space,
    Spin,
    Statistic,
    Tag,
    Tooltip
} from 'antd'
import {
    ApartmentOutlined,
    AppstoreOutlined,
    CloudOutlined,
    DatabaseOutlined,
    DeleteOutlined,
    EyeOutlined,
    FolderOutlined,
    GlobalOutlined,
    LockOutlined,
    MoreOutlined,
    PlusOutlined,
    ReloadOutlined,
    TeamOutlined
} from '@ant-design/icons'
import {ossApi} from '../../api'
import {useNavigate} from 'react-router-dom'

const ACL_LABELS = {
    'private': {label: '私有', color: 'default', icon: <LockOutlined/>},
    'public-read': {label: '公共读', color: 'blue', icon: <EyeOutlined/>},
    'public-read-write': {label: '公共读写', color: 'orange', icon: <GlobalOutlined/>},
    'authenticated-read': {label: '认证读', color: 'purple', icon: <TeamOutlined/>},
}

export default function BucketList() {
    const [buckets, setBuckets] = useState([])
    const [loading, setLoading] = useState(false)
    const [createOpen, setCreateOpen] = useState(false)
    const [createForm] = Form.useForm()
    const [creating, setCreating] = useState(false)
    const navigate = useNavigate()

    const loadBuckets = useCallback(async () => {
        setLoading(true)
        try {
            const res = await ossApi.listBuckets()
            const list = Array.isArray(res) ? res : (res?.data || [])
            setBuckets(list)
        } catch (e) {
            console.error('loadBuckets error:', e)
            message.error('加载桶列表失败：' + (e?.message || '未知错误'))
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        loadBuckets()
    }, [loadBuckets])

    const handleCreate = async () => {
        try {
            const vals = await createForm.validateFields()
            setCreating(true)
            await ossApi.createBucket(vals)
            message.success('桶创建成功')
            setCreateOpen(false)
            createForm.resetFields()
            loadBuckets()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        } finally {
            setCreating(false)
        }
    }

    const handleDelete = async (name) => {
        try {
            await ossApi.deleteBucket({params: {bucketName: name}})
            message.success('已删除')
            loadBuckets()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const handleSetAcl = async (name, acl) => {
        try {
            await ossApi.setBucketAcl({data: {acl}, params: {bucketName: name}})
            message.success('ACL 已更新')
            loadBuckets()
        } catch (e) {
            message.error('设置 ACL 失败：' + (e?.message || '未知错误'))
        }
    }

    const aclMenu = (name) => ({
        items: Object.keys(ACL_LABELS).map(acl => ({
            key: acl,
            label: ACL_LABELS[acl].label,
            icon: ACL_LABELS[acl].icon,
        })),
        onClick: ({key}) => handleSetAcl(name, key),
    })

    return (
        <div>
            {/* 顶部工具栏 */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 12,
                background: '#fff', padding: '14px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.04)', marginBottom: 16,
            }}>
                <DatabaseOutlined style={{fontSize: 18, color: '#1890ff'}}/>
                <span style={{fontSize: 16, fontWeight: 600}}>对象存储</span>
                <Tag color="blue">OSS Buckets</Tag>
                <div style={{flex: 1}}/>
                <Button icon={<ReloadOutlined/>} onClick={loadBuckets}>刷新</Button>
                <Button
                    type="primary"
                    icon={<PlusOutlined/>}
                    onClick={() => setCreateOpen(true)}
                >
                    创建桶
                </Button>
            </div>

            {/* 统计卡片 */}
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={8}>
                    <Card>
                        <Statistic title="桶总数" value={buckets.length}
                                   prefix={<ApartmentOutlined style={{color: '#1890ff'}}/>}/>
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="公共读" value={buckets.filter(b => b.acl === 'public-read').length}
                                   styles={{content: {color: '#1890ff'}}} prefix={<EyeOutlined/>}/>
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="私有" value={buckets.filter(b => !b.acl || b.acl === 'private').length}
                                   styles={{content: {color: '#999'}}} prefix={<LockOutlined/>}/>
                    </Card>
                </Col>
            </Row>

            {loading ? (
                <div style={{textAlign: 'center', padding: 80}}><Spin size="large"/></div>
            ) : buckets.length === 0 ? (
                <Empty
                    image={<CloudOutlined style={{fontSize: 64, color: '#bfbfbf'}}/>}
                    description={
                        <div>
                            <div style={{fontSize: 16, marginBottom: 8}}>还没有桶</div>
                            <div style={{color: '#999', fontSize: 13, marginBottom: 16}}>
                                创建一个桶来开始使用对象存储
                            </div>
                            <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateOpen(true)}>
                                创建第一个桶
                            </Button>
                        </div>
                    }
                    style={{padding: 80, background: '#fff', borderRadius: 10}}
                />
            ) : (
                <Row gutter={[16, 16]}>
                    {buckets.map(bucket => {
                        const acl = ACL_LABELS[bucket.acl] || ACL_LABELS.private
                        return (
                            <Col key={bucket.id || bucket.name} xs={24} sm={12} md={8} lg={6}>
                                <Card
                                    hoverable
                                    onClick={() => navigate(`/ai/oss/bucket/${encodeURIComponent(bucket.name)}`)}
                                    style={{
                                        borderRadius: 12, border: 'none',
                                        boxShadow: '0 2px 12px rgba(0,0,0,0.07)',
                                    }}
                                    styles={{body: {padding: 18}}}
                                >
                                    <div style={{display: 'flex', alignItems: 'flex-start', gap: 12}}>
                                        <div style={{
                                            width: 48, height: 48, borderRadius: 10,
                                            background: 'linear-gradient(135deg, #1890ff 0%, #36cfc9 100%)',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}>
                                            <FolderOutlined style={{fontSize: 22, color: '#fff'}}/>
                                        </div>
                                        <div style={{flex: 1, minWidth: 0}}>
                                            <div style={{
                                                fontSize: 15, fontWeight: 600, color: '#262626',
                                                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                                            }}>
                                                {bucket.name}
                                            </div>
                                            <div style={{fontSize: 12, color: '#999', marginTop: 4}}>
                                                {bucket.region || 'default'} · {bucket.createTime?.slice(0, 10) || '-'}
                                            </div>
                                        </div>
                                        <Dropdown
                                            trigger={['click']}
                                            menu={aclMenu(bucket.name)}
                                            onClick={e => e.stopPropagation()}
                                        >
                                            <Button
                                                type="text" size="small" icon={<MoreOutlined/>}
                                                onClick={e => e.stopPropagation()}
                                            />
                                        </Dropdown>
                                    </div>

                                    <div style={{marginTop: 14, display: 'flex', gap: 6, flexWrap: 'wrap'}}>
                                        <Tag color={acl.color} icon={acl.icon}>{acl.label}</Tag>
                                    </div>

                                    <div style={{
                                        marginTop: 14, paddingTop: 12, borderTop: '1px solid #f5f5f5',
                                        display: 'flex', gap: 8, justifyContent: 'space-between',
                                    }}>
                                        <Tooltip title="进入桶">
                                            <Button
                                                type="text" size="small" icon={<AppstoreOutlined/>}
                                                onClick={e => {
                                                    e.stopPropagation()
                                                    navigate(`/ai/oss/bucket/${encodeURIComponent(bucket.name)}`)
                                                }}
                                            >
                                                浏览对象
                                            </Button>
                                        </Tooltip>
                                        <Popconfirm
                                            title={`确定删除桶 "${bucket.name}"?`}
                                            description="桶必须为空才能删除"
                                            onConfirm={e => {
                                                e?.stopPropagation();
                                                handleDelete(bucket.name)
                                            }}
                                            onCancel={e => e?.stopPropagation()}
                                            okText="确认"
                                            cancelText="取消"
                                            okType="danger"
                                        >
                                            <Button
                                                type="text" size="small" danger icon={<DeleteOutlined/>}
                                                onClick={e => e.stopPropagation()}
                                            >
                                                删除
                                            </Button>
                                        </Popconfirm>
                                    </div>
                                </Card>
                            </Col>
                        )
                    })}
                </Row>
            )}

            {/* 创建桶弹窗 */}
            <Modal
                title={
                    <Space>
                        <CloudOutlined style={{color: '#1890ff'}}/>
                        <span>创建存储桶</span>
                    </Space>
                }
                open={createOpen}
                onCancel={() => {
                    setCreateOpen(false);
                    createForm.resetFields()
                }}
                onOk={handleCreate}
                confirmLoading={creating}
                okText="创建"
                cancelText="取消"
                destroyOnHidden
            >
                <Form form={createForm} layout="vertical" initialValues={{acl: 'private'}} style={{marginTop: 16}}>
                    <Form.Item
                        name="name"
                        label="桶名称"
                        rules={[
                            {required: true, message: '请输入桶名称'},
                            {
                                pattern: /^[a-z0-9][a-z0-9-]{2,62}[a-z0-9]$/,
                                message: '只能包含小写字母、数字、连字符，长度 3-63'
                            },
                        ]}
                        extra="仅支持小写字母、数字、连字符，长度 3-63 字符"
                    >
                        <Input placeholder="my-bucket" prefix={<FolderOutlined/>}/>
                    </Form.Item>
                    <Form.Item name="region" label="区域" initialValue="default">
                        <Input placeholder="default"/>
                    </Form.Item>
                    <Form.Item name="acl" label="访问权限" initialValue="private">
                        <Select
                            options={Object.entries(ACL_LABELS).map(([k, v]) => ({
                                value: k, label: <Space>{v.icon}{v.label}</Space>,
                            }))}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
