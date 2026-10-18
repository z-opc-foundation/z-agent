package com.zifang.z.agent.center.core.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.z.agent.center.core.app.dto.AgentAppDto;
import com.zifang.z.agent.center.core.app.dto.AgentAppReq;
import com.zifang.z.agent.common.entity.AgentAppVersion;

import java.util.List;

/**
 * AgentApp 业务服务接口（平台层扩展，继承自 z-agent-common.AgentAppService）
 * 添加了 DTO 相关方法和版本列表方法
 */
public interface AgentAppService extends com.zifang.z.agent.common.service.AgentAppService {

    IPage<AgentAppDto> pageResp(String keyword, String status, String groupCode, int pageNum, int pageSize);

    AgentAppDto getRespByAppCode(String appCode);

    AgentAppDto createResp(AgentAppReq req);

    AgentAppDto updateResp(AgentAppReq req);

    /**
     * 升级：基于已发布版本创建新草稿版本（version+1），同时将主表 status 改为 DRAFT 以便编辑
     */
    AgentAppDto upgradeResp(String appCode);

    List<AgentAppDto> listVersionResp(String appCode);

    @Override
    List<AgentAppVersion> listVersions(String appCode);

    @Override
    AgentAppVersion createVersion(AgentAppVersion version);

    @Override
    String getDraft(String appCode);

    @Override
    void saveDraft(String appCode, String draftData);

    @Override
    void incrementVisitCount(String instanceCode);

    /**
     * 切换分享开关：开启时若 shareCode 为空则生成并保存；关闭时只把 shareEnabled 置为 false（保留 shareCode 便于再开复用）
     *
     * @return 切换后的 shareCode（无论开关都返回，便于前端展示）
     */
    String toggleShare(String appCode, boolean enabled);
}
