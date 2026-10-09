import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
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
