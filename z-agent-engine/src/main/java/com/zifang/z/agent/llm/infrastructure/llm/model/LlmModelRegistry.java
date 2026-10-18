package com.zifang.z.agent.llm.infrastructure.llm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for LLM models。
 *
 * <p>Phase 7 扩展：0815 业务侧使用 {@link #registerAll(List)} 批量注册与 {@link #listModels()}
 * 列举模型；0615 旧契约（{@link #register(LlmModel)} / {@link #get(String)} / 单例 {@link #getInstance()}）
 * 保持兼容。</p>
 */
public class LlmModelRegistry {

    private static final LlmModelRegistry INSTANCE = new LlmModelRegistry();
    private final Map<String, LlmModel> models = new ConcurrentHashMap<>();

    public static LlmModelRegistry getInstance() {
        return INSTANCE;
    }

    public void register(LlmModel model) {
        if (model != null && model.getCode() != null) {
            models.put(model.getCode(), model);
        }
    }

    /**
     * 批量注册：跳过 null 项，并按 code 去重覆盖。
     */
    public void registerAll(List<LlmModel> list) {
        if (list == null) {
            return;
        }
        for (LlmModel model : list) {
            register(model);
        }
    }

    public List<LlmModel> listModels() {
        return new ArrayList<>(models.values());
    }

    public List<LlmModel> getAll() {
        return Collections.unmodifiableList(listModels());
    }

    public LlmModel get(String code) {
        return models.get(code);
    }

    public boolean contains(String code) {
        return models.containsKey(code);
    }

    public void remove(String code) {
        models.remove(code);
    }
}
