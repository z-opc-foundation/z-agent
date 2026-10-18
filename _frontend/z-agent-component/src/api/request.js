/**
 * 包内自用的 axios 实例 + umi-request 风格 shim。
 *
 * 兼容主壳 pages/ai/{ak,llm,usage} 的老调用:
 *   request('/ak/list', {method: 'GET', params: {akType}})
 *   request('/agent/app/page', {method: 'POST', body: JSON.stringify({...})})
 *   request('/ak/update', {method: 'POST', data: {...}})
 *
 * 行为与主壳 services/request.ts 保持一致:
 *   - baseURL = /api
 *   - 401 自动跳 /login
 *   - 业务 {code, data, message} 自动解包
 *   - 支持 umi 风格: (url, {method, params, body, data})
 */
import {createRequest} from '@yuku123/z-frontend-common'

const request = createRequest({baseURL: '/api', timeout: 15000})
const authRequest = createRequest({baseURL: '/api', timeout: 15000})
const ctcRequest = createRequest({baseURL: '/api', timeout: 15000})

/**
 * umi-request 风格 shim: 把 (url, {method, params, body, data}) 转成 axios 调用。
 * axios 实例本身支持 callable，但只接受 config object。
 * 我们包一层函数，让它接受 (url, config) 并合成 config。
 */
function umiStyleWrap(axiosInstance) {
    const callable = function (url, opts = {}) {
        const config = {...opts}
        // 兼容 body / data: body 是 umi 风格, data 是 axios 风格
        if (config.body !== undefined) {
            config.data = config.body
            delete config.body
        }
        // 兼容 method 大小写
        if (typeof config.method === 'string') {
            config.method = config.method.toUpperCase()
        }
        // 把 url 放 config 里
        config.url = url
        return axiosInstance.request(config)
    }
    // 透传 axios 实例的所有方法
    const methods = ['get', 'post', 'put', 'delete', 'patch', 'head', 'request', 'create', 'interceptors']
    methods.forEach((m) => {
        if (typeof axiosInstance[m] === 'function') {
            callable[m] = axiosInstance[m].bind(axiosInstance)
        } else {
            callable[m] = axiosInstance[m]
        }
    })
    return callable
}

const umiRequest = umiStyleWrap(request)
const umiAuthRequest = umiStyleWrap(authRequest)
const umiCtcRequest = umiStyleWrap(ctcRequest)

export {authRequest, ctcRequest, umiAuthRequest, umiCtcRequest}
export default umiRequest
