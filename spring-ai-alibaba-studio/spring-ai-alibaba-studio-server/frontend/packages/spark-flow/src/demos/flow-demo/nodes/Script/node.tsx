import type { IWorkFlowNode } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { NodeProps } from '@xyflow/react';
import React, { memo } from 'react';
import { IScriptNodeParam } from '../../types/flow';

export default memo(function Script(props: NodeProps<IWorkFlowNode>) {
  return (
    <BaseNode
      hasFailBranch={
        (props.data.node_param as IScriptNodeParam).try_catch_config
          .strategy === 'failBranch'
      }
      {...props}
    ></BaseNode>
  );
});
