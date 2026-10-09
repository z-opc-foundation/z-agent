import {useEffect, useState} from 'react'
import {Button, Col, Form, Input, InputNumber, message, Modal, Row, Select, Space, Table, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined} from '@ant-design/icons'
import request from '../../../api'
import {EmptyState, PageHeader} from '../../../ui'

const {confirm} = Modal

const LlmModel = () => {
    const [data, setData] = useState([])
    const [providers, setProviders] = useState([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const fetchModels = async () => {
        setLoading(true)
        try {
            const res = await request('/llm-center/model/list', {method: 'GET'})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setData(list)
        } catch (e) {
            message.error('加载失败')
        } finally {
            setLoading(false)
        }
    }

    const fetchProviders = async () => {
        try {
            const res = await request('/llm-center/provider/list', {method: 'GET'})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setProviders(list)
        } catch (e) {
        }
    }

    useEffect(() => {
        fetchModels()
        fetchProviders()
    }, [])

    const handleAdd = () => {
        setEditing(null)
        form.resetFields()
        form.setFieldsValue({status: 'ACTIVE', contextWindow: 4096})
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
            content: `删除模型 ${record.modelName} ？`,
            onOk: async () => {
                await request('/llm-center/model/delete', {method: 'POST', data: {id: record.id}})
                message.success('删除成功')
                fetchModels()
            }
        })
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            await request(editing ? '/llm-center/model/update' : '/llm-center/model/create', {
                method: 'POST',
                data: values
            })
            message.success(editing ? '更新成功' : '创建成功')
            setModalVisible(false)
            fetchModels()
        } catch (e) {
        }
    }

    const columns = [
        {title: '模型编码', dataIndex: 'modelCode', width: 120},
        {title: '模型名称', dataIndex: 'modelName', width: 150},
        {
            title: '供应商',
            dataIndex: 'providerCode',
            render: v => <Tag>{v}</Tag>
        },
        {title: '上下文窗口', dataIndex: 'contextWindow', render: v => v ? `${v.toLocaleString()} tokens` : '-'},
        {
            title: '输入价格',
            dataIndex: 'inputPrice',
            render: v => v ? `¥${v}/千token` : '-'
        },
        {
            title: '输出价格',
            dataIndex: 'outputPrice',
            render: v => v ? `¥${v}/千token` : '-'
        },
        {
            title: '状态',
            dataIndex: 'status',
            render: v => <Tag color={v === 'ACTIVE' ? 'green' : 'red'}>{v === 'ACTIVE' ? '正常' : '禁用'}</Tag>
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
                title="模型管理"
                subtitle="管理已接入的 LLM 模型，配置参数和价格"
            />
            <div style={{marginBottom: 16}}>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>添加模型</Button>
            </div>

            {data.length === 0 && !loading ? (
                <EmptyState title="暂无模型" description="点击上方按钮添加第一个模型"/>
            ) : (
                <Table columns={columns} dataSource={data} loading={loading} rowKey="id"/>
            )}

            <Modal
                title={editing ? '编辑模型' : '添加模型'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                size="large"
            >
                <Form form={form} layout="vertical">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="modelCode" label="模型编码" rules={[{required: true}]}>
                                <Input placeholder="如: qwen2.5, gpt-4o" disabled={!!editing}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="modelName" label="模型名称" rules={[{required: true}]}>
                                <Input placeholder="如: Qwen 2.5 72B"/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="providerCode" label="供应商" rules={[{required: true}]}>
                        <Select placeholder="选择供应商">
                            {providers.map(p => (
                                <Select.Option key={p.providerCode}
                                               value={p.providerCode}>{p.providerName}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="contextWindow" label="上下文窗口 (tokens)">
                                <InputNumber style={{width: '100%'}} placeholder="如: 4096"/>
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
                                <InputNumber style={{width: '100%'}} min={0} precision={6} placeholder="如: 0.001"/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="outputPrice" label="输出价格 (元/千token)">
                                <InputNumber style={{width: '100%'}} min={0} precision={6} placeholder="如: 0.002"/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="模型描述..."/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default LlmModel
