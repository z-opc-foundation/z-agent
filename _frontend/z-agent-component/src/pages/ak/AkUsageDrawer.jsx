import {useEffect, useState} from 'react'
import {Card, Col, Divider, Drawer, Empty, Row, Space, Spin, Statistic, Table, Tabs, Tag, Typography} from 'antd'
import {
    ApiOutlined,
    BarChartOutlined,
    ClockCircleOutlined,
    CopyOutlined,
    DollarOutlined,
    ThunderboltOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import request from '../../api'

const {Text} = Typography

const formatTokens = (n) => {
    if (!n) return '0'
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
    return Number(n).toLocaleString()
}

const formatCost = (v) => {
    if (!v) return '0.0000'
    return Number(v).toFixed(4)
}

const AkUsageDrawer = ({open, ak, onClose}) => {
    const [overview, setOverview] = useState(null)
    const [trend, setTrend] = useState([])
    const [records, setRecords] = useState([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!open || !ak) return
        fetchAll()
    }, [open, ak])

    const fetchAll = async () => {
        setLoading(true)
        try {
            // 并行加载 3 个接口
            const [ov, tr, rec] = await Promise.allSettled([
                request('/llm-gateway/usage/overview', {method: 'GET', params: {accessKey: ak.accessKey}}),
                request('/llm-gateway/usage/trend', {method: 'GET', params: {accessKey: ak.accessKey, days: 7}}),
                request('/llm-gateway/usage/records', {
                    method: 'GET',
                    params: {accessKey: ak.accessKey, page: 1, size: 20}
                }),
            ])
            setOverview(ov.status === 'fulfilled' ? (ov.value?.data || ov.value || null) : null)
            setTrend(tr.status === 'fulfilled' ? (Array.isArray(tr.value) ? tr.value : (tr.value?.data || [])) : [])
            const recVal = rec.status === 'fulfilled' ? rec.value : null
            setRecords(recVal ? (Array.isArray(recVal) ? recVal : (recVal.records || recVal.data || [])) : [])
        } catch (e) {
            // 单个接口失败不影响其他
        } finally {
            setLoading(false)
        }
    }

    const handleCopy = (text, label) => {
        navigator.clipboard.writeText(text)
        // eslint-disable-next-line no-undef
        message.success(`${label} 已复制`)
    }

    const trendOption = {
        tooltip: {trigger: 'axis'},
        legend: {data: ['Token消耗', '调用次数']},
        grid: {top: 40, right: 40, bottom: 30, left: 50},
        xAxis: {
            type: 'category',
            data: trend.map(t => t.date || t.statDate || ''),
        },
        yAxis: [
            {type: 'value', name: 'Tokens', position: 'left'},
            {type: 'value', name: '调用次数', position: 'right'},
        ],
        series: [
            {name: 'Token消耗', type: 'bar', data: trend.map(t => t.dailyTokens || t.totalTokens || 0)},
            {name: '调用次数', type: 'line', yAxisIndex: 1, data: trend.map(t => t.dailyCalls || t.totalCalls || 0)},
        ],
    }

    const recordColumns = [
        {title: '时间', dataIndex: 'gmtCreate', width: 160},
        {title: '应用', dataIndex: 'appCode', render: v => v ? <Tag color="blue">{v}</Tag> : '-'},
        {title: '用户', dataIndex: 'userName', render: v => v || '-'},
        {title: '供应商', dataIndex: 'providerCode', render: v => v ? <Tag color="purple">{v}</Tag> : '-'},
        {title: '模型', dataIndex: 'modelCode', render: v => v ? <Tag>{v}</Tag> : '-'},
        {title: '输入Token', dataIndex: 'inputTokens', render: v => v?.toLocaleString() || '-'},
        {title: '输出Token', dataIndex: 'outputTokens', render: v => v?.toLocaleString() || '-'},
        {
            title: '状态', dataIndex: 'status',
            render: v => <Tag color={v === 'SUCCESS' ? 'green' : 'red'}>{v || '-'}</Tag>,
        },
        {title: '费用', dataIndex: 'totalCost', render: v => <Text style={{color: '#faad14'}}>¥{v || '0.0000'}</Text>},
    ]

    return (
        <Drawer
            title={
                <Space>
                    <span>用量详情 ·</span>
                    <Text strong>{ak?.akName}</Text>
                    {ak?.accessKey && (
                        <Space size={4}>
                            <code style={{
                                fontSize: 12,
                                color: '#595959',
                                background: '#f5f5f5',
                                padding: '2px 6px',
                                borderRadius: 4
                            }}>
                                {ak.accessKey}
                            </code>
                            <CopyOutlined
                                style={{cursor: 'pointer', color: '#1677ff'}}
                                onClick={() => handleCopy(ak.accessKey, 'AccessKey')}
                            />
                        </Space>
                    )}
                </Space>
            }
            open={open}
            onClose={onClose}
            size="large"
            destroyOnHidden
        >
            <Spin spinning={loading}>
                {/* 4 列统计 */}
                <Row gutter={12} style={{marginBottom: 16}}>
                    <Col span={6}>
                        <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                            <Statistic
                                title="累计 Token"
                                value={overview?.totalTokens || 0}
                                prefix={<ThunderboltOutlined style={{color: '#667eea'}}/>}
                                formatter={v => Number(v).toLocaleString()}
                                styles={{content: {fontSize: 18}}}
                            />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                            <Statistic
                                title="累计费用"
                                value={formatCost(overview?.totalCost)}
                                prefix={<DollarOutlined style={{color: '#faad14'}}/>}
                                styles={{content: {fontSize: 18}}}
                            />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                            <Statistic
                                title="今日 Token"
                                value={overview?.todayTokens || 0}
                                prefix={<ClockCircleOutlined style={{color: '#52c41a'}}/>}
                                formatter={v => Number(v).toLocaleString()}
                                styles={{content: {fontSize: 18}}}
                            />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small" style={{background: '#f5f5f5'}} styles={{body: {padding: '10px 14px'}}}>
                            <Statistic
                                title="今日费用"
                                value={formatCost(overview?.todayCost)}
                                prefix={<DollarOutlined style={{color: '#faad14'}}/>}
                                styles={{content: {fontSize: 18}}}
                            />
                        </Card>
                    </Col>
                </Row>

                <Divider style={{margin: '12px 0'}}/>

                {/* Tab 切换 */}
                <Tabs defaultActiveKey="trend" size="small">
                    <Tabs.TabPane tab="用量趋势" key="trend" icon={<BarChartOutlined/>}>
                        <Card size="small" styles={{body: {padding: 12}}}>
                            {trend.length > 0 ? (
                                <ReactECharts option={trendOption} style={{height: 300, width: '100%'}}/>
                            ) : (
                                <Empty description="该 AK 暂无用量数据" style={{padding: '40px 0'}}/>
                            )}
                        </Card>
                    </Tabs.TabPane>

                    <Tabs.TabPane tab="调用明细" key="records" icon={<ApiOutlined/>}>
                        <Table
                            columns={recordColumns}
                            dataSource={records}
                            rowKey="id"
                            size="small"
                            pagination={{pageSize: 20, showSizeChanger: false}}
                            locale={{emptyText: <Empty description="暂无调用记录"/>}}
                        />
                    </Tabs.TabPane>
                </Tabs>
            </Spin>
        </Drawer>
    )
}

export default AkUsageDrawer