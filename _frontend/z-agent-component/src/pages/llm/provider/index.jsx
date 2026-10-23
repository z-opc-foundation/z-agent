import {useEffect, useState} from 'react'
import {Button, Card, Col, Form, Input, message, Modal, Row, Select, Space, Statistic, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, GlobalOutlined, PlusOutlined} from '@ant-design/icons'
import request from '../../../api'
import {ListTable, PageHeader} from '@yuku123/z-frontend-common/ui'

const {confirm} = Modal

/** 后端序列化成 snake_case，页面按 camelCase 取；状态字段是 enabled(1/0)。 */
const toModel = (p) => ({
    ...p,
    providerCode: p.providerCode ?? p.provider_code,
    providerName: p.providerName ?? p.provider_name,
    baseUrl: p.baseUrl ?? p.base_url,
    apiKey: p.apiKey ?? p.api_key,
    providerType: p.providerType ?? p.provider_type,
    priority: p.priority ?? 100,
    status: (p.enabled ?? 0) === 1 ? 'ACTIVE' : 'INACTIVE',
})

const LlmProvider = () => {
    const [data, setData] = useState([])
    const [loading, setSetLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const fetchProviders = async () => {
        setSetLoading(true)
        try {
            const res = await request('/z-llm/config/provider/list', {method: 'GET'})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setData(list.map(toModel))
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
                await request('/z-llm/config/provider/delete', {method: 'POST', data: record.id})
                message.success('删除成功')
                fetchProviders()
            }
        })
    }

    const handleSubmit = async () => {
        try {
            const {status, ...rest} = await form.validateFields()
            await request(editing ? '/z-llm/config/provider/update' : '/z-llm/config/provider/create', {
                method: 'POST',
                data: {...rest, enabled: status === 'ACTIVE' ? 1 : 0}
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
            <PageHeader title="供应商管理"/>
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={6}>
                    <Card><Statistic title="供应商数" value={data.length}/></Card>
                </Col>
                <Col span={6}>
                    <Card><Statistic title="活跃模型"
                                     value={data.reduce((sum, p) => sum + (p.modelCount || 0), 0)}/></Card>
                </Col>
            </Row>

            {/* 列表页三件套（分页 + 查询 + EmptyState）由基座 ListTable 一次带齐；
                「添加供应商」放 cardExtra，与搜索框并排（lead 008 §2）。 */}
            <ListTable
                rows={data}
                columns={columns}
                loading={loading}
                rowKey="id"
                searchKeys={['providerName', 'providerCode']}
                searchPlaceholder="按供应商名称 / 编码过滤"
                emptyTitle="暂无供应商"
                emptyDescription="点击上方按钮添加第一个供应商"
                cardExtra={
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>添加供应商</Button>
                }
                cardProps={{variant: 'borderless', styles: {body: {padding: 0}}}}
            />

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
