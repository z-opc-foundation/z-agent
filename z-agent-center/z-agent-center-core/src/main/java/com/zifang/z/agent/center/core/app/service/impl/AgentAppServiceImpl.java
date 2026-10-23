package com.zifang.z.agent.center.core.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.share.service.AgentShareService;
import com.zifang.z.agent.center.core.app.dto.AgentAppDto;
import com.zifang.z.agent.center.core.app.dto.AgentAppReq;
import com.zifang.z.agent.center.core.app.entity.AgentAppDraft;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.app.mapper.AgentAppDraftMapper;
import com.zifang.z.agent.center.core.app.mapper.AgentAppMapper;
import com.zifang.z.agent.center.core.app.mapper.AgentAppVersionMapper;
import com.zifang.z.agent.center.core.app.service.AgentAppService;
import com.zifang.z.agent.common.entity.AgentApp;
import com.zifang.z.agent.common.entity.AgentAppVersion;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service("agentAppSvc")
@Primary
public class AgentAppServiceImpl extends ServiceImpl<AgentAppMapper, AgentApp> implements AgentAppService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Resource
    private AgentAppVersionMapper versionMapper;
    @Resource
    private AgentAppDraftMapper draftMapper;
    /**
     * 注入分享服务：开启 App 分享时同步 z_agent_share 记录（verify 路由表），关闭时置为 DISABLED。
     * AgentShareServiceImpl 又依赖 AgentAppService，此处用 @Lazy 打破循环依赖。
     */
    @Lazy
    @Resource
    private AgentShareService shareService;
    /**
     * 注入实例服务：开启 App 分享时确保有一个 instanceCode=appCode 的公开实例，
     * 让后续 AgentRuntimeService.chat/chatHistory 能据此找到 app 配置。
     */
    @Resource
    private AgentInstanceService instanceService;

    @Override
    public IPage<AgentAppDto> pageResp(String keyword, String status, String groupCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<AgentApp> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.like(AgentApp::getAppName, keyword).or()
                    .like(AgentApp::getAppCode, keyword);
        }
        if (StringUtils.isNotEmpty(status)) {
            wrapper.eq(AgentApp::getStatus, status);
        }
        if (StringUtils.isNotEmpty(groupCode)) {
            wrapper.eq(AgentApp::getGroupCode, groupCode);
        }
        wrapper.eq(AgentApp::getIsDeleted, 0);
        wrapper.orderByDesc(AgentApp::getGmtCreate);

        // 手动 count 再分页，避免 MybatisPlus 分页插件未生效导致 total=0
        long total = this.count(wrapper);
        IPage<AgentApp> page = new Page<>(pageNum, pageSize, total);
        IPage<AgentApp> result = this.page(page, wrapper);
        return result.convert(this::toDto);
    }

    @Override
    public AgentAppDto getRespByAppCode(String appCode) {
        return toDto(getByAppCode(appCode));
    }

    @Override
    public AgentAppDto upgradeResp(String appCode) {
        AgentApp published = getByAppCode(appCode);
        if (published == null) {
            throw new RuntimeException("App not found: " + appCode);
        }
        if (!"PUBLISHED".equals(published.getStatus())) {
            throw new RuntimeException("仅已发布状态的应用可升级");
        }

        // 1. 计算下一个 version 字符串（基于 version 表最大版本号，格式如 v1, v2）
        String nextVersion = computeNextVersion(appCode);

        // 2. 取最近一条已发布 version 记录（携带温度/历史窗口等扩展字段）
        AgentAppVersion latestVersion = getLatestPublishedVersion(appCode);

        // 3. 把当前主表数据 + 扩展字段合并 → 新 version 行（status = DRAFT）
        AgentAppVersion newVersion = new AgentAppVersion();
        newVersion.setAppCode(appCode);
        newVersion.setVersion(nextVersion);
        newVersion.setPrompt(published.getPrompt());
        newVersion.setModelName(published.getModelName());
        newVersion.setModelProvider(published.getModelProvider());
        newVersion.setTools(published.getTools());
        newVersion.setKnowledgeIds(published.getKnowledgeIds());
        newVersion.setSkillCodes(published.getSkillCodes());
        newVersion.setVariables(published.getVariables());
        newVersion.setStatus("DRAFT");
        if (latestVersion != null) {
            newVersion.setTemperature(latestVersion.getTemperature());
            newVersion.setTopP(latestVersion.getTopP());
            newVersion.setMaxTokens(latestVersion.getMaxTokens());
            newVersion.setResponseFormat(latestVersion.getResponseFormat());
            newVersion.setHistoryWindowSize(latestVersion.getHistoryWindowSize());
            newVersion.setEnableLongMemory(latestVersion.getEnableLongMemory());
            newVersion.setMemoryPrompt(latestVersion.getMemoryPrompt());
        }
        createVersion(newVersion);

        // 4. 主表 status 改为 DRAFT，让前端能进入编辑器编辑新版本
        published.setStatus("DRAFT");
        published.setGmtModified(LocalDateTime.now());
        updateById(published);

        return toDto(published);
    }

    /**
     * 计算下一个版本号：取 version 表最大版本号，+1，返回 "v{n}"
     */
    private String computeNextVersion(String appCode) {
        LambdaQueryWrapper<AgentAppVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppVersion::getAppCode, appCode);
        wrapper.orderByDesc(AgentAppVersion::getGmtCreate);
        List<AgentAppVersion> all = versionMapper.selectList(wrapper);
        int max = 0;
        for (AgentAppVersion v : all) {
            String ver = v.getVersion();
            if (ver == null) {
                continue;
            }
            // 解析 "v1", "v2", "1", "2" 等格式
            String num = ver.toLowerCase().startsWith("v") ? ver.substring(1) : ver;
            try {
                int n = Integer.parseInt(num);
                if (n > max) {
                    max = n;
                }

            } catch (NumberFormatException ignore) {
            }
        }
        return "v" + (max + 1);
    }

    /**
     * 获取最近一条已发布的 version 记录（升级时用于携带扩展字段）
     */
    private AgentAppVersion getLatestPublishedVersion(String appCode) {
        LambdaQueryWrapper<AgentAppVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppVersion::getAppCode, appCode);
        wrapper.eq(AgentAppVersion::getStatus, "PUBLISHED");
        wrapper.orderByDesc(AgentAppVersion::getGmtCreate);
        wrapper.last("LIMIT 1");
        return versionMapper.selectOne(wrapper);
    }

    @Override
    public AgentAppDto createResp(AgentAppReq req) {
        AgentApp app = toEntity(req);
        if (StringUtils.isEmpty(app.getAppCode())) {
            app.setAppCode("app_" + RandomUtil.uuidShort(12));
        }
        app.setStatus("DRAFT");
        app.setGmtCreate(LocalDateTime.now());
        app.setGmtModified(LocalDateTime.now());
        app.setIsDeleted(0);
        this.save(app);
        return toDto(app);
    }

    @Override
    public AgentAppDto updateResp(AgentAppReq req) {
        AgentApp existing = getByAppCode(req.getAppCode());
        if (existing == null) {
            throw new RuntimeException("App not found: " + req.getAppCode());
        }
        AgentApp app = toEntity(req);
        app.setId(existing.getId());
        app.setGmtModified(LocalDateTime.now());
        this.updateById(app);
        return toDto(this.getById(existing.getId()));
    }

    @Override
    public AgentApp getByAppCode(String appCode) {
        LambdaQueryWrapper<AgentApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentApp::getAppCode, appCode);
        wrapper.eq(AgentApp::getIsDeleted, 0);
        return this.getOne(wrapper);
    }

    @Override
    public AgentApp create(AgentApp app) {
        if (StringUtils.isEmpty(app.getAppCode())) {
            app.setAppCode("app_" + RandomUtil.uuidShort(12));
        }
        app.setStatus("DRAFT");
        app.setGmtCreate(LocalDateTime.now());
        app.setGmtModified(LocalDateTime.now());
        app.setIsDeleted(0);
        this.save(app);
        return app;
    }

    @Override
    public AgentApp update(AgentApp app) {
        app.setGmtModified(LocalDateTime.now());
        this.updateById(app);
        return app;
    }

    @Override
    public void remove(Long id) {
        AgentApp app = this.getById(id);
        if (app != null) {
            app.setIsDeleted(1);
            this.updateById(app);
        }
    }

    @Override
    public void publish(String appCode) {
        AgentApp app = getByAppCode(appCode);
        if (app == null) {
            throw new RuntimeException("App not found: " + appCode);
        }
        app.setStatus("PUBLISHED");
        updateById(app);
        // 发布即确保运行时实例存在（instanceCode=appCode），
        // 否则应用列表的就地聊天/分享验证都会撞「实例不存在或已停用」
        ensurePublicInstance(appCode);
    }

    @Override
    public List<AgentAppDto> listVersionResp(String appCode) {
        List<AgentAppVersion> list = listVersions(appCode);
        return list.stream().map(v -> {
            AgentAppDto dto = new AgentAppDto();
            dto.setAppCode(v.getAppCode());
            dto.setGmtCreate(v.getGmtCreate() != null ? v.getGmtCreate().format(DF) : null);
            dto.setStatus(v.getVersion());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AgentAppVersion> listVersions(String appCode) {
        LambdaQueryWrapper<AgentAppVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppVersion::getAppCode, appCode);
        wrapper.orderByDesc(AgentAppVersion::getGmtCreate);
        return versionMapper.selectList(wrapper);
    }

    @Override
    public AgentAppVersion createVersion(AgentAppVersion version) {
        version.setGmtCreate(LocalDateTime.now());
        version.setGmtModified(LocalDateTime.now());
        versionMapper.insert(version);
        return version;
    }

    @Override
    public String getDraft(String appCode) {
        LambdaQueryWrapper<AgentAppDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppDraft::getAppCode, appCode);
        AgentAppDraft draft = draftMapper.selectOne(wrapper);
        return draft != null ? draft.getDraftData() : null;
    }

    @Override
    public void saveDraft(String appCode, String draftData) {
        LambdaQueryWrapper<AgentAppDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppDraft::getAppCode, appCode);
        AgentAppDraft draft = draftMapper.selectOne(wrapper);
        if (draft == null) {
            draft = new AgentAppDraft();
            draft.setAppCode(appCode);
            draft.setDraftData(draftData);
            draft.setVersion(1);
            draft.setGmtCreate(LocalDateTime.now());
            draft.setGmtModified(LocalDateTime.now());
            draftMapper.insert(draft);
        } else {
            draft.setDraftData(draftData);
            draft.setVersion(draft.getVersion() + 1);
            draft.setGmtModified(LocalDateTime.now());
            draftMapper.updateById(draft);
        }
    }

    @Override
    public void incrementVisitCount(String instanceCode) {
    }

    @Override
    public String toggleShare(String appCode, boolean enabled) {
        AgentApp app = getByAppCode(appCode);
        if (app == null) {
            throw new RuntimeException("App not found: " + appCode);
        }
        app.setShareEnabled(enabled);
        if (enabled && StringUtils.isEmpty(app.getShareCode())) {
            app.setShareCode("shr_" + RandomUtil.uuidShort(16));
        }
        app.setGmtModified(LocalDateTime.now());
        updateById(app);

        // 同步 z_agent_share 记录（verify 路由查的表）以及对应的 z_agent_instance
        // 之前只写 z_agent_app.shareCode，验证接口永远查不到，导致分享链接报“无效或已过期”
        if (StringUtils.isNotEmpty(app.getShareCode())) {
            if (enabled) {
                ensurePublicInstance(appCode);
                shareService.upsertActiveShare(app.getShareCode(), appCode, appCode);
            } else {
                shareService.disable(app.getShareCode());
            }
        }
        return app.getShareCode();
    }

    /**
     * 为公开分享创建一个 instanceCode=appCode 的"虚拟"实例。
     * AgentRuntimeService 的 chat/chatStream/chatHistory 都是按 instanceCode 路由，
     * 没有实例就没有可聊天的 app 配置。
     */
    private void ensurePublicInstance(String appCode) {
        if (instanceService == null) {
            return;
        }
        if (instanceService.getByInstanceCode(appCode) != null) {
            return;
        }
        AgentInstance instance = new AgentInstance();
        instance.setInstanceCode(appCode);
        instance.setAppCode(appCode);
        instance.setInstanceName("公开分享实例-" + appCode);
        instance.setOwnerId("public-share");
        instance.setOwnerName("公开分享");
        instance.setStatus("ACTIVE");
        instance.setVisitCount(0);
        instance.setRunMode("IN_PROCESS");
        instance.setGmtCreate(LocalDateTime.now());
        instance.setGmtModified(LocalDateTime.now());
        instance.setIsDeleted(0);
        instanceService.save(instance);
    }

    private AgentAppDto toDto(AgentApp app) {
        if (app == null) {
            return null;
        }

        AgentAppDto dto = new AgentAppDto();
        BeanUtils.copyProperties(app, dto);
        dto.setGmtCreate(app.getGmtCreate() != null ? app.getGmtCreate().format(DF) : null);
        dto.setGmtModified(app.getGmtModified() != null ? app.getGmtModified().format(DF) : null);
        // 从 version 表取最大 version 字符串
        LambdaQueryWrapper<AgentAppVersion> vw = new LambdaQueryWrapper<>();
        vw.eq(AgentAppVersion::getAppCode, app.getAppCode());
        vw.orderByDesc(AgentAppVersion::getGmtCreate);
        vw.last("LIMIT 1");
        AgentAppVersion latest = versionMapper.selectOne(vw);
        if (latest != null && latest.getVersion() != null) {
            dto.setVersion(latest.getVersion());
        } else {
            dto.setVersion("v1");
        }
        return dto;
    }

    private AgentApp toEntity(AgentAppReq req) {
        AgentApp app = new AgentApp();
        app.setAppCode(req.getAppCode());
        app.setAppName(req.getAppName());
        app.setDescription(req.getDescription());
        app.setIconUrl(req.getIconUrl());
        app.setPrompt(req.getPrompt());
        app.setModelName(req.getModelName());
        app.setModelProvider(req.getModelProvider());
        app.setStatus(req.getStatus());
        app.setGroupCode(req.getGroupCode());
        app.setAgentMode(req.getAgentMode());
        // 模型参数
        app.setTemperature(req.getTemperature());
        app.setTopP(req.getTopP());
        app.setMaxTokens(req.getMaxTokens());
        app.setResponseFormat(req.getResponseFormat());
        // 记忆配置
        app.setHistoryWindowSize(req.getHistoryWindowSize());
        app.setEnableLongMemory(req.getEnableLongMemory());
        app.setMemoryPrompt(req.getMemoryPrompt());
        if (req.getTools() != null) {
            try {
                app.setTools(JsonUtil.toJsonExcludingNulls(req.getTools()));
            } catch (Exception e) {
            }
        }
        if (req.getKnowledgeIds() != null) {
            try {
                app.setKnowledgeIds(JsonUtil.toJsonExcludingNulls(req.getKnowledgeIds()));
            } catch (Exception e) {
            }
        }
        if (req.getSkillCodes() != null) {
            try {
                app.setSkillCodes(JsonUtil.toJsonExcludingNulls(req.getSkillCodes()));
            } catch (Exception e) {
            }
        }
        if (req.getVariables() != null) {
            try {
                app.setVariables(JsonUtil.toJsonExcludingNulls(req.getVariables()));
            } catch (Exception e) {
            }
        }
        return app;
    }
}
