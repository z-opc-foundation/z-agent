import React from 'react';
import {Card, Col, Empty, Progress, Row, Statistic, Tag} from 'antd';
import {
    ClockCircleOutlined,
    CodeOutlined,
    CommentOutlined,
    FileSearchOutlined,
    RocketOutlined,
    ThunderboltOutlined,
} from '@ant-design/icons';

/**
 * MetricsDashboard 组件 (FEATURE013 T7)
 *
 * <p>顶部 KPI 卡 + Token/延迟仪表盘:
 * <ul>
 *   <li>总耗时 / 状态 / 调用计数 (LLM / Tool / RAG)</li>
 *   <li>各类型 span 数量占比</li>
 *   <li>Token 使用 (若 trace 包含 metrics 信息)</li>
 * </ul>
 */
const MetricsDashboard = ({trace, spans = []}) => {
    if (!trace) return <Empty description="请选择左侧 trace" image={Empty.PRESENTED_IMAGE_SIMPLE}/>;

    // 计算各类 span 数量
    const counts = spans.reduce((acc, s) => {
        acc[s.spanType] = (acc[s.spanType] || 0) + 1;
        return acc;
    }, {});
    const totalSpans = spans.length;
    const llmCalls = counts.llm_call || 0;
    const toolCalls = counts.tool_call || 0;
    const ragCalls = counts.retrieve || 0;
    const thinkSteps = counts.think || 0;
    const failedSpans = spans.filter(s => s.status === 'FAILURE' || s.status === 'ERROR').length;

    // 状态颜色
    const statusColor = {
        SUCCESS: 'success', FAILURE: 'error', RUNNING: 'processing', ERROR: 'error',
    }[trace.status] || 'default';

    return (
        <div>
            {/* 顶部 KPI 卡 */}
            <Row gutter={[12, 12]}>
                <Col xs={24} sm={12} md={4}>
                    <Card size="small" hoverable>
                        <Statistic
                            title={<span><RocketOutlined/> 状态</span>}
                            value={trace.status}
                            styles={{
                                content: {
                                    color: statusColor === 'success' ? '#3f8600' :
                                        statusColor === 'error' ? '#cf1322' : '#1677ff', fontSize: 16
                                }
                            }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={4}>
                    <Card size="small" hoverable>
                        <Statistic
                            title={<span><ClockCircleOutlined/> 总耗时</span>}
                            value={trace.durationMs || 0}
                            suffix="ms"
                            styles={{content: {color: '#1677ff'}}}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={4}>
                    <Card size="small" hoverable>
                        <Statistic
                            title={<span><CommentOutlined/> Span 总数</span>}
                            value={totalSpans}
                            suffix="个"
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={4}>
                    <Card size="small" hoverable>
                        <Statistic
                            title="失败 Span"
                            value={failedSpans}
                            suffix="个"
                            styles={{content: {color: failedSpans > 0 ? '#cf1322' : '#3f8600'}}}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={8}>
                    <Card size="small" hoverable>
                        <Statistic
                            title={<span><CommentOutlined/> 用户目标 / Agent</span>}
                            value={trace.userGoal || '(空)'}
                            styles={{content: {fontSize: 13, lineHeight: 1.4}}}
                        />
                        <div style={{marginTop: 6, fontSize: 11, color: '#666'}}>
                            agent: <Tag>{trace.agentName || '—'}</Tag>
                            app: <Tag>{trace.appCode || '—'}</Tag>
                            session: <Tag>{trace.sessionId || '—'}</Tag>
                        </div>
                    </Card>
                </Col>
            </Row>

            {/* 调用分布 */}
            <Row gutter={[12, 12]} style={{marginTop: 12}}>
                <Col xs={24} sm={8}>
                    <Card size="small" title={<span><ThunderboltOutlined/> LLM 调用</span>}>
                        <Statistic value={llmCalls} suffix="次" styles={{content: {color: '#1677ff'}}}/>
                        <Progress
                            percent={totalSpans ? Math.round(llmCalls * 100 / totalSpans) : 0}
                            strokeColor="#1677ff" size="small" showInfo={false}
                            style={{marginTop: 6}}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card size="small" title={<span><CodeOutlined/> 工具调用</span>}>
                        <Statistic value={toolCalls} suffix="次" styles={{content: {color: '#fa8c16'}}}/>
                        <Progress
                            percent={totalSpans ? Math.round(toolCalls * 100 / totalSpans) : 0}
                            strokeColor="#fa8c16" size="small" showInfo={false}
                            style={{marginTop: 6}}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card size="small" title={<span><FileSearchOutlined/> RAG 检索</span>}>
                        <Statistic value={ragCalls} suffix="次" styles={{content: {color: '#52c41a'}}}/>
                        <Progress
                            percent={totalSpans ? Math.round(ragCalls * 100 / totalSpans) : 0}
                            strokeColor="#52c41a" size="small" showInfo={false}
                            style={{marginTop: 6}}
                        />
                    </Card>
                </Col>
            </Row>

            {/* 时间线 + Trace 元信息 */}
            <Card size="small" title="Trace 元信息" style={{marginTop: 12}}>
                <Row gutter={[8, 4]}>
                    <Col span={12}>
                        <span style={{color: '#666'}}>trace_id:</span>{' '}
                        <code>{trace.traceId}</code>
                    </Col>
                    <Col span={12}>
                        <span style={{color: '#666'}}>DB ID:</span> {trace.id}
                    </Col>
                    <Col span={12}>
                        <span style={{color: '#666'}}>开始:</span> {trace.startedAt}
                    </Col>
                    <Col span={12}>
                        <span style={{color: '#666'}}>结束:</span> {trace.endedAt || '(进行中)'}
                    </Col>
                    <Col span={24}>
                        <span style={{color: '#666'}}>LLM 调用 / Tool 调用 / RAG / Think:</span>{' '}
                        <Tag color="blue">{llmCalls}</Tag>
                        <Tag color="orange">{toolCalls}</Tag>
                        <Tag color="green">{ragCalls}</Tag>
                        <Tag color="gold">{thinkSteps}</Tag>
                    </Col>
                </Row>
            </Card>
        </div>
    );
};

export default MetricsDashboard;