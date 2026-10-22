# z-agent-component

z-agent（智能体工作台）跨仓唯一前端面：把「Agent 应用 / LLM 与凭证 / MCP 工具 / OSS /
产品与场景 / 脚本 / 技能 / 模板 / 调用追踪 / 用量统计」这一整套智能体平台页面
收敛成一个可复用的 React 组件包，由 `z-agent-suit` 独立壳、z-team 工作台的
SystemShell 或 z-opc 主壳挂载。

- **npm 包名**：`@yuku123/z-agent-component`
- **宿主接入**：`@yuku123/z-agent-component/pages`（manifest）与
  `@yuku123/z-agent-component`（主入口的具名页面导出），主壳可只取 `ComponentDocPage`
- **后端**：z-agent 的 `/agent/**`、`/script/**`、`/skill/**`、`/mcp/**`、
  `/v1/products`、`/v1/scenes`、`/v1/bucket`、`/v1/object`、`/z-llm/config/**`
- **技术栈**：React 19 + antd 6 + react-router-dom 7 + echarts 5 + @logicflow/core +
  react-markdown + vite（library mode，双入口 `.` / `./pages`）

---

## 一、能力概览

⚠ **这个包里有两套页面树**，路由前缀与导出名都对不上，是最容易看错的地方：

- **树 A：`src/agent/pages/**`** —— manifest（`menuItems` + `routes`）用的是这一套。
- **树 B：`src/pages/**`** —— 主入口（`src/index.js`）的具名页面导出用的是这一套，
  z-team 的 `SystemShell` / z-opc 主壳引的也是这一套。

两棵树里大部分页面**同名同功能**（例如 `ak/index.jsx`、`llm/model/index.jsx`），
但它们是**两份独立文件**，改其中一份**不会**同步到另一份。

### 1.1 树 A —— manifest 面（`/z-agent/*`，`src/pages-manifest.jsx`）

| 页面 | 路由（相对 `/z-agent/*`） | 源文件（树 A） | 能力 |
|------|--------------------------|----------------|------|
| 首页 | `home` | `pages/HomePage.jsx` | 读 `userInfo` 显示登录人，按 `menuItems` 渲染功能入口卡片 |
| 应用总览 | `overview` | `agent/pages/agent/index.jsx` | Agent 应用总览 |
| 运行看板 | `agent/dashboard` | `agent/pages/agent/dashboard/index.jsx` | 运行指标看板 |
| 应用编辑器 | `agent/editor` | `agent/pages/agent/editor/AgentAppEditor.jsx` | 应用编排编辑器（@logicflow） |
| 分享页 | `agent/share` | `agent/pages/agent/share.jsx` | 应用分享与校验 |
| 应用实例 | `agent/app` | `agent/pages/agent/app/index.jsx` | 应用实例列表 |
| LLM 管理 | `llm` | `agent/pages/llm/index.jsx` | 模型与凭证入口 |
| 模型管理 | `llm/model` | `agent/pages/llm/model/index.jsx` | 模型管理 |
| 供应商 | `llm/provider` | `agent/pages/llm/provider/index.jsx` | 供应商管理 |
| AK 管理 | `ak` | `agent/pages/ak/index.jsx` | 访问密钥管理 |
| MCP 工具 | `mcp` | `agent/pages/mcp/index.jsx` | MCP server 与工具 |
| OSS 浏览 | `oss` | `agent/pages/oss/BucketList.jsx` | 存储桶清单 |
| 存储桶 | `oss/bucket` | `agent/pages/oss/BucketList.jsx` | 同上（两条路由指向同一个组件） |
| 桶内对象 | `oss/bucket/:bucketName` | `agent/pages/oss/ObjectBrowser.jsx` | 桶内对象浏览 |
| 产品管理 | `product` | `agent/pages/product/index.jsx` | 产品清单 |
| 场景编排 | `product/scene` | `agent/pages/product/scene.jsx` | 场景画布与编排 |
| 产品执行 | `product/execute` | `agent/pages/product/execute.jsx` | 产品执行 |
| 脚本 | `script` | `agent/pages/script/index.jsx` | 脚本中心 |
| 脚本产品化 | `script/productization` | `agent/pages/script/ScriptProductizationPage.jsx` | 脚本产品化 |
| 技能 | `skill` | `agent/pages/skill/index.jsx` | 技能市场 |
| 模板 | `template` | `agent/pages/template/index.jsx` | 模板市场 |
| 调用追踪 | `trace` | `agent/pages/trace/index.jsx` | 调用链追踪 |
| 用量统计 | `usage` | `agent/pages/usage/index.jsx` | 用量看板 |
| 登录 | （不在菜单内） | `pages/LoginPage.jsx` | mock 登录，写 `token` / `userInfo` 后跳 `/z-agent/home` |
| 组件文档 | `docs` | `pages/ComponentDoc.jsx` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

### 1.2 树 B —— 主入口具名导出面（`src/pages/**`）

主入口 `src/index.js` 导出的页面组件（z-team `SystemShell` 与 z-opc 主壳按名字取）：

| 导出名 | 源文件（树 B） | 说明 |
|--------|----------------|------|
| `AgentAppPage` | `pages/agent/index.jsx` | Agent 应用页 |
| `AgentAppList` | `pages/agent/app/index.jsx` | 应用实例列表 |
| `AgentAppEditor` | `pages/agent/editor/AgentAppEditor.jsx` | 应用编排编辑器 |
| `AgentSharePage` | `pages/agent/share.jsx` | 分享页 |
| `WorkflowEditor` | `pages/agent/editor/WorkflowEditor.jsx` | 工作流编辑器（组件） |
| `AgentDashboard` | `pages/agent/dashboard/index.jsx` | 运行看板 |
| `AgentInstanceManager` | `pages/ai/agent-instances/index.jsx` | 多 Agent 实例管理（**只在树 B 有**） |
| `ModelManage` | `pages/llm/index.jsx` | 模型管理 |
| `LlmManage` | `pages/llm/LlmManage.jsx` | 模型与凭证合并单页入口（**只在树 B 有**） |
| `LlmProvider` / `LlmModel` | `pages/llm/{provider,model}/index.jsx` | 供应商 / 模型 |
| `AkManage` / `AkUsageDrawer` | `pages/ak/{index,AkUsageDrawer}.jsx` | AK 管理 / 用量抽屉 |
| `McpPage` | `pages/mcp/index.jsx` | MCP 工具 |
| `BucketList` / `ObjectBrowser` | `pages/oss/{BucketList,ObjectBrowser}.jsx` | 存储桶 / 对象浏览 |
| `ScriptCenterPage` | `pages/script/index.jsx` | 脚本中心 |
| `ScriptProductizationPage` | `pages/script/ScriptProductizationPage.jsx` | 脚本产品化 |
| `ApiBridgeEditor` / `CurlImportModal` / `OpenApiImportModal` / `FieldMappingEditor` | `pages/script/*.jsx` | 脚本导入与映射编辑（组件） |
| `SkillMarket` | `pages/skill/index.jsx` | 技能市场 |
| `TemplateMarket` | `pages/template/index.jsx` | 模板市场 |
| `ProductList` / `ProductScene` / `ProductExecute` | `pages/product/*.jsx` | 产品 / 场景 / 执行 |
| `UsageDashboard` | `pages/usage/index.jsx` | 用量看板 |

**两棵树的不对称**（除上表标注外）：
- `agent/pages/trace/**`（调用追踪）**只在树 A 有**，树 B 没有对应导出；
- `pages/ai/agent-instances/**`、`pages/llm/LlmManage.jsx` **只在树 B 有**，树 A 没有。

## 二、导出面

```js
// 主入口（. 桶）：请求层 + 树 B 的具名页面导出
import {configureAgent, agentApi, flowApi, statsApi, pluginApi, memoryApi, triggerApi,
        templateApi, textGenApi, humanInputApi, llmApi, mcpApi, scriptApi, skillApi,
        productApi, sceneApi, ossApi, authRequest, ctcRequest, request,
        AgentAppPage, AgentAppList, AgentDashboard, LlmManage, McpPage, ObjectBrowser,
        ScriptCenterPage, ScriptProductizationPage, SkillMarket, BucketList,
        ComponentDocPage} from '@yuku123/z-agent-component'

// 宿主组装面（./pages 桶）：manifest
import {menuItems, routes, appMeta, HomePage, LoginPage, ComponentDocPage} from '@yuku123/z-agent-component/pages'
```

⚠ **构建入口不是 `src/pages.js`**：这个包的 vite `lib.entry` 是
`{ 'z-agent-component': './src/index.js', 'z-agent-pages': './src/pages-manifest.jsx' }`，
`package.json` 的 `exports['./pages']` 指向 `./dist/z-agent-pages.js`
⇒ **`./pages` 桶的内容就是 manifest 本身**（不是 `src/pages.js`）。
`src/pages.js` 这个文件在包里是**死代码**，没有进任何构建产物。
⇒ 文档页只要在 manifest 里导出，就会自动出现在 `./pages` 桶里。

## 三、API 端点

请求实例由基座 `createRequest` 工厂创建（`src/agent/api/request.js` 与 `src/api/request.js`
各一份，内容一致），**`baseURL` 默认 `/api`**，超时 15s。
路由表在 `src/agent/api/routes.js`（树 A）与 `src/api/routes.js`（树 B），
用 `reg(命名空间, 方法名, HTTP 方法, 路径)` 注册；**未注册的调用会抛 `__missing_route`**。

下表路径是 `src/agent/api/routes.js` 里**实际注册**的路径（前缀 `/api` 由 baseURL 提供）。

### 3.1 Agent 应用 / 工作流 / 监控 / 插件 / 触发器 / 模板 / 文本生成 / 人工干预

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/agent/app/page` | 应用分页 |
| GET | `/agent/app/get` | 应用详情 |
| POST | `/agent/app` | 新建应用 |
| POST | `/agent/app/update` | 更新应用 |
| POST | `/agent/app/delete` | 删除应用 |
| POST | `/agent/app/publish` | 发布应用 |
| POST | `/agent/app/upgrade` | 升级应用 |
| POST | `/agent/app/toggleShare` | 切换分享 |
| GET | `/agent/app/versions` | 版本列表 |
| GET | `/agent/group/tree` | 分组树 |
| POST | `/agent/group` | 新建分组 |
| POST | `/agent/group/update` | 更新分组 |
| POST | `/agent/group/delete` | 删除分组 |
| GET | `/agent/chat/history` | 会话历史 |
| POST | `/agent/chat/send` | 发送消息（流式） |
| POST | `/agent/chat/clear` | 清空会话 |
| GET | `/agent/share/verify` | 分享校验 |
| GET | `/agent/flow/list` | 工作流列表 |
| GET | `/agent/flow/byId` | 工作流详情 |
| GET | `/agent/flow/byFlowId` | 按 flowId 取工作流 |
| GET | `/agent/flow/versions` | 工作流版本 |
| POST | `/agent/flow/create` | 新建工作流 |
| POST | `/agent/flow/update` | 更新工作流 |
| POST | `/agent/flow/publish` | 发布工作流 |
| POST | `/agent/flow/archive` | 归档工作流 |
| POST | `/agent/flow/newVersion` | 新建版本 |
| POST | `/agent/flow/delete` | 删除工作流 |
| POST | `/agent/flow/execute` | 执行工作流 |
| GET | `/agent/flow/health` | 工作流健康 |
| GET | `/agent/stats/overview` | 监控总览 |
| GET | `/agent/stats/trend` | 趋势 |
| GET | `/agent/stats/latency` | 时延 |
| GET | `/agent/stats/by-app` | 按应用 |
| GET | `/agent/stats/by-user` | 按用户 |
| GET | `/agent/stats/health` | 监控健康 |
| GET | `/agent/plugin/page` | 插件分页 |
| GET | `/agent/plugin/byId` | 插件详情 |
| GET | `/agent/plugin/byCode` | 按 code 取插件 |
| POST | `/agent/plugin/create` | 新建插件 |
| POST | `/agent/plugin/update` | 更新插件 |
| POST | `/agent/plugin/status` | 插件状态 |
| POST | `/agent/plugin/delete` | 删除插件 |
| POST | `/agent/plugin/execute` | 执行插件 |
| GET | `/agent/plugin/builtin-beans` | 内置 bean 清单 |
| GET | `/agent/plugin/health` | 插件健康 |
| GET | `/agent/trigger/page` | 触发器分页 |
| GET | `/agent/trigger/byId` | 触发器详情 |
| POST | `/agent/trigger/create` | 新建触发器 |
| POST | `/agent/trigger/update` | 更新触发器 |
| POST | `/agent/trigger/status` | 触发器状态 |
| POST | `/agent/trigger/delete` | 删除触发器 |
| POST | `/agent/trigger/fire` | 手动触发 |
| GET | `/agent/trigger/health` | 触发器健康 |
| GET | `/agent/template/page` | 模板分页 |
| GET | `/agent/template/byId` | 模板详情 |
| GET | `/agent/template/hot` | 热门模板 |
| POST | `/agent/template/create` | 新建模板 |
| POST | `/agent/template/update` | 更新模板 |
| POST | `/agent/template/status` | 模板状态 |
| POST | `/agent/template/delete` | 删除模板 |
| POST | `/agent/template/fork` | 复刻模板 |
| GET | `/agent/template/health` | 模板健康 |
| GET | `/agent/textgen/page` | 文本生成器分页 |
| GET | `/agent/textgen/byId` | 文本生成器详情 |
| GET | `/agent/textgen/results` | 生成结果 |
| POST | `/agent/textgen/create` | 新建 |
| POST | `/agent/textgen/delete` | 删除 |
| POST | `/agent/textgen/run` | 运行 |
| POST | `/agent/textgen/createAndRun` | 建并运行 |
| GET | `/agent/textgen/health` | 健康 |
| GET | `/agent/human/inbox` | 人工干预收件箱 |
| GET | `/agent/human/execution` | 按执行取 |
| GET | `/agent/human/byId` | 按 id 取 |
| GET | `/agent/human/by-code` | 按 code 取 |
| POST | `/agent/human/decide` | 决策 |
| POST | `/agent/human/create` | 新建 |
| GET | `/agent/human/health` | 健康 |

### 3.2 脚本 / 技能 / MCP / LLM 工具模板

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/script/list` | 脚本列表 |
| POST | `/script` | 新建脚本 |
| POST | `/script/update` | 更新脚本 |
| POST | `/script/delete` | 删除脚本 |
| POST | `/script/publish` | 发布脚本 |
| POST | `/script/unpublish` | 下架脚本 |
| POST | `/script/run` | 运行脚本 |
| POST | `/script/import-curl` | curl 导入 |
| POST | `/script/import-openapi` | OpenAPI 导入 |
| POST | `/script/preview-mapping` | 字段映射预览 |
| GET | `/skill/search` | 技能搜索 |
| GET | `/skill/categories` | 技能分类 |
| GET | `/skill/sources` | 技能来源 |
| GET | `/skill/stats` | 技能统计 |
| GET | `/skill/installed` | 已安装技能 |
| GET | `/skill/detail` | 技能详情 |
| GET | `/skill/content` | 技能内容 |
| POST | `/skill/install` | 安装技能 |
| DELETE | `/skill/uninstall` | 卸载技能 |
| POST | `/skill/refresh` | 刷新技能源 |
| GET | `/mcp/server/list` | MCP server 清单 |
| POST | `/mcp/server/tools/list` | server 的工具清单 |
| POST | `/mcp/server/tools/call` | 调用工具 |
| GET | `/z-llm/config/tool-template/list` | LLM 工具模板目录 |

### 3.3 产品 / 场景 / OSS

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/v1/products` | 产品分页 |
| POST | `/v1/products` | 新建产品 |
| PUT | `/v1/products` | 更新产品 |
| DELETE | `/v1/products` | 删除产品 |
| POST | `/v1/products/publish` | 发布产品 |
| POST | `/v1/products/offline` | 下架产品 |
| GET | `/v1/products/config` | 产品配置 |
| PUT | `/v1/products/config` | 保存产品配置 |
| GET | `/v1/scenes` | 场景分页 |
| POST | `/v1/scenes` | 新建场景 |
| PUT | `/v1/scenes` | 更新场景 |
| DELETE | `/v1/scenes` | 删除场景 |
| POST | `/v1/scenes/duplicate` | 复制场景 |
| POST | `/v1/scenes/publish` | 发布场景 |
| POST | `/v1/scenes/offline` | 下架场景 |
| POST | `/v1/scenes/execute` | 执行场景 |
| GET | `/v1/scenes/canvas` | 场景画布 |
| GET | `/v1/scenes/config` | 场景配置 |
| GET | `/v1/scenes/nodes` | 场景节点 |
| GET | `/v1/scenes/chat-history` | 场景会话历史 |
| GET | `/v1/bucket` | 存储桶列表 |
| POST | `/v1/bucket` | 创建桶 |
| DELETE | `/v1/bucket` | 删除桶 |
| PUT | `/v1/bucket/acl` | 设置桶 ACL |
| GET | `/v1/bucket/stats` | 桶统计 |
| GET | `/v1/object` | 对象列表 |
| POST | `/v1/object` | 上传对象 |
| DELETE | `/v1/object` | 删除对象 |
| POST | `/v1/object/copy` | 复制对象 |
| POST | `/v1/object/batch-delete` | 批量删除对象 |
| GET | `/v1/object/url` | 预签名 URL |
| POST | `/v1/folder` | 创建目录 |

### 3.4 绕过路由表的直连调用

页面里还有一批**不走路由表**、直接用 umi 风格 `request(url, {method, params|data})` 打的路径。
它们**没有 `__missing_route` 保护** —— 注册缺失时请求会原样打到后端并拿 404 / 业务错误。

| 方法 | 路径 | 用在哪个页面 |
|---|---|---|
| GET | `/ak/list` | AK 管理（query：`akType`） |
| POST | `/ak/create` | AK 管理 |
| POST | `/ak/update` | AK 管理 |
| POST | `/ak/delete` | AK 管理 |
| POST | `/ak/toggle` | AK 管理（query：`id` / `status`） |
| GET | `/llm-center/model/list` | 模型管理 |
| POST | `/llm-center/model/delete` | 模型管理 |
| GET | `/llm-center/provider/list` | 供应商 |
| POST | `/llm-center/provider/delete` | 供应商 |
| GET | `/llm-gateway/usage/overview` | 用量统计 |
| GET | `/llm-gateway/usage/trend` | 用量统计 |
| GET | `/llm-gateway/usage/records` | 用量统计 |
| GET | `/llm-gateway/usage/by-app` | 用量统计 |
| GET | `/llm-gateway/usage/by-user` | 用量统计 |

⚠ `/llm-gateway/usage/*` 这一组与路由表注释里说的「llm-gateway 前缀在本 JVM 一条都没有」
是同一件事：路由表把工具模板改接到了 `/api/z-llm/config/**`，但用量页仍在打
`/api/llm-gateway/usage/*` ⇒ 本 JVM 上这五个接口预期是 404。

## 四、页面说明

### 4.1 首页 / 登录

首页读 `localStorage.userInfo` 显示欢迎语与角色标签，再把 `menuItems`（去掉首页自身）
渲染成入口卡片 —— 菜单加一项，首页卡片自动多一张，不用改页面代码。
登录页当前是 mock（比对写死的管理员凭据，明文口令不在本文复述，见源码）。

### 4.2 Agent 应用族（总览 / 看板 / 编辑器 / 分享 / 实例）

应用是这一域的主线：应用记录里的 `tools` / `skill_codes` / `knowledge_ids` 三列
决定它绑了哪些工具、技能与知识库（**不走 LLM 工具模板接口**）。
编辑器用 @logicflow/core 画编排图。

### 4.3 LLM 与凭证

`llm` 是模型与凭证的合并入口；`llm/model` 管模型，`llm/provider` 管供应商；
`ak` 管访问密钥（带用量抽屉）。工具模板目录读 `/z-llm/config/tool-template/list`。

### 4.4 MCP 工具

只读三格：server 清单 / server 的工具清单 / 调用工具。
⚠ MCP 的写端点（create / update / delete / test）随 z-agent-mcp 一族退场，**别再往回挂**。

### 4.5 OSS / 产品与场景 / 脚本 / 技能 / 模板 / 追踪 / 用量

对象存储浏览（桶 → 对象）、产品与场景编排（`/v1/products` + `/v1/scenes`）、
脚本中心（含 curl / OpenAPI 导入与字段映射预览）、技能市场、模板市场、
调用追踪与用量看板。

### 4.6 组件文档

本文件。内容以 `?raw` 在编译期内联，零额外请求；左侧 240px TOC 随滚动联动。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `configureAgent({apiBase})` | `'/api'` | 同时改写 `request` / `authRequest` / `ctcRequest` 三个实例的 `baseURL` |
| 请求超时 | `15000` ms | 三个实例都是 |
| 路由表 | `src/agent/api/routes.js` + `src/api/routes.js` | 两份内容几乎相同（树 B 的多注册 3 条 `/mcp/admin/*`） |
| `window.__Z_GRAPH_API__` 之类环境注入 | — | 本包**不**用全局注入，只认 `configureAgent` |

## 六、已知限制

- ⚠ **两套页面树并存**：manifest 用 `src/agent/pages/**`，主入口导出用 `src/pages/**`。
  同名页面是两份独立文件，改一份不影响另一份。`trace`（调用追踪）只在树 A，
  `AgentInstanceManager` 与 `LlmManage` 只在树 B。
  ⇒ 改页面前先确认改的是哪一棵。
- ⚠ **`src/pages.js` 是死代码**：vite 的 `lib.entry` 是 `src/index.js` 与
  `src/pages-manifest.jsx`，`exports['./pages']` 指向 `./dist/z-agent-pages.js`
  ⇒ `./pages` 桶 = manifest 本身。`src/pages.js` 没进任何产物。
- ⚠ **主壳对 `ScriptCenterPage` 做了主壳级覆盖**：z-opc 主壳的
  `src/shims/z-agent-component/index.js` 把 `ScriptCenterPage` 换成主壳自己的只读页
  （数据源 `/api/script-admin/scripts`，绕开 AK 鉴权关）。
  ⇒ 在主壳里看到的脚本清单**不是**本包这页。覆盖只发生在主壳侧，本包源码未改。
- **脚本接口受 AK 鉴权关**：`/api/script/**` 被 `z.script.api-key.path-patterns` 钉在 AK 关里，
  直连本包的页面会拿到 `401 {"errorCode":"MISSING_API_KEY"}`。
- **LLM 工具模板**：旧路由表里的 `/llm-gateway/**` 四条全是 404（实测 :8888 的
  `/actuator/mappings` 里 llm-gateway 前缀一条都没有），真实前缀是 `/api/z-llm/config/**`。
  技能目录 / 知识库目录**本 JVM 无对应端点** ⇒ 不注册，调用即抛 `__missing_route`。
- **未注册的 API 调用会抛 `__missing_route`**（不是 404），页面要显式写
  「本 JVM 未装配 xxx」，不能显示成「暂无可用项」。
- **`memoryApi` 在导出列表里但路由表里没有注册**：调用它必然抛 `__missing_route`。
- **应用绑定关系不走 LLM 工具模板接口**：由 `agentApi.appGet` / `appUpdate` 读写。
- echarts 与 @logicflow/core 走 external（`id.startsWith('echarts/')` 等前缀判定），
  由宿主用自己的 node_modules 解析 ⇒ 本包 dist 里没有 echarts，宿主必须自己装。
- z-agent 已被 z-opc 主壳的 `domainRoutes.ts`、z-team 的 `SystemShell` 与
  `SmartWorkbench` 引用 ⇒ `/z-agent/docs` 在平台主壳里**可达**。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.5 | 新增组件文档页 `ComponentDocPage`（lead 008 §7）：`src/DOC.md` + 基座 `ComponentDoc` 薄包装 |
| 0.1.5 | 首版：Agent / LLM / MCP / OSS / 产品场景 / 脚本 / 技能 / 模板 / 追踪 / 用量页面集 + 双页面树 + 双路由表 |
