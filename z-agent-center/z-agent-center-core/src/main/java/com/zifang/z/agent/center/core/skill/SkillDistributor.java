package com.zifang.z.agent.center.core.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.skill.adapter.SkillAdapter;
import com.zifang.z.agent.center.core.skill.entity.SkillBinding;
import com.zifang.z.agent.center.core.skill.entity.SkillSyncLog;
import com.zifang.z.agent.center.core.skill.mapper.SkillBindingMapper;
import com.zifang.z.agent.center.core.skill.mapper.SkillSyncLogMapper;
import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 分发器 — FEATURE014 §3.3
 *
 * <p>负责把 SkillCenter 的内容分发到绑定实例，支持 PUSH / PULL 两种模式。
 * 所有同步行为都会写入 {@code z_agent_skill_sync_log} 便于审计与排查。</p>
 */
@Service
public class SkillDistributor extends ServiceImpl<SkillBindingMapper, SkillBinding> {

    private static final Logger log = LogManager.getLogger(SkillDistributor.class);

    @Resource
    private SkillAdapterFactory adapterFactory;

    @Resource
    private AgentInstanceService instanceService;

    @Resource
    private SkillSyncLogMapper syncLogMapper;

    @Resource
    private SkillContentFetcher contentFetcher;

    @Resource
    private com.zifang.z.agent.center.core.bot.hook.BotHookDispatcher hookDispatcher;

    /**
     * 绑定 Skill 到一个 Agent 实例，并立即触发同步。
     *
     * <p>对 ACTIVE 实例会立即拉取内容 + 写盘 + 通知 Agent（如果是 PUSH 模式）；
     * 对 ERROR/UNBOUND 实例只创建绑定记录，等下次 reconcile / heartbeat 时再分发。</p>
     *
     * @param instanceCode Agent 实例
     * @param skillCodes   要绑定的 Skill 列表
     * @param bindingType  PUSH 或 PULL
     * @return 实际创建的绑定数（跳过已存在的）
     */
    public int bind(String instanceCode, List<String> skillCodes, String bindingType) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new RuntimeException("Instance not found: " + instanceCode);
        }
        AgentType agentType = AgentType.parse(instance.getAgentType());

        int created = 0;
        for (String skillCode : skillCodes) {
            // 跳过已存在
            if (findBinding(instanceCode, skillCode) != null) {
                continue;
            }
            SkillBinding binding = new SkillBinding();
            binding.setInstanceCode(instanceCode);
            binding.setSkillCode(skillCode);
            binding.setBindingType(bindingType != null ? bindingType : "PUSH");
            binding.setStatus("BOUND");
            binding.setGmtBind(LocalDateTime.now());
            binding.setTenantCode("default");
            this.save(binding);
            created++;

            // ACTIVE 实例立即同步（ZBot 同进程、外部 Agent 在 PUSH 模式下也可推）
            if ("ACTIVE".equalsIgnoreCase(instance.getStatus())) {
                String content = contentFetcher.fetch(skillCode);
                if (content != null) {
                    distributeFromContent(instanceCode, skillCode, content, agentType);
                }
            }
        }
        log.info("Bound {} skills to instance {} (agentType={}, mode={})",
                created, instanceCode, agentType, bindingType);
        return created;
    }

    /**
     * 解绑 Skill。
     */
    public boolean unbind(String instanceCode, List<String> skillCodes) {
        // in() 传空集合会拼出 `IN ()`，MySQL 直接报语法错；这里挡在 SQL 生成之前。
        if (skillCodes == null || skillCodes.isEmpty()) {
            throw new IllegalArgumentException("skillCodes 不能为空");
        }
        LambdaQueryWrapper<SkillBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillBinding::getInstanceCode, instanceCode)
                .in(SkillBinding::getSkillCode, skillCodes);
        boolean removed = this.remove(wrapper);
        log.info("Unbound skills {} from instance {} (removed={})",
                skillCodes, instanceCode, removed);
        return removed;
    }

    /**
     * 查询一个实例的所有绑定。
     */
    public List<SkillBinding> listBindings(String instanceCode) {
        LambdaQueryWrapper<SkillBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillBinding::getInstanceCode, instanceCode)
                .orderByDesc(SkillBinding::getGmtBind);
        return this.list(wrapper);
    }

    /**
     * 把指定 Skill 内容分发到一个实例（由 Adapter 决定落盘路径）。
     *
     * <p>注意：本方法只处理"分发到本地"——SkillCenter 的内容获取是上层职责，
     * 调用方应当已经从 SkillCenter 拿到 content 字符串再传入。</p>
     */
    public boolean distribute(String instanceCode, String skillCode, String content) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            log.warn("distribute: instance not found: {}", instanceCode);
            return false;
        }
        return distributeFromContent(instanceCode, skillCode, content,
                AgentType.parse(instance.getAgentType()));
    }

    /**
     * distribute 的核心实现（agentType 已知，避免重复 parse）。
     */
    private boolean distributeFromContent(String instanceCode, String skillCode, String content, AgentType agentType) {
        // preSkill hook（Wishlist 5）
        java.util.Map<String, Object> preCtx = new java.util.HashMap<>();
        preCtx.put("instanceCode", instanceCode);
        preCtx.put("skillCode", skillCode);
        preCtx.put("agentType", agentType.name());
        hookDispatcher.fire("preSkill", preCtx);

        try {
            SkillAdapter adapter = adapterFactory.getAdapter(agentType);
            long t0 = System.currentTimeMillis();
            String path = adapter.writeSkill(instanceCode, skillCode, content);

            // PUSH 模式：通知 Agent 重载
            SkillBinding binding = findBinding(instanceCode, skillCode);
            AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
            if (binding != null && "PUSH".equalsIgnoreCase(binding.getBindingType())
                    && instance != null && instance.getCallbackUrl() != null) {
                adapter.notifyAgent(instance.getCallbackUrl(), Arrays.asList(skillCode));
            }
            // 更新 binding 状态 + gmtSync
            if (binding != null) {
                binding.setStatus("BOUND");
                binding.setGmtSync(LocalDateTime.now());
                binding.setErrorMsg(null);
                this.updateById(binding);
            }
            long ms = System.currentTimeMillis() - t0;
            logSync(instanceCode, skillCode, agentType, "INSTALL", "SUCCESS", ms, null);
            log.info("Distributed skill {} to {} via {} -> {} ({}ms)",
                    skillCode, instanceCode, agentType, path, ms);

            // postSkill hook — SUCCESS
            java.util.Map<String, Object> postCtx = new java.util.HashMap<>();
            postCtx.put("instanceCode", instanceCode);
            postCtx.put("skillCode", skillCode);
            postCtx.put("result", "SUCCESS");
            postCtx.put("durationMs", ms);
            postCtx.put("path", path);
            hookDispatcher.fire("postSkill", postCtx);
            return true;
        } catch (Exception e) {
            log.error("Failed to distribute skill {} to {}: {}",
                    skillCode, instanceCode, e.getMessage(), e);
            logSync(instanceCode, skillCode, agentType, "INSTALL", "FAIL",
                    0L, e.getMessage());
            SkillBinding binding = findBinding(instanceCode, skillCode);
            if (binding != null) {
                binding.setStatus("ERROR");
                binding.setErrorMsg(e.getMessage());
                this.updateById(binding);
            }
            // postSkill hook — FAIL
            java.util.Map<String, Object> failCtx = new java.util.HashMap<>();
            failCtx.put("instanceCode", instanceCode);
            failCtx.put("skillCode", skillCode);
            failCtx.put("result", "FAIL");
            failCtx.put("error", e.getMessage());
            hookDispatcher.fire("postSkill", failCtx);
            return false;
        }
    }

    /**
     * 心跳触发：重新分发指定实例的全部绑定（Wishlist 2）。
     *
     * <p>调用时机：Agent 上报心跳后、Agent 主动拉取 /pull 后、定时 reconcile。
     * 本方法会从 SkillCenter 拉取每条绑定对应的最新内容，并写盘 + 通知。</p>
     *
     * @param instanceCode Agent 实例
     * @return 成功分发的 skill 数（content 拉取失败或分发失败的不计）
     */
    public int reconcileInstance(String instanceCode) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            return 0;
        }

        AgentType agentType = AgentType.parse(instance.getAgentType());

        List<SkillBinding> bindings = listBindings(instanceCode);
        int ok = 0;
        for (SkillBinding b : bindings) {
            if ("UNBOUND".equalsIgnoreCase(b.getStatus())) {
                continue;
            }

            String content = contentFetcher.fetch(b.getSkillCode());
            if (content == null) {
                log.warn("reconcile: failed to fetch content for skillCode={}", b.getSkillCode());
                continue;
            }
            if (distributeFromContent(instanceCode, b.getSkillCode(), content, agentType)) {
                ok++;
            }
        }
        log.info("Reconciled instance {}: {} skills OK out of {}", instanceCode, ok, bindings.size());
        return ok;
    }

    /**
     * 全量 reconcile（定时任务入口）。
     *
     * <p>扫所有 ACTIVE 实例，调用 {@link #reconcileInstance}。单实例失败不影响其他实例。</p>
     *
     * @return 处理的实例数
     */
    public int reconcileAll() {
        // 拿所有 ACTIVE 实例（FEATURE014 Wishlist 2）
        List<AgentInstance> allActive = instanceService.listAllActive();

        int count = 0;
        for (AgentInstance inst : allActive) {
            try {
                reconcileInstance(inst.getInstanceCode());
                count++;
            } catch (Exception e) {
                log.warn("reconcile failed for instance {}: {}",
                        inst.getInstanceCode(), e.getMessage());
            }
        }
        log.info("Reconcile-all done: processed {} ACTIVE instances", count);
        return count;
    }

    /**
     * Agent 拉取模式：返回该实例的所有绑定 Skill 元数据 + 已就绪的内容路径。
     *
     * <p>注意：本方法只返回绑定关系，不返回 skill content 本身
     * （content 由 Agent 端按 skillCode 自行 GET /api/skill/content?skillCode=xxx）。</p>
     */
    public List<String> pullSkillCodes(String instanceCode) {
        return listBindings(instanceCode).stream()
                .map(SkillBinding::getSkillCode)
                .collect(Collectors.toList());
    }

    /**
     * 列出最近的同步日志。
     */
    public List<SkillSyncLog> recentLogs(String instanceCode, int limit) {
        LambdaQueryWrapper<SkillSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillSyncLog::getInstanceCode, instanceCode)
                .orderByDesc(SkillSyncLog::getGmtSync)
                .last("limit " + Math.max(1, Math.min(limit, 200)));
        return syncLogMapper.selectList(wrapper);
    }

    // ============== private helpers ==============

    private SkillBinding findBinding(String instanceCode, String skillCode) {
        LambdaQueryWrapper<SkillBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillBinding::getInstanceCode, instanceCode)
                .eq(SkillBinding::getSkillCode, skillCode);
        return this.getOne(wrapper);
    }

    private void logSync(String instanceCode, String skillCode, AgentType agentType,
                         String action, String result, long durationMs, String errorMsg) {
        try {
            SkillSyncLog entry = new SkillSyncLog();
            entry.setInstanceCode(instanceCode);
            entry.setSkillCode(skillCode);
            entry.setAgentType(agentType.name());
            entry.setAction(action);
            entry.setResult(result);
            entry.setErrorMsg(errorMsg);
            entry.setDurationMs(durationMs);
            entry.setTenantCode("default");
            entry.setGmtSync(LocalDateTime.now());
            syncLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("Failed to write sync log: {}", e.getMessage());
        }
    }
}
