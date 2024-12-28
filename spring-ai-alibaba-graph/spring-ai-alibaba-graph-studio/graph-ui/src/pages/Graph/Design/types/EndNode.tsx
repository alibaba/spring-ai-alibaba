import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React from 'react';
import './base.less';

type props = {
  data: any;
};
const EndNode: React.FC<props> = ({ data }) => {
  return (
    <>
      <div
        style={{
          width: data.width || '120px',
          height: data.height || '60px',
        }}
      >
        <Handle
          type="target"
          position={Position.Left}
          className={'graph-node__handle'}
        ></Handle>
        <Flex vertical={true} className="cust-node-wrapper center">
          <div className="node-type">
            <Icon
              className="type-icon end"
              icon={'material-symbols:stop-circle'}
            ></Icon>
            End
          </div>
        </Flex>
      </div>
    </>
  );
};
export default EndNode;
