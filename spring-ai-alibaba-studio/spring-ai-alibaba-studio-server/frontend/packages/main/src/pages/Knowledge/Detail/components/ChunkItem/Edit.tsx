import classNames from 'classnames';
import React, { useState } from 'react';
import { IChunkItem } from '../../type';
import Content from './Content';
import Header from './Header';
import styles from './index.module.less';

interface ChunkEditItemProps {
  data: IChunkItem;
  index: number;
  expandButtonText?: string;
  onDelete?: () => void;
  onUpdate?: (text: string) => void;
  onDisplay?: (display: boolean) => void;
  refreshList?: () => void;
}

const ChunkEditItem: React.FC<ChunkEditItemProps> = ({
  data,
  index,
  onDelete,
  onUpdate,
  onDisplay,
  ...restProps
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [showExpand, setShowExpand] = useState(false);
  const handleExpand = () => {
    setIsExpanded(!isExpanded);
  };

  return (
    <div className={styles['chunk-item']}>
      <Header
        data={data}
        index={index}
        mode="edit"
        showExpand={showExpand}
        isExpanded={isExpanded}
        onExpand={handleExpand}
        onDisplay={onDisplay}
        onDelete={onDelete}
        {...restProps}
      />
      <Content
        isExpanded={isExpanded}
        className={classNames({
          [styles['expanded']]: isExpanded,
        })}
        content={data.content}
        onShowExpand={setShowExpand}
      />
    </div>
  );
};

export default ChunkEditItem;
