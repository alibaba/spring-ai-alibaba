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
    planExecution: 'Plan Execution',
    configsRoot: 'Configuration Center',
    configsBasic: 'Basic Configuration',
    configsAgent: 'Agent Configuration',
    configsMcp: 'MCP Configuration',
    conversation: 'Conversation'
  },
  configs: {
    mcpConfig: {
      title: 'MCP Configuration',
      description: 'MCP Configuration Management',
      detail: 'MCP Configuration Detail'
    },
    basicConfig: {
      title: 'Basic Configuration',
      description: 'Basic Configuration Management',
      detail: 'Basic Configuration Detail'
    },
    agentConfig: {
      title: 'Agent Configuration',
      description: 'Agent Configuration Management',
      detail: 'Agent Configuration Detail'
    }
  },
  conversation: {
    title: 'Conversation',
    description: 'Conversation Management',
    detail: 'Conversation Detail'
  }
}

export default words
