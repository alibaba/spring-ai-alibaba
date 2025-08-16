import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { memo } from 'react';

export default memo(function ParallelStartNode(
  props: NodeProps<IWorkFlowNode>,
) {
  return <BaseNode disableAction disableShowTargetHandle {...props}></BaseNode>;
});
