import type { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import { GroupNode } from '@spark-ai/flow';
import React, { memo } from 'react';

export default memo(function IteratorNode(props: NodeProps<IWorkFlowNode>) {
  return <GroupNode {...props} />;
});
