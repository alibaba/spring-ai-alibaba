// 计划相关 API 封装（TypeScript 版，适用于 Vue 项目）
import type { Ref } from 'vue'

const PLAN_TEMPLATE_URL = '/api/plan-template'

// 生成计划
export async function generatePlan(query: string, existingJson?: string): Promise<any> {
  const requestBody: Record<string, any> = { query }
  if (existingJson) requestBody.existingJson = existingJson
  const response = await fetch(`${PLAN_TEMPLATE_URL}/generate`, {
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
export async function executePlan(planTemplateId: string, rawParam?: string): Promise<any> {
  const requestBody: Record<string, any> = { planTemplateId }
  if (rawParam) requestBody.rawParam = rawParam
  const response = await fetch(`${PLAN_TEMPLATE_URL}/executePlanByTemplateId`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestBody)
  })
  if (!response.ok) throw new Error(`执行计划失败: ${response.status}`)
  return await response.json()
}

// 保存计划到服务器
export async function savePlanTemplate(planId: string, planJson: string): Promise<any> {
  const response = await fetch(`${PLAN_TEMPLATE_URL}/save`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planId, planJson })
  })
  if (!response.ok) throw new Error(`保存计划失败: ${response.status}`)
  return await response.json()
}

// 获取计划的所有版本
export async function getPlanVersions(planId: string): Promise<any> {
  const response = await fetch(`${PLAN_TEMPLATE_URL}/versions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planId })
  })
  if (!response.ok) throw new Error(`获取计划版本失败: ${response.status}`)
  return await response.json()
}

// 获取特定版本的计划
export async function getVersionPlan(planId: string, versionIndex: number): Promise<any> {
  const response = await fetch(`${PLAN_TEMPLATE_URL}/get-version`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planId, versionIndex: versionIndex.toString() })
  })
  if (!response.ok) throw new Error(`获取特定版本计划失败: ${response.status}`)
  return await response.json()
}

// 获取所有计划模板列表
export async function getAllPlanTemplates(): Promise<any> {
  const response = await fetch(`${PLAN_TEMPLATE_URL}/list`)
  if (!response.ok) throw new Error(`获取计划模板列表失败: ${response.status}`)
  return await response.json()
}

// 更新现有计划模板
export async function updatePlanTemplate(planId: string, query: string, existingJson?: string): Promise<any> {
  const requestBody: Record<string, any> = { planId, query }
  if (existingJson) requestBody.existingJson = existingJson
  const response = await fetch(`${PLAN_TEMPLATE_URL}/update`, {
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
export async function deletePlanTemplate(planId: string): Promise<any> {
  const response = await fetch(`${PLAN_TEMPLATE_URL}/delete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planId })
  })
  if (!response.ok) throw new Error(`删除计划模板失败: ${response.status}`)
  return await response.json()
}
