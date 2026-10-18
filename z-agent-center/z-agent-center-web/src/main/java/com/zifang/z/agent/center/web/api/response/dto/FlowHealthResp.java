package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.Set;

/**
 * 工作流健康检查响应 DTO.
 */
public class FlowHealthResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private int cachedFlowCount;
    private Set<String> flowIds;

    public int getCachedFlowCount() {
        return cachedFlowCount;
    }

    public void setCachedFlowCount(int cachedFlowCount) {
        this.cachedFlowCount = cachedFlowCount;
    }

    public Set<String> getFlowIds() {
        return flowIds;
    }

    public void setFlowIds(Set<String> flowIds) {
        this.flowIds = flowIds;
    }
}
