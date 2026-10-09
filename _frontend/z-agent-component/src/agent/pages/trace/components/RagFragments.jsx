import React from 'react';
import {Empty, Tag, Tooltip} from 'antd';
import {FileTextOutlined} from '@ant-design/icons';

/**
 * RagFragments 组件 (FEATURE013 T7)
 *
 * <p>展示 retrieve 类型的 span 召回的 top-k 分块, 含相似度分数.
 */
const RagFragments = ({spans = []}) => {
    // 找所有 retrieve span
    const retrieveSpans = spans.filter(s => s.spanType === 'retrieve');
    if (!retrieveSpans.length) {
        return (
            <Empty
                description="本次执行没有 RAG 检索"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
        );
    }

    let totalFragments = 0;
    let topScore = -1;

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 10}}>
            {retrieveSpans.map((span, idx) => {
                let chunks = [];
                let retrievalType = 'VECTOR';
                let knowledgeBaseCode = null;
                try {
                    const attrs = span.attributes
                        ? (typeof span.attributes === 'string' ? JSON.parse(span.attributes) : span.attributes)
                        : null;
                    if (attrs) {
                        chunks = attrs.chunks || attrs.results || [];
                        retrievalType = attrs.retrieval_type || attrs.retrievalType || 'VECTOR';
                        knowledgeBaseCode = attrs.knowledge_base_code || attrs.knowledgeBaseCode;
                    }
                } catch { /* ignore */
                }
                if (!Array.isArray(chunks)) chunks = [];

                totalFragments += chunks.length;
                chunks.forEach(c => {
                    const s = parseFloat(c.score);
                    if (!isNaN(s) && s > topScore) topScore = s;
                });

                return (
                    <div key={span.spanId || idx} style={{
                        border: '1px solid #b7eb8f',
                        borderRadius: 6,
                        background: '#f6ffed',
                        padding: '10px 12px',
                    }}>
                        <div style={{
                            display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8,
                        }}>
                            <FileTextOutlined style={{color: '#52c41a'}}/>
                            <Tag color="green">{retrievalType}</Tag>
                            {knowledgeBaseCode && (
                                <Tag color="blue">知识库: {knowledgeBaseCode}</Tag>
                            )}
                            <Tag>{chunks.length} 个分块</Tag>
                            <div style={{flex: 1}}/>
                            <span style={{fontSize: 11, color: '#999'}}>
                                耗时 {span.durationMs != null ? `${span.durationMs}ms` : '—'}
                            </span>
                        </div>
                        {chunks.length > 0 ? (
                            <div style={{display: 'flex', flexDirection: 'column', gap: 6}}>
                                {chunks.map((chunk, ci) => {
                                    const score = parseFloat(chunk.score) || 0;
                                    const scoreColor = score >= 0.8 ? 'green' : score >= 0.5 ? 'orange' : 'red';
                                    return (
                                        <div key={ci} style={{
                                            background: '#fff',
                                            padding: '8px 10px',
                                            borderRadius: 4,
                                            border: '1px solid #d9f7be',
                                        }}>
                                            <div style={{
                                                display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4,
                                            }}>
                                                <Tag color="cyan">#{ci + 1}</Tag>
                                                {chunk.document_code && (
                                                    <span style={{fontSize: 11, color: '#666'}}>
                                                        doc: {chunk.document_code}
                                                    </span>
                                                )}
                                                {chunk.chunk_index != null && (
                                                    <span style={{fontSize: 11, color: '#999'}}>
                                                        chunk #{chunk.chunk_index}
                                                    </span>
                                                )}
                                                <div style={{flex: 1}}/>
                                                <Tooltip title="相似度分数">
                                                    <Tag color={scoreColor}>{(score * 100).toFixed(1)}%</Tag>
                                                </Tooltip>
                                            </div>
                                            <div style={{
                                                fontSize: 12, color: '#333',
                                                lineHeight: 1.5,
                                                whiteSpace: 'pre-wrap',
                                                wordBreak: 'break-word',
                                                maxHeight: 100, overflow: 'auto',
                                            }}>
                                                {truncate(chunk.chunk_content || chunk.content || '(空)', 300)}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div style={{fontSize: 12, color: '#999', padding: '6px 8px'}}>
                                无召回结果
                            </div>
                        )}
                    </div>
                );
            })}
            <div style={{fontSize: 11, color: '#666', textAlign: 'right'}}>
                汇总: {retrieveSpans.length} 次检索 / {totalFragments} 个分块 / top
                score {topScore >= 0 ? topScore.toFixed(3) : '—'}
            </div>
        </div>
    );
};

function truncate(s, max) {
    if (!s) return s;
    return s.length > max ? s.substring(0, max) + '…' : s;
}

export default RagFragments;