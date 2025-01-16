import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { graphState } from '@/store/GraphState';
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import { Handle, Position } from '@xyflow/react';
import { Flex, Tag } from 'antd';
import React, { memo } from 'react';
import './base.less';

interface Props {
  data: any;
}

const LLMNode: React.FC<Props> = ({ data }) => {
  const graphStore = useProxy(graphState);
  const onClick = () => {
    graphStore.formDrawer.isOpen = true;
  };

  return (
    <div
      onClick={onClick}
      style={{
        width: data.width,
        height: data.height,
      }}
    >
      <ExpandNodeToolBar></ExpandNodeToolBar>
      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Handle
        className={'graph-node__handle'}
        type="source"
        id="source"
        position={Position.Right}
      />

      <Flex vertical={true} className="cust-node-wrapper">
        <Flex>
          <div className="node-type">
            <Icon icon="material-symbols:android-safety-outline"></Icon>LLM Node
          </div>
        </Flex>
        <Flex className="cust-node-wrapper">
          <Tag color="volcano" className="node-body-tag">
            {data.label}
          </Tag>
        </Flex>
      </Flex>
    </div>
  );
};

export default memo(LLMNode);
