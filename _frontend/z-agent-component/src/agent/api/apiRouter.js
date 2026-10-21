/**
 * 智能路由器：依据路由表把 agentApi.xxx() 翻译成真实的 HTTP 请求。
 * 移植自主壳 src/services/apiRouter.ts (FEATURE015)，保持兼容。
 *
 * ── 2026-10-22 TASK-20260925-037：三级兜底已拆除（R1-1 / R1-2）──────────────
 * 旧实现有三处兜底，把三种"没做成"洗成同一种"成功且空"：
 *   ① 方法名没注册 → 直接 return {code:200,message:'ok',data:[],total:0}（**请求根本没发出去**）
 *   ② 404 / 网络错 / 非 2xx → catch 里同样 return {code:200,...}（**请求发出去了但失败了**）
 *   ③ 参数形状不对 → POST 分支把字符串当 body / GET 分支不设 params ⇒ 后端 400 ⇒ 又落进 ②
 * 三者返回的形状完全合法，页面显示空列表 + "加载成功"，翻代码、看日志都看不出来。
 *
 * 现在：
 *   - ① 抛 `ApiRouteError`（带 `__missing_route` 标记），**不发请求**；
 *   - ② 抛原始错误（带 `response.status`），**不再谎称成功**；
 *   - ③ GET/DELETE 统一吃 `{params:{...}}` 形状（旧的 GET 分支把 `{params:{...}}` 整个当 query，
 *     axios 会序列化成 `params[appCode]=xxx` ⇒ 必填参数丢失 ⇒ 400 ⇒ 又落进 ②）。
 * 失败必须**看得见**：调用方 catch 后渲染 ErrorState，而不是显示"暂无可用项"。
 */
import request, {authRequest, ctcRequest} from './request.js'

const Verb = {GET: 'GET', POST: 'POST', PUT: 'PUT', DELETE: 'DELETE'}

const ROUTES = {}

function reg(api, method, verb, path, transform) {
    if (!ROUTES[api]) ROUTES[api] = {}
    ROUTES[api][method] = {verb, path, transform}
}

function fillPath(path, args) {
    let i = 0
    return path.replace(/\{(\w+)\}/g, () => String(args[i++] ?? ''))
}

/**
 * 未注册路由（①）。`__missing_route` 是给调用方/探针看的机器可读标记。
 * 用法：本模块尚未实现该端点，请先在 agent/api/routes.js 注册对应 reg()。
 */
export class ApiRouteError extends Error {
    constructor(apiName, method, reason) {
        super(`[apiRouter] ${apiName}.${method}() 未注册路由（${reason}）：本次没有发出任何请求。` +
            `若该端点本应存在，请先在 agent/api/routes.js 里 reg('${apiName}', '${method}', ...)。`)
        this.name = 'ApiRouteError'
        this.__missing_route = true
        this.api = apiName
        this.method = method
    }
}

/**
 * 参数形状不对（③）。不静默丢弃参数 —— 丢弃 = 请求必填参数缺失 = 400 = 假绿。
 */
export class ApiParamError extends Error {
    constructor(apiName, method, message) {
        super(`[apiRouter] ${apiName}.${method}() 参数形状不对：${message}`)
        this.name = 'ApiParamError'
        this.__param_shape = true
        this.api = apiName
        this.method = method
    }
}

function pickClient(verb, path) {
    if (path.startsWith('/ctc/ac') || path.startsWith('/ctc/authorization') || path.startsWith('/ctc/surl')) {
        return ctcRequest
    }
    if (path.startsWith('/auth/')) {
        return authRequest
    }
    return request
}

/**
 * 把调用参数拆成 {data, params}。
 *
 * 支持的形状（两种动词都吃同一套，调用点不用记哪条规则）：
 *   api({params: {k: v}})      → query 参数
 *   api({data: {...}})          → body
 *   api({data: {...}, params})  → body + query
 *   api({k: v})                 → POST/PUT 当 body；GET/DELETE 当 query
 *   api(plainObject, {params})  → body + query
 */
function splitArgs(args) {
    const out = {data: undefined, params: undefined}
    if (!args.length) return out
    const a = args[0]
    if (a && typeof a === 'object' && !Array.isArray(a)) {
        if ('data' in a || 'params' in a) {
            if ('data' in a) out.data = a.data
            if ('params' in a) out.params = a.params
        } else {
            out.data = a
        }
    } else {
        out.data = a
    }
    if (args.length > 1) {
        const b = args[1]
        if (b && typeof b === 'object' && !Array.isArray(b)) {
            if ('params' in b) out.params = b.params
            else if ('data' in b) out.data = b.data
            else out.params = b
        }
    }
    return out
}

function buildConfig(verb, args, apiName, method) {
    const config = {}
    if (verb === Verb.GET || verb === Verb.DELETE) {
        const a = args[0]
        if (a !== undefined && a !== null) {
            if (typeof a !== 'object' || Array.isArray(a)) {
                // query 参数名未知 ⇒ 不能瞎猜，也不许静默丢弃（旧实现就是静默丢弃 ⇒ 必填参数缺失 ⇒ 400 ⇒ 假绿）
                throw new ApiParamError(apiName, method,
                    `GET/DELETE 端点收到标量参数 ${JSON.stringify(a)}，query 参数名未知，请改用 {params:{<参数名>: 值}}`)
            }
            config.params = ('params' in a) ? a.params : a
        }
        return config
    }
    const {data, params} = splitArgs(args)
    if (data !== undefined) config.data = data
    if (params !== undefined) config.params = params
    return config
}

export function makeApi(apiName) {
    return new Proxy({}, {
        get: (_t, prop) => {
            if (prop === 'then' || typeof prop === 'symbol') return undefined
            if (prop === 'toJSON' || prop === 'constructor' || prop === 'toString') {
                return () => `[apiRouter:${apiName}]`
            }
            return async (...args) => {
                const route = ROUTES[apiName]?.[prop]
                if (!route) {
                    // ① 未注册 ⇒ 抛错，绝不返回"成功且空"
                    const reason = ROUTES[apiName] ? '方法名未注册' : 'api key 未注册'
                    const err = new ApiRouteError(apiName, prop, reason)
                    console.error(err.message)
                    throw err
                }
                try {
                    const path = fillPath(route.path, args)
                    const client = pickClient(route.verb, path)
                    const config = buildConfig(route.verb, args, apiName, prop)
                    const resp = await client.request({
                        method: route.verb,
                        url: path,
                        ...config,
                    })
                    return route.transform ? route.transform(resp, args) : resp
                } catch (e) {
                    // ② 请求真的发出去了但失败了（404 / 非 2xx / 网络错 / 业务 code≠200）
                    //    —— 旧实现在这里 return {code:200,...}，把失败洗成"成功且空"。
                    //    现在把**原始错误**原样抛出去（保留 response.status / response.data，
                    //    ErrorState 靠它显示后端的真实原因），并打上机器可读标记。
                    if (e && e.__missing_route) throw e
                    if (e && e.__param_shape) {
                        console.error(e.message)
                        throw e
                    }
                    if (e && typeof e === 'object') {
                        e.__api_error = true
                        e.__api = apiName
                        e.__api_method = prop
                    }
                    const status = e?.response?.status
                    console.error(
                        `[apiRouter] ${apiName}.${prop}() 请求失败` +
                        `${status ? `（HTTP ${status}）` : ''}：${e?.message || e}`
                    )
                    throw e
                }
            }
        },
    })
}

export {reg}
