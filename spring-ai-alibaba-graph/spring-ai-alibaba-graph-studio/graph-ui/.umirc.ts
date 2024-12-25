import { defineConfig } from '@umijs/max';
import { DEFAULT_NAME } from './src/constants';
export default defineConfig({
  title: 'site.title',
  antd: {
    configProvider: {
      theme: { cssVar: true },
    },
    // dark: true
  },
  access: {},
  model: {},

  initialState: {},
  request: {},
  layout: {
    title: DEFAULT_NAME,
  },
  locale: {
    // 默认使用 src/locales/zh-CN.ts 作为多语言文件
    default: 'zh-CN',
    baseSeparator: '-',
    antd: true,
    useLocalStorage: true,
    title: true,
    baseNavigator: true,
  },
  valtio: {
  },
  routes: [
    {
      path: '/',
      redirect: '/graph',
    },
    {
      path: '/home',
      component: './Home',
      title: 'router.home',
    },
    {
      title: 'router.chatbot',
      path: '/chatbot',
      component: './Chatbot',
    },
    {
      title: 'router.agent',
      path: '/agent',
      component: './Agent',
    },
    {
      title: 'router.graph',
      path: '/graph',
      component: './Graph',
    },
    {
      hide: true,
      path: '/graph/design',
      component: './Graph/Design',
    },
  ],
  npmClient: 'pnpm',
  chainWebpack: config=>{

  }
});
