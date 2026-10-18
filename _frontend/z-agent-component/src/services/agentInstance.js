/**
 * Agent Instance API Client (FEATURE014)
 *
 * 调用后端 z-agent-center 的实例管理 API。
 */
import {request} from '@yuku123/z-frontend-common'

// D18: request 实例 baseURL 已是 '/api', 此处用相对路径, 不要再写 /api 前缀.
const BASE = '/agent/instance'

export const agentInstanceApi = {
    /** 列出所有可用的 Agent 类型（前端下拉数据源） */
    listAgentTypes: () => request.get(`${BASE}/agent-types`),

    /** 按 ownerId 列出实例 */
    listByOwner: (ownerId) => request.get(`${BASE}/list`, {params: {ownerId}}),

    /** 查询单个实例详情 */
    get: (instanceCode) => request.get(`${BASE}/get`, {params: {instanceCode}}),

    /** 创建 ZBOT 实例（IN_PROCESS 模式，旧 API） */
    createFromApp: (appCode, userId, userName) =>
        request.post(`${BASE}/create`, {appCode, userId, userName}),

    /**
     * FEATURE014 — 创建多类型 Agent 实例。
     * @param agentType   ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM
     * @param callbackUrl 外部 Agent 必填
     * @param customCommand CUSTOM 类型时必填（含 {port} 占位符）
     */
    createExternal: ({appCode, userId, userName, agentType, callbackUrl, customCommand}) =>
        request.post(`${BASE}/create-external`, {
            appCode, userId, userName, agentType, callbackUrl, customCommand,
        }),

    /** 创建 Docker 容器实例（旧 API） */
    createContainer: (appCode, userId, userName) =>
        request.post(`${BASE}/create-container`, {appCode, userId, userName}),

    /** 销毁 Docker 容器实例 */
    destroyContainer: (instanceCode) =>
        request.post(`${BASE}/destroy-container`, null, {params: {instanceCode}}),

    /** 销毁本地进程实例 */
    destroyLocalProcess: (instanceCode) =>
        request.post(`${BASE}/destroy-local-process`, null, {params: {instanceCode}}),

    /** 健康检查 */
    health: (instanceCode) => request.get(`${BASE}/health`, {params: {instanceCode}}),

    /** 更新实例状态 */
    updateStatus: (instanceCode, status) =>
        request.post(`${BASE}/status`, null, {params: {instanceCode, status}}),
}

export const agentSkillBindApi = {
    bind: (instanceCode, skillCodes, bindingType = 'PUSH') =>
        request.post('/agent/skill/bind', {instanceCode, skillCodes, bindingType}),

    unbind: (instanceCode, skillCodes) =>
        request.post('/agent/skill/unbind', {instanceCode, skillCodes}),

    listBindings: (instanceCode) =>
        request.get('/agent/skill/bindings', {params: {instanceCode}}),

    syncLogs: (instanceCode, limit = 20) =>
        request.get('/agent/skill/sync-logs', {params: {instanceCode, limit}}),

    pull: (instanceCode) =>
        request.get('/agent/skill/pull', {params: {instanceCode}}),

    heartbeat: (instanceCode, status = 'ACTIVE', agentVersion = null) =>
        request.post('/agent/skill/heartbeat', {instanceCode, status, agentVersion}),

    register: ({appCode, userId, userName, agentType, callbackUrl, agentVersion, capabilities}) =>
        request.post('/agent/skill/register', {
            appCode, userId, userName, agentType, callbackUrl, agentVersion, capabilities,
        }),
}

/**
 * FEATURE014 Wishlist 7 — Skill 市场选择器
 * 调用 SkillCenter 的 REST API（运行在同 main-starter 上）。
 */
export const skillMarketApi = {
    /** 分页列出所有 PUBLISHED skill（FEATURE014 用作下拉数据源） */
    page: ({pageNum = 1, pageSize = 50, keyword = '', categoryCode = ''} = {}) =>
        request.post('/skill/page', {
            id: pageNum, keyword, status: 'PUBLISHED', categoryCode, size: pageSize,
        }),

    /** 取单个 skill 的完整元数据 */
    get: (skillCode) =>
        request.get('/skill/get', {params: {skillCode}}),
}

/**
 * z-bot 用户反馈 API（Wishlist 4）
 */
export const botFeedbackApi = {
    submit: ({instanceCode, traceId, userId, rating, comment, appCode, skillCodes}) =>
        request.post('/bot/feedback', {
            instanceCode, traceId, userId, rating, comment, appCode, skillCodes,
        }),

    recent: (instanceCode, limit = 20) =>
        request.get('/bot/feedback/recent', {params: {instanceCode, limit}}),

    score: (instanceCode) =>
        request.get('/bot/feedback/score', {params: {instanceCode}}),
}
