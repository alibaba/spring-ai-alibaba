import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React, { useState } from 'react';
import './base.less';

const startNodeFormSchema = {
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

interface IStartNodeFormData {
  aaa: string;
}

type props = {
  data: any;
};
const StartNode: React.FC<props> = ({ data }) => {
  const [formData, setFormData] = useState<IStartNodeFormData>();
  const onClick = () => {
    openPanel<IStartNodeFormData>(startNodeFormSchema, {
      onConfirm: (values) => setFormData(values),
      data: formData,
    });
  };

  return (
    <>
      <div
        onClick={onClick}
        style={{
          width: data.width || '120px',
          height: data.height || '60px',
        }}
      >
        <Handle
          type="source"
          position={Position.Right}
          className={'graph-node__handle'}
        ></Handle>
        <Flex vertical={true} className="cust-node-wrapper center">
          <div className="node-type">
            <Icon
              className="type-icon start"
              icon={'ic:baseline-not-started'}
            ></Icon>
            Start
          </div>
        </Flex>
      </div>
    </>
  );
};

export default StartNode;
