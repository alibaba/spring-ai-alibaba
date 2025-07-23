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

// Define interface types
import type {Model} from "@/api/model-api-service";

export interface Agent {
  id: string
  name: string
  description: string
  availableTools: string[]
  nextStepPrompt?: string,
  model?: Model | null
}

export interface Tool {
  key: string
  name: string
  description: string
  enabled: boolean
  serviceGroup?: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
}

/**
 * Agent API service class
 * Responsible for interacting with backend AgentController
 */
export class AgentApiService {
  private static readonly BASE_URL = '/api/agents'

  /**
   * Handle HTTP response
   */
  private static async handleResponse(response: Response) {
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `API request failed: ${response.status}`)
      } catch {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`)
      }
    }
    return response
  }

  /**
   * Get all Agent list
   */
  static async getAllAgents(namespace?:string): Promise<Agent[]> {
    try {
     if(namespace){
      const response = await fetch(`${this.BASE_URL}/namespace/${namespace}`)
      const result = await this.handleResponse(response)
      return await result.json()
     }else{
      const response = await fetch(`${this.BASE_URL}`)
      const result = await this.handleResponse(response)
      return await result.json()

     }
    } catch (error) {
      console.error('Failed to get Agent list:', error)
      throw error
    }
  }

  /**
   * Get Agent details by ID
   */
  static async getAgentById(id: string): Promise<Agent> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to get Agent[${id}] details:`, error)
      throw error
    }
  }

  /**
   * Create new Agent
   */
  static async createAgent(agentConfig: Omit<Agent, 'id'>): Promise<Agent> {
    try {
      const response = await fetch(this.BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(agentConfig)
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to create Agent:', error)
      throw error
    }
  }

  /**
   * Update Agent configuration
   */
  static async updateAgent(id: string, agentConfig: Agent): Promise<Agent> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(agentConfig)
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to update Agent[${id}]:`, error)
      throw error
    }
  }

  /**
   * Delete Agent
   */
  static async deleteAgent(id: string): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE'
      })
      if (response.status === 400) {
        throw new Error('Cannot delete default Agent')
      }
      await this.handleResponse(response)
    } catch (error) {
      console.error(`Failed to delete Agent[${id}]:`, error)
      throw error
    }
  }

  /**
   * Get all available tools list
   */
  static async getAvailableTools(): Promise<Tool[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/tools`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get available tools list:', error)
      throw error
    }
  }
}

/**
 * Agent configuration management model class
 * Provides CRUD operations and local state management for Agent configuration
 */
export class AgentConfigModel {
  public agents: Agent[] = []
  public currentAgent: Agent | null = null
  public availableTools: Tool[] = []

  /**
   * Load all Agent list
   */
  async loadAgents(): Promise<Agent[]> {
    try {
      this.agents = await AgentApiService.getAllAgents()
      return this.agents
    } catch (error) {
      console.error('Failed to load Agent list:', error)
      throw error
    }
  }

  /**
   * Load Agent details
   */
  async loadAgentDetails(id: string): Promise<Agent> {
    try {
      this.currentAgent = await AgentApiService.getAgentById(id)
      return this.currentAgent
    } catch (error) {
      console.error('Failed to load Agent details:', error)
      throw error
    }
  }

  /**
   * Load available tools list
   */
  async loadAvailableTools(): Promise<Tool[]> {
    try {
      this.availableTools = await AgentApiService.getAvailableTools()
      return this.availableTools
    } catch (error) {
      console.error('Failed to load available tools list:', error)
      throw error
    }
  }

  /**
   * Save Agent configuration
   */
  async saveAgent(agentData: Agent, isImport = false): Promise<Agent> {
    try {
      let result: Agent

      if (isImport) {
        // Import new Agent, remove ID
        const importData = { ...agentData }
        delete (importData as any).id
        result = await AgentApiService.createAgent(importData)
      } else if (agentData.id) {
        // Update existing Agent
        result = await AgentApiService.updateAgent(agentData.id, agentData)
      } else {
        // Create new Agent
        result = await AgentApiService.createAgent(agentData)
      }

      // Update local data
      const index = this.agents.findIndex(agent => agent.id === result.id)
      if (index !== -1) {
        this.agents[index] = result
      } else {
        this.agents.push(result)
      }

      return result
    } catch (error) {
      console.error('Failed to save Agent:', error)
      throw error
    }
  }

  /**
   * Delete Agent
   */
  async deleteAgent(id: string): Promise<void> {
    try {
      await AgentApiService.deleteAgent(id)
      this.agents = this.agents.filter(agent => agent.id !== id)
      
      // If deleted is the current selected Agent, clear selection
      if (this.currentAgent?.id === id) {
        this.currentAgent = null
      }
    } catch (error) {
      console.error('Failed to delete Agent:', error)
      throw error
    }
  }

  /**
   * Find Agent by ID
   */
  findAgentById(id: string): Agent | undefined {
    return this.agents.find(agent => agent.id === id)
  }

  /**
   * Add tool to Agent
   */
  addToolToAgent(agentId: string, toolId: string): boolean {
    const agent = this.findAgentById(agentId)
    if (agent && !agent.availableTools.includes(toolId)) {
      agent.availableTools.push(toolId)
      return true
    }
    return false
  }

  /**
   * Remove tool from Agent
   */
  removeToolFromAgent(agentId: string, toolId: string): boolean {
    const agent = this.findAgentById(agentId)
    if (agent) {
      const index = agent.availableTools.indexOf(toolId)
      if (index !== -1) {
        agent.availableTools.splice(index, 1)
        return true
      }
    }
    return false
  }

  /**
   * Export Agent configuration as JSON
   */
  exportAgent(agent: Agent): string {
    return JSON.stringify(agent, null, 2)
  }

  /**
   * Import Agent configuration from JSON string
   */
  parseAgentFromJson(jsonString: string): Agent {
    try {
      const agent = JSON.parse(jsonString)
      // Basic validation
      if (!agent.name || !agent.description) {
        throw new Error('Agent configuration format is incorrect: missing required fields')
      }
      return agent
    } catch (error) {
      console.error('Failed to parse Agent JSON:', error)
      throw new Error('Agent configuration format is incorrect')
    }
  }
}

// Default export an instance for use
export default new AgentConfigModel()
