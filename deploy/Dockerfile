# ============================================================
# z-agent-center 镜像构建定义（2026-10-22 补进本仓，闭环 README §6 的「本仓缺 Dockerfile」）
#
# 【为什么是「叠加」而不是「从零构建」】
#   线上原镜像 zopc/z-agent:local 是 ubuntu 26.04 + jdk8u504（entrypoint:
#   `sh -c "exec java $JAVA_OPTS -jar /app/app.jar"`），已在线上验证过 java/locale/时区。
#   本轮只换 jar（/app/app.jar），**不换运行时** ⇒ 行为差异面最小。
#   从零构建（FROM eclipse-temurin:8-jre）会换掉基础运行时，等于把「构建可复现」
#   与「运行时不变」二者对立；本仓选前者让位给后者，并把从零构建的命令写进
#   deploy/k8s/README.md §1（需要换运行时基座时再用那条）。
#
# 【前置：基镜像必须在本地 docker 里】
#   基镜像只存在于两个节点的 containerd、不在任何 registry，且不在本仓（168MB，不入 git）。
#   取法（任一节点导出 → 本机 load）：
#     ssh zifang@192.168.31.136 'sudo -n k3s ctr -n k8s.io images export /tmp/z-agent-local.tar \
#       docker.io/zopc/z-agent:local'
#     scp zifang@192.168.31.136:/tmp/z-agent-local.tar /tmp/
#     docker load -i /tmp/z-agent-local.tar      # ⇒ zopc/z-agent:local
#
# 【构建】
#   cd z-agent
#   mvn -pl z-agent-center/z-agent-center-starter -am clean package -DskipTests
#   cp z-agent-center/z-agent-center-starter/target/z-agent-center-starter-1.0.3-boot.jar app.jar
#   docker build --platform linux/amd64 -t zopc/z-agent:1.0.3-r2 -f deploy/Dockerfile .
#
# 【tag 铁律】
#   产出的 tag 必须与 deploy/k8s/02-deployment.yaml 的 `image:` 逐字一致 ——
#   本集群 imagePullPolicy=Never，k3s 按字面引用去节点本地 content store 找。
# ============================================================
FROM zopc/z-agent:local

# 启动类 jar：z-agent-center-starter 的 boot jar（com.zifang.z.agent.center.starter.Main）
COPY app.jar /app/app.jar

# locale 显式钉 UTF-8（与主壳 z-opc 镜像同一口径）：
#   JVM 的 sun.jnu.encoding 由 locale 决定；非 UTF-8 时中文路径（agent 目录 /
#   displayName）会抛 Malformed input or input contains unmappable characters。
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
