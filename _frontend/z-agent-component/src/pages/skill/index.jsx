import {useCallback, useEffect, useRef, useState} from 'react'
import {
    Button,
    Col,
    Drawer,
    Dropdown,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Pagination,
    Row,
    Segmented,
    Select,
    Spin,
    Tabs,
    Tag,
    Timeline,
    Tree,
    Upload
} from 'antd'
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloudUploadOutlined,
    CodeOutlined,
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    EditOutlined,
    FileTextOutlined,
    FileZipOutlined,
    FireOutlined,
    FolderOutlined,
    HistoryOutlined,
    InfoCircleOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    SettingOutlined,
    StarOutlined
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import {skillApi} from '../../api'
import styles from './index.module.css'

// 示例数据（后端无 skill 模块时的回退）
const mockSkills = [
    {
        id: 1,
        skillCode: 'code-review',
        skillName: '代码审查',
        categoryCode: 'backend',
        version: '1.2.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: '自动审查代码质量，检测潜在 Bug 和安全漏洞，支持 Java/Python/Go 等多种语言。',
        tags: 'java,python,code-quality,lint',
        downloadCount: 128,
        content: '# 代码审查技能\n\n自动分析代码变更并给出审查意见。',
        files: [{
            path: 'SKILL.md',
            content: '# Code Review Skill\n\nAutomated code review with AI-powered analysis.\n\n## Features\n- Static analysis\n- Security vulnerability detection\n- Style enforcement\n'
        }, {
            path: 'scripts/analyze.py',
            content: 'import ast\n\ndef analyze_file(filepath):\n    with open(filepath) as f:\n        tree = ast.parse(f.read())\n    results = []\n    for node in ast.walk(tree):\n        if isinstance(node, ast.FunctionDef):\n            results.append({"type": "function", "name": node.name})\n    return results\n'
        }, {
            path: 'rules/java-rules.yaml',
            content: 'rules:\n  - id: avoid-system-out\n    pattern: System.out.println\n    severity: WARNING\n  - id: catch-exception\n    pattern: catch (Exception e)\n    severity: ERROR\n'
        }]
    },
    {
        id: 2,
        skillCode: 'data-analyzer',
        skillName: '数据分析师',
        categoryCode: 'backend',
        version: '1.0.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: 'SQL 查询生成与数据分析助手，支持 MySQL/PostgreSQL/ClickHouse。',
        tags: 'sql,data,analysis,dashboard',
        downloadCount: 86,
        content: '# 数据分析师技能\n\n通过自然语言生成SQL查询。',
        files: [{
            path: 'SKILL.md',
            content: '# Data Analyzer Skill\n\nNatural language to SQL query generation.\n## Supported Databases: MySQL, PostgreSQL, ClickHouse\n'
        }, {
            path: 'queries/templates.sql',
            content: '-- User retention\nSELECT DATE(created_at) as day, COUNT(DISTINCT user_id) as active_users\nFROM user_events\nWHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)\nGROUP BY DATE(created_at) ORDER BY day;\n-- Revenue summary\nSELECT product_category, SUM(amount) as total_revenue\nFROM transactions GROUP BY product_category;\n'
        }]
    },
    {
        id: 3,
        skillCode: 'ui-gen',
        skillName: 'UI 生成器',
        categoryCode: 'frontend',
        version: '2.1.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: '从设计稿或描述生成 React 组件代码，支持 Ant Design 组件库。',
        tags: 'react,antd,ui,component',
        downloadCount: 215,
        content: '# UI 生成器技能\n\n从文字描述生成UI代码。',
        files: [{
            path: 'SKILL.md',
            content: '# UI Generator Skill\n\nGenerate React components from natural language.\n## Tech Stack: React 18, Ant Design 5, TypeScript\n'
        }, {
            path: 'templates/component.ejs',
            content: 'import React from "react";\nimport { Card, Button, Space } from "antd";\n\ninterface <%= compName %>Props { title?: string; onAction?: () => void }\n\nconst <%= compName %>: React.FC<<%= compName %>Props> = ({ title, onAction }) => (\n  <Card title={title}><Space><Button onClick={onAction}>Action</Button></Space></Card>\n);\nexport default <%= compName %>;\n'
        }, {
            path: 'styles/default.css',
            content: '.ui-gen-container { padding: 24px; background: #fff; border-radius: 8px; }\n.ui-gen-header { display: flex; align-items: center; margin-bottom: 16px; }\n'
        }]
    },
    {
        id: 4,
        skillCode: 'doc-writer',
        skillName: '文档编写',
        categoryCode: 'doc',
        version: '1.0.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: '自动生成项目文档、API 文档、README。支持 Javadoc/Swagger/Markdown 格式。',
        tags: 'documentation,api,markdown,javadoc',
        downloadCount: 67,
        content: '# 文档编写技能\n\n自动生成各类文档。',
        files: [{
            path: 'SKILL.md',
            content: '# Doc Writer Skill\n\nAutomated documentation generation.\n## Formats: Markdown, Javadoc, OpenAPI/Swagger\n'
        }, {
            path: 'templates/api-doc.md',
            content: '# {{apiName}}\n\n{{description}}\n\n{{#each endpoints}}\n### {{method}} {{path}}\n**Parameters:**\n| Name | Type | Required | Description |\n|------|------|----------|-------------|\n{{/each}}\n'
        }]
    },
    {
        id: 5,
        skillCode: 'ci-helper',
        skillName: 'CI/CD 助手',
        categoryCode: 'devops',
        version: '2.0.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: 'Pipeline 配置生成、Dockerfile 优化、K8s 部署清单自动生成。',
        tags: 'ci/cd,docker,kubernetes,devops',
        downloadCount: 94,
        content: '# CI/CD 助手技能\n\n自动化运维配置生成。',
        files: [{
            path: 'SKILL.md',
            content: '# CI/CD Helper Skill\n\nAutomated CI/CD configuration generation.\n## Platforms: GitHub Actions, GitLab CI, Jenkins\n'
        }, {
            path: 'pipelines/build.yaml',
            content: 'name: build-and-test\non: [push, pull_request]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - name: Setup JDK 17\n        uses: actions/setup-java@v3\n        with:\n          java-version: "17"\n          distribution: "temurin"\n      - run: mvn clean verify\n'
        }, {
            path: 'docker/Dockerfile',
            content: 'FROM eclipse-temurin:17-jre-alpine\nWORKDIR /app\nCOPY target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT ["java", "-jar", "app.jar"]\n'
        }]
    },
    {
        id: 6,
        skillCode: 'test-gen',
        skillName: '测试生成器',
        categoryCode: 'testing',
        version: '1.3.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: '基于代码自动生成单元测试和集成测试用例，支持 JUnit/TestNG/Pytest。',
        tags: 'testing,junit,pytest,tdd',
        downloadCount: 156,
        content: '# 测试生成器技能\n\n自动生成测试代码。',
        files: [{
            path: 'SKILL.md',
            content: '# Test Generator Skill\n\nAutomated test generation.\n## Frameworks: JUnit 5, TestNG, Pytest\n'
        }, {
            path: 'configs/junit-config.yaml',
            content: 'generator:\n  framework: junit5\n  mockLibrary: mockito\n  coverage: 0.8\n  rules:\n    - skipGettersSetters: true\n    - generateEdgeCases: true\n    - maxAssertionsPerTest: 5\n'
        }]
    },
    {
        id: 7,
        skillCode: 'agent-flow',
        skillName: 'Agent 工作流',
        categoryCode: 'ai-agent',
        version: '1.1.0',
        author: 'zifang',
        status: 'PUBLISHED',
        description: '多步骤 Agent 工作流编排引擎，支持条件判断、循环、并行执行。',
        tags: 'agent,workflow,automation,llm',
        downloadCount: 201,
        content: '# Agent 工作流技能\n\n编排多步骤 Agent 工作流。',
        files: [{
            path: 'SKILL.md',
            content: '# Agent Flow Skill\n\nMulti-step agent workflow orchestration.\n## Features: Conditional branching, Loops, Parallel execution, Retry\n'
        }, {
            path: 'workflows/order-flow.yaml',
            content: 'workflow:\n  id: order-processing\n  steps:\n    - id: validate-order\n      type: function\n      handler: validateOrder\n    - id: check-inventory\n      type: function\n      handler: checkInventory\n      retry: 3\n    - id: process-payment\n      type: function\n      handler: processPayment\n    - id: send-notification\n      type: function\n      parallel: true\n'
        }, {
            path: 'nodes/check-condition.js',
            content: 'module.exports = async (context, params) => {\n  const { field, operator, value } = params;\n  const actual = context.get(field);\n  const ops = { eq: (a, b) => a === b, gt: (a, b) => a > b, lt: (a, b) => a < b, in: (a, b) => b.includes(a) };\n  if (!ops[operator]) throw new Error("Unknown operator: " + operator);\n  return ops[operator](actual, value);\n};\n'
        }]
    },
    {
        id: 8,
        skillCode: 'api-mocker',
        skillName: 'API Mock',
        categoryCode: 'frontend',
        version: '1.0.0',
        author: 'zifang',
        status: 'DRAFT',
        description: '快速生成 API Mock 服务，基于 OpenAPI 规范自动生成模拟数据。',
        tags: 'api,mock,prototype',
        downloadCount: 45,
        content: '# API Mock 技能\n\n快速生成API Mock。',
        files: [{
            path: 'SKILL.md',
            content: '# API Mock Skill\n\nRapid API mock server from OpenAPI specs.\n## Features: OpenAPI 3.0, Realistic data, Custom delay\n'
        }, {
            path: 'configs/openapi-spec.yaml',
            content: 'openapi: "3.0.0"\ninfo:\n  title: Sample API\n  version: "1.0.0"\npaths:\n  /users:\n    get:\n      summary: List users\n      responses:\n        "200":\n          description: User list\n          content:\n            application/json:\n              schema:\n                type: array\n                items:\n                  $ref: "#/components/schemas/User"\ncomponents:\n  schemas:\n    User:\n      type: object\n      properties:\n        id: { type: integer }\n        name: { type: string }\n        email: { type: string }\n'
        }]
    },
]

const mockCategories = [
    {categoryCode: 'backend', categoryName: '后端开发', parentCode: '', skillCount: 2},
    {categoryCode: 'frontend', categoryName: '前端开发', parentCode: '', skillCount: 2},
    {categoryCode: 'devops', categoryName: 'DevOps', parentCode: '', skillCount: 1},
    {categoryCode: 'testing', categoryName: '测试', parentCode: '', skillCount: 1},
    {categoryCode: 'ai-agent', categoryName: 'AI Agent', parentCode: '', skillCount: 1},
    {categoryCode: 'doc', categoryName: '文档', parentCode: '', skillCount: 1},
]

const mockStats = {total: 8, published: 7}

const {TextArea} = Input

const GRADIENTS = {
    backend: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    frontend: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    devops: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    testing: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'ai-agent': 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    doc: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    default: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
}

const getGradient = (code) => GRADIENTS[code] || GRADIENTS.default
const getContrast = (code) => {
    const map = {
        backend: '#fff',
        frontend: '#fff',
        devops: '#fff',
        testing: '#333',
        'ai-agent': '#333',
        doc: '#333',
        default: '#fff'
    }
    return map[code] || '#fff'
}

const STATUS_LABEL = {PUBLISHED: '已发布', DRAFT: '草稿', ARCHIVED: '已归档'}

// API 返回 snake_case（skill_name/category_code/download_count 等）→ 统一映射为组件用的 camelCase
const normalizeSkill = (s) => ({
    ...s,
    skillCode: s.skillCode || s.skill_code,
    skillName: s.skillName || s.skill_name,
    categoryCode: s.categoryCode || s.category_code,
    downloadCount: s.downloadCount ?? s.download_count,
    gmtCreate: s.gmtCreate || s.gmt_create,
    gmtModified: s.gmtModified || s.gmt_modified,
})

const TAG_COLORS = ['blue', 'green', 'cyan', 'purple', 'orange', 'red']

// 分类图标底色（浅色市场风格：彩色软底方块）
const CATEGORY_TINT = {
    backend: {bg: '#e5f1ff', fg: '#007aff'},
    frontend: {bg: '#f5f0ff', fg: '#8b5cf6'},
    devops: {bg: '#e5f5ff', fg: '#0aa2c0'},
    testing: {bg: '#e8f9ee', fg: '#34c759'},
    'ai-agent': {bg: '#fff3e0', fg: '#f5a623'},
    doc: {bg: '#e0f7f4', fg: '#14b8a6'},
    default: {bg: '#f2f2f3', fg: '#676767'},
}

// 树形数据扁平化（从 API 获取树后转为扁平列表用于 CRUD）
const flattenTree = (nodes) => {
    const result = []
    const walk = (items, parentCode) => {
        items.forEach(n => {
            const code = n.categoryCode || n.key
            result.push({
                categoryCode: code,
                categoryName: n.categoryName || n.title,
                parentCode: parentCode || n.parentCode || '',
                skillCount: n.skillCount || 0,
            })
            if (n.children?.length) walk(n.children, code)
        })
    }
    if (nodes?.length) walk(nodes, '')
    return result
}

// 文件路径 → 嵌套树
function buildFileTree(files) {
    const root = {key: '__root__', children: []}
    const dirMap = {'': root}
    // collect all dirs
    files.forEach(f => {
        const parts = f.path.split('/')
        parts.pop() // leaf filename
        let parentKey = ''
        parts.forEach((dir, i) => {
            const key = parts.slice(0, i + 1).join('/')
            if (!dirMap[key]) {
                dirMap[key] = {key, title: dir, isLeaf: false, children: []}
            }
            if (!dirMap[parentKey].children.some(c => c.key === key)) {
                dirMap[parentKey].children.push(dirMap[key])
            }
            parentKey = key
        })
    })
    // add leaf files
    files.forEach(f => {
        const parentKey = f.path.includes('/') ? f.path.split('/').slice(0, -1).join('/') : ''
        const fileName = f.path.includes('/') ? f.path.split('/').pop() : f.path
        dirMap[parentKey].children.push({
            key: f.path,
            title: fileName,
            isLeaf: true,
            file: f,
        })
    })
    return root.children
}

export default function SkillMarket() {
    const [skills, setSkills] = useState([])
    const [loading, setLoading] = useState(false)
    const [total, setTotal] = useState(0)
    const [current, setCurrent] = useState(1)
    const pageSizeRef = useRef(12)

    const [keyword, setKeyword] = useState('')
    const [categoryCode, setCategoryCode] = useState(undefined)
    const [sortBy, setSortBy] = useState('download_count')
    const [categories, setCategories] = useState([])

    const [drawerOpen, setDrawerOpen] = useState(false)
    const [detailSkill, setDetailSkill] = useState(null)
    const [detailContent, setDetailContent] = useState('')
    const [versions, setVersions] = useState([])
    const [detailLoading, setDetailLoading] = useState(false)
    const [detailTab, setDetailTab] = useState('overview')
    const [selectedFile, setSelectedFile] = useState(null)
    const [fileTreeKeys, setFileTreeKeys] = useState([])

    const [pubModalOpen, setPubModalOpen] = useState(false)
    const [pubForm] = Form.useForm()
    const [pubLoading, setPubLoading] = useState(false)
    const [uploadLoading, setUploadLoading] = useState(false)

    const [stats, setStats] = useState({total: 0, published: 0})

    // 分类 CRUD
    const [catModalVisible, setCatModalVisible] = useState(false)
    const [editingCategory, setEditingCategory] = useState(null)
    const [parentForCreate, setParentForCreate] = useState(null)
    const [catLoading, setCatLoading] = useState(false)
    const [categoryForm] = Form.useForm()

    const loadStats = async () => {
        try {
            const res = await skillApi.stats()
            if (res && (res.total !== undefined || res.published !== undefined)) {
                setStats(res)
            } else if (res?.data) {
                setStats(res.data)
            } else {
                setStats(mockStats)
            }
        } catch (e) {
            setStats(mockStats)
        }
    }

    const loadSkills = useCallback(async (page = 1) => {
        setLoading(true)
        try {
            const params = {current: page, size: pageSizeRef.current, sortBy}
            if (keyword) params.keyword = keyword
            if (categoryCode) params.categoryCode = categoryCode
            if (keyword || categoryCode) params.status = 'PUBLISHED'

            const res = await skillApi.page(params)
            const norm = (list) => (list || []).map(normalizeSkill)
            if (res && res.records) {
                setSkills(norm(res.records))
                setTotal(Number(res.total) || res.records.length)
            } else if (Array.isArray(res)) {
                setSkills(norm(res))
                setTotal(res.length)
            } else if (res?.data?.records) {
                setSkills(norm(res.data.records))
                setTotal(Number(res.data.total) || res.data.records.length)
            } else {
                // 回退到 mock 数据
                const filtered = mockSkills.filter(s => {
                    if (keyword && !s.skillName.includes(keyword) && !s.description.includes(keyword)) return false
                    if (categoryCode && s.categoryCode !== categoryCode) return false
                    return true
                })
                setSkills(filtered)
                setTotal(filtered.length)
            }
        } catch (e) {
            console.error('loadSkills error:', e)
            const filtered = mockSkills.filter(s => {
                if (keyword && !s.skillName.includes(keyword) && !s.description.includes(keyword)) return false
                if (categoryCode && s.categoryCode !== categoryCode) return false
                return true
            })
            setSkills(filtered)
            setTotal(filtered.length)
        } finally {
            setLoading(false)
        }
    }, [keyword, categoryCode, sortBy])

    const loadCategories = async () => {
        try {
            const res = await skillApi.categoryTree()
            // API 返回 catCode/catName（或 snake_case 的 cat_code/cat_name）→ 映射为 categoryCode/categoryName
            const mapCat = (items) => (items || []).map(c => ({
                categoryCode: c.catCode || c.categoryCode || c.cat_code,
                categoryName: c.catName || c.categoryName || c.cat_name,
                parentCode: c.parentCode || c.parent_code || '',
                skillCount: c.skillCount || 0,
                children: c.children ? mapCat(c.children) : undefined,
            }))
            if (Array.isArray(res) && res.length > 0) {
                const mapped = mapCat(res)
                if (mapped[0]?.children) {
                    setCategories(flattenTree(mapped))
                } else {
                    setCategories(mapped)
                }
            } else if (res?.data && res.data.length > 0) {
                const mapped = mapCat(res.data)
                if (mapped[0]?.children) {
                    setCategories(flattenTree(mapped))
                } else {
                    setCategories(mapped)
                }
            } else {
                setCategories(mockCategories)
            }
        } catch (e) {
            // API 失败时从 localStorage 恢复，没有则用 mock
            const cached = localStorage.getItem('z_skill_categories')
            if (cached) {
                try {
                    setCategories(JSON.parse(cached))
                } catch {
                    setCategories(mockCategories)
                }
            } else {
                setCategories(mockCategories)
            }
        }
    }

    useEffect(() => {
        loadStats()
        loadCategories()
    }, [])

    useEffect(() => {
        loadSkills(current)
    }, [loadSkills, current])

    const handleKeywordSearch = () => {
        setCurrent(1);
        loadSkills(1)
    }
    const handleCategoryChange = (code) => {
        setCategoryCode(code)
        setCurrent(1)
    }
    const handleSortChange = (sort) => {
        setSortBy(sort);
        setCurrent(1)
    }

    const openDetail = async (skill) => {
        setDetailSkill(skill)
        setDrawerOpen(true)
        setDetailContent(skill.content || '')
        setVersions([])
        setSelectedFile(null)
        setFileTreeKeys([])
        setDetailTab('overview')
        setDetailLoading(true)
        try {
            // Load detail and versions in parallel
            const [detailRes, versionsRes] = await Promise.allSettled([
                skillApi.getBySkillCode(skill.skillCode),
                skillApi.versions(skill.skillCode),
            ])
            // Process detail
            if (detailRes.status === 'fulfilled' && detailRes.value) {
                const data = detailRes.value?.data || detailRes.value
                if (data && typeof data === 'object') {
                    const merged = {...skill, ...data}
                    setDetailSkill(merged)
                    setDetailContent(data.content || data.readme || skill.content || '')
                    if (data.files?.length) setSelectedFile(data.files[0])
                }
            }
            // Process versions
            if (versionsRes.status === 'fulfilled' && versionsRes.value) {
                const vData = versionsRes.value?.data || versionsRes.value
                if (Array.isArray(vData)) setVersions(vData)
            }
        } catch (e) {
            console.warn('openDetail error:', e)
        } finally {
            setDetailLoading(false)
        }
    }

    const handleCopyContent = async (text) => {
        try {
            await navigator.clipboard.writeText(text)
            message.success('已复制到剪贴板')
        } catch {
            message.error('复制失败')
        }
    }

    const handleDownload = (skill) => {
        if (!skill) return
        if (skill.packageType === 'ZIP' && (skill.packagePath || skill.version)) {
            const url = skillApi.downloadPackage(skill.skillCode, skill.version)
            window.open(url, '_blank')
        } else if (skill.content) {
            handleCopyContent(skill.content)
        }
    }

    const handleInstall = async (skillCode) => {
        try {
            const res = await skillApi.install({skillCode})
            if (res !== undefined && res !== null) {
                message.success('安装成功');
                loadStats()
            } else message.error('安装失败')
        } catch (e) {
            message.error('安装异常')
        }
    }

    // 上传技能包（ZIP）
    const handleUploadPackage = async (file) => {
        if (!detailSkill) return false
        if (!file.name.toLowerCase().endsWith('.zip')) {
            message.error('仅支持 .zip 格式')
            return Upload.LIST_IGNORE
        }
        setUploadLoading(true)
        try {
            await skillApi.uploadPackage(detailSkill.skillCode, detailSkill.version || '1.0.0', file)
            message.success('上传成功')
            // 刷新当前详情
            setDetailSkill(prev => prev ? {...prev, packageType: 'ZIP'} : prev)
            // 重新拉取服务端最新数据
            try {
                const res = await skillApi.getBySkillCode(detailSkill.skillCode)
                const data = res && typeof res === 'object' && 'data' in res ? res.data : res
                if (data) setDetailSkill(prev => prev ? {...prev, ...data} : prev)
            } catch (_) {
            }
            // 同步刷新列表（packageType 改变）
            loadSkills(current)
        } catch (e) {
            console.error('uploadPackage error:', e)
            message.error('上传失败：' + (e?.message || '未知错误'))
        } finally {
            setUploadLoading(false)
        }
        return false // 阻止 antd Upload 默认上传
    }

    const handlePublish = async () => {
        try {
            const vals = await pubForm.validateFields()
            setPubLoading(true)
            const res = await skillApi.create({...vals, status: 'PUBLISHED'})
            if (res !== undefined && res !== null) {
                message.success('发布成功')
                setPubModalOpen(false)
                pubForm.resetFields()
                loadSkills(1)
                loadStats()
            } else {
                message.error('发布失败')
            }
        } catch (e) {
            if (e?.errorFields) return
            message.error('发布异常')
        } finally {
            setPubLoading(false)
        }
    }

    // 分类 CRUD 操作
    const openAddCategory = (parent) => {
        setEditingCategory(null)
        setParentForCreate(parent)
        categoryForm.resetFields()
        if (parent) {
            categoryForm.setFieldsValue({parentCode: parent.categoryCode})
        }
        setCatModalVisible(true)
    }

    const openEditCategory = (cat) => {
        setEditingCategory(cat)
        setParentForCreate(null)
        categoryForm.setFieldsValue({
            categoryCode: cat.categoryCode,
            categoryName: cat.categoryName,
            parentCode: cat.parentCode || '',
        })
        setCatModalVisible(true)
    }

    const handleCategorySubmit = async () => {
        try {
            const values = await categoryForm.validateFields()
            setCatLoading(true)
            if (editingCategory) {
                // API 期望 catCode/catName，前端用 categoryCode/categoryName
                await skillApi.createCategory({
                    catCode: values.categoryCode,
                    catName: values.categoryName,
                    parentCode: values.parentCode || '',
                })
                message.success('更新成功')
            } else {
                await skillApi.createCategory({
                    catCode: values.categoryCode,
                    catName: values.categoryName,
                    parentCode: values.parentCode || '',
                })
                message.success('创建成功')
            }
            setCatModalVisible(false)
            loadCategories()
        } catch (e) {
            if (e?.errorFields) return
            // 回退：本地 mock 操作 + localStorage 持久化
            const values = categoryForm.getFieldsValue()
            let updated = [...categories]
            if (editingCategory) {
                const idx = updated.findIndex(c => c.categoryCode === editingCategory.categoryCode)
                if (idx >= 0) {
                    updated[idx] = {...updated[idx], ...values}
                }
            } else {
                updated.push({
                    categoryCode: values.categoryCode,
                    categoryName: values.categoryName,
                    parentCode: values.parentCode || '',
                    skillCount: 0,
                })
            }
            setCategories(updated)
            localStorage.setItem('z_skill_categories', JSON.stringify(updated))
            setCatModalVisible(false)
            message.success(editingCategory ? '更新成功' : '创建成功')
        } finally {
            setCatLoading(false)
        }
    }

    const handleCategoryDelete = async (catCode) => {
        try {
            const cat = categories.find(c => c.categoryCode === catCode)
            const apiId = cat?.id
            if (apiId) {
                await skillApi.deleteCategory({params: {id: apiId}})
            } else {
                throw new Error('no api id')
            }
            message.success('删除成功')
            loadCategories()
        } catch (e) {
            // 回退：本地 mock
            const updated = categories.filter(c => c.categoryCode !== catCode)
            setCategories(updated)
            localStorage.setItem('z_skill_categories', JSON.stringify(updated))
            message.success('删除成功')
        }
    }

    const rootCategories = categories.filter(c => !c.parentCode)

    return (
        <div className={styles.marketRoot}>
            {/* 顶行：分类 tab + 搜索/排序/发布/分类管理 */}
            <div className={styles.topRow}>
                <div className={styles.tabs}>
                    <button
                        type="button"
                        className={`${styles.tab} ${categoryCode === undefined ? styles.tabActive : ''}`}
                        onClick={() => handleCategoryChange(undefined)}
                    >
                        全部
                    </button>
                    {rootCategories.map(cat => (
                        <button
                            key={cat.categoryCode}
                            type="button"
                            className={`${styles.tab} ${categoryCode === cat.categoryCode ? styles.tabActive : ''}`}
                            onClick={() => handleCategoryChange(cat.categoryCode)}
                        >
                            {cat.categoryName}
                        </button>
                    ))}
                </div>
                <Input
                    prefix={<SearchOutlined style={{color: '#bfbfbf'}}/>}
                    placeholder="搜索技能..."
                    value={keyword}
                    onChange={e => setKeyword(e.target.value)}
                    onPressEnter={handleKeywordSearch}
                    allowClear
                    style={{width: 200, flexShrink: 0}}
                />
                <Segmented
                    value={sortBy}
                    onChange={handleSortChange}
                    options={[
                        {value: 'download_count', label: '热门'},
                        {value: 'gmt_create', label: '最新'},
                    ]}
                    style={{flexShrink: 0}}
                />
                <Button
                    type="primary"
                    icon={<CloudUploadOutlined/>}
                    onClick={() => setPubModalOpen(true)}
                    style={{
                        flexShrink: 0, borderRadius: 8, fontWeight: 600,
                        background: '#007aff', border: 'none',
                    }}
                >
                    发布技能
                </Button>
                <Dropdown
                    trigger={['click']}
                    menu={{
                        items: [
                            {key: 'add', icon: <PlusOutlined/>, label: '新增分类'},
                            {key: 'reload', icon: <ReloadOutlined/>, label: '刷新分类'},
                            {type: 'divider'},
                            ...rootCategories.map(cat => ({
                                key: cat.categoryCode,
                                label: cat.categoryName,
                                children: [
                                    {key: `edit:${cat.categoryCode}`, icon: <EditOutlined/>, label: '编辑分类'},
                                    {
                                        key: `delete:${cat.categoryCode}`,
                                        icon: <DeleteOutlined/>,
                                        label: '删除分类',
                                        danger: true
                                    },
                                ],
                            })),
                        ],
                        onClick: ({key}) => {
                            if (key === 'add') {
                                openAddCategory(null)
                                return
                            }
                            if (key === 'reload') {
                                loadCategories()
                                return
                            }
                            const [action, code] = String(key).split(':')
                            const cat = categories.find(c => c.categoryCode === code)
                            if (!cat) return
                            if (action === 'edit') {
                                openEditCategory(cat)
                            } else if (action === 'delete') {
                                Modal.confirm({
                                    title: `删除分类"${cat.categoryName}"？`,
                                    content: '子分类也会被删除',
                                    okText: '确认删除',
                                    okType: 'danger',
                                    cancelText: '取消',
                                    onOk: () => handleCategoryDelete(code),
                                })
                            }
                        },
                    }}
                >
                    <Button size="small" icon={<SettingOutlined/>} title="分类管理"/>
                </Dropdown>
            </div>

            {/* Card Grid */}
                {loading ? (
                    <div style={{textAlign: 'center', padding: 60}}>
                        <Spin size="large"/>
                    </div>
                ) : skills.length === 0 ? (
                    <Empty description="暂无技能" style={{padding: 60}}/>
                ) : (
                    <Row gutter={[20, 20]}>
                        {skills.map(skill => {
                            const tint = CATEGORY_TINT[skill.categoryCode] || CATEGORY_TINT.default
                            return (
                                <Col key={skill.id} xs={24} sm={12} xl={8}>
                                    <div className={styles.card} onClick={() => openDetail(skill)}>
                                        <div className={styles.cardIcon}
                                             style={{background: tint.bg, color: tint.fg}}>
                                            <CodeOutlined style={{fontSize: 22}}/>
                                        </div>
                                        <div className={styles.cardBody}>
                                            <div className={styles.cardTitleRow}>
                                                <span className={styles.cardTitle}>{skill.skillName}</span>
                                                {skill.status !== 'PUBLISHED' && (
                                                    <Tag style={{
                                                        fontSize: 11,
                                                        lineHeight: '16px',
                                                        marginInlineEnd: 0,
                                                        flexShrink: 0
                                                    }}>
                                                        {STATUS_LABEL[skill.status] || skill.status}
                                                    </Tag>
                                                )}
                                            </div>
                                            <div className={styles.cardDesc} title={skill.description}>
                                                {skill.description || '暂无描述'}
                                            </div>
                                        </div>
                                        <Button
                                            className={styles.installBtn}
                                            type="primary"
                                            size="small"
                                            icon={<DownloadOutlined/>}
                                            onClick={e => {
                                                e.stopPropagation();
                                                handleInstall(skill.skillCode)
                                            }}
                                            style={{
                                                borderRadius: 999, background: '#007aff',
                                                border: 'none', flexShrink: 0,
                                            }}
                                        >
                                            安装
                                        </Button>
                                    </div>
                                </Col>
                            )
                        })}
                    </Row>
                )}

                {/* Pagination */}
                {total > pageSizeRef.current && (
                    <div style={{textAlign: 'right', padding: '8px 0'}}>
                        <Pagination
                            current={current}
                            pageSize={pageSizeRef.current}
                            total={total}
                            onChange={p => {
                                setCurrent(p);
                                window.scrollTo({top: 0, behavior: 'smooth'})
                            }}
                            showSizeChanger={false}
                        />
                    </div>
                )}

            {/* Detail Drawer */}
            <Drawer
                title={
                    <div style={{display: 'flex', alignItems: 'center', gap: 12}}>
                        <div style={{
                            width: 36, height: 36, borderRadius: 8,
                            background: getGradient(detailSkill?.categoryCode),
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}>
                            <CodeOutlined style={{fontSize: 18, color: '#fff'}}/>
                        </div>
                        <div>
                            <div style={{fontSize: 15, fontWeight: 600}}>{detailSkill?.skillName}</div>
                            <div style={{fontSize: 11, color: '#999', fontWeight: 400}}>
                                v{detailSkill?.version} · {detailSkill?.author} · {STATUS_LABEL[detailSkill?.status] || detailSkill?.status}
                            </div>
                        </div>
                    </div>
                }
                open={drawerOpen}
                onClose={() => setDrawerOpen(false)}
                size="large"
                styles={{header: {padding: '14px 24px', borderBottom: '1px solid #f0f0f0'}, body: {padding: 0}}}
            >
                {detailSkill && (
                    <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
                        {/* Top action bar */}
                        <div style={{
                            padding: '16px 24px', borderBottom: '1px solid #f0f0f0',
                            display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap',
                            background: '#fafafa',
                        }}>
                            {/* Download / Copy button */}
                            {detailSkill.packageType === 'ZIP' && detailSkill.packagePath ? (
                                <Button type="primary" size="large" icon={<DownloadOutlined/>}
                                        onClick={() => handleDownload(detailSkill)}
                                        style={{
                                            borderRadius: 8,
                                            background: getGradient(detailSkill.categoryCode),
                                            border: 'none',
                                            fontWeight: 600,
                                            height: 40,
                                            paddingInline: 24
                                        }}>
                                    下载技能包 (ZIP)
                                </Button>
                            ) : detailSkill.content ? (
                                <Button type="primary" size="large" icon={<CopyOutlined/>}
                                        onClick={() => handleCopyContent(detailSkill.content)}
                                        style={{
                                            borderRadius: 8,
                                            background: getGradient(detailSkill.categoryCode),
                                            border: 'none',
                                            fontWeight: 600,
                                            height: 40,
                                            paddingInline: 24
                                        }}>
                                    复制技能内容
                                </Button>
                            ) : (
                                <Button size="large" disabled icon={<DownloadOutlined/>} style={{height: 40}}>
                                    暂无下载包
                                </Button>
                            )}

                            {/* Install button */}
                            <Button icon={<DownloadOutlined/>}
                                    onClick={() => handleInstall(detailSkill.skillCode)}
                                    style={{borderRadius: 8, height: 40}}>
                                安装此技能
                            </Button>

                            {/* Info tags */}
                            <div style={{
                                flex: 1,
                                display: 'flex',
                                gap: 8,
                                flexWrap: 'wrap',
                                justifyContent: 'flex-end'
                            }}>
                                <Tag icon={<CheckCircleOutlined/>} color="blue">v{detailSkill.version}</Tag>
                                <Tag icon={<FireOutlined/>} color="red">{detailSkill.downloadCount || 0} 次安装</Tag>
                                <Tag icon={<StarOutlined/>} color="orange">{detailSkill.author}</Tag>
                                {detailSkill.gmtModified && (
                                    <Tag icon={<ClockCircleOutlined/>} color="default">
                                        {new Date(detailSkill.gmtModified).toLocaleDateString()}
                                    </Tag>
                                )}
                            </div>
                        </div>

                        {/* Tab content */}
                        <div style={{flex: 1, overflow: 'auto'}}>
                            <Tabs
                                activeKey={detailTab}
                                onChange={setDetailTab}
                                style={{height: '100%'}}
                                tabBarStyle={{padding: '0 24px', marginBottom: 0}}
                                items={[
                                    {
                                        key: 'overview',
                                        label: <span><InfoCircleOutlined/> 概述</span>,
                                        children: (
                                            <div style={{padding: '20px 24px'}}>
                                                {detailLoading ? (
                                                    <div style={{textAlign: 'center', padding: 40}}><Spin/></div>
                                                ) : (
                                                    <>
                                                        {/* Description */}
                                                        <div style={{marginBottom: 20}}>
                                                            <div style={{
                                                                fontSize: 14,
                                                                fontWeight: 600,
                                                                color: '#333',
                                                                marginBottom: 8
                                                            }}>简介
                                                            </div>
                                                            <div style={{fontSize: 14, color: '#555', lineHeight: 1.8}}>
                                                                {detailSkill.description || '暂无描述'}
                                                            </div>
                                                        </div>

                                                        {/* Tags */}
                                                        {detailSkill.tags && (
                                                            <div style={{marginBottom: 20}}>
                                                                <div style={{
                                                                    fontSize: 14,
                                                                    fontWeight: 600,
                                                                    color: '#333',
                                                                    marginBottom: 8
                                                                }}>标签
                                                                </div>
                                                                <div
                                                                    style={{display: 'flex', gap: 6, flexWrap: 'wrap'}}>
                                                                    {(detailSkill.tags || '').split(',').filter(Boolean).map((t, i) => (
                                                                        <Tag key={t}
                                                                             color={TAG_COLORS[i % TAG_COLORS.length]}
                                                                             style={{
                                                                                 fontSize: 12,
                                                                                 borderRadius: 4,
                                                                                 padding: '2px 8px'
                                                                             }}>
                                                                            {t}
                                                                        </Tag>
                                                                    ))}
                                                                </div>
                                                            </div>
                                                        )}

                                                        {/* Content / README */}
                                                        {detailContent && (
                                                            <div>
                                                                <div style={{
                                                                    fontSize: 14,
                                                                    fontWeight: 600,
                                                                    color: '#333',
                                                                    marginBottom: 8
                                                                }}>技能说明
                                                                </div>
                                                                <div style={{
                                                                    background: '#fafafa', border: '1px solid #f0f0f0',
                                                                    padding: 16, borderRadius: 8, lineHeight: 1.8,
                                                                    fontSize: 14, color: '#333',
                                                                }}>
                                                                    <ReactMarkdown
                                                                        components={{
                                                                            code: ({
                                                                                       node,
                                                                                       inline,
                                                                                       className,
                                                                                       children,
                                                                                       ...props
                                                                                   }) =>
                                                                                inline
                                                                                    ? <code style={{
                                                                                        background: '#f0f0f0',
                                                                                        padding: '2px 6px',
                                                                                        borderRadius: 4,
                                                                                        fontSize: 13
                                                                                    }} {...props}>{children}</code>
                                                                                    : <pre style={{
                                                                                        background: '#1e1e1e',
                                                                                        color: '#d4d4d4',
                                                                                        padding: 14,
                                                                                        borderRadius: 8,
                                                                                        overflow: 'auto',
                                                                                        fontSize: 13
                                                                                    }}><code {...props}>{children}</code></pre>,
                                                                            h1: ({children}) => <h1 style={{
                                                                                fontSize: 20,
                                                                                fontWeight: 700,
                                                                                borderBottom: '1px solid #eee',
                                                                                paddingBottom: 8,
                                                                                marginBottom: 12
                                                                            }}>{children}</h1>,
                                                                            h2: ({children}) => <h2 style={{
                                                                                fontSize: 17,
                                                                                fontWeight: 600,
                                                                                marginTop: 20,
                                                                                marginBottom: 8
                                                                            }}>{children}</h2>,
                                                                            h3: ({children}) => <h3 style={{
                                                                                fontSize: 15,
                                                                                fontWeight: 600,
                                                                                marginTop: 16,
                                                                                marginBottom: 6
                                                                            }}>{children}</h3>,
                                                                            ul: ({children}) => <ul style={{
                                                                                paddingLeft: 20,
                                                                                marginBottom: 12
                                                                            }}>{children}</ul>,
                                                                            li: ({children}) => <li
                                                                                style={{marginBottom: 4}}>{children}</li>,
                                                                            p: ({children}) => <p
                                                                                style={{marginBottom: 10}}>{children}</p>,
                                                                        }}
                                                                    >
                                                                        {detailContent}
                                                                    </ReactMarkdown>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </>
                                                )}
                                            </div>
                                        ),
                                    },
                                    {
                                        key: 'files',
                                        label: <span><FileTextOutlined/> 文件</span>,
                                        children: (
                                            <div style={{padding: '20px 24px'}}>
                                                {detailSkill.files && detailSkill.files.length > 0 ? (
                                                    <div style={{display: 'flex', gap: 16, height: 400}}>
                                                        {/* File tree */}
                                                        <div style={{
                                                            width: 220,
                                                            flexShrink: 0,
                                                            border: '1px solid #f0f0f0',
                                                            borderRadius: 8,
                                                            overflow: 'auto',
                                                            padding: '4px 0'
                                                        }}>
                                                            <Tree
                                                                treeData={buildFileTree(detailSkill.files)}
                                                                defaultExpandAll showIcon
                                                                selectedKeys={selectedFile ? [selectedFile.path] : []}
                                                                onSelect={(_, info) => {
                                                                    if (info.node.isLeaf && info.node.file) setSelectedFile(info.node.file)
                                                                }}
                                                                titleRender={(n) => <span
                                                                    style={{fontSize: 13}}>{n.title}</span>}
                                                                icon={(n) => !n.isLeaf
                                                                    ? <FolderOutlined
                                                                        style={{fontSize: 13, color: '#faad14'}}/>
                                                                    : <FileTextOutlined
                                                                        style={{fontSize: 13, color: '#8c8c8c'}}/>}
                                                            />
                                                        </div>
                                                        {/* File preview */}
                                                        <div
                                                            style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                                                            {selectedFile ? (
                                                                <>
                                                                    <div style={{
                                                                        fontSize: 12,
                                                                        color: '#999',
                                                                        marginBottom: 6
                                                                    }}>{selectedFile.path}</div>
                                                                    <pre style={{
                                                                        flex: 1,
                                                                        background: '#1e1e1e',
                                                                        color: '#d4d4d4',
                                                                        padding: 14,
                                                                        borderRadius: 8,
                                                                        fontSize: 12,
                                                                        lineHeight: 1.6,
                                                                        overflow: 'auto',
                                                                        whiteSpace: 'pre-wrap',
                                                                        fontFamily: '"SF Mono","Fira Code","Consolas",monospace',
                                                                        margin: 0
                                                                    }}>
                                    {selectedFile.content || '(empty)'}
                                  </pre>
                                                                </>
                                                            ) : (
                                                                <div style={{
                                                                    color: '#999',
                                                                    textAlign: 'center',
                                                                    paddingTop: 80
                                                                }}>选择一个文件查看内容</div>
                                                            )}
                                                        </div>
                                                    </div>
                                                ) : detailSkill.packageType === 'ZIP' && detailSkill.packagePath ? (
                                                    <div style={{
                                                        background: '#f6ffed', border: '1px solid #b7eb8f',
                                                        borderRadius: 8, padding: 20, textAlign: 'center',
                                                    }}>
                                                        <FileZipOutlined
                                                            style={{fontSize: 36, color: '#52c41a', marginBottom: 12}}/>
                                                        <div style={{
                                                            fontSize: 14,
                                                            fontWeight: 600,
                                                            color: '#389e0d',
                                                            marginBottom: 8
                                                        }}>ZIP 包已上传
                                                        </div>
                                                        <div style={{
                                                            fontSize: 12,
                                                            color: '#999',
                                                            wordBreak: 'break-all'
                                                        }}>{detailSkill.packagePath}</div>
                                                        <Button type="link" icon={<DownloadOutlined/>}
                                                                onClick={() => handleDownload(detailSkill)}
                                                                style={{marginTop: 12}}>
                                                            下载并解压查看文件
                                                        </Button>
                                                    </div>
                                                ) : (
                                                    <div style={{textAlign: 'center', padding: 40, color: '#999'}}>
                                                        <FileTextOutlined
                                                            style={{fontSize: 36, color: '#d9d9d9', marginBottom: 12}}/>
                                                        <div>暂无文件，上传 ZIP 包后可查看文件目录</div>
                                                    </div>
                                                )}

                                                {/* Upload section for skill author */}
                                                <div style={{
                                                    marginTop: 20,
                                                    borderTop: '1px solid #f0f0f0',
                                                    paddingTop: 16
                                                }}>
                                                    <div style={{
                                                        fontSize: 13,
                                                        fontWeight: 600,
                                                        color: '#333',
                                                        marginBottom: 8
                                                    }}>上传技能包
                                                    </div>
                                                    <Upload accept=".zip" showUploadList={false}
                                                            beforeUpload={handleUploadPackage} disabled={uploadLoading}>
                                                        <div style={{
                                                            border: '2px dashed #d9d9d9',
                                                            borderRadius: 8,
                                                            padding: 16,
                                                            textAlign: 'center',
                                                            cursor: uploadLoading ? 'not-allowed' : 'pointer',
                                                            opacity: uploadLoading ? 0.6 : 1,
                                                        }}>
                                                            {uploadLoading ? <Spin size="small"/> : <CloudUploadOutlined
                                                                style={{fontSize: 20, color: '#bfbfbf'}}/>}
                                                            <div style={{fontSize: 12, color: '#999', marginTop: 6}}>
                                                                {uploadLoading ? '上传中...' : (detailSkill.packageType === 'ZIP' ? '重新上传 ZIP' : '上传 ZIP 包')}
                                                            </div>
                                                        </div>
                                                    </Upload>
                                                </div>
                                            </div>
                                        ),
                                    },
                                    {
                                        key: 'versions',
                                        label:
                                            <span><HistoryOutlined/> 版本历史 {versions.length > 0 && `(${versions.length})`}</span>,
                                        children: (
                                            <div style={{padding: '20px 24px'}}>
                                                {versions.length > 0 ? (
                                                    <Timeline
                                                        items={versions.map((v, i) => ({
                                                            color: i === 0 ? 'green' : 'gray',
                                                            children: (
                                                                <div>
                                                                    <div style={{
                                                                        display: 'flex',
                                                                        alignItems: 'center',
                                                                        gap: 8
                                                                    }}>
                                                                        <Tag color={i === 0 ? 'green' : 'default'}
                                                                             style={{fontWeight: 600}}>
                                                                            {v.version || v.versionNumber || `v${i + 1}`}
                                                                        </Tag>
                                                                        {i === 0 && <Tag color="blue">当前版本</Tag>}
                                                                    </div>
                                                                    {v.changeLog && (
                                                                        <div style={{
                                                                            fontSize: 13,
                                                                            color: '#666',
                                                                            marginTop: 4
                                                                        }}>{v.changeLog}</div>
                                                                    )}
                                                                    <div style={{
                                                                        fontSize: 11,
                                                                        color: '#bbb',
                                                                        marginTop: 2
                                                                    }}>
                                                                        {v.gmtCreate ? new Date(v.gmtCreate).toLocaleString() : ''}
                                                                    </div>
                                                                </div>
                                                            ),
                                                        }))}
                                                    />
                                                ) : (
                                                    <div style={{textAlign: 'center', padding: 40, color: '#999'}}>
                                                        <HistoryOutlined
                                                            style={{fontSize: 36, color: '#d9d9d9', marginBottom: 12}}/>
                                                        <div>暂无版本记录</div>
                                                        <div style={{fontSize: 12, color: '#bbb', marginTop: 4}}>当前版本:
                                                            v{detailSkill.version}</div>
                                                    </div>
                                                )}
                                            </div>
                                        ),
                                    },
                                ]}
                            />
                        </div>
                    </div>
                )}
            </Drawer>

            {/* Publish Modal */}
            <Modal
                title={
                    <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                        <div style={{
                            width: 32, height: 32, borderRadius: 8,
                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}>
                            <CloudUploadOutlined style={{fontSize: 16, color: '#fff'}}/>
                        </div>
                        <span style={{fontSize: 16}}>发布新技能</span>
                    </div>
                }
                open={pubModalOpen}
                onCancel={() => {
                    setPubModalOpen(false);
                    pubForm.resetFields()
                }}
                onOk={handlePublish}
                confirmLoading={pubLoading}
                okText="确认发布"
                cancelText="取消"
                size="large"
                style={{top: 80}}
            >
                <Form form={pubForm} layout="vertical" style={{marginTop: 12}}>
                    <div style={{display: 'flex', gap: 12}}>
                        <Form.Item name="skillCode" label="技能编码"
                                   rules={[{required: true, message: '请输入技能编码'}]} style={{flex: 1}}>
                            <Input placeholder="如 java-entity-gen" style={{borderRadius: 8}}/>
                        </Form.Item>
                        <Form.Item name="skillName" label="技能名称"
                                   rules={[{required: true, message: '请输入技能名称'}]} style={{flex: 1}}>
                            <Input placeholder="如 Java Entity 生成器" style={{borderRadius: 8}}/>
                        </Form.Item>
                    </div>
                    <div style={{display: 'flex', gap: 12}}>
                        <Form.Item name="categoryCode" label="分类" style={{flex: 1}}>
                            <Input placeholder="如 backend" style={{borderRadius: 8}}/>
                        </Form.Item>
                        <Form.Item name="version" label="版本号" initialValue="1.0.0" rules={[{required: true}]}
                                   style={{flex: 1}}>
                            <Input placeholder="1.0.0" style={{borderRadius: 8}}/>
                        </Form.Item>
                    </div>
                    <Form.Item name="description" label="描述">
                        <TextArea rows={3} placeholder="简要描述技能功能..." style={{borderRadius: 8}}/>
                    </Form.Item>
                    <Form.Item name="tags" label="标签">
                        <Input placeholder="用逗号分隔，如 java,entity,generator" style={{borderRadius: 8}}/>
                    </Form.Item>
                    <Form.Item name="author" label="作者">
                        <Input placeholder="技能作者" style={{borderRadius: 8}}/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* Category Create/Edit Modal */}
            <Modal
                title={editingCategory ? '编辑分类' : parentForCreate ? `新建子分类 - ${parentForCreate.categoryName}` : '新建分类'}
                open={catModalVisible}
                onCancel={() => setCatModalVisible(false)}
                onOk={() => categoryForm.submit()}
                confirmLoading={catLoading}
                destroyOnHidden
                okText="确认"
                cancelText="取消"
            >
                <Form form={categoryForm} layout="vertical" onFinish={handleCategorySubmit} style={{marginTop: 8}}>
                    <Form.Item name="categoryCode" label="分类编码"
                               rules={[{required: true, message: '请输入分类编码'}]}>
                        <Input placeholder="如 backend" disabled={!!editingCategory} style={{borderRadius: 8}}/>
                    </Form.Item>
                    <Form.Item name="categoryName" label="分类名称"
                               rules={[{required: true, message: '请输入分类名称'}]}>
                        <Input placeholder="如 后端开发" style={{borderRadius: 8}}/>
                    </Form.Item>
                    <Form.Item name="parentCode" label="父分类">
                        <Select
                            allowClear
                            placeholder="留空为顶级分类"
                            popupMatchSelectWidth={false}
                        >
                            {categories
                                .filter(c => editingCategory ? c.categoryCode !== editingCategory.categoryCode : true)
                                .map(c => (
                                    <Select.Option key={c.categoryCode} value={c.categoryCode}>
                                        {c.categoryName}
                                    </Select.Option>
                                ))}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
