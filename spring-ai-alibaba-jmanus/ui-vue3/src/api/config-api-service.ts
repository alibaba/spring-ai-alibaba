import type { AxiosResponse } from 'axios'
import axios from 'axios'

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
      const response: AxiosResponse<AvailableModelsResponse> = await axios({
        url: '/api/models/available-models',
        method: 'GET',
        baseURL: '' // Override the default /api/v1 baseURL
      })
      return response.data
    } catch (error) {
      console.error('Failed to fetch available models:', error)
      return { options: [], total: 0 }
    }
  }
} 
