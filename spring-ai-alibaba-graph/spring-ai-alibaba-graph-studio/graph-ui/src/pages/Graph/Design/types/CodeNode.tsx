import { Handle, Position } from '@xyflow/react';
import React from 'react';
import { Icon } from '@iconify/react';
import { Flex } from 'antd';
import './base.less';

type props = {
  data: any
}
const StartNode: React.FC<props> = ({ data }) => {
  return <>
    <div
      style={{
        width: data.width,
        height: data.height,
      }}
    >
      <Flex vertical={true} className="cust-node-wrapper">
        <div
          className="node-type"
        >
          <Icon
            style={{
              marginBottom: '-2px',
            }}
            icon="material-symbols-light:terminal-sharp"></Icon>Code Node
        </div>
        <Flex className='body'>{data.label}</Flex>
      </Flex>
      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Handle
        type="source"
        position={Position.Right}
        className={'graph-node__handle'}
      ></Handle>

    </div>
  </>;
};
export default StartNode;