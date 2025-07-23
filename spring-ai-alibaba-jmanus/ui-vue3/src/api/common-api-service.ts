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

// Common request methods (TypeScript version, suitable for Vue projects)

import type { PlanExecutionRecordResponse } from '@/types/plan-execution-record'

export class CommonApiService {
  private static readonly BASE_URL = '/api/executor'

  // Get detailed execution records
  public static async getDetails(planId: string): Promise<PlanExecutionRecordResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/details/${planId}`)
      if (response.status === 404) {
        // 404 returns null
        return null
      }
      if (!response.ok) throw new Error(`Failed to get detailed information: ${response.status}`)
      const rawText = await response.text()
      const data = JSON.parse(rawText)

      // Type validation - ensure the response contains required currentPlanId
      if (data && typeof data === 'object' && !data.currentPlanId) {
        // If currentPlanId is missing from response, add it from the parameter
        data.currentPlanId = planId
      }

      return data
    } catch (error: any) {
      // Log error but don't throw exception
      console.error('[CommonApiService] Failed to get plan details:', error)
      return null
    }
  }

  // Submit user form input
  public static async submitFormInput(planId: string, formData: any): Promise<any> {
    const response = await fetch(`${this.BASE_URL}/submit-input/${planId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData),
    })
    if (!response.ok) {
      let errorData
      try {
        errorData = await response.json()
      } catch {
        errorData = { message: `Failed to submit form input: ${response.status}` }
      }
      throw new Error(errorData.message || `Failed to submit form input: ${response.status}`)
    }
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.indexOf('application/json') !== -1) {
      return await response.json()
    }
    return { success: true }
  }

  /**
   * Get all Prompt list
   */
  static async getAllPrompts(): Promise<any[]> {
    try {
      const response = await fetch(this.BASE_URL)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get Prompt list:', error)
      throw error
    }
  }

  private static async handleResponse(response: Response) {
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `API request failed: ${response.status}`)
      } catch {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`)
      }
    }
    return response
  }
}
