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

import { graphState } from '@/store/GraphState';
import { DeleteOutlined } from '@ant-design/icons';
import { useProxy } from '@umijs/max';
import { Button, Form, FormInstance, Input, Select, Typography } from 'antd';

interface IEndNodeFormProps {
  form: FormInstance;
}

const EndNodeForm: React.FC<IEndNodeFormProps> = ({ form }) => {
  const graphStore = useProxy(graphState);

  const findPredecessorNodes = (nodeId: string | null) => {
    if (!nodeId) return [];
    const predecessors: string[] = [];
    const visited = new Set<string>();
    const dfs = (currentId: string) => {
      const incomingEdges = graphStore.edges.filter(
        (edge) => edge.target === currentId,
      );

      for (const edge of incomingEdges) {
        const sourceId = edge.source;
        if (!visited.has(sourceId)) {
          visited.add(sourceId);
          predecessors.push(sourceId);
          dfs(sourceId);
        }
      }
    };

    dfs(nodeId);
    return predecessors;
  };

  const currentNode = graphStore.currentNodeId;
  const predecessorNodes = findPredecessorNodes(currentNode);
  const options = predecessorNodes
    ?.map((nodeId) => {
      const formData = graphStore.formDrawer.formDataMap[nodeId];
      if (!formData?.inputFields) return [];

      return formData.inputFields.map((field: any) => ({
        label: `${field.labelName} (${field.variableName})`,
        value: field.variableName,
      }));
    })
    .flat()
    .filter(Boolean);

  return (
    <div style={{ marginBottom: 16 }}>
      {/* {JSON.stringify(options)} */}
      <Typography.Title level={5}>OUTPUT FIELDS</Typography.Title>

      <Form.List name="outputFields">
        {(fields, { add, remove }) => (
          <>
            <div
              style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}
            >
              {fields.map((field) => (
                <div
                  key={field.key}
                  style={{
                    display: 'flex',
                    gap: '8px',
                    alignItems: 'flex-start',
                  }}
                >
                  <Form.Item
                    {...field}
                    name={[field.name, 'variableName']}
                    rules={[
                      { required: true, message: 'Please input variable name' },
                      {
                        pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
                        message:
                          'Variable name must start with a letter and can only contain letters, numbers, and underscores',
                      },
                    ]}
                    style={{ flex: 1, marginBottom: 0 }}
                  >
                    <Input placeholder="Output Variable Name" />
                  </Form.Item>
                  <Form.Item
                    {...field}
                    name={[field.name, 'sourceVariable']}
                    rules={[
                      {
                        required: true,
                        message: 'Please select source variable',
                      },
                    ]}
                    style={{ flex: 1, marginBottom: 0 }}
                  >
                    <Select
                      placeholder="Select Source Variable"
                      options={options}
                    />
                  </Form.Item>
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => remove(field.name)}
                  />
                </div>
              ))}
            </div>
            <Button
              type="dashed"
              onClick={() => add()}
              style={{ width: '100%', marginTop: 16 }}
            >
              + Add Output Field
            </Button>
          </>
        )}
      </Form.List>
    </div>
  );
};

export default EndNodeForm;
