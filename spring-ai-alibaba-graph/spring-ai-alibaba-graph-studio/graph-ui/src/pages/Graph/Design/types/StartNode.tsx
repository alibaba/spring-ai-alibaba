import { Handle, Position } from '@xyflow/react';
import React from 'react';
import './base.less';
import { Flex } from 'antd';
import NodeTypes from '@/pages/Graph/Design/types/index';
import { Icon } from '@iconify/react';
type props = {
  data: any
}
 const StartNode: React.FC<props> = ({ data }) => {
  return <>
    <div
      style={{
        width: data.width||'120px',
        height: data.height||'60px',
      }}
    >
      <Handle
        type="source"
        position={Position.Right}
        className={'graph-node__handle'}
      ></Handle>
      <Flex vertical={true} className='cust-node-wrapper center'>
        <div className='node-type'>
          <Icon className='type-icon start' icon={'ic:baseline-not-started'}></Icon>Start
        </div>
      </Flex>
    </div>
  </>;
 };
export default StartNode;
