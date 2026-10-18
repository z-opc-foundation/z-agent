package com.zifang.z.agent.center.core.agent.share.service;

import com.zifang.z.agent.center.core.agent.share.dto.AgentShareDto;
import com.zifang.z.agent.center.core.agent.share.dto.AgentShareReq;
import com.zifang.z.agent.center.core.agent.share.entity.AgentShare;

import java.util.List;

public interface AgentShareService {

    AgentShareDto createResp(AgentShareReq req);

    AgentShareDto verifyResp(String shareCode);

    List<AgentShareDto> listRespByInstance(String instanceCode);

    AgentShare getByShareCode(String shareCode);

    AgentShare createShare(String instanceCode, String appCode);

    /**
     * 创建或更新一条 ACTIVE 状态的分享记录。若 shareCode 相同则激活已有记录；
     * 不存在则新建。用于 App 级别分享开关在 z_agent_app 上持久化 shareCode 时同步 z_agent_share。
     */
    AgentShare upsertActiveShare(String shareCode, String instanceCode, String appCode);

    List<AgentShare> listByInstance(String instanceCode);

    void disable(String shareCode);

    void incrementVisitCount(String shareCode);
}
