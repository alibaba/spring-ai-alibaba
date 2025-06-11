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

// 定义接口类型
export interface Agent {
  id: string
  name: string
  description: string
  availableTools: string[]
  nextStepPrompt?: string
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
 * Agent API 服务类
 * 负责与后端 AgentController 进行交互
 */
export class AgentApiService {
  private static readonly BASE_URL = '/api/agents'

  /**
   * 处理 HTTP 响应
   */
  private static async handleResponse(response: Response) {
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `API请求失败: ${response.status}`)
      } catch (e) {
        throw new Error(`API请求失败: ${response.status} ${response.statusText}`)
      }
    }
    return response
  }

  /**
   * 获取所有 Agent 列表
   */
  static async getAllAgents(): Promise<Agent[]> {
    try {
      const response = await fetch(this.BASE_URL)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('获取Agent列表失败:', error)
      throw error
    }
  }

  /**
   * 根据 ID 获取 Agent 详情
   */
  static async getAgentById(id: string): Promise<Agent> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`获取Agent[${id}]详情失败:`, error)
      throw error
    }
  }

  /**
   * 创建新的 Agent
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
      console.error('创建Agent失败:', error)
      throw error
    }
  }

  /**
   * 更新 Agent 配置
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
      console.error(`更新Agent[${id}]失败:`, error)
      throw error
    }
  }

  /**
   * 删除 Agent
   */
  static async deleteAgent(id: string): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE'
      })
      if (response.status === 400) {
        throw new Error('不能删除默认Agent')
      }
      await this.handleResponse(response)
    } catch (error) {
      console.error(`删除Agent[${id}]失败:`, error)
      throw error
    }
  }

  /**
   * 获取所有可用工具列表
   */
  static async getAvailableTools(): Promise<Tool[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/tools`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('获取可用工具列表失败:', error)
      throw error
    }
  }
}

/**
 * Agent 配置管理模型类
 * 提供 Agent 配置的 CRUD 操作和本地状态管理
 */
export class AgentConfigModel {
  public agents: Agent[] = []
  public currentAgent: Agent | null = null
  public availableTools: Tool[] = []

  /**
   * 加载所有 Agent 列表
   */
  async loadAgents(): Promise<Agent[]> {
    try {
      this.agents = await AgentApiService.getAllAgents()
      return this.agents
    } catch (error) {
      console.error('加载Agent列表失败:', error)
      throw error
    }
  }

  /**
   * 加载 Agent 详情
   */
  async loadAgentDetails(id: string): Promise<Agent> {
    try {
      this.currentAgent = await AgentApiService.getAgentById(id)
      return this.currentAgent
    } catch (error) {
      console.error('加载Agent详情失败:', error)
      throw error
    }
  }

  /**
   * 加载可用工具列表
   */
  async loadAvailableTools(): Promise<Tool[]> {
    try {
      this.availableTools = await AgentApiService.getAvailableTools()
      return this.availableTools
    } catch (error) {
      console.error('加载可用工具列表失败:', error)
      throw error
    }
  }

  /**
   * 保存 Agent 配置
   */
  async saveAgent(agentData: Agent, isImport = false): Promise<Agent> {
    try {
      let result: Agent

      if (isImport) {
        // 导入时创建新的 Agent，移除 ID
        const importData = { ...agentData }
        delete (importData as any).id
        result = await AgentApiService.createAgent(importData)
      } else if (agentData.id) {
        // 更新现有 Agent
        result = await AgentApiService.updateAgent(agentData.id, agentData)
      } else {
        // 创建新 Agent
        result = await AgentApiService.createAgent(agentData)
      }

      // 更新本地数据
      const index = this.agents.findIndex(agent => agent.id === result.id)
      if (index !== -1) {
        this.agents[index] = result
      } else {
        this.agents.push(result)
      }

      return result
    } catch (error) {
      console.error('保存Agent失败:', error)
      throw error
    }
  }

  /**
   * 删除 Agent
   */
  async deleteAgent(id: string): Promise<void> {
    try {
      await AgentApiService.deleteAgent(id)
      this.agents = this.agents.filter(agent => agent.id !== id)
      
      // 如果删除的是当前选中的 Agent，清除选中状态
      if (this.currentAgent?.id === id) {
        this.currentAgent = null
      }
    } catch (error) {
      console.error('删除Agent失败:', error)
      throw error
    }
  }

  /**
   * 通过 ID 查找 Agent
   */
  findAgentById(id: string): Agent | undefined {
    return this.agents.find(agent => agent.id === id)
  }

  /**
   * 添加工具到 Agent
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
   * 从 Agent 移除工具
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
   * 导出 Agent 配置为 JSON
   */
  exportAgent(agent: Agent): string {
    return JSON.stringify(agent, null, 2)
  }

  /**
   * 从 JSON 字符串导入 Agent 配置
   */
  parseAgentFromJson(jsonString: string): Agent {
    try {
      const agent = JSON.parse(jsonString)
      // 基本验证
      if (!agent.name || !agent.description) {
        throw new Error('Agent 配置格式不正确：缺少必要字段')
      }
      return agent
    } catch (error) {
      console.error('解析 Agent JSON 失败:', error)
      throw new Error('Agent 配置格式不正确')
    }
  }
}

// 默认导出一个实例，便于使用
export default new AgentConfigModel()
