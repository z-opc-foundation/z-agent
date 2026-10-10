import React from 'react';
import {Empty, Tag} from 'antd';
import {BranchesOutlined} from '@ant-design/icons';

/**
 * ToolCallTree 组件 (FEATURE013 T7)
 *
 * <p>树形展示 tool_call 类型 span, 按 parent_span_id 嵌套.
 * 每个节点展示: 工具名 / 参数 / 结果 / 耗时 / 状态.
 */
const ToolCallTree = ({spans = []}) => {
    // 过滤出 tool_call / llm_call 类型
    const callSpans = spans.filter(s =>
        s.spanType === 'tool_call' || s.spanType === 'llm_call',
    );
    if (!callSpans.length) {
        return (
            <Empty
                description="本次执行没有工具/LLM 调用"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
        );
    }

    // 构建 map
    const byId = new Map(callSpans.map(s => [s.spanId, s]));
    // 找根节点 (parent_span_id 不在 callSpans 里 或为 null)
    const roots = callSpans.filter(s => !s.parentSpanId || !byId.has(s.parentSpanId));

    const renderNode = (span, depth) => {
        let args = null, result = null;
        try {
            const attrs = span.attributes
                ? (typeof span.attributes === 'string' ? JSON.parse(span.attributes) : span.attributes)
                : null;
            if (attrs) {
                args = attrs.arguments || attrs.argumentsJson || attrs.tool_input;
                result = attrs.tool_result || attrs.result || attrs.output;
            }
        } catch { /* ignore */
        }

        const children = callSpans.filter(s => s.parentSpanId === span.spanId);

        return (
            <div key={span.spanId} style={{
                marginLeft: depth * 20, marginBottom: 8,
                borderLeft: depth > 0 ? '2px solid #d9d9d9' : 'none',
                paddingLeft: depth > 0 ? 12 : 0,
            }}>
                <div style={{
                    background: '#fff',
                    border: '1px solid #e8e8e8',
                    borderRadius: 6,
                    padding: '8px 12px',
                    borderLeft: `3px solid ${span.spanType === 'tool_call' ? '#fa8c16' : '#1677ff'}`,
                }}>
                    <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4}}>
                        <BranchesOutlined/>
                        <Tag color={span.spanType === 'tool_call' ? 'orange' : 'blue'}>
                            {span.spanType === 'tool_call' ? 'Tool' : 'LLM'}
                        </Tag>
                        <span style={{fontWeight: 600}}>{span.name}</span>
                        <Tag color={
                            span.status === 'SUCCESS' ? 'success' :
                                span.status === 'FAILURE' ? 'error' : 'default'
                        }>{span.status}</Tag>
                        <div style={{flex: 1}}/>
                        <span style={{fontSize: 11, color: '#999'}}>
                            {span.durationMs != null ? `${span.durationMs}ms` : ''}
                        </span>
                    </div>
                    {args && (
                        <details>
                            <summary style={{cursor: 'pointer', fontSize: 12, color: '#666'}}>
                                📥 入参
                            </summary>
                            <pre style={{
                                background: '#fafafa', padding: 6, marginTop: 4,
                                borderRadius: 4, fontSize: 11, maxHeight: 100, overflow: 'auto',
                            }}>{typeof args === 'string' ? args : JSON.stringify(args, null, 2)}</pre>
                        </details>
                    )}
                    {result && (
                        <details>
                            <summary style={{cursor: 'pointer', fontSize: 12, color: '#666'}}>
                                📤 结果
                            </summary>
                            <pre style={{
                                background: '#f6ffed', padding: 6, marginTop: 4,
                                borderRadius: 4, fontSize: 11, maxHeight: 150, overflow: 'auto',
                            }}>{typeof result === 'string' ? result : JSON.stringify(result, null, 2)}</pre>
                        </details>
                    )}
                    {span.errorMessage && (
                        <div style={{fontSize: 12, color: '#ff4d4f', marginTop: 4}}>
                            ❌ {span.errorMessage}
                        </div>
                    )}
                </div>
                {children.length > 0 && (
                    <div style={{marginTop: 6}}>
                        {children.map(c => renderNode(c, depth + 1))}
                    </div>
                )}
            </div>
        );
    };

    return (
        <div>
            {roots.map(r => renderNode(r, 0))}
        </div>
    );
};

export default ToolCallTree;