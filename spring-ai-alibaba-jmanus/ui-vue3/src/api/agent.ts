export interface AgentEntity {
  id: number
  agentName: string
  agentDescription: string
  nextStepPrompt: string
  availableToolKeys: string[]
  namespace: string
  className: string
}

export interface AgentLanguageInfo {
  languages: string[]
  default: string
}

export interface AgentStats {
  total: number
  namespace: string
  supportedLanguages: string[]
}

export interface ResetAgentsRequest {
  language: string
}

export interface ResetAgentsResponse {
  message: string
  language: string
  namespace: string
}

/**
 * Handle fetch response
 */
const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Network error' }))
    throw new Error(error.message || `HTTP error! status: ${response.status}`)
  }
  return response.json()
}

/**
 * Get all agents
 */
export const getAllAgents = async (): Promise<AgentEntity[]> => {
  const response = await fetch('/api/agent-management')
  return handleResponse(response)
}

/**
 * Get supported languages
 */
export const getSupportedLanguages = async (): Promise<AgentLanguageInfo> => {
  const response = await fetch('/api/agent-management/languages')
  return handleResponse(response)
}

/**
 * Reset all agents to specific language
 */
export const resetAllAgents = async (data: ResetAgentsRequest): Promise<ResetAgentsResponse> => {
  const response = await fetch('/api/agent-management/reset', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })
  return handleResponse(response)
}

/**
 * Initialize agents with specific language
 */
export const initializeAgents = async (data: ResetAgentsRequest): Promise<ResetAgentsResponse> => {
  const response = await fetch('/api/agent-management/initialize', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })
  return handleResponse(response)
}

/**
 * Get agent statistics
 */
export const getAgentStats = async (): Promise<AgentStats> => {
  const response = await fetch('/api/agent-management/stats')
  return handleResponse(response)
}
