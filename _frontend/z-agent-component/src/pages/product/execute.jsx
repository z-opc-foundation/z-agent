import {useCallback, useEffect, useRef, useState} from 'react'
import {
    Avatar,
    Button,
    Card,
    Col,
    Empty,
    Form,
    Input,
    InputNumber,
    List,
    message,
    Modal,
    Row,
    Select,
    Space,
    Spin,
    Tag,
    Tooltip,
    Typography
} from 'antd'
import {
    CheckCircleOutlined,
    ClearOutlined,
    CloseCircleOutlined,
    CopyOutlined,
    HistoryOutlined,
    LoadingOutlined,
    ReloadOutlined,
    RobotOutlined,
    SendOutlined,
    ThunderboltOutlined,
    UserOutlined
} from '@ant-design/icons'
import {productApi, sceneApi} from '@/agent/api'

const {TextArea} = Input
const {Text, Title} = Typography
const {Option} = Select

// ===== 用户端场景执行器 =====
const SceneExecutorPage = () => {
    // 从 URL 参数获取 productCode / sceneCode
    const params = new URLSearchParams(window.location.search)
    const initProductCode = params.get('productCode') || ''
    const initSceneCode = params.get('sceneCode') || ''

    const [productCode, setProductCode] = useState(initProductCode)
    const [sceneCode, setSceneCode] = useState(initSceneCode)
    const [products, setProducts] = useState([])
    const [scenes, setScenes] = useState([])
    const [messages, setMessages] = useState([])
    const [inputValue, setInputValue] = useState('')
    const [loading, setLoading] = useState(false)
    const [streaming, setStreaming] = useState(false)
    const [currentReply, setCurrentReply] = useState('')
    const [historyOpen, setHistoryOpen] = useState(false)
    const [chatHistory, setChatHistory] = useState([])
    const [configModalVisible, setConfigModalVisible] = useState(false)
    const [form] = Form.useForm()
    const [execConfig, setExecConfig] = useState({temperature: 0.7, maxTokens: 2048, topP: 1})
    const [sceneConfig, setSceneConfig] = useState(null)

    const messagesEndRef = useRef(null)
    const eventSourceRef = useRef(null)
    const inputRef = useRef(null)

    // 加载产品列表
    useEffect(() => {
        productApi.page({id: 1, pageSize: 100, status: 'PUBLISHED'}).then(res => {
            setProducts(res.data?.records || [])
        })
    }, [])

    // 加载场景列表（当产品变化时）
    useEffect(() => {
        if (!productCode) {
            setScenes([]);
            return
        }
        sceneApi.page({id: 1, pageSize: 100, status: 'PUBLISHED', productCode}).then(res => {
            setScenes(res.data?.records || [])
        })
    }, [productCode])

    // 加载场景配置
    useEffect(() => {
        if (!sceneCode) {
            setSceneConfig(null);
            return
        }
        sceneApi.configGet({params: {sceneCode}}).then(res => {
            setSceneConfig(res.data)
        }).catch(() => setSceneConfig(null))
    }, [sceneCode])

    // 加载历史记录
    useEffect(() => {
        if (!sceneCode) return
        sceneApi.chatHistory({params: {sceneCode}}).then(res => {
            setChatHistory(Array.isArray(res.data) ? res.data : [])
        }).catch(() => setChatHistory([]))
    }, [sceneCode])

    // 自动滚动
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages, currentReply])

    // 自动聚焦输入框
    useEffect(() => {
        inputRef.current?.focus()
    }, [sceneCode])

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }

    const handleSend = useCallback(async () => {
        if (!inputValue.trim() || !sceneCode || loading || streaming) return
        const userMessage = inputValue.trim()
        setInputValue('')

        // 添加用户消息
        const userMsg = {
            id: Date.now(),
            role: 'user',
            content: userMessage,
            timestamp: new Date().toLocaleTimeString(),
        }
        setMessages(prev => [...prev, userMsg])
        setLoading(true)
        setCurrentReply('')

        try {
            setStreaming(true)

            // 使用 EventSource 流式调用
            const userId = localStorage.getItem('userId') || 'anonymous'
            const userName = localStorage.getItem('userName') || '匿名用户'

            const sse = new EventSource(`/api/scene/execute/stream?sceneCode=${sceneCode}&message=${encodeURIComponent(userMessage)}&userId=${userId}&userName=${encodeURIComponent(userName)}`)
            eventSourceRef.current = sse

            sse.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data)
                    if (data.type === 'chunk') {
                        setCurrentReply(prev => prev + data.content)
                    } else if (data.type === 'done') {
                        // 完成，添加AI消息
                        const aiMsg = {
                            id: Date.now() + 1,
                            role: 'assistant',
                            content: currentReply + data.content,
                            timestamp: new Date().toLocaleTimeString(),
                        }
                        setMessages(prev => [...prev, aiMsg])
                        setCurrentReply('')
                        setStreaming(false)
                        sse.close()
                    } else if (data.type === 'error') {
                        message.error(data.content || '执行出错')
                        setCurrentReply('')
                        setStreaming(false)
                        sse.close()
                    }
                } catch (e) {
                    // 原始文本
                    setCurrentReply(prev => prev + event.data)
                }
            }

            sse.onerror = async () => {
                // 如果是简单非流式响应，尝试普通调用
                sse.close()
                setStreaming(false)
                setLoading(false)

                // 降级：普通请求
                try {
                    const res = await sceneApi.execute({
                        sceneCode,
                        message: userMessage,
                        userId: userId,
                        userName: userName,
                        config: execConfig,
                    })
                    const aiMsg = {
                        id: Date.now() + 1,
                        role: 'assistant',
                        content: res.data?.content || res.data?.reply || JSON.stringify(res.data) || '无响应',
                        timestamp: new Date().toLocaleTimeString(),
                    }
                    setMessages(prev => [...prev, aiMsg])
                } catch (e) {
                    const aiMsg = {
                        id: Date.now() + 1,
                        role: 'assistant',
                        content: '抱歉，执行过程中出现错误，请稍后重试。',
                        timestamp: new Date().toLocaleTimeString(),
                    }
                    setMessages(prev => [...prev, aiMsg])
                }
            }
        } catch (e) {
            message.error('执行失败')
            setLoading(false)
            setStreaming(false)
        }
    }, [inputValue, sceneCode, loading, streaming, execConfig, currentReply])

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSend()
        }
    }

    const handleClear = () => {
        setMessages([])
        message.success('对话已清空')
    }

    const handleStop = () => {
        eventSourceRef.current?.close()
        if (currentReply) {
            const aiMsg = {
                id: Date.now() + 1,
                role: 'assistant',
                content: currentReply,
                timestamp: new Date().toLocaleTimeString(),
            }
            setMessages(prev => [...prev, aiMsg])
        }
        setCurrentReply('')
        setStreaming(false)
        setLoading(false)
        message.info('已停止生成')
    }

    const handleCopyMessage = (content) => {
        navigator.clipboard.writeText(content).then(() => message.success('已复制')).catch(() => {
        })
    }

    const handleLoadHistory = (item) => {
        if (item.messages) {
            setMessages(Array.isArray(item.messages) ? item.messages : [])
        }
        setHistoryOpen(false)
        message.success('已加载历史记录')
    }

    const renderMessageContent = (content) => {
        // 简单渲染：支持代码块和换行
        return content.split('\n').map((line, i) => {
            if (line.startsWith('```') && line.endsWith('```')) {
                return <pre key={i} style={{
                    background: '#f5f5f5',
                    padding: 8,
                    borderRadius: 6,
                    fontSize: 12,
                    overflow: 'auto'
                }}>{line.slice(3, -3)}</pre>
            }
            return <div key={i} style={{lineHeight: 1.7}}>{line}</div>
        })
    }

    return (
        <div style={{display: 'flex', flexDirection: 'column', height: 'calc(100vh - 140px)', padding: 0}}>
            {/* 顶部：场景选择栏 */}
            <div style={{
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                padding: '14px 20px',
                display: 'flex',
                alignItems: 'center',
                gap: 16,
                flexWrap: 'wrap',
            }}>
                <Select
                    value={productCode || undefined}
                    onChange={v => {
                        setProductCode(v);
                        setSceneCode('')
                    }}
                    placeholder="选择产品"
                    allowClear
                    style={{width: 180, borderRadius: 8}}
                    size="large"
                >
                    {products.map(p => <Option key={p.productCode} value={p.productCode}>{p.productName}</Option>)}
                </Select>

                <Select
                    value={sceneCode || undefined}
                    onChange={v => setSceneCode(v)}
                    placeholder="选择场景"
                    allowClear
                    style={{width: 200, borderRadius: 8}}
                    size="large"
                    disabled={!productCode}
                >
                    {scenes.map(s => <Option key={s.sceneCode} value={s.sceneCode}>{s.sceneName}</Option>)}
                </Select>

                {sceneCode && (
                    <Tag icon={<CheckCircleOutlined/>} color="green" style={{borderRadius: 20}}>
                        {scenes.find(s => s.sceneCode === sceneCode)?.sceneName || sceneCode} 已就绪
                    </Tag>
                )}

                <div style={{flex: 1}}/>

                <Space>
                    <Tooltip title="执行配置">
                        <Button icon={<ThunderboltOutlined/>} onClick={() => setConfigModalVisible(true)}
                                style={{borderRadius: 8}}>
                            参数
                        </Button>
                    </Tooltip>
                    <Tooltip title="历史记录">
                        <Button icon={<HistoryOutlined/>} onClick={() => setHistoryOpen(true)}
                                style={{borderRadius: 8}}>
                            历史
                        </Button>
                    </Tooltip>
                    <Tooltip title="清空对话">
                        <Button icon={<ClearOutlined/>} onClick={handleClear} style={{borderRadius: 8}}>
                            清空
                        </Button>
                    </Tooltip>
                </Space>
            </div>

            {/* 中部：消息区域 */}
            <div style={{
                flex: 1,
                overflowY: 'auto',
                padding: '16px 20px',
                background: '#f5f5f5',
                display: 'flex',
                flexDirection: 'column',
            }}>
                {!sceneCode ? (
                    <div style={{flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                        <Empty
                            image={<div style={{fontSize: 48, opacity: 0.3}}><RobotOutlined/></div>}
                            description={
                                <div>
                                    <div style={{fontWeight: 500, fontSize: 15, marginBottom: 6}}>请先选择产品与场景
                                    </div>
                                    <div style={{color: '#999', fontSize: 13}}>在顶部选择要执行的场景，即可开始体验</div>
                                </div>
                            }
                            style={{padding: 40}}
                        />
                    </div>
                ) : (
                    <>
                        {/* 欢迎消息 */}
                        {messages.length === 0 && (
                            <div style={{textAlign: 'center', padding: '30px 0 20px'}}>
                                <Avatar
                                    size={56}
                                    icon={<RobotOutlined/>}
                                    style={{
                                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                        margin: '0 auto'
                                    }}
                                />
                                <Title level={5} style={{marginTop: 12, marginBottom: 4}}>
                                    {scenes.find(s => s.sceneCode === sceneCode)?.sceneName || '场景助手'}
                                </Title>
                                <Text type="secondary">
                                    {sceneConfig?.description || scenes.find(s => s.sceneCode === sceneCode)?.description || '开始对话吧！'}
                                </Text>
                            </div>
                        )}

                        {/* 消息列表 */}
                        <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                            {messages.map((msg) => (
                                <div key={msg.id} style={{display: 'flex', gap: 12, alignItems: 'flex-start'}}>
                                    <Avatar
                                        size={36}
                                        icon={msg.role === 'user' ? <UserOutlined/> : <RobotOutlined/>}
                                        style={{
                                            background: msg.role === 'user' ? '#1890ff' : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            flexShrink: 0,
                                        }}
                                    />
                                    <div style={{flex: 1}}>
                                        <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4}}>
                                            <Text strong
                                                  style={{fontSize: 13}}>{msg.role === 'user' ? '我' : '助手'}</Text>
                                            <Text type="secondary" style={{fontSize: 11}}>{msg.timestamp}</Text>
                                            {msg.role === 'assistant' && (
                                                <Tooltip title="复制回复">
                                                    <Button size="small" type="text" icon={<CopyOutlined/>}
                                                            onClick={() => handleCopyMessage(msg.content)}
                                                            style={{padding: 0}}/>
                                                </Tooltip>
                                            )}
                                        </div>
                                        <Card
                                            size="small"
                                            style={{
                                                background: msg.role === 'user' ? '#1890ff' : '#fff',
                                                color: msg.role === 'user' ? '#fff' : '#333',
                                                borderRadius: 12,
                                                boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
                                            }}
                                            styles={{body: {padding: '10px 14px'}}}
                                        >
                                            {renderMessageContent(msg.content)}
                                        </Card>
                                    </div>
                                </div>
                            ))}

                            {/* 流式输出中 */}
                            {streaming && currentReply && (
                                <div style={{display: 'flex', gap: 12, alignItems: 'flex-start'}}>
                                    <Avatar
                                        size={36}
                                        icon={<RobotOutlined/>}
                                        style={{
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            flexShrink: 0
                                        }}
                                    />
                                    <div style={{flex: 1}}>
                                        <Card
                                            size="small"
                                            style={{
                                                background: '#fff',
                                                borderRadius: 12,
                                                boxShadow: '0 1px 4px rgba(0,0,0,0.08)'
                                            }}
                                            styles={{body: {padding: '10px 14px'}}}
                                        >
                                            {renderMessageContent(currentReply)}
                                            <span style={{
                                                display: 'inline-block',
                                                width: 8,
                                                height: 16,
                                                background: '#667eea',
                                                marginLeft: 2,
                                                animation: 'blink 1s infinite',
                                                borderRadius: 2
                                            }}/>
                                        </Card>
                                    </div>
                                </div>
                            )}

                            {/* 加载中 */}
                            {loading && !streaming && (
                                <div style={{display: 'flex', gap: 12, alignItems: 'flex-start'}}>
                                    <Avatar
                                        size={36}
                                        icon={<RobotOutlined/>}
                                        style={{
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            flexShrink: 0
                                        }}
                                    />
                                    <Card
                                        size="small"
                                        style={{
                                            background: '#fff',
                                            borderRadius: 12,
                                            boxShadow: '0 1px 4px rgba(0,0,0,0.08)'
                                        }}
                                        styles={{body: {padding: '10px 14px'}}}
                                    >
                                        <Spin indicator={<LoadingOutlined style={{fontSize: 16, color: '#667eea'}}
                                                                          spin/>}/>
                                        <Text type="secondary" style={{marginLeft: 8}}>正在思考...</Text>
                                    </Card>
                                </div>
                            )}
                        </div>
                        <div ref={messagesEndRef}/>
                    </>
                )}
            </div>

            {/* 底部：输入区 */}
            <div style={{
                background: '#fff',
                padding: '12px 20px',
                borderTop: '1px solid #f0f0f0',
                display: 'flex',
                gap: 12,
                alignItems: 'flex-end',
            }}>
                <TextArea
                    ref={inputRef}
                    value={inputValue}
                    onChange={e => setInputValue(e.target.value)}
                    onKeyDown={handleKeyPress}
                    placeholder={sceneCode ? '输入消息，Enter 发送，Shift+Enter 换行...' : '请先选择场景...'}
                    disabled={!sceneCode || streaming}
                    autoSize={{minRows: 1, maxRows: 4}}
                    style={{flex: 1, borderRadius: 20, resize: 'none'}}
                />
                <div style={{display: 'flex', flexDirection: 'column', gap: 4}}>
                    {streaming ? (
                        <Tooltip title="停止生成">
                            <Button danger icon={<CloseCircleOutlined/>} onClick={handleStop}
                                    style={{borderRadius: 20}}/>
                        </Tooltip>
                    ) : (
                        <Tooltip title="发送">
                            <Button
                                type="primary"
                                icon={<SendOutlined/>}
                                onClick={handleSend}
                                disabled={!sceneCode || !inputValue.trim() || loading}
                                style={{
                                    borderRadius: 20,
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    border: 'none'
                                }}
                            />
                        </Tooltip>
                    )}
                </div>
            </div>

            {/* 历史记录 Modal */}
            <Modal
                title={<span><HistoryOutlined style={{marginRight: 8}}/>对话历史</span>}
                open={historyOpen}
                onCancel={() => setHistoryOpen(false)}
                footer={null}
                size="large"
            >
                {chatHistory.length === 0 ? (
                    <Empty description="暂无历史记录"/>
                ) : (
                    <List
                        dataSource={chatHistory}
                        renderItem={(item, idx) => (
                            <List.Item
                                actions={[
                                    <Button key="load" size="small" icon={<ReloadOutlined/>}
                                            onClick={() => handleLoadHistory(item)}>加载</Button>
                                ]}
                            >
                                <List.Item.Meta
                                    avatar={<Avatar icon={<ChatIcon/>} style={{background: '#667eea'}}/>}
                                    title={`会话 ${idx + 1} — ${item.startTime || ''}`}
                                    description={<Text type="secondary"
                                                       style={{fontSize: 12}}>{item.messages?.length || 0} 条消息</Text>}
                                />
                            </List.Item>
                        )}
                    />
                )}
            </Modal>

            {/* 执行配置 Modal */}
            <Modal
                title={<span><ThunderboltOutlined style={{marginRight: 8}}/>执行参数配置</span>}
                open={configModalVisible}
                onCancel={() => setConfigModalVisible(false)}
                onOk={() => {
                    setConfigModalVisible(false);
                    message.success('参数已保存')
                }}
                okText="保存"
            >
                <Form form={form} layout="vertical" initialValues={execConfig}>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="temperature" label="Temperature (随机性)">
                                <InputNumber min={0} max={2} step={0.1} style={{width: '100%'}}/>
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="maxTokens" label="Max Tokens (最大字数)">
                                <InputNumber min={100} max={8192} step={100} style={{width: '100%'}}/>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="topP" label="Top P">
                        <InputNumber min={0} max={1} step={0.05} style={{width: '100%'}}/>
                    </Form.Item>
                </Form>
            </Modal>

            <style>{`
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0; }
        }
      `}</style>
        </div>
    )
}

// 小图标组件
const ChatIcon = () => <RobotOutlined/>

export default SceneExecutorPage
