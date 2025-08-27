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
import { message } from 'ant-design-vue';

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

response.use(
  response => {
    if (
      response.status === 200 &&
      (response.data.code === 200 || response.data.status === 'success')
    ) {
      return Promise.resolve(response.data)
    }
    message.error(response.data.message)
    return Promise.reject(response.data)
  },
  error => {
    if (error) {
      console.error('error', error)
    }
    message.error(error.response.data.message)
    return Promise.reject(error.response.data)
  }
)
export default service
