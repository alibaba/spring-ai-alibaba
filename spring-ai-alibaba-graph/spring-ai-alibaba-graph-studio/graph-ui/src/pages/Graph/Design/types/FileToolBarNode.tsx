/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
