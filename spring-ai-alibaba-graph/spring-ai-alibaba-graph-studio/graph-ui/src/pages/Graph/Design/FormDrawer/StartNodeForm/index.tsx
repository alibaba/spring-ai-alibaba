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

import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import {
  Button,
  Checkbox,
  Form,
  FormInstance,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Typography,
} from 'antd';
import { useState } from 'react';

interface IStartNodeFormProps {
  form: FormInstance;
}

const StartNodeForm: React.FC<IStartNodeFormProps> = ({ form }) => {
  const [isVisible, setIsVisible] = useState(false);
  const [editingField, setEditingField] = useState<{
    name: number;
    values: any;
  } | null>(null);
  const [modalForm] = Form.useForm();

  const onModalClose = () => {
    setIsVisible(false);
    setEditingField(null);
    modalForm.resetFields();
  };

  const renderFieldsByType = (type: string) => {
    switch (type) {
      case 'text':
      case 'paragraph':
        return (
          <>
            <Form.Item
              label="Max Length"
              name="maxLength"
              rules={[{ type: 'number', max: 256 }]}
            >
              <InputNumber />
            </Form.Item>
          </>
        );
      case 'select':
        return (
          <>
            <Form.List name="options">
              {(fields, { add, remove /* move */ }) => (
                <>
                  {fields.map((field, index) => (
                    <Form.Item
                      required={false}
                      key={field.key}
                      label={index === 0 ? 'Options' : ''}
                    >
                      <Form.Item
                        {...field}
                        validateTrigger={['onChange', 'onBlur']}
                        rules={[
                          {
                            required: true,
                            whitespace: true,
                            message:
                              'Please input option text or delete this field',
                          },
                        ]}
                        noStyle
                      >
                        <Input
                          placeholder="Option text"
                          style={{ width: '60%' }}
                        />
                      </Form.Item>
                      <DeleteOutlined
                        className="dynamic-delete-button"
                        onClick={() => remove(field.name)}
                        style={{ marginLeft: 8 }}
                      />
                    </Form.Item>
                  ))}
                  <Form.Item>
                    <Button
                      type="dashed"
                      onClick={() => add()}
                      style={{ width: '60%' }}
                    >
                      Add Option
                    </Button>
                  </Form.Item>
                </>
              )}
            </Form.List>
          </>
        );
      case 'file':
      case 'files':
        return (
          <>
            <Form.Item
              label="Support File Types"
              name="supportFileTypes"
              rules={[{ required: true, message: 'Please select file types!' }]}
            >
              <Select
                mode="multiple"
                options={[
                  { label: 'Document', value: 'document' },
                  { label: 'Image', value: 'image' },
                  { label: 'Audio', value: 'audio' },
                  { label: 'Video', value: 'video' },
                  { label: 'Other File Types', value: 'other' },
                ]}
              />
            </Form.Item>
            <Form.Item
              noStyle
              shouldUpdate={(prevValues, currentValues) =>
                prevValues.supportFileTypes !== currentValues.supportFileTypes
              }
            >
              {({ getFieldValue }) => {
                const types = getFieldValue('supportFileTypes') || [];
                return types.includes('other') ? (
                  <Form.Item
                    label="Custom File Types"
                    name="customFileTypes"
                    rules={[
                      {
                        required: true,
                        message: 'Please input custom file types!',
                      },
                    ]}
                  >
                    <Input placeholder="e.g. .txt,.csv" />
                  </Form.Item>
                ) : null;
              }}
            </Form.Item>
            <Form.Item
              label="Upload File Method"
              name="uploadMethod"
              rules={[
                { required: true, message: 'Please select upload method!' },
              ]}
            >
              <Select
                mode="multiple"
                options={[
                  { label: 'URL', value: 'url' },
                  { label: 'Local File', value: 'local' },
                ]}
              />
            </Form.Item>
          </>
        );
        return;
      case 'number':
      default:
        return null;
    }
  };

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Typography.Title level={5}>INPUT FIELD</Typography.Title>

        <Form.List name="inputFields">
          {(fields, { add, remove }) => {
            const handleModalOk = async () => {
              try {
                const values = await modalForm.validateFields();
                if (editingField !== null) {
                  form.setFields([
                    {
                      name: ['inputFields', editingField.name],
                      value: values,
                    },
                  ]);
                } else {
                  add(values);
                }
                setIsVisible(false);
                setEditingField(null);
                modalForm.resetFields();
              } catch (error) {
                console.error('Validation failed:', error);
              }
            };

            return (
              <>
                <Modal
                  open={isVisible}
                  onCancel={onModalClose}
                  onOk={handleModalOk}
                  title={
                    editingField !== null
                      ? 'Edit Input Field'
                      : 'Add Input Field'
                  }
                >
                  <Form form={modalForm} layout="vertical">
                    <Form.Item
                      label="Type"
                      name="type"
                      rules={[
                        { required: true, message: 'Please select type!' },
                      ]}
                    >
                      <Select
                        options={[
                          { label: 'Short Text', value: 'text' },
                          { label: 'Paragraph', value: 'paragraph' },
                          { label: 'Select', value: 'select' },
                          { label: 'Number', value: 'number' },
                          { label: 'Single File', value: 'file' },
                          { label: 'File List', value: 'files' },
                        ]}
                      />
                    </Form.Item>

                    <Form.Item
                      label="Variable Name"
                      name="variableName"
                      rules={[
                        {
                          required: true,
                          message: 'Please input variable name',
                        },
                        {
                          pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
                          message:
                            'Variable name must start with a letter and can only contain letters, numbers, and underscores',
                        },
                      ]}
                    >
                      <Input />
                    </Form.Item>

                    <Form.Item
                      label="Label Name"
                      name="labelName"
                      rules={[
                        { required: true, message: 'Please input label name!' },
                      ]}
                    >
                      <Input />
                    </Form.Item>

                    <Form.Item
                      noStyle
                      shouldUpdate={(prevValues, currentValues) =>
                        prevValues.type !== currentValues.type
                      }
                    >
                      {({ getFieldValue }) =>
                        renderFieldsByType(getFieldValue('type'))
                      }
                    </Form.Item>

                    <Form.Item label="Required" name="required">
                      <Checkbox></Checkbox>
                    </Form.Item>
                  </Form>
                </Modal>

                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                  }}
                >
                  {fields.map((field) => (
                    <Form.Item
                      key={field.key}
                      noStyle
                      shouldUpdate={(prevValues, currentValues) => {
                        const prev = prevValues.inputFields?.[field.name];
                        const curr = currentValues.inputFields?.[field.name];
                        return JSON.stringify(prev) !== JSON.stringify(curr);
                      }}
                    >
                      {({ getFieldValue }) => {
                        const fieldData = getFieldValue([
                          'inputFields',
                          field.name,
                        ]);
                        if (!fieldData) return null;

                        return (
                          <div
                            style={{
                              padding: 16,
                              border: '1px solid #d9d9d9',
                              borderRadius: 8,
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                            }}
                          >
                            <div>
                              <Typography.Text strong>
                                {fieldData.labelName}
                              </Typography.Text>
                              <div style={{ color: '#666' }}>
                                <div>Variable: {fieldData.variableName}</div>
                                <div>Type: {fieldData.type}</div>
                                {fieldData.required && <div>Required: Yes</div>}
                              </div>
                            </div>
                            <Space>
                              <Button
                                type="text"
                                icon={<EditOutlined />}
                                onClick={() => {
                                  setEditingField({
                                    name: field.name,
                                    values: fieldData,
                                  });
                                  modalForm.setFieldsValue(fieldData);
                                  setIsVisible(true);
                                }}
                              />
                              <Button
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={() => remove(field.name)}
                              />
                            </Space>
                          </div>
                        );
                      }}
                    </Form.Item>
                  ))}
                </div>

                <Button
                  type="dashed"
                  style={{ width: '100%', marginTop: 16 }}
                  onClick={() => {
                    setEditingField(null);
                    modalForm.resetFields();
                    setIsVisible(true);
                  }}
                >
                  + Add Input Field
                </Button>
              </>
            );
          }}
        </Form.List>
      </div>
    </>
  );
};

export default StartNodeForm;
