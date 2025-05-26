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

import type { I18nType } from './type.ts'

const words: I18nType = {
  menu: {
    planExecution: '计划执行',
    configsRoot: '配置中心',
    configsBasic: '基础配置',
    configsAgent: 'Agent 配置',
    configsMcp: 'MCP 配置',
    conversation: '对话'
  },
  configs: {
    mcpConfig: {
      title: 'MCP 配置',
      description: 'MCP 配置管理',
      detail: 'MCP 配置详情'
    },
    basicConfig: {
      title: '基础配置',
      description: '基础配置管理',
      detail: '基础配置详情'
    },
    agentConfig: {
      title: 'Agent 配置',
      description: 'Agent 配置管理',
      detail: 'Agent 配置详情'
    }
  },
  conversation: {
    title: '对话',
    description: '对话管理',
    detail: '对话详情'
  }
}

export default words
