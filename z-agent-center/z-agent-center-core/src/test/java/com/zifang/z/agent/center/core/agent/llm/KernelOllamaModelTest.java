package com.zifang.z.agent.center.core.agent.llm;

import com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.llm.Model;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.types.MessageRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * pin 住 engine Model → kernel LlmProvider 的映射契约。
 */
class KernelOllamaModelTest {

    private final List<ChatCompletionsRequest> captured = new ArrayList<>();

    private LlmProvider stub(String content) {
        return new LlmProvider() {
            @Override
            public String name() {
                return "stub";
            }

            @Override
            public List<Model> listModels() {
                return Collections.emptyList();
            }

            @Override
            public boolean supportsModel(String modelId) {
                return true;
            }

            @Override
            public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
                captured.add(request);
                return new ChatCompletionsResponse("id-1", request.getModel(),
                        Collections.singletonList(new ChatCompletionsResponse.Choice(
                                0, content, Collections.emptyList(), "stop")),
                        null, "stop", Collections.emptyMap());
            }

            @Override
            public void streamChat(ChatCompletionsRequest request, Consumer<ChatCompletionsResponse> onChunk,
                                   Consumer<Throwable> onError) {
                chat(request);
            }
        };
    }

    private KernelOllamaModel model(LlmProvider provider) {
        return new KernelOllamaModel(provider, "http://stub/v1", "qwen:7b");
    }

    @Test
    void chat_returnsFirstChoiceContent() {
        assertEquals("hello", model(stub("hello")).chat(
                Collections.singletonList(ModelMessage.of("user", "hi"))));
    }

    @Test
    void chat_passesModelNameAndAllMessages() {
        model(stub("ok")).chat(Arrays.asList(
                ModelMessage.of("system", "be brief"),
                ModelMessage.of("user", "hi")));

        assertEquals(1, captured.size());
        assertEquals("qwen:7b", captured.get(0).getModel());
        assertEquals(2, captured.get(0).getMessages().size());
    }

    @Test
    void roles_mapToKernelEnum() {
        model(stub("ok")).chat(Arrays.asList(
                ModelMessage.of("system", "s"),
                ModelMessage.of("user", "u"),
                ModelMessage.of("assistant", "a")));

        List<Msg> sent = captured.get(0).getMessages();
        assertEquals(MessageRole.SYSTEM, sent.get(0).getRole());
        assertEquals(MessageRole.USER, sent.get(1).getRole());
        assertEquals(MessageRole.ASSISTANT, sent.get(2).getRole());
    }

    @Test
    void blankRole_fallsBackToUser() {
        model(stub("ok")).chat(Collections.singletonList(ModelMessage.of(null, "x")));
        assertEquals(MessageRole.USER, captured.get(0).getMessages().get(0).getRole());
    }

    @Test
    void nullMessageList_yieldsEmptyRequestNotNpe() {
        assertEquals("ok", model(stub("ok")).chat(null));
        assertNotNull(captured.get(0).getMessages());
        assertEquals(0, captured.get(0).getMessages().size());
    }

    @Test
    void identity_mentionsEndpointAndModel() {
        assertEquals("KernelOllamaModel(http://stub/v1/qwen:7b)",
                model(stub("ok")).identity());
    }
}
