<!-- 
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
-->
<template>
  <div class="config-panel">
    <div class="panel-header">
      <h2>Agent配置</h2>
      <div class="panel-actions">
        <button class="action-btn">
          <Icon icon="carbon:upload" />
          导入
        </button>
        <button class="action-btn">
          <Icon icon="carbon:download" />
          导出
        </button>
      </div>
    </div>

    <div class="agent-layout">
      <!-- Agent列表 -->
      <div class="agent-list">
        <div
          v-for="agent in agents"
          :key="agent.id"
          class="agent-card"
          :class="{ active: selectedAgent?.id === agent.id }"
          @click="selectedAgent = agent"
        >
          <div class="agent-card-header">
            <span class="agent-name">{{ agent.name }}</span>
            <Icon icon="carbon:chevron-right" />
          </div>
          <p class="agent-desc">{{ agent.description }}</p>
          <div class="agent-tools">
            <span v-for="tool in agent.tools" :key="tool" class="tool-tag">
              {{ tool }}
            </span>
          </div>
        </div>

        <button class="add-btn" @click="showAddAgentModal">
          <Icon icon="carbon:add" />
          新建Agent
        </button>
      </div>

      <!-- Agent详情 -->
      <div class="agent-detail" v-if="selectedAgent">
        <div class="form-item">
          <label>Agent名称</label>
          <input type="text" v-model="selectedAgent.name" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="selectedAgent.description" rows="3"></textarea>
        </div>
        <div class="form-item">
          <label>提示词配置</label>
          <textarea
            v-model="selectedAgent.prompt"
            rows="6"
            placeholder="设置Agent执行任务时的提示词"
          ></textarea>
        </div>

        <div class="form-item">
          <label>可用工具</label>
          <div class="tools-list">
            <div v-for="tool in selectedAgent.tools" :key="tool" class="tool-item">
              <span>{{ tool }}</span>
              <Icon icon="carbon:close" class="remove-tool" @click="removeTool(tool)" />
            </div>
          </div>
          <button class="add-btn" @click="handleAddTool">
            <Icon icon="carbon:add" />
            添加工具
          </button>
        </div>

        <div class="detail-actions">
          <Flex justify="space-between" style="width: 100%">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              保存
            </button>
            <button class="action-btn danger" @click="showDeleteConfirm">
              <Icon icon="carbon:trash-can" />
              删除
            </button>
          </Flex>
        </div>
      </div>
    </div>

    <!-- 新建Agent弹窗 -->
    <Modal v-model="showModal" title="新建Agent" @confirm="handleAddAgent">
      <div class="modal-form">
        <div class="form-item">
          <label>Agent名称</label>
          <input type="text" v-model="newAgent.name" placeholder="输入Agent名称" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea
            v-model="newAgent.description"
            rows="3"
            placeholder="描述这个Agent的功能和用途"
          ></textarea>
        </div>
        <div class="form-item">
          <label>提示词配置</label>
          <textarea
            v-model="newAgent.prompt"
            rows="6"
            placeholder="设置Agent执行任务时的提示词"
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteModal" title="删除确认">
      <div class="delete-confirm">
        <p>确定要删除 {{ selectedAgent?.name }} 吗？此操作不可恢复。</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
        <button class="confirm-btn danger" @click="handleDelete">删除</button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '@/components/modal/index.vue'
import Flex from '@/components/flex/index.vue'

interface Agent {
  id: number | string
  name: string
  description: string
  tools: string[]
  prompt: string
  nextPrompt?: string
}

interface Tool {
  id: string
  name: string
  description: string
  enabled: boolean
}

// API 服务类
class AdminAPI {
  static AGENT_URL = '/api/agents'

  static async _handleResponse(response: Response) {
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

  static async getAllAgents(): Promise<Agent[]> {
    try {
      const response = await fetch(this.AGENT_URL)
      const result = await this._handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('获取Agent列表失败:', error)
      throw error
    }
  }

  static async getAgentById(id: string | number): Promise<Agent> {
    try {
      const response = await fetch(`${this.AGENT_URL}/${id}`)
      const result = await this._handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`获取Agent[${id}]详情失败:`, error)
      throw error
    }
  }

  static async createAgent(agentConfig: Omit<Agent, 'id'>): Promise<Agent> {
    try {
      const response = await fetch(this.AGENT_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(agentConfig)
      })
      const result = await this._handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('创建Agent失败:', error)
      throw error
    }
  }

  static async updateAgent(id: string | number, agentConfig: Agent): Promise<Agent> {
    try {
      const response = await fetch(`${this.AGENT_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(agentConfig)
      })
      const result = await this._handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`更新Agent[${id}]失败:`, error)
      throw error
    }
  }

  static async deleteAgent(id: string | number): Promise<void> {
    try {
      const response = await fetch(`${this.AGENT_URL}/${id}`, {
        method: 'DELETE'
      })
      if (response.status === 400) {
        throw new Error('不能删除默认Agent')
      }
      await this._handleResponse(response)
    } catch (error) {
      console.error(`删除Agent[${id}]失败:`, error)
      throw error
    }
  }

  static async getAvailableTools(): Promise<Tool[]> {
    try {
      const response = await fetch(`${this.AGENT_URL}/tools`)
      const result = await this._handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('获取可用工具列表失败:', error)
      throw error
    }
  }
}

// Agent配置模型类
class AgentConfigModel {
  public agents: Agent[] = []
  public currentAgent: Agent | null = null
  public availableTools: Tool[] = []

  async loadAgents(): Promise<Agent[]> {
    try {
      this.agents = await AdminAPI.getAllAgents()
      return this.agents
    } catch (error) {
      console.error('加载Agent列表失败:', error)
      throw error
    }
  }

  async loadAgentDetails(id: string | number): Promise<Agent> {
    try {
      this.currentAgent = await AdminAPI.getAgentById(id)
      return this.currentAgent
    } catch (error) {
      console.error('加载Agent详情失败:', error)
      throw error
    }
  }

  async loadAvailableTools(): Promise<Tool[]> {
    try {
      this.availableTools = await AdminAPI.getAvailableTools()
      return this.availableTools
    } catch (error) {
      console.error('加载可用工具列表失败:', error)
      throw error
    }
  }

  async saveAgent(agentData: Agent, isImport = false): Promise<Agent> {
    try {
      let result: Agent
      
      if (isImport) {
        const importData = { ...agentData }
        delete (importData as any).id
        result = await AdminAPI.createAgent(importData)
      } else if (agentData.id) {
        result = await AdminAPI.updateAgent(agentData.id, agentData)
      } else {
        result = await AdminAPI.createAgent(agentData)
      }

      // 更新本地数据
      if (agentData.id) {
        const index = this.agents.findIndex(agent => agent.id === agentData.id)
        if (index !== -1) {
          this.agents[index] = result
        }
      } else {
        this.agents.push(result)
      }

      return result
    } catch (error) {
      console.error('保存Agent失败:', error)
      throw error
    }
  }

  async deleteAgent(id: string | number): Promise<void> {
    try {
      await AdminAPI.deleteAgent(id)
      this.agents = this.agents.filter(agent => agent.id !== id)
    } catch (error) {
      console.error('删除Agent失败:', error)
      throw error
    }
  }
}

// 创建配置模型实例
const agentConfigModel = new AgentConfigModel()

// 响应式数据
const agents = reactive<Agent[]>([])
const selectedAgent = ref<Agent | null>(null)
const availableTools = reactive<Tool[]>([])
const showModal = ref(false)
const showDeleteModal = ref(false)
const showToolModal = ref(false)
const loading = ref(false)

const newAgent = reactive<Omit<Agent, 'id' | 'tools'>>({
  name: '',
  description: '',
  prompt: '',
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 加载Agent列表
    const loadedAgents = await agentConfigModel.loadAgents()
    agents.splice(0, agents.length, ...loadedAgents)
    
    // 选中第一个Agent
    if (loadedAgents.length > 0) {
      selectedAgent.value = loadedAgents[0]
      await loadAgentDetails(loadedAgents[0].id)
    }

    // 加载可用工具
    const loadedTools = await agentConfigModel.loadAvailableTools()
    availableTools.splice(0, availableTools.length, ...loadedTools)
  } catch (error) {
    console.error('加载数据失败:', error)
    // 这里可以显示错误提示
  } finally {
    loading.value = false
  }
}

// 加载Agent详情
const loadAgentDetails = async (id: string | number) => {
  try {
    const agent = await agentConfigModel.loadAgentDetails(id)
    selectedAgent.value = agent
  } catch (error) {
    console.error('加载Agent详情失败:', error)
  }
}

// 显示新建Agent弹窗
const showAddAgentModal = () => {
  newAgent.name = ''
  newAgent.description = ''
  newAgent.prompt = ''
  showModal.value = true
}

// 显示删除确认弹窗
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 处理新建Agent
const handleAddAgent = async () => {
  try {
    const agent: Omit<Agent, 'id'> = {
      name: newAgent.name,
      description: newAgent.description,
      prompt: newAgent.prompt,
      tools: []
    }

    const createdAgent = await agentConfigModel.saveAgent(agent as Agent)
    agents.push(createdAgent)
    selectedAgent.value = createdAgent
    showModal.value = false
  } catch (error) {
    console.error('创建Agent失败:', error)
    // 这里可以显示错误提示
  }
}

// 处理添加工具
const handleAddTool = () => {
  showToolModal.value = true
}

// 移除工具
const removeTool = (tool: string) => {
  if (selectedAgent.value) {
    selectedAgent.value.tools = selectedAgent.value.tools.filter(t => t !== tool)
  }
}

// 添加工具到Agent
const addToolToAgent = (toolId: string) => {
  if (selectedAgent.value && !selectedAgent.value.tools.includes(toolId)) {
    selectedAgent.value.tools.push(toolId)
  }
}

// 保存Agent
const handleSave = async () => {
  if (!selectedAgent.value) return

  try {
    await agentConfigModel.saveAgent(selectedAgent.value)
    console.log('Agent保存成功')
    // 这里可以显示成功提示
  } catch (error) {
    console.error('保存Agent失败:', error)
    // 这里可以显示错误提示
  }
}

// 删除Agent
const handleDelete = async () => {
  if (!selectedAgent.value) return

  try {
    await agentConfigModel.deleteAgent(selectedAgent.value.id)
    
    // 从列表中移除
    const index = agents.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.splice(index, 1)
    }

    // 清除选中状态或选择其他Agent
    selectedAgent.value = agents.length > 0 ? agents[0] : null
    showDeleteModal.value = false
  } catch (error) {
    console.error('删除Agent失败:', error)
    // 这里可以显示错误提示
  }
}

// 导入Agent
const handleImport = () => {
  // 创建文件输入
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = (event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = async (e) => {
        try {
          const agentData = JSON.parse(e.target?.result as string)
          await agentConfigModel.saveAgent(agentData, true)
          await loadData() // 重新加载数据
        } catch (error) {
          console.error('导入Agent失败:', error)
        }
      }
      reader.readAsText(file)
    }
  }
  input.click()
}

// 导出Agent
const handleExport = () => {
  if (!selectedAgent.value) return

  const dataStr = JSON.stringify(selectedAgent.value, null, 2)
  const dataBlob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(dataBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = `agent-${selectedAgent.value.name}.json`
  link.click()
  URL.revokeObjectURL(url)
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel-actions {
  display: flex;
  gap: 12px;
}

.agent-layout {
  display: flex;
  gap: 30px;
}

.agent-list {
  width: 300px;
}

.agent-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  &.active {
    border-color: #667eea;
  }
}

.agent-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.agent-name {
  font-weight: 500;
}

.agent-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin-bottom: 12px;
}

.tool-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
  margin-bottom: 8px;
}

.agent-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
}

.form-item {
  margin-bottom: 16px;
  label {
    display: block;
    margin-bottom: 8px;
    color: rgba(255, 255, 255, 0.8);
  }

  input,
  textarea {
    width: 100%;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    transition: all 0.3s;
  }

  input:focus,
  textarea:focus {
    border-color: #667eea;
    outline: none;
  }
}

.tools-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
}

.remove-tool {
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.3s;
  &:hover {
    opacity: 1;
  }
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.3);
  }
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
  &.primary {
    /* background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); */
    color: #ffffff;
    border-color: rgba(102, 126, 234, 0.2);
  }
  &.danger {
    background: rgba(234, 102, 102, 0.1);
    border: 1px solid rgba(234, 102, 102, 0.2);
    color: #ea6666;
    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;

  p {
    color: rgba(255, 255, 255, 0.8);
  }
}

.confirm-btn {
  padding: 8px 16px;
  &.danger {
    background: rgba(255, 255, 255, 0.05);
    /* background: rgba(234, 102, 102, 0.2); */
    border-color: rgba(234, 102, 102, 0.2);
    color: #ea6666;
    border-radius: 6px;

    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}

.cancel-btn {
  padding: 8px 16px;
  /* background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); */
  color: #ffffff;
  border-radius: 6px;
  &:hover {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
}
</style>
