import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React, { useState } from 'react';
import './base.less';

const codeNodeFormSchema = {
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

interface ICodeNodeFormData {
  aaa: string;
}

type props = {
  data: any;
};
const StartNode: React.FC<props> = ({ data }) => {
  const [formData, setFormData] = useState<ICodeNodeFormData>();
  const onClick = () => {
    openPanel<ICodeNodeFormData>(codeNodeFormSchema, {
      onConfirm: (values) => setFormData(values),
      data: formData,
    });
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
        <Flex vertical={true} className="cust-node-wrapper">
          <div className="node-type">
            <Icon
              style={{
                marginBottom: '-2px',
              }}
              icon="material-symbols-light:terminal-sharp"
            ></Icon>
            Code Node
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
