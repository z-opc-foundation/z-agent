package com.zifang.z.agent.engine.agent.memory;

import com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage;

import java.util.ArrayList;
import java.util.List;

public class Memory {
    private static final int MAX_HISTORY = 10;
    private final List<ModelMessage> history = new ArrayList<>();

    public void add(ModelMessage msg) {
        history.add(msg);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

    }

    public List<ModelMessage> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }

}