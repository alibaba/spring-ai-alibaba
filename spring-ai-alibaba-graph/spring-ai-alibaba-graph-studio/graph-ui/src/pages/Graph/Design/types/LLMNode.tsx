import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex, Tag } from 'antd';
import React, { memo, useState } from 'react';
import './base.less';

const llmNodeFormSchema = {
  type: 'object',
  properties: {
    aaa: {
      type: 'string',
      title: 'input 1',
      required: true,
      'x-decorator': 'FormItem',
      'x-component': 'Input',
    },
  },
};

interface ILLMNodeFormData {
  aaa: string;
}

interface Props {
  data: any;
}

const LLMNode: React.FC<Props> = ({ data }) => {
  const [formData, setFormData] = useState<ILLMNodeFormData>();
  const onClick = () => {
    openPanel<ILLMNodeFormData>(llmNodeFormSchema, {
      onConfirm: (values) => setFormData(values),
      data: formData,
    });
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
