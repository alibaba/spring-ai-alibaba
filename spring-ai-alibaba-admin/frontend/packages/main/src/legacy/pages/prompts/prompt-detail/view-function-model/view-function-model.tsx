import { Modal, Card, Input, Button, message, Typography, Popconfirm } from "antd";
import { useEffect, useState } from "react";
import type { MockTool } from "../hooks/useFunctions";
import CodeMirror from '@uiw/react-codemirror';
import { json } from '@codemirror/lang-json';
import { githubLight } from '@uiw/codemirror-theme-github';
import { DeleteOutlined } from '@ant-design/icons';
import { safeJSONParse, safeJSONStringify } from "../../../../utils/util";

interface ViewFunctionModelProps {
  open: boolean;
  onCancel: () => void;
  onOk: (updatedFunctions: MockTool[]) => void;
  functions: MockTool[];
  selectedFunction: MockTool | null;
}

function ViewFunctionModel(props: ViewFunctionModelProps) {
  const { open, onCancel, onOk, functions, selectedFunction: defaultSelectedFunction } = props;
  const [selectedFunction, setSelectedFunction] = useState<MockTool | null>(defaultSelectedFunction || functions[0] || null);
  const [editingFunction, setEditingFunction] = useState<MockTool | null>(null);
  const [schema, setSchema] = useState('');
  const [defaultValue, setDefaultValue] = useState('');
  const [localFunctions, setLocalFunctions] = useState<MockTool[]>(functions);

  // Update local functions when props change
  useEffect(() => {
    setLocalFunctions(functions);
    if (functions.length > 0 && !selectedFunction) {
      setSelectedFunction(functions[0]);
    }
  }, [functions]);

  const handleFunctionClick = (fn: MockTool) => {
    setSelectedFunction(fn);
    setEditingFunction(null);
  };

  const handleEdit = (fn: MockTool) => {
    setEditingFunction(fn);
    setSchema(safeJSONStringify({
      name: fn.toolDefinition.name,
      description: fn.toolDefinition.description,
      parameters: safeJSONParse(fn.toolDefinition.parameters)
    }, undefined, undefined, 2));
    setDefaultValue(fn.output);
  };

  const handleSaveEdit = () => {
    try {
      const toolDefinition = JSON.parse(schema);
      const { name, description, parameters } = toolDefinition as {
        name: string; description: string; parameters: any;
      };

      const updatedFunction: MockTool = {
        toolDefinition: {
          name,
          description,
          parameters: JSON.stringify(parameters)
        },
        output: defaultValue
      };

      const updatedFunctions = localFunctions.map(fn =>
        fn.toolDefinition.name === editingFunction?.toolDefinition.name ? updatedFunction : fn
      );

      setLocalFunctions(updatedFunctions);
      setSelectedFunction(updatedFunction);
      setEditingFunction(null);
      message.success('Function updated successfully');
    } catch (error) {
      message.error('Invalid JSON format. Please check and try again.');
    }
  };

  const handleCancelEdit = () => {
    setEditingFunction(null);
    setSchema('');
    setDefaultValue('');
  };

  const handleOk = () => {
    onOk(localFunctions);
    setLocalFunctions(localFunctions);
    setSelectedFunction(defaultSelectedFunction || localFunctions[0] || null);
    setEditingFunction(null);
  };

  const handleDelete = (functionName: string) => {
    const updatedFunctions = localFunctions.filter(fn => fn.toolDefinition.name !== functionName);
    setLocalFunctions(updatedFunctions);
    message.success('Function deleted successfully');

    if (selectedFunction?.toolDefinition.name === functionName) {
      setSelectedFunction(updatedFunctions[0] || null);
      setEditingFunction(null);
    }
  };

  const handleCancel = () => {
    setLocalFunctions(functions);
    setSelectedFunction(defaultSelectedFunction || functions[0] || null);
    setEditingFunction(null);
    onCancel();
  };

  useEffect(() => {
    if (defaultSelectedFunction) {
      setSelectedFunction(defaultSelectedFunction);
    }
  }, [defaultSelectedFunction]);

  return (
    <Modal
      destroyOnHidden
      open={open}
      onCancel={handleCancel}
      onOk={handleOk}
      title="View Function"
      width={1200}
      okText="Save"
      cancelText="Cancel"
      okButtonProps={{
        disabled: Boolean(editingFunction)
      }}
    >
      <div className="flex gap-4" style={{ height: "600px" }}>
        {/* 左侧Function List */}
        <div className="w-80 border-r border-gray-200 pr-4">
          <div className="text-sm font-medium text-gray-700 mb-3">Function List</div>
          <div className="space-y-2 overflow-y-auto">
            {localFunctions.map((fn) => (
              <Card
                key={fn.toolDefinition.name}
                size="small"
                className={`cursor-pointer transition-colors ${selectedFunction?.toolDefinition.name === fn.toolDefinition.name
                  ? 'border-blue-500 bg-blue-50'
                  : 'hover:border-gray-400'
                  }`}
                onClick={() => handleFunctionClick(fn)}
                title={fn.toolDefinition.name}
                extra={
                  <Popconfirm
                    title="Delete this function?"
                    onConfirm={(e) => {
                      e?.stopPropagation();
                      handleDelete(fn.toolDefinition.name);
                    }}
                    onCancel={(e) => e?.stopPropagation()}
                    okText="OK"
                    cancelText="Cancel"
                  >
                    <Button
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Popconfirm>
                }
              >
                <div className="text-xs text-gray-500 mt-1 line-clamp-2">
                  {fn.toolDefinition.description}
                </div>
              </Card>
            ))}
          </div>
        </div>

        {/* 右侧函数详情 */}
        <div className="flex-1">
          {selectedFunction ? (
            editingFunction?.toolDefinition.name === selectedFunction.toolDefinition.name ? (
              // Edit模式
              <div className="h-full">
                <div className="flex justify-between items-center mb-4">
                  <div className="text-sm font-medium text-gray-700">Edit Function</div>
                  <div className="space-x-2">
                    <Button size="small" onClick={handleCancelEdit}>Cancel</Button>
                    <Button size="small" type="primary" onClick={handleSaveEdit}>Save</Button>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div style={{ width: "60%" }}>
                    <div className="flex justify-between">
                      <Typography.Paragraph className="flex items-center" style={{ marginBottom: 0 }} copyable={{ text: schema }}>SCHEMA</Typography.Paragraph>
                    </div>
                    <div className="border border-solid border-[#d9d9d9] hover:border-[#4096ff] rounded-md mt-2 overflow-hidden">
                      <CodeMirror
                        style={{ outline: "none" }}
                        value={schema}
                        height="514px"
                        extensions={[json()]}
                        onChange={(value) => setSchema(value)}
                        theme={githubLight}
                      />
                    </div>
                  </div>
                  <div style={{ width: "40%" }}>
                    <div className="flex justify-between">
                      <Typography.Paragraph className="flex items-center" style={{ marginBottom: 0 }} copyable={{ text: JSON.stringify(defaultValue) }} >Default Mock Value</Typography.Paragraph>
                      <Button type="text" className="invisible"></Button>
                    </div>
                    <Input.TextArea
                      value={defaultValue}
                      onChange={(e) => setDefaultValue(e.target.value)}
                      rows={23}
                    />
                  </div>
                </div>
              </div>
            ) : (
              // 查看模式
              <div className="h-full">
                <div className="flex justify-between items-center mb-4">
                  <div className="text-lg font-medium">{selectedFunction.toolDefinition.name}</div>
                  <Button size="small" type="primary" onClick={() => handleEdit(selectedFunction)}>
                    Edit
                  </Button>
                </div>

                <div className="space-y-4">
                  <div>
                    <div className="text-sm font-medium text-gray-700 mb-2">Description</div>
                    <div className="text-sm text-gray-600 p-3 bg-gray-50 rounded">
                      {selectedFunction.toolDefinition.description}
                    </div>
                  </div>

                  <div>
                    <div className="text-sm font-medium text-gray-700 mb-2">Parameters</div>
                    <pre className="text-xs bg-gray-50 p-3 rounded overflow-auto max-h-70 font-mono">
                      {JSON.stringify(JSON.parse(selectedFunction.toolDefinition.parameters), null, 2)}
                    </pre>
                  </div>

                  <div>
                    <div className="text-sm font-medium text-gray-700 mb-2">Default Mock Value</div>
                    <pre className="text-xs bg-gray-50 p-3 rounded overflow-auto max-h-32 font-mono">
                      {selectedFunction.output}
                    </pre>
                  </div>
                </div>
              </div>
            )
          ) : (
            <div className="flex items-center justify-center h-full text-gray-400">
              Select a function to view details
            </div>
          )}
        </div>
      </div>
    </Modal>
  )
}

export default ViewFunctionModel;
