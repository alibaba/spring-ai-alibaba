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

import { notification } from 'ant-design-vue'

/**
 * get function  invoke stack
 * @param more Offset relative to the default stack number
 */
function getCurrentFunctionLocation(more = 0): string {
  try {
    throw new Error()
  } catch (error: any) {
    // error.stack
    const stackLines = error.stack.split('\n')
    // The forth line typically contains information about the calling location.
    const offset = 2 + more
    if (offset >= 0 && stackLines.length >= offset) {
      return stackLines[offset].trim()
    }
    return 'wrong location'
  }
}

/**
 * custom notification about to-do fun
 * for developer
 * @param todoDetail
 */
const todo = (todoDetail: string) => {
  notification.warn({
    message: `TODO: ${todoDetail} =>: ${devTool.getCurrentFunctionLocation(1)}`
  })
}

const mockUrl = (raw: string) => {
  return RegExp(raw + '.*')
}

const devTool = {
  getCurrentFunctionLocation,
  todo,
  mockUrl
}

export default devTool
