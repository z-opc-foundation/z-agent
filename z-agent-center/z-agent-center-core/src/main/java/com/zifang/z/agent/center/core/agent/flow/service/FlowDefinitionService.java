package com.zifang.z.agent.center.core.agent.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowDefinitionPO;
import com.zifang.z.agent.center.core.agent.flow.mapper.FlowDefinitionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义 Service (FEATURE013 A3)
 * 提供基于 DB 的 CRUD + 版本管理 + 启动加载
 */
@Service
public class FlowDefinitionService {

    private static final Logger log = LogManager.getLogger(FlowDefinitionService.class);

    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;

    // ==================== 启动加载 ====================

    /**
     * 把 FlowDefinitionPO 转成 in-memory FlowDefinition (供 FlowEngine 直接执行)
     * 节点类型映射: LogicFlow 'llm' -> 后端 TYPE_LLM
     */
    public static Map<String, com.zifang.z.agent.center.core.agent.flow.FlowNode> buildNodes(FlowDefinitionPO po,
                                                                                             com.zifang.z.agent.center.core.agent.flow.FlowOrchestrationService orchestrationService) {
        Map<String, com.zifang.z.agent.center.core.agent.flow.FlowNode> nodes = new HashMap<>();
        if (po.getNodes() == null || po.getNodes().isEmpty()) return nodes;

        List<Map<String, Object>> arr = JsonUtil.fromJson(po.getNodes(), new com.zifang.util.json.define.TypeReference<List<Map<String, Object>>>() {
        });
        for (int i = 0; i < arr.size(); i++) {
            Map<String, Object> n = arr.get(i);
            String nodeId = n.get("id") != null ? n.get("id").toString() : null;
            String type = n.get("type") != null ? n.get("type").toString() : null;
            com.zifang.z.agent.center.core.agent.flow.FlowNode node =
                    createNodeByType(nodeId, type, n, orchestrationService);
            if (node != null) {
                nodes.put(nodeId, node);
            }
        }
        return nodes;
    }

    // ==================== CRUD ====================

    @SuppressWarnings("unchecked")
    private static com.zifang.z.agent.center.core.agent.flow.FlowNode createNodeByType(
            String nodeId, String type, Map<String, Object> props,
            com.zifang.z.agent.center.core.agent.flow.FlowOrchestrationService svc) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case "start":
            case "end":
                // start / end 是占位节点, 不需要实现, 路由中由 FlowEngine 跳过
                return new com.zifang.z.agent.center.core.agent.flow.FlowNode() {
                    @Override
                    public String getType() {
                        return type;
                    }

                    @Override
                    public Object execute(java.util.Map<String, Object> inputs,
                                          com.zifang.z.agent.center.core.agent.flow.FlowContext context) {
                        return inputs;
                    }
                };
            case "llm": {
                String modelName = props.get("model") != null ? props.get("model").toString() : "qwen2.5:7b";
                return new com.zifang.z.agent.center.core.agent.flow.node.LLMNode(nodeId, modelName);
            }
            case "agent": {
                String appCode = props.get("appCode") != null ? props.get("appCode").toString() : null;
                String runMode = props.get("runMode") != null ? props.get("runMode").toString() : "IN_PROCESS";
                // 调用 orchestrationService.createAgentNode 需要 AgentExecutorFactory, 这里走最简
                return svc.createAgentNode(nodeId, appCode, runMode);
            }
            case "condition": {
                String expr = props.get("expression") != null ? props.get("expression").toString() : null;
                String trueNode = props.get("trueNodeId") != null ? props.get("trueNodeId").toString() : null;
                String falseNode = props.get("falseNodeId") != null ? props.get("falseNodeId").toString() : null;
                return svc.createConditionNode(nodeId, expr, trueNode, falseNode);
            }
            case "http": {
                String url = props.get("url") != null ? props.get("url").toString() : "http://localhost";
                if (url == null || url.isEmpty()) {
                    url = "http://localhost";
                }
                String method = props.get("method") != null ? props.get("method").toString() : "GET";
                if (method == null || method.isEmpty()) {
                    method = "GET";
                }
                return new com.zifang.z.agent.center.core.agent.flow.node.HttpNode(nodeId, url, method);
            }
            case "template": {
                String tpl = props.get("template") != null ? props.get("template").toString() : "";
                return new com.zifang.z.agent.center.core.agent.flow.node.TemplateNode(nodeId, tpl);
            }
            case "human_input": {
                // FEATURE013 T3.5: 构造 HumanInputNode, 完整 config 由 props 提供
                java.util.Map<String, Object> hConfig = new java.util.HashMap<>(props);
                return new com.zifang.z.agent.center.core.agent.humaninput.node.HumanInputNode(nodeId, hConfig);
            }
            case "parallel":
                return new com.zifang.z.agent.center.core.agent.flow.node.ParallelNode(nodeId);
            case "sub_flow": {
                String subFlowId = props.get("subFlowId") != null ? props.get("subFlowId").toString() : null;
                return svc.createSubFlowNode(nodeId, subFlowId);
            }
            default:
                log.warn("[FlowDefinitionService] 未知节点类型: {}", type);
                return null;
        }
    }

    /**
     * 从 PO 重建 FlowDefinition (供 FlowOrchestrationService.execute 用)
     */
    public static com.zifang.z.agent.center.core.agent.flow.FlowDefinition toFlowDefinition(
            FlowDefinitionPO po,
            com.zifang.z.agent.center.core.agent.flow.FlowOrchestrationService svc) {
        com.zifang.z.agent.center.core.agent.flow.FlowDefinition def =
                new com.zifang.z.agent.center.core.agent.flow.FlowDefinition(po.getFlowId(), po.getName());
        def.setDescription(po.getDescription());
        def.setTimeoutMs(po.getTimeoutMs() != null ? po.getTimeoutMs() : 1800000L);

        // nodes
        Map<String, com.zifang.z.agent.center.core.agent.flow.FlowNode> nodes = buildNodes(po, svc);
        for (Map.Entry<String, com.zifang.z.agent.center.core.agent.flow.FlowNode> e : nodes.entrySet()) {
            def.addNode(e.getKey(), e.getValue());
        }

        // edges
        if (po.getEdges() != null && !po.getEdges().isEmpty()) {
            List<Map<String, Object>> arr = JsonUtil.fromJson(po.getEdges(), new com.zifang.util.json.define.TypeReference<List<Map<String, Object>>>() {
            });
            for (int i = 0; i < arr.size(); i++) {
                Map<String, Object> e = arr.get(i);
                def.addEdge(e.get("sourceNodeId") != null ? e.get("sourceNodeId").toString() : null,
                        e.get("targetNodeId") != null ? e.get("targetNodeId").toString() : null);
            }
        }
        return def;
    }

    /**
     * 启动时加载所有 PUBLISHED 状态 flow 到内存
     * 调用方传入 put 回调 (通常是 FlowOrchestrationService.registerFlow)
     */
    public int loadAllPublished(Map<String, String> flowIdVersionMap) {
        QueryWrapper<FlowDefinitionPO> qw = new QueryWrapper<>();
        qw.eq("status", "PUBLISHED").eq("is_deleted", 0);
        List<FlowDefinitionPO> list = flowDefinitionMapper.selectList(qw);
        log.info("[FlowDefinitionService] 启动加载 PUBLISHED flow 数: {}", list.size());
        for (FlowDefinitionPO po : list) {
            flowIdVersionMap.put(po.getFlowId(), po.getVersion().toString());
        }
        return list.size();
    }

    public FlowDefinitionPO getById(Long id) {
        return flowDefinitionMapper.selectById(id);
    }

    public FlowDefinitionPO getByFlowIdAndVersion(String flowId, Integer version) {
        QueryWrapper<FlowDefinitionPO> qw = new QueryWrapper<>();
        qw.eq("flow_id", flowId).eq("version", version).eq("is_deleted", 0);
        return flowDefinitionMapper.selectOne(qw);
    }

    public FlowDefinitionPO getLatestPublished(String flowId) {
        QueryWrapper<FlowDefinitionPO> qw = new QueryWrapper<>();
        qw.eq("flow_id", flowId).eq("status", "PUBLISHED").eq("is_deleted", 0)
                .orderByDesc("version").last("LIMIT 1");
        return flowDefinitionMapper.selectOne(qw);
    }

    public List<FlowDefinitionPO> listByFlowId(String flowId) {
        QueryWrapper<FlowDefinitionPO> qw = new QueryWrapper<>();
        qw.eq("flow_id", flowId).eq("is_deleted", 0).orderByDesc("version");
        return flowDefinitionMapper.selectList(qw);
    }

    public IPage<FlowDefinitionPO> page(String keyword, String status, String appCode,
                                        int pageNum, int pageSize) {
        QueryWrapper<FlowDefinitionPO> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0).orderByDesc("updated_at");
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("name", keyword).or().like("description", keyword).or().like("flow_id", keyword));
        }
        if (status != null && !status.isEmpty()) {
            qw.eq("status", status);
        }
        if (appCode != null && !appCode.isEmpty()) {
            qw.eq("app_code", appCode);
        }
        return flowDefinitionMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public Long create(FlowDefinitionPO po) {
        if (po.getVersion() == null) po.setVersion(1);

        if (po.getStatus() == null) po.setStatus("DRAFT");

        if (po.getTimeoutMs() == null) po.setTimeoutMs(1800000L);

        if (po.getFlowType() == null) po.setFlowType("AGENT");

        if (po.getIsDeleted() == null) po.setIsDeleted(0);

        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        // 校验 JSON 字段格式
        validateJson(po);
        flowDefinitionMapper.insert(po);
        log.info("[FlowDefinitionService] 创建 flow: flowId={}, version={}", po.getFlowId(), po.getVersion());
        return po.getId();
    }

    public boolean update(FlowDefinitionPO po) {
        if (po.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        validateJson(po);
        po.setUpdatedAt(LocalDateTime.now());
        int rows = flowDefinitionMapper.updateById(po);
        log.info("[FlowDefinitionService] 更新 flow: id={}, rows={}", po.getId(), rows);
        return rows > 0;
    }

    /**
     * 创建新版本 (FEATURE013 A9)
     * - 复制旧版本的 nodes/edges/variables
     * - version + 1
     * - parent_version_id = 旧版本.id
     * - status = DRAFT
     * - changelog = 用户填写
     */
    public Long createNewVersion(Long parentId, String changelog, String creator) {
        FlowDefinitionPO parent = flowDefinitionMapper.selectById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父版本不存在: " + parentId);
        }
        FlowDefinitionPO child = new FlowDefinitionPO();
        child.setFlowId(parent.getFlowId());
        child.setName(parent.getName());
        child.setDescription(parent.getDescription());
        child.setNodes(parent.getNodes());
        child.setEdges(parent.getEdges());
        child.setVariables(parent.getVariables());
        child.setTimeoutMs(parent.getTimeoutMs());
        child.setRetryPolicy(parent.getRetryPolicy());
        child.setAppCode(parent.getAppCode());
        child.setFlowType(parent.getFlowType());
        child.setVersion(parent.getVersion() + 1);
        child.setParentVersionId(parent.getId());
        child.setChangelog(changelog);
        child.setStatus("DRAFT");
        child.setCreator(creator != null ? creator : parent.getCreator());
        child.setTenantCode(parent.getTenantCode());
        return create(child);
    }

    // ==================== JSON 校验 ====================

    public boolean publish(Long id) {
        FlowDefinitionPO po = flowDefinitionMapper.selectById(id);
        if (po == null) {
            throw new IllegalArgumentException("flow 不存在: " + id);
        }
        po.setStatus("PUBLISHED");
        po.setUpdatedAt(LocalDateTime.now());
        int rows = flowDefinitionMapper.updateById(po);
        log.info("[FlowDefinitionService] 发布 flow: id={}, flowId={}, version={}",
                id, po.getFlowId(), po.getVersion());
        return rows > 0;
    }

    public boolean archive(Long id) {
        FlowDefinitionPO po = flowDefinitionMapper.selectById(id);
        if (po == null) {
            throw new IllegalArgumentException("flow 不存在: " + id);
        }
        po.setStatus("ARCHIVED");
        po.setUpdatedAt(LocalDateTime.now());
        return flowDefinitionMapper.updateById(po) > 0;
    }

    public boolean delete(Long id) {
        FlowDefinitionPO po = flowDefinitionMapper.selectById(id);
        if (po == null) {
            return false;
        }

        po.setIsDeleted(1);
        po.setUpdatedAt(LocalDateTime.now());
        return flowDefinitionMapper.updateById(po) > 0;
    }

    private void validateJson(FlowDefinitionPO po) {
        if (po.getNodes() != null && !po.getNodes().isEmpty()) {
            try {
                List<Object> arr = JsonUtil.fromJson(po.getNodes(), new com.zifang.util.json.define.TypeReference<List<Object>>() {
                });
                if (arr == null) {
                    throw new IllegalArgumentException("nodes 不是合法 JSON 数组");
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("nodes JSON 解析失败: " + e.getMessage());
            }
        }
        if (po.getEdges() != null && !po.getEdges().isEmpty()) {
            try {
                List<Object> arr = JsonUtil.fromJson(po.getEdges(), new com.zifang.util.json.define.TypeReference<List<Object>>() {
                });
                if (arr == null) {
                    throw new IllegalArgumentException("edges 不是合法 JSON 数组");
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("edges JSON 解析失败: " + e.getMessage());
            }
        }
        if (po.getVariables() != null && !po.getVariables().isEmpty()) {
            try {
                Object o = JsonUtil.toPlainValue(JsonUtil.parse(po.getVariables()));
                if (!(o instanceof Map)) {
                    // 兜底: 允许 array, 但记录 warning
                    log.warn("[FlowDefinitionService] variables 不是 map, 实际类型: {}",
                            o == null ? "null" : o.getClass().getSimpleName());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("variables JSON 解析失败: " + e.getMessage());
            }
        }
    }
}