package com.zifang.z.agent.center.core.agent.textgen.service;

import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenResult;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenTask;

import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T3.4: Agent 文本生成器 Service
 */
public interface AgentTextGenService {

    AgentTextGenTask getById(Long id);

    AgentTextGenTask getByCode(String taskCode);

    List<AgentTextGenTask> pageList(String keyword, String appCode, String status, int page, int size);

    long count(String keyword, String appCode, String status);

    /**
     * 创建任务 (不立即执行)
     */
    AgentTextGenTask create(AgentTextGenTask task);

    /**
     * 删除任务
     */
    boolean delete(Long id);

    /**
     * 同步执行任务: 逐行调 Agent, 写结果, 完成后回填 output_csv.
     */
    AgentTextGenTask run(Long taskId);

    /**
     * 取某任务的所有行结果
     */
    List<AgentTextGenResult> listResults(Long taskId);

    /**
     * 一键创建 + 执行 (用于 demo / 小批量)
     */
    Map<String, Object> createAndRun(String taskName, String appCode, String promptTemplate,
                                     List<Map<String, Object>> rows, String ownerId, String ownerName);
}
