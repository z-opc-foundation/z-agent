package com.zifang.z.agent.engine.agent.llm.tool;

import com.zifang.util.json.JsonUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool executor for running tool calls.
 */
public class ToolExecutor {

    private static final Logger log = LogManager.getLogger(ToolExecutor.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Map<String, ToolHandler> handlers;

    public ToolExecutor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.handlers = new ConcurrentHashMap<>();

        // Register built-in handlers
        registerHandler("calculator", new CalculatorHandler());
        registerHandler("weather", new WeatherHandler());
    }

    /**
     * Register a tool handler.
     */
    public void registerHandler(String toolName, ToolHandler handler) {
        handlers.put(toolName, handler);
        log.info("Registered tool handler: {}", toolName);
    }

    /**
     * Execute a tool call.
     */
    public ToolResult execute(String toolName, String argumentsJson) {
        ToolHandler handler = handlers.get(toolName);
        if (handler == null) {
            return ToolResult.error("Unknown tool: " + toolName);
        }

        try {
            return handler.execute(argumentsJson);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Tool handler interface.
     */
    public interface ToolHandler {
        ToolResult execute(String argumentsJson) throws Exception;
    }

    /**
     * Tool execution result.
     */
    public static class ToolResult {
        private boolean success;
        private String content;
        private String error;

        public static ToolResult ok(String content) {
            ToolResult result = new ToolResult();
            result.success = true;
            result.content = content;
            return result;
        }

        public static ToolResult error(String error) {
            ToolResult result = new ToolResult();
            result.success = false;
            result.error = error;
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Calculator handler - simple math expression evaluation.
     */
    private static class CalculatorHandler implements ToolHandler {
        @Override
        public ToolResult execute(String argumentsJson) throws Exception {
            // Simple calculator - just evaluate basic expressions
            // In production, use a proper expression evaluator
            return ToolResult.ok("Calculator: " + argumentsJson);
        }
    }

    /**
     * Weather handler - placeholder for weather API.
     */
    private static class WeatherHandler implements ToolHandler {
        @Override
        public ToolResult execute(String argumentsJson) throws Exception {
            // In production, call a real weather API
            return ToolResult.ok("Weather data for: " + argumentsJson);
        }
    }
}
