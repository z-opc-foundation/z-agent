export {configureAgent} from './agent/api/request.js'
export * from './agent/api/index.js'

// §8.7 域目录清退：agent 域 named exports（SystemShell 引用）
/**
 * z-agent-frontend-component 包入口
 *
 * 命名导出所有 agent / llm / mcp / oss / skill / script / product / scene 页面组件。
 * 与主壳 src/pages/ai/* 一一对应。
 */

export {default as AgentAppPage} from './pages/agent/index.jsx'
export {default as AgentAppList} from './pages/agent/app/index.jsx'
export {default as AgentAppEditor} from './pages/agent/editor/AgentAppEditor.jsx'
export {default as AgentSharePage} from './pages/agent/share.jsx'
export {default as WorkflowEditor} from './pages/agent/editor/WorkflowEditor.jsx'

export {default as ModelManage} from './pages/llm/index.jsx'
// FEATURE050: 模型与凭证 — 3 菜单合并单页入口
export {default as LlmManage} from './pages/llm/LlmManage.jsx'
export {default as LlmProvider} from './pages/llm/provider/index.jsx'
export {default as LlmModel} from './pages/llm/model/index.jsx'

export {default as AkManage} from './pages/ak/index.jsx'
export {default as AkUsageDrawer} from './pages/ak/AkUsageDrawer.jsx'

export {default as McpPage} from './pages/mcp/index.jsx'

export {default as BucketList} from './pages/oss/BucketList.jsx'
export {default as ObjectBrowser} from './pages/oss/ObjectBrowser.jsx'

export {default as ScriptCenterPage} from './pages/script/index.jsx'
export {default as ScriptProductizationPage} from './pages/script/ScriptProductizationPage.jsx'
export {default as ApiBridgeEditor} from './pages/script/ApiBridgeEditor.jsx'
export {default as CurlImportModal} from './pages/script/CurlImportModal.jsx'
export {default as OpenApiImportModal} from './pages/script/OpenApiImportModal.jsx'
export {default as FieldMappingEditor} from './pages/script/FieldMappingEditor.jsx'

export {default as SkillMarket} from './pages/skill/index.jsx'

export {default as ProductList} from './pages/product/index.jsx'
export {default as ProductExecute} from './pages/product/execute.jsx'
export {default as ProductScene} from './pages/product/scene.jsx'

export {default as UsageDashboard} from './pages/usage/index.jsx'
export {default as AgentDashboard} from './pages/agent/dashboard/index.jsx'
export {default as TemplateMarket} from './pages/template/index.jsx'

// FEATURE014: 多 Agent 实例管理 + Skill 绑定
export {default as AgentInstanceManager} from '../pages/ai/agent-instances/index.jsx'

export {
    agentApi,
    flowApi,
    statsApi,
    pluginApi,
    memoryApi,
    triggerApi,
    templateApi,
    textGenApi,
    humanInputApi,
    llmApi,
    mcpApi,
    scriptApi,
    skillApi,
    productApi,
    sceneApi,
    ossApi,
    authRequest,
    ctcRequest,
} from './api/index.js'
import request from './api/index.js'

export {request}
