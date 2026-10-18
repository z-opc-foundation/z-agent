package com.zifang.z.agent.llm.builtins.extensions.compaction;

import com.zifang.z.agent.llm.builtins.extensions.session.ArchivistRunner;

/**
 * Configuration options for the compaction extension.
 */
public class CompactionOptions {

    private final boolean enabled;
    private final ArchivistRunner archivist;
    private final int keepRecentToolRounds;
    private final int maxVisibleMessages;
    private final int maxVisibleChars;
    private final int messageCapHysteresis;
    private final CompactionListener listener;

    private CompactionOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.archivist = builder.archivist;
        this.keepRecentToolRounds = builder.keepRecentToolRounds;
        this.maxVisibleMessages = builder.maxVisibleMessages;
        this.maxVisibleChars = builder.maxVisibleChars;
        this.messageCapHysteresis = builder.messageCapHysteresis;
        this.listener = builder.listener;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ArchivistRunner getArchivist() {
        return archivist;
    }

    public int getKeepRecentToolRounds() {
        return keepRecentToolRounds;
    }

    public int getMaxVisibleMessages() {
        return maxVisibleMessages;
    }

    public int getMaxVisibleChars() {
        return maxVisibleChars;
    }

    public int getMessageCapHysteresis() {
        return messageCapHysteresis;
    }

    public CompactionListener getListener() {
        return listener;
    }

    public static class Builder {
        private boolean enabled = true;
        private ArchivistRunner archivist;
        private int keepRecentToolRounds = 5;
        private int maxVisibleMessages = 20;
        private int maxVisibleChars = 32000;
        private int messageCapHysteresis = 5;
        private CompactionListener listener;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder archivist(ArchivistRunner archivist) {
            this.archivist = archivist;
            return this;
        }

        public Builder keepRecentToolRounds(int keepRecentToolRounds) {
            this.keepRecentToolRounds = keepRecentToolRounds;
            return this;
        }

        public Builder maxVisibleMessages(int maxVisibleMessages) {
            this.maxVisibleMessages = maxVisibleMessages;
            return this;
        }

        public Builder maxVisibleChars(int maxVisibleChars) {
            this.maxVisibleChars = maxVisibleChars;
            return this;
        }

        public Builder messageCapHysteresis(int messageCapHysteresis) {
            this.messageCapHysteresis = messageCapHysteresis;
            return this;
        }

        public Builder listener(CompactionListener listener) {
            this.listener = listener;
            return this;
        }

        public CompactionOptions build() {
            return new CompactionOptions(this);
        }
    }
}
