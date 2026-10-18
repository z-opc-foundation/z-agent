package com.zifang.z.agent.llm.infrastructure.llm.engine;

import com.zifang.z.agent.llm.infrastructure.events.Message;
import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;
import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;

import java.util.List;

/**
 * LLM React Engine — drives the ReAct (Reason + Act) loop.
 *
 * <p>支持两种构造模式（向后兼容）：
 * <ul>
 *   <li>{@code new LlmReactEngine(String defaultLlmCode)} — 仅占位，stream/chat 返回 stub（用于早期 demo）</li>
 *   <li>{@code new LlmReactEngine(String defaultLlmCode, LlmStreamProvider provider)} — 真实转发到 provider</li>
 *   <li>{@code setProvider(LlmStreamProvider)} — Spring 装配时延迟注入</li>
 * </ul>
 */
public class LlmReactEngine {

    private final String defaultLlmCode;
    private LlmStreamProvider provider;

    public LlmReactEngine() {
        this("default", null);
    }

    public LlmReactEngine(String defaultLlmCode) {
        this(defaultLlmCode, null);
    }

    public LlmReactEngine(String defaultLlmCode, LlmStreamProvider provider) {
        this.defaultLlmCode = defaultLlmCode;
        this.provider = provider;
    }

    private static String extractSystemPrompt(List<Message> messages) {
        if (messages == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (m == null) {
                continue;
            }
            String role = m.getRole();
            if ("system".equalsIgnoreCase(role) && m.getContent() != null) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }

                sb.append(m.getContent());
            }
        }
        return sb.toString();
    }

    private static String concatUserTurns(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (m == null) {
                continue;
            }
            String role = m.getRole();
            if ("system".equalsIgnoreCase(role)) {
                continue;
            }

            if (m.getContent() != null && !m.getContent().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }

                sb.append("[").append(role == null ? "user" : role).append("]\n")
                        .append(m.getContent());
            }
        }
        return sb.toString();
    }

    public String getDefaultLlmCode() {
        return defaultLlmCode;
    }

    public LlmStreamProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmStreamProvider provider) {
        this.provider = provider;
    }

    /**
     * Simple synchronous chat — sends messages to LLM and returns the full response.
     * <p>如有 provider：拼装 prompt + systemPrompt 后走 {@link LlmStreamProvider#stream(String, String, StreamCallbacks)}
     * 并累积 onToken；如无 provider：保留原 stub 返回（向后兼容）。
     */
    public String chat(List<Message> messages, LlmStreamRequest request) {
        if (provider == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[LLM Response Stub] messages=").append(messages.size());
            return sb.toString();
        }
        String prompt = concatUserTurns(messages);
        String systemPrompt = extractSystemPrompt(messages);
        StringBuilder acc = new StringBuilder();
        provider.stream(prompt, systemPrompt, new LlmStreamProvider.StreamCallbacks() {
            @Override
            public void onToken(String token) {
                if (token != null && !token.isEmpty()) {
                    acc.append(token);
                }
            }

            @Override
            public void onComplete(String fullText) {
                // provider 内部已同步等待 close；fullText 由 provider 提供，此处无需额外动作
            }

            @Override
            public void onError(Throwable error) {
                throw new RuntimeException("LLM chat() stream error: " + error, error);
            }
        });
        return acc.toString();
    }

    /**
     * Stream chat — invokes handler callbacks for incremental output.
     * <p>如有 provider：onText 接到 provider.onToken；onComplete 接到 provider.onComplete；onError 接到 provider.onError。
     * 如无 provider：保留原 stub 行为（onText + onComplete 一次）。
     */
    public void stream(List<Message> messages, LlmStreamRequest request, StreamHandler handler) {
        if (provider == null) {
            if (handler != null) {
                handler.onText("[LLM Stream Stub] messages=" + messages.size());
                handler.onComplete();
            }
            return;
        }
        String prompt = concatUserTurns(messages);
        String systemPrompt = extractSystemPrompt(messages);
        provider.stream(prompt, systemPrompt, new LlmStreamProvider.StreamCallbacks() {
            @Override
            public void onToken(String token) {
                if (handler != null) {
                    handler.onText(token);
                }
            }

            @Override
            public void onComplete(String fullText) {
                if (handler != null) {
                    handler.onComplete();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (handler != null) {
                    handler.onError(error);
                }
            }
        });
    }

    /**
     * Stream handler interface.
     */
    public interface StreamHandler {
        void onText(String text);

        void onComplete();

        void onError(Throwable t);
    }
}
