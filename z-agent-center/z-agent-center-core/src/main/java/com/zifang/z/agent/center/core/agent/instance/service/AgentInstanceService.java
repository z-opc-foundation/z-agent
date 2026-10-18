package com.zifang.z.agent.center.core.agent.instance.service;

import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceDto;
import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceReq;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;

import java.util.List;

public interface AgentInstanceService {

    AgentInstanceDto createResp(AgentInstanceReq req);

    AgentInstanceDto getRespByInstanceCode(String instanceCode);

    List<AgentInstanceDto> listRespByOwner(String ownerId);

    AgentInstance getByInstanceCode(String instanceCode);

    AgentInstance createFromApp(String appCode, String userId, String userName);

    /**
     * FEATURE014 — 创建多类型 Agent 实例（外部 Agent 接入入口）。
     *
     * @param appCode       所属应用编码
     * @param userId        所有者 ID
     * @param userName      所有者名称
     * @param agentTypeCode AgentType.name()（ZBOT / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM 等）
     * @param callbackUrl   外部 Agent 回调地址（HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE 必填）
     * @return 创建后的实例（agentType / callbackUrl / skillRootPath / authToken 已落库）
     */
    AgentInstance createExternalAgent(String appCode, String userId, String userName,
                                      String agentTypeCode, String callbackUrl);

    /**
     * 直接保存一个 AgentInstance 实体（公开分享初始化"虚拟"实例时使用）。
     * 继承自 ServiceImpl.save，因此需要在接口层显式声明。
     */
    boolean save(AgentInstance instance);

    List<AgentInstance> listByOwner(String ownerId);

    /**
     * FEATURE014 Wishlist 2 — 全量列出所有 ACTIVE 实例（用于定时 reconcile）。
     */
    java.util.List<AgentInstance> listAllActive();

    void updateStatus(String instanceCode, String status);

    void recordVisit(String instanceCode);

    /**
     * FEATURE013 T2.2: 按 widget_api_key 查 Agent 实例. 用于 widget 嵌入式调用认证.
     *
     * @param widgetApiKey 客户端传入的 widget API Key
     * @return 命中的实例, 未找到返回 null
     */
    AgentInstance getByWidgetApiKey(String widgetApiKey);
}
