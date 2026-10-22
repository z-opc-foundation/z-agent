import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
    plugins: [react()],
    resolve: {
        // 仓内仍有 @/ 引用 (如 src/pages/product/execute.jsx → @/agent/api),
        // standalone 构建时必须自己解析; LOCAL_SIBLINGS 模式下宿主 alias 会接管。
        alias: {
            '@': path.resolve(__dirname, 'src'),
        },
    },
    build: {
        lib: {
            entry: {
                'z-agent-component': './src/index.js',
                'z-agent-pages': './src/pages-manifest.jsx',
            },
            formats: ['es'],
        },
        rollupOptions: {
            external: (id) => [
                'react', 'react-dom', 'react-dom/client', 'react-router-dom',
                'antd', '@ant-design/icons', 'axios',
                '@yuku123/z-frontend-common',
                'echarts', 'echarts-for-react',
                '@logicflow/core', '@logicflow/extension',
                'react-markdown', 'remark-gfm',
            ].includes(id)
                // echarts/* 与 echarts-for-react/lib/* 同样要 external。
                // 2026-10-22: 图表改成 tree-shaken 后源码引的是子路径
                // （echarts/core、echarts/charts、echarts/components、echarts/renderers、
                //  echarts-for-react/lib/core）。若这些子路径不 external，
                // 本包会把 800KB+ 的 echarts **打进自己的 dist** ——
                // 既让包体凭空膨胀，又绕开了宿主侧"按需注册"的 tree-shaking 能力。
                // external 后由宿主（主壳）用自己的 node_modules 解析。
                || id.startsWith('echarts/')
                || id.startsWith('echarts-for-react/'),
        },
    },
})
