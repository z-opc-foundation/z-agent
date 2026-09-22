package com.zifang.z.agent.core.registry;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.kernel.agent.Agent;
import com.zifang.z.agent.kernel.agent.AgentRequest;
import com.zifang.z.agent.kernel.agent.AgentResponse;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AgentRegistryTest {

    private static Agent stubAgent(final String name) {
        return new Agent() {
            @Override public String getName() { return name; }
            @Override public AgentResponse run(AgentRequest req) {
                return new AgentResponse(Msg.assistant("stub:" + req.getInput().getContent()),
                        Collections.<AgentResponse.Step>emptyList(), TokenUsage.empty(),
                        "completed", null, null);
            }
            @Override public void streamRun(AgentRequest req,
                                            java.util.function.Consumer<AgentResponse.Step> onStep,
                                            java.util.function.Consumer<AgentResponse> onComplete,
                                            java.util.function.Consumer<Throwable> onError) {
                onComplete.accept(run(req));
            }
            @Override public void reset() {}
        };
    }

    private AgentDto stubDto(String name) {
        return AgentDto.builder().name(name).kind("react").description("test agent").build();
    }

    @Test
    public void register_then_get_returns_same_instance() {
        AgentRegistry reg = new AgentRegistry();
        Agent agent = stubAgent("a1");
        reg.register("a1", stubDto("a1"), agent);
        assertEquals(agent, reg.get("a1"));
        assertEquals("a1", reg.describe("a1").getName());
    }

    @Test
    public void list_returns_all() {
        AgentRegistry reg = new AgentRegistry();
        reg.register("a1", stubDto("a1"), stubAgent("a1"));
        reg.register("a2", stubDto("a2"), stubAgent("a2"));
        reg.register("a3", stubDto("a3"), stubAgent("a3"));
        assertEquals(3, reg.size());
        assertEquals(3, reg.list().size());
        assertTrue(reg.names().contains("a1"));
        assertTrue(reg.names().contains("a2"));
        assertTrue(reg.names().contains("a3"));
    }

    @Test
    public void unregister_removes_entry() {
        AgentRegistry reg = new AgentRegistry();
        reg.register("a1", stubDto("a1"), stubAgent("a1"));
        reg.unregister("a1");
        assertFalse(reg.has("a1"));
        assertEquals(0, reg.size());
    }

    @Test
    public void get_unknown_throws() {
        AgentRegistry reg = new AgentRegistry();
        try {
            reg.get("nope");
            fail("expected AgentException");
        } catch (AgentException e) {
            assertEquals(AgentException.NOT_FOUND, e.getCode());
            assertTrue(e.getMessage().contains("nope"));
        }
    }

    @Test
    public void register_rejects_null_dto() {
        AgentRegistry reg = new AgentRegistry();
        try {
            reg.register("a1", null, stubAgent("a1"));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void register_rejects_null_agent() {
        AgentRegistry reg = new AgentRegistry();
        try {
            reg.register("a1", stubDto("a1"), null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void register_rejects_empty_name() {
        AgentRegistry reg = new AgentRegistry();
        try {
            reg.register("", stubDto(""), stubAgent(""));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void describe_unknown_throws_not_found() {
        AgentRegistry reg = new AgentRegistry();
        try {
            reg.describe("ghost");
            fail("expected AgentException");
        } catch (AgentException e) {
            assertEquals(AgentException.NOT_FOUND, e.getCode());
        }
    }

    @Test
    public void list_initially_empty() {
        AgentRegistry reg = new AgentRegistry();
        assertEquals(0, reg.size());
        assertNotNull(reg.list());
        assertTrue(reg.list().isEmpty());
    }

    @Test
    public void register_overwrites_same_name() {
        AgentRegistry reg = new AgentRegistry();
        Agent a1 = stubAgent("a1");
        Agent a2 = stubAgent("a1-v2");
        reg.register("a1", stubDto("a1"), a1);
        reg.register("a1", stubDto("a1"), a2);
        assertEquals(1, reg.size());
        assertEquals(a2, reg.get("a1"));
    }
}