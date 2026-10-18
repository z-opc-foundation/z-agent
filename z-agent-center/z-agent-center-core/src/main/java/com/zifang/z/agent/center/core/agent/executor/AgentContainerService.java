package com.zifang.z.agent.center.core.agent.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 容器管理服务 (使用 docker 命令行)
 * 负责创建/销毁 Hermes Agent 容器
 */
@Service
public class AgentContainerService {

    private static final Logger log = LogManager.getLogger(AgentContainerService.class);

    // 预定义的 Hermes Agent 镜像
    private static final String HERMES_IMAGE = "hermes-api:latest";

    // 容器端口范围
    private static final int PORT_BASE = 8080;
    private static final int PORT_MAX = 9000;

    // 已使用的端口 (instanceCode -> port)
    private final Map<String, Integer> instancePortMap = new ConcurrentHashMap<String, Integer>();

    /**
     * 创建 Agent 容器
     *
     * @param instanceCode 实例编码
     * @return 容器端口
     */
    public Integer createContainer(String instanceCode) {
        // 1. 检查是否已存在
        if (instancePortMap.containsKey(instanceCode)) {
            return instancePortMap.get(instanceCode);
        }

        // 2. 分配端口
        int port = allocatePort();

        // 3. 启动容器
        String containerName = "agent-" + instanceCode;

        try {
            // 检查 Docker 是否可用
            if (!isDockerAvailable()) {
                throw new RuntimeException("Docker is not available");
            }

            // 构建 docker run 命令
            // -d: 后台运行
            // --name: 容器名称
            // -p: 端口映射 (hostPort:containerPort)
            String[] cmd = {
                    "docker", "run", "-d",
                    "--name", containerName,
                    "-p", port + ":8888",
                    HERMES_IMAGE
            };

            String result = executeCommand(cmd);
            String containerId = result.trim();

            if (containerId.isEmpty()) {
                throw new RuntimeException("Failed to create container, no container ID returned");
            }

            instancePortMap.put(instanceCode, port);
            log.info("Created agent container: {} on port {}", containerName, port);

            return port;
        } catch (Exception e) {
            log.error("Failed to create container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create agent container: " + e.getMessage(), e);
        }
    }

    /**
     * 销毁 Agent 容器
     */
    public void destroyContainer(String instanceCode) {
        Integer port = instancePortMap.remove(instanceCode);
        if (port == null) {
            return;
        }

        String containerName = "agent-" + instanceCode;

        try {
            // docker rm -f: 强制删除运行中的容器
            executeCommand(new String[]{"docker", "rm", "-f", containerName});
            log.info("Destroyed agent container: {}", containerName);
        } catch (Exception e) {
            log.warn("Failed to destroy container: {}", containerName, e);
        }
    }

    /**
     * 获取容器状态
     */
    public String getContainerStatus(String instanceCode) {
        String containerName = "agent-" + instanceCode;
        try {
            String result = executeCommand(new String[]{"docker", "inspect", "-f", "{.State.Status}}", containerName});
            return result.trim();
        } catch (Exception e) {
            return "not_found";
        }
    }

    /**
     * 获取容器信息
     */
    public Map<String, Object> getContainerInfo(String instanceCode) {
        Map<String, Object> info = new HashMap<String, Object>();
        String containerName = "agent-" + instanceCode;

        try {
            String result = executeCommand(new String[]{"docker", "inspect", "-f", "{json .State}}", containerName});
            info.put("name", containerName);
            info.put("port", instancePortMap.get(instanceCode));
            info.put("status", result.trim());
            return info;
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return info;
        }
    }

    /**
     * 检查 Docker 是否可用
     */
    public boolean isDockerAvailable() {
        try {
            executeCommand(new String[]{"docker", "info"});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 分配可用端口
     */
    private synchronized int allocatePort() {
        for (int port = PORT_BASE; port <= PORT_MAX; port++) {
            if (!instancePortMap.containsValue(port)) {
                return port;
            }
        }
        throw new RuntimeException("No available port for agent container");
    }

    /**
     * 执行系统命令
     */
    private String executeCommand(String[] cmd) throws Exception {
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode + ", output: " + output.toString());
        }

        return output.toString();
    }
}