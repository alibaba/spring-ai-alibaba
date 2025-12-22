import { Dropdown, IconFont, Tag } from '@spark-ai/design';
import type { IWorkFlowNodeResultItem, IWorkFlowStatus } from '@spark-ai/flow';
import classNames from 'classnames';
import { memo, useMemo } from 'react';
import styles from './index.module.less';

export interface IResultStatusProps {
  status: IWorkFlowStatus;
  usages?: IWorkFlowNodeResultItem['usages'];
  execTime: string;
}

export default memo(function ResultStatus(props: IResultStatusProps) {
  const { status, usages = [], execTime } = props;

  const totalTokens = useMemo(() => {
    return usages.reduce(
      (acc, usage) => {
        return {
          input: acc.input + usage.prompt_tokens,
          output: acc.output + usage.completion_tokens,
          total: acc.total + usage.total_tokens,
        };
      },
      { input: 0, output: 0, total: 0 },
    );
  }, [usages]);

  const memoStatusIcon = useMemo(() => {
    if (status === 'success') {
      return <IconFont className={styles.icon} type="spark-checkCircle-fill" />;
    }

    if (status === 'fail')
      return (
        <IconFont className={styles.icon} type="spark-warningCircle-fill" />
      );
    return <IconFont className={styles.icon} type="spark-loading-line" />;
  }, [status]);

  return (
    <div className="flex gap-[4px]">
      {!!totalTokens.total && (
        <Dropdown
          menu={{
            items: [
              {
                type: 'item',
                key: 'output',
                label: (
                  <div className={styles.tokenItem}>
                    <span>Output Tokens</span>
                    <span>{totalTokens.output}</span>
                  </div>
                ),
              },
              {
                type: 'item',
                key: 'input',
                label: (
                  <div className={styles.tokenItem}>
                    <span>Input Tokens</span>
                    <span>{totalTokens.input}</span>
                  </div>
                ),
              },
              {
                type: 'divider',
              },
              {
                type: 'item',
                key: 'total',
                label: (
                  <div className={styles.tokenItem}>
                    <span>Total Tokens</span>
                    <span>{totalTokens.total}</span>
                  </div>
                ),
              },
            ],
            selectable: false,
          }}
        >
          <Tag
            color="mauve"
            className={styles.tokenTag}
            bordered
            icon={<IconFont className={styles.icon} type="spark-token-line" />}
          >
            {totalTokens.total}
          </Tag>
        </Dropdown>
      )}
      <Tag
        color={status === 'executing' ? 'mauve' : ''}
        icon={memoStatusIcon}
        bordered
        className={classNames(styles.tagResult, styles[status])}
      >
        {execTime}
      </Tag>
    </div>
  );
});
