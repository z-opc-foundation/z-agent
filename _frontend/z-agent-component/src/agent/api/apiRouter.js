/**
 * 智能路由器：依据路由表把 agentApi.xxx() 翻译成真实的 HTTP 请求。
 * 移植自主壳 src/services/apiRouter.ts (FEATURE015)，保持兼容。
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
 * 「未实现」告警：下面两处兜底都会把失败伪装成 {code:200, data:[]}，
 * 调用方拿到的形状完全合法 —— 页面显示空列表 + 提示「加载成功」，
 * 翻代码看不出来，看日志也看不出来，tsc 更不会报。
 *
 * 兜底的返回值本轮刻意不改：实测全仓 666 处 makeApi 调用中有 254 处落在
 * 兜底分支上，直接改成抛错会让所有现在显示空列表的页面当场报错，
 * 其中很可能有页面正被当成「本来就没数据」在用。
 * 先让它们在开发环境出声，生产行为一字不变。
 *
 * 用法：本模块尚未实现该端点，请先在 agent/api/routes.js 注册对应 reg()。
 */
const FAKE_OK = {code: 200, message: 'ok', data: [], total: 0}

function warnUnimplemented(apiName, method, reason) {
    if (!import.meta.env?.DEV) return
    console.error(
        `[apiRouter] ${apiName}.${method}() 未实现，走了假成功兜底（${reason}）：` +
        `本次没有发出任何请求，返回的是空壳 {code:200, data:[]}。` +
        `若该端点本应存在，请先在 agent/api/routes.js 里 reg('${apiName}', '${method}', ...)。`
    )
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
                    warnUnimplemented(
                        apiName, prop,
                        ROUTES[apiName] ? '方法名未注册' : 'api key 未注册'
                    )
                    return FAKE_OK
                }
                try {
                    const path = fillPath(route.path, args)
                    const client = pickClient(route.verb, path)
                    const config = {}
                    if (route.verb === Verb.GET || route.verb === Verb.DELETE) {
                        if (args.length > 0 && typeof args[0] === 'object' && args[0] !== null) {
                            config.params = args[0]
                        }
                    } else {
                        // POST/PUT: 支持三种调用形式
                        //  1) api(args0)                  → args0 作为 body
                        //  2) api(args0, args1)            → args0 作为 body, args1 作为 query params
                        //  3) api({data, params})          → 解构 data 作为 body, params 作为 query params
                        //  4) api(args0, {params})         → args0 作为 body, params 作为 query params
                        if (args.length === 1) {
                            const a = args[0]
                            if (a && typeof a === 'object' && ('data' in a || 'params' in a)) {
                                if ('data' in a) config.data = a.data
                                if ('params' in a) config.params = a.params
                            } else {
                                config.data = a
                            }
                        } else if (args.length > 1) {
                            if (typeof args[0] === 'object' && args[0] !== null) {
                                if ('data' in args[0] || 'params' in args[0]) {
                                    if ('data' in args[0]) config.data = args[0].data
                                    if ('params' in args[0]) config.params = args[0].params
                                } else {
                                    config.data = args[0]
                                }
                            }
                            if (typeof args[1] === 'object' && args[1] !== null) {
                                if ('data' in args[1] || 'params' in args[1]) {
                                    if ('data' in args[1]) config.data = args[1].data
                                    if ('params' in args[1]) config.params = args[1].params
                                } else {
                                    config.params = args[1]
                                }
                            }
                        }
                    }
                    const resp = await client.request({
                        method: route.verb,
                        url: path,
                        ...config,
                    })
                    return route.transform ? route.transform(resp, args) : resp
                } catch (e) {
                    // 这一支比上面更危险：请求真的发出去了，是服务端/网络失败，
                    // 但返回值仍然谎称成功。调用方无从区分「没做」和「做成了但是空的」。
                    warnUnimplemented(apiName, prop, `请求失败: ${e?.message || e}`)
                    return FAKE_OK
                }
            }
        },
    })
}

export {reg}
