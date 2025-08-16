import ChunkItem from '@/pages/Knowledge/Detail/components/ChunkItem';
import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import classNames from 'classnames';
import React from 'react';
import styles from './index.module.less';

import $i18n from '@/i18n';
import { Empty } from '@spark-ai/design';

interface ChunkListProps {
  /**
   * Custom style
   */
  className?: string;
  /**
   * List data
   */
  list: IChunkItem[];
}

const ChunkList: React.FC<ChunkListProps> = ({ list = [], className }) => {
  return (
    <div className={classNames(styles['chunk-list'], className)}>
      {!!list?.length && (
        <div className={styles['chunk-list-header']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.SliceConfiguration.components.ChunkList.index.chunkPreview',
            dm: '切片预览',
          })}
          <span className={styles['chunk-list-header-count']}>
            {list?.length}
          </span>
        </div>
      )}
      {list?.length ? (
        list?.map((item, index) => (
          <ChunkItem
            key={item.id}
            data={{
              title: item.title,
              content: item.text || '',
              enabled: item.enabled,
            }}
            index={index}
            mode="detail"
          />
        ))
      ) : (
        <div className={styles['empty']}>
          <Empty
            description={
              <div className={styles['empty-content']}>
                <span className={styles['empty-title']}>
                  {$i18n.get({
                    id: 'main.pages.Knowledge.Detail.SliceConfiguration.components.ChunkList.index.noData',
                    dm: '暂无数据',
                  })}
                </span>
                <span className={styles['empty-desc']}>
                  {$i18n.get({
                    id: 'main.pages.Knowledge.Detail.SliceConfiguration.components.ChunkList.index.enterContentOnLeftClickToPreview',
                    dm: '请在左侧输入内容，点击立即预览进行结果查看',
                  })}
                </span>
              </div>
            }
          />
        </div>
      )}
    </div>
  );
};

export default ChunkList;
