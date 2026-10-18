package com.zifang.z.agent.center.core.agent.humaninput.service;

import com.zifang.z.agent.center.core.agent.humaninput.entity.AgentFlowHumanInput;

import java.util.List;

/**
 * FEATURE013 T3.5: Human-in-the-Loop Service
 */
public interface AgentFlowHumanInputService {

    AgentFlowHumanInput getById(Long id);

    AgentFlowHumanInput getByCode(String requestCode);

    /**
     * 创建一条 PENDING 请求 (Flow 中 HumanInputNode 调用).
     */
    AgentFlowHumanInput createPending(AgentFlowHumanInput request);

    /**
     * 收件箱: 列出某用户的待处理请求 (无 assignee 时列出全部).
     */
    List<AgentFlowHumanInput> listPending(String assignee, int limit);

    /**
     * 列出某 Flow 执行的所有请求 (含已决策的, 用于流程回溯).
     */
    List<AgentFlowHumanInput> listByExecution(String executionId);

    /**
     * 决策: 通过 / 拒绝 / 取消
     */
    AgentFlowHumanInput decide(Long id, String status, String decidedBy, String resultJson, String resultText, String comments);
}
