import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { GroupNode } from '@spark-ai/flow';
import { memo } from 'react';

export default memo(function ParallelNode(props: NodeProps<IWorkFlowNode>) {
  return <GroupNode {...props} />;
});
