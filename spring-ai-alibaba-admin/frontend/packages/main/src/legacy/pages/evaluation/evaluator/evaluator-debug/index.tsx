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
      handleApiError(error, '加载评估器详情');
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

    message.success('表单已清空');
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
        notifySuccess({ message: '评估完成' });
      } else {
        throw new Error(response.message || '评估失败');
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必要的测试数据');
      } else {
        handleApiError(error, '运行评估');
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
          <Title level={2} className='m-0'>评估器调试</Title>
        </div>
        <Text type="secondary">测试和调试评估器的评估逻辑</Text>
      </div>

      <Row gutter={[24, 24]}>
        {/* 左侧：评估器配置信息 */}
        <Col xs={24} lg={12}>
          <Card title="评估器配置信息" style={{ height: 'fit-content' }}>
            {evaluator && (
              <Descriptions column={3} size="small">
                <Descriptions.Item label="评估器名称">
                  <Text strong>{evaluator.name}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="描述">
                  <Text>{evaluator.description || '-'}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="当前版本">
                  {evaluator.latestVersion ? (
                    <Tag color="blue">{evaluator.latestVersion}</Tag>
                  ) : (
                    <Tag color="default">暂无版本</Tag>
                  )}
                </Descriptions.Item>
              </Descriptions>
            )}

            <Divider orientation="left">模型配置</Divider>

            <Descriptions column={3} size="small">
              <Descriptions.Item span={24} label="模型">
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
                系统提示词
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
                {debugConfig.systemPrompt || '未配置系统提示词'}
              </div>
            </div>

            {/* 显示变量及其值 */}
            {debugConfig.variables && Object.keys(debugConfig.variables).length > 0 && (
              <>
                <Divider orientation="left">变量配置</Divider>
                <div className='mb-4'>
                  <Text type="secondary" className='text-sm mb-2 block'>
                    检测到的变量 ({Object.keys(debugConfig.variables).length} 个)
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
            title="测试数据"
            extra={
              <Space>
                <Button
                  icon={<ClearOutlined />}
                  onClick={handleClear}
                  disabled={evaluationLoading}
                >
                  清空
                </Button>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleRun}
                  loading={evaluationLoading}
                >
                  运行
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
                        模版变量配置
                      </div>
                      <div className="template-variables-description">
                        请为评估器模版中的变量设置值
                      </div>

                      {Object.entries(templateVariables).map(([variableName, defaultValue]) => (
                        <Form.Item
                          key={variableName}
                          className="variable-input-item"
                          label={
                            <div>
                              <Text strong>{variableName}</Text>
                              <Tag color="blue" className="ml-2">模版变量</Tag>
                            </div>
                          }
                          name={variableName}
                          initialValue={defaultValue || ''}
                          rules={[
                            {
                              required: true,
                              whitespace: true,
                              message: `请输入${variableName}的值`,
                            }
                          ]}
                        >
                          <Input
                            placeholder={`请输入 ${variableName} 的值`}
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
                <Divider orientation="left">评估结果</Divider>
                <Alert
                  message="评估完成"
                  description={
                    <div>
                      <Row gutter={[16, 8]}>
                        <Col span={12}>
                          <Text strong>评估得分：</Text>
                          <Tag
                            color={evaluationResult.score >= 0.8 ? 'success' : evaluationResult.score >= 0.6 ? 'warning' : 'error'}
                            style={{ marginLeft: 8 }}
                          >
                            {evaluationResult.score}
                          </Tag>
                        </Col>
                      </Row>
                      <div className='mt-3'>
                        <Text strong>评估理由：</Text>
                        <div
                          className='mt-2 p-3 bg-[#f9f9f9] border border-[#e8e8e8] rounded-md'
                        >
                          <Text>{evaluationResult.reason || '无详细理由'}</Text>
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
                message="配置信息提示"
                description="当前使用默认配置进行调试，建议从评估器详情页进入以使用完整配置信息。"
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