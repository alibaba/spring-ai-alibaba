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

import { useState } from 'react';
import { Form, Input, Flex } from 'antd';
import { ChatOptions } from '@/types/options';

type Props = {
  initialConfig: ChatOptions;
  onchangePrompt: (prompt: string) => void;
};
const { TextArea } = Input;

export default function ConfigAndPrompt(props: Props) {
  const { initialConfig, onchangePrompt } = props;

  const [form] = Form.useForm();

  const [prompt, setPrompt] = useState(initialConfig.prompt);


  const sliderLabel = (left, right) => {
    return (
      <Flex justify="space-between" style={{ width: 300 }}>
        <span>{left}</span>
        <span>{right}</span>
      </Flex>
    );
  };

  return (
    <>
      <Form layout="vertical" form={form} initialValues={initialConfig}>
        <Form.Item
          label={sliderLabel('Prompt', prompt)}
        >
          <TextArea rows={3} onChange={(e) => { onchangePrompt(e.target.value); setPrompt(e.target.value); }} />
        </Form.Item>
      </Form>
    </>
  );
}
