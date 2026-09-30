# z-agent

> 云端 Agent 平台的**库与 starter** —— ReAct / Plan 两种 agent 默认实现 + 内存版注册中心 + REST 网关 + 只读控制面

`z-agent-kernel` 只给抽象（`Agent` / `LlmProvider` / `Msg` / `Tool` 这些 SPI），一个业务应用要真正"跑一个 agent 并被 HTTP 调到"，
还缺实现与装配那一段：推理循环、模型→provider 路由、注册表、REST 网关、控制台。本仓补的就是这一段，
并以 Spring Boot starter 的形式交付。

> **本仓不产出可执行应用**：全仓没有 `@SpringBootApplication`、没有 `main()`、没有任何 `application*.yml`。
> 复算：`rg --no-ignore -n "SpringBootApplication|public static void main" --glob '*.java' .` ⇒ 只命中 javadoc 里一句；
> `find . -name 'application*.yml' -o -name 'application*.properties'` ⇒ 空。跑起来由宿主 boot 应用引 `z-agent-starter`（见「怎么接」）。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-agent`（`z-opc-foundation/z-agent`，分支 `main`） |
| **Maven 坐标** | `io.github.yuku123:z-agent:${revision}`（根 POM `packaging=pom` 聚合） |
| **当前版本** | `0.1.4`（根 POM `<revision>`；CI-friendly versions + flatten-maven-plugin `1.7.2`，`flattenMode=oss`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘） |
| **Maven Central** | 2026-09-30 ranged GET 实测全部 200/206：`z-agent` / `z-agent-api` / `z-agent-core` / `z-agent-starter` / `z-agent-admin` 五个坐标的 `0.1.0`、`0.1.1`、`0.1.2`、`0.1.3`、`0.1.4` 都在；`z-agent` 是 pom 聚合，**没有 jar**（`.jar` 路径 404）。`maven-metadata.xml`：`latest=release=0.1.4`，`lastUpdated=20260929054528` |
| **内核依赖** | `io.github.yuku123:z-agent-kernel-{agent,llm,message,tool,types,memory,event,middleware}:0.1.0` —— 根 POM 属性 `z-agent-kernel.version=0.1.0` + 8 条按坐标写的直接 DM 条目 |
| **默认端口** | 无。本仓不监听端口、不产出 boot jar；`server.port` 与 context-path 由宿主应用决定 |
| **运行口径** | Java 8 · Spring Boot 2.7.18 —— 两格都由父链下发，本仓 POM 里没有 `maven.compiler.*` / `spring-boot.version` 声明（复算：`rg -n "spring-boot.version|maven.compiler" pom.xml */pom.xml` ⇒ 只命中注释） |
| **最近更新** | 2026-09-30 |

---

## 🧭 分层：本仓有什么，内核有什么

| | 归属 | 内容 |
|---|---|---|
| SPI 与协议类型 | `z-agent-kernel`（**25 个子模块**，本仓只吃其中 8 个） | `kernel.agent.Agent/AgentRequest/AgentResponse`、`kernel.llm.LlmProvider/ChatCompletionsRequest/ChatCompletionsResponse/Model`、`kernel.message.Msg/ToolCall`、`kernel.tool.Tool/ToolResult`、`kernel.types.MessageRole/TokenUsage`、memory/event/middleware |
| 默认实现 + 装配 + HTTP 面 | **本仓** | `ReActAgent` / `PlanAgent` 推理循环、`LlmAdapter` 模型路由、`AgentRegistry` 内存注册中心、`AgentRunner` 门面、`AgentController` / `AdminController`、`ZAgentAutoConfiguration` |

三个必须知道的边界：

1. **本仓只依赖 8/25 个内核模块**。内核源码树里剩下的 17 个（state / pipeline / workspace / formatter / embedding /
   rag / tts / realtime / skill / mcp / credential / permission / exception / classifier / console / tui / app）本仓没有声明依赖。
2. **内核是纯 POJO，不带 Spring 注解**，所以 provider 不会被自动装配。复算：
   `rg --no-ignore -l "org.springframework" --glob '*.java' ../z-agent-kernel/z-agent-kernel-llm` ⇒ `0` 个文件。
   内核 `z-agent-kernel-llm` 现树里有 6 个 provider 类（`OpenAI`/`Anthropic`/`Gemini`/`DashScope` 直接实现 `LlmProvider`，
   `DeepSeek`/`Qwen` 继承 `OpenAIProvider`），要用的得自己 `@Bean` 出来（见「怎么接」）。
3. **版本分歧是刻意的**：本仓钉 `0.1.0`，而父链 `z-boot-parent:1.0.21` import 的 `z-boot-fleet:1.0.1` 里
   `z-agent-kernel.version` 槽位是 `0.2.1`（`0.1.1` 那一格属于 fleet `1.0.0`）。中央上 `z-agent-core:0.1.4` 的发布 POM
   把 8 条 kernel 直接依赖落成字面 `0.1.0`（复算：`curl -s https://repo1.maven.org/maven2/io/github/yuku123/z-agent-core/0.1.4/z-agent-core-0.1.4.pom`）。
   ⇒ 消费方若继承了 fleet 的 DM，Maven 会用 DM 覆盖传递版本、把 kernel 抬到 `0.2.1`，与本仓的编译基线不一致；跨版本联调前先对齐这一格。

---

## 🎯 能力清单

每一条都能对应到本仓的一个类：

| 能力 | 实现 | 说明 |
|------|------|------|
| ReAct 推理循环 | `core/agent/ReActAgent.java`（203 行） | `think → tool_calls → tool_result` 迭代，上限 `z.agent.default-max-steps`，超出置 `finishedReason="max_steps"`（`:161`）；可选 `EventListener` 工具回调 |
| Plan 两阶段推理 | `core/agent/PlanAgent.java`（191 行） | 先出编号计划（正则 `^\s*(\d+)[.\)、]\s*(.+)$`，支持中英文括号）再逐步执行、末尾综合；解析不出编号就把整段当一步 |
| 模型→provider 路由 | `core/agent/LlmAdapter.java`（134 行） | `resolve()` 遍历 `supportsModel`，无人支持即抛 `AgentException(RUN_FAILED, "no provider supports model: …")`；另管工具参数 JSON 解析、`ToolResult→Msg`、usage 累加 |
| 调用门面 | `core/runner/AgentRunner.java`（166 行） | `RunRequest → kernel AgentRequest`、`AgentResponse → RunResponse/StreamChunk` 回转，`steps`/`usage`/`durationMs` 都在这里成形 |
| Agent 注册中心 | `core/registry/AgentRegistry.java`（86 行） | `ConcurrentHashMap` 内存版：`register/unregister/get/describe/list/names/has/size`，未知名字抛 `NOT_FOUND` |
| REST 网关 | `core/controller/AgentController.java` | `/agent/list`、`/agent/{name}`、`/agent/{name}/run`、`/agent/{name}/stream` |
| 控制面只读查询 | `admin/controller/AdminController.java` + `admin/service/AdminQueryService.java` | `/z-agent/admin/overview`（`agentCount`/`agents`/`providerCount`/`models`/`status`）、`/agents`、`/models` |
| 控制台页面 | `z-agent-admin/src/main/resources/index.html` | 纯静态脚本，`fetch('/z-agent/admin/overview')` 渲染 agents/providers/models |
| Spring Boot 自动装配 | `starter/autoconfig/ZAgentAutoConfiguration.java` + `META-INF/spring.factories` | `EnableAutoConfiguration` 注册该类；开关 `z.agent.enabled`；产出 3 个 `@Bean` |
| 对外 DTO | `z-agent-api` | `AgentDto`（`kind` 缺省 `"react"`）、`RunRequest`、`RunResponse`、`StreamChunk`、`TokenUsageDto` + `AgentException`（6 个错误码常量）；POM **没有任何 `<dependencies>`**，真·零依赖 |

复算 bean 口径（**别用 `grep -c '@Bean'`**：那个文件里有 4 处 `@Bean` 文本，第 4 处是"不要重复 @Bean"那句注释；
取"注解后紧跟方法签名"才是 3）：

```bash
grep -A1 '@Bean' z-agent-starter/src/main/java/com/zifang/z/agent/starter/autoconfig/ZAgentAutoConfiguration.java \
  | grep -c 'public'      # ⇒ 3（agentRegistry / llmAdapter / agentRunner）
```

---

## 🏗️ 项目结构

```
z-agent/
├── pom.xml                    # 聚合根 POM：parent z-boot-parent:1.0.21、<revision>0.1.4</revision>、4 个 <module>、central profile
├── LICENSE                    # MIT（Copyright (c) 2026 z-opc-foundation）
├── README.md
├── z-agent-api/               # 零依赖 DTO + 异常（POM 无 <dependencies>）
│   └── src/main/java/com/zifang/z/agent/api/
│       ├── dto/               # AgentDto / RunRequest / RunResponse / StreamChunk / TokenUsageDto
│       └── exception/         # AgentException
├── z-agent-core/              # 全部逻辑与全部测试
│   ├── src/main/java/com/zifang/z/agent/core/
│   │   ├── agent/             # LlmAdapter / ReActAgent / PlanAgent
│   │   ├── controller/        # AgentController（/agent/**）
│   │   ├── properties/        # AgentProperties（@ConfigurationProperties("z.agent")）
│   │   ├── registry/          # AgentRegistry
│   │   └── runner/            # AgentRunner
│   └── src/test/java/…/core/  # AgentRegistryTest / LlmAdapterTest / PlanAgentTest / AgentEndToEndTest
├── z-agent-starter/           # 自动装配
│   └── src/main/
│       ├── java/…/starter/autoconfig/ZAgentAutoConfiguration.java
│       └── resources/META-INF/spring.factories
└── z-agent-admin/             # 控制面（REST + 静态页）
    └── src/main/
        ├── java/…/admin/{controller/AdminController,service/AdminQueryService}.java
        └── resources/index.html          # 该模块 resources 下只有这一个文件，没有 application*.yml
```

规模实测：main 16 个 `.java` / test 4 个，合计 2137 行。

```bash
find . -path '*/target' -prune -o -name '*.java' -print | grep -c '/src/main/'   # ⇒ 16
find . -path '*/target' -prune -o -name '*.java' -print | grep -c '/src/test/'    # ⇒ 4
```

本仓**没有** `_doc/`、`deploy/`、`k8s/`、`Dockerfile*`、`docker-compose*.yml`、`Makefile`、`_frontend/`（逐个 `ls -d` 实测均不存在），
因此这里不写部署章节，也不写「文档目录」章节 —— 按 `lead/008_组织规范/002_项目文档收口规范`，无 `_doc/` 的仓不开这一节。

四个模块都留在 reactor 里，且**全仓没有一处 `maven.deploy.skip`**（复算：`rg --no-ignore -n "maven.deploy.skip" .` ⇒ 空）
⇒ 与 `z-ctc-admin` 不同，`z-agent-admin` 也一起上 Maven Central（已实测 200）。

---

## 🔧 技术栈

| 层级 | 技术 | 出处 |
|------|------|------|
| 语言 / 运行时 | Java 8 | 父链 `z-boot-parent:1.0.21` 的 `pluginManagement` |
| 框架 | Spring Boot 2.7.18（`spring-boot` / `spring-boot-autoconfigure`） | 版本由 `z-boot-dependencies` 地板供给；中央发布 POM 实测 `2.7.18` |
| Web 层 | `spring-web` / `spring-webmvc` `5.3.39` | `z-agent-core:0.1.4` 发布 POM 实测 |
| 序列化 | `jackson-databind` `2.18.6` | 同上 |
| 日志门面 | `slf4j-api` `1.7.36` | 同上 |
| Agent 抽象 | `z-agent-kernel-*` `0.1.0`（8 个 artifact） | 根 POM `z-agent-kernel.version` |
| 测试 | JUnit 4（`junit:junit`，`z-agent-starter` 另加 `spring-boot-starter-test` + `junit-vintage-engine`，均 test scope） | 各模块 POM |
| 构建 | Maven + flatten-maven-plugin `1.7.2`（`flattenMode=oss`）+ maven-release-plugin `3.1.1`（`tagNameFormat=v@{project.version}`） | 根 POM |
| 发布 | `central` profile：`maven-source-plugin` 3.3.1 / `maven-javadoc-plugin` 3.11.2 / `maven-gpg-plugin` 3.2.7 / `central-publishing-maven-plugin` 0.8.0（`autoPublish=true`） | 根 POM `<profiles>` |

本仓不依赖 `z-util`，也不依赖任何数据库/ORM（`rg -n "z-util|mybatis" pom.xml */pom.xml` ⇒ 空）。

`flattenMode=oss` 是 2026-09-29 从 `resolveCiFriendliesOnly` 改的（commit `63d340b`），效果可回读验证：
中央 `z-agent-starter:0.1.3` 的发布 POM 里还有 `<parent>`，`0.1.4` 的没有 ⇒ 消费方不再需要顺着
`z-agent → z-boot-parent → z-boot-fleet` 才能补齐版本。

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

版本口径全部由父链下发；构建报找不到版本时，先确认能从 repo1 解析到
`io.github.yuku123:z-boot-parent:1.0.21` 与 `z-agent-kernel-*:0.1.0`（两者都已实测存在）。

### 怎么接（三步，缺一不可）

```xml
<dependency>
  <groupId>io.github.yuku123</groupId>
  <artifactId>z-agent-starter</artifactId>
  <version>0.1.4</version>
</dependency>
```

**第一步：显式打开装配。** 装配类上挂的是
`@ConditionalOnProperty(name = "z.agent.enabled", havingValue = "true", matchIfMissing = false)`，缺省不装。
这条是刻意加的，防止与 z-opc 内部遗留的 `z-agent-center-*` 撞 bean（出处 `ZAgentAutoConfiguration` 类注释，FEATURE066 2026-09-23）。

```properties
z.agent.enabled=true
```

**第二步：自己提供 `LlmProvider` bean。** 装配只是把容器里**已有**的 `LlmProvider` 收进 `LlmAdapter`
（`ObjectProvider<LlmProvider>` 迭代 + `registerProvider`），内核 provider 是 POJO、没有 Spring 注解，
所以 starter 不会替你装任何 provider。provider 数为 0 时，任何 `POST /agent/{name}/run` 都会以
`no provider supports model: …` 失败：

```java
@Bean
public LlmProvider openAiProvider() {
    return new OpenAIProvider();   // com.zifang.z.agent.kernel.llm.provider
}
```

**第三步：自己把 agent 注册进注册中心。** 全仓生产代码里没有任何 `registry.register(...)` 调用，
也没有按 `AgentDto.kind` 分流的工厂（复算：`rg --no-ignore -n "\.register\(|new ReActAgent|new PlanAgent" --glob '*.java' */src/main` ⇒ 空，
只有测试在调）。`kind` 目前只是展示用元数据：

```java
@Bean
public ApplicationRunner registerAgents(AgentRegistry registry, AgentProperties props, LlmAdapter llm) {
    return args -> registry.register(
            "researcher",
            AgentDto.builder().name("researcher").kind("react").model("gpt-4o-mini").build(),
            new ReActAgent(dto, props, llm, tools));   // tools 在构造期绑定
}
```

### 配置键（`z.agent.*`，取自 `core/properties/AgentProperties.java`）

字段是驼峰，配置文件写 kebab 或驼峰都认（Spring relaxed binding）；下表给 kebab 写法。
"生效"列按现读代码判定，不看注释。

| 键 | 缺省值 | 实际读取点 | 生效 |
|---|---|---|---|
| `z.agent.enabled` | 无（必须显式 `true`） | `ZAgentAutoConfiguration` 类注解 | 是（总开关） |
| `z.agent.default-model` | `gpt-4o-mini` | `ReActAgent:175` / `PlanAgent:171` / `LlmAdapter:55` | 是（三级回退末端：`config.model` → `AgentDto.model` → 本键） |
| `z.agent.default-max-steps` | `8` | `ReActAgent:182` | 是（只约束 ReAct；`PlanAgent` 步数 = 计划条目数，不读这个键） |
| `z.agent.default-temperature` | `0.7` | `ReActAgent:106` / `PlanAgent:100,123,143` | 是 |
| `z.agent.default-top-p` | `0.0` | 同上 | 是 |
| `z.agent.default-max-tokens` | `2048` | 同上 | 是 |
| `z.agent.default-timeout-ms` | `300000` | **无**：除自身 getter/setter 外全仓零引用 | 否（死键；单次调用没有超时控制） |
| `z.agent.providers` | 空 map | **无**：同上 | 否（死键；路由走 `LlmAdapter.supportsModel`，与本 map 无关） |

复算：`grep -n 'private \|public ' z-agent-core/src/main/java/com/zifang/z/agent/core/properties/AgentProperties.java`，
再 `rg --no-ignore -n "getProviders|getDefaultTimeoutMs" --glob '*.java' .` ⇒ 只命中该类自身。

单次请求还能覆盖 `config` 里的 **驼峰** 键（`{"config":{"maxSteps":3,"model":"gpt-4o"}}`，
`ReActAgent:171-182`）—— 这一处**不是** relaxed binding，写 `max-steps` 不生效；且 `maxSteps` 必须是 JSON 整数
（`instanceof Integer`），传字符串会被忽略。

### 环境变量

本仓**不读任何环境变量**，也没有任何 profile 差异（无 `application*.yml`、无 `@Value("${…:ENV"}`）。
API key 之类由宿主应用自己注入到 provider bean 的构造参数里，不落本仓；因此这里没有需要覆盖的机密项。

### 起控制面

starter 的 `@ComponentScan` 只扫 `core.controller / core.registry / core.runner / core.agent / core.properties` 五个包，
**不含 `com.zifang.z.agent.admin`**。要拿 `/z-agent/admin/**`，宿主应用得自己扫到它（`AdminController` 是 `@RestController`、
`AdminQueryService` 是 `@Service`）。同理，`index.html` 在 jar 的 classpath 根，而 Spring Boot 默认静态目录是
`classpath:/META-INF/resources/`、`/resources/`、`/static/`、`/public/` 四处 ⇒ 默认端不出来，需要宿主映射。

---

## 🔌 API 一览

全仓 `@RequestMapping` 实测就 9 行（2 个类级 + 7 个方法级）：

| 方法 | 路径 | 干什么 | 出处 |
|---|---|---|---|
| GET | `/agent/list` | 列全部已注册 agent 元信息（`List<AgentDto>`） | `core/controller/AgentController.java:48` |
| GET | `/agent/{name}` | 取单个 agent 元信息；未知名抛 `NOT_FOUND` | 同上 `:53` |
| POST | `/agent/{name}/run` | 同步跑一轮，返回 `RunResponse`（`output`/`steps`/`usage`/`finishedReason`/`errorMessage`/`metadata`） | 同上 `:58` |
| POST | `/agent/{name}/stream` | **占位，不流式**（声明 `produces=text/event-stream`，实际返回 JSON map） | 同上 `:65` |
| GET | `/z-agent/admin/overview` | `agentCount` / `agents` / `providerCount` / `models` / `status` | `admin/controller/AdminController.java:24` |
| GET | `/z-agent/admin/agents` | agent 名字列表（`List<String>`） | 同上 `:29` |
| GET | `/z-agent/admin/models` | `LlmAdapter.listModels()` | 同上 `:34` |

`POST /agent/{name}/run` 的请求体字段（`AgentController.parseRequest`，全部可省）：`input`、`history`（对象数组）、
`toolNames`（数组）、`config`（对象）。路径无 context-path 前缀，无鉴权拦截 —— 认证/租户隔离由宿主应用负责。

复算整张表：`rg --no-ignore -n "Mapping\(" --glob '*.java' .`；只数方法级：
`grep -rhE '@(Get|Post|Put|Delete)Mapping' --include='*.java' . | wc -l` ⇒ `7`。

---

## 🧪 测试

```bash
mvn test          # 37 支 @Test，全程 mock provider：不发真网络请求、不起 Spring 上下文
```

| 测试类 | 支数 | 覆盖 |
|---|---|---|
| `core/agent/AgentEndToEndTest` | 10 | ReAct 工具链多步、`max_steps`、tool not found、LLM 异常、按 model id 路由；Plan 正常计划与"解析不出→整段当一步" |
| `core/agent/LlmAdapterTest` | 10 | provider 路由与未知模型抛错、`parseArguments`、`findTool`、`toolResultMsg`、usage 累加、`listModels`/`providerCount` |
| `core/registry/AgentRegistryTest` | 10 | register/get/describe/list/unregister/覆盖同名、空 name 与 null 入参拒绝 |
| `core/agent/PlanAgentTest` | 7 | 编号计划解析（点号、中英文括号、全角、部分编号、空/null） |

分模块复算：`for m in z-agent-*; do printf "%-16s %s\n" "$m" "$(grep -rho '@Test' --include='*.java' $m 2>/dev/null | wc -l | tr -d ' ')"; done`
⇒ `z-agent-core` 37，`z-agent-api` / `z-agent-starter` / `z-agent-admin` 各 0。

如实说明的缺口：全仓没有一支 `@SpringBootTest` / `@WebMvcTest` / `MockMvc`
（复算：`rg --no-ignore -n "SpringBootTest|MockMvc|WebMvcTest" --glob '*.java' .` ⇒ 空），
`AgentController`、`AdminController`、`ZAgentAutoConfiguration` 在测试里一次都没被引用
⇒ 上面 7 个端点的真实返回形态、以及装配条件本身，目前没有任何自动化验收。

---

## ⚠️ 明确没做的（别当成已完成）

- **`/agent/{name}/stream` 是占位。** 它不进 runner，直接返回
  `{status:"sse-ready", agent:<name>, note:"SSE streaming endpoint placeholder; production wires runner.stream() to SseEmitter"}`
  （`AgentController:65-74`）。类注释里"POST /agent/{name}/stream — SSE 流式"这一句**当前不成立**。
- **整条链路都没有真流式。** `AgentRunner.stream(...)` 在生产里没有任何调用方（全仓对它的引用只有上面那句 `note`）；
  更下一层，`ReActAgent.streamRun` / `PlanAgent.streamRun` 都只是调 `run()` 后触发 `onComplete`，
  回调入参 `onStep` 从未被调用（复算：`rg --no-ignore -n "onStep" --glob '*.java' */src/main` ⇒ 只命中两处参数声明）。
  ⇒ `StreamChunk`（api 模块，58 行）没有任何生产出口。
- **请求级 `toolNames` 是死参。** `AgentRunner:105` 把它塞进 `config`，但 `core` 里没有任何读取点
  （复算：`rg --no-ignore -n "toolNames" --glob '*.java' */src/main` ⇒ 只有 DTO、Controller 解析、Runner 写入三处）。
  工具只能在构造 `ReActAgent` 时通过 `List<Tool>` 绑定。
- **两个配置键不生效**：`z.agent.default-timeout-ms`、`z.agent.providers`（详见上表"生效"列）。
- **注册中心是内存版**：`ConcurrentHashMap`，进程重启即空，没有持久化、没有多实例同步。
- **控制面 `models` 的口径 = `LlmAdapter.listModels()`**，即"注册进来的 provider 声称支持什么"，
  不是平台真的可用的模型清单；一个 provider 都没注册时它就是空列表（`providerCount` 同步为 0）。
- **`z-boot-fleet:1.0.1` 里 `z-agent.version` 槽位已是 `0.1.4`**，与本仓 `<revision>` 同号；
  下一刀抬版本时本仓必须晚于 fleet，否则 fleet 会指向一个还没上的版本。

---

## 📄 License

MIT，见仓库根 [`LICENSE`](LICENSE)（`Copyright (c) 2026 z-opc-foundation`）；根 POM `<licenses>` 同样声明 MIT License。

_Maintained by the z-opc-foundation organization._
