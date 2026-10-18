package com.zifang.z.agent.llm.builtins.extensions.support;

import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Transcript utilities — message transcript manipulation.
 */
public class Transcript {

    /**
     * Split transcript into system message and conversation messages.
     */
    public static MessageSplit splitTranscript(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new MessageSplit(null, messages != null ? new ArrayList<AgentMessage>(messages) : new ArrayList<AgentMessage>());
        }
        AgentMessage systemMessage = null;
        List<AgentMessage> conversation = new ArrayList<>();
        for (AgentMessage msg : messages) {
            if ("system".equals(msg.getRole()) && systemMessage == null) {
                systemMessage = msg;
            } else {
                conversation.add(msg);
            }
        }
        return new MessageSplit(systemMessage, conversation);
    }

    /**
     * Result of splitting a transcript.
     */
    public static class MessageSplit {
        private final AgentMessage systemMessage;
        private final List<AgentMessage> conversationMessages;

        public MessageSplit(AgentMessage systemMessage, List<AgentMessage> conversationMessages) {
            this.systemMessage = systemMessage;
            this.conversationMessages = conversationMessages;
        }

        public AgentMessage getSystemMessage() {
            return systemMessage;
        }

        public List<AgentMessage> getConversationMessages() {
            return conversationMessages;
        }
    }
}
