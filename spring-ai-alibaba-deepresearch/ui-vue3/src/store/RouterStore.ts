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
import { type Reactive, reactive } from 'vue'
import { useRouter } from 'vue-router'

export const useRouterStore = <Message extends SimpleType>() =>
  defineStore('routerStore', {
    state(): Reactive<{ items: any[] }> {
      return reactive({
        items: [],
        needRecordPath: ['config', 'login'],
      })
    },
    getters: {
      size: state => state.items.length,
      isEmpty: state => state.items.length === 0,
    },
    actions: {
      peek() {
        if (this.isEmpty) return null
        return this.items[this.items.length - 1]
      },
      clear() {
        this.items = []
      },
      pop() {
        if (this.isEmpty) return null
        return this.items.pop()
      },
      push(element: string) {
        if (this.peek() === element) return
        this.items.push(element)
        if (this.size > 30) {
          this.items.splice(0, 10)
        }
      },
    },
    persist: true,
  })()
