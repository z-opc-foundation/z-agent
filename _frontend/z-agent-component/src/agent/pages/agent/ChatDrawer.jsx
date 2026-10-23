import {useEffect, useRef, useState} from 'react'
import {Button, Drawer, Input, Modal, Spin, Tag, Tooltip} from 'antd'
import {
    ClearOutlined,
    ExclamationCircleFilled,
    LoadingOutlined,
    SendOutlined,
} from '@ant-design/icons'
import {agentApi} from '../../api'
import MessageBubble, {ChatMessageStyles} from './ChatUI.jsx'

const {TextArea} = Input

// 就地聊天用的固定会话身份（与分享页 visitor、widget 匿名身份互不串台）
const CONSOLE_USER_ID = 'console'
const CONSOLE_USER_NAME = '控制台'

/**
 * Agent 应用就地聊天抽屉 — 在应用列表里直接验证应用，不跳分享页。
 *
 * 后端契约：instanceCode = app_code（发布/开启分享时由 ensurePublicInstance 创建的
 * 虚拟实例），SSE 流式端点 GET /api/agent/chat/stream。
 */
const AgentChatDrawer = ({open, app, onClose}) => {
    const [messages, setMessages] = useState([])
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [historyLoading, setHistoryLoading] = useState(false)
    const [historyError, setHistoryError] = useState('')
    const bottomRef = useRef(null)
    const textareaRef = useRef(null)

    const instanceCode = app?.app_code || ''

    useEffect(() => {
        if (!open || !instanceCode) return
        let cancelled = false
        setMessages([])
        setHistoryError('')
        setHistoryLoading(true)
        agentApi.chatHistory({params: {instanceCode, limit: 50}})
            .then((res) => {
                if (cancelled) return
                const rows = res?.data || []
                const historyMsgs = []
                rows.forEach((h) => {
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
            })
            .catch((e) => {
                if (!cancelled) setHistoryError(e?.message || '历史加载失败')
            })
            .finally(() => {
                if (!cancelled) setHistoryLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [open, instanceCode])

    useEffect(() => {
        bottomRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages])

    const handleSend = () => {
        const content = inputValue.trim()
        if (!content || sending || !instanceCode) return

        const assistantMsgId = `a_${Date.now()}`
        setMessages((prev) => [
            ...prev,
            {id: `u_${Date.now()}`, role: 'user', content, status: 'SUCCESS'},
            {id: assistantMsgId, role: 'assistant', content: '', status: 'LOADING'},
        ])
        setInputValue('')
        setSending(true)

        // SSE 是浏览器原生 EventSource：无法带 Authorization 头，鉴权靠同源会话；
        // 后端按 instanceCode_userId 维度隔离会话历史。
        const streamUrl =
            `/api/agent/chat/stream?instanceCode=${encodeURIComponent(instanceCode)}` +
            `&message=${encodeURIComponent(content)}` +
            `&userId=${encodeURIComponent(CONSOLE_USER_ID)}` +
            `&userName=${encodeURIComponent(CONSOLE_USER_NAME)}`
        const eventSource = new EventSource(streamUrl)
        let assistantContent = ''

        eventSource.onmessage = (e) => {
            if (e.data === '[DONE]') {
                eventSource.close()
                setMessages((prev) => prev.map((m) =>
                    m.id === assistantMsgId ? {...m, content: assistantContent, status: 'SUCCESS'} : m
                ))
                setSending(false)
                textareaRef.current?.focus()
            } else {
                assistantContent += e.data
                setMessages((prev) => prev.map((m) =>
                    m.id === assistantMsgId ? {...m, content: assistantContent} : m
                ))
            }
        }

        eventSource.onerror = () => {
            eventSource.close()
            setMessages((prev) => prev.map((m) =>
                m.id === assistantMsgId
                    ? {
                        ...m,
                        content: m.content || '连接中断：应用可能未发布（未发布的应用没有运行实例），或 AI 服务暂不可用。',
                        status: assistantContent ? 'SUCCESS' : 'FAILED',
                    }
                    : m
            ))
            setSending(false)
        }
    }

    const handleClear = () => {
        Modal.confirm({
            title: '清空对话',
            content: '确定清空该应用在控制台身份下的对话记录？此操作不可恢复。',
            okText: '清空',
            okButtonProps: {danger: true},
            onOk: () => {
                setMessages([])
                agentApi.chatClear({params: {instanceCode}}).catch(() => {
                })
            },
        })
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSend()
        }
    }

    return (
        <Drawer
            title={app ? `对话验证 — ${app.app_name || app.app_code}` : '对话验证'}
            width={560}
            open={open}
            onClose={onClose}
            destroyOnHidden
            styles={{body: {padding: 0, display: 'flex', flexDirection: 'column'}}}
            extra={
                <Tooltip title="清空对话">
                    <Button size="small" icon={<ClearOutlined/>} onClick={handleClear}
                            disabled={messages.length === 0 || sending}>
                        清空
                    </Button>
                </Tooltip>
            }
        >
            <ChatMessageStyles/>
            <div style={{flex: 1, overflowY: 'auto', padding: '16px 20px', background: '#fafafa'}}>
                {app?.status !== 'PUBLISHED' && (
                    <Tag color="warning" style={{marginBottom: 12}}>
                        该应用尚未发布，聊天会提示「实例不存在」——先发布再验证
                    </Tag>
                )}
                {historyLoading ? (
                    <div style={{textAlign: 'center', padding: 32}}>
                        <Spin indicator={<LoadingOutlined style={{fontSize: 24}} spin/>}/>
                    </div>
                ) : historyError ? (
                    <div style={{textAlign: 'center', color: '#ff4d4f', padding: 24, fontSize: 13}}>
                        <ExclamationCircleFilled/> 历史加载失败：{historyError}
                    </div>
                ) : messages.length === 0 ? (
                    <div style={{textAlign: 'center', color: '#999', padding: '48px 0', fontSize: 13}}>
                        发一条消息，立即验证这个 Agent 的真实表现
                    </div>
                ) : (
                    messages.map((item) => <MessageBubble key={item.id} msg={item}/>)
                )}
                <div ref={bottomRef}/>
            </div>
            <div style={{
                borderTop: '1px solid #e8e8e8',
                background: '#fff',
                padding: '12px 20px 16px',
                flexShrink: 0,
            }}>
                <div style={{display: 'flex', gap: 10, alignItems: 'flex-end'}}>
                    <TextArea
                        ref={textareaRef}
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                        autoSize={{minRows: 1, maxRows: 4}}
                        style={{flex: 1, borderRadius: 16}}
                        disabled={sending}
                    />
                    <Button
                        type="primary"
                        icon={sending ? <LoadingOutlined/> : <SendOutlined/>}
                        onClick={handleSend}
                        loading={sending}
                        disabled={!inputValue.trim()}
                        style={{borderRadius: 16, width: 52, height: 34}}
                    />
                </div>
                <div style={{fontSize: 11, color: '#ccc', marginTop: 6, textAlign: 'center'}}>
                    控制台身份（console）下的会话与分享页游客会话相互独立
                </div>
            </div>
        </Drawer>
    )
}

export default AgentChatDrawer
