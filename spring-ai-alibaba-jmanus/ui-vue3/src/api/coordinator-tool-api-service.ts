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
   * Get CoordinatorTool configuration information
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
          console.error('Failed to get CoordinatorTool configuration:', error)
          // Return default configuration
      return {
        enabled: true,
        success: false,
        message: error.message
      }
    }
  }

  /**
   * Get all endpoint list
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
      console.error('Failed to get endpoints:', error)
      throw new Error('Failed to get endpoints: ' + error.message)
    }
  }

  /**
   * Get coordinator tool by plan template ID (only get existing ones)
   */
  public static async getCoordinatorToolsByTemplate(planTemplateId: string): Promise<CoordinatorToolVO | null> {
        console.log('[CoordinatorToolApiService] Starting to get coordinator tool, planTemplateId:', planTemplateId)
    console.log('[CoordinatorToolApiService] Request URL:', `${this.BASE_URL}/get-get-or-new-by-template/${planTemplateId}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] Response status:', response.status)
      console.log('[CoordinatorToolApiService] Response status text:', response.statusText)

      if (response.status === 404) {
        console.log('[CoordinatorToolApiService] 404')
        return null
      }

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] Response error content:', errorText)
        throw new Error(`Failed to get coordinator tools: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] Successfully got coordinator tool, result:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] Failed to get coordinator tool:', error)
      throw new Error('Failed to get coordinator tool: ' + error.message)
    }
  }

  /**
   * Get or create coordinator tool by plan template ID
   */
  public static async getOrNewCoordinatorToolsByTemplate(planTemplateId: string): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] Starting to get or create coordinator tool, planTemplateId:', planTemplateId)
    console.log('[CoordinatorToolApiService] Request URL:', `${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/get-or-new-by-template/${planTemplateId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] Response status:', response.status)
      console.log('[CoordinatorToolApiService] Response status text:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] Response error content:', errorText)
        throw new Error(`Failed to get coordinator tools: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] Successfully got coordinator tool, result:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] Failed to get coordinator tool:', error)
      throw new Error('Failed to get coordinator tool: ' + error.message)
    }
  }

  /**
   * Create coordinator tool
   */
  public static async createCoordinatorTool(tool: CoordinatorToolVO): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] Starting to create coordinator tool')
    console.log('[CoordinatorToolApiService] Original data:', JSON.stringify(tool, null, 2))
    console.log('[CoordinatorToolApiService] Request URL:', `${this.BASE_URL}`)
    
    // Only send necessary fields, excluding createTime and updateTime
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
    
    console.log('[CoordinatorToolApiService] Cleaned sending data:', JSON.stringify(requestData, null, 2))
    
    try {
      const response = await fetch(`${this.BASE_URL}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })

      console.log('[CoordinatorToolApiService] Response status:', response.status)
      console.log('[CoordinatorToolApiService] Response status text:', response.statusText)

      if (!response.ok) {
        let errorMessage = 'Unknown error'
        try {
          const errorData = await response.json()
          errorMessage = errorData.message || errorData.error || errorMessage
        } catch {
          // If JSON parsing fails, try to get text
          const errorText = await response.text()
          errorMessage = errorText || errorMessage
        }
        console.error('[CoordinatorToolApiService] Response error content:', errorMessage)
        throw new Error(`Failed to create coordinator tool: ${response.status} - ${errorMessage}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] Created successfully, result:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] Failed to create coordinator tool:', error)
      throw new Error('Failed to create coordinator tool: ' + error.message)
    }
  }

  /**
   * Update coordinator tool
   */
  public static async updateCoordinatorTool(id: number, tool: CoordinatorToolVO): Promise<CoordinatorToolVO> {
    console.log('[CoordinatorToolApiService] Starting to update coordinator tool, ID:', id)
    console.log('[CoordinatorToolApiService] Sending data:', tool)
    console.log('[CoordinatorToolApiService] Request URL:', `${this.BASE_URL}/${id}`)
    
    // Only send necessary fields, excluding createTime and updateTime
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
    
    console.log('[CoordinatorToolApiService] Cleaned sending data:', requestData)
    
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })

      console.log('[CoordinatorToolApiService] Response status:', response.status)
      console.log('[CoordinatorToolApiService] Response status text:', response.statusText)

      if (!response.ok) {
        let errorMessage = 'Unknown error'
        try {
          const errorData = await response.json()
          errorMessage = errorData.message || errorData.error || errorMessage
        } catch {
          // If JSON parsing fails, try to get text
          const errorText = await response.text()
          errorMessage = errorText || errorMessage
        }
        console.error('[CoordinatorToolApiService] Response error content:', errorMessage)
        throw new Error(`Failed to update coordinator tool: ${response.status} - ${errorMessage}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] Updated successfully, result:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] Failed to update coordinator tool:', error)
      throw new Error('Failed to update coordinator tool: ' + error.message)
    }
  }


  /**
   * Delete coordinator tool
   */
  public static async deleteCoordinatorTool(id: number): Promise<{ success: boolean; message: string }> {
    console.log('[CoordinatorToolApiService] Starting to delete coordinator tool, ID:', id)
    console.log('[CoordinatorToolApiService] Request URL:', `${this.BASE_URL}/${id}`)
    
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('[CoordinatorToolApiService] Response status:', response.status)
      console.log('[CoordinatorToolApiService] Response status text:', response.statusText)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[CoordinatorToolApiService] Response error content:', errorText)
        throw new Error(`Failed to delete coordinator tool: ${response.status} - ${errorText}`)
      }

      const result = await response.json()
      console.log('[CoordinatorToolApiService] Deleted successfully, result:', result)
      return result
    } catch (error: any) {
      console.error('[CoordinatorToolApiService] Failed to delete coordinator tool:', error)
      throw new Error('Failed to delete coordinator tool: ' + error.message)
    }
  }
} 
