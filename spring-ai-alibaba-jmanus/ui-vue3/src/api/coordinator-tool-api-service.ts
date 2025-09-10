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

export interface CoordinatorToolVO {
  id?: number
  toolName: string
  toolDescription: string
  planTemplateId: string
  httpEndpoint?: string | undefined
  mcpEndpoint?: string | undefined
  inputSchema: string
  serviceGroup?: string
  enableInternalToolcall?: boolean
  enableHttpService?: boolean
  enableMcpService?: boolean
  createTime?: string
  updateTime?: string
}

export interface CoordinatorToolConfig {
  enabled: boolean
  success: boolean
  message?: string
}

export class CoordinatorToolApiService {
  private static readonly BASE_URL = '/api/coordinator-tools'

  /**
   * 获取CoordinatorTool配置信息
   */
  public static async getCoordinatorToolConfig(): Promise<CoordinatorToolConfig> {
    try {
      const response = await fetch(`${this.BASE_URL}/config`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error(`Failed to get coordinator tool config: ${response.status}`)
      }

      return await response.json()
    } catch (error: any) {
      console.error('获取CoordinatorTool配置失败:', error)
      // 返回默认配置
      return {
        enabled: true,
        success: false,
        message: error.message
      }
    }
  }

  /**
   * 获取所有endpoint列表
   */
  public static async getAllEndpoints(): Promise<string[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/endpoints`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error(`Failed to get endpoints: ${response.status}`)
      }

      return await response.json()
    } catch (error: any) {
      console.error('获取endpoints失败:', error)
      throw new Error('获取endpoints失败: ' + error.message)
    }
  }

  /**
   * 根据计划模板ID获取协调器工具（仅获取已存在的）
   */
  public static async getCoordinatorToolsByTemplate(planTemplateId: string): Promise<CoordinatorToolVO | null> {
    console.log('[CoordinatorToolApiService] 开始获取协调器工具，planTemplateId:', planTemplateId)
    console.log('[CoordinatorToolApiService] 请求URL:', `${this.BASE_URL}/get-get-or-new-by-template/${planTemplateId}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] 响应状态:', response.status)
      console.log('[CoordinatorToolApiService] 响应状态文本:', response.statusText)

      if (response.status === 404) {
        console.log('[CoordinatorToolApiService] 404')
        return null
      }

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] 响应错误内容:', errorText)
        throw new Error(`Failed to get coordinator tools: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] 获取协调器工具成功，结果:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] 获取协调器工具失败:', error)
      throw new Error('获取协调器工具失败: ' + error.message)
    }
  }

  /**
   * 根据计划模板ID获取或创建协调器工具
   */
  public static async getOrNewCoordinatorToolsByTemplate(planTemplateId: string): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] 开始获取或创建协调器工具，planTemplateId:', planTemplateId)
    console.log('[CoordinatorToolApiService] 请求URL:', `${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] 响应状态:', response.status)
      console.log('[CoordinatorToolApiService] 响应状态文本:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] 响应错误内容:', errorText)
        throw new Error(`Failed to get coordinator tools: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] 获取协调器工具成功，结果:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] 获取协调器工具失败:', error)
      throw new Error('获取协调器工具失败: ' + error.message)
    }
  }

  /**
   * 创建协调器工具
   */
  public static async createCoordinatorTool(tool: CoordinatorToolVO): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] 开始创建协调器工具')
    console.log('[CoordinatorToolApiService] 原始数据:', JSON.stringify(tool, null, 2))
    console.log('[CoordinatorToolApiService] 请求URL:', `${this.BASE_URL}`)
    
    // 只发送必要的字段，不包含createTime和updateTime
    const requestData = {
      id: tool.id,
      toolName: tool.toolName,
      toolDescription: tool.toolDescription,
      inputSchema: tool.inputSchema,
      planTemplateId: tool.planTemplateId,
      httpEndpoint: tool.httpEndpoint,
      mcpEndpoint: tool.mcpEndpoint,
      serviceGroup: tool.serviceGroup,
      enableInternalToolcall: tool.enableInternalToolcall,
      enableHttpService: tool.enableHttpService,
      enableMcpService: tool.enableMcpService,
    }
    
    console.log('[CoordinatorToolApiService] 清理后的发送数据:', JSON.stringify(requestData, null, 2))
    
    try {
      const response = await fetch(`${this.BASE_URL}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })

      console.log('[CoordinatorToolApiService] 响应状态:', response.status)
      console.log('[CoordinatorToolApiService] 响应状态文本:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] 响应错误内容:', errorText)
        throw new Error(`Failed to create coordinator tool: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] 创建成功，结果:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] 创建协调器工具失败:', error)
      throw new Error('创建协调器工具失败: ' + error.message)
    }
  }

  /**
   * 更新协调器工具
   */
  public static async updateCoordinatorTool(id: number, tool: CoordinatorToolVO): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] 开始更新协调器工具，ID:', id)
    console.log('[CoordinatorToolApiService] 发送的数据:', tool)
    console.log('[CoordinatorToolApiService] 请求URL:', `${this.BASE_URL}/${id}`)
    
    // 只发送必要的字段，不包含createTime和updateTime
    const requestData = {
      id: tool.id,
      toolName: tool.toolName,
      toolDescription: tool.toolDescription,
      inputSchema: tool.inputSchema,
      planTemplateId: tool.planTemplateId,
      httpEndpoint: tool.httpEndpoint,
      mcpEndpoint: tool.mcpEndpoint,
      serviceGroup: tool.serviceGroup,
      enableInternalToolcall: tool.enableInternalToolcall,
      enableHttpService: tool.enableHttpService,
      enableMcpService: tool.enableMcpService,
    }
    
    console.log('[CoordinatorToolApiService] 清理后的发送数据:', requestData)
    
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })

      console.log('[CoordinatorToolApiService] 响应状态:', response.status)
      console.log('[CoordinatorToolApiService] 响应状态文本:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] 响应错误内容:', errorText)
        throw new Error(`Failed to update coordinator tool: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] 更新成功，结果:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] 更新协调器工具失败:', error)
      throw new Error('更新协调器工具失败: ' + error.message)
    }
  }


  /**
   * 删除协调器工具
   */
  public static async deleteCoordinatorTool(id: number): Promise<{ success: boolean; message: string }> {
    console.log('[CoordinatorToolApiService] 开始删除协调器工具，ID:', id)
    console.log('[CoordinatorToolApiService] 请求URL:', `${this.BASE_URL}/${id}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] 响应状态:', response.status)
      console.log('[CoordinatorToolApiService] 响应状态文本:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] 响应错误内容:', errorText)
        throw new Error(`Failed to delete coordinator tool: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] 删除成功，结果:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] 删除协调器工具失败:', error)
      throw new Error('删除协调器工具失败: ' + error.message)
    }
  }
} 