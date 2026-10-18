package com.zifang.z.agent.engine.agent.plugin;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class LifecycleHookDispatcherTest {
    @Test
    public void testHooksCalledInOrder() {
        List<String> callOrder = new ArrayList<>();
        AgentLifecycleHook h1 = new AgentLifecycleHook() {
            @Override
            public void onStepStart(String s, int step) {
                callOrder.add("h1-" + step);
            }
        };
        AgentLifecycleHook h2 = new AgentLifecycleHook() {
            @Override
            public void onStepStart(String s, int step) {
                callOrder.add("h2-" + step);
            }
        };
        LifecycleHookDispatcher d = new LifecycleHookDispatcher(Arrays.asList(h1, h2));
        d.dispatchStepStart("s1", 1);
        d.dispatchStepStart("s1", 2);
        assertEquals(Arrays.asList("h1-1", "h2-1", "h1-2", "h2-2"), callOrder);
    }

    @Test
    public void testNoOps_doesNotThrow() {
        LifecycleHookDispatcher d = new LifecycleHookDispatcher(new ArrayList<>());
        d.dispatchCreated(new Object());
        d.dispatchStarted("s1", "hi");
        d.dispatchStepStart("s1", 1);
        d.dispatchFinished("s1", "SUCCESS", "answer");
        // no exception = pass
    }

    @Test
    public void testPluginRegistry_registerAndRetrieve() {
        PluginRegistry reg = PluginRegistry.getInstance();
        reg.clear();
        AgentPlugin p1 = new AgentPlugin() {
            public String name() {
                return "test-plugin-1";
            }

            public String version() {
                return "1.0";
            }
        };
        reg.register(p1);
        assertEquals(1, reg.getPlugins().size());
        assertEquals("test-plugin-1", reg.getPlugins().get(0).name());
        reg.unregister("test-plugin-1");
        assertTrue(reg.getPlugins().isEmpty());
    }
}