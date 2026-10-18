package com.zifang.z.agent.engine.agent.react;

import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Lightweight Agent contract.
 * <p>
 * Four methods, all that an agent should expose:
 * - name()      : identifier
 * - stream(...) : primary reactive API — emits a sequence of {@link StreamEvent}
 * - stop()      : cancel an in-flight run
 * - reset()     : clear session state
 * <p>
 * The legacy blocking helpers (run, runWithHistory, getMemory, ...) live on
 * {@link ReActAgent} directly. They are convenience adapters over stream().
 */
public interface Agent {

    String name();

    /**
     * Drive a ReAct execution for {@code input}. The returned Flux is cold;
     * subscribe to start the agent.
     */
    Flux<StreamEvent> stream(String input);

    void stop();

    void reset();

    default boolean isRunning() {
        return false;
    }

    // ===== Convenience adapters (default) =====

    /**
     * Block until the final answer is fully assembled.
     */
    default String run(String input) {
        return stream(input)
                .ofType(StreamEvent.FinalDelta.class)
                .map(e -> e.text)
                .reduce("", (a, b) -> a + b)
                .block(Duration.ofMinutes(5));
    }

    /**
     * Same as {@link #run(String)} but also returns the full event log.
     */
    default java.util.List<StreamEvent> runWithHistory(String input) {
        return stream(input).collectList().block(Duration.ofMinutes(5));
    }
}
