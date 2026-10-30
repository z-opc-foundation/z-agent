package com.zifang.z.agent.center.core.agent.flow;

import com.zifang.z.agent.center.core.agent.executor.AgentExecutor;
import com.zifang.z.agent.center.core.agent.executor.AgentExecutorFactory;
import com.zifang.z.agent.center.core.agent.executor.ReActAgentFactory;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowDefinitionPO;
import com.zifang.z.agent.center.core.agent.flow.service.FlowDefinitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程编排服务 - 管理和执行流程 (FEATURE013 A3 改造: 从内存注册改为 DB 加载)
 *
 * <p>兼容性:
 * <ul>
 *   <li>registerFlow(...) 仍保留 (向后兼容 demo / 单元测试)</li>
 *   <li>getFlow(flowId) 优先从 in-memory cache 取; miss 时回源到 DB 加载</li>
 *   <li>execute(flowId, context) 行为不变</li>
 * </ul>
 */
@Service
public class FlowOrchestrationService {

    private static final Logger log = LogManager.getLogger(FlowOrchestrationService.class);

    private final FlowEngine flowEngine;
    private final AgentExecutorFactory agentExecutorFactory;
    private final ReActAgentFactory reActAgentFactory;
    /**
     * 启动时从 DB 加载的流程缓存 (FEATURE013 A3: 替代原 ConcurrentHashMap 全量注册)
     */
    private final Map<String, FlowDefinition> flowDefinitions = new ConcurrentHashMap<>();
    /**
     * 正在执行的流程 Map<executionId, FlowResult>
     */
    private final Map<String, FlowResult> runningFlows = new ConcurrentHashMap<>();
    /**
     * FEATURE013 A7: 流程追踪记录器 (Spring @Autowired)
     */
    @Autowired
    private FlowTraceRecorder flowTraceRecorder;
    @Autowired
    private FlowDefinitionService flowDefinitionService;

    public FlowOrchestrationService(AgentExecutorFactory agentExecutorFactory, ReActAgentFactory reActAgentFactory) {
        this.agentExecutorFactory = agentExecutorFactory;
        this.reActAgentFactory = reActAgentFactory;
        this.flowEngine = new FlowEngine(4); // 4个并发
    }

    @PostConstruct
    public void init() {
        // FEATURE013 A7: 把追踪记录器注入到 FlowEngine
        if (flowTraceRecorder != null) {
            flowEngine.setTraceRecorder(flowTraceRecorder);
            log.info("[FlowOrchestrationService] 已注入 FlowTraceRecorder (FEATURE013 A7)");
        }
        // FEATURE013 A3: 启动时从 DB 加载所有 PUBLISHED 状态的 flow
        loadAllPublishedFromDb();
    }

    /**
     * FEATURE013 A3: 从 DB 加载所有 PUBLISHED 状态 flow 到内存缓存
     * 后续新增 / 更新 flow 时由 Controller 显式调用 refreshFlowCache(flowId)
     */
    private void loadAllPublishedFromDb() {
        try {
            int loaded = flowDefinitionService.loadAllPublished(new java.util.HashMap<>());
            // 实际装载 flowDefinition
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FlowDefinitionPO> qw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("status", "PUBLISHED").eq("is_deleted", 0);
            java.util.List<FlowDefinitionPO> list = flowDefinitionService.page(null, "PUBLISHED", null, 1, 1000)
                    .getRecords();
            for (FlowDefinitionPO po : list) {
                FlowDefinition def = FlowDefinitionService.toFlowDefinition(po, this);
                if (def.isDAG()) {
                    flowDefinitions.put(po.getFlowId(), def);
                    log.info("[FlowOrchestrationService] 启动加载 flow: flowId={}, version={}",
                            po.getFlowId(), po.getVersion());
                } else {
                    log.warn("[FlowOrchestrationService] 跳过非 DAG flow: flowId={}, version={}",
                            po.getFlowId(), po.getVersion());
                }
            }
            log.info("[FlowOrchestrationService] 启动加载完成: 共 {} 条 PUBLISHED flow", loaded);
        } catch (Exception e) {
            log.error("[FlowOrchestrationService] 启动加载 PUBLISHED flow 失败 (DB 未就绪?)", e);
        }
    }

    /**
     * FEATURE013 A3: 刷新指定 flowId 的缓存 (Controller 保存 / 发布后调用)
     */
    public void refreshFlowCache(String flowId) {
        FlowDefinitionPO latestPublished = flowDefinitionService.getLatestPublished(flowId);
        if (latestPublished == null) {
            flowDefinitions.remove(flowId);
            log.info("[FlowOrchestrationService] 刷新 flow cache: 移除 (无 PUBLISHED) flowId={}", flowId);
            return;
        }
        FlowDefinition def = FlowDefinitionService.toFlowDefinition(latestPublished, this);
        flowDefinitions.put(flowId, def);
        log.info("[FlowOrchestrationService] 刷新 flow cache: flowId={}, version={}",
                flowId, latestPublished.getVersion());
    }

    /**
     * 注册流程定义 (保留向后兼容)
     * 新的保存流程请走 FlowDefinitionService.create(...) + refreshFlowCache(flowId)
     */
    public void registerFlow(FlowDefinition flow) {
        if (!flow.isDAG()) {
            throw new IllegalArgumentException("Flow is not a valid DAG: " + flow.getFlowId());
        }
        flowDefinitions.put(flow.getFlowId(), flow);
        log.info("Registered flow (in-memory only): {}", flow.getFlowId());
    }

    /**
     * 获取流程定义 (FEATURE013 A3: miss 时回源 DB)
     */
    public FlowDefinition getFlow(String flowId) {
        FlowDefinition cached = flowDefinitions.get(flowId);
        if (cached != null) {
            return cached;
        }

        // miss → 查 DB
        FlowDefinitionPO po = flowDefinitionService.getLatestPublished(flowId);
        if (po == null) {
            return null;
        }

        FlowDefinition def = FlowDefinitionService.toFlowDefinition(po, this);
        flowDefinitions.put(flowId, def);
        return def;
    }

    /**
     * 执行流程
     */
    public FlowResult execute(String flowId, FlowContext context) {
        FlowDefinition flow = getFlow(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("Flow not found: " + flowId);
        }

        log.info("Executing flow: {} for user: {}", flowId, context.getUserId());

        FlowResult result = flowEngine.execute(flow, context);

        if (result.isSuccess()) {
            log.info("Flow {} completed successfully in {}ms", flowId, result.getElapsedMs());
        } else {
            log.warn("Flow {} completed with {} errors", flowId, result.getErrors().size());
        }

        return result;
    }

    /**
     * 执行流程（简化版，使用实例编码）
     */
    public FlowResult execute(String flowId, String instanceCode, String userMessage,
                              String userId, String userName, Map<String, Object> initialVars) {
        FlowContext context = new FlowContext(userId, userName);
        context.setVariable("_user_message", userMessage);
        if (initialVars != null) {
            for (Map.Entry<String, Object> entry : initialVars.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        return execute(flowId, context);
    }

    /**
     * 创建Agent节点
     */
    public FlowNode createAgentNode(String nodeId, String agentInstanceCode, String runMode) {
        AgentExecutor executor = agentExecutorFactory.getExecutor(runMode);
        return new com.zifang.z.agent.center.core.agent.flow.node.AgentNode(
                nodeId, agentInstanceCode, runMode, executor
        );
    }

    /**
     * 创建子流程节点
     */
    public FlowNode createSubFlowNode(String nodeId, String subFlowId) {
        return new SubFlowNode(nodeId, subFlowId, this);
    }

    /**
     * 创建条件节点
     */
    public FlowNode createConditionNode(String nodeId, String expression,
                                        String trueNodeId, String falseNodeId) {
        return new ConditionNode(nodeId, expression, trueNodeId, falseNodeId);
    }

    /**
     * 获取运行中的流程
     */
    public FlowResult getRunningFlow(String executionId) {
        return runningFlows.get(executionId);
    }

    /**
     * 取消流程执行
     */
    public boolean cancelFlow(String executionId) {
        FlowResult result = runningFlows.get(executionId);
        if (result != null) {
            log.info("Cancelling flow execution: {}", executionId);
            return true;
        }
        return false;
    }

    /**
     * 获取所有已注册的流程 (FEATURE013 A3: 仅返回 in-memory 缓存, 不穿透 DB)
     */
    public Map<String, FlowDefinition> getAllFlows() {
        return flowDefinitions;
    }

    /**
     * 子流程节点
     */
    static class SubFlowNode implements FlowNode {
        private final String nodeId;
        private final String subFlowId;
        private final FlowOrchestrationService orchestrationService;

        public SubFlowNode(String nodeId, String subFlowId, FlowOrchestrationService orchestrationService) {
            this.nodeId = nodeId;
            this.subFlowId = subFlowId;
            this.orchestrationService = orchestrationService;
        }

        @Override
        public String getType() {
            return TYPE_SUB_FLOW;
        }

        @Override
        public Object execute(Map<String, Object> inputs, FlowContext context) {
            FlowContext subContext = context.createSubContext(context.getRootFlowId(), nodeId);
            subContext.setVariable("_sub_flow_input", inputs);

            FlowResult result = orchestrationService.execute(subFlowId, subContext);

            // 合并子流程结果
            context.mergeSubContext(result.getContext());

            return result.isSuccess() ? result.getOutputs() : null;
        }
    }

    /**
     * 条件节点 (FEATURE013 A6: 表达式用 SpEL 引擎, 支持 ${var} > 10 / context.xxx / 输入参数)
     */
    static class ConditionNode implements FlowNode {
        // FEATURE013 A6: Spring SpEL 解析器, 比手撕字符串 split 强很多
        private static final org.springframework.expression.ExpressionParser SPEL_PARSER =
                new org.springframework.expression.spel.standard.SpelExpressionParser();
        private final String nodeId;
        private final String expression;
        private final String trueNodeId;
        private final String falseNodeId;

        public ConditionNode(String nodeId, String expression, String trueNodeId, String falseNodeId) {
            this.nodeId = nodeId;
            this.expression = expression;
            this.trueNodeId = trueNodeId;
            this.falseNodeId = falseNodeId;
        }

        @Override
        public String getType() {
            return TYPE_CONDITION;
        }

        @Override
        public long timeoutMs() {
            return 10_000L; // 条件求值应很快
        }

        @Override
        public Object execute(Map<String, Object> inputs, FlowContext context) {
            // FEATURE013 A6: SpEL 求值
            boolean condition = evaluateCondition(expression, inputs, context);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("condition_met", condition);
            result.put("next_node", condition ? trueNodeId : falseNodeId);

            return result;
        }

        /**
         * FEATURE013 A6: SpEL 求值, 上下文 = inputs (前置节点输出) + FlowContext.variables
         */
        private boolean evaluateCondition(String expr, Map<String, Object> inputs, FlowContext context) {
            if (expr == null || expr.trim().isEmpty()) {
                return false;
            }
            try {
                // 构造 SpEL 上下文
                org.springframework.expression.EvaluationContext ec =
                        new org.springframework.expression.spel.support.StandardEvaluationContext();
                if (inputs != null) {
                    for (Map.Entry<String, Object> e : inputs.entrySet()) {
                        ec.setVariable(e.getKey(), e.getValue());
                    }
                }
                if (context != null) {
                    for (String varName : context.getVariables().keySet()) {
                        ec.setVariable(varName, context.getVariable(varName));
                    }
                    // 用户消息快捷访问
                    Object userMsg = context.getVariable("_user_message");
                    if (userMsg != null) {
                        ec.setVariable("user_message", userMsg);
                    }

                }
                Object value = SPEL_PARSER.parseExpression(expr).getValue(ec);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }

                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }

                if (value instanceof Number) {
                    return ((Number) value).doubleValue() != 0;
                }

                if (value != null) {
                    return true;
                }

                return false;
            } catch (Exception e) {
                // 求值失败兜底为 false, 不阻塞流程
                return false;
            }
        }
    }
}