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

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30 * 1000,
})
const request: AxiosInterceptorManager<InternalAxiosRequestConfig> = service.interceptors.request
const response: AxiosInterceptorManager<AxiosResponse> = service.interceptors.response

request.use(
  config => {
    config.data = JSON.stringify(config.data)
    config.headers = <AxiosRequestHeaders>{
      'Content-Type': 'application/json',
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
    console.error(response.data)
    return Promise.reject(response.data)
  },
  error => {
    if (error) {
      console.error(error)
    }
    return Promise.reject(error.response.data)
  }
)
export default service
