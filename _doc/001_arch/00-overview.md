# 项目总览

## 1. 项目简介

z-agent 是 z-opc-foundation 里的**云端 Agent 平台仓**：把「一个 agent 怎么被定义、被路由、被调用、被注册、被管理」这五件事收口到一组 Maven 库里，用 Spring Boot starter 的形式交付给上层平台（z-opc / z-team / z-bot 等）装配。

它不是一个可独立部署的应用。仓内唯一的 `@SpringBootApplication` 是 `z-agent-center/z-agent-center-starter` 里的 `Main`，其余模块都是库件；`z-agent-starter` 提供的 `ZAgentAutoConfiguration` 靠 `@ConditionalOnProperty("z.agent.enabled")` 显式打开，默认不装，宿主应用自己提供 `LlmProvider` bean 并把 agent 注册进内存注册表。

**技术栈：** Java 8 · Spring Boot 2.7.18 · Spring MVC · Jackson · MyBatis-Plus（center 侧）· Maven（CI-friendly `${revision}` + flatten-maven-plugin `flattenMode=oss`）

## 2. 模块架构

```
z-agent/                                # 聚合根：7 个 <module>，<revision>1.0.1</revision>
├── z-agent-api/                        # 零依赖 DTO + 异常（AgentDto / RunRequest / RunResponse / StreamChunk / TokenUsageDto / AgentException）
├── z-agent-core/                       # kernel-SPI 版默认实现：ReAct / Plan 推理循环、LlmAdapter 路由、AgentRegistry、AgentRunner、REST 网关
├── z-agent-starter/                    # Spring Boot 自动装配（ZAgentAutoConfiguration + META-INF/spring.factories）
├── z-agent-admin/                      # 只读控制面（AdminController / AdminQueryService + 静态 index.html）
├── z-agent-common/                     # 迁入的公共实体：AgentApp / AgentAppVersion / AgentType / AgentAppService
├── z-agent-engine/                     # 迁入的独立 agent 运行时：自带 react / plan / multi / observe / stream / skill 栈 + LLM 网关 + 内置扩展
└── z-agent-center/                     # 迁入的业务控制台（三子模块）
    ├── z-agent-center-core/            # 领域层：app / bot / skill / workflow + agent 的 flow / executor / plugin / instance / memory / humaninput …
    ├── z-agent-center-web/             # REST 层：18 个 Controller，路径前缀 /api/agent/** 等
    └── z-agent-center-starter/         # 可执行入口 Main（唯一 @SpringBootApplication）
```

仓内另有三块非 Maven 内容：`_frontend/`（`z-agent-suit` 管理台壳 + `z-agent-component` 组件包）、`skills/`（31 个 JSON 格式的技能定义）、`_sql-migrated/`（10 个从 z-opc 迁出的 SQL 文件）。

## 3. 核心功能

### 3.1 两套并存的 agent 推理栈

- **`z-agent-core`（kernel-SPI 版）**：`ReActAgent` 走 `think → tool_calls → tool_result` 迭代，上限取 `z.agent.default-max-steps`；`PlanAgent` 先出编号计划再逐步执行、末尾综合。`LlmAdapter.resolve()` 遍历已注册 provider 的 `supportsModel`，无人支持即抛 `AgentException`。
- **`z-agent-engine`（自带栈）**：`com.zifang.z.agent.engine.agent.react.ReActAgent` + `agent.plan.PlannedReActAgent`/`LlmPlanner` + `agent.multi.SupervisorAgent`/`WorkerAgent` + `agent.observe.AgentTracer` + `agent.stream.StreamingReActAgent`；另一路是 `com.zifang.z.agent.llm.agent.runtime.ReactAgent`（`AgentBuilder` / `TurnRun` / `AgentStream` 一组），两套并存，靠包名区分。

### 3.2 工具、会话与韧性

- `z-agent-engine` 的 `agent.tool.resilience` 提供 `RetryPolicy` / `CircuitBreaker` / `ResilientToolExecutor`，把工具调用的重试与熔断收在一处。
- `z-agent-engine` 的 `agent.skill`（`Skill` / `SkillLoader` / `ExecutionPlan`）与 `llm.builtins.extensions`（`CompactionExtension` 上下文压缩、`SessionStateStore` 会话归档、`SkillsExtension` 技能目录）构成内置扩展面。
- `z-agent-core` 的 `AgentRegistry` 是 `ConcurrentHashMap` 内存版注册中心，提供 `register/unregister/get/describe/list/names/has/size`。

### 3.3 业务控制台（z-agent-center）

- **流程编排**：`center.core.agent.flow` 提供 `FlowDefinition` / `FlowEngine` / `FlowNode` 与 `AgentNode` / `ConditionNode` / `HttpNode` / `LLMNode` / `ParallelNode` / `TemplateNode` 六种节点，配套 `FlowTraceRecorder` 与三张表（定义 / 执行 / 节点执行）。
- **执行器**：`center.core.agent.executor` 下有 `InProcessAgentExecutor` / `ContainerAgentExecutor` / `SandboxExecutor` / `OpenCodeExecutor` / `ClaudeCodeExecutor` / `HermesLocalExecutor` / `HermesProxyExecutor` / `LocalProcessProxyExecutor`。
- **插件与人工介入**：`center.core.agent.plugin` 提供 `HttpPluginExecutor` / `BeanPluginExecutor` 与四个内置插件（`CalculatorPluginExecutor` / `CurrentTimePluginExecutor` / `EchoPluginExecutor` / `RandomNumberPluginExecutor`）；`center.core.agent.humaninput` 提供人工节点与收件箱。
- **数据面**：`center.core.agent.instance` / `memory` / `share` / `stats` / `widget` / `template` / `textgen` / `trigger` / `skill` / `conversation` 各带 entity + mapper + service 三件套。
- **REST 面**：`z-agent-center-web` 下 18 个 `@RestController`，类级前缀含 `/api/agent/app`、`/api/agent/group`、`/api/agent/instance`、`/api/agent/flow`、`/api/agent/chat`、`/api/agent/plugin`、`/api/agent/skill`、`/api/agent/human`、`/api/agent/widget`、`/api/agent/memory`、`/api/agent/share`、`/api/agent/template`、`/api/agent/textgen`、`/api/agent/trigger`、`/api/agent/stats`、`/api/ak`、`/api/bot/feedback`、`/api/knowledge-search`。

### 3.4 对外网关与控制面

- `z-agent-core` 的 `AgentController` 暴露 `/agent/list`、`/agent/{name}`、`/agent/{name}/run`、`/agent/{name}/stream`；`z-agent-admin` 的 `AdminController` 暴露 `/z-agent/admin/overview`、`/z-agent/admin/agents`、`/z-agent/admin/models`，并由 `z-agent-admin/src/main/resources/index.html` 提供纯静态控制台页面。
- `z-agent-core` 的 `AgentProperties`（`@ConfigurationProperties("z.agent")`）暴露 `enabled` / `default-model` / `default-max-steps` / `default-temperature` / `default-top-p` / `default-max-tokens` 等配置键。

### 3.5 前端配套

- `_frontend/z-agent-suit`：React 19 + antd 6 + Vite 6 的管理台壳，用 `@yuku123/z-frontend-common` 的 `AppLayout` + 登录页，dev 端口 3022，`/api` 与 `/actuator` 代理到 `http://localhost:8888`。
- `_frontend/z-agent-component`：独立发布的组件包 `@yuku123/z-agent-component`（0.1.1），构建产物为 `dist/z-agent-component.js` + `dist/z-agent-pages.js` + `dist/z-agent-component.css`。

## 4. 对外集成 / 上下游

**本仓不自带 HTTP 端口。** 仓内没有任何 `application*.yml` / `application*.properties`（`find` 实测为空），因此 `server.port` 与 context-path 全部由部署侧下发；`z-agent-center-starter` 虽然是可执行入口，端口也不在仓内固定。

**上游依赖（坐标取自各模块 `pom.xml`）：**

| 依赖 | 出现在 | 作用 |
|---|---|---|
| `z-agent-kernel-{agent,llm,message,tool,types,memory,event,middleware}` `0.2.3` | 根 POM `dependencyManagement` + `z-agent-core` | kernel SPI：`Agent` / `LlmProvider` / `Msg` / `Tool` 等契约 |
| `z-util-core` / `z-util-parser-json` / `z-util-dsl-kernel` `1.2.4` | 根 POM | 内部工具库（JSON 解析等） |
| `z-agent-kernel-tool` | `z-agent-engine` | 引擎侧工具契约 |
| `z-boot-base` / `z-boot-datasource-starter` / `z-boot-web-starter` | engine / center 子模块 | 内部 Boot 地板与数据源 |
| `okhttp` / `okhttp-sse` / `okio` / `commons-lang3` | `z-agent-engine` | LLM 网关的 HTTP 与 SSE 传输 |
| `langchain4j-core` / `reactor-core` / `protobuf-java` / `grpc-netty-shaded` | `z-agent-engine` | 引擎的响应式与 gRPC 依赖面 |
| `spring-boot-starter-web` / `spring-boot-starter-jdbc` | `z-agent-engine` | Web 与数据访问 |
| `spring-web` / `spring-webmvc` / `spring-boot` / `jackson-databind` / `slf4j-api` | `z-agent-core` | REST 网关与序列化 |

**下游消费方：** 宿主 Spring Boot 应用引 `io.github.yuku123:z-agent-starter` 后显式打开 `z.agent.enabled=true`、自备 `LlmProvider` bean、自行把 agent 注册进 `AgentRegistry`；`z-agent-center-starter` 是可执行 fat jar（`spring-boot-maven-plugin` 的 `classifier=boot`）。

**发布边界：** 根 POM 的 `central` profile 用 `central-publishing-maven-plugin` 0.8.0（`autoPublish=true`）发布，`z-agent-admin` 被 `excludeArtifacts` 排除在中央发布之外（它是可执行应用而非库件）。仓内另设 `maven-central` 仓库 id（原注释：迁入模块的本地构件记着 `from maven-central`，id 不符会导致离线解析失败）。

## 5. 技术特点

1. **两套 agent 栈并存**：迁入的 `z-agent-engine` 带来 `engine.agent.react.ReActAgent` / `llm.agent.runtime.ReactAgent`，与 `z-agent-core` 的 kernel-SPI 版 `ReActAgent` 同名不同包；两者由 `z-agent-center-starter` 的 `Main` 一次 `@ComponentScan` 同时扫到（`com.zifang.z.agent.center` + `com.zifang.z.agent.engine`）。
2. **kernel 版本线已收敛为一条**：根 POM 的 `z-agent-kernel.version=0.2.3`，并对 8 个 kernel 坐标按坐标逐条写直接 DM 条目，避免继承来的 fleet 版本改写传递依赖。
3. **装配默认关闭**：`ZAgentAutoConfiguration` 带 `@ConditionalOnProperty(name = "z.agent.enabled", havingValue = "true", matchIfMissing = false)`，防止与 z-opc 内部遗留的 `z-agent-center-*` 撞 bean。
4. **starter 不装 provider**：装配只把容器里**已有**的 `LlmProvider` 收进 `LlmAdapter`（`ObjectProvider` 迭代 + `registerProvider`）；kernel 的 provider 是纯 POJO、无 Spring 注解，所以本仓不会替宿主造出任何 provider。
5. **`z-agent-api` 真·零依赖**：`z-agent-api/pom.xml` 里没有任何 `<dependencies>` 段，DTO 与 `AgentException` 不拖 Spring，可被任何宿主直接引用。
6. **两套前端形态并存**：`_frontend/z-agent-suit` 是不发布的独立壳（`"private": true`），`_frontend/z-agent-component` 是发布的组件包（0.1.1），两者用 `LOCAL_SIBLINGS=1` 走 `alias` 做本地联调。
7. **可执行入口只有一个**：`z-agent-center-starter` 的 `Main` 是全仓唯一的 `@SpringBootApplication`；`z-agent-starter` 的 `ZAgentAutoConfiguration` 是 `@EnableAutoConfiguration` 注册的装配类，不是应用。

## 6. 目录结构

源码按 Java 包树组织，根包 `com.zifang.z.agent` 下分两片：

```
z-agent/
├── z-agent-api/src/main/java/com/zifang/z/agent/api/
│   ├── dto/            # AgentDto / RunRequest / RunResponse / StreamChunk / TokenUsageDto
│   └── exception/      # AgentException（错误码常量）
├── z-agent-core/src/main/java/com/zifang/z/agent/core/
│   ├── agent/          # LlmAdapter / ReActAgent / PlanAgent
│   ├── controller/     # AgentController（/agent/**）
│   ├── properties/     # AgentProperties（z.agent.*）
│   ├── registry/       # AgentRegistry
│   └── runner/         # AgentRunner
├── z-agent-starter/src/main/java/com/zifang/z/agent/starter/autoconfig/
│   └── ZAgentAutoConfiguration.java
├── z-agent-admin/src/main/
│   ├── java/com/zifang/z/agent/admin/{controller,service}/
│   └── resources/index.html
├── z-agent-common/src/main/java/com/zifang/z/agent/common/
│   ├── entity/         # AgentApp / AgentAppVersion
│   ├── enums/          # AgentType
│   └── service/        # AgentAppService
├── z-agent-engine/src/main/java/com/zifang/z/agent/
│   ├── engine/agent/   # guardrails llm mcp memory multi observe plan plugin react skill stream tool
│   └── llm/            # agent（agentunit/builtins/extensions/observability/runtime/turn）
│                       #   gateway（OpenAI 兼容 + SSE）
│                       #   infrastructure（events/llm/messaging/workspace）
├── z-agent-center/z-agent-center-core/src/main/java/com/zifang/z/agent/center/core/
│   ├── app/ bot/ skill/ workflow/          # 应用、bot、技能、工作流
│   └── agent/                              # conversation executor flow humaninput instance llm
│                                            #   memory plugin runtime share stats template
│                                            #   textgen tool trigger widget
├── z-agent-center/z-agent-center-web/src/main/
│   ├── java/com/zifang/z/agent/center/{config,web/api}/
│   └── resources/META-INF/spring/
└── z-agent-center/z-agent-center-starter/src/main/
    ├── java/com/zifang/z/agent/center/starter/Main.java
    └── resources/static/agent/app.html
```

规模实测：main 383 个 `.java`、test 26 个（`z-agent-center` 204/1、`z-agent-engine` 159/20、`z-agent-core` 7/5、`z-agent-api` 6/0、`z-agent-common` 4/0、`z-agent-admin` 2/0、`z-agent-starter` 1/0）。
