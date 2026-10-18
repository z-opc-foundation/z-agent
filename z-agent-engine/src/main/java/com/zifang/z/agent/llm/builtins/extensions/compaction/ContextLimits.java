package com.zifang.z.agent.llm.builtins.extensions.compaction;

/**
 * Context limits configuration for message compaction.
 */
public class ContextLimits {

    private final int keepRecentToolRounds;
    private final int maxVisibleMessages;
    private final int messageCapHysteresis;
    private final int maxVisibleChars;

    public ContextLimits(int keepRecentToolRounds, int maxVisibleMessages,
                         int messageCapHysteresis, int maxVisibleChars) {
        this.keepRecentToolRounds = keepRecentToolRounds;
        this.maxVisibleMessages = maxVisibleMessages;
        this.messageCapHysteresis = messageCapHysteresis;
        this.maxVisibleChars = maxVisibleChars;
    }

    public int getKeepRecentToolRounds() {
        return keepRecentToolRounds;
    }

    public int getMaxVisibleMessages() {
        return maxVisibleMessages;
    }

    public int getMessageCapHysteresis() {
        return messageCapHysteresis;
    }

    public int getMaxVisibleChars() {
        return maxVisibleChars;
    }
}
