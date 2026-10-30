package com.zifang.z.agent.center.core.agent.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.template.entity.AgentTemplate;
import com.zifang.z.agent.center.core.agent.template.mapper.AgentTemplateMapper;
import com.zifang.z.agent.center.core.agent.template.service.AgentTemplateService;
import com.zifang.z.agent.center.core.app.dto.AgentAppDto;
import com.zifang.z.agent.center.core.app.dto.AgentAppReq;
import com.zifang.z.agent.center.core.app.service.AgentAppService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentTemplateServiceImpl implements AgentTemplateService {

    private static final Logger log = LogManager.getLogger(AgentTemplateServiceImpl.class);

    @Resource
    private AgentTemplateMapper templateMapper;

    @Resource
    private AgentAppService agentAppService;

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof CharSequence) {
            return o.toString();
        }

        return String.valueOf(o);
    }

    private static Object toJsonIfNeeded(Object o) {
        if (o == null) {
            return null;
        }

        // 已经是 String 类型, 直接返回
        if (o instanceof CharSequence) {
            return o.toString();
        }

        // 否则 JSON 序列化 (数组/对象)
        try {
            return JsonUtil.toJsonExcludingNulls(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    @Override
    public AgentTemplate getById(Long id) {
        if (id == null) {
            return null;
        }

        return templateMapper.selectById(id);
    }

    @Override
    public AgentTemplate getByCode(String templateCode) {
        if (templateCode == null || templateCode.isEmpty()) {
            return null;
        }
        return templateMapper.selectOne(new LambdaQueryWrapper<AgentTemplate>()
                .eq(AgentTemplate::getTemplateCode, templateCode)
                .eq(AgentTemplate::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public List<AgentTemplate> pageList(String keyword, String category, String status,
                                        Integer isOfficial, int page, int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        // 老库 z_agent_template 缺少 status 列, 不做 status 过滤;
        // isOfficial / isDeleted 列已存在, 可正常过滤.
        LambdaQueryWrapper<AgentTemplate> q = new LambdaQueryWrapper<>();
        q.eq(AgentTemplate::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTemplate::getTemplateCode, kw)
                    .or().like(AgentTemplate::getName, kw)
                    .or().like(AgentTemplate::getDescription, kw)
                    .or().like(AgentTemplate::getTags, kw));
        }
        if (category != null && !category.trim().isEmpty()) {
            q.eq(AgentTemplate::getCategory, category.trim());
        }
        if (isOfficial != null) {
            q.eq(AgentTemplate::getIsOfficial, isOfficial);
        }
        q.orderByDesc(AgentTemplate::getId);
        Page<AgentTemplate> p = templateMapper.selectPage(new Page<>(page, size), q);
        return p.getRecords();
    }

    @Override
    public long count(String keyword, String category, String status, Integer isOfficial) {
        LambdaQueryWrapper<AgentTemplate> q = new LambdaQueryWrapper<>();
        q.eq(AgentTemplate::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTemplate::getTemplateCode, kw)
                    .or().like(AgentTemplate::getName, kw)
                    .or().like(AgentTemplate::getDescription, kw)
                    .or().like(AgentTemplate::getTags, kw));
        }
        if (category != null && !category.trim().isEmpty()) {
            q.eq(AgentTemplate::getCategory, category.trim());
        }
        if (isOfficial != null) {
            q.eq(AgentTemplate::getIsOfficial, isOfficial);
        }
        return templateMapper.selectCount(q);
    }

    @Override
    public List<AgentTemplate> listHot(int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 12;
        }

        return templateMapper.selectHot(limit);
    }

    @Override
    public AgentTemplate create(AgentTemplate template) {
        if (template == null || template.getTemplateCode() == null) {
            throw new IllegalArgumentException("templateCode 必填");
        }
        if (getByCode(template.getTemplateCode()) != null) {
            throw new IllegalStateException("templateCode 已存在: " + template.getTemplateCode());
        }
        if (template.getName() == null) template.setName(template.getTemplateCode());

        if (template.getCategory() == null) template.setCategory("GENERAL");

        if (template.getStatus() == null) template.setStatus("PUBLISHED");

        if (template.getIsOfficial() == null) template.setIsOfficial(0);

        if (template.getDownloadCount() == null) template.setDownloadCount(0);

        if (template.getViewCount() == null) template.setViewCount(0);

        if (template.getRating() == null) template.setRating(new java.math.BigDecimal("5.00"));

        if (template.getRatingCount() == null) template.setRatingCount(0);

        template.setIsDeleted(0);
        template.setGmtCreate(LocalDateTime.now());
        template.setGmtModified(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    @Override
    public AgentTemplate update(AgentTemplate template) {
        if (template == null || template.getId() == null) {
            throw new IllegalArgumentException("id 必填");
        }
        AgentTemplate old = templateMapper.selectById(template.getId());
        if (old == null) {
            throw new IllegalStateException("Template 不存在");
        }

        template.setTemplateCode(old.getTemplateCode()); // code 不可改
        template.setIsDeleted(old.getIsDeleted());
        template.setDownloadCount(old.getDownloadCount());
        template.setViewCount(old.getViewCount());
        template.setGmtModified(LocalDateTime.now());
        templateMapper.updateById(template);
        return templateMapper.selectById(template.getId());
    }

    @Override
    public boolean delete(Long id) {
        AgentTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return false;
        }

        t.setIsDeleted(1);
        t.setGmtModified(LocalDateTime.now());
        return templateMapper.updateById(t) > 0;
    }

    @Override
    public AgentTemplate setStatus(Long id, String status) {
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status) && !"DEPRECATED".equals(status)) {
            throw new IllegalArgumentException("status 必须是 DRAFT/PUBLISHED/DEPRECATED");
        }
        AgentTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new IllegalStateException("Template 不存在");
        }

        t.setStatus(status);
        t.setGmtModified(LocalDateTime.now());
        templateMapper.updateById(t);
        return t;
    }

    @Override
    public Map<String, Object> fork(Long templateId, String newAppName, String userId, String userName) {
        AgentTemplate tpl = templateMapper.selectById(templateId);
        if (tpl == null) {
            throw new IllegalArgumentException("模板不存在: id=" + templateId);
        }
        if (tpl.getConfigSnapshot() == null || tpl.getConfigSnapshot().isEmpty()) {
            throw new IllegalStateException("模板 config_snapshot 为空, 无法 fork");
        }
        // 解析 snapshot
        Map<String, Object> snap;
        try {
            snap = toPlainMap(tpl.getConfigSnapshot());
        } catch (Exception e) {
            throw new IllegalStateException("config_snapshot 解析失败: " + e.getMessage(), e);
        }
        // 生成新 appCode
        String newAppCode = "app_from_" + tpl.getTemplateCode() + "_"
                + RandomUtil.uuidShort(6);
        String name = newAppName != null && !newAppName.isEmpty()
                ? newAppName : (tpl.getName() + " (Fork)");

        // 把 snapshot 字段塞进 AgentAppReq
        AgentAppReq req = new AgentAppReq();
        req.setAppCode(newAppCode);
        req.setAppName(name);
        req.setPrompt(asString(snap.get("prompt")));
        req.setDescription(tpl.getDescription());
        req.setModelName(asString(snap.get("modelName")));
        req.setModelProvider(asString(snap.get("modelProvider")));
        // tools/knowledgeIds/skillCodes/variables 是 Object 类型, 需 JSON 序列化
        req.setTools(toJsonIfNeeded(snap.get("tools")));
        req.setKnowledgeIds(toJsonIfNeeded(snap.get("knowledgeIds")));
        req.setSkillCodes(toJsonIfNeeded(snap.get("skillCodes")));
        req.setVariables(toJsonIfNeeded(snap.get("variables")));
        req.setStatus("ACTIVE");
        req.setAgentMode("chat");
        if (snap.get("temperature") instanceof Number) {
            req.setTemperature(((Number) snap.get("temperature")).floatValue());
        }
        if (snap.get("maxTokens") instanceof Number) {
            req.setMaxTokens(((Number) snap.get("maxTokens")).intValue());
        }
        if (snap.get("historyWindowSize") instanceof Number) {
            req.setHistoryWindowSize(((Number) snap.get("historyWindowSize")).intValue());
        }
        req.setMemoryPrompt(asString(snap.get("memoryPrompt")));
        if (snap.get("enableLongMemory") instanceof Boolean) {
            req.setEnableLongMemory((Boolean) snap.get("enableLongMemory"));
        }

        AgentAppDto newApp = agentAppService.createResp(req);
        // fork 计数 +1
        templateMapper.incrementDownload(templateId);
        log.info("[AgentTemplate] fork OK template={} -> new app={}", tpl.getTemplateCode(), newAppCode);
        Map<String, Object> resp = new HashMap<>();
        resp.put("templateId", templateId);
        resp.put("templateCode", tpl.getTemplateCode());
        resp.put("newAppCode", newAppCode);
        resp.put("newAppName", name);
        resp.put("requiredPlugins", tpl.getRequiredPlugins());
        resp.put("requiredSkills", tpl.getRequiredSkills());
        resp.put("requiredKnowledge", tpl.getRequiredKnowledge());
        resp.put("appDto", newApp);
        return resp;
    }

    /**
     * JSON 字符串 → 深度摊平的 {@code Map<String,Object>}。
     * <p>
     * 必须过 {@code JsonUtil.toPlainValue}：{@code parseToMap} 只摊平第一层，
     * 嵌套节点仍是 {@code JsonObject}/{@code JsonArray}，下游按 {@code List}/{@code Map}
     * 强转会 ClassCastException。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(String json) {
        return (Map<String, Object>) JsonUtil.toPlainValue(JsonUtil.parse(json));
    }
}
