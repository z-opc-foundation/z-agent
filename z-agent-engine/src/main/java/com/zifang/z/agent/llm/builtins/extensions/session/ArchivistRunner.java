package com.zifang.z.agent.llm.builtins.extensions.session;

/**
 * Archivist runner — manages session history archiving and summarization.
 */
public interface ArchivistRunner {

    /**
     * Summarize the given instruction/text.
     */
    String summarize(String instruction);
}
