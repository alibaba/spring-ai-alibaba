
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
 * Admin API Service
 * Handles all API interactions related to administration configuration
 */

export interface ConfigOption {
  value: string
  label: string
}

export interface ConfigItem {
  id: number
  configGroup: string
  configKey: string
  configValue: string
  inputType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'TEXTAREA' | 'CHECKBOX' | 'TEXT'
  configSubGroup?: string
  description?: string
  options?: (string | ConfigOption)[] // For SELECT/CHECKBOX type
  min?: number
  max?: number
  _modified?: boolean
}

export interface ApiResponse<T = any> {
  success: boolean
  message: string
  data?: T
}

export class AdminApiService {
  private static readonly BASE_URL = '/api/config'

  /**
   * Get configuration items by group name
   */
  public static async getConfigsByGroup(groupName: string): Promise<ConfigItem[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/group/${groupName}`)
      if (!response.ok) {
        throw new Error(`Failed to get ${groupName} group configuration: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error(`Failed to get ${groupName} group configuration:`, error)
      throw error
    }
  }

  /**
   * Batch update configuration items
   */
  public static async batchUpdateConfigs(configs: ConfigItem[]): Promise<ApiResponse> {
    if (configs.length === 0) {
      return { success: true, message: 'No configuration needs to be updated' }
    }

    try {
      const response = await fetch(`${this.BASE_URL}/batch-update`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(configs)
      })

      if (!response.ok) {
        throw new Error(`Batch update configuration failed: ${response.status}`)
      }

      return { success: true, message: 'Configuration saved successfully' }
    } catch (error) {
      console.error('Batch update configuration failed:', error)
      return { 
        success: false, 
        message: error instanceof Error ? error.message : 'Update failed, please try again' 
      }
    }
  }

  /**
   * Get single configuration item
   */
  public static async getConfigById(id: number): Promise<ConfigItem> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      if (!response.ok) {
        throw new Error(`Failed to get configuration item: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error(`Failed to get configuration item[${id}]:`, error)
      throw error
    }
  }

  /**
   * Update single configuration item
   */
  public static async updateConfig(config: ConfigItem): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/${config.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(config)
      })

      if (!response.ok) {
        throw new Error(`Failed to update configuration item: ${response.status}`)
      }

      return { success: true, message: 'Configuration updated successfully' }
    } catch (error) {
      console.error('Failed to update configuration item:', error)
      return { 
        success: false, 
        message: error instanceof Error ? error.message : 'Update failed, please try again' 
      }
    }
  }

  /**
   * Reset all configurations to default values
   */
  public static async resetAllConfigsToDefaults(): Promise<ApiResponse> {
    try {
      const response = await fetch(`${this.BASE_URL}/reset-all-defaults`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error(`Failed to reset configurations to defaults: ${response.status}`)
      }

      return { success: true, message: 'All configurations reset to defaults successfully' }
    } catch (error) {
      console.error('Failed to reset configurations to defaults:', error)
      return { 
        success: false, 
        message: error instanceof Error ? error.message : 'Reset failed, please try again' 
      }
    }
  }

}
