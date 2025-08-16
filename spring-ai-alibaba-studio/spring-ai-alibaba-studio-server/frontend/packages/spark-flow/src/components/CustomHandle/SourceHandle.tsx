import { useNodesInteraction } from '@/hooks';
import { Handle, Position } from '@xyflow/react';
import React, { useState } from 'react';
import CustomIcon from '../CustomIcon';
import { PopoverNodeMenu } from '../NodeMenu';

interface IProps {
  handleId: string;
  nodeType: string;
  nodeId: string;
  className?: string;
  parentId?: string;
}

export default function SourceHandle(props: IProps) {
  const [open, setOpen] = useState(false);
  const { handleSelectNode } = useNodesInteraction();

  return (
    <PopoverNodeMenu
      source={{
        id: props.nodeId,
        type: props.nodeType,
        handleId: props.handleId,
      }}
      parentId={props.parentId}
      onOpenChange={(val) => {
        if (val) {
          handleSelectNode(props.nodeId);
        }
        setOpen(val);
      }}
    >
      <Handle
        onClick={(e) => {
          e.stopPropagation();
          e.preventDefault();
        }}
        className={`spark-flow-source-handle flex-center ${open ? 'spark-flow-source-handle-open' : ''} ${props.className}`}
        type="source"
        position={Position.Right}
        id={props.handleId}
      >
        <CustomIcon
          size="small"
          className="spark-flow-source-handle-add-btn"
          type="spark-plus-line"
        />
      </Handle>
    </PopoverNodeMenu>
  );
}
