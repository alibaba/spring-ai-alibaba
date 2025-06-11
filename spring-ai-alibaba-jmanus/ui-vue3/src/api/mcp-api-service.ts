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


/**
 * MCP API 服务
 * 负责处理所有与 MCP 配置相关的 API 交互
 */

export interface McpServer {
  id: number
  mcpServerName: string
  connectionType: 'STUDIO' | 'SSE'
  connectionConfig: string
}

export interface McpServerRequest {
  connectionType: 'STUDIO' | 'SSE'
  configJson: string
}

export interface ApiResponse<T = any> {
  success: boolean
  message: string
  data?: T
}

export class McpApiService {
  private static readonly BASE_URL = '/api/mcp'

  /**
   * 获取所有 MCP 服务器配置
   */
  public static async getAllMcpServers(): Promise<McpServer[]> {
    const response = await fetch(`${this.BASE_URL}/list`)
    if (!response.ok) {
      throw new Error(`获取 MCP 服务器列表失败: ${response.status}`)
    }
    return await response.json()
  }

  /**
   * 添加新的 MCP 服务器配置
   */
  public static async addMcpServer(mcpConfig: McpServerRequest): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(mcpConfig)
      })
      
      if (!response.ok) {
        throw new Error(`添加 MCP 服务器失败: ${response.status}`)
      }
      
      return { success: true, message: '添加 MCP 服务器成功' }
    } catch (error) {
      console.error('添加 MCP 服务器失败:', error)
      return {
        success: false,
        message: error instanceof Error ? error.message : '添加失败，请重试'
      }
    }
  }

  /**
   * 删除 MCP 服务器配置
   */
  public static async removeMcpServer(id: number): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/remove?id=${id}`)
      if (!response.ok) {
        throw new Error(`删除 MCP 服务器失败: ${response.status}`)
      }
      return { success: true, message: '删除 MCP 服务器成功' }
    } catch (error) {
      console.error(`删除 MCP 服务器[${id}]失败:`, error)
      return {
        success: false,
        message: error instanceof Error ? error.message : '删除失败，请重试'
      }
    }
  }
}
