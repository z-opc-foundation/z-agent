/**
 * 路由表: 把 agentApi / flowApi / llmApi / mcpApi / scriptApi / skillApi /
 * productApi / sceneApi / ossApi 方法名映射到真实后端 endpoint。
 *
 * 移植自主壳 src/services/apiRouter.ts，只保留本包页面实际用到的 Api。
 * 未命中 → 返空响应（不影响 build）。
 */
import {reg} from './apiRouter.js'

// =============================================================
// llmApi (LLM 模型)
// =============================================================
reg('llmApi', 'knowledgeList', 'GET', '/llm-gateway/knowledge/list')
reg('llmApi', 'skillTemplateList', 'GET', '/llm-gateway/skill/template-list')
reg('llmApi', 'toolTemplateList', 'GET', '/llm-gateway/tool/template-list')
reg('llmApi', 'appConfig', 'GET', '/llm-gateway/app/config')
reg('llmApi', 'appSkillAdd', 'POST', '/llm-gateway/app/skill-add')
reg('llmApi', 'appToolAdd', 'POST', '/llm-gateway/app/tool-add')

// =============================================================
// mcpApi
// =============================================================
// z-mcp 的上游是配置项 (z.mcp.servers[*]) 驱动的，没有 server 表 ⇒ 只读三格。
// create/update/delete/test 四个写端点随 z-agent-mcp 一族退场，别再往回挂。
reg('mcpApi', 'list', 'GET', '/mcp/server/list')
reg('mcpApi', 'listTools', 'POST', '/mcp/server/tools/list')
reg('mcpApi', 'callTool', 'POST', '/mcp/server/tools/call')

// =============================================================
// ossApi (对象存储 z-oss)
// =============================================================
reg('ossApi', 'listBuckets', 'GET', '/v1/bucket')
reg('ossApi', 'getBucketStats', 'GET', '/v1/bucket/stats')
reg('ossApi', 'createBucket', 'POST', '/v1/bucket')
reg('ossApi', 'deleteBucket', 'DELETE', '/v1/bucket')
reg('ossApi', 'setBucketAcl', 'PUT', '/v1/bucket/acl')
reg('ossApi', 'listObjects', 'GET', '/v1/object')
reg('ossApi', 'presignedUrl', 'GET', '/v1/object/url')
reg('ossApi', 'uploadObject', 'POST', '/v1/object')
reg('ossApi', 'downloadUrl', 'GET', '/v1/object/url')
reg('ossApi', 'deleteObject', 'DELETE', '/v1/object')
reg('ossApi', 'copyObject', 'POST', '/v1/object/copy')
reg('ossApi', 'batchDelete', 'POST', '/v1/object/batch-delete')
reg('ossApi', 'createFolder', 'POST', '/v1/folder')

// =============================================================
// scriptApi (脚本中心)
// =============================================================
reg('scriptApi', 'list', 'GET', '/script/list')
reg('scriptApi', 'create', 'POST', '/script')
reg('scriptApi', 'update', 'POST', '/script/update')
reg('scriptApi', 'delete', 'POST', '/script/delete')
reg('scriptApi', 'publish', 'POST', '/script/publish')
reg('scriptApi', 'unpublish', 'POST', '/script/unpublish')
reg('scriptApi', 'run', 'POST', '/script/run')
reg('scriptApi', 'importCurl', 'POST', '/script/import-curl')
reg('scriptApi', 'importOpenApi', 'POST', '/script/import-openapi')
reg('scriptApi', 'previewMapping', 'POST', '/script/preview-mapping')

// =============================================================
// skillApi (技能市场)
// =============================================================
reg('skillApi', 'page', 'POST', '/skill/page')
reg('skillApi', 'categoryTree', 'GET', '/skill/category/tree')
reg('skillApi', 'create', 'POST', '/skill')
reg('skillApi', 'createCategory', 'POST', '/skill/category')
reg('skillApi', 'deleteCategory', 'POST', '/skill/category/delete')
reg('skillApi', 'getBySkillCode', 'GET', '/skill/by-code')
reg('skillApi', 'install', 'POST', '/skill/install')
reg('skillApi', 'stats', 'GET', '/skill/stats')
reg('skillApi', 'versions', 'GET', '/skill/versions')
reg('skillApi', 'downloadPackage', 'GET', '/skill/download')
reg('skillApi', 'uploadPackage', 'POST', '/skill/upload')

// =============================================================
// agentApi (Agent 应用)
// =============================================================
reg('agentApi', 'appPage', 'POST', '/agent/app/page')
reg('agentApi', 'appGet', 'GET', '/agent/app/get')
reg('agentApi', 'appCreate', 'POST', '/agent/app')
reg('agentApi', 'appUpdate', 'POST', '/agent/app/update')
reg('agentApi', 'appDelete', 'POST', '/agent/app/delete')
reg('agentApi', 'appPublish', 'POST', '/agent/app/publish')
reg('agentApi', 'appUpgrade', 'POST', '/agent/app/upgrade')
reg('agentApi', 'appToggleShare', 'POST', '/agent/app/toggleShare')
reg('agentApi', 'versions', 'GET', '/agent/app/versions')
reg('agentApi', 'groupTree', 'GET', '/agent/group/tree')
reg('agentApi', 'groupCreate', 'POST', '/agent/group')
reg('agentApi', 'groupUpdate', 'POST', '/agent/group/update')
reg('agentApi', 'groupDelete', 'POST', '/agent/group/delete')
reg('agentApi', 'chatHistory', 'GET', '/agent/chat/history')
reg('agentApi', 'chatStream', 'POST', '/agent/chat/send')
reg('agentApi', 'chatClear', 'POST', '/agent/chat/clear')
reg('agentApi', 'shareVerify', 'GET', '/agent/share/verify')

// =============================================================
// flowApi (Agent 工作流 FEATURE013 A5)
// =============================================================
reg('flowApi', 'list', 'GET', '/agent/flow/list')
reg('flowApi', 'get', 'GET', '/agent/flow/byId')
reg('flowApi', 'byFlowId', 'GET', '/agent/flow/byFlowId')
reg('flowApi', 'versions', 'GET', '/agent/flow/versions')
reg('flowApi', 'create', 'POST', '/agent/flow/create')
reg('flowApi', 'update', 'POST', '/agent/flow/update')
reg('flowApi', 'publish', 'POST', '/agent/flow/publish')
reg('flowApi', 'archive', 'POST', '/agent/flow/archive')
reg('flowApi', 'newVersion', 'POST', '/agent/flow/newVersion')
reg('flowApi', 'delete', 'POST', '/agent/flow/delete')
reg('flowApi', 'execute', 'POST', '/agent/flow/execute')
reg('flowApi', 'health', 'GET', '/agent/flow/health')

// =============================================================
// statsApi (Agent 监控面板 FEATURE013 T3.2)
// =============================================================
reg('statsApi', 'overview', 'GET', '/agent/stats/overview')
reg('statsApi', 'trend', 'GET', '/agent/stats/trend')
reg('statsApi', 'latency', 'GET', '/agent/stats/latency')
reg('statsApi', 'byApp', 'GET', '/agent/stats/by-app')
reg('statsApi', 'byUser', 'GET', '/agent/stats/by-user')
reg('statsApi', 'health', 'GET', '/agent/stats/health')

// =============================================================
// pluginApi (Agent 可执行插件 FEATURE013 T2.1)
// =============================================================
reg('pluginApi', 'page', 'GET', '/agent/plugin/page')
reg('pluginApi', 'get', 'GET', '/agent/plugin/byId')
reg('pluginApi', 'byCode', 'GET', '/agent/plugin/byCode')
reg('pluginApi', 'create', 'POST', '/agent/plugin/create')
reg('pluginApi', 'update', 'POST', '/agent/plugin/update')
reg('pluginApi', 'status', 'POST', '/agent/plugin/status')
reg('pluginApi', 'delete', 'POST', '/agent/plugin/delete')
reg('pluginApi', 'execute', 'POST', '/agent/plugin/execute')
reg('pluginApi', 'builtinBeans', 'GET', '/agent/plugin/builtin-beans')
reg('pluginApi', 'health', 'GET', '/agent/plugin/health')

// =============================================================
// triggerApi (Agent 定时触发 FEATURE013 T3.1)
// =============================================================
reg('triggerApi', 'page', 'GET', '/agent/trigger/page')
reg('triggerApi', 'get', 'GET', '/agent/trigger/byId')
reg('triggerApi', 'create', 'POST', '/agent/trigger/create')
reg('triggerApi', 'update', 'POST', '/agent/trigger/update')
reg('triggerApi', 'status', 'POST', '/agent/trigger/status')
reg('triggerApi', 'delete', 'POST', '/agent/trigger/delete')
reg('triggerApi', 'fire', 'POST', '/agent/trigger/fire')
reg('triggerApi', 'health', 'GET', '/agent/trigger/health')

// =============================================================
// templateApi (Agent 模板市场 FEATURE013 T3.3)
// =============================================================
reg('templateApi', 'page', 'GET', '/agent/template/page')
reg('templateApi', 'get', 'GET', '/agent/template/byId')
reg('templateApi', 'hot', 'GET', '/agent/template/hot')
reg('templateApi', 'create', 'POST', '/agent/template/create')
reg('templateApi', 'update', 'POST', '/agent/template/update')
reg('templateApi', 'status', 'POST', '/agent/template/status')
reg('templateApi', 'delete', 'POST', '/agent/template/delete')
reg('templateApi', 'fork', 'POST', '/agent/template/fork')
reg('templateApi', 'health', 'GET', '/agent/template/health')

// =============================================================
// textGenApi (Agent 文本生成器 FEATURE013 T3.4)
// =============================================================
reg('textGenApi', 'page', 'GET', '/agent/textgen/page')
reg('textGenApi', 'get', 'GET', '/agent/textgen/byId')
reg('textGenApi', 'results', 'GET', '/agent/textgen/results')
reg('textGenApi', 'create', 'POST', '/agent/textgen/create')
reg('textGenApi', 'delete', 'POST', '/agent/textgen/delete')
reg('textGenApi', 'run', 'POST', '/agent/textgen/run')
reg('textGenApi', 'createAndRun', 'POST', '/agent/textgen/createAndRun')
reg('textGenApi', 'health', 'GET', '/agent/textgen/health')

// =============================================================
// humanInputApi (Human-in-the-Loop FEATURE013 T3.5)
// =============================================================
reg('humanInputApi', 'inbox', 'GET', '/agent/human/inbox')
reg('humanInputApi', 'listByExecution', 'GET', '/agent/human/execution')
reg('humanInputApi', 'getById', 'GET', '/agent/human/byId')
reg('humanInputApi', 'getByCode', 'GET', '/agent/human/by-code')
reg('humanInputApi', 'decide', 'POST', '/agent/human/decide')
reg('humanInputApi', 'create', 'POST', '/agent/human/create')
reg('humanInputApi', 'health', 'GET', '/agent/human/health')

// =============================================================
// productApi (产品中心)
// =============================================================
reg('productApi', 'page', 'GET', '/v1/products')
reg('productApi', 'create', 'POST', '/v1/products')
reg('productApi', 'update', 'PUT', '/v1/products')
reg('productApi', 'delete', 'DELETE', '/v1/products')
reg('productApi', 'publish', 'POST', '/v1/products/publish')
reg('productApi', 'offline', 'POST', '/v1/products/offline')
reg('productApi', 'configGet', 'GET', '/v1/products/config')
reg('productApi', 'configSave', 'PUT', '/v1/products/config')

// =============================================================
// sceneApi (场景编排)
// =============================================================
reg('sceneApi', 'page', 'GET', '/v1/scenes')
reg('sceneApi', 'create', 'POST', '/v1/scenes')
reg('sceneApi', 'update', 'PUT', '/v1/scenes')
reg('sceneApi', 'delete', 'DELETE', '/v1/scenes')
reg('sceneApi', 'duplicate', 'POST', '/v1/scenes/duplicate')
reg('sceneApi', 'publish', 'POST', '/v1/scenes/publish')
reg('sceneApi', 'offline', 'POST', '/v1/scenes/offline')
reg('sceneApi', 'execute', 'POST', '/v1/scenes/execute')
reg('sceneApi', 'canvasGet', 'GET', '/v1/scenes/canvas')
reg('sceneApi', 'configGet', 'GET', '/v1/scenes/config')
reg('sceneApi', 'nodes', 'GET', '/v1/scenes/nodes')
reg('sceneApi', 'chatHistory', 'GET', '/v1/scenes/chat-history')
