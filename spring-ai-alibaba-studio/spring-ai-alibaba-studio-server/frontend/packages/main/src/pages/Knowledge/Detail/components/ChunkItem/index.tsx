import $i18n from '@/i18n';
import React, { useState } from 'react';
import { IChunkItem } from '../../type';
import Content from './Content';
import Header from './Header';
import styles from './index.module.less';

interface ChunkItemProps {
  data: Partial<IChunkItem>;
  index: number;
  mode: 'detail' | 'simple';
  expandButtonText?: string;
  showSimilarValue?: boolean;
  showSource?: boolean;
}

const ChunkItem: React.FC<ChunkItemProps> = ({
  data,
  index,
  mode,
  showSource,
  ...restProps
}) => {
  const { content, doc_name } = data;
  const [isExpanded, setIsExpanded] = useState(false);
  const [showExpand, setShowExpand] = useState(false);

  const handleExpand = () => {
    setIsExpanded(!isExpanded);
  };

  return (
    <div className={styles['chunk-item']}>
      <Header
        data={data as IChunkItem}
        index={index}
        mode={mode}
        showExpand={showExpand}
        isExpanded={isExpanded}
        onExpand={handleExpand}
        {...restProps}
      />

      <Content
        isExpanded={isExpanded}
        className={isExpanded ? styles['expanded'] : ''}
        content={content || ''}
        onShowExpand={setShowExpand}
      />

      {showSource && (
        <div className={styles['chunk-source']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.ChunkItem.index.document',
            dm: '文档：',
          })}
          {doc_name}
        </div>
      )}
    </div>
  );
};

export default ChunkItem;
