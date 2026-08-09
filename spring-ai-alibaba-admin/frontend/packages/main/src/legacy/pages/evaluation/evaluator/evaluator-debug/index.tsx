import React, { useState, useEffect, useCallback, useContext } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Card,
  Button,
  Row,
  Col,
  Typography,
  Form,
  Input,
  Space,
  Tag,
  Descriptions,
  Spin,
  Alert,
  Divider,
  message
} from 'antd';
import {
  ArrowLeftOutlined,
  PlayCircleOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { handleApiError, notifySuccess } from '../../../../utils/notification';
import API from '../../../../services';
import './index.css';
import { ModelsContext } from '../../../../context/models';

const { Title, Text } = Typography;

function EvaluatorDebug() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm();

  // 从路由状态获取评估器配置信息
  const debugConfig = location.state || {};

  // 状态管理
  const [loading, setLoading] = useState(false);
  const [evaluator, setEvaluator] = useState<any>(null);
  const { models, modelNameMap } = useContext(ModelsContext);
  const [evaluationResult, setEvaluationResult] = useState<any>(null);
  const [evaluationLoading, setEvaluationLoading] = useState(false);


  // 加载评估器详情（如果有ID）
  const loadEvaluatorDetail = useCallback(async () => {
    if (!debugConfig.evaluatorId) return;

    setLoading(true);
    try {
      const response = await API.getEvaluator({ id: debugConfig.evaluatorId });
      if (response.code === 200) {
        setEvaluator(response.data);
      }
    } catch (error) {
      handleApiError(error, 'Load evaluator details');
    } finally {
      setLoading(false);
    }
  }, [debugConfig.evaluatorId]);

  // 获取模型名称
  const getModelName = useCallback((modelId: string) => {
    const name = modelNameMap[Number(modelId)];
    return name || modelId || '-';
  }, [modelNameMap]);

  // 从配置中提取模型信息
  const getModelConfig = useCallback(() => {
    if (debugConfig.modelConfig) {
      return debugConfig.modelConfig;
    }
    return {
    };
  }, [debugConfig]);

  // 从评估器详情中获取模版变量
  const getTemplateVariables = useCallback(() => {
    if (debugConfig && debugConfig.variables) {
      try {
        return debugConfig.variables;
      } catch (error) {
        console.log('Error parsing evaluator variables:', error);
        return {};
      }
    }
    return {};
  }, [debugConfig]);

  // 处理清空表单
  const handleClear = () => {
    form.resetFields();
    setEvaluationResult(null);

    // 重置变量值为默认值
    const templateVariables = getTemplateVariables();
    console.log('Resetting form with templateVariables:', templateVariables);
    if (templateVariables && Object.keys(templateVariables).length > 0) {
      const initialValues: any = {};
      Object.entries(templateVariables).forEach(([key, value]) => {
        initialValues[key] = value || '';
      });
      form.setFieldsValue(initialValues);

    }

    message.success('Form cleared');
  };

  // 处理运行评估
  const handleRun = async () => {
    try {
      // 首先进行表单校验
      await form.validateFields();
      
      const modelConfig = getModelConfig();

      // 构建统一的variables参数，包含所有变量和测试数据


      const { systemPrompt, ...otherConfig } = (debugConfig?.modelConfig) || {};
      console.log(form.getFieldsValue(), 'asd...')
      // 构建请求参数
      const params: EvaluatorsAPI.DebugEvaluatorParams = {
        modelConfig: JSON.stringify({
          modelId: modelConfig.modelId,
          ...otherConfig
        }),
        prompt: debugConfig.systemPrompt,
        variables: JSON.stringify(form.getFieldsValue()) // 将所有参数统一放入variables中
      };

      setEvaluationLoading(true);
      setEvaluationResult(null);

      const response = await API.debugEvaluator(params);

      if (response.code === 200) {
        setEvaluationResult(response.data);
        notifySuccess({ message: 'Evaluation completed' });
      } else {
        throw new Error(response.message || 'Evaluation failed');
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('Please provide the required test data');
      } else {
        handleApiError(error, 'Run evaluation');
      }
    } finally {
      setEvaluationLoading(false);
    }
  };

  // 初始化
  useEffect(() => {
    loadEvaluatorDetail();
  }, []);

  // 初始化变量表单值
  useEffect(() => {
    if (evaluator) {
      const templateVariables = getTemplateVariables();
      console.log('Initializing form with templateVariables:', templateVariables);
      if (templateVariables && Object.keys(templateVariables).length > 0) {
        const initialValues: any = {};
        Object.entries(templateVariables).forEach(([key, value]) => {
          initialValues[key] = value || '';
        });
        console.log('Setting form initial values:', initialValues);
        form.setFieldsValue(initialValues);

        // 验证表单值是否正确设置
        setTimeout(() => {
          const currentValues = form.getFieldsValue();
          console.log('Form current values after initialization:', currentValues);
        }, 100);
      }
    }
  }, [evaluator, getTemplateVariables, form]);

  if (loading && !evaluator) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Spin size="large" />
      </div>
    );
  }

  // 返回上一页
  const goBackPageFun = () => {
    const targetPathname = debugConfig?.prePathname;
    if(debugConfig && targetPathname) {
      // 创建新的状态对象，将systemPrompt作为顶层属性传递
      navigate(targetPathname, { 
        state: {
          ...debugConfig,
          prePathname: location.pathname,
        }
      });
    } else {
      navigate(-1);
    }
  };

  const { modelId, ...otherConfig } = debugConfig.modelConfig;

  return (
    <div className="p-8 fade-in evaluator-debug-page">
      {/* 页面头部 */}
      <div className="mb-8">
        <div className='flex mb-2'>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={goBackPageFun}
            size="large"
          />
          <Title level={2} className='m-0'>Evaluator Debug</Title>
        </div>
        <Text type="secondary">Test and debug evaluator logic</Text>
      </div>

      <Row gutter={[24, 24]}>
        {/* 左侧：评估器配置信息 */}
        <Col xs={24} lg={12}>
          <Card title="Evaluator Configuration" style={{ height: 'fit-content' }}>
            {evaluator && (
              <Descriptions column={3} size="small">
                <Descriptions.Item label="Evaluator Name">
                  <Text strong>{evaluator.name}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Description">
                  <Text>{evaluator.description || '-'}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Current Version">
                  {evaluator.latestVersion ? (
                    <Tag color="blue">{evaluator.latestVersion}</Tag>
                  ) : (
                    <Tag color="default">No Versions</Tag>
                  )}
                </Descriptions.Item>
              </Descriptions>
            )}

            <Divider orientation="left">Model Configuration</Divider>

            <Descriptions column={3} size="small">
              <Descriptions.Item span={24} label="Model">
                <Tag color="geekblue">{getModelName(modelId)}</Tag>
              </Descriptions.Item>
              {
                Object.entries(otherConfig).map(([key, value]) => {
                  return (
                    <Descriptions.Item key={key} label={key}>
                      <Text>{value as string}</Text>
                    </Descriptions.Item>
                  )
                })
              }
            </Descriptions>

            <Divider orientation="left">Prompt</Divider>

            <div className='mb-4'>
              <Text type="secondary" className='text-sm mb-2 block'>
                System Prompt
              </Text>
              <div
                style={{
                  background: '#f5f5f5',
                  padding: 12,
                  borderRadius: 6,
                  maxHeight: 200,
                  overflow: 'auto',
                  fontFamily: 'monospace',
                  fontSize: '13px',
                  lineHeight: '1.5'
                }}
                className="prompt-display"
              >
                {debugConfig.systemPrompt || 'System prompt not configured'}
              </div>
            </div>

            {/* 显示变量及其值 */}
            {debugConfig.variables && Object.keys(debugConfig.variables).length > 0 && (
              <>
                <Divider orientation="left">Variable Configuration</Divider>
                <div className='mb-4'>
                  <Text type="secondary" className='text-sm mb-2 block'>
                    Detected Variables ({Object.keys(debugConfig.variables).length})
                  </Text>
                  <div className="p-3 bg-[#f9f9f9] border border-[#e8e8e8] rounded-md">
                    <Space direction="vertical" className='w-full' size="small">
                      {Object.entries(debugConfig.variables).map(([key]) => (
                        <div key={key} className='flex justify-between items-center'>
                          <Tag color="blue" className='m-0'>{key}</Tag>
                        </div>
                      ))}
                    </Space>
                  </div>
                </div>
              </>
            )}
          </Card>
        </Col>

        {/* 右侧：测试数据区域 */}
        <Col xs={24} lg={12}>
          <Card
            title="Test Data"
            extra={
              <Space>
                <Button
                  icon={<ClearOutlined />}
                  onClick={handleClear}
                  disabled={evaluationLoading}
                >
                  Clear
                </Button>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleRun}
                  loading={evaluationLoading}
                >
                  Run
                </Button>
              </Space>
            }
          >
            <Form form={form} layout="vertical">
              {/* 评估器模版变量输入框 */}
              {(() => {
                const templateVariables = getTemplateVariables();
                return templateVariables && Object.keys(templateVariables).length > 0 ? (
                  <>
                    <div className="template-variables-section">
                      <div className="template-variables-title">
                        Template Variable Configuration
                      </div>
                      <div className="template-variables-description">
                        Set values for variables in the evaluator template
                      </div>

                      {Object.entries(templateVariables).map(([variableName, defaultValue]) => (
                        <Form.Item
                          key={variableName}
                          className="variable-input-item"
                          label={
                            <div>
                              <Text strong>{variableName}</Text>
                              <Tag color="blue" className="ml-2">Template Variable</Tag>
                            </div>
                          }
                          name={variableName}
                          initialValue={defaultValue || ''}
                          rules={[
                            {
                              required: true,
                              whitespace: true,
                              message: `Please enter a value for ${variableName}`,
                            }
                          ]}
                        >
                          <Input
                            placeholder={`Enter a value for ${variableName}`}
                            showCount
                            maxLength={500}
                          />
                        </Form.Item>
                      ))}
                    </div>

                    <Divider className="variables-divider" />
                  </>
                ) : null;
              })()}

            </Form>

            {/* 评估结果 */}
            {evaluationResult && (
              <>
                <Divider orientation="left">Evaluation Result</Divider>
                <Alert
                  message="Evaluation Completed"
                  description={
                    <div>
                      <Row gutter={[16, 8]}>
                        <Col span={12}>
                          <Text strong>Evaluation Score:</Text>
                          <Tag
                            color={evaluationResult.score >= 0.8 ? 'success' : evaluationResult.score >= 0.6 ? 'warning' : 'error'}
                            style={{ marginLeft: 8 }}
                          >
                            {evaluationResult.score}
                          </Tag>
                        </Col>
                      </Row>
                      <div className='mt-3'>
                        <Text strong>Evaluation Reason:</Text>
                        <div
                          className='mt-2 p-3 bg-[#f9f9f9] border border-[#e8e8e8] rounded-md'
                        >
                          <Text>{evaluationResult.reason || 'No detailed reason provided'}</Text>
                        </div>
                      </div>
                    </div>
                  }
                  type="success"
                  showIcon
                  className='mt-4'
                />
              </>
            )}

            {/* 提示信息 */}
            {!debugConfig.evaluatorId && (
              <Alert
                message="Configuration Notice"
                description="Debugging is using the default configuration. Open this page from evaluator details to use the full configuration."
                type="info"
                showIcon
                className='mt-4'
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default EvaluatorDebug;