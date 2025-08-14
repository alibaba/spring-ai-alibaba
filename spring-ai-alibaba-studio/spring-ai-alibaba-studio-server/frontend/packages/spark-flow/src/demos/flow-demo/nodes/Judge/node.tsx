import type { IWorkFlowNode } from '@spark-ai/flow';
import { BaseNode, SourceHandle } from '@spark-ai/flow';
import { NodeProps } from '@xyflow/react';
import React, { memo } from 'react';
import { IJudgeNodeParam } from '../../types/flow';

export default memo(function Judge(props: NodeProps<IWorkFlowNode>) {
  const nodeParam = props.data.node_param as IJudgeNodeParam;
  return (
    <BaseNode disableShowSourceHandle {...props}>
      {nodeParam.branches.map((item) => (
        <div
          key={item.id}
          className="spark-flow-judge-branch flex-justify-between"
        >
          <span>{item.label}</span>
          <span>{item.id === 'default' ? 'ELSE' : 'IF'}</span>
          <SourceHandle
            className="spark-flow-judge-branch-handle"
            nodeType={props.type}
            nodeId={props.id}
            handleId={`${props.id}_${item.id}`}
          />
        </div>
      ))}
    </BaseNode>
  );
});
