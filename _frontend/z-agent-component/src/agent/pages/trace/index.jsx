import React from 'react';
import {Empty} from 'antd';

/**
 * Agent Trace 路由壳子（/z-agent/trace）
 *
 * ⚠️ 2026-10-22 TASK-20260925-037 R3：本页原来只渲染一个 `<Outlet/>` —— 路由在、子路由不在 ⇒ 渲染出**空白页**。
 *    它下面的 DebugPlayground / MetricsDashboard / RagFragments / ToolCallTree / Timeline 共 906 行，
 *    实测**引用数 = 0**（口径与证据见 lead 仓
 *    002_项目需求/003_定时任务空间/evidence/TASK-20260925-037/r3_deadcode_reference_count.txt），
 *    已删除；删除前**没有**补 traceApi 导出 / reg 行 —— 那一处坏导入在 0 引用的死文件里，
 *    补上只会把"不执行的面"变成"恒空的面"（指标变好、真实变差）。
 *
 *    宿主（z-opc）当前**没有** /api/agent/trace/* 端点（实测 :8888 /actuator/mappings 1377 条 predicate 里 0 条），
 *    ⇒ 这一面按 004 的"宁占位不接假数据"处理：明写未孵化，不接假数据。
 */
const TraceIndex = () => (
    <div style={{padding: 48, textAlign: 'center'}}>
        <Empty description="调用追踪尚未孵化：宿主没有 /api/agent/trace/* 端点（本页不接假数据）"/>
    </div>
);

export default TraceIndex;
