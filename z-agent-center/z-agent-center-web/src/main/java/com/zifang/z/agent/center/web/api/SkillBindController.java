package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.skill.SkillDistributor;
import com.zifang.z.agent.center.core.skill.entity.SkillBinding;
import com.zifang.z.agent.center.core.skill.entity.SkillSyncLog;
import com.zifang.z.agent.center.web.api.response.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Skill 绑定 / 分发 / 拉取 API — FEATURE014 §4
 *
 * <p>由三类调用方使用：</p>
 * <ol>
 *   <li>控制台 (admin UI)：{@code POST /bind-skill}, {@code POST /unbind-skill}, {@code GET /bindings}</li>
 *   <li>外部 Agent (Claude Code / OpenCode / Hermes / z-bot):
 *       {@code POST /agent/skill/pull}, {@code POST /agent/instance/heartbeat},
 *       {@code POST /agent/instance/register}</li>
 * </ol>
 */
@Tag(name = "Agent Skill 绑定")
@RestController
@RequestMapping("/api/agent/skill")
public class SkillBindController {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(SkillBindController.class);
    @Resource
    private SkillDistributor distributor;

    // ========== 控制台侧 API ==========
    @Resource
    private AgentInstanceService instanceService;

    /**
     * FEATURE014 给 Agent 实例绑定 Skill 并立即分发.
     *
     * @param req 绑定请求体（instanceCode / skillCodes / bindingType）
     * @return 包含 created / instanceCode / skillCodes / bindingType 的响应
     */
    @Operation(summary = "FEATURE014 给 Agent 实例绑定 Skill 并立即分发")
    @PostMapping("/bind")
    public Result<SkillBindResp> bind(@RequestBody BindReq req) {
        int created = distributor.bind(req.instanceCode, req.skillCodes,
                req.bindingType != null ? req.bindingType : "PUSH");
        SkillBindResp resp = new SkillBindResp();
        resp.setCreated(created);
        resp.setInstanceCode(req.instanceCode);
        resp.setSkillCodes(req.skillCodes);
        resp.setBindingType(req.bindingType != null ? req.bindingType : "PUSH");
        return Result.success(resp);
    }

    /**
     * FEATURE014 给 Agent 实例解绑 Skill.
     *
     * @param req 解绑请求体（instanceCode / skillCodes）
     * @return 包含 removed / instanceCode / skillCodes 的响应
     */
    @Operation(summary = "FEATURE014 给 Agent 实例解绑 Skill")
    @PostMapping("/unbind")
    public Result<SkillUnbindResp> unbind(@RequestBody BindReq req) {
        boolean removed = distributor.unbind(req.instanceCode, req.skillCodes);
        SkillUnbindResp resp = new SkillUnbindResp();
        resp.setRemoved(removed);
        resp.setInstanceCode(req.instanceCode);
        resp.setSkillCodes(req.skillCodes);
        return Result.success(resp);
    }

    /**
     * 查询一个 Agent 实例的所有绑定.
     *
     * @param instanceCode Agent 实例编码
     * @return Skill 绑定列表
     */
    @Operation(summary = "查询一个 Agent 实例的所有绑定")
    @GetMapping("/bindings")
    public Result<List<SkillBinding>> bindings(@RequestParam String instanceCode) {
        return Result.success(distributor.listBindings(instanceCode));
    }

    // ========== 外部 Agent 侧 API (FEATURE014 §4.2) ==========

    /**
     * 查询一个 Agent 实例的最近同步日志.
     *
     * @param instanceCode Agent 实例编码
     * @param limit        返回条数上限，默认 20
     * @return Skill 同步日志列表
     */
    @Operation(summary = "查询一个 Agent 实例的最近同步日志")
    @GetMapping("/sync-logs")
    public Result<List<SkillSyncLog>> syncLogs(@RequestParam String instanceCode,
                                               @RequestParam(defaultValue = "20") int limit) {
        return Result.success(distributor.recentLogs(instanceCode, limit));
    }

    /**
     * FEATURE014 外部 Agent 拉取自己应该加载的 Skill 列表.
     *
     * @param httpRequest  HTTP 请求（用于读取拦截器已认证的 instance）
     * @param instanceCode Agent 实例编码
     * @return 包含 instanceCode / agentType / skillCodes / timestamp 的响应
     */
    @Operation(summary = "FEATURE014 外部 Agent 拉取自己应该加载的 Skill 列表")
    @GetMapping("/pull")
    public Result<SkillPullResp> pull(jakarta.servlet.http.HttpServletRequest httpRequest,
                                      @RequestParam String instanceCode) {
        AgentInstance instance = resolveInstance(httpRequest, instanceCode);
        if (instance == null) {
            return Result.<SkillPullResp>fail("Instance not found: " + instanceCode).code(BaseStatusCode.FAIL.getCode());
        }
        List<String> skillCodes = distributor.pullSkillCodes(instanceCode);
        SkillPullResp resp = new SkillPullResp();
        resp.setInstanceCode(instanceCode);
        resp.setAgentType(instance.getAgentType());
        resp.setSkillCodes(skillCodes);
        resp.setTimestamp(System.currentTimeMillis());
        return Result.success(resp);
    }

    /**
     * FEATURE014 Agent 心跳上报 — 更新 lastHeartbeat.
     *
     * @param httpRequest HTTP 请求（用于读取拦截器已认证的 instance）
     * @param req         心跳请求体（instanceCode / status / agentVersion / capabilities）
     * @return 包含 ok / serverTime / pendingSkills / reconciledSkills / commands 的响应
     */
    @Operation(summary = "FEATURE014 Agent 心跳上报 — 更新 lastHeartbeat")
    @PostMapping("/heartbeat")
    public Result<HeartbeatResp> heartbeat(jakarta.servlet.http.HttpServletRequest httpRequest,
                                           @RequestBody HeartbeatReq req) {
        return doHeartbeat(httpRequest, req.instanceCode, req.status, req.agentVersion, req.capabilities);
    }

    /**
     * FEATURE014 §4.1 — 同路径 GET 别名，与 POST 行为一致。
     */
    @Operation(summary = "FEATURE014 Agent 心跳（GET 别名）")
    @GetMapping("/heartbeat")
    public Result<HeartbeatResp> heartbeatGet(jakarta.servlet.http.HttpServletRequest httpRequest,
                                              @RequestParam String instanceCode,
                                              @RequestParam(required = false, defaultValue = "ACTIVE") String status,
                                              @RequestParam(required = false) String agentVersion,
                                              @RequestParam(required = false) String capabilities) {
        return doHeartbeat(httpRequest, instanceCode, status, agentVersion, capabilities);
    }

    /**
     * 心跳核心逻辑 — 供 POST 和 GET 复用。
     */
    private Result<HeartbeatResp> doHeartbeat(jakarta.servlet.http.HttpServletRequest httpRequest,
                                              String instanceCode, String status,
                                              String agentVersion, String capabilities) {
        AgentInstance instance = resolveInstance(httpRequest, instanceCode);
        if (instance == null) {
            return Result.<HeartbeatResp>fail("Instance not found: " + instanceCode).code(BaseStatusCode.FAIL.getCode());
        }
        // 从已认证 instance 拷回必要字段（避免覆盖/篡改）
        instanceCode = instance.getInstanceCode();
        instance.setLastHeartbeat(LocalDateTime.now());
        if (agentVersion != null) {
            instance.setAgentVersion(agentVersion);
        }
        if (capabilities != null) {
            instance.setCapabilities(capabilities);
        }
        instance.setStatus(status != null ? status : "ACTIVE");
        instanceService.updateStatus(instanceCode, instance.getStatus());
        // 回填版本 / 能力（走 updateById 路径）
        try {
            instanceService.getByInstanceCode(instanceCode);
            ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                    .updateById(instance);
        } catch (Exception e) {
            // ignore — heartbeat 失败不应阻断
        }

        // 计算增量 Skill 列表
        List<String> pendingSkills = distributor.pullSkillCodes(instanceCode);

        // 心跳触发自动 reconcile
        int reconciled = 0;
        try {
            reconciled = distributor.reconcileInstance(instanceCode);
        } catch (Exception e) {
            log.warn("heartbeat reconcile failed for {}: {}", instanceCode, e.getMessage());
        }

        HeartbeatResp resp = new HeartbeatResp();
        resp.setOk(true);
        resp.setServerTime(System.currentTimeMillis());
        resp.setPendingSkills(pendingSkills);
        resp.setReconciledSkills(reconciled);
        resp.setCommands(Collections.emptyList());
        return Result.success(resp);
    }

    // ========== 请求体 ==========

    /**
     * FEATURE014 外部 Agent 注册到中心（首次启动时）.
     *
     * @param req 注册请求体
     * @return 包含 instanceCode / authToken / skillRootPath / assignedSkills 的响应
     */
    @Operation(summary = "FEATURE014 外部 Agent 注册到中心（首次启动时）")
    @PostMapping("/register")
    public Result<SkillRegisterResp> register(@RequestBody RegisterReq req) {
        AgentInstance instance = instanceService.createExternalAgent(
                req.appCode, req.userId, req.userName,
                req.agentType, req.callbackUrl);
        // 回填版本 / 能力
        instance.setAgentVersion(req.agentVersion);
        instance.setCapabilities(req.capabilities);
        instance.setStatus("ACTIVE");
        instance.setLastHeartbeat(LocalDateTime.now());
        ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                .updateById(instance);

        SkillRegisterResp resp = new SkillRegisterResp();
        resp.setInstanceCode(instance.getInstanceCode());
        resp.setAuthToken(instance.getAuthToken());
        resp.setSkillRootPath(instance.getSkillRootPath());
        resp.setAssignedSkills(distributor.pullSkillCodes(instance.getInstanceCode()));
        return Result.success(resp);
    }

    /**
     * 优先使用拦截器已认证的 instance（从 request attribute 取），
     * 回退到 {@link AgentInstanceService#getByInstanceCode} 查 DB。
     */
    private AgentInstance resolveInstance(jakarta.servlet.http.HttpServletRequest req, String requestedCode) {
        Object attr = req.getAttribute("agentInstance");
        if (attr instanceof AgentInstance) {
            AgentInstance authed = (AgentInstance) attr;
            if (requestedCode == null || authed.getInstanceCode().equals(requestedCode)) {
                return authed;
            }
            // 越权：声称是 A 实际 token 是 B
            log.warn("Authed instance {} != requested {}", authed.getInstanceCode(), requestedCode);
            return null;
        }
        return instanceService.getByInstanceCode(requestedCode);
    }

    /**
     * Skill 绑定 / 解绑请求体.
     */
    public static class BindReq {
        public String instanceCode;
        public java.util.List<String> skillCodes;
        public String bindingType;
    }

    /**
     * Agent 心跳请求体.
     */
    public static class HeartbeatReq {
        public String instanceCode;
        public String status;
        public String agentVersion;
        public String capabilities;
    }

    /**
     * 外部 Agent 注册请求体.
     */
    public static class RegisterReq {
        public String appCode;
        public String userId;
        public String userName;
        public String agentType;
        public String callbackUrl;
        public String agentVersion;
        public String capabilities;
    }
}
