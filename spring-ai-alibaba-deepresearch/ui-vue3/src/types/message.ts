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

import { type MessageInfo } from 'ant-design-x-vue'
import type { UploadedFile } from './upload'

/**
 * 消息状态类型定义
 */
export interface MessageState<Message = any> {
  // 会话 id
  info: MessageInfo<Message | any>
  // 是否候选, 是: 不显示在界面上
  candidate: boolean
  // 是否展示研究细节
  deepResearchDetail: boolean
  // 记录ai内容的类型
  aiType: 'normal' | 'startDS' | 'onDS' | 'endDS'
  // 极速模式 或者 深度模式
  deepResearch?: boolean
  threadId: string
}


/**
 * 消息存储类型定义
 */
export interface MsgType<Message> {
  convId: string
  currentState: { [key: string]: MessageState<Message> }
  // 记录历史
  history: { [key: string]: MessageInfo<string>[] }
  htmlReport: { [key: string]: string[] }
  report: { [key: string]: any[] }
  uploadedFiles: { [key: string]: UploadedFile[] },
}
