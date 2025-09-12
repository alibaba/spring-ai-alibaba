/**
 * Plan Parameter API Service
 * Handles API calls for plan parameter requirements
 */

export interface ParameterRequirements {
  parameters: string[]
  hasParameters: boolean
  requirements: string
}

export class PlanParameterApiService {
  private static readonly BASE_URL = '/api/plan-template'

  /**
   * Get parameter requirements for a plan template
   * @param planTemplateId The plan template ID
   * @returns Parameter requirements
   */
  static async getParameterRequirements(planTemplateId: string): Promise<ParameterRequirements> {
    try {
      const response = await fetch(`${this.BASE_URL}/${planTemplateId}/parameters`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error(`Failed to get parameter requirements: ${response.statusText}`)
      }

      const data = await response.json()
      return data
    } catch (error) {
      console.error('Error getting parameter requirements:', error)
      throw error
    }
  }
}
