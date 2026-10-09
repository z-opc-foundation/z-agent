/**
 * FEATURE051 - z-script 产品化中心页面
 * 包含 5 个 Tab:
 *   - API Key 台账（**只读**，写入口已摘，理由见 ApiKeyTab 上方注释与 TASK-20260925-031）
 *   - 调用监控
 *   - 版本管理
 *   - 标签管理
 *   - 配额管理
 */
import {useEffect, useState} from 'react'
import {
    Button,
    Card,
    Empty,
    Input,
    message,
    Progress,
    Select,
    Space,
    Table,
    Tabs,
    Tag,
    Tooltip
} from 'antd'
import {
    CloudUploadOutlined,
    KeyOutlined,
    LineChartOutlined,
    NumberOutlined,
    PlusOutlined,
    ReloadOutlined,
    TagsOutlined
} from '@ant-design/icons'
import {PageHeader} from '../../ui'
import {App as AntApp} from 'antd'

const useMessage = () => AntApp.useApp().message

const API_BASE = '/api'

// 本文件刻意用裸 fetch 而不是 @/common 的 axios 实例：axios 拦截器把任何 401 当成"会话过期"，
// 会清 localStorage 并跳 /login（见 common/utils/request.ts 的 redirectToLogin）。
// z-script 的 ApiKeyAuthInterceptor 对被拦截的 /api/script/** 回的正是 401 MISSING_API_KEY，
// 走 axios 的页面一点进来就被踢下线。
//
// 字段名一律 snake_case：main-starter 的全局 JacksonConfig 对实体属性生效（实测
// /api/script/invoke-log/page 与 /api/script/api-key/page 的 key 都是 api_key_id / cost_ms 这种形状），
// 而 MyBatis-Plus 分页体里的 total / pages 是**字符串**，参与算术前必须 Number()。
const stamp = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')
const n = (v) => (v === null || v === undefined || v === '' ? 0 : Number(v) || 0)
const text = (v) => (v === null || v === undefined || v === '' ? '-' : String(v))

/** /api/script/api-key/page 的实测口径：current/size 生效，但 total 恒 "0"（未开 count），所以行数只认 records。 */
const pageRecords = (r) => (r && r.data && Array.isArray(r.data.records) ? r.data.records : [])

async function http(method, url, body) {
    const res = await fetch(API_BASE + url, {
        method,
        headers: {'Content-Type': 'application/json', 'X-User-Id': localStorage.getItem('userId') || 'admin'},
        body: body ? JSON.stringify(body) : undefined
    })
    return res.json()
}

function ApiKeyTab() {
    const [list, setList] = useState([])
    const [loading, setLoading] = useState(false)

    const load = async () => {
        setLoading(true)
        try {
            // 原先打的是 /script/api-key/list —— 这个路径不存在，会被 @GetMapping("/{id}") 吃掉
            // (id="list") ⇒ 500 MethodArgumentTypeMismatch，表格永远空。实测可用的是 /page。
            const r = await http('GET', '/script/api-key/page?current=1&size=200')
            setList(pageRecords(r))
        } finally {
            setLoading(false)
        }
    }
    useEffect(() => {
        load()
    }, [])

    // 🔴 写入口（创建 / 启停 / 删除 API Key）本轮从这页**摘掉**了，不是漏做：
    //   ① `ApiKeyController` 全文没有任何角色或归属检查（TASK-20260925-031，P0）——
    //      合并进程里"能登录平台"就等于"能增删任意应用的凭据"；
    //   ② `DELETE /api/script/api-key/{id}` 打掉的是一把**在用**的 `zsk_live_…`，
    //      消费方应用会当场调不通，且这页拿不到归属信息去拦"这不是你的 key"；
    //   ③ 本轮把 `/script/keys` 接进**运维面**菜单，是这些按钮第一次出现在运维入口上 ——
    //      暴露面是我加的，所以摘掉也是我的责任，与 z-mist 那轮"写入口全部摘掉"同一条判断。
    //   修法在 z-script 侧（出口 DTO 脱敏 + 读端点按角色/归属校验），修好之后这些按钮该由**带鉴权的那一面**长回来，
    //   而不是在 z-opc 这边先接上。
    return (
        <Card>
            <Space style={{marginBottom: 16}}>
                <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                <span style={{color: '#999'}}>本页只读：创建/启停/删除 API Key 需要 z-script 侧的归属校验先落地（TASK-20260925-031）</span>
            </Space>
            <Table
                rowKey="id" loading={loading} dataSource={list}
                columns={[
                    {title: 'ID', dataIndex: 'id', width: 60},
                    {title: '应用名', dataIndex: 'app_name'},
                    // ⚠️ 这里刻意**不渲染** api_key / api_secret_hash 两列。
                    // 接口本身仍在往浏览器里发明文 key（TASK-20260925-031，P0，属 L3 侧要改成 DTO 的事），
                    // 但把它在管理页上打印出来不是修复，只是把泄漏做得"像个功能"。
                    // 需要定位某把 key 时用 ID + 应用名，二者足够且不扩大外泄面。
                    {title: 'Scope', dataIndex: 'scope', render: v => v ? <Tag color="blue">{v}</Tag> : '-'},
                    {
                        title: '状态',
                        dataIndex: 'status',
                        render: v => v === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>
                    },
                    {title: '调用次数', dataIndex: 'total_calls', render: v => text(v)},
                    {title: '最后使用', dataIndex: 'last_used_at', render: v => stamp(v)},
                    {title: '过期时间', dataIndex: 'expire_at', render: v => stamp(v)}
                ]}
            />
        </Card>
    )
}

function MonitorTab() {
    const [logs, setLogs] = useState([])
    const [stats, setStats] = useState({total: 0, success: 0, failed: 0})
    const [loading, setLoading] = useState(false)
    const [filter, setFilter] = useState({apiKeyId: undefined, success: undefined})
    const [trend, setTrend] = useState([])
    const [trendDays, setTrendDays] = useState(7)

    const load = async () => {
        setLoading(true)
        try {
            const params = new URLSearchParams()
            if (filter.apiKeyId) params.append('apiKeyId', filter.apiKeyId)
            if (filter.success !== undefined && filter.success !== '') params.append('success', filter.success)
            params.append('pageSize', '50')
            const r = await http('GET', '/script/invoke-log/page?' + params)
            const list = r.data?.records || []
            setLogs(list)
            const success = list.filter(x => x.success === 1).length
            setStats({total: list.length, success, failed: list.length - success})
        } finally {
            setLoading(false)
        }
    }

    const loadTrend = async () => {
        const params = new URLSearchParams()
        params.append('days', trendDays)
        if (filter.apiKeyId) params.append('apiKeyId', filter.apiKeyId)
        const r = await http('GET', '/script/invoke-log/stats/trend?' + params)
        setTrend(r.data || [])
    }

    useEffect(() => {
        load()
    }, [])
    useEffect(() => {
        loadTrend()
    }, [trendDays, filter.apiKeyId])

    // 渲染简易 SVG 折线图
    const renderTrendChart = () => {
        if (!trend.length) return <Empty description="暂无数据"/>
        const width = 700, height = 200, padding = 30
        const maxVal = Math.max(1, ...trend.map(p => n(p.total)))
        const xStep = (width - padding * 2) / Math.max(1, trend.length - 1)

        const pointStr = (key) => trend.map((p, i) => {
            const x = padding + i * xStep
            const y = height - padding - (n(p[key]) / maxVal) * (height - padding * 2)
            return `${x},${y}`
        }).join(' ')

        return (
            <svg width={width} height={height} style={{background: '#fafafa', borderRadius: 4}}>
                {/* 网格 */}
                {[0, 0.25, 0.5, 0.75, 1].map((p, i) => (
                    <line key={i} x1={padding} y1={height - padding - p * (height - padding * 2)}
                          x2={width - padding} y2={height - padding - p * (height - padding * 2)}
                          stroke="#e0e0e0" strokeDasharray="2,2"/>
                ))}
                {/* 总数线 (蓝色) */}
                <polyline points={pointStr('total')} fill="none" stroke="#1890ff" strokeWidth="2"/>
                {/* 成功线 (绿色) */}
                <polyline points={pointStr('success')} fill="none" stroke="#52c41a" strokeWidth="2"/>
                {/* 失败线 (红色) */}
                <polyline points={pointStr('failed')} fill="none" stroke="#f5222d" strokeWidth="2"/>
                {/* X 轴标签 */}
                {trend.map((p, i) => (
                    <text key={i} x={padding + i * xStep} y={height - 8}
                          fontSize="10" textAnchor="middle" fill="#666">
                        {p.date.slice(5)}
                    </text>
                ))}
                {/* 数据点 */}
                {trend.map((p, i) => (
                    <g key={i}>
                        <circle cx={padding + i * xStep}
                                cy={height - padding - (p.total / maxVal) * (height - padding * 2)}
                                r="3" fill="#1890ff"/>
                    </g>
                ))}
            </svg>
        )
    }

    return (
        <Card>
            <Space style={{marginBottom: 16}} wrap>
                <Select placeholder="API Key ID" allowClear style={{width: 200}}
                        onChange={v => setFilter(s => ({...s, apiKeyId: v}))}/>
                <Select placeholder="成功/失败" allowClear style={{width: 200}}
                        onChange={v => setFilter(s => ({...s, success: v}))} options={[
                    {value: 1, label: '成功'}, {value: 0, label: '失败'}
                ]}/>
                <Select value={trendDays} onChange={setTrendDays} style={{width: 120}} options={[
                    {value: 7, label: '近 7 天'},
                    {value: 14, label: '近 14 天'},
                    {value: 30, label: '近 30 天'}
                ]}/>
                <Button type="primary" icon={<ReloadOutlined/>} onClick={() => {
                    load();
                    loadTrend()
                }}>刷新</Button>
            </Space>

            {/* 统计卡片 */}
            <Card type="inner" style={{marginBottom: 16, background: '#f5f5f5'}}>
                <Space size="large" wrap>
                    <span>📊 总调用: <b style={{fontSize: 18}}>{stats.total}</b></span>
                    <span style={{color: '#52c41a'}}>✅ 成功: <b style={{fontSize: 18}}>{stats.success}</b></span>
                    <span style={{color: '#f5222d'}}>❌ 失败: <b style={{fontSize: 18}}>{stats.failed}</b></span>
                    <span>📈 成功率: <b style={{
                        fontSize: 18,
                        color: stats.success / Math.max(1, stats.total) > 0.9 ? '#52c41a' : '#faad14'
                    }}>
                        {stats.total ? ((stats.success / stats.total) * 100).toFixed(1) : 0}%
                    </b></span>
                    <span>📅 {trendDays} 天趋势: <b style={{fontSize: 18, color: '#1890ff'}}>
                        {trend.reduce((acc, p) => acc + n(p.total), 0)} 次
                    </b></span>
                </Space>
            </Card>

            {/* 趋势图 (SVG) */}
            <Card type="inner" title="📈 调用趋势 (蓝=总 / 绿=成功 / 红=失败)" style={{marginBottom: 16}}>
                {renderTrendChart()}
                <div style={{marginTop: 8, textAlign: 'center'}}>
                    <Space>
                        <span><span style={{
                            display: 'inline-block',
                            width: 12,
                            height: 2,
                            background: '#1890ff',
                            marginRight: 4
                        }}></span>总数</span>
                        <span><span style={{
                            display: 'inline-block',
                            width: 12,
                            height: 2,
                            background: '#52c41a',
                            marginRight: 4
                        }}></span>成功</span>
                        <span><span style={{
                            display: 'inline-block',
                            width: 12,
                            height: 2,
                            background: '#f5222d',
                            marginRight: 4
                        }}></span>失败</span>
                    </Space>
                </div>
            </Card>

            {/* 调用日志表 */}
            <Table rowKey="id" loading={loading} dataSource={logs} pagination={{pageSize: 20}}
                   columns={[
                       {title: '时间', dataIndex: 'create_time', width: 170, render: v => stamp(v)},
                       {title: 'API Key ID', dataIndex: 'api_key_id', width: 100, render: v => text(v)},
                       {title: '应用', dataIndex: 'app_name', render: v => text(v)},
                       {title: '脚本', dataIndex: 'script_code', render: v => text(v)},
                       {title: '方法', dataIndex: 'http_method', width: 80, render: v => text(v)},
                       {title: '状态', dataIndex: 'status_code', width: 80, render: v => text(v)},
                       {title: '耗时(ms)', dataIndex: 'cost_ms', width: 100, render: v => text(v)},
                       {
                           title: '结果',
                           dataIndex: 'success',
                           width: 80,
                           render: v => v === 1 ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>
                       },
                       {title: 'IP', dataIndex: 'client_ip', render: v => text(v)},
                       {title: '错误', dataIndex: 'error_message', ellipsis: true, render: v => text(v)}
                   ]}
            />
        </Card>
    )
}

function VersionTab() {
    const [versions, setVersions] = useState([])
    const [loading, setLoading] = useState(false)
    const [scriptId, setScriptId] = useState(null)
    const [scripts, setScripts] = useState([])

    // 候选脚本来自 z-opc 侧的 /api/script-admin/scripts：z-script 自家的 /api/script/list
    // 在它自己的 ApiKeyAuthInterceptor 名单里（带平台会话仍 401），前端拿不到脚本主表 ID，
    // 而 /api/script/version/list/{scriptId} 的唯一入参就是这个 ID。见 TASK-20260925-032。
    useEffect(() => {
        (async () => {
            const r = await http('GET', '/script-admin/scripts')
            const list = Array.isArray(r.data) ? r.data : []
            setScripts(list)
            if (list.length) setScriptId(Number(list[0].id))
        })()
    }, [])

    const load = async () => {
        if (!scriptId) {
            setVersions([]);
            return
        }
        setLoading(true)
        try {
            const r = await http('GET', '/script/version/list/' + scriptId)
            setVersions(r.data || [])
        } finally {
            setLoading(false)
        }
    }
    useEffect(() => {
        load()
    }, [scriptId])

    return (
        <Card>
            <Space style={{marginBottom: 16}}>
                <Select style={{width: 360}} placeholder="选择脚本" value={scriptId}
                        onChange={setScriptId}
                        options={scripts.map(s => ({
                            value: Number(s.id),
                            label: `#${s.id} ${text(s.script_code)}${s.script_name ? ' · ' + s.script_name : ''}`
                        }))}/>
                <Button type="primary" onClick={load}>加载版本</Button>
                {!scripts.length && <span style={{color: '#999'}}>z_script 表当前 0 行</span>}
            </Space>
            <Table rowKey="id" loading={loading} dataSource={versions}
                   columns={[
                       {title: '版本号', dataIndex: 'version_no', render: v => text(v)},
                       {title: 'DSL', dataIndex: 'dsl_type', render: v => text(v)},
                       {
                           title: '状态', dataIndex: 'status',
                           // 实测返回的是字符串枚举（"PUBLISHED"），不是旧代码假设的 1/0/-1；
                           // 只有这一条样本，所以照原样渲染，不再编造别的取值
                           render: v => v ? <Tag color="green">{v}</Tag> : '-'
                       },
                       {title: '当前版本', dataIndex: 'is_current', render: v => v === 1 ? <Tag color="blue">current</Tag> : '-'},
                       {title: '灰度权重', dataIndex: 'canary_weight', render: v => n(v) ? `${v}%` : '-'},
                       {title: '灰度比例', dataIndex: 'gray_percentage', render: v => n(v) ? `${v}%` : '-'},
                       {title: '说明', dataIndex: 'change_log', render: v => text(v)},
                       {title: '发布时间', dataIndex: 'published_at', render: v => stamp(v)},
                       {title: '创建时间', dataIndex: 'create_time', render: v => stamp(v)}
                   ]}
            />
        </Card>
    )
}

function TagTab() {
    const message = useMessage()
    const [tags, setTags] = useState([])
    const [loading, setLoading] = useState(false)
    const [name, setName] = useState('')
    const [category, setCategory] = useState('biz')

    const load = async () => {
        setLoading(true)
        try {
            const r = await http('GET', '/script/tag/list')
            setTags(r.data || [])
        } finally {
            setLoading(false)
        }
    }
    useEffect(() => {
        load()
    }, [])

    const create = async () => {
        if (!name) {
            message.warning('请输入标签名');
            return
        }
        await http('POST', '/script/tag/create', {tagName: name, tagCategory: category})
        setName('')
        load()
    }

    return (
        <Card>
            <Space style={{marginBottom: 16}}>
                <Input placeholder="标签名" style={{width: 200}} value={name} onChange={e => setName(e.target.value)}/>
                <Select value={category} onChange={setCategory} options={[
                    {value: 'biz', label: '业务'}, {value: 'env', label: '环境'}, {value: 'owner', label: '负责人'}
                ]}/>
                <Button type="primary" icon={<PlusOutlined/>} onClick={create}>创建标签</Button>
                <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
            </Space>
            <Table rowKey="id" loading={loading} dataSource={tags}
                   columns={[
                       {title: 'ID', dataIndex: 'id'},
                       {title: '标签名', dataIndex: 'tag_name', render: (v, r) => <Tag color={r.tag_color || r.color}>{text(v)}</Tag>},
                       {title: '分类', dataIndex: 'tag_category', render: v => text(v)},
                       {title: '描述', dataIndex: 'description', render: v => text(v)},
                       {title: '创建时间', dataIndex: 'create_time', render: v => stamp(v)}
                   ]}
            />
        </Card>
    )
}

function QuotaTab() {
    const message = useMessage()
    const [list, setList] = useState([])
    const [loading, setLoading] = useState(false)

    const load = async () => {
        setLoading(true)
        try {
            const r = await http('GET', '/script/api-key/page?current=1&size=200')
            const keyList = pageRecords(r)
            const quotaList = await Promise.all(keyList.map(async k => {
                const qr = await http('GET', '/script/quota/by-api-key/' + k.id)
                return {...k, quota: qr.data}
            }))
            setList(quotaList)
        } finally {
            setLoading(false)
        }
    }
    useEffect(() => {
        load()
    }, [])

    return (
        <Card>
            <Space style={{marginBottom: 16}}>
                <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
            </Space>
            <Table rowKey="id" loading={loading} dataSource={list}
                   columns={[
                       {title: '应用名', dataIndex: 'app_name', render: v => text(v)},
                       // 不渲染 api_key（同 ApiKeyTab 的理由，见 TASK-20260925-031）
                       {
                           title: '日用量', render: (_, r) => {
                               const quota = r.quota
                               if (!quota) return '-'
                               const used = n(quota.used_today)
                               const max = n(quota.max_per_day) || 1
                               const percent = Math.min(100, Math.round((used * 100) / max))
                               return (
                                   <Tooltip title={`${used} / ${max}`}>
                                       <Progress percent={percent} size="small"
                                                 status={percent >= 80 ? 'exception' : 'normal'}/>
                                   </Tooltip>
                               )
                           }
                       },
                       {title: '日配额', render: (_, r) => text(r.quota?.max_per_day)},
                       {title: '分钟配额', render: (_, r) => text(r.quota?.max_per_minute)},
                       {title: '今日已用', render: (_, r) => text(r.quota?.used_today)},
                       {title: '并发', render: (_, r) => text(r.quota?.max_concurrent)}
                   ]}
            />
        </Card>
    )
}

export default function ScriptCenterPage({initialTab = 'apikey'}) {
    // 必须受控：5 个菜单项挂在同一个组件上，跨菜单跳转时 React 只换 props 不重挂载，
    // defaultActiveKey 会被忽略 —— 实测点「调用监控」URL 变了但表格仍是 API Key 台账。
    const [active, setActive] = useState(initialTab)
    useEffect(() => setActive(initialTab), [initialTab])
    return (
        <div style={{padding: 16}}>
            <PageHeader
                title="脚本中心"
                subtitle="API Key · 调用监控 · 配额 · 版本 · 标签管理"
            />
            <Tabs activeKey={active} onChange={setActive} items={[
                {key: 'apikey', label: <span><KeyOutlined/> API Key 管理</span>, children: <ApiKeyTab/>},
                {key: 'monitor', label: <span><LineChartOutlined/> 调用监控</span>, children: <MonitorTab/>},
                {key: 'quota', label: <span><NumberOutlined/> 配额管理</span>, children: <QuotaTab/>},
                {key: 'version', label: <span><CloudUploadOutlined/> 版本管理</span>, children: <VersionTab/>},
                {key: 'tag', label: <span><TagsOutlined/> 标签管理</span>, children: <TagTab/>}
            ]}/>
        </div>
    )
}