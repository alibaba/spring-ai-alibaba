import { useNodesInteraction } from '@/hooks';
import { getCommonConfig } from '@spark-ai/design';
import { NodeResizeControl } from '@xyflow/react';
import React, { memo } from 'react';
import './index.less';

const Icon = () => {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
    >
      <path
        d="M5.19009 11.8398C8.26416 10.6196 10.7144 8.16562 11.9297 5.08904"
        stroke={`var(--${getCommonConfig().antPrefix}-color-border-secondary)`}
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
};

type NodeResizerProps = {
  nodeId: string;
  icon?: JSX.Element;
  minWidth?: number;
  minHeight?: number;
  maxWidth?: number;
};
const NodeResizer = ({
  nodeId,
  icon = <Icon />,
  minWidth = 320,
  minHeight = 58,
  maxWidth,
}: NodeResizerProps) => {
  const { onNodeResize } = useNodesInteraction();

  return (
    <NodeResizeControl
      position="bottom-right"
      className={'spark-flow-node-resizer'}
      onResize={(_, resizeBound) => onNodeResize(resizeBound, nodeId)}
      minWidth={minWidth}
      minHeight={minHeight}
      maxWidth={maxWidth}
    >
      <div className={'spark-flow-node-resizer-icon'}>{icon}</div>
    </NodeResizeControl>
  );
};

export default memo(NodeResizer);
