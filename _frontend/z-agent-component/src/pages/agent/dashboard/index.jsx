import {useEffect, useMemo, useState} from 'react'
import {Card, Col, Empty, Progress, Row, Select, Statistic, Table, Tabs, Tag, Typography} from 'antd'
import {
    AppstoreOutlined,
    ClockCircleOutlined,
    DollarOutlined,
    LineChartOutlined,
    RocketOutlined,
    ThunderboltOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {agentApi, statsApi} from '@/agent/api'
import {PageHeader} from '@/common/components/ui'

const {Text} = Typography
const {TabPane} = Tabs

/**
 * FEATURE013 T3.2: Agent 应用维度监控面板
 *
 * <p>与 {@code UsageDashboard} (LLM 网关维度) 互补 — 本页是 Agent 应用维度:
 * <ul>
 *     <li>顶部 KPI: 累计/今日 对话数、Token、成功率、P95 延迟、独立应用/用户数</li>
 *     <li>趋势图: 近 7 天对话 + Token 趋势 (可切 7/30 天)</li>
 *     <li>延迟分布: 6 桶直方图</li>
 *     <li>Top-N 排行: 按应用/用户</li>
 * </ul>
 */
const AgentDashboard = () => {
    // 顶部 KPI
    const [overview, setOverview] = useState(null)
    // 趋势
    const [trend, setTrend] = useState([])
    // 延迟分布
    const [latency, setLatency] = useState([])
    // 按应用
    const [byApp, setByApp] = useState([])
    // 按用户
    const [byUser, setByUser] = useState([])
    const [loading, setLoading] = useState(false)
    const [appFilter, setAppFilter] = useState(null)
    const [apps, setApps] = useState([])
    const [trendDays, setTrendDays] = useState(7)

    // 加载应用下拉
    useEffect(() => {
        agentApi.appPage({id: 1}).then(res => {
            const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
            setApps(list)
        }).catch(() => {
        })
    }, [])

    const fetchAll = async (appCode) => {
        setLoading(true)
        try {
            const params = appCode ? {appCode} : {}
            const [ov, tr, lat, appR, userR] = await Promise.allSettled([
                statsApi.overview(params),
                statsApi.trend({...params, days: trendDays}),
                statsApi.latency(params),
                statsApi.byApp({...params, limit: 20}),
                statsApi.byUser({...params, limit: 20}),
            ])
            setOverview(ov.status === 'fulfilled' ? (ov.value?.data || ov.value || null) : null)
            setTrend(tr.status === 'fulfilled' ? (tr.value?.data || tr.value || []) : [])
            setLatency(lat.status === 'fulfilled' ? (lat.value?.data || lat.value || []) : [])
            setByApp(appR.status === 'fulfilled' ? (appR.value?.data || appR.value || []) : [])
            setByUser(userR.status === 'fulfilled' ? (userR.value?.data || userR.value || []) : [])
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchAll(appFilter)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [appFilter, trendDays])

    // ============ 图表配置 ============

    const trendOption = useMemo(() => ({
        tooltip: {trigger: 'axis'},
        legend: {data: ['对话数', 'Token消耗', '平均延迟(ms)']},
        grid: {top: 50, left: 60, right: 60, bottom: 40},
        xAxis: {
            type: 'category',
            data: trend.map(t => t.date),
            axisLabel: {rotate: trend.length > 14 ? 45 : 0}
        },
        yAxis: [
            {type: 'value', name: '次数', position: 'left'},
            {type: 'value', name: 'Tokens', position: 'right'},
            {type: 'value', name: 'ms', position: 'right', splitLine: {show: false}, axisLabel: {show: false}},
        ],
        series: [
            {
                name: '对话数', type: 'bar', smooth: true,
                itemStyle: {color: '#667eea'},
                data: trend.map(t => t.daily_conversations || 0)
            },
            {
                name: 'Token消耗', type: 'line', yAxisIndex: 1, smooth: true,
                itemStyle: {color: '#52c41a'},
                lineStyle: {width: 3},
                data: trend.map(t => t.daily_tokens || 0)
            },
            {
                name: '平均延迟(ms)', type: 'line', yAxisIndex: 2, smooth: true,
                itemStyle: {color: '#faad14'},
                lineStyle: {width: 2, type: 'dashed'},
                data: trend.map(t => Math.round(t.avg_latency_ms || 0))
            }
        ]
    }), [trend])

    const latencyOption = useMemo(() => ({
        tooltip: {trigger: 'axis'},
        grid: {top: 30, left: 50, right: 30, bottom: 40},
        xAxis: {
            type: 'category',
            data: latency.map(b => b.label),
            axisLabel: {interval: 0}
        },
        yAxis: {type: 'value', name: '对话数'},
        series: [{
            name: '对话数',
            type: 'bar',
            data: latency.map(b => b.count || 0),
            itemStyle: {
                color: (params) => {
                    // 颜色按延迟分桶: <500 绿, 500-1k 青, 1k-2k 蓝, 2k-5k 黄, 5k-10k 橙, >=10k 红
                    const colors = ['#52c41a', '#13c2c2', '#1677ff', '#faad14', '#fa8c16', '#f5222d']
                    return colors[params.dataIndex] || '#1677ff'
                },
                borderRadius: [4, 4, 0, 0]
            },
            label: {
                show: true,
                position: 'top',
                formatter: (p) => (p.value > 0 ? p.value : '')
            }
        }]
    }), [latency])

    // 本文件所有行键/KPI 键一律 snake_case（2026-09-25 逐字段实测 /agent/stats/* 五个端点）：
    // 全局 Jackson SNAKE_CASE 对 DTO 属性生效，页面按 camelCase 读就永远是 undefined ⇒
    // 表格每格 '-'、趋势图三条序列贴着 0、KPI 卡恒显 0，而请求全是 200、构建也过。
    // 只有 appCode / days / limit 保持 camel —— 它们是 @RequestParam 查询参数，不走 Jackson 命名。
    // ============ 表格列 ============

    const appColumns = [
        {
            title: '应用编码', dataIndex: 'app_code', width: 180,
            render: v => <Tag color="blue">{v || '-'}</Tag>
        },
        {
            title: '调用次数', dataIndex: 'total_calls', width: 110,
            render: v => <Text strong>{(v || 0).toLocaleString()}</Text>,
            sorter: (a, b) => (a.total_calls || 0) - (b.total_calls || 0),
        },
        {
            title: '成功率', dataIndex: 'success_rate', width: 140,
            render: v => {
                const pct = ((v || 0) * 100).toFixed(1)
                const color = v >= 0.95 ? '#52c41a' : v >= 0.8 ? '#faad14' : '#f5222d'
                return (
                    <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                        <Progress
                            percent={Number(pct)} size="small" showInfo={false}
                            strokeColor={color} style={{flex: 1, marginBottom: 0}}
                        />
                        <Text style={{color, minWidth: 50}}>{pct}%</Text>
                    </div>
                )
            },
            sorter: (a, b) => (a.success_rate || 0) - (b.success_rate || 0),
        },
        {
            title: '总Token', dataIndex: 'total_tokens', width: 110,
            render: v => (v || 0).toLocaleString(),
            sorter: (a, b) => (a.total_tokens || 0) - (b.total_tokens || 0),
        },
        {
            title: '平均Token', dataIndex: 'avg_tokens', width: 100,
            render: v => v ? Number(v).toFixed(0) : '-',
        },
        {
            title: '平均延迟', dataIndex: 'avg_latency_ms', width: 110,
            render: v => v ? (
                <Tag color={v < 1000 ? 'green' : v < 3000 ? 'orange' : 'red'}>
                    {Number(v).toFixed(0)}ms
                </Tag>
            ) : '-',
            sorter: (a, b) => (a.avg_latency_ms || 0) - (b.avg_latency_ms || 0),
        },
    ]

    const userColumns = [
        {title: '用户', dataIndex: 'user_name', render: (v, r) => v || r.user_id || '-'},
        {
            title: '调用次数', dataIndex: 'total_calls', width: 110,
            render: v => (v || 0).toLocaleString(),
            sorter: (a, b) => (a.total_calls || 0) - (b.total_calls || 0),
        },
        {
            title: '成功/失败', dataIndex: 'success_count', width: 130,
            render: (v, r) => (
                <span>
                    <Tag color="green">{v || 0}</Tag>
                    <Tag color="red">{r.failed_count || 0}</Tag>
                </span>
            )
        },
        {
            title: '总Token', dataIndex: 'total_tokens', width: 110,
            render: v => (v || 0).toLocaleString(),
        },
        {
            title: '平均延迟', dataIndex: 'avg_latency_ms', width: 110,
            render: v => v ? `${Number(v).toFixed(0)}ms` : '-',
        },
        {title: '最近调用', dataIndex: 'last_call_at', width: 180},
    ]

    // 成功率颜色
    const successRateColor = (overview?.success_rate || 0) >= 0.95 ? '#52c41a'
        : (overview?.success_rate || 0) >= 0.8 ? '#faad14' : '#f5222d'

    return (
        <div>
            <PageHeader
                title="Agent 监控面板"
                subtitle="实时监控 Agent 应用性能、对话趋势和延迟分布"
            />
            {/* 维度筛选栏 */}
            <div style={{
                display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap',
                background: '#fff', padding: '12px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 16
            }}>
                <span style={{fontWeight: 600, color: '#333', marginRight: 4}}>维度筛选：</span>
                <Select
                    placeholder="筛选 Agent 应用"
                    allowClear
                    style={{width: 220}}
                    value={appFilter}
                    onChange={setAppFilter}
                    showSearch
                    optionFilterProp="label"
                    options={apps.map(a => ({
                        value: a.app_code,
                        label: a.app_name ? `${a.app_name} (${a.app_code})` : a.app_code
                    }))}
                />
                <Select
                    value={trendDays}
                    style={{width: 140}}
                    onChange={setTrendDays}
                    options={[
                        {value: 7, label: '近 7 天'},
                        {value: 14, label: '近 14 天'},
                        {value: 30, label: '近 30 天'},
                        {value: 90, label: '近 90 天'},
                    ]}
                />
                {(appFilter) && (
                    <a onClick={() => setAppFilter(null)} style={{fontSize: 12}}>
                        清空筛选
                    </a>
                )}
                <Text type="secondary" style={{marginLeft: 'auto', fontSize: 12}}>
                    FEATURE013 T3.2 · Agent 应用维度监控
                </Text>
            </div>

            {/* 顶部 6 个 KPI 卡 */}
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>累计对话数</span>}
                            value={overview?.total_conversations || 0}
                            prefix={<RocketOutlined style={{color: '#667eea'}}/>}
                            formatter={v => Number(v).toLocaleString()}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>成功率</span>}
                            value={((overview?.success_rate || 0) * 100)}
                            precision={2}
                            suffix="%"
                            prefix={<LineChartOutlined style={{color: successRateColor}}/>}
                            styles={{content: {color: successRateColor, fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>累计 Token</span>}
                            value={overview?.total_tokens || 0}
                            prefix={<DollarOutlined style={{color: '#52c41a'}}/>}
                            formatter={v => Number(v).toLocaleString()}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>平均延迟</span>}
                            value={overview?.avg_latency_ms || 0}
                            precision={0}
                            suffix="ms"
                            prefix={<ClockCircleOutlined style={{color: '#1677ff'}}/>}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>P95 延迟</span>}
                            value={overview?.p95_latency_ms || 0}
                            suffix="ms"
                            prefix={<ThunderboltOutlined style={{color: '#fa8c16'}}/>}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>活跃 Agent / 用户</span>}
                            value={overview?.active_app_count || 0}
                            suffix={` / ${overview?.active_user_count || 0}`}
                            prefix={<AppstoreOutlined style={{color: '#722ed1'}}/>}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
            </Row>

            {/* 今日统计 */}
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={8}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Row align="middle" gutter={16}>
                            <Col span={8}>
                                <Statistic
                                    title="今日对话"
                                    value={overview?.today_conversations || 0}
                                    formatter={v => Number(v).toLocaleString()}
                                    styles={{content: {fontSize: 20, fontWeight: 700}}}
                                />
                            </Col>
                            <Col span={8}>
                                <Statistic
                                    title="今日 Token"
                                    value={overview?.today_tokens || 0}
                                    formatter={v => Number(v).toLocaleString()}
                                    styles={{content: {fontSize: 20, fontWeight: 700, color: '#52c41a'}}}
                                />
                            </Col>
                            <Col span={8}>
                                <Statistic
                                    title="今日失败"
                                    value={overview?.today_failed_count || 0}
                                    styles={{
                                        content: {
                                            fontSize: 20, fontWeight: 700,
                                            color: (overview?.today_failed_count || 0) > 0 ? '#f5222d' : '#52c41a'
                                        }
                                    }}
                                />
                            </Col>
                        </Row>
                    </Card>
                </Col>
                <Col span={16}>
                    <Card
                        size="small"
                        title="⚡ 系统提示"
                        style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}
                    >
                        <div style={{fontSize: 13, color: '#666', lineHeight: 1.7}}>
                            本面板聚合 <Text code>z_agent_conversation</Text> 表的实时数据。
                            所有指标按 <Text code>instance_code → app_code</Text> 维度关联。
                            <br/>
                            1) 累计数据 = 近 90 天（性能与代表性折中）；
                            2) 趋势按天聚合，横轴日期已自动补全空日；
                            3) P95 延迟 = 第 95 百分位（剔除极值）。
                        </div>
                    </Card>
                </Col>
            </Row>

            <Tabs defaultActiveKey="trend">
                <TabPane tab="📈 趋势图" key="trend">
                    <Card
                        style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}
                        loading={loading}
                    >
                        {trend.length === 0 ? (
                            <Empty description="暂无趋势数据"/>
                        ) : (
                            <ReactECharts option={trendOption} style={{height: 360, width: '100%'}}/>
                        )}
                    </Card>
                </TabPane>

                <TabPane tab="⏱ 延迟分布" key="latency">
                    <Card
                        style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}
                        loading={loading}
                    >
                        {latency.length === 0 ? (
                            <Empty description="暂无延迟数据"/>
                        ) : (
                            <>
                                <ReactECharts option={latencyOption} style={{height: 320, width: '100%'}}/>
                                <div style={{marginTop: 12, fontSize: 12, color: '#999'}}>
                                    * 颜色编码: <Tag color="green">&lt;500ms 优秀</Tag>
                                    <Tag color="cyan">500-1k 良好</Tag>
                                    <Tag color="blue">1k-2k 一般</Tag>
                                    <Tag color="orange">2k-5k 偏慢</Tag>
                                    <Tag color="red">≥5k 异常</Tag>
                                </div>
                            </>
                        )}
                    </Card>
                </TabPane>

                <TabPane tab="🏆 按 Agent 应用 (TOP-N)" key="byApp">
                    <Card
                        style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}
                        loading={loading}
                    >
                        <Table
                            columns={appColumns}
                            dataSource={byApp}
                            rowKey="app_code"
                            pagination={false}
                            locale={{emptyText: '暂无数据'}}
                            size="middle"
                        />
                    </Card>
                </TabPane>

                <TabPane tab="👥 按用户 (TOP-N)" key="byUser">
                    <Card
                        style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}
                        loading={loading}
                    >
                        <Table
                            columns={userColumns}
                            dataSource={byUser}
                            rowKey="user_id"
                            pagination={false}
                            locale={{emptyText: '暂无数据'}}
                            size="middle"
                        />
                    </Card>
                </TabPane>
            </Tabs>
        </div>
    )
}

export default AgentDashboard
