import { useAtom } from 'jotai';
import React, { useRef } from 'react';
import { Handle, Position } from 'react-flow-renderer';
import { selectedNodeAtom } from '../atoms/flowState';
import { BaseNodeProps } from '../types/BaseNodeProps';

export const BaseNode: React.FC<BaseNodeProps> = ({
  id,
  data,
  isConnectable,
}) => {
  const [selectedNode, setSelectedNode] = useAtom(selectedNodeAtom);
  const nodeRef = useRef<HTMLDivElement>(null);

  // 判断当前节点是否选中
  const isSelected = selectedNode === id;

  // 点击节点时选中节点，阻止事件冒泡
  const handleClick = (event: React.MouseEvent) => {
    event.stopPropagation(); 
    setSelectedNode(id);
  };

  return (
    <div
      ref={nodeRef}
      onClick={handleClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '10px 15px',
        background: data.background || '#FFFFFF',
        border: isSelected ? '2px solid #007BFF' : '2px solid #e0e4eb',
        borderRadius: '12px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        color: '#333',
        fontWeight: 500,
        fontSize: '14px',
        position: 'relative',
        whiteSpace: 'nowrap',
        width: '150px',
      }}
    >
      {/* 左侧连接头 */}
      {!data.isStartNode && (
        <Handle
          type="target"
          position={Position.Left}
          style={{
            background: '#007BFF',
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            border: '2px solid white',
          }}
          isConnectable={isConnectable}
        />
      )}

      {/* 图标区域 */}
      <div
        style={{
          width: '24px',
          height: '24px',
          marginRight: '10px',
          background: data.iconBg || '#007BFF',
          color: '#fff',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 'bold',
        }}
      >
        {data.icon || '🔹'}
      </div>

      {/* 文本区域 */}
      <div>{data.label}</div>

      {/* 右侧连接头 */}
      {!data.isEndNode && (
        <div
          style={{
            position: 'absolute',
            top: '50%',
            right: '0px',
            transform: 'translateY(-50%)',
          }}
        >
          <Handle
            type="source"
            position={Position.Right}
            style={{
              background: '#007BFF',
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              border: '2px solid white',
            }}
            isConnectable={isConnectable}
          />
          {isSelected && (
            <div
              style={{
                position: 'absolute',
                right: '-8px',
                color: '#007BFF',
                fontSize: '15px',
                fontWeight: 'bold',
                transform: 'translateY(-50%)',
                background: '#FFFFFF',
                borderRadius: '50%',
                width: '16px',
                height: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '1px solid #007BFF',
              }}
            >
              +
            </div>
          )}
        </div>
      )}
    </div>
  );
};
