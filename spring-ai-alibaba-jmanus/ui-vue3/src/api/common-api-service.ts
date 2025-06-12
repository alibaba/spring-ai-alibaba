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

// 通用请求方法（TypeScript 版，适用于 Vue 项目）

export class CommonApiService {
  private static readonly BASE_URL = '/api/executor'

  // 获取详细的执行记录
  public static async getDetails(planId: string): Promise<any | null> {
    try {
      const response = await fetch(`${this.BASE_URL}/details/${planId}`)
      if (response.status === 404) {
        // 404 返回 null
        return null
      }
      if (!response.ok) throw new Error(`获取详细信息失败: ${response.status}`)
      const rawText = await response.text()
      try {
        return JSON.parse(rawText)
      } catch (jsonParseError) {
        throw jsonParseError
      }
    } catch (error: any) {
      // 记录错误但不抛出异常
      return null
    }
  }

  // 提交用户表单输入
  public static async submitFormInput(planId: string, formData: any): Promise<any> {
    const response = await fetch(`${this.BASE_URL}/submit-input/${planId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    })
    if (!response.ok) {
      let errorData
      try {
        errorData = await response.json()
      } catch (e) {
        errorData = { message: `提交表单输入失败: ${response.status}` }
      }
      throw new Error(errorData.message || `提交表单输入失败: ${response.status}`)
    }
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.indexOf('application/json') !== -1) {
      return await response.json()
    }
    return { success: true }
  }
}
