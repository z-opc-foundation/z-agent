package com.zifang.z.agent.center.core.agent.template.service;

import com.zifang.z.agent.center.core.agent.template.entity.AgentTemplate;

import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T3.3: Agent 模板市场 Service
 */
public interface AgentTemplateService {

    AgentTemplate getById(Long id);

    AgentTemplate getByCode(String templateCode);

    List<AgentTemplate> pageList(String keyword, String category, String status,
                                 Integer isOfficial, int page, int size);

    long count(String keyword, String category, String status, Integer isOfficial);

    /**
     * 热门模板 (按 download_count + rating 排序)
     */
    List<AgentTemplate> listHot(int limit);

    AgentTemplate create(AgentTemplate template);

    AgentTemplate update(AgentTemplate template);

    boolean delete(Long id);

    AgentTemplate setStatus(Long id, String status);

    /**
     * 一键 fork: 用 config_snapshot 创建新 AgentApp, 返回新 appCode.
     * 调用方拿到 appCode 后可继续调用 instanceService.createFromApp 创建实例.
     */
    Map<String, Object> fork(Long templateId, String newAppName, String userId, String userName);
}
