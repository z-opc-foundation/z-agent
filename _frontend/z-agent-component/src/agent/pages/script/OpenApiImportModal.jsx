import {useState} from 'react'
import {Alert, Button, Card, Input, message, Modal, Progress, Space, Table, Tag} from 'antd'
import {ApiOutlined, FileTextOutlined, UploadOutlined} from '@ant-design/icons'
import {scriptApi} from '../../api'

const {TextArea} = Input

export default function OpenApiImportModal({open, onClose, onSuccess}) {
    const [jsonContent, setJsonContent] = useState('')
    const [parsedItems, setParsedItems] = useState(null)    // 前端预览
    const [importResult, setImportResult] = useState(null)   // 后端返回
    const [loading, setLoading] = useState(false)

    const reset = () => {
        setJsonContent('')
        setParsedItems(null)
        setImportResult(null)
    }

    const handleClose = () => {
        reset()
        onClose()
    }

    // 前端预解析展示预览
    const handlePreview = () => {
        if (!jsonContent.trim()) {
            message.warning('请粘贴 OpenAPI JSON')
            return
        }
        try {
            const spec = JSON.parse(jsonContent)
            const paths = spec.paths
            if (!paths) {
                message.warning('OpenAPI 中没有 paths 段')
                return
            }
            const items = []
            for (const path of Object.keys(paths)) {
                const methods = paths[path]
                for (const method of ['get', 'post', 'put', 'delete', 'patch']) {
                    const op = methods[method]
                    if (!op) continue
                    items.push({
                        key: `${method}_${path}`,
                        method: method.toUpperCase(),
                        path,
                        summary: op.summary || op.operationId || '',
                        operationId: op.operationId || '-',
                    })
                }
            }
            setParsedItems(items)
            if (items.length === 0) {
                message.info('未找到可解析的接口')
            } else {
                message.success(`解析到 ${items.length} 个接口`)
            }
        } catch (e) {
            message.error('JSON 解析失败: ' + e.message)
        }
    }

    // 导入到后端
    const handleImport = async () => {
        if (!jsonContent.trim()) return
        setLoading(true)
        try {
            const res = await scriptApi.importOpenApi({openApiJson: jsonContent})
            if (res?.success) {
                setImportResult(res)
                message.success(`成功导入 ${res.count} 个脚本`)
            } else {
                message.error(res?.message || '导入失败')
            }
        } catch (e) {
            message.error('导入失败: ' + (e?.message || '网络错误'))
        } finally {
            setLoading(false)
        }
    }

    const columns = [
        {
            title: '方法', dataIndex: 'method', key: 'method', width: 80,
            render: (v) => {
                const colorMap = {GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red', PATCH: 'purple'}
                return <Tag color={colorMap[v] || 'default'}>{v}</Tag>
            },
        },
        {
            title: '路径', dataIndex: 'path', key: 'path', width: 280,
            render: (v) => <code style={{fontSize: 12}}>{v}</code>,
        },
        {title: '描述', dataIndex: 'summary', key: 'summary', ellipsis: true},
        {
            title: 'OperationId', dataIndex: 'operationId', key: 'operationId', width: 160,
            render: (v) => <Tag style={{fontFamily: 'monospace'}}>{v}</Tag>,
        },
    ]

    const resultColumns = [
        {
            title: '方法', dataIndex: 'method', key: 'method', width: 80,
            render: (v) => {
                const colorMap = {GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red', PATCH: 'purple'}
                return <Tag color={colorMap[v] || 'default'}>{v}</Tag>
            },
        },
        {
            title: '路径', dataIndex: 'path', key: 'path', width: 280,
            render: (v) => <code style={{fontSize: 12}}>{v}</code>,
        },
        {
            title: '脚本编码', dataIndex: 'scriptCode', key: 'scriptCode', width: 200,
            render: (v) => <Tag color="blue" style={{fontFamily: 'monospace'}}>{v}</Tag>,
        },
        {title: '脚本名称', dataIndex: 'scriptName', key: 'scriptName', ellipsis: true},
        {
            title: '结果', dataIndex: 'created', key: 'created', width: 80,
            render: (v) => v ? <Tag color="green">成功</Tag> : <Tag color="red">跳过</Tag>,
        },
    ]

    return (
        <Modal
            title={<Space><ApiOutlined/>从 OpenAPI 批量导入脚本</Space>}
            open={open}
            onCancel={handleClose}
            size="large"
            footer={null}
            destroyOnHidden
        >
            {/* 上传区域 */}
            {!parsedItems && !importResult && (
                <div>
                    <p style={{color: '#666', marginBottom: 8}}>
                        粘贴 OpenAPI 3.0 / Swagger 2.0 规范的 JSON 内容，系统将自动为每个端点生成 API_BRIDGE 脚本。
                    </p>
                    <TextArea
                        rows={12}
                        value={jsonContent}
                        onChange={e => setJsonContent(e.target.value)}
                        placeholder={`{\n  "openapi": "3.0.0",\n  "info": {...},\n  "paths": {\n    "/users": {\n      "get": {\n        "operationId": "listUsers",\n        "summary": "获取用户列表",\n        ...\n      }\n    }\n  }\n}`}
                        style={{fontFamily: 'monospace', fontSize: 13, marginBottom: 12}}
                    />
                    <Button type="primary" icon={<FileTextOutlined/>} onClick={handlePreview} block>
                        解析预览
                    </Button>
                </div>
            )}

            {/* 预览列表 */}
            {parsedItems && !importResult && (
                <div>
                    <Alert
                        type="info"
                        title={`共解析到 ${parsedItems.length} 个接口，确认后将批量导入为 API_BRIDGE 脚本`}
                        style={{marginBottom: 12}}
                        showIcon
                    />
                    <Table
                        dataSource={parsedItems}
                        columns={columns}
                        pagination={{pageSize: 10}}
                        size="small"
                        bordered
                        rowKey="key"
                    />
                    <div style={{marginTop: 16, textAlign: 'right'}}>
                        <Space>
                            <Button onClick={() => {
                                setParsedItems(null);
                                setJsonContent('')
                            }}>返回修改</Button>
                            <Button type="primary" icon={<UploadOutlined/>} onClick={handleImport} loading={loading}>
                                导入 {parsedItems.length} 个脚本
                            </Button>
                        </Space>
                    </div>
                </div>
            )}

            {/* 导入结果 */}
            {importResult && (
                <div>
                    <Card style={{marginBottom: 12, textAlign: 'center'}}>
                        <Progress type="circle" percent={100} format={() => `${importResult.count}`} size="large"/>
                        <div style={{marginTop: 8, fontWeight: 500}}>成功导入 {importResult.count} 个脚本</div>
                    </Card>
                    <Table
                        dataSource={importResult.items || []}
                        columns={resultColumns}
                        pagination={{pageSize: 10}}
                        size="small"
                        bordered
                        rowKey="scriptCode"
                    />
                    <div style={{marginTop: 16, textAlign: 'right'}}>
                        <Space>
                            <Button onClick={() => {
                                reset();
                                onSuccess?.();
                                onClose()
                            }}>完成</Button>
                        </Space>
                    </div>
                </div>
            )}
        </Modal>
    )
}
