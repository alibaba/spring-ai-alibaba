import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import React from 'react';
import styles from './index.module.less';

interface BatchOperationProps {
  selectedCount: number;
  onCancelSelect: () => void;
  onBatchDisable: () => void;
  onBatchDelete: () => void;
  onBatchEnable: () => void;
  onExitOperation?: () => void;
}

/**
 * Batch operation component
 * @description Used for batch operations on the knowledge base detail page
 */
const BatchOperation: React.FC<BatchOperationProps> = ({
  selectedCount,
  onCancelSelect,
  onBatchDelete,
  onExitOperation,
}) => {
  return (
    <div className={styles['batch-operation']}>
      <span className={styles['selected-count']}>
        {$i18n.get({
          id: 'main.pages.Knowledge.Detail.components.BatchOperation.index.selected',
          dm: '已选',
        })}
        &nbsp;{selectedCount}&nbsp;
        {$i18n.get({
          id: 'main.pages.Knowledge.Detail.components.BatchOperation.index.items',
          dm: '项',
        })}
      </span>
      <span className={styles['cancel-select']} onClick={onCancelSelect}>
        {$i18n.get({
          id: 'main.pages.Knowledge.Detail.components.BatchOperation.index.cancelSelection',
          dm: '取消选择',
        })}
      </span>
      <div className={styles['button-group']}>
        <Button type="default" onClick={onBatchDelete}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.BatchOperation.index.batchDelete',
            dm: '批量删除',
          })}
        </Button>

        <Button type="default" onClick={onExitOperation}>
          <IconFont type="spark-escape-line" />
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.BatchOperation.index.exitOperation',
            dm: '退出操作',
          })}
        </Button>
      </div>
    </div>
  );
};

export default BatchOperation;
