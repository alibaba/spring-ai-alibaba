import { Modal, Input, message, Typography, Button } from "antd";
import { useState } from "react";
import type { MockTool } from "../hooks/useFunctions";
import CodeMirror from '@uiw/react-codemirror';
import { json } from '@codemirror/lang-json';
import { githubLight } from '@uiw/codemirror-theme-github';
import { safeJSONParse, safeJSONStringify } from "../../../../utils/util";


interface AddFunctionModalProps {
  open: boolean;
  onCancel: () => void;
  onOk: (data: { toolDefinition: MockTool["toolDefinition"], output: string }) => void;
  functions: MockTool[];
}

function AddFunctionModal(props: AddFunctionModalProps) {
  const { open, onCancel, onOk, functions } = props;

  const [schema, setSchema] = useState('');
  const [defaultValue, setDefaultValue] = useState('');

  const handleOk = () => {
    try {
      const toolDefinition = safeJSONParse(schema);
      const { name, description, parameters } = toolDefinition as {
        name: string; description: string; parameters: any;
      };
      if (functions.find(f => f.toolDefinition.name === name)) {
        message.error('函数已存在')
        return
      }
      onOk({ toolDefinition: { name, description, parameters: safeJSONStringify(parameters) }, output: defaultValue });
      setSchema('');
      setDefaultValue('');
    } catch (e) {
      message.error('JSON 格式错误，请检查后重试');
    }
  }

  const handleCancel = () => {
    setSchema('');
    setDefaultValue('');
    onCancel?.();
  }

  const handleInsertTemplate = () => {
    setSchema(safeJSONStringify({
      name: 'get_weather',
      description: 'Determine weather in my location',
      parameters: {
        "type": "object",
        "properties": {
          "location": {
            "type": "string",
            "description": "The city and state e.g. San Francisco, CA"
          },
          "unit": {
            "type": "string",
            "enum": [
              "c",
              "f"
            ]
          }
        },
        "required": [
          "location"
        ]
      }
    }, undefined, undefined, 2));
  }

  return (
    <Modal
      destroyOnHidden
      open={open}
      onCancel={handleCancel}
      title="新增函数"
      width={900}
      onOk={handleOk}
    >
      <section className="flex gap-6 w-full">
        <div style={{ width: "60%" }}>
          <div className="flex justify-between">
            <Typography.Paragraph className="flex items-center" style={{ marginBottom: 0 }} copyable={{ text: schema }} >SCHEMA</Typography.Paragraph>
            <Button type="text" onClick={handleInsertTemplate}>插入模版</Button>
          </div>
          <div
            className="border border-solid border-[#d9d9d9] hover:border-[#4096ff] rounded-md mt-2 overflow-hidden"
          >
            <CodeMirror
              style={{ outline: "none" }}
              value={schema}
              height="514px"
              extensions={[json()]}
              onChange={(value) => setSchema(value)}
              theme={githubLight}
              className="font-mono text-xs"
            />
          </div>
        </div>
        <div style={{ width: "40%" }}>
          <div className="flex justify-between">
            <Typography.Paragraph className="flex items-center" style={{ marginBottom: 0 }} copyable={{ text: safeJSONStringify(defaultValue) }} >默认模拟值</Typography.Paragraph>
            <Button type="text" className="invisible"></Button>
          </div>
          <div className="border rounded-md mt-2">
            <Input.TextArea
              placeholder="请输入模拟值来模拟函数返回"
              value={defaultValue}
              onChange={(e) => setDefaultValue(e.target.value)}
              rows={23}
              style={{ outline: "none" }}
            />
          </div>
        </div>
      </section>

    </Modal>
  )
}

export default AddFunctionModal;
