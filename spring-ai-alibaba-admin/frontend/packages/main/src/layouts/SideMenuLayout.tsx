import React, { useState, useMemo, useEffect } from 'react';
import { useLocation, useNavigate } from 'umi';
import { Layout as AntLayout, Menu } from 'antd';
import {
  AppstoreOutlined,
  BulbOutlined,
  ExperimentOutlined,
  LineChartOutlined,
  UnorderedListOutlined,
  PlayCircleOutlined,
  BarChartOutlined,
  NodeIndexOutlined,
  SettingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ApiOutlined,
  DatabaseOutlined,
  ToolOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import $i18n from '@/i18n';
import Header from './Header';
import styles from './index.module.less';
import LangSelect from './LangSelect';
import LoginProvider from './LoginProvider';
import SettingDropdown from './SettingDropdown';
import ThemeSelect from './ThemeSelect';
import UserAccountModal from '@/components/UserAccountModal';
import PureLayout from './Pure';
import { ModelsContext } from '@/legacy/context/models';
import PromptAPI from '@/legacy/services';

const { Sider, Content } = AntLayout;

// 获取应该高亮的菜单项 key
const getSelectedMenuKey = (pathname: string): string => {
  // 应用相关页面
  if (pathname.startsWith('/app')) {
    return '/app';
  }

  // MCP 相关页面
  if (pathname.startsWith('/mcp')) {
    return '/mcp';
  }

  // 组件相关页面
  if (pathname.startsWith('/component')) {
    return '/component';
  }

  // 知识库相关页面
  if (pathname.startsWith('/knowledge')) {
    return '/knowledge';
  }

  // 设置相关页面
  if (pathname.startsWith('/setting')) {
    return '/setting';
  }

  // 调试页面
  if (pathname.startsWith('/debug')) {
    return '/debug';
  }

  // Dify 转换页面
  if (pathname.startsWith('/dify')) {
    return '/dify';
  }

  // Agent Schema 页面
  if (pathname.startsWith('/agent-schema')) {
    return '/agent-schema';
  }

  // 评测集相关页面
  if (pathname.startsWith('/admin/evaluation/gather')) {
    return '/admin/evaluation/gather';
  }

  // 评估器相关页面
  if (pathname.startsWith('/admin/evaluation/evaluator') || pathname === '/admin/evaluation/debug') {
    return '/admin/evaluation/evaluator';
  }

  // 实验相关页面
  if (pathname.startsWith('/admin/evaluation/experiment')) {
    return '/admin/evaluation/experiment';
  }

  // Prompt 相关页面
  if (
    pathname.startsWith('/admin/prompt') ||
    pathname === '/admin/prompts' ||
    pathname === '/admin/playground' ||
    pathname === '/admin/version-history'
  ) {
    if (pathname === '/admin/playground') {
      return '/admin/playground';
    }
    return '/admin/prompts';
  }

  // Tracing 页面
  if (pathname.startsWith('/admin/tracing')) {
    return '/admin/tracing';
  }

  // 默认情况，直接返回当前路径
  return pathname;
};

export default function SideMenuLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [models, setModels] = useState<any[]>([]);
  const [modelNameMap, setModelNameMap] = useState<Record<number, string>>({});

  // 加载模型列表（用于 legacy 页面）
  useEffect(() => {
    PromptAPI.getModels()
      .then((res) => {
        const nameMap = res.data.pageItems.reduce((acc: Record<number, string>, item: any) => {
          acc[item.id] = item.name;
          return acc;
        }, {});
        setModelNameMap(nameMap);
        setModels(res.data.pageItems);
      })
      .catch((err) => {
        console.error('Failed to load models:', err);
      });
  }, []);

  // 获取应该高亮的菜单项 key
  const selectedKey = useMemo(() => getSelectedMenuKey(location.pathname), [location.pathname]);

  // 构建菜单项
  const menuItems = useMemo(
    () => [
      {
        key: 'studio',
        label: $i18n.get({
          id: 'main.layouts.SideMenu.studio',
          dm: ' Agent Builder',
        }),
        icon: <AppstoreOutlined />,
        children: [
          {
            key: '/app',
            label: $i18n.get({
              id: 'main.layouts.MenuList.application',
              dm: '应用',
            }),
            icon: <AppstoreOutlined />,
          },
          {
            key: '/mcp',
            label: 'MCP',
            icon: <ApiOutlined />,
          },
          {
            key: '/component',
            label: $i18n.get({
              id: 'main.pages.Component.AppComponent.index.component',
              dm: '组件',
            }),
            icon: <ToolOutlined />,
          },
          {
            key: '/knowledge',
            label: $i18n.get({
              id: 'main.pages.Knowledge.Test.index.knowledgeBase',
              dm: '知识库',
            }),
            icon: <DatabaseOutlined />,
          },
          {
            key: '/dify',
            label: $i18n.get({
              id: 'main.layouts.SideMenu.dify',
              dm: 'Dify To Graph',
            }),
            icon: <SwapOutlined />,
          },
        ],
      },
      {
        key: 'prompt',
        label: 'Prompt工程',
        icon: <BulbOutlined />,
        children: [
          {
            key: '/admin/prompts',
            label: 'Prompts',
            icon: <UnorderedListOutlined />,
          },
          {
            key: '/admin/playground',
            label: 'Playground',
            icon: <PlayCircleOutlined />,
          },
        ],
      },
      {
        key: 'evaluation',
        label: '评测',
        icon: <ExperimentOutlined />,
        children: [
          {
            key: '/admin/evaluation/gather',
            label: '评测集',
            icon: <UnorderedListOutlined />,
          },
          {
            key: '/admin/evaluation/evaluator',
            label: '评估器',
            icon: <BarChartOutlined />,
          },
          {
            key: '/admin/evaluation/experiment',
            label: '实验',
            icon: <ExperimentOutlined />,
          },
        ],
      },
      {
        key: 'observability',
        label: '可观测',
        icon: <LineChartOutlined />,
        children: [
          {
            key: '/admin/tracing',
            label: 'Tracing',
            icon: <NodeIndexOutlined />,
          },
        ],
      },
      {
        key: '/setting',
        label: $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.setting',
          dm: '设置',
        }),
        icon: <SettingOutlined />,
      },
    ],
    [],
  );

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  // 判断是否应该隐藏侧边栏（登录页、首页等）
  const shouldHideSidebar = ['/login', '/', '/home'].includes(location.pathname);

  if (shouldHideSidebar) {
    return (
      <PureLayout>
        <LoginProvider>
          <Header
            right={
              <>
                <ThemeSelect />
                <LangSelect />
                <SettingDropdown />
                <UserAccountModal avatarProps={{ className: styles.avatar }} />
              </>
            }
          />
          <div className={styles['body']}>{children}</div>
        </LoginProvider>
      </PureLayout>
    );
  }

  return (
    <PureLayout>
      <LoginProvider>
        <ModelsContext.Provider
          value={{
            models,
            modelNameMap,
            setModels,
          }}
        >
          <AntLayout className="h-screen">
            <Sider
              width={256}
              collapsedWidth={80}
              collapsed={collapsed}
              theme="light"
              className="shadow-lg border-r border-gray-200"
              style={{ height: '100vh', position: 'fixed', left: 0, top: 0, bottom: 0 }}
            >
              <div className="p-6 border-b border-gray-200">
                <h1 className="text-xl font-bold text-gray-800 flex items-center whitespace-nowrap overflow-hidden">
                  <SettingOutlined className="mr-1 text-blue-500" />
                  {!collapsed && 'SAA Admin'}
                </h1>
              </div>

              <Menu
                mode="inline"
                selectedKeys={[selectedKey]}
                defaultOpenKeys={collapsed ? [] : ['studio']}
                items={menuItems}
                onClick={handleMenuClick}
                className="border-r-0 mt-6"
                inlineCollapsed={collapsed}
              />

              <div className="absolute bottom-0 left-0 right-0 border-t border-gray-200 bg-white">
                <div
                  className="flex items-center justify-center p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                  onClick={() => setCollapsed(!collapsed)}
                >
                  {collapsed ? (
                    <MenuUnfoldOutlined className="text-gray-600 text-lg" />
                  ) : (
                    <MenuFoldOutlined className="text-gray-600 text-lg" />
                  )}
                  {!collapsed && <span className="ml-2 text-gray-600">收起菜单</span>}
                </div>
              </div>
            </Sider>

            <AntLayout style={{ marginLeft: collapsed ? 80 : 256, transition: 'margin-left 0.2s' }}>
              <Header
                right={
                  <>
                    <ThemeSelect />
                    <LangSelect />
                    <SettingDropdown />
                    <UserAccountModal avatarProps={{ className: styles.avatar }} />
                  </>
                }
              />
              <Content className="overflow-hidden">
                <div className="h-full overflow-y-auto bg-gray-50" style={{ minHeight: 'calc(100vh - 56px)' }}>
                  {children}
                </div>
              </Content>
            </AntLayout>
          </AntLayout>
        </ModelsContext.Provider>
      </LoginProvider>
    </PureLayout>
  );
}

