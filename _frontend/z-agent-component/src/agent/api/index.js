/**
 * API 客户端入口: 注册路由表后导出 *Api 代理对象。
 *
 * 使用：
 *   import {agentApi, flowApi, llmApi} from '@yuku123/z-agent-frontend-component/api'
 *   import request from '@yuku123/z-agent-frontend-component/api'  // 裸 axios 实例
 */
import './routes.js'
import {makeApi} from './apiRouter.js'
import request, {authRequest, ctcRequest} from './request.js'

export const agentApi = makeApi('agentApi')
export const flowApi = makeApi('flowApi')
export const statsApi = makeApi('statsApi')
export const pluginApi = makeApi('pluginApi')
export const memoryApi = makeApi('memoryApi')
export const triggerApi = makeApi('triggerApi')
export const templateApi = makeApi('templateApi')
export const textGenApi = makeApi('textGenApi')
export const humanInputApi = makeApi('humanInputApi')
export const llmApi = makeApi('llmApi')
export const mcpApi = makeApi('mcpApi')
export const scriptApi = makeApi('scriptApi')
export const skillApi = makeApi('skillApi')
export const productApi = makeApi('productApi')
export const sceneApi = makeApi('sceneApi')
export const ossApi = makeApi('ossApi')

export {authRequest, ctcRequest}
export default request
