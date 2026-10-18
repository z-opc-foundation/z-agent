package com.zifang.z.agent.center.core.agent.humaninput.node;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import com.zifang.z.agent.center.core.agent.humaninput.entity.AgentFlowHumanInput;
import com.zifang.z.agent.center.core.agent.humaninput.service.AgentFlowHumanInputService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE013 T3.5: Human-in-the-Loop 流程节点
 *
 * <p>节点执行语义:
 * <ol>
 *     <li>构造一条 PENDING 的 human-input 请求 (z_agent_flow_human_input), 含 prompt / form_schema / assignee</li>
 *     <li>在节点线程内轮询请求状态 (默认 2s 一次), 直至决策 (APPROVED/REJECTED/CANCELLED) 或超时</li>
 *     <li>返回 Map { status, decidedBy, resultText, resultJson, requestCode } 供后续节点使用</li>
 * </ol>
 *
 * <p>设计取舍:
 * <ul>
 *     <li>同步阻塞式: 简单可控, 适合 demo + 中小规模; 大规模应用建议拆分为子流程 + 异步事件 (FEATURE017 演进)</li>
 *     <li>超时默认 5 分钟 (可由 node config.pollTimeoutMs 覆盖), 单次 poll 间隔 2s</li>
 *     <li>不需要 Spring 容器: 节点用懒加载 ServiceLocator 拿到 service (FlowEngine 是手工 new 出来的)</li>
 * </ul>
 *
 * <p>节点 config (JSON):
 * <pre>
 * {
 *   "prompt": "请审批订单 #${orderId}, 金额 ${amount}",
 *   "assignee": "manager_alice",
 *   "approverRole": "manager",
 *   "priority": "HIGH",
 *   "pollIntervalMs": 2000,
 *   "pollTimeoutMs": 300000,
 *   "formSchema": [{"name":"approve","label":"是否通过","type":"boolean","required":true},
 *                  {"name":"note","label":"审批意见","type":"text","required":false}]
 * }
 * </pre>
 */
public class HumanInputNode implements FlowNode {

    // ==================== FEATURE013 T3.5 节点类型常量 ====================
    public static final String TYPE_HUMAN_INPUT = "human_input";
    private static final Logger log = LogManager.getLogger(HumanInputNode.class);
    /**
     * Spring service 懒加载定位器 (FlowEngine 内部手工 new 节点, 不走 Spring 注入)
     */
    private static AgentFlowHumanInputService SERVICE_LOCATOR;
    private final String nodeId;
    private final Map<String, Object> config;

    public HumanInputNode(String nodeId, Map<String, Object> config) {
        this.nodeId = nodeId;
        this.config = config == null ? new HashMap<>() : config;
    }

    public static void registerServiceLocator(AgentFlowHumanInputService service) {
        SERVICE_LOCATOR = service;
        log.info("[HumanInputNode] ServiceLocator 已注册: {}", service.getClass().getSimpleName());
    }

    @Override
    public String getType() {
        return TYPE_HUMAN_INPUT;
    }

    @Override
    public long timeoutMs() {
        Object t = config.get("pollTimeoutMs");
        if (t instanceof Number) return ((Number) t).longValue() + 5000L; // 留 5s 余量给 FlowEngine latch
        return 5L * 60_000L + 5_000L; // 默认 5 分钟
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        if (SERVICE_LOCATOR == null) {
            throw new IllegalStateException("HumanInputNode ServiceLocator 未注册, 请在 Spring 启动时调用 HumanInputNode.registerServiceLocator(...)");
        }
        long pollInterval = getLongConfig("pollIntervalMs", 2000L);
        long pollTimeout = getLongConfig("pollTimeoutMs", 5L * 60_000L);

        // 1) 解析 prompt 模板 (替换 ${var} 占位符)
        String prompt = String.valueOf(config.getOrDefault("prompt", "请处理: " + nodeId));
        prompt = renderTemplate(prompt, inputs, context);

        // 2) 构造请求
        AgentFlowHumanInput req = new AgentFlowHumanInput();
        req.setRequestCode(null); // 让 service 自动生成
        req.setFlowCode(context.getRootFlowId());
        req.setExecutionId(context.getRootFlowId());
        req.setNodeId(nodeId);
        req.setNodeName(String.valueOf(config.getOrDefault("nodeName", nodeId)));
        req.setAppCode(String.valueOf(config.getOrDefault("appCode", "")));
        req.setPrompt(prompt);
        try {
            Object inputData = inputs.isEmpty() ? null : inputs;
            req.setInputData(inputData == null ? null : JsonUtil.toJsonExcludingNulls(inputData));
        } catch (Exception e) {
            log.warn("[HumanInputNode] inputData 序列化失败: {}", e.getMessage());
        }
        Object formSchema = config.get("formSchema");
        if (formSchema != null) {
            try {
                req.setFormSchema(JsonUtil.toJsonExcludingNulls(formSchema));
            } catch (Exception ignored) {
            }
        }
        req.setAssignee(stringConfig("assignee", null));
        req.setApproverRole(stringConfig("approverRole", null));
        req.setPriority(stringConfig("priority", "NORMAL"));
        Object dueAt = config.get("dueAt");
        if (dueAt != null) {
            try {
                req.setDueAt(java.time.LocalDateTime.parse(String.valueOf(dueAt)));
            } catch (Exception ignored) {
            }
        }
        req.setRequesterId(context.getUserId());

        AgentFlowHumanInput created = SERVICE_LOCATOR.createPending(req);
        log.info("[HumanInputNode] 创建 PENDING 请求 id={} requestCode={} node={} assignee={}",
                created.getId(), created.getRequestCode(), nodeId, created.getAssignee());

        // 3) 轮询状态
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("HumanInputNode 等待被中断: " + nodeId, ie);
            }
            AgentFlowHumanInput cur = SERVICE_LOCATOR.getById(created.getId());
            if (cur == null) {
                throw new RuntimeException("HumanInputNode 请求丢失 id=" + created.getId());
            }
            String st = cur.getStatus();
            if (st != null && !"PENDING".equals(st)) {
                log.info("[HumanInputNode] 收到决策: id={} status={} decidedBy={} elapsed={}ms",
                        cur.getId(), st, cur.getDecidedBy(), System.currentTimeMillis() - start);
                Map<String, Object> out = new HashMap<>();
                out.put("status", st);
                out.put("decidedBy", cur.getDecidedBy());
                out.put("resultText", cur.getResultText());
                out.put("resultJson", cur.getResultJson());
                out.put("comments", cur.getComments());
                out.put("requestCode", cur.getRequestCode());
                out.put("nodeId", nodeId);
                out.put("approved", "APPROVED".equals(st));
                return out;
            }
            if (System.currentTimeMillis() - start > pollTimeout) {
                log.warn("[HumanInputNode] 等待超时 id={} elapsed={}ms, 自动标记 TIMEOUT", created.getId(), System.currentTimeMillis() - start);
                try {
                    SERVICE_LOCATOR.decide(created.getId(), "REJECTED", "system_timeout",
                            "{}", "TIMEOUT", "超过 " + pollTimeout + "ms 未决策, 系统自动拒绝");
                } catch (Exception ee) {
                    log.warn("[HumanInputNode] 超时标记失败 (可能已被决策): {}", ee.getMessage());
                }
                Map<String, Object> timeout = new HashMap<>();
                timeout.put("status", "TIMEOUT");
                timeout.put("approved", false);
                timeout.put("requestCode", created.getRequestCode());
                timeout.put("nodeId", nodeId);
                timeout.put("error", "HumanInputNode 超时未决策");
                return timeout;
            }
        }
    }

    @Override
    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }

    @Override
    public boolean validate() {
        Object prompt = config.get("prompt");
        return prompt != null && !String.valueOf(prompt).trim().isEmpty();
    }

    private long getLongConfig(String key, long def) {
        Object v = config.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }

        if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private String stringConfig(String key, String def) {
        Object v = config.get(key);
        if (v == null) {
            return def;
        }

        String s = String.valueOf(v);
        return s.isEmpty() ? def : s;
    }

    /**
     * 简易 ${var} 占位符替换: inputs / context.variables / context.user_message
     */
    private String renderTemplate(String tpl, Map<String, Object> inputs, FlowContext context) {
        if (tpl == null) {
            return "";
        }

        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher m = p.matcher(tpl);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1).trim();
            Object val = null;
            if (inputs != null && inputs.containsKey(var)) {
                val = inputs.get(var);
            } else if (context != null) {
                if ("user_message".equals(var) || "_user_message".equals(var)) {
                    val = context.getVariable("_user_message");
                } else {
                    val = context.getVariable(var);
                }
            }
            String rep = val == null ? "" : String.valueOf(val);
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
