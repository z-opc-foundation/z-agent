import {useCallback, useEffect, useRef, useState} from 'react'
import {useParams} from 'react-router-dom'
import {Avatar, Button, Card, Input, List, Modal, Spin, Tag, Tooltip} from 'antd'
import {
    CheckCircleFilled,
    ClearOutlined,
    ClockCircleOutlined,
    CopyOutlined,
    ExclamationCircleFilled,
    LoadingOutlined,
    RobotOutlined,
    SendOutlined,
    UserOutlined
} from '@ant-design/icons'
import {agentApi} from '../../api'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

const {TextArea} = Input

// 简单的代码块样式组件
const CodeBlock = ({children, className}) => {
    const code = String(children).replace(/\n$/, '')
    const [copied, setCopied] = useState(false)

    const handleCopy = () => {
        navigator.clipboard.writeText(code).then(() => {
            setCopied(true)
            setTimeout(() => setCopied(false), 2000)
        })
    }

    return (
        <div style={{position: 'relative', margin: '8px 0'}}>
            <Button
                size="small"
                icon={copied ? <CheckCircleFilled/> : <CopyOutlined/>}
                onClick={handleCopy}
                style={{position: 'absolute', right: 8, top: 8, opacity: 0.7}}
            >
                {copied ? '已复制' : '复制'}
            </Button>
            <pre style={{
                background: '#1e1e1e',
                color: '#d4d4d4',
                borderRadius: 6,
                padding: '12px 16px',
                overflow: 'auto',
                fontSize: 13,
                lineHeight: 1.5,
                fontFamily: 'Consolas, Monaco, "Courier New", monospace',
            }}>
        <code className={className}>{code}</code>
      </pre>
        </div>
    )
}

// 消息气泡
const MessageBubble = ({msg}) => {
    const isUser = msg.role === 'user'
    const isError = msg.status === 'FAILED'

    return (
        <div style={{
            display: 'flex',
            justifyContent: isUser ? 'flex-end' : 'flex-start',
            marginBottom: 16,
            animation: 'fadeIn 0.3s ease',
        }}>
            {!isUser && (
                <Avatar icon={<RobotOutlined/>} size={36}
                        style={{marginRight: 10, background: '#1890ff', flexShrink: 0}}/>
            )}
            <div style={{
                maxWidth: '72%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: isUser ? 'flex-end' : 'flex-start',
            }}>
                {isError && (
                    <Tag color="red" icon={<ExclamationCircleFilled/>} style={{marginBottom: 4}}>
                        出错了
                    </Tag>
                )}
                {isUser ? (
                    <div style={{
                        padding: '10px 14px',
                        borderRadius: '16px 16px 4px 16px',
                        background: 'linear-gradient(135deg, #1890ff, #096dd9)',
                        color: '#fff',
                        lineHeight: 1.6,
                        fontSize: 15,
                        boxShadow: '0 2px 8px rgba(24,144,255,0.3)',
                    }}>
                        {msg.content}
                    </div>
                ) : (
                    <div style={{
                        padding: '12px 16px',
                        borderRadius: '16px 16px 16px 4px',
                        background: '#fff',
                        color: '#333',
                        lineHeight: 1.7,
                        fontSize: 15,
                        boxShadow: '0 1px 4px rgba(0,0,0,0.1)',
                        border: '1px solid #f0f0f0',
                    }}>
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            components={{
                                code({node, inline, className, children, ...props}) {
                                    if (inline) {
                                        return <code style={{
                                            background: '#f5f5f5',
                                            padding: '2px 6px',
                                            borderRadius: 4,
                                            fontFamily: 'monospace',
                                            fontSize: '0.9em',
                                            color: '#c7254e'
                                        }} {...props}>{children}</code>
                                    }
                                    return <CodeBlock className={className}>{children}</CodeBlock>
                                },
                                p({children}) {
                                    return <p style={{margin: '0 0 8px 0'}}>{children}</p>
                                },
                                ul({children}) {
                                    return <ul style={{margin: '4px 0', paddingLeft: 20}}>{children}</ul>
                                },
                                ol({children}) {
                                    return <ol style={{margin: '4px 0', paddingLeft: 20}}>{children}</ol>
                                },
                                li({children}) {
                                    return <li style={{marginBottom: 2}}>{children}</li>
                                },
                                strong({children}) {
                                    return <strong style={{color: '#1890ff'}}>{children}</strong>
                                },
                                h1({children}) {
                                    return <h1 style={{fontSize: 18, margin: '8px 0'}}>{children}</h1>
                                },
                                h2({children}) {
                                    return <h2 style={{fontSize: 16, margin: '8px 0'}}>{children}</h2>
                                },
                                h3({children}) {
                                    return <h3 style={{fontSize: 14, margin: '6px 0'}}>{children}</h3>
                                },
                                blockquote({children}) {
                                    return <blockquote style={{
                                        borderLeft: '3px solid #1890ff',
                                        margin: '8px 0',
                                        paddingLeft: 12,
                                        color: '#666'
                                    }}>{children}</blockquote>
                                },
                            }}
                        >
                            {msg.content}
                        </ReactMarkdown>
                    </div>
                )}
                {msg.latencyMs && !isUser && !isError && (
                    <span style={{fontSize: 11, color: '#bbb', marginTop: 4}}>
            <ClockCircleOutlined style={{marginRight: 3}}/>
                        {msg.latencyMs}ms
          </span>
                )}
            </div>
            {isUser && (
                <Avatar icon={<UserOutlined/>} size={36}
                        style={{marginLeft: 10, background: '#52c41a', flexShrink: 0}}/>
            )}
        </div>
    )
}

// 欢迎页
const WelcomeScreen = ({appName, appDesc}) => (
    <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '60vh',
        color: '#666'
    }}>
        <div style={{
            fontSize: 48,
            marginBottom: 16,
            background: 'linear-gradient(135deg, #1890ff, #52c41a)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            fontWeight: 'bold'
        }}>
            {appName || 'Agent'}
        </div>
        {appDesc && <p style={{color: '#999', marginBottom: 24}}>{appDesc}</p>}
        <div style={{
            display: 'flex',
            gap: 12,
            flexWrap: 'wrap',
            justifyContent: 'center',
            maxWidth: 500
        }}>
            {['有什么可以帮助你的？', '告诉我你的问题', '试试问我任何事情'].map((tip, i) => (
                <Tag key={i} style={{padding: '6px 12px', fontSize: 13, cursor: 'pointer', borderRadius: 16}}
                     onClick={() => {
                         const event = new CustomEvent('insertTip', {detail: tip})
                         window.dispatchEvent(event)
                     }}>
                    {tip}
                </Tag>
            ))}
        </div>
    </div>
)

const AgentSharePage = () => {
    const {shareCode} = useParams()
    const [loading, setLoading] = useState(true)
    const [instanceCode, setInstanceCode] = useState('')
    const [appName, setAppName] = useState('')
    const [appDesc, setAppDesc] = useState('')
    const [messages, setMessages] = useState([])
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [connected, setConnected] = useState(false)
    const [errorMsg, setErrorMsg] = useState('')
    const [insufficientInfo, setInsufficientInfo] = useState(false)
    const bottomRef = useRef(null)
    const textareaRef = useRef(null)
    const insertTipEventRef = useRef(null)

    useEffect(() => {
        verifyAndLoad()
        // 监听快速插入提示
        const handler = (e) => setInputValue(prev => prev ? prev : e.detail)
        window.addEventListener('insertTip', handler)
        return () => window.removeEventListener('insertTip', handler)
    }, [shareCode])

    useEffect(() => {
        bottomRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages])

    const verifyAndLoad = async () => {
        setLoading(true)
        setErrorMsg('')
        try {
            const res = await agentApi.shareVerify({params: {shareCode}})
            if (res) {
                setInstanceCode(res.instanceCode)
                setAppName(res.appName || 'Agent')
                setAppDesc(res.appDesc || '')
                setConnected(true)
                // 加载历史消息
                try {
                    const historyRes = await agentApi.chatHistory({params: {instanceCode: res.instanceCode, limit: 50}})
                    if (historyRes.data && historyRes.data.length > 0) {
                        const historyMsgs = []
                        historyRes.data.forEach(h => {
                            historyMsgs.push({
                                id: `u_${h.id}`,
                                role: 'user',
                                content: h.userMessage,
                                time: h.gmtCreate,
                                status: 'SUCCESS',
                            })
                            historyMsgs.push({
                                id: `a_${h.id}`,
                                role: 'assistant',
                                content: h.assistantMessage,
                                time: h.gmtCreate,
                                status: h.status,
                                latencyMs: h.latencyMs,
                            })
                        })
                        setMessages(historyMsgs)
                    }
                } catch (e) {
                    console.warn('加载历史失败', e)
                }
            }
        } catch (e) {
            setErrorMsg('分享链接无效或已过期')
            setInsufficientInfo(true)
        }
        setLoading(false)
    }

    const handleSend = useCallback(async () => {
        const content = inputValue.trim()
        if (!content || sending) return

        const userMsgId = `u_${Date.now()}`
        const userMsg = {id: userMsgId, role: 'user', content, status: 'SUCCESS'}
        setMessages(prev => [...prev, userMsg])
        setInputValue('')
        setSending(true)

        // 立即添加一条助手占位
        const assistantMsgId = `a_${Date.now()}`
        let assistantContent = ''
        setMessages(prev => [...prev, {
            id: assistantMsgId,
            role: 'assistant',
            content: '',
            status: 'LOADING',
        }])

        try {
            // 优先使用流式接口 (SSE)
            const token = localStorage.getItem('token')
            const streamUrl = `/api/agent/chat/stream?instanceCode=${encodeURIComponent(instanceCode)}&message=${encodeURIComponent(content)}&userId=visitor&userName=${encodeURIComponent('游客')}${token ? `&token=${encodeURIComponent(token)}` : ''}`
            const eventSource = new EventSource(streamUrl)

            eventSource.onmessage = (e) => {
                if (e.data === '[DONE]') {
                    eventSource.close()
                    setMessages(prev => prev.map(m =>
                        m.id === assistantMsgId
                            ? {...m, content: assistantContent || '好的，我明白了。', status: 'SUCCESS'}
                            : m
                    ))
                    setSending(false)
                } else {
                    assistantContent += e.data
                    setMessages(prev => prev.map(m =>
                        m.id === assistantMsgId
                            ? {...m, content: assistantContent, status: 'LOADING'}
                            : m
                    ))
                }
            }

            eventSource.onerror = (e) => {
                eventSource.close()
                setMessages(prev => prev.map(m =>
                    m.id === assistantMsgId
                        ? {...m, content: '抱歉，AI 服务暂时不可用，请稍后重试。', status: 'FAILED'}
                        : m
                ))
                setSending(false)
            }
        } catch (e) {
            setMessages(prev => prev.map(m =>
                m.id === assistantMsgId
                    ? {...m, content: '抱歉，AI 服务暂时不可用，请稍后重试。', status: 'FAILED'}
                    : m
            ))
            setSending(false)
        }
        textareaRef.current?.focus()
    }, [inputValue, sending, instanceCode])

    const handleClear = () => {
        Modal.confirm({
            title: '清空对话',
            content: '确定清空当前对话记录？此操作不可恢复。',
            okText: '清空',
            okButtonProps: {danger: true},
            onOk: () => {
                setMessages([])
                agentApi.chatClear({params: {instanceCode}}).catch(() => {
                })
            }
        })
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSend()
        }
    }

    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                background: '#f0f2f5'
            }}>
                <Spin indicator={<LoadingOutlined style={{fontSize: 32}} spin/>} description="正在连接..."/>
            </div>
        )
    }

    if (insufficientInfo || errorMsg) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                background: '#f0f2f5'
            }}>
                <Card style={{textAlign: 'center', maxWidth: 400}}>
                    <ExclamationCircleFilled style={{fontSize: 48, color: '#ff4d4f', marginBottom: 16}}/>
                    <h3>{errorMsg || '无法访问此应用'}</h3>
                    <p style={{color: '#999'}}>可能的原因：链接已失效、应用已被删除或访问权限不足。</p>
                    <Button type="primary" href="/" icon={<RobotOutlined/>}>返回首页</Button>
                </Card>
            </div>
        )
    }

    return (
        <div style={{
            height: '100vh',
            display: 'flex',
            flexDirection: 'column',
            background: 'linear-gradient(180deg, #f0f2f5 0%, #fff 100%)'
        }}>
            {/* Header */}
            <div style={{
                borderBottom: '1px solid #e8e8e8',
                background: '#fff',
                padding: '12px 24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexShrink: 0
            }}>
                <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <Avatar shape="square" size={36} icon={<RobotOutlined/>} style={{background: '#1890ff'}}/>
                    <div>
                        <div style={{fontWeight: 600, fontSize: 16}}>{appName}</div>
                        {appDesc && <div style={{fontSize: 12, color: '#999'}}>{appDesc}</div>}
                    </div>
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
          <span style={{fontSize: 12, color: connected ? '#52c41a' : '#ff4d4f'}}>
            <CheckCircleFilled style={{marginRight: 4}}/>
              {connected ? '已连接' : '未连接'}
          </span>
                    <Tooltip title="清空对话">
                        <Button size="small" icon={<ClearOutlined/>} onClick={handleClear}
                                disabled={messages.length === 0}>
                            清空
                        </Button>
                    </Tooltip>
                </div>
            </div>

            {/* Chat Area */}
            <div style={{flex: 1, overflowY: 'auto', padding: '16px 24px'}}>
                {messages.length === 0 ? (
                    <WelcomeScreen appName={appName} appDesc={appDesc}/>
                ) : (
                    <>
                        <List
                            dataSource={messages}
                            renderItem={(item) => <MessageBubble msg={item}/>}
                            locale={{emptyText: ''}}
                        />
                    </>
                )}
                <div ref={bottomRef}/>
            </div>

            {/* Quick suggestions */}
            {messages.length === 0 && (
                <div style={{padding: '0 24px 8px', display: 'flex', gap: 8, flexWrap: 'wrap'}}>
                    {['你好', '你能做什么？', '介绍下自己'].map((t, i) => (
                        <Button key={i} size="small" type="default" onClick={() => setInputValue(t)}>{t}</Button>
                    ))}
                </div>
            )}

            {/* Input Area */}
            <div style={{
                borderTop: '1px solid #e8e8e8',
                background: '#fff',
                padding: '12px 24px 20px',
                flexShrink: 0
            }}>
                <div style={{display: 'flex', gap: 12, alignItems: 'flex-end'}}>
                    <TextArea
                        ref={textareaRef}
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                        autoSize={{minRows: 1, maxRows: 4}}
                        style={{flex: 1, borderRadius: 20}}
                        disabled={sending || !connected}
                    />
                    <Button
                        type="primary"
                        icon={sending ? <LoadingOutlined/> : <SendOutlined/>}
                        onClick={handleSend}
                        loading={sending}
                        disabled={!inputValue.trim() || !connected}
                        style={{borderRadius: 20, width: 56, height: 36}}
                    />
                </div>
                <div style={{fontSize: 11, color: '#ccc', marginTop: 6, textAlign: 'center'}}>
                    AI 助手可能会产生不准确的信息，请仔细甄别
                </div>
            </div>

            <style>{`
        @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
        </div>
    )
}

export default AgentSharePage
