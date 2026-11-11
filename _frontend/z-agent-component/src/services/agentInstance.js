/**
 * Agent Instance API Client (FEATURE014)
 *
 * 调用后端 z-agent-center 的实例管理 API。
 */
import {request} from '@yuku123/z-frontend-common'

// D18: request 实例 baseURL 已是 '/api', 此处用相对路径, 不要再写 /api 前缀.
const BASE = '/agent/instance'

// aio 胖 jar 里 z-team-core 的 JacksonConfig 把全局 ObjectMapper 钉成 SNAKE_CASE,
// z-agent-center 作为内嵌域响应行键全部变 snake_case (instance_code / app_code / ...),
// 而本组页面按独立部署默认 camelCase 书写 —— 在服务层边界深转归一, 页面零改动。
const snakeToCamel = (k) => String(k).replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase())
const deepCamel = (v) => {
    if (Array.isArray(v)) return v.map(deepCamel)
    if (v && typeof v === 'object') {
        const out = {}
        for (const [k, val] of Object.entries(v)) out[snakeToCamel(k)] = deepCamel(val)
        return out
    }
    return v
}
const unwrap = (res) => {
    const data = res?.data !== undefined ? res.data : res
    return deepCamel(data ?? [])
}

// 请求体方向的 DTO (BindReq / HeartbeatReq / RegisterReq / AgentInstanceReq) 都是
// camelCase 字段且无 @JsonAlias, 在全局 SNAKE_CASE mapper 下只认 snake_case 键;
// @RequestParam 查询参数不受命名策略影响, 保持 camelCase。
const unwrapRows = (res) => {
    const data = unwrap(res)
    if (Array.isArray(data)) return data
    return data?.records || data?.list || []
}

export const agentInstanceApi = {
    /** 列出所有可用的 Agent 类型（前端下拉数据源） */
    listAgentTypes: async () => unwrap(await request.get(`${BASE}/agent-types`)),

    /** 按 ownerId 列出实例 */
    listByOwner: async (ownerId) => unwrapRows(await request.get(`${BASE}/list`, {params: {ownerId}})),

    /** 查询单个实例详情 */
    get: async (instanceCode) => unwrap(await request.get(`${BASE}/get`, {params: {instanceCode}})),

    /** 创建 ZBOT 实例（IN_PROCESS 模式，旧 API） */
    createFromApp: (appCode, userId, userName) =>
        request.post(`${BASE}/create`, {app_code: appCode, user_id: userId, user_name: userName}),

    /**
     * FEATURE014 — 创建多类型 Agent 实例。
     * @param agentType   ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM
     * @param callbackUrl 外部 Agent 必填
     * @param customCommand CUSTOM 类型时必填（含 {port} 占位符）
     */
    createExternal: ({appCode, userId, userName, agentType, callbackUrl, customCommand}) =>
        request.post(`${BASE}/create-external`, {
            app_code: appCode, user_id: userId, user_name: userName,
            agent_type: agentType, callback_url: callbackUrl, custom_command: customCommand,
        }),

    /** 创建 Docker 容器实例（旧 API） */
    createContainer: (appCode, userId, userName) =>
        request.post(`${BASE}/create-container`, {app_code: appCode, user_id: userId, user_name: userName}),

    /** 销毁 Docker 容器实例 */
    destroyContainer: (instanceCode) =>
        request.post(`${BASE}/destroy-container`, null, {params: {instanceCode}}),

    /** 销毁本地进程实例 */
    destroyLocalProcess: (instanceCode) =>
        request.post(`${BASE}/destroy-local-process`, null, {params: {instanceCode}}),

    /** 健康检查 */
    health: async (instanceCode) => unwrap(await request.get(`${BASE}/health`, {params: {instanceCode}})),

    /** 更新实例状态 */
    updateStatus: (instanceCode, status) =>
        request.post(`${BASE}/status`, null, {params: {instanceCode, status}}),
}

export const agentSkillBindApi = {
    bind: (instanceCode, skillCodes, bindingType = 'PUSH') =>
        request.post('/agent/skill/bind', {instance_code: instanceCode, skill_codes: skillCodes, binding_type: bindingType}),

    unbind: (instanceCode, skillCodes) =>
        request.post('/agent/skill/unbind', {instance_code: instanceCode, skill_codes: skillCodes}),

    listBindings: async (instanceCode) =>
        unwrapRows(await request.get('/agent/skill/bindings', {params: {instanceCode}})),

    syncLogs: async (instanceCode, limit = 20) =>
        unwrapRows(await request.get('/agent/skill/sync-logs', {params: {instanceCode, limit}})),

    pull: async (instanceCode) =>
        unwrap(await request.get('/agent/skill/pull', {params: {instanceCode}})),

    heartbeat: (instanceCode, status = 'ACTIVE', agentVersion = null) =>
        request.post('/agent/skill/heartbeat', {
            instance_code: instanceCode, status, agent_version: agentVersion,
        }),

    register: ({appCode, userId, userName, agentType, callbackUrl, agentVersion, capabilities}) =>
        request.post('/agent/skill/register', {
            app_code: appCode, user_id: userId, user_name: userName, agent_type: agentType,
            callback_url: callbackUrl, agent_version: agentVersion, capabilities,
        }),
}

/**
 * FEATURE014 Wishlist 7 — Skill 市场选择器
 *
 * <p>原实现 POST /skill/page 在全后端不存在（没有任何模块声明该路由）。z-skill
 * 的真实市场面是 {@code GET /api/skill/search?q=&page=&perPage=}，行形状是
 * SkillDto（id/slug/name/category/...），这里改接真面并映射成抽屉市场选择器
 * 读的键（skillCode/skillName/categoryCode）。</p>
 *
 * <p>已知后端缺口（前端无法补）：绑定后 SkillContentFetcher 走
 * {@code GET /api/skill/content?skillCode=}，z-skill 的 content 面是
 * {@code ?id=} 且 data 是对象不是 SKILL.md 原文 ⇒ 分发内容会记失败日志
 * （正好落在同步日志 tab 里可见），绑定关系本身成立。</p>
 */
export const skillMarketApi = {
    /** 检索 PUBLISHED skill（z-skill 市场面，page 从 0 计） */
    page: async ({pageNum = 1, pageSize = 50, keyword = ''} = {}) => {
        const res = await request.get('/skill/search', {
            params: {q: keyword || undefined, page: Math.max(0, pageNum - 1), perPage: pageSize},
        })
        const data = res?.data !== undefined ? res.data : res
        const skills = Array.isArray(data) ? data : (data?.skills || data?.records || [])
        const records = skills.map((s) => ({
            ...s,
            skillCode: s.skillCode || s.id || s.slug,
            skillName: s.skillName || s.name,
            categoryCode: s.categoryCode || s.category,
        }))
        return {...(data || {}), records, list: records, total: data?.total ?? records.length}
    },

    /** 取单个 skill 的完整元数据 */
    get: async (skillCode) =>
        unwrap(await request.get('/skill/detail', {params: {id: skillCode}})),
}

/**
 * z-bot 用户反馈 API（Wishlist 4）
 */
export const botFeedbackApi = {
    submit: ({instanceCode, traceId, userId, rating, comment, appCode, skillCodes}) =>
        request.post('/bot/feedback', {
            instance_code: instanceCode, trace_id: traceId, user_id: userId,
            rating, comment, app_code: appCode, skill_codes: skillCodes,
        }),

    recent: (instanceCode, limit = 20) =>
        request.get('/bot/feedback/recent', {params: {instanceCode, limit}}),

    score: (instanceCode) =>
        request.get('/bot/feedback/score', {params: {instanceCode}}),
}
