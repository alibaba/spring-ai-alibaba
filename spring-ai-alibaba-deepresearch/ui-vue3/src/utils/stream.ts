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
import { ref } from 'vue'
import { XStream } from 'ant-design-x-vue'

type MethodType = 'POST' | 'GET'

/**
 * sse request
 */
export class XStreamBody {
  requestInfo: {
    url: string
    config: {
      method: MethodType
      headers: any
      body: any
    }
  }

  // records
  lines = ref<Record<string, string>[]>([])

  constructor(url: string, config: any) {
    if (config.body) {
      config.body = JSON.stringify(config.body)
    }
    // 如果URL不是完整的HTTP URL，则添加BASE_URL前缀
    const baseURL = import.meta.env.VITE_BASE_URL || ''
    const fullUrl = url.startsWith('http') ? url : `${baseURL}${url.startsWith('/') ? url : '/' + url}`
    
    this.requestInfo = {
      url: fullUrl,
      config,
    }
  }

  content() {
    return this.lines.value.map(line => JSON.parse(line.data).content).join('')
  }

  async readStream(updateHandle?: any) {
    let tmp = ''
    const response = await fetch(this.requestInfo.url, this.requestInfo.config)

    if (response.status !== 200) {
      return Promise.reject(response)
    }
    // Read the stream
    for await (const chunk of XStream({
      readableStream: response.body,
    })) {
      const newChunk = {
        event: 'message',
        data: JSON.stringify({
          content: chunk.data,
        }),
      }
      this.lines.value = [...this.lines.value, newChunk]
      if (updateHandle) {
        updateHandle(chunk.data)
      }
    }
  }
}
