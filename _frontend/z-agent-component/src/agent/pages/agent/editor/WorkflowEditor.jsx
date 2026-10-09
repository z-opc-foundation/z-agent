import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Button, message, Modal, Space, Tag, Tooltip} from 'antd';
import {CloudUploadOutlined, DeleteOutlined, RedoOutlined, SaveOutlined, UndoOutlined} from '@ant-design/icons';
import LogicFlow from '@logicflow/core';
import {Control, DndPanel, Menu, MiniMap, SelectionSelect} from '@logicflow/extension';
import '@logicflow/core/es/index.css';
import '@logicflow/extension/es/index.css';
import {AGENT_FLOW_NODE_TYPES, registerAgentNodes} from './nodes';
import {flowApi} from '../../../api';

// ============ Register LogicFlow extensions ONCE at module level ============
// LogicFlow.use() is global; calling it inside a component will throw
// "Extension already registered" on remount.
LogicFlow.use(Control);
LogicFlow.use(MiniMap);
LogicFlow.use(Menu);
LogicFlow.use(DndPanel);
LogicFlow.use(SelectionSelect);

// ============ Default Starting Flow ============
const DEFAULT_FLOW_DATA = {
    nodes: [
        {id: 'start_1', type: 'start', x: 150, y: 280, text: '开始'},
        {id: 'llm_1', type: 'llm', x: 380, y: 280, text: 'LLM 调用'},
        {id: 'end_1', type: 'end', x: 610, y: 280, text: '结束'},
    ],
    edges: [
        {id: 'e1', type: 'polyline', sourceNodeId: 'start_1', targetNodeId: 'llm_1', text: ''},
        {id: 'e2', type: 'polyline', sourceNodeId: 'llm_1', targetNodeId: 'end_1', text: ''},
    ],
};

// ============ Node Config Panel (side panel for selected node) ============
const NodeConfigPanel = ({node, onChange, onClose}) => {
    if (!node) return null;
    const typeLabel = AGENT_FLOW_NODE_TYPES.find(n => n.type === node.type)?.label || node.type;

    return (
        <div style={{
            position: 'absolute', right: 16, top: 16, width: 260,
            background: '#fff', borderRadius: 8, boxShadow: '0 2px 12px rgba(0,0,0,0.12)',
            padding: 16, zIndex: 10,
        }}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12}}>
                <div>
                    <Tag color="blue">{typeLabel}</Tag>
                </div>
                <Button type="text" size="small" onClick={onClose}>✕</Button>
            </div>
            <div style={{marginBottom: 8}}>
                <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>节点 ID</div>
                <div style={{fontSize: 13, color: '#333'}}>{node.id}</div>
            </div>
            <div style={{marginBottom: 8}}>
                <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>显示文本</div>
                <input
                    style={{
                        width: '100%',
                        border: '1px solid #d9d9d9',
                        borderRadius: 4,
                        padding: '4px 8px',
                        fontSize: 13
                    }}
                    defaultValue={node.text}
                    onBlur={(e) => onChange(node.id, {text: e.target.value})}
                    placeholder="输入显示文本"
                />
            </div>
            {node.type === 'llm' && (
                <div style={{marginBottom: 8}}>
                    <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>模型</div>
                    <select
                        style={{
                            width: '100%',
                            border: '1px solid #d9d9d9',
                            borderRadius: 4,
                            padding: '4px 8px',
                            fontSize: 13
                        }}
                        defaultValue={node.properties?.model || 'qwen2.5:7b'}
                        onChange={(e) => onChange(node.id, {properties: {...node.properties, model: e.target.value}})}
                    >
                        <option value="qwen2.5:7b">Qwen 2.5 7B</option>
                        <option value="qwen2.5:72b">Qwen 2.5 72B</option>
                        <option value="gpt-4o">GPT-4o</option>
                    </select>
                </div>
            )}
            {node.type === 'http' && (
                <>
                    <div style={{marginBottom: 8}}>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>HTTP Method</div>
                        <select
                            style={{
                                width: '100%',
                                border: '1px solid #d9d9d9',
                                borderRadius: 4,
                                padding: '4px 8px',
                                fontSize: 13
                            }}
                            defaultValue={node.properties?.method || 'GET'}
                            onChange={(e) => onChange(node.id, {
                                properties: {
                                    ...node.properties,
                                    method: e.target.value
                                }
                            })}
                        >
                            <option value="GET">GET</option>
                            <option value="POST">POST</option>
                            <option value="PUT">PUT</option>
                            <option value="DELETE">DELETE</option>
                        </select>
                    </div>
                    <div style={{marginBottom: 8}}>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>URL</div>
                        <input
                            style={{
                                width: '100%',
                                border: '1px solid #d9d9d9',
                                borderRadius: 4,
                                padding: '4px 8px',
                                fontSize: 13
                            }}
                            defaultValue={node.properties?.url || ''}
                            onBlur={(e) => onChange(node.id, {properties: {...node.properties, url: e.target.value}})}
                            placeholder="https://api.example.com/..."
                        />
                    </div>
                </>
            )}
            {node.type === 'condition' && (
                <div style={{marginBottom: 8}}>
                    <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>条件表达式</div>
                    <textarea
                        style={{
                            width: '100%',
                            border: '1px solid #d9d9d9',
                            borderRadius: 4,
                            padding: '4px 8px',
                            fontSize: 12,
                            resize: 'vertical'
                        }}
                        rows={3}
                        defaultValue={node.properties?.expression || ''}
                        onBlur={(e) => onChange(node.id, {
                            properties: {
                                ...node.properties,
                                expression: e.target.value
                            }
                        })}
                        placeholder="e.g. status == 'success'"
                    />
                </div>
            )}
        </div>
    );
};

// ============ Main WorkflowEditor Component ============
const WorkflowEditor = ({readOnly = false, value, onChange, appCode, flowName}) => {
    const containerRef = useRef(null);
    const lfRef = useRef(null);
    const [selectedNode, setSelectedNode] = useState(null);
    const [graphData, setGraphData] = useState(value || DEFAULT_FLOW_DATA);
    // FEATURE013 A5: 后端保存状态
    const [savedFlowId, setSavedFlowId] = useState(null);  // 已保存的 flow_definition.id
    const [savingToBackend, setSavingToBackend] = useState(false);
    const [showPublishModal, setShowPublishModal] = useState(false);

    // Initialize LogicFlow
    useEffect(() => {
        if (!containerRef.current) return;

        const lf = new LogicFlow({
            container: containerRef.current,
            grid: {
                size: 20,
                visible: true,
                type: 'dot',
                config: {color: '#e5e5e5'},
            },
            keyboard: {enabled: true},
            snapline: true,
            outline: true,
            textEdit: !readOnly,
            isSilentMode: readOnly,
            style: {
                rect: {rx: 6, ry: 6},
                polyline: {stroke: '#bfbfbf', strokeWidth: 2},
            },
            // Default edge type
            edgeType: 'polyline',
        });

        // Register custom Agent nodes
        registerAgentNodes(lf);

        // Set up drag-and-drop palette
        lf.extension.dndPanel.setPatternItems(
            AGENT_FLOW_NODE_TYPES.map(n => ({
                type: n.type,
                label: n.label,
                icon: n.icon,
            }))
        );

        // Show mini map via extension API (NOT as a React component)
        if (lf.extension.miniMap) {
            lf.extension.miniMap.show({
                width: 160,
                height: 100,
                showEdge: true,
                isShowHeader: false,
            });
        }

        // Load initial data
        const initialData = value || DEFAULT_FLOW_DATA;
        lf.render(initialData);
        setGraphData(initialData);

        // Node click → show config panel
        lf.on('node:click', ({data}) => {
            if (readOnly) return;
            setSelectedNode(data);
        });

        // Blank click → deselect
        lf.on('blank:click', () => {
            setSelectedNode(null);
        });

        // Graph change → notify parent
        lf.on('graph:change', () => {
            const data = lf.getGraphData();
            setGraphData(data);
            onChange?.(data);
        });

        // History
        lfRef.current = lf;

        return () => {
            lf.destroy();
        };
    }, []);  // eslint-disable-line react-hooks/exhaustive-deps

    // Update config panel when node changes
    const handleNodeConfigChange = useCallback((nodeId, changes) => {
        if (!lfRef.current) return;
        lfRef.current.updateText(nodeId, changes.text || '');
        if (changes.properties) {
            lfRef.current.updateProperties(nodeId, changes.properties);
        }
        // Refresh selected node data
        const node = lfRef.current.getGraphData().nodes.find(n => n.id === nodeId);
        if (node) setSelectedNode(node);
    }, []);

    // Toolbar actions
    const handleAddNode = useCallback((type) => {
        if (!lfRef.current || readOnly) return;
        const center = lfRef.current.getGraphData();
        const avgX = center.nodes.reduce((s, n) => s + n.x, 0) / (center.nodes.length || 1);
        const avgY = center.nodes.reduce((s, n) => s + n.y, 0) / (center.nodes.length || 1);
        lfRef.current.addNode({
            type,
            x: avgX + 80,
            y: avgY,
            text: AGENT_FLOW_NODE_TYPES.find(n => n.type === type)?.label || type
        });
    }, [readOnly]);

    const handleDeleteSelected = useCallback(() => {
        if (!lfRef.current || readOnly) return;
        const selected = lfRef.current.getSelectedModels();
        if (selected.nodes?.length) {
            selected.nodes.forEach(n => lfRef.current.deleteNode(n.id));
        }
        if (selected.edges?.length) {
            selected.edges.forEach(e => lfRef.current.deleteEdge(e.id));
        }
        setSelectedNode(null);
    }, [readOnly]);

    const handleUndo = useCallback(() => {
        if (!lfRef.current || readOnly) return;
        lfRef.current.undo();
    }, [readOnly]);

    const handleRedo = useCallback(() => {
        if (!lfRef.current || readOnly) return;
        lfRef.current.redo();
    }, [readOnly]);

    const handleSave = useCallback(() => {
        if (!lfRef.current) return;
        const data = lfRef.current.getGraphData();
        setGraphData(data);
        onChange?.(data);
        message.success('流程已保存到前端');
    }, [onChange]);

    // FEATURE013 A5: 保存到后端 (创建或更新 z_agent_flow_definition)
    const handleSaveToBackend = useCallback(async () => {
        if (!lfRef.current || !appCode) {
            message.warning('缺少 appCode, 无法保存到后端');
            return;
        }
        setSavingToBackend(true);
        try {
            const data = lfRef.current.getGraphData();
            const payload = {
                flowId: appCode,
                name: flowName || appCode,
                description: `App ${appCode} 的工作流`,
                nodes: JSON.stringify(data.nodes || []),
                edges: JSON.stringify(data.edges || []),
                variables: JSON.stringify(data.variables || {}),
                flowType: 'AGENT',
                appCode: appCode,
                status: 'DRAFT',
                version: 1,
                creator: localStorage.getItem('userInfo')
                    ? JSON.parse(localStorage.getItem('userInfo') || '{}').userCode || 'anonymous'
                    : 'anonymous',
            };
            let resp;
            if (savedFlowId) {
                // 已存在, 走 update
                resp = await flowApi.update({...payload, id: savedFlowId});
            } else {
                // 首次, 走 create
                resp = await flowApi.create(payload);
                const result = resp?.data ?? resp;
                if (result && typeof result === 'number') {
                    setSavedFlowId(result);
                } else if (result && result.id) {
                    setSavedFlowId(result.id);
                }
            }
            message.success('流程已保存到后端 z_agent_flow_definition');
            // 同步给父组件
            onChange?.(data);
        } catch (e) {
            message.error('保存到后端失败: ' + (e?.message || '未知错误'));
        } finally {
            setSavingToBackend(false);
        }
    }, [appCode, flowName, savedFlowId, onChange]);

    // FEATURE013 A5: 发布 (DRAFT → PUBLISHED, 写入执行内存)
    const handlePublish = useCallback(async () => {
        if (!savedFlowId) {
            message.warning('请先点击"保存到后端"');
            return;
        }
        try {
            await flowApi.publish({params: {id: savedFlowId}});
            setShowPublishModal(false);
            message.success('流程已发布, 现在可以被 Agent Runtime 执行');
        } catch (e) {
            message.error('发布失败: ' + (e?.message || '未知错误'));
        }
    }, [savedFlowId]);

    // FEATURE013 A5: 启动时尝试加载已有的 flow (按 appCode 找)
    useEffect(() => {
        if (!appCode || readOnly) return;
        (async () => {
            try {
                const resp = await flowApi.byFlowId({flowId: appCode, version: 1});
                const result = resp?.data ?? resp;
                if (result && result.id && result.nodes) {
                    setSavedFlowId(result.id);
                    // 把后端的 nodes/edges 灌入 LogicFlow
                    if (lfRef.current) {
                        lfRef.current.render({
                            nodes: JSON.parse(result.nodes),
                            edges: JSON.parse(result.edges),
                        });
                    }
                    message.info('已加载上次保存的工作流 v' + (result.version || 1));
                }
            } catch {
                // 首次保存, 不报错
            }
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [appCode]);

    const handleCloseConfig = useCallback(() => {
        setSelectedNode(null);
    }, []);

    return (
        <div style={{
            position: 'relative',
            height: 520,
            borderRadius: 8,
            overflow: 'hidden',
            background: '#fafafa',
            border: '1px solid #e8e8e8'
        }}>
            {/* Toolbar */}
            {!readOnly && (
                <div style={{
                    position: 'absolute', top: 8, left: 8, zIndex: 5,
                    display: 'flex', gap: 6, alignItems: 'center',
                    background: '#fff', borderRadius: 6, padding: '4px 8px',
                    boxShadow: '0 1px 6px rgba(0,0,0,0.08)',
                }}>
                    <Space size={4}>
                        <Tooltip title="撤销"><Button size="small" icon={<UndoOutlined/>}
                                                      onClick={handleUndo}/></Tooltip>
                        <Tooltip title="重做"><Button size="small" icon={<RedoOutlined/>}
                                                      onClick={handleRedo}/></Tooltip>
                        <div style={{width: 1, height: 16, background: '#e8e8e8', margin: '0 2px'}}/>
                        <Tooltip title="删除选中"><Button size="small" danger icon={<DeleteOutlined/>}
                                                          onClick={handleDeleteSelected}/></Tooltip>
                        <Tooltip title="保存到前端 (AgentApp)">
                            <Button size="small" icon={<SaveOutlined/>} onClick={handleSave}/>
                        </Tooltip>
                        {/* FEATURE013 A5: 后端保存 + 发布 */}
                        <div style={{width: 1, height: 16, background: '#e8e8e8', margin: '0 2px'}}/>
                        <Tooltip title="保存到后端 z_agent_flow_definition (FEATURE013 A5)">
                            <Button size="small" type="primary" ghost
                                    icon={<CloudUploadOutlined/>}
                                    loading={savingToBackend}
                                    disabled={!appCode}
                                    onClick={handleSaveToBackend}>
                                保存流程
                            </Button>
                        </Tooltip>
                        <Tooltip title="发布 (DRAFT → PUBLISHED, FlowEngine 可加载执行)">
                            <Button size="small" type="primary"
                                    disabled={!savedFlowId}
                                    onClick={() => setShowPublishModal(true)}>
                                发布
                            </Button>
                        </Tooltip>
                    </Space>
                    {savedFlowId && (
                        <Tag color="blue" style={{marginLeft: 8}}>flowId={savedFlowId}</Tag>
                    )}
                    <div style={{fontSize: 12, color: '#999', marginLeft: 8}}>
                        从左侧拖入节点到画布，双击节点编辑文本
                    </div>
                </div>
            )}

            {/* FEATURE013 A5: 发布确认 Modal */}
            <Modal
                title="发布工作流"
                open={showPublishModal}
                onOk={handlePublish}
                onCancel={() => setShowPublishModal(false)}
                okText="确认发布"
                cancelText="取消"
            >
                <p>发布后, <b>FlowOrchestrationService</b> 会在下次请求时加载这个 flow,
                    Agent Runtime 可以通过 <code>/api/agent/flow/execute/{savedFlowId}</code> 触发执行。</p>
                <p style={{color: '#999', fontSize: 12}}>提示: 如需修改, 需点击「升级到新版本」创建草稿。</p>
            </Modal>

            {/* LogicFlow Canvas */}
            <div ref={containerRef} style={{width: '100%', height: '100%'}}/>

            {/* Node Config Side Panel */}
            {selectedNode && !readOnly && (
                <NodeConfigPanel
                    node={selectedNode}
                    onChange={handleNodeConfigChange}
                    onClose={handleCloseConfig}
                />
            )}

            {/* MiniMap 已通过 lf.extension.miniMap.show() 渲染到 LogicFlow 容器内，不需要额外 React 元素 */}

            {/* Read-only badge */}
            {readOnly && (
                <div style={{
                    position: 'absolute', top: 8, right: 8, zIndex: 5,
                    background: 'rgba(0,0,0,0.5)', color: '#fff', borderRadius: 4,
                    padding: '2px 8px', fontSize: 12,
                }}>
                    只读模式
                </div>
            )}
        </div>
    );
};

export default WorkflowEditor;
