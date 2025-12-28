import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import McpManage from './Manage';

export default function () {
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.MCP.index.mcpManagement',
            dm: 'MCP管理',
          }),
        },
      ]}
    >
      <McpManage />
    </InnerLayout>
  );
}
