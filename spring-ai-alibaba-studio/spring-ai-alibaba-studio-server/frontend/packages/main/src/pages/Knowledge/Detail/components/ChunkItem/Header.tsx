import $i18n from '@/i18n';
import { Button, IconFont, Switch } from '@spark-ai/design';
import { Flex } from 'antd';
import classNames from 'classnames';
import React, { useState } from 'react';
import { IChunkItem } from '../../type';
import ChunkEditDrawer from '../ChunkEditDrawer';
import ChunkViewDrawer from '../ChunkViewDrawer';
import styles from './index.module.less';
import { ChunkItemMode } from './type';

interface HeaderProps {
  /**
   * Data
   */
  data: IChunkItem;
  /**
   * Index
   */
  index: number;
  /**
   * Whether to show expand button
   */
  showExpand: boolean;
  /**
   * Whether expanded
   */
  isExpanded: boolean;
  /**
   * Expand callback
   */
  onExpand: (isExpanded: boolean) => void;
  /**
   * Display mode
   */
  mode: ChunkItemMode;
  /**
   * Display switch callback
   */
  onDisplay?: (display: boolean) => void;
  /**
   * Expand button text
   */
  expandButtonText?: string;
  /**
   * Whether to show similarity value
   * */
  showSimilarValue?: boolean;
  /**
   * Delete chunk callback
   */
  onDelete?: () => void;
  /**
   * Refresh list callback
   *  */
  refreshList?: () => void;
}

const Header: React.FC<HeaderProps> = (props) => {
  const {
    data,
    isExpanded,
    onExpand,
    mode,
    onDisplay,
    onDelete,
    expandButtonText,
    showSimilarValue,
    refreshList,
  } = props;
  const { score, title, enabled, content, chunk_id } = data;
  // Show expand button
  const showExpand = props.showExpand;
  // Show edit button
  const showEditButton = mode === 'edit';
  const [visible, setVisible] = useState(false);
  const [viewVisible, setViewVisible] = useState(false);

  const handleExpand = () => {
    onExpand(!isExpanded);
  };

  const handleDisplay = (checked: boolean) => {
    onDisplay?.(checked);
  };

  return (
    <div className={classNames(styles['chunk-header'], {})}>
      <div className={styles['chunk-title']}>
        {title && (
          <>
            <span>{title}</span>
            <span className={styles['divider']}></span>
          </>
        )}
        <span>
          {content.length}
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.character',
            dm: '字符',
          })}
        </span>
        {showSimilarValue && (
          <div className={styles['similarity']}>
            {Math.floor(score! * 100)}%
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.index.similarityValue',
              dm: '相似值：',
            })}
          </div>
        )}
      </div>
      <div className={styles['chunk-right']}>
        <div className={styles['chunk-actions']}>
          {showEditButton && (
            <>
              <Button
                className={styles['edit-button']}
                type="link"
                size="small"
                onClick={onDelete}
              >
                <IconFont type="spark-delete-line" size={16} />
                {$i18n.get({
                  id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.index.delete',
                  dm: '删除',
                })}
              </Button>
              <Button
                className={styles['edit-button']}
                type="link"
                size="small"
                onClick={() => {
                  setVisible(true);
                }}
              >
                <IconFont type="spark-edit-line" size={16} />
                {$i18n.get({
                  id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.index.edit',
                  dm: '编辑',
                })}
              </Button>
            </>
          )}
          {showExpand && (
            <>
              {mode === 'edit' && <div className={styles['divider']} />}
              {mode !== 'edit' && (
                <IconFont
                  type="spark-enlarge-line"
                  size={20}
                  className={styles['view-all']}
                  onClick={() => setViewVisible(true)}
                />
              )}
              <div className={styles['expand']} onClick={handleExpand}>
                {isExpanded
                  ? $i18n.get({
                      id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.index.collapse',
                      dm: '收起',
                    })
                  : expandButtonText ||
                    $i18n.get({
                      id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.index.expand',
                      dm: '展开',
                    })}
              </div>
            </>
          )}
        </div>
        {mode === 'edit' && (
          <Flex align="center" gap={8}>
            <Switch checked={enabled} onChange={handleDisplay} size="small" />
            <div
              className={classNames(styles['display-status'], {
                [styles['enabled']]: !enabled,
              })}
            >
              {enabled
                ? $i18n.get({
                    id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.enabled',
                    dm: '已启用',
                  })
                : $i18n.get({
                    id: 'main.pages.Knowledge.Detail.components.ChunkItem.Header.disabled',
                    dm: '已禁用',
                  })}
            </div>
          </Flex>
        )}
      </div>
      {visible && (
        <ChunkEditDrawer
          visible={visible}
          title={title}
          onClose={() => {
            setVisible(false);
          }}
          content={content}
          refreshList={refreshList}
          chunk_id={chunk_id}
        />
      )}
      {viewVisible && (
        <ChunkViewDrawer
          visible={viewVisible}
          title={title}
          onClose={() => {
            setViewVisible(false);
          }}
          content={content}
        />
      )}
    </div>
  );
};

export default Header;
