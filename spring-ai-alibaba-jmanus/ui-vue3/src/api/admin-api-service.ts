
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
 * Admin API 服务
 * 负责处理所有与管理配置相关的 API 交互
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
   * 根据组名获取配置项
   */
  public static async getConfigsByGroup(groupName: string): Promise<ConfigItem[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/group/${groupName}`)
      if (!response.ok) {
        throw new Error(`获取${groupName}组配置失败: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error(`获取${groupName}组配置失败:`, error)
      throw error
    }
  }

  /**
   * 批量更新配置项
   */
  public static async batchUpdateConfigs(configs: ConfigItem[]): Promise<ApiResponse> {
    if (!configs || configs.length === 0) {
      return { success: true, message: '没有需要更新的配置' }
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
        throw new Error(`批量更新配置失败: ${response.status}`)
      }

      return { success: true, message: '配置保存成功' }
    } catch (error) {
      console.error('批量更新配置失败:', error)
      return { 
        success: false, 
        message: error instanceof Error ? error.message : '更新失败，请重试' 
      }
    }
  }

  /**
   * 获取单个配置项
   */
  public static async getConfigById(id: number): Promise<ConfigItem> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      if (!response.ok) {
        throw new Error(`获取配置项失败: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error(`获取配置项[${id}]失败:`, error)
      throw error
    }
  }

  /**
   * 更新单个配置项
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
        throw new Error(`更新配置项失败: ${response.status}`)
      }

      return { success: true, message: '配置更新成功' }
    } catch (error) {
      console.error('更新配置项失败:', error)
      return { 
        success: false, 
        message: error instanceof Error ? error.message : '更新失败，请重试' 
      }
    }
  }


}
