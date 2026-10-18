package com.zifang.z.agent.center.core.agent.trigger.service;

import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;

import java.util.List;

/**
 * FEATURE013 T3.1: Agent 定时触发 Service
 */
public interface AgentTriggerService {

    AgentTrigger getById(Long id);

    AgentTrigger getByCode(String triggerCode);

    List<AgentTrigger> pageList(String keyword, String appCode, String status, int page, int size);

    long count(String keyword, String appCode, String status);

    AgentTrigger create(AgentTrigger trigger);

    AgentTrigger update(AgentTrigger trigger);

    boolean delete(Long id);

    AgentTrigger setStatus(Long id, String status);

    /**
     * 手动触发 (忽略 cron, 立即执行一次)
     */
    String fireNow(Long id);

    /**
     * 计算下次触发时间 (基于 cron 表达式)
     *
     * @return null if cron 解析失败
     */
    java.time.LocalDateTime computeNextFireTime(String cron, java.time.LocalDateTime from);
}
