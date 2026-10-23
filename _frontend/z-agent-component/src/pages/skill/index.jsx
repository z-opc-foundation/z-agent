import {useCallback, useEffect, useMemo, useState} from 'react'
import {Alert, Button, Drawer, Empty, Input, Pagination, Select, Spin, Tag, Tooltip, message} from 'antd'
import {
    AppstoreOutlined,
    CheckCircleFilled,
    CloudDownloadOutlined,
    DeleteOutlined,
    FileTextOutlined,
    ReloadOutlined,
    SafetyCertificateOutlined,
    SearchOutlined,
    ThunderboltFilled
} from '@ant-design/icons'
import {skillApi} from '../../api'
import styles from './index.module.css'

/**
 * 技能市场 —— 对接 z-skill 的真实市场面 (2026-10-22 重构).
 *
 * <p>旧版对着 z-skill 里**从未存在过**的端点 (/skill/page、/skill/category/tree、
 * /skill/upload …) 写, 失败后回退 8 条 mock 假数据, 于是页面上永远是那几条编出来的技能。
 * 现在只认 z-skill 真实供给的那组面: search / categories / sources / stats / installed /
 * detail / content / install / uninstall / refresh, 拉不到就是拉不到, 不再编。
 *
 * <p>版面按真实技能市场 (skills.sh / awesome-copilot) 的做法组织: 左侧分类与来源分面,
 * 中间卡片墙 (封面 + 名称 + 描述 + 标签 + 安装量 + 风险徽标), 右侧抽屉看详情与 SKILL.md 正文。
 */

const RISK_META = {
    safe: {label: '安全', color: 'green'},
    low: {label: '低风险', color: 'green'},
    medium: {label: '中风险', color: 'orange'},
    high: {label: '高风险', color: 'red'},
    critical: {label: '严重', color: 'darkred'},
    unscanned: {label: '未扫描', color: 'default'}
}

const COVER_PALETTES = [
    ['#e6f4ff', '#f0e9ff'],
    ['#fff1e6', '#ffe7f0'],
    ['#e6fffb', '#e6f4ff'],
    ['#f6ffed', '#fff7e6'],
    ['#fff1f0', '#f0f5ff'],
    ['#f9f0ff', '#fff0f6']
]

const SORTS = [
    {value: 'relevance', label: '相关度'},
    {value: 'updated', label: '最近更新'},
    {value: 'installs', label: '安装量'},
    {value: 'name', label: '名称'}
]

function coverOf(id) {
    const s = String(id || '')
    let h = 0
    for (let i = 0; i < s.length; i += 1) h = (h * 31 + s.charCodeAt(i)) % 997
    return COVER_PALETTES[h % COVER_PALETTES.length]
}

function riskOf(level) {
    return RISK_META[level] || RISK_META.unscanned
}

function errText(e) {
    if (!e) return '未知错误'
    if (e.response?.status) return `HTTP ${e.response.status} · ${e.message}`
    return e.message || String(e)
}

export default function SkillMarket() {
    const [tab, setTab] = useState('market')
    const [keyword, setKeyword] = useState('')
    const [category, setCategory] = useState('')
    const [source, setSource] = useState('')
    const [risk, setRisk] = useState('')
    const [sort, setSort] = useState('relevance')
    const [page, setPage] = useState(0)
    const [perPage] = useState(12)

    const [skills, setSkills] = useState([])
    const [total, setTotal] = useState(0)
    const [loading, setLoading] = useState(false)
    const [listError, setListError] = useState('')

    const [categories, setCategories] = useState({})
    const [tags, setTags] = useState({})
    const [sources, setSources] = useState([])
    const [stats, setStats] = useState(null)
    const [metaError, setMetaError] = useState('')

    const [detail, setDetail] = useState(null)
    const [detailLoading, setDetailLoading] = useState(false)
    const [detailError, setDetailError] = useState('')
    const [content, setContent] = useState(null)
    const [contentLoading, setContentLoading] = useState(false)
    const [activeFile, setActiveFile] = useState('')
    const [busy, setBusy] = useState('')

    const loadMeta = useCallback(async () => {
        setMetaError('')
        const errors = []
        try {
            const data = await skillApi.categories()
            setCategories(data?.categories || {})
            setTags(data?.tags || {})
        } catch (e) {
            errors.push(`分类: ${errText(e)}`)
        }
        try {
            const data = await skillApi.sources()
            setSources(Array.isArray(data) ? data : [])
        } catch (e) {
            errors.push(`来源: ${errText(e)}`)
        }
        try {
            const data = await skillApi.stats()
            setStats(data || null)
        } catch (e) {
            errors.push(`统计: ${errText(e)}`)
        }
        if (errors.length) setMetaError(errors.join('；'))
    }, [])

    const loadList = useCallback(async () => {
        setLoading(true)
        setListError('')
        try {
            const params = {sort, page, perPage}
            if (keyword) params.q = keyword
            if (category) params.category = category
            if (source) params.source = source
            if (risk) params.risk = risk
            if (tab === 'installed') params.installed = true
            const data = await skillApi.search({params})
            const list = Array.isArray(data?.skills) ? data.skills : []
            setSkills(list)
            setTotal(typeof data?.total === 'number' ? data.total : list.length)
        } catch (e) {
            setSkills([])
            setTotal(0)
            setListError(errText(e))
        } finally {
            setLoading(false)
        }
    }, [keyword, category, source, risk, sort, page, perPage, tab])

    useEffect(() => {
        loadMeta()
    }, [loadMeta])

    useEffect(() => {
        loadList()
    }, [loadList])

    const categoryList = useMemo(
        () => Object.entries(categories).sort((a, b) => b[1] - a[1]),
        [categories]
    )

    const openDetail = useCallback(async (skill) => {
        setDetail(skill)
        setDetailError('')
        setContent(null)
        setActiveFile('')
        setDetailLoading(true)
        try {
            const data = await skillApi.detail({params: {id: skill.id}})
            setDetail((prev) => (prev ? {...prev, ...data, id: skill.id} : data))
        } catch (e) {
            setDetailError(errText(e))
        } finally {
            setDetailLoading(false)
        }
    }, [])

    const loadContent = useCallback(async (file) => {
        if (!detail?.id) return
        setActiveFile(file || '')
        setContentLoading(true)
        try {
            const params = {id: detail.id}
            if (file) params.file = file
            const data = await skillApi.content({params})
            setContent(data || null)
        } catch (e) {
            setContent({available: false, reason: errText(e)})
        } finally {
            setContentLoading(false)
        }
    }, [detail?.id])

    const doInstall = useCallback(async (skill) => {
        setBusy(skill.id)
        try {
            await skillApi.install({id: skill.id, installedBy: 'console', source: 'marketplace'})
            message.success(`已安装 ${skill.name || skill.id}`)
            await loadList()
            await loadMeta()
            if (detail?.id === skill.id) await openDetail(skill)
        } catch (e) {
            message.error(`安装失败: ${errText(e)}`)
        } finally {
            setBusy('')
        }
    }, [detail?.id, loadList, loadMeta, openDetail])

    const doUninstall = useCallback(async (skill) => {
        setBusy(skill.id)
        try {
            await skillApi.uninstall({params: {id: skill.id}})
            message.success(`已卸载 ${skill.name || skill.id}`)
            await loadList()
            await loadMeta()
            if (detail?.id === skill.id) await openDetail(skill)
        } catch (e) {
            message.error(`卸载失败: ${errText(e)}`)
        } finally {
            setBusy('')
        }
    }, [detail?.id, loadList, loadMeta, openDetail])

    const doRefresh = useCallback(async () => {
        setLoading(true)
        try {
            await skillApi.refresh()
            message.success('已重新聚合全部来源')
            await loadMeta()
            await loadList()
        } catch (e) {
            message.error(`聚合失败: ${errText(e)}`)
        } finally {
            setLoading(false)
        }
    }, [loadList, loadMeta])

    const statChips = useMemo(() => {
        if (!stats) return []
        return [
            {key: 'total', label: '技能总数', value: stats.skillCount},
            {key: 'installed', label: '已安装', value: stats.installedCount},
            {key: 'category', label: '分类数', value: stats.categoryCount},
            {key: 'source', label: '来源数', value: stats.sourceCount}
        ].filter((c) => typeof c.value === 'number')
    }, [stats])

    const files = Array.isArray(detail?.files) ? detail.files : []

    return (
        <div className={styles.root}>
            <header className={styles.header}>
                <div className={styles.headerMain}>
                    <h2 className={styles.title}>技能市场</h2>
                    <p className={styles.subtitle}>
                        聚合各来源的 Agent Skills · 沙箱扫描风险分级 · 装前先看正文
                    </p>
                </div>
                <div className={styles.statRow}>
                    {statChips.map((c) => (
                        <div key={c.key} className={styles.statChip}>
                            <span className={styles.statValue}>{c.value}</span>
                            <span className={styles.statLabel}>{c.label}</span>
                        </div>
                    ))}
                </div>
                <Button icon={<ReloadOutlined/>} onClick={doRefresh} loading={loading}>
                    重新聚合
                </Button>
            </header>

            {metaError && (
                <Alert type="warning" showIcon closable style={{marginBottom: 12}}
                       message="市场元信息加载不完整"
                       description={metaError}/>
            )}

            <div className={styles.body}>
                <aside className={styles.side}>
                    <div className={styles.sideBlock}>
                        <div className={styles.sideTitle}>分类</div>
                        <button
                            type="button"
                            className={`${styles.sideItem} ${!category ? styles.sideItemActive : ''}`}
                            onClick={() => {setCategory(''); setPage(0)}}
                        >
                            <span>全部</span>
                            <span className={styles.sideCount}>{stats?.skillCount ?? ''}</span>
                        </button>
                        {categoryList.map(([name, count]) => (
                            <button
                                key={name}
                                type="button"
                                className={`${styles.sideItem} ${category === name ? styles.sideItemActive : ''}`}
                                onClick={() => {setCategory(name); setPage(0)}}
                            >
                                <span>{name}</span>
                                <span className={styles.sideCount}>{count}</span>
                            </button>
                        ))}
                        {categoryList.length === 0 && !metaError && (
                            <div className={styles.sideHint}>暂无分类数据</div>
                        )}
                    </div>

                    <div className={styles.sideBlock}>
                        <div className={styles.sideTitle}>来源</div>
                        <button
                            type="button"
                            className={`${styles.sideItem} ${!source ? styles.sideItemActive : ''}`}
                            onClick={() => {setSource(''); setPage(0)}}
                        >
                            <span>全部来源</span>
                        </button>
                        {sources.map((s) => (
                            <button
                                key={s.id}
                                type="button"
                                className={`${styles.sideItem} ${source === s.id ? styles.sideItemActive : ''}`}
                                onClick={() => {setSource(s.id); setPage(0)}}
                            >
                                <span className={styles.sideName}>{s.id}</span>
                                <span className={styles.sideCount}>{s.skillCount}</span>
                            </button>
                        ))}
                        {sources.length === 0 && !metaError && (
                            <div className={styles.sideHint}>暂无来源（未配置 z.skill.sources）</div>
                        )}
                    </div>
                </aside>

                <main className={styles.main}>
                    <div className={styles.toolbar}>
                        <Input
                            allowClear
                            prefix={<SearchOutlined/>}
                            placeholder="搜索技能名称、描述、标签"
                            value={keyword}
                            onChange={(e) => {setKeyword(e.target.value); setPage(0)}}
                            onPressEnter={loadList}
                            className={styles.search}
                        />
                        <Select
                            value={risk}
                            onChange={(v) => {setRisk(v); setPage(0)}}
                            allowClear
                            placeholder="风险等级"
                            style={{width: 130}}
                            options={Object.entries(RISK_META).map(([value, m]) => ({value, label: m.label}))}
                        />
                        <Select
                            value={sort}
                            onChange={(v) => {setSort(v); setPage(0)}}
                            style={{width: 130}}
                            options={SORTS}
                        />
                        <div className={styles.tabs}>
                            <button
                                type="button"
                                className={`${styles.tab} ${tab === 'market' ? styles.tabActive : ''}`}
                                onClick={() => {setTab('market'); setPage(0)}}
                            >
                                广场
                            </button>
                            <button
                                type="button"
                                className={`${styles.tab} ${tab === 'installed' ? styles.tabActive : ''}`}
                                onClick={() => {setTab('installed'); setPage(0)}}
                            >
                                已安装
                            </button>
                        </div>
                    </div>

                    {listError && (
                        <Alert
                            type="error"
                            showIcon
                            message="技能列表拉取失败"
                            description={listError}
                            action={<Button size="small" onClick={loadList}>重试</Button>}
                            style={{marginBottom: 12}}
                        />
                    )}

                    {loading && skills.length === 0 ? (
                        <div className={styles.loadingWrap}>
                            <Spin tip="正在拉取技能市场…"/>
                        </div>
                    ) : skills.length === 0 ? (
                        <Empty
                            className={styles.empty}
                            description={
                                listError
                                    ? '接口不可用，暂无数据'
                                    : tab === 'installed'
                                        ? '还没有安装任何技能'
                                        : '市场里还没有技能 —— 配置 z.skill.sources[*] 或点右上角「重新聚合」'
                            }
                        />
                    ) : (
                        <>
                            <div className={styles.grid}>
                                {skills.map((s) => (
                                    <MarketCard
                                        key={s.id}
                                        skill={s}
                                        busy={busy === s.id}
                                        onOpen={() => openDetail(s)}
                                        onInstall={() => doInstall(s)}
                                        onUninstall={() => doUninstall(s)}
                                    />
                                ))}
                            </div>
                            {total > perPage && (
                                <div className={styles.pager}>
                                    <Pagination
                                        current={page + 1}
                                        pageSize={perPage}
                                        total={total}
                                        onChange={(p) => setPage(p - 1)}
                                        showSizeChanger={false}
                                        size="small"
                                    />
                                </div>
                            )}
                        </>
                    )}
                </main>
            </div>

            <Drawer
                open={!!detail}
                onClose={() => setDetail(null)}
                width={720}
                title={detail?.name || detail?.id || '技能详情'}
                destroyOnHidden
            >
                {detailLoading && <Spin tip="加载详情…"/>}
                {detailError && (
                    <Alert type="error" showIcon message="详情加载失败" description={detailError}/>
                )}
                {detail && !detailLoading && (
                    <div className={styles.detail}>
                        <div className={styles.detailHero} style={{
                            background: `linear-gradient(135deg, ${coverOf(detail.id)[0]}, ${coverOf(detail.id)[1]})`
                        }}>
                            <div className={styles.detailHeroIcon}><AppstoreOutlined/></div>
                            <div>
                                <div className={styles.detailHeroName}>{detail.name || detail.id}</div>
                                <div className={styles.detailHeroId}>{detail.id}</div>
                            </div>
                        </div>

                        <div className={styles.detailBadges}>
                            <Tag color={riskOf(detail.riskLevel).color}>{riskOf(detail.riskLevel).label}</Tag>
                            {detail.installed && <Tag color="blue" icon={<CheckCircleFilled/>}>已安装</Tag>}
                            {detail.version && <Tag>版本 {detail.version}</Tag>}
                            {detail.format && <Tag>格式 {detail.format}</Tag>}
                            {detail.source && <Tag>来源 {detail.source}</Tag>}
                        </div>

                        {detail.description && <p className={styles.detailDesc}>{detail.description}</p>}

                        <div className={styles.metaGrid}>
                            <MetaItem label="作者" value={detail.author || '—'}/>
                            <MetaItem label="许可" value={detail.license || '—'}/>
                            <MetaItem label="安装量" value={String(detail.installCount ?? 0)}/>
                            <MetaItem label="正文行数" value={String(detail.bodyLines ?? 0)}/>
                            <MetaItem label="触发词" value={detail.trigger || '—'}/>
                            <MetaItem label="兼容" value={detail.compatibility || '—'}/>
                        </div>

                        {Array.isArray(detail.tags) && detail.tags.length > 0 && (
                            <div className={styles.tagRow}>
                                {detail.tags.map((t) => <Tag key={t}>{t}</Tag>)}
                            </div>
                        )}

                        {Array.isArray(detail.allowedTools) && detail.allowedTools.length > 0 && (
                            <div className={styles.section}>
                                <div className={styles.sectionTitle}>允许的工具</div>
                                <div className={styles.tagRow}>
                                    {detail.allowedTools.map((t) => (
                                        <Tag key={t} color="geekblue">{t}</Tag>
                                    ))}
                                </div>
                            </div>
                        )}

                        {Array.isArray(detail.issues) && detail.issues.length > 0 && (
                            <div className={styles.section}>
                                <div className={styles.sectionTitle}>
                                    <SafetyCertificateOutlined/> 扫描提示（{detail.issues.length}）
                                </div>
                                {detail.issues.map((issue, i) => (
                                    <div key={i} className={styles.issueRow}>{issue}</div>
                                ))}
                            </div>
                        )}

                        {files.length > 0 && (
                            <div className={styles.section}>
                                <div className={styles.sectionTitle}>随包文件</div>
                                <div className={styles.fileList}>
                                    {files.map((f) => (
                                        <button
                                            key={f.path}
                                            type="button"
                                            className={`${styles.fileItem} ${activeFile === (f.path || '') ? styles.fileItemActive : ''}`}
                                            onClick={() => loadContent(f.entry ? '' : f.path)}
                                        >
                                            <FileTextOutlined/>
                                            <span className={styles.filePath}>{f.path}</span>
                                            {f.sizeBytes ? (
                                                <span className={styles.fileSize}>{f.sizeBytes} B</span>
                                            ) : null}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className={styles.section}>
                            <div className={styles.sectionTitle}>SKILL.md 正文</div>
                            {!content && (
                                <Button size="small" onClick={() => loadContent('')} loading={contentLoading}>
                                    加载正文
                                </Button>
                            )}
                            {content && !content.available && (
                                <Alert type="info" showIcon message="正文不可读"
                                       description={content.reason || '该来源未提供可读正文'}/>
                            )}
                            {content && content.available && (
                                <pre className={styles.codeBlock}>{content.content || '(空文件)'}</pre>
                            )}
                        </div>

                        <div className={styles.detailActions}>
                            {detail.installed ? (
                                <Button danger icon={<DeleteOutlined/>} loading={busy === detail.id}
                                        onClick={() => doUninstall(detail)}>
                                    卸载
                                </Button>
                            ) : (
                                <Button type="primary" icon={<CloudDownloadOutlined/>}
                                        loading={busy === detail.id}
                                        onClick={() => doInstall(detail)}>
                                    安装
                                </Button>
                            )}
                            <span className={styles.detailHint}>
                                安装只是登记一条安装记录，不写入本机目录
                            </span>
                        </div>
                    </div>
                )}
            </Drawer>
        </div>
    )
}

function MetaItem({label, value}) {
    return (
        <div className={styles.metaItem}>
            <span className={styles.metaLabel}>{label}</span>
            <span className={styles.metaValue}>{value}</span>
        </div>
    )
}

function MarketCard({skill, busy, onOpen, onInstall, onUninstall}) {
    const risk = riskOf(skill.riskLevel)
    const cover = coverOf(skill.id)
    return (
        <div className={styles.card} onClick={onOpen}>
            <div className={styles.cardCover} style={{
                background: `linear-gradient(135deg, ${cover[0]}, ${cover[1]})`
            }}>
                <div className={styles.cardCoverIcon}><AppstoreOutlined/></div>
                <div className={styles.cardCoverCategory}>{skill.category || 'general'}</div>
                {skill.installed && (
                    <div className={styles.cardCoverBadge}><CheckCircleFilled/> 已安装</div>
                )}
            </div>
            <div className={styles.cardBody}>
                <div className={styles.cardTitleRow}>
                    <span className={styles.cardTitle} title={skill.name || skill.id}>
                        {skill.name || skill.id}
                    </span>
                    <Tooltip title={`风险等级：${risk.label}`}>
                        <Tag color={risk.color} className={styles.cardRisk}>{risk.label}</Tag>
                    </Tooltip>
                </div>
                <div className={styles.cardDesc}>
                    {skill.description || '（该来源未提供描述）'}
                </div>
                {Array.isArray(skill.tags) && skill.tags.length > 0 && (
                    <div className={styles.cardTags}>
                        {skill.tags.slice(0, 4).map((t) => <Tag key={t}>{t}</Tag>)}
                    </div>
                )}
                <div className={styles.cardFooter}>
                    <span className={styles.cardMeta} title={skill.author}>
                        {skill.author || '未知作者'}
                    </span>
                    <span className={styles.cardMeta}>
                        <ThunderboltFilled/> {skill.installCount ?? 0}
                    </span>
                    <span className={styles.cardMeta}>{skill.version || '—'}</span>
                    <Button
                        size="small"
                        type={skill.installed ? 'default' : 'primary'}
                        className={styles.cardBtn}
                        loading={busy}
                        onClick={(e) => {
                            e.stopPropagation()
                            skill.installed ? onUninstall() : onInstall()
                        }}
                    >
                        {skill.installed ? '卸载' : '安装'}
                    </Button>
                </div>
            </div>
        </div>
    )
}
