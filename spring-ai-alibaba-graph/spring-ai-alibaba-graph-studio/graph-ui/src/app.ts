// 运行时配置

// 全局初始化数据配置，用于 Layout 用户信息和权限初始化
// 更多信息见文档：https://umijs.org/docs/api/runtime-config#getinitialstate
import { RunTimeLayoutConfig } from '@@/plugin-layout/types';
import logo from './assets/logo.png';

export async function getInitialState(): Promise<{ name: string }> {
  return { name: '' };
}
export const layout: RunTimeLayoutConfig = () => {
  return {
    logo: logo,
    menu: {
      locale: true,
    },
  };
};
