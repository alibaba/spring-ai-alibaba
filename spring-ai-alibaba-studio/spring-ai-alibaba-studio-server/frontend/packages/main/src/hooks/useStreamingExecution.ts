import { useState, useEffect, useCallback } from 'react';
import { EnhancedNodeOutput, NodeOutput, StateSnapshot } from '@/types/graph';
import graphDebugService from '@/services/graphDebugService';

// 流式执行结果类型
interface StreamingResult {
  data: Array<EnhancedNodeOutput | NodeOutput | StateSnapshot>;
  isConnected: boolean;
  isRunning: boolean;
  error: string | null;
  cleanup: (() => void) | null;
}

// 流式执行配置
interface StreamingConfig {
  streamType: 'enhanced' | 'basic' | 'snapshots';
  graphId?: string; // 如果提供，使用当前图接口；否则使用独立节点接口
}

/**
 * 流式执行监控 Hook
 * 支持三种流式监控接口的统一使用
 */
export function useStreamingExecution(): {
  result: StreamingResult;
  execute: (inputText: string, config: StreamingConfig) => Promise<void>;
  stop: () => void;
  reset: () => void;
} {
  const [result, setResult] = useState<StreamingResult>({
    data: [],
    isConnected: false,
    isRunning: false,
    error: null,
    cleanup: null,
  });

  // 重置状态
  const reset = useCallback(() => {
    if (result.cleanup) {
      result.cleanup();
    }
    setResult({
      data: [],
      isConnected: false,
      isRunning: false,
      error: null,
      cleanup: null,
    });
  }, [result.cleanup]);

  // 停止执行
  const stop = useCallback(() => {
    if (result.cleanup) {
      result.cleanup();
      setResult(prev => ({
        ...prev,
        isConnected: false,
        isRunning: false,
        cleanup: null,
      }));
    }
  }, [result.cleanup]);

  // 执行流式监控
  const execute = useCallback(async (inputText: string, config: StreamingConfig) => {
    // 先清理之前的连接
    reset();

    if (!inputText.trim()) {
      setResult(prev => ({ ...prev, error: '输入文本不能为空' }));
      return;
    }

    setResult(prev => ({ 
      ...prev, 
      isRunning: true, 
      error: null,
      data: [] 
    }));

    try {
      let cleanup: () => void;

      if (config.graphId) {
        // 使用当前图接口
        switch (config.streamType) {
          case 'enhanced':
            cleanup = await graphDebugService.executeGraphEnhanced(
              config.graphId,
              inputText,
              (nodeOutput: EnhancedNodeOutput) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, nodeOutput],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Enhanced graph stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '增强流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          case 'basic':
            cleanup = await graphDebugService.executeGraphBasic(
              config.graphId,
              inputText,
              (nodeOutput: NodeOutput) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, nodeOutput],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Basic graph stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '基础流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          case 'snapshots':
            cleanup = await graphDebugService.executeGraphSnapshots(
              config.graphId,
              inputText,
              (snapshot: StateSnapshot) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, snapshot],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Snapshot graph stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '快照流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          default:
            throw new Error(`不支持的流式类型: ${config.streamType}`);
        }
      } else {
        // 使用独立节点接口
        switch (config.streamType) {
          case 'enhanced':
            cleanup = await graphDebugService.executeNodeEnhanced(
              inputText,
              (nodeOutput: EnhancedNodeOutput) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, nodeOutput],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Enhanced node stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '增强节点流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          case 'basic':
            cleanup = await graphDebugService.executeNodeBasic(
              inputText,
              (nodeOutput: NodeOutput) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, nodeOutput],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Basic node stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '基础节点流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          case 'snapshots':
            cleanup = await graphDebugService.executeNodeSnapshots(
              inputText,
              (snapshot: StateSnapshot) => {
                setResult(prev => ({
                  ...prev,
                  data: [...prev.data, snapshot],
                  isConnected: true,
                }));
              },
              (error) => {
                console.error('Snapshot node stream error:', error);
                setResult(prev => ({
                  ...prev,
                  error: '快照节点流执行错误: ' + String(error),
                  isConnected: false,
                  isRunning: false,
                }));
              },
              () => {
                setResult(prev => ({
                  ...prev,
                  isRunning: false,
                }));
              }
            );
            break;

          default:
            throw new Error(`不支持的流式类型: ${config.streamType}`);
        }
      }

      setResult(prev => ({ ...prev, cleanup }));
    } catch (error) {
      console.error('Execute streaming error:', error);
      setResult(prev => ({
        ...prev,
        error: '启动流式执行失败: ' + String(error),
        isRunning: false,
      }));
    }
  }, [reset]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      if (result.cleanup) {
        result.cleanup();
      }
    };
  }, [result.cleanup]);

  return {
    result,
    execute,
    stop,
    reset,
  };
}

/**
 * 简化版的增强节点流 Hook
 */
export function useEnhancedNodeStream(inputText: string, graphId?: string) {
  const [data, setData] = useState<EnhancedNodeOutput[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isRunning, setIsRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!inputText) {
      setData([]);
      setIsConnected(false);
      setIsRunning(false);
      setError(null);
      return;
    }

    setIsRunning(true);
    setError(null);
    setData([]);

    const execute = async () => {
      try {
        const cleanup = graphId
          ? await graphDebugService.executeGraphEnhanced(
              graphId,
              inputText,
              (nodeOutput) => {
                setData(prev => [...prev, nodeOutput]);
                setIsConnected(true);
              },
              (err) => {
                setError('流式执行错误: ' + String(err));
                setIsConnected(false);
                setIsRunning(false);
              },
              () => {
                setIsRunning(false);
              }
            )
          : await graphDebugService.executeNodeEnhanced(
              inputText,
              (nodeOutput) => {
                setData(prev => [...prev, nodeOutput]);
                setIsConnected(true);
              },
              (err) => {
                setError('流式执行错误: ' + String(err));
                setIsConnected(false);
                setIsRunning(false);
              },
              () => {
                setIsRunning(false);
              }
            );

        return cleanup;
      } catch (err) {
        setError('启动流式执行失败: ' + String(err));
        setIsRunning(false);
        return () => {};
      }
    };

    let cleanup: (() => void) | undefined;
    execute().then(fn => cleanup = fn);

    return () => {
      if (cleanup) {
        cleanup();
      }
    };
  }, [inputText, graphId]);

  return { data, isConnected, isRunning, error };
}

export default useStreamingExecution;


