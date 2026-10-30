package com.zifang.z.agent.center.core.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.app.entity.AgentAk;
import com.zifang.z.agent.center.core.app.mapper.AgentAkMapper;
import com.zifang.z.agent.center.core.app.service.AgentAkService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentAkServiceImpl implements AgentAkService {

    @Resource(name = "agentAkMapper")
    private AgentAkMapper akMapper;

    @Resource
    private PlatformAkGatewayBridge gatewayBridge;

    @Override
    public List<AgentAk> list(String tenantCode) {
        return list(tenantCode, null);
    }

    @Override
    public List<AgentAk> list(String tenantCode, String akType) {
        LambdaQueryWrapper<AgentAk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAk::getIsDeleted, 0);
        if (tenantCode != null && !tenantCode.isEmpty()) {
            wrapper.eq(AgentAk::getTenantCode, tenantCode);
        }
        if (akType != null && !akType.isEmpty()) {
            wrapper.eq(AgentAk::getAkType, akType);
        }
        wrapper.orderByDesc(AgentAk::getCreatedTime);
        return akMapper.selectList(wrapper);
    }

    @Override
    public AgentAk getByAccessKey(String accessKey) {
        return akMapper.selectOne(new LambdaQueryWrapper<AgentAk>()
                .eq(AgentAk::getAccessKey, accessKey));
    }

    @Override
    public AgentAk create(AgentAk ak) {
        // 自动生成 AccessKey / SecretKey
        if (ak.getAccessKey() == null || ak.getAccessKey().isEmpty()) {
            ak.setAccessKey("ak-zopc-" + RandomUtil.uuidShort(8));
        }
        if (ak.getSecretKey() == null || ak.getSecretKey().isEmpty()) {
            ak.setSecretKey("sk-" + RandomUtil.uuidCompact());
        }
        // 类型校验：APP 类型必须绑定 appCode；PERSONAL 类型 appCode 必须为空
        String akType = ak.getAkType() == null ? "PERSONAL" : ak.getAkType();
        if ("APP".equals(akType)) {
            if (ak.getAppCode() == null || ak.getAppCode().isEmpty()) {
                throw new RuntimeException("应用类型 AK 必须绑定 appCode");
            }
        } else {
            ak.setAkType("PERSONAL");
            ak.setAppCode(null);
            ak.setAppName(null);
        }
        ak.setTotalTokens(0L);
        ak.setCreatedTime(LocalDateTime.now());
        ak.setUpdatedTime(LocalDateTime.now());
        akMapper.insert(ak);
        gatewayBridge.onUpsert(ak);
        return ak;
    }

    @Override
    public AgentAk update(AgentAk ak) {
        // 不允许通过 update 改 akType，一旦创建类型就锁死
        AgentAk existing = akMapper.selectById(ak.getId());
        if (existing == null) {
            throw new RuntimeException("AK not found: " + ak.getId());
        }
        ak.setAkType(existing.getAkType());
        // APP 类型保持 appCode 不变
        if ("APP".equals(existing.getAkType())) {
            if (ak.getAppCode() != null && !ak.getAppCode().isEmpty()) {
                // 允许更新 appCode（前端可能允许切换应用）
            } else {
                ak.setAppCode(existing.getAppCode());
            }
        }
        ak.setUpdatedTime(LocalDateTime.now());
        akMapper.updateById(ak);
        gatewayBridge.onUpsert(akMapper.selectById(ak.getId()));
        return akMapper.selectById(ak.getId());
    }

    @Override
    public void delete(Long id) {
        AgentAk existing = akMapper.selectById(id);
        AgentAk ak = new AgentAk();
        ak.setId(id);
        ak.setIsDeleted(1);
        ak.setUpdatedTime(LocalDateTime.now());
        akMapper.updateById(ak);
        if (existing != null) {
            gatewayBridge.onRemove(existing);
        }
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        AgentAk ak = new AgentAk();
        ak.setId(id);
        ak.setStatus(status);
        ak.setUpdatedTime(LocalDateTime.now());
        akMapper.updateById(ak);
        gatewayBridge.onUpsert(akMapper.selectById(id));
    }
}
