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

import { defineStore } from 'pinia'
import { type MessageInfo, type SimpleType } from 'ant-design-x-vue'
import { reactive } from 'vue'
import { type MessageState, type MsgType } from '@/types/message'
import { parseJsonTextStrict } from '@/utils/jsonParser'
import type { LlmStreamNode, NormalNode } from '@/types/node'
export const useMessageStore = <Message extends SimpleType>() =>
  defineStore('messageStore', {
    state(): MsgType<Message> {
      return reactive({
        convId: '',
        currentState: {} as { [key: string]: MessageState<Message> },
        // { 会话id: [{ 线程id: 消息列表 }] }
        history: {} as { [key: string]: MessageInfo<any>[] },
        htmlReport: {} as { [key: string]: string[] },
        report: {} as { [key: string]: any[] },
      })
    },
    getters: {
      // 获取消息列表
      messages: (state): MessageInfo<string>[]  => {
        const res: MessageInfo<string>[] = []
        if (state.convId) {
          const messages = state.history[state.convId]
          const threadId = state.currentState[state.convId].threadId
          if(!messages) {
            return []
          }
          for(const msg of messages) {
            if(!msg.message) {
              continue
            }
            const jsonArray = parseJsonTextStrict(msg.message)
            jsonArray.forEach(item => {
              if(item.graphId.thread_id === threadId) {
                res.push(msg)
              }
            })
          }
        }
        return res
      },
      // 获取当下消息状态
      current: (state): MessageState<Message> => {
        if (state.convId) {
          return state.currentState[state.convId]
        }
        return {} as MessageState<Message>
      }
    },
    actions: {
      nextAIType() {
        if (!this.current.aiType || this.current.aiType === 'normal') {
          this.current.aiType = 'startDS'
        } else if (this.current.aiType === 'startDS') {
          this.current.aiType = 'onDS'
        } else if (this.current.aiType === 'onDS') {
          this.current.aiType = 'endDS'
        } else {
          this.current.aiType = 'normal'
        }
      },
      addReport(report: any) {
        if(!report) {
          return
        }
        const node = JSON.parse(report)
        if(!this.report[node.graphId.thread_id]) {
          this.report[node.graphId.thread_id] = []
        }
        this.report[node.graphId.thread_id].push(node) 
      },
      isEnd(threadId: string): boolean {
        const report = this.report[threadId]
        if(!report) {
          return false
        }
        for(const item of report) {
          if(item.nodeName === '__END__') {
            return true
          }
        }
        return false
      }
    },
    persist: true,
  })()
