import {useEffect, useState} from 'react'
import {Button, Card, Col, Form, Input, message, Modal, Row, Select, Space, Statistic, Table, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, GlobalOutlined, PlusOutlined} from '@ant-design/icons'
import request from '../../../api'
import {EmptyState, PageHeader} from '../../../ui'

const {confirm} = Modal

const LlmProvider = () => {
    const [data, setData] = useState([])
    const [loading, setSetLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const fetchProviders = async () => {
        setSetLoading(true)
        try {
            const res = await request('/llm-center/provider/list', {method: 'GET'})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setData(list)
        } catch (e) {
            message.error('加载失败')
        } finally {
            setSetLoading(false)
        }
    }

    useEffect(() => {
        fetchProviders()
    }, [])

    const handleAdd = () => {
        setEditing(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record) => {
        setEditing(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDelete = async (record) => {
        confirm({
            title: '确认删除',
            content: `删除供应商 ${record.providerName} ？`,
            onOk: async () => {
                await request('/llm-center/provider/delete', {method: 'POST', data: {id: record.id}})
                message.success('删除成功')
                fetchProviders()
            }
        })
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            await request(editing ? '/llm-center/provider/update' : '/llm-center/provider/create', {
                method: 'POST',
                data: values
            })
            message.success(editing ? '更新成功' : '创建成功')
            setModalVisible(false)
            fetchProviders()
        } catch (e) {
        }
    }

    const providerTypeColor = {
        'ollama': 'blue',
        'openai': 'green',
        'azure': 'purple',
        'anthropic': 'orange'
    }

    const columns = [
        {
            title: '供应商',
            render: (_, record) => (
                <Space>
                    <GlobalOutlined style={{fontSize: 20, color: providerTypeColor[record.providerCode] || 'blue'}}/>
                    <div>
                        <div><strong>{record.providerName}</strong></div>
                        <div style={{fontSize: 12, color: '#999'}}>{record.providerCode}</div>
                    </div>
                </Space>
            )
        },
        {
            title: '类型',
            dataIndex: 'providerType',
            render: v => <Tag color={providerTypeColor[v] || 'default'}>{v?.toUpperCase()}</Tag>
        },
        {title: 'Base URL', dataIndex: 'baseUrl', ellipsis: true},
        {title: '模型数量', dataIndex: 'modelCount', render: v => <Tag>{v || 0}</Tag>},
        {
            title: '状态',
            dataIndex: 'status',
            render: v => <Tag color={v === 'ACTIVE' ? 'green' : 'red'}>{v === 'ACTIVE' ? '正常' : '异常'}</Tag>
        },
        {
            title: '操作',
            width: 150,
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined/>} onClick={() => handleEdit(record)}>编辑</Button>
                    <Button size="small" danger icon={<DeleteOutlined/>}
                            onClick={() => handleDelete(record)}>删除</Button>
                </Space>
            )
        }
    ]

    return (
        <div>
            <PageHeader
                title="供应商管理"
                subtitle="配置 LLM 供应商接入信息，管理 API 密钥"
            />
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={6}>
                    <Card><Statistic title="供应商数" value={data.length}/></Card>
                </Col>
                <Col span={6}>
                    <Card><Statistic title="活跃模型"
                                     value={data.reduce((sum, p) => sum + (p.modelCount || 0), 0)}/></Card>
                </Col>
            </Row>

            <div style={{marginBottom: 16}}>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>添加供应商</Button>
            </div>

            {data.length === 0 && !loading ? (
                <EmptyState title="暂无供应商" description="点击上方按钮添加第一个供应商"/>
            ) : (
                <Table columns={columns} dataSource={data} loading={loading} rowKey="id"/>
            )}

            <Modal
                title={editing ? '编辑供应商' : '添加供应商'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                size="large"
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="providerCode" label="供应商编码" rules={[{required: true}]}>
                        <Input placeholder="如: ollama, openai, azure" disabled={!!editing}/>
                    </Form.Item>
                    <Form.Item name="providerName" label="供应商名称" rules={[{required: true}]}>
                        <Input placeholder="如: Ollama, OpenAI"/>
                    </Form.Item>
                    <Form.Item name="providerType" label="类型" rules={[{required: true}]}>
                        <Select>
                            <Select.Option value="ollama">Ollama</Select.Option>
                            <Select.Option value="openai">OpenAI</Select.Option>
                            <Select.Option value="azure">Azure OpenAI</Select.Option>
                            <Select.Option value="anthropic">Anthropic</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="baseUrl" label="Base URL" rules={[{required: true}]}>
                        <Input placeholder="如: http://localhost:11434"/>
                    </Form.Item>
                    <Form.Item name="apiKey" label="API Key">
                        <Input.Password placeholder="可选，部分供应商需要"/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue="ACTIVE">
                        <Select>
                            <Select.Option value="ACTIVE">正常</Select.Option>
                            <Select.Option value="INACTIVE">异常</Select.Option>
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default LlmProvider
