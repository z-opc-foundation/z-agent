import {useEffect, useState} from 'react'
import {Button, Card, Col, message, Modal, Row, Tag, Tooltip, Typography} from 'antd'
import {
    CloudDownloadOutlined,
    EyeOutlined,
    ReloadOutlined,
    RocketOutlined,
    SearchOutlined,
    StarOutlined,
} from '@ant-design/icons'
import {templateApi} from '@/agent/api'
import {EmptyState, PageHeader} from '@/common/components/ui'

const {Text, Paragraph, Title} = Typography

const CATEGORIES = {
    GENERAL: '通用', CUSTOMER_SERVICE: '客服', CODE: '代码',
    DATA: '数据', EDUCATION: '教育', MARKETING: '营销',
    HEALTHCARE: '医疗', FINANCE: '金融', HR: '人力资源',
}

const categoryColor = (c) => ({
    GENERAL: 'default', CUSTOMER_SERVICE: 'blue', CODE: 'cyan',
    DATA: 'green', EDUCATION: 'purple', MARKETING: 'orange',
    HEALTHCARE: 'red', FINANCE: 'gold', HR: 'magenta',
})[c] || 'default'

const TemplateMarket = () => {
    const [records, setRecords] = useState([])
    const [loading, setLoading] = useState(false)
    const [category, setCategory] = useState(null)
    const [fModal, setFModal] = useState(null)

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await templateApi.page({
                category: category || undefined,
                page: 1,
                size: 50,
            })
            const data = res?.data || res
            setRecords(data?.records || (Array.isArray(data) ? data : []))
        } catch (e) {
            message.error('加载失败: ' + (e?.message || e))
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchData()
    }, [category])

    const handleFork = async (tpl) => {
        setFModal(null)
        try {
            const res = await templateApi.fork({params: {id: tpl.id, appName: tpl.name + ' (我的)'}})
            const data = res?.data || res
            message.success(`从 "${tpl.name}" 创建成功: ${data?.newAppCode || ''}`)
            fetchData()
        } catch (e) {
            message.error('Fork 失败: ' + (e?.message || e))
        }
    }

    return (
        <div>
            <PageHeader
                title="模板市场"
                subtitle="浏览和使用 Agent 模板，快速创建应用"
            />
            {/* 分类栏 */}
            <div style={{
                display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap',
                background: '#fff', padding: '12px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 16
            }}>
                <Button icon={<SearchOutlined/>} type={!category ? 'primary' : 'default'}
                        onClick={() => setCategory(null)} size="small">
                    全部
                </Button>
                {Object.entries(CATEGORIES).map(([k, v]) => (
                    <Button key={k} size="small"
                            type={category === k ? 'primary' : 'default'}
                            onClick={() => setCategory(k)}>
                        {v}
                    </Button>
                ))}
                <Button icon={<ReloadOutlined/>} onClick={fetchData} size="small" style={{marginLeft: 'auto'}}>
                    刷新
                </Button>
                <Text type="secondary" style={{fontSize: 12}}>
                    FEATURE013 T3.3 · Agent 模板市场
                </Text>
            </div>

            <Row gutter={[16, 16]}>
                {records.length === 0 && !loading && (
                    <Col span={24}>
                        <EmptyState title="暂无模板" description="当前分类下没有可用模板"/>
                    </Col>
                )}
                {records.map(tpl => (
                    <Col xs={24} sm={12} md={8} lg={6} key={tpl.id}>
                        <Card
                            hoverable
                            style={{borderRadius: 10, height: '100%'}}
                            loading={loading}
                            actions={[
                                <Tooltip title="一键使用此模板创建 Agent App">
                                    <Button type="primary" size="small"
                                            icon={<RocketOutlined/>}
                                            onClick={() => setFModal(tpl)}>
                                        使用模板
                                    </Button>
                                </Tooltip>,
                            ]}
                        >
                            <div style={{textAlign: 'center', marginBottom: 8}}>
                                <span style={{fontSize: 40}}>{tpl.icon || '📋'}</span>
                            </div>
                            <Title level={5} style={{textAlign: 'center', marginBottom: 4}}>{tpl.name}</Title>
                            <div style={{textAlign: 'center', marginBottom: 8}}>
                                <Tag
                                    color={categoryColor(tpl.category)}>{CATEGORIES[tpl.category] || tpl.category}</Tag>
                                {tpl.isOfficial === 1 && <Tag color="red">官方</Tag>}
                            </div>
                            <Paragraph type="secondary" ellipsis={{rows: 3}} style={{fontSize: 13, marginBottom: 8}}>
                                {tpl.description}
                            </Paragraph>
                            <div style={{display: 'flex', justifyContent: 'space-around', fontSize: 12, color: '#999'}}>
                                <span><CloudDownloadOutlined/> {tpl.downloadCount}</span>
                                <span><EyeOutlined/> {tpl.viewCount}</span>
                                <span><StarOutlined/> {tpl.rating?.toFixed(1) || '5.0'} ({tpl.ratingCount})</span>
                            </div>
                            <div style={{fontSize: 11, color: '#bbb', textAlign: 'center', marginTop: 6}}>
                                {tpl.authorName || tpl.author || '系统'}
                            </div>
                        </Card>
                    </Col>
                ))}
            </Row>

            <Modal
                open={!!fModal}
                onCancel={() => setFModal(null)}
                title={fModal ? `使用模板: ${fModal.name}` : ''}
                onOk={() => handleFork(fModal)}
                okText="确认创建"
                cancelText="取消"
            >
                <Paragraph>
                    将从模板 <Text strong>{fModal?.name}</Text> 创建新的 Agent 应用。
                    创建后可在 Agent 应用列表中查看和配置。
                </Paragraph>
                {fModal?.requiredPlugins && (
                    <Paragraph type="warning">需插件: {fModal.requiredPlugins}</Paragraph>
                )}
            </Modal>
        </div>
    )
}

export default TemplateMarket
