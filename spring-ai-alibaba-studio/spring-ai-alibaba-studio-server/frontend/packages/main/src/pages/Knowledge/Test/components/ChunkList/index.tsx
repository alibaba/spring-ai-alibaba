import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import classNames from 'classnames';
import React from 'react';
import styles from './index.module.less';

import $i18n from '@/i18n';
import ChunkItem from '@/pages/Knowledge/Detail/components/ChunkItem/index';
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

const ChunkList: React.FC<ChunkListProps> = ({ list, className }) => {
  return (
    <div className={classNames(styles['chunk-list'], className)}>
      {list?.length ? (
        <>
          <div className={styles['chunk-list-header']}>
            <span>
              {$i18n.get({
                id: 'main.pages.Knowledge.Test.components.ChunkList.index.recallResults',
                dm: '召回结果',
              })}
            </span>
            <span className={styles['chunk-list-header-count']}>
              {list?.length}
            </span>
          </div>
          {list.map((item, index) => (
            <ChunkItem
              key={item.id}
              data={{
                title: item.title,
                content: item.text || '',
                score: item.score,
                doc_name: item.doc_name,
              }}
              index={index}
              showSimilarValue={true}
              mode="detail"
              showSource
            />
          ))}
        </>
      ) : (
        <div className={styles['empty']}>
          <Empty
            title={$i18n.get({
              id: 'main.pages.Knowledge.Test.components.ChunkList.index.noData',
              dm: '暂无数据',
            })}
            description={$i18n.get({
              id: 'main.pages.Knowledge.Test.components.ChunkList.index.enterContentLeftClickTest',
              dm: '请在左侧输入内容，点击测试进行结果查看',
            })}
          />
        </div>
      )}
    </div>
  );
};

export default ChunkList;
