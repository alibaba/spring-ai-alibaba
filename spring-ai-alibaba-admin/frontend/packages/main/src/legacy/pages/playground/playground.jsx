import React, { useState, useEffect, useContext, useMemo } from 'react';
import {
  Spin,
  Button,
  Alert,
  Empty,
  Card,
  Typography,
  Row,
  Col,
  Input,
  Select,
  InputNumber,
  Space,
  Tag,
  Badge,
  Avatar,
  Modal,
  message,
  Tooltip,
  Divider,
} from 'antd';
import {
  LoadingOutlined,
  DownloadOutlined,
  CopyOutlined,
  DeleteOutlined,
  RocketOutlined,
  CommentOutlined,
  SendOutlined,
  RobotOutlined,
  ClearOutlined,
  EditOutlined,
  PlusOutlined,
  EyeOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import { handleApiError, notifyError } from '../../utils/notification';
import { executeStreamingPrompt } from '../../utils/streamingPrompt';
import CreatePromptModal from '../../components/CreatePromptModal';
import PublishVersionModal from '../../components/PublishVersionModal';
import TemplateImportModal from '../../components/TemplateImportModal';
import API from '../../services';
import { ModelsContext } from '../../context/models';
import AddFunctionModal from '../prompts/prompt-detail/add-function-modal/add-function-modal';
import ViewFunctionModel from '../prompts/prompt-detail/view-function-model/view-function-model';
import FunctionList from '../prompts/prompt-detail/FunctionList';
import { safeJSONParse } from '../../utils/util';
import { useNavigate } from 'react-router-dom';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { Option } = Select;

// 添加闪烁光标的CSS动画样式
const cursorBlinkStyle = `
  @keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
  }
`;

const PlaygroundPage = () => {
  // Working in standalone mode with real API data
  const playgroundData = null; // Will be enhanced later for proper prompt navigation
  const navigate = useNavigate();

  // Get default model ID (first available model or fallback)

  const [showFunctionModal, setShowFunctionModal] = useState(false);
  const [showFunctionViewModal, setShowFunctionViewModal] = useState(false);
  const [selectedFunction, setSelectedFunction] = useState(null);

  const getDefaultModelId = () => {
    return models.length > 0 ? models[0].id : '';
  };

  // Get model parameters with fallback to model's defaultParameters
  const getModelParams = (modelId, modelConfig = {}) => {
    console.log('PlaygroundPage - getModelParams called with:', { modelId, modelConfig, availableModels: models.length }); // Debug log
    const selectedModel = models.find(m => m.id === modelId);
    let defaultParams = selectedModel?.defaultParameters || {};
    console.log('PlaygroundPage - Selected model:', selectedModel?.name, 'defaultParams:', defaultParams); // Debug log

    // Add fallback parameters if API doesn't return any
    if (Object.keys(defaultParams).length === 0) {
      defaultParams = {
        max_tokens: 1000,
        temperature: 0.7,
        top_p: 1.0,
      };
    }

    // Filter out model identifier fields from modelConfig before merging
    const { model, modelId: configModelId, ...filteredModelConfig } = modelConfig;

    // Merge filtered modelConfig with defaultParameters dynamically
    const mergedParams = { ...defaultParams, ...filteredModelConfig };
    console.log('PlaygroundPage - Merged params (after filtering model identifiers):', mergedParams); // Debug log
    return mergedParams;
  };

  // 获取模型参数显示值的辅助函数
  const getDisplayModelParams = (modelParams, selectedModel) => {
    if (modelParams) {
      // Filter out model identifier fields from display parameters
      const { model, modelId, ...filteredParams } = modelParams;
      return filteredParams; // Return filtered parameters dynamically
    }

    // 如果没有modelParams，使用模型的默认参数
    const selectedModelData = models.find(m => m.id === selectedModel);
    const defaultParams = selectedModelData?.defaultParameters || {};

    // Filter out any model identifier fields from default parameters as well
    const { model, modelId, ...filteredDefaultParams } = defaultParams;
    return filteredDefaultParams; // Return filtered default parameters dynamically
  };
  // Local extractParameters function
  const extractParameters = (content) => {
    const regex = /\{\{(\w+)\}\}/g;
    const parameters = [];
    let match;
    while ((match = regex.exec(content)) !== null) {
      if (!parameters.includes(match[1])) {
        parameters.push(match[1]);
      }
    }
    return parameters;
  };

  // State for API data
  const [prompts, setPrompts] = useState([]);
  const { models } = useContext(ModelsContext); // Dynamic models from API
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [versionDetailsCache, setVersionDetailsCache] = useState({}); // Cache for version details

  // Initial state with proper model parameter defaults
  const getInitialPromptInstance = () => {
    const defaultModelId = models.length > 0 ? models[0].id : '';
    // Always use getModelParams to ensure proper priority system even when models array is empty
    const modelParams = getModelParams(defaultModelId);

    return {
      id: 1,
      promptName: playgroundData?.name || '',
      version: playgroundData?.version || '',
      content: playgroundData?.content || '',
      parameters: playgroundData?.parameters || [],
      parameterValues: {},
      results: [],
      isLoading: false,
      selectedPromptId: playgroundData?.id || null,
      selectedVersionId: playgroundData?.currentVersion?.id || null,
      selectedModel: defaultModelId,
      modelParams: modelParams,
      isContentModified: false, // 标记内容是否被修改
      chatHistory: [], // 独立的对话历史
      sessionId: null, // 会话 ID
      mockTools: [] // 函数工具列表
    };
  };

  const [promptInstances, setPromptInstances] = useState(() => {
    // Only create initial instance if we have models, otherwise start with empty array
    // The useEffect will populate this once models are loaded
    return [];
  });

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showPublishModal, setShowPublishModal] = useState(false);
  const [showTemplateModal, setShowTemplateModal] = useState(null); // 存储要导入模版的prompt ID
  const [recentlyDeletedSessions, setRecentlyDeletedSessions] = useState({}); // 存储最近删除的会话 ID
  const [selectedSessionId, setSelectedSessionId] = useState(null);

  // 为每个 prompt 实例添加输入状态
  const [promptInputs, setPromptInputs] = useState({});

  // Add refs for cleaning up streaming requests
  const eventSourceRefs = React.useRef({});
  // Add refs for chat containers to enable auto-scroll
  const chatContainerRefs = React.useRef({});

  // Utility function for reliable auto-scroll
  const scrollToBottom = (promptId, immediate = false) => {
    const chatContainer = chatContainerRefs.current[promptId];
    if (chatContainer) {
      if (immediate) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
      } else {
        chatContainer.scrollTo({
          top: chatContainer.scrollHeight,
          behavior: 'smooth'
        });
      }
    }
  };

  // Time formatting function
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Load prompts data from API
  const loadPromptsData = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await API.getPrompts({
        pageNo: 1,
        pageSize: 100 // Load all prompts for selection
      });

      if (response.code !== 200) {
        throw new Error(response.message || '获取 Prompts 列表失败');
      }

      // Transform API data to match expected format
      const promptsData = response.data.pageItems || [];
      const transformedPrompts = await Promise.all(
        promptsData.map(async (promptData) => {
          try {
            // Load versions for each prompt
            const versionsResponse = await API.getPromptVersions({
              promptKey: promptData.promptKey,
              pageNo: 1,
              pageSize: 50
            });

            const versions = versionsResponse.code === 200 ?
              (versionsResponse.data.pageItems || []).map(version => ({
                id: `${promptData.promptKey}-${version.version}`,
                version: version.version,
                content: '', // Will be loaded when needed
                description: version.versionDescription,
                parameters: [],
                createdAt: formatTime(version.createTime),
                status: version.status,
                versionType: version.status,
                modelConfig: null // Will be loaded when version is selected
              })) : [];

            return {
              id: promptData.promptKey, // Use promptKey as ID
              promptKey: promptData.promptKey,
              name: promptData.promptKey,
              tags: promptData.tags ? promptData.tags.split(',').map(tag => tag.trim()) : [],
              description: promptData.promptDescription,
              createdAt: formatTime(promptData.createTime),
              updatedAt: formatTime(promptData.updateTime),
              currentVersion: versions.length > 0 ? {
                id: versions[0].id,
                version: promptData.latestVersion,
                content: '',
                description: '',
                parameters: [],
                createdAt: formatTime(promptData.updateTime),
                changeType: 'update',
                status: promptData.latestVersionStatus,
                modelConfig: null
              } : null,
              versions: versions
            };
          } catch (err) {
            console.error(`Error loading versions for ${promptData.promptKey}:`, err);
            return {
              id: promptData.promptKey,
              promptKey: promptData.promptKey,
              name: promptData.promptKey,
              tags: promptData.tags ? promptData.tags.split(',').map(tag => tag.trim()) : [],
              description: promptData.promptDescription,
              createdAt: formatTime(promptData.createTime),
              updatedAt: formatTime(promptData.updateTime),
              currentVersion: null,
              versions: []
            };
          }
        })
      );

      setPrompts(transformedPrompts);
    } catch (err) {
      console.error('加载 Prompts 数据失败:', err);
      handleApiError(err, '加载 Prompts 数据');
      setError(err.message || '加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // Load version detail with caching
  const loadVersionDetail = async (promptKey, version) => {
    const cacheKey = `${promptKey}-${version}`;

    // Check cache first
    if (versionDetailsCache[cacheKey]) {
      return versionDetailsCache[cacheKey];
    }

    try {
      const response = await API.getPromptVersion({
        promptKey,
        version
      });

      if (response.code !== 200) {
        throw new Error(response.message || '获取版本详情失败');
      }

      const versionDetail = response.data;

      // Parse JSON strings
      const variables = versionDetail.variables ? safeJSONParse(versionDetail.variables) : {};
      const modelConfig = versionDetail.modelConfig ? safeJSONParse(versionDetail.modelConfig) : null;
      const parameters = Object.keys(variables);

      // Create enhanced version object
      const enhancedVersion = {
        content: versionDetail.template,
        parameters,
        variables,
        modelConfig
      };

      // Cache the result
      setVersionDetailsCache(prev => ({
        ...prev,
        [cacheKey]: enhancedVersion
      }));

      return enhancedVersion;
    } catch (err) {
      console.error('加载版本详情失败:', err);
      handleApiError(err, '加载版本详情');
      throw err;
    }
  };

  useEffect(() => {
    loadPromptsData();
  }, []);

  // Update prompt instances when models are loaded to use proper default parameters
  useEffect(() => {
    console.log('PlaygroundPage - useEffect triggered, models:', models.length, 'promptInstances:', promptInstances.length); // Debug log

    if (models.length > 0) {
      // If we don't have any prompt instances yet, create the initial one
      if (promptInstances.length === 0) {
        console.log('PlaygroundPage - Creating initial prompt instance with loaded models'); // Debug log
        const initialInstance = getInitialPromptInstance();
        setPromptInstances([initialInstance]);
      }
    }
  }, [models]);

  useEffect(() => {
    if (playgroundData) {
      const defaultModelId = getDefaultModelId();
      const modelParams = getModelParams(defaultModelId);
      setPromptInstances([{
        id: 1,
        promptName: playgroundData.name,
        version: playgroundData.version,
        content: playgroundData.content,
        parameters: playgroundData.parameters || [],
        parameterValues: {},
        results: [],
        isLoading: false,
        selectedPromptId: playgroundData.id,
        selectedVersionId: playgroundData.currentVersion?.id || null,
        selectedModel: playgroundData.currentVersion?.modelConfig?.modelId || defaultModelId,
        modelParams: playgroundData.currentVersion?.modelConfig ?
          getModelParams(
            playgroundData.currentVersion.modelConfig.modelId || defaultModelId,
            playgroundData.currentVersion.modelConfig
          ) : modelParams,
        isContentModified: false
      }]);
    }
    // Initialize with empty prompt instance if no playgroundData
  }, [playgroundData]);


  const updateParameterValue = (promptId, paramName, value) => {
    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === promptId
        ? {
          ...prompt,
          parameterValues: { ...prompt.parameterValues, [paramName]: value }
        }
        : prompt
    ));
  };

  const selectPrompt = (promptInstanceId, selectedPromptKey) => {
    if (selectedPromptKey === '') {
      // 清空选择
      setPromptInstances(prev => prev.map(prompt =>
        prompt.id === promptInstanceId
          ? {
            ...prompt,
            promptName: '',
            version: '',
            content: '',
            parameters: [],
            parameterValues: {},
            selectedPromptId: null,
            selectedVersionId: null,
            isContentModified: false
          }
          : prompt
      ));
    } else {
      // 设置选中的Prompt，清空版本选择
      const selectedPrompt = prompts.find(p => p.promptKey === selectedPromptKey);
      setPromptInstances(prev => prev.map(prompt =>
        prompt.id === promptInstanceId
          ? {
            ...prompt,
            selectedPromptId: selectedPromptKey,
            selectedVersionId: null,
            promptName: selectedPrompt?.name || '',
            version: '',
            content: '',
            parameters: [],
            parameterValues: {},
            isContentModified: false
          }
          : prompt
      ));
    }
  };

  const selectVersion = async (promptInstanceId, selectedVersionId) => {
    if (selectedVersionId === '') {
      // 清空版本选择，但保持Prompt选择
      const promptInstance = promptInstances.find(p => p.id === promptInstanceId);
      const selectedPrompt = prompts.find(p => p.promptKey === promptInstance.selectedPromptId);

      setPromptInstances(prev => prev.map(prompt =>
        prompt.id === promptInstanceId
          ? {
            ...prompt,
            selectedVersionId: null,
            promptName: selectedPrompt?.name || '',
            version: '',
            content: '',
            parameters: [],
            parameterValues: {},
            isContentModified: false
          }
          : prompt
      ));
    } else {
      const promptInstance = promptInstances.find(p => p.id === promptInstanceId);
      const selectedPrompt = prompts.find(p => p.promptKey === promptInstance.selectedPromptId);
      const selectedVersion = selectedPrompt?.versions?.find(v => v.id === selectedVersionId);

      if (selectedVersion && selectedPrompt) {
        try {
          // Load version details from API
          const versionDetail = await loadVersionDetail(selectedPrompt.promptKey, selectedVersion.version);

          const parameters = versionDetail.parameters || [];
          const modelConfig = versionDetail.modelConfig;

          setPromptInstances(prev => prev.map(prompt =>
            prompt.id === promptInstanceId
              ? {
                ...prompt,
                selectedVersionId: selectedVersionId,
                promptName: selectedPrompt.name,
                version: selectedVersion.version,
                content: versionDetail.content,
                parameters,
                parameterValues: parameters.reduce((acc, param) => {
                  acc[param] = versionDetail.variables[param] || '';
                  return acc;
                }, {}),
                selectedModel: modelConfig?.modelId || getDefaultModelId(),
                modelParams: getModelParams(
                  modelConfig?.modelId || getDefaultModelId(),
                  modelConfig
                ),
                isContentModified: false
              }
              : prompt
          ));
        } catch (err) {
          console.error('加载版本详情失败:', err);
          notifyError({ message: '加载版本详情失败' });
        }
      }
    }
  };

  const updatePromptModel = (promptId, modelId) => {
    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === promptId ? {
        ...prompt,
        selectedModel: modelId,
        // Update model parameters to use the new model's defaults
        modelParams: getModelParams(modelId, {})
      } : prompt
    ));
  };

  const updatePromptModelParams = (promptId, paramName, value) => {
    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === promptId
        ? {
          ...prompt,
          modelParams: { ...prompt.modelParams, [paramName]: value }
        }
        : prompt
    ));
  };

  const copyPrompt = (promptId) => {
    if (promptInstances.length >= 3) {
      alert('最多只能同时对比3个Prompt');
      return;
    }

    const promptToCopy = promptInstances.find(p => p.id === promptId);
    if (promptToCopy) {
      const newPrompt = {
        ...promptToCopy,
        id: Date.now(),
        promptName: '', // 置空promptName
        version: '', // 置空version
        content: promptToCopy.content, // 保留内容
        parameters: [...promptToCopy.parameters], // 保留参数
        parameterValues: { ...promptToCopy.parameterValues },
        results: [],
        isLoading: false,
        selectedPromptId: null, // 置空选择
        selectedVersionId: null, // 置空版本选择
        selectedModel: promptToCopy.selectedModel, // 保留模型选择
        modelParams: { ...promptToCopy.modelParams }, // 保留模型参数
        isContentModified: false,
        chatHistory: [], // 新窗口独立的对话历史
        sessionId: null, // 新的会话 ID
        mockTools: [] // 函数工具列表
      };
      setPromptInstances(prev => [...prev, newPrompt]);
    }
  };

  const removePrompt = (promptId) => {
    if (promptInstances.length > 1) {
      setPromptInstances(prev => prev.filter(p => p.id !== promptId));
    }
  };

  // 单个 Prompt 执行函数
  const runSinglePrompt = async (promptInstance, inputText) => {
    const {
      id: promptId, content, parameterValues, selectedModel, modelParams, sessionId, mockTools,
      enableFn
    } = promptInstance;

    const config = {
      promptId,
      content,
      parameterValues,
      selectedModel,
      modelParams,
      sessionId,
      promptKey: currentPrompt?.promptKey || 'playground',
      version: currentPrompt?.latestVersion || '1.0',
      mockTools: enableFn === false ? [] : mockTools,
    };

    const callbacks = {
      onUpdateChatHistory: (promptId, updater) => {
        setPromptInstances(prev => prev.map(prompt =>
          prompt.id === promptId
            ? { ...prompt, chatHistory: updater(prompt.chatHistory || []) }
            : prompt
        ));
      },
      onUpdateSessionId: (promptId, sessionId) => {
        setPromptInstances(prev => prev.map(prompt =>
          prompt.id === promptId
            ? { ...prompt, sessionId }
            : prompt
        ));
      },
      onUpdateMetrics: (promptId, data) => {
        setPromptInstances(prev => {
          const instance = prev.map(prompt =>
            prompt.id === promptId
              ? { ...prompt, ...data }
              : prompt
          );
          return instance
        });
      },
      formatTime,
      replaceParameters
    };

    // 为单个 prompt 添加用户消息到对话历史
    setPromptInstances(prev => prev.map(prompt => {
      if (prompt.id === promptId) {
        const userMessage = {
          id: Date.now() + prompt.id,
          type: 'user',
          content: inputText,
          timestamp: formatTime(Date.now())
        };
        return {
          ...prompt,
          chatHistory: [...(prompt.chatHistory || []), userMessage],
          isLoading: true
        };
      }
      return prompt;
    }));

    try {
      await executeStreamingPrompt(config, inputText, callbacks, eventSourceRefs.current);
    } finally {
      // 结束加载状态
      setPromptInstances(prev => prev.map(prompt =>
        prompt.id === promptId
          ? { ...prompt, isLoading: false }
          : prompt
      ));
    }
  };

  // 单个配置的对话发送函数
  const handleSendMessage = (promptId, inputText) => {
    if (!inputText?.trim()) return;

    const promptInstance = promptInstances.find(p => p.id === promptId);
    if (promptInstance) {
      runSinglePrompt(promptInstance, inputText);
      // 清空输入框
      setPromptInputs(prev => ({
        ...prev,
        [promptId]: ''
      }));
    }
  };

  // 更新输入内容
  const updatePromptInput = (promptId, value) => {
    setPromptInputs(prev => ({
      ...prev,
      [promptId]: value
    }));
  };

  // Streaming runPrompt implementation using shared utility

  const clearChatHistory = (promptId = null) => {
    if (promptId) {
      // 存储即将清除的会话 ID
      const prompt = promptInstances.find(p => p.id === promptId);
      if (prompt && prompt.sessionId) {
        setRecentlyDeletedSessions(prev => ({
          ...prev,
          [promptId]: prompt.sessionId
        }));
      }

      // 清空指定prompt的对话历史和会话
      setPromptInstances(prev => prev.map(prompt =>
        prompt.id === promptId
          ? { ...prompt, chatHistory: [], sessionId: null }
          : prompt
      ));
    } else {
      // 存储所有即将清除的会话 ID
      const sessionsToStore = {};
      promptInstances.forEach(prompt => {
        if (prompt.sessionId) {
          sessionsToStore[prompt.id] = prompt.sessionId;
        }
      });
      setRecentlyDeletedSessions(prev => ({
        ...prev,
        ...sessionsToStore
      }));

      // 清空所有prompt的对话历史和会话
      setPromptInstances(prev => prev.map(prompt =>
        ({ ...prompt, chatHistory: [], sessionId: null })
      ));
    }
  };

  // 恢复会话功能
  const restoreSession = async (promptId) => {
    const sessionId = recentlyDeletedSessions[promptId];
    if (!sessionId) {
      message.error('没有可恢复的会话');
      return false;
    }

    try {
      const response = await API.getPromptSession(sessionId);
      if (response.code === 200) {
        const sessionData = response.data;

        // 转换会话数据为聊天历史格式
        const chatHistory = sessionData.messages.map((msg, index) => {
          const displayParams = msg.role === 'assistant' && msg.modelParams ?
            msg.modelParams :
            getDisplayModelParams(null, msg.model || getDefaultModelId());

          return {
            id: Date.now() + index,
            type: msg.role === 'user' ? 'user' : 'assistant',
            content: msg.content,
            timestamp: formatTime(msg.timestamp),
            model: msg.role === 'assistant' ? 'AI Model' : undefined,
            modelParams: msg.role === 'assistant' ? displayParams : undefined
          };
        });

        // 更新 prompt 实例
        setPromptInstances(prev => prev.map(prompt =>
          prompt.id === promptId
            ? { ...prompt, sessionId, chatHistory }
            : prompt
        ));

        // 清除已恢复的会话 ID
        setRecentlyDeletedSessions(prev => {
          const newSessions = { ...prev };
          delete newSessions[promptId];
          return newSessions;
        });

        message.success('会话恢复成功');
        return true;
      } else {
        message.error(response.message || '恢复会话失败');
        return false;
      }
    } catch (error) {
      console.error('Restore session error:', error);
      message.error('恢复会话失败');
      return false;
    }
  };

  // 删除会话功能
  const deleteSession = async (promptId) => {
    const prompt = promptInstances.find(p => p.id === promptId);
    if (!prompt || !prompt.sessionId) return false;

    try {
      const response = await API.deletePromptSession(prompt.sessionId);
      if (response.code === 200) {
        // 清空对话历史和会话ID
        setPromptInstances(prev => prev.map(p =>
          p.id === promptId
            ? { ...p, sessionId: null, chatHistory: [] }
            : p
        ));
        message.success('会话删除成功');
        return true;
      } else {
        message.error(response.message || '删除会话失败');
        return false;
      }
    } catch (error) {
      console.error('Delete session error:', error);
      message.error('删除会话失败');
      return false;
    }
  };

  // Cleanup streaming connections on component unmount
  React.useEffect(() => {
    return () => {
      // 清理所有流连接
      Object.values(eventSourceRefs.current).forEach(eventSource => {
        if (eventSource && eventSource.close) {
          eventSource.close();
        }
      });
      eventSourceRefs.current = {};
    };
  }, []);

  // Auto-scroll to bottom when chat history changes
  React.useEffect(() => {
    promptInstances.forEach(prompt => {
      if (prompt.chatHistory && prompt.chatHistory.length > 0) {
        // Use setTimeout to ensure DOM is updated after render
        setTimeout(() => {
          scrollToBottom(prompt.id);
        }, 50);

        // Also trigger immediate scroll for real-time updates
        requestAnimationFrame(() => {
          scrollToBottom(prompt.id, true);
        });
      }
    });
  }, [promptInstances.map(p => p.chatHistory).flat(), promptInstances.map(p => p.chatHistory?.map(msg => msg.content)).flat()]);

  // Additional effect to handle streaming message content updates
  React.useEffect(() => {
    promptInstances.forEach(prompt => {
      const hasStreamingMessage = prompt.chatHistory?.some(msg => msg.isLoading);
      if (hasStreamingMessage) {
        scrollToBottom(prompt.id, true);
      }
    });
  }, [promptInstances.map(p => p.chatHistory?.filter(msg => msg.isLoading)).flat()]);

  const replaceParameters = (content, parameterValues) => {
    let result = content;
    Object.entries(parameterValues).forEach(([key, value]) => {
      result = result.replace(new RegExp(`\{\{${key}\}\}`, 'g'), value || `{{${key}}}`);
    });
    return result;
  };

  const handleContentChange = (id, content) => {
    const parameters = extractParameters(content);
    console.log(parameters, content, 'asd...22')
    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === id
        ? {
          ...prompt,
          content,
          parameters,
          parameterValues: parameters.reduce((acc, param) => {
            acc[param] = prompt.parameterValues[param] || '';
            return acc;
          }, {}),
          // 手动修改内容时清空版本选择，标记为已修改
          selectedVersionId: content !== '' ? null : prompt.selectedVersionId,
          version: content !== '' ? '' : prompt.version,
          isContentModified: content !== '' && prompt.selectedPromptId ? true : false
        }
        : prompt
    ));
  };

  // 处理模板导入，包括模型配置
  const handleTemplateImport = (promptId, template) => {
    const parameters = extractParameters(template.content);
    const templateModelConfig = template.modelConfig || {};

    // 如果模板有模型配置，使用模板的配置；否则使用当前选中的模型的默认参数
    const selectedModelId = templateModelConfig.model || getDefaultModelId();
    const modelParams = getModelParams(selectedModelId, templateModelConfig);

    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === promptId
        ? {
          ...prompt,
          content: template.content,
          parameters,
          parameterValues: parameters.reduce((acc, param) => {
            acc[param] = template.parameters?.includes(param) ?
              (templateModelConfig.variables?.[param] || '') : '';
            return acc;
          }, {}),
          selectedModel: selectedModelId,
          modelParams: modelParams,
          // 清除版本选择和修改标记
          selectedVersionId: null,
          version: '',
          isContentModified: false
        }
        : prompt
    ));
  };

  const modelNameMap = useMemo(() => {
    return models.reduce((acc, model) => {
      acc[model.id] = model.name;
      return acc;
    }, {});
  }, [models]);

  const currentPromptInstance = useMemo(() => promptInstances.find(p => p.id === selectedSessionId), [promptInstances, selectedSessionId])

  // Show loading state while fetching prompts data
  if (loading) {
    return (
      <div className="p-8 min-h-[400px] flex items-center justify-center">
        <Spin
          indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />}
          size="large"
        >
          <div className="text-center pt-4">
            <Text type="secondary" className="mt-4 block">
              加载 Prompts 数据中...
            </Text>
          </div>
        </Spin>
      </div>
    );
  }

  return (
    <>
      <style>{cursorBlinkStyle}</style>
      <div className="p-8">

      <div className="mb-8">
        <Title level={1} className="m-0 mb-2">Playground</Title>
        <Paragraph type="secondary" className="m-0">测试和调试你的AI提示词</Paragraph>
      </div>

      <Space direction="vertical" size={32} className="w-full">
        {/* Prompt配置区域 */}
        <div
          className="grid gap-4"
          style={{
            gridTemplateColumns: promptInstances.length === 1
              ? '1fr'
              : promptInstances.length === 2
                ? 'repeat(2, 1fr)'
                : 'repeat(3, 1fr)',
            minHeight: 'fit-content'
          }}
        >
          {/* 响应式布局优化 */}
          <style>{`
            @media (max-width: 1600px) {
              .grid {
                gap: 12px !important;
              }
            }
            @media (max-width: 1400px) {
              .grid {
                grid-template-columns: ${promptInstances.length === 3 ? 'repeat(2, 1fr)' : 'repeat(auto-fit, minmax(400px, 1fr))'} !important;
                gap: 16px !important;
              }
            }
            @media (max-width: 1200px) {
              .grid {
                grid-template-columns: 1fr !important;
                gap: 20px !important;
              }
            }
          `}</style>
          {/* Show loading state if models haven't loaded yet */}
          {models.length === 0 ? (
            <div className="col-span-full">
              <Card className="h-[400px] flex items-center justify-center">
                <div className="text-center">
                  <Spin size="large" />
                  <div className="mt-4">
                    <Text type="secondary">正在加载模型...</Text>
                  </div>
                </div>
              </Card>
            </div>
          ) : promptInstances.length === 0 ? (
            <div className="col-span-full">
              <Card className="h-[400px] flex items-center justify-center">
                <div className="text-center">
                  <Text type="secondary">正在初始化...</Text>
                </div>
              </Card>
            </div>
          ) : (
            promptInstances.map((prompt, index) => {
              const userInput = promptInputs[prompt.id] || '';

              return (
                <Card
                  key={prompt.id}
                  className="h-fit"
                  size={promptInstances.length >= 3 ? "small" : "default"}
                  title={
                      <div className="flex items-center justify-between">
                        <div>
                          <Text strong style={{ fontSize: promptInstances.length === 3 ? '16px' : '18px' }}>
                            配置 {index + 1}
                          </Text>
                          {prompt.promptName && (
                            <Text
                              type="secondary"
                              className="ml-2"
                              style={{
                                fontSize: promptInstances.length === 3 ? '12px' : '14px'
                              }}
                            >
                              ({prompt.promptName})
                            </Text>
                          )}
                        </div>
                        <Space size="small">
                          <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            size={promptInstances.length === 3 ? "small" : "default"}
                            onClick={() => {
                              setShowFunctionModal(true);
                              setSelectedSessionId(prompt.id);
                            }}
                          >
                            {promptInstances.length >= 3 ? '新增' : '新增函数'}
                          </Button>
                          <Button
                            type="primary"
                            icon={<DownloadOutlined />}
                            size={promptInstances.length === 3 ? "small" : "default"}
                            onClick={() => setShowTemplateModal(prompt.id)}
                            className="border-none"
                            style={{
                              background: 'linear-gradient(90deg, #16a085 0%, #2ecc71 100%)'
                            }}
                          >
                            {promptInstances.length === 3 ? '模板' : '从模板导入'}
                          </Button>
                          <Button
                            type="text"
                            icon={<CopyOutlined />}
                            size={promptInstances.length === 3 ? "small" : "default"}
                            onClick={() => copyPrompt(prompt.id)}
                            disabled={promptInstances.length >= 3}
                            title={promptInstances.length >= 3 ? '最多同时调试三个prompt' : '复制Prompt进行对比'}
                          />
                          {promptInstances.length > 1 && (
                            <Button
                              type="text"
                              danger
                              icon={<DeleteOutlined />}
                              size={promptInstances.length === 3 ? "small" : "default"}
                              onClick={() => removePrompt(prompt.id)}
                              title="删除Prompt"
                            />
                          )}
                        </Space>
                      </div>
                    }
                  >
                    {/* 配置区域 */}
                    <div className={promptInstances.length >= 3 ? "mb-4" : "mb-6"}>
                      {
                        (!prompt.selectedPromptId || !prompt.selectedVersionId) && (
                          <Alert
                            message={!prompt.selectedPromptId ? "请选择 Prompt 或直接编辑内容" : "请选择版本"}
                            type="info"
                            showIcon
                            className="w-full text-center mb-3"
                          />
                        )
                      }

                      <div className={promptInstances.length >= 3 ? "space-y-3" : "space-y-4"}>
                        {/* Prompt选择下拉框 */}
                        <div>
                          <Text
                            strong
                            className="block mb-2"
                            style={{
                              fontSize: promptInstances.length === 3 ? '12px' : '14px'
                            }}
                          >
                            选择Prompt
                          </Text>
                          <Select
                            value={prompt.selectedPromptId || undefined}
                            onChange={(value) => selectPrompt(prompt.id, value || '')}
                            placeholder="选择已有Prompt..."
                            className="w-full"
                            size={promptInstances.length === 3 ? 'small' : 'middle'}
                            allowClear
                          >
                            {prompts.filter(p => p.versions && p.versions.length > 0).length === 0 ? (
                              <Option disabled value="">暂无可用的 Prompts</Option>
                            ) : (
                              prompts.filter(p => p.versions && p.versions.length > 0).map((p) => (
                                <Option key={p.promptKey} value={p.promptKey}>
                                  {p.name} ({p.versions.length} 个版本)
                                </Option>
                              ))
                            )}
                          </Select>
                        </div>

                        {/* 版本选择下拉框 */}
                        {prompt.selectedPromptId && (
                          <div>
                            <Text
                              strong
                              className="block mb-2"
                              style={{
                                fontSize: promptInstances.length === 3 ? '12px' : '14px'
                              }}
                            >
                              选择版本
                            </Text>
                            <Select
                              value={prompt.selectedVersionId || undefined}
                              onChange={(value) => selectVersion(prompt.id, value || '')}
                              placeholder="选择版本..."
                              className="w-full"
                              size={promptInstances.length === 3 ? 'small' : 'middle'}
                              allowClear
                            >
                              {(() => {
                                const selectedPrompt = prompts.find(p => p.promptKey === prompt.selectedPromptId);
                                return selectedPrompt?.versions ?
                                  selectedPrompt.versions.slice().reverse().map((version) => (
                                    <Option key={version.id} value={version.id}>
                                      {version.version} - {version.description}
                                      {version.versionType === 'release' || version.status === 'release' || version.status === 'published' ? ' (正式版)' : ' (PRE版)'}
                                    </Option>
                                  )) : [];
                              })()}
                            </Select>
                          </div>
                        )}

                        {/* Prompt内容展示 */}
                        <div>
                          <Text
                            strong
                            className="block mb-2"
                            style={{
                              fontSize: promptInstances.length === 3 ? '12px' : '14px'
                            }}
                          >
                            Prompt内容
                          </Text>
                          <TextArea
                            value={prompt.content}
                            onChange={(e) => handleContentChange(prompt.id, e.target.value)}
                            placeholder="输入Prompt内容，使用 {{参数名}} 来定义参数..."
                            style={{
                              height: promptInstances.length >= 3 ? 100 : 120,
                              resize: 'none'
                            }}
                            autoSize={false}
                          />
                        </div>

                        {/* 模型配置区域 */}
                        <div>
                          <Space direction="vertical" className='w-full' size="small">
                            {/* 模型选择 */}
                            <div>
                              <Text strong className='mb-2 block'>
                                模型
                              </Text>
                              <Select
                                value={prompt.selectedModel}
                                onChange={(value) => updatePromptModel(prompt.id, value)}
                                style={{ width: '100%' }}
                                size={promptInstances.length === 3 ? 'small' : 'middle'}
                                placeholder={models.length === 0 ? "正在加载模型..." : "选择模型"}
                                disabled={models.length === 0}
                              >
                                {models.length === 0 ? (
                                  <Option disabled value="">暂无可用模型</Option>
                                ) : (
                                  models.map((model) => (
                                    <Option key={model.id} value={model.id}>
                                      {model.name}
                                    </Option>
                                  ))
                                )}
                              </Select>
                            </div>

                            {/* 模型参数 */}
                            <Card size="small" style={{ backgroundColor: '#fafafa' }}>
                              <Text strong className="block mb-2">
                                模型参数
                              </Text>
                              <Row gutter={[8, 8]}>
                                {(() => {
                                  const { model, modelId, ...filteredParams } = prompt.modelParams || {};
                                  const paramEntries = Object.entries(filteredParams);

                                  return paramEntries.map(([paramName, paramValue]) => {
                                    const isNumeric = typeof paramValue === 'number';

                                    return (
                                      <Col span={12} key={paramName}>
                                        <Text className='block text-xs mb-1'>
                                          {paramName}
                                        </Text>
                                        {
                                          isNumeric
                                            ? (
                                              <InputNumber
                                                value={paramValue}
                                                onChange={(value) => updatePromptModelParams(prompt.id, paramName, value || 0)}
                                                size="small"
                                                className='w-full'
                                              />
                                            )
                                            : (
                                              <Input
                                                value={paramValue}
                                                onChange={(e) => updatePromptModelParams(prompt.id, paramName, e.target.value)}
                                                size="small"
                                                className='w-full'
                                              />
                                            )
                                        }
                                      </Col>
                                    );
                                  });
                                })()}
                              </Row>
                            </Card>

                            <div>
                              <FunctionList
                                size="middle"
                                onEnableChange={(enable) => {
                                  setPromptInstances(v => v.map(p => p.id === prompt.id ? { ...p, enableFn: enable } : p))
                                }}
                                functions={prompt.mockTools}
                                onClick={(fn) => {
                                  setSelectedFunction(fn);
                                  setShowFunctionViewModal(true);
                                  setSelectedSessionId(prompt.id);
                                }}
                                onDelete={(fn) => {
                                  setPromptInstances(v => v.map(p => p.id === prompt.id ? {
                                    ...p,
                                    mockTools: p.mockTools.filter(f => f.toolDefinition.name !== fn.toolDefinition.name)
                                  } : p))
                                  setShowFunctionViewModal(false);
                                }}
                              />
                            </div>
                          </Space>
                        </div>

                        {/* 参数配置 */}
                        {prompt.parameters.length > 0 && (
                          <div>
                            <Text strong className="block mb-2">
                              参数配置
                            </Text>
                            <Row gutter={[8, 8]}>
                              {prompt.parameters.map((param) => (
                                <Col span={12} key={param}>
                                  <Text className="block mb-1 text-sm">
                                    {param}
                                  </Text>
                                  <Input
                                    value={prompt.parameterValues[param] || ''}
                                    onChange={(e) => updateParameterValue(prompt.id, param, e.target.value)}
                                    placeholder={`输入 ${param} 的值...`}
                                    size="small"
                                  />
                                </Col>
                              ))}
                            </Row>
                          </div>
                        )}

                        {/* 创建Prompt和发布版本按钮 */}
                        <div className="flex flex-wrap gap-2">
                          {/* 发布新版本按钮 - 选择了已有Prompt时显示 */}
                          {prompt.selectedPromptId && (
                            <Button
                              type="primary"
                              icon={<RocketOutlined />}
                              onClick={() => {
                                const selectedPrompt = prompts.find(p => p.promptKey === prompt.selectedPromptId);
                                if (selectedPrompt) {
                                  const { model, modelId, ...filteredParams } = prompt.modelParams || {};
                                  setShowPublishModal({
                                    prompt: selectedPrompt,
                                    content: prompt.content,
                                    variablesWithValues: prompt.parameterValues,
                                    modelConfig: {
                                      modelId: prompt.selectedModel,
                                      ...filteredParams,
                                    }
                                  });
                                }
                              }}
                              disabled={!prompt.content.trim()}
                              size={promptInstances.length === 3 ? 'small' : 'middle'}
                              className="border-none"
                              style={{
                                background: 'linear-gradient(90deg, #52c41a 0%, #2ecc71 100%)'
                              }}
                            >
                              发布新版本
                            </Button>
                          )}

                          {/* 快速创建新Prompt按钮 - 在没有从详情页进入时总是显示 */}
                          {!playgroundData && (
                            <Button
                              type="primary"
                              icon={<RocketOutlined />}
                              onClick={() => setShowCreateModal({
                                quickCreate: true,
                                content: prompt.content,
                                parameters: prompt.parameters,
                                variablesWithValues: prompt.parameterValues,
                                modelConfig: {
                                  modelId: prompt.selectedModel,
                                  ...prompt.modelParams
                                }
                              })}
                              disabled={!prompt.content.trim()}
                              size={promptInstances.length === 3 ? 'small' : 'middle'}
                              className="border-none"
                            >
                              快速创建新 Prompt
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>

                    <Divider />

                    {/* 对话测试区域 */}
                    <div>
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <Avatar icon={<CommentOutlined />} style={{ backgroundColor: '#e6f7ff' }} />
                          <div>
                            <Text strong className="text-lg">对话测试</Text>
                            <div>
                              <Text type="secondary" className="text-sm">
                                测试配置 {index + 1} 的效果
                                {prompt.sessionId && (
                                  <Tag color="green" size="small" className="ml-2">
                                    会话: {prompt.sessionId.substring(0, 8)}...
                                  </Tag>
                                )}
                              </Text>
                            </div>
                          </div>
                        </div>
                        <Space>
                          {recentlyDeletedSessions[prompt.id] && (
                            <Button
                              type="text"
                              size="small"
                              icon={<RocketOutlined />}
                              onClick={() => restoreSession(prompt.id)}
                              title="恢复上一次会话"
                              style={{ color: '#52c41a' }}
                            >
                              恢复会话
                            </Button>
                          )}
                          {prompt.chatHistory && prompt.chatHistory.length > 0 && (
                            <Button
                              type="text"
                              size="small"
                              icon={<ClearOutlined />}
                              onClick={() => clearChatHistory(prompt.id)}
                              title="清空对话"
                            >
                              清空
                            </Button>
                          )}
                          <Badge
                            count={prompt.chatHistory ? prompt.chatHistory.filter(msg => msg.type === 'user').length : 0}
                            showZero
                            size="small"
                          />
                        </Space>
                      </div>

                      {/* 对话内容区域 */}
                      <div
                        ref={(el) => {
                          if (el) {
                            chatContainerRefs.current[prompt.id] = el;
                          }
                        }}
                        className="border border-gray-200 rounded-lg mb-4 bg-gray-50"
                        style={{
                          height: promptInstances.length >= 3 ? 250 : 300,
                          overflowY: 'auto',
                          padding: promptInstances.length >= 3 ? '12px' : '16px'
                        }}
                      >
                        {!prompt.chatHistory || prompt.chatHistory.length === 0 ? (
                          <div className="flex flex-col items-center justify-center h-full text-center">
                            <Avatar
                              size={64}
                              icon={<RobotOutlined />}
                              style={{
                                marginBottom: 16,
                                backgroundColor: '#f0f0f0',
                                color: '#bfbfbf'
                              }}
                            />
                            <Title level={5} style={{ margin: 0, marginBottom: 8, color: '#8c8c8c' }}>
                              等待开始对话
                            </Title>
                            <Text type="secondary" style={{ fontSize: '13px' }}>
                              在下方输入框中发送消息开始测试
                            </Text>
                          </div>
                        ) : (
                          <Space direction="vertical" style={{ width: '100%' }} size={12}>
                            {prompt.chatHistory.map((message) => (
                              <div key={message.id}>
                                {message.type === 'user' ? (
                                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}>
                                    <div style={{
                                      maxWidth: '80%',
                                      backgroundColor: '#1890ff',
                                      color: 'white',
                                      padding: '8px 12px',
                                      borderRadius: '12px 12px 4px 12px',
                                      wordBreak: 'break-word'
                                    }}>
                                      <Text style={{ color: 'white', fontSize: '13px', whiteSpace: 'pre-wrap' }}>
                                        {message.content}
                                      </Text>
                                      <div style={{ marginTop: 4, textAlign: 'right' }}>
                                        <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: '11px' }}>
                                          {message.timestamp}
                                        </Text>
                                      </div>
                                    </div>
                                  </div>
                                ) : (
                                  <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: 8 }}>
                                    <div style={{ maxWidth: '90%' }}>
                                      {/* AI消息头部 */}
                                      <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 6,
                                        marginBottom: 6
                                      }}>
                                        <Avatar
                                          size={20}
                                          icon={<RobotOutlined />}
                                          style={{ backgroundColor: '#52c41a' }}
                                        />
                                        <Text strong style={{ fontSize: '12px', color: '#52c41a' }}>
                                          {modelNameMap[message.model] || message.model}
                                        </Text>
                                        {!message.isLoading && (
                                          <Button
                                            type="text"
                                            size="small"
                                            icon={<CopyOutlined />}
                                            onClick={() => {
                                              navigator.clipboard.writeText(message.content);
                                              message.success('已复制到剪贴板');
                                            }}
                                            title="复制回复"
                                            style={{ fontSize: '10px', padding: '2px 4px', height: 20 }}
                                          />
                                        )}
                                      </div>

                                      {/* AI消息内容 */}
                                      <div style={{
                                        backgroundColor: '#fff',
                                        padding: '10px 12px',
                                        borderRadius: '4px 12px 12px 12px',
                                        border: '1px solid #e8e8e8'
                                      }}>
                                        {message.isLoading ? (
                                          <div>
                                            <Text style={{
                                              fontSize: '13px',
                                              whiteSpace: 'pre-wrap',
                                              lineHeight: '1.5',
                                              color: '#262626'
                                            }}>
                                              {message.content}
                                            </Text>
                                            {/* 流式输入闪烁光标 */}
                                            <span style={{
                                              display: 'inline-block',
                                              width: '2px',
                                              height: '16px',
                                              backgroundColor: '#1890ff',
                                              marginLeft: '2px',
                                              animation: 'blink 1s infinite'
                                            }} />
                                          </div>
                                        ) : (
                                          <>
                                            <Text style={{
                                              fontSize: '13px',
                                              whiteSpace: 'pre-wrap',
                                              lineHeight: '1.5',
                                              color: '#262626'
                                            }}>
                                              {message.content}
                                            </Text>
                                            <div className='flex gap-2 mt-2'>
                                              <Tag color="geekblue">输入 Token: {message?.usage?.promptTokens}</Tag>
                                              <Tag color='geekblue'>输出 Token: {message?.usage?.completionTokens}</Tag>
                                              <Tag color='geekblue'>总 Token: {message?.usage?.totalTokens}</Tag>
                                            </div>
                                            {/* 模型参数信息 */}
                                            <div className='flex justify-between items-center mt-2 gap-2'>
                                              <Text type="secondary" style={{ fontSize: '11px' }}>
                                                {message.timestamp}
                                              </Text>
                                              {
                                                Boolean(message.traceId) && (
                                                  <Tooltip title="查看调用链路跟踪">
                                                    <Button
                                                      type="text"
                                                      size="small"
                                                      icon={<ShareAltOutlined />}
                                                      onClick={() => {
                                                        navigate("/tracing", {
                                                          state: {
                                                            traceId: message.traceId
                                                          }
                                                        })
                                                      }}
                                                    />
                                                  </Tooltip>
                                                )
                                              }
                                            </div>
                                          </>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                )}
                              </div>
                            ))}
                          </Space>
                        )}
                      </div>

                      {/* 输入区域 */}
                      <div className="flex gap-4">
                        <div style={{ flex: 1 }}>
                          <TextArea
                            value={userInput}
                            onChange={(e) => updatePromptInput(prompt.id, e.target.value)}
                            onPressEnter={(e) => {
                              if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                handleSendMessage(prompt.id, userInput);
                              }
                            }}
                            placeholder="输入您的问题进行测试... (Enter发送，Shift+Enter换行)"
                            rows={3}
                            disabled={prompt.isLoading}
                            style={{
                              resize: 'none',
                              fontSize: '14px',
                              lineHeight: '1.5'
                            }}
                            autoSize={{ minRows: 2, maxRows: 6 }}
                          />
                        </div>
                        <div className="flex flex-col justify-end">
                          <Button
                            type="primary"
                            size="large"
                            icon={prompt.isLoading ? <Spin size="small" /> : <SendOutlined />}
                            onClick={() => {
                              handleSendMessage(prompt.id, userInput);
                            }}
                            disabled={!userInput.trim() || prompt.isLoading}
                            style={{
                              height: 'auto',
                              padding: '12px 20px',
                              background: prompt.isLoading ? '#d9d9d9' : 'linear-gradient(135deg, #1890ff 0%, #722ed1 100%)',
                              border: 'none',
                              borderRadius: '8px',
                              fontWeight: 600,
                              minWidth: 100,
                              color: 'white'
                            }}
                          >
                            {prompt.isLoading ? '处理中...' : '发送'}
                          </Button>
                        </div>
                      </div>
                    </div>

                  </Card>
              );
            }
          )
          )}
        </div>
      </Space>

      {/* 模态框 */}
      {showCreateModal && (
        <CreatePromptModal
          initialData={typeof showCreateModal === 'object' ? {
            content: showCreateModal.content || '',
            parameters: showCreateModal.parameters || [],
            variablesWithValues: showCreateModal.variablesWithValues || {}
          } : {
            content: promptInstances[0]?.content || '',
            parameters: promptInstances[0]?.parameters || [],
            variablesWithValues: promptInstances[0]?.parameterValues || {}
          }}
          quickCreate={typeof showCreateModal === 'object' ? showCreateModal.quickCreate : false}
          modelConfig={typeof showCreateModal === 'object' ? showCreateModal.modelConfig : null}
          models={models}
          onClose={() => setShowCreateModal(false)}
        />
      )}

      {/* 发布版本模态框 - 支持从详情页进入和选择已有Prompt两种情况 */}
      {showPublishModal && (
        <PublishVersionModal
          prompt={typeof showPublishModal === 'object' ? showPublishModal.prompt : (playgroundData || null)}
          newContent={typeof showPublishModal === 'object' ? showPublishModal.content : (promptInstances[0]?.content || '')}
          variables={typeof showPublishModal === 'object' ? showPublishModal.variablesWithValues : {}}
          modelConfig={typeof showPublishModal === 'object' ? showPublishModal.modelConfig : (promptInstances[0]?.modelParams ? {
            modelId: promptInstances[0].selectedModel,
            ...promptInstances[0].modelParams
          } : undefined)}
          models={models}
          onClose={() => setShowPublishModal(false)}
        />
      )}

      {showTemplateModal && (
        <TemplateImportModal
          models={models}
          onImport={(template) => {
            // 使用新的模板导入处理函数，支持模型配置
            handleTemplateImport(showTemplateModal, template);
            setShowTemplateModal(null);
          }}
          onClose={() => setShowTemplateModal(null)}
        />
      )}

      <AddFunctionModal
        open={showFunctionModal}
        onCancel={() => setShowFunctionModal(false)}
        functions={currentPromptInstance?.mockTools || []}
        onOk={(data) => {
          setPromptInstances(v => v.map(p => p.id === selectedSessionId ? {
            ...p,
            mockTools: p?.mockTools ? [...p.mockTools, data] : [data]
          } : p))
          setShowFunctionModal(false)
        }}
      />
      <ViewFunctionModel
        selectedFunction={selectedFunction}
        open={showFunctionViewModal}
        onCancel={() => setShowFunctionViewModal(false)}
        onOk={(data) => {
          setPromptInstances(v => v.map(p => p.id === selectedSessionId ? {
            ...p,
            mockTools: data
          } : p))
          setShowFunctionViewModal(false)
        }}
        functions={currentPromptInstance?.mockTools || []}
      />
    </div>
    </>
  );
};

export default PlaygroundPage;
