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
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  connectionConfig: string
  status: 'ENABLE' | 'DISABLE'
}

export interface McpServerRequest {
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  configJson: string
}

export interface McpServerFieldRequest {
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  mcpServerName: string
  command?: string | undefined
  url?: string | undefined
  args?: string[] | undefined
  env?: Record<string, string> | undefined
  status: 'ENABLE' | 'DISABLE'
}

// Merged request interface for adding and updating
export interface McpServerSaveRequest extends McpServerFieldRequest {
  id?: number // Optional, if present it's an update, otherwise it's a new addition
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
        // Read detailed error information from the response body
        const errorText = await response.text()
        throw new Error(`Failed to add MCP server: ${response.status} - ${errorText}`)
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
   * Import MCP server configurations from JSON
   */
  public static async importMcpServers(jsonData: any): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/batch-import`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          configJson: JSON.stringify(jsonData),
          overwrite: false
        })
      })
      
      if (!response.ok) {
        // Read detailed error information from the response body
        const errorText = await response.text()
        throw new Error(`Failed to import MCP servers: ${response.status} - ${errorText}`)
      }
      
      return { success: true, message: 'Successfully imported MCP servers' }
    } catch (error) {
      console.error('Failed to import MCP servers:', error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to import, please retry'
      }
    }
  }



  /**
   * Save MCP server configuration (add or update based on whether id is provided)
   */
  public static async saveMcpServer(mcpConfig: McpServerSaveRequest): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/server`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(mcpConfig)
      })
      
      if (!response.ok) {
        // Read detailed error information from the response body
        const errorText = await response.text()
        const action = mcpConfig.id !== undefined ? 'update' : 'add'
        throw new Error(`Failed to ${action} MCP server: ${response.status} - ${errorText}`)
      }
      
      const action = mcpConfig.id !== undefined ? 'updated' : 'added'
      return { success: true, message: `Successfully ${action} MCP server` }
    } catch (error) {
      console.error('Failed to save MCP server:', error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to save, please retry'
      }
    }
  }

  /**
   * Update MCP server configuration using field-based data (deprecated, use saveMcpServer instead)
   */
  public static async updateMcpServer(id: number, mcpConfig: McpServerFieldRequest): Promise<ApiResponse> {
    return this.saveMcpServer({ ...mcpConfig, id })
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

  /**
   * Enable MCP server configuration
   */
  public static async enableMcpServer(id: number): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/enable/${id}`, {
        method: 'POST'
      })
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Failed to enable MCP server: ${response.status} - ${errorText}`)
      }
      return { success: true, message: 'Successfully enabled MCP server' }
    } catch (error) {
      console.error(`Failed to enable MCP server[${id}]:`, error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to enable, please retry'
      }
    }
  }

  /**
   * Disable MCP server configuration
   */
  public static async disableMcpServer(id: number): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/disable/${id}`, {
        method: 'POST'
      })
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Failed to disable MCP server: ${response.status} - ${errorText}`)
      }
      return { success: true, message: 'Successfully disabled MCP server' }
    } catch (error) {
      console.error(`Failed to disable MCP server[${id}]:`, error)
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to disable, please retry'
      }
    }
  }
}
