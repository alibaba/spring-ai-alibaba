import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import { IWorkFlowStatus, IWorkFlowTaskProcess } from '@/types/work-flow';
import { Button, copy, Popover, Tag } from '@spark-ai/design';
import { Flex, message, Table, Typography } from 'antd';
import { ColumnType } from 'antd/es/table';
import classNames from 'classnames';
import React, { memo, useCallback, useMemo } from 'react';
import { NodeStatusIcon } from '../BaseNode';
import CustomIcon from '../CustomIcon';
import FlowIcon from '../FlowIcon';
import './index.less';

const statusNameMap: Record<IWorkFlowStatus, string> = {
  success: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.runSuccess',
    dm: '运行成功',
  }),
  executing: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.running',
    dm: '运行中',
  }),
  skip: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.skipped',
    dm: '跳过',
  }),
  fail: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.runFailed',
    dm: '运行失败',
  }),
  stop: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.stopped',
    dm: '已停止',
  }),
  pause: $i18n.get({
    id: 'spark-flow.components.TaskStatus.index.paused',
    dm: '已暂停',
  }),
};

interface ITokenDetail {
  id: string;
  name: string;
  type: string;
  input: number;
  output: number;
}

function TaskStatus() {
  const taskStore = useStore(
    (state) => state.taskStore,
  ) as IWorkFlowTaskProcess;
  const showResults = useStore((state) => state.showResults);
  const setShowResults = useStore((state) => state.setShowResults);

  const copyRequestId = useCallback(() => {
    if (!taskStore) return;
    copy(taskStore.request_id);
    message.success(
      $i18n.get({
        id: 'spark-flow.components.TaskStatus.index.requestIdCopied',
        dm: 'Request ID 已复制',
      }),
    );
  }, [taskStore?.request_id]);

  const dataSource = useMemo(() => {
    const list: ITokenDetail[] = [];

    taskStore.node_results.forEach((item) => {
      if (item.usages) {
        const tokenMap = item.usages.reduce(
          (acc, cur) => {
            acc.input += cur.prompt_tokens;
            acc.output += cur.completion_tokens;
            return acc;
          },
          { input: 0, output: 0 },
        );

        list.push({
          id: item.node_id,
          name: item.node_name,
          type: item.node_type,
          ...tokenMap,
        });
      }
    });

    return list;
  }, [taskStore.node_results]);

  const columns = useMemo(() => {
    return [
      {
        title: $i18n.get({
          id: 'spark-flow.components.TaskStatus.index.nodeId',
          dm: '节点ID',
        }),
        dataIndex: 'id',
      },
      {
        title: $i18n.get({
          id: 'spark-flow.components.TaskStatus.index.nodeType',
          dm: '节点类型',
        }),
        dataIndex: 'type',
        render: (_, record) => {
          return (
            <Flex gap={8} align="center">
              <span className="flex-shrink-0">
                <FlowIcon nodeType={record.type} />
              </span>
              <Typography.Text
                ellipsis={{ tooltip: true }}
                style={{ maxWidth: 120 }}
              >
                {record.name}
              </Typography.Text>
            </Flex>
          );
        },
      },
      {
        title: $i18n.get({
          id: 'spark-flow.components.TaskStatus.index.inputTokens',
          dm: '输入Tokens',
        }),
        dataIndex: 'input',
      },
      {
        title: $i18n.get({
          id: 'spark-flow.components.TaskStatus.index.outputTokens',
          dm: '输出Tokens',
        }),
        dataIndex: 'output',
      },
    ] as ColumnType<ITokenDetail>[];
  }, []);

  return (
    <>
      <div
        className={classNames(
          `spark-flow-task-status flex-justify-between spark-flow-task-status-${taskStore.task_status}`,
          {
            'spark-flow-task-status-hidden-results': !showResults,
          },
        )}
      >
        <div className="gap-[16px] flex items-center">
          <div className="flex items-center gap-[8px]">
            <NodeStatusIcon status={taskStore.task_status} />
            <span>{statusNameMap[taskStore.task_status]}</span>
            <span>{taskStore.task_exec_time}</span>
          </div>
          <div className="flex items-center gap-[4px]">
            <Popover
              placement="bottom"
              rootClassName="spark-flow-task-token-popover"
              getPopupContainer={(ele) => ele}
              content={
                <Table
                  pagination={{
                    pageSize: dataSource.length,
                    hideOnSinglePage: true,
                  }}
                  dataSource={dataSource}
                  columns={columns}
                />
              }
              destroyTooltipOnHide
            >
              <Tag
                icon={<CustomIcon size="small" type="spark-token-line" />}
                className="spark-flow-task-status-tag"
                color="mauve"
                bordered
              >
                {$i18n.get({
                  id: 'spark-flow.components.TaskStatus.index.tokenDetails',
                  dm: 'Token 详情',
                })}
              </Tag>
            </Popover>
            <Popover
              content={
                <>
                  <div className="font-medium">Request ID</div>
                  <div className="flex gap-[4px] items-center">
                    {taskStore.request_id}
                    <CustomIcon
                      className="cursor-pointer"
                      onClick={copyRequestId}
                      type="spark-copy-line"
                    />
                  </div>
                </>
              }
            >
              <Tag
                icon={<CustomIcon size="small" type="spark-code01-line" />}
                className="spark-flow-task-status-tag"
                color="mauve"
                bordered
              >
                Request ID
              </Tag>
            </Popover>
          </div>
        </div>
        <Button
          onClick={() => setShowResults(!showResults)}
          className="spark-flow-task-status-button"
          type="text"
          icon={<CustomIcon type="spark-hide-line" />}
        >
          {showResults
            ? $i18n.get({
                id: 'spark-flow.components.TaskStatus.index.hideTestResult',
                dm: '隐藏测试结果',
              })
            : $i18n.get({
                id: 'spark-flow.components.TaskStatus.index.showTestResult',
                dm: '显示测试结果',
              })}
        </Button>
      </div>
    </>
  );
}

const TaskStatusWrap = () => {
  const taskStore = useStore((state) => state.taskStore);
  if (!taskStore) return null;
  return <TaskStatus />;
};

export default memo(TaskStatusWrap);
