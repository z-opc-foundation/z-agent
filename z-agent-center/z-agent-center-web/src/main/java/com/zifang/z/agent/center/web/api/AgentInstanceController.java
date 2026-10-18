package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceDto;
import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceReq;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.skill.SkillDistributor;
import com.zifang.z.agent.center.web.api.SkillBindController.BindReq;
import com.zifang.z.agent.center.web.api.SkillBindController.HeartbeatReq;
import com.zifang.z.agent.center.web.api.SkillBindController.RegisterReq;
import com.zifang.z.agent.center.web.api.response.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 实例管理 Controller.
 * <p>
 * API 基础路径: /api/agent/instance
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/agent/instance/create             — 从应用创建实例</li>
 *   <li>POST /api/agent/instance/create-container   — 创建容器模式 Agent 实例</li>
 *   <li>POST /api/agent/instance/destroy-container  — 销毁容器模式 Agent 实例</li>
 *   <li>POST /api/agent/instance/create-external    — FEATURE014 创建多类型 Agent 实例</li>
 *   <li>POST /api/agent/instance/destroy-local-process — FEATURE014 销毁本地进程模式实例</li>
 *   <li>POST /api/agent/instance/register           — FEATURE014 §4.1 alias for {@code /api/agent/skill/register}</li>
 *   <li>POST /api/agent/instance/bind-skill         — FEATURE014 §4.1 alias for {@code /api/agent/skill/bind}</li>
 *   <li>POST /api/agent/instance/unbind-skill       — FEATURE014 §4.1 alias for {@code /api/agent/skill/unbind}</li>
 *   <li>GET  /api/agent/instance/skill/pull         — FEATURE014 §4.1 alias for {@code /api/agent/skill/pull}</li>
 *   <li>GET  /api/agent/instance/heartbeat          — FEATURE014 §4.1 alias for {@code /api/agent/skill/heartbeat}（GET）</li>
 *   <li>GET  /api/agent/instance/agent-types        — FEATURE014 列出可用的 Agent 类型</li>
 *   <li>GET  /api/agent/instance/health             — FEATURE014 健康检查某个 Agent 实例</li>
 *   <li>GET  /api/agent/instance/get                — 查询实例详情</li>
 *   <li>GET  /api/agent/instance/list               — 查询用户的实例列表</li>
 *   <li>POST /api/agent/instance/status             — 更新实例状态</li>
 * </ul>
 */
@Tag(name = "Agent实例")
@RestController
@RequestMapping("/api/agent/instance")
public class AgentInstanceController {

    @Resource
    private AgentInstanceService instanceService;

    @Resource
    private com.zifang.z.agent.center.core.agent.executor.AgentContainerService containerService;

    @Resource
    private com.zifang.z.agent.center.core.agent.executor.LocalProcessAgentRuntime localProcessRuntime;

    @Resource
    private com.zifang.z.agent.center.core.agent.executor.AgentExecutorFactory executorFactory;

    /**
     * FEATURE014 §4.1 — 别名端点使用的分销服务。
     */
    @Resource
    private SkillDistributor distributor;

    /**
     * 从应用创建实例.
     *
     * @param req 实例创建请求
     * @return 创建后的实例 DTO
     */
    @Operation(summary = "从应用创建实例")
    @PostMapping("/create")
    public Result<AgentInstanceDto> create(@RequestBody AgentInstanceReq req) {
        return Result.success(instanceService.createResp(req));
    }

    /**
     * 创建容器模式 Agent 实例（Hermes Docker 容器）.
     *
     * @param req 实例创建请求
     * @return 创建后的实例 DTO
     */
    @Operation(summary = "创建容器模式Agent实例")
    @PostMapping("/create-container")
    public Result<AgentInstanceDto> createContainer(@RequestBody AgentInstanceReq req) {
        // 1. 创建 Hermes 容器
        String instanceCode = "inst_" + System.currentTimeMillis();
        Integer port = containerService.createContainer(instanceCode);

        // 2. 创建实例记录
        AgentInstance instance = ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                .createFromAppWithMode(req.getAppCode(), req.getUserId(), req.getUserName(), "CONTAINER", port);

        // 3. 更新容器ID
        instance.setContainerId("agent-" + instance.getInstanceCode());

        return Result.success(instanceService.getRespByInstanceCode(instance.getInstanceCode()));
    }

    /**
     * 销毁容器模式 Agent 实例.
     *
     * @param instanceCode Agent 实例编码
     * @return 通用成功响应
     */
    @Operation(summary = "销毁容器模式Agent实例")
    @PostMapping("/destroy-container")
    public Result<Object> destroyContainer(@RequestParam String instanceCode) {
        containerService.destroyContainer(instanceCode);
        return Result.success();
    }

    // ===== FEATURE014 多 Agent — 外部 Agent 接入入口 =====

    /**
     * FEATURE014 创建多类型 Agent 实例.
     *
     * @param req 实例创建请求（含 agentType / callbackUrl 等）
     * @return 创建后的实例 DTO
     */
    @Operation(summary = "FEATURE014 创建多类型 Agent 实例（Claude Code / OpenCode / Hermes 本地 / 自定义）")
    @PostMapping("/create-external")
    public Result<AgentInstanceDto> createExternal(@RequestBody AgentInstanceReq req) {
        // 1. 创建实例记录
        AgentInstance instance = instanceService.createExternalAgent(
                req.getAppCode(),
                req.getUserId(),
                req.getUserName(),
                req.getAgentType(),
                req.getCallbackUrl());

        // 2. 按 agentType 决定要不要再拉起一个外部进程
        com.zifang.z.agent.common.enums.AgentType agentType =
                com.zifang.z.agent.common.enums.AgentType.parse(req.getAgentType());

        try {
            switch (agentType) {
                case HERMES_CONTAINER:
                    Integer containerPort = containerService.createContainer(instance.getInstanceCode());
                    instance.setContainerId("agent-" + instance.getInstanceCode());
                    instance.setContainerPort(containerPort);
                    break;
                case HERMES_LOCAL:
                case CLAUDE_CODE:
                case OPENCODE:
                case CUSTOM:
                    Integer processPort = localProcessRuntime.startProcess(
                            instance.getInstanceCode(), agentType, req.getCustomCommand());
                    if (instance.getCallbackUrl() == null || instance.getCallbackUrl().isEmpty()) {
                        instance.setCallbackUrl("http://localhost:" + processPort);
                    }
                    break;
                case ZBOT:
                default:
                    break;
            }
            ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                    .updateById(instance);
        } catch (RuntimeException e) {
            instance.setStatus("ERROR");
            ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                    .updateById(instance);
            return Result.<AgentInstanceDto>fail("Agent 实例已记录但子进程启动失败: " + e.getMessage()).code(BaseStatusCode.FAIL.getCode());
        }

        return Result.success(instanceService.getRespByInstanceCode(instance.getInstanceCode()));
    }

    /**
     * FEATURE014 销毁本地进程模式 Agent 实例.
     *
     * @param instanceCode Agent 实例编码
     * @return 通用成功响应
     */
    @Operation(summary = "FEATURE014 销毁本地进程模式 Agent 实例")
    @PostMapping("/destroy-local-process")
    public Result<Object> destroyLocalProcess(@RequestParam String instanceCode) {
        localProcessRuntime.stopProcess(instanceCode);
        return Result.success();
    }

    /**
     * FEATURE014 列出当前所有可用的 Agent 类型（前端下拉数据源）.
     *
     * @return Agent 类型 → 描述的 Map
     */
    @Operation(summary = "FEATURE014 列出当前所有可用的 Agent 类型（前端下拉数据源）")
    @GetMapping("/agent-types")
    public Result<java.util.Map<String, String>> agentTypes() {
        return Result.success(executorFactory.getAvailableAgentTypes());
    }

    /**
     * FEATURE014 健康检查某个 Agent 实例（按 agentType 选择健康检查策略）.
     *
     * @param instanceCode Agent 实例编码
     * @return 健康检查结果
     */
    @Operation(summary = "FEATURE014 健康检查某个 Agent 实例（按 agentType 选择健康检查策略）")
    @GetMapping("/health")
    public Result<InstanceHealthResp> health(@RequestParam String instanceCode) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            return Result.<InstanceHealthResp>fail("Instance not found: " + instanceCode).code(BaseStatusCode.FAIL.getCode());
        }
        InstanceHealthResp resp = new InstanceHealthResp();
        resp.setInstanceCode(instanceCode);
        resp.setAgentType(instance.getAgentType());
        resp.setRunMode(instance.getRunMode());
        boolean healthy = false;
        com.zifang.z.agent.common.enums.AgentType agentType =
                com.zifang.z.agent.common.enums.AgentType.parse(instance.getAgentType());
        switch (agentType) {
            case HERMES_CONTAINER:
                String status = containerService.getContainerStatus(instanceCode);
                healthy = "running".equalsIgnoreCase(status);
                resp.setContainerStatus(status);
                break;
            case HERMES_LOCAL:
            case CLAUDE_CODE:
            case OPENCODE:
            case CUSTOM:
                healthy = localProcessRuntime.isHealthy(instanceCode);
                break;
            default:
                healthy = true;
        }
        resp.setHealthy(healthy);
        return Result.success(resp);
    }

    /**
     * 按 instanceCode 查询实例详情.
     *
     * @param instanceCode Agent 实例编码
     * @return 实例详情 DTO
     */
    @Operation(summary = "查询实例详情")
    @GetMapping("/get")
    public Result<AgentInstanceDto> get(@RequestParam String instanceCode) {
        return Result.success(instanceService.getRespByInstanceCode(instanceCode));
    }

    /**
     * 查询指定用户的所有实例列表.
     *
     * @param ownerId 所有者用户 ID
     * @return 实例详情 DTO 列表
     */
    @Operation(summary = "查询用户的实例列表")
    @GetMapping("/list")
    public Result<List<AgentInstanceDto>> list(@RequestParam String ownerId) {
        return Result.success(instanceService.listRespByOwner(ownerId));
    }

    /**
     * 更新实例状态.
     *
     * @param instanceCode Agent 实例编码
     * @param status       目标状态值
     * @return 通用成功响应
     */
    @Operation(summary = "更新实例状态")
    @PostMapping("/status")
    public Result<Object> status(@RequestParam String instanceCode, @RequestParam String status) {
        instanceService.updateStatus(instanceCode, status);
        return Result.success();
    }

    // ===== FEATURE014 §4.1 spec 路由别名（/api/agent/instance/...） =====

    /**
     * FEATURE014 §4.1 — Agent 注册 (alias for {@code /api/agent/skill/register})。
     */
    @Operation(summary = "FEATURE014 §4.1 Agent 注册（spec alias）")
    @PostMapping("/register")
    public Result<SkillRegisterResp> instanceRegister(@RequestBody java.util.Map<String, Object> rawBody) {
        RegisterReq req = new RegisterReq();
        Object appCode = rawBody.get("appCode");
        req.appCode = appCode != null ? appCode.toString() : "default-chat";
        Object userId = rawBody.get("userId");
        req.userId = userId != null ? userId.toString() : "anonymous";
        Object userName = rawBody.get("userName");
        req.userName = userName != null ? userName.toString() : req.userId;
        Object agentType = rawBody.get("agentType");
        req.agentType = agentType != null ? agentType.toString() : "ZBOT";
        Object callbackUrl = rawBody.get("callbackUrl");
        req.callbackUrl = callbackUrl != null ? callbackUrl.toString() : null;
        Object version = rawBody.get("agentVersion");
        if (version == null) {
            version = rawBody.get("version");
        }

        req.agentVersion = version != null ? version.toString() : null;
        Object capabilities = rawBody.get("capabilities");
        if (capabilities == null) {
            req.capabilities = null;
        } else if (capabilities instanceof String) {
            req.capabilities = (String) capabilities;
        } else {
            try {
                req.capabilities = com.zifang.util.json.JsonUtil.toJson(capabilities);
            } catch (RuntimeException e) {
                req.capabilities = capabilities.toString();
            }
        }
        AgentInstance instance = instanceService.createExternalAgent(
                req.appCode, req.userId, req.userName, req.agentType, req.callbackUrl);
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
     * FEATURE014 §4.1 — 给 Agent 实例绑定 Skill (alias for {@code /api/agent/skill/bind})。
     */
    @Operation(summary = "FEATURE014 §4.1 绑定技能（spec alias）")
    @PostMapping("/bind-skill")
    public Result<SkillBindResp> instanceBindSkill(@RequestBody BindReq req) {
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
     * FEATURE014 §4.1 — Agent 实例解绑 Skill (alias for {@code /api/agent/skill/unbind})。
     */
    @Operation(summary = "FEATURE014 §4.1 解绑技能（spec alias）")
    @PostMapping("/unbind-skill")
    public Result<SkillUnbindResp> instanceUnbindSkill(@RequestBody BindReq req) {
        boolean removed = distributor.unbind(req.instanceCode, req.skillCodes);
        SkillUnbindResp resp = new SkillUnbindResp();
        resp.setRemoved(removed);
        resp.setInstanceCode(req.instanceCode);
        resp.setSkillCodes(req.skillCodes);
        return Result.success(resp);
    }

    /**
     * FEATURE014 §4.1 — Agent 拉取 Skill (alias for {@code /api/agent/skill/pull} GET)。
     */
    @Operation(summary = "FEATURE014 §4.1 拉取技能（spec alias）")
    @GetMapping("/skill/pull")
    public Result<SkillPullResp> instanceSkillPull(HttpServletRequest httpRequest,
                                                   @RequestParam String instanceCode) {
        AgentInstance instance = resolveInstance(httpRequest, instanceCode);
        if (instance == null) {
            return Result.<SkillPullResp>fail("Instance not found: " + instanceCode)
                    .code(BaseStatusCode.FAIL.getCode());
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
     * FEATURE014 §4.1 — Agent 心跳 (spec 用 GET；原生 /api/agent/skill/heartbeat 是 POST)。
     */
    @Operation(summary = "FEATURE014 §4.1 Agent 心跳（spec alias GET）")
    @GetMapping("/heartbeat")
    public Result<HeartbeatResp> instanceHeartbeat(HttpServletRequest httpRequest,
                                                   @RequestParam String instanceCode,
                                                   @RequestParam(required = false, defaultValue = "ACTIVE") String status,
                                                   @RequestParam(required = false) String agentVersion,
                                                   @RequestParam(required = false) String capabilities) {
        HeartbeatReq req = new HeartbeatReq();
        req.instanceCode = instanceCode;
        req.status = status;
        req.agentVersion = agentVersion;
        req.capabilities = capabilities;
        AgentInstance instance = resolveInstance(httpRequest, req.instanceCode);
        if (instance == null) {
            return Result.<HeartbeatResp>fail("Instance not found: " + req.instanceCode)
                    .code(BaseStatusCode.FAIL.getCode());
        }
        req.instanceCode = instance.getInstanceCode();
        instance.setLastHeartbeat(LocalDateTime.now());
        if (req.agentVersion != null) {
            instance.setAgentVersion(req.agentVersion);
        }

        if (req.capabilities != null) {
            instance.setCapabilities(req.capabilities);
        }

        instance.setStatus(req.status != null ? req.status : "ACTIVE");
        instanceService.updateStatus(req.instanceCode, instance.getStatus());
        try {
            instanceService.getByInstanceCode(req.instanceCode);
            ((com.zifang.z.agent.center.core.agent.instance.service.impl.AgentInstanceServiceImpl) instanceService)
                    .updateById(instance);
        } catch (Exception ignored) {
        }
        List<String> pendingSkills = distributor.pullSkillCodes(req.instanceCode);
        int reconciled = 0;
        try {
            reconciled = distributor.reconcileInstance(req.instanceCode);
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger(AgentInstanceController.class)
                    .warn("instanceHeartbeat reconcile failed for {}: {}", req.instanceCode, e.getMessage());
        }
        HeartbeatResp resp = new HeartbeatResp();
        resp.setOk(true);
        resp.setServerTime(System.currentTimeMillis());
        resp.setPendingSkills(pendingSkills);
        resp.setReconciledSkills(reconciled);
        resp.setCommands(java.util.Collections.emptyList());
        return Result.success(resp);
    }

    /**
     * 优先使用拦截器已认证的 instance（从 request attribute 取），
     * 回退到 {@link AgentInstanceService#getByInstanceCode} 查 DB。
     */
    private AgentInstance resolveInstance(HttpServletRequest req, String requestedCode) {
        Object attr = req.getAttribute("agentInstance");
        if (attr instanceof AgentInstance) {
            AgentInstance authed = (AgentInstance) attr;
            if (requestedCode == null || authed.getInstanceCode().equals(requestedCode)) {
                return authed;
            }
            return null;
        }
        return instanceService.getByInstanceCode(requestedCode);
    }
}
