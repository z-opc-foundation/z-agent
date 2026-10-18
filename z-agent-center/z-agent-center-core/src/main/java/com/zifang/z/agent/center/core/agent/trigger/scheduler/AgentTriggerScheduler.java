package com.zifang.z.agent.center.core.agent.trigger.scheduler;

import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;
import com.zifang.z.agent.center.core.agent.trigger.mapper.AgentTriggerMapper;
import com.zifang.z.agent.center.core.agent.trigger.service.impl.AgentTriggerServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FEATURE013 T3.1: Agent 定时触发调度器
 *
 * <p>每分钟扫表 (z_agent_trigger WHERE status='ACTIVE' AND next_fire_time <= now),
 * 对每条到期触发执行一次 Agent 调用, 记录 last_fire_* 字段, 重新计算 next_fire_time.
 *
 * <p>设计取舍:
 * <ul>
 *     <li>不走 z-schedule (Quartz), 自建轻量级轮询 — 简单可控, 业务字段直读</li>
 *     <li>1 分钟粒度 (实际可改 cron "0 * * * * ?")</li>
 *     <li>失败降级: 单条 trigger 失败不影响其他</li>
 *     <li>支持 Quartz 兼容 cron 表达式 (6-7 字段)</li>
 * </ul>
 */
@Component
public class AgentTriggerScheduler {

    private static final Logger log = LogManager.getLogger(AgentTriggerScheduler.class);

    @Resource
    private AgentTriggerMapper triggerMapper;

    @Resource
    private AgentTriggerServiceImpl triggerService;

    /**
     * 每分钟执行一次: 扫描到期的 trigger, 调用 Agent, 记录结果, 重排 next_fire_time.
     * fixedRate=60000ms = 1 分钟.
     */
    @Scheduled(fixedRate = 60000L, initialDelay = 30000L)
    public void scanAndFire() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AgentTrigger> dueList = triggerMapper.selectDueTriggers(now);
            if (dueList == null || dueList.isEmpty()) {
                return;
            }
            log.info("[AgentTriggerScheduler] 扫描到 {} 条到期 trigger @{}", dueList.size(), now);
            for (AgentTrigger t : dueList) {
                try {
                    String instanceCode = t.getInstanceCode();
                    if (instanceCode == null || instanceCode.isEmpty()) {
                        // 找应用下任一 ACTIVE 实例 - 通过 instanceService.findByOwner 之类可能更灵活
                        // 简单起见: 找 appCode 下第一个 ACTIVE 实例
                        instanceCode = findFirstActiveInstance(t.getAppCode());
                    }
                    if (instanceCode == null) {
                        log.warn("[AgentTriggerScheduler] trigger={} app={} 无可用实例, 跳过",
                                t.getTriggerCode(), t.getAppCode());
                        triggerMapper.recordFireResult(t.getId(), now, "FAILED",
                                "应用 " + t.getAppCode() + " 下无 ACTIVE 实例");
                        continue;
                    }
                    triggerService.executeTrigger(t, instanceCode, now);
                } catch (Exception e) {
                    log.warn("[AgentTriggerScheduler] trigger={} 执行异常: {}",
                            t.getTriggerCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[AgentTriggerScheduler] 扫描异常: {}", e.getMessage());
        }
    }

    private String findFirstActiveInstance(String appCode) {
        try {
            // 调用 instanceService.listAllActive() 找匹配 app_code 的
            // 简单做法: 通过 BaseMapper 查
            return triggerMapper.selectActiveTriggers().stream()
                    .filter(t -> appCode.equals(t.getAppCode()))
                    .map(AgentTrigger::getInstanceCode)
                    .filter(s -> s != null && !s.isEmpty())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
