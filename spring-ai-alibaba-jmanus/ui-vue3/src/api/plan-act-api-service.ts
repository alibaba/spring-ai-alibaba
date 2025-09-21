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

// Plan-related API wrapper (TypeScript version for Vue projects)

import type { CronConfig } from '@/types/cron-task'
import { LlmCheckService } from '@/utils/llm-check'

export class PlanActApiService {
  private static readonly PLAN_TEMPLATE_URL = '/api/plan-template'
  private static readonly CRON_TASK_URL = '/api/cron-tasks'

  // Generate plan
  public static async generatePlan(query: string, existingJson?: string, planType: string = 'simple'): Promise<any> {
    return LlmCheckService.withLlmCheck(async () => {
      const requestBody: Record<string, any> = { query, planType }
      if (existingJson) requestBody.existingJson = existingJson
      
      const response = await fetch(`${this.PLAN_TEMPLATE_URL}/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      })
      if (!response.ok) throw new Error(`Failed to generate plan: ${response.status}`)
      const responseData = await response.json()
      if (responseData.planJson) {
        try {
          responseData.plan = JSON.parse(responseData.planJson)
        } catch {
          responseData.plan = { error: 'Unable to parse plan data' }
        }
      }
      return responseData
    })
  }

  // Execute generated plan using ManusController.executeByToolNameAsync
  public static async executePlan(planTemplateId: string, rawParam?: string, uploadedFiles?: any[], replacementParams?: Record<string, string>): Promise<any> {
    return LlmCheckService.withLlmCheck(async () => {
      console.log('[PlanActApiService] executePlan called with:', { planTemplateId, rawParam, uploadedFiles, replacementParams })
      
      // Use planTemplateId as toolName to call executeByToolNameAsync
      const requestBody: Record<string, any> = { 
        toolName: planTemplateId  // Use planTemplateId as toolName
      }
      if (rawParam) requestBody.rawParam = rawParam
      if (uploadedFiles && uploadedFiles.length > 0) {
        requestBody.uploadedFiles = uploadedFiles
        console.log('[PlanActApiService] Including uploaded files:', uploadedFiles.length)
      }
      if (replacementParams && Object.keys(replacementParams).length > 0) {
        requestBody.replacementParams = replacementParams
        console.log('[PlanActApiService] Including replacement params:', replacementParams)
      }
      
      console.log('[PlanActApiService] Making request to:', `/api/executor/executeByToolNameAsync`)
      console.log('[PlanActApiService] Request body:', requestBody)
      
      const response = await fetch(`/api/executor/executeByToolNameAsync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      })
      
      console.log('[PlanActApiService] Response status:', response.status, response.ok)
      
      if (!response.ok) {
        const errorText = await response.text()
        console.error('[PlanActApiService] Request failed:', errorText)
        throw new Error(`Failed to execute plan: ${response.status}`)
      }
      
      const result = await response.json()
      console.log('[PlanActApiService] executePlan response:', result)
      return result
    })
  }

  // Save plan to server
  public static async savePlanTemplate(planId: string, planJson: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId, planJson })
    })
    if (!response.ok) throw new Error(`Failed to save plan: ${response.status}`)
    return await response.json()
  }

  // Get all versions of plan
  public static async getPlanVersions(planId: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/versions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId })
    })
    if (!response.ok) throw new Error(`Failed to get plan versions: ${response.status}`)
    return await response.json()
  }

  // Get specific version of plan
  public static async getVersionPlan(planId: string, versionIndex: number): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/get-version`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId, versionIndex: versionIndex.toString() })
    })
    if (!response.ok) throw new Error(`Failed to get specific version plan: ${response.status}`)
    return await response.json()
  }

  // Get all plan template list
  public static async getAllPlanTemplates(): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/list`)
    if (!response.ok) throw new Error(`Failed to get plan template list: ${response.status}`)
    return await response.json()
  }

  // Update existing plan template
  public static async updatePlanTemplate(planId: string, query: string, existingJson?: string, planType: string = 'simple'): Promise<any> {
    return LlmCheckService.withLlmCheck(async () => {
      const requestBody: Record<string, any> = { planId, query, planType }
      if (existingJson) requestBody.existingJson = existingJson
      
      const response = await fetch(`${this.PLAN_TEMPLATE_URL}/update`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      })
      if (!response.ok) throw new Error(`Failed to update plan template: ${response.status}`)
      const responseData = await response.json()
      if (responseData.planJson) {
        try {
          responseData.plan = JSON.parse(responseData.planJson)
        } catch {
          responseData.plan = { error: 'Unable to parse plan data' }
        }
      }
      return responseData
    })
  }

  // Delete plan template
  public static async deletePlanTemplate(planId: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId })
    })
    if (!response.ok) throw new Error(`Failed to delete plan template: ${response.status}`)
    return await response.json()
  }

  // Create cron task
  public static async createCronTask(cronConfig: CronConfig): Promise<CronConfig> {
    const response = await fetch(this.CRON_TASK_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cronConfig)
    })
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `Failed to create cron task: ${response.status}`)
      } catch {
        throw new Error(`Failed to create cron task: ${response.status}`)
      }
    }
    return await response.json()
  }

}
