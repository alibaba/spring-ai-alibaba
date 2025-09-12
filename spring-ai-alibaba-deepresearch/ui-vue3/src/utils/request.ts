/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type {
  AxiosInstance,
  AxiosInterceptorManager,
  AxiosRequestHeaders,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import axios from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types/base'
import { isSuccessResponse } from '@/types/base'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_BASE_URL || '',
  timeout: 30 * 1000,
  withCredentials: false, // 跨域请求时是否需要使用凭证
  headers: {
    'Content-Type': 'application/json'
  }
})
const request: AxiosInterceptorManager<InternalAxiosRequestConfig> = service.interceptors.request
const response: AxiosInterceptorManager<AxiosResponse> = service.interceptors.response
request.use(
  config => {
    // 如果是FormData，不要进行JSON序列化和设置Content-Type
    if (config.data instanceof FormData) {
      // 删除默认的Content-Type，让浏览器自动设置multipart/form-data
      delete config.headers['Content-Type']
    } else {
      config.data = JSON.stringify(config.data)
      config.headers = <AxiosRequestHeaders>{
        'Content-Type': 'application/json',
      }
    }
    return config
  },
  error => {
    Promise.reject(error)
  }
)

/**
 * 响应拦截器封装
 * 处理 ApiResponse 类型的响应数据
 */
response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const apiResponse: ApiResponse = response.data

    // 使用类型守卫检查响应是否成功
    if (response.status === 200 && isSuccessResponse(apiResponse)) {
      // 返回原始响应对象，保持 axios 响应结构
      return response
    }

    // 显示错误消息
    if (apiResponse.message) {
      message.error(apiResponse.message)
    }
    return Promise.reject(apiResponse)
  },
  error => {
    console.error('Request error:', error)

    // 处理网络错误或其他错误
    if (error.response?.data) {
      const errorResponse: ApiResponse = error.response.data
      if (errorResponse.message) {
        message.error(errorResponse.message)
      }
      return Promise.reject(errorResponse)
    }

    // 处理网络错误等情况
    const networkError: ApiResponse = {
      code: 500,
      status: 'error',
      message: error.message || '网络请求失败',
      data: null
    }
    message.error(networkError.message)
    return Promise.reject(networkError)
  }
)
/**
 * 封装的请求函数，返回 ApiResponse 数据部分
 * @param config axios 请求配置
 * @returns Promise<T> 直接返回 ApiResponse.data 的类型
 */
export async function apiRequest<T = any>(config: Partial<InternalAxiosRequestConfig>): Promise<T> {
  try {
    const response = await service(config as InternalAxiosRequestConfig)
    const apiResponse: ApiResponse<T> = response.data

    if (isSuccessResponse(apiResponse)) {
      return apiResponse.data
    }

    throw apiResponse
  } catch (error: any) {
    throw error
  }
}

/**
 * GET 请求封装
 */
export function get<T = any>(url: string, params?: any): Promise<T> {
  return apiRequest<T>({
    method: 'GET',
    url,
    params
  })
}

/**
 * POST 请求封装
 */
export function post<T = any>(url: string, data?: any): Promise<T> {
  return apiRequest<T>({
    method: 'POST',
    url,
    data
  })
}

/**
 * PUT 请求封装
 */
export function put<T = any>(url: string, data?: any): Promise<T> {
  return apiRequest<T>({
    method: 'PUT',
    url,
    data
  })
}

/**
 * DELETE 请求封装
 */
export function del<T = any>(url: string, params?: any): Promise<T> {
  return apiRequest<T>({
    method: 'DELETE',
    url,
    params
  })
}

// 导出原始 axios 实例，用于特殊需求
export default service
