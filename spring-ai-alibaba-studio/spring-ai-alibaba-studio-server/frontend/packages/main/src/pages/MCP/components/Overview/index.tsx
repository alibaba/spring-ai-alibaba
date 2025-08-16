import $i18n from '@/i18n';
import { IMcpServer } from '@/types/mcp';
import { getCommonConfig } from '@spark-ai/design';
import CodeBlock from '@spark-ai/design/dist/components/commonComponents/CodeBlock';
import { Empty } from 'antd';
import React from 'react';
import styles from './index.module.less';

interface OverviewProps {
  detail: IMcpServer;
}

const Overview: React.FC<OverviewProps> = ({ detail }) => {
  const darkMode = getCommonConfig().isDarkMode;

  const renderItem = (label: string, value: string) => (
    <div className={styles['info-item']}>
      <div className={styles['info-label']}>{label}</div>
      <div className={styles['info-value']}>{value}</div>
    </div>
  );

  if (!detail) {
    return (
      <Empty
        description={$i18n.get({
          id: 'main.pages.MCP.components.Overview.index.noData',
          dm: '暂无数据',
        })}
      />
    );
  }

  return (
    <div className={styles['overview-container']}>
      {renderItem(
        $i18n.get({
          id: 'main.pages.MCP.components.Overview.index.serviceName',
          dm: '服务名称',
        }),
        detail.name,
      )}

      {renderItem(
        $i18n.get({
          id: 'main.pages.MCP.components.Overview.index.serviceDescription',
          dm: '服务描述',
        }),
        detail.description,
      )}

      <div className={styles['info-item']}>
        <div className={styles['info-label']}>
          {$i18n.get({
            id: 'main.pages.MCP.components.Overview.index.mcpServiceConfigurationFile',
            dm: 'MCP服务配置文件',
          })}
        </div>
        <div className={styles['code-container']}>
          <CodeBlock
            className={styles['code-block']}
            language="json"
            theme={darkMode ? 'dark' : 'light'}
            value={detail.deploy_config}
            readOnly={true}
          />
        </div>
      </div>
    </div>
  );
};

export default Overview;
