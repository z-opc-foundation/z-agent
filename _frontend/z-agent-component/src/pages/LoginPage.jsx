import { useState } from 'react'
import { Button, Card, Form, Input, message } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'

/**
 * 登录页（lead 008 §16）—— CTC/SSO 接入前的 mock 登录。
 *
 * mock 账号**不再内置任何字面量**（001_凭证管理规范：仓内不留明文凭据）：
 * 由宿主在构建期注入 VITE_Z_AGENT_MOCK_USER / VITE_Z_AGENT_MOCK_PASS，
 * 未注入 ⇒ 本页直接说明「未配置 mock 账号」，不做任何账号兜底。
 */
const MOCK_AUTH = {
    username: import.meta.env?.VITE_Z_AGENT_MOCK_USER || '',
    password: import.meta.env?.VITE_Z_AGENT_MOCK_PASS || '',
    user: { name: import.meta.env?.VITE_Z_AGENT_MOCK_USER || '', role: '管理员' },
}

export default function LoginPage() {
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (values) => {
        setLoading(true)
        try {
            if (!MOCK_AUTH.username || !MOCK_AUTH.password) {
                message.error('未配置 mock 账号（VITE_Z_AGENT_MOCK_USER / VITE_Z_AGENT_MOCK_PASS）')
                return
            }
            if (values.username === MOCK_AUTH.username && values.password === MOCK_AUTH.password) {
                localStorage.setItem('token', 'mock-' + MOCK_AUTH.username)
                localStorage.setItem('userInfo', JSON.stringify(MOCK_AUTH.user))
                message.success('登录成功（mock 模式，待 CTC/SSO 接入）')
                window.location.href = '/z-agent/home'
            } else {
                message.error('用户名或密码错误')
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{
            height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center',
            background: 'linear-gradient(135deg, #eef2ff 0%, #f5f3ff 50%, #eff6ff 100%)',
        }}>
            <Card style={{ width: 400, boxShadow: '0 10px 40px rgba(124,58,237,0.12)' }}>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                    <div style={{
                        width: 48, height: 48, borderRadius: 12, margin: '0 auto 12px',
                        background: 'linear-gradient(135deg, #7c3aed, #6d28d9)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        color: '#fff', fontSize: 24, fontWeight: 700,
                    }}>z</div>
                    <div style={{ fontSize: 18, fontWeight: 600, color: '#0f172a' }}>z-agent 管理台</div>
                </div>
                <Form onFinish={handleSubmit} size="large">
                    <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                        <Input prefix={<UserOutlined />} placeholder="用户名" />
                    </Form.Item>
                    <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 8 }}>
                        <Button type="primary" htmlType="submit" loading={loading} block
                                style={{ background: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)', border: 'none' }}>
                            登 录
                        </Button>
                    </Form.Item>
                    <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>
                        Mock 模式：账号由宿主注入（VITE_Z_AGENT_MOCK_USER / VITE_Z_AGENT_MOCK_PASS）
                    </div>
                </Form>
            </Card>
        </div>
    )
}
