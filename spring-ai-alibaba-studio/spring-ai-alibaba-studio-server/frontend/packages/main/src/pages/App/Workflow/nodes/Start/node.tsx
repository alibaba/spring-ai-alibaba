import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { memo } from 'react';

export default memo(function StartNode(props: NodeProps<IWorkFlowNode>) {
  return <BaseNode disableShowTargetHandle {...props}></BaseNode>;
});
