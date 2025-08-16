import $i18n from '@/i18n';
import { matchRoutes } from 'umi';

console.log(
  // @ts-ignore
  `%cBUILD_ID: ${BUILD_ID}`,
  'color: #fff; background: #615ced; font-size: 10px;border-radius:6px;padding:2px 4px;',
);

// Initialize window.g_config
// @ts-ignore
window.g_config = {
  user: {},
  config: {},
};

// @ts-ignore
export function onRouteChange({ clientRoutes, location }) {
  const route = matchRoutes(clientRoutes, location.pathname)?.pop()?.route;

  const firstLevelRouteMaps = {
    '/app': $i18n.get({
      id: 'main.layouts.MenuList.application',
      dm: '应用',
    }),
    '/mcp': 'MCP',
    '/component': $i18n.get({
      id: 'main.pages.Component.AppComponent.index.component',
      dm: '组件',
    }),
    '/knowledge': $i18n.get({
      id: 'main.pages.Knowledge.Test.index.knowledgeBase',
      dm: '知识库',
    }),
    '/setting': $i18n.get({
      id: 'main.pages.Setting.ModelService.Detail.setting',
      dm: '设置',
    }),
    '/login': $i18n.get({
      id: 'main.pages.Login.components.Register.index.login',
      dm: '登录',
    }),
    '/debug': $i18n.get({
      id: 'main.pages.Debug.index.title',
      dm: 'Agent Chat UI',
    }),
    '/dify': $i18n.get({
      id: 'main.pages.Dify.index.title',
      dm: 'Dify转换',
    }),
  };

  Object.entries(firstLevelRouteMaps).some((item) => {
    if (route?.path?.startsWith(item[0])) {
      document.title = `SAA - ${item[1]}`;
      return true;
    } else {
      return false;
    }
  });
}
