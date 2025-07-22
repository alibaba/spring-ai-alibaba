import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { memo } from 'react';

export default memo(function ParallelEndNode(props: NodeProps<IWorkFlowNode>) {
  return <BaseNode disableAction disableShowSourceHandle {...props}></BaseNode>;
});
