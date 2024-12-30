import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React, { useState } from 'react';
import './base.less';

const variableAggregatorNodeFormSchema = {
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

interface IVariableAggregatorNodeFormData {
  aaa: string;
}

type props = {
  data: any;
};
const StartNode: React.FC<props> = ({ data }) => {
  const [formData, setFormData] = useState<IVariableAggregatorNodeFormData>();
  const onClick = () => {
    openPanel<IVariableAggregatorNodeFormData>(
      variableAggregatorNodeFormSchema,
      {
        onConfirm: (values) => setFormData(values),
        data: formData,
      },
    );
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
