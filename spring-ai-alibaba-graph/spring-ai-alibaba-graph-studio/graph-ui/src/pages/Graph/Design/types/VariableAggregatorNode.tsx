import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { graphState } from '@/store/GraphState';
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React from 'react';
import './base.less';

type props = {
  data: any;
};
const StartNode: React.FC<props> = ({ data }) => {
  const graphStore = useProxy(graphState);
  const onClick = () => {
    graphStore.formDrawer.isOpen = true;
  };

  return (
    <>
      <div
        onClick={onClick}
        style={{
          width: data.width,
          height: data.height,
        }}
      >
        <ExpandNodeToolBar></ExpandNodeToolBar>
        <Flex vertical={true} className="cust-node-wrapper">
          <div className="node-type">
            <Icon className="type-icon" icon="arcticons:aggregator"></Icon>
            Variable Aggregator
          </div>
          <Flex className="body">{data.label}</Flex>
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
    </>
  );
};

export default StartNode;
