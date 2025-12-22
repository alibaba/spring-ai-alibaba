import $i18n from '@/i18n';
import { debugMcpTool } from '@/services/mcp';
import { IMCPTool } from '@/types/mcp';
import { Empty, message } from '@spark-ai/design';
import { Flex, Space } from 'antd';
import React from 'react';
import ToolPanel from '../ToolPanel';
import styles from './index.module.less';

interface McpToolsProps {
  tools: IMCPTool[];
  code: string;
  activated: boolean;
}

const MCPTools: React.FC<McpToolsProps> = ({ tools, code, activated }) => {
  const handleExecute = async (params: any) => {
    try {
      const result = await debugMcpTool(params);
      message.success(
        $i18n.get({
          id: 'main.pages.MCP.components.McpTools.index.executionSuccessful',
          dm: '执行成功',
        }),
      );
      return result?.data;
    } catch (error) {}
  };

  return (
    <div className={styles.container}>
      <Space direction="vertical" style={{ width: '100%' }}>
        {tools?.length ? (
          tools.map((tool) => (
            <ToolPanel
              key={tool.name}
              server_Code={code}
              tool={tool}
              onExecute={handleExecute}
              btnDisabled={!activated}
            />
          ))
        ) : (
          <Flex className="h-full" align="center" justify="center">
            <Empty
              description={$i18n.get({
                id: 'main.pages.MCP.components.McpTools.index.serviceStartingPleaseRefreshLater',
                dm: '服务启动中，请稍后刷新',
              })}
            />
          </Flex>
        )}
      </Space>
    </div>
  );
};

export default MCPTools;
