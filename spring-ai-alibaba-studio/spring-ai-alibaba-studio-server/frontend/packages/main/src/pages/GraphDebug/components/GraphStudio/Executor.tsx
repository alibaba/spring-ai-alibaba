import React, { useState, useEffect } from 'react';
import {
  Form,
  Input,
  Button,
  Select,
  InputNumber,
  Switch,
  Space,
  Divider,
  Typography,
  message,
} from 'antd';
import {
  UpOutlined,
  DownOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { IGraphData } from '@/types/graph';
import type { GraphStudioEvent } from './index';
import graphDebugService from '@/services/graphDebugService';
import styles from './index.module.less';

const { TextArea } = Input;
const { Text } = Typography;
const { Option } = Select;

interface ExecutorProps {
  graphData: IGraphData;
  isExpanded: boolean;
  onToggle: () => void;
  onSubmit: (formData: any) => void;
  dispatchEvent: (event: GraphStudioEvent) => void;
}

// 动态表单字段类型
interface FormField {
  name: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'select' | 'switch';
  required?: boolean;
  options?: { label: string; value: any }[];
  placeholder?: string;
  defaultValue?: any;
}

// Executor输入与执行组件
const Executor: React.FC<ExecutorProps> = ({
  graphData,
  isExpanded,
  onToggle,
  onSubmit,
  dispatchEvent,
}) => {
  const [form] = Form.useForm();
  const [isLoading, setIsLoading] = useState(false);
  const [isInitializing, setIsInitializing] = useState(false);
  const [formFields, setFormFields] = useState<FormField[]>([]);
  const [executionMode, setExecutionMode] = useState<'new' | 'resume'>('new');

  // 模拟的动态表单字段
  const mockFormFields: FormField[] = [
    {
      name: 'customerFeedback',
      label: 'Customer Feedback',
      type: 'textarea',
      required: true,
      placeholder: '请输入客户反馈内容...',
    },
    {
      name: 'customerId',
      label: 'Customer Id',
      type: 'text',
      placeholder: '客户ID（可选）',
    },
    {
      name: 'sentimentClassification',
      label: 'Sentiment Classification',
      type: 'select',
      options: [
        { label: '正面', value: 'positive' },
        { label: '负面', value: 'negative' },
        { label: '中性', value: 'neutral' },
      ],
    },
    {
      name: 'issueClassification',
      label: 'Issue Classification',
      type: 'select',
      options: [
        { label: '产品问题', value: 'product' },
        { label: '服务问题', value: 'service' },
        { label: '其他', value: 'other' },
      ],
    },
    {
      name: 'processingConfidence',
      label: 'Processing Confidence',
      type: 'number',
      defaultValue: 0.8,
    },
    {
      name: 'debugMode',
      label: 'Debug Info',
      type: 'switch',
      defaultValue: false,
    },
  ];

  // 初始化表单
  const callInit = async () => {
    setIsInitializing(true);
    try {
      // 模拟API调用获取参数元数据
      await new Promise(resolve => setTimeout(resolve, 1000));

      setFormFields(mockFormFields);

      // 设置默认值
      const defaultValues = mockFormFields.reduce((acc, field) => {
        if (field.defaultValue !== undefined) {
          acc[field.name] = field.defaultValue;
        }
        return acc;
      }, {} as any);

      form.setFieldsValue(defaultValues);

      dispatchEvent({
        type: 'init',
        payload: { formFields: mockFormFields, graphData },
      });

      message.success('表单初始化成功');
    } catch (error) {
      console.error('初始化失败:', error);
      requestShowError('初始化失败，请重试');
    } finally {
      setIsInitializing(false);
    }
  };

  // 提交表单，启动新流程
  const callSubmit = async (formData: any) => {
    setIsLoading(true);
    try {
      // 构建输入文本（从表单数据中提取主要输入）
      const inputText = formData.customerFeedback || JSON.stringify(formData);

      // 使用新的API服务创建流式执行
      const cleanup = await graphDebugService.executeGraph(
        graphData.id,
        inputText,
        (nodeOutput: any) => {
          // 实时更新节点输出
          dispatchEvent({
            type: 'result',
            payload: {
              type: 'node_update',
              data: nodeOutput,
              timestamp: new Date().toISOString(),
            },
          });
        },
        (error) => {
          console.error('流式执行错误:', error);
          requestShowError('执行过程中出现错误');
          setIsLoading(false);
        },
        () => {
          // 执行完成
          dispatchEvent({
            type: 'result',
            payload: {
              type: 'execution_complete',
              data: { graphId: graphData.id, inputText },
              timestamp: new Date().toISOString(),
            },
          });
          
          message.success('图执行完成');
          setIsLoading(false);
        }
      );

      // 触发初始事件
      dispatchEvent({
        type: 'result',
        payload: {
          type: 'execution_start',
          data: { 
            graphId: graphData.id, 
            inputText, 
            formData,
            cleanup 
          },
          timestamp: new Date().toISOString(),
        },
      });

      onSubmit(formData);
      message.success('开始执行图工作流');
    } catch (error) {
      console.error('执行失败:', error);
      requestShowError('执行失败，请重试');
      setIsLoading(false);
    }
  };

  // 恢复中断流程
  const callResume = async (nodeId: string) => {
    setIsLoading(true);
    try {
      const formData = form.getFieldsValue();

      // 模拟恢复API调用
      await new Promise(resolve => setTimeout(resolve, 1500));

      dispatchEvent({
        type: 'result',
        payload: {
          type: 'resume',
          nodeId,
          data: formData,
          timestamp: new Date().toISOString(),
        },
      });

      message.success(`从节点 ${nodeId} 恢复执行成功`);
    } catch (error) {
      console.error('恢复执行失败:', error);
      requestShowError('恢复执行失败，请重试');
    } finally {
      setIsLoading(false);
    }
  };

  // 错误提示
  const requestShowError = (errorMessage: string) => {
    message.error(errorMessage);
    dispatchEvent({
      type: 'state-updated',
      payload: { error: errorMessage },
    });
  };

  // 初始化时获取表单字段
  useEffect(() => {
    if (isExpanded && formFields.length === 0) {
      callInit();
    }
  }, [isExpanded]);

  // 渲染动态表单字段
  const renderFormField = (field: FormField) => {
    switch (field.type) {
      case 'textarea':
        return (
          <TextArea
            rows={4}
            placeholder={field.placeholder}
          />
        );

      case 'number':
        return (
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            max={1}
            step={0.1}
          />
        );

      case 'select':
        return (
          <Select placeholder={`请选择${field.label}`}>
            {field.options?.map(option => (
              <Option key={option.value} value={option.value}>
                {option.label}
              </Option>
            ))}
          </Select>
        );

      case 'switch':
        return <Switch />;

      default:
        return (
          <Input placeholder={field.placeholder} />
        );
    }
  };

  // 处理表单提交
  const handleSubmit = () => {
    form.validateFields().then(values => {
      if (executionMode === 'new') {
        callSubmit(values);
      } else {
        // 恢复模式需要指定节点
        callResume('feedback_classifier');
      }
    });
  };

  return (
    <div className={`${styles['executor-panel']} ${!isExpanded ? styles.collapsed : ''}`}>
      {isExpanded && (
        <>
          <div className={styles['executor-header']}>
            <Text strong>输入参数</Text>
            <Space>
              <Select
                size="small"
                value={executionMode}
                onChange={setExecutionMode}
                style={{ width: 100 }}
              >
                <Option value="new">新流程</Option>
                <Option value="resume">恢复</Option>
              </Select>
              <Button
                size="small"
                icon={<CloseOutlined />}
                onClick={onToggle}
              />
            </Space>
          </div>

          <div className={styles['executor-content']}>
            {isInitializing ? (
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <Text>正在初始化表单...</Text>
              </div>
            ) : (
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSubmit}
              >
                {formFields.map(field => (
                  <Form.Item
                    key={field.name}
                    name={field.name}
                    label={field.label}
                    rules={field.required ? [{ required: true, message: `请输入${field.label}` }] : []}
                  >
                    {renderFormField(field)}
                  </Form.Item>
                ))}

                <Divider />

                <Space style={{ width: '100%', justifyContent: 'center' }}>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={callInit}
                    disabled={isInitializing}
                  >
                    重置
                  </Button>

                  <Button
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    loading={isLoading}
                    onClick={handleSubmit}
                  >
                    {executionMode === 'new' ? '执行' : '恢复执行'}
                  </Button>
                </Space>
              </Form>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default Executor;
