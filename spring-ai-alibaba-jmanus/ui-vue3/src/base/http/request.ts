/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
  InternalAxiosRequestConfig
} from 'axios'
import axios from 'axios'
import NProgress from 'nprogress'

const service: AxiosInstance = axios.create({
  //  change this to decide where to go
  // baseURL: '/mock',
  baseURL: '/api/v1',
  timeout: 30 * 1000
})
const request: AxiosInterceptorManager<InternalAxiosRequestConfig> = service.interceptors.request
const response: AxiosInterceptorManager<AxiosResponse> = service.interceptors.response

request.use(
  (config) => {
    config.data = JSON.stringify(config.data) //数据转化,也可以使用qs转换
    config.headers = <AxiosRequestHeaders>{
      'Content-Type': 'application/json' //配置请求头
    }
    // NProgress.start()
    // console.log(config)
    return config
  },
  (error) => {
    Promise.reject(error)
  }
)
const rejectState: { errorHandler: Function | null } = {
  errorHandler: null
}

response.use(
  (response) => {
    NProgress.done()
    if (
      response.status === 200 &&
      (response.data.code === 200 || response.data.status === 'success')
    ) {
      return Promise.resolve(response.data)
    }
    console.error(response.data.code + ':' + response.data.msg)
    return Promise.reject(response.data)
  },
  (error) => {
    NProgress.done()
    if (error.response.data) {
      console.error(error.response.data.code + ':' + error.response.data.msg)
    } else {
      console.error(error.response)
    }

    return Promise.reject(error.response.data)
  }
)
export default service
