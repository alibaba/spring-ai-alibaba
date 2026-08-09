import React, { useState, useEffect, useCallback, useMemo, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import {
  Card,
  Button,
  Descriptions,
  Tag,
  Tabs,
  Form,
  Input,
  Select,
  InputNumber,
  Slider,
  Space,
  Table,
  Spin,
  Alert,
  message,
  Modal,
  Tooltip,
  Row,
  Col,
  Typography,
  Divider
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  BugOutlined,
  RocketOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { handleApiError, notifySuccess, notifyError } from '../../../../utils/notification';
import API from '../../../../services';
import { formatDateTime } from '../../../../utils/formatDateTime';
import './index.css';
import usePagination from '../../../../hooks/usePagination';
import $i18n from '@/i18n';
import { ModelsContext } from '../../../../context/models';

const { TextArea } = Input;
const { Option } = Select;
const { Title, Text } = Typography;

function EvaluatorDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const stateFromDebug = location.state || {};
  const [form] = Form.useForm();
  const [configForm] = Form.useForm();
  const [publishForm] = Form.useForm();
  const formValues = Form.useWatch([], configForm);

  // 基础状态
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [evaluator, setEvaluator] = useState<EvaluatorsAPI.GetEvaluatorResult | null>(null);
  const { models, modelNameMap } = useContext(ModelsContext);

  // 编辑状态
  const [isEditing, setIsEditing] = useState(false);
  const [editLoading, setEditLoading] = useState(false);

  // Tab 相关状态
  const [activeTab, setActiveTab] = useState('config');
  const [versions, setVersions] = useState<EvaluatorsAPI.GetEvaluatorVersionsResult["pageItems"]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);

  // 版本发布相关状态
  const [showPublishModal, setShowPublishModal] = useState(false);
  const [publishLoading, setPublishLoading] = useState(false);
  const [variableValues, setVariableValues] = useState<Record<string, string>>({});
  const [modelConf, setModelConf] = useState<Record<string, any>>({});
  const [defaultUsedModelId, setDefaultUsedModelId] = useState(-1);
  const [defaultUsedModelConfig, setDefaultUsedModelConfig] = useState<Record<string, any>>({});

  // 模板导入相关状态
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [templates, setTemplates] = useState<EvaluatorsAPI.GetEvaluatorTemplatesResult["pageItems"]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [templateDetailLoading, setTemplateDetailLoading] = useState(false);
  const [selectedTemplateDetail, setSelectedTemplateDetail] = useState<EvaluatorsAPI.GetEvaluatorTemplateResult | null>(null);

  const [experiments, setExperiments] = useState<EvaluatorsAPI.GetEvaluatorExperimentsResult["pageItems"]>([]);

  const [experimentsLoading, setExperimentsLoading] = useState(false);
  const [experimentSearch, setExperimentSearch] = useState('');

  const {
    pagination: experimentPagination,
    onPaginationChange: onExperimentPaginationChange,
    onShowSizeChange: onExperimentShowSizeChange,
    setPagination: setExperimentPagination
  } = usePagination();


  const {
    pagination: versionPagination,
    setPagination: setVersionPagination,
    onPaginationChange: onVersionPaginationChange,
    onShowSizeChange: onVersionShowSizeChange
  } = usePagination();

  const handleModelChange = (newModelId: number) => {
    const selectedModel = models.find(m => m.id === newModelId);
    if (!selectedModel) return;

    const defaultParams = selectedModel.id === defaultUsedModelId ? defaultUsedModelConfig : selectedModel.defaultParameters || {};
    const currentValues = configForm.getFieldsValue();

    const paramKeys = Object.keys(currentValues).filter(
      key => key !== 'modelId' && key !== 'systemPrompt'
    );

    const fieldsToClear: Record<string, any> = {};
    paramKeys.forEach(key => {
      fieldsToClear[key] = undefined;
    });

    setModelConf(defaultParams);
    configForm.setFieldsValue({ ...fieldsToClear, ...defaultParams });
  };


  // 加载评估器详情
  const loadEvaluatorDetail = useCallback(async () => {
    if (!id) return;

    setLoading(true);
    setError(null);

    try {
      // 获取评估器基础信息
      const evaluatorResponse = await API.getEvaluator({ id: parseInt(id) });

      if (evaluatorResponse.code !== 200) {
        throw new Error(evaluatorResponse.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.loadFailedError', dm: '获取评估器详情失败' }));
      }

      const evaluatorData = evaluatorResponse.data;
      setEvaluator(evaluatorData);
      try {
        const isFromEvaluationDebug = stateFromDebug?.prePathname === '/evaluation-debug';
        const modelConfig = isFromEvaluationDebug ? (stateFromDebug?.modelConfig || {}) : JSON.parse(evaluatorData.modelConfig || "{}");
        const { modelId, ...conf } = modelConfig;
        const defaultModelParams = !isFromEvaluationDebug && models[0]?.defaultParameters || {};
        const promptVal = isFromEvaluationDebug ? stateFromDebug.systemPrompt : evaluatorData.prompt;
        setModelConf({ ...defaultModelParams, ...conf });
        setDefaultUsedModelConfig({ ...defaultModelParams, ...conf });
        setDefaultUsedModelId(modelId);
        configForm.setFieldsValue({ modelId, systemPrompt: promptVal, ...{ ...defaultModelParams, ...conf } });

        navigate(location.pathname, {replace: true, state: {}});
      } catch (error) {

      }

      // 设置表单初始值
      form.setFieldsValue({
        name: evaluatorData.name,
        description: evaluatorData.description
      });

      // 配置表单将在 useEffect 中设置，以便使用正确的模型配置

    } catch (err: any) {
      console.error('Failed to load evaluator details:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.loadContext', dm: '加载评估器详情' }));
      setError(err.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.loadFailedRetry', dm: '加载失败，请稍后重试' }));
    } finally {
      setLoading(false);
    }
  }, [id, form]);

  // 加载版本列表
  const loadVersions = useCallback(async () => {
    if (!id) return;

    setVersionsLoading(true);

    try {
      const response = await API.getEvaluatorVersions({
        evaluatorId: parseInt(id),
        pageNumber: versionPagination.current,
        pageSize: versionPagination.pageSize
      });

      if (response.code === 200) {
        const responseData = response.data;
        setVersions(responseData.pageItems || []);
        setVersionPagination(prev => ({
          ...prev,
          total: responseData.totalCount || 0
        }));
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionListFailed', dm: '获取版本列表失败' }));
      }
    } catch (err: any) {
      console.error('Failed to load version list:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionListContext', dm: '加载版本列表' }));
    } finally {
      setVersionsLoading(false);
    }
  }, [id, versionPagination.current, versionPagination.pageSize]);

  // 处理基础信息编辑保存
  const handleSaveBasicInfo = async () => {
    if (!evaluator) return;

    try {
      const values = await form.validateFields();
      setEditLoading(true);

      const response = await API.updateEvaluator({
        id: evaluator.id,
        name: values.name,
        description: values.description
      });

      if (response.code === 200) {
        notifySuccess({ message: $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.updateSuccess', dm: '评估器信息更新成功' }) });
        setEvaluator(prev => ({
          ...prev!,
          ...values,
          updateTime: new Date().toISOString()
        }));
        setIsEditing(false);
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.updateFailed', dm: '更新失败' }));
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error($i18n.get({ id: 'legacy.evaluation.common.checkForm', dm: '请检查表单填写是否正确' }));
      } else {
        handleApiError(error, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.updateContext', dm: '更新评估器信息' }));
      }
    } finally {
      setEditLoading(false);
    }
  };

  // 加载评估器模板列表
  const loadEvaluatorTemplates = useCallback(async () => {
    setTemplatesLoading(true);
    try {
      const response = await API.getEvaluatorTemplates();
      if (response.code === 200) {
        setTemplates(response.data.pageItems || []);
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateListFailed', dm: '获取模板列表失败' }));
      }
    } catch (error: any) {
      console.error('Failed to load template list:', error);
      handleApiError(error, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateListContext', dm: '加载模板列表' }));
    } finally {
      setTemplatesLoading(false);
    }
  }, []);

  // 加载模板详情
  const loadTemplateDetail = useCallback(async (templateId: number) => {
    setTemplateDetailLoading(true);
    try {
      const response = await API.getEvaluatorTemplate({ templateId });
      if (response.code === 200) {
        setSelectedTemplateDetail(response.data);
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateDetailFailed', dm: '获取模板详情失败' }));
      }
    } catch (error: any) {
      console.error('Failed to load template details:', error);
      handleApiError(error, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateDetailContext', dm: '加载模板详情' }));
    } finally {
      setTemplateDetailLoading(false);
    }
  }, []);

  // 处理模板选择
  const handleTemplateSelect = (templateId: number) => {
    setSelectedTemplateId(templateId);
    loadTemplateDetail(templateId);
  };

  // 处理模板导入
  const handleTemplateImport = () => {
    if (!selectedTemplateDetail) return;

    // 设置 system prompt
    configForm.setFieldsValue({
      systemPrompt: selectedTemplateDetail.template
    });

    // 解析并设置模型配置
    if (selectedTemplateDetail.modelConfig) {
      try {
        const modelConfig = JSON.parse(selectedTemplateDetail.modelConfig);
        const { modelId, ...otherConfig } = modelConfig;

        configForm.setFieldsValue({
          modelId: Number(modelId),
          ...otherConfig
        });

        setModelConf(otherConfig);
      } catch (error) {
        console.warn('Failed to parse template model configuration:', error);
      }
    }

    // 提取变量并设置变量值
    const variables = extractVariablesFromPrompt(selectedTemplateDetail.template || '');
    const newVariableValues: Record<string, string> = {};
    variables.forEach(varName => {
      newVariableValues[varName] = '';
    });
    setVariableValues(newVariableValues);

    // 关闭弹窗并重置状态
    setShowTemplateModal(false);
    setSelectedTemplateId(null);
    setSelectedTemplateDetail(null);

    // 显示成功消息
    message.success($i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateImportSuccess', dm: '模板导入成功' }));
  };

  // 打开模板导入弹窗
  const handleOpenTemplateModal = () => {
    setShowTemplateModal(true);
    loadEvaluatorTemplates();
  };

  // 处理发布跳转
  const handleDebug = () => {
    const configValues = configForm.getFieldsValue();
    const { systemPrompt, ...otherValues } = configValues;

    console.log(configValues, 'asd...2')
    // 提取 prompt 中的变量并生成变量对象
    const promptContent = systemPrompt;
    const variablesWithValues = generateVariablesWithValues(promptContent, variableValues);

    // 构建调试页面所需的配置参数
    const debugConfig = {
      evaluatorId: evaluator?.id,
      modelConfig: {
        ...otherValues,
      },
      variables: variablesWithValues, // 传递变量和它们的值
      systemPrompt: systemPrompt,
      prePathname: location.pathname,
    };

    // 跳转到调试页面，携带修改后的配置
    navigate('/evaluation-debug', { state: debugConfig });
  };

  // 计算下一个版本号
  const getNextVersion = useCallback(() => {
    if (!versions || versions.length === 0) {
      return '0.0.1';
    }

    // 解析版本号并找到最大版本
    const versionNumbers = versions
      .map(v => {
        const match = v.version.match(/^(\d+)\.(\d+)\.(\d+)$/);
        if (match) {
          return {
            major: parseInt(match[1]),
            minor: parseInt(match[2]),
            patch: parseInt(match[3]),
            original: v.version
          };
        }
        return null;
      })
      .filter((item): item is NonNullable<typeof item> => item !== null)
      .sort((a, b) => {
        if (a.major !== b.major) return b.major - a.major;
        if (a.minor !== b.minor) return b.minor - a.minor;
        return b.patch - a.patch;
      });

    if (versionNumbers.length === 0) {
      return '0.0.1';
    }

    const latest = versionNumbers[0];
    return `${latest.major}.${latest.minor}.${latest.patch + 1}`;
  }, [versions]);

  // 处理发布新版本
  const handlePublishVersion = () => {
    const nextVersion = getNextVersion();
    publishForm.setFieldsValue({
      version: nextVersion,
      description: ''
    });
    setShowPublishModal(true);
  };

  // 提取 prompt 中的变量
  const extractVariablesFromPrompt = useCallback((prompt: string): string[] => {
    if (!prompt) return [];

    // 匹配双花括号中的变量，如 {{variable_name}}
    const variableMatches = prompt.match(/\{\{\s*([^}]+)\s*\}\}/g);
    if (!variableMatches) return [];

    const variableNames: string[] = [];
    variableMatches.forEach(match => {
      // 提取变量名，去掉花括号和空格
      const variableName = match.replace(/\{\{\s*|\s*\}\}/g, '').trim();
      if (variableName && !variableNames.includes(variableName)) {
        variableNames.push(variableName);
      }
    });

    return variableNames;
  }, []);

  // 生成带有用户值的变量对象
  const generateVariablesWithValues = useCallback((prompt: string, userValues: Record<string, string>) => {
    const variableNames = extractVariablesFromPrompt(prompt);
    const variables: Record<string, string> = {};

    variableNames.forEach(name => {
      variables[name] = userValues[name] || '';
    });

    return variables;
  }, [extractVariablesFromPrompt]);

  // 处理发布版本确认
  const handlePublishConfirm = async () => {
    try {
      const values = await publishForm.validateFields();
      const configValues = configForm.getFieldsValue();

      // 检查版本号是否已存在
      const existingVersion = versions.find(v => v.version === values.version);
      if (existingVersion) {
        message.error($i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionExists', dm: '版本号 {version} 已存在，请使用其他版本号' }, { version: values.version }));
        return;
      }

      setPublishLoading(true);

      // 提取 prompt 中的变量并获取用户输入的值

      const { systemPrompt, ...otherModelConfig } = configValues;
      const variablesWithValues = generateVariablesWithValues(systemPrompt, variableValues);

      // 调用创建版本的API
      const response = await API.createEvaluatorVersion({
        evaluatorId: evaluator!.id.toString(),
        version: values.version,
        description: values.description || '',
        modelConfig: JSON.stringify(otherModelConfig),
        prompt: systemPrompt,
        variables: JSON.stringify(variablesWithValues)
      });

      if (response.code === 200) {
        message.success($i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionPublished', dm: '版本 {version} 发布成功' }, { version: values.version }));
        setShowPublishModal(false);
        loadVersions(); // 重新加载版本列表
        loadEvaluatorDetail(); // 重新加载评估器详情
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.publishFailed', dm: '发布失败' }));
      }
    } catch (error: any) {
      if (error.errorFields) {
        // 表单验证错误，不需要额外处理
        return;
      }
      handleApiError(error, $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.publishContext', dm: '发布新版本' }));
    } finally {
      setPublishLoading(false);
    }
  };


  // Tab 切换处理
  const handleTabChange = (key: string) => {
    setActiveTab(key);
    if (key === 'versions' && versions.length === 0) {
      loadVersions();
    }
  };

  // 初始化加载
  useEffect(() => {
    loadEvaluatorDetail();
    // 同时加载版本数据，用于获取当前模型信息
    loadVersions();
  }, []);


  // 当分页变化时重新加载版本列表
  useEffect(() => {
    if (activeTab === 'versions') {
      loadVersions();
    }
  }, [loadVersions, activeTab]);

  // 从 modelConfig 中提取 modelId 和 modelName
  const extractModelInfoFromConfig = useCallback((modelConfig: string) => {
    try {
      const config = JSON.parse(modelConfig);
      const name = modelNameMap[config.modelId];
      const modelId = config.modelId || '';
      return {
        modelId,
        modelName: name || modelId || '-'
      };
    } catch {
      return {
        modelId: '',
        modelName: '-'
      };
    }
  }, [models]);

  // 获取当前模型信息（从最新版本的 modelConfig 中提取）
  const getCurrentModelInfo = useCallback(() => {
    if (!versions || versions.length === 0) {
      // 如果没有版本数据，尝试从评估器的 modelConfig 中获取
      if (evaluator?.modelConfig) {
        return extractModelInfoFromConfig(evaluator.modelConfig);
      }
      return { modelId: '', modelName: '-' };
    }

    // 按创建时间排序，获取最新版本
    const latestVersion = versions
      .slice()
      .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())[0];

    if (latestVersion?.modelConfig) {
      return extractModelInfoFromConfig(latestVersion.modelConfig);
    }

    return { modelId: '', modelName: '-' };
  }, [versions, evaluator, extractModelInfoFromConfig]);

  // 当版本列表和模型列表加载完成后，更新配置表单
  useEffect(() => {
    // 确保 models 和 evaluator 都已加载，并且不在加载状态
    if (models.length > 0 && evaluator && !loading) {
      // 获取最新版本的配置信息或使用默认值
      let modelConfig = null;
      let variablesData = null;

      // 优先从最新版本中获取配置
      if (versions && versions.length > 0) {
        const latestVersion = versions
          .slice()
          .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())[0];

        if (latestVersion?.modelConfig) {
          try {
            modelConfig = JSON.parse(latestVersion.modelConfig);
          } catch (error) {
            console.warn('Failed to parse modelConfig from latest version:', error);
          }
        }

        if (latestVersion?.variables) {
          try {
            variablesData = JSON.parse(latestVersion.variables);
            console.log('Found variables in latest version:', variablesData);
          } catch (error) {
            console.warn('Failed to parse variables from latest version:', error);
          }
        }
      }

      // 如果没有版本数据，尝试从评估器本身的 modelConfig 和 variables 中获取
      if (!modelConfig && evaluator.modelConfig) {
        try {
          modelConfig = JSON.parse(evaluator.modelConfig);
        } catch (error) {
          console.warn('Failed to parse modelConfig from evaluator:', error);
        }
      }

      if (!variablesData && evaluator.variables) {
        try {
          variablesData = JSON.parse(evaluator.variables);
          console.log('Found variables in evaluator:', variablesData);
        } catch (error) {
          console.warn('Failed to parse variables from evaluator:', error);
        }
      }
      // 设置变量值 - 改进变量值设置逻辑
      if (variablesData && typeof variablesData === 'object') {
        console.log('Setting variable values from API:', variablesData);
        // 确保变量值对象不为空且包含有效数据
        const validVariables = Object.keys(variablesData).length > 0 ? variablesData : {};
        setVariableValues(validVariables);
      } else {
        // 如果没有 API 变量数据，尝试从 systemPrompt 中提取变量并初始化空值
        const promptContent = formValues.systemPrompt;
        if (promptContent) {
          const detectedVariables = extractVariablesFromPrompt(promptContent);
          if (detectedVariables.length > 0) {
            const emptyVariables: Record<string, string> = {};
            detectedVariables.forEach(varName => {
              emptyVariables[varName] = '';
            });
            console.log('Initializing empty variable values from prompt:', emptyVariables);
            setVariableValues(emptyVariables);
          }
        }
      }
    }
  }, [models, versions, evaluator, loading, configForm, getCurrentModelInfo, extractVariablesFromPrompt]);

  // 检查当前配置是否可以发布版本
  const canPublishVersion = useMemo(() => {
    const currentModelId = configForm.getFieldValue('modelId')
    const currentPrompt = configForm.getFieldValue('systemPrompt') || '';
    const variables = extractVariablesFromPrompt(currentPrompt);
    return variables.length > 0 && currentModelId !== undefined;
  }, [configForm, extractVariablesFromPrompt, formValues]);

  const modelConfigList = useMemo(() => {
    return Object.entries(modelConf).map(([key, value]) => {
      return { key, value }
    })
  }, [modelConf]);


  // 版本记录表格列配置
  const versionColumns = [
    {
      title: $i18n.get({ id: 'legacy.evaluation.common.version', dm: '版本号' }),
      dataIndex: 'version',
      key: 'version',
      render: (text: string) => <Tag color="blue">{text}</Tag>
    },
    {
      title: $i18n.get({ id: 'legacy.evaluation.common.description', dm: '描述' }),
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => text || '-'
    },
    {
      title: $i18n.get({ id: 'legacy.evaluation.common.judgeModel', dm: '裁判模型' }),
      dataIndex: 'modelConfig',
      key: 'modelConfig',
      render: (modelConfig: string) => {
        const { modelName } = extractModelInfoFromConfig(modelConfig);
        return <Tag color="geekblue">{modelName}</Tag>;
      }
    },
    {
      title: $i18n.get({ id: 'legacy.evaluation.common.createdAt', dm: '创建时间' }),
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text: string) => formatDateTime(text)
    }
  ];


  const handleExperimentSearch = (value: string) => {
    setExperimentPagination(prev => ({
      ...prev,
      current: 1,
    }));
    setExperimentSearch(value);
  }

  const loadExperiments = (search = experimentSearch, page = { pageNumber: experimentPagination.current, pageSize: experimentPagination.pageSize }) => {
    setExperimentsLoading(true);
    if (!evaluator) return;
    API.getEvaluatorExperiments({
      pageNumber: page.pageNumber,
      pageSize: page.pageSize,
      evaluatorId: evaluator!.id,
    }).then(({ data }) => {
      const dataPageItems = data.pageItems || [];
      dataPageItems.map((item: any) => {
        const itemEvaluatorConfig = JSON.parse(item.evaluatorConfig || '[]');
        const findItemEvaluatorConfig = itemEvaluatorConfig.find((c: any) => 
          c.evaluatorId === evaluator!.id
        )
        item.version = findItemEvaluatorConfig?.evaluatorVersionName || '';
        return item;
      })
      setExperiments(dataPageItems);
      setExperimentPagination({
        ...experimentPagination,
        current: data.pageNumber,
        pageSize: data.pageSize,
        total: data.totalCount,
      });
    }).finally(() => {
      setExperimentsLoading(false);
    })
  }

  useEffect(() => {
    loadExperiments();
  }, [evaluator?.id, experimentPagination.current, experimentPagination.pageSize])

  if (loading) {
    return (
      <div className="p-6">
        <div className="flex items-center justify-center h-64">
          <Spin size="large">
            <div className="text-center pt-4">
              <p className="text-gray-600 mt-4">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.loading', dm: '加载评估器详情中...' })}</p>
            </div>
          </Spin>
        </div>
      </div>
    );
  }

  if (error || !evaluator) {
    return (
      <div className="p-6">
        <Alert
          message={$i18n.get({ id: 'legacy.evaluation.common.loadFailed', dm: '加载失败' })}
          description={error || $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.notFound', dm: '评估器不存在' })}
          type="error"
          showIcon
          action={
            <Space>
              <Button size="small" onClick={loadEvaluatorDetail}>
                {$i18n.get({ id: 'legacy.evaluation.common.retry', dm: '重试' })}
              </Button>
              <Button size="small" onClick={() => navigate('/evaluation-evaluator')}>
                {$i18n.get({ id: 'legacy.evaluation.common.backToList', dm: '返回列表' })}
              </Button>
            </Space>
          }
        />
      </div>
    );
  }

  return (
    <div className="evaluator-detail-page p-8 fade-in">
      {/* 页面头部 */}
      <div className="flex mb-6">
          <Button 
            type="text" 
            icon={<ArrowLeftOutlined />} 
            onClick={() => navigate('/evaluation-evaluator')}
            size="large"
          />
          <Title level={2} className="m-0">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.pageTitle', dm: '评估器详情' })}</Title>
      </div>

      {/* 评估器基础信息 */}
      <Card
        title={
          <div className="flex justify-between items-center">
            <span>{$i18n.get({ id: 'legacy.evaluation.common.basicInformation', dm: '基本信息' })}</span>
            <div>
              {isEditing ? (
                <Space>
                  <Button
                    size="small"
                    onClick={() => {
                      setIsEditing(false);
                      form.setFieldsValue({
                        name: evaluator.name,
                        description: evaluator.description
                      });
                    }}
                    icon={<CloseOutlined />}
                  >
                    {$i18n.get({ id: 'legacy.evaluation.common.cancel', dm: '取消' })}
                  </Button>
                  <Button
                    type="primary"
                    size="small"
                    loading={editLoading}
                    onClick={handleSaveBasicInfo}
                    icon={<SaveOutlined />}
                  >
                    {$i18n.get({ id: 'legacy.evaluation.common.save', dm: '保存' })}
                  </Button>
                </Space>
              ) : (
                <Button
                  size="small"
                  onClick={() => setIsEditing(true)}
                  icon={<EditOutlined />}
                >
                  {$i18n.get({ id: 'legacy.evaluation.common.edit', dm: '编辑' })}
                </Button>
              )}
            </div>
          </div>
        }
        className="mb-6"
      >
        {isEditing ? (
          <Form form={form} layout="vertical">
            <Row gutter={24}>
              <Col span={12}>
                <Form.Item
                  label={$i18n.get({ id: 'legacy.evaluation.common.name', dm: '名称' })}
                  name="name"
                  rules={[
                    { required: true, message: $i18n.get({ id: 'legacy.evaluation.common.pleaseEnterEvaluatorName', dm: '请输入评估器名称' }) },
                    { max: 50, message: $i18n.get({ id: 'legacy.evaluation.common.nameMax50', dm: '名称不能超过50个字符' }) }
                  ]}
                >
                  <Input placeholder={$i18n.get({ id: 'legacy.evaluation.common.enterEvaluatorName', dm: '输入评估器名称' })} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label={$i18n.get({ id: 'legacy.evaluation.common.description', dm: '描述' })}
                  name="description"
                  rules={[{ max: 500, message: $i18n.get({ id: 'legacy.evaluation.common.descMax500', dm: '描述不能超过500个字符' }) }]}
                >
                  <TextArea
                    placeholder={$i18n.get({ id: 'legacy.evaluation.common.enterEvaluatorDescOptional', dm: '输入评估器描述（可选）' })}
                    rows={3}
                    showCount
                    maxLength={500}
                  />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        ) : (
          <Row gutter={[24, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <div>
                <Text type="secondary" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '12px' }}>
                  {$i18n.get({ id: 'legacy.evaluation.common.evaluatorName', dm: '评估器名称' })}
                </Text>
                <div style={{ marginTop: 4 }}>
                  <Text strong style={{ fontSize: '16px' }}>{evaluator.name}</Text>
                </div>
              </div>
            </Col>

            <Col xs={24} sm={12} lg={6}>
              <div>
                <Text type="secondary" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '12px' }}>
                  {$i18n.get({ id: 'legacy.evaluation.common.currentVersion', dm: '当前版本' })}
                </Text>
                <div style={{ marginTop: 4 }}>
                  {evaluator.latestVersion ? (
                    <Tag color="blue">{evaluator.latestVersion}</Tag>
                  ) : (
                    <Tag color="default">{$i18n.get({ id: 'legacy.evaluation.common.noVersions', dm: '暂无版本' })}</Tag>
                  )}
                </div>
              </div>
            </Col>

            <Col xs={24} sm={12} lg={6}>
              <div>
                <Text type="secondary" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '12px' }}>
                  {$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.currentModel', dm: '当前模型' })}
                </Text>
                <div style={{ marginTop: 4 }}>
                  {(() => {
                    const { modelName } = getCurrentModelInfo();
                    return (
                      <Tag color="geekblue">{modelName}</Tag>
                    );
                  })()}
                </div>
              </div>
            </Col>
          </Row>
        )}

        {!isEditing && evaluator.description && (
          <div style={{ marginTop: 16 }}>
            <Text type="secondary" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '12px' }}>
              {$i18n.get({ id: 'legacy.evaluation.common.description', dm: '描述' })}
            </Text>
            <div style={{ marginTop: 8 }}>
              <Text>{evaluator.description}</Text>
            </div>
          </div>
        )}

        {!isEditing && (
          <>
            <Divider />
            <Row gutter={[16, 8]}>
              <Col span={12}>
                <Text type="secondary">
                  {$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.createdAtLabel', dm: '创建时间：{time}' }, { time: formatDateTime(evaluator.createTime) })}
                </Text>
              </Col>
              <Col span={12}>
                <Text type="secondary">
                  {$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.updatedAtLabel', dm: '更新时间：{time}' }, { time: formatDateTime(evaluator.updateTime) })}
                </Text>
              </Col>
            </Row>
          </>
        )}
      </Card>

      {/* 底部Tab区域 */}
      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          items={[
            {
              key: 'config',
              label: $i18n.get({ id: 'legacy.evaluation.common.modelConfiguration', dm: '模型配置' }),
              children: (
                <div>
                  <div className="flex justify-between items-center mb-4">
                    <Title level={4} className="m-0">{$i18n.get({ id: 'legacy.evaluation.common.modelConfiguration', dm: '模型配置' })}</Title>
                    <Space>
                      <Button onClick={handleOpenTemplateModal}>{$i18n.get({ id: 'legacy.evaluation.common.importFromTemplate', dm: '从模版导入' })}</Button>
                      <Tooltip title={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.openDebugTooltip', dm: '跳转至调试页面' })}>
                        <Button
                          icon={<BugOutlined />}
                          onClick={handleDebug}
                          disabled={!canPublishVersion}
                        >
                          {$i18n.get({ id: 'legacy.evaluation.common.debug', dm: '调试' })}
                        </Button>
                      </Tooltip>
                      <Tooltip title={canPublishVersion ? $i18n.get({ id: 'legacy.evaluation.common.publishNewVersion', dm: '发布新版本' }) : $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.needVariablesTooltip', dm: 'System Prompt 中必须包含变量才能发布版本' })}>
                        <Button
                          type="primary"
                          icon={<RocketOutlined />}
                          onClick={handlePublishVersion}
                          disabled={!canPublishVersion}
                        >
                          {$i18n.get({ id: 'legacy.evaluation.common.publishNewVersion', dm: '发布新版本' })}
                        </Button>
                      </Tooltip>
                    </Space>
                  </div>

                  <Form form={configForm} layout="vertical">
                    <Row gutter={24}>
                      <Col span={24}>
                        <Form.Item label={$i18n.get({ id: 'legacy.evaluation.common.judgeModel', dm: '裁判模型' })} name="modelId" required>
                          <Select placeholder={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.selectModel', dm: '选择模型' })} onChange={handleModelChange}>
                            {models.map(model => (
                              <Option key={model.id} value={model.id}>
                                {model.name}
                              </Option>
                            ))}
                          </Select>
                        </Form.Item>
                      </Col>
                      {
                        modelConfigList.map(({ key, value }) => {
                          const isNumber = !isNaN(Number(value))
                          if (isNumber) {
                            return (
                              <Col span={12} key={key}>
                                <Form.Item label={key} name={key}>
                                  <InputNumber
                                    style={{ width: '100%' }}
                                    placeholder={value}
                                  />
                                </Form.Item>
                              </Col>
                            )
                          }
                          return (
                            <Col span={12} key={key}>
                              <Form.Item label={key} name={key}>
                                <Input
                                  style={{ width: '100%' }}
                                  placeholder={value}
                                />
                              </Form.Item>
                            </Col>
                          )
                        })
                      }
                      <Col span={24}>
                        <Form.Item
                          required
                          label={
                            <div className="flex items-center gap-2">
                              <span>System Prompt</span>
                              <Tooltip title={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.varFormatTooltip', dm: '使用 {{variable_name}} 格式定义变量' })}>
                                <InfoCircleOutlined className="text-gray-400" />
                              </Tooltip>
                            </div>
                          }
                          name="systemPrompt"
                        >
                          <TextArea
                            rows={3}
                            placeholder={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.systemPromptPlaceholder', dm: '输入系统提示词，使用 {{variable_name}} 定义变量' })}
                            onChange={(e) => {
                              // 实时显示检测到的变量
                              const newPrompt = e.target.value;
                              const variables = extractVariablesFromPrompt(newPrompt);

                              // 清理不再存在的变量值
                              setVariableValues(prev => {
                                const newValues: Record<string, string> = {};
                                variables.forEach(name => {
                                  newValues[name] = prev[name] || '';
                                });
                                return newValues;
                              });
                            }}
                          />
                        </Form.Item>
                        {/* 显示检测到的变量 */}
                        <Form.Item required noStyle>
                          <Form.Item required dependencies={['systemPrompt']} noStyle>
                            {({ getFieldValue }) => {
                              const promptValue = getFieldValue('systemPrompt') || '';
                              const variableNames = extractVariablesFromPrompt(promptValue);

                              if (variableNames.length > 0) {
                                return (
                                  <div className="mb-4">
                                    <div className="p-3 bg-blue-50 border border-blue-200 rounded mb-4">
                                      <div className="flex items-center gap-2 mb-2">
                                        <InfoCircleOutlined className="text-blue-500" />
                                        <span className="text-sm font-medium text-blue-700">
                                          {$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.detectedVariables', dm: '检测到的变量 ({count} 个)' }, { count: variableNames.length })}
                                        </span>
                                      </div>
                                      <div className="flex flex-wrap gap-2">
                                        {variableNames.map(name => (
                                          <Tag key={name} color="blue" className="mb-1">
                                            {name}
                                          </Tag>
                                        ))}
                                      </div>
                                    </div>
                                  </div>
                                );
                              } else if (promptValue.trim()) {
                                // 如果有prompt内容但没有变量，显示警告
                                return (
                                  <div className="mb-4">
                                    <Alert
                                      message={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.noVariablesTitle', dm: '未检测到变量' })}
                                      description={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.noVariablesDesc', dm: 'System Prompt 中未检测到变量（格式：{{变量名}}）。需要添加变量才能发布版本。' })}
                                      type="warning"
                                      showIcon
                                      icon={<ExclamationCircleOutlined />}
                                    />
                                  </div>
                                );
                              }
                              return null;
                            }}
                          </Form.Item>
                        </Form.Item>
                      </Col>
                    </Row>
                  </Form>
                </div>
              )
            },
            {
              key: 'versions',
              label: $i18n.get({ id: 'legacy.evaluation.common.versionHistory', dm: '版本记录' }),
              children: (
                <Table
                  columns={versionColumns}
                  dataSource={versions}
                  loading={versionsLoading}
                  rowKey="id"
                  pagination={{
                    ...versionPagination,
                    onChange: onVersionPaginationChange,
                    onShowSizeChange: onVersionShowSizeChange
                  }}
                />
              )
            },
            {
              key: 'experiments',
              label: $i18n.get({ id: 'legacy.evaluation.common.relatedExperiments', dm: '关联实验' }),
              children: (
                <div>
                  {/* <Row className="mb-4">
                    <Col span={6}>
                      <Input.Search
                        placeholder="Search experiments"
                        onSearch={handleExperimentSearch}
                      />
                    </Col>
                  </Row> */}
                  <Table
                    dataSource={experiments}
                    rowKey="id"
                    loading={experimentsLoading}
                    pagination={{
                      ...experimentPagination,
                      onChange: onExperimentPaginationChange,
                      onShowSizeChange: onExperimentShowSizeChange,
                    }}
                    columns={[
                      {
                        title: $i18n.get({ id: 'legacy.evaluation.common.version', dm: '版本号' }),
                        dataIndex: 'version',
                        width: '15%',
                        render: (version: string) => (
                          <Tag color="cyan">{version}</Tag>
                        )
                      },
                      {
                        title: $i18n.get({ id: 'legacy.evaluation.common.experimentName', dm: '实验名称' }),
                        dataIndex: 'name',
                        width: '10%',
                        render: (name: string, record: any) => (
                          <span
                            className="text-blue-600 cursor-pointer hover:text-blue-800 hover:underline font-medium"
                            onClick={() => {
                              // 跳转到实验详情页面
                              navigate(`/evaluation-experiment/detail/${record.id}`);
                            }}
                          >
                            {name}
                          </span>
                        )
                      },
                      {
                        title: $i18n.get({ id: 'legacy.evaluation.common.description', dm: '描述' }),
                        dataIndex: 'description',
                        width: '25%',
                        ellipsis: true,
                        render: (description: string) => (
                          <Tooltip title={description}>
                            <span className="text-xs truncate">{description}</span>
                          </Tooltip>
                        )
                      },
                      {
                        title: $i18n.get({ id: 'legacy.evaluation.common.status', dm: '状态' }),
                        dataIndex: 'status',
                        width: '15%',
                        render: (status: string) => {
                          const statusConfig = {
                            'RUNNING': { color: 'processing', text: $i18n.get({ id: 'legacy.evaluation.common.statusRunning', dm: '运行中' }) },
                            'COMPLETED': { color: 'success', text: $i18n.get({ id: 'legacy.evaluation.common.statusCompleted', dm: '已完成' }) },
                            'FAILED': { color: 'error', text: $i18n.get({ id: 'legacy.evaluation.common.statusStopped', dm: '已停止' }) },
                            'WAITING': { color: 'default', text: $i18n.get({ id: 'legacy.evaluation.common.statusWaiting', dm: '等待中' }) },
                            '运行中': { color: 'processing', text: $i18n.get({ id: 'legacy.evaluation.common.statusRunning', dm: '运行中' }) },
                            '已完成': { color: 'success', text: $i18n.get({ id: 'legacy.evaluation.common.statusCompleted', dm: '已完成' }) },
                            '已停止': { color: 'error', text: $i18n.get({ id: 'legacy.evaluation.common.statusStopped', dm: '已停止' }) },
                            '等待中': { color: 'default', text: $i18n.get({ id: 'legacy.evaluation.common.statusWaiting', dm: '等待中' }) }
                          };
                          const config = statusConfig[status as keyof typeof statusConfig] || statusConfig['WAITING'];
                          return <Tag color={config.color}>{config.text}</Tag>;
                        }
                      },
                      {
                        title: $i18n.get({ id: 'legacy.evaluation.common.createdAt', dm: '创建时间' }),
                        dataIndex: 'createTime',
                        width: '30%',
                        render: (text: string) => formatDateTime(text)
                      }
                    ]}
                  />
                </div>
              )
            }
          ]}
        />
      </Card>

      {/* 版本发布弹窗 */}
      <Modal
        title={
          <div className="flex items-center gap-3">
            <RocketOutlined className="text-blue-500" />
            <span>{$i18n.get({ id: 'legacy.evaluation.common.publishNewVersion', dm: '发布新版本' })}</span>
          </div>
        }
        open={showPublishModal}
        onCancel={() => {
          setShowPublishModal(false);
          publishForm.resetFields();
        }}
        onOk={handlePublishConfirm}
        confirmLoading={publishLoading}
        okText={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.publishVersion', dm: '发布版本' })}
        cancelText={$i18n.get({ id: 'legacy.evaluation.common.cancel', dm: '取消' })}
        width={520}
        centered
      >
        <div className="pt-4">
          <Form
            form={publishForm}
            layout="vertical"
          >
            <Form.Item
              label={$i18n.get({ id: 'legacy.evaluation.common.version', dm: '版本号' })}
              name="version"
              rules={[
                { required: true, message: $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.pleaseEnterVersion', dm: '请输入版本号' }) },
                {
                  pattern: /^\d+\.\d+\.\d+$/,
                  message: $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionFormat', dm: '版本号格式应为 x.y.z (如: 1.0.0)' })
                }
              ]}
            >
              <Input placeholder={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionPlaceholder', dm: '请输入版本号，如: 1.0.0' })} />
            </Form.Item>

            <Form.Item
              label={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionDescription', dm: '版本描述' })}
              name="description"
              rules={[
                { max: 200, message: $i18n.get({ id: 'legacy.evaluation.evaluatorDetail.descMax200', dm: '描述不能超过200个字符' }) }
              ]}
            >
              <TextArea
                placeholder={$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.versionDescPlaceholder', dm: '请输入版本描述（可选）' })}
                rows={3}
                showCount
                maxLength={200}
              />
            </Form.Item>
          </Form>
        </div>
      </Modal>

      {/* 模板导入弹窗 */}
      <Modal
        title={
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-50 rounded-full flex items-center justify-center">
              <InfoCircleOutlined className="text-blue-500 text-xl" />
            </div>
            <div>
              <Title level={3} className="m-0">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.importTemplateTitle', dm: '从模板导入' })}</Title>
              <Text type="secondary">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.importTemplateSubtitle', dm: '选择一个预设模板快速配置评估器' })}</Text>
            </div>
          </div>
        }
        open={showTemplateModal}
        onCancel={() => {
          setShowTemplateModal(false);
          setSelectedTemplateId(null);
          setSelectedTemplateDetail(null);
        }}
        width={1000}
        centered
        footer={[
          <Button key="cancel" onClick={() => {
            setShowTemplateModal(false);
            setSelectedTemplateId(null);
            setSelectedTemplateDetail(null);
          }}>
            {$i18n.get({ id: 'legacy.evaluation.common.cancel', dm: '取消' })}
          </Button>,
          <Button
            key="import"
            type="primary"
            disabled={!selectedTemplateDetail}
            onClick={handleTemplateImport}
            icon={<SaveOutlined />}
          >
            {$i18n.get({ id: 'legacy.evaluation.common.importTemplate', dm: '导入模板' })}
          </Button>
        ]}
      >
        <Row gutter={24}>
          {/* 左侧：模板列表 */}
          <Col span={16}>
            <Card
              title={
                <div className="flex items-center gap-2">
                  <span>{$i18n.get({ id: 'legacy.evaluation.common.selectTemplate', dm: '选择模板' })}</span>
                  <Text type="secondary">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.templateCount', dm: '({count} 个模板)' }, { count: templates.length })}</Text>
                </div>
              }
              size="small"
            >
              <Spin spinning={templatesLoading}>
                {templates.length > 0 ? (
                  <Row gutter={[16, 16]}>
                    {templates.map(template => (
                      <Col span={12} key={template.id}>
                        <Card
                          size="small"
                          hoverable
                          onClick={() => handleTemplateSelect(template.id)}
                          className={selectedTemplateId === template.id ? 'border-blue-500 bg-blue-50' : ''}
                          classNames={{
                            body: 'p-3'
                          }}
                        >
                          <div className="flex justify-between items-start mb-2">
                            <Text strong className="text-sm">{template.templateDesc}</Text>
                            {selectedTemplateId === template.id && (
                              <CheckCircleOutlined className="text-blue-500" />
                            )}
                          </div>
                          <Text type="secondary" className="text-xs block mb-2" ellipsis>
                            {template.evaluatorTemplateKey}
                          </Text>
                        </Card>
                      </Col>
                    ))}
                  </Row>
                ) : (
                  <div className="text-center py-16">
                    <Text type="secondary">{$i18n.get({ id: 'legacy.evaluation.common.noTemplateData', dm: '暂无模板数据' })}</Text>
                  </div>
                )}
              </Spin>
            </Card>
          </Col>

          {/* 右侧：模板预览 */}
          <Col span={8}>
            <Card
              title={
                <div className="flex items-center gap-2">
                  <InfoCircleOutlined />
                  <span>{$i18n.get({ id: 'legacy.evaluation.common.templatePreview', dm: '模板预览' })}</span>
                </div>
              }
              size="small"
            >
              <Spin spinning={templateDetailLoading}>
                {selectedTemplateDetail ? (
                  <div className="space-y-4">
                    <div>
                      <Text strong className="block mb-1">{$i18n.get({ id: 'legacy.evaluation.common.templateName', dm: '模板名称' })}</Text>
                      <Text>{selectedTemplateDetail.templateDesc}</Text>
                    </div>

                    <div>
                      <Text strong className="block mb-1">{$i18n.get({ id: 'legacy.evaluation.common.templateKey', dm: '模板Key' })}</Text>
                      <Text>{selectedTemplateDetail.evaluatorTemplateKey}</Text>
                    </div>

                    {selectedTemplateDetail.template && (
                      <div>
                        <Text strong className="block mb-2">{$i18n.get({ id: 'legacy.evaluation.common.promptContent', dm: 'Prompt 内容' })}</Text>
                        <div className="bg-gray-50 p-3 rounded border text-xs font-mono max-h-48 overflow-y-auto whitespace-pre-wrap">
                          {selectedTemplateDetail.template}
                        </div>
                      </div>
                    )}

                    {selectedTemplateDetail.modelConfig && (
                      <div>
                        <Text strong className="block mb-2">{$i18n.get({ id: 'legacy.evaluation.common.modelConfiguration', dm: '模型配置' })}</Text>
                        <div className="bg-gray-50 p-3 rounded border text-xs font-mono">
                          {JSON.stringify(JSON.parse(selectedTemplateDetail.modelConfig), null, 2)}
                        </div>
                      </div>
                    )}

                    {selectedTemplateDetail.variables && (
                      <div>
                        <Text strong className="block mb-2">{$i18n.get({ id: 'legacy.evaluation.common.variables', dm: '变量' })}</Text>
                        <div className="bg-gray-50 p-3 rounded border text-xs font-mono">
                          {selectedTemplateDetail.variables}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <InfoCircleOutlined className="text-4xl text-gray-300 mb-4" />
                    <br />
                    <Text type="secondary">{$i18n.get({ id: 'legacy.evaluation.evaluatorDetail.selectTemplateHint', dm: '点击左侧模板查看详情' })}</Text>
                  </div>
                )}
              </Spin>
            </Card>
          </Col>
        </Row>
      </Modal>
    </div>
  );
}

export default EvaluatorDetail;
