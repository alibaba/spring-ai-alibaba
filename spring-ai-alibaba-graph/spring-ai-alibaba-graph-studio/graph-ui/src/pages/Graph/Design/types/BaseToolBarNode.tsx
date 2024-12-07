import { EditOutlined, StepForwardOutlined } from '@ant-design/icons';
import { Handle, NodeToolbar, Position } from '@xyflow/react';
import { Button } from 'antd';
import ButtonGroup from 'antd/es/button/button-group';
import React, { memo } from 'react';
import './base.less';

interface Props {
  data: any;
  children: any;
}

const ToolbarNode: React.FC<Props> = ({ data, children }) => {
  return (
    <>
      <NodeToolbar align={'end'} isVisible>
        <ButtonGroup size={'small'}>
          <Button>
            <StepForwardOutlined
              style={{
                color: 'var(--ant-color-primary)',
              }}
            />
          </Button>
          <Button>
            <EditOutlined
              style={{
                color: 'var(--ant-color-primary)',
              }}
            />
          </Button>
          <Button
            style={{
              color: 'var(--ant-color-primary)',
            }}
          >
            ...
          </Button>
        </ButtonGroup>
      </NodeToolbar>

      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Handle
        className={'graph-node__handle'}
        type="source"
        position={Position.Right}
      />
      <div
        style={{
          marginBottom: '10px',
          fontWeight: 'bold',
          fontSize: '16px',
        }}
      >
        {data.label}
      </div>
      {children}
    </>
  );
};

export default memo(ToolbarNode);
