package com.zifang.z.agent.center.core.app.service;

import com.zifang.z.agent.center.core.app.entity.AgentAk;

import java.util.List;

public interface AgentAkService {

    List<AgentAk> list(String tenantCode);

    /**
     * 查询 AK 列表，支持按类型过滤
     */
    List<AgentAk> list(String tenantCode, String akType);

    AgentAk getByAccessKey(String accessKey);

    AgentAk create(AgentAk ak);

    AgentAk update(AgentAk ak);

    void delete(Long id);

    void toggleStatus(Long id, Integer status);
}
