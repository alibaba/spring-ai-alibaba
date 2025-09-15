import type { IWorkFlowNode } from '@spark-ai/flow';
import { BaseNode } from '@spark-ai/flow';
import { NodeProps } from '@xyflow/react';
import React, { memo } from 'react';

export default memo(function IteratorEndNode(props: NodeProps<IWorkFlowNode>) {
  return <BaseNode disableAction disableShowSourceHandle {...props}></BaseNode>;
});
