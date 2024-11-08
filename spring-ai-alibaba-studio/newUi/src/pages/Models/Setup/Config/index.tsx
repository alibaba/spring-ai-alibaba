/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Form, Select, Slider, Input } from 'antd';
import type { SelectProps } from 'antd';

export default function ConfigAndTool() {
  const [form] = Form.useForm();
  const modelOptions: SelectProps['options'] = [
    { value: 'ollama/llama3.2', label: 'ollama/llama3.2' },
    { value: 'chatgpt', label: 'chatgpt' },
  ];
  const versionOptions: SelectProps['options'] = [
    { value: '(Unspecified)', label: '(Unspecified)' },
  ];

  const handleChange = () => {};

  return (
    <Form layout="vertical" form={form}>
      <Form.Item label="Model">
        <Select
          defaultValue="ollama/llama3.2"
          onChange={handleChange}
          style={{ width: 200 }}
          options={modelOptions}
        />
      </Form.Item>
      <Form.Item label="Temperature">
        <Slider defaultValue={30} />
      </Form.Item>
      <Form.Item label="Top P">
        <Slider defaultValue={30} />
      </Form.Item>
      <Form.Item label="Top K">
        <Slider defaultValue={30} />
      </Form.Item>
      <Form.Item label="Max output tokens">
        <Input placeholder="input placeholder" />
      </Form.Item>
      <Form.Item label="Stop sequences">
        <Input placeholder="input placeholder" />
      </Form.Item>
      <Form.Item label="Model version">
        <Select
          defaultValue="ollama/llama3.2"
          onChange={handleChange}
          style={{ width: 200 }}
          options={versionOptions}
        />
      </Form.Item>
    </Form>
  );
}
