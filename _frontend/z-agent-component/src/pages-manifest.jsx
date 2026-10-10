/**
 * z-agent 页面清单（lead 005 §9）：menuItems + routeTable + 具名页面导出。
 */
import { AppstoreOutlined, BarChartOutlined, CloudOutlined, ClusterOutlined, CodeOutlined, DashboardOutlined, DeploymentUnitOutlined, EditOutlined, FileTextOutlined, HomeOutlined, KeyOutlined, NodeIndexOutlined, RobotOutlined, ShareAltOutlined, ShoppingOutlined } from '@ant-design/icons'
import AgentHome from './agent/pages/agent/index.jsx'
import AgentDashboard from './agent/pages/agent/dashboard/index.jsx'
import AgentAppEditor from './agent/pages/agent/editor/AgentAppEditor.jsx'
import AgentShare from './agent/pages/agent/share.jsx'
import AgentAppPage from './agent/pages/agent/app/index.jsx'
import AkManage from './agent/pages/ak/index.jsx'
import LlmHome from './agent/pages/llm/index.jsx'
import LlmModel from './agent/pages/llm/model/index.jsx'
import LlmProvider from './agent/pages/llm/provider/index.jsx'
import McpManage from './agent/pages/mcp/index.jsx'
import OssBrowser from './agent/pages/oss/ObjectBrowser.jsx'
import BucketList from './agent/pages/oss/BucketList.jsx'
import ProductManage from './agent/pages/product/index.jsx'
import ProductScene from './agent/pages/product/scene.jsx'
import ProductExecute from './agent/pages/product/execute.jsx'
import ScriptHome from './agent/pages/script/index.jsx'
import ScriptProductization from './agent/pages/script/ScriptProductizationPage.jsx'
import SkillManage from './agent/pages/skill/index.jsx'
import TemplateManage from './agent/pages/template/index.jsx'
import TraceHome from './agent/pages/trace/index.jsx'
import UsageHome from './agent/pages/usage/index.jsx'




export {
    AgentHome, AgentDashboard, AgentAppEditor, AgentShare, AgentAppPage,
    AkManage, LlmHome, LlmModel, LlmProvider, McpManage,
    OssBrowser, BucketList, ProductManage, ProductScene, ProductExecute,
    ScriptHome, ScriptProductization, SkillManage, TemplateManage, TraceHome, UsageHome,
}
import HomePage from './pages/HomePage'

/** 菜单 + 路由清单（lead 008 §10/§14/§16 批量落地）。App 壳在 suit 侧组装。 */
export const appMeta = { title: 'z-agent 智能体工作台', short: 'z-agent' }

export const menuItems = [
    { key: '/z-agent/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-agent/overview', label: '应用总览', icon: <AppstoreOutlined /> },
    { key: '/z-agent/agent/dashboard', label: '运行看板', icon: <DashboardOutlined /> },
    { key: '/z-agent/agent/editor', label: '应用编辑器', icon: <EditOutlined /> },
    { key: '/z-agent/agent/share', label: '分享页', icon: <ShareAltOutlined /> },
    { key: '/z-agent/agent/app', label: '应用实例', icon: <NodeIndexOutlined /> },
    { key: '/z-agent/llm', label: 'LLM 管理', icon: <RobotOutlined /> },
    { key: '/z-agent/llm/model', label: '模型管理', icon: <RobotOutlined /> },
    { key: '/z-agent/llm/provider', label: '供应商', icon: <RobotOutlined /> },
    { key: '/z-agent/ak', label: 'AK 管理', icon: <KeyOutlined /> },
    { key: '/z-agent/mcp', label: 'MCP 工具', icon: <ClusterOutlined /> },
    { key: '/z-agent/oss', label: 'OSS 浏览', icon: <CloudOutlined /> },
    { key: '/z-agent/oss/bucket', label: '存储桶', icon: <CloudOutlined /> },
    { key: '/z-agent/product', label: '产品管理', icon: <ShoppingOutlined /> },
    { key: '/z-agent/product/scene', label: '场景编排', icon: <ShoppingOutlined /> },
    { key: '/z-agent/product/execute', label: '产品执行', icon: <ShoppingOutlined /> },
    { key: '/z-agent/script', label: '脚本', icon: <CodeOutlined /> },
    { key: '/z-agent/script/productization', label: '脚本产品化', icon: <CodeOutlined /> },
    { key: '/z-agent/skill', label: '技能', icon: <DeploymentUnitOutlined /> },
    { key: '/z-agent/template', label: '模板', icon: <FileTextOutlined /> },
    { key: '/z-agent/trace', label: '调用追踪', icon: <NodeIndexOutlined /> },
    { key: '/z-agent/usage', label: '用量统计', icon: <BarChartOutlined /> },
]

export const routeTable = [
    { path: '/z-agent/home', Component: HomePage },
    { path: '/z-agent/overview', Component: AgentHome },
    { path: '/z-agent/agent/dashboard', Component: AgentDashboard },
    { path: '/z-agent/agent/editor', Component: AgentAppEditor },
    { path: '/z-agent/agent/share', Component: AgentShare },
    { path: '/z-agent/agent/app', Component: AgentAppPage },
    { path: '/z-agent/llm', Component: LlmHome },
    { path: '/z-agent/llm/model', Component: LlmModel },
    { path: '/z-agent/llm/provider', Component: LlmProvider },
    { path: '/z-agent/ak', Component: AkManage },
    { path: '/z-agent/mcp', Component: McpManage },
    { path: '/z-agent/oss', Component: BucketList },
    { path: '/z-agent/oss/bucket', Component: BucketList },
    { path: '/z-agent/oss/bucket/:bucketName', Component: OssBrowser },
    { path: '/z-agent/product', Component: ProductManage },
    { path: '/z-agent/product/scene', Component: ProductScene },
    { path: '/z-agent/product/execute', Component: ProductExecute },
    { path: '/z-agent/script', Component: ScriptHome },
    { path: '/z-agent/script/productization', Component: ScriptProductization },
    { path: '/z-agent/skill', Component: SkillManage },
    { path: '/z-agent/template', Component: TemplateManage },
    { path: '/z-agent/trace', Component: TraceHome },
    { path: '/z-agent/usage', Component: UsageHome },
]

export { default as HomePage } from './pages/HomePage'
export { default as LoginPage } from './pages/LoginPage'
