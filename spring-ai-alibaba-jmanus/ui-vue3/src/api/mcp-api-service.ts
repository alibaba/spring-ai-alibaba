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
 * MCP API Service
 * Responsible for handling all API interactions related to MCP configuration
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
   * Get all MCP server configurations
   */
  public static async getAllMcpServers(): Promise<McpServer[]> {
    const response = await fetch(`${this.BASE_URL}/list`)
    if (!response.ok) {
      throw new Error(`Failed to get MCP server list: ${response.status}`)
    }
    return await response.json()
  }

  /**
   * Add new MCP server configuration
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
        throw new Error(`Failed to add MCP server: ${response.status}`)
      }
      
      return { success: true, message: 'Successfully added MCP server' }
    } catch (error) {
      console.error('Failed to add MCP server:', error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to add, please retry'
      }
    }
  }

  /**
   * Delete MCP server configuration
   */
  public static async removeMcpServer(id: number): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/remove?id=${id}`)
      if (!response.ok) {
        throw new Error(`Failed to delete MCP server: ${response.status}`)
      }
      return { success: true, message: 'Successfully deleted MCP server' }
    } catch (error) {
      console.error(`Failed to delete MCP server[${id}]:`, error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to delete, please retry'
      }
    }
  }
}
