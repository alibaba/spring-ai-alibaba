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
  },
  routes: [
    {
      path: '/',
      redirect: '/home',
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
      path: '/app/:tab',
      component: 'App/index',
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
  tailwindcss: {},
  esbuildMinifyIIFE: true,
  mfsu: false,
  plugins: ['@umijs/plugins/dist/tailwindcss'],
  lessLoader: {
    javascriptEnabled: true,
    modifyVars: {
      '@ant-prefix': 'ag-ant',
    },
  },
});
