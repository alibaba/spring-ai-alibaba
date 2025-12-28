import { Handle, Position, useConnection } from '@xyflow/react';
import React from 'react';

interface IProps {
  handleId: string;
}

export default function TargetHandle(props: IProps) {
  const { fromNode } = useConnection();
  return (
    <>
      {!fromNode
        ? false
        : fromNode.id !== props.handleId && (
            <Handle
              className={'spark-flow-target-handle-full'}
              type="target"
              id={props.handleId}
              position={Position.Left}
            />
          )}
      <Handle
        className="spark-flow-target-handle"
        type="target"
        position={Position.Left}
        id={props.handleId}
        onClick={(event) => {
          event.stopPropagation();
          event.preventDefault();
        }}
      ></Handle>
    </>
  );
}
