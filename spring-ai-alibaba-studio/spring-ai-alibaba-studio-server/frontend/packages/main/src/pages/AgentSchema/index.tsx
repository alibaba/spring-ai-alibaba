import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { Button, Form, Input, Select, Card, message, Space, Typography, List, Checkbox, Tooltip, Divider } from 'antd';
import { PlusOutlined, CopyOutlined, SaveOutlined, CloseOutlined, DownOutlined, UpOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';
import { AgentSchemaService } from '@/services/agentSchema';
import { ToolService } from '@/services/tool';
import { IAgentSchema, AgentType } from '@/types/agentSchema';
import { ITool } from '@/types/tool';
import { session } from '@/request/session';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

// Agent类型定义
type AgentType = 'ReactAgent' | 'ParallelAgent' | 'SequentialAgent' | 'LLMRoutingAgent' | 'LoopAgent';

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
  type: AgentType;
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
  agentType: AgentType;
  name: string;
  description: string;
  instruction: string;
  inputKey: string;
  outputKey: string;
  model: string;
  handle: HandleConfig;
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
  const [agentsLoading, setAgentsLoading] = useState(false);
  const [toolsLoading, setToolsLoading] = useState(false);
  const [subAgentsExpanded, setSubAgentsExpanded] = useState(false);
  const [toolsExpanded, setToolsExpanded] = useState(false);

  // 获取已保存的智能体列表
  const fetchSavedAgents = async () => {
    setAgentsLoading(true);
    try {
      const response = await AgentSchemaService.getAgentSchemas();
      console.log('Fetched agents response:', response); // 调试信息
      console.log('Response type:', typeof response); // 调试信息
      console.log('Is Array.isArray(response):', Array.isArray(response)); // 调试信息

      let agents: any[] = [];

      // 检查响应格式 - 后端返回 Result<List<AgentSchemaEntity>>
      if (Array.isArray(response)) {
        // 如果直接是数组，直接使用
        agents = response;
        console.log('Response is already an array, length:', agents.length);
      } else if (response && typeof response === 'object') {
        // 检查是否是包装的响应对象
        if (response.data && Array.isArray(response.data)) {
          // 标准的 Result<T> 格式
          agents = response.data;
          console.log('Found data array in Result wrapper, length:', agents.length);
        } else if (response.records && Array.isArray(response.records)) {
          // 分页响应格式
          agents = response.records;
          console.log('Found records array in paging response, length:', agents.length);
        } else {
          console.warn('Response is object but no array found in expected fields:', response);
        }
      } else {
        console.warn('Unexpected response format:', response);
      }

      console.log('Final agents array to set:', agents);
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
      console.log('Fetched tools:', tools); // 调试信息
      // 确保tools是数组
      if (Array.isArray(tools)) {
        setAvailableTools(tools);
      } else {
        console.warn('Tools response is not an array:', tools);
        setAvailableTools([]);
      }
    } catch (error) {
      message.error('获取工具列表失败');
      console.error('Failed to fetch tools:', error);
      setAvailableTools([]); // 出错时设置为空数组
    } finally {
      setToolsLoading(false);
    }
  };

  // 初始化加载数据
  useEffect(() => {
    fetchSavedAgents();
    fetchAvailableTools();
  }, []);

  // Agent类型选项
  const agentTypeOptions = [
    { label: 'React Agent', value: 'ReactAgent' },
    { label: 'Parallel Agent', value: 'ParallelAgent' },
    { label: 'Sequential Agent', value: 'SequentialAgent' },
    { label: 'LLM Routing Agent', value: 'LLMRoutingAgent' },
    { label: 'Loop Agent', value: 'LoopAgent' },
  ];

  // 模型选项
  const modelOptions = [
    { label: 'Qwen2.5-72B-Instruct', value: 'qwen2.5-72b-instruct' },
    { label: 'Qwen2.5-32B-Instruct', value: 'qwen2.5-32b-instruct' },
    { label: 'Qwen2.5-14B-Instruct', value: 'qwen2.5-14b-instruct' },
    { label: 'GPT-4', value: 'gpt-4' },
    { label: 'GPT-3.5-turbo', value: 'gpt-3.5-turbo' },
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

  // 子代理选项 - 从已保存的智能体获取
  const subAgentOptions = Array.isArray(savedAgents) ? savedAgents.map(agent => ({
    label: agent.name,
    value: agent.id?.toString() || agent.name,
  })) : [];

  // 生成 YAML 内容
  const generateYaml = (values: AgentSchemaForm) => {
    const instruction = values.instruction || '';
    const inputKeys = values.inputKey ? [values.inputKey] : ['input'];
    const subAgents = values.subAgents || [];

    // 生成handle配置
    const generateHandleYaml = (handle: HandleConfig): string => {
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

      // ReactAgent特有配置
      if ('model' in handle && handle.model) {
        yaml += `  model:\n`;
        yaml += `    name: "${handle.model.name}"\n`;
        yaml += `    url: "${handle.model.url}"\n`;
        yaml += `    api-key: "${handle.model['api-key']}"\n`;
      }

      if ('max_iterations' in handle && handle.max_iterations) {
        yaml += `  max_iterations: ${handle.max_iterations}\n`;
      }

      if ('tools' in handle && handle.tools && handle.tools.length > 0) {
        yaml += `  tools:\n`;
        handle.tools.forEach(tool => {
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
        if (field in handle && handle[field as keyof ReactAgentHandleConfig]) {
          yaml += `  ${field}: "${handle[field as keyof ReactAgentHandleConfig]}"\n`;
        }
      });

      return yaml;
    };

    // 生成子代理配置
    const generateSubAgentsYaml = (subAgents: string[]): string => {
      if (subAgents.length === 0) return '';

      let yaml = 'sub_agents:\n';
      subAgents.forEach(subAgentId => {
        // 简化处理，直接使用ID引用
        yaml += `  - agent: ${subAgentId}\n`;
      });
      return yaml;
    };

    const handleYaml = values.handle ? generateHandleYaml(values.handle) : 'handle:\n  state:\n    strategies:\n      input: "replace"\n      output: "replace"\n';
    const yaml = `agent:\n  type: "${values.agentType || 'ReactAgent'}"\n  name: "${values.name || ''}"\n  description: "${values.description || ''}"\n  instruction: |\n    ${instruction.replace(/\n/g, '\n    ')}\n  input_keys:\n${inputKeys.length > 0 ? inputKeys.map(key => `    - "${key}"`).join('\n') : '    - "input"'}\n  output_key: "${values.outputKey || 'output'}"\n${handleYaml}${subAgents.length > 0 ? generateSubAgentsYaml(subAgents) : ''}`;
    return yaml;
  };

  // 监听表单变化，实时更新 YAML
  useEffect(() => {
    const subscription = form.getFieldsValue() as AgentSchemaForm;
    if (subscription.name) {
      const yaml = generateYaml(subscription);
      setYamlContent(yaml);
    }
  }, [form, selectedAgentId]);

  // 选择智能体
  const handleSelectAgent = (agent: IAgentSchema) => {
    setSelectedAgentId(agent.id || null);
    // 将后端数据转换为前端表单格式
    const formData = {
      name: agent.name,
      description: agent.description,
      type: agent.type,
      instruction: agent.instruction,
      input_keys: agent.inputKeys,
      output_key: agent.outputKey,
      handle: JSON.parse(agent.handle || '{}'),
      sub_agents: agent.subAgents ? JSON.parse(agent.subAgents) : [],
    };
    form.setFieldsValue(formData);
    const yaml = generateYaml(formData);
    setYamlContent(yaml);
  };

  // 创建新智能体
  const handleNewAgent = () => {
    setSelectedAgentId(null);
    form.resetFields();
    setYamlContent('');
  };

  // 保存智能体
  const handleSaveAgent = async () => {
    console.log('=== HANDLE SAVE AGENT START ===');
    setLoading(true);
    try {
      console.log('1. Getting form fields directly...');
      const rawValues = form.getFieldsValue();
      console.log('2. Raw form fields retrieved:', rawValues);

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
      console.log('3. Processed form values:', values);

      // 手动验证必填字段
      if (!values.name) {
        message.error('请输入智能体名称');
        return;
      }
      if (!values.instruction) {
        message.error('请输入系统提示词');
        return;
      }

      console.log('3. Form validation passed manually');

      let yaml: string;
      try {
        console.log('4. Generating YAML...');
        yaml = generateYaml(values);
        console.log('5. YAML generated:', yaml);
      } catch (yamlError) {
        console.error('YAML generation failed:', yamlError);
        message.error('YAML生成失败');
        return;
      }

      // 构建保存到后端的数据格式
      const agentData: any = {
        name: values.name,
        description: values.description,
        type: values.agentType || 'ReactAgent',
        instruction: values.instruction,
        inputKeys: values.inputKey ? [values.inputKey] : ['input'],
        outputKey: values.outputKey || 'output',
        handle: JSON.stringify({
          state: {
            strategies: {
              input: 'replace',
              output: 'replace'
            }
          },
          model: {
            name: values.model,
            url: 'https://api.example.com/v1',
            'api-key': 'your-api-key'
          },
          tools: values.tools || []
        }),
        subAgents: values.subAgents ? JSON.stringify(values.subAgents.map((id: string) => ({ agent: { id } }))) : undefined,
        yamlSchema: yaml,
      };

      console.log('6. Agent data prepared:', agentData);
      console.log('7. About to call API...');
      console.log('Base URL:', process.env.WEB_SERVER);
      console.log('Full API URL:', `${process.env.WEB_SERVER}/console/v1/agent-schemas`);

      // 检查认证状态
      const token = await session.asyncGet();
      console.log('Current token:', token ? 'Token exists' : 'No token found');

      if (selectedAgentId) {
        console.log('6. Updating existing agent, ID:', selectedAgentId);
        const updatedAgent = await AgentSchemaService.updateAgentSchema(selectedAgentId, agentData);
        console.log('7. Agent updated successfully:', updatedAgent);
        message.success('Agent Schema 更新成功！');
      } else {
        console.log('6. Creating new agent...');
        try {
          const createdAgent = await AgentSchemaService.createAgentSchema(agentData);
          console.log('7. Agent created successfully:', createdAgent);
          message.success('Agent Schema 创建成功！');
        } catch (apiError) {
          console.error('API call failed:', apiError);
          // 不要在这里重新抛出错误，让外层的catch处理
          throw apiError;
        }
      }

      console.log('8. Refreshing agent list...');
      await fetchSavedAgents();
      console.log('9. Agent list refreshed');

      setTimeout(() => {
        console.log('10. Saved agents after timeout:', savedAgents);
      }, 100);

    } catch (error) {
      console.error('=== SAVE FAILED ===', error);
      message.error('保存失败，请重试');
    } finally {
      console.log('=== HANDLE SAVE AGENT END ===');
      setLoading(false);
    }
  };

  // 复制 YAML
  const handleCopyYaml = () => {
    navigator.clipboard.writeText(yamlContent);
    message.success('YAML 已复制到剪贴板');
  };

  // 下载 YAML
  const handleDownloadYaml = () => {
    const values = form.getFieldsValue();
    const yaml = generateYaml(values);
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
                      }
                    },
                    subAgents: [],
                    tools: []
                  }}
                  onValuesChange={(changedValues, allValues) => {
                    if (allValues.name) {
                      const yaml = generateYaml(allValues as AgentSchemaForm);
                      setYamlContent(yaml);
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
                  rules={[{ required: true, message: '请输入智能体名称' }]}
                >
                  <Input placeholder="Enter agent name" />
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
                  <Select placeholder="Select model">
                    {modelOptions.map(option => (
                      <Option key={option.value} value={option.value}>
                        {option.label}
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
                >
                  <CustomSelector
                    options={subAgentOptions.map(option => ({
                      value: option.value,
                      label: option.label,
                      description: `Agent type: ${option.value}`
                    }))}
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
