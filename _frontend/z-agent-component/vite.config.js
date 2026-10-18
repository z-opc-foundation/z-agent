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
            external: [
                'react', 'react-dom', 'react-dom/client', 'react-router-dom',
                'antd', '@ant-design/icons', 'axios',
                '@yuku123/z-frontend-common',
                'echarts', 'echarts-for-react',
                '@logicflow/core', '@logicflow/extension',
                'react-markdown', 'remark-gfm',
            ],
        },
    },
})
