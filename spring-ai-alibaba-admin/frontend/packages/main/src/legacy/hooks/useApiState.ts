import { useState, useCallback } from 'react';
import { handleApiError, handleNetworkError, notifySuccess } from '../utils/notification';

export interface UseApiStateOptions {
  successMessage?: string;
  errorContext?: string;
  showSuccessNotification?: boolean;
}

export interface ApiState<T = any> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

export interface UseApiStateReturn<T = any> {
  state: ApiState<T>;
  execute: (apiCall: () => Promise<T>) => Promise<T | null>;
  reset: () => void;
  setData: (data: T | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useApiState = <T = any>(
  options: UseApiStateOptions = {}
): UseApiStateReturn<T> => {
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const setData = useCallback((data: T | null) => {
    setState(prev => ({ ...prev, data }));
  }, []);

  const setLoading = useCallback((loading: boolean) => {
    setState(prev => ({ ...prev, loading }));
  }, []);

  const setError = useCallback((error: string | null) => {
    setState(prev => ({ ...prev, error }));
  }, []);

  const reset = useCallback(() => {
    setState({
      data: null,
      loading: false,
      error: null,
    });
  }, []);

  const execute = useCallback(async (apiCall: () => Promise<T>): Promise<T | null> => {
    setState(prev => ({ ...prev, loading: true, error: null }));

    try {
      const result = await apiCall();
      
      setState(prev => ({ 
        ...prev, 
        data: result, 
        loading: false, 
        error: null 
      }));

      // 显示成功通知
      if (options.showSuccessNotification && options.successMessage) {
        notifySuccess({ message: options.successMessage });
      }

      return result;
    } catch (error: any) {
      const errorMessage = error?.message || '请求失败';
      
      setState(prev => ({ 
        ...prev, 
        loading: false, 
        error: errorMessage 
      }));

      // 处理不同类型的错误
      if (error?.name === 'NetworkError' || error?.code === 'NETWORK_ERROR') {
        handleNetworkError(options.errorContext);
      } else {
        handleApiError(error, options.errorContext);
      }

      return null;
    }
  }, [options.successMessage, options.errorContext, options.showSuccessNotification]);

  return {
    state,
    execute,
    reset,
    setData,
    setLoading,
    setError,
  };
};