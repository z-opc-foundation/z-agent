package com.zifang.z.agent.center.core.agent.instance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceDto;
import com.zifang.z.agent.center.core.agent.instance.dto.AgentInstanceReq;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.mapper.AgentInstanceMapper;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.common.enums.AgentType;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service("agentInstanceSvc")
@Primary
public class AgentInstanceServiceImpl extends ServiceImpl<AgentInstanceMapper, AgentInstance> implements AgentInstanceService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private com.zifang.z.agent.center.core.agent.executor.AgentContainerService agentContainerService;

    @Override
    public AgentInstanceDto createResp(AgentInstanceReq req) {
        AgentInstance instance = createFromApp(req.getAppCode(), req.getUserId(), req.getUserName());
        return toDto(instance);
    }

    @Override
    public boolean save(AgentInstance instance) {
        // 接口层显式声明，本类继承自 ServiceImpl（MyBatis-Plus），
        // 委托到 baseMapper.insert 以满足接口契约且语义清晰
        if (instance.getGmtCreate() == null) {
            instance.setGmtCreate(LocalDateTime.now());
        }
        if (instance.getGmtModified() == null) {
            instance.setGmtModified(LocalDateTime.now());
        }
        return this.baseMapper.insert(instance) > 0;
    }

    @Override
    public AgentInstanceDto getRespByInstanceCode(String instanceCode) {
        return toDto(getByInstanceCode(instanceCode));
    }

    @Override
    public List<AgentInstanceDto> listRespByOwner(String ownerId) {
        return listByOwner(ownerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public AgentInstance getByInstanceCode(String instanceCode) {
        LambdaQueryWrapper<AgentInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentInstance::getInstanceCode, instanceCode);
        wrapper.eq(AgentInstance::getIsDeleted, 0);
        return this.getOne(wrapper);
    }

    @Override
    public AgentInstance createFromApp(String appCode, String userId, String userName) {
        return createFromAppWithMode(appCode, userId, userName, "IN_PROCESS", null);
    }

    /**
     * 创建 Agent 实例 (指定运行模式)
     */
    public AgentInstance createFromAppWithMode(String appCode, String userId, String userName, String runMode, Integer containerPort) {
        AgentInstance instance = new AgentInstance();
        instance.setInstanceCode("inst_" + RandomUtil.uuidShort(12));
        instance.setAppCode(appCode);
        instance.setOwnerId(userId);
        instance.setOwnerName(userName);
        instance.setInstanceName("我的" + appCode);
        instance.setStatus("ACTIVE");
        instance.setVisitCount(0);
        instance.setRunMode(runMode);
        instance.setContainerPort(containerPort);
        instance.setGmtCreate(LocalDateTime.now());
        instance.setGmtModified(LocalDateTime.now());
        instance.setIsDeleted(0);
        this.save(instance);
        return instance;
    }

    @Override
    public List<AgentInstance> listByOwner(String ownerId) {
        LambdaQueryWrapper<AgentInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentInstance::getOwnerId, ownerId);
        wrapper.eq(AgentInstance::getIsDeleted, 0);
        wrapper.orderByDesc(AgentInstance::getGmtCreate);
        return this.list(wrapper);
    }

    @Override
    public java.util.List<AgentInstance> listAllActive() {
        LambdaQueryWrapper<AgentInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentInstance::getStatus, "ACTIVE");
        wrapper.eq(AgentInstance::getIsDeleted, 0);
        wrapper.orderByDesc(AgentInstance::getGmtCreate);
        return this.list(wrapper);
    }

    @Override
    public void updateStatus(String instanceCode, String status) {
        AgentInstance instance = getByInstanceCode(instanceCode);
        if (instance != null) {
            instance.setStatus(status);
            instance.setGmtModified(LocalDateTime.now());
            this.updateById(instance);
        }
    }

    @Override
    public AgentInstance getByWidgetApiKey(String widgetApiKey) {
        if (widgetApiKey == null || widgetApiKey.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<AgentInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentInstance::getWidgetApiKey, widgetApiKey.trim());
        wrapper.eq(AgentInstance::getIsDeleted, 0);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public void recordVisit(String instanceCode) {
        AgentInstance instance = getByInstanceCode(instanceCode);
        if (instance != null) {
            instance.setVisitCount(instance.getVisitCount() == null ? 1 : instance.getVisitCount() + 1);
            instance.setLastVisitTime(LocalDateTime.now());
            instance.setGmtModified(LocalDateTime.now());
            this.updateById(instance);
        }
    }

    private AgentInstanceDto toDto(AgentInstance instance) {
        if (instance == null) {
            return null;
        }

        AgentInstanceDto dto = new AgentInstanceDto();
        BeanUtils.copyProperties(instance, dto);
        dto.setLastVisitTime(instance.getLastVisitTime() != null ? instance.getLastVisitTime().format(DF) : null);
        dto.setRunMode(instance.getRunMode());
        dto.setContainerId(instance.getContainerId());
        dto.setContainerPort(instance.getContainerPort());
        // FEATURE014 多 Agent 扩展字段
        dto.setAgentType(instance.getAgentType());
        dto.setCallbackUrl(instance.getCallbackUrl());
        dto.setSkillRootPath(instance.getSkillRootPath());
        dto.setAgentVersion(instance.getAgentVersion());
        dto.setLastHeartbeat(instance.getLastHeartbeat() != null ? instance.getLastHeartbeat().format(DF) : null);
        return dto;
    }

    // ===== FEATURE014 多 Agent 实例创建 =====

    @Override
    public AgentInstance createExternalAgent(String appCode, String userId, String userName,
                                             String agentTypeCode, String callbackUrl) {
        AgentType agentType = AgentType.parse(agentTypeCode);

        AgentInstance instance = new AgentInstance();
        instance.setInstanceCode("inst_" + RandomUtil.uuidShort(12));
        instance.setAppCode(appCode);
        instance.setOwnerId(userId);
        instance.setOwnerName(userName);
        instance.setInstanceName("我的" + appCode);
        instance.setStatus("ACTIVE");
        instance.setVisitCount(0);

        // 同步填入 agentType + 推导的 skillRootPath
        instance.setAgentType(agentType.name());

        // runMode 同步旧字段（兼容旧的 dispatch 逻辑）
        switch (agentType.getExecutionMode()) {
            case "in-process":
                instance.setRunMode("IN_PROCESS");
                break;
            case "container":
                instance.setRunMode("CONTAINER");
                break;
            case "local-process":
                instance.setRunMode("LOCAL_PROCESS");
                break;
            default:
                instance.setRunMode("IN_PROCESS");
        }

        // Skill 落盘路径：未显式指定时按 agentType 默认值推导
        instance.setSkillRootPath(agentType.getDefaultSkillRootPath());

        // 生成 AuthToken（用于外部 Agent 反向调用 center）
        instance.setAuthToken(RandomUtil.uuidCompact());

        // 回调地址（ZBOT 留空）
        if (agentType != AgentType.ZBOT) {
            instance.setCallbackUrl(callbackUrl);
        }

        instance.setGmtCreate(LocalDateTime.now());
        instance.setGmtModified(LocalDateTime.now());
        instance.setIsDeleted(0);

        this.save(instance);
        return instance;
    }
}
