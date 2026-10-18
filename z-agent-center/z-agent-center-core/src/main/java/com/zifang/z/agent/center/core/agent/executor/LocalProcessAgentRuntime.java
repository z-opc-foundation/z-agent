package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地进程 Agent 运行时 — FEATURE014
 *
 * <p>与 {@link AgentContainerService}（Docker 路径）并列存在，负责拉起本地 CLI 子进程：</p>
 * <ul>
 *   <li>CLAUDE_CODE   → {@code claude serve --port {port}}（或回退到 {@code claude-code --port}）</li>
 *   <li>OPENCODE      → {@code opencode serve --port {port}}</li>
 *   <li>HERMES_LOCAL  → {@code hermes serve --port {port}}</li>
 *   <li>CUSTOM        → 用户在配置中提供的 shell 命令（{@code bash -c "<user-cmd> --port {port}"})</li>
 * </ul>
 *
 * <p>每个实例对应一个常驻子进程，由 {@link Process} 句柄持有。
 * 健康检查通过 {@code GET http://localhost:{port}/health} 进行（5s 超时）。</p>
 *
 * <p><b>注意</b>：本地进程路径没有 Docker 沙箱隔离，安全等级低于 HERMES_CONTAINER，
 * 仅推荐在受信任的开发机或同一台机器上使用。</p>
 */
@Service
public class LocalProcessAgentRuntime {

    private static final Logger log = LogManager.getLogger(LocalProcessAgentRuntime.class);

    /**
     * 端口分配范围（避开 AgentContainerService 的 8080-9000 段以免冲突）
     */
    private static final int PORT_BASE = 9100;
    private static final int PORT_MAX = 9999;

    /**
     * instanceCode → 进程句柄 + 端口
     */
    private final Map<String, ProcessHandle> processMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> portMap = new ConcurrentHashMap<>();

    /**
     * 兼容 Java 8: Process.pid() 是 Java 9+ API, 通过反射调用, 失败回退 0L。
     */
    private static long safePid(Process process) {
        if (process == null) {
            return 0L;
        }

        try {
            java.lang.reflect.Method m = Process.class.getMethod("pid");
            Object pid = m.invoke(process);
            return pid instanceof Long ? (Long) pid : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    /**
     * 启动一个本地进程型 Agent，返回分配的端口。
     *
     * @param instanceCode  实例编码（唯一）
     * @param agentType     Agent 类型（CLAUDE_CODE / OPENCODE / HERMES_LOCAL / CUSTOM）
     * @param customCommand CUSTOM 类型时由调用方传入的 shell 命令（可包含占位符 {@code {port}}）
     * @return 分配的本地端口
     */
    public synchronized Integer startProcess(String instanceCode, AgentType agentType, String customCommand) {
        if (portMap.containsKey(instanceCode)) {
            return portMap.get(instanceCode);
        }

        int port = allocatePort();
        String[] cmd = buildCommand(agentType, port, customCommand);
        if (cmd == null || cmd.length == 0) {
            throw new RuntimeException("Unable to build launch command for agentType=" + agentType);
        }

        log.info("Starting local agent process: instanceCode={} agentType={} cmd={} port={}",
                instanceCode, agentType, Arrays.toString(cmd), port);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            // 在用户主目录下执行，避免权限问题
            pb.directory(new File(System.getProperty("user.home")));

            // 给子进程传入 center 回调地址 / authToken（外部 Agent 可以用它反向调 center 注册 / 拉 skill）
            Map<String, String> env = pb.environment();
            env.put("Z_AGENT_CENTER_URL", "http://localhost:8888");
            // authToken 暂留空 — 创建实例时再注入（避免启动顺序耦合）

            Process process = pb.start();

            // 后台线程消费 stdout / stderr，避免管道阻塞导致子进程挂起
            drainAsync(process, instanceCode);

            // 等待最多 8 秒直到端口可达（或超时失败）
            if (!waitForHealthy(port, 8000)) {
                process.destroyForcibly();
                throw new RuntimeException("Agent process did not become healthy on port " + port
                        + " within 8s; cmd=" + Arrays.toString(cmd));
            }

            processMap.put(instanceCode, new ProcessHandle(process));
            portMap.put(instanceCode, port);
            log.info("Local agent process started: instanceCode={} port={} pid={}",
                    instanceCode, port, safePid(process));
            return port;
        } catch (Exception e) {
            log.error("Failed to start local agent process: instanceCode={} agentType={}",
                    instanceCode, agentType, e);
            throw new RuntimeException("Failed to start local agent: " + e.getMessage(), e);
        }
    }

    /**
     * 停止一个本地进程型 Agent。
     */
    public synchronized void stopProcess(String instanceCode) {
        Integer port = portMap.remove(instanceCode);
        ProcessHandle handle = processMap.remove(instanceCode);
        if (handle == null) {
            log.warn("stopProcess: no running process for instanceCode={}", instanceCode);
            return;
        }
        try {
            handle.process.destroy();
            if (!handle.process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                handle.process.destroyForcibly();
            }
            log.info("Stopped local agent process: instanceCode={} port={} pid={}",
                    instanceCode, port, safePid(handle.process));
        } catch (Exception e) {
            log.warn("Error stopping local agent process: instanceCode={}", instanceCode, e);
        }
    }

    /**
     * 健康检查：HTTP GET /health。
     */
    public boolean isHealthy(String instanceCode) {
        Integer port = portMap.get(instanceCode);
        if (port == null) {
            return false;
        }
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL("http://localhost:" + port + "/health").openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500; // 404 也算"进程在" — 某些 CLI 不实现 /health
        } catch (Exception e) {
            log.debug("Health check failed for instanceCode={}: {}", instanceCode, e.getMessage());
            return false;
        }
    }

    // ============== private helpers ==============

    /**
     * 检查外部 CLI 是否已安装在系统中（which / where 命令）。
     */
    public boolean isCliAvailable(AgentType agentType) {
        String binary = resolveBinary(agentType);
        if (binary == null) {
            return false;
        }

        try {
            Process p = new ProcessBuilder("which", binary).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String[] buildCommand(AgentType agentType, int port, String customCommand) {
        switch (agentType) {
            case CLAUDE_CODE:
                // 优先尝试 `claude serve`，回退到 `claude-code --port`
                return new String[]{"claude", "serve", "--port", String.valueOf(port)};
            case OPENCODE:
                return new String[]{"opencode", "serve", "--port", String.valueOf(port)};
            case HERMES_LOCAL:
                return new String[]{"hermes", "serve", "--port", String.valueOf(port)};
            case CUSTOM:
                if (customCommand == null || customCommand.trim().isEmpty()) {
                    throw new RuntimeException("CUSTOM agentType requires customCommand");
                }
                // 把 {port} 占位符替换成实际端口，整个命令用 bash -c 执行
                String rendered = customCommand.replace("{port}", String.valueOf(port));
                return new String[]{"bash", "-c", rendered};
            default:
                log.warn("AgentType {} does not support local-process mode", agentType);
                return null;
        }
    }

    private String resolveBinary(AgentType agentType) {
        switch (agentType) {
            case CLAUDE_CODE:
                return "claude";
            case OPENCODE:
                return "opencode";
            case HERMES_LOCAL:
                return "hermes";
            default:
                return null;
        }
    }

    private void drainAsync(Process process, String instanceCode) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[agent:{}] {}", instanceCode, line);
                }
            } catch (Exception e) {
                log.debug("drainAsync ended for {}: {}", instanceCode, e.getMessage());
            }
        }, "agent-stdout-" + instanceCode);
        t.setDaemon(true);
        t.start();
    }

    private boolean waitForHealthy(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL("http://localhost:" + port).openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                int code = conn.getResponseCode();
                if (code > 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // 还没起来，等
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private synchronized int allocatePort() {
        for (int p = PORT_BASE; p <= PORT_MAX; p++) {
            if (!portMap.containsValue(p)) {
                return p;
            }
        }
        throw new RuntimeException("No available port in range " + PORT_BASE + "-" + PORT_MAX);
    }

    /**
     * 内部数据结构：包一层 Process + 启动时间，便于后续做 uptime 统计。
     */
    public static class ProcessHandle {
        public final Process process;
        public final long startedAt;

        public ProcessHandle(Process process) {
            this.process = process;
            this.startedAt = System.currentTimeMillis();
        }

        public long uptimeMs() {
            return System.currentTimeMillis() - startedAt;
        }
    }
}
