import {useEffect, useState} from 'react'
import {Card, Col, Row, Select, Statistic, Table, Tabs, Tag, Typography} from 'antd'
import {BarChartOutlined, DollarOutlined, RocketOutlined} from '@ant-design/icons'
import request from '@/agent/api/request'
import ReactECharts from 'echarts-for-react'
import {PageHeader} from '@/common/components/ui'

const {Text} = Typography
const {TabPane} = Tabs

const UsageDashboard = () => {
    const [overview, setOverview] = useState(null)
    const [byApp, setByApp] = useState([])
    const [byUser, setByUser] = useState([])
    const [trend, setTrend] = useState([])
    const [records, setRecords] = useState([])
    const [loading, setLoading] = useState(false)

    // 维度筛选
    const [appFilter, setAppFilter] = useState(null)
    const [providerFilter, setProviderFilter] = useState(null)
    const [modelFilter, setModelFilter] = useState(null)
    const [providers, setProviders] = useState([])
    const [models, setModels] = useState([])
    const [apps, setApps] = useState([])

    // 加载维度下拉数据
    useEffect(() => {
        // 加载供应商
        request('/llm-center/provider/list').then(res => {
            const list = Array.isArray(res) ? res : (res?.data || [])
            setProviders(list)
        }).catch(() => {
        })

        // 加载模型（支持按供应商过滤）
        loadModels(null)

        // 加载应用列表
        request('/agent/app/page', {method: 'POST', body: JSON.stringify({current: 1, size: 100})}).then(res => {
            const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
            setApps(list)
        }).catch(() => {
        })
    }, [])

    const loadModels = (providerCode) => {
        request('/llm-center/model/list', {params: {providerCode}}).then(res => {
            const list = Array.isArray(res) ? res : (res?.data || [])
            setModels(list)
        }).catch(() => {
        })
    }

    const handleProviderChange = (val) => {
        setProviderFilter(val)
        setModelFilter(null)
        loadModels(val)
    }

    const buildFilterParams = () => ({
        appCode: appFilter,
        providerCode: providerFilter,
        modelCode: modelFilter,
    })

    const fetchOverview = async () => {
        try {
            const res = await request('/llm-gateway/usage/overview', {
                method: 'GET',
                params: buildFilterParams()
            })
            setOverview(res?.data || res || null)
        } catch (e) {
            console.warn('Usage overview API unavailable:', e?.message)
            setOverview(null)
        }
    }

    const fetchByApp = async () => {
        try {
            const res = await request('/llm-gateway/usage/by-app', {method: 'GET'})
            setByApp(Array.isArray(res) ? res : (res?.data || []))
        } catch (e) {
            console.warn('Usage by-app API unavailable:', e?.message)
            setByApp([])
        }
    }

    const fetchByUser = async () => {
        try {
            const res = await request('/llm-gateway/usage/by-user', {
                method: 'GET',
                params: buildFilterParams()
            })
            setByUser(Array.isArray(res) ? res : (res?.data || []))
        } catch (e) {
            console.warn('Usage by-user API unavailable:', e?.message)
            setByUser([])
        }
    }

    const fetchTrend = async () => {
        try {
            const res = await request('/llm-gateway/usage/trend', {
                method: 'GET',
                params: {...buildFilterParams(), days: 7}
            })
            setTrend(Array.isArray(res) ? res : (res?.data || []))
        } catch (e) {
            console.warn('Usage trend API unavailable:', e?.message)
            setTrend([])
        }
    }

    const fetchRecords = async () => {
        setLoading(true)
        try {
            const res = await request('/llm-gateway/usage/records', {
                method: 'GET',
                params: {...buildFilterParams(), page: 1, size: 20}
            })
            const records = Array.isArray(res) ? res : (res?.records || res?.data || [])
            setRecords(records)
        } catch (e) {
            console.warn('Usage records API unavailable:', e?.message)
            setRecords([])
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchOverview()
        fetchByApp()
        fetchByUser()
        fetchTrend()
        fetchRecords()
    }, [appFilter, providerFilter, modelFilter])

    // 趋势图配置
    const trendOption = {
        tooltip: {trigger: 'axis'},
        legend: {data: ['Token消耗', '调用次数']},
        xAxis: {
            type: 'category',
            data: trend.map(t => t.date)
        },
        yAxis: [
            {type: 'value', name: 'Tokens', position: 'left'},
            {type: 'value', name: '调用次数', position: 'right'}
        ],
        series: [
            {
                name: 'Token消耗',
                type: 'bar',
                data: trend.map(t => t.dailyTokens)
            },
            {
                name: '调用次数',
                type: 'line',
                yAxisIndex: 1,
                data: trend.map(t => t.dailyCalls)
            }
        ]
    }

    const appColumns = [
        {title: '应用编码', dataIndex: 'appCode'},
        {title: '调用次数', dataIndex: 'totalCalls', render: v => v?.toLocaleString()},
        {title: '输入Token', dataIndex: 'inputTokens', render: v => v?.toLocaleString()},
        {title: '输出Token', dataIndex: 'outputTokens', render: v => v?.toLocaleString()},
        {title: '总Token', dataIndex: 'totalTokens', render: v => v?.toLocaleString()},
        {
            title: '费用',
            dataIndex: 'totalCost',
            render: v => <Text style={{color: '#faad14'}}>¥{v?.toFixed(4) || '0'}</Text>
        }
    ]

    const userColumns = [
        {title: '用户ID', dataIndex: 'userId'},
        {title: '调用次数', dataIndex: 'totalCalls', render: v => v?.toLocaleString()},
        {title: '总Token', dataIndex: 'totalTokens', render: v => v?.toLocaleString()},
        {
            title: '费用',
            dataIndex: 'totalCost',
            render: v => <Text style={{color: '#faad14'}}>¥{v?.toFixed(4) || '0'}</Text>
        }
    ]

    const recordColumns = [
        {title: '时间', dataIndex: 'gmtCreate', width: 160},
        {title: '应用', dataIndex: 'appCode', render: v => <Tag color="blue">{v || '-'}</Tag>},
        {title: '用户', dataIndex: 'userName'},
        {title: '供应商', dataIndex: 'providerCode', render: v => <Tag color="purple">{v || '-'}</Tag>},
        {title: '模型', dataIndex: 'modelCode', render: v => <Tag>{v || '-'}</Tag>},
        {title: '输入Token', dataIndex: 'inputTokens', render: v => v?.toLocaleString()},
        {title: '输出Token', dataIndex: 'outputTokens', render: v => v?.toLocaleString()},
        {title: '延迟', dataIndex: 'latencyMs', render: v => v ? `${v}ms` : '-'},
        {
            title: '状态',
            dataIndex: 'status',
            render: v => <Tag color={v === 'SUCCESS' ? 'green' : 'red'}>{v || '-'}</Tag>
        },
        {
            title: '费用',
            dataIndex: 'totalCost',
            render: v => <Text style={{color: '#faad14'}}>¥{v || 0}</Text>
        }
    ]

    return (
        <div>
            <PageHeader
                title="用量统计"
                subtitle="查看 LLM 调用 Token 消耗、费用和趋势分析"
            />
            {/* 维度筛选栏 */}
            <div style={{
                display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap',
                background: '#fff', padding: '12px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 16
            }}>
                <span style={{fontWeight: 600, color: '#333', marginRight: 4}}>维度筛选：</span>
                <Select
                    placeholder="筛选应用"
                    allowClear
                    style={{width: 180}}
                    value={appFilter}
                    onChange={setAppFilter}
                    options={apps.map(a => ({value: a.appCode, label: a.appName || a.appCode}))}
                />
                <Select
                    placeholder="筛选供应商"
                    allowClear
                    style={{width: 180}}
                    value={providerFilter}
                    onChange={handleProviderChange}
                    options={providers.map(p => ({value: p.providerCode, label: p.providerName}))}
                />
                <Select
                    placeholder="筛选模型"
                    allowClear
                    style={{width: 180}}
                    value={modelFilter}
                    onChange={setModelFilter}
                    options={models.map(m => ({value: m.modelCode, label: m.modelName}))}
                />
                {(appFilter || providerFilter || modelFilter) && (
                    <a onClick={() => {
                        setAppFilter(null);
                        setProviderFilter(null);
                        setModelFilter(null)
                    }} style={{fontSize: 12}}>
                        清空筛选
                    </a>
                )}
            </div>

            {/* 统计卡片 */}
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={6}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>累计 Token</span>}
                            value={overview?.totalTokens || 0}
                            prefix={<RocketOutlined style={{color: '#667eea'}}/>}
                            formatter={v => v?.toLocaleString()}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>累计费用</span>}
                            value={overview?.totalCost || 0}
                            prefix={<DollarOutlined style={{color: '#faad14'}}/>}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>今日 Token</span>}
                            value={overview?.todayTokens || 0}
                            prefix={<BarChartOutlined style={{color: '#52c41a'}}/>}
                            formatter={v => v?.toLocaleString()}
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small" style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Statistic
                            title={<span style={{color: '#666', fontSize: 13}}>今日费用</span>}
                            value={overview?.todayCost || 0}
                            prefix={<DollarOutlined style={{color: '#faad14'}}/>}
                            precision={4}
                            suffix="元"
                            styles={{content: {color: '#333', fontSize: 22, fontWeight: 700}}}
                        />
                    </Card>
                </Col>
            </Row>

            <Tabs defaultActiveKey="trend">
                <TabPane tab="用量趋势" key="trend">
                    <Card style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <ReactECharts option={trendOption} style={{height: 300, width: '100%'}}/>
                    </Card>
                </TabPane>

                <TabPane tab="按应用统计" key="byApp">
                    <Card style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Table
                            columns={appColumns}
                            dataSource={byApp}
                            rowKey="appCode"
                            pagination={false}
                            locale={{emptyText: '暂无数据'}}
                        />
                    </Card>
                </TabPane>

                <TabPane tab="按用户统计" key="byUser">
                    <Card style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Table
                            columns={userColumns}
                            dataSource={byUser}
                            rowKey="userId"
                            pagination={false}
                            locale={{emptyText: '暂无数据'}}
                        />
                    </Card>
                </TabPane>

                <TabPane tab="调用明细" key="records">
                    <Card style={{borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)'}}>
                        <Table
                            columns={recordColumns}
                            dataSource={records}
                            loading={loading}
                            rowKey="id"
                            pagination={{pageSize: 20, showSizeChanger: false}}
                            locale={{emptyText: '暂无数据'}}
                        />
                    </Card>
                </TabPane>
            </Tabs>
        </div>
    )
}

export default UsageDashboard