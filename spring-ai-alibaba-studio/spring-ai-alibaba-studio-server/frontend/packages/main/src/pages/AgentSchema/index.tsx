import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { Button, Form, Input, Select, Card, message, Space, Typography, List, Checkbox, Tooltip, Divider } from 'antd';
import { PlusOutlined, CopyOutlined, SaveOutlined, CloseOutlined, DownOutlined, UpOutlined, DeleteOutlined } from '@ant-design/icons';
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';
import { AgentSchemaService } from '@/services/agentSchema';
import { ToolService } from '@/services/tool';
import { listModels, getModelSelector } from '@/services/modelService';
import { IAgentSchema, AgentType as IAgentType } from '@/types/agentSchema';
import { ITool } from '@/types/tool';
import { IModel } from '@/types/modelService';
import { session } from '@/request/session';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

// Agent类型定义
type LocalAgentType = 'ReactAgent' | 'ParallelAgent' | 'SequentialAgent' | 'LLMRoutingAgent' | 'LoopAgent';

// State策略类型
type StateStrategy = 'replace' | 'append';

// 模型配置
interface ModelConfig {
  name: string;
  url: string;
  'api-key': string;
}

// 通用handle配置
interface CommonHandleConfig {
  chat_options?: Record<string, any>;
  compile_config?: Record<string, any>;
  state?: {
    strategies: Record<string, StateStrategy>;
  };
}

// ReactAgent特有配置
interface ReactAgentHandleConfig extends CommonHandleConfig {
  model?: ModelConfig;
  max_iterations?: number;
  tools?: string[];
  resolver?: string;
  pre_llm_hook?: string;
  post_llm_hook?: string;
  pre_tool_hook?: string;
  post_tool_hook?: string;
  should_continue_func?: string;
}

// ParallelAgent特有配置
interface ParallelAgentHandleConfig extends CommonHandleConfig {
  merge_strategy?: string;
  max_concurrency?: number;
  separator?: string;
}

// LoopAgent特有配置
interface LoopAgentHandleConfig extends CommonHandleConfig {
  loop_mode: 'COUNT' | 'CONDITION';
  loop_count?: number;
}

// Handle配置联合类型
type HandleConfig =
  | ReactAgentHandleConfig
  | ParallelAgentHandleConfig
  | LoopAgentHandleConfig
  | CommonHandleConfig;

// 子代理引用方式
interface SubAgentDirectRef {
  agent: AgentSchema;
}

interface SubAgentFileRef {
  config_path: string;
}

interface SubAgentCodeRef {
  code: string;
}

type SubAgentRef = SubAgentDirectRef | SubAgentFileRef | SubAgentCodeRef;

// 主Agent Schema接口
interface AgentSchema {
  type: LocalAgentType;
  name: string;
  description: string;
  instruction: string;
  input_keys: string[];
  output_key: string;
  handle: HandleConfig;
  sub_agents?: SubAgentRef[];
}

// 表单数据接口
interface AgentSchemaForm {
  agentType: LocalAgentType;
  name: string;
  description: string;
  instruction: string;
  inputKey: string;
  outputKey: string;
  model: string;
  handle: any;
  subAgents?: string[];
  tools?: string[];
}

// 保存的Agent数据接口
interface SavedAgent {
  id: string;
  schema: AgentSchema;
}

const AgentSchemaCreator: React.FC = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);
  const [yamlContent, setYamlContent] = useState<string>('');
  const [savedAgents, setSavedAgents] = useState<IAgentSchema[]>([]);
  const [availableTools, setAvailableTools] = useState<ITool[]>([]);
  const [availableModels, setAvailableModels] = useState<IModel[]>([]);
  const [agentsLoading, setAgentsLoading] = useState(false);
  const [toolsLoading, setToolsLoading] = useState(false);
  const [modelsLoading, setModelsLoading] = useState(false);
  const [subAgentsExpanded, setSubAgentsExpanded] = useState(false);
  const [toolsExpanded, setToolsExpanded] = useState(false);
  const [nameError, setNameError] = useState<string | null>(null);

  // 防抖的名称校验
  const nameCheckTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // 防抖检查名称重复
  const checkNameDuplicate = useCallback((name: string) => {
    if (!name || savedAgents.length === 0) {
      setNameError(null);
      return;
    }

    // 清除之前的定时器
    if (nameCheckTimeoutRef.current) {
      clearTimeout(nameCheckTimeoutRef.current);
    }

    // 设置新的定时器（500ms防抖）
    nameCheckTimeoutRef.current = setTimeout(() => {
      const isDuplicate = savedAgents.some(agent => {
        const currentAgentId = selectedAgentId?.toString();
        const existingAgentId = agent.id?.toString();
        return agent.name === name && existingAgentId !== currentAgentId;
      });

      if (isDuplicate) {
        setNameError('智能体名称已存在，请使用其他名称');
      } else {
        setNameError(null);
      }
    }, 500);
  }, [savedAgents, selectedAgentId]);

  // 获取已保存的智能体列表
  const fetchSavedAgents = async () => {
    setAgentsLoading(true);
    try {
      const response = await AgentSchemaService.getAgentSchemas();

      let agents: any[] = [];

      // 检查响应格式 - 后端返回 Result<List<AgentSchemaEntity>>
      if (Array.isArray(response)) {
        // 如果直接是数组，直接使用
        agents = response;
      } else if (response && typeof response === 'object') {
        // 检查是否是包装的响应对象
        if ((response as any).data && Array.isArray((response as any).data)) {
          // 标准的 Result<T> 格式
          agents = (response as any).data;
        } else if ((response as any).records && Array.isArray((response as any).records)) {
          // 分页响应格式
          agents = (response as any).records;
        } else {
        }
      } else {
      }


      setSavedAgents(agents);
    } catch (error) {
      message.error('获取智能体列表失败');
      console.error('Failed to fetch agents:', error);
      setSavedAgents([]); // 出错时设置为空数组
    } finally {
      setAgentsLoading(false);
    }
  };

  // 获取可用工具列表
  const fetchAvailableTools = async () => {
    setToolsLoading(true);
    try {
      const tools = await ToolService.getTools();
      // 确保tools是数组
      if (Array.isArray(tools)) {
        setAvailableTools(tools);
      } else {
        setAvailableTools([]);
      }
    } catch (error) {
      message.error('获取工具列表失败');
      setAvailableTools([]); // 出错时设置为空数组
    } finally {
      setToolsLoading(false);
    }
  };

  // 获取可用模型列表
  const fetchAvailableModels = async () => {
    setModelsLoading(true);
    try {
      // 使用 getModelSelector 获取所有启用的提供商的模型
      const modelSelectorData = await getModelSelector('llm');


      // 提取所有模型
      const allModels: IModel[] = [];
      if (modelSelectorData.data) {
        modelSelectorData.data.forEach(providerGroup => {
          if (providerGroup.models) {
            allModels.push(...providerGroup.models);
          }
        });
      }

      setAvailableModels(allModels);
    } catch (error) {
      message.error('获取模型列表失败');
      setAvailableModels([]);
    } finally {
      setModelsLoading(false);
    }
  };

  // 初始化加载数据
  useEffect(() => {
    fetchSavedAgents();
    fetchAvailableTools();
    fetchAvailableModels();
  }, []);

  // 当savedAgents加载完成时，重新处理当前选中agent的subAgents
  useEffect(() => {
    if (selectedAgentId && savedAgents.length > 0) {
      const currentAgent = savedAgents.find(agent => agent.id === selectedAgentId);
      if (currentAgent && currentAgent.subAgents) {
        try {
          const subAgentsData = JSON.parse(currentAgent.subAgents);
          if (Array.isArray(subAgentsData)) {
            const subAgentsNames = subAgentsData.map(subAgent => {
              // 如果是 { agent: { id, name } } 格式，优先使用名称
              if (subAgent.agent) {
                if (subAgent.agent.name) {
                  return subAgent.agent.name;
                } else if (subAgent.agent.id) {
                  // 如果只有ID，查找对应的名称
                  const foundAgent = savedAgents.find(sa => sa.id === subAgent.agent.id);
                  return foundAgent ? foundAgent.name : subAgent.agent.id.toString();
                }
              }
              return subAgent.toString();
            }).filter(name => name && name !== 'undefined' && name !== 'null');

            if (subAgentsNames.length > 0) {
              form.setFieldsValue({ subAgents: subAgentsNames });
            }
          }
        } catch (error) {
        }
      }
    }
  }, [savedAgents, selectedAgentId, form]);

  // Agent类型选项
  const agentTypeOptions = [
    { label: 'React Agent', value: 'ReactAgent' },
    { label: 'Parallel Agent', value: 'ParallelAgent' },
    { label: 'Sequential Agent', value: 'SequentialAgent' },
    { label: 'LLM Routing Agent', value: 'LLMRoutingAgent' },
    { label: 'Loop Agent', value: 'LoopAgent' },
  ];

  // State策略选项
  const stateStrategyOptions = [
    { label: 'Replace', value: 'replace' },
    { label: 'Append', value: 'append' },
  ];

  // Loop模式选项
  const loopModeOptions = [
    { label: 'Count-based', value: 'COUNT' },
    { label: 'Condition-based', value: 'CONDITION' },
  ];

  // 子代理引用类型选项
  const subAgentRefTypeOptions = [
    { label: 'Direct Embed', value: 'direct' },
    { label: 'File Reference', value: 'file' },
    { label: 'Code Reference', value: 'code' },
  ];

  // 工具选项 - 从API获取
  const toolOptions = Array.isArray(availableTools) ? availableTools.map(tool => ({
    label: tool.name,
    value: tool.toolId || tool.id?.toString() || tool.name,
  })) : [];

  // 子代理选项 - 从已保存的智能体获取，过滤掉当前选中的agent
  const subAgentOptions = Array.isArray(savedAgents) ? savedAgents.filter(agent => {
    // 过滤掉当前选中的agent，防止自引用
    return agent.id !== selectedAgentId;
  }).map(agent => ({
    label: agent.name,
    value: agent.name, // 使用名称作为值
    id: agent.id?.toString() // 保存ID以备后用
  })) : [];

  // 生成 YAML 内容
  const generateYaml = async (values: AgentSchemaForm): Promise<string> => {

    const instruction = values.instruction || '';
    const inputKeys = values.inputKey ? [values.inputKey] : ['input'];
    const subAgents = values.subAgents || [];

    // 生成handle配置
    const generateHandleYaml = (handle: any, formValues: AgentSchemaForm): string => {
      if (!handle) {
        return 'handle:\n  state:\n    strategies:\n      input: "replace"\n      output: "replace"\n';
      }

      let yaml = 'handle:\n';

      // 通用配置
      if (handle.chat_options && Object.keys(handle.chat_options).length > 0) {
        yaml += `  chat_options:\n`;
        Object.entries(handle.chat_options).forEach(([key, value]) => {
          yaml += `    ${key}: ${typeof value === 'string' ? `"${value}"` : value}\n`;
        });
      }

      if (handle.compile_config && Object.keys(handle.compile_config).length > 0) {
        yaml += `  compile_config:\n`;
        Object.entries(handle.compile_config).forEach(([key, value]) => {
          yaml += `    ${key}: ${typeof value === 'string' ? `"${value}"` : value}\n`;
        });
      }

      if (handle.state?.strategies && Object.keys(handle.state.strategies).length > 0) {
        yaml += `  state:\n`;
        yaml += `    strategies:\n`;
        Object.entries(handle.state.strategies).forEach(([key, strategy]) => {
          yaml += `      ${key}: "${strategy}"\n`;
        });
      }

      // ReactAgent特有配置 - 优先使用form中的model值，回退到handle中的模型
      const modelName = formValues.model || (handle.model ? handle.model.name : null);

      // 如果有model信息，生成model配置
      if (modelName || handle.model) {
        const finalModelName = modelName || handle.model?.name || 'qwen2.5-72b-instruct';

        yaml += `  model:\n`;
        yaml += `    name: "${finalModelName}"\n`;

        // 如果handle中有完整的模型配置，优先使用handle中的配置
        if (handle.model && handle.model.url) {
          yaml += `    url: "${handle.model.url}"\n`;
          yaml += `    api-key: "${handle.model['api-key'] || 'your-api-key'}"\n`;
        } else {
          // 尝试从动态模型列表中查找选中的模型信息
          const selectedModel = availableModels.find(m =>
            m.model_id === finalModelName ||
            m.name === finalModelName ||
            m.model_id === formValues.model ||
            m.name === formValues.model
          );
          if (selectedModel) {
            // 根据模型提供商设置不同的默认URL
            const defaultUrl = selectedModel.provider === 'Tongyi'
              ? 'https://dashscope.aliyuncs.com/api/v1'
              : selectedModel.provider === 'OpenAI'
              ? 'https://api.openai.com/v1'
              : 'https://api.example.com/v1';
            yaml += `    url: "${defaultUrl}"\n`;
            yaml += `    api-key: "your-api-key"\n`;
          } else {
            // 使用handle中的URL，或者默认值
            const defaultUrl = handle.model?.url || 'https://api.example.com/v1';
            const defaultApiKey = handle.model?.['api-key'] || 'your-api-key';
            yaml += `    url: "${defaultUrl}"\n`;
            yaml += `    api-key: "${defaultApiKey}"\n`;
          }
        }
      }

      if ('max_iterations' in handle && handle.max_iterations) {
        yaml += `  max_iterations: ${handle.max_iterations}\n`;
      }

      // 工具配置 - 优先使用form中的tools值
      const toolsList = formValues.tools || handle.tools || [];
      if (toolsList.length > 0) {
        yaml += `  tools:\n`;
        toolsList.forEach((tool: string) => {
          yaml += `    - "${tool}"\n`;
        });
      }

      // ParallelAgent特有配置
      if ('merge_strategy' in handle && handle.merge_strategy) {
        yaml += `  merge_strategy: "${handle.merge_strategy}"\n`;
      }

      if ('max_concurrency' in handle && handle.max_concurrency) {
        yaml += `  max_concurrency: ${handle.max_concurrency}\n`;
      }

      if ('separator' in handle && handle.separator) {
        yaml += `  separator: "${handle.separator}"\n`;
      }

      // LoopAgent特有配置
      if ('loop_mode' in handle && handle.loop_mode) {
        yaml += `  loop_mode: "${handle.loop_mode}"\n`;
      }

      if ('loop_count' in handle && handle.loop_count) {
        yaml += `  loop_count: ${handle.loop_count}\n`;
      }

      // 其他钩子函数配置
      const hookFields = ['resolver', 'pre_llm_hook', 'post_llm_hook', 'pre_tool_hook', 'post_tool_hook', 'should_continue_func'];
      hookFields.forEach(field => {
        if (field in handle && handle[field]) {
          yaml += `  ${field}: "${handle[field]}"\n`;
        }
      });

      return yaml;
    };

    // 生成子代理配置
    const generateSubAgentsYaml = (subAgents: string[]): string => {
      if (!subAgents || !Array.isArray(subAgents) || subAgents.length === 0) return '';

      // 过滤掉无效的名称
      const validAgentNames = subAgents.filter(name =>
        name &&
        typeof name === 'string' &&
        name.trim() !== '' &&
        name !== 'undefined' &&
        name !== 'null'
      );

      if (validAgentNames.length === 0) return '';

      let yaml = 'sub_agents:\n';

      validAgentNames.forEach(agentName => {
        // 直接使用名称，因为表单现在存储的是名称
        yaml += `  - agent: ${agentName.trim()}\n`;
      });

      return yaml;
    };

    const handleYaml = values.handle ? generateHandleYaml(values.handle, values) : 'handle:\n  state:\n    strategies:\n      input: "replace"\n      output: "replace"\n';
    const subAgentsYaml = subAgents.length > 0 ? generateSubAgentsYaml(subAgents) : '';
    const yaml = `agent:\n  type: "${values.agentType || 'ReactAgent'}"\n  name: "${values.name || ''}"\n  description: "${values.description || ''}"\n  instruction: |\n    ${instruction.replace(/\n/g, '\n    ')}\n  input_keys:\n${inputKeys.length > 0 ? inputKeys.map(key => `    - "${key}"`).join('\n') : '    - "input"'}\n  output_key: "${values.outputKey || 'output'}"\n${handleYaml}${subAgentsYaml}`;

    return yaml;
  };

  // 监听表单变化，实时更新 YAML
  useEffect(() => {
    const updateYaml = async () => {
      const subscription = form.getFieldsValue() as AgentSchemaForm;
      if (subscription.name) {
        const yaml = await generateYaml(subscription);
        setYamlContent(yaml);
      }
    };
    updateYaml();
  }, [form, selectedAgentId, availableModels]);

  // 选择智能体
  const handleSelectAgent = async (agent: IAgentSchema) => {

    setSelectedAgentId(agent.id || null);

    // 将后端数据转换为前端表单格式
    let subAgentsNames: string[] = [];

    // 同步处理subAgents，确保在设置表单值时已经完成转换
    if (agent.subAgents) {

      try {
        let subAgentsData;

        // 检查subAgents是否已经是对象或数组
        if (typeof agent.subAgents === 'string') {
          // 如果是JSON字符串，解析它
          subAgentsData = JSON.parse(agent.subAgents);
        } else if (Array.isArray(agent.subAgents)) {
          // 如果已经是数组，直接使用
          subAgentsData = agent.subAgents;
        } else {
          subAgentsData = [];
        }


        if (Array.isArray(subAgentsData)) {
          subAgentsNames = subAgentsData.map(subAgent => {
            // 如果是 { agent: { id, name } } 格式，优先使用名称
            if (subAgent.agent) {
              if (subAgent.agent.name) {
                return subAgent.agent.name;
              } else if (subAgent.agent.id && savedAgents.length > 0) {
                // 如果有ID且savedAgents已加载，查找对应的名称
                const foundAgent = savedAgents.find(sa => sa.id === subAgent.agent.id);
                return foundAgent ? foundAgent.name : subAgent.agent.id.toString();
              }
            }
            // 如果直接是名称，直接返回
            return subAgent.toString();
          }).filter(name => name && name !== 'undefined' && name !== 'null');
        }

      } catch (error) {
      }
    } else {
    }

    // 确保subAgents名称数组有效

    // 如果subAgents解析失败且savedAgents未完全加载，设置一个回调来重新处理
    const needsRetry = subAgentsNames.length === 0 && agent.subAgents && savedAgents.length === 0;
    if (needsRetry) {
    }

    // 解析handle配置，提取模型信息
    let modelName = 'qwen2.5-72b-instruct'; // 默认模型
    let tools: string[] = [];

    try {
      const handleData = JSON.parse(agent.handle || '{}');

      if (handleData.model && handleData.model.name) {
        modelName = handleData.model.name;
        // 检查模型是否存在于当前的模型列表中，如果不存在，添加到列表中
        const modelExists = availableModels.some(m => m.model_id === modelName || m.name === modelName);
        if (!modelExists && modelName) {
          // 如果模型不在当前列表中，添加一个临时模型项
          const tempModel: IModel = {
            model_id: modelName,
            name: modelName,
            provider: 'unknown',
            type: 'llm'
          };
          setAvailableModels(prev => [...prev, tempModel]);
        }
      } else {
      }
      if (handleData.tools && Array.isArray(handleData.tools)) {
        tools = handleData.tools;
      }
    } catch (error) {
    }

    // 构建完整的handle配置，确保包含model信息
    let parsedHandle: any = {};
    try {
      parsedHandle = JSON.parse(agent.handle || '{}');
    } catch (error) {
    }

    const completeHandle = {
      ...parsedHandle,
      state: parsedHandle.state || {
        strategies: {
          input: 'replace',
          output: 'replace'
        }
      },
      model: {
        name: modelName,
        url: parsedHandle.model?.url || 'https://api.example.com/v1',
        'api-key': parsedHandle.model?.['api-key'] || 'your-api-key'
      },
      tools: tools || []
    };

    // 解析inputKeys
    let inputKey = 'input';
    if (agent.inputKeys) {
      try {
        let inputKeysArray: string[];
        if (typeof agent.inputKeys === 'string') {
          // 如果是JSON字符串，解析它
          inputKeysArray = JSON.parse(agent.inputKeys);
        } else if (Array.isArray(agent.inputKeys)) {
          // 如果已经是数组，直接使用
          inputKeysArray = agent.inputKeys;
        } else {
          inputKeysArray = ['input'];
        }

        if (Array.isArray(inputKeysArray) && inputKeysArray.length > 0) {
          inputKey = inputKeysArray[0];
        }
      } catch (error) {
        // 解析失败时使用默认值
      }
    }

    const formData = {
      name: agent.name,
      description: agent.description || '',
      agentType: agent.type,
      instruction: agent.instruction,
      inputKey: inputKey,
      outputKey: agent.outputKey || 'output',
      model: modelName,
      handle: completeHandle,
      subAgents: subAgentsNames,
      tools: tools,
    };


    // 设置表单值
    form.setFieldsValue(formData);

    // 等待表单更新完成后生成YAML，确保subAgents值已正确设置
    setTimeout(async () => {
      try {
        // 重新获取表单值，确保包含最新的subAgents和完整的handle
        const updatedFormValues = form.getFieldsValue();

        // 确保handle包含完整的model信息
        if (updatedFormValues.handle && !updatedFormValues.handle.model) {
          updatedFormValues.handle.model = {
            name: updatedFormValues.model || modelName,
            url: 'https://api.example.com/v1',
            'api-key': 'your-api-key'
          };
        }

        // 确保 subAgents 数据存在
        if (!updatedFormValues.subAgents || updatedFormValues.subAgents.length === 0) {
          // 如果表单中没有subAgents，但原始数据中有，尝试恢复
          if (subAgentsNames && subAgentsNames.length > 0) {
            updatedFormValues.subAgents = subAgentsNames;
          }
        }

        const yaml = await generateYaml(updatedFormValues as AgentSchemaForm);
        setYamlContent(yaml);
      } catch (error) {
      }
    }, 100); // 给表单更新留出时间
  };

  // 创建新智能体
  const handleNewAgent = () => {
    setSelectedAgentId(null);
    form.resetFields();
    setYamlContent('');
  };

  // 删除智能体
  const handleDeleteAgent = async (agent: IAgentSchema, e: React.MouseEvent) => {
    e.stopPropagation(); // 阻止事件冒泡，避免触发选择

    if (!agent.id) {
      message.error('无法删除：智能体ID无效');
      return;
    }

    // 显示确认对话框
    const confirmed = window.confirm(`确定要删除智能体 "${agent.name}" 吗？此操作不可撤销。`);

    if (!confirmed) {
      return;
    }

    try {
      await AgentSchemaService.deleteAgentSchema(agent.id);

      message.success(`智能体 "${agent.name}" 删除成功`);

      // 如果删除的是当前选中的智能体，清空表单
      if (selectedAgentId === agent.id) {
        setSelectedAgentId(null);
        form.resetFields();
        setYamlContent('');
      }

      // 重新加载智能体列表
      await fetchSavedAgents();

    } catch (error) {
      message.error(`删除智能体 "${agent.name}" 失败`);
    }
  };

  // 保存智能体
  const handleSaveAgent = async () => {
    setLoading(true);
    try {
      const rawValues = form.getFieldsValue();

      // 确保所有必需字段都有值
      const values: AgentSchemaForm = {
        agentType: rawValues.agentType || 'ReactAgent',
        name: rawValues.name || '',
        description: rawValues.description || '',
        instruction: rawValues.instruction || '',
        inputKey: rawValues.inputKey || 'input',
        outputKey: rawValues.outputKey || 'output',
        model: rawValues.model || 'qwen2.5-72b-instruct',
        handle: rawValues.handle || {
          state: {
            strategies: {
              input: 'replace',
              output: 'replace'
            }
          }
        },
        subAgents: rawValues.subAgents || [],
        tools: rawValues.tools || []
      };

      // 手动验证必填字段
      if (!values.name) {
        message.error('请输入智能体名称');
        return;
      }
      if (!values.instruction) {
        message.error('请输入系统提示词');
        return;
      }

      // 校验agentName重复

      // 使用已加载的savedAgents进行校验，避免重复API调用
      if (savedAgents.length > 0) {

        const isDuplicate = savedAgents.some(agent => {
          // 确保类型匹配：将ID都转换为字符串比较
          const currentAgentId = selectedAgentId?.toString();
          const existingAgentId = agent.id?.toString();


          return agent.name === values.name && existingAgentId !== currentAgentId;
        });


        if (isDuplicate) {
          message.error('智能体名称已存在，请使用其他名称');
          setLoading(false); // 重置loading状态
          return;
        }
      } else {
        // 如果没有加载到智能体列表，暂时跳过前端校验，依赖后端校验
      }

      // 检查自引用
      if (values.subAgents && values.subAgents.length > 0) {
        const currentAgentName = values.name;
        if (values.subAgents.includes(currentAgentName)) {
          message.error('不能将当前智能体设置为自身的子智能体');
          setLoading(false);
          return;
        }
      }

      let yaml: string;
      try {
        yaml = await generateYaml(values);
      } catch (yamlError) {
        message.error('YAML生成失败');
        return;
      }

      // 构建保存到后端的数据格式 - 保留现有handle配置
      let existingHandle: any = {};
      try {
        // 尝试获取当前选中的agent的现有handle配置
        if (selectedAgentId) {
          const selectedAgent = savedAgents.find(agent => agent.id === selectedAgentId);
          if (selectedAgent && selectedAgent.handle) {
            existingHandle = JSON.parse(selectedAgent.handle);
          }
        }
      } catch (error) {
      }

      // 合并配置：保留现有配置，更新模型和工具
      const updatedHandle = {
        ...existingHandle,
        state: existingHandle.state || {
          strategies: {
            input: 'replace',
            output: 'replace'
          }
        },
        model: {
          name: values.model,
          url: existingHandle.model?.url || 'https://api.example.com/v1',
          'api-key': existingHandle.model?.['api-key'] || 'your-api-key'
        },
        tools: values.tools || []
      };

      const agentData: any = {
        name: values.name,
        description: values.description,
        type: values.agentType || 'ReactAgent',
        instruction: values.instruction,
        inputKeys: JSON.stringify(values.inputKey ? [values.inputKey] : ['input']), // 转换为JSON字符串
        outputKey: values.outputKey || 'output',
        handle: JSON.stringify(updatedHandle),
        subAgents: (values.subAgents && values.subAgents.length > 0) ? JSON.stringify(values.subAgents.map((agentName: string) => {
          // 根据名称查找对应的ID
          const agent = savedAgents.find(sa => sa.name === agentName);
          if (agent && agent.id) {
            return {
              agent: {
                id: agent.id,
                name: agentName // 同时保存名称，便于YAML生成和显示
              }
            };
          } else {
            // 如果找不到对应的代理，记录警告并使用名称作为后备
            return {
              agent: {
                name: agentName // 使用名称作为后备
              }
            };
          }
        })) : null,
        yamlSchema: yaml,
      };


      // 检查认证状态
      const token = await session.asyncGet();

      if (selectedAgentId) {
        const updatedAgent = await AgentSchemaService.updateAgentSchema(selectedAgentId, agentData);
        message.success('Agent Schema 更新成功！');
      } else {
        try {
          const createdAgent = await AgentSchemaService.createAgentSchema(agentData);
          message.success('Agent Schema 创建成功！');
        } catch (apiError) {
          // 不要在这里重新抛出错误，让外层的catch处理
          throw apiError;
        }
      }

      await fetchSavedAgents();

    } catch (error) {
      message.error('保存失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 复制 YAML
  const handleCopyYaml = () => {
    navigator.clipboard.writeText(yamlContent);
    message.success('YAML 已复制到剪贴板');
  };

  // 下载 YAML
  const handleDownloadYaml = async () => {
    const values = form.getFieldsValue();
    const yaml = await generateYaml(values);
    const blob = new Blob([yaml], { type: 'text/yaml' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${values.name || 'agent'}-schema.yaml`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const handleGoBack = () => {
    navigate('/');
  };

  // 自定义选择器组件
  const CustomSelector: React.FC<{
    options: Array<{ value: string; label: string; description?: string }>;
    value?: string[];
    onChange?: (value: string[]) => void;
    expanded: boolean;
    onExpandChange: (expanded: boolean) => void;
    maxVisible?: number;
    icon?: string;
  }> = ({
    options,
    value = [],
    onChange,
    expanded,
    onExpandChange,
    maxVisible = 3,
    icon = 'F'
  }) => {
    const visibleOptions = expanded ? options : options.slice(0, maxVisible);
    const hasMore = options.length > maxVisible;

    const handleItemClick = (optionValue: string) => {
      const newValue = value.includes(optionValue)
        ? value.filter(v => v !== optionValue)
        : [...value, optionValue];
      onChange?.(newValue);
    };

    return (
      <div className={styles.selectorGroup}>
        {visibleOptions.map((option) => {
          const isSelected = value.includes(option.value);
          return (
            <div
              key={option.value}
              className={`${styles.selectorItem} ${isSelected ? styles.selected : ''}`}
              onClick={() => handleItemClick(option.value)}
            >
              <div className={styles.selectorItemIcon}>
                {icon}
              </div>
              <div className={styles.selectorItemContent}>
                <div className={styles.selectorItemTitle}>{option.label}</div>
                {option.description && (
                  <div className={styles.selectorItemDescription}>{option.description}</div>
                )}
              </div>
              <div className={styles.selectorCheckbox}>
                <Checkbox checked={isSelected} />
              </div>
            </div>
          );
        })}

        {hasMore && (
          <div
            className={styles.expandButton}
            onClick={() => onExpandChange(!expanded)}
          >
            {expanded ? <UpOutlined /> : <DownOutlined />}
            {expanded ? `Show less` : `${options.length - maxVisible} more`}
          </div>
        )}
      </div>
    );
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: 'Agent Schema 创建',
        },
      ]}
    >
      <div className={styles.container}>
        {/* 三栏布局 */}
        <div className={styles.threeColumnLayout}>
          {/* 左侧：已保存的智能体 */}
          <div className={styles.leftPanel}>
            <Card title="Saved Agents" className={styles.savedAgentsCard}>
              <div
                className={styles.listContainer}
                style={{
                  height: '320px', // 减少高度，更紧凑
                  overflowY: 'scroll',
                  overflowX: 'hidden',
                  padding: '0 4px',
                  // 强制显示滚动条的CSS技巧
                  scrollbarGutter: 'stable'
                }}
              >
                <List
                  loading={agentsLoading}
                  dataSource={savedAgents}
                  renderItem={(agent) => (
                  <List.Item
                    className={`${styles.agentItem} ${selectedAgentId === agent.id ? styles.selectedAgent : ''}`}
                    onClick={() => handleSelectAgent(agent)}
                    actions={[
                      <Tooltip title="删除智能体" key="delete">
                        <Button
                          type="text"
                          size="small"
                          icon={<DeleteOutlined />}
                          onClick={(e) => handleDeleteAgent(agent, e)}
                          className={styles.deleteButton}
                          style={{ color: '#ff4d4f' }}
                        />
                      </Tooltip>
                    ]}
                  >
                    <div className={styles.agentInfo}>
                      <Text strong className={styles.agentName}>{agent.name}</Text>
                      <Text type="secondary" className={styles.agentType}>{agent.type}</Text>
                      <Text type="secondary" className={styles.agentDescription}>
                        {agent.description}
                      </Text>
                    </div>
                  </List.Item>
                )}
                />
              </div>
              <Divider />
              <Button
                type="dashed"
                block
                icon={<PlusOutlined />}
                onClick={handleNewAgent}
                className={styles.newAgentButton}
              >
                + new Agent
              </Button>
            </Card>
          </div>

          {/* 中间：智能体配置 */}
          <div className={styles.centerPanel}>
            <Card title="Agent Configuration" className={styles.configCard}>
              <div className={styles.independentScrollContainer}>
                <div className={styles.formScrollContainer}>
                  <Form
                    form={form}
                    layout="vertical"
                    className={styles.form}
                  initialValues={{
                    agentType: 'ReactAgent',
                    inputKey: 'input',
                    outputKey: 'output',
                    model: 'qwen2.5-72b-instruct',
                    handle: {
                      state: {
                        strategies: {
                          input: 'replace',
                          output: 'replace'
                        }
                      },
                      model: {
                        name: 'qwen2.5-72b-instruct',
                        url: 'https://api.example.com/v1',
                        'api-key': 'your-api-key'
                      }
                    },
                    subAgents: [],
                    tools: []
                  }}
                  onValuesChange={(changedValues, allValues) => {

                    if (allValues.name) {
                      // 同步模型选择到handle配置
                      if (changedValues.model) {
                        const currentHandle = allValues.handle || {} as any;
                        const updatedHandle = {
                          ...currentHandle,
                          // 确保包含state配置
                          state: currentHandle.state || {
                            strategies: {
                              input: 'replace',
                              output: 'replace'
                            }
                          },
                          model: {
                            name: changedValues.model,
                            url: currentHandle.model?.url || 'https://api.example.com/v1',
                            'api-key': currentHandle.model?.['api-key'] || 'your-api-key'
                          },
                          // 确保保留tools配置
                          tools: currentHandle.tools || allValues.tools || []
                        };
                        form.setFieldsValue({ handle: updatedHandle });

                        // 使用更新后的handle生成YAML
                        generateYaml({ ...allValues, handle: updatedHandle } as AgentSchemaForm).then(yaml => {
                          setYamlContent(yaml);
                        }).catch(error => {
                        });
                      } else {
                        // 异步更新YAML，不阻塞表单交互
                        // 确保handle中包含完整的model信息
                        const currentHandle = allValues.handle || {} as any;

                        const completeValues = {
                          ...allValues,
                          handle: {
                            ...currentHandle,
                            // 确保包含state配置
                            state: currentHandle.state || {
                              strategies: {
                                input: 'replace',
                                output: 'replace'
                              }
                            },
                            // 确保保留现有的model配置
                            model: currentHandle.model || {
                              name: allValues.model || 'qwen2.5-72b-instruct',
                              url: 'https://api.example.com/v1',
                              'api-key': 'your-api-key'
                            },
                            // 确保保留tools配置
                            tools: currentHandle.tools || allValues.tools || []
                          }
                        };


                        generateYaml(completeValues as AgentSchemaForm).then(yaml => {
                          setYamlContent(yaml);
                        }).catch(error => {
                        });
                      }
                    }
                  }}
                >
                <Form.Item
                  label={
                    <span>
                      Agent Name
                      <Tooltip title="A unique name for your agent (required)">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="name"
                  rules={[
                    { required: true, message: '请输入智能体名称' },
                    {
                      validator: (_, value) => {
                        if (!value) return Promise.resolve();

                        // 基本格式校验：只允许字母、数字、下划线和连字符
                        if (!/^[a-zA-Z0-9_\-\s\u4e00-\u9fa5]+$/.test(value)) {
                          return Promise.reject('智能体名称只能包含字母、数字、下划线、连字符和空格');
                        }

                        // 长度校验
                        if (value.length < 2 || value.length > 50) {
                          return Promise.reject('智能体名称长度应在2-50个字符之间');
                        }

                        // 检查重复性错误
                        if (nameError && value === form.getFieldValue('name')) {
                          return Promise.reject(nameError);
                        }

                        return Promise.resolve();
                      }
                    }
                  ]}
                  help={nameError || undefined}
                  validateStatus={nameError ? 'error' : undefined}
                >
                  <Input
                    placeholder="Enter agent name"
                    onChange={(e) => checkNameDuplicate(e.target.value)}
                  />
                </Form.Item>


                <Form.Item
                  label={
                    <span>
                      Model Selection
                      <Tooltip title="Select the LLM model for this agent">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="model"
                  rules={[{ required: true, message: '请选择模型' }]}
                >
                  <Select
                    placeholder="Select model"
                    loading={modelsLoading}
                    disabled={modelsLoading}
                  >
                    {availableModels.map(model => (
                      <Option key={model.model_id} value={model.model_id}>
                        {model.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      Agent Type
                      <Tooltip title="Functional type of the agent">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="agentType"
                >
                  <Select placeholder="Select agent type">
                    {agentTypeOptions.map(option => (
                      <Option key={option.value} value={option.value}>
                        {option.label}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      Description
                      <Tooltip title="Brief summary of agent capabilities (optional)">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="description"
                >
                  <TextArea
                    rows={3}
                    placeholder="Enter agent description"
                  />
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      Instructions (System Prompt)
                      <Tooltip title="System instructions to guide agent behavior">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="instruction"
                  rules={[{ required: true, message: '请输入系统提示词' }]}
                >
                  <TextArea
                    rows={8}
                    placeholder="Enter system instructions..."
                  />
                </Form.Item>

                <div className={styles.keyRow}>
                  <Form.Item
                    label={
                      <span>
                        Input Key
                        <Tooltip title="Key for input data in agent interactions">
                          <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                        </Tooltip>
                      </span>
                    }
                    name="inputKey"
                    className={styles.keyItem}
                  >
                    <Input placeholder="user_query" />
                  </Form.Item>
                  <Form.Item
                    label={
                      <span>
                        Output Key
                        <Tooltip title="Key for output data from agent responses">
                          <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                        </Tooltip>
                      </span>
                    }
                    name="outputKey"
                    className={styles.keyItem}
                  >
                    <Input placeholder="agent_response" />
                  </Form.Item>
                </div>

                <Form.Item
                  label={
                    <span>
                      Sub Agents
                      <Tooltip title="Select sub-agents to enable collaboration">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="subAgents"
                  rules={[
                    {
                      validator: (_, value) => {
                        if (!value || !Array.isArray(value) || value.length === 0) {
                          return Promise.resolve();
                        }

                        // 获取当前选中的agent名称
                        const currentAgentName = form.getFieldValue('name');
                        if (!currentAgentName) {
                          return Promise.resolve();
                        }

                        // 检查是否选择了当前agent自身
                        if (value.includes(currentAgentName)) {
                          return Promise.reject('不能将当前智能体设置为自身的子智能体');
                        }

                        return Promise.resolve();
                      }
                    }
                  ]}
                >
                  <CustomSelector
                    options={subAgentOptions.map(option => {
                      const agent = savedAgents.find(sa => sa.name === option.value);
                      return {
                        value: option.value,
                        label: option.label,
                        description: `Type: ${agent?.type || 'Unknown'}`
                      };
                    })}
                    expanded={subAgentsExpanded}
                    onExpandChange={setSubAgentsExpanded}
                    maxVisible={3}
                    icon="A"
                  />
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      Tools
                      <Tooltip title="Tools available for the agent to use">
                        <Text type="secondary" className={styles.tooltipText}> ℹ️</Text>
                      </Tooltip>
                    </span>
                  }
                  name="tools"
                >
                  {toolsLoading ? (
                    <div>加载工具中...</div>
                  ) : (
                    <CustomSelector
                      options={toolOptions.map(option => ({
                        value: option.value,
                        label: option.label,
                        description: `Tool: ${option.value}`
                      }))}
                      expanded={toolsExpanded}
                      onExpandChange={setToolsExpanded}
                      maxVisible={8}
                      icon="F"
                    />
                  )}
                </Form.Item>
                </Form>
                </div>

                <div className={styles.configActions}>
                  <Button onClick={handleGoBack} icon={<CloseOutlined />}>
                    Cancel
                  </Button>
                  <Button
                    type="primary"
                    onClick={handleSaveAgent}
                    loading={loading}
                    icon={<SaveOutlined />}
                  >
                    Save Agent
                  </Button>
                </div>
              </div>
            </Card>
          </div>

          {/* 右侧：Agent Schema (YAML) */}
          <div className={styles.rightPanel}>
            <Card
              title={
                <div className={styles.yamlHeader}>
                  Agent Schema (YAML)
                  <Button
                    type="text"
                    icon={<CopyOutlined />}
                    onClick={handleCopyYaml}
                    className={styles.copyButton}
                  >
                    Copy
                  </Button>
                </div>
              }
              className={styles.yamlCard}
            >
              <pre className={styles.yamlContent}>
                {yamlContent || '// YAML will appear here as you configure the agent'}
              </pre>
            </Card>
          </div>
        </div>
      </div>
    </InnerLayout>
  );
};

export default AgentSchemaCreator;
