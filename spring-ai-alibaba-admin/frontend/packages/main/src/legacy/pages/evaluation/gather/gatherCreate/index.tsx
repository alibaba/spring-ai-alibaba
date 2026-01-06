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

      console.log('提交数据:', submitData);
      
      // 这里调用创建评测集的API
      await API.createDataset(submitData);
      
      message.success('评测集创建成功');
      
      // 如果提供了onSuccess回调，则调用它，否则导航到列表页面
      if (onSuccess) {
        onSuccess();
      } else {
        navigate('/evaluation-gather');
      }
    } catch (error) {
      message.error('创建失败，请重试');
      console.error('创建评测集失败:', error);
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
            <h1 className="text-2xl font-semibold mb-0">创建评测集</h1>
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
                description: '实际输入（作为输入)过程给评测对象)',
                required: true
              },
              {
                name: 'reference_output',
                dataType: 'String',
                displayFormat: 'PlainText',
                description: '参考输出答案（预期理想输出，可作为评估时的参考标准)',
                required: true
              }
            ]
          }}
        >
          {/* 基本信息 */}
          <Card title="基本信息" className="mb-6">
            <Form.Item
              name="name"
              label="评测集名称"
              rules={[
                { required: true, message: '请输入评测集名称' },
                { max: 100, message: '名称不能超过100个字符' }
              ]}
            >
              <Input placeholder="如：问答机器人" />
            </Form.Item>

            <Form.Item
              name="description"
              label="评测集描述"
              rules={[
                { max: 500, message: '描述不能超过500个字符' }
              ]}
            >
              <TextArea 
                placeholder="可选填写评测集描述"
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
                  title="评测集列结构配置" 
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
                      添加列
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
                                placeholder="列名称" 
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
                              label="列名称"
                              rules={[{ required: true, message: '请输入列名称' }]}
                            >
                              <Input placeholder="如：input" />
                            </Form.Item>

                            <Form.Item
                              {...restField}
                              name={[name, 'dataType']}
                              label="数据类型"
                              rules={[{ required: true, message: '请选择数据类型' }]}
                            >
                              <Select placeholder="请选择">
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
                              label="查看格式"
                              rules={[{ required: true, message: '请选择查看格式' }]}
                            >
                              <Select placeholder="请选择">
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
                            label="列描述"
                            rules={[{ required: true, message: '请输入列描述' }]}
                          >
                            <TextArea 
                              placeholder="请输入列的描述信息"
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
            取消
          </Button>
          <Button 
            type="primary" 
            size="large" 
            htmlType="submit"
            loading={loading}
            onClick={() => form.submit()}
          >
            创建
          </Button>
        </div>
      </div>
    </div>
  );
};

export default GatherCreate;