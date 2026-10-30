package com.zifang.z.agent.center.core.agent.trigger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.time.DateUtil;
import com.zifang.z.agent.center.core.agent.conversation.entity.AgentConversation;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.runtime.service.AgentRuntimeService;
import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;
import com.zifang.z.agent.center.core.agent.trigger.mapper.AgentTriggerMapper;
import com.zifang.z.agent.center.core.agent.trigger.service.AgentTriggerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class AgentTriggerServiceImpl implements AgentTriggerService {

    private static final Logger log = LogManager.getLogger(AgentTriggerServiceImpl.class);

    @Resource
    private AgentTriggerMapper triggerMapper;

    @Resource
    private AgentInstanceService instanceService;

    @Resource
    private AgentRuntimeService agentRuntimeService;

    @Override
    public AgentTrigger getById(Long id) {
        if (id == null) {
            return null;
        }

        return triggerMapper.selectById(id);
    }

    @Override
    public AgentTrigger getByCode(String triggerCode) {
        if (triggerCode == null || triggerCode.isEmpty()) {
            return null;
        }
        return triggerMapper.selectOne(new LambdaQueryWrapper<AgentTrigger>()
                .eq(AgentTrigger::getTriggerCode, triggerCode)
                .eq(AgentTrigger::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public List<AgentTrigger> pageList(String keyword, String appCode, String status, int page, int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1 || size > 200) {
            size = 20;
        }

        LambdaQueryWrapper<AgentTrigger> q = new LambdaQueryWrapper<>();
        q.eq(AgentTrigger::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTrigger::getTriggerCode, kw)
                    .or().like(AgentTrigger::getTriggerName, kw));
        }
        if (appCode != null && !appCode.trim().isEmpty()) {
            q.eq(AgentTrigger::getAppCode, appCode.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentTrigger::getStatus, status.trim());
        }
        q.orderByDesc(AgentTrigger::getId);
        Page<AgentTrigger> p = triggerMapper.selectPage(new Page<>(page, size), q);
        return p.getRecords();
    }

    @Override
    public long count(String keyword, String appCode, String status) {
        LambdaQueryWrapper<AgentTrigger> q = new LambdaQueryWrapper<>();
        q.eq(AgentTrigger::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTrigger::getTriggerCode, kw)
                    .or().like(AgentTrigger::getTriggerName, kw));
        }
        if (appCode != null && !appCode.trim().isEmpty()) {
            q.eq(AgentTrigger::getAppCode, appCode.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentTrigger::getStatus, status.trim());
        }
        return triggerMapper.selectCount(q);
    }

    @Override
    public AgentTrigger create(AgentTrigger trigger) {
        if (trigger == null || trigger.getTriggerCode() == null) {
            throw new IllegalArgumentException("triggerCode 必填");
        }
        if (getByCode(trigger.getTriggerCode()) != null) {
            throw new IllegalStateException("triggerCode 已存在: " + trigger.getTriggerCode());
        }
        if (trigger.getStatus() == null) trigger.setStatus("ACTIVE");

        if (trigger.getFireCount() == null) trigger.setFireCount(0);

        if (trigger.getCron() != null && !trigger.getCron().isEmpty()) {
            trigger.setNextFireTime(computeNextFireTime(trigger.getCron(), LocalDateTime.now()));
        }
        trigger.setIsDeleted(0);
        trigger.setGmtCreate(LocalDateTime.now());
        trigger.setGmtModified(LocalDateTime.now());
        triggerMapper.insert(trigger);
        return trigger;
    }

    @Override
    public AgentTrigger update(AgentTrigger trigger) {
        if (trigger == null || trigger.getId() == null) {
            throw new IllegalArgumentException("id 必填");
        }
        AgentTrigger old = triggerMapper.selectById(trigger.getId());
        if (old == null) {
            throw new IllegalStateException("Trigger 不存在");
        }

        trigger.setTriggerCode(old.getTriggerCode()); // code 不可改
        trigger.setIsDeleted(old.getIsDeleted());
        trigger.setFireCount(old.getFireCount());
        trigger.setGmtModified(LocalDateTime.now());
        // 重新计算 next_fire_time
        if (trigger.getCron() != null && !trigger.getCron().isEmpty()) {
            trigger.setNextFireTime(computeNextFireTime(trigger.getCron(), LocalDateTime.now()));
        }
        triggerMapper.updateById(trigger);
        return triggerMapper.selectById(trigger.getId());
    }

    @Override
    public boolean delete(Long id) {
        AgentTrigger t = triggerMapper.selectById(id);
        if (t == null) {
            return false;
        }

        t.setIsDeleted(1);
        t.setGmtModified(LocalDateTime.now());
        return triggerMapper.updateById(t) > 0;
    }

    @Override
    public AgentTrigger setStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"PAUSED".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("status 必须是 ACTIVE/PAUSED/DISABLED");
        }
        AgentTrigger t = triggerMapper.selectById(id);
        if (t == null) {
            throw new IllegalStateException("Trigger 不存在");
        }

        t.setStatus(status);
        // 恢复 ACTIVE 时重算 next_fire_time
        if ("ACTIVE".equals(status) && t.getCron() != null) {
            t.setNextFireTime(computeNextFireTime(t.getCron(), LocalDateTime.now()));
        }
        if ("PAUSED".equals(status) || "DISABLED".equals(status)) {
            t.setNextFireTime(null);
        }
        t.setGmtModified(LocalDateTime.now());
        triggerMapper.updateById(t);
        return t;
    }

    @Override
    public String fireNow(Long id) {
        AgentTrigger t = triggerMapper.selectById(id);
        if (t == null) {
            throw new IllegalStateException("Trigger 不存在");
        }

        String instanceCode = t.getInstanceCode();
        if (instanceCode == null || instanceCode.isEmpty()) {
            // 找应用下任一 ACTIVE 实例
            AgentInstance inst = instanceService.getByInstanceCode(t.getAppCode());
            if (inst == null) {
                throw new IllegalStateException("应用 " + t.getAppCode() + " 下无可用实例");
            }
            instanceCode = inst.getInstanceCode();
        }
        return executeTrigger(t, instanceCode, LocalDateTime.now());
    }

    @Override
    public LocalDateTime computeNextFireTime(String cronExpr, LocalDateTime from) {
        if (cronExpr == null || cronExpr.trim().isEmpty()) {
            return null;
        }
        try {
            CronExpression cron = CronExpression.parse(cronExpr.trim());
            LocalDateTime next = cron.next(from);
            return next;
        } catch (Exception e) {
            log.warn("[AgentTrigger] cron 表达式解析失败: {} err={}", cronExpr, e.getMessage());
            return null;
        }
    }

    /**
     * 内部: 执行一次触发, 调 AgentRuntimeService
     */
    public String executeTrigger(AgentTrigger trigger, String instanceCode, LocalDateTime fireTime) {
        String message = trigger.getMessage() == null ? "" : trigger.getMessage();
        // 占位符替换
        message = message.replace("${date}", DateUtil.format(new Date(), "yyyy-MM-dd"))
                .replace("${time}", DateUtil.format(new Date(), "HH:mm:ss"))
                .replace("${datetime}", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        try {
            String userId = "trigger_" + trigger.getTriggerCode();
            String userName = "定时触发 [" + trigger.getTriggerName() + "]";
            AgentConversation result = agentRuntimeService.chat(instanceCode, message, userId, userName);
            String answer = result == null ? "(空响应)" : result.getAssistantMessage();
            String status = "SUCCESS".equals(result == null ? null : result.getStatus()) ? "SUCCESS" : "FAILED";
            String detail = status + " | " + (answer == null ? "" : (answer.length() > 1000 ? answer.substring(0, 1000) : answer));
            triggerMapper.recordFireResult(trigger.getId(), fireTime, status, detail);
            // 重新计算下次触发时间
            if (trigger.getCron() != null && !trigger.getCron().isEmpty()) {
                triggerMapper.updateNextFireTime(trigger.getId(), computeNextFireTime(trigger.getCron(), LocalDateTime.now()));
            }
            log.info("[AgentTrigger] fire OK trigger={} instance={} answer_len={}", trigger.getTriggerCode(), instanceCode, answer == null ? 0 : answer.length());
            return answer;
        } catch (Exception e) {
            String err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            triggerMapper.recordFireResult(trigger.getId(), fireTime, "FAILED", err);
            log.warn("[AgentTrigger] fire FAIL trigger={}: {}", trigger.getTriggerCode(), err);
            throw new RuntimeException("触发失败: " + err, e);
        }
    }
}
