import { TopExpandBtn } from '@/components/ExpandBtn';
import $i18n from '@/i18n';
import { FlowIcon, IWorkFlowNodeResultItem } from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { Flex, Pagination, Tag } from 'antd';
import classNames from 'classnames';
import { memo, useMemo, useState } from 'react';
import ResultStatus, { IResultStatusProps } from '../ResultStatus';
import styles from './index.module.less';
import JSONViewer from './JsonViewerComp';
import JudgeResult from './JudgeResult';

export interface INodeResultPanelProps {
  nodeResult: IWorkFlowNodeResultItem;
}

const errorStrategyMap = {
  failBranch: $i18n.get({
    id: 'main.pages.App.Workflow.components.NodeResultPanel.index.exceptionBranch',
    dm: '异常分支',
  }),
  defaultValue: $i18n.get({
    id: 'main.pages.App.Workflow.components.NodeResultPanel.index.defaultValue',
    dm: '默认值',
  }),
};

export function NodeResultPanel(props: INodeResultPanelProps) {
  const { nodeResult } = props;
  const [state, setState] = useSetState({
    current: 1,
    showError: false,
  });
  const [expand, setExpand] = useState(true);

  const errorList = useMemo(() => {
    return nodeResult.batches.filter((item) => item.node_status === 'fail');
  }, [nodeResult.batches]);

  const dataList = useMemo(() => {
    return state.showError ? errorList : nodeResult.batches;
  }, [nodeResult, state.showError, errorList]);

  const renderData = useMemo(() => {
    if (!nodeResult.batches.length) return nodeResult;
    return dataList[state.current - 1];
  }, [state.current, dataList, nodeResult]);

  const renderCatchResult = useMemo(() => {
    return (
      <>
        {!!renderData.retry?.happened && (
          <Tag className={styles['tag']}>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.retryProcessing',
              dm: '重试处理：',
            })}
            {renderData.retry.retry_times}
          </Tag>
        )}
        {!!renderData.try_catch?.happened && (
          <Tag className={styles['tag']}>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.exceptionHandling',
              dm: '异常处理：',
            })}
            {
              errorStrategyMap[
                renderData.try_catch.strategy as keyof typeof errorStrategyMap
              ]
            }
          </Tag>
        )}
      </>
    );
  }, [renderData]);

  const memoResult = useMemo(() => {
    switch (renderData.node_type) {
      case 'Start':
        return (
          <JSONViewer
            label={$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.input',
              dm: '输入',
            })}
            value={renderData.input}
          />
        );
      case 'Judge':
        return (
          <JudgeResult
            multiBranchResults={renderData.multi_branch_results}
            input={renderData.input}
          />
        );
      case 'End':
        return (
          <JSONViewer
            type={renderData.output_type}
            label={$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.output',
              dm: '输出',
            })}
            value={renderData.output}
          />
        );

      case 'LLM':
        return (
          <>
            <JSONViewer
              label={$i18n.get({
                id: 'main.pages.App.Workflow.components.NodeResultPanel.index.input',
                dm: '输入',
              })}
              value={renderData.input}
            />
            {renderCatchResult}
            <JSONViewer
              type={renderData.output_type}
              label={$i18n.get({
                id: 'main.pages.App.Workflow.components.NodeResultPanel.index.output',
                dm: '输出',
              })}
              value={renderData.output}
            />
          </>
        );

      case 'Input':
        return (
          <JSONViewer
            label={$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.input',
              dm: '输入',
            })}
            value={renderData.input}
          />
        );

      case 'Output':
        return (
          <JSONViewer
            label={$i18n.get({
              id: 'main.pages.App.Workflow.components.NodeResultPanel.index.output',
              dm: '输出',
            })}
            type={renderData.output_type}
            value={JSON.stringify({ output: renderData.output })}
          />
        );

      default:
        return (
          <>
            <JSONViewer
              label={$i18n.get({
                id: 'main.pages.App.Workflow.components.NodeResultPanel.index.input',
                dm: '输入',
              })}
              value={renderData.input}
            />
            {renderCatchResult}
            {!!renderData.output && (
              <JSONViewer
                type={renderData.output_type}
                label={$i18n.get({
                  id: 'main.pages.App.Workflow.components.NodeResultPanel.index.output',
                  dm: '输出',
                })}
                value={renderData.output}
              />
            )}
          </>
        );
    }
  }, [renderData]);

  return (
    <div
      className={classNames(styles['node-result-panel'], {
        [styles['node-result-panel-child']]: nodeResult.parent_node_id,
      })}
    >
      <div
        className={classNames(
          styles['node-result-header'],
          'flex-justify-between',
        )}
        onClick={() => {
          setExpand(!expand);
        }}
      >
        <div className="flex items-center gap-[6px]">
          <FlowIcon size="small" nodeType={nodeResult.node_type} />
          <span className={styles['node-result-title']}>
            {nodeResult.node_name}
          </span>
          <TopExpandBtn
            expand={expand}
            setExpand={() => {
              setExpand(!expand);
            }}
          />
        </div>
        <Flex onClick={(e) => e.stopPropagation()} gap={8}>
          {nodeResult.batches?.length > 0 && (
            <Pagination
              total={nodeResult.batches.length}
              pageSize={1}
              current={state.current}
              onChange={(val) => setState({ current: val })}
              size="small"
              simple
              showSizeChanger={false}
            />
          )}
          <ResultStatus
            usages={nodeResult.usages}
            status={nodeResult.node_status}
            execTime={nodeResult.node_exec_time}
          />
        </Flex>
      </div>
      {expand && (
        <div className={styles['node-result-content']}>{memoResult}</div>
      )}
    </div>
  );
}

export const NodeResultPanelList = memo(
  ({
    data = [],
    statusInfo,
  }: {
    data?: IWorkFlowNodeResultItem[];
    statusInfo?: IResultStatusProps;
  }) => {
    const memoData = useMemo(() => {
      return data.filter(
        (item) =>
          ![
            'IteratorStart',
            'IteratorEnd',
            'ParallelStart',
            'ParallelEnd',
          ].includes(item.node_type),
      );
    }, [data]);
    if (!memoData.length) return null;
    return (
      <div className="flex flex-col gap-[12px]">
        {statusInfo && (
          <div className="flex justify-end">
            <ResultStatus {...statusInfo} />
          </div>
        )}
        <div className={styles['result-panel-list']}>
          {memoData.map((item) => (
            <NodeResultPanel key={item.node_id} nodeResult={item} />
          ))}
        </div>
      </div>
    );
  },
);

export default memo(NodeResultPanel);
