import React, { useState, useEffect, useRef } from 'react';
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
  StopOutlined,
  BugOutlined,
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
  const [streamType, setStreamType] = useState<'enhanced' | 'basic' | 'snapshots'>('enhanced');
  const [cleanupFn, setCleanupFn] = useState<(() => void) | null>(null);
  const [executionStatus, setExecutionStatus] = useState<string>('');
  
  // 使用ref来立即标记执行状态，避免状态更新延迟导致的重复执行
  const isExecutingRef = useRef(false);

  // 动态表单字段定义
  const staticFormFields: FormField[] = [
    {
      name: 'inputText',
      label: 'Input Text',
      type: 'textarea',
      required: true,
      placeholder: '请输入要处理的文本内容...',
    },
    {
      name: 'streamType',
      label: 'Stream Type',
      type: 'select',
      required: true,
      defaultValue: 'enhanced',
      options: [
        { label: '增强节点输出流 (Enhanced)', value: 'enhanced' },
        { label: '基础节点输出流 (Basic)', value: 'basic' },
        { label: '节点状态快照流 (Snapshots)', value: 'snapshots' },
      ],
    },
    {
      name: 'debugMode',
      label: 'Debug Mode',
      type: 'switch',
      defaultValue: false,
    },
  ];

  // 初始化表单
  const callInit = async () => {
    setIsInitializing(true);
    try {
      // 设置表单字段
      setFormFields(staticFormFields);

      // 设置默认值
      const defaultValues = staticFormFields.reduce((acc, field) => {
        if (field.defaultValue !== undefined) {
          acc[field.name] = field.defaultValue;
        }
        return acc;
      }, {} as any);

      form.setFieldsValue(defaultValues);

      dispatchEvent({
        type: 'init',
        payload: { formFields: staticFormFields, graphData },
      });

      message.success('表单初始化成功');
    } catch (error) {
      requestShowError('初始化失败，请重试');
    } finally {
      setIsInitializing(false);
    }
  };

  // 提交表单，启动新流程
  const callSubmit = async (formData: any) => {
    // 防止重复执行：如果已经在执行中，直接返回
    if (isExecutingRef.current) {
      message.warning('任务正在执行中，请勿重复点击');
      return;
    }
    isExecutingRef.current = true;
    
    if (cleanupFn) {
      cleanupFn();
      setCleanupFn(null);
    }
    
    setIsLoading(true);
    setExecutionStatus('正在初始化...');
    
    try {
      const inputText = formData.inputText;
      const selectedStreamType = formData.streamType || 'enhanced';
      
      
      let cleanup: () => void;

      // 根据选择的流式类型调用不同的API
      switch (selectedStreamType) {
        case 'basic':
          setExecutionStatus('连接基础节点输出流...');
          cleanup = await graphDebugService.executeGraphBasic(
            graphData.id,
            inputText,
            (nodeOutput: any) => {
              setExecutionStatus(`执行节点: ${nodeOutput.node}`);
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'node_update',
                  streamType: 'basic',
                  data: nodeOutput,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              requestShowError('基础流执行过程中出现错误');
              setExecutionStatus('执行失败');
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false; 
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'basic',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 基础流执行完成');
              setExecutionStatus('执行完成');
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false; 
            }
          );
          break;

        case 'snapshots':
          setExecutionStatus('连接节点状态快照流...');
          cleanup = await graphDebugService.executeGraphSnapshots(
            graphData.id,
            inputText,
            (snapshot: any) => {
              const keys = Object.keys(snapshot).slice(0, 3).join(', ');
              setExecutionStatus(`接收状态快照: ${keys}...`);
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'state_update',
                  streamType: 'snapshots',
                  data: snapshot,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              requestShowError('快照流执行过程中出现错误');
              setExecutionStatus('执行失败');
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false;
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'snapshots',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 快照流执行完成');
              setExecutionStatus('执行完成');
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false; 
            }
          );
          break;

        case 'enhanced':
        default:
          setExecutionStatus('连接增强节点输出流...');
          cleanup = await graphDebugService.executeGraphEnhanced(
            graphData.id,
            inputText,
            (nodeOutput: any) => {
              const status = nodeOutput.execution_status || 'EXECUTING';
              const order = nodeOutput.execution_order ? `[${nodeOutput.execution_order}]` : '';
              setExecutionStatus(`${order} ${nodeOutput.node_id}: ${status}`);
              
              // 确保每个节点更新都能被正确处理              
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'node_update',
                  streamType: 'enhanced',
                  data: nodeOutput,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              const errorMsg = error instanceof Error ? error.message : '增强流执行过程中出现错误';
              requestShowError(errorMsg);
              setExecutionStatus('执行失败: ' + errorMsg);
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false;
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'enhanced',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 增强流执行完成');
              setExecutionStatus('执行完成');
              setIsLoading(false);
              setCleanupFn(null);
              isExecutingRef.current = false; 
            }
          );
          break;
      }

      // 保存清理函数
      setCleanupFn(() => cleanup);

      // 触发初始事件
      dispatchEvent({
        type: 'result',
        payload: {
          type: 'execution_start',
          streamType: selectedStreamType,
          data: { 
            graphId: graphData.id, 
            graphName: graphData.name,
            inputText, 
            formData,
          },
          timestamp: new Date().toISOString(),
        },
      });

      onSubmit(formData);
      setExecutionStatus(`执行中 (${selectedStreamType})...`);
      message.success(`🚀 开始执行图工作流 (${selectedStreamType})`);
    } catch (error) {
      requestShowError(`执行失败: ${error instanceof Error ? error.message : '未知错误'}`);
      setExecutionStatus('执行失败');
      setIsLoading(false);
      setCleanupFn(null);
      isExecutingRef.current = false; 
    }
  };

  // 恢复中断流程
  const callResume = async (nodeId: string) => {
    setIsLoading(true);
    try {
      const formData = form.getFieldsValue();

      // 模拟恢复API调用
      await new Promise<void>(resolve => {
        setTimeout(() => resolve(), 1500);
      });

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
    if (isExpanded) {
      callInit();
    }
  }, [isExpanded]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      if (cleanupFn) {
        cleanupFn();
        isExecutingRef.current = false; // 组件卸载时重置执行标记
      }
    };
  }, [cleanupFn]);

  // 停止执行
  const handleStop = () => {
    if (cleanupFn) {
      cleanupFn();
      setCleanupFn(null);
      setIsLoading(false);
      setExecutionStatus('已停止');
      isExecutingRef.current = false; // 重置执行标记
      message.warning('执行已停止');
    }
  };

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
    if (isExecutingRef.current) {
      message.warning('任务正在执行中，请勿重复点击');
      return;
    }

    form.validateFields().then(values => {
      if (executionMode === 'new') {
        callSubmit(values);
      } else {
        // 恢复模式需要指定节点
        callResume('feedback_classifier');
      }
    }).catch(errorInfo => {
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

                {/* 执行状态显示 */}
                {executionStatus && (
                  <div style={{ 
                    padding: '8px 12px', 
                    marginBottom: '12px', 
                    background: isLoading ? '#e6f7ff' : '#f6ffed',
                    border: `1px solid ${isLoading ? '#91d5ff' : '#b7eb8f'}`,
                    borderRadius: '4px',
                  }}>
                    <Text type={isLoading ? 'secondary' : 'success'} style={{ fontSize: '12px' }}>
                      {executionStatus}
                    </Text>
                  </div>
                )}

                <Space style={{ width: '100%', justifyContent: 'center' }}>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={callInit}
                    disabled={isInitializing || isLoading}
                  >
                    重置
                  </Button>


                  {isLoading ? (
                    <Button
                      danger
                      icon={<StopOutlined />}
                      onClick={handleStop}
                    >
                      停止
                    </Button>
                  ) : (
                    <Button
                      type="primary"
                      icon={<PlayCircleOutlined />}
                      onClick={handleSubmit}
                    >
                      {executionMode === 'new' ? '执行' : '恢复执行'}
                    </Button>
                  )}
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
