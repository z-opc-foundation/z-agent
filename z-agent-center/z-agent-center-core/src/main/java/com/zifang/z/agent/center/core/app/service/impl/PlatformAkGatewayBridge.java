package com.zifang.z.agent.center.core.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.z.agent.center.core.app.entity.AgentAk;
import com.zifang.z.agent.center.core.app.mapper.AgentAkMapper;
import com.zifang.z.llm.gateway.api.dto.ApiKey;
import com.zifang.z.llm.gateway.core.credential.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台 AK → 网关凭证桥.
 *
 * <p>AK 管理的"玩法"是: 用户在 AK 管理页建的 {@code sk-*} 密钥要能直接当网关凭证调
 * {@code /v1/chat/completions}。网关自己的 {@link ApiKeyService} 只认配置文件里的
 * {@code z.llm.apiKeys}, 看不见平台 AK 表 —— 不桥接的话 AK 管理就是一张装饰表。
 *
 * <p>网关不在 classpath (z-agent 单独部署给别的宿主时) 一切静默跳过, AK 管理照常。
 */
@Component
public class PlatformAkGatewayBridge {

    private static final Logger log = LoggerFactory.getLogger(PlatformAkGatewayBridge.class);

    private final AgentAkMapper akMapper;
    private final ObjectProvider<ApiKeyService> gatewayKeys;

    @Autowired
    public PlatformAkGatewayBridge(AgentAkMapper akMapper, ObjectProvider<ApiKeyService> gatewayKeys) {
        this.akMapper = akMapper;
        this.gatewayKeys = gatewayKeys;
    }

    /** 启动完成后把全量启用 AK 同步进网关; 用 ApplicationReady 而不是构造期, 避免网关 bean 还没建好. */
    @EventListener(ApplicationReadyEvent.class)
    public void syncAllOnStartup() {
        ApiKeyService keys = gatewayKeys.getIfAvailable();
        if (keys == null) {
            log.info("PlatformAkGatewayBridge: gateway ApiKeyService absent, skip");
            return;
        }
        List<AgentAk> aks = akMapper.selectList(new LambdaQueryWrapper<AgentAk>()
                .eq(AgentAk::getIsDeleted, 0));
        int ok = 0;
        for (AgentAk ak : aks) {
            if (register(keys, ak)) {
                ok++;
            }
        }
        log.info("PlatformAkGatewayBridge: synced {} active AKs into gateway (of {})", ok, aks.size());
    }

    public void onUpsert(AgentAk ak) {
        ApiKeyService keys = gatewayKeys.getIfAvailable();
        if (keys == null || ak == null) {
            return;
        }
        register(keys, ak);
    }

    public void onRemove(AgentAk ak) {
        ApiKeyService keys = gatewayKeys.getIfAvailable();
        if (keys == null || ak == null || ak.getSecretKey() == null) {
            return;
        }
        keys.unregister(ak.getSecretKey());
    }

    private boolean register(ApiKeyService keys, AgentAk ak) {
        if (ak.getSecretKey() == null || ak.getSecretKey().isEmpty()) {
            return false;
        }
        ApiKey k = new ApiKey();
        k.setId("platform-ak:" + ak.getId());
        k.setName(ak.getAkName());
        k.setKey(ak.getSecretKey());
        k.setOwnerId(ak.getOwnerUserId());
        k.setStatus(ak.getStatus() != null && ak.getStatus() == 1 ? "active" : "disabled");
        return keys.register(k);
    }
}
