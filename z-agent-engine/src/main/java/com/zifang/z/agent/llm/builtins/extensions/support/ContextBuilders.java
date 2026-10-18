package com.zifang.z.agent.llm.builtins.extensions.support;

import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for context builders — provides common patterns for message insertion.
 */
public class ContextBuilders {

    private ContextBuilders() {
    }

    /**
     * Insert a block of text after the system message (or at the beginning if no system message).
     */
    public static List<AgentMessage> insertAfterSystem(List<AgentMessage> messages, String block, String position) {
        if (messages == null || messages.isEmpty()) {
            List<AgentMessage> result = new ArrayList<>();
            result.add(AgentMessage.system(block));
            return result;
        }

        List<AgentMessage> result = new ArrayList<>();
        boolean inserted = false;
        for (AgentMessage msg : messages) {
            result.add(msg);
            if (!inserted && "system".equals(msg.getRole()) && "after_system".equals(position)) {
                result.add(AgentMessage.system(block));
                inserted = true;
            }
        }

        if (!inserted && "after_system".equals(position)) {
            result.add(0, AgentMessage.system(block));
        }

        return result;
    }
}
