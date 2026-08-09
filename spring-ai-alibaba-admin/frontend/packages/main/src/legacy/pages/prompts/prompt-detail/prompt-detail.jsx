import { useState, useEffect, useRef, useCallback, useContext, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Spin, Result, Button, Alert,
  Card, Tag, Typography, Row,
  Col, Input, Select, InputNumber, Space,
  Tooltip, Divider, Badge, Avatar, message,
  Modal, Drawer } from 'antd';
import {
  LoadingOutlined, ArrowLeftOutlined, DownloadOutlined,
  HistoryOutlined, CopyOutlined, DeleteOutlined,
  RocketOutlined, CommentOutlined, EditOutlined,
  SendOutlined, RobotOutlined, ClearOutlined,
  CheckCircleOutlined, ExperimentOutlined, QuestionCircleOutlined,
  MessageOutlined, UserOutlined, PlusOutlined,
  EyeOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import { handleApiError } from '../../../utils/notification';
import { executeStreamingPrompt } from '../../../utils/streamingPrompt';
import PublishVersionModal from '../../../components/PublishVersionModal';
import TemplateImportModal from '../../../components/TemplateImportModal';
import API from '../../../services';
import { ModelsContext } from '../../../context/models';
import dayjs from 'dayjs';
import { extractParametersFromDoubleBrace, safeJSONParse, safeJSONStringify } from '../../../utils/util';
import { buildLegacyPath } from '../../../utils/path';
import AddFunctionModal from './add-function-modal/add-function-modal';
import ViewFunctionModel from './view-function-model/view-function-model';
import FunctionList from './FunctionList';
import $i18n from '@/i18n';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

// 添加闪烁光标的CSS动画样式
const cursorBlinkStyle = `
  @keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
  }
`;

const PromptDetailPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const promptKey = searchParams.get('promptKey');
  const [currentPrompt, setCurrentPrompt] = useState(null);
  const [promptVersions, setPromptVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [defaultPromptInstances] = useState(() => {
    const defaultPromptInstances = safeJSONParse(localStorage.getItem("prompt-sessions") || "{}");
    return defaultPromptInstances;
  })

  const { models, modelNameMap } = useContext(ModelsContext);

  const [showPublishModal, setShowPublishModal] = useState(false);
  const [showTemplateModal, setShowTemplateModal] = useState(null);
  const [showRestoreSuccess, setShowRestoreSuccess] = useState(false);
  const [restoredVersion, setRestoredVersion] = useState(null);
  const [restoredWindowId, setRestoredWindowId] = useState(null);
  const [showFunctionModal, setShowFunctionModal] = useState(false);
  const [showFunctionViewModal, setShowFunctionViewModal] = useState(false);

  // Session-related state
  const [sessions, setSessions] = useState({}); // sessionId -> session data
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [selectedFunction, setSelectedFunction] = useState(null);
  const [recentlyDeletedSessions, setRecentlyDeletedSessions] = useState({}); // 存储最近删除的会话 ID
  const eventSourceRefs = useRef({}); // promptId -> EventSource
  // Add refs for chat containers to enable auto-scroll
  const chatContainerRefs = useRef({}); // promptId -> chat container element
  
  // Get model parameters with fallback to model's defaultParameters
  const getModelParams = (modelId, modelConfig = {}) => {
    console.log('getModelParams called with:', { modelId, modelConfig, availableModels: models.length }); // Debug log
    const selectedModel = models.find(m => m.id === modelId);
    const defaultParams = selectedModel?.defaultParameters || {};
    console.log('Selected model:', selectedModel?.name, 'defaultParams:', defaultParams); // Debug log

    // Filter out model identifier fields from modelConfig before merging
    const { model, modelId: configModelId, ...filteredModelConfig } = modelConfig;

    // Merge filtered modelConfig with defaultParameters dynamically
    const mergedParams = { ...defaultParams, ...filteredModelConfig };
    console.log('Merged params (after filtering model identifiers):', mergedParams); // Debug log
    return mergedParams;
  };

  const [promptInstances, setPromptInstances] = useState([{
    id: 1,
    promptName: '',
    content: '',
    parameters: [],
    parameterValues: {},
    results: [],
    isLoading: false,
    selectedModel: '',
    modelParams: {}, // Start with empty object, will be populated when models load
    chatHistory: [] // 每个prompt独立的对话历史
  }]);

  // 为每个 prompt 实例添加输入状态
  const [promptInputs, setPromptInputs] = useState({});

  // Get default model ID (first available model or fallback)
  const getDefaultModelId = () => {
    return models.length > 0 ? models[0].id : '-';
  };

  // 获取Model Parameters显示值的辅助函数
  const getDisplayModelParams = (modelParams, selectedModel) => {
    if (modelParams) {
      // Filter out model identifier fields from display parameters
      const { model, modelId, ...filteredParams } = modelParams;
      return filteredParams; // Return filtered parameters dynamically
    }

    // 如果没有modelParams，使用Model的默认参数
    const selectedModelData = models.find(m => m.id === selectedModel);
    const defaultParams = selectedModelData?.defaultParameters || {};

    // Filter out any model identifier fields from default parameters as well
    const { model, modelId, ...filteredDefaultParams } = defaultParams;
    return filteredDefaultParams; // Return filtered default parameters dynamically
  };

  // 🔥 修复：使用useRef避免状态竞争
  const isRestoringRef = useRef(false);
  const hasInitializedRef = useRef(false);
  const timeoutRefs = useRef([]);

  // Load Prompt details
  const loadPromptDetail = useCallback(async () => {
    if (!promptKey) {
      navigate(buildLegacyPath('/prompts'));
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // 1. 首先获取 Prompt 基本信息
      const promptResponse = await API.getPrompt({ promptKey });

      if (promptResponse.code !== 200) {
        throw new Error(promptResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.prompt.details', dm: '获取 Prompt 详情失败' }));
      }

      const promptData = promptResponse.data;

      // 2. 并行获取版本列表和Latest Version详情
      const promises = [
        API.getPromptVersions({ promptKey, pageNo: 1, pageSize: 100 })
      ];

      // 如果有Latest Version，获取其详细信息
      if (promptData.latestVersion) {
        promises.push(
          API.getPromptVersion({ promptKey, version: promptData.latestVersion })
        );
      }

      const responses = await Promise.all(promises);
      const [versionsResponse, versionDetailResponse] = responses;

      // 3. 处理版本列表
      let versions = [];
      if (versionsResponse.code === 200) {
        versions = versionsResponse.data.pageItems || [];
        setPromptVersions(versions);
      } else {
        console.warn('Failed to get version list:', versionsResponse.message);
        setPromptVersions([]);
      }

      // 4. 聚合 Prompt 数据和Latest Version详情
      let aggregatedPrompt = { ...promptData };

      if (versionDetailResponse && versionDetailResponse.code === 200) {
        const versionDetail = versionDetailResponse.data;

        // 聚合Latest Version的详细信息到 Prompt 对象中
        aggregatedPrompt = {
          ...promptData,
          currentVersionDetail: {
            template: versionDetail.template,
            variables: versionDetail.variables ? safeJSONParse(versionDetail.variables) : {},
            modelConfig: versionDetail.modelConfig ? safeJSONParse(versionDetail.modelConfig) : {},
            versionDescription: versionDetail.versionDescription,
            createTime: versionDetail.createTime,
            previousVersion: versionDetail.previousVersion
          }
        };
      }

      setCurrentPrompt(aggregatedPrompt);

    } catch (err) {
      console.error('Failed to load Prompt details:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.prompts.load.prompt.details', dm: '加载 Prompt 详情' }));
      setError(err.message || $i18n.get({ id: 'legacy.prompts.loading.failed.please.try.again.later', dm: '加载失败，请稍后重试' }));
    } finally {
      setLoading(false);
    }
  }, [promptKey, navigate]);

  // 🔥 修复：清理定时器的函数
  const clearAllTimeouts = useCallback(() => {
    timeoutRefs.current.forEach(timeout => clearTimeout(timeout));
    timeoutRefs.current = [];
  }, []);

  // Session management functions
  const loadSession = async (sessionId) => {
    try {
      const response = await API.getPromptSession(sessionId);
      if (response.code === 200) {
        setSessions(prev => ({
          ...prev,
          [sessionId]: response.data
        }));
        return response.data;
      } else {
        message.error(response.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.session', dm: '获取会话失败' }));
        return null;
      }
    } catch (error) {
      console.error('Load session error:', error);
      message.error($i18n.get({ id: 'legacy.prompts.failed.to.get.session', dm: '获取会话失败' }));
      return null;
    }
  };

  const deleteSession = async (sessionId) => {
    try {
      const response = await API.deletePromptSession(sessionId);
      if (response.code === 200) {
        setSessions(prev => {
          const newSessions = { ...prev };
          delete newSessions[sessionId];
          return newSessions;
        });
        message.success($i18n.get({ id: 'legacy.prompts.session.deleted.successfully', dm: '会话删除成功' }));
        return true;
      } else {
        message.error(response.message || $i18n.get({ id: 'legacy.prompts.failed.to.delete.session', dm: '删除会话失败' }));
        return false;
      }
    } catch (error) {
      console.error('Delete session error:', error);
      message.error($i18n.get({ id: 'legacy.prompts.failed.to.delete.session', dm: '删除会话失败' }));
      return false;
    }
  };

  // 单个 Prompt 执行函数
  const runSinglePrompt = async (promptInstance, inputText, newSession = false) => {
    const {
       id: promptId, content, parameterValues, selectedModel, modelParams, sessionId, mockTools ,
       enableFn
      } = promptInstance;

    const config = {
      promptId,
      content,
      parameterValues,
      selectedModel,
      modelParams,
      sessionId: newSession ? null : sessionId, // Force new session if requested
      promptKey: currentPrompt.promptKey,
      version: currentPrompt.latestVersion || '1.0',
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
        setPromptInstances(prev => {
          const instance = prev.map(prompt =>
            prompt.id === promptId
              ? { ...prompt, sessionId }
              : prompt
          );
          defaultPromptInstances[promptKey] = instance.map(v => v.sessionId);
          localStorage.setItem("prompt-sessions", safeJSONStringify({[promptKey]: instance.map(v => v.sessionId)}));
          return instance
        });
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
      formatTime: (timestamp) => dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss'),
      replaceParameters
    };

    // 为单个 prompt 添加用户消息到对话History
    setPromptInstances(prev => prev.map(prompt => {
      if (prompt.id === promptId) {
        const userMessage = {
          id: Date.now() + prompt.id,
          type: 'user',
          content: inputText,
          timestamp: new Date().toLocaleString()
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

  // 🔥 修复：组件卸载时清理资源
  useEffect(() => {
    return () => {
      // 清理所有EventSource连接
      Object.values(eventSourceRefs.current).forEach(eventSource => {
        if (eventSource && eventSource.close) {
          eventSource.close();
        }
      });
      eventSourceRefs.current = {};

      // 清理定时器
      clearAllTimeouts();
    };
  }, [clearAllTimeouts]);

  // Auto-scroll to bottom when chat history changes
  useEffect(() => {
    promptInstances.forEach(prompt => {
      const chatContainer = chatContainerRefs.current[prompt.id];
      if (chatContainer && prompt.chatHistory && prompt.chatHistory.length > 0) {
        // Use setTimeout to ensure DOM is updated
        setTimeout(() => {
          chatContainer.scrollTo({
            top: chatContainer.scrollHeight,
            behavior: 'smooth'
          });
        }, 100);
      }
    });
  }, [promptInstances.map(p => p.chatHistory).flat()]);

  // 🔥 修复：安全的URL参数清理函数
  const clearRestoreParams = useCallback(() => {
    const newSearchParams = new URLSearchParams(searchParams);
    const hasRestoreParams = newSearchParams.has('restoreVersionId') || newSearchParams.has('targetWindowId');

    console.log('=== 清理URL参数 ===');
    console.log('当前参数:', Object.fromEntries(newSearchParams));
    console.log('需要清理:', hasRestoreParams);

    if (hasRestoreParams) {
      newSearchParams.delete('restoreVersionId');
      newSearchParams.delete('targetWindowId');
      console.log('=== 清理后参数 ===', Object.fromEntries(newSearchParams));
      // 🔥 修复：使用React Router的方式更新URL
      setSearchParams(newSearchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const resetPromptInstances = (sessions) => {
    Promise.all(sessions.map(sessionId => API.getPromptSession(sessionId)))
    .then((resList) => {
      const datas = resList.map(v => v.data);
      const instances = datas.map((data, idx) => {
        const { modelId, allParameters } = data.modelConfig;
        const variables = safeJSONParse(data.variables || '{}');
        return {
          id: idx,
          promptName: data.promptKey,
          content: data.template,
          parameters: Object.keys(variables),
          parameterValues: variables,
          results: [],
          isLoading: false,
          selectedModel: modelId,
          modelParams: allParameters,
          sessionId: data.sessionId,
          chatHistory: data.messages.map((msg, index) => {
            const displayParams = msg.role === 'assistant' && msg.modelParams
              ? msg.modelParams
              : getDisplayModelParams(null, msg.model || getDefaultModelId());

            const metrics = msg.metrics || {}
            return {
              id: Date.now() + index,
              type: msg.role === 'user' ? 'user' : 'assistant',
              content: msg.content,
              timestamp: dayjs(msg.timestamp).format('YYYY-MM-DD HH:mm:ss'),
              model: msg.role === 'assistant' ? 'AI Model' : undefined,
              modelParams: msg.role === 'assistant' ? displayParams : undefined,
              ...metrics
            }
          })
        }
      })
      setPromptInstances(instances);
      window.$$_prompts = instances;
    })
  }

  // 初始加载数据
  useEffect(() => {
    loadPromptDetail();
  }, [loadPromptDetail]);

  // Update prompt instances when models are loaded to use proper default parameters
  useEffect(() => {
    console.log('PromptDetailPage - useEffect triggered, models:', models.length, 'promptInstances:', promptInstances.length); // Debug log
    if (models.length > 0 && promptInstances.length > 0) {
      console.log('PromptDetailPage - Force updating all prompt instances with model parameters'); // Debug log
      const defaultModelId = models[0].id;
      let defaultParams = models[0].defaultParameters || {};

      // Add fallback parameters if API doesn't return any
      if (Object.keys(defaultParams).length === 0) {
        defaultParams = {
          max_tokens: 1000,
          temperature: 0.7,
          top_p: 1.0
        };
        console.log('Using fallback default parameters:', defaultParams);
      }

      console.log('Default model defaultParameters:', defaultParams);

      setPromptInstances(prev => {
        const updated = prev.map(instance => {
          const currentModelParams = instance.modelParams || {};
          const shouldUpdate = Object.keys(currentModelParams).length === 0 || !instance.selectedModel;

          console.log('Processing instance:', instance.id, 'shouldUpdate:', shouldUpdate, 'currentParams:', currentModelParams);

          if (shouldUpdate) {
            return {
              ...instance,
              selectedModel: instance.selectedModel || defaultModelId,
              modelParams: defaultParams
            };
          }
          return instance;
        });

        console.log('Updated instances:', updated);
        return updated;
      });
    }
  }, [models]); // Only depend on models, not promptInstances to avoid infinite loops

  // Monitor promptInstances changes
  useEffect(() => {
    console.log('=== PROMPT INSTANCES CHANGED ===');
    console.log('Current promptInstances:', promptInstances);
    promptInstances.forEach((instance, index) => {
      console.log(`Instance ${index}:`, {
        id: instance.id,
        selectedModel: instance.selectedModel,
        modelParams: instance.modelParams,
        modelParamsKeys: Object.keys(instance.modelParams || {})
      });
    });
  }, [promptInstances]);

  // 加载会话数据当模态框打开时
  useEffect(() => {
    if (showSessionModal && selectedSessionId && !sessions[selectedSessionId]) {
      loadSession(selectedSessionId);
    }
  }, [showSessionModal, selectedSessionId]);

  // 🔥 修复：统一的初始化和恢复逻辑
  useEffect(() => {
    if (loading || !currentPrompt) {
      return;
    }

    const restoreVersionId = searchParams.get('restoreVersionId');
    const targetWindowId = searchParams.get('targetWindowId');

    // 🔥 修复：版本恢复逻辑
    if (restoreVersionId && !isRestoringRef.current) {
      isRestoringRef.current = true;
      hasInitializedRef.current = true; // 🔥 关键：立即标记已初始化，防止后续初始化逻辑执行

      const versionToRestore = promptVersions?.find(v => v.version === restoreVersionId);

      if (versionToRestore) {
        console.log('=== 开始版本恢复 ===');
        console.log('版本号:', versionToRestore.version);

        // 获取版本详细内容
        const loadVersionDetail = async () => {
          try {
            const versionDetailResponse = await API.getPromptVersion({
              promptKey,
              version: versionToRestore.version
            });

            if (versionDetailResponse.code === 200) {
              const versionDetail = versionDetailResponse.data;
              const content = versionDetail.template || '';
              const variables = versionDetail.variables ? safeJSONParse(versionDetail.variables) : {};
              const parameters = Object.keys(variables);
              const modelConfig = versionDetail.modelConfig ? safeJSONParse(versionDetail.modelConfig) : {};

              console.log('版本内容:', content);

              const selectedModelId = modelConfig?.modelId || getDefaultModelId();
              const restoredWindowConfig = {
                promptName: currentPrompt.promptKey,
                content,
                parameters,
                parameterValues: parameters.reduce((acc, param) => {
                  acc[param] = variables[param] || '';
                  return acc;
                }, {}),
                selectedModel: selectedModelId,
                modelParams: getModelParams(selectedModelId, modelConfig),
                isLoading: false,
              };

              console.log('=== 恢复Configuration ===', restoredWindowConfig);

              // 🔥 关键修复：立即执行恢复逻辑，不使用延迟
              if (targetWindowId) {
                const windowId = parseInt(targetWindowId);
                console.log('=== 恢复到指定窗口 ===', windowId);

                setPromptInstances(_prev => {
                  const prev = window.$$_prompts || _prev;
                  if (prev.length === 0) {
                    return [{
                      id: windowId,
                      ...restoredWindowConfig
                    }];
                  }

                  const targetWindowExists = prev.some(w => w.id === windowId);

                  if (targetWindowExists) {
                    return prev.map(win =>
                      win.id === windowId
                        ? { ...win, ...restoredWindowConfig, sessionId: win.sessionId }
                        : win
                    );
                  } else {
                    return [...prev, {
                      id: windowId,
                      ...restoredWindowConfig
                    }];
                  }
                });
              } else {
                setPromptInstances(prev => {
                  if (prev.length === 0) {
                    return [{
                      id: 1,
                      ...restoredWindowConfig
                    }];
                  } else {
                    return prev.map((window, index) =>
                      index === 0
                        ? { ...window, ...restoredWindowConfig }
                        : window
                    );
                  }
                });
              }

              // 设置恢复成功状态
              setRestoredVersion(versionToRestore);
              setRestoredWindowId(targetWindowId ? parseInt(targetWindowId) : 1);
              setShowRestoreSuccess(true);

              // 🔥 修复：使用安全的定时器管理
              const successTimeoutId = setTimeout(() => {
                setShowRestoreSuccess(false);
                setRestoredVersion(null);
                setRestoredWindowId(null);
              }, 5000);
              timeoutRefs.current.push(successTimeoutId);

              // 🔥 修复：安全清理URL参数
              clearRestoreParams();

              console.log('=== 版本恢复完成 ===');
            } else {
              throw new Error(versionDetailResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.version.details', dm: '获取版本详情失败' }));
            }
          } catch (err) {
            console.error('Failed to restore version:', err);
            handleApiError(err, $i18n.get({ id: 'legacy.prompts.restore.version', dm: '恢复版本' }));
            setError(err.message || $i18n.get({ id: 'legacy.prompts.failed.to.restore.version', dm: '恢复版本失败' }));
          } finally {
            // 🔥 修复：安全重置标志
            const resetTimeoutId = setTimeout(() => {
              isRestoringRef.current = false;
            }, 100);
            timeoutRefs.current.push(resetTimeoutId);
          }
        };

        loadVersionDetail();
      } else {
        console.error('Version to restore was not found:', restoreVersionId);
        console.log('Available versions:', promptVersions);
        isRestoringRef.current = false;
        clearRestoreParams();
      }

      return; // 🔥 修复：恢复逻辑执行后直接返回，避免执行初始化逻辑
    }

    // 🔥 修复：正常初始化逻辑 - 使用Latest Version初始化
    if (!hasInitializedRef.current && !isRestoringRef.current && !restoreVersionId) {
      console.log('=== 正常初始化 ===');
      hasInitializedRef.current = true;

      // 使用聚合后的数据进行初始化
      if (currentPrompt.currentVersionDetail) {
        const versionDetail = currentPrompt.currentVersionDetail;
        const content = versionDetail.template || '';
        const variables = versionDetail.variables || {};
        const parameters = Object.keys(variables);
        const modelConfig = versionDetail.modelConfig || {};

        const selectedModelId = modelConfig?.modelId || getDefaultModelId();
        const sessions = defaultPromptInstances[promptKey];
        if (sessions?.length) {
          resetPromptInstances(sessions);
        } else {
          setPromptInstances([{
            id: 1,
            promptName: currentPrompt.promptKey,
            content,
            parameters,
            parameterValues: variables,
            results: [],
            isLoading: false,
            selectedModel: selectedModelId,
            modelParams: getModelParams(selectedModelId, modelConfig),
            chatHistory: []
          }]);
        }
      } else {
        // 如果没有版本详情，创建空的实例
        const sessions = defaultPromptInstances[promptKey];
        if (sessions?.length) {
          resetPromptInstances(sessions);
        } else {
          const defaultModelId = getDefaultModelId();
          setPromptInstances([{
            id: 1,
            promptName: currentPrompt.promptKey,
            content: '',
            parameters: [],
            parameterValues: {},
            results: [],
            isLoading: false,
            selectedModel: defaultModelId,
            modelParams: getModelParams(defaultModelId),
            chatHistory: []
          }]);
        }

      }
    }
  }, [currentPrompt, promptVersions, navigate, searchParams, clearRestoreParams, promptKey]);

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
      alert($i18n.get({ id: 'legacy.prompts.you.can.compare.up.to.3.configurations.at.once', dm: '最多只能同时对比3个配置' }));
      return;
    }

    const promptToCopy = promptInstances.find(p => p.id === promptId);
    if (promptToCopy) {
      const newPrompt = {
        ...promptToCopy,
        id: Date.now(),
        parameterValues: { ...promptToCopy.parameterValues },
        results: [],
        isLoading: false,
        modelParams: { ...promptToCopy.modelParams },
        chatHistory: [], // 新窗口独立的对话历史
        sessionId: "",
      };
      setPromptInstances(prev => {
        window.$$_prompts = [...prev, newPrompt];
        return [...prev, newPrompt]
      });
    }
  };

  const removePrompt = (promptId) => {
    if (promptInstances.length > 1) {
      setPromptInstances(prev => {
        const filtered = prev.filter(p => p.id !== promptId);
        defaultPromptInstances[promptKey] = filtered;
        localStorage.setItem("prompt-sessions", safeJSONStringify(defaultPromptInstances));
        window.$$_prompts = filtered;
        return filtered
      });
    }
  };

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

      // Clear指定prompt的对话History和会话
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

      // Clear所有prompt的对话History和会话
      setPromptInstances(prev => prev.map(prompt =>
        ({ ...prompt, chatHistory: [], sessionId: null })
      ));
    }
  };

  // Restore Session功能
  const restoreSession = async (promptId) => {
    const sessionId = recentlyDeletedSessions[promptId];
    if (!sessionId) {
      message.error($i18n.get({ id: 'legacy.prompts.no.session.is.available.to.restore', dm: '没有可恢复的会话' }));
      return false;
    }

    try {
      const response = await API.getPromptSession(sessionId);
      if (response.code === 200) {
        const sessionData = response.data;

        // 转换会话数据为聊天History格式
        const chatHistory = sessionData.messages.map((msg, index) => {
          const displayParams = msg.role === 'assistant' && msg.modelParams ?
            msg.modelParams :
            getDisplayModelParams(null, msg.model || getDefaultModelId());

          return {
            id: Date.now() + index,
            type: msg.role === 'user' ? 'user' : 'assistant',
            content: msg.content,
            timestamp: dayjs(msg.timestamp).format('YYYY-MM-DD HH:mm:ss'),
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

        message.success($i18n.get({ id: 'legacy.prompts.session.restored.successfully', dm: '会话恢复成功' }));
        return true;
      } else {
        message.error(response.message || $i18n.get({ id: 'legacy.prompts.failed.to.restore.session', dm: '恢复会话失败' }));
        return false;
      }
    } catch (error) {
      console.error('Restore session error:', error);
      message.error($i18n.get({ id: 'legacy.prompts.failed.to.restore.session', dm: '恢复会话失败' }));
      return false;
    }
  };

  const replaceParameters = (content, parameterValues) => {
    let result = content;
    Object.entries(parameterValues).forEach(([key, value]) => {
      result = result.replace(new RegExp(`\\{\\{${key}\\}\\}`, 'g'), value || `{{${key}}}`);
    });
    return result;
  };

  const handleContentChange = (id, content) => {
    const parameters = extractParametersFromDoubleBrace(content);
    setPromptInstances(prev => prev.map(prompt =>
      prompt.id === id
        ? {
          ...prompt,
          content,
          parameters,
          parameterValues: parameters.reduce((acc, param) => {
            acc[param] = prompt.parameterValues[param] || '';
            return acc;
          }, {})
        }
        : prompt
    ));
  };

  // 处理模板Import，包括Model Configuration
  const handleTemplateImport = (promptId, template) => {
    const parameters = extractParametersFromDoubleBrace(template.content);
    const templateModelConfig = template.modelConfig || {};

    // 如果模板有Model Configuration，使用模板的配置；否则使用当前选中的Model的默认参数
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
          modelParams: modelParams
        }
        : prompt
    ));
  };

  // 单个配置的对话Send函数
  const handleSendMessage = (promptId, inputText) => {
    if (!inputText?.trim()) return;
    
    const promptInstance = promptInstances.find(p => p.id === promptId);
    if (promptInstance) {
      runSinglePrompt(promptInstance, inputText);
      // Clear输入框
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

  const currentPromptInstance = useMemo(() => promptInstances.find(p => p.id === selectedSessionId), [promptInstances, selectedSessionId])

  console.log(promptInstances, 'asd...qwe')

  if (loading) {
    return (
      <div className="p-8 fade-in">
        <div className="flex items-center justify-center h-64">
          <Spin
            indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />}
            size="large"
          >
            <div className="text-center pt-4">
              <p className="text-gray-600 mt-4">{$i18n.get({ id: 'legacy.prompts.loading.prompt.details', dm: '加载 Prompt 详情中...' })}</p>
            </div>
          </Spin>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 fade-in">
        <Result
          status="error"
          title={$i18n.get({ id: 'legacy.prompts.failed.to.load.prompt.details', dm: '加载 Prompt 详情失败' })}
          subTitle={error}
          extra={[
            <Button type="primary" key="retry" onClick={() => loadPromptDetail()}>
              {$i18n.get({ id: 'legacy.prompts.retry', dm: '重试' })}
            </Button>,
            <Button key="back" onClick={() => navigate(buildLegacyPath('/prompts'))}>
              {$i18n.get({ id: 'legacy.prompts.back.to.list', dm: '返回列表' })}
            </Button>,
          ]}
        />
      </div>
    );
  }

  if (!currentPrompt) {
    return (
      <div className="p-8 fade-in">
        <Result
          status="404"
          title={$i18n.get({ id: 'legacy.prompts.prompt.not.found', dm: 'Prompt 不存在' })}
          subTitle={$i18n.get({ id: 'legacy.prompts.the.requested.prompt.was.not.found.it.may.have.been.deleted', dm: '未找到指定的 Prompt，可能已被删除或不存在。' })}
          extra={
            <Button type="primary" onClick={() => navigate(buildLegacyPath('/prompts'))}>
              {$i18n.get({ id: 'legacy.prompts.back.to.list', dm: '返回列表' })}
            </Button>
          }
        />
      </div>
    );
  }

  const currentSession = sessions[selectedSessionId];

  return (
    <>
      <style>{cursorBlinkStyle}</style>
      <div className="p-8 fade-in">
      <div className="mb-8">
        <div className='flex items-center gap-3 mb-2' >
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate(buildLegacyPath('/prompts'))}
            size="large"
          />
          <Title level={2} className='m-0' >{currentPrompt.promptKey}</Title>
        </div>
        <Paragraph type="secondary">{$i18n.get({ id: 'legacy.prompts.test.and.debug.your.ai.prompts', dm: '测试和调试你的AI提示词' })}</Paragraph>
      </div>

      <div className="mb-8" />

      {/* Prompt 详情信息卡片 */}
      <Card className='mb-6' >
        <Row gutter={[24, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <div>
              <Text type="secondary" className='text-sm uppercase' >
                Prompt Key
              </Text>
              <div className='mt-1' >
                <Text strong className='text-lg' >{currentPrompt.promptKey}</Text>
              </div>
            </div>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <div>
              <Text type="secondary" className='text-sm uppercase' >
                {$i18n.get({ id: 'legacy.prompts.latest.version', dm: '最新版本' })}
              </Text>
              <div className='mt-1' >
                {currentPrompt.latestVersion ? (
                  <Tag color="blue">{currentPrompt.latestVersion}</Tag>
                ) : (
                  <Tag color="default">{$i18n.get({ id: 'legacy.prompts.no.version.3', dm: '无版本' })}</Tag>
                )}
              </div>
            </div>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <div>
              <Text type="secondary" className='text-sm uppercase' >
                {$i18n.get({ id: 'legacy.prompts.version.status', dm: '版本状态' })}
              </Text>
              <div className='mt-1' >
                {currentPrompt.latestVersionStatus ? (
                  currentPrompt.latestVersionStatus === 'release' ? (
                    <Tag color="success" icon={<CheckCircleOutlined />}>
                      {$i18n.get({ id: 'legacy.prompts.release', dm: '正式版本' })}
                    </Tag>
                  ) : (
                    <Tag color="processing" icon={<ExperimentOutlined />}>
                      {$i18n.get({ id: 'legacy.prompts.pre.release', dm: 'PRE版本' })}
                    </Tag>
                  )
                ) : (
                  <Tag color="default" icon={<QuestionCircleOutlined />}>
                    {$i18n.get({ id: 'legacy.prompts.unknown.status', dm: '未知状态' })}
                  </Tag>
                )}
              </div>
            </div>
          </Col>

          <Col xs={24} sm={12} lg={6}>
            <div>
              <Text type="secondary" className='text-sm uppercase' >
                {$i18n.get({ id: 'legacy.prompts.version.count', dm: '版本数量' })}
              </Text>
              <div className='mt-1' >
                <Text strong className='text-lg' >
                  {$i18n.get(
                    { id: 'legacy.prompts.version.count.value', dm: '{count} 个版本' },
                    { count: promptVersions.length },
                  )}
                </Text>
              </div>
            </div>
          </Col>
        </Row>

        <div className="flex mt-3">
          <div className='flex-1'>
            <Text type="secondary" className='text-sm uppercase' >
              {$i18n.get({ id: 'legacy.prompts.description', dm: '描述' })}
            </Text>
            <div className='mt-1' >
              <Text>{currentPrompt?.promptDescription || "-"}</Text>
            </div>
          </div>

          {currentPrompt.tags && (
            <div className='flex-1 ml-6'>
              <Text type="secondary" className='text-sm uppercase' >
                {$i18n.get({ id: 'legacy.prompts.tags', dm: '标签' })}
              </Text>
              <div className='mt-2' >
                <Space size={[0, 8]} wrap>
                  {(() => {
                    try {
                      const tags = safeJSONParse(currentPrompt.tags || '[]');
                      return tags.map((tag, index) => (
                        <Tag key={index} color="geekblue">
                          {tag}
                        </Tag>
                      ));
                    } catch (e) {
                      const tags = currentPrompt.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
                      return tags.map((tag, index) => (
                        <Tag key={index} color="geekblue">
                          {tag}
                        </Tag>
                      ));
                    }
                  })()}
                </Space>
              </div>
            </div>
          )}
        </div>

        <Divider />
        <Row gutter={[16, 8]}>
          <Col span={12}>
            <Text type="secondary">
              Created: {dayjs(currentPrompt.createTime).format('YYYY-MM-DD HH:mm:ss')}
            </Text>
          </Col>
          <Col span={12}>
            <Text type="secondary">
              Updated: {dayjs(currentPrompt.updateTime).format('YYYY-MM-DD HH:mm:ss')}
            </Text>
          </Col>
        </Row>
      </Card>

      {/* 横向布局：配置和Chat Test整合 */}
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
        {promptInstances.map((prompt, index) => {
          const userInput = promptInputs[prompt.id] || '';
          
          return (
            <Card key={prompt.id} className="h-fit" size={promptInstances.length >= 3 ? "small" : "default"}>
              {/* 配置区域 */}
              <div className={promptInstances.length >= 3 ? "mb-4" : "mb-6"}>
                {/* 标题栏 */}
                <div className='flex flex-col gap-3 mb-4'>
                  <div className='flex flex-wrap justify-between items-center gap-2'>
                    <div>
                      <Text strong size="lg">
                        {$i18n.get({ id: 'legacy.prompts.configuration', dm: '配置 {n}' }, { n: index + 1 })}
                      </Text>
                      <Text type="secondary" className='ml-2'>
                        ({currentPrompt.promptKey})
                      </Text>
                    </div>
                    <div className='flex flex-wrap gap-2 items-center'>
                      {/* 功能按钮组 - 响应式布局 */}
                      <div className='flex flex-wrap gap-2'>
                        <Button
                          type="primary" 
                          icon={<PlusOutlined />}
                          size={promptInstances.length >= 3 ? "small" : "default"}
                          onClick={() => {
                            setShowFunctionModal(true);
                            setSelectedSessionId(prompt.id);
                          }}
                        >
                          {promptInstances.length >= 3 ? $i18n.get({ id: 'legacy.prompts.add', dm: '新增' }) : $i18n.get({ id: 'legacy.prompts.add.function', dm: '新增函数' })}
                        </Button>
                        <Button
                          type="primary"
                          icon={<DownloadOutlined />}
                          size={promptInstances.length >= 3 ? "small" : "default"}
                          onClick={() => setShowTemplateModal(prompt.id)}
                          style={{ background: 'linear-gradient(90deg, #16a085 0%, #2ecc71 100%)', border: 'none' }}
                        >
                          {promptInstances.length >= 3 ? $i18n.get({ id: 'legacy.prompts.import', dm: '导入' }) : $i18n.get({ id: 'legacy.prompts.import.template', dm: '导入模板' })}
                        </Button>
                        {promptVersions && promptVersions.length > 0 && (
                          <Button
                            icon={<HistoryOutlined />}
                            size={promptInstances.length >= 3 ? "small" : "default"}
                            onClick={() => navigate(buildLegacyPath('/version-history', { promptKey, targetWindowId: prompt.id }))}
                          >
                            {promptInstances.length >= 3 ? $i18n.get({ id: 'legacy.prompts.history', dm: '历史' }) : $i18n.get({ id: 'legacy.prompts.version.history', dm: '版本历史' })}
                          </Button>
                        )}
                        <Button
                          type="primary"
                          icon={<RocketOutlined />}
                          size={promptInstances.length >= 3 ? "small" : "default"}
                          disabled={!prompt.content.trim()}
                          onClick={() => setShowPublishModal({
                            prompt: currentPrompt,
                            content: prompt.content,
                            variablesWithValues: prompt.parameterValues,
                            modelConfig: {
                              modelId: prompt.selectedModel,
                              ...(() => {
                                const { model, modelId, ...filteredParams } = prompt.modelParams || {};
                                return filteredParams;
                              })()
                            }
                          })}
                        >
                          {promptInstances.length >= 3 ? $i18n.get({ id: 'legacy.prompts.publish.2', dm: '发布' }) : $i18n.get({ id: 'legacy.prompts.publish.new.version', dm: '发布新版本' })}
                        </Button>
                      </div>
                      {/* 基础操作按钮 - 只显示最重要的 */}
                      <Space size="small">
                        <Button
                          type="text"
                          icon={<CopyOutlined />}
                          onClick={() => copyPrompt(prompt.id)}
                          disabled={promptInstances.length >= 3}
                          title={promptInstances.length >= 3 ? $i18n.get({ id: 'legacy.prompts.you.can.debug.up.to.3.configurations.at.once', dm: '最多同时调试3个配置' }) : $i18n.get({ id: 'legacy.prompts.copy.configuration.for.comparison', dm: '复制配置进行对比' })}
                        />
                        {promptInstances.length > 1 && (
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => removePrompt(prompt.id)}
                            title={$i18n.get({ id: 'legacy.prompts.delete.configuration', dm: '删除配置' })}
                          />
                        )}
                      </Space>
                    </div>
                  </div>
                </div>

                {/* 状态提示区域 */}
                <div className="mb-4">
                  {showRestoreSuccess && restoredVersion && restoredWindowId === prompt.id ? (
                    <Alert
                      message={$i18n.get({ id: 'legacy.prompts.version.restored.successfully', dm: '版本恢复成功！' })}
                      description={$i18n.get({ id: 'legacy.prompts.restored.version.content', dm: '已恢复版本 {version} 的内容' }, { version: restoredVersion.version })}
                      type="success"
                      showIcon
                      closable
                      onClose={() => {
                        setShowRestoreSuccess(false);
                        setRestoredVersion(null);
                        setRestoredWindowId(null);
                      }}
                    />
                  ) : null}
                </div>

                <div className={promptInstances.length >= 3 ? "space-y-3" : "space-y-4"}>
                  {/* Prompt Content展示 */}
                  <div>
                    <Text strong className="block mb-2">
                      {$i18n.get({ id: 'legacy.prompts.prompt.content', dm: 'Prompt内容' })}
                    </Text>
                    <TextArea
                      value={prompt.content}
                      onChange={(e) => handleContentChange(prompt.id, e.target.value)}
                      placeholder={$i18n.get({ id: 'legacy.prompts.enter.prompt.content.use.to.define.parameters', dm: '输入Prompt内容，使用 {{参数名}} 来定义参数...' })}
                      style={{
                        height: promptInstances.length >= 3 ? 100 : 120,
                        resize: 'none'
                      }}
                      autoSize={false}
                    />
                  </div>

                  {/* Model Configuration区域 */}
                  <div>
                    <Space direction="vertical" className='w-full' size="small">
                      {/* Model选择 */}
                      <div>
                        <Text strong className='mb-2 block'>
                          {$i18n.get({ id: 'legacy.prompts.model', dm: '模型' })}
                        </Text>
                        <Select
                          value={prompt.selectedModel}
                          onChange={(value) => updatePromptModel(prompt.id, value)}
                          style={{ width: '100%' }}
                        >
                          {models.map((model) => (
                            <Select.Option key={model.id} value={model.id}>
                              {model.name}
                            </Select.Option>
                          ))}
                        </Select>
                      </div>

                      {/* Model Parameters */}
                      <Card size="small" style={{ backgroundColor: '#fafafa' }}>
                        <Text strong className="block mb-2">
                          {$i18n.get({ id: 'legacy.prompts.model.parameters', dm: '模型参数' })}
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

                  {/* Parameter Configuration */}
                  {prompt.parameters.length > 0 && (
                    <div>
                      <Text strong className="block mb-2">
                        {$i18n.get({ id: 'legacy.prompts.parameter.configuration', dm: '参数配置' })}
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
                              placeholder={$i18n.get({ id: 'legacy.prompts.enter.a.value.for', dm: '输入 {param} 的值...' }, { param: param })}
                              size="small"
                            />
                          </Col>
                        ))}
                      </Row>
                    </div>
                  )}
                </div>
              </div>

              <Divider />

              {/* Chat Test区域 */}
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <Avatar icon={<CommentOutlined />} style={{ backgroundColor: '#e6f7ff' }} />
                    <div>
                      <Text strong className="text-lg">{$i18n.get({ id: 'legacy.prompts.chat.test', dm: '对话测试' })}</Text>
                      <div>
                        <Text type="secondary" className="text-sm">
                          {$i18n.get({ id: 'legacy.prompts.test.configuration', dm: '测试配置 {n} 的效果' }, { n: index + 1 })}
                          {prompt.sessionId && (
                            <Tag color="green" size="small" className="ml-2">
                              {$i18n.get({ id: 'legacy.prompts.session', dm: '会话: {id}...' }, { id: prompt.sessionId.substring(0, 8) })}
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
                        title={$i18n.get({ id: 'legacy.prompts.restore.previous.session', dm: '恢复上一次会话' })}
                        style={{ color: '#52c41a' }}
                      >
                        {$i18n.get({ id: 'legacy.prompts.restore.session', dm: '恢复会话' })}
                      </Button>
                    )}
                    {prompt.sessionId && (
                      <Space size={2}>
                        <Button
                          type="text"
                          size="small"
                          icon={<UserOutlined />}
                          onClick={() => {
                            setSelectedSessionId(prompt.sessionId);
                            setShowSessionModal(true);
                          }}
                          title={$i18n.get({ id: 'legacy.prompts.view.session.details', dm: '查看会话详情' })}
                        />
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={async () => {
                            Modal.confirm({
                              title: $i18n.get({ id: 'legacy.prompts.delete.session', dm: '删除会话' }),
                              content: $i18n.get({ id: 'legacy.prompts.delete.this.session.this.will.clear.all.chat.history', dm: '确定要删除这个会话吗？这将清除所有对话历史。' }),
                              onOk: async () => {
                                const success = await deleteSession(prompt.sessionId);
                                if (success) {
                                  setPromptInstances(prev => prev.map(p =>
                                    p.id === prompt.id
                                      ? { ...p, sessionId: null, chatHistory: [] }
                                      : p
                                  ));
                                }
                              }
                            });
                          }}
                          title={$i18n.get({ id: 'legacy.prompts.delete.session', dm: '删除会话' })}
                        />
                      </Space>
                    )}
                    {prompt.chatHistory && prompt.chatHistory.length > 0 && (
                      <Button
                        type="text"
                        size="small"
                        icon={<ClearOutlined />}
                        onClick={() => clearChatHistory(prompt.id)}
                        title={$i18n.get({ id: 'legacy.prompts.clear.chat', dm: '清空对话' })}
                      >
                        {$i18n.get({ id: 'legacy.prompts.clear', dm: '清空' })}
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
                        {$i18n.get({ id: 'legacy.prompts.ready.to.start.chatting', dm: '等待开始对话' })}
                      </Title>
                      <Text type="secondary" style={{ fontSize: '13px' }}>
                        {$i18n.get({ id: 'legacy.prompts.send.a.message.in.the.input.field.below.to.start.testing', dm: '在下方输入框中发送消息开始测试' })}
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
                                        message.success($i18n.get({ id: 'legacy.prompts.copied.to.clipboard', dm: '已复制到剪贴板' }));
                                      }}
                                      title={$i18n.get({ id: 'legacy.prompts.copy.response', dm: '复制回复' })}
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
                                        <Tag color="geekblue">Input Tokens: {message?.usage?.promptTokens}</Tag>
                                        <Tag color='geekblue'>Output Tokens: {message?.usage?.completionTokens}</Tag>
                                        <Tag color='geekblue'>Total Tokens: {message?.usage?.totalTokens}</Tag>
                                      </div>
                                      {/* Model Parameters信息 */}
                                      <div className='flex justify-between items-center mt-2 gap-2'>
                                        <Text type="secondary" style={{ fontSize: '11px' }}>
                                          {message.timestamp}
                                        </Text>
                                        {
                                          Boolean(message.traceId) && (
                                            <Tooltip title={$i18n.get({ id: 'legacy.prompts.view.trace', dm: '查看调用链路跟踪' })}>
                                              <Button
                                                type="text"
                                                size="small"
                                                icon={<ShareAltOutlined />}
                                                onClick={() => {
                                                  navigate(buildLegacyPath("/tracing"), {
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
                      placeholder={$i18n.get({ id: 'legacy.prompts.enter.your.question.to.test.enter.to.send.shift.enter.for.a', dm: '输入您的问题进行测试... (Enter发送，Shift+Enter换行)' })}
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
                      {prompt.isLoading ? $i18n.get({ id: 'legacy.prompts.processing', dm: '处理中...' }) : $i18n.get({ id: 'legacy.prompts.send', dm: '发送' })}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* 模态框保持不变 */}
      {showPublishModal && currentPrompt && (
        <PublishVersionModal
          prompt={typeof showPublishModal === 'object' ? showPublishModal.prompt : currentPrompt}
          newContent={typeof showPublishModal === 'object' ? showPublishModal.content : (promptInstances[0]?.content || '')}
          modelConfig={typeof showPublishModal === 'object' ? showPublishModal.modelConfig : (promptInstances[0]?.modelParams ? {
            modelId: promptInstances[0].selectedModel,
            ...(() => {
              const { model, modelId, ...filteredParams } = promptInstances[0].modelParams || {};
              return filteredParams;
            })()
          } : undefined)}
          variables={typeof showPublishModal === 'object' ? showPublishModal.variablesWithValues : {}}
          models={models}
          onClose={() => setShowPublishModal(false)}
          onSuccess={() => {
            setShowPublishModal(false);
            loadPromptDetail();
          }}
        />
      )}

      {showTemplateModal !== null && (
        <TemplateImportModal
          models={models}
          onImport={(template) => {
            handleTemplateImport(showTemplateModal, template);
            setShowTemplateModal(null);
          }}
          onClose={() => setShowTemplateModal(null)}
        />
      )}

      {/* Session Details模态框 */}
      {showSessionModal && selectedSessionId && (
        <Modal
          title={
            <Space>
              <MessageOutlined />
              <span>{$i18n.get({ id: 'legacy.prompts.session.details', dm: '会话详情' })}</span>
              <Tag color="blue">{selectedSessionId.substring(0, 8)}...</Tag>
            </Space>
          }
          open={true}
          onCancel={() => {
            setShowSessionModal(false);
            setSelectedSessionId(null);
          }}
          width={800}
          footer={[
            <Button key="close" onClick={() => {
              setShowSessionModal(false);
              setSelectedSessionId(null);
            }}>
              {$i18n.get({ id: 'legacy.prompts.close', dm: '关闭' })}
            </Button>,
            <Button
              key="delete"
              danger
              icon={<DeleteOutlined />}
              onClick={async () => {
                Modal.confirm({
                  title: $i18n.get({ id: 'legacy.prompts.delete.session', dm: '删除会话' }),
                  content: $i18n.get({ id: 'legacy.prompts.delete.this.session.this.will.clear.all.chat.history', dm: '确定要删除这个会话吗？这将清除所有对话历史。' }),
                  onOk: async () => {
                    const success = await deleteSession(selectedSessionId);
                    if (success) {
                      setPromptInstances(prev => prev.map(p =>
                        p.sessionId === selectedSessionId
                          ? { ...p, sessionId: null, chatHistory: [] }
                          : p
                      ));
                      setShowSessionModal(false);
                      setSelectedSessionId(null);
                    }
                  }
                });
              }}
            >
              {$i18n.get({ id: 'legacy.prompts.delete.session', dm: '删除会话' })}
            </Button>
          ]}
        >
          {currentSession ? (
            <div>
              <Card title={$i18n.get({ id: 'legacy.prompts.session.information', dm: '会话信息' })} size="small" style={{ marginBottom: 16 }}>
                <Row gutter={[16, 8]}>
                  <Col span={12}>
                    <Text strong>{$i18n.get({ id: 'legacy.prompts.session.id.2', dm: '会话 ID：' })}</Text>
                    <Text code style={{ fontSize: '12px' }}>{currentSession.sessionId}</Text>
                  </Col>
                  <Col span={12}>
                    <Text strong>Prompt Key：</Text>
                    <Text>{currentSession.promptKey}</Text>
                  </Col>
                  <Col span={12}>
                    <Text strong>{$i18n.get({ id: 'legacy.prompts.version.3', dm: '版本：' })}</Text>
                    <Tag color="blue">{currentSession.version}</Tag>
                  </Col>
                  <Col span={12}>
                    <Text strong>{$i18n.get({ id: 'legacy.prompts.created.2', dm: '创建时间：' })}</Text>
                    <Text>{dayjs(currentSession.createTime).format('YYYY-MM-DD HH:mm:ss')}</Text>
                  </Col>
                </Row>
              </Card>
              <Card title={$i18n.get({ id: 'legacy.prompts.model.configuration', dm: '模型配置' })} size="small">
                <Row gutter={[16, 8]}>
                  <Col span={24}>
                    <Space>
                      <Text strong>Model: </Text>
                      <Text code>{modelNameMap[currentSession.modelConfig.modelId]}</Text>
                    </Space>
                  </Col>
                  {
                    Object.entries(currentSession.modelConfig.allParameters).map(([key, value]) => {
                      return (
                        <Col span={12} key={key}>
                          <Text strong>{key}：</Text>
                          <Text>{value}</Text>
                        </Col>
                      )
                    })
                  }
                </Row>
              </Card>
              <Card title={$i18n.get({ id: 'legacy.prompts.parameter.configuration', dm: '参数配置' })} size="small">
                <Row gutter={[16, 8]}>
                  {
                    Object.entries(safeJSONParse(currentSession.variables)).map(([key, value]) => {
                      return (
                        <Col span={6} key={key}>
                          <Text strong>{key}：</Text>
                          <Text>{value || "-"}</Text>
                        </Col>
                      )
                    })
                  }
                </Row>
              </Card>
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <Spin size="large" />
              <div style={{ marginTop: 16 }}>
                <Text>{$i18n.get({ id: 'legacy.prompts.loading.session.details', dm: '加载会话详情中...' })}</Text>
              </div>
            </div>
          )}
        </Modal>
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

export default PromptDetailPage;