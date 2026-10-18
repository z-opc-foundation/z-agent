package com.zifang.z.agent.engine.agent.llm.provider;


import com.zifang.z.agent.engine.agent.llm.model.define.Model;

public class LlmCallerFactory {

    public static String LLM_CALLER_TYPE_OLLAMA = "ollama";

    public static Model create(String llmCallerTypeOllama, LlmCallerConfig LlmCallerConfig) {
        if (llmCallerTypeOllama == null || llmCallerTypeOllama.isEmpty()) {
            throw new RuntimeException("不存在llm调用类型");
        }

        if (llmCallerTypeOllama.equalsIgnoreCase(LLM_CALLER_TYPE_OLLAMA)) {
            OllamaModel llmCaller = new OllamaModel();
            llmCaller.init(LlmCallerConfig);
            return llmCaller;
        }

        throw new RuntimeException("not support");
    }
}
