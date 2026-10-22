# z-agent 部署清单（deploy/k8s）

本目录是 **z-agent** 在家庭 k3s 集群（节点 `zifang001` / `zifang002`）的部署清单。

这批 Deployment 当初是手工 `kubectl apply` 上去的，仓里查不到「它是怎么部署的」——
集群无法从代码复现，发版只能靠记忆和手记。本目录把**线上实况**（`kubectl get deploy,svc -o yaml` 的真实导出）固化回代码，
作用有两个：让集群可从代码复现、让发版有据可依。

> ⚠ 本目录**只产出清单，不代跑**。`kubectl apply` 会改动生产集群，需要人工确认后执行。

## 0. 清单内容

| 文件 | 对象 | 线上对应 |
|---|---|---|
| `00-namespace.yaml` | Namespace `z-agent` | 已存在 |
| `01-service.yaml` | Service `z-agent-center`（NodePort 31893 → 8080） | 已存在 |
| `02-deployment.yaml` | Deployment `z-agent-center`（镜像 `zopc/z-agent:1.0.3-r2`，replicas **1**） | 已存在 |
| `../Dockerfile` | 镜像构建定义（2026-10-22 补进本仓，见 §1） | — |
| `README.md` | 本文件 | — |

线上实况要点（与基线导出逐条对齐）：

- Deployment 名 `z-agent-center`，容器名 `z-agent`，容器端口 `8080`（端口名 `http`）
- Service `z-agent-center`，`type: NodePort`，`port 8080 → targetPort http`，`nodePort 31893`
- 镜像 `zopc/z-agent:1.0.3-r2`，`imagePullPolicy: Never`
- env：`JAVA_OPTS=-Xms256m -Xmx1024m` + 5 个 `Z_BASE_DB_CONFIG_*`（口令走 secret）
- resources：requests `300m` / `1280Mi`，limits `2` CPU / `2Gi`
- **replicas 线上是 1**（2026-10-22 20:21 拉起；此前 2026-10-19 23:05 手工 scale down 到 0，
  而 0 状态下它**根本起不来** —— 容器启动即崩，见 §7）

## 0.1 怎么确认它是活的（2026-10-22 实测过的口径）

```bash
# 1) Pod 稳态
kubectl -n z-agent get pods -o wide
#    期望：1/1 Running，RESTARTS 0
# 2) 业务端点（不是 /actuator —— 这个服务不暴露 actuator）
curl -s http://192.168.31.136:31893/api/agent/template/health
#    期望：{"data":"Agent Template API is healthy","success":true,"code":200,...}
# 3) 真读库（不是只回健康）：app/list 会真的查 z_agent_app
curl -s http://192.168.31.136:31893/api/agent/app/list | head -c 200
# 4) 真造 agent（写库）：POST /api/agent/app，见 §7.4 的实测记录
```

## 1. 怎么构建镜像

### 现状（2026-10-22 已补齐）

**本仓现在有 Dockerfile 了**：`z-agent/deploy/Dockerfile`（叠加式：基镜像 `zopc/z-agent:local` + 只换 `/app/app.jar`）。
基镜像只存在于两个节点的 containerd、不在任何 registry、也不在本仓（168MB，不入 git），取法见 Dockerfile 头部注释。

线上镜像口径：裸 tag `zopc/z-agent:1.0.3-r2`，只存在于两个节点的 containerd，不在任何 registry。

### 实际执行过的构建命令（2026-10-22，r2 版）

```bash
# 1) 打 boot jar —— 启动类是 z-agent-center/z-agent-center-starter 里的 Main.java
cd z-agent
mvn -o -pl z-agent-center/z-agent-center-starter -am clean package -DskipTests
# 产物：z-agent-center/z-agent-center-starter/target/z-agent-center-starter-1.0.3-boot.jar

# 2) 送 jar 到构建机（.231 有 docker + 基镜像）
scp z-agent-center/z-agent-center-starter/target/z-agent-center-starter-1.0.3-boot.jar \
    192.168.31.231:/tmp/z-agent-center.jar

# 3) 取基镜像（.136 的 containerd → 本机 docker）
ssh 192.168.31.136 'sudo -n k3s ctr -n k8s.io images export /tmp/z-agent-local.tar \
    docker.io/zopc/z-agent:local'
scp 192.168.31.136:/tmp/z-agent-local.tar /tmp/
scp /tmp/z-agent-local.tar 192.168.31.231:/tmp/
ssh 192.168.31.231 'docker load -i /tmp/z-agent-local.tar'

# 4) 构建（tag 必须与 02-deployment.yaml 的 image: 逐字一致）
ssh 192.168.31.231 'cd ~/z-agent-build && cp /tmp/z-agent-center.jar app.jar && \
    docker build --platform linux/amd64 -t zopc/z-agent:1.0.3-r2 -f deploy/Dockerfile .'
```

> ⚠ **从零构建的变体**（想换基础运行时时才用，本轮**未验证**）：
> `FROM eclipse-temurin:8-jre` + `COPY app.jar /app/app.jar` + `ENTRYPOINT ["java","-jar","/app/app.jar"]`。
> 它会换掉 ubuntu26.04+jdk8u504 这个已在线上验证过的运行时基座 ⇒ 与主壳镜像口径不一致，
> 默认不走这条。

## 2. 怎么喂 containerd

铁律三条：

1. **裸 tag，禁 Nexus 前缀**——本集群 `imagePullPolicy: Never`，k3s 不会去 registry 拉，
   它按 `image:` 的字面引用去节点本地 content store 找。喂进去的 tag 带前缀
   （如 `nexus.internal/zopc/z-agent:local`）而清单里写的是裸 tag（或反过来）⇒ **ErrImageNeverPull**。
2. **必须用 `cat` 管道喂 `k3s ctr images import -`**——`ctr images import` 从 **stdin** 读，
   直接把文件路径当参数会报 `unrecognized image format`。
3. **两个节点都要喂**——两个节点都是工作节点，Pod 可能落在任一节点；只喂一个会出现
   `ErrImageNeverPull` + Pod 反复重建。

```bash
# 1) 在开发机上导出镜像（build 可在开发机做，不必上节点）
docker save zopc/z-agent:local > /tmp/z-agent-local.tar
scp /tmp/z-agent-local.tar zifang@192.168.31.231:/tmp/     # zifang001
scp /tmp/z-agent-local.tar zifang@192.168.31.136:/tmp/     # zifang002

# 2) 喂 zifang001（192.168.31.231）——该节点 sudo 需要口令，会交互提示
ssh zifang@192.168.31.231 'cat /tmp/z-agent-local.tar | sudo k3s ctr images import -'

# 3) 喂 zifang002（192.168.31.136）——该节点 sudo -n 可免密
ssh zifang@192.168.31.136 'cat /tmp/z-agent-local.tar | sudo -n k3s ctr images import -'

# 4) 核验（两个节点都要看到，且 tag 必须与清单一致）
ssh zifang@192.168.31.231 'sudo k3s ctr images ls | grep z-agent'
ssh zifang@192.168.31.136 'sudo -n k3s ctr images ls | grep z-agent'
```

一步到位的写法（不落临时文件，效果等价）：

```bash
docker save zopc/z-agent:local | ssh zifang@192.168.31.231 'cat - | sudo k3s ctr images import -'
docker save zopc/z-agent:local | ssh zifang@192.168.31.136 'cat - | sudo -n k3s ctr images import -'
```

## 3. 怎么 apply

```bash
# ⚠ 会改动生产集群，需要人工确认后再执行；本仓只产出清单，不代跑。
kubectl apply -f deploy/k8s/
```

执行顺序与前置条件（apply 前置检查）：

1. **secret 先在**：`z-agent-db`（键 `password`）必须已存在于 ns `z-agent`，
   否则 Pod 会卡 `CreateContainerConfigError`（找不到 secret key）。
   集群里这个 secret 已经存在；重建集群时按下面补灌（口令真值在
   `lead/004_重要秘钥/`，不入仓）：

   ```bash
   kubectl -n z-agent create secret generic z-agent-db \
     --from-literal=password='<从 lead/004_重要秘钥/ 取真值>'
   ```

2. **namespace 先在**：`00-namespace.yaml` 已包含在目录里，`kubectl apply -f deploy/k8s/`
   会按文件名顺序（00 → 01 → 02）依次 apply，namespace 自动先建。
3. **镜像先在**：见 §2。两个节点都喂完再 apply，否则 Pod 起不来。

apply 后自检：

```bash
kubectl -n z-agent get deploy,svc,pods
kubectl -n z-agent describe deploy z-agent-center | tail -30
```

## 4. 怎么回滚

```bash
# 看历史版本
kubectl -n z-agent rollout history deploy/z-agent-center

# 回滚到上一个版本
kubectl -n z-agent rollout undo deploy/z-agent-center

# 回滚到指定版本
kubectl -n z-agent rollout undo deploy/z-agent-center --to-revision=<N>

# 看回滚是否完成
kubectl -n z-agent rollout status deploy/z-agent-center
```

⚠ **PVC 数据不会自动回滚**：本域没有 PVC（纯无状态 Deployment），但 `oc` 库在**共享 RDS** 上，
回滚 Pod **不会**回滚数据库 schema / 数据。镜像里带 DDL 变更时，回滚镜像后旧 schema 仍在，
需要人工确认兼容性。

⚠ **回滚的前提是节点上有旧镜像**：回滚只改 Deployment 里的 `image:` 字段指向的旧 tag，
该 tag 必须仍在这两个节点的 containerd 里（`k3s ctr images ls` 能看到）。旧镜像被清掉就回滚不动。

## 5. 本清单与线上的差异（抄写时发现）

| 项 | 线上 | 本清单 | 原因 |
|---|---|---|---|
| 探针 | 无 | `httpGet /api/agent/template/health`（readiness + liveness） | 2026-10-22 已拉起 1 副本并**实测过该 path 返回 200** ⇒ 换成真能判"应用活着"的探针（原先的 tcpSocket 只证端口在监听） |
| replicas | 1 | 1 | 2026-10-22 20:21 拉起（旧清单的 0 是 2026-10-19 scale down 的残留，而 0 状态下它起不来） |
| 镜像 | `zopc/z-agent:1.0.3-r2` | 同左 | 含 §7 修好的数据源装配 |
| labels | 资源上只有 `app.kubernetes.io/name` | 额外加 `app.kubernetes.io/part-of` | 仅加标签，不影响 selector 匹配；`apply` 是幂等合并 |
| SA 令牌自动挂载 | `automountServiceAccountToken: false` | 不含该字段 | 该收敛由**独立清单**执行（卡 `TASK-20261021-021` 交付、用户亲自执行），刻意与各域部署清单分开，避免抢同一批字段。⚠ `apply` 本清单**不会回退**这项加固（字段既不在本清单、也不在 last-applied ⇒ 三向合并不构成删除动作，已用 `kubectl apply --dry-run=server` 逐域实测：该字段仍为 `false`） |

## 6. 待办

- ~~本仓缺 Dockerfile~~ ✅ 2026-10-22 已补 `z-agent/deploy/Dockerfile`（§1）。
- ~~探针换成 HTTP 端点~~ ✅ 2026-10-22 已换成 `/api/agent/template/health`（实测 200）。
- 工厂与主壳**双份运行**的边界需要长期口径（见 §7.5）：两边读同一张 `z_agent_app`，
  逻辑删除 / 驼峰映射 / 分页语义必须一致（已在本轮对齐，见 `AgentCenterDataSourceConfig` 的 javadoc）。

## 7. 2026-10-22：工厂从「永远起不来」修到「可用」

### 7.1 三个缺陷（按踩到的顺序）

1. **缺 `sqlSessionFactoryConfig` bean** ⇒ `APPLICATION FAILED TO START`
   ```
   Field instanceMapper in com.zifang.z.agent.center.core.skill.auth.AgentAuthInterceptor
     required a bean named 'sqlSessionFactoryConfig' that could not be found.
   ```
   该 bean 一直由 **z-team-starter 的 `TeamSqlSessionFactoryConfig`** 提供（复用 `@Primary` dataSource，
   并额外暴露名为 `sqlSessionFactoryConfig` 的别名）。z-agent-center 独立进程里**没有 z-team-starter**
   ⇒ 无人提供。日志里完全没有 DataSource/Hikari/MyBatis 装配痕迹，可反证是「根本没装配数据源」
   而不是「连不上库」：`z-boot-datasource-starter` 只提供 `ModuleDataSourceTemplate` 等辅助类，
   **不带 spring.factories**，不会自动创建 DataSource —— 每个独立服务必须自持一份装配。
   ⇒ 修法：`z-agent-center-starter` 新增 `AgentCenterDataSourceConfig`（照 `z-ops` 的
   `OpsDataSourceConfig` 范式：`extends ModuleDataSourceTemplate`）。

2. **`AgentAkService` 双 bean** ⇒ `NoUniqueBeanDefinitionException: expected single matching bean
   but found 2: agentAkServiceImpl,agentAkService`
   根因：`Main` 的 `@MapperScan(basePackages = {"com.zifang.z.agent.center"})` —— **`@MapperScan`
   会把范围内的每一个接口都注册成 mapper bean，不要求接口带 `@Mapper` 注解**，
   于是 service 接口 `AgentAkService` 被注册成了名为 `agentAkService` 的 mapper 代理。
   主壳不炸是因为它不走 `Main`（主壳依赖 center-core/-web，mapper 由 z-team-starter 的
   `TeamSqlSessionFactoryConfig` 用精确到 `*.mapper` 的清单注册）。
   ⇒ 修法：`Main` 的 `@MapperScan` 收窄到 15 个精确 mapper 包。

3. **与主壳的 MyBatis 语义漂移**（不是启动错，是**数据错**）
   独立实例与主壳内嵌副本读写**同一张表**（同一个 RDS `oc` 库）。若两边的逻辑删除 /
   驼峰映射 / 分页开关不一致，就是**两个运行时用不同语义改同一批数据**。
   ⇒ 修法：`AgentCenterDataSourceConfig` 里显式对齐 z-team-starter 的口径
   （`map-underscore-to-camel-case=true`；逻辑删除 `deleted` 字段 / 值 1 / 未删值 0；
   显式挂 `PaginationInnerInterceptor`）。

### 7.2 为什么修在 starter 而不是 core

`z-agent-center-starter` **没有**被主壳依赖（z-opc 的 `dependencyManagement` 里只是版本钉，
实测线上主壳 fat jar 的 `BOOT-INF/lib` 里没有 `z-agent-center-starter`）⇒ 数据源装配放这里
**不会**给主壳造出第二个 DataSource。放 core 就等于污染主壳。

### 7.3 版本口径

本轮把 z-agent 仓 `revision` 从 `1.0.2` 抬到 `1.0.3`（新增 starter 的数据源装配 + 收窄 @MapperScan），
镜像 `zopc/z-agent:1.0.3-r2` 里装的就是 `z-agent-center-starter-1.0.3-boot.jar`。
⚠ 中央仓**同一版本不能重发**，所以 1.0.2 的旧字节永远留在中央 —— 拉取 1.0.3 才是修复后的版本。

### 7.4 实测记录（2026-10-22 20:21–20:23）

- Pod `z-agent-center-7c574485b9-wtdcw`：**1/1 Running，RESTARTS 0**，落点 zifang002
- 旧 ReplicaSet 全部 scale 到 0（无残留 Pod）
- `/api/agent/template/health` → 200 `{"success":true,"data":"Agent Template API is healthy"}`
- `/api/agent/plugin/health` → 200
- `/api/agent/app/list` → 200，**返回真实 `z_agent_app` 数据**（如 `agent_catalog_bot`/集群册子助手）
  ⇒ 证明数据源真的接上了 RDS，不是只回健康
- `/api/agent/group/tree` → 200，真实分组数据
- **造 agent 实测**：`POST /api/agent/app`（appName=工厂冒烟-202303, agentMode=agent, status=DRAFT）
  → 200，落库 `id=27` / `appCode=app_ffd3f231786c`，且模型被自动补全（qwen2.5:7b / ollama）；
  `GET /api/agent/app/list` 立即回查到该行 ⇒ **写→读闭环成立**；
  测完用 `POST /api/agent/app/delete?id=27` 清掉，**复查剩余 0 行**（不污染生产库）

### 7.5 双份运行的边界（遗留口径，非本轮解决）

主壳 z-opc 里也内嵌了一份工厂代码（`z-agent-center-web` / `-core` 是主壳依赖），
个人模式造 agent 走的是**同进程**路径：`POST /api/personal-agents`
→ `TeamPersonalAgentServiceImpl.createBackingAgent()` → **直写 `z_agent_app` 表**
（`z-team.integration.use-local-agent-center=true`），**不经过**独立 z-agent-center 进程。
⇒ 独立部署是"同一份实现的另一种拓扑"（同库同码），不是第二份实现；
两边语义必须一致，见 §7.1 第 3 条。

### 7.6 ⚠ 暴露面：独立实例的写接口**无鉴权**（2026-10-22 起生效，需业务侧定夺）

**事实**（实测）：
- Service 是 `NodePort 31893`（这是基线导出的原拓扑，本轮未改）
- 公网**不可达**：`curl http://101.37.80.51:31893/...` = **000**（阿里云 ECS 只转发 ingress 域名）
- **LAN 可达**：`curl http://192.168.31.136:31893/api/agent/template/health` = **200**
- `AgentCenterWebConfig` 里**只有** `/api/agent/skill/**` 挂了 AK 鉴权拦截器
  ⇒ 其余 `/api/agent/**` 的写接口（app 建/删/发布、template、plugin、group、
  instance、ak、share、trigger、memory、textgen…）**无鉴权**
- 主壳里同一份代码**是**有 SSO 保护的（主壳的 SSO filter 拦 `/api/agent/**`），
  独立实例**没有** SSO ⇒ **同一个 API 在两个拓扑上的暴露面不同**

**为什么现在才要紧**：本域此前 replicas=0 且**启动即崩** ⇒ 等于这个暴露面一直不存在。
本轮修好并拉起后，它**真的开着**了 ⇒ 暴露面由「不存在」变成「存在」。

**本轮未擅自改**（理由：改拓扑/加鉴权会影响其它消费方，且原清单就是 NodePort）：
- 三个可选口径，请业务侧定：
  ① **Service 改 ClusterIP**（最小改动：撤掉 LAN 暴露；集群内 z-team 照常走
     `z-agent-center.z-agent.svc.cluster.local:8080`，只是外部探针要从集群内做）
  ② **给独立实例补 SSO**（与主壳同口径；代价是要处理 `TeamAgentHubHttpClient`
     的凭据传递，且我的冒烟测试需改成带 Cookie）
  ③ **接受现状**（仅 LAN 可达、仅家庭/办公内网；与本集群其它域如 z-config-web 同级）

**⇒ 在定夺之前，不要把 31893 暴露到公网入口（ingress / ECS 反代）**，
那会把「LAN 内网可写」升级成「公网可写」。


## 8. ⚠️ 纪律：命令式改完，**必须用清单 apply 收口**（2026-10-22 落地 · 卡 `TASK-20261022-054` 第 3 项）

### 8.1 为什么

`kubectl set image` / `kubectl scale` / `kubectl edit` **按构造不会更新**
`kubectl.kubernetes.io/last-applied-configuration` 注解 ⇒
**线上 spec 已经变了，注解还停在「上一个被 apply 过的版本」** ⇒ 注解与实况失同步。

活标本（本域，2026-10-22 22:4x 只读实测）：

| 口径 | 值 |
|---|---|
| 线上 live | `image=zopc/z-agent:1.0.3-r2`、`replicas=1` |
| last-applied 注解 | **仍写** `image=zopc/z-agent:local`、`replicas=0`（2026-10-19 手工 scale down 那个时代的值）|
| 仓内清单 `origin/main` | `image=zopc/z-agent:1.0.3-r2`、`replicas=1`（与线上 spec **逐字段一致**，见 §5）|

⇒ 20:26 那次修复是用命令式上去的（`kubectl set image` + `kubectl scale`）⇒ 注解没被收口。

### 8.2 危害分两档（**别混读**）

| 档 | 判据 | 危害 |
|---|---|---|
| **注解陈旧、清单已对齐**（本域现状）| `repo_target_state=aligned` | **低危** —— 用本清单 apply 是**幂等**的，不改业务；注解反而被顺手刷新成正确值 |
| **清单落后于线上**（`z-oss` 那种）| `repo_target_state=repo_behind_live` | **高危** —— 任何一次例行 apply 都会把线上回滚 |

⇒ **读漂移报告先看 `repo_target_state`**：别把「N 个注解陈旧」读成「N 个会被 apply 回滚」。

### 8.3 纪律（三条）

1. **能用清单改的，就不用命令式改**；发布走 lead 仓 `006_部署方案/010_发版收尾清单.md` 的完整链路。
2. **命令式改完必须用清单 apply 收口**（**唯一能止血的动作**）——
   命令式修复是**递归**的：每做一次命令式改动，就再造一个「last-applied 落后于线上」的实例 ⇒
   **只修当前这一个实例不能止血**，必须把「收口」写进纪律。
   ```bash
   # ⚠ 写生产对象 ⇒ 需人工确认后再执行；本仓只产出清单，不代跑。
   kubectl apply -f deploy/k8s/02-deployment.yaml
   ```
3. **收口后自查注解与实况一致**（只读）：
   ```bash
   kubectl -n z-agent get deploy z-agent-center \
     -o jsonpath='{.metadata.annotations.kubectl\.kubernetes\.io/last-applied-configuration}' \
     | python3 -c "import sys,json; d=json.load(sys.stdin); print('last-applied image=',d['spec']['template']['spec']['containers'][0]['image'],'replicas=',d['spec'].get('replicas'))"
   ```
   期望：`image` == `zopc/z-agent:1.0.3-r2` 且 `replicas` == 1（与 §5 表里的清单一致）。
   ⚠ **注解里可能带明文 env 值**（本域实测注解里就有 DB host / 用户名）⇒
   **只摘 `image` / `replicas` 字段入库**，不要把注解全文贴进卡、报告或提交记录。

> 通用版纪律见 lead 仓 `006_部署方案/010_发版收尾清单.md` §三·十八。
