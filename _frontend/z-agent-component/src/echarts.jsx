/**
 * z-agent-component 的 echarts 统一入口 —— tree-shaken（2026-10-22 包体优化）。
 *
 * **改之前**：3 个图表页直接 `import ReactECharts from 'echarts-for-react'`，
 * 而 echarts-for-react 的默认入口内部 `import * as echarts from 'echarts'` ——
 * **全量包**（190+ 图表类型 + 全部组件）。
 * 实测（vite lib build, esbuild minify）：全量 echarts 单包 **1,549.5 KB raw / 422.7 KB gzip**。
 * 宿主主壳把它整个打进 z-agent-component chunk ⇒ 只画 bar/line 两种图，却为 190+ 种图表类型付费。
 *
 * **改之后**：走 `echarts-for-react/lib/core`（该入口**不从 prop 之外的任何地方引 echarts**，
 * 由调用方通过 `echarts` prop 注入实例）+ `echarts/core` 按需注册。
 * 实测同一口径：**843.7 KB raw / 229.7 KB gzip** ⇒ 省约 700KB raw / **190KB gzip**。
 *
 * ⚠ 本包实际用到的 series 只有 bar / line（扫过 3 个图表页：
 *   dashboard/index.jsx、usage/index.jsx、ak/AkUsageDrawer.jsx），
 *   组件用到 grid / legend / title / tooltip。未用到故未注册：
 *   pie / scatter / graph / heatmap / gauge / sankey / treemap / sunburst 等。
 *
 * ⚠ 新增图表类型时**必须**在本文件补注册，否则该图运行时抛
 *   "Series xxx is not exists"（echarts-for-react 不 catch，直接冒泡成渲染错误）。
 *
 * ⚠ 注意本仓有**两套并行页面树**：`src/pages/**`（src/index.js 入口）与
 *   `src/agent/pages/**`（src/pages-manifest.jsx 入口）。两个入口都会被打进产物，
 *   ⇒ 改图表引入时**两边都要改**，否则只有一半页面瘦身。
 */
import * as echarts from 'echarts/core';
import {BarChart, LineChart} from 'echarts/charts';
import {
    GridComponent,
    LegendComponent,
    TitleComponent,
    TooltipComponent,
} from 'echarts/components';
import {CanvasRenderer} from 'echarts/renderers';
import ReactEChartsCore from 'echarts-for-react/lib/core';

echarts.use([
    BarChart,
    LineChart,
    GridComponent,
    LegendComponent,
    TitleComponent,
    TooltipComponent,
    CanvasRenderer,
]);

export {echarts};

/**
 * 与 `echarts-for-react` 默认导出的**等价 drop-in 包装**。
 * 差别只有一处：把 tree-shaken 的 echarts 实例通过 prop 注入，
 * 而不是在库里静态引全量包。其它 props（option / style / onEvents …）原样透传。
 */
export default function ReactECharts(props) {
    return <ReactEChartsCore echarts={echarts} {...props}/>;
}
