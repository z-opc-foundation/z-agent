import {useState} from 'react'
import {Alert, Button, Card, Descriptions, Input, message, Modal, Space, Steps, Tabs, Tag, Typography} from 'antd'
import {ApiOutlined, CodeOutlined, ExperimentOutlined} from '@ant-design/icons'
import {scriptApi} from '../../api'
import FieldMappingEditor from './FieldMappingEditor'

const {TextArea} = Input
const {Text} = Typography
const {Step} = Steps

export default function CurlImportModal({open, onClose, onSuccess}) {
    const [current, setCurrent] = useState(0)
    const [loading, setLoading] = useState(false)
    const [previewLoading, setPreviewLoading] = useState(false)
    const [curl, setCurl] = useState('')
    const [parsed, setParsed] = useState(null)       // 解析结果
    const [outputMapping, setOutputMapping] = useState([])
    const [previewResult, setPreviewResult] = useState(null)
    const [activeTab, setActiveTab] = useState('mapping')

    // 重置状态
    const reset = () => {
        setCurrent(0)
        setCurl('')
        setParsed(null)
        setOutputMapping([])
        setPreviewResult(null)
        setActiveTab('mapping')
    }

    const handleClose = () => {
        reset()
        onClose()
    }

    // Step 1: 解析 curl
    const handleParse = async () => {
        if (!curl.trim()) {
            message.warning('请粘贴 curl 命令')
            return
        }
        setLoading(true)
        try {
            const res = await scriptApi.importCurl({curl})
            if (res?.success) {
                setParsed(res)
                // 默认从 outputMapping 恢复，如果没有则为空
                setOutputMapping(res.outputMapping || [])
                setCurrent(1)
            } else {
                message.error(res?.message || '解析失败')
            }
        } catch (e) {
            message.error('解析失败: ' + (e?.message || '网络错误'))
        } finally {
            setLoading(false)
        }
    }

    // Step 2: 预览执行
    const handlePreview = async () => {
        setPreviewLoading(true)
        try {
            const res = await scriptApi.previewMapping({
                curl,
                outputMapping: outputMapping.filter(m => m.name && m.jsonPath),
            })
            setPreviewResult(res)
            setActiveTab('result')
        } catch (e) {
            message.error('预览失败: ' + (e?.message || '网络错误'))
        } finally {
            setPreviewLoading(false)
        }
    }

    // Step 3: 确认创建
    const handleCreate = async () => {
        setLoading(true)
        try {
            const res = await scriptApi.importCurl({
                curl,
                outputMapping: outputMapping.filter(m => m.name && m.jsonPath),
            })
            if (res?.success) {
                message.success(`脚本 [${res.scriptCode}] 创建成功`)
                reset()
                onSuccess?.()
                onClose()
            } else {
                message.error(res?.message || '创建失败')
            }
        } catch (e) {
            message.error('创建失败: ' + (e?.message || '网络错误'))
        } finally {
            setLoading(false)
        }
    }

    const requestDescr = parsed?.request?.httpRequestLine
    const headers = parsed?.request?.httpRequestHeader?.headers

    return (
        <Modal
            title={<Space><ApiOutlined/>从 Curl 导入 API Bridge 脚本</Space>}
            open={open}
            onCancel={handleClose}
            size="large"
            footer={null}
            destroyOnHidden
        >
            <Steps current={current} size="small" style={{marginBottom: 24}}>
                <Step title="粘贴 curl" description="输入 curl 命令"/>
                <Step title="配置映射" description="设置字段映射"/>
                <Step title="确认创建" description="保存脚本"/>
            </Steps>

            {/* Step 0: 粘贴 curl */}
            {current === 0 && (
                <div>
                    <p style={{color: '#666', marginBottom: 8}}>
                        粘贴任意 HTTP curl 命令，系统将自动解析并生成 API_BRIDGE 脚本
                    </p>
                    <TextArea
                        rows={8}
                        value={curl}
                        onChange={e => setCurl(e.target.value)}
                        placeholder={`curl -X POST https://api.example.com/users \\
  -H "Authorization: Bearer \${token}" \\
  -H "Content-Type: application/json" \\
  -d '{"name": "\${name}", "email": "\${email}"}'`}
                        style={{fontFamily: 'monospace', fontSize: 13, marginBottom: 12}}
                    />
                    <Button type="primary" icon={<CodeOutlined/>} onClick={handleParse} loading={loading} block>
                        解析 curl
                    </Button>
                </div>
            )}

            {/* Step 1: 配置映射 */}
            {current === 1 && parsed && (
                <div>
                    {/* 请求概览 */}
                    <Card size="small" title="请求信息" style={{marginBottom: 16}}>
                        <Descriptions column={3} size="small">
                            <Descriptions.Item label="方法"><Tag
                                color="blue">{requestDescr?.method || 'GET'}</Tag></Descriptions.Item>
                            <Descriptions.Item label="URL" span={2}>
                                <Text copyable
                                      style={{fontFamily: 'monospace', fontSize: 12}}>{requestDescr?.url}</Text>
                            </Descriptions.Item>
                        </Descriptions>
                        {headers && Object.keys(headers).length > 0 && (
                            <div style={{marginTop: 8}}>
                                <span style={{fontSize: 12, color: '#666', fontWeight: 500}}>请求头: </span>
                                {Object.entries(headers).map(([k, v]) => (
                                    <Tag key={k} style={{
                                        fontFamily: 'monospace',
                                        fontSize: 11
                                    }}>{k}: {String(v).substring(0, 40)}</Tag>
                                ))}
                            </div>
                        )}
                        {parsed.inputParams?.length > 0 && (
                            <div style={{marginTop: 8}}>
                                <span style={{fontSize: 12, color: '#666', fontWeight: 500}}>输入参数: </span>
                                {parsed.inputParams.map(p => (
                                    <Tag key={p.name} color={p.required ? 'orange' : 'default'}
                                         style={{fontFamily: 'monospace'}}>
                                        {p.name}{' '}
                                        <span style={{fontSize: 10}}>({p.in})</span>
                                    </Tag>
                                ))}
                            </div>
                        )}
                    </Card>

                    {/* 字段映射 + 预览 */}
                    <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                        {
                            key: 'mapping',
                            label: '输出字段映射',
                            children: (
                                <div>
                                    <p style={{fontSize: 12, color: '#666', marginBottom: 8}}>
                                        配置 JSONPath 从响应中抽取字段。MCP 工具只返回映射后的字段。
                                    </p>
                                    <FieldMappingEditor value={outputMapping} onChange={setOutputMapping}/>
                                </div>
                            ),
                        },
                        {
                            key: 'result',
                            label: '试运行结果',
                            children: (
                                <div>
                                    <Button icon={<ExperimentOutlined/>} onClick={handlePreview}
                                            loading={previewLoading} style={{marginBottom: 12}}>
                                        试运行并查看映射结果
                                    </Button>
                                    {previewResult && (
                                        <div>
                                            {previewResult.error ? (
                                                <Alert type="error" title={previewResult.error}
                                                       style={{marginBottom: 12}}/>
                                            ) : (
                                                <Descriptions column={3} size="small" bordered
                                                              style={{marginBottom: 12}}>
                                                    <Descriptions.Item
                                                        label="状态码">{previewResult.status}</Descriptions.Item>
                                                    <Descriptions.Item
                                                        label="耗时">{previewResult.durationMs}ms</Descriptions.Item>
                                                    <Descriptions.Item
                                                        label="响应体大小">{(previewResult.bodyRaw?.length || 0).toLocaleString()} chars</Descriptions.Item>
                                                </Descriptions>
                                            )}
                                            {previewResult.mapped && Object.keys(previewResult.mapped).length > 0 && (
                                                <Card size="small" title="映射结果" style={{marginBottom: 12}}>
                          <pre style={{fontFamily: 'monospace', fontSize: 12, margin: 0, whiteSpace: 'pre-wrap'}}>
                            {JSON.stringify(previewResult.mapped, null, 2)}
                          </pre>
                                                </Card>
                                            )}
                                            {previewResult.bodyObject && (
                                                <Card size="small" title="原始响应 (JSON)" style={{marginBottom: 12}}>
                          <pre style={{
                              fontFamily: 'monospace',
                              fontSize: 12,
                              margin: 0,
                              maxHeight: 300,
                              overflow: 'auto',
                              whiteSpace: 'pre-wrap'
                          }}>
                            {JSON.stringify(previewResult.bodyObject, null, 2)}
                          </pre>
                                                </Card>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ),
                        },
                    ]}/>
                </div>
            )}

            {/* Step 2: 确认创建 (在当前步骤合并) */}
            {current === 1 && parsed && (
                <div style={{marginTop: 16, textAlign: 'right'}}>
                    <Space>
                        <Button onClick={() => {
                            setCurrent(0);
                            setParsed(null)
                        }}>返回</Button>
                        <Button onClick={handlePreview} loading={previewLoading}>试运行</Button>
                        <Button type="primary" onClick={handleCreate} loading={loading}>
                            创建脚本
                        </Button>
                    </Space>
                </div>
            )}
        </Modal>
    )
}
