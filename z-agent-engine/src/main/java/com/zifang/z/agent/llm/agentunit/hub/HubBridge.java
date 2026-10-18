package com.zifang.z.agent.llm.agentunit.hub;

import com.zifang.z.agent.llm.infrastructure.events.Message;
import com.zifang.z.agent.llm.infrastructure.messaging.payload.UserInputPayload;

/**
 * Hub bridge — connects agent units to the messaging hub for inter-agent communication.
 */
public class HubBridge {

    /**
     * Create a Message from text and an optional payload.
     */
    public static Message messageFromTextAndPayload(String text, UserInputPayload payload) {
        return Message.user(text);
    }

    public void send(Object message) {
        // Stub: route message to hub
    }

    public void onReceive(Object handler) {
        // Stub: register message handler
    }
}
