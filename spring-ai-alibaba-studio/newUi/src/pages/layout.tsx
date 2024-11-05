import { createElement } from 'react';
import ProLayout, { DefaultFooter } from '@ant-design/pro-layout';
import { Link } from 'ice'; // 用于实现路由跳转
import { Outlet } from 'ice'; // 用于渲染子路由组件
import { useLocation } from 'ice';
import { asideMenuConfig } from './menuConfig';

// 菜单递归处理
const loopMenuItem = (menus) =>
  menus.map(({ icon, children, ...item }) => ({
    ...item,
    icon: createElement(icon), // 动态渲染图标
    children: children && loopMenuItem(children),
  }));



export default function Layout() {
  const location = useLocation();
  return (
    <ProLayout
      title="alibaba-studio"
      style={{
        minHeight: '100vh', // 设置最小高度为 100vh
      }}
      location={{
        pathname: location.pathname, // 当前路径
      }}
      menuDataRender={() => loopMenuItem(asideMenuConfig)} // 渲染菜单项
      menuItemRender={(item, defaultDom) => {
        if (!item.path) {
          return defaultDom; // 如果菜单项没有 path，直接渲染默认的 DOM
        }
        return <Link to={item.path}>{defaultDom}</Link>; // 使用 Link 实现路由跳转
      }}
      footerRender={() => (
        <DefaultFooter
          links={[
            {
              key: 'spring-ai-alibaba',
              title: 'spring-ai-alibaba',
              href: 'https://github.com/alibaba/spring-ai-alibaba',
            },
            {
              key: 'antd',
              title: 'antd',
              href: 'https://github.com/ant-design/ant-design',
            },
          ]}
          copyright="by spring-ai-alibaba"
        />
      )}
    >
      <div style={{ minHeight: '60vh' }}>
        <Outlet /> {/* 渲染当前路由对应的子组件 */}
      </div>
    </ProLayout>
  );
}
