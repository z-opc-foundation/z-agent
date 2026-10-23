# z-agent · Secret 键位说明（`z-agent-db`）

> ⚠ **本文件不含任何真值**：口令与 RDS 主机名的真值只在 `lead/004_重要秘钥/`（不入仓）
> 与集群里已存在的 Secret 里，**明文零落盘**。

## 1. Secret 与键位

| Secret | 命名空间 | 键 | 内容 | 消费方 |
|---|---|---|---|---|
| `z-agent-db` | `z-agent` | `password` | 共享 RDS `oc` 库的口令 | `Z_BASE_DB_CONFIG_PASSWORD` |
| `z-agent-db` | `z-agent` | `db-host` | 共享 RDS 的**主机名**（只含主机名，不含端口） | `Z_BASE_DB_CONFIG_HOST` |

**两个键都必须齐全。** `deploy/k8s/02-deployment.yaml` 里 `Z_BASE_DB_CONFIG_HOST` 已改为
`secretKeyRef: z-agent-db / db-host`（与 `z-ops` 的 `01-secret.yaml` 同构：主机名也走 secret，
因为 `001_凭证管理规范` 禁止仓内出现云端地址字面量）。

⚠ **口令型与地址型是两条独立的边界**：本仓只解决**地址字面量**，口令本来就走 secret，
本次一个字都没动。

## 2. 灌 Secret 的命令（运维执行；本仓只出清单，不代跑）

⚠ **本命令取代 `deploy/k8s/README.md` §3 里那条「只写 `password` 一个键」的旧命令**
（README 不在本卡的允许改动路径内，故未同步改；**以本文件为准**）。

```bash
# ⚠ 值由运维从口令源现取（lead/004_重要秘钥/ + 集群现况），不写进本仓、不写进 commit
kubectl -n z-agent create secret generic z-agent-db \
  --from-literal=password='<从 lead/004_重要秘钥/ 取真值>' \
  --from-literal=db-host='<RDS 主机名，取集群现况真值>'
```

### 2.1 集群里该 Secret 已存在时（当前线上就是这种状态）

`kubectl create secret generic` 对**已存在**的同名 Secret 会报 `AlreadyExists`
⇒ 补 `db-host` 这个新键时用**增量 patch**（只加新键，**不碰**已有的 `password` 真值）：

```bash
kubectl -n z-agent patch secret z-agent-db --type=merge \
  -p '{"stringData":{"db-host":"<RDS 主机名，取集群现况真值>"}}'
```

## 3. ⚠ apply 的先后顺序（顺序错了 ⇒ Pod 起不来）

1. **先**灌好 Secret（`password` + `db-host` 两个键齐全）；
2. **再** apply Deployment：`kubectl apply -f deploy/k8s/`。

反过来（先 apply Deployment、Secret 里还没有 `db-host`）⇒ Pod 直接卡
`CreateContainerConfigError`（找不到 secret key）⇒ **不是**静默回落 localhost。

⚠ 同理，`Z_BASE_DB_CONFIG_*` 这 5 个 env 一个都不能少：少了 ⇒ 数据源回落 localhost ⇒ 启动即失败。
本卡只把 `Z_BASE_DB_CONFIG_HOST` 的**取值来源**从明文改成 secret，**env 键名一字未改**
（`Z_BASE_DB_CONFIG_HOST` / `_PORT` / `_DATABASE` / `_USERNAME` / `_PASSWORD`），
所以 Spring relaxed binding（`Z_BASE_DB_CONFIG_HOST` → `z.base.db.config.host`）照旧成立。

## 4. 边界

- 本文件与 `02-deployment.yaml` 的改动**只影响仓内清单**；**零 kubectl 写 / 零 apply /
  零生产改动** ⇒ 线上跑的 Pod 仍是旧配置，直到有人按 §2 灌完 Secret 并 apply。
- Secret 真值**永不入仓**：任何一次提交（含 commit message）里都不得出现主机名/口令明文。
