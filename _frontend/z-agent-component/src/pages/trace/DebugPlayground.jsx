import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {
    Badge,
    Button,
    Card,
    Empty,
    Input,
    Layout,
    List,
    message,
    Segmented,
    Space,
    Spin,
    Tabs,
    Tag,
    Tooltip,
    Typography,
} from 'antd';
import {
    CodeOutlined,
    DashboardOutlined,
    FileSearchOutlined,
    HistoryOutlined,
    ReloadOutlined,
    SearchOutlined,
} from '@ant-design/icons';
import MetricsDashboard from './components/MetricsDashboard';
import Timeline from './components/Timeline';
import ToolCallTree from './components/ToolCallTree';
import RagFragments from './components/RagFragments';
import {traceApi} from '@/agent/api';

const {Sider, Content} = Layout;
const {Text, Title} = Typography;

/**
 * Agent Debug Playground 主页面 (FEATURE013 T7)
 *
 * <p>布局:
 * <pre>
 *   ┌──────────────────────────────────────────────────┐
 *   │ Toolbar: 刷新 / 状态过滤 / 类型过滤 / 搜索       │
 *   ├─────────┬────────────────────────────────────────┤
 *   │ Trace   │ MetricsDashboard (KPI + 调用分布)     │
 *   │ 列表    ├────────────────────────────────────────┤
 *   │         │ Tabs: Timeline / Tool Tree / RAG       │
 *   └─────────┴────────────────────────────────────────┘
 * </pre>
 */
const DebugPlayground = () => {
    // ====== Trace 列表 ======
    const [traces, setTraces] = useState([]);
    const [tracesLoading, setTracesLoading] = useState(false);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [keyword, setKeyword] = useState('');

    // ====== 当前选中 trace ======
    const [selectedTraceId, setSelectedTraceId] = useState(null);
    const [traceDetail, setTraceDetail] = useState(null); // {trace, spans}
    const [detailLoading, setDetailLoading] = useState(false);

    // ====== Tab ======
    const [activeTab, setActiveTab] = useState('timeline');

    // ====== 健康检查 ======
    const [health, setHealth] = useState(null);

    // ============ 加载 trace 列表 ============
    const loadTraces = useCallback(async () => {
        setTracesLoading(true);
        try {
            const resp = await traceApi.list({pageNum: 1, pageSize: 50});
            const data = resp?.data || resp?.records || resp || [];
            setTraces(Array.isArray(data) ? data : []);
        } catch (e) {
            message.error('加载 trace 列表失败: ' + (e?.message || '未知错误'));
        } finally {
            setTracesLoading(false);
        }
    }, []);

    // ============ 加载健康检查 ============
    const loadHealth = useCallback(async () => {
        try {
            const resp = await traceApi.health();
            setHealth(resp?.data || resp);
        } catch {
            setHealth(null);
        }
    }, []);

    useEffect(() => {
        loadTraces();
        loadHealth();
    }, [loadTraces, loadHealth]);

    // ============ 过滤 trace 列表 ============
    const filteredTraces = useMemo(() => {
        let arr = traces;
        if (statusFilter !== 'ALL') {
            arr = arr.filter(t => t.status === statusFilter);
        }
        if (keyword.trim()) {
            const kw = keyword.trim().toLowerCase();
            arr = arr.filter(t =>
                (t.traceId && t.traceId.toLowerCase().includes(kw)) ||
                (t.agentName && t.agentName.toLowerCase().includes(kw)) ||
                (t.userGoal && t.userGoal.toLowerCase().includes(kw)) ||
                (t.appCode && t.appCode.toLowerCase().includes(kw)),
            );
        }
        return arr;
    }, [traces, statusFilter, keyword]);

    // ============ 选中 trace 后加载详情 ============
    const loadTraceDetail = useCallback(async (traceId) => {
        setDetailLoading(true);
        try {
            const resp = await traceApi.get({params: {traceId}});
            const data = resp?.data || resp;
            if (data && data.trace) {
                setTraceDetail(data);
                setSelectedTraceId(traceId);
            } else {
                message.warning('未找到 trace: ' + traceId);
                setTraceDetail(null);
            }
        } catch (e) {
            message.error('加载 trace 详情失败: ' + (e?.message || '未知错误'));
            setTraceDetail(null);
        } finally {
            setDetailLoading(false);
        }
    }, []);

    // 自动加载最新一个 trace
    useEffect(() => {
        if (traces.length && !selectedTraceId) {
            loadTraceDetail(traces[0].traceId);
        }
    }, [traces, selectedTraceId, loadTraceDetail]);

    // ============ 渲染左侧 trace 列表项 ============
    const renderTraceItem = (trace) => {
        const isSelected = selectedTraceId === trace.traceId;
        const statusBadge = {
            SUCCESS: 'success', FAILURE: 'error', RUNNING: 'processing', ERROR: 'error',
        }[trace.status] || 'default';

        return (
            <List.Item
                key={trace.traceId}
                style={{
                    padding: '10px 12px',
                    cursor: 'pointer',
                    background: isSelected ? '#e6f4ff' : 'transparent',
                    borderLeft: isSelected ? '3px solid #1677ff' : '3px solid transparent',
                }}
                onClick={() => loadTraceDetail(trace.traceId)}
            >
                <div style={{width: '100%'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4}}>
                        <Badge status={statusBadge}/>
                        <Tag color={statusBadge === 'success' ? 'green' : statusBadge === 'error' ? 'red' : 'blue'}>
                            {trace.status}
                        </Tag>
                        <Tooltip title="DB ID">
                            <span style={{fontSize: 11, color: '#999'}}>#{trace.id}</span>
                        </Tooltip>
                        <div style={{flex: 1}}/>
                        <span style={{fontSize: 11, color: '#999'}}>
                            {trace.durationMs != null ? `${trace.durationMs}ms` : '—'}
                        </span>
                    </div>
                    <div style={{fontSize: 12, color: '#333', fontWeight: 500, marginBottom: 4}}
                         title={trace.userGoal}>
                        {truncate(trace.userGoal, 60)}
                    </div>
                    <div style={{fontSize: 10, color: '#999', display: 'flex', gap: 6}}>
                        <Tag color="cyan" style={{margin: 0}}>{trace.agentName || '—'}</Tag>
                        <span>{trace.startedAt ? new Date(trace.startedAt).toLocaleTimeString() : '—'}</span>
                    </div>
                </div>
            </List.Item>
        );
    };

    const trace = traceDetail?.trace;
    const spans = traceDetail?.spans || [];

    return (
        <Layout style={{height: 'calc(100vh - 64px)', background: '#f5f5f5'}}>
            {/* ============ 左侧 trace 列表 ============ */}
            <Sider size="large" theme="light" style={{
                borderRight: '1px solid #e8e8e8',
                overflow: 'auto',
            }}>
                {/* 健康检查横幅 */}
                {health && (
                    <div style={{
                        padding: '8px 12px',
                        background: health.trace_mapper_ready ? '#f6ffed' : '#fff7e6',
                        borderBottom: '1px solid #e8e8e8',
                        fontSize: 11,
                    }}>
                        <Space size={4}>
                            <Badge status={health.trace_mapper_ready ? 'success' : 'warning'}/>
                            <span>
                                trace 模块: {health.trace_mapper_ready ? '就绪' : '未启动'}
                                {health.total_traces != null && ` · 共 ${health.total_traces} 条`}
                            </span>
                        </Space>
                    </div>
                )}

                {/* Toolbar */}
                <div style={{padding: '10px 12px', borderBottom: '1px solid #e8e8e8'}}>
                    <Title level={5} style={{margin: '0 0 8px 0'}}>
                        <HistoryOutlined/> Trace 列表
                    </Title>
                    <Space.Compact style={{width: '100%', marginBottom: 8}}>
                        <Input
                            prefix={<SearchOutlined/>}
                            placeholder="搜索 trace_id / agent / goal"
                            value={keyword}
                            onChange={e => setKeyword(e.target.value)}
                            allowClear
                        />
                        <Button icon={<ReloadOutlined/>} onClick={loadTraces}/>
                    </Space.Compact>
                    <Segmented
                        block
                        size="small"
                        value={statusFilter}
                        onChange={setStatusFilter}
                        options={[
                            {label: '全部', value: 'ALL'},
                            {label: '成功', value: 'SUCCESS'},
                            {label: '失败', value: 'FAILURE'},
                            {label: '运行中', value: 'RUNNING'},
                        ]}
                    />
                </div>

                {/* Trace 列表 */}
                <Spin spinning={tracesLoading}>
                    {filteredTraces.length > 0 ? (
                        <List
                            size="small"
                            dataSource={filteredTraces}
                            renderItem={renderTraceItem}
                            split={false}
                            locale={{
                                emptyText: <Empty description="无匹配 trace" image={Empty.PRESENTED_IMAGE_SIMPLE}/>
                            }}
                        />
                    ) : !tracesLoading && (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description={health?.trace_mapper_ready === false ? 'trace 表未启用' : '暂无 trace'}
                            style={{padding: '40px 0'}}
                        />
                    )}
                </Spin>
            </Sider>

            {/* ============ 右侧详情 ============ */}
            <Content style={{padding: '16px', overflow: 'auto'}}>
                <Spin spinning={detailLoading}>
                    {!trace ? (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description="选择左侧 trace 查看详情"
                            style={{paddingTop: 100}}
                        />
                    ) : (
                        <Space orientation="vertical" size="middle" style={{width: '100%'}}>
                            {/* 顶部 Metrics 仪表盘 */}
                            <Card
                                title={<span><DashboardOutlined/> 调试仪表盘</span>}
                                size="small"
                                extra={
                                    <Space>
                                        <Tooltip title="刷新">
                                            <Button
                                                size="small" icon={<ReloadOutlined/>}
                                                onClick={() => loadTraceDetail(trace.traceId)}
                                            />
                                        </Tooltip>
                                        <Tag color="blue">{trace.traceId}</Tag>
                                    </Space>
                                }
                            >
                                <MetricsDashboard trace={trace} spans={spans}/>
                            </Card>

                            {/* 4 段式 Tab */}
                            <Card size="small" styles={{body: {paddingTop: 4}}}>
                                <Tabs
                                    activeKey={activeTab}
                                    onChange={setActiveTab}
                                    items={[
                                        {
                                            key: 'timeline',
                                            label: <span><HistoryOutlined/> 时间线 ({spans.length})</span>,
                                            children: <Timeline spans={spans}/>,
                                        },
                                        {
                                            key: 'tools',
                                            label: (
                                                <span>
                                                    <CodeOutlined/> 工具/LLM 树 (
                                                    {spans.filter(s =>
                                                        s.spanType === 'tool_call' || s.spanType === 'llm_call').length})
                                                </span>
                                            ),
                                            children: <ToolCallTree spans={spans}/>,
                                        },
                                        {
                                            key: 'rag',
                                            label: (
                                                <span>
                                                    <FileSearchOutlined/> RAG 片段 (
                                                    {spans.filter(s => s.spanType === 'retrieve').length})
                                                </span>
                                            ),
                                            children: <RagFragments spans={spans}/>,
                                        },
                                    ]}
                                />
                            </Card>
                        </Space>
                    )}
                </Spin>
            </Content>
        </Layout>
    );
};

function truncate(s, max) {
    if (!s) return s;
    return s.length > max ? s.substring(0, max) + '…' : s;
}

export default DebugPlayground;