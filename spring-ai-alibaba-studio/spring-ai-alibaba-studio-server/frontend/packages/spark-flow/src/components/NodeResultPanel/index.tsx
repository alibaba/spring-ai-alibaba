import $i18n from '@/i18n';
import { IWorkFlowNodeResultItem, IWorkFlowStatus } from '@/types/work-flow';
import { Typography } from 'antd';
import classNames from 'classnames';
import React, { memo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import './index.less';

interface INodeResultPanelProps {
  data: IWorkFlowNodeResultItem;
}

const statusNameMap: Record<IWorkFlowStatus, string> = {
  success: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.success',
    dm: '成功',
  }),
  executing: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.executing',
    dm: '执行中',
  }),
  skip: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.skipped',
    dm: '跳过',
  }),
  fail: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.failed',
    dm: '失败',
  }),
  stop: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.stopped',
    dm: '停止',
  }),
  pause: $i18n.get({
    id: 'spark-flow.components.NodeResultPanel.index.paused',
    dm: '暂停',
  }),
};

const NodeResultPanel = (props: INodeResultPanelProps) => {
  const [expand, setExpand] = useState(false);
  return (
    <div
      onClick={(e) => {
        e.stopPropagation();
      }}
      className={classNames(
        `spark-flow-node-result-panel nodrag spark-flow-node-result-panel-${props.data.node_status}`,
        {
          ['spark-flow-node-result-panel-hidden']: !expand,
        },
      )}
    >
      <div
        onClick={() => setExpand(!expand)}
        className="spark-flow-node-result-header flex flex-col gap-[4px]"
      >
        <div className="flex-justify-between">
          <div className="flex-center gap-[8px]">
            <span className="spark-flow-node-result-status">
              {statusNameMap[props.data.node_status]}
            </span>
            <span className="spark-flow-node-result-time">
              {props.data.node_exec_time}
            </span>
          </div>
          <CustomIcon
            className="text-base spark-flow-node-result-expand-icon"
            type="spark-up-line"
          />
        </div>
        {expand && props.data.node_status === 'fail' && (
          <Typography.Text
            ellipsis={{
              tooltip: {
                title: props.data.error_info,
                destroyTooltipOnHide: true,
              },
            }}
            className={'spark-flow-node-result-error-info'}
          >
            {props.data.error_info}
          </Typography.Text>
        )}
      </div>
      {expand && (!!props.data.input || !!props.data.output) && (
        <div className="spark-flow-node-result-content">
          {!!props.data.input && (
            <>
              <div className="spark-flow-node-result-content-title">
                {$i18n.get({
                  id: 'spark-flow.components.NodeResultPanel.index.input',
                  dm: '输入',
                })}
              </div>
              <div className="spark-flow-node-result-content-area">
                {props.data.input}
              </div>
            </>
          )}
          {!!props.data.output && (
            <>
              <div className="spark-flow-node-result-content-title">
                {$i18n.get({
                  id: 'spark-flow.components.NodeResultPanel.index.output',
                  dm: '输出',
                })}
              </div>
              <div className="spark-flow-node-result-content-area">
                {props.data.output}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default memo(NodeResultPanel);
