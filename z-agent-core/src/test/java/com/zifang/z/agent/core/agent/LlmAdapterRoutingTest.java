package com.zifang.z.agent.core.agent;

import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.llm.Model;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link LlmAdapter#resolve} 的路由必须是<b>确定的、可配置的</b>。
 *
 * <p>两处缺陷：
 * <ol>
 *   <li>{@link AgentProperties#getProviders()} 那张「模型 → provider bean 名」映射表
 *       <b>零消费</b>——类 Javadoc 把 {@code supportsModel} 扫描写成唯一机制，
 *       映射表只在字段/getter/setter 里存在。实测：配了
 *       {@code providers.gpt-4o-mini=openai}，只注册 {@code other-vendor} 时
 *       {@code resolve} 照样返回 {@code other-vendor}。</li>
 *   <li>回落扫描遍历 {@code ConcurrentHashMap.values()}，迭代序由 <b>bean 名的
 *       {@code String.hashCode()}</b> 决定，与注册顺序无关、任何配置都改不动。
 *       实测：注册 {@code b,a} 与 {@code a,b} 都由 {@code a} 胜出；
 *       {@code m1,m2} 由 {@code m1}；{@code azure-openai,openai} 由 {@code openai}。
 *       两个 provider 同时 {@code supportsModel} 同一 model（OpenAI 兼容代理与官方
 *       openai 共存是常态）时，流量可能被静默劫持，且无任何日志。</li>
 * </ol>
 */
public class LlmAdapterRoutingTest {

    /** 固定支持某一个 model 的假 provider。 */
    private static LlmProvider supportsOnly(String name, final String... models) {
        final List<String> ids = Arrays.asList(models);
        return new LlmProvider() {
            @Override public String name() { return name; }
            @Override public List<Model> listModels() {
                List<Model> out = new ArrayList<Model>();
                for (String id : ids) {
                    out.add(new Model(id, id, name,
                            Collections.<Model.Capability>emptyList(), 128000L, 16384L));
                }
                return out;
            }
            @Override public boolean supportsModel(String modelId) {
                return modelId != null && ids.contains(modelId);
            }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest r) { return null; }
            @Override public void streamChat(ChatCompletionsRequest r,
                                             Consumer<ChatCompletionsResponse> c,
                                             Consumer<Throwable> e) { }
        };
    }

    // ==================================================================
    // ① 显式映射表必须真的生效
    // ==================================================================

    @Test
    public void explicitProviderMapIsHonoured() {
        AgentProperties props = new AgentProperties();
        props.getProviders().put("gpt-4o-mini", "azure");

        LlmAdapter adapter = new LlmAdapter(props);
        adapter.registerProvider("openai", supportsOnly("openai", "gpt-4o-mini"));
        adapter.registerProvider("azure", supportsOnly("azure", "gpt-4o-mini"));

        // 两个都支持同一 model，映射表说走 azure
        assertEquals("z.agent.providers 的显式映射必须优先于 supportsModel 扫描", "azure", adapter.resolve("gpt-4o-mini").name());
    }

    @Test
    public void explicitMapBeatsRegistrationOrder() {
        // 映射指定的 provider 即使是**后**注册的也必须胜出
        AgentProperties props = new AgentProperties();
        props.getProviders().put("gpt-4o-mini", "second");

        LlmAdapter adapter = new LlmAdapter(props);
        adapter.registerProvider("first", supportsOnly("first", "gpt-4o-mini"));
        adapter.registerProvider("second", supportsOnly("second", "gpt-4o-mini"));

        assertEquals("显式映射应压过注册顺序", "second", adapter.resolve("gpt-4o-mini").name());
    }

    @Test
    public void mapPointingAtUnregisteredProviderFailsFast() {
        AgentProperties props = new AgentProperties();
        props.getProviders().put("gpt-4o-mini", "azure");   // 但只注册了别的

        LlmAdapter adapter = new LlmAdapter(props);
        adapter.registerProvider("other-vendor", supportsOnly("other-vendor", "gpt-4o-mini"));

        try {
            LlmProvider got = adapter.resolve("gpt-4o-mini");
            fail("映射表被静默忽略，实际路由到了: " + got.name()
                    + " —— 运维配的 z.agent.providers 完全没生效");
        } catch (AgentException expected) {
            assertTrue("错误消息应点名映射的 bean 与已注册列表，实际: " + expected.getMessage(),
                    expected.getMessage().contains("azure")
                            && expected.getMessage().contains("other-vendor"));
        }
    }

    @Test
    public void emptyOrBlankMapEntryFallsBackToScan() {
        AgentProperties props = new AgentProperties();
        props.getProviders().put("gpt-4o-mini", "   ");     // 空白值不算配置

        LlmAdapter adapter = new LlmAdapter(props);
        adapter.registerProvider("openai", supportsOnly("openai", "gpt-4o-mini"));

        assertEquals("空白映射值应回落扫描，而不是查一个名为空串的 bean", "openai", adapter.resolve("gpt-4o-mini").name());
    }

    // ==================================================================
    // ② 回落扫描必须按注册顺序，不受 bean 名影响
    // ==================================================================

    @Test
    public void fallbackFollowsRegistrationOrderNotBeanName() {
        // 这条是本次改动的判别式：bean 名 "a" 的 String.hashCode() 恰好排在 "b" 之前，
        // 所以修复前无论注册顺序如何都返回 "a"（探针实测：b,a → a；a,b → a）。
        // 修复后按注册顺序走，于是 b,a 必须返回 "b" —— 与修复前相反，红→绿。
        LlmAdapter bFirst = new LlmAdapter(new AgentProperties());
        bFirst.registerProvider("b", supportsOnly("b", "gpt-4o-mini"));
        bFirst.registerProvider("a", supportsOnly("a", "gpt-4o-mini"));
        assertEquals("b 先注册就该由 b 服务，实际取了 "
                        + bFirst.resolve("gpt-4o-mini").name()
                        + " —— 赢家仍被 bean 名的 String.hashCode() 决定",
                "b", bFirst.resolve("gpt-4o-mini").name());

        LlmAdapter aFirst = new LlmAdapter(new AgentProperties());
        aFirst.registerProvider("a", supportsOnly("a", "gpt-4o-mini"));
        aFirst.registerProvider("b", supportsOnly("b", "gpt-4o-mini"));
        assertEquals("a 先注册就该由 a 服务", "a", aFirst.resolve("gpt-4o-mini").name());
    }

    @Test
    public void routingIsReproducibleAcrossNameSets() {
        // 同一注册顺序，换一组 bean 名，赢家必须还是第一个
        LlmAdapter x = new LlmAdapter(new AgentProperties());
        x.registerProvider("m1", supportsOnly("m1", "gpt-4o-mini"));
        x.registerProvider("m2", supportsOnly("m2", "gpt-4o-mini"));
        assertEquals("m1 先注册就该先胜出，bean 名不该影响谁赢",
                "m1", x.resolve("gpt-4o-mini").name());

        LlmAdapter y = new LlmAdapter(new AgentProperties());
        y.registerProvider("zzz-first", supportsOnly("zzz-first", "gpt-4o-mini"));
        y.registerProvider("aaa-second", supportsOnly("aaa-second", "gpt-4o-mini"));
        assertEquals("zzz-first", y.resolve("gpt-4o-mini").name());
    }

    @Test
    public void unknownModelStillThrows() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider("openai", supportsOnly("openai", "gpt-4o-mini"));
        try {
            adapter.resolve("nope");
            fail("未知 model 应报错");
        } catch (AgentException expected) {
            assertTrue(expected.getMessage().contains("nope"));
            assertTrue("错误消息应带上已注册列表便于排查，实际: " + expected.getMessage(),
                    expected.getMessage().contains("openai"));
        }
    }

    @Test
    public void reregisteringKeepsOriginalPosition() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider("a", supportsOnly("a", "gpt-4o-mini"));
        adapter.registerProvider("b", supportsOnly("b", "gpt-4o-mini"));
        adapter.registerProvider("a", supportsOnly("a", "gpt-4o-mini"));   // 重注册
        assertEquals("a", adapter.resolve("gpt-4o-mini").name());
        assertEquals("重注册不应让 provider 数量虚增", 2, adapter.providerCount());
    }

    // ==================================================================
    // ③ listModels 顺序也应是稳定的
    // ==================================================================

    @Test
    public void listModelsFollowsRegistrationOrder() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider("b", supportsOnly("b", "model-b"));
        adapter.registerProvider("a", supportsOnly("a", "model-a"));

        assertEquals("模型列表顺序应跟注册顺序一致，而不是 ConcurrentHashMap 的哈希序",
                Arrays.asList("model-b", "model-a"), adapter.listModels());
    }

    @Test
    public void noProvidersAtAllStillConstructsAndFails() {
        LlmAdapter adapter = new LlmAdapter(null);   // props 传 null 也不该 NPE
        assertEquals(0, adapter.providerCount());
        try {
            adapter.resolve("gpt-4o-mini");
            fail("没有 provider 时应报错");
        } catch (AgentException expected) {
            assertTrue(expected.getMessage().contains("gpt-4o-mini"));
        }
    }
}
