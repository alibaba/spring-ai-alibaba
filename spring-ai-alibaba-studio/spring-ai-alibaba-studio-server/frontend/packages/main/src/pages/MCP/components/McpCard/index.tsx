import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { IMcpServer, McpStatus, McpStatusMap } from '@/types/mcp';
import { Button, Dropdown, IconFont } from '@spark-ai/design';
import type { MenuProps } from 'antd';
import classNames from 'classnames';
import dayjs from 'dayjs';
import React, { useMemo } from 'react';
import styles from './index.module.less';

interface McpCardProps {
  data: IMcpServer;
  onClick?: (action?: string, data?: IMcpServer) => void;
  className?: string;
}

const McpCard: React.FC<McpCardProps> = ({ data, onClick, className }) => {
  const {
    name: serverName,
    server_code: serverCode,
    gmt_modified,
    description,
    status,
  } = data;
  const currentStatus = status as McpStatus;
  const { color, text } =
    McpStatusMap[currentStatus] || McpStatusMap[McpStatus.DELETED];

  const updateTime = useMemo(() => {
    return dayjs(gmt_modified).format('YYYY-MM-DD HH:mm:ss');
  }, [gmt_modified]);

  const handleDropdownClick: MenuProps['onClick'] = (info) => {
    info.domEvent.stopPropagation();
    onClick?.(info.key as string, data);
  };

  const handleButtonClick = (action: string, e: React.MouseEvent) => {
    e.stopPropagation();
    onClick?.(action, data);
  };

  const renderActions = () => {
    const menuItems: {
      key: string;
      label: React.ReactNode;
      danger?: boolean;
    }[] = [];
    menuItems.push({
      key: 'delete',
      label: $i18n.get({
        id: 'main.pages.MCP.components.McpCard.index.delete',
        dm: '删除',
      }),
      danger: true,
    });

    return (
      <>
        {currentStatus === McpStatus.ENABLED && (
          <Button
            type="primary"
            className={'flex-1'}
            onClick={(e) => handleButtonClick('stop', e)}
          >
            {$i18n.get({
              id: 'main.pages.MCP.components.McpCard.index.stopService',
              dm: '停止服务',
            })}
          </Button>
        )}
        {currentStatus === McpStatus.DISABLED && (
          <Button
            type="primary"
            className={'flex-1'}
            onClick={(e) => handleButtonClick('start', e)}
          >
            {$i18n.get({
              id: 'main.pages.MCP.components.McpCard.index.startService',
              dm: '启动服务',
            })}
          </Button>
        )}
        <Button
          type="default"
          className={'flex-1'}
          onClick={(e) => handleButtonClick('edit', e)}
        >
          {$i18n.get({
            id: 'main.pages.MCP.components.McpCard.index.editService',
            dm: '编辑服务',
          })}
        </Button>
        <Dropdown
          getPopupContainer={(ele) => ele}
          trigger={['click']}
          menu={{ items: menuItems, onClick: handleDropdownClick }}
        >
          <div onClick={(e) => e.stopPropagation()}>
            <Button icon={<IconFont type="spark-more-line" />} />
          </div>
        </Dropdown>
      </>
    );
  };

  return (
    <ProCard
      title={serverName}
      logo="spark-MCP-mcp-line"
      statusNode={
        <div
          className={styles['status-tag']}
          style={{ color }}
          data-color={color}
        >
          <span className={styles.dot}></span>
          <span>{text}</span>
        </div>
      }
      info={[
        {
          label: $i18n.get({
            id: 'main.pages.MCP.components.McpCard.index.description',
            dm: '描述',
          }),
          content: description || '-',
        },
        {
          label: $i18n.get({
            id: 'main.pages.MCP.components.McpCard.index.serviceId',
            dm: 'ID',
          }),
          content: serverCode,
        },
      ]}
      footerDescNode={
        <div className={styles['update-time']}>
          {$i18n.get({
            id: 'main.pages.MCP.components.McpCard.index.updatedAt',
            dm: '更新于',
          })}
          {updateTime}
        </div>
      }
      footerOperateNode={renderActions()}
      className={classNames(styles['container'], className)}
      onClick={() => onClick?.('detail', data)}
    ></ProCard>
  );
};

export default McpCard;
