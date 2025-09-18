import type { AgentExecutionRecordDetail } from '@/types/agent-execution-detail'

const API_BASE_URL = '/api/executor'

/**
 * Fetch detailed agent execution record by stepId
 * @param stepId The stepId to query
 * @returns Promise<AgentExecutionRecordDetail | null>
 */
export async function fetchAgentExecutionDetail(stepId: string): Promise<AgentExecutionRecordDetail | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/agent-execution/${stepId}`)
    
    if (!response.ok) {
      if (response.status === 404) {
        console.warn(`Agent execution detail not found for stepId: ${stepId}`)
        return null
      }
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    return data as AgentExecutionRecordDetail
  } catch (error) {
    console.error(`Error fetching agent execution detail for stepId: ${stepId}:`, error)
    return null
  }
}

/**
 * Refresh agent execution detail by stepId
 * This is a wrapper around fetchAgentExecutionDetail for explicit refresh operations
 * @param stepId The stepId to refresh
 * @returns Promise<AgentExecutionRecordDetail | null>
 */
export async function refreshAgentExecutionDetail(stepId: string): Promise<AgentExecutionRecordDetail | null> {
  console.log(`Refreshing agent execution detail for stepId: ${stepId}`)
  return fetchAgentExecutionDetail(stepId)
}
