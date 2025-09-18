/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { LlmCheckService } from '@/utils/llm-check'
import type { InputMessage } from "@/stores/memory"

export class DirectApiService {
  private static readonly BASE_URL = '/api/executor'

  // Send task directly (direct execution mode)
  public static async sendMessage(query: InputMessage): Promise<any> {
    return LlmCheckService.withLlmCheck(async () => {
      // Add Vue identification flag to distinguish from HTTP requests
      const requestBody = {
        ...query,
        isVueRequest: true
      }
      
      const response = await fetch(`${this.BASE_URL}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      })
      if (!response.ok) throw new Error(`API request failed: ${response.status}`)
      return await response.json()
    })
  }
}
