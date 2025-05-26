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

// resort words in en.ts and zh.ts;
// check words exist in en.ts and zh.ts;

import EN_MAP from './en'
import ZH_MAP from './zh'

let sortArr: { label: string; value: any }[] = []
let checkArr: string[] = []

function mapToArr() {
  for (let enKey in EN_MAP) {
    sortArr.push({
      label: enKey,
      value: EN_MAP[enKey]
    })
    let zh = ZH_MAP[enKey]
    if (!zh) {
      checkArr.push(enKey)
    }
  }
}

mapToArr()
console.log(sortArr.sort((a, b) => (a.label > b.label ? 1 : -1)))
