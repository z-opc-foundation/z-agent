import React from 'react';
import {Empty, Tag, Tooltip} from 'antd';
import {
    BulbOutlined,
    CodeOutlined,
    FileSearchOutlined,
    MailOutlined,
    PlayCircleOutlined,
    StopOutlined,
    ThunderboltOutlined,
} from '@ant-design/icons';

/**
 * Timeline 组件 (FEATURE013 T7)
 *
 * <p>按 sequence 排序展示该 trace 的所有 span. 每个 span 一行:
 * <ul>
 *   <li>序号 + 类型图标 + 类型 Tag</li>
 *   <li>span 名称 + 耗时</li>
 *   <li>content 摘要 (think 文本 / tool_call 参数 / retrieve chunks)</li>
 *   <li>attributes 详情 (折叠展开)</li>
 * </ul>
 */
const TYPE_META = {
    think: {color: 'gold', icon: <BulbOutlined/>, label: '思考'},
    llm_call: {color: 'blue', icon: <ThunderboltOutlined/>, label: 'LLM'},
    tool_call: {color: 'orange', icon: <CodeOutlined/>, label: '工具'},
    retrieve: {color: 'green', icon: <FileSearchOutlined/>, label: 'RAG'},
    observe: {color: 'purple', icon: <MailOutlined/>, label: '观察'},
    step: {color: 'default', icon: <PlayCircleOutlined/>, label: '步骤'},
    flow_node: {color: 'cyan', icon: <PlayCircleOutlined/>, label: 'Flow节点'},
};

const STATUS_META = {
    RUNNING: {color: 'processing', icon: <PlayCircleOutlined/>},
    SUCCESS: {color: 'success', icon: <PlayCircleOutlined/>},
    FAILURE: {color: 'error', icon: <StopOutlined/>},
    ERROR: {color: 'error', icon: <StopOutlined/>},
    PENDING: {color: 'default', icon: null},
};

const Timeline = ({spans = []}) => {
    if (!spans.length) {
        return (
            <Empty
                description="暂无 span 数据"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
        );
    }

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 8}}>
            {spans.map((span, idx) => {
                const meta = TYPE_META[span.spanType] || {color: 'default', icon: null, label: span.spanType};
                const statusMeta = STATUS_META[span.status] || STATUS_META.PENDING;
                // 解析 attributes
                let attrs = null;
                if (span.attributes) {
                    try {
                        attrs = typeof span.attributes === 'string' ? JSON.parse(span.attributes) : span.attributes;
                    } catch { /* ignore */
                    }
                }
                const isError = span.status === 'FAILURE' || span.status === 'ERROR';
                return (
                    <div
                        key={span.spanId || idx}
                        style={{
                            border: '1px solid #f0f0f0',
                            borderLeft: `3px solid ${isError ? '#ff4d4f' : '#1677ff'}`,
                            borderRadius: 6,
                            padding: '10px 12px',
                            background: '#fafafa',
                        }}
                    >
                        {/* Header 行 */}
                        <div style={{
                            display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6,
                        }}>
                            <span style={{
                                width: 22, height: 22, borderRadius: 11, background: '#1677ff',
                                color: '#fff', display: 'inline-flex', alignItems: 'center',
                                justifyContent: 'center', fontSize: 11, fontWeight: 600,
                            }}>{idx + 1}</span>
                            <Tag color={meta.color} icon={meta.icon}>{meta.label}</Tag>
                            <Tag color={statusMeta.color} icon={statusMeta.icon}>{span.status}</Tag>
                            <span style={{fontSize: 12, color: '#666'}}>{span.name}</span>
                            <div style={{flex: 1}}/>
                            <Tooltip title="耗时">
                                <Tag>{span.durationMs != null ? `${span.durationMs}ms` : '—'}</Tag>
                            </Tooltip>
                            <span style={{fontSize: 11, color: '#999'}}>
                                #{span.sequence}
                            </span>
                        </div>
                        {/* Content 行 */}
                        {span.content && (
                            <div style={{
                                fontSize: 12,
                                color: '#333',
                                background: '#fff',
                                padding: '6px 8px',
                                borderRadius: 4,
                                border: '1px dashed #e8e8e8',
                                marginBottom: attrs ? 6 : 0,
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                                maxHeight: 120,
                                overflow: 'auto',
                            }}>
                                {truncate(span.content, 500)}
                            </div>
                        )}
                        {/* Attributes 详情 */}
                        {attrs && Object.keys(attrs).length > 0 && (
                            <details style={{fontSize: 11, color: '#888'}}>
                                <summary style={{cursor: 'pointer', userSelect: 'none'}}>
                                    查看 attributes ({Object.keys(attrs).length})
                                </summary>
                                <pre style={{
                                    background: '#fff', padding: 6, marginTop: 4,
                                    borderRadius: 4, maxHeight: 150, overflow: 'auto',
                                }}>{JSON.stringify(attrs, null, 2)}</pre>
                            </details>
                        )}
                        {/* Error message */}
                        {span.errorMessage && (
                            <div style={{marginTop: 6, fontSize: 12, color: '#ff4d4f'}}>
                                ❌ {span.errorMessage}
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
};

function truncate(s, max) {
    if (!s) return s;
    return s.length > max ? s.substring(0, max) + '…' : s;
}

export default Timeline;