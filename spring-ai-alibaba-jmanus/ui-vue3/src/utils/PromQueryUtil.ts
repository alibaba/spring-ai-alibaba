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

import { nextTick, reactive } from 'vue'

export function promQueryList(res: any, initArr: string[], asyncFun: any) {
  const react_res = reactive(res)
  for (let key of initArr) {
    let list = res?.data?.list
    for (let r of list) {
      r[key] = 'skeleton-loading'
    }
  }
  nextTick(async () => {
    try {
      let list = react_res?.data?.list || []
      for (let r of list) {
        asyncFun(r)
      }
    } catch (e) {
      console.error(e)
    }
  })
  return react_res
}
