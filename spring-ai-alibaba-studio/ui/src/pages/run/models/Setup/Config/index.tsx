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
import { Form, Select, Slider, Input, Flex, InputNumber, Button } from 'antd';
import type { SelectProps } from 'antd';
import { InitialConfig } from '../types';

type Props = {
  initialConfig: InitialConfig;
};

export default function ConfigAndTool(props: Props) {
  const { initialConfig } = props;

  const [form] = Form.useForm();

  const [temperature, setTemperature] = useState(initialConfig.temperature);
  const [topP, setTopP] = useState(initialConfig.topP);
  const [topK, setTopK] = useState(initialConfig.topK);

  const modelOptions: SelectProps['options'] = [
    { value: 'ollama/llama3.2', label: 'ollama/llama3.2' },
    { value: 'chatgpt', label: 'chatgpt' },
  ];

  const versionOptions: SelectProps['options'] = [
    { value: '1', label: '1' },
    { value: '2', label: '2' },
  ];

  const sliderLabel = (left, right) => {
    return (
      <Flex justify="space-between" style={{ width: 300 }}>
        <span>{left}</span>
        <span>{right}</span>
      </Flex>
    );
  };

  const onTemperatureChange = (value) => {
    setTemperature(value);
  };

  const onTopPChange = (value) => {
    setTopP(value);
  };

  const onTopKChange = (value) => {
    setTopK(value);
  };

  const reset = () => {
    setTemperature(initialConfig.temperature);
    setTopK(initialConfig.topK);
    setTopP(initialConfig.topP);
    form.resetFields();
  };

  return (
    <>
      <Form layout="vertical" form={form} initialValues={initialConfig}>
        <Form.Item label="Model" name="modelCOnfig">
          <Select style={{ width: 200 }} options={modelOptions} />
        </Form.Item>
        <Form.Item
          label={sliderLabel('Temperature', temperature)}
          name="temperature"
        >
          <Slider onChange={onTemperatureChange} />
        </Form.Item>
        <Form.Item label={sliderLabel('Top P', topP)} name="topP">
          <Slider onChange={onTopPChange} />
        </Form.Item>
        <Form.Item label={sliderLabel('Top K', topK)} name="topK">
          <Slider onChange={onTopKChange} />
        </Form.Item>
        <Form.Item label="Max output tokens" name="maxTokens">
          <InputNumber min={1} max={10} />
        </Form.Item>
        <Form.Item label="Stop sequences" name="sequences">
          <Input placeholder="input placeholder" />
        </Form.Item>
        <Form.Item label="Model version" name="version">
          <Select style={{ width: 200 }} options={versionOptions} />
        </Form.Item>
      </Form>
      <Flex justify="center">
        <Button onClick={reset}>Reset</Button>
      </Flex>
    </>
  );
}
