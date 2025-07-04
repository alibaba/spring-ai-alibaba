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
import type { I18nType } from './type.ts'

const words: I18nType = {
  conversation: '对话',
  plan: '计划执行',
  backHome: '返回首页',
  noPageTip: '您访问的页面不存在。',
  home: {
    welcome: '欢迎使用 ',
    agent: '智能体',
    input: {
      placeholder: '描述您想构建或完成的内容...',
    },
    introduce: '您的 Java AI 智能助手，帮助您构建和完成各种任务。',
    examples: {
      titles: {
        stockPrice: '查询股价',
        novelGeneration: '生成一个中篇小说',
        weatherForecast: '查询天气',
      },
      descriptions: {
        stockPrice: '获取今天阿里巴巴的最新股价（Agent可以使用浏览器工具）',
        novelGeneration: '帮我生成一个中篇小说（Agent可以生成更长的内容）',
        weatherForecast: '获取北京今天的天气情况（Agent可以使用MCP工具服务）',
      },
    },
  },
}

export default words
