/**
 * z-agent 页面清单（lead 005 §9）：menuItems + routeTable + 具名页面导出。
 */
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

import {
    AppstoreOutlined,
    DashboardOutlined,
    EditOutlined,
    ShareAltOutlined,
    KeyOutlined,
    RobotOutlined,
    ClusterOutlined,
    CloudOutlined,
    ShoppingOutlined,
    CodeOutlined,
    DeploymentUnitOutlined,
    FileTextOutlined,
    NodeIndexOutlined,
    BarChartOutlined,
} from '@ant-design/icons'

export const menuItems = [
    {key: '/', label: '应用总览', icon: <AppstoreOutlined/>},
    {key: '/agent/dashboard', label: '运行看板', icon: <DashboardOutlined/>},
    {key: '/agent/editor', label: '应用编辑器', icon: <EditOutlined/>},
    {key: '/agent/share', label: '分享页', icon: <ShareAltOutlined/>},
    {key: '/agent/app', label: '应用实例', icon: <NodeIndexOutlined/>},
    {key: '/llm', label: 'LLM 管理', icon: <RobotOutlined/>},
    {key: '/llm/model', label: '模型管理', icon: <RobotOutlined/>},
    {key: '/llm/provider', label: '供应商', icon: <RobotOutlined/>},
    {key: '/ak', label: 'AK 管理', icon: <KeyOutlined/>},
    {key: '/mcp', label: 'MCP 工具', icon: <ClusterOutlined/>},
    {key: '/oss', label: 'OSS 浏览', icon: <CloudOutlined/>},
    {key: '/oss/bucket', label: '存储桶', icon: <CloudOutlined/>},
    {key: '/product', label: '产品管理', icon: <ShoppingOutlined/>},
    {key: '/product/scene', label: '场景编排', icon: <ShoppingOutlined/>},
    {key: '/product/execute', label: '产品执行', icon: <ShoppingOutlined/>},
    {key: '/script', label: '脚本', icon: <CodeOutlined/>},
    {key: '/script/productization', label: '脚本产品化', icon: <CodeOutlined/>},
    {key: '/skill', label: '技能', icon: <DeploymentUnitOutlined/>},
    {key: '/template', label: '模板', icon: <FileTextOutlined/>},
    {key: '/trace', label: '调用追踪', icon: <NodeIndexOutlined/>},
    {key: '/usage', label: '用量统计', icon: <BarChartOutlined/>},
]

export const routeTable = [
    {path: '/', Component: AgentHome},
    {path: '/agent/dashboard', Component: AgentDashboard},
    {path: '/agent/editor', Component: AgentAppEditor},
    {path: '/agent/share', Component: AgentShare},
    {path: '/agent/app', Component: AgentAppPage},
    {path: '/llm', Component: LlmHome},
    {path: '/llm/model', Component: LlmModel},
    {path: '/llm/provider', Component: LlmProvider},
    {path: '/ak', Component: AkManage},
    {path: '/mcp', Component: McpManage},
    {path: '/oss', Component: BucketList},
    {path: '/oss/bucket', Component: BucketList},
    {path: '/oss/bucket/:bucketName', Component: OssBrowser},
    {path: '/product', Component: ProductManage},
    {path: '/product/scene', Component: ProductScene},
    {path: '/product/execute', Component: ProductExecute},
    {path: '/script', Component: ScriptHome},
    {path: '/script/productization', Component: ScriptProductization},
    {path: '/skill', Component: SkillManage},
    {path: '/template', Component: TemplateManage},
    {path: '/trace', Component: TraceHome},
    {path: '/usage', Component: UsageHome},
]

export {
    AgentHome, AgentDashboard, AgentAppEditor, AgentShare, AgentAppPage,
    AkManage, LlmHome, LlmModel, LlmProvider, McpManage,
    OssBrowser, BucketList, ProductManage, ProductScene, ProductExecute,
    ScriptHome, ScriptProductization, SkillManage, TemplateManage, TraceHome, UsageHome,
}
