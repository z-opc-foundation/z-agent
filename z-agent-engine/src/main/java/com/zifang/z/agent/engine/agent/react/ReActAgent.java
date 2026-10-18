package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import com.zifang.z.agent.engine.agent.llm.model.OpenAITool;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolParam;
import com.zifang.z.agent.engine.agent.memory.MemoryManager;
import com.zifang.z.agent.engine.agent.memory.model.MemoryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct (Reasoning + Action) agent.
 * <p>
 * Public API (lightweight, see {@link Agent}):
 * - name() / stop() / reset() / isRunning()
 * - stream(input) : primary reactive entry — emits a sequence of {@link StreamEvent}
 * - run(input)    : blocking convenience over stream()
 * - runWithHistory(input) : blocking variant returning the full event log
 * <p>
 * Per-step logic:
 * 1. Build messages from current memory + system prompt + tool descriptions.
 * 2. Call LLM via {@link LlmClient#streamChatFlux(String, ChatCompletionsRequest)} —
 * each chunk becomes a {@link StreamEvent.ThoughtDelta} or {@link StreamEvent.FinalDelta}.
 * 3. If the terminal delta carries tool_calls, emit {@link StreamEvent.ToolCallRequest},
 * execute the tool, emit {@link StreamEvent.ToolResult}, then loop.
 * 4. Otherwise emit a final {@link StreamEvent.Done}.
 * 5. Repeat up to maxSteps.
 * <p>
 * Cancellation: stop() flips a flag; the running sink checks it on every event.
 */
public class ReActAgent implements Agent {

    private static final Logger log = LogManager.getLogger(ReActAgent.class);

    private static final int DEFAULT_MAX_STEPS = 10;
    private static final int DEFAULT_MAX_TOKENS = 2048;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final String name;
    private final String description;
    private final LlmClient llmClient;
    private final String providerCode;
    private final String model;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    // ========== SubAgent 支持 ==========
    private final List<SubAgent> children = new ArrayList<>();
    private final Object childrenLock = new Object();
    private InMemoryMemory memory;
    private Toolkit toolkit;
    private int maxSteps;
    private int maxTokens;
    private double temperature;
    private Boolean parallelToolCalls;
    private MemoryManager memoryManager;
    private String sessionId;
    private String systemPrompt;

    public ReActAgent(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.llmClient = builder.llmClient;
        this.providerCode = builder.providerCode;
        this.model = builder.model;
        this.memory = builder.memory != null ? builder.memory : new InMemoryMemory();
        this.toolkit = builder.toolkit != null ? builder.toolkit : new Toolkit();
        this.maxSteps = builder.maxSteps;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.parallelToolCalls = builder.parallelToolCalls;
        this.systemPrompt = builder.systemPrompt;
    }

    // ===== Core reactive API =====

    @Override
    public Flux<StreamEvent> stream(String input) {
        return Flux.create(sink -> {
            if (!running.compareAndSet(false, true)) {
                sink.error(new IllegalStateException("Agent " + name + " is already running"));
                return;
            }
            stopped.set(false);
            running.set(true);

            sink.onCancel(() -> {
                stopped.set(true);
                running.set(false);
            });

            try {
                memory.add(Msg.user(input));
                if (memoryManager != null && sessionId != null) {
                    memoryManager.append(sessionId, input, MemoryEntry.MemoryType.SHORT, null);
                }
                runReActLoop(sink);
                sink.complete();
            } catch (Throwable t) {
                sink.next(new StreamEvent.ErrorEvent(t));
                sink.complete();
            } finally {
                running.set(false);
            }
        });
    }

    /**
     * Blocking non-streaming ReAct loop.
     * Uses llmClient.chat() instead of streamChatFlux() to avoid
     * tool_call_id tracking issues across requests.
     */
    public String run(String userMessage) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Agent " + name + " is already running");
        }
        try {
            memory.add(Msg.user(userMessage));
            if (memoryManager != null && sessionId != null) {
                memoryManager.append(sessionId, userMessage, MemoryEntry.MemoryType.SHORT, null);
            }

            // 本次 run() 里是否已经执行过工具。用于区分「模型只是在描述、还没动手」
            // 与「工具已经跑完、现在在给最终答案」——两者的外部表现都是一段纯文本，
            // 只能靠"有没有 tool_result 进过 memory"区分。
            boolean toolExecuted = false;

            for (int step = 1; step <= maxSteps && !stopped.get(); step++) {
                ChatCompletionsRequest request = buildRequest();
                LlmClient.ChatResult result;
                try {
                    result = llmClient.chat(providerCode, request);
                } catch (IOException e) {
                    return "Error: " + e.getMessage();
                }
                // 2026-10-04 补：chat() 返回 null 的那一路。上面只接住了 IOException，
                // 而 chat() 可以正常返回 null —— LlmClient.toChatResult 在 response 为 null 时
                // 自己先 NPE，任何自定义 LlmClient 子类（provider 异常 / 限流 / 空响应体）
                // 都能直接给 null。原来直接 result.getContent()，null 一路 NPE 穿出 run()，
                // 调用方拿到 NullPointerException 而不是一句能读懂的话。
                // 契约见 ReActAgentTest#testRun_llmReturnsNull_returnsErrorMsg。
                if (result == null) {
                    log.warn("LLM provider {} 返回 null，按调用失败处理", providerCode);
                    return "Error: LLM 调用失败（provider " + providerCode + " 返回空结果）";
                }

                String content = result.getContent();
                List<ToolCall> toolCalls = result.getToolCalls();

                // Case 1: Tool calls — execute ALL of them in sequence
                if (result.hasToolCalls()) {
                    // Persist assistant message
                    if (content != null && !content.isEmpty()) {
                        memory.add(Msg.assistant(content));
                    }
                    // Execute ALL tool calls in order
                    for (ToolCall tc : result.getToolCalls()) {
                        String toolName = tc.getFunction() != null ? tc.getFunction().getName() : "?";
                        String argsJson = tc.getFunction() != null ? tc.getFunction().getArguments() : "{}";
                        Map<String, Object> args = parseArgs(argsJson);

                        if ("final_answer".equals(toolName) || "final".equals(toolName)) {
                            String answer = args != null && args.get("answer") != null
                                    ? args.get("answer").toString() : content;
                            Msg finalMsg = Msg.assistant(answer);
                            finalMsg.setFinal(true);
                            memory.add(finalMsg);
                            return answer;
                        }

                        Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                        if (tr.isNeedsConfirmation()) {
                            throw new ToolConfirmationNeeded(toolName, argsJson, tr.getConfirmationMessage());
                        }
                        String toolResultStr = tr.isSuccess() ? tr.getResultAsString()
                                : ("Error: " + tr.getError());
                        log.info("Tool {} executed, result={}", toolName, toolResultStr);
                        String toolCallId = tc.getId() != null ? tc.getId() : ("call_" + toolName + "_" + System.currentTimeMillis());
                        memory.add(Msg.assistantToolCall(toolCallId, toolName,
                                tc.getFunction() == null ? "{}" : tc.getFunction().getArguments()));
                        memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                        toolExecuted = true;
                    }
                    continue;
                }

                // Case 2: Text response — check if it describes a tool call
                if (content != null && !content.isEmpty()) {
                    // Fallback: parse text-described tool calls
                    ToolCall parsedTc = parseTextToolCall(content);
                    if (parsedTc != null) {
                        String toolName = parsedTc.getFunction().getName();
                        String argsJson = parsedTc.getFunction().getArguments();
                        Map<String, Object> args = parseArgs(argsJson);
                        log.info("Fallback: parsed text tool call: {} args={}", toolName, argsJson);
                        Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                        if (tr.isNeedsConfirmation()) {
                            throw new ToolConfirmationNeeded(toolName, argsJson, tr.getConfirmationMessage());
                        }
                        String toolResultStr = tr.isSuccess() ? tr.getResultAsString()
                                : ("Error: " + tr.getError());
                        log.info("Tool {} executed (fallback), result={}", toolName, toolResultStr);
                        String toolCallId = "fb_" + System.currentTimeMillis();
                        memory.add(Msg.assistantToolCall(toolCallId, toolName, argsJson));
                        memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                        toolExecuted = true;
                        continue;
                    }
                    // 未执行过任何工具：模型只是在描述、还没动手，给一次紧凑 nudge。
                    // 已执行过工具时不能走这里 —— 标准的 ReAct 流程恰恰是
                    // think → tool_call →（拿到 tool result）→ **纯文本最终答案**。
                    // 原实现只看 step > 1，于是第 2 步的最终答案被当成"只描述不执行"，
                    // 循环一路转到 maxSteps 也不返回，agent 把答案换成了
                    // "已已达到最大步数限制(N)"，工具调用能力形同虚设。
                    if (step > 1 && !toolExecuted) {
                        log.info("Model described without tool call (step={}), compact nudge", step);
                        memory.add(Msg.user(content + "\n\nExecute now."));
                        continue;
                    }
                    // First response: could be a greeting or direct answer；
                    // 工具跑完之后的纯文本则就是最终答案
                    Msg finalMsg = Msg.assistant(content);
                    finalMsg.setFinal(true);
                    memory.add(finalMsg);
                    return content;
                }

                return "Error: LLM returned empty content and no tool calls";
            }

            return "已已达到最大步数限制(" + maxSteps + ")，请简化您的请求。";
        } finally {
            running.set(false);
        }
    }

    private void runReActLoop(FluxSink<StreamEvent> sink) {
        StringBuilder finalAnswer = new StringBuilder();
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        boolean gaveFinal = false;

        for (int step = 1; step <= maxSteps && !stopped.get(); step++) {
            sink.next(new StreamEvent.StepStart(step));

            StepOutcome outcome = runOneStep(sink, step);
            if (outcome == null) {
                break;
            }

            if (outcome.promptTokens != null) {
                totalPromptTokens += outcome.promptTokens;
            }

            if (outcome.completionTokens != null) {
                totalCompletionTokens += outcome.completionTokens;
            }


            if (outcome.gaveFinalAnswer) {
                if (outcome.finalText != null && !outcome.finalText.isEmpty()) {
                    finalAnswer.append(outcome.finalText);
                }
                gaveFinal = true;
                break;
            }
            // tool was called; loop continues
        }

        if (!gaveFinal && !stopped.get()) {
            String exhausted = "已达到最大步数限制(" + maxSteps + ")，请简化您的请求。";
            sink.next(new StreamEvent.FinalDelta(exhausted));
            finalAnswer.append(exhausted);
        }
        if (stopped.get() && finalAnswer.length() == 0) {
            // stop() was called, no answer yet
            finalAnswer.append("");
        }

        sink.next(new StreamEvent.Done(
                finalAnswer.toString(),
                Math.min(maxSteps, totalStepsOrMax()),
                totalPromptTokens == 0 ? null : totalPromptTokens,
                totalCompletionTokens == 0 ? null : totalCompletionTokens));
    }

    private StepOutcome runOneStep(FluxSink<StreamEvent> sink, int step) {
        StepOutcome outcome = new StepOutcome();
        ChatCompletionsRequest request = buildRequest();

        // Subscribed Flux: each event updates local state; errors short-circuit.
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        final StringBuilder contentBuf = new StringBuilder();
        final List<ToolCall> collectedToolCalls = new ArrayList<>();
        try {
            llmClient.streamChatFlux(providerCode, request)
                    .doOnNext(delta -> {
                        if (stopped.get()) {
                            return;
                        }

                        if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                            contentBuf.append(delta.getContent());
                            // While we don't know yet whether this is a thought or the final answer,
                            // emit as ThoughtDelta. When the terminal delta arrives without
                            // tool_calls, we re-emit the buffer as FinalDelta.
                            sink.next(new StreamEvent.ThoughtDelta(delta.getContent()));
                        }
                        if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
                            collectedToolCalls.addAll(delta.getToolCalls());
                        }
                        if (delta.isDone()) {
                            if (delta.getPromptTokens() != null) {
                                outcome.promptTokens = delta.getPromptTokens();
                            }
                            if (delta.getCompletionTokens() != null) {
                                outcome.completionTokens = delta.getCompletionTokens();
                            }
                        }
                    })
                    .doOnError(firstError::set)
                    .blockLast();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            sink.next(new StreamEvent.ErrorEvent(cause));
            return null;
        }
        if (firstError.get() != null) {
            sink.next(new StreamEvent.ErrorEvent(firstError.get()));
            return null;
        }

        String fullContent = contentBuf.toString();

        // Case 1: LLM asked to call tools — execute ALL in sequence
        if (!collectedToolCalls.isEmpty()) {
            for (ToolCall tc : collectedToolCalls) {
                Map<String, Object> args = parseArgs(tc.getFunction() == null ? null : tc.getFunction().getArguments());
                String toolName = tc.getFunction() == null ? "?" : tc.getFunction().getName();

                if ("final_answer".equals(toolName) || "final".equals(toolName)) {
                    String answer = args != null && args.get("answer") != null
                            ? args.get("answer").toString() : fullContent;
                    sink.next(new StreamEvent.FinalDelta(answer));
                    Msg finalMsg = Msg.assistant(answer);
                    finalMsg.setFinal(true);
                    memory.add(finalMsg);
                    if (memoryManager != null && sessionId != null) {
                        memoryManager.append(sessionId, answer, MemoryEntry.MemoryType.SHORT, null);
                    }
                    outcome.gaveFinalAnswer = true;
                    outcome.finalText = answer;
                    return outcome;
                }

                sink.next(new StreamEvent.ToolCallRequest(toolName, args,
                        tc.getFunction() == null ? "{}" : tc.getFunction().getArguments()));

                Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                if (tr.isNeedsConfirmation()) {
                    throw new ToolConfirmationNeeded(toolName,
                            tc.getFunction() == null ? "{}" : tc.getFunction().getArguments(),
                            tr.getConfirmationMessage());
                }
                String toolResultStr = tr.isSuccess() ? tr.getResultAsString() : ("Error: " + tr.getError());
                sink.next(new StreamEvent.ToolResult(toolName, tr.getResult(), tr.getError(), tr.isSuccess()));
                String toolCallId = tc.getId() == null ? ("call_" + toolName + "_" + System.currentTimeMillis()) : tc.getId();
                memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                if (memoryManager != null && sessionId != null) {
                    memoryManager.append(sessionId, "tool:" + toolName + " result:" + toolResultStr, MemoryEntry.MemoryType.SHORT, null);
                }
                memory.add(Msg.assistantToolCall(toolCallId, toolName,
                        tc.getFunction() == null ? "{}" : tc.getFunction().getArguments()));
            }
            return outcome; // not final; loop continues
        }

        // Case 2: LLM gave a direct final answer
        if (fullContent != null && !fullContent.isEmpty()) {
            // Fallback: parse text-described tool calls
            ToolCall parsedTc = parseTextToolCall(fullContent);
            if (parsedTc != null) {
                String toolName = parsedTc.getFunction().getName();
                String argsJson = parsedTc.getFunction().getArguments();
                Map<String, Object> args = parseArgs(argsJson);
                log.info("Fallback: parsed text tool call: {} args={}", toolName, argsJson);
                Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                if (tr.isNeedsConfirmation()) {
                    throw new ToolConfirmationNeeded(toolName, argsJson, tr.getConfirmationMessage());
                }
                String toolResultStr = tr.isSuccess() ? tr.getResultAsString()
                        : ("Error: " + tr.getError());
                log.info("Tool {} executed (fallback), result={}", toolName, toolResultStr);
                String toolCallId = "fb_" + System.currentTimeMillis();
                memory.add(Msg.assistantToolCall(toolCallId, toolName, argsJson));
                memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                sink.next(new StreamEvent.ToolResult(toolName, tr.getResult(), tr.getError(), tr.isSuccess()));
                return outcome; // not final; loop continues
            }
            // Nudge: if model is describing without executing, push back
            if (step > 1) {
                log.info("Nudging model to call tools instead of describing (step={})", step);
                memory.add(Msg.assistant(fullContent));
                memory.add(Msg.user("请实际调用工具执行上述操作，不要只描述。直接使用 write_file / exec / mvn_build / curl_test 等工具。"));
                return outcome; // not final; loop continues
            }
            // Persist assistant final answer in memory
            Msg finalMsg = Msg.assistant(fullContent);
            finalMsg.setFinal(true);
            memory.add(finalMsg);
            if (memoryManager != null && sessionId != null) {
                memoryManager.append(sessionId, fullContent, MemoryEntry.MemoryType.SHORT, null);
            }
            // The thought-deltas already emitted; re-emit as FinalDelta so consumers
            // who only care about the final answer can subscribe just to that.
            sink.next(new StreamEvent.FinalDelta(fullContent));
            outcome.gaveFinalAnswer = true;
            outcome.finalText = fullContent;
        } else {
            // Empty response with no tool call and no content: treat as terminal error
            sink.next(new StreamEvent.ErrorEvent(new RuntimeException(
                    "LLM returned empty content and no tool calls")));
        }
        return outcome;
    }

    private int totalStepsOrMax() {
        return maxSteps;
    }

    // ===== Builder for maxSteps/usage reporting =====

    private ChatCompletionsRequest buildRequest() {
        ChatCompletionsRequest request = ChatCompletionsRequest.builder()
                .model(model)
                .messages(buildMessages())
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        if (!toolkit.getAllTools().isEmpty()) {
            request.setTools(convertTools(toolkit.getAllTools()));
            request.setToolChoice("required");
        }
        return request;
    }

    // ===== Request building =====

    private List<ChatMessage> buildMessages() {
        List<Msg> memoryMsgs = memory.getMessages();
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(ChatMessage.system(buildSystemPrompt()));
        for (Msg msg : memoryMsgs) {
            if (Msg.ROLE_SYSTEM.equals(msg.getRole())) {
                continue;
            }

            if (Msg.ROLE_TOOL.equals(msg.getRole())) {
                chatMessages.add(ChatMessage.toolResult(msg.getToolCallId(), msg.getToolName(), msg.getContent()));
            } else if (Msg.CONTENT_TYPE_TOOL_CALL.equals(msg.getContentType())) {
                // assistant tool-call message — must include tool_calls array for the API
                List<ToolCall> toolCalls = new ArrayList<>();
                ToolCall tc =
                        new ToolCall();
                tc.setId(msg.getToolCallId());
                tc.setType("function");
                ToolCall.FunctionCall func =
                        new ToolCall.FunctionCall();
                func.setName(msg.getToolName());
                func.setArguments(msg.getContent()); // content is now pure args JSON
                tc.setFunction(func);
                toolCalls.add(tc);
                chatMessages.add(ChatMessage.assistantWithToolCalls(null, toolCalls));
            } else {
                chatMessages.add(new ChatMessage(msg.getRole(), msg.getContent()));
            }
        }
        return chatMessages;
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append(systemPrompt).append("\n\n");
        } else {
            sb.append("You are an autonomous coding agent. You complete tasks by calling tools.\n");
        }
        sb.append("Available tools:\n");
        sb.append(toolkit.getToolsDescription());
        sb.append("\nCRITICAL RULES:\n");
        sb.append("1. Call tools to accomplish the user's request. Each response MUST include a tool call.\n");
        sb.append("2. After a tool executes, its result is automatically added to the conversation. You do NOT need to wait — keep working.\n");
        sb.append("3. Never describe tool calls in text. Always use the tool_calls mechanism.\n");
        sb.append("4. If a tool result indicates success, proceed to the next step. If it fails, try a different approach.\n");
        sb.append("5. Keep working until the entire task is complete. Do not stop after one tool call.\n");
        if (memoryManager != null && sessionId != null) {
            sb.append(memoryManager.buildMemoryPrompt(sessionId, 10));
        }
        return sb.toString();
    }

    private List<OpenAITool> convertTools(List<Tool> tools) {
        List<OpenAITool> result = new ArrayList<>();
        for (Tool tool : tools) {
            result.add(OpenAITool.of(tool.getName(), tool.getDesc(), buildParamsMap(tool)));
        }
        return result;
    }

    private Map<String, Object> buildParamsMap(Tool tool) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        if (tool.getParams() != null) {
            for (ToolParam param : tool.getParams()) {
                Map<String, Object> prop = new LinkedHashMap<>();
                prop.put("type", param.getType() != null ? param.getType() : "string");
                if (param.getDesc() != null) {
                    prop.put("description", param.getDesc());
                }

                properties.put(param.getName(), prop);
                if (param.isRequired()) {
                    required.add(param.getName());
                }

            }
        }
        root.put("properties", properties);
        if (!required.isEmpty()) {
            root.put("required", required);
        }

        return root;
    }

    /**
     * Parse text-described tool calls from model output.
     * Some models return text like:
     * ```typescript
     * functions.write_file({"path":"...","content":"..."})
     * ```
     * instead of proper tool_calls. This fallback parses those.
     */
    private ToolCall parseTextToolCall(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        // Pattern 1: functions.toolName({...})
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                "functions\\.(\\w+)\\(\\s*(\\{.*?\\})\\s*\\)",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m1 = p1.matcher(content);
        if (m1.find()) {
            String name = m1.group(1);
            String args = m1.group(2);
            ToolCall tc = new ToolCall();
            tc.setId("fb_" + System.currentTimeMillis());
            tc.setType("function");
            ToolCall.FunctionCall fn = new ToolCall.FunctionCall();
            fn.setName(name);
            fn.setArguments(args);
            tc.setFunction(fn);
            return tc;
        }
        // Pattern 2: toolName({"key":"value",...})
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                "(?:use|call|execute|run)\\s+(?:the\\s+)?(\\w+)\\s+(?:tool\\s+)?(?:with\\s+)?(?:args\\s+)?(\\{.*?\\})",
                java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m2 = p2.matcher(content);
        if (m2.find()) {
            String name = m2.group(1);
            String args = m2.group(2);
            ToolCall tc = new ToolCall();
            tc.setId("fb_" + System.currentTimeMillis());
            tc.setType("function");
            ToolCall.FunctionCall fn = new ToolCall.FunctionCall();
            fn.setName(name);
            fn.setArguments(args);
            tc.setFunction(fn);
            return tc;
        }
        // Pattern 3: Chinese descriptions — extract tool name and args
        for (Tool tool : toolkit.getAllTools()) {
            String tname = tool.getName();
            if (content.contains(tname)) {
                // Extract quoted strings as potential args
                java.util.regex.Pattern argP = java.util.regex.Pattern.compile(
                        "[\"']([^\"']+)[\"']", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher argM = argP.matcher(content);
                java.util.List<String> quoted = new java.util.ArrayList<>();
                while (argM.find()) quoted.add(argM.group(1));
                if (!quoted.isEmpty()) {
                    // Map args by position for known tools
                    java.util.Map<String, String[]> knownParams = new java.util.HashMap<>();
                    knownParams.put("write_file", new String[]{"path", "content"});
                    knownParams.put("exec", new String[]{"command"});
                    knownParams.put("mvn_build", new String[]{"goal"});
                    knownParams.put("curl_test", new String[]{"url", "method", "body"});
                    knownParams.put("search", new String[]{"name", "dir"});
                    String[] paramNames = knownParams.getOrDefault(tname, new String[]{"arg0", "arg1", "arg2"});
                    StringBuilder argsJson = new StringBuilder("{");
                    for (int i = 0; i < quoted.size() && i < paramNames.length; i++) {
                        if (i > 0) {
                            argsJson.append(",");
                        }

                        argsJson.append("\"").append(paramNames[i]).append("\":\"").append(quoted.get(i)).append("\"");
                    }
                    argsJson.append("}");
                    ToolCall tc = new ToolCall();
                    tc.setId("fb_" + System.currentTimeMillis());
                    tc.setType("function");
                    ToolCall.FunctionCall fn = new ToolCall.FunctionCall();
                    fn.setName(tname);
                    fn.setArguments(argsJson.toString());
                    tc.setFunction(fn);
                    return tc;
                }
            }
        }
        return null;
    }

    private Map<String, Object> parseArgs(Object arguments) {
        if (arguments == null) {
            return new HashMap<>();
        }

        if (arguments instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) arguments;
            return m;
        }
        if (arguments instanceof String) {
            try {
                return parseJsonArgs((String) arguments);
            } catch (Exception e) {
                log.warn("Failed to parse tool arguments: {}", arguments);
            }
        }
        return new HashMap<>();
    }

    private Map<String, Object> parseJsonArgs(String jsonStr) {
        Map<String, Object> result = new HashMap<>();
        jsonStr = jsonStr.trim();
        if (!jsonStr.startsWith("{") || !jsonStr.endsWith("}")) return result;
        jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
        // 极简：只支持 flat key:value / number / string。足够覆盖 ReAct 文本协议。
        int depth = 0;
        StringBuilder buf = new StringBuilder();
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                pairs.add(buf.toString());
                buf.setLength(0);
                continue;
            }
            buf.append(c);
        }
        if (buf.length() > 0) {
            pairs.add(buf.toString());
        }

        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = pair.substring(0, colon).trim().replaceAll("^\"|\"$", "");
            String value = pair.substring(colon + 1).trim().replaceAll("^\"|\"$", "");
            result.put(key, value);
        }
        return result;
    }

    @Override
    public String name() {
        return name;
    }

    // ===== Agent contract =====

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void stop() {
        stopped.set(true);
        running.set(false);
    }

    @Override
    public void reset() {
        memory.clear();
        if (memoryManager != null && sessionId != null) {
            memoryManager.clearSession(sessionId);
        }
        stopped.set(false);
        running.set(false);
    }

    public InMemoryMemory getMemory() {
        return memory;
    }

    public Toolkit getToolkit() {
        return toolkit;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Blocking run with extra context. Back-compat for callers that passed a
     * Map&lt;String,Object&gt; alongside the input. The Agent interface intentionally
     * doesn't expose this — only ReActAgent does, for the legacy test surface.
     */
    public String runWithContext(String input, java.util.Map<String, Object> context) {
        if (context != null && context.containsKey("system_prompt")) {
            memory.addSystemPrompt(String.valueOf(context.get("system_prompt")));
        }
        return run(input);
    }

    /**
     * Used by Agent.runWithHistory to materialize Msg-form history (legacy compat).
     */
    public List<Msg> runWithHistoryAsMsgs(String input) {
        return memory.getConversationMessages();
    }

    /**
     * Stop hook used by stream tests to verify cancellation.
     */
    boolean isStopped() {
        return stopped.get();
    }

    @Override
    public String toString() {
        return "ReActAgent{" +
                "name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", maxSteps=" + maxSteps +
                ", tools=" + toolkit.size() +
                '}';
    }

    /**
     * 创建并启动一个子代理。
     * 子代理与父代理共享 memory，但有独立的 ReAct 执行循环。
     *
     * @param name 子代理名称（用于调试）
     * @param task 任务描述
     * @return 新创建的 AgentNode（已自动启动）
     */
    public synchronized SubAgent spawnSubAgent(String name, String task) {
        return spawnSubAgent(name, task, null);
    }

    // ===== SubAgent 管理 =====

    /**
     * 创建并启动一个子代理，带指定上下文。
     *
     * @param name          子代理名称
     * @param task          任务描述
     * @param parentContext 父上下文（null 则创建新上下文）
     * @return 新创建的 AgentNode
     */
    public synchronized SubAgent spawnSubAgent(String name, String task, AgentContext parentContext) {
        if (!running.get() && !isRunning()) {
            throw new IllegalStateException("Cannot spawn subagent when agent is not running");
        }

        AgentContext ctx = parentContext != null ? parentContext : createDefaultContext();

        // 创建子 ReActAgent（共享 memory，独立的 toolkit）
        ReActAgent childAgent = new ReActAgent.Builder()
                .name(name)
                .llmClient(this.llmClient)
                .provider(this.providerCode)
                .model(this.model)
                .memory(this.memory)  // 共享 memory
                .toolkit(this.toolkit)  // 共享 toolkit
                .maxSteps(this.maxSteps)
                .maxTokens(this.maxTokens)
                .temperature(this.temperature)
                .parallelToolCalls(this.parallelToolCalls)
                .build();

        AgentNode node = new AgentNode(name, task, ctx, childAgent);
        ctx.set("parent_agent", this);

        synchronized (childrenLock) {
            children.add(node);
        }

        node.start();
        return node;
    }

    /**
     * 创建默认上下文（用于子代理）
     */
    private AgentContext createDefaultContext() {
        return new AgentContext((InMemoryMemory) this.memory, null);
    }

    /**
     * 等待所有子代理完成。
     * 调用者应定期检查 interrupted 状态。
     *
     * @return 子代理结果列表（按创建顺序）
     * @throws InterruptedException 如果等待被中断
     */
    public List<String> awaitChildren() throws InterruptedException {
        List<SubAgent> snapshot;
        synchronized (childrenLock) {
            snapshot = new ArrayList<>(children);
        }

        List<String> results = new ArrayList<>();
        for (SubAgent child : snapshot) {
            child.await();
            results.add(child.getResult());
        }
        return results;
    }

    /**
     * 等待所有子代理完成（带超时）。
     *
     * @param timeoutMs 超时毫秒
     * @return 完成的子代理列表
     * @throws InterruptedException 如果等待被中断
     */
    public List<SubAgent> awaitChildren(long timeoutMs) throws InterruptedException {
        List<SubAgent> snapshot;
        synchronized (childrenLock) {
            snapshot = new ArrayList<>(children);
        }

        long deadline = System.currentTimeMillis() + timeoutMs;
        List<SubAgent> completed = new ArrayList<>();

        for (SubAgent child : snapshot) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }

            // 简单轮询等待
            long start = System.currentTimeMillis();
            while (child.getState() == SubAgent.State.RUNNING
                    || child.getState() == SubAgent.State.WAITING) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                Thread.sleep(50);
                if (System.currentTimeMillis() - start > remaining) {
                    break;
                }

            }
            if (child.getState() == SubAgent.State.COMPLETED
                    || child.getState() == SubAgent.State.CANCELLED
                    || child.getState() == SubAgent.State.FAILED) {
                completed.add(child);
            }
        }
        return completed;
    }

    /**
     * 强制终止所有子代理。
     */
    public void killChildren() {
        List<SubAgent> snapshot;
        synchronized (childrenLock) {
            snapshot = new ArrayList<>(children);
        }
        for (SubAgent child : snapshot) {
            child.kill();
        }
    }

    /**
     * 获取当前子代理列表（快照）。
     */
    public List<SubAgent> getChildren() {
        synchronized (childrenLock) {
            return new ArrayList<>(children);
        }
    }

    /**
     * 检查是否有运行中的子代理。
     */
    public boolean hasActiveChildren() {
        synchronized (childrenLock) {
            for (SubAgent child : children) {
                SubAgent.State s = child.getState();
                if (s == SubAgent.State.RUNNING || s == SubAgent.State.WAITING) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 运行 ReAct 循环（使用指定上下文）。
     * 用于子代理的执行。
     */
    public String run(String task, AgentContext context) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Agent " + name + " is already running");
        }
        try {
            // 添加任务消息到 memory
            memory.add(Msg.user(task));

            for (int step = 1; step <= maxSteps && !stopped.get(); step++) {
                ChatCompletionsRequest request = buildRequest();
                LlmClient.ChatResult result;
                try {
                    result = llmClient.chat(providerCode, request);
                } catch (IOException e) {
                    return "Error: " + e.getMessage();
                }
                // 2026-10-04 补：chat() 返回 null 的那一路。上面只接住了 IOException，
                // 而 chat() 可以正常返回 null —— LlmClient.toChatResult 在 response 为 null 时
                // 自己先 NPE，任何自定义 LlmClient 子类（provider 异常 / 限流 / 空响应体）
                // 都能直接给 null。原来直接 result.getContent()，null 一路 NPE 穿出 run()，
                // 调用方拿到 NullPointerException 而不是一句能读懂的话。
                // 契约见 ReActAgentTest#testRun_llmReturnsNull_returnsErrorMsg。
                if (result == null) {
                    log.warn("LLM provider {} 返回 null，按调用失败处理", providerCode);
                    return "Error: LLM 调用失败（provider " + providerCode + " 返回空结果）";
                }

                String content = result.getContent();
                List<ToolCall> toolCalls = result.getToolCalls();

                if (content != null && !content.isEmpty()) {
                    memory.add(Msg.assistant(content));
                }

                if (result.hasToolCalls()) {
                    ToolCall tc = result.getToolCalls().get(0);
                    String toolName = tc.getFunction() != null ? tc.getFunction().getName() : "?";
                    String argsJson = tc.getFunction() != null ? tc.getFunction().getArguments() : "{}";
                    Map<String, Object> args = parseArgs(argsJson);

                    if ("final_answer".equals(toolName) || "final".equals(toolName)) {
                        String answer = args != null && args.get("answer") != null
                                ? args.get("answer").toString() : content;
                        Msg finalMsg = Msg.assistant(answer);
                        finalMsg.setFinal(true);
                        memory.add(finalMsg);
                        return answer;
                    }

                    Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                    if (tr.isNeedsConfirmation()) {
                        throw new ToolConfirmationNeeded(toolName, argsJson, tr.getConfirmationMessage());
                    }
                    String toolResultStr = tr.isSuccess() ? tr.getResultAsString()
                            : ("Error: " + tr.getError());
                    log.info("Tool {} executed, result={}", toolName, toolResultStr);
                    String toolCallId = tc.getId();
                    memory.add(Msg.assistantToolCall(toolCallId, toolName,
                            tc.getFunction() == null ? "{}" : tc.getFunction().getArguments()));
                    memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                    continue;
                }

                if (content != null && !content.isEmpty()) {
                    // Fallback: parse text-described tool calls
                    ToolCall parsedTc = parseTextToolCall(content);
                    if (parsedTc != null) {
                        String toolName = parsedTc.getFunction().getName();
                        String argsJson = parsedTc.getFunction().getArguments();
                        Map<String, Object> args = parseArgs(argsJson);
                        log.info("Fallback: parsed text tool call: {} args={}", toolName, argsJson);
                        Toolkit.ToolResult tr = toolkit.execute(toolName, args);
                        if (tr.isNeedsConfirmation()) {
                            throw new ToolConfirmationNeeded(toolName, argsJson, tr.getConfirmationMessage());
                        }
                        String toolResultStr = tr.isSuccess() ? tr.getResultAsString()
                                : ("Error: " + tr.getError());
                        log.info("Tool {} executed (fallback), result={}", toolName, toolResultStr);
                        String toolCallId = "fb_" + System.currentTimeMillis();
                        memory.add(Msg.assistantToolCall(toolCallId, toolName, argsJson));
                        memory.add(Msg.toolResult(toolCallId, toolName, toolResultStr));
                        continue;
                    }
                    Msg finalMsg = Msg.assistant(content);
                    finalMsg.setFinal(true);
                    memory.add(finalMsg);
                    return content;
                }

                return "Error: LLM returned empty content and no tool calls";
            }

            return "已达到最大步数限制(" + maxSteps + ")，请简化您的请求。";
        } finally {
            running.set(false);
        }
    }

    /**
     * Result of a single LLM call: tool called, final answer, or error/cancel.
     */
    private static final class StepOutcome {
        boolean gaveFinalAnswer;
        String finalText;
        Integer promptTokens;
        Integer completionTokens;
    }

    // ===== Builder =====

    public static class Builder {
        private String name = "ReActAgent";
        private String description = "A ReAct (Reasoning + Action) agent";
        private LlmClient llmClient;
        private String providerCode = "ollama";
        private String model = "llama3.2:3b";
        private InMemoryMemory memory;
        private Toolkit toolkit;
        private int maxSteps = DEFAULT_MAX_STEPS;
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private double temperature = DEFAULT_TEMPERATURE;
        private Boolean parallelToolCalls;
        private MemoryManager memoryManager;
        private String sessionId;
        private String systemPrompt;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder llmClient(LlmClient llmClient) {
            this.llmClient = llmClient;
            return this;
        }

        public Builder provider(String providerCode) {
            this.providerCode = providerCode;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder memory(InMemoryMemory memory) {
            this.memory = memory;
            return this;
        }

        public Builder toolkit(Toolkit toolkit) {
            this.toolkit = toolkit;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder memoryManager(MemoryManager memoryManager) {
            this.memoryManager = memoryManager;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder parallelToolCalls(Boolean v) {
            this.parallelToolCalls = v;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder addTool(Tool tool, Toolkit.ToolExecutor executor) {
            if (this.toolkit == null) {
                this.toolkit = new Toolkit();
            }

            this.toolkit.registerTool(tool, executor);
            return this;
        }

        public ReActAgent build() {
            if (llmClient == null) {
                throw new IllegalStateException("LlmClient is required");
            }
            return new ReActAgent(this);
        }
    }

    /**
     * Thrown when a tool requires user confirmation before execution.
     * The ReAct loop is aborted and control returned to the caller.
     */
    public static class ToolConfirmationNeeded extends RuntimeException {
        private final String toolName;
        private final String toolArgs;
        private final String reason;

        public ToolConfirmationNeeded(String toolName, String toolArgs, String reason) {
            super("Confirmation required for tool '" + toolName + "': " + reason);
            this.toolName = toolName;
            this.toolArgs = toolArgs;
            this.reason = reason;
        }

        public String getToolName() {
            return toolName;
        }

        public String getToolArgs() {
            return toolArgs;
        }

        public String getReason() {
            return reason;
        }
    }
}
