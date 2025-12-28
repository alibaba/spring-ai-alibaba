import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Select, message, Tag, AutoComplete } from 'antd';
import { ArrowLeftOutlined, EyeOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import API from '../../../../services';
import { getLegacyPath, buildLegacyPath } from '../../../../utils/path';
import './index.css';

const { TextArea } = Input;
const { Option } = Select;



// 表单数据接口
interface ExperimentCreateForm {
  // 步骤1：配置信息
  name: string;
  description: string;

  // 步骤2：配置评测集
  datasetId: string;
  datasetVersionId?: string;

  // 步骤3：配置评测对象
  objectType: string;
  promptKey?: string;
  version?: string;

  // 步骤4：配置评估器
  evaluatorId: string;
}

// 对象类型选项
const objectTypes = [
  { value: 'prompt', label: 'Prompt' }
];

// 组件属性接口
interface GatherCreateProps {
  onCancel?: () => void;
  onSuccess?: () => void;
  hideTitle?: boolean; // 添加hideTitle属性来控制是否隐藏标题
}

const ExperimentCreate: React.FC<GatherCreateProps> = ({ onCancel, onSuccess, hideTitle = false }) => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [selectedDataset, setSelectedDataset] = useState<any>(null); // 选中的评测集
  const [datasets, setDatasets] = useState<any[]>([]); // 评测集列表数据
  const [datasetsLoading, setDatasetsLoading] = useState(false); // 评测集数据加载状态
  const [datasetVersions, setDatasetVersions] = useState<any[]>([]); // 评测集版本列表
  const [datasetVersionsLoading, setDatasetVersionsLoading] = useState(false); // 评测集版本加载状态

  // Prompts相关状态
  const [prompts, setPrompts] = useState<any[]>([]); // Prompts列表数据
  const [promptsLoading, setPromptsLoading] = useState(false); // Prompts数据加载状态
  const [selectedPrompt, setSelectedPrompt] = useState<any>(null); // 选中的Prompt
  const [promptVersions, setPromptVersions] = useState<any[]>([]); // 选中的Prompt的版本列表
  const [promptVersionsLoading, setPromptVersionsLoading] = useState(false); // 版本数据加载状态
  console.log(promptVersions, 'zxc...')

  // 评估器相关状态
  const [evaluators, setEvaluators] = useState<any[]>([]); // 评估器列表数据
  const [evaluatorsLoading, setEvaluatorsLoading] = useState(false); // 评估器数据加载状态
  const [selectedEvaluators, setSelectedEvaluators] = useState<any[]>([]); // 已选择的评估器列表

  // 评估器版本相关状态
  const [evaluatorVersions, setEvaluatorVersions] = useState<Record<string, any[]>>({}); // 按评估器ID存储版本列表
  const [evaluatorVersionsLoading, setEvaluatorVersionsLoading] = useState<Record<string, boolean>>({}); // 按评估器ID存储加载状态
  const [selectedEvaluatorVersions, setSelectedEvaluatorVersions] = useState<Record<string, string>>({}); // 按评估器ID存储选中的版本

  // 评估器配置模式状态
  const [evaluatorConfigMode, setEvaluatorConfigMode] = useState<Record<string, boolean>>({}); // 按评估器ID存储配置模式（true为配置模式，false为映射模式）
  const [evaluatorParams, setEvaluatorParams] = useState<Record<string, string[]>>({}); // 按评估器ID存储参数列表
  const [evaluatorParamMappings, setEvaluatorParamMappings] = useState<Record<string, Record<string, string>>>({}); // 按评估器ID存储参数映射关系

  // Prompt版本详情相关状态
  const [promptVersionDetail, setPromptVersionDetail] = useState<any>(null); // 选中的Prompt版本详情
  const [promptVersionDetailLoading, setPromptVersionDetailLoading] = useState(false); // Prompt版本详情加载状态

  // 评测集详情相关状态
  const [datasetDetail, setDatasetDetail] = useState<any>(null); // 选中的评测集详情
  const [datasetDetailLoading, setDatasetDetailLoading] = useState(false); // 评测集详情加载状态

  // 字段映射配置状态
  const [fieldMapping, setFieldMapping] = useState<Record<string, string>>({}); // 字段映射配置 {promptParam: datasetField}

  // 对象类型选择状态
  const [selectedObjectType, setSelectedObjectType] = useState<string>(''); // 选中的对象类型

  // 返回列表页面
  const handleGoBack = () => {
    if (onCancel) {
      onCancel();
    } else {
      navigate('/evaluation-experiment');
    }
  };

  // 获取评测集列表
  const fetchDatasets = async () => {
    try {
      setDatasetsLoading(true);
      const response = await API.getDatasets({
        pageNumber: 1,
        pageSize: 100 // 获取较多数据，保证可以获取到所有评测集
      });

      if (response.code === 200 && response.data) {
        const responseData = response.data as any;
        const dataItems = responseData.pageItems || responseData.records || [];

        // 如果接口返回的数据为空，设置为空数组
        const transformedDatasets = dataItems.map((item: any) => {
          // 解析 columnsConfig 获取列信息
          let columns = ['input', 'reference_output']; // 默认列
          try {
            if (item.columnsConfig) {
              const parsedConfig = JSON.parse(item.columnsConfig);
              if (Array.isArray(parsedConfig) && parsedConfig.length > 0) {
                columns = parsedConfig.map((col: any) => col.name || col);
              }
            }
          } catch (e) {
            // 解析 columnsConfig 失败处理
          }

          return {
            id: item.id.toString(),
            name: item.name,
            description: item.description || '',
            dataCount: item.dataCount || 0,
            versions: ['v1.0.0'], // 默认版本，后续可以扩展为动态获取
            columns: columns,
            createTime: item.createTime,
            updateTime: item.updateTime
          };
        });
        setDatasets(transformedDatasets);
      } else {
        // API调用失败，设置为空数组
        setDatasets([]);
      }
    } catch (error) {
      // 发生错误时设置为空数组
      setDatasets([]);
      message.error('获取评测集列表失败，请重试');
    } finally {
      setDatasetsLoading(false);
    }
  };

  // 获取Prompts列表
  const fetchPrompts = async () => {
    try {
      setPromptsLoading(true);
      const response = await API.getPrompts({
        pageNo: 1,
        pageSize: 100 // 获取较多数据
      });

      if (response.code === 200 && response.data) {
        const promptsData = (response.data as any).pageItems || [];
        setPrompts(promptsData);
      } else {
        setPrompts([]);
        message.error('获取Prompts列表失败');
      }
    } catch (error) {
      setPrompts([]);
      message.error('获取Prompts列表失败，请重试');
    } finally {
      setPromptsLoading(false);
    }
  };

  // 获取评估器列表
  const fetchEvaluators = async () => {
    try {
      setEvaluatorsLoading(true);
      const response = await API.getEvaluators({
        pageNumber: 1,
        pageSize: 100 // 获取较多数据
      });

      if (response.code === 200 && response.data) {
        const evaluatorsData = (response.data as any).pageItems || [];
        setEvaluators(evaluatorsData);
      } else {
        setEvaluators([]);
        message.error('获取评估器列表失败');
      }
    } catch (error) {
      setEvaluators([]);
      message.error('获取评估器列表失败，请重试');
    } finally {
      setEvaluatorsLoading(false);
    }
  };

  // 获取评测集版本列表
  const fetchDatasetVersions = async (datasetId: string) => {
    try {
      setDatasetVersionsLoading(true);
      const response = await API.getDatasetVersions({
        datasetId: Number(datasetId),
        pageNumber: 1,
        pageSize: 50
      });

      if (response.code === 200 && response.data) {
        const versionsData = (response.data as any).pageItems || [];
        setDatasetVersions(versionsData);
      } else {
        setDatasetVersions([]);
        message.error('获取评测集版本列表失败');
      }
    } catch (error) {
      setDatasetVersions([]);
      message.error('获取评测集版本列表失败');
    } finally {
      setDatasetVersionsLoading(false);
    }
  };

  // 处理Prompt版本选择
  const handlePromptVersionChange = async (version: string) => {
    const promptKey = form.getFieldValue('promptKey');
    if (promptKey && version) {
      await fetchPromptVersionDetail(promptKey, version);
    }
  };

  // 处理字段映射变化
  const handleFieldMappingChange = (promptParam: string, datasetField: string) => {
        // 确保有实际值
    const actualValue = datasetField || 'input';
    setFieldMapping(prev => ({
      ...prev,
      [promptParam]: actualValue
    }));
  };

  // 处理评测集选择
  const handleDatasetChange = async (datasetId: string) => {
    const dataset = datasets.find(d => d.id === datasetId);
    setSelectedDataset(dataset);
    // 清空版本选择
    form.setFieldValue('datasetVersionId', undefined);
    setDatasetVersions([]);
    setDatasetDetail(null);

    if (dataset) {
      // 获取该评测集的版本列表
      await fetchDatasetVersions(datasetId);
      // 获取评测集详情
      await fetchDatasetDetail(datasetId);
    }
  };

  // 智能匹配字段映射默认值
  const generateDefaultFieldMapping = (promptParams: string[], datasetFields: string[]): Record<string, string> => {
    const mapping: Record<string, string> = {};

    // 定义常见的字段映射规则
    const mappingRules = [
      // 完全匹配
      { pattern: /^input$/i, target: 'input' },
      { pattern: /^output$/i, target: 'output' },
      { pattern: /^reference_output$/i, target: 'reference_output' },
      { pattern: /^expected_output$/i, target: 'reference_output' },
      { pattern: /^answer$/i, target: 'reference_output' },
      { pattern: /^question$/i, target: 'input' },
      { pattern: /^query$/i, target: 'input' },
      { pattern: /^text$/i, target: 'input' },
      { pattern: /^content$/i, target: 'input' },

      // 模糊匹配
      { pattern: /input|question|query|prompt/i, target: 'input' },
      { pattern: /output|answer|response|result/i, target: 'reference_output' },
      { pattern: /reference|expected|target|ground_truth/i, target: 'reference_output' }
    ];

    promptParams.forEach(param => {
      let matchedField = '';

      // 首先尝试完全匹配
      for (const rule of mappingRules) {
        if (rule.pattern.test(param) && datasetFields.includes(rule.target)) {
          matchedField = rule.target;
          break;
        }
      }

      // 如果没有匹配到，尝试在数据集字段中找到包含相似关键词的字段
      if (!matchedField) {
        for (const field of datasetFields) {
          if (param.toLowerCase().includes(field.toLowerCase()) ||
              field.toLowerCase().includes(param.toLowerCase())) {
            matchedField = field;
            break;
          }
        }
      }

      // 如果还是没有匹配到，必须设置一个有效的默认字段
      if (!matchedField && datasetFields.length > 0) {
        // 优先选择 input 字段，然后是 reference_output，最后是第一个字段
        if (datasetFields.includes('input')) {
          matchedField = 'input';
        } else if (datasetFields.includes('reference_output')) {
          matchedField = 'reference_output';
        } else {
          matchedField = datasetFields[0];
        }
      }

      // 确保总是有一个有效值，即使没有数据集字段也要有默认值
      if (!matchedField || matchedField === '') {
        matchedField = 'input'; // 最后的安全默认值
      }

      mapping[param] = matchedField;

    });

    return mapping;
  };

  // 更新字段映射（在获取到评测集详情后调用）
  const updateFieldMappingWithDefaults = () => {
    if (promptVersionDetail && datasetDetail) {
      try {
        const variables = JSON.parse(promptVersionDetail.variables || '{}');
        const promptParams = Object.keys(variables);

        // 解析评测集字段
        let datasetFields: string[] = [];
        try {
          const columnsConfig = JSON.parse(datasetDetail.columnsConfig || '[]');
          datasetFields = Array.isArray(columnsConfig) ? columnsConfig.map((col: any) => col.name || col) : [];
        } catch (e) {
          datasetFields = ['input', 'reference_output']; // 默认字段
        }

        // 生成默认映射
        const defaultMapping = generateDefaultFieldMapping(promptParams, datasetFields);
                // 再次确认所有映射值非空
        Object.keys(defaultMapping).forEach(key => {
          if (!defaultMapping[key] || defaultMapping[key] === '') {
            defaultMapping[key] = datasetFields[0] || 'input';
          }
        });

        // 强制更新状态
        setFieldMapping({});
        setTimeout(() => {
          setFieldMapping(defaultMapping);
        }, 10);
      } catch (e) {
        // 更新字段映射默认值失败处理
      }
    }
  };

  // 获取Prompt版本详情
  const fetchPromptVersionDetail = async (promptKey: string, version: string) => {
    try {
      setPromptVersionDetailLoading(true);
      const response = await API.getPromptVersion({
        promptKey,
        version
      });

      if (response.code === 200 && response.data) {
        setPromptVersionDetail(response.data);

        // 解析variables字段，获取参数信息
        try {
          const variables = JSON.parse(response.data.variables || '{}');
          // 初始化字段映射为空，等待评测集详情加载后再设置默认值
          const initialMapping: Record<string, string> = {};
          const promptParams = Object.keys(variables);

          // 即使评测集详情未加载，也设置一个初始字段值
          promptParams.forEach(param => {
            initialMapping[param] = 'input'; // 默认使用input字段
          });
          setFieldMapping(initialMapping);

          // 如果评测集详情已经加载，立即更新默认值
          if (datasetDetail) {
            setTimeout(() => updateFieldMappingWithDefaults(), 100);
          }
        } catch (e) {
          setFieldMapping({});
        }
      } else {
        setPromptVersionDetail(null);
        message.error('获取Prompt版本详情失败');
      }
    } catch (error) {
      setPromptVersionDetail(null);
      message.error('获取Prompt版本详情失败');
    } finally {
      setPromptVersionDetailLoading(false);
    }
  };

  // 获取评测集详情
  const fetchDatasetDetail = async (datasetId: string) => {
    try {
      setDatasetDetailLoading(true);
      const response = await API.getDataset({
        datasetId: Number(datasetId)
      });

      if (response.code === 200 && response.data) {
        setDatasetDetail(response.data);

        // 如果Prompt版本详情已经加载，立即更新字段映射默认值
        if (promptVersionDetail) {
          setTimeout(() => updateFieldMappingWithDefaults(), 100);
        }
      } else {
        setDatasetDetail(null);
        message.error('获取评测集详情失败');
      }
    } catch (error) {
      setDatasetDetail(null);
      message.error('获取评测集详情失败');
    } finally {
      setDatasetDetailLoading(false);
    }
  };

  // 处理Prompt Key选择
  const handlePromptKeyChange = async (promptKey: string) => {
    const prompt = prompts.find(p => p.promptKey === promptKey);
    setSelectedPrompt(prompt);

    // 清空版本和详情选择
    form.setFieldValue('version', undefined);
    setPromptVersionDetail(null);
    setFieldMapping({});

    if (prompt) {
      // 获取该Prompt的版本列表
      try {
        setPromptVersionsLoading(true);
        const response = await API.getPromptVersions({
          promptKey: prompt.promptKey,
          pageNo: 1,
          pageSize: 50
        });

        if (response.code === 200 && response.data) {
          setPromptVersions((response.data as any).pageItems || []);
        } else {
          setPromptVersions([]);
          message.error('获取Prompt版本失败');
        }
      } catch (error) {
        setPromptVersions([]);
        message.error('获取Prompt版本失败');
      } finally {
        setPromptVersionsLoading(false);
      }
    } else {
      setPromptVersions([]);
    }
  };

  // 获取评估器参数
  const fetchEvaluatorParams = async (evaluatorId: string, versionId: string) => {
    try {
      // 调用接口获取评估器详情
      const response = await API.getEvaluator({
        id: Number(evaluatorId)
      });

      if (response.code === 200 && response.data) {
        // 获取评估器参数
        const defaultParams = ['input', 'output', 'reference_output'];
        let params: string[] = [];

        try {
          // 尝试解析variables字段
          const variables = JSON.parse(response.data.variables || '{}');
          params = Object.keys(variables);

          if (params.length === 0) {
            params = defaultParams;
          }
        } catch (e) {
          params = defaultParams;
        }

        // 更新评估器参数
        setEvaluatorParams(prev => ({
          ...prev,
          [evaluatorId]: params
        }));

        // 初始化映射关系
        const initialMappings: Record<string, string> = {};
        const dataSourceFields = getDataSourceFields();

        // 根据参数智能匹配数据源
        params.forEach(param => {
          // 默认映射关系
          if (param === 'input') {
            initialMappings[param] = 'input';
          } else if (param === 'output') {
            // 对于output参数，映射到actual_output字段
            initialMappings[param] = 'actual_output';
          } else if (param === 'reference_output') {
            initialMappings[param] = 'reference_output';
          } else if (dataSourceFields.some(item => item.field === param)) {
            // 如果数据源中有同名字段，直接映射
            initialMappings[param] = param;
          } else {
            // 查找相似字段
            let matchedField = '';

            // 定义常见的字段映射规则
            const mappingRules = [
              { pattern: /input|question|query|prompt/i, target: 'input' },
              { pattern: /output|answer|response|result/i, target: 'actual_output' },
              { pattern: /reference|expected|target|ground_truth/i, target: 'reference_output' }
            ];

            // 尝试匹配
            for (const rule of mappingRules) {
              if (rule.pattern.test(param)) {
                // 查找匹配的目标字段是否存在于数据源中
                const targetField = dataSourceFields.find(item => item.field === rule.target);
                if (targetField) {
                  matchedField = rule.target;
                  break;
                }
              }
            }

            // 如果没有匹配到，尝试在数据源字段中找到包含相似关键词的字段
            if (!matchedField) {
              for (const field of dataSourceFields) {
                if (param.toLowerCase().includes(field.field.toLowerCase()) ||
                    field.field.toLowerCase().includes(param.toLowerCase())) {
                  matchedField = field.field;
                  break;
                }
              }
            }

            // 如果还是没有匹配到，使用第一个可用字段或默认input
            if (!matchedField && dataSourceFields.length > 0) {
              matchedField = dataSourceFields[0].field;
            }

            initialMappings[param] = matchedField || 'input';
          }
        });

        // 更新评估器参数映射
        setEvaluatorParamMappings(prev => ({
          ...prev,
          [evaluatorId]: initialMappings
        }));

        // 切换到映射模式
        setEvaluatorConfigMode(prev => ({
          ...prev,
          [evaluatorId]: false // 设置为非配置模式，显示映射界面
        }));

        return true;
      } else {
        message.error(`获取评估器${evaluatorId}的参数失败`);
        return false;
      }
    } catch (error) {
      message.error(`获取评估器${evaluatorId}的参数失败`);
      return false;
    }
  };

  // 获取评测集字段
  const getDatasetFields = (): string[] => {
    if (!datasetDetail) return ['input', 'reference_output'];

    try {
      const columnsConfig = JSON.parse(datasetDetail.columnsConfig || '[]');
      const fields = Array.isArray(columnsConfig) ? columnsConfig.map((col: any) => col.name || col) : [];

      // 确保至少有默认字段
      if (fields.length === 0) {
        return ['input', 'reference_output'];
      }

      return fields;
    } catch (e) {
      return ['input', 'reference_output'];
    }
  };

  // 定义数据源字段接口，包含字段名和来源信息
  interface DataSourceField {
    field: string;
    source: string;
    displayName: string;
  }

  // 获取数据来源字段（只使用评测集字段，并添加固定的actual_output字段）
  const getDataSourceFields = (): DataSourceField[] => {
    let fields: DataSourceField[] = [];

    // 获取评测集字段
    if (datasetDetail) {
      try {
        const columnsConfig = JSON.parse(datasetDetail.columnsConfig || '[]');
        const datasetFields = Array.isArray(columnsConfig) ? columnsConfig.map((col: any) => col.name || col) : [];
        // 添加评测集来源信息
        fields = [
          ...fields,
          ...datasetFields.map(field => ({
            field,
            source: '评测集',
            displayName: `${field} (评测集)`
          }))
        ];
      } catch (e) {
        // 解析评测集字段失败处理
      }
    }

    // 添加固定的actual_output字段，数据来源为"评测对象"
    fields = [
      ...fields,
      {
        field: 'actual_output',
        source: '评测对象',
        displayName: 'actual_output (评测对象)'
      }
    ];

    // 去重（基于field字段）
    const uniqueFields = Array.from(
      new Map(fields.map(item => [item.field, item])).values()
    );

    return uniqueFields;
  };

  // 切换评估器配置模式
  const toggleEvaluatorConfigMode = (evaluatorId: string) => {
    setEvaluatorConfigMode(prev => ({
      ...prev,
      [evaluatorId]: !prev[evaluatorId]
    }));

    // 清空评估器和版本的选择
    setSelectedEvaluators(prev => prev.map(evaluator =>
      evaluator.evaluatorId === evaluatorId
        ? { ...evaluator, evaluatorId: '', versionId: '' }
        : evaluator
    ));

    // 清空选中的评估器版本
    setSelectedEvaluatorVersions(prev => ({
      ...prev,
      [evaluatorId]: ''
    }));
  };

  // 处理评估器参数映射变化
  const handleEvaluatorParamMappingChange = (evaluatorId: string, param: string, fieldValue: string) => {
    // 获取完整的数据源字段对象
    const dataSourceFields = getDataSourceFields();
    const selectedField = dataSourceFields.find(item => item.field === fieldValue);

    setEvaluatorParamMappings(prev => ({
      ...prev,
      [evaluatorId]: {
        ...prev[evaluatorId],
        [param]: fieldValue
      }
    }));
  };

  // 获取评估器版本列表
  const fetchEvaluatorVersions = async (evaluatorId: string) => {
    try {
      // 设置对应评估器的加载状态
      setEvaluatorVersionsLoading(prev => ({ ...prev, [evaluatorId]: true }));

      // 调用接口获取评估器版本列表
      const response = await API.getEvaluatorVersions({
        evaluatorId: Number(evaluatorId),
        pageNumber: 1,
        pageSize: 50
      });

      if (response.code === 200 && response.data) {
        // 获取版本列表
        const versionsData = (response.data as any).pageItems || [];

        // 更新版本列表状态
        setEvaluatorVersions(prev => ({
          ...prev,
          [evaluatorId]: versionsData
        }));

        // 不再自动选择第一个版本，让用户自己选择
        // 清空当前选中的版本
        setSelectedEvaluatorVersions(prev => ({
          ...prev,
          [evaluatorId]: ''
        }));
      } else {
        // 设置空数组
        setEvaluatorVersions(prev => ({
          ...prev,
          [evaluatorId]: []
        }));
        message.error(`获取评估器${evaluatorId}的版本列表失败`);
      }
    } catch (error) {
      // 设置空数组
      setEvaluatorVersions(prev => ({
        ...prev,
        [evaluatorId]: []
      }));
      message.error(`获取评估器${evaluatorId}的版本列表失败`);
    } finally {
      // 重置加载状态
      setEvaluatorVersionsLoading(prev => ({ ...prev, [evaluatorId]: false }));
    }
  };

  // 处理评估器选择变化
  const handleEvaluatorSelectChange = async (index: number, evaluatorId: string) => {
    // 更新指定索引的评估器ID，同时保留其他评估器的状态
    const newEvaluators = [...selectedEvaluators];
    newEvaluators[index] = { ...newEvaluators[index], evaluatorId: evaluatorId };
    setSelectedEvaluators(newEvaluators);

    // 为当前评估器实例初始化配置模式为true（配置模式）
    // 使用索引和评估器ID组合作为key，确保每个实例独立
    const instanceKey = `${index}-${evaluatorId}`;
    setEvaluatorConfigMode(prev => ({
      ...prev,
      [instanceKey]: true
    }));

    // 清空当前评估器实例的版本选择
    setSelectedEvaluatorVersions(prev => ({
      ...prev,
      [instanceKey]: ''
    }));

    // 如果评估器ID有效，获取其版本列表
    if (evaluatorId) {
      await fetchEvaluatorVersions(evaluatorId);
    }
  };

  // 处理评估器版本选择变化
  const handleEvaluatorVersionChange = async (index: number, evaluatorId: string, versionId: string) => {
    // 使用索引和评估器ID组合作为key，确保每个实例独立
    const instanceKey = `${index}-${evaluatorId}`;

    // 更新选中的版本
    setSelectedEvaluatorVersions(prev => ({
      ...prev,
      [instanceKey]: versionId
    }));

    // 如果选择了评估器和版本，获取评估器参数
    if (evaluatorId && versionId && datasetDetail) {
      // 获取参数并初始化映射
      const success = await fetchEvaluatorParams(evaluatorId, versionId);

      if (success) {
        // 已在fetchEvaluatorParams中切换到映射模式
        // 字段映射已自动配置
        setEvaluatorConfigMode(prev => ({
          ...prev,
          [instanceKey]: false // 切换到映射模式
        }));
      }
    } else if (evaluatorId && versionId && !datasetDetail) {
      // 有评估器和版本但没有数据集详情，仍然切换到映射模式
      setEvaluatorConfigMode(prev => ({
        ...prev,
        [instanceKey]: false // 切换到映射模式
      }));

      // 获取参数并初始化映射
      await fetchEvaluatorParams(evaluatorId, versionId);
    } else {
      // 没有选择完整的评估器和版本，保持配置模式
      if (evaluatorId) {
        setEvaluatorConfigMode(prev => ({
          ...prev,
          [instanceKey]: true
        }));
      }
    }
  };

  // 添加评估器
  const handleAddEvaluator = () => {
    // 创建一个新的评估器项
    const newEvaluator = {
      id: Date.now().toString(), // 临时ID，提交时会替换为实际选择的评估器ID
      evaluatorId: '',
      versionId: ''
    };

    // 添加到评估器列表
    setSelectedEvaluators(prev => [...prev, newEvaluator]);

    // 为新添加的评估器初始化状态
    const newIndex = selectedEvaluators.length;
    const instanceKey = `${newIndex}-`;

    // 初始化配置模式为true（配置模式）
    setEvaluatorConfigMode(prev => ({
      ...prev,
      [instanceKey]: true
    }));

    // 初始化版本选择为空
    setSelectedEvaluatorVersions(prev => ({
      ...prev,
      [instanceKey]: ''
    }));
  };

  // 移除评估器
  const handleRemoveEvaluator = (index: number) => {
    setSelectedEvaluators(prev => prev.filter((_, i) => i !== index));
  };

  // 查看评估器详情
  const handleViewEvaluatorDetail = (evaluatorId: string) => {
    navigate(getLegacyPath(`/evaluation/evaluator/${evaluatorId}`));
  };

  // 查看Prompt详情
  const handleViewPromptDetail = () => {
    const promptKey = form.getFieldValue('promptKey');
    if (promptKey) {
      navigate(buildLegacyPath('/prompt-detail', { promptKey }));
    }
  };

  // 查看评测集详情
  const handleViewDatasetDetail = () => {
    if (selectedDataset) {
      navigate(getLegacyPath(`/evaluation/gather/detail/${selectedDataset.id}`));
    }
  };

  // 组件加载时获取数据
  useEffect(() => {
    fetchDatasets();
    fetchPrompts();
    fetchEvaluators();
  }, []);

  // 监听字段映射状态变化
  useEffect(() => {
    if (promptVersionDetail && datasetDetail) {
      const variables = JSON.parse(promptVersionDetail.variables || '{}');
      const promptParams = Object.keys(variables);

      // 检查是否有没有设置映射值的参数
      let hasEmptyMapping = false;
      promptParams.forEach(param => {
        if (!fieldMapping[param]) {
          hasEmptyMapping = true;
        }
      });

      // 如果有空值，则重新生成默认映射
      if (hasEmptyMapping) {
        updateFieldMappingWithDefaults();
      }
    }
  }, [promptVersionDetail, datasetDetail, fieldMapping]);

  // 提交表单
  const handleSubmit = async (values: any) => {
    try {
      setLoading(true);

      // 检查是否有效的评估器配置
      if (selectedEvaluators.length === 0) {
        message.error('请至少添加一个评估器');
        setLoading(false);
        return;
      }

      // 检查是否所有评估器都有选择版本
      // 使用instanceKey来正确检查每个评估器实例的版本选择状态
      const missingVersions = selectedEvaluators.some((evaluator, index) => {
        const instanceKey = `${index}-${evaluator.evaluatorId}`;
        return !evaluator.evaluatorId || !selectedEvaluatorVersions[instanceKey];
      });
      if (missingVersions) {
        message.error('请确保所有评估器都选择了版本');
        setLoading(false);
        return;
      }

      // 检查评估器配置是否有效
      const invalidEvaluators = selectedEvaluators.some((evaluator, index) => {
        const instanceKey = `${index}-${evaluator.evaluatorId}`;
        return !evaluator.evaluatorId || !selectedEvaluatorVersions[instanceKey];
      });
      if (invalidEvaluators) {
        message.error('请确保所有评估器都已正确配置');
        setLoading(false);
        return;
      }

      // 构造评测对象配置
      // 解析Prompt参数变量
      let promptVariables = {};
      try {
        promptVariables = JSON.parse(promptVersionDetail?.variables || '{}');
      } catch (e) {
        // 解析失败则使用空对象
      }

      // 构造字段映射关系
      const variableMap = Object.keys(promptVariables).map(param => ({
        promptVariable: param,
        datasetVolumn: fieldMapping[param] || 'input'
      }));

      // 构造评测对象配置（将variableMap放在config内部，与promptKey、version平级）
      const evaluationObjectConfig = {
        type: values.objectType,
        config: {
          promptKey: values.promptKey,
          version: values.version, // 使用prompt的version字段
          variableMap: variableMap // 将variableMap放在config内部
        }
      };

      // 构造评估器配置
      const evaluatorConfig = selectedEvaluators.map((evaluator, index) => {
        // 获取评估器名称
        const evaluatorInfo = evaluators.find(e => e.id.toString() === evaluator.evaluatorId);

        // 使用instanceKey获取正确的版本信息
        const instanceKey = `${index}-${evaluator.evaluatorId}`;

        // 获取评估器版本名称
        let evaluatorVersionName = '';
        if (evaluator.evaluatorId && selectedEvaluatorVersions[instanceKey]) {
          const versions = evaluatorVersions[evaluator.evaluatorId];
          if (versions) {
            const selectedVersion = versions.find(v => v.id.toString() === selectedEvaluatorVersions[instanceKey]);
            if (selectedVersion) {
              evaluatorVersionName = selectedVersion.version || '';
            }
          }
        }

        // 构造评估器参数映射关系
        const variableMap: { evaluatorVariable: string; source: string; dataSource?: string }[] = [];
        if (evaluatorParams[evaluator.evaluatorId]) {
          evaluatorParams[evaluator.evaluatorId].forEach(param => {
            // 获取参数映射的源字段
            const sourceField = evaluatorParamMappings[evaluator.evaluatorId]?.[param] || 'input';

            // 获取源字段的来源信息
            const dataSourceFields = getDataSourceFields();
            const sourceFieldInfo = dataSourceFields.find(item => item.field === sourceField);

            variableMap.push({
              evaluatorVariable: param,
              source: sourceField,
              dataSource: sourceFieldInfo?.source || '默认' // 添加数据来源信息
            });
          });
        }

        return {
          evaluatorId: Number(evaluator.evaluatorId),
          evaluatorVersionId: Number(selectedEvaluatorVersions[instanceKey]),
          variableMap: variableMap,
          evaluatorName: evaluatorInfo?.name || '',
          evaluatorVersionName: evaluatorVersionName // 添加版本名称
        };
      }).filter(e => e.evaluatorId && e.evaluatorVersionId); // 过滤掉无效的评估器配置

      // 获取正确的datasetVersion值
      let datasetVersionValue = '';
      if (values.datasetVersionId) {
        const selectedVersion = datasetVersions.find(version => version.id === values.datasetVersionId);
        if (selectedVersion) {
          datasetVersionValue = selectedVersion.version;
        }
      }

      // 在最终提交时才使用JSON.stringify转换
      const submitData = {
        name: values.name,
        description: values.description,
        datasetId: Number(values.datasetId),
        datasetVersionId: Number(values.datasetVersionId),
        datasetVersion: datasetVersionValue, // 使用从datasetVersions中找到的version字段值
        evaluationObjectConfig: JSON.stringify(evaluationObjectConfig),
        evaluatorConfig: JSON.stringify(evaluatorConfig),
      };

      // 调用创建实验的API
      await API.createExperiment(submitData);

      message.success('实验创建成功');

      // 如果提供了onSuccess回调，则调用它，否则导航到列表页面
      if (onSuccess) {
        onSuccess();
      } else {
        navigate('/evaluation-experiment');
      }
    } catch (error) {
      message.error('创建失败，请重试');

    } finally {
      setLoading(false);
    }
  };

  // 取消创建
  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    } else {
      navigate('/evaluation-experiment');
    }
  };



  return (
    <div className="experiment-create-page">
      {/* 页面头部 - 固定在顶部 */}
      {!hideTitle && (
        <div className="experiment-create-header">
          <div className="flex items-center mb-4">
            <Button
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={handleGoBack}
              className="mr-3"
            >
              返回
            </Button>
            <h1 className="text-2xl font-semibold mb-0">新建实验</h1>
          </div>
        </div>
      )}

      {/* 表单区域 - 可滚动区域 */}
      <div className={`experiment-create-content ${hideTitle ? 'pt-6' : ''}`}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          {/* 步骤1：配置信息 */}
          <Card title="步骤1：配置信息" className="mb-6">
            <Form.Item
              name="name"
              label="实验名称"
              rules={[
                { required: true, message: '请输入实验名称' },
                { max: 100, message: '名称不能超过100个字符' }
              ]}
            >
              <Input placeholder="如：问答机器人实验测试" />
            </Form.Item>

            <Form.Item
              name="description"
              label="实验描述"
              rules={[{ max: 500, message: '描述不能超过500个字符' }]}
            >
              <TextArea
                placeholder="描述实验目的和内容"
                rows={4}
                showCount
                maxLength={500}
              />
            </Form.Item>
          </Card>

          {/* 步骤2：配置评测集 */}
          <Card title="步骤2：配置评测集" className="mb-6">
            <Form.Item
              name="datasetId"
              label="选择评测集"
              rules={[{ required: true, message: '请选择评测集' }]}
            >
              <Select
                placeholder="选择已创建的评测集"
                onChange={handleDatasetChange}
                loading={datasetsLoading}
                notFoundContent={datasetsLoading ? '加载中...' : '暂无数据'}
              >
                {datasets.map(dataset => (
                  <Option key={dataset.id} value={dataset.id}>
                    {dataset.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>

            {/* 当选择了评测集后，显示版本选择和评测集信息 */}
            {selectedDataset && (
              <>
                <Form.Item
                  name="datasetVersionId"
                  label="选择版本"
                  rules={[{ required: true, message: '请选择评测集版本' }]}
                >
                  <Select
                    placeholder="选择评测集版本"
                    loading={datasetVersionsLoading}
                    notFoundContent={datasetVersionsLoading ? '加载中...' : '暂无版本数据'}
                  >
                    {datasetVersions.map((version: any) => (
                      <Option key={version.id} value={version.id}>
                        {version.version} - {version.description}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                {/* 评测集信息 */}
                <div className="bg-gray-50 rounded-lg p-4 mt-4">
                  <div className="flex justify-between items-start mb-3">
                    <h4 className="text-base font-medium text-gray-900">评测集信息</h4>
                    <Button
                      type="link"
                      icon={<EyeOutlined />}
                      className="text-blue-600 hover:text-blue-800 font-medium"
                      onClick={handleViewDatasetDetail}
                    >
                      查看详情
                    </Button>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm text-gray-600">描述：</span>
                      <span className="ml-2 text-sm text-gray-900">{selectedDataset.description}</span>
                    </div>
                    <div>
                      <span className="text-sm text-gray-600">数据量：</span>
                      <span className="ml-2 text-sm text-gray-900">{selectedDataset.dataCount} 条</span>
                    </div>
                  </div>

                  <div className="mt-3">
                    <span className="text-sm text-gray-600">列名称：</span>
                    <div className="mt-1 flex flex-wrap gap-2">
                      {selectedDataset.columns.map((column: string) => (
                        <span
                          key={column}
                          className="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded"
                        >
                          {column}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </>
            )}
          </Card>

          {/* 步骤3：配置评测对象 */}
          <Card title="步骤3：配置评测对象" className="mb-6">
            <div className="grid grid-cols-1 md:grid-cols-1 gap-4 mb-4">
              <Form.Item
                name="objectType"
                label="对象类型"
                rules={[{ required: true, message: '请选择对象类型' }]}
              >
                <Select
                  placeholder="选择评测对象类型"
                  onChange={(value) => {
                    setSelectedObjectType(value);
                    // 当切换对象类型时，清空相关字段
                    if (value !== 'prompt') {
                      form.setFieldsValue({
                        promptKey: undefined,
                        version: undefined
                      });
                      setSelectedPrompt(null);
                      setPromptVersions([]);
                      setPromptVersionDetail(null);
                      setFieldMapping({});
                    }
                  }}
                >
                  {objectTypes.map(type => (
                    <Option key={type.value} value={type.value}>
                      {type.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              {/* 只有当选择了 prompt 类型时才显示 Prompt 相关配置 */}
              {selectedObjectType === 'prompt' && (
                <>
                  <Form.Item
                    name="promptKey"
                    label="Prompt Key"
                    rules={[{ required: true, message: '请输入或选择Prompt Key' }]}
                  >
                    <AutoComplete
                      placeholder="请输入或选择Prompt Key"
                      onChange={handlePromptKeyChange}
                      filterOption={(inputValue, option) => {
                        if (!option || !option.value) return false;
                        const value = option.value.toString().toLowerCase();
                        const input = inputValue.toLowerCase();
                        return value.indexOf(input) !== -1;
                      }}
                      notFoundContent={promptsLoading ? '加载中...' : '暂无数据'}
                    >
                      {prompts.map(prompt => (
                        <AutoComplete.Option key={prompt.promptKey} value={prompt.promptKey}>
                          {prompt.promptKey} { prompt.promptDescription ? " - " : ""} {prompt.promptDescription}
                        </AutoComplete.Option>
                      ))}
                    </AutoComplete>
                  </Form.Item>
                </>
              )}
            </div>

            {/* 只有当选择了 prompt 类型时才显示版本选择 */}
            {selectedObjectType === 'prompt' && (
              <div className="grid grid-cols-1 md:grid-cols-1 gap-4">
                <Form.Item
                  name="version"
                  label="版本"
                  rules={[{ required: true, message: '请选择版本' }]}
                >
                  <Select
                    placeholder="选择版本"
                    loading={promptVersionsLoading}
                    disabled={!selectedPrompt}
                    onChange={handlePromptVersionChange}
                    notFoundContent={promptVersionsLoading ? '加载中...' : '请先选择Prompt Key'}
                  >
                    {promptVersions.map(version => {
                      const versionStatus = version.status;
                      return (
                        <Option key={version.version} value={version.version}>
                          <span className='mr-2'>
                            {version.version} {version.versionDescription ? " - " : ""} {version.versionDescription}
                          </span>
                          <Tag color={versionStatus === "release" ? "green" : "blue"}>{version.status === "release" ? "正式版本" : "PRE版本"}</Tag>
                        </Option>
                      )
                    })}
                  </Select>
                </Form.Item>
              </div>
            )}

            {/* 只有当选择了 prompt 类型时才显示 Prompt版本详情信息卡片 */}
            {selectedObjectType === 'prompt' && promptVersionDetail && (
              <div className="bg-gray-50 rounded-lg p-4 mt-4">
                <div className="flex justify-between items-start mb-3">
                  <h4 className="text-base font-medium text-gray-900">Prompt版本详情</h4>
                  <Button
                    type="link"
                    icon={<EyeOutlined />}
                    className="text-blue-600 hover:text-blue-800 font-medium"
                    onClick={handleViewPromptDetail}
                  >
                    查看完整详情
                  </Button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>版本：</span>
                    <span className="ml-2 text-sm text-gray-900">{promptVersionDetail.version}</span>
                  </div>
                  <div>
                    <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>版本描述：</span>
                    <span className="ml-2 text-sm text-gray-900">{promptVersionDetail.versionDescription}</span>
                  </div>
                </div>

                <div className="mt-3">
                  <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>模板：</span>
                  <div className="mt-1 bg-white rounded border p-3 text-sm text-gray-900 max-h-32 overflow-y-auto">
                    {promptVersionDetail.template || '暂无模板内容'}
                  </div>
                </div>

                {promptVersionDetail.variables && (
                  <div className="mt-3">
                    <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>参数：</span>
                    <div className="mt-1 flex flex-wrap gap-2">
                      {Object.keys(JSON.parse(promptVersionDetail.variables || '{}')).map((param: string) => (
                        <span
                          key={param}
                          className="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded"
                        >
                          {param}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 只有当选择了 prompt 类型时才显示字段映射配置卡片 */}
            {selectedObjectType === 'prompt' && selectedDataset && promptVersionDetail && datasetDetail && (
              <div className="bg-gray-50 rounded-lg p-4 mt-4">
                <h4 className="text-base font-medium text-gray-900 mb-4">字段映射配置</h4>
                <div className="flex items-center justify-between mb-4">
                  <p className="text-sm text-gray-600">配置Prompt参数与评测集字段的映射关系：</p>
                  <div className="text-xs text-blue-600 bg-blue-50 px-2 py-1 rounded">
                    ✨ 系统已智能匹配默认值
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-4 items-center">
                  <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>Prompt参数：</span>
                  <div></div>
                  <span className="text-sm" style={{color: 'rgba(0, 0, 0, 0.45)'}}>映射到评测集字段：</span>
                </div>
                <div className="space-y-3">
                  {Object.keys(JSON.parse(promptVersionDetail.variables || '{}')).map((param: string) => {
                    // 解析评测集的字段列表
                    let datasetFields: string[] = [];
                    try {
                      const columnsConfig = JSON.parse(datasetDetail.columnsConfig || '[]');
                      datasetFields = Array.isArray(columnsConfig) ? columnsConfig.map((col: any) => col.name || col) : [];
                      // 确保至少有一个字段
                      if (datasetFields.length === 0) {
                        datasetFields = ['input', 'reference_output'];
                      }
                    } catch (e) {
                      datasetFields = ['input', 'reference_output']; // 默认字段
                    }

                    // 确保当前参数有映射值
                    const currentValue = fieldMapping[param] || datasetFields[0];
                    if (!fieldMapping[param] && datasetFields.length > 0) {
                      // 立即设置一个默认值
                      setTimeout(() => {
                        handleFieldMappingChange(param, datasetFields[0]);
                      }, 0);
                    }

                    return (
                      <div key={param} className="grid grid-cols-3 gap-4 items-center">
                        <div>
                          <Input
                            value={param}
                            readOnly
                            className="mt-1"
                            placeholder="Prompt参数"
                          />
                        </div>
                        <div style={{textAlign: 'center'}}>↔</div>
                        <div>
                          <Select
                            defaultActiveFirstOption
                            defaultValue={currentValue}
                            value={currentValue}
                            onChange={(value) => handleFieldMappingChange(param, value)}
                            className="mt-1 w-full"
                            showSearch
                            optionFilterProp="children"
                          >
                            {datasetFields.map(field => (
                              <Option key={field} value={field}>
                                {field}
                              </Option>
                            ))}
                          </Select>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {Object.keys(JSON.parse(promptVersionDetail.variables || '{}')).length === 0 && (
                  <div className="text-center py-4 text-gray-500">
                    该Prompt版本暂无参数需要映射
                  </div>
                )}

                {/* 字段映射说明 */}
                <div className="mt-6 bg-blue-50 rounded-lg p-4">
                  <div className="flex items-start">
                    <span className="mr-2 text-blue-500 text-xl">💡</span>
                    <div>
                      <div className="text-base font-medium text-gray-900 mb-2">字段映射说明：</div>
                      <ul className="list-disc pl-5 space-y-1 text-sm text-gray-600">
                        <li>系统已自动检测Prompt中的参数变量并尝试智能匹配评测集字段</li>
                        <li>请确认每个映射关系是否正确，确保实验执行时数据能正确传递</li>
                        <li>映射关系将在实验执行时用于将评测集数据填充到Prompt参数中</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </Card>

          {/* 步骤4：配置评估器 */}
          <Card title="步骤4：配置评估器" className="mb-6">
            <p className="text-gray-600 mb-4">选个添加评估器，每个评估器需要选择版本并配置字段映射</p>

            {/* 添加评估器按钮 */}
            <div className="text-center border-2 border-dashed rounded-lg p-4 mb-6 cursor-pointer hover:bg-gray-50"
              onClick={handleAddEvaluator}>
              <PlusOutlined className="text-xl text-gray-500" />
              <div className="mt-2 text-gray-600">添加评估器</div>
            </div>

            {/* 已配置的评估器列表 */}
            {selectedEvaluators.length > 0 && (
              <div className="mt-4">
                <h4 className="text-base font-medium text-gray-900 mb-4">已配置的评估器：</h4>
                <div className="space-y-6">
                  {selectedEvaluators.map((evaluator, index) => {
                    // 获取当前评估器的ID
                    const evaluatorId = evaluator.evaluatorId;
                    // 使用索引和评估器ID组合作为key，确保每个实例独立
                    const instanceKey = `${index}-${evaluatorId}`;
                    // 判断是否处于映射模式（评估器和版本都已选择，且配置模式为false）
                    const isMappingMode = evaluatorId &&
                                              selectedEvaluatorVersions[instanceKey] &&
                                              !evaluatorConfigMode[instanceKey];

                    return (
                      <div key={index} className="bg-gray-50 rounded-lg p-4 relative">
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <div style={{ display: 'flex' }}>
                            <h5 className="text-base font-medium text-gray-900 mb-4 mr-4">
                              评估器 {index + 1}
                            </h5>
                            {isMappingMode && <>
                              <h5 className="text-base font-medium text-blue-500 mb-4 mr-4">
                                {evaluators.find(e => e.id.toString() === evaluatorId)?.name}
                              </h5>
                              <h5 className="text-base font-medium text-blue-500 mb-4">
                                {evaluatorVersions[evaluatorId]?.find((v: any) => v.id.toString() === selectedEvaluatorVersions[instanceKey])?.version}
                              </h5>
                            </>}
                          </div>
                          <div className="top-2 right-2">
                            <Button
                              type="text"
                              danger
                              icon={<DeleteOutlined />}
                              onClick={() => handleRemoveEvaluator(index)}
                            >
                              移除
                            </Button>
                          </div>
                        </div>

                        {isMappingMode ? (
                          // 映射模式：显示字段映射配置
                          <div>
                            {/* 查看详情、Prompt详情按钮、重新配置按钮 */}
                            <div className="flex mb-4">
                              <div
                                className="flex items-center text-blue-500 cursor-pointer mr-6"
                                style={{marginRight: '6px'}}
                                onClick={() => handleViewEvaluatorDetail(evaluatorId)}
                              >
                                <EyeOutlined className="mr-1" /> 查看详情
                              </div>
                              <div
                                className="text-blue-500 cursor-pointer mr-6"
                                style={{marginRight: '6px'}}
                                onClick={handleViewPromptDetail}
                              >
                                Prompt详情
                              </div>
                              <div
                                className="text-blue-500 cursor-pointer"
                                onClick={() => {
                                  // 使用实例key来切换配置模式
                                  setEvaluatorConfigMode(prev => ({
                                    ...prev,
                                    [instanceKey]: true
                                  }));

                                  // 清空当前评估器实例的版本选择
                                  setSelectedEvaluatorVersions(prev => ({
                                    ...prev,
                                    [instanceKey]: ''
                                  }));
                                }}
                              >
                                重新配置
                              </div>
                            </div>

                            {/* 评估器信息展示 */}
                            <div className="bg-white rounded-lg p-4 mb-4">
                              <div className="grid grid-cols-1 gap-2">
                                <div>
                                  <span className="text-gray-600">描述：</span>
                                  <span className="ml-2 text-gray-900">
                                    {evaluators.find(e => e.id.toString() === evaluatorId)?.description || '-'}
                                  </span>
                                </div>
                                <div>
                                  <span className="text-gray-600">模型：</span>
                                  <span className="ml-2">
                                    <span className="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded">
                                      {evaluators.find(e => e.id.toString() === evaluatorId)?.modelName || '-'}
                                    </span>
                                  </span>
                                </div>
                              </div>
                            </div>

                            {/* 字段映射配置 */}
                            <div className="bg-white rounded-lg p-4">
                              <h4 className="text-base font-medium text-gray-900 mb-4">字段映射配置</h4>
                              <div className="space-y-3">
                                {/* 映射表头 */}
                                <div className="grid grid-cols-3 gap-4 font-medium text-gray-700">
                                  <div>评估器参数</div>
                                  <div style={{textAlign: 'center'}}>映射关系</div>
                                  <div>数据来源</div>
                                </div>

                                {/* 映射项 */}
                                {evaluatorParams[evaluatorId] && evaluatorParams[evaluatorId].length > 0 ? (
                                  evaluatorParams[evaluatorId].map((param) => {
                                    // 获取数据来源字段
                                    const dataSourceFields = getDataSourceFields();
                                    // 获取当前映射值
                                    const currentMapping = evaluatorParamMappings[evaluatorId]?.[param] || dataSourceFields[0]?.field || 'input';
                                    // 查找当前选中的数据源字段对象
                                    const currentDataSourceField = dataSourceFields.find(item => item.field === currentMapping);

                                    return (
                                      <div key={param} className="grid grid-cols-3 gap-4 items-center">
                                        <div>
                                          <Input
                                            value={param}
                                            readOnly
                                            className="w-full"
                                          />
                                        </div>
                                        <div className="text-center">
                                          <span className="text-gray-500">↔</span>
                                        </div>
                                        <div>
                                          <Select
                                            defaultValue={currentMapping}
                                            value={currentMapping}
                                            onChange={(value) => handleEvaluatorParamMappingChange(evaluatorId, param, value)}
                                            className="w-full"
                                            showSearch
                                            optionFilterProp="children"
                                          >
                                            {dataSourceFields.map(field => (
                                              <Option key={field.field} value={field.field}>
                                                {field.displayName}
                                              </Option>
                                            ))}
                                          </Select>
                                        </div>
                                      </div>
                                    );
                                  })
                                ) : (
                                  <div className="text-center py-4 text-gray-500">
                                    该评估器暂无参数需要映射
                                  </div>
                                )}
                              </div>
                            </div>

                            {/* 字段映射说明 */}
                            <div className="mt-6 bg-blue-50 rounded-lg p-4">
                              <div className="flex items-start">
                                <span className="mr-2 text-blue-500 text-xl">💡</span>
                                <div>
                                  <div className="text-base font-medium text-gray-900 mb-2">映射说明：</div>
                                  <ul className="list-disc pl-5 space-y-1 text-sm text-gray-600">
                                    <li>评估器参数映射到相应的数据来源字段</li>
                                    <li>可选的数据来源包含评测集字段和评测对象输出（actual_output）</li>
                                    <li>确保每个映射关系是合理的，以便正确评估</li>
                                  </ul>
                                </div>
                              </div>
                            </div>

                          </div>
                        ) : (
                          // 配置模式：显示评估器和版本选择
                          <div>
                            <div className="grid grid-cols-2 gap-4">
                              {/* 选择评估器 */}
                              <div>
                                <div className="mb-1 text-sm" style={{color: 'rgba(0, 0, 0, 0.85)'}}>
                                  <span className="text-red-500">*</span> 选择评估器
                                </div>
                                <Select
                                  className="w-full"
                                  placeholder="选择评估器"
                                  value={evaluator.evaluatorId || undefined}
                                  onChange={(value) => {
                                    // 更新评估器ID
                                    const newEvaluators = [...selectedEvaluators];
                                    newEvaluators[index] = { ...newEvaluators[index], evaluatorId: value };
                                    setSelectedEvaluators(newEvaluators);

                                    // 获取该评估器的版本列表
                                    handleEvaluatorSelectChange(index, value);
                                  }}
                                  loading={evaluatorsLoading}
                                  notFoundContent={evaluatorsLoading ? '加载中...' : '暂无数据'}
                                >
                                  {evaluators.map(e => (
                                    <Option key={e.id} value={e.id.toString()}>
                                      {e.name}
                                    </Option>
                                  ))}
                                </Select>
                              </div>

                              {/* 选择版本 */}
                              <div>
                                <div className="mb-1 text-sm" style={{color: 'rgba(0, 0, 0, 0.85)'}}>
                                  <span className="text-red-500">*</span> 选择版本
                                </div>
                                <Select
                                  className="w-full"
                                  placeholder="选择版本"
                                  value={evaluator.evaluatorId ? selectedEvaluatorVersions[instanceKey] : undefined}
                                  onChange={(value) => handleEvaluatorVersionChange(index, evaluator.evaluatorId, value)}
                                  disabled={!evaluator.evaluatorId}
                                  loading={evaluator.evaluatorId ? evaluatorVersionsLoading[evaluator.evaluatorId] : false}
                                  notFoundContent={
                                    !evaluator.evaluatorId ? '请先选择评估器' :
                                    evaluatorVersionsLoading[evaluator.evaluatorId] ? '加载中...' :
                                    evaluatorVersions[evaluator.evaluatorId]?.length === 0 ? '暂无版本数据' : '请选择版本'
                                  }
                                >
                                    {evaluator.evaluatorId && evaluatorVersions[evaluator.evaluatorId]?.map((version: any) => {
                                      console.log(version, 'zxc...')
                                      return (
                                        <Option key={version.id} value={version.id.toString()}>
                                          {version.version} - {version.description}
                                        </Option>
                                      )
                                    })}
                                </Select>
                              </div>
                            </div>

                            {/* 查看详情按钮 */}
                            {evaluator.evaluatorId && (
                              <div className="mt-4 flex justify-end">
                                <div
                                  className="flex items-center text-blue-500 cursor-pointer"
                                  onClick={() => handleViewEvaluatorDetail(evaluator.evaluatorId)}
                                >
                                  <EyeOutlined className="mr-1" /> 查看详情
                                </div>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </Card>
        </Form>
      </div>

      {/* 底部操作按钮 - 固定在底部 */}
      <div className="experiment-create-footer">
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
            发起实验
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ExperimentCreate;
