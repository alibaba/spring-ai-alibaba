import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { memo } from 'react';
import { ILLMNodeParam } from '../../types';

export default memo(function LLMNode(props: NodeProps<IWorkFlowNode>) {
  return (
    <BaseNode
      hasFailBranch={
        (props.data.node_param as ILLMNodeParam).try_catch_config.strategy ===
        'failBranch'
      }
      {...props}
    ></BaseNode>
  );
});
