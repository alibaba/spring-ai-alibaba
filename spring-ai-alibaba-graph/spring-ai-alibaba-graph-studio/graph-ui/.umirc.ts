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

import { defineConfig } from '@umijs/max';
import { codeInspectorPlugin } from 'code-inspector-plugin';
import { DEFAULT_NAME } from './src/constants';

export default defineConfig({
  title: 'site.title',
  antd: {
    configProvider: {
      theme: { cssVar: true },
    },
    // dark: true
  },
  chainWebpack(memo) {
    memo.plugin('code-inspector-plugin').use(
      codeInspectorPlugin({
        bundler: 'webpack',
      }),
    );
  },
  access: {},
  model: {},

  initialState: {},
  request: {},
  layout: {
    title: DEFAULT_NAME,
  },
  locale: {
    // 默认使用 src/locales/zh-CN.ts 作为多语言文件
    default: 'zh-CN',
    baseSeparator: '-',
    antd: true,
    useLocalStorage: true,
    title: true,
    baseNavigator: true,
  },
  valtio: {},
  routes: [
    {
      path: '/',
      redirect: '/graph',
    },
    {
      path: '/home',
      component: './Home',
      title: 'router.home',
    },
    {
      title: 'router.chatbot',
      path: '/chatbot',
      component: './Chatbot',
    },
    {
      hide: true,
      path: '/chatbot/edit',
      component: './Chatbot/Edit',
    },
    {
      title: 'router.agent',
      path: '/agent',
      component: './Agent',
    },
    {
      hide: true,
      path: '/agent/edit',
      component: './Agent/Edit',
    },
    {
      title: 'router.graph',
      path: '/graph',
      component: './Graph',
    },
    {
      hide: true,
      path: '/graph/design',
      component: './Graph/Design',
    },
  ],
  npmClient: 'pnpm',
});
