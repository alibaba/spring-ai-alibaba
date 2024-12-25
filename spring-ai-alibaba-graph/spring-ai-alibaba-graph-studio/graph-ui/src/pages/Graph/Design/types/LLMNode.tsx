import { Flex, Tag } from 'antd';
import React, { memo } from 'react';
import './base.less';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';

interface Props {
  data: any;
}

const LLMNode: React.FC<Props> = ({ data }) => {
  return (
    <div
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
          <div
            className="node-type"
          >
            <Icon
              icon="material-symbols:android-safety-outline"></Icon>LLM Node
          </div>
        </Flex>
        <Flex className="cust-node-wrapper">
          <Tag color="volcano" className="node-body-tag">{data.label}</Tag>
        </Flex>
      </Flex>
    </div>
  );
};

export default memo(LLMNode);
