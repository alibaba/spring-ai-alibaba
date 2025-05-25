import BaseToolBarNode from '@/pages/Graph/Design/types/BaseToolBarNode';
import { Form, Input } from 'antd';
import FormItem from 'antd/es/form/FormItem';
import { memo } from 'react';
import './base.less';

interface Props {
  data: any;
}

const ToolbarNode: React.FC<Props> = ({ data }) => {
  let [formIns] = Form.useForm();
  return (
    <>
      <BaseToolBarNode data={data}>
        <Form
          form={formIns}
          initialValues={data.form}
          labelCol={{ span: 4 }}
          wrapperCol={{ span: 20 }}
        >
          <FormItem
            rules={[
              { required: true, message: '请输入名称' },
              { min: 5, max: 10, message: '长度在 5-10' },
            ]}
            validateTrigger="onChange"
            label={'label'}
            name="name"
          >
            <Input placeholder="please input "></Input>
          </FormItem>
        </Form>
      </BaseToolBarNode>
    </>
  );
};

export default memo(ToolbarNode);
