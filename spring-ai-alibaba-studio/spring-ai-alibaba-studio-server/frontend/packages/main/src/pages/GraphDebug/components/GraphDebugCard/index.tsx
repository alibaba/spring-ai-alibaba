import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { Button, Tag, Dropdown, IconButton } from '@spark-ai/design';
import dayjs from 'dayjs';
import React, { useMemo } from 'react';
import styles from './index.module.less';
import Status from './Status';

export interface IGraphCard {
  id: string;
  name: string;
  description?: string;
  tags?: string[];
  gmt_modified: string;
  status: 'ACTIVE' | 'DRAFT' | 'DISABLED';
}

export interface GraphDebugCardProps extends IGraphCard {
  onClick?: () => void;
  onAction?: (action: string) => void;
}

const GraphDebugCard: React.FC<GraphDebugCardProps> = ({
  id,
  name,
  description,
  tags = [],
  gmt_modified,
  status,
  onClick,
  onAction,
}) => {
  const updateTime = useMemo(() => {
    return dayjs(gmt_modified).format('YYYY-MM-DD HH:mm:ss');
  }, [gmt_modified]);

  return (
    <ProCard
      title={name}
      logo={
        <div className={styles['logo']}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M3 12c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm9 2c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm7-2c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2z"
              fill="currentColor"
            />
            <path
              d="m7 12 5-5 5 5"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          </svg>
        </div>
      }
      statusNode={<Status status={status} />}
      info={[
        {
          content: `${$i18n.get({
            id: 'main.pages.GraphDebug.components.Card.updateTime',
            dm: '更新时间',
          })}: ${updateTime}`,
        },
        {
          content: `${$i18n.get({
            id: 'main.pages.GraphDebug.components.Card.graphId',
            dm: '图形ID',
          })}: ${id}`,
        },
      ]}
      onClick={onClick}
      footerDescNode={
        <div className={styles['tags']}>
          {tags.slice(0, 3).map((tag) => (
            <Tag key={tag} color="blue">
              {tag}
            </Tag>
          ))}
          {tags.length > 3 && (
            <Tag color="default">+{tags.length - 3}</Tag>
          )}
        </div>
      }
      footerOperateNode={
        <>
          <Button
            type="primary"
            className="flex-1"
            onClick={(e) => {
              e.stopPropagation();
              onClick?.();
            }}
          >
            {$i18n.get({
              id: 'main.pages.GraphDebug.components.Card.debug',
              dm: '调试',
            })}
          </Button>
          <Dropdown
            getPopupContainer={(ele) => ele}
            menu={{
              onClick: (e) => {
                onAction?.(e.key);
              },
              items: [
                {
                  label: $i18n.get({
                    id: 'main.pages.GraphDebug.components.Card.edit',
                    dm: '编辑',
                  }),
                  key: 'edit',
                },
                {
                  label: $i18n.get({
                    id: 'main.pages.GraphDebug.components.Card.copy',
                    dm: '复制',
                  }),
                  key: 'copy',
                },
                {
                  label: $i18n.get({
                    id: 'main.pages.GraphDebug.components.Card.delete',
                    dm: '删除',
                  }),
                  danger: true,
                  key: 'delete',
                },
              ],
            }}
          >
            <IconButton
              shape="default"
              icon="spark-more-line"
              onClick={(e) => e.stopPropagation()}
            />
          </Dropdown>
        </>
      }
    />
  );
};

export default GraphDebugCard;
