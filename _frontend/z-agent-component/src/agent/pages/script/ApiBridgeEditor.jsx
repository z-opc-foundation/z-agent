import {useCallback, useEffect, useState} from 'react'
import {Button, Card, Input, Select, Space, Switch, Table, Tag, Tooltip, Typography} from 'antd'
import {DeleteOutlined, PlusOutlined} from '@ant-design/icons'
import FieldMappingEditor from './FieldMappingEditor'

const {Text, Paragraph} = Typography

const METHOD_OPTIONS = [
    {value: 'GET', label: 'GET'},
    {value: 'POST', label: 'POST'},
    {value: 'PUT', label: 'PUT'},
    {value: 'DELETE', label: 'DELETE'},
    {value: 'PATCH', label: 'PATCH'},
    {value: 'HEAD', label: 'HEAD'},
    {value: 'OPTIONS', label: 'OPTIONS'},
]

const IN_OPTIONS = [
    {value: 'path', label: 'path'},
    {value: 'query', label: 'query'},
    {value: 'header', label: 'header'},
    {value: 'body', label: 'body'},
]

/**
 * API Bridge 结构化编辑器
 *
 * value: string (sourceCode JSON — ApiBridgeDefinition)
 * onChange: (newSourceCode: string) => void
 */
export default function ApiBridgeEditor({value, onChange, readonly = false}) {
    const [def, setDef] = useState(null)
    const [parseError, setParseError] = useState(null)

    // Parse sourceCode JSON on mount / value change
    useEffect(() => {
        if (!value) {
            setDef({
                request: {
                    httpRequestLine: {url: '', requestMethod: 'GET'},
                    httpRequestHeader: {headers: {}},
                    httpRequestBody: {body: null},
                },
                inputParams: [],
                outputMapping: [],
            })
            setParseError(null)
            return
        }
        try {
            const parsed = typeof value === 'string' ? JSON.parse(value) : value
            // Normalize structure
            if (!parsed.request) parsed.request = {}
            if (!parsed.request.httpRequestLine) parsed.request.httpRequestLine = {}
            if (!parsed.request.httpRequestHeader) parsed.request.httpRequestHeader = {}
            if (!parsed.request.httpRequestBody) parsed.request.httpRequestBody = {}
            if (!parsed.inputParams) parsed.inputParams = []
            if (!parsed.outputMapping) parsed.outputMapping = []
            setDef(parsed)
            setParseError(null)
        } catch (e) {
            setParseError('JSON 解析失败: ' + e.message)
        }
    }, [value])

    // Emit change
    const emit = useCallback((next) => {
        setDef(next)
        onChange?.(JSON.stringify(next, null, 2))
    }, [onChange])

    if (parseError) {
        return (
            <Card size="small" style={{borderColor: '#ff4d4f'}}>
                <Text type="danger">{parseError}</Text>
                <pre style={{fontFamily: 'monospace', fontSize: 12, marginTop: 8, maxHeight: 200, overflow: 'auto'}}>
          {typeof value === 'string' ? value.substring(0, 500) : String(value)}
        </pre>
            </Card>
        )
    }

    if (!def) return null

    const line = def.request?.httpRequestLine || {}
    const headers = def.request?.httpRequestHeader?.headers || {}

    // --- Request info update ---
    const updateMethod = (m) => {
        const next = JSON.parse(JSON.stringify(def))
        next.request.httpRequestLine.requestMethod = m
        emit(next)
    }

    const updateUrl = (u) => {
        const next = JSON.parse(JSON.stringify(def))
        next.request.httpRequestLine.url = u
        emit(next)
    }

    // --- Input params ---
    const updateParam = (index, field, val) => {
        const next = JSON.parse(JSON.stringify(def))
        next.inputParams[index][field] = val
        emit(next)
    }

    const removeParam = (index) => {
        const next = JSON.parse(JSON.stringify(def))
        next.inputParams = next.inputParams.filter((_, i) => i !== index)
        emit(next)
    }

    const addParam = () => {
        const next = JSON.parse(JSON.stringify(def))
        next.inputParams.push({name: '', in: 'query', required: false, description: '', defaultValue: ''})
        emit(next)
    }

    // --- Output mapping ---
    const updateOutputMapping = (list) => {
        const next = JSON.parse(JSON.stringify(def))
        next.outputMapping = list
        emit(next)
    }

    const paramColumns = [
        {
            title: '参数名',
            dataIndex: 'name',
            width: 140,
            render: (v, _, i) => readonly ? <Text code>{v}</Text> : (
                <Input size="small" value={v} onChange={e => updateParam(i, 'name', e.target.value)}
                       placeholder="param name"/>
            ),
        },
        {
            title: '位置',
            dataIndex: 'in',
            width: 100,
            render: (v, _, i) => readonly ? <Tag>{v}</Tag> : (
                <Select size="small" value={v} onChange={v2 => updateParam(i, 'in', v2)} options={IN_OPTIONS}
                        style={{width: '100%'}}/>
            ),
        },
        {
            title: '必填',
            dataIndex: 'required',
            width: 70,
            render: (v, _, i) => readonly ? (
                <span style={{color: v ? '#ff4d4f' : '#999'}}>{v ? '是' : '否'}</span>
            ) : (
                <Switch size="small" checked={v} onChange={v2 => updateParam(i, 'required', v2)}/>
            ),
        },
        {
            title: '默认值',
            dataIndex: 'defaultValue',
            width: 120,
            render: (v, _, i) => readonly ? <span>{v || '-'}</span> : (
                <Input size="small" value={v || ''} onChange={e => updateParam(i, 'defaultValue', e.target.value)}
                       placeholder="default"/>
            ),
        },
        {
            title: '描述',
            dataIndex: 'description',
            render: (v, _, i) => readonly ? <span style={{color: '#666'}}>{v}</span> : (
                <Input size="small" value={v} onChange={e => updateParam(i, 'description', e.target.value)}
                       placeholder="描述"/>
            ),
        },
        ...(!readonly ? [{
            title: '',
            key: 'action',
            width: 50,
            render: (_, __, i) => (
                <Tooltip title="删除">
                    <Button size="small" type="text" danger icon={<DeleteOutlined/>} onClick={() => removeParam(i)}/>
                </Tooltip>
            ),
        }] : []),
    ]

    return (
        <div>
            {/* Request Info */}
            <Card size="small" title="请求配置" style={{marginBottom: 16}}>
                <Space style={{width: '100%'}} orientation="vertical" size="small">
                    <Space align="center" style={{width: '100%'}}>
                        {readonly ? (
                            <Tag color="blue"
                                 style={{fontSize: 14, padding: '2px 12px'}}>{line.requestMethod || 'GET'}</Tag>
                        ) : (
                            <Select
                                value={line.requestMethod || 'GET'}
                                onChange={updateMethod}
                                options={METHOD_OPTIONS}
                                style={{width: 120}}
                            />
                        )}
                        {readonly ? (
                            <Text code style={{fontSize: 13, wordBreak: 'break-all'}}>{line.url}</Text>
                        ) : (
                            <Input
                                value={line.url || ''}
                                onChange={e => updateUrl(e.target.value)}
                                placeholder="https://api.example.com/resource/{id}?key=${key}"
                                style={{fontFamily: 'monospace', flex: 1}}
                            />
                        )}
                    </Space>
                    {Object.keys(headers).length > 0 && (
                        <div>
                            <Text type="secondary" style={{fontSize: 12}}>请求头: </Text>
                            {Object.entries(headers).map(([k, v]) => (
                                <Tag key={k} style={{fontFamily: 'monospace', fontSize: 11}}>
                                    {k}: {String(v).substring(0, 50)}
                                </Tag>
                            ))}
                        </div>
                    )}
                </Space>
            </Card>

            {/* Input Params */}
            <Card
                size="small"
                title={`输入参数 (${def.inputParams?.length || 0})`}
                style={{marginBottom: 16}}
                extra={!readonly && (
                    <Button size="small" type="link" icon={<PlusOutlined/>} onClick={addParam}>添加</Button>
                )}
            >
                <Table
                    dataSource={def.inputParams || []}
                    columns={paramColumns}
                    rowKey={(_, i) => i}
                    pagination={false}
                    size="small"
                    locale={{emptyText: '暂无输入参数'}}
                />
            </Card>

            {/* Output Mapping */}
            <Card size="small" title={`输出字段映射 (${def.outputMapping?.length || 0})`}>
                <FieldMappingEditor
                    value={def.outputMapping || []}
                    onChange={updateOutputMapping}
                    readonly={readonly}
                />
            </Card>
        </div>
    )
}
