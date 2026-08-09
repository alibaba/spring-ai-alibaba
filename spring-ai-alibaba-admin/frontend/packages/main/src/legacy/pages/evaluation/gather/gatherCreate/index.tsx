import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Select, message, Space, Divider } from 'antd';
import { ArrowLeftOutlined, PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import API from '../../../../services';
import './index.css';

const { TextArea } = Input;
const { Option } = Select;

// 数据类型选项
const DATA_TYPES = [
  { value: 'String', label: 'String' },
  { value: 'Number', label: 'Number' },
  { value: 'Boolean', label: 'Boolean' },
  { value: 'Array', label: 'Array' },
  { value: 'Object', label: 'Object' }
];

// 查看格式选项
const VIEW_FORMATS = [
  { value: 'PlainText', label: 'PlainText' },
  { value: 'JSON', label: 'JSON' },
  { value: 'Markdown', label: 'Markdown' },
  { value: 'HTML', label: 'HTML' }
];

// 列配置接口
interface ColumnConfig {
  name: string;
  dataType: string;
  displayFormat: string;
  description: string;
  required: boolean;
}

// 表单数据接口
interface CreateDatasetForm {
  name: string;
  description: string;
  columns: ColumnConfig[];
}

// 组件属性接口
interface GatherCreateProps {
  onCancel?: () => void;
  onSuccess?: () => void;
  hideTitle?: boolean; // 添加hideTitle属性来控制是否隐藏标题
}

const GatherCreate: React.FC<GatherCreateProps> = ({ onCancel, onSuccess, hideTitle = false }) => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // 返回列表页面
  const handleGoBack = () => {
    if (onCancel) {
      onCancel();
    } else {
      navigate('/evaluation-gather');
    }
  };

  // 提交表单
  const handleSubmit = async (values: CreateDatasetForm) => {
    try {
      setLoading(true);
      
      // 构造提交数据
      const submitData = {
        name: values.name,
        description: values.description,
        columnsConfig: values.columns.map(column => ({
          ...column,
          required: true as const // API要求required字段必须为true
        })),
      };

      console.log('Submitting data:', submitData);
      
      // 这里调用创建评测集的API
      await API.createDataset(submitData);
      
      message.success('Evaluation set created successfully');
      
      // 如果提供了onSuccess回调，则调用它，否则导航到列表页面
      if (onSuccess) {
        onSuccess();
      } else {
        navigate('/evaluation-gather');
      }
    } catch (error) {
      message.error('Creation failed. Please try again.');
      console.error('Failed to create evaluation set:', error);
    } finally {
      setLoading(false);
    }
  };

  // 取消创建
  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    } else {
      navigate('/evaluation-gather');
    }
  };

  return (
    <div className="gather-create-page">
      {/* 页面头部 - 固定在顶部 */}
      {!hideTitle && (
        <div className="gather-create-header">
          <div className="flex items-center">
            <Button 
              type="text" 
              icon={<ArrowLeftOutlined />} 
              onClick={handleGoBack}
              className="mr-3"
            >
            </Button>
            <h1 className="text-2xl font-semibold mb-0">Create Evaluation Set</h1>
          </div>
        </div>
      )}

      {/* 页面内容 - 可滚动区域 */}
      <div className={`gather-create-content ${hideTitle ? 'pt-6' : ''}`}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            columns: [
              {
                name: 'input',
                dataType: 'String',
                displayFormat: 'PlainText',
                description: 'Actual input provided to the item being evaluated.',
                required: true
              },
              {
                name: 'reference_output',
                dataType: 'String',
                displayFormat: 'PlainText',
                description: 'Reference answer (the expected ideal output used as an evaluation standard).',
                required: true
              }
            ]
          }}
        >
          {/* 基本信息 */}
          <Card title="Basic Information" className="mb-6">
            <Form.Item
              name="name"
              label="Evaluation Set Name"
              rules={[
                { required: true, message: 'Please enter an evaluation set name' },
                { max: 100, message: 'Name cannot exceed 100 characters' }
              ]}
            >
              <Input placeholder="e.g., Q&A Assistant" />
            </Form.Item>

            <Form.Item
              name="description"
              label="Evaluation Set Description"
              rules={[
                { max: 500, message: 'Description cannot exceed 500 characters' }
              ]}
            >
              <TextArea 
                placeholder="Optionally enter an evaluation set description"
                rows={4}
                showCount
                maxLength={500}
              />
            </Form.Item>
          </Card>

          {/* 数据集列结构配置 */}
          <Form.List name="columns">
            {(fields, { add, remove }) => {
              const formValues = form.getFieldsValue();
              
              return (
                <Card 
                  title="Evaluation Set Column Configuration" 
                  extra={
                    <Button
                      type="primary"
                      onClick={() => add({
                        name: '',
                        dataType: 'String',
                        displayFormat: 'PlainText',
                        description: '',
                        required: false
                      })}
                      icon={<PlusOutlined />}
                      size="small"
                    >
                      Add Column
                    </Button>
                  }
                  className="mb-6"
                >
                    {fields.map(({ key, name, ...restField }) => {
                      const currentColumn = formValues?.columns?.[name];
                      const isRequired = currentColumn?.required;
                      
                      return (
                        <Card 
                          key={key}
                          type="inner"
                          className="mb-4"
                          title={
                            <Form.Item
                              {...restField}
                              name={[name, 'name']}
                              className="mb-0"
                            >
                              <Input 
                                placeholder="Column Name" 
                                variant="borderless"
                                className="font-medium"
                              />
                            </Form.Item>
                          }
                          extra={
                            !isRequired && (
                              <Button
                                type="text"
                                danger
                                icon={<MinusCircleOutlined />}
                                onClick={() => remove(name)}
                              />
                            )
                          }
                        >
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                            <Form.Item
                              {...restField}
                              name={[name, 'name']}
                              label="Column Name"
                              rules={[{ required: true, message: 'Please enter a column name' }]}
                            >
                              <Input placeholder="e.g., input" />
                            </Form.Item>

                            <Form.Item
                              {...restField}
                              name={[name, 'dataType']}
                              label="Data Type"
                              rules={[{ required: true, message: 'Please select a data type' }]}
                            >
                              <Select placeholder="Please select">
                                {DATA_TYPES.map(type => (
                                  <Option key={type.value} value={type.value}>
                                    {type.label}
                                  </Option>
                                ))}
                              </Select>
                            </Form.Item>

                            <Form.Item
                              {...restField}
                              name={[name, 'displayFormat']}
                              label="Display Format"
                              rules={[{ required: true, message: 'Please select a display format' }]}
                            >
                              <Select placeholder="Please select">
                                {VIEW_FORMATS.map(format => (
                                  <Option key={format.value} value={format.value}>
                                    {format.label}
                                  </Option>
                                ))}
                              </Select>
                            </Form.Item>
                          </div>

                          <Form.Item
                            {...restField}
                            name={[name, 'description']}
                            label="Column Description"
                            rules={[{ required: true, message: 'Please enter a column description' }]}
                          >
                            <TextArea 
                              placeholder="Enter a description for this column"
                              rows={3}
                            />
                          </Form.Item>
                        
                          {/* 隐藏的required字段 */}
                          <Form.Item
                            {...restField}
                            name={[name, 'required']}
                            hidden
                          >
                            <Input />
                          </Form.Item>
                        </Card>
                      );
                    })}
                </Card>
                );
              }}
            </Form.List>
        </Form>
      </div>

      {/* 底部操作按钮 - 固定在底部 */}
      <div className="gather-create-footer">
        <div className="flex justify-end space-x-4">
          <Button size="large" onClick={handleCancel}>
            Cancel
          </Button>
          <Button 
            type="primary" 
            size="large" 
            htmlType="submit"
            loading={loading}
            onClick={() => form.submit()}
          >
            Create
          </Button>
        </div>
      </div>
    </div>
  );
};

export default GatherCreate;