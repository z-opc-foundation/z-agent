package com.zifang.z.agent.common.service;

import com.zifang.z.agent.common.entity.AgentApp;

import java.util.List;

/**
 * AgentApp 业务服务接口（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）
 * 注意：不含 IPage/Dto 等依赖，保持轻量级
 */
public interface AgentAppService {

    /**
     * 根据 appCode 查询应用
     */
    AgentApp getByAppCode(String appCode);

    /**
     * 创建应用
     */
    AgentApp create(AgentApp app);

    /**
     * 更新应用
     */
    AgentApp update(AgentApp app);

    /**
     * 删除应用
     */
    void remove(Long id);

    /**
     * 发布应用
     */
    void publish(String appCode);

    /**
     * 查询版本列表
     */
    List<com.zifang.z.agent.common.entity.AgentAppVersion> listVersions(String appCode);

    /**
     * 创建版本
     */
    com.zifang.z.agent.common.entity.AgentAppVersion createVersion(com.zifang.z.agent.common.entity.AgentAppVersion version);

    /**
     * 获取草稿
     */
    String getDraft(String appCode);

    /**
     * 保存草稿
     */
    void saveDraft(String appCode, String draftData);

    /**
     * 增加访问计数
     */
    void incrementVisitCount(String instanceCode);
}