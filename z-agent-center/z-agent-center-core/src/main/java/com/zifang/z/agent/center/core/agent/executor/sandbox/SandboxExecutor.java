package com.zifang.z.agent.center.core.agent.executor.sandbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 沙箱执行器 - 提供安全的命令执行环境
 * 使用独立的进程/线程池隔离执行危险操作
 */
public class SandboxExecutor {

    private static final Logger log = LogManager.getLogger(SandboxExecutor.class);

    private final ExecutorService executor;
    private final long timeoutMs;
    private final int maxOutputSize;

    public SandboxExecutor() {
        this(60 * 1000, 1024 * 1024); // 60秒超时, 1MB输出
    }

    public SandboxExecutor(long timeoutMs, int maxOutputSize) {
        this.timeoutMs = timeoutMs;
        this.maxOutputSize = maxOutputSize;
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "sandbox-worker");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    /**
     * 执行危险命令（文件操作、系统命令等）
     */
    public SandboxResult execute(String command, String... args) {
        long start = System.currentTimeMillis();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            Future<String> outputFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    char[] buffer = new char[8192];
                    int len;
                    while ((len = reader.read(buffer)) > 0) {
                        if (sb.length() < maxOutputSize) {
                            sb.append(buffer, 0, len);
                        }
                    }
                }
                return sb.toString();
            });

            Future<String> errorFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    char[] buffer = new char[8192];
                    int len;
                    while ((len = reader.read(buffer)) > 0) {
                        if (sb.length() < maxOutputSize) {
                            sb.append(buffer, 0, len);
                        }
                    }
                }
                return sb.toString();
            });

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionMs = System.currentTimeMillis() - start;

            if (!finished) {
                process.destroyForcibly();
                return new SandboxResult(false, "", "Execution timeout after " + timeoutMs + "ms", executionMs);
            }

            String output = outputFuture.get(1, TimeUnit.SECONDS);
            String error = errorFuture.get(1, TimeUnit.SECONDS);

            return new SandboxResult(process.exitValue() == 0, output, error, executionMs);

        } catch (Exception e) {
            log.error("Sandbox execution failed", e);
            return new SandboxResult(false, "", e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    /**
     * 执行 Java 代码片段（通过脚本引擎）
     */
    public SandboxResult executeScript(String script, String language) {
        long start = System.currentTimeMillis();
        try {
            if ("javascript".equals(language) || "js".equals(language)) {
                javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager()
                        .getEngineByName("Nashorn");
                if (engine == null) {
                    engine = new javax.script.ScriptEngineManager()
                            .getEngineByName("JavaScript");
                }
                if (engine != null) {
                    Object result = engine.eval(script);
                    return new SandboxResult(true, String.valueOf(result), "", System.currentTimeMillis() - start);
                }
            }
            return new SandboxResult(false, "", "Unsupported script language: " + language, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new SandboxResult(false, "", e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 沙箱执行结果
     */
    public static class SandboxResult {
        private final boolean success;
        private final String output;
        private final String error;
        private final long executionMs;

        public SandboxResult(boolean success, String output, String error, long executionMs) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.executionMs = executionMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public long getExecutionMs() {
            return executionMs;
        }
    }
}