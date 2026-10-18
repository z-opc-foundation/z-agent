package com.zifang.z.agent.center.core.app.service;

import com.zifang.z.agent.center.core.app.entity.AgentAppGroup;

import java.util.List;

public interface AgentAppGroupService {

    List<AgentAppGroup> listTree(String tenantCode);

    AgentAppGroup getByCode(String groupCode);

    AgentAppGroup create(AgentAppGroup group);

    AgentAppGroup update(AgentAppGroup group);

    void delete(String groupCode);
}