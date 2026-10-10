import {useCallback, useEffect, useRef, useState} from 'react'
import {useNavigate, useParams} from 'react-router-dom'
import {
    Breadcrumb,
    Button,
    Card,
    Col,
    Dropdown,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Row,
    Space,
    Spin,
    Statistic,
    Table,
    Tag,
    Upload
} from 'antd'
import {
    ArrowLeftOutlined,
    CloudOutlined,
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    FileImageOutlined,
    FileOutlined,
    FileTextOutlined,
    FileZipOutlined,
    FolderAddOutlined,
    FolderOutlined,
    MoreOutlined,
    ReloadOutlined,
    UploadOutlined
} from '@ant-design/icons'
import {ossApi} from '../../api'
import request from '../../api/request.js'
import {SearchInput} from '@/common/components/ui'

const fmtSize = (size) => {
    if (!size || size === 0) return '-'
    const u = ['B', 'KB', 'MB', 'GB', 'TB']
    let i = 0, n = size
    while (n >= 1024 && i < u.length - 1) {
        n /= 1024;
        i++
    }
    return n.toFixed(i === 0 ? 0 : 1) + ' ' + u[i]
}

const fileIcon = (key) => {
    if (key.endsWith('/')) return <FolderOutlined style={{color: '#faad14'}}/>
    const ext = key.split('.').pop()?.toLowerCase()
    if (['zip', 'tar', 'gz', 'rar', '7z'].includes(ext)) return <FileZipOutlined style={{color: '#722ed1'}}/>
    if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext)) return <FileImageOutlined
        style={{color: '#13c2c2'}}/>
    if (['md', 'txt', 'log', 'json', 'xml', 'yaml', 'yml'].includes(ext)) return <FileTextOutlined
        style={{color: '#52c41a'}}/>
    return <FileOutlined style={{color: '#8c8c8c'}}/>
}

export default function ObjectBrowser() {
    const {bucketName} = useParams()
    const decodedBucket = decodeURIComponent(bucketName || '')
    const navigate = useNavigate()

    const [objects, setObjects] = useState([])
    const [loading, setLoading] = useState(false)
    const [currentPrefix, setCurrentPrefix] = useState('')
    const [stats, setStats] = useState({objectCount: 0, totalSize: 0})
    const [searchKey, setSearchKey] = useState('')
    const [selectedRowKeys, setSelectedRowKeys] = useState([])
    const [uploadLoading, setUploadLoading] = useState(false)
    const [folderModalOpen, setFolderModalOpen] = useState(false)
    const [folderForm] = Form.useForm()
    const fileInputRef = useRef(null)

    const loadObjects = useCallback(async (prefix = currentPrefix) => {
        if (!decodedBucket) return
        setLoading(true)
        try {
            const res = await ossApi.listObjects({params: {bucketName: decodedBucket, prefix: prefix || undefined}})
            const list = Array.isArray(res) ? res : (res?.data || [])
            setObjects(list)
        } catch (e) {
            message.error('加载对象失败：' + (e?.message || '未知错误'))
            setObjects([])
        } finally {
            setLoading(false)
        }
    }, [decodedBucket, currentPrefix])

    const loadStats = useCallback(async () => {
        try {
            const res = await ossApi.getBucketStats({params: {bucketName: decodedBucket}})
            setStats(res || {objectCount: 0, totalSize: 0})
        } catch (e) {
            // 静默
        }
    }, [decodedBucket])

    useEffect(() => {
        setCurrentPrefix('')
        setSelectedRowKeys([])
    }, [decodedBucket])

    useEffect(() => {
        loadObjects(currentPrefix)
        loadStats()
    }, [currentPrefix, loadObjects, loadStats])

    const handleEnterFolder = (key) => {
        if (key.endsWith('/')) {
            setCurrentPrefix(key)
            setSelectedRowKeys([])
        }
    }

    const handleBreadcrumb = (prefix) => {
        setCurrentPrefix(prefix)
        setSelectedRowKeys([])
    }

    const handleUpload = async (file) => {
        if (!file) return false
        setUploadLoading(true)
        const key = (currentPrefix || '') + file.name
        try {
            // multipart/form-data 上传：apiRouter 不支持，绕过它直接调后端
            const form = new FormData()
            form.append('bucketName', decodedBucket)
            form.append('objectKey', key)
            form.append('file', file)
            await request.post('/v1/object', form, {headers: {'Content-Type': 'multipart/form-data'}})
            message.success(`已上传: ${file.name}`)
            loadObjects(currentPrefix)
            loadStats()
        } catch (e) {
            message.error('上传失败：' + (e?.message || '未知错误'))
        } finally {
            setUploadLoading(false)
        }
        return false // 阻止 antd Upload 默认行为
    }

    const handleDelete = async (keys) => {
        if (!keys || keys.length === 0) return
        try {
            if (keys.length === 1) {
                await ossApi.deleteObject({params: {bucketName: decodedBucket, objectKey: keys[0]}})
            } else {
                await ossApi.batchDelete({data: keys, params: {bucketName: decodedBucket}})
            }
            message.success(`已删除 ${keys.length} 个`)
            setSelectedRowKeys([])
            loadObjects(currentPrefix)
            loadStats()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const handleCreateFolder = async () => {
        try {
            const vals = await folderForm.validateFields()
            const key = (currentPrefix || '') + vals.name + '/'
            await ossApi.createFolder({params: {bucketName: decodedBucket, folderKey: key}})
            message.success('文件夹已创建')
            setFolderModalOpen(false)
            folderForm.resetFields()
            loadObjects(currentPrefix)
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    const handleDownload = async (key) => {
        // 阿里云模式：走签名 URL（流量不经过主服务）
        // local 模式：直接走 /api/v1/object/{bucket}/{key}
        try {
            const res = await ossApi.presignedUrl({params: {bucketName: decodedBucket, objectKey: key, expires: 3600}})
            const url = res?.url || ossApi.downloadUrl({
                params: {
                    bucketName: decodedBucket,
                    objectKey: key
                }
            }).then(r => r?.data?.url).catch(() => null) || `/api/v1/object?bucketName=${encodeURIComponent(decodedBucket)}&objectKey=${encodeURIComponent(key)}`
            if (url && typeof url === 'string') {
                window.open(url, '_blank')
            } else {
                // 降级：直接打开 GET URL
                window.open(`/api/v1/object?bucketName=${encodeURIComponent(decodedBucket)}&objectKey=${encodeURIComponent(key)}`, '_blank')
            }
        } catch (e) {
            // 降级：直接打开 GET URL
            window.open(`/api/v1/object?bucketName=${encodeURIComponent(decodedBucket)}&objectKey=${encodeURIComponent(key)}`, '_blank')
        }
    }

    const handleCopy = async (key) => {
        // 简化版：复制到同桶 _copy/ 前缀
        const newKey = (currentPrefix || '') + '_copy_' + key.split('/').pop()
        try {
            await ossApi.copyObject({
                data: {destBucketName: decodedBucket, destObjectKey: newKey},
                params: {bucketName: decodedBucket, objectKey: key}
            })
            message.success('已复制')
            loadObjects(currentPrefix)
        } catch (e) {
            message.error('复制失败：' + (e?.message || '未知错误'))
        }
    }

    // 客户端搜索过滤
    const filtered = searchKey
        ? objects.filter(o => o.key?.toLowerCase().includes(searchKey.toLowerCase()))
        : objects

    const columns = [
        {
            title: '名称',
            dataIndex: 'key',
            render: (key, record) => {
                const isFolder = key?.endsWith('/') || record.folder
                const name = key?.split('/').filter(Boolean).pop() || key
                return (
                    <span
                        style={{cursor: isFolder ? 'pointer' : 'default', color: isFolder ? '#faad14' : '#262626'}}
                        onClick={() => isFolder && handleEnterFolder(key)}
                    >
            <Space>
              {fileIcon(key)}
                <span style={{fontWeight: isFolder ? 500 : 400}}>{name}</span>
            </Space>
          </span>
                )
            },
        },
        {title: '大小', dataIndex: 'size', width: 110, render: (s) => fmtSize(s)},
        {title: '类型', dataIndex: 'contentType', width: 200, ellipsis: true, render: (t) => t ? <Tag>{t}</Tag> : '-'},
        {
            title: '修改时间',
            dataIndex: 'lastModified',
            width: 180,
            render: (t) => t ? new Date(t).toLocaleString() : '-'
        },
        {
            title: '操作', width: 160, fixed: 'right',
            render: (_, record) => {
                const isFolder = record.key?.endsWith('/') || record.folder
                const items = isFolder
                    ? [
                        {key: 'enter', icon: <FolderOutlined/>, label: '进入'},
                        {key: 'delete', icon: <DeleteOutlined/>, label: '删除', danger: true},
                    ]
                    : [
                        {key: 'download', icon: <DownloadOutlined/>, label: '下载'},
                        {key: 'copy', icon: <CopyOutlined/>, label: '复制'},
                        {key: 'delete', icon: <DeleteOutlined/>, label: '删除', danger: true},
                    ]
                return (
                    <Dropdown
                        trigger={['click']}
                        menu={{
                            items,
                            onClick: ({key, domEvent}) => {
                                domEvent.stopPropagation()
                                if (key === 'enter') handleEnterFolder(record.key)
                                else if (key === 'download') handleDownload(record.key)
                                else if (key === 'copy') handleCopy(record.key)
                                else if (key === 'delete') handleDelete([record.key])
                            },
                        }}
                    >
                        <Button type="text" size="small" icon={<MoreOutlined/>}/>
                    </Dropdown>
                )
            },
        },
    ]

    // 面包屑
    const crumbs = [{title: '所有桶', onClick: () => navigate('/ai/oss/bucket')}, {title: decodedBucket}]
    if (currentPrefix) {
        const parts = currentPrefix.split('/').filter(Boolean)
        let acc = ''
        parts.forEach(p => {
            acc += p + '/'
            crumbs.push({title: p, onClick: () => handleBreadcrumb(acc)})
        })
    }

    return (
        <div>
            {/* 顶部 */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 12,
                background: '#fff', padding: '14px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.04)', marginBottom: 16,
            }}>
                <Button type="text" icon={<ArrowLeftOutlined/>} onClick={() => navigate('/ai/oss/bucket')}>
                    返回
                </Button>
                <FolderOutlined style={{fontSize: 18, color: '#faad14'}}/>
                <span style={{fontSize: 16, fontWeight: 600}}>{decodedBucket}</span>
                <div style={{flex: 1}}/>
                <SearchInput
                    placeholder="搜索对象名..."
                    value={searchKey}
                    onChange={setSearchKey}
                    size="large"
                    allowClear
                />
                <Button icon={<ReloadOutlined/>} onClick={() => {
                    loadObjects(currentPrefix);
                    loadStats()
                }}/>
            </div>

            {/* 统计 */}
            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={8}><Card><Statistic title="对象数" value={stats.objectCount || 0}/></Card></Col>
                <Col span={8}><Card><Statistic title="总大小" value={fmtSize(stats.totalSize)}/></Card></Col>
                <Col span={8}>
                    <Card>
                        <Statistic
                            title="Provider"
                            value={stats.activeProvider || '-'}
                            styles={{content: {fontSize: 18, color: '#1890ff'}}}
                            prefix={<CloudOutlined/>}
                        />
                    </Card>
                </Col>
            </Row>

            {/* 工具栏 */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                background: '#fff', padding: '10px 16px', borderRadius: 10,
                boxShadow: '0 1px 4px rgba(0,0,0,0.04)', marginBottom: 12,
            }}>
                <Breadcrumb items={crumbs} style={{flex: 1}}/>
                {selectedRowKeys.length > 0 && (
                    <Popconfirm
                        title={`确定删除 ${selectedRowKeys.length} 个对象?`}
                        onConfirm={() => handleDelete(selectedRowKeys)}
                        okText="确认"
                        cancelText="取消"
                        okType="danger"
                    >
                        <Button danger size="small" icon={<DeleteOutlined/>}>
                            批量删除 ({selectedRowKeys.length})
                        </Button>
                    </Popconfirm>
                )}
                <Button size="small" icon={<FolderAddOutlined/>} onClick={() => setFolderModalOpen(true)}>
                    新建文件夹
                </Button>
                <Upload beforeUpload={handleUpload} showUploadList={false} disabled={uploadLoading}>
                    <Button size="small" type="primary" icon={<UploadOutlined/>} loading={uploadLoading}>
                        上传文件
                    </Button>
                </Upload>
            </div>

            {/* 对象列表 */}
            <Card styles={{body: {padding: 0}}}>
                {loading ? (
                    <div style={{textAlign: 'center', padding: 60}}><Spin size="large"/></div>
                ) : filtered.length === 0 ? (
                    <Empty
                        image={<CloudOutlined style={{fontSize: 48, color: '#bfbfbf'}}/>}
                        description={searchKey ? '没有匹配的对象' : '桶是空的，上传第一个文件吧'}
                        style={{padding: 60}}
                    />
                ) : (
                    <Table
                        rowKey="key"
                        columns={columns}
                        dataSource={filtered}
                        pagination={false}
                        size="middle"
                        rowSelection={{
                            selectedRowKeys,
                            onChange: setSelectedRowKeys,
                        }}
                        onRow={(record) => ({
                            onDoubleClick: () => {
                                if (record.key?.endsWith('/') || record.folder) handleEnterFolder(record.key)
                            },
                        })}
                    />
                )}
            </Card>

            {/* 新建文件夹 */}
            <Modal
                title="新建文件夹"
                open={folderModalOpen}
                onCancel={() => {
                    setFolderModalOpen(false);
                    folderForm.resetFields()
                }}
                onOk={handleCreateFolder}
                okText="创建"
                cancelText="取消"
                destroyOnHidden
            >
                <Form form={folderForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item
                        name="name"
                        label="文件夹名"
                        rules={[{required: true, message: '请输入文件夹名'}]}
                    >
                        <Input prefix={<FolderOutlined/>} placeholder="my-folder"/>
                    </Form.Item>
                    <div style={{fontSize: 12, color: '#999'}}>
                        将创建在：<code>{decodedBucket}/{currentPrefix || ''}</code>
                    </div>
                </Form>
            </Modal>
        </div>
    )
}
