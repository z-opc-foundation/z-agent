import {useEffect, useRef, useState} from 'react'
import {
    Alert,
    Avatar,
    Button,
    Card,
    Card as AntCard,
    Col,
    Divider,
    Drawer,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    QRCode,
    Row,
    Select,
    Space,
    Switch,
    Table,
    Tabs,
    Tag,
    Tooltip,
    Tree,
    TreeSelect,
    Typography
} from 'antd'
import {
    AppstoreFilled,
    BookOutlined,
    CheckCircleFilled,
    CopyOutlined,
    DeleteOutlined,
    DiffOutlined,
    EditOutlined,
    EyeOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    GlobalOutlined,
    LinkOutlined,
    LoadingOutlined,
    LockOutlined,
    PlusOutlined,
    ReloadOutlined,
    RobotOutlined,
    RocketOutlined,
    SettingOutlined,
    ShareAltOutlined,
    StarOutlined,
    UnorderedListOutlined
} from '@ant-design/icons'
import {agentApi, llmApi} from '../../api'
import AgentAppEditor from './editor/AgentAppEditor'
import {SearchInput} from '@/common/components/ui'

// 本文件读写的行键一律 snake_case（2026-09-25 逐字段实测，别"顺手改回 camelCase"）：
//   POST /api/agent/app/page   → {app_name, app_code, group_code, model_provider, gmt_create, share_enabled ...}
//   GET  /api/agent/group/tree → {group_code, group_name, parent_code ...}
//   请求体同向：{"app_name":"zzznotfound"} 过滤生效(0 行)，{"appName":...} 被忽略(仍 15 行)。
//   但 @RequestParam 不走 Jackson 命名：GET /agent/app/get?appCode= 200，?app_code= 400「缺少参数: appCode」。
// 页面状态/表单字段名/Form.Item name/antd treeData 的 node.* 仍用 camelCase —— 那半边是我们自己造的键。

const {TextArea} = Input
const {Option} = Select
const {TabPane} = Tabs
const {Text, Title} = Typography

// ===== 应用列表页 =====
const AgentAppPage = () => {
    const [selectedGroupCode, setSelectedGroupCode] = useState('')
    const [selectedGroupName, setSelectedGroupName] = useState('')
    const [refreshKey, setRefreshKey] = useState(0)
    const [editingApp, setEditingApp] = useState(null)
    const handleGroupRefresh = () => setRefreshKey(k => k + 1)

    if (editingApp) {
        return (
            <div style={{
                flex: 1,
                overflow: 'hidden',
                borderRadius: 8,
                background: '#fff',
                boxShadow: '0 1px 4px rgba(0,0,0,0.06)'
            }}>
                <AgentAppEditor
                    app={editingApp}
                    onBack={() => {
                        setEditingApp(null);
                        handleGroupRefresh()
                    }}
                    onSaved={() => {
                        setEditingApp(null);
                        handleGroupRefresh()
                    }}
                />
            </div>
        )
    }

    return (
        <div style={{display: 'flex', gap: 16, height: 'calc(100vh - 180px)', padding: 16}}>
            {/* 左侧：分组树 */}
            <Card size="small" style={{width: 300, overflow: 'auto', flexShrink: 0}}
                  title="分组"
                  extra={
                      <Button size="small" icon={<ReloadOutlined/>} onClick={handleGroupRefresh}/>
                  }>
                <GroupTree key={refreshKey}
                           selectedGroupCode={selectedGroupCode}
                           onGroupSelect={(code, name) => {
                               setSelectedGroupCode(code || '');
                               setSelectedGroupName(code ? (name || '') : '')
                           }}
                           onGroupRefresh={handleGroupRefresh}
                />
            </Card>

            {/* 右侧：应用列表 */}
            <Card size="small" style={{flex: 1, overflow: 'auto'}}
                  title={selectedGroupName || undefined}>
                <AppListPage selectedGroupCode={selectedGroupCode} onEditApp={setEditingApp}/>
            </Card>
        </div>
    )
}

// 模型数据（按供应商）
const MODELS_BY_PROVIDER = {
    ollama: [
        {code: 'qwen2.5:7b-instruct-q4_K_M', name: 'Qwen 2.5 7B'},
        {code: 'qwen3:8b', name: 'Qwen3 8B'},
        {code: 'llama3:latest', name: 'Llama 3'},
        {code: 'qwen2.5:14b-instruct-q4_K_M', name: 'Qwen 2.5 14B'},
    ],
    openai: [
        {code: 'gpt-4o', name: 'GPT-4o'},
        {code: 'gpt-4o-mini', name: 'GPT-4o Mini'},
        {code: 'gpt-4-turbo', name: 'GPT-4 Turbo'},
    ],
    dashscope: [
        {code: 'qwen-max', name: 'Qwen Max'},
        {code: 'qwen-plus', name: 'Qwen Plus'},
        {code: 'qwen-turbo', name: 'Qwen Turbo'},
    ],
}

// ===== 分组树辅助函数 =====
const buildGroupTree = (groups) => {
    const itemMap = {}
    groups.forEach(g => {
        itemMap[g.group_code] = {
            key: g.group_code,
            title: g.group_name,
            children: [],
        }
    })
    const roots = []
    groups.forEach(g => {
        const node = itemMap[g.group_code]
        if (g.parent_code && itemMap[g.parent_code]) {
            itemMap[g.parent_code].children.push(node)
        } else if (!g.parent_code) {
            roots.push(node)
        }
    })
    return roots
}

// ===== 分组树 =====
const GroupTree = ({selectedGroupCode, onGroupSelect, onGroupRefresh}) => {
    const [groups, setGroups] = useState([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editingGroup, setEditingGroup] = useState(null)
    const [parentForCreate, setParentForCreate] = useState(null)
    const [form] = Form.useForm()
    const inputRef = useRef(null)

    // 点击处理：选中/取消选中
    const handleNodeClick = (groupCode) => {
        // 点击已选中节点 → 取消选中
        onGroupSelect(groupCode === selectedGroupCode ? '' : groupCode, '')
    }

    const loadGroups = () => {
        setLoading(true)
        agentApi.groupTree().then(res => {
            setGroups(res?.data || res || [])
        }).finally(() => setLoading(false))
    }

    useEffect(() => {
        loadGroups()
    }, [])

    // 构造嵌套树数据
    const buildTreeData = () => {
        const itemMap = {}
        groups.forEach(g => {
            itemMap[g.group_code] = {
                key: g.group_code,
                title: g.group_name,
                groupCode: g.group_code,
                parentCode: g.parent_code || '',
                icon: selectedGroupCode === g.group_code ? <FolderOpenOutlined/> : <FolderOutlined/>,
                data: g,
                children: [],
            }
        })
        const roots = []
        groups.forEach(g => {
            const node = itemMap[g.group_code]
            if (g.parent_code && itemMap[g.parent_code]) {
                itemMap[g.parent_code].children.push(node)
            } else if (!g.parent_code) {
                roots.push(node)
            }
        })
        return roots
    }

    // 节点标题渲染
    const titleRender = (node) => {
        const data = groups.find(g => g.group_code === node.groupCode)
        return (
            <span
                style={{display: 'flex', alignItems: 'center', gap: 4, padding: '2px 0', cursor: 'pointer'}}
                onClick={() => handleNodeClick(node.groupCode)}
            >
        <span style={{flex: 1, fontSize: 13}}>{node.title}</span>
        <>
          <Button type="text" size="small"
                  icon={<PlusOutlined style={{fontSize: 11, color: '#1890ff'}}/>}
                  onClick={(e) => {
                      e.stopPropagation();
                      setParentForCreate(data);
                      setEditingGroup(null);
                      form.resetFields();
                      form.setFieldsValue({parentCode: node.groupCode});
                      setModalVisible(true)
                  }}
                  title="新增子分组"/>
            <Button type="text" size="small"
                    icon={<EditOutlined style={{fontSize: 11}}/>}
                    onClick={(e) => {
                        e.stopPropagation();
                        setEditingGroup(data);
                        form.setFieldsValue({groupName: data.group_name, parentCode: data.parent_code || ''});
                        setParentForCreate(null);
                        setModalVisible(true)
                    }}/>
            <Popconfirm title={`删除分组"${data.group_name}"？`} description="子分组也会被删除"
                        onConfirm={() => handleDelete(node.groupCode)} onCancel={(e) => e?.stopPropagation()}>
              <Button type="text" size="small" danger
                      icon={<DeleteOutlined style={{fontSize: 11}}/>}
                      onClick={(e) => e.stopPropagation()}/>
            </Popconfirm>
        </>
      </span>
        )
    }

    const handleDelete = async (groupCode) => {
        try {
            await agentApi.groupDelete(groupCode)
            message.success('删除成功')
            onGroupRefresh()
        } catch (e) {
            message.error('删除失败')
        }
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            setLoading(true)
            if (editingGroup) {
                await agentApi.groupUpdate({
                    group_code: editingGroup.group_code,
                    group_name: values.groupName,
                    parent_code: values.parentCode || ''
                })
                message.success('更新成功')
            } else {
                await agentApi.groupCreate({
                    group_name: values.groupName,
                    parent_code: values.parentCode || '',
                    group_code: 'grp_' + Date.now()
                })
                message.success('创建成功')
            }
            setModalVisible(false)
            onGroupRefresh()
        } catch (e) {
            message.error('操作失败')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{padding: '4px 0'}}>
            {/* 树节点 */}
            <div style={{maxHeight: 'calc(100vh - 320px)', overflow: 'auto'}}>
                <Tree
                    treeData={buildTreeData()}
                    defaultExpandAll
                    showLine
                    blockNode
                    titleRender={titleRender}
                    selectedKeys={selectedGroupCode ? [selectedGroupCode] : []}
                    onSelect={() => {
                    }}
                />
            </div>

            {/* 新建/编辑分组弹窗 */}
            <Modal
                title={editingGroup ? '编辑分组' : parentForCreate ? `新建子分组 - ${parentForCreate.group_name}` : '新建分组'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" onFinish={handleSubmit}>
                    <Form.Item name="groupName" label="分组名称" rules={[{required: true, message: '请输入分组名称'}]}>
                        <Input ref={inputRef} placeholder="如：客服机器人"/>
                    </Form.Item>
                    <Form.Item name="parentCode" label="父分组">
                        <Select allowClear placeholder="留空为顶级分组">
                            {groups.filter(g => editingGroup ? g.group_code !== editingGroup.group_code : true).map(g => (
                                <Select.Option key={g.group_code} value={g.group_code}>{g.group_name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

// ===== 应用卡片 =====
const AppCard = ({
                     record,
                     onEdit,
                     onView,
                     onPublish,
                     onUpgrade,
                     onShare,
                     onDelete,
                     onToolConfig,
                     onToggleShare,
                     loading
                 }) => {
    const isPublished = record.status === 'PUBLISHED'
    const version = record.version || 1
    const shareEnabled = !!record.share_enabled

    return (
        <AntCard
            hoverable
            style={{
                borderRadius: 12,
                border: '1px solid #f0f0f0',
                transition: 'all 0.3s',
            }}
            styles={{body: {padding: 16}}}
            actions={[
                isPublished ? (
                    <Tooltip key="view" title="查看发布版本（只读）">
                        <Button type="text" icon={<EyeOutlined/>} onClick={() => onView?.(record)}>查看</Button>
                    </Tooltip>
                ) : (
                    <Tooltip key="edit" title="编辑草稿">
                        <Button type="text" icon={<EditOutlined/>} onClick={() => onEdit(record)}>编辑</Button>
                    </Tooltip>
                ),
                isPublished ? (
                    <Tooltip key="upgrade" title="基于当前版本创建新草稿">
                        <Button type="text" icon={<DiffOutlined/>} onClick={() => onUpgrade?.(record)}>升级</Button>
                    </Tooltip>
                ) : (
                    <Tooltip key="publish" title="发布后即可分享">
                        <Button type="text" icon={<RocketOutlined/>}
                                onClick={() => onPublish(record.app_code)}>发布</Button>
                    </Tooltip>
                ),
                <Tooltip key="share" title={shareEnabled ? '查看/复制分享链接' : '开启后生成分享链接'}>
                    <Button type="text"
                            icon={shareEnabled ? <GlobalOutlined style={{color: '#52c41a'}}/> : <LockOutlined/>}
                            onClick={() => onShare?.(record)}>分享</Button>
                </Tooltip>,
                <Tooltip key="tools" title="工具配置">
                    <Button type="text" icon={<SettingOutlined/>} onClick={() => onToolConfig?.(record)}>工具</Button>
                </Tooltip>,
                <Popconfirm key="delete" title="确定删除此应用?" onConfirm={() => onDelete(record.id)} okText="删除"
                            okButtonProps={{danger: true}}>
                    <Button type="text" danger icon={<DeleteOutlined/>}>删除</Button>
                </Popconfirm>,
            ]}
        >
            <div style={{display: 'flex', gap: 16}}>
                <Avatar
                    shape="square"
                    size={64}
                    src={record.icon_url}
                    icon={<RobotOutlined/>}
                    style={{
                        background: isPublished
                            ? 'linear-gradient(135deg, #52c41a 0%, #389e0d 100%)'
                            : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        flexShrink: 0
                    }}
                />
                <div style={{flex: 1, minWidth: 0}}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        justifyContent: 'space-between',
                        marginBottom: 8
                    }}>
                        <div>
                            <Title level={5} style={{margin: 0, color: '#262626'}}>{record.app_name}</Title>
                            <Text type="secondary"
                                  style={{fontSize: 12, fontFamily: 'monospace'}}>{record.app_code}</Text>
                        </div>
                        <Space size={4}>
                            <Tag style={{borderRadius: 20, fontSize: 11}}>v{version}</Tag>
                            <Tag color={isPublished ? 'success' : 'default'} style={{borderRadius: 20}}>
                                {isPublished ? '已发布' : '草稿'}
                            </Tag>
                            {shareEnabled && (
                                <Tooltip title="已开启分享，点击「分享」按钮获取链接">
                                    <Tag color="green" icon={<GlobalOutlined/>}
                                         style={{borderRadius: 20, fontSize: 11, margin: 0}}>
                                        已分享
                                    </Tag>
                                </Tooltip>
                            )}
                        </Space>
                    </div>

                    {record.description && (
                        <div style={{color: '#595959', fontSize: 13, marginBottom: 12, lineHeight: 1.6}}>
                            {record.description}
                        </div>
                    )}

                    {/* 模型信息 */}
                    <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12}}>
                        <Tag color="blue" style={{margin: 0}}>{record.model_provider || 'ollama'}</Tag>
                        <Text style={{fontSize: 13, color: '#8c8c8c'}}>{record.model_name || 'qwen2.5:7b'}</Text>
                    </div>

                    {/* 时间信息 */}
                    <div style={{display: 'flex', gap: 16, fontSize: 12, color: '#8c8c8c', marginTop: 8}}>
                        <span>创建于 {record.gmt_create?.split(' ')[0] || '-'}</span>
                        {record.gmt_modified !== record.gmt_create && (
                            <span>更新于 {record.gmt_modified?.split(' ')[0] || '-'}</span>
                        )}
                    </div>
                </div>
            </div>
        </AntCard>
    )
}

// ===== 分享抽屉 · 未开启状态 =====
const ShareDrawerEmpty = ({app, onEnable}) => {
    const [loading, setLoading] = useState(false)
    return (
        <div style={{padding: '24px 0', textAlign: 'center'}}>
            <div style={{
                width: 96, height: 96, borderRadius: '50%', margin: '24px auto',
                background: 'linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%)',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
            }}>
                <LockOutlined style={{fontSize: 40, color: '#52c41a'}}/>
            </div>
            <Title level={5} style={{marginTop: 8}}>该应用尚未开启分享</Title>
            <Text type="secondary" style={{display: 'block', marginBottom: 24, fontSize: 13}}>
                开启后，任何人都可通过专属链接访问该 Agent<br/>
                无需登录、可嵌入网页 / 群聊 / 二维码
            </Text>
            <Button
                type="primary"
                size="large"
                icon={<GlobalOutlined/>}
                loading={loading}
                onClick={async () => {
                    setLoading(true);
                    try {
                        await onEnable()
                    } finally {
                        setLoading(false)
                    }
                }}
                style={{borderRadius: 8, minWidth: 160}}
            >
                开启分享
            </Button>
            <Divider style={{margin: '32px 0 16px'}} plain>
                <Text type="secondary" style={{fontSize: 12}}>分享后你可以获得</Text>
            </Divider>
            <Space orientation="vertical" size="small" style={{width: '100%', textAlign: 'left', padding: '0 16px'}}>
                {[
                    {icon: <LinkOutlined/>, text: '独立的访问链接与二维码'},
                    {icon: <CopyOutlined/>, text: '一键复制到微信 / 飞书 / 邮件'},
                    {icon: <EyeOutlined/>, text: '实时统计访问人数与对话轮次'},
                ].map((item, i) => (
                    <div key={i}
                         style={{display: 'flex', alignItems: 'center', gap: 8, color: '#595959', fontSize: 13}}>
                        <span style={{color: '#52c41a'}}>{item.icon}</span>
                        {item.text}
                    </div>
                ))}
            </Space>
        </div>
    )
}

// ===== 分享抽屉 · 已开启状态 =====
const ShareDrawerContent = ({app, onDisable, onRefresh}) => {
    const shareLink = app.share_code
        ? `${window.location.origin}/share/${app.share_code}`
        : ''
    const [copied, setCopied] = useState(false)
    const [refreshing, setRefreshing] = useState(false)

    const copy = async () => {
        if (!shareLink) return
        try {
            await navigator.clipboard.writeText(shareLink)
            setCopied(true)
            message.success('链接已复制')
            setTimeout(() => setCopied(false), 2000)
        } catch {
            const ta = document.createElement('textarea')
            ta.value = shareLink;
            document.body.appendChild(ta);
            ta.select()
            document.execCommand('copy');
            document.body.removeChild(ta)
            setCopied(true);
            message.success('链接已复制')
            setTimeout(() => setCopied(false), 2000)
        }
    }

    const handleRefresh = async () => {
        setRefreshing(true)
        try {
            await onRefresh()
        } finally {
            setRefreshing(false)
        }
    }

    return (
        <div>
            {/* 顶部状态条 */}
            <Alert
                type="success"
                showIcon
                title="已开启分享"
                description="链接现在可被任何人访问，关闭分享后旧链接立即失效。"
                style={{borderRadius: 8, marginBottom: 20}}
            />

            {/* 二维码 + 链接 */}
            <div style={{
                display: 'flex', gap: 20, padding: 20, background: '#fafafa',
                borderRadius: 12, alignItems: 'center', marginBottom: 20
            }}>
                <div style={{
                    background: '#fff', padding: 12, borderRadius: 8,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.06)', flexShrink: 0
                }}>
                    {shareLink ? (
                        <QRCode value={shareLink} size={140} bordered={false}/>
                    ) : (
                        <div style={{
                            width: 140,
                            height: 140,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <Text type="secondary">无链接</Text>
                        </div>
                    )}
                </div>
                <div style={{flex: 1, minWidth: 0}}>
                    <Text type="secondary" style={{fontSize: 12}}>分享链接</Text>
                    <Input.TextArea
                        value={shareLink}
                        readOnly
                        autoSize={{minRows: 2, maxRows: 3}}
                        style={{marginTop: 4, fontSize: 12, fontFamily: 'monospace', resize: 'none'}}
                        onClick={(e) => e.target.select()}
                    />
                    <Space style={{marginTop: 8}} size="small">
                        <Button
                            type="primary"
                            size="small"
                            icon={copied ? <CheckCircleFilled/> : <CopyOutlined/>}
                            onClick={copy}
                            disabled={!shareLink}
                        >
                            {copied ? '已复制' : '复制链接'}
                        </Button>
                        <Button
                            size="small"
                            icon={<LinkOutlined/>}
                            onClick={() => shareLink && window.open(shareLink, '_blank')}
                            disabled={!shareLink}
                        >
                            打开
                        </Button>
                        <Button
                            size="small"
                            icon={<ReloadOutlined/>}
                            loading={refreshing}
                            onClick={handleRefresh}
                        >
                            刷新链接
                        </Button>
                    </Space>
                </div>
            </div>

            {/* 设置项 */}
            <div style={{marginBottom: 20}}>
                <Title level={5} style={{fontSize: 13, marginBottom: 12}}>分享设置</Title>
                <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12}}>
                    <div style={{padding: 12, background: '#fafafa', borderRadius: 8}}>
                        <Text type="secondary" style={{fontSize: 11}}>访问方式</Text>
                        <div style={{marginTop: 4}}>
                            <Tag color="blue" style={{margin: 0}}>公开链接</Tag>
                        </div>
                    </div>
                    <div style={{padding: 12, background: '#fafafa', borderRadius: 8}}>
                        <Text type="secondary" style={{fontSize: 11}}>是否需要登录</Text>
                        <div style={{marginTop: 4}}>
                            <Tag style={{margin: 0}}>不需要</Tag>
                        </div>
                    </div>
                    <div style={{padding: 12, background: '#fafafa', borderRadius: 8}}>
                        <Text type="secondary" style={{fontSize: 11}}>创建时间</Text>
                        <div style={{marginTop: 4, fontSize: 13}}>
                            {app.share_gmt_create?.split(' ')[0] || app.gmt_modified?.split(' ')[0] || '-'}
                        </div>
                    </div>
                    <div style={{padding: 12, background: '#fafafa', borderRadius: 8}}>
                        <Text type="secondary" style={{fontSize: 11}}>累计访问</Text>
                        <div style={{marginTop: 4, fontSize: 13}}>
                            {app.share_visit_count ?? 0} 次
                        </div>
                    </div>
                </div>
            </div>

            {/* 危险操作 */}
            <Divider style={{margin: '12px 0'}}/>
            <Popconfirm
                title="确定要关闭分享吗？"
                description="关闭后，链接将立即失效。"
                okText="关闭分享"
                okButtonProps={{danger: true}}
                cancelText="取消"
                onConfirm={onDisable}
            >
                <Button block danger icon={<LockOutlined/>}>
                    关闭分享
                </Button>
            </Popconfirm>
        </div>
    )
}

// ===== 应用列表（左侧分组树 + 右侧卡片） =====
const AppListPage = ({selectedGroupCode, onEditApp}) => {
    const [dataSource, setDataSource] = useState([])
    const [loading, setLoading] = useState(false)
    const [searchKey, setSearchKey] = useState('')
    const [statusFilter, setStatusFilter] = useState('')
    const [pageNum, setPageNum] = useState(1)
    const [pageSize] = useState(12)
    const [total, setTotal] = useState(0)
    const [createLoading, setCreateLoading] = useState(false)
    const [groups, setGroups] = useState([])
    const [toolDrawerVisible, setToolDrawerVisible] = useState(false)
    const [toolDrawerApp, setToolDrawerApp] = useState(null)
    const [shareDrawerVisible, setShareDrawerVisible] = useState(false)
    const [shareDrawerApp, setShareDrawerApp] = useState(null)
    // 视图模式
    const [viewMode, setViewMode] = useState('card')
    // 创建弹窗的模型列表
    const [createModelOptions, setCreateModelOptions] = useState(MODELS_BY_PROVIDER.ollama)

    // 新建应用 Modal（简版）
    const [createModalVisible, setCreateModalVisible] = useState(false)
    const [createForm] = Form.useForm()
    const [groupOptions, setGroupOptions] = useState([])

    useEffect(() => {
        fetchData();
        handleGroupRefresh()
    }, [pageNum, searchKey, statusFilter, selectedGroupCode])

    const fetchData = () => {
        setLoading(true)
        agentApi.appPage({
            app_name: searchKey,
            status: statusFilter,
            group_code: selectedGroupCode,
            id: pageNum
        }).then(pageData => {
            if (pageData) {
                setDataSource(pageData.records || [])
                setTotal(pageData.total || 0)
            }
        }).catch(e => message.error('加载失败')).finally(() => setLoading(false))
    }

    const handleGroupRefresh = () => {
        agentApi.groupTree().then(res => {
            const list = res?.data || res || []
            setGroups(list)
            setGroupOptions(list.filter(g => g.group_code))
            fetchData()
        })
    }

    // ===== 新建（简版） =====
    const handleCreate = () => {
        createForm.resetFields()
        const provider = 'ollama'
        const models = MODELS_BY_PROVIDER[provider] || []
        setCreateModelOptions(models)
        createForm.setFieldsValue({
            modelProvider: provider,
            modelName: models[0]?.code || '',
            groupCode: selectedGroupCode
        })
        setCreateModalVisible(true)
    }

    const handleCreateSubmit = async () => {
        try {
            const values = await createForm.validateFields()
            setCreateLoading(true)
            // 表单字段名仍是 camelCase，只有请求体键要 snake_case（同一个 AgentAppReq 实体）。
            await agentApi.appCreate({
                app_name: values.appName,
                group_code: values.groupCode,
                description: values.description,
                agent_mode: values.agentMode,
                model_provider: values.modelProvider,
                model_name: values.modelName,
            })
            message.success('创建成功')
            setCreateModalVisible(false)
            fetchData()
        } catch (e) {
            if (!e.errorFields) message.error('操作失败')
        } finally {
            setCreateLoading(false)
        }
    }

    // ===== 编辑 =====
    const handleEdit = (record) => {
        onEditApp(record)
    }

    // ===== 查看（发布版本只读） =====
    // 编辑器内部已根据 record.status 自动设置 disabled，这里仅作语义区分
    const handleView = (record) => {
        onEditApp(record)
    }

    const handleDelete = async (id) => {
        try {
            await agentApi.appDelete({params: {id}});
            message.success('删除成功');
            fetchData()
        } catch (e) {
            message.error('删除失败')
        }
    }

    const handlePublish = async (appCode) => {
        try {
            await agentApi.appPublish({params: {appCode}});
            message.success('发布成功');
            fetchData()
        } catch (e) {
            message.error('发布失败')
        }
    }

    const handleUpgrade = async (record) => {
        try {
            await agentApi.appUpgrade(record.app_code)
            message.success('已升级到新草稿版本，可编辑后发布')
            fetchData()
            // 重新拉取最新 app（含 status=DRAFT 和新 version）并跳到编辑器
            const updated = await agentApi.appGet(record.app_code)
            const appData = updated?.data || updated
            if (appData) onEditApp(appData)
        } catch (e) {
            message.error('升级失败：' + (e?.message || '未知错误'))
        }
    }

    const handleShare = (record) => {
        // 总是打开右侧抽屉，由抽屉内部处理开启/关闭分享
        setShareDrawerApp(record)
        setShareDrawerVisible(true)
    }

    const handleToggleShare = async (record, enabled) => {
        try {
            const res = await agentApi.appToggleShare(record.app_code, enabled)
            const code = res?.data || res
            message.success(enabled ? '已开启分享' : '已关闭分享')
            // 本地更新 record 避免再次拉取
            setDataSource(prev => prev.map(r => r.id === record.id ? {
                ...r,
                share_enabled: enabled,
                share_code: code || r.share_code
            } : r))
            // 同步更新抽屉中的 app
            setShareDrawerApp(prev => prev && prev.id === record.id ? {
                ...prev,
                share_enabled: enabled,
                share_code: code || prev.share_code
            } : prev)
        } catch (e) {
            message.error('操作失败：' + (e?.message || '未知错误'))
        }
    }

    const copyShareLink = (shareCode) => {
        const link = `${window.location.origin}/share/${shareCode}`
        navigator.clipboard.writeText(link).then(() => message.success('链接已复制')).catch(() => {
            const ta = document.createElement('textarea');
            ta.value = link;
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
            message.success('链接已复制')
        })
    }

    const publishedCount = dataSource.filter(r => r.status === 'PUBLISHED').length

    return (
        <div>
            {/* 搜索和操作栏 */}
            <div style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                marginBottom: 16, padding: '16px 20px', background: '#fafafa', borderRadius: 8
            }}>
                <Space wrap>
                    <SearchInput placeholder="搜索应用名称/编码" onSearch={v => {
                        setSearchKey(v);
                        setPageNum(1)
                    }} size="large" allowClear enterButton/>
                    <Select placeholder="状态筛选" allowClear style={{width: 140}} onChange={v => {
                        setStatusFilter(v);
                        setPageNum(1)
                    }}>
                        <Option value="DRAFT">草稿</Option>
                        <Option value="PUBLISHED">已发布</Option>
                    </Select>
                </Space>
                <Space>
                    {total > 0 && (<Text type="secondary">共 <Text strong>{total}</Text> 个应用，已发布 <Text strong
                                                                                                             style={{color: '#52c41a'}}>{publishedCount}</Text> 个</Text>)}
                    <Button size="small" type={viewMode === 'card' ? 'primary' : 'default'} icon={<AppstoreFilled/>}
                            onClick={() => setViewMode('card')} style={{borderRadius: 6}}/>
                    <Button size="small" type={viewMode === 'list' ? 'primary' : 'default'}
                            icon={<UnorderedListOutlined/>} onClick={() => setViewMode('list')}
                            style={{borderRadius: 6}}/>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}
                            style={{borderRadius: 8}}>新建应用</Button>
                </Space>
            </div>

            {/* 加载状态 */}
            {loading && (<div style={{textAlign: 'center', padding: 48}}><LoadingOutlined
                style={{fontSize: 32, color: '#667eea'}}/>
                <div style={{marginTop: 16, color: '#8c8c8c'}}>加载中...</div>
            </div>)}

            {/* 空状态 */}
            {!loading && dataSource.length === 0 && (
                <div style={{textAlign: 'center', padding: '64px 24px', background: '#fafafa', borderRadius: 12}}>
                    <div style={{
                        width: 80,
                        height: 80,
                        borderRadius: '50%',
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        margin: '0 auto 24px'
                    }}>
                        <RobotOutlined style={{fontSize: 36, color: '#fff'}}/>
                    </div>
                    <Title level={4} style={{marginBottom: 8}}>还没有应用</Title>
                    <Text type="secondary" style={{display: 'block', marginBottom: 24}}>点击「新建应用」开始创建您的第一个
                        Agent</Text>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}
                            style={{borderRadius: 8}}>新建应用</Button>
                </div>
            )}

            {/* 应用卡片/列表 */}
            {!loading && dataSource.length > 0 && viewMode === 'card' && (
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))',
                    gap: 16,
                    marginBottom: 16
                }}>
                    {dataSource.map(record => (
                        <AppCard key={record.id} record={record} onEdit={handleEdit} onView={handleView}
                                 onPublish={handlePublish} onUpgrade={handleUpgrade} onShare={handleShare}
                                 onDelete={handleDelete} onToolConfig={(app) => {
                            setToolDrawerApp(app);
                            setToolDrawerVisible(true)
                        }} onShareManage={(app) => {
                            setShareDrawerApp(app);
                            setShareDrawerVisible(true)
                        }}/>
                    ))}
                </div>
            )}

            {!loading && dataSource.length > 0 && viewMode === 'list' && (
                <Table
                    dataSource={dataSource}
                    rowKey="id"
                    size="small"
                    pagination={false}
                    columns={[
                        {
                            title: '名称',
                            dataIndex: 'app_name',
                            width: 180,
                            render: (t, r) => <a onClick={() => handleEdit(r)}
                                                 style={{fontWeight: 500}}>{t || r.app_code}</a>
                        },
                        {
                            title: '版本',
                            dataIndex: 'version',
                            width: 60,
                            render: v => <Tag style={{fontSize: 11}}>v{v || 1}</Tag>
                        },
                        {
                            title: '编码',
                            dataIndex: 'app_code',
                            width: 180,
                            render: t => <code style={{fontSize: 12, color: '#8c8c8c'}}>{t}</code>
                        },
                        {
                            title: '状态',
                            dataIndex: 'status',
                            width: 80,
                            render: s => <Tag
                                color={s === 'PUBLISHED' ? 'green' : 'default'}>{s === 'PUBLISHED' ? '已发布' : '草稿'}</Tag>
                        },
                        {title: '供应商', dataIndex: 'model_provider', width: 100},
                        {title: '模型', dataIndex: 'model_name', width: 140, ellipsis: true},
                        {title: '创建时间', dataIndex: 'gmt_create', width: 170},
                        {
                            title: '操作', width: 220, render: (_, r) => (
                                <Space size="small">
                                    {r.status === 'PUBLISHED' ? (
                                        <>
                                            <Button size="small" type="link" icon={<EyeOutlined/>}
                                                    onClick={() => handleView(r)}>查看</Button>
                                            <Button size="small" type="link" icon={<DiffOutlined/>}
                                                    onClick={() => handleUpgrade(r)}>升级</Button>
                                        </>
                                    ) : (
                                        <>
                                            <Button size="small" type="link" icon={<EditOutlined/>}
                                                    onClick={() => handleEdit(r)}>编辑</Button>
                                            <Button size="small" type="link" icon={<RocketOutlined/>}
                                                    onClick={() => handlePublish(r.app_code)}>发布</Button>
                                        </>
                                    )}
                                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(r.id)}>
                                        <Button size="small" type="link" danger icon={<DeleteOutlined/>}>删除</Button>
                                    </Popconfirm>
                                </Space>
                            )
                        },
                    ]}
                />
            )}

            {/* 分页 */}
            {!loading && total > pageSize && (
                <div style={{textAlign: 'center', marginTop: 16}}>
                    <Button onClick={() => setPageNum(p => Math.max(1, p - 1))} disabled={pageNum === 1}>上一页</Button>
                    <span style={{margin: '0 16px'}}>第 <Text strong>{pageNum}</Text> / {Math.ceil(total / pageSize)} 页</span>
                    <Button onClick={() => setPageNum(p => p + 1)}
                            disabled={pageNum >= Math.ceil(total / pageSize)}>下一页</Button>
                </div>
            )}

            {/* ===== 新建应用 ===== */}
            <Modal
                title="新建应用"
                open={createModalVisible}
                onOk={handleCreateSubmit}
                onCancel={() => setCreateModalVisible(false)}
                confirmLoading={createLoading}
                size="large"
                okText="创建"
                cancelText="取消"
            >
                <Form form={createForm} layout="vertical" size="middle">
                    <div style={{display: 'flex', gap: 12}}>
                        <Form.Item name="appName" label="应用名称" rules={[{required: true, message: '请输入应用名称'}]}
                                   style={{flex: 1}}>
                            <Input placeholder="如：智能客服助手"/>
                        </Form.Item>
                        <Form.Item name="groupCode" label="分组" style={{width: 160}}>
                            <TreeSelect
                                allowClear
                                placeholder="默认"
                                treeData={buildGroupTree(groupOptions)}
                                treeDefaultExpandAll
                                onChange={(val) => createForm.setFieldsValue({groupCode: val || ''})}
                                style={{width: '100%'}}
                            />
                        </Form.Item>
                    </div>
                    <Form.Item name="description" label="应用描述">
                        <TextArea rows={2} placeholder="简短描述这个应用的能力和使用场景"/>
                    </Form.Item>
                    <Divider style={{margin: '12px 0', fontSize: 12, color: '#8c8c8c'}}>应用类型</Divider>
                    <Form.Item name="agentMode" label="类型" rules={[{required: true, message: '请选择应用类型'}]}
                               initialValue="chat">
                        <Select>
                            <Option value="chat">对话 - 纯文本对话</Option>
                            <Option value="agent">智能体 - 工具调用 + 知识库</Option>
                            <Option value="workflow">工作流 - 多步骤流程编排</Option>
                        </Select>
                    </Form.Item>
                    <Divider style={{margin: '12px 0', fontSize: 12, color: '#8c8c8c'}}>模型配置</Divider>
                    <Form.Item name="modelProvider" label="提供商" rules={[{required: true}]} initialValue="ollama">
                        <Select onChange={(val) => {
                            const models = MODELS_BY_PROVIDER[val] || []
                            setCreateModelOptions(models)
                            createForm.setFieldsValue({modelName: models[0]?.code || ''})
                        }}>
                            <Option value="ollama">Ollama (本地)</Option>
                            <Option value="openai">OpenAI</Option>
                            <Option value="dashscope">阿里通义</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="modelName" label="模型" rules={[{required: true}]}>
                        <Select>
                            {createModelOptions.map(m => (
                                <Option key={m.code} value={m.code}>{m.name}</Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>

            {/* ===== 工具配置 Drawer ===== */}
            <Drawer
                title={toolDrawerApp ? `${toolDrawerApp.app_name} - 工具配置` : '工具配置'}
                size="large"
                open={toolDrawerVisible}
                onClose={() => setToolDrawerVisible(false)}
            >
                {toolDrawerApp && <ToolConfigPage appCode={toolDrawerApp.app_code}/>}
            </Drawer>

            {/* ===== 分享管理 Drawer ===== */}
            <Drawer
                title={shareDrawerApp ? `${shareDrawerApp.app_name} · 分享` : '分享'}
                size="large"
                open={shareDrawerVisible}
                onClose={() => setShareDrawerVisible(false)}
                destroyOnHidden
                extra={
                    shareDrawerApp && (
                        <Space>
                            <Text type="secondary" style={{fontSize: 12}}>分享</Text>
                            <Switch
                                size="small"
                                checked={!!shareDrawerApp.share_enabled}
                                onChange={(checked) => handleToggleShare(shareDrawerApp, checked)}
                            />
                        </Space>
                    )
                }
            >
                {shareDrawerApp && (
                    shareDrawerApp.share_enabled ? (
                        <ShareDrawerContent
                            app={shareDrawerApp}
                            onDisable={() => handleToggleShare(shareDrawerApp, false)}
                            onRefresh={() => handleToggleShare(shareDrawerApp, true)}
                        />
                    ) : (
                        <ShareDrawerEmpty
                            app={shareDrawerApp}
                            onEnable={() => handleToggleShare(shareDrawerApp, true)}
                        />
                    )
                )}
            </Drawer>
        </div>
    )
}

// ===== 工具配置页 =====
const ToolConfigPage = () => {
    const [apps, setApps] = useState([])
    const [selectedApp, setSelectedApp] = useState(null)
    const [tools, setTools] = useState([])
    const [skills, setSkills] = useState([])
    const [knowledge, setKnowledge] = useState([])
    const [boundTools, setBoundTools] = useState([])
    const [boundSkills, setBoundSkills] = useState([])
    const [boundKnowledge, setBoundKnowledge] = useState([])
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [activeTab, setActiveTab] = useState('tools')
    const [appLoading, setAppLoading] = useState(false)

    useEffect(() => {
        setAppLoading(true)
        agentApi.appPage({id: 1, pageSize: 100}).then(pageData => {
            setApps(pageData?.records || [])
        }).finally(() => setAppLoading(false))
    }, [])

    useEffect(() => {
        if (!selectedApp) return
        setLoading(true)
        Promise.all([
            llmApi.toolTemplateList(),
            llmApi.skillTemplateList(),
            llmApi.knowledgeList(),
        ]).then(([tRes, sRes, kRes]) => {
            setTools(tRes?.data || tRes || [])
            setSkills(sRes?.data || sRes || [])
            setKnowledge(kRes?.data || kRes || [])
            loadBoundConfigs(selectedApp.app_code)
        }).finally(() => setLoading(false))
    }, [selectedApp])

    const loadBoundConfigs = (appCode) => {
        llmApi.appConfig({params: {appCode}}).then(res => {
            const cfg = res?.data || res || {}
            setBoundTools(cfg.tools || [])
            setBoundSkills(cfg.skills || [])
            setBoundKnowledge(cfg.knowledgeBases || [])
        }).catch(() => {
        })
    }

    const isBound = (list, code) => list.includes(code)

    const toggleItem = (type, code) => {
        if (type === 'tool') setBoundTools(prev => prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code])
        else if (type === 'skill') setBoundSkills(prev => prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code])
        else setBoundKnowledge(prev => prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code])
    }

    const handleSave = async () => {
        if (!selectedApp) return
        setSaving(true)
        try {
            for (const toolCode of boundTools) await llmApi.appToolAdd({
                appCode: selectedApp.app_code,
                toolCode
            }).catch(() => {
            })
            for (const skillId of boundSkills) await llmApi.appSkillAdd({
                appCode: selectedApp.app_code,
                skillId: Number(skillId)
            }).catch(() => {
            })
            message.success('工具配置已保存')
        } catch (e) {
            message.error('保存失败')
        } finally {
            setSaving(false)
        }
    }

    const renderItemList = (title, icon, items, boundList, type, renderItem) => (
        <div>
            <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16}}>
                {icon}<span style={{fontWeight: 500}}>{title}</span><Tag>{items.length}</Tag>
            </div>
            {items.length === 0 ? (<Empty description="暂无可用项" image={Empty.PRESENTED_IMAGE_SIMPLE}/>) : (
                <div style={{display: 'flex', flexDirection: 'column', gap: 8}}>
                    {items.map(renderItem)}
                </div>
            )}
        </div>
    )

    if (apps.length === 0) return (
        <div style={{padding: 48, textAlign: 'center'}}><Empty description="请先创建应用后再配置工具"/></div>)

    return (
        <div style={{padding: 24}}>
            <Row gutter={24}>
                <Col span={8}>
                    <Title level={5} style={{marginBottom: 16}}>选择应用</Title>
                    <div
                        style={{background: '#fafafa', borderRadius: 8, padding: 16, maxHeight: 500, overflow: 'auto'}}>
                        {apps.map(app => (
                            <div key={app.id} onClick={() => setSelectedApp(app)} style={{
                                padding: '12px 16px', marginBottom: 8, borderRadius: 8, cursor: 'pointer',
                                background: selectedApp?.id === app.id ? '#fff' : 'transparent',
                                border: selectedApp?.id === app.id ? '2px solid #667eea' : '2px solid transparent',
                                transition: 'all 0.2s'
                            }}>
                                <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                                    <Avatar size="small" icon={<RobotOutlined/>}
                                            style={{background: app.status === 'PUBLISHED' ? 'linear-gradient(135deg, #52c41a, #389e0d)' : 'linear-gradient(135deg, #667eea, #764ba2)'}}/>
                                    <div>
                                        <div style={{fontWeight: 500, fontSize: 14}}>{app.app_name}</div>
                                        <div style={{fontSize: 11, color: '#8c8c8c'}}>{app.app_code}</div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </Col>
                <Col span={16}>
                    {!selectedApp ? (
                        <div style={{padding: 48, textAlign: 'center', background: '#fafafa', borderRadius: 12}}>
                            <RobotOutlined style={{fontSize: 48, color: '#d9d9d9'}}/>
                            <div style={{marginTop: 16, color: '#8c8c8c'}}>请从左侧选择一个应用</div>
                        </div>) : (
                        <div>
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                marginBottom: 20
                            }}>
                                <Title level={5} style={{margin: 0}}>配置 {selectedApp.app_name}</Title>
                                <Button type="primary" icon={<CheckCircleFilled/>} onClick={handleSave}
                                        loading={saving}>保存配置</Button>
                            </div>
                            <Tabs activeKey={activeTab} onChange={setActiveTab}>
                                <TabPane tab="工具" key="tools">
                                    <div style={{
                                        padding: 16,
                                        background: '#fafafa',
                                        borderRadius: 8
                                    }}>{renderItemList('可用工具', <ToolOutlined/>, tools, boundTools, 'tool', (t) => (
                                        <div key={t.toolCode} onClick={() => toggleItem('tool', t.toolCode)} style={{
                                            padding: '10px 14px',
                                            background: isBound(boundTools, t.toolCode) ? '#e6f7ff' : '#fff',
                                            border: `1px solid ${isBound(boundTools, t.toolCode) ? '#91d5ff' : '#f0f0f0'}`,
                                            borderRadius: 6,
                                            cursor: 'pointer',
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 10
                                        }}>
                                            <div style={{
                                                width: 20,
                                                height: 20,
                                                borderRadius: 4,
                                                background: isBound(boundTools, t.toolCode) ? '#1890ff' : '#d9d9d9',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center'
                                            }}>{isBound(boundTools, t.toolCode) &&
                                                <CheckCircleFilled style={{color: '#fff', fontSize: 12}}/>}</div>
                                            <div>
                                                <div style={{
                                                    fontWeight: 500,
                                                    fontSize: 13
                                                }}>{t.toolName || t.toolCode}</div>
                                                <div style={{
                                                    fontSize: 11,
                                                    color: '#8c8c8c'
                                                }}>{t.description || t.toolCode}</div>
                                            </div>
                                        </div>))}</div>
                                </TabPane>
                                <TabPane tab="技能" key="skills">
                                    <div style={{
                                        padding: 16,
                                        background: '#fafafa',
                                        borderRadius: 8
                                    }}>{renderItemList('可用技能',
                                        <StarOutlined/>, skills, boundSkills, 'skill', (s) => (
                                            <div key={s.id} onClick={() => toggleItem('skill', String(s.id))} style={{
                                                padding: '10px 14px',
                                                background: isBound(boundSkills, String(s.id)) ? '#fff7e6' : '#fff',
                                                border: `1px solid ${isBound(boundSkills, String(s.id)) ? '#ffd591' : '#f0f0f0'}`,
                                                borderRadius: 6,
                                                cursor: 'pointer',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 10
                                            }}>
                                                <div style={{
                                                    width: 20,
                                                    height: 20,
                                                    borderRadius: 4,
                                                    background: isBound(boundSkills, String(s.id)) ? '#fa8c16' : '#d9d9d9',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center'
                                                }}>{isBound(boundSkills, String(s.id)) &&
                                                    <CheckCircleFilled style={{color: '#fff', fontSize: 12}}/>}</div>
                                                <div>
                                                    <div style={{fontWeight: 500, fontSize: 13}}>{s.name}</div>
                                                    <div style={{
                                                        fontSize: 11,
                                                        color: '#8c8c8c'
                                                    }}>{s.description || s.code}</div>
                                                </div>
                                            </div>))}</div>
                                </TabPane>
                                <TabPane tab="知识库" key="knowledge">
                                    <div style={{
                                        padding: 16,
                                        background: '#fafafa',
                                        borderRadius: 8
                                    }}>{renderItemList('可用知识库',
                                        <BookOutlined/>, knowledge, boundKnowledge, 'kb', (k) => (
                                            <div key={k.id} onClick={() => toggleItem('kb', String(k.id))} style={{
                                                padding: '10px 14px',
                                                background: isBound(boundKnowledge, String(k.id)) ? '#f6ffed' : '#fff',
                                                border: `1px solid ${isBound(boundKnowledge, String(k.id)) ? '#b7eb8f' : '#f0f0f0'}`,
                                                borderRadius: 6,
                                                cursor: 'pointer',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 10
                                            }}>
                                                <div style={{
                                                    width: 20,
                                                    height: 20,
                                                    borderRadius: 4,
                                                    background: isBound(boundKnowledge, String(k.id)) ? '#52c41a' : '#d9d9d9',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center'
                                                }}>{isBound(boundKnowledge, String(k.id)) &&
                                                    <CheckCircleFilled style={{color: '#fff', fontSize: 12}}/>}</div>
                                                <div>
                                                    <div style={{fontWeight: 500, fontSize: 13}}>{k.name}</div>
                                                    <div style={{
                                                        fontSize: 11,
                                                        color: '#8c8c8c'
                                                    }}>{k.description || `ID: ${k.id}`}</div>
                                                </div>
                                            </div>))}</div>
                                </TabPane>
                            </Tabs>
                        </div>
                    )}
                </Col>
            </Row>
        </div>
    )
}

// ===== 分享管理页 =====
const ShareManagementPage = () => (
    <div style={{padding: 48, textAlign: 'center'}}>
        <ShareAltOutlined style={{fontSize: 48, color: '#d9d9d9'}}/>
        <div style={{marginTop: 16, color: '#8c8c8c'}}>分享管理功能开发中...</div>
    </div>
)

export default AgentAppPage