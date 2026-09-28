# z-agent

云端 Agent 平台的**库与 starter**：ReAct / Plan 两种 agent 默认实现 + 内存版注册中心 + REST 网关 + 只读控制面，
LLM 调用走 `z-agent-kernel` 的 `kernel.llm.LlmProvider` SPI。

> **本仓不产出可执行应用**：全仓没有 `@SpringBootApplication`，也没有 `main()`。
> 复算：`grep -rn 'SpringBootApplication\|public static void main' --include='*.java' .` ⇒ 只命中一句注释。
> 要跑起来得由业务方自己的 boot 应用引 `z-agent-starter`（见"怎么接"）。

| 项 | 值 | 怎么自己验一遍 |
|---|---|---|
| 坐标 | `io.github.yuku123:z-agent`（`packaging=pom` 聚合） | `grep -n '<packaging>' pom.xml` |
| 当前源码版本 | `0.1.2`（`<revision>`，唯一真源） | `grep -n '<revision>' pom.xml` |
| Central 上实际有的版本 | `0.1.0` / `0.1.1` / `0.1.2`（2026-09-27 实测；清单会变，信命令别信这一行） | `curl -s https://repo1.maven.org/maven2/io/github/yuku123/z-agent/maven-metadata.xml \| grep -o '<version>[^<]*'` |
| 模块 | `z-agent-api`（DTO）/ `z-agent-core`（全部逻辑与测试）/ `z-agent-starter`（自动装配）/ `z-agent-admin`（控制面） | `grep -n '<module>' pom.xml` |
| JDK | Java 8，Spring Boot 2.7.18 —— 两格都由 parent `z-boot-parent:1.0.19` 下发，本仓不再自声明 | `grep -n 'z-boot-parent' pom.xml` |
| 内核 pin | `z-agent-kernel.version=0.1.0` —— `z-boot-fleet:1.0.0` 的槽位是 `0.1.1`，本仓刻意钉在 0.1.0；内核源码树已到 `0.2.1`（`0.2.x` 未发 Central） | `grep -n 'z-agent-kernel.version' pom.xml` |
| 源码规模 | main 16 个 `.java` / test 4 个 | `find . -path '*/target' -prune -o -name '*.java' -print \| grep -c '/src/main/'`（test 同形改 `/src/test/`） |
| 测试数 | **37 支 `@Test`，全在 `z-agent-core`**（api/starter/admin 三个模块各 0 支） | `grep -rho '@Test' --include='*.java' . \| wc -l`；分模块：`for m in z-agent-*; do printf "%-16s %s\n" "$m" "$(grep -rho '@Test' --include='*.java' $m 2>/dev/null \| wc -l \| tr -d ' ')"; done` |

```bash
mvn -o test      # 37 支，全程 mock provider，不发任何真网络请求、不起 Spring 上下文
```

2026-09-27 04:4x 清树（`rm -rf target */target`）实测：类级 4 个测试类求和 = **37** = 模块汇总行 `Tests run: 37`，
`Failures=Errors=Skipped=0`，`BUILD SUCCESS`。

## 怎么接

```xml
<dependency>
  <groupId>io.github.yuku123</groupId>
  <artifactId>z-agent-starter</artifactId>
  <version>0.1.2</version>
</dependency>
```

然后**必须显式打开**：装配类上挂的是
`@ConditionalOnProperty(name = "z.agent.enabled", havingValue = "true", matchIfMissing = false)`，
缺省不装（`matchIfMissing = false`）。这条是刻意加的，防止与 z-opc 内部遗留的 `z-agent-center-*` 撞 bean
（出处 `ZAgentAutoConfiguration.java` 类注释）。

```properties
z.agent.enabled=true
```

打开后 Spring 拿到 3 个 `@Bean`：`AgentRegistry`、`LlmAdapter`、`AgentRunner`；
`AgentController` 与 `AdminQueryService` 不归 `@Bean` 管，是 `@ComponentScan` 通过 `@RestController` / `@Service` 发现的
（同一个类注释里写着"不要重复 @Bean"）。复算 bean 清单（**别用 `grep -c '@Bean'`**：那个文件里有 4 处 `@Bean` 文本，
第 4 处就是那句注释；取"注解后紧跟方法签名"的口径才是 3）：

```bash
grep -A1 '@Bean' z-agent-starter/src/main/java/com/zifang/z/agent/starter/autoconfig/ZAgentAutoConfiguration.java \
  | grep -c 'public'      # ⇒ 3
```

**6 个 LlmProvider 不是本仓给你装好的**。装配代码是把容器里已有的 `LlmProvider` bean 收进 `LlmAdapter`
（`ObjectProvider<LlmProvider>` 迭代），而内核 `z-agent-kernel-llm` 是纯 POJO、不带任何 Spring 注解
（复算：`grep -rl 'org.springframework' --include='*.java' ../z-agent-kernel | wc -l` ⇒ `0`）。
⇒ 你要自己 `@Bean` 出 provider，否则 `providerCount()=0`，任何 `/agent/{name}/run` 都会以
`no provider supports model: …` 失败。

## REST 面：7 个端点

| 方法 | 路径 | 干什么 | 出处 |
|---|---|---|---|
| GET | `/agent/list` | 列全部已注册 agent 元信息 | `core/controller/AgentController.java:48` |
| GET | `/agent/{name}` | 取单个 agent 元信息 | 同上 `:53` |
| POST | `/agent/{name}/run` | 同步跑一轮 | 同上 `:58` |
| POST | `/agent/{name}/stream` | **占位，不流式**（见下面"明确没做的"） | 同上 `:65` |
| GET | `/z-agent/admin/overview` | `agentCount` / `agents` / `providerCount` / `models` / `status` | `admin/controller/AdminController.java:24` |
| GET | `/z-agent/admin/agents` | agent 名字列表 | 同上 `:29` |
| GET | `/z-agent/admin/models` | `LlmAdapter.listModels()` | 同上 `:34` |

复算整张表：`grep -rn 'Mapping(' --include='*.java' */src/main`（9 行 = 7 个方法级 + 2 个类级 `@RequestMapping`）；
只数方法级：`grep -rhE '@(Get|Post|Put|Delete)Mapping' --include='*.java' . | wc -l` ⇒ `7`。

## 配置键（`z.agent.*`，全部从 `AgentProperties.java` 取）

代码里的字段是驼峰（`defaultModel`），配置文件写 kebab（`z.agent.default-model`）或驼峰都认（Spring relaxed binding）；
下表左列给 kebab 写法，右列缺省值一律以 `AgentProperties.java` 现读为准。

| 键 | 缺省值 | 作用 |
|---|---|---|
| `z.agent.enabled` | 无（必须显式 `true`） | 装配总开关 |
| `z.agent.default-model` | `gpt-4o-mini` | agent 不指定 model 时用 |
| `z.agent.default-max-steps` | `8` | ReAct 循环上限，超了 `finishedReason="max_steps"`（`ReActAgent.java:161`） |
| `z.agent.default-temperature` | `0.7` | 采样 |
| `z.agent.default-top-p` | `0.0` | 采样 |
| `z.agent.default-max-tokens` | `2048` | 输出上限 |
| `z.agent.default-timeout-ms` | `300000` | provider 调用超时 |
| `z.agent.providers` | 空 map | 模型 → provider bean 名映射 |

单次请求还能覆盖：`POST /agent/{name}/run` 的 `config` 里放 **驼峰** 键（`{"config":{"maxSteps":3}}`，
`ReActAgent.java:179-181`）——这一处**不是** relaxed binding，写 `max-steps` 不生效。
复算：`grep -n 'getConfig()' z-agent-core/src/main/java/com/zifang/z/agent/core/agent/ReActAgent.java`。

复算：`grep -n 'private ' z-agent-core/src/main/java/com/zifang/z/agent/core/properties/AgentProperties.java`
—— 缺省值就在这一行的右边，别信本表（本表是 2026-09-27 那次读的）。

## 三种 agent 实现是怎么分工的

| 类 | 行 | 角色 |
|---|---|---|
| `core/agent/LlmAdapter.java` | 134 | `model id → LlmProvider` 路由（`resolve()` 遍历 `supportsModel`），工具参数解析、usage 累加 |
| `core/agent/ReActAgent.java` | 203 | 思考→工具→观察循环，受 `defaultMaxSteps` 约束 |
| `core/agent/PlanAgent.java` | 191 | 先把目标拆成编号计划再逐步执行；解析不了就整段当一步 |
| `core/runner/AgentRunner.java` | 166 | 从 `AgentRegistry` 取 `Agent`，跑并把 `AgentResponse` 转成 DTO |
| `core/registry/AgentRegistry.java` | 86 | `ConcurrentHashMap` 内存版注册中心（**没有持久化**） |

复算行号列：`wc -l z-agent-core/src/main/java/com/zifang/z/agent/core/*/*.java`。

## 明确没做的（别当成已完成）

- **`POST /agent/{name}/stream` 是占位**。它不进 runner，直接返回一个 map：
  `{status: "sse-ready", agent: <name>, note: "SSE streaming endpoint placeholder; production wires runner.stream() to SseEmitter"}`
  （`AgentController.java:65-74`）。类注释里"POST /agent/{name}/stream — SSE 流式"这一句**当前不成立**。
- **`AgentRunner.stream(...)` 在生产里没有任何调用方**：全仓对它的引用只有上一条那句 `note` 字符串。
  复算：`grep -rn 'runner.stream\|\.streamRun' --include='*.java' .` ⇒ 只命中 `AgentRunner` 自己的实现行和那句注释。
  ⇒ `StreamChunk` 这个 DTO（api 模块，58 行）目前没有任何生产出口。
- **HTTP 层与自动装配零测试**：37 支全在 `z-agent-core` 的纯单元/E2E（mock provider），
  没有一支 `@SpringBootTest` / `MockMvc`，`AgentController`、`AdminController`、`ZAgentAutoConfiguration`
  在测试里**一次都没被引用**。复算：`grep -rn 'SpringBootTest\|MockMvc\|WebMvcTest' --include='*.java' .` ⇒ 空。
  ⇒ 上面那 7 个端点的实际字节返回形态目前没有任何自动化验收。
- **`z-agent-admin` 的 `models` 口径 = `LlmAdapter.listModels()`**，即"注册进来的 provider 支持什么"，
  不是平台真的能用的模型清单（provider 一个都没注册时它就是空列表）。
- **注册中心是内存版**：`AgentRegistry` 用 `ConcurrentHashMap`，进程重启即空，没有落库、没有多实例同步。
- **内核 pin 落后**：本仓 pom 钉 `0.1.0`，而内核源码树是 `0.2.1`（且 `0.2.x` 没发 Central，见 `../z-agent-kernel/README.md`）。
  要针对新内核构建：`mvn -o test -Dz-agent-kernel.version=0.2.1`，前提是你先在内核仓 `mvn -o install`。

## 许可

MIT，见 [`LICENSE`](LICENSE)。
