import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { Button, Dropdown, IconButton, IconFont } from '@spark-ai/design';
import classNames from 'classnames';
import dayjs from 'dayjs';
import React from 'react';
import { history } from 'umi';
import styles from './index.module.less';

/**
 * Knowledge base list component props interface
 */
interface KnowledgeCardProps {
  /**
   * Loading state
   */
  loading?: boolean;
  /**
   * Custom class name for overriding default styles
   */
  className?: string;
  /**
   * Knowledge base ID
   */
  kb_id: string;
  /**
   * Knowledge base name
   */
  name: string;
  /**
   * Knowledge base description
   */
  description: string;
  /**
   * Update time
   */
  gmt_modified: string;
  /**
   * Number of documents
   */
  total_docs: number;
  /**
   * Click action handler
   */
  handleClickAction?: (key: string, id: string) => void;
}

const KnowledgeCard: React.FC<KnowledgeCardProps> = ({
  name,
  kb_id,
  description,
  gmt_modified,
  total_docs,
  handleClickAction,
}) => {
  return (
    <ProCard
      title={name}
      logo={<img className={styles['logo']} src={'/images/knowledge.svg'} />}
      info={[
        {
          label: $i18n.get({
            id: 'main.pages.Knowledge.List.components.Card.index.knowledgeBaseDescription',
            dm: '描述',
          }),
          content: (
            <div
              className={classNames(styles['description'], {
                [styles['no-description']]: !description,
              })}
            >
              {description ||
                $i18n.get({
                  id: 'main.pages.Knowledge.List.components.Card.index.nullName',
                  dm: '暂无描述',
                })}
            </div>
          ),
        },
        {
          label: $i18n.get({
            id: 'main.pages.Knowledge.List.components.Card.index.knowledgeBaseId',
            dm: 'ID',
          }),
          content: kb_id,
        },
      ]}
      footerDescNode={
        <div className={styles['card-footer']}>
          <div className={styles['update-time']}>
            {$i18n.get({
              id: 'main.pages.Knowledge.List.components.Card.index.updatedAt',
              dm: '更新于',
            })}
            {dayjs(gmt_modified).format('YYYY-MM-DD HH:mm:ss')}
          </div>
          <div className={styles['document-count']}>
            <div className={styles['count-wrapper']}>
              <IconFont
                type="spark-document-line"
                className={styles['doc-icon']}
              />

              <div className={styles['count']}>{total_docs}</div>
            </div>
          </div>
        </div>
      }
      footerOperateNode={
        <>
          <Button
            type="primary"
            className={styles['operate-button']}
            onClick={(e) => {
              e.stopPropagation();
              e.preventDefault();
              history.push(`/knowledge/edit/${kb_id}`);
            }}
          >
            {$i18n.get({
              id: 'main.pages.Knowledge.List.components.Card.index.edit',
              dm: '编辑',
            })}
          </Button>
          <Button
            type="default"
            className={styles['operate-button']}
            onClick={(e) => {
              e.stopPropagation();
              e.preventDefault();
              history.push(`/knowledge/test/${kb_id}`);
            }}
          >
            {$i18n.get({
              id: 'main.pages.Knowledge.List.components.Card.index.hitTest',
              dm: '命中测试',
            })}
          </Button>
          <Dropdown
            getPopupContainer={(ele) => ele}
            menu={{
              items: [
                {
                  danger: true,
                  label: $i18n.get({
                    id: 'main.pages.Knowledge.List.components.Card.index.delete',
                    dm: '删除',
                  }),
                  key: 'delete',
                  onClick: () =>
                    handleClickAction && handleClickAction('delete', kb_id),
                },
              ],
            }}
          >
            <IconButton shape="default" icon="spark-more-line" />
          </Dropdown>
        </>
      }
      onClick={() => history.push(`/knowledge/${kb_id}`)}
      className={styles['knowledge-card']}
    />
  );
};

export default KnowledgeCard;
