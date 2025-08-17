import $i18n from '@/i18n';
import { SliderSelector } from '@spark-ai/design';
import { Flex } from 'antd';
import { memo } from 'react';
import { IParallelNodeParam } from '../../types';
import InfoIcon from '../InfoIcon';
import styles from './index.module.less';

export interface IParallelConfigFormProps {
  value: IParallelNodeParam;
  onChange: (value: Partial<IParallelNodeParam>) => void;
}

export default memo(function ParallelConfigForm({
  value,
  onChange,
}: IParallelConfigFormProps) {
  return (
    <div className={styles.form}>
      <Flex align="center" gap={8}>
        <Flex className={styles['label-wrap']} gap={4} align="center">
          <span>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ParallelConfigForm.index.maxBatchCount',
              dm: '批处理次数上限',
            })}
          </span>
          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.components.ParallelConfigForm.index.batchExecutionLimit',
              dm: '批处理运行的次数不大于批处理次数上限',
            })}
          />
        </Flex>
        <SliderSelector
          min={1}
          max={200}
          step={1}
          value={value.batch_size}
          inputNumberWrapperStyle={{ width: 54 }}
          className="flex-1"
          onChange={(val) => onChange({ batch_size: val as number })}
        />
      </Flex>
      <Flex align="center" gap={8}>
        <Flex className={styles['label-wrap']} gap={4} align="center">
          <span>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ParallelConfigForm.index.parallelCount',
              dm: '并行运行数量',
            })}
          </span>
          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.components.ParallelConfigForm.index.concurrentLimit',
              dm: '批处理的并发限制，设置为 1 表示串行执行所有任务',
            })}
          />
        </Flex>
        <SliderSelector
          min={1}
          max={10}
          step={1}
          inputNumberWrapperStyle={{ width: 54 }}
          className="flex-1"
          value={value.concurrent_size}
          onChange={(val) => onChange({ concurrent_size: val as number })}
        />
      </Flex>
    </div>
  );
});
