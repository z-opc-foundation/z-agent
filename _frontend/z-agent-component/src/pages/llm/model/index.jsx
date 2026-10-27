import {useEffect, useState} from 'react'
import {Button, Col, Form, Input, InputNumber, message, Modal, Row, Select, Space, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined} from '@ant-design/icons'
import request from '../../../api'
import { ListTable } from '@yuku123/z-frontend-common/ui'
const {confirm} = Modal

/** 后端序列化成 snake_case（model_code），页面按 camelCase 取；状态字段是 enabled(1/0)，不是 status。 */
const toModel = (m) => ({
    ...m,
    modelCode: m.modelCode ?? m.model_code,
    modelName: m.modelName ?? m.model_name,
    providerCode: m.providerCode ?? m.provider_code,
    contextWindow: m.contextWindow ?? m.context_window,
    inputPrice: m.inputPrice ?? m.input_price,
    outputPrice: m.outputPrice ?? m.output_price,
    description: m.description ?? '',
    status: (m.enabled ?? 0) === 1 ? 'ACTIVE' : 'INACTIVE',
})

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
            const res = await request('/z-llm/config/model/list', {method: 'GET'})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setData(list.map(toModel))
        } catch (e) {
            message.error('加载失败')
        } finally {
            setLoading(false)
        }
    }

    const fetchProviders = async () => {
        try {
            const res = await request('/z-llm/config/provider/list', {method: 'GET'})
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
                await request('/z-llm/config/model/delete', {method: 'POST', data: record.id})
                message.success('删除成功')
                fetchModels()
            }
        })
    }

    const handleSubmit = async () => {
        try {
            const {status, ...rest} = await form.validateFields()
            await request(editing ? '/z-llm/config/model/update' : '/z-llm/config/model/create', {
                method: 'POST',
                data: {...rest, enabled: status === 'ACTIVE' ? 1 : 0}
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
            {/* 列表页三件套由基座 ListTable 一次带齐；「添加模型」放 cardExtra，与搜索框并排。 */}
            <ListTable
                rows={data}
                columns={columns}
                loading={loading}
                rowKey="id"
                searchKeys={['modelCode', 'modelName']}
                searchPlaceholder="按模型编码 / 名称过滤"
                emptyTitle="暂无模型"
                emptyDescription="点击上方按钮添加第一个模型"
                cardExtra={
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>添加模型</Button>
                }
                cardProps={{variant: 'borderless', styles: {body: {padding: 0}}}}
            />

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
