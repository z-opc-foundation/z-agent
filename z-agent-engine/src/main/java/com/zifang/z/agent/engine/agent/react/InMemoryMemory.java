package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * In-memory implementation of conversation memory for ReAct agent.
 * Stores messages in a rolling buffer with maximum capacity.
 */
public class InMemoryMemory {

    private static final int DEFAULT_MAX_SIZE = 100;

    private final int maxSize;
    private final LinkedList<Msg> messages;
    private final List<Msg> systemPrompt;

    public InMemoryMemory() {
        this(DEFAULT_MAX_SIZE);
    }

    public InMemoryMemory(int maxSize) {
        this.maxSize = maxSize;
        this.messages = new LinkedList<>();
        this.systemPrompt = new ArrayList<>();
    }

    /**
     * Create a new memory with system prompt.
     */
    public static InMemoryMemory createWithSystemPrompt(String systemPrompt) {
        InMemoryMemory memory = new InMemoryMemory();
        memory.addSystemPrompt(systemPrompt);
        return memory;
    }

    /**
     * Add a message to memory.
     */
    public void add(Msg msg) {
        if (msg == null) {
            return;
        }
        messages.addLast(msg);
        trimToSize();
    }

    /**
     * Add a system prompt message.
     */
    public void addSystemPrompt(String content) {
        if (content != null && !content.isEmpty()) {
            systemPrompt.add(Msg.system(content));
        }
    }

    /**
     * Get all messages including system prompt.
     */
    public List<Msg> getMessages() {
        List<Msg> result = new ArrayList<>(systemPrompt);
        result.addAll(messages);
        return result;
    }

    /**
     * Get only conversation messages (without system prompt).
     */
    public List<Msg> getConversationMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Get messages formatted for LLM.
     */
    public List<ChatMessage> toChatMessages() {
        List<ChatMessage> result = new ArrayList<>();

        for (Msg msg : systemPrompt) {
            result.add(convertToChatMessage(msg));
        }

        for (Msg msg : messages) {
            result.add(convertToChatMessage(msg));
        }

        return result;
    }

    private ChatMessage convertToChatMessage(Msg msg) {
        if (msg.getRole().equals(Msg.ROLE_TOOL)) {
            return ChatMessage.toolResult(
                    msg.getToolCallId(),
                    msg.getToolName(),
                    msg.getContent()
            );
        }
        return new ChatMessage(msg.getRole(), msg.getContent());
    }

    /**
     * Clear all conversation messages (keep system prompt).
     */
    public void clear() {
        messages.clear();
    }

    /**
     * Clear everything including system prompt.
     */
    public void clearAll() {
        messages.clear();
        systemPrompt.clear();
    }

    /**
     * Get the number of messages in memory.
     */
    public int size() {
        return messages.size();
    }

    /**
     * Check if memory is empty.
     */
    public boolean isEmpty() {
        return messages.isEmpty() && systemPrompt.isEmpty();
    }

    /**
     * Get the last N messages.
     */
    public List<Msg> getLastMessages(int n) {
        if (n <= 0) {
            return new ArrayList<>();
        }
        int size = messages.size();
        if (size <= n) {
            return new ArrayList<>(messages);
        }
        List<Msg> result = new ArrayList<>();
        List<Msg> all = new ArrayList<>(messages);
        for (int i = size - n; i < size; i++) {
            result.add(all.get(i));
        }
        return result;
    }

    private void trimToSize() {
        while (messages.size() > maxSize) {
            messages.removeFirst();
        }
    }
}