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

// 计划相关 API 封装（TypeScript 版，适用于 Vue 项目）
import type { Ref } from 'vue'

export class PlanActApiService {
  private static readonly PLAN_TEMPLATE_URL = '/api/plan-template'

  // 生成计划
  public static async generatePlan(query: string, existingJson?: string): Promise<any> {
    const requestBody: Record<string, any> = { query }
    if (existingJson) requestBody.existingJson = existingJson
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    })
    if (!response.ok) throw new Error(`生成计划失败: ${response.status}`)
    const responseData = await response.json()
    if (responseData.planJson) {
      try {
        responseData.plan = JSON.parse(responseData.planJson)
      } catch (e) {
        responseData.plan = { error: '无法解析计划数据' }
      }
    }
    return responseData
  }

  // 执行已生成的计划
  public static async executePlan(planTemplateId: string, rawParam?: string): Promise<any> {
    console.log('[PlanActApiService] executePlan called with:', { planTemplateId, rawParam })
    
    const requestBody: Record<string, any> = { planTemplateId }
    if (rawParam) requestBody.rawParam = rawParam
    
    console.log('[PlanActApiService] Making request to:', `${this.PLAN_TEMPLATE_URL}/executePlanByTemplateId`)
    console.log('[PlanActApiService] Request body:', requestBody)
    
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/executePlanByTemplateId`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    })
    
    console.log('[PlanActApiService] Response status:', response.status, response.ok)
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error('[PlanActApiService] Request failed:', errorText)
      throw new Error(`执行计划失败: ${response.status}`)
    }
    
    const result = await response.json()
    console.log('[PlanActApiService] executePlan response:', result)
    return result
  }

  // 保存计划到服务器
  public static async savePlanTemplate(planId: string, planJson: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId, planJson })
    })
    if (!response.ok) throw new Error(`保存计划失败: ${response.status}`)
    return await response.json()
  }

  // 获取计划的所有版本
  public static async getPlanVersions(planId: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/versions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId })
    })
    if (!response.ok) throw new Error(`获取计划版本失败: ${response.status}`)
    return await response.json()
  }

  // 获取特定版本的计划
  public static async getVersionPlan(planId: string, versionIndex: number): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/get-version`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId, versionIndex: versionIndex.toString() })
    })
    if (!response.ok) throw new Error(`获取特定版本计划失败: ${response.status}`)
    return await response.json()
  }

  // 获取所有计划模板列表
  public static async getAllPlanTemplates(): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/list`)
    if (!response.ok) throw new Error(`获取计划模板列表失败: ${response.status}`)
    return await response.json()
  }

  // 更新现有计划模板
  public static async updatePlanTemplate(planId: string, query: string, existingJson?: string): Promise<any> {
    const requestBody: Record<string, any> = { planId, query }
    if (existingJson) requestBody.existingJson = existingJson
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/update`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    })
    if (!response.ok) throw new Error(`更新计划模板失败: ${response.status}`)
    const responseData = await response.json()
    if (responseData.planJson) {
      try {
        responseData.plan = JSON.parse(responseData.planJson)
      } catch (e) {
        responseData.plan = { error: '无法解析计划数据' }
      }
    }
    return responseData
  }

  // 删除计划模板
  public static async deletePlanTemplate(planId: string): Promise<any> {
    const response = await fetch(`${this.PLAN_TEMPLATE_URL}/delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId })
    })
    if (!response.ok) throw new Error(`删除计划模板失败: ${response.status}`)
    return await response.json()
  }

}
