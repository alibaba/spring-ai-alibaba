// 拦截器接口
interface RequestInterceptor {
  request?: (config: RequestConfig) => RequestConfig | Promise<RequestConfig>;
  response?: <T>(response: ApiResponse<T>) => ApiResponse<T> | Promise<ApiResponse<T>>;
  requestError?: (error: any) => any;
  responseError?: (error: RequestError) => any;
}

// Request 配置接口
interface RequestConfig {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  params?: Record<string, any>;
  data?: Record<string, any> | any;
  headers?: Record<string, string>;
  timeout?: number;
  skipInterceptors?: boolean; // 是否跳过拦截器
}

// 响应接口
interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

// 请求错误类
class RequestError extends Error {
  public code: number;
  public response?: any;

  constructor(message: string, code: number, response?: any) {
    super(message);
    this.name = 'RequestError';
    this.code = code;
    this.response = response;
  }
}

// 拦截器管理
class InterceptorManager {
  private interceptors: (RequestInterceptor | null)[] = [];

  // 添加拦截器
  use(interceptor: RequestInterceptor): number {
    this.interceptors.push(interceptor);
    return this.interceptors.length - 1;
  }

  // 移除拦截器
  eject(id: number): void {
    if (this.interceptors[id]) {
      this.interceptors[id] = null;
    }
  }

  // 执行请求拦截器
  async processRequest(config: RequestConfig): Promise<RequestConfig> {
    let processedConfig = config;
    
    for (const interceptor of this.interceptors) {
      if (interceptor && interceptor.request) {
        try {
          processedConfig = await interceptor.request(processedConfig);
        } catch (error) {
          if (interceptor.requestError) {
            throw interceptor.requestError(error);
          }
          throw error;
        }
      }
    }
    
    return processedConfig;
  }

  // 执行响应拦截器
  async processResponse<T>(response: ApiResponse<T>): Promise<ApiResponse<T>> {
    let processedResponse = response;
    
    for (const interceptor of this.interceptors) {
      if (interceptor && interceptor.response) {
        try {
          processedResponse = await interceptor.response(processedResponse);
        } catch (error) {
          if (interceptor.responseError && error instanceof RequestError) {
            throw interceptor.responseError(error);
          }
          throw error;
        }
      }
    }
    
    return processedResponse;
  }

  // 执行错误拦截器
  async processError(error: RequestError): Promise<never> {
    for (const interceptor of this.interceptors) {
      if (interceptor && interceptor.responseError) {
        try {
          throw interceptor.responseError(error);
        } catch (processedError) {
          // 如果拦截器返回了新的错误，继续抛出
          error = processedError instanceof RequestError ? processedError : error;
        }
      }
    }
    throw error;
  }

  // 清空所有拦截器
  clear(): void {
    this.interceptors = [];
  }
}

// 全局拦截器管理器
const interceptors = new InterceptorManager();

// 添加默认拦截器 - 自动添加认证头
interceptors.use({
  request: async (config: RequestConfig) => {
    // 可以在这里添加认证 token
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers = {
        ...config.headers,
        'Authorization': `Bearer ${token}`
      };
    }
    
    // 添加通用请求头
    config.headers = {
      'X-Client-Version': '1.0.0',
      'X-Request-ID': Math.random().toString(36).substring(2),
      ...config.headers,
    };
    
    return config;
  },
  
  responseError: (error: RequestError) => {
    // 处理 401 未授权错误
    if (error.code === 401) {
      console.warn('Authentication failed, redirecting to login...');
      // 可以在这里处理登录重定向
      localStorage.removeItem('authToken');
    }
    
    return error;
  }
});

// 将参数对象转换为查询字符串
function buildQueryString(params: Record<string, any>): string {
  const searchParams = new URLSearchParams();
  
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      if (typeof value === 'object') {
        searchParams.append(key, JSON.stringify(value));
      } else {
        searchParams.append(key, String(value));
      }
    }
  });
  
  return searchParams.toString();
}

// 配置基础 URL（可选）
let baseURL = '';

// 通用请求函数
async function baseRequest<T = any>(
  url: string,
  config: RequestConfig = {}
): Promise<ApiResponse<T>> {
  let processedConfig = { ...config };
  const {
    method = 'GET',
    skipInterceptors = false,
    timeout = 10000
  } = processedConfig;

  try {
    // 处理请求拦截器
    if (!skipInterceptors) {
      processedConfig = await interceptors.processRequest(processedConfig);
    }

    const {
      params,
      data,
      headers = {},
    } = processedConfig;

    // 构建完整URL
    let fullUrl = baseURL && !url.startsWith('http') ? baseURL + url : url;
    if (params && Object.keys(params).length > 0) {
      const queryString = buildQueryString(params);
      fullUrl += (fullUrl.includes('?') ? '&' : '?') + queryString;
    }

    // 准备请求配置
    const fetchConfig: RequestInit = {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
      },
    };

    // 添加请求体（仅对非GET请求）
    if (method !== 'GET' && data) {
      if (typeof data === 'object') {
        fetchConfig.body = JSON.stringify(data);
      } else {
        fetchConfig.body = data;
      }
    }

    // 创建超时控制
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);
    fetchConfig.signal = controller.signal;

    console.log(`[REQUEST] ${method} ${fullUrl}`, {
      params,
      data,
      headers: fetchConfig.headers
    });

    // 发送请求
    const response = await fetch(fullUrl, fetchConfig);
    
    // 清除超时
    clearTimeout(timeoutId);

    // 检查响应状态
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`[REQUEST ERROR] ${method} ${fullUrl} - ${response.status}`, errorText);
      
      const requestError = new RequestError(
        `HTTP ${response.status}: ${response.statusText}`,
        response.status,
        errorText
      );

      // 处理错误拦截器
      if (!skipInterceptors) {
        await interceptors.processError(requestError);
      }
      
      throw requestError;
    }

    // 解析响应
    let responseData = await response.json();
    
    console.log(`[RESPONSE] ${method} ${fullUrl}`, responseData);

    // 检查业务状态码
    if (responseData.code !== undefined && responseData.code !== 200 && responseData.code !== 0) {
      const requestError = new RequestError(
        responseData.message || '请求失败',
        responseData.code,
        responseData
      );

      // 处理错误拦截器
      if (!skipInterceptors) {
        await interceptors.processError(requestError);
      }
      
      throw requestError;
    }

    // 处理响应拦截器
    if (!skipInterceptors) {
      responseData = await interceptors.processResponse(responseData);
    }

    return responseData;

  } catch (error: unknown) {
    if (error instanceof RequestError) {
      throw error;
    }

    if (error instanceof Error && error.name === 'AbortError') {
      console.error(`[REQUEST TIMEOUT] ${method} ${url}`);
      const timeoutError = new RequestError('请求超时', 408);
      
      // 处理错误拦截器
      if (!skipInterceptors) {
        await interceptors.processError(timeoutError);
      }
      
      throw timeoutError;
    }

    console.error(`[REQUEST ERROR] ${method} ${url}`, error);
    
    // 网络错误或其他错误
    const networkError = new RequestError(
      error instanceof Error ? error.message : '网络请求失败',
      0,
      error
    );

    // 处理错误拦截器
    if (!skipInterceptors) {
      await interceptors.processError(networkError);
    }
    
    throw networkError;
  }
}

// 主要请求函数
export const request = baseRequest;

// 便捷方法
export const get = <T = any>(url: string, params?: Record<string, any>, config?: Omit<RequestConfig, 'method' | 'params'>) => {
  return request<T>(url, { ...config, method: 'GET', params });
};

export const post = <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'data'>) => {
  return request<T>(url, { ...config, method: 'POST', data });
};

export const put = <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'data'>) => {
  return request<T>(url, { ...config, method: 'PUT', data });
};

export const del = <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'data'>) => {
  return request<T>(url, { ...config, method: 'DELETE', data });
};

// 拦截器管理接口
export const requestInterceptors = {
  // 添加拦截器
  use: (interceptor: RequestInterceptor) => interceptors.use(interceptor),
  
  // 移除拦截器
  eject: (id: number) => interceptors.eject(id),
  
  // 清空所有拦截器
  clear: () => interceptors.clear()
};

// 基础URL配置
export const setBaseURL = (url: string) => {
  baseURL = url;
};

export const getBaseURL = () => baseURL;

// 默认导出
export default {
  request,
  get,
  post,
  put,
  delete: del,
  RequestError,
  interceptors: requestInterceptors,
  setBaseURL,
  getBaseURL
};

/* 
使用示例:

// 基本使用
import { request, get, post } from './utils/request';

// GET 请求
const prompts = await get('/api/prompts', { pageSize: 10 });

// POST 请求
const newPrompt = await post('/api/prompt', { 
  promptKey: 'test',
  promptDescription: 'Test prompt' 
});

// 使用拦截器
import { requestInterceptors } from './utils/request';

// 添加请求拦截器
const interceptorId = requestInterceptors.use({
  request: (config) => {
    config.headers = { ...config.headers, 'Custom-Header': 'value' };
    return config;
  },
  response: (response) => {
    console.log('Response received:', response);
    return response;
  }
});

// 移除拦截器
requestInterceptors.eject(interceptorId);

// 设置基础URL
import { setBaseURL } from './utils/request';
setBaseURL('https://api.example.com');
*/