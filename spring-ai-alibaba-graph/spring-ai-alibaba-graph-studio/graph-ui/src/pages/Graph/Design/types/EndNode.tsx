import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React, { useState } from 'react';
import './base.less';

const endNodeFormSchema = {
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

interface IEndNodeFormData {
  aaa: string;
}

type props = {
  data: any;
};
const EndNode: React.FC<props> = ({ data }) => {
  const [formData, setFormData] = useState<IEndNodeFormData>();
  const onClick = () => {
    openPanel<IEndNodeFormData>(endNodeFormSchema, {
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
