/**
 * API 响应基础类型定义
 * 对应 Java 类：ApiResponse<T>
 */
export interface ApiResponse<T = any> {
  /** 响应状态码 */
  code: number;
  /** 响应状态 */
  status: string;
  /** 响应消息 */
  message: string;
  /** 响应数据 */
  data: T;
}

/**
 * 创建成功响应的工具函数
 * @param data 响应数据
 * @returns 成功的 API 响应
 */
export function createSuccessResponse<T>(data: T): ApiResponse<T> {
  return {
    code: 200,
    status: 'success',
    message: '',
    data
  };
}

/**
 * 创建错误响应的工具函数
 * @param message 错误消息
 * @param data 可选的错误数据
 * @returns 错误的 API 响应
 */
export function createErrorResponse<T = null>(message: string, data: T = null as T): ApiResponse<T> {
  return {
    code: 500,
    status: 'error',
    message,
    data
  };
}

/**
 * 类型守卫：检查响应是否为成功响应
 * @param response API 响应
 * @returns 是否为成功响应
 */
export function isSuccessResponse<T>(response: ApiResponse<T>): boolean {
  return response.code === 200 && response.status === 'success';
}

/**
 * 类型守卫：检查响应是否为错误响应
 * @param response API 响应
 * @returns 是否为错误响应
 */
export function isErrorResponse<T>(response: ApiResponse<T>): boolean {
  return response.code !== 200 || response.status === 'error';
}
