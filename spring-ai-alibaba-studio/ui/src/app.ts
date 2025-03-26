///
/// Copyright 2024-2025 the original author or authors.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///      https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineAppConfig } from 'ice';
import { defineRequestConfig } from '@ice/plugin-request/types';
import { message } from 'antd';
import './i18n/i18n';

// App config, see https://v3.ice.work/docs/guide/basic/app
export default defineAppConfig(() => ({}));

export const requestConfig = defineRequestConfig({
  baseURL: process.env.ICE_BASE_URL,

  // 拦截器
  interceptors: {
    request: {
      onConfig: (config) => {
        // 发送请求前：可以对 RequestConfig 做一些统一处理
        // config.headers = {a: 1};
        return config;
      },
      onError: (error) => {
        return Promise.reject(error);
      },
    },
    response: {
      onConfig: (response) => {
        if (response.data.code != 200) {
          message.error(response.data.msg);
          return Promise.reject(response.data.msg);
        }
        return response.data;
      },
      onError: (error) => {
        console.log(error);
        // 请求出错：服务端返回错误状态码
        console.log(error.response);
        return Promise.reject(error);
      },
    },
  },
});
