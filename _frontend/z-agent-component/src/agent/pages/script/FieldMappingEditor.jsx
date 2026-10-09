import {Button, Input, Select, Space, Switch, Table, Tooltip} from 'antd'
import {ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined} from '@ant-design/icons'

const TYPE_OPTIONS = [
    {value: 'string', label: 'string'},
    {value: 'number', label: 'number'},
    {value: 'boolean', label: 'boolean'},
    {value: 'object', label: 'object'},
    {value: 'array', label: 'array'},
]

/**
 * 字段映射编辑器
 * value: Array<{ name, type, jsonPath, required, description }>
 * onChange: (newList) => void
 */
export default function FieldMappingEditor({value = [], onChange, readonly = false}) {
    const update = (index, field, val) => {
        const next = [...value]
        next[index] = {...next[index], [field]: val}
        onChange(next)
    }

    const remove = (index) => {
        const next = value.filter((_, i) => i !== index)
        onChange(next)
    }

    const move = (index, dir) => {
        const target = index + dir
        if (target < 0 || target >= value.length) return
        const next = [...value]
        const tmp = next[index]
        next[index] = next[target]
        next[target] = tmp
        onChange(next)
    }

    const add = () => {
        onChange([...value, {name: '', type: 'string', jsonPath: '', required: false, description: ''}])
    }

    const columns = [
        {
            title: '输出字段名',
            dataIndex: 'name',
            width: 150,
            render: (_v, _r, i) => readonly ? <span style={{fontFamily: 'monospace'}}>{_v}</span> : (
                <Input size="small" value={_v} onChange={e => update(i, 'name', e.target.value)}
                       placeholder="e.g. user_name"/>
            ),
        },
        {
            title: '类型',
            dataIndex: 'type',
            width: 100,
            render: (_v, _r, i) => readonly ? <span>{_v}</span> : (
                <Select size="small" value={_v} onChange={v => update(i, 'type', v)} options={TYPE_OPTIONS}
                        style={{width: '100%'}}/>
            ),
        },
        {
            title: 'JSONPath',
            dataIndex: 'jsonPath',
            width: 220,
            render: (_v, _r, i) => readonly ? <span style={{fontFamily: 'monospace', color: '#0958d9'}}>{_v}</span> : (
                <Input size="small" value={_v} onChange={e => update(i, 'jsonPath', e.target.value)}
                       placeholder="$.data.user.name" style={{fontFamily: 'monospace'}}/>
            ),
        },
        {
            title: '必填',
            dataIndex: 'required',
            width: 60,
            render: (_v, _r, i) => readonly ? (
                <span style={{color: _v ? '#ff4d4f' : '#999'}}>{_v ? '是' : '否'}</span>
            ) : (
                <Switch size="small" checked={_v} onChange={v => update(i, 'required', v)}/>
            ),
        },
        {
            title: '描述',
            dataIndex: 'description',
            width: 160,
            render: (_v, _r, i) => readonly ? <span style={{color: '#666'}}>{_v}</span> : (
                <Input size="small" value={_v} onChange={e => update(i, 'description', e.target.value)}
                       placeholder="字段说明"/>
            ),
        },
        ...(readonly ? [] : [{
            title: '操作',
            key: 'action',
            width: 100,
            render: (_v, _r, i) => (
                <Space size="small">
                    <Tooltip title="上移"><Button size="small" type="text" icon={<ArrowUpOutlined/>} disabled={i === 0}
                                                  onClick={() => move(i, -1)}/></Tooltip>
                    <Tooltip title="下移"><Button size="small" type="text" icon={<ArrowDownOutlined/>}
                                                  disabled={i === value.length - 1}
                                                  onClick={() => move(i, 1)}/></Tooltip>
                    <Tooltip title="删除"><Button size="small" type="text" danger icon={<DeleteOutlined/>}
                                                  onClick={() => remove(i)}/></Tooltip>
                </Space>
            ),
        }]),
    ]

    return (
        <div>
            <Table
                dataSource={value}
                columns={columns}
                rowKey={(_, i) => i}
                pagination={false}
                size="small"
                bordered
                style={{marginBottom: 8}}
            />
            {!readonly && (
                <Button size="small" type="dashed" icon={<PlusOutlined/>} onClick={add} block>
                    添加字段映射
                </Button>
            )}
            {value.length === 0 && !readonly && (
                <div style={{textAlign: 'center', color: '#999', padding: 12}}>
                    暂未配置输出字段映射。发布后 MCP 工具将返回原始响应体。
                </div>
            )}
        </div>
    )
}
