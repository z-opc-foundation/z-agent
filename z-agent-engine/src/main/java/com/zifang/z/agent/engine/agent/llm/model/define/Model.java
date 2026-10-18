package com.zifang.z.agent.engine.agent.llm.model.define;

import com.zifang.z.agent.engine.agent.llm.tool.core.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 定义各种模型的标准行为
 */
public interface Model {


    Flux<ModelResponse> stream(ModelRequest modelRequest);

    /**
     * 与模型流式对话的核心方法
     * note : 优先使用流式，可以使用block转换为非流式
     */
    Flux<ModelResponse> stream(List<ModelMessage> messages, List<ToolSchema> tools, ModelOptions options);

    /**
     * 给出模型的唯一身份信息
     */
    String identity();


    default List<ModelMessage> adapter() {
        return new ArrayList<>();
    }

    ;

    String generate(List<ModelMessage> modelMessages);

    String chat(List<ModelMessage> modelMessages);


}
