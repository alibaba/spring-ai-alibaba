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
