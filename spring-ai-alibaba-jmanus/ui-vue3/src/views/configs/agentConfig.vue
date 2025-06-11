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
        <button class="action-btn" @click="handleImport">
          <Icon icon="carbon:upload" />
          导入
        </button>
        <button class="action-btn" @click="handleExport" :disabled="!selectedAgent">
          <Icon icon="carbon:download" />
          导出
        </button>
      </div>
    </div>

    <div class="agent-layout">
      <!-- Agent列表 -->
      <div class="agent-list">
        <div class="list-header">
          <h3>已配置的Agent</h3>
          <span class="agent-count">({{ agents.length }})</span>
        </div>
        
        <div class="agents-container" v-if="!loading">
          <div
            v-for="agent in agents"
            :key="agent.id"
            class="agent-card"
            :class="{ active: selectedAgent?.id === agent.id }"
            @click="selectAgent(agent)"
          >
            <div class="agent-card-header">
              <span class="agent-name">{{ agent.name }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p class="agent-desc">{{ agent.description }}</p>
            <div class="agent-tools" v-if="agent.availableTools && Array.isArray(agent.availableTools) && agent.availableTools.length > 0">
              <span v-for="tool in agent.availableTools.slice(0, 3)" :key="tool" class="tool-tag">
                {{ getToolDisplayName(tool) }}
              </span>
              <span v-if="agent.availableTools.length > 3" class="tool-more">
                +{{ agent.availableTools.length - 3 }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="loading-icon" />
          加载中...
        </div>

        <div v-if="!loading && agents.length === 0" class="empty-state">
          <Icon icon="carbon:bot" class="empty-icon" />
          <p>暂无Agent配置</p>
        </div>

        <button class="add-btn" @click="showAddAgentModal">
          <Icon icon="carbon:add" />
          新建Agent
        </button>
      </div>

      <!-- Agent详情 -->
      <div class="agent-detail" v-if="selectedAgent">
        <div class="detail-header">
          <h3>{{ selectedAgent.name }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              保存
            </button>
            <button class="action-btn danger" @click="showDeleteConfirm">
              <Icon icon="carbon:trash-can" />
              删除
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>Agent名称 <span class="required">*</span></label>
          <input 
            type="text" 
            v-model="selectedAgent.name" 
            placeholder="输入Agent名称"
            required
          />
        </div>
        
        <div class="form-item">
          <label>描述 <span class="required">*</span></label>
          <textarea 
            v-model="selectedAgent.description" 
            rows="3"
            placeholder="描述这个Agent的功能和用途"
            required
          ></textarea>
        </div>
        
        <div class="form-item">
          <label>Agent提示词（人设，要求，以及下一步动作的指导）</label>
          <textarea
            v-model="selectedAgent.nextStepPrompt"
            rows="8"
            placeholder="设置Agent的人设、要求以及下一步动作的指导..."
          ></textarea>
        </div>

        <!-- 工具分配区域 -->
        <div class="tools-section">
          <h4>工具配置</h4>
          
          <!-- 已分配的工具 -->
          <div class="assigned-tools">
            <div class="section-header">
              <span>已分配工具 ({{ (selectedAgent.availableTools || []).length }})</span>
              <button class="action-btn small" @click="showToolSelectionModal" v-if="availableTools.length > 0">
                <Icon icon="carbon:add" />
                添加/删除工具
              </button>
            </div>
            
            <div class="tools-grid">
              <div v-for="toolId in (selectedAgent.availableTools || [])" :key="toolId" class="tool-item assigned">
                <div class="tool-info">
                  <span class="tool-name">{{ getToolDisplayName(toolId) }}</span>
                  <span class="tool-desc">{{ getToolDescription(toolId) }}</span>
                </div>
              </div>
              
              <div v-if="!selectedAgent.availableTools || selectedAgent.availableTools.length === 0" class="no-tools">
                <Icon icon="carbon:tool-box" />
                <span>暂无分配的工具</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>请选择一个Agent进行配置</p>
      </div>
    </div>

    <!-- 新建Agent弹窗 -->
    <Modal v-model="showModal" title="新建Agent" @confirm="handleAddAgent">
      <div class="modal-form">
        <div class="form-item">
          <label>Agent名称 <span class="required">*</span></label>
          <input 
            type="text" 
            v-model="newAgent.name" 
            placeholder="输入Agent名称"
            required 
          />
        </div>
        <div class="form-item">
          <label>描述 <span class="required">*</span></label>
          <textarea
            v-model="newAgent.description"
            rows="3"
            placeholder="描述这个Agent的功能和用途"
            required
          ></textarea>
        </div>
        <div class="form-item">
          <label>Agent提示词（人设，要求，以及下一步动作的指导）</label>
          <textarea
            v-model="newAgent.nextStepPrompt"
            rows="8"
            placeholder="设置Agent的人设、要求以及下一步动作的指导..."
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- 工具选择弹窗 -->
    <ToolSelectionModal
      v-model="showToolModal"
      :tools="availableTools"
      :selected-tool-ids="selectedAgent?.availableTools || []"
      @confirm="handleToolSelectionConfirm"
    />

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteModal" title="删除确认">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>确定要删除 <strong>{{ selectedAgent?.name }}</strong> 吗？</p>
        <p class="warning-text">此操作不可恢复。</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
        <button class="confirm-btn danger" @click="handleDelete">删除</button>
      </template>
    </Modal>

    <!-- 错误提示 -->
    <div v-if="error" class="error-toast" @click="error = ''">
      <Icon icon="carbon:error" />
      {{ error }}
    </div>

    <!-- 成功提示 -->
    <div v-if="success" class="success-toast" @click="success = ''">
      <Icon icon="carbon:checkmark" />
      {{ success }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '@/components/modal/index.vue'
import ToolSelectionModal from '@/components/tool-selection-modal/index.vue'
import { AgentApiService, type Agent, type Tool } from '@/api/agent-api-service'

// 响应式数据
const loading = ref(false)
const error = ref('')
const success = ref('')
const agents = reactive<Agent[]>([])
const selectedAgent = ref<Agent | null>(null)
const availableTools = reactive<Tool[]>([])
const showModal = ref(false)
const showDeleteModal = ref(false)
const showToolModal = ref(false)

// 新建Agent表单数据
const newAgent = reactive<Omit<Agent, 'id' | 'availableTools'>>({
  name: '',
  description: '',
  nextStepPrompt: ''
})

// 计算属性
const unassignedTools = computed(() => {
  if (!selectedAgent.value || !selectedAgent.value.availableTools || !Array.isArray(selectedAgent.value.availableTools)) {
    return availableTools
  }
  return availableTools.filter(tool => !selectedAgent.value!.availableTools.includes(tool.key))
})

// 工具显示名称获取
const getToolDisplayName = (toolId: string): string => {
  const tool = availableTools.find(t => t.key === toolId)
  return tool ? tool.name : toolId
}

// 工具描述获取
const getToolDescription = (toolId: string): string => {
  const tool = availableTools.find(t => t.key === toolId)
  return tool ? tool.description : ''
}

// 消息提示
const showMessage = (msg: string, type: 'success' | 'error') => {
  if (type === 'success') {
    success.value = msg
    setTimeout(() => { success.value = '' }, 3000)
  } else {
    error.value = msg
    setTimeout(() => { error.value = '' }, 5000)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 并行加载Agent列表和可用工具
    const [loadedAgents, loadedTools] = await Promise.all([
      AgentApiService.getAllAgents(),
      AgentApiService.getAvailableTools()
    ])
    
    // 确保每个agent都有availableTools数组
    const normalizedAgents = loadedAgents.map(agent => ({
      ...agent,
      availableTools: Array.isArray(agent.availableTools) ? agent.availableTools : []
    }))
    
    agents.splice(0, agents.length, ...normalizedAgents)
    availableTools.splice(0, availableTools.length, ...loadedTools)
    
    // 选中第一个Agent
    if (normalizedAgents.length > 0) {
      await selectAgent(normalizedAgents[0])
    }
  } catch (err: any) {
    console.error('加载数据失败:', err)
    showMessage('加载数据失败: ' + err.message, 'error')
    
    // 提供演示数据作为后备
    const demoTools = [
      {
        key: 'search-web',
        name: '网络搜索',
        description: '在互联网上搜索信息',
        enabled: true,
        serviceGroup: '搜索服务'
      },
      {
        key: 'search-local',
        name: '本地搜索',
        description: '在本地文件中搜索内容',
        enabled: true,
        serviceGroup: '搜索服务'
      },
      {
        key: 'file-read',
        name: '读取文件',
        description: '读取本地或远程文件内容',
        enabled: true,
        serviceGroup: '文件服务'
      },
      {
        key: 'file-write',
        name: '写入文件',
        description: '创建或修改文件内容',
        enabled: true,
        serviceGroup: '文件服务'
      },
      {
        key: 'file-delete',
        name: '删除文件',
        description: '删除指定的文件',
        enabled: false,
        serviceGroup: '文件服务'
      },
      {
        key: 'calculator',
        name: '计算器',
        description: '执行数学计算',
        enabled: true,
        serviceGroup: '计算服务'
      },
      {
        key: 'code-execute',
        name: '代码执行',
        description: '执行Python或JavaScript代码',
        enabled: true,
        serviceGroup: '计算服务'
      },
      {
        key: 'weather',
        name: '天气查询',
        description: '获取指定地区的天气信息',
        enabled: true,
        serviceGroup: '信息服务'
      },
      {
        key: 'currency',
        name: '汇率查询',
        description: '查询货币汇率信息',
        enabled: true,
        serviceGroup: '信息服务'
      },
      {
        key: 'email',
        name: '发送邮件',
        description: '发送电子邮件',
        enabled: false,
        serviceGroup: '通信服务'
      },
      {
        key: 'sms',
        name: '发送短信',
        description: '发送短信消息',
        enabled: false,
        serviceGroup: '通信服务'
      }
    ]
    
    const demoAgents = [
      {
        id: 'demo-1',
        name: '通用助手',
        description: '一个能够处理各种任务的智能助手',
        nextStepPrompt: 'You are a helpful assistant that can answer questions and help with various tasks. What would you like me to help you with next?',
        availableTools: ['search-web', 'calculator', 'weather']
      },
      {
        id: 'demo-2',
        name: '数据分析师',
        description: '专门用于数据分析和可视化的Agent',
        nextStepPrompt: 'You are a data analyst assistant specialized in analyzing data and creating visualizations. Please provide the data you would like me to analyze.',
        availableTools: ['file-read', 'file-write', 'calculator', 'code-execute']
      }
    ]
    availableTools.splice(0, availableTools.length, ...demoTools)
    agents.splice(0, agents.length, ...demoAgents)
    
    if (demoAgents.length > 0) {
      selectedAgent.value = demoAgents[0]
    }
  } finally {
    loading.value = false
  }
}

// 选择Agent
const selectAgent = async (agent: Agent) => {
  if (!agent) return
  
  try {
    // 加载详细信息
    const detailedAgent = await AgentApiService.getAgentById(agent.id)
    // 确保availableTools是数组
    selectedAgent.value = {
      ...detailedAgent,
      availableTools: Array.isArray(detailedAgent.availableTools) ? detailedAgent.availableTools : []
    }
  } catch (err: any) {
    console.error('加载Agent详情失败:', err)
    showMessage('加载Agent详情失败: ' + err.message, 'error')
    // 使用基本信息作为后备
    selectedAgent.value = {
      ...agent,
      availableTools: Array.isArray(agent.availableTools) ? agent.availableTools : []
    }
  }
}

// 显示新建Agent弹窗
const showAddAgentModal = () => {
  newAgent.name = ''
  newAgent.description = ''
  newAgent.nextStepPrompt = ''
  showModal.value = true
}

// 创建新Agent
const handleAddAgent = async () => {
  if (!newAgent.name.trim() || !newAgent.description.trim()) {
    showMessage('请填写必要的字段', 'error')
    return
  }

  try {
    const agentData: Omit<Agent, 'id'> = {
      name: newAgent.name.trim(),
      description: newAgent.description.trim(),
      nextStepPrompt: newAgent.nextStepPrompt?.trim() || '',
      availableTools: []
    }

    const createdAgent = await AgentApiService.createAgent(agentData)
    agents.push(createdAgent)
    selectedAgent.value = createdAgent
    showModal.value = false
    showMessage('Agent创建成功', 'success')
  } catch (err: any) {
    showMessage('创建Agent失败: ' + err.message, 'error')
  }
}

// 显示工具选择弹窗
const showToolSelectionModal = () => {
  showToolModal.value = true
}

// 处理工具选择确认
const handleToolSelectionConfirm = (selectedToolIds: string[]) => {
  if (selectedAgent.value) {
    selectedAgent.value.availableTools = [...selectedToolIds]
  }
}

// 添加工具
const addTool = (toolId: string) => {
  if (selectedAgent.value) {
    if (!selectedAgent.value.availableTools) {
      selectedAgent.value.availableTools = []
    }
    if (!selectedAgent.value.availableTools.includes(toolId)) {
      selectedAgent.value.availableTools.push(toolId)
    }
  }
}



// 保存Agent
const handleSave = async () => {
  if (!selectedAgent.value) return

  if (!selectedAgent.value.name.trim() || !selectedAgent.value.description.trim()) {
    showMessage('请填写必要的字段', 'error')
    return
  }

  try {
    const savedAgent = await AgentApiService.updateAgent(selectedAgent.value.id, selectedAgent.value)
    
    // 更新本地列表中的数据
    const index = agents.findIndex(a => a.id === savedAgent.id)
    if (index !== -1) {
      agents[index] = savedAgent
    }
    
    selectedAgent.value = savedAgent
    showMessage('Agent保存成功', 'success')
  } catch (err: any) {
    showMessage('保存Agent失败: ' + err.message, 'error')
  }
}

// 显示删除确认
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 删除Agent
const handleDelete = async () => {
  if (!selectedAgent.value) return

  try {
    await AgentApiService.deleteAgent(selectedAgent.value.id)
    
    // 从列表中移除
    const index = agents.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.splice(index, 1)
    }

    // 选择其他Agent或清除选中状态
    selectedAgent.value = agents.length > 0 ? agents[0] : null
    showDeleteModal.value = false
    showMessage('Agent删除成功', 'success')
  } catch (err: any) {
    showMessage('删除Agent失败: ' + err.message, 'error')
  }
}

// 导入Agent
const handleImport = () => {
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
          // 基本验证
          if (!agentData.name || !agentData.description) {
            throw new Error('Agent配置格式不正确：缺少必要字段')
          }
          
          // 移除id字段，让后端分配新的id
          const { id, ...importData } = agentData
          const savedAgent = await AgentApiService.createAgent(importData)
          agents.push(savedAgent)
          selectedAgent.value = savedAgent
          showMessage('Agent导入成功', 'success')
        } catch (err: any) {
          showMessage('导入Agent失败: ' + err.message, 'error')
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

  try {
    const jsonString = JSON.stringify(selectedAgent.value, null, 2)
    const dataBlob = new Blob([jsonString], { type: 'application/json' })
    const url = URL.createObjectURL(dataBlob)
    const link = document.createElement('a')
    link.href = url
    link.download = `agent-${selectedAgent.value.name}-${new Date().toISOString().split('T')[0]}.json`
    link.click()
    URL.revokeObjectURL(url)
    showMessage('Agent导出成功', 'success')
  } catch (err: any) {
    showMessage('导出Agent失败: ' + err.message, 'error')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.config-panel {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.panel-actions {
  display: flex;
  gap: 12px;
}

.agent-layout {
  display: flex;
  gap: 30px;
  flex: 1;
  min-height: 0;
}

.agent-list {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
}

.agent-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.agents-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.6);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.6);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.4;
}

.empty-tip {
  font-size: 14px;
  margin-top: 8px;
}

.agent-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
  }

  &.active {
    border-color: #667eea;
    background: rgba(102, 126, 234, 0.1);
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
  font-size: 16px;
}

.agent-desc {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 12px;
}

.agent-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tool-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: #a8b3ff;
}

.tool-more {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  padding: 4px 8px;
}

.no-tools-indicator {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  font-style: italic;
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.3);
    color: #fff;
  }
}

.agent-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  overflow-y: auto;
}

.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.3;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.detail-header h3 {
  margin: 0;
  font-size: 20px;
}

.detail-actions {
  display: flex;
  gap: 12px;
}

.form-section {
  margin-bottom: 32px;
}

.form-item {
  margin-bottom: 20px;
  
  label {
    display: block;
    margin-bottom: 8px;
    color: rgba(255, 255, 255, 0.9);
    font-weight: 500;
  }

  input,
  textarea {
    width: 100%;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    color: #fff;
    font-size: 14px;
    transition: all 0.3s ease;
    
    &:focus {
      border-color: #667eea;
      outline: none;
      background: rgba(255, 255, 255, 0.08);
    }
    
    &::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }
  }
  
  textarea {
    resize: vertical;
    min-height: 80px;
    line-height: 1.5;
  }
}

.required {
  color: #ff6b6b;
}

.tools-section {
  h4 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  span {
    font-weight: 500;
    color: rgba(255, 255, 255, 0.8);
  }
}

.tools-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  transition: all 0.3s ease;
  
  &.assigned {
    border-color: rgba(102, 126, 234, 0.3);
    background: rgba(102, 126, 234, 0.1);
  }
}

.tool-info {
  flex: 1;
  
  .tool-name {
    display: block;
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .tool-desc {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.3;
  }
}

.no-tools {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  &.primary {
    background: rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.3);
    color: #a8b3ff;
    
    &:hover:not(:disabled) {
      background: rgba(102, 126, 234, 0.3);
    }
  }
  
  &.danger {
    background: rgba(234, 102, 102, 0.1);
    border-color: rgba(234, 102, 102, 0.2);
    color: #ff8a8a;
    
    &:hover:not(:disabled) {
      background: rgba(234, 102, 102, 0.2);
    }
  }
  
  &.small {
    padding: 6px 12px;
    font-size: 12px;
  }
}

/* 弹窗样式 */
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
    margin: 8px 0;
  }
  
  .warning-text {
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;
  }
}

.warning-icon {
  font-size: 48px;
  color: #ffa726;
  margin-bottom: 16px;
}

.confirm-btn, .cancel-btn {
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &.danger {
    background: rgba(234, 102, 102, 0.2);
    border: 1px solid rgba(234, 102, 102, 0.3);
    color: #ff8a8a;
    
    &:hover {
      background: rgba(234, 102, 102, 0.3);
    }
  }
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

/* 提示消息 */
.error-toast, .success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.error-toast {
  background: rgba(234, 102, 102, 0.9);
  border: 1px solid rgba(234, 102, 102, 0.5);
}

.success-toast {
  background: rgba(102, 234, 102, 0.9);
  border: 1px solid rgba(102, 234, 102, 0.5);
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
