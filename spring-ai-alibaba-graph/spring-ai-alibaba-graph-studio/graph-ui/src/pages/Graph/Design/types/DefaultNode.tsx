import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { openPanel } from '@/utils/FormUtils';
import { Icon } from '@iconify/react';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React, { useState } from 'react';
import './base.less';

const defaultNodeFormSchema = {
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

interface IDefaultNodeFormData {
  aaa: string;
}

type props = {
  data: any;
};
const StartNode: React.FC<props> = ({ data }) => {
  const [formData, setFormData] = useState<IDefaultNodeFormData>();
  const onClick = () => {
    openPanel<IDefaultNodeFormData>(defaultNodeFormSchema, {
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
        <ExpandNodeToolBar></ExpandNodeToolBar>
        <Flex vertical={true} className="cust-node-wrapper">
          <div className="node-type">
            <Icon
              style={{
                marginBottom: '-2px',
              }}
              icon="material-symbols:android-safety-outline"
            ></Icon>
            KR Node
          </div>
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
        {data.label}
      </div>
    </>
  );
};

export default StartNode;
