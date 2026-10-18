package com.zifang.z.agent.engine.agent.stream;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StreamingReActAgentTest {
    @Test
    public void testStreamingText_eventConversion() throws Exception {
        // 测试 AgentEvent.StreamingText 能正确构造
        AgentEvent.StreamingText textEvent = new AgentEvent.StreamingText("hello", true, "thought");
        assertEquals("hello", textEvent.text);
        assertEquals("thought", textEvent.contentType);
        assertTrue(textEvent.isDelta);
        assertEquals(AgentEvent.Kind.STREAMING_TEXT, textEvent.kind());
    }

    @Test
    public void testAgentStatus_event() {
        AgentEvent.AgentStatus status = new AgentEvent.AgentStatus("RUNNING", "test started");
        assertEquals("RUNNING", status.status);
        assertEquals("test started", status.message);
        assertEquals(AgentEvent.Kind.AGENT_STATUS, status.kind());
    }

    @Test
    public void testMetadata_event() {
        Map<String, Object> data = new HashMap<>();
        data.put("tokens", 100);
        AgentEvent.Metadata meta = new AgentEvent.Metadata(data);
        assertEquals(100, meta.data.get("tokens"));
        assertEquals(AgentEvent.Kind.METADATA_EVENT, meta.kind());
    }

    @Test
    public void testStreamingUtils_toTextStream() {
        // 测试 filter + map
        Flux<AgentEvent> events = Flux.just(
                new AgentEvent.StreamingText("hello", true, "thought"),
                new AgentEvent.StreamingText(" world", true, "final"),
                new AgentEvent.AgentStatus("DONE", "finished")
        );
        List<String> texts = StreamingUtils.toTextStream(events).collectList().block();
        assertEquals(2, texts.size());
        assertEquals("hello", texts.get(0));
        assertEquals(" world", texts.get(1));
    }

    @Test
    public void testAgentEvent_timestamp() {
        AgentEvent.StreamingText e = new AgentEvent.StreamingText("test", true, "thought");
        assertNotNull(e.timestamp());
    }
}