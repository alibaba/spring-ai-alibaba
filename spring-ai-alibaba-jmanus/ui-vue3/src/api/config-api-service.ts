import type { AxiosResponse } from 'axios'
import request from '@/utils/request'

export interface ModelOption {
  value: string
  label: string
}

export interface AvailableModelsResponse {
  options: ModelOption[]
  total: number
}

export class ConfigApiService {
  /**
   * Get available model list
   */
  public static async getAvailableModels(): Promise<AvailableModelsResponse> {
    try {
      const response: AxiosResponse<AvailableModelsResponse> = await request({
        url: '/api/config/available-models',
        method: 'GET'
      })
      return response.data
    } catch (error) {
      console.error('Failed to fetch available models:', error)
      return { options: [], total: 0 }
    }
  }
} 
