import {useState} from 'react'
import {Avatar, Button, Tag} from 'antd'
import {
    CheckCircleFilled,
    ClockCircleOutlined,
    CopyOutlined,
    ExclamationCircleFilled,
    RobotOutlined,
    UserOutlined
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

// 消息气泡进场动画（share 页与就地聊天抽屉共用）
export const ChatMessageStyles = () => (
    <style>{`
        @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
    `}</style>
)

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
export const MessageBubble = ({msg}) => {
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

export default MessageBubble
