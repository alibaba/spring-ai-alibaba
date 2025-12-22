import path from 'path';
import { defineConfig } from 'umi';

export default defineConfig({
  title: 'SAA',
  define: {
    'process.env.WEB_SERVER': process.env.WEB_SERVER,
    'process.env.BACK_END': process.env.BACK_END,
    'process.env.DEFAULT_USERNAME': process.env.DEFAULT_USERNAME,
    'process.env.DEFAULT_PASSWORD': process.env.DEFAULT_PASSWORD,
    BUILD_ID: new Date().toString(),
  },
  alias: {
    '@src': path.resolve(__dirname, './src'),
    '@': path.resolve(__dirname, './src'),
    '@spark-ai/flow': path.resolve(__dirname, '../spark-flow/dist'),
  },
  routes: [
    {
      path: '/',
      redirect: '/app',
    },
    {
      path: '/admin',
      routes: [
        { path: '/admin', component: '@/legacy/pages/index' },
        { path: '/admin/playground', component: '@/legacy/pages/playground/playground' },
        { path: '/admin/prompts', component: '@/legacy/pages/prompts/prompts' },
        { path: '/admin/prompt-detail', component: '@/legacy/pages/prompts/prompt-detail/prompt-detail' },
        { path: '/admin/version-history', component: '@/legacy/pages/prompts/version-history/version-history' },
        { path: '/admin/tracing', component: '@/legacy/pages/tracing/tracing' },
        { path: '/admin/evaluation/experiment', component: '@/legacy/pages/evaluation/experiment/index' },
        { path: '/admin/evaluation/experiment/create', component: '@/legacy/pages/evaluation/experiment/experimentCreate' },
        { path: '/admin/evaluation/experiment/detail/:id', component: '@/legacy/pages/evaluation/experiment/experimentDetail' },
        { path: '/admin/evaluation/gather', component: '@/legacy/pages/evaluation/gather/index' },
        { path: '/admin/evaluation/gather/create', component: '@/legacy/pages/evaluation/gather/gatherCreate' },
        { path: '/admin/evaluation/gather/detail/:id', component: '@/legacy/pages/evaluation/gather/gatherDetail' },
        { path: '/admin/evaluation/evaluator', component: '@/legacy/pages/evaluation/evaluator/index' },
        { path: '/admin/evaluation/evaluator/:id', component: '@/legacy/pages/evaluation/evaluator/evaluator-detail' },
        { path: '/admin/evaluation/debug', component: '@/legacy/pages/evaluation/evaluator/evaluator-debug' },
      ],
    },
    {
      path: '/app/assistant/:id',
      component: 'App/AssistantAppEdit',
    },
    {
      path: '/app/workflow/:id',
      component: 'App/Workflow',
    },
    {
      path: '/app',
      component: 'App/AppList',
    },
    {
      path: '/app/:tab',
      component: 'App/AppList',
    },
    {
      path: '/home',
      component: 'App/index',
    },
    {
      path: '/dify',
      component: 'Dify/index',
    },
    {
      path: '/debug',
      component: 'Debug/index',
    },
    {
      path: '/login',
      component: 'Login/index',
      layout: false,
    },
    {
      path: '/mcp',
      component: 'MCP/index',
    },
    {
      path: '/mcp/create',
      component: 'MCP/Create',
    },
    {
      path: '/mcp/edit/:id',
      component: 'MCP/Create',
    },
    {
      path: '/mcp/detail/:id',
      component: 'MCP/Detail',
    },
    {
      path: '/component/:tab',
      component: 'Component/index',
    },
    {
      path: '/component',
      redirect:
        process.env.BACK_END === 'python'
          ? '/component/flow'
          : '/component/plugin',
    },
    {
      path: '/component/plugin/create',
      component: 'Component/Plugin/Info/Create',
    },
    {
      path: '/component/plugin/:id',
      component: 'Component/Plugin/Info/Edit',
    },
    {
      path: '/component/plugin/:id/tool/create',
      component: 'Component/Plugin/Tools/Edit',
    },
    {
      path: '/component/plugin/:id/tool/:toolId',
      component: 'Component/Plugin/Tools/Edit',
    },
    {
      path: '/component/plugin/:id/tools',
      component: 'Component/Plugin/Tools/List',
    },
    {
      path: '/knowledge',
      component: 'Knowledge/List/index',
    },
    {
      path: '/knowledge/:kb_id',
      component: 'Knowledge/Detail/index',
    },
    {
      path: '/knowledge/test/:kb_id',
      component: 'Knowledge/Test/index',
    },
    {
      path: '/knowledge/create',
      component: 'Knowledge/Create/index',
    },
    {
      path: '/knowledge/edit/:kb_id',
      component: 'Knowledge/Editor/index',
    },
    {
      path: '/knowledge/sliceConfiguration/:kb_id/:doc_id',
      component: 'Knowledge/Detail/SliceConfiguration/index',
    },
    {
      path: '/knowledge/sliceEditing/:kb_id/:doc_id',
      component: 'Knowledge/Detail/SliceEditing/index',
    },
    {
      path: '/setting',
      redirect: '/setting/modelService',
    },
    {
      path: '/setting/modelService',
      component: 'Setting/ModelService',
    },
    {
      path: '/setting/modelService/:id',
      component: 'Setting/ModelService/Detail',
    },
    {
      path: '/setting/account',
      component: 'Setting/Account',
    },
    {
      path: '/setting/apiKeys',
      component: 'Setting/APIKeys',
    },
    {
      path: '/agent-schema',
      component: 'AgentSchema/index',
    },
  ],
  clickToComponent: {},
  // tailwindcss: {},
  esbuildMinifyIIFE: true,
  mfsu: false,
  plugins: [
    // '@umijs/plugins/dist/tailwindcss'
  ],
  proxy: {
    '/api': {
      target: process.env.WEB_SERVER || 'http://localhost:8080',
      changeOrigin: true,
    },
    '/console': {
      target: process.env.WEB_SERVER || 'http://localhost:8080',
      changeOrigin: true,
    },
    '/oauth2': {
      target: process.env.WEB_SERVER || 'http://localhost:8080',
      changeOrigin: true,
    },
  },
  lessLoader: {
    javascriptEnabled: true,
    modifyVars: {
      '@ant-prefix': 'ag-ant',
    },
  },
});
