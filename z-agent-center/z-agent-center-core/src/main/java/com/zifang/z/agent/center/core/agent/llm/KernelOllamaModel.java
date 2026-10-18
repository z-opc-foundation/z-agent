package com.zifang.z.agent.center.core.agent.llm;

import com.zifang.z.agent.engine.agent.llm.model.define.Model;
import com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage;
import com.zifang.z.agent.engine.agent.llm.model.define.ModelOptions;
import com.zifang.z.agent.engine.agent.llm.model.define.ModelRequest;
import com.zifang.z.agent.engine.agent.llm.model.define.ModelResponse;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolSchema;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.llm.provider.OllamaProvider;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.types.MessageRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把 engine 的 {@link Model} 契约落到 kernel 的 {@link LlmProvider} 上。
 *
 * <p>这是 z-agent 用 kernel 替换 z-agent-engine 的第一刀: 保留 engine 侧的接口形态（AgentEngine
 * 只认 {@code Model}），但底下真正的 HTTP 调用走 kernel 的 provider SPI。这样 Ollama/OpenAI/
 * Anthropic 等协议实现只有一份，在 kernel 里演进。
 *
 * <p>与被替换掉的 engine {@code OllamaModel} 的行为差异: 走的是 OpenAI 兼容面
 * （{@code POST {baseUrl}/chat/completions}）而非 Ollama 原生的 {@code /api/chat}，所以
 * baseUrl 需带 {@code /v1} 后缀。
 */
public class KernelOllamaModel implements Model {

    private final LlmProvider provider;
    private final String baseUrl;
    private String modelName;

    public KernelOllamaModel(String baseUrl, String modelName) {
        this(new OllamaProvider(baseUrl == null || baseUrl.isEmpty() ? OllamaProvider.DEFAULT_BASE_URL : baseUrl),
                baseUrl, modelName);
    }

    KernelOllamaModel(LlmProvider provider, String baseUrl, String modelName) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String identity() {
        return "KernelOllamaModel(" + baseUrl + "/" + modelName + ")";
    }

    @Override
    public String chat(List<ModelMessage> modelMessages) {
        return contentOf(provider.chat(toRequest(modelMessages, null, null)));
    }

    @Override
    public String generate(List<ModelMessage> modelMessages) {
        return chat(modelMessages);
    }

    @Override
    public Flux<ModelResponse> stream(ModelRequest modelRequest) {
        List<ModelMessage> messages = modelRequest == null
                ? Collections.<ModelMessage>emptyList() : modelRequest.getMessages();
        List<ToolSchema> tools = modelRequest == null
                ? Collections.<ToolSchema>emptyList() : modelRequest.getTools();
        return stream(messages, tools, modelRequest == null ? null : modelRequest.getOptions());
    }

    @Override
    public Flux<ModelResponse> stream(List<ModelMessage> messages, List<ToolSchema> tools, ModelOptions options) {
        return Flux.create(new java.util.function.Consumer<FluxSink<ModelResponse>>() {
            @Override
            public void accept(FluxSink<ModelResponse> sink) {
                provider.streamChat(toRequest(messages, tools, options), chunk -> sink.next(chunkOf(chunk)),
                        sink::error);
                sink.complete();
            }
        });
    }

    private ChatCompletionsRequest toRequest(List<ModelMessage> messages, List<ToolSchema> tools, ModelOptions options) {
        List<Msg> msgs = new ArrayList<Msg>();
        if (messages != null) {
            for (ModelMessage message : messages) {
                msgs.add(new Msg(toRole(message.getRole()), message.getContent()));
            }
        }
        return new ChatCompletionsRequest(modelName, msgs);
    }

    private MessageRole toRole(String role) {
        if (role == null || role.isEmpty()) {
            return MessageRole.USER;
        }
        for (MessageRole candidate : MessageRole.values()) {
            if (candidate.name().equalsIgnoreCase(role)) {
                return candidate;
            }
        }
        return MessageRole.USER;
    }

    private String contentOf(ChatCompletionsResponse response) {
        if (response == null || response.getChoices().isEmpty()) {
            return "";
        }
        String content = response.getChoices().get(0).getContent();
        return content == null ? "" : content;
    }

    private ModelResponse chunkOf(ChatCompletionsResponse chunk) {
        return new ModelResponse(contentOf(chunk));
    }
}
