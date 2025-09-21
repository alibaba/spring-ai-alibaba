import $i18n from '@/i18n';
import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { BaseNode, SourceHandle } from '@spark-ai/flow';
import { Typography } from 'antd';
import { memo } from 'react';
import { IJudgeNodeParam } from '../../types';
import styles from './index.module.less';

export default memo(function Judge(props: NodeProps<IWorkFlowNode>) {
  const nodeParam = props.data.node_param as IJudgeNodeParam;
  return (
    <BaseNode disableShowSourceHandle {...props}>
      {nodeParam.branches.map((item) => {
        if (item.id === 'default') return null;
        return (
          <div
            key={item.id}
            className="spark-flow-judge-branch flex-justify-between"
          >
            <Typography.Text
              className={styles['judge-label']}
              ellipsis={{ tooltip: item.label }}
              style={{ maxWidth: 200 }}
            >
              {item.label}
            </Typography.Text>
            <span>{item.id === 'default' ? 'ELSE' : 'IF'}</span>
            <SourceHandle
              className="spark-flow-judge-branch-handle"
              nodeType={props.type}
              nodeId={props.id}
              handleId={`${props.id}_${item.id}`}
            />
          </div>
        );
      })}
      <div className="spark-flow-judge-branch flex-justify-between">
        <span>
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.Judge.node.default',
            dm: '默认',
          })}
        </span>
        <span>ELSE</span>
        <SourceHandle
          className="spark-flow-judge-branch-handle"
          nodeType={props.type}
          nodeId={props.id}
          handleId={`${props.id}_default`}
        />
      </div>
    </BaseNode>
  );
});
