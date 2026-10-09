import React from 'react';
import {Outlet} from 'react-router-dom';

/**
 * Agent Trace 路由壳子 (FEATURE013 T7)
 * 子路由:
 *   - /ai/trace/debug  -> DebugPlayground (默认重定向)
 */
const TraceIndex = () => {
    return <Outlet/>;
};

export default TraceIndex;