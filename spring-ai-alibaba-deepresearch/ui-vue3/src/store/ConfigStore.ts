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
import { type SimpleType } from 'ant-design-x-vue'
import { reactive } from 'vue'

type ConfigType = {
  form: {
    auto_accepted_plan: boolean
    optimize_query_num: number
    max_plan_iterations: number
    max_step_num: number
    mcp_settings: any
    search_engine: 'tavily',
  }
}
export const useConfigStore = () =>
  defineStore('configStore', {
    state(): ConfigType {
      return reactive({
        form: {
          auto_accepted_plan: true,
          optimize_query_num: 3,
          max_plan_iterations: 1,
          max_step_num: 3,
          mcp_settings: {},
          search_engine: 'tavily',
        },
      })
    },
    getters: {
      chatConfig: state => state.form,
    },
    actions: {},
    persist: true,
  })()
