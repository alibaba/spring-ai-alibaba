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
      <h2>Tools/MCP配置</h2>
      <div class="panel-actions">
        <button class="action-btn" @click="showToolModal()">
          <Icon icon="carbon:add" />
          新建工具
        </button>
      </div>
    </div>

    <div class="tools-grid">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>正在加载 MCP 服务器...</p>
      </div>
      <div v-else-if="tools.length === 0" class="empty-container">
        <p>暂无 MCP 服务器配置</p>
        <button class="action-btn" @click="showToolModal()">
          <Icon icon="carbon:add" />
          添加第一个 MCP 服务器
        </button>
      </div>
      <div v-else v-for="tool in tools" :key="tool.id" class="tool-card">
        <div class="tool-header">
          <span class="tool-name">{{ tool.name }}</span>
          <div class="tool-actions">
            <Switch
              update:switchValue="handleSwitchChange(tool, $event)"
              :enabled="tool.enabled"
              :label="tool.enabled ? '已启用' : '已禁用'"
              @update:switchValue="handleSwitchChange(tool, $event)"
            />
            <button 
              class="edit-btn disabled-btn" 
              @click="showEditDisabledMessage()" 
              title="当前版本不支持编辑，请删除后重新添加"
            >
              <Icon icon="carbon:edit" />
              编辑
            </button>
          </div>
        </div>
        <p class="tool-desc">{{ tool.description }}</p>
      </div>
    </div>

    <!-- 工具配置弹窗 -->
    <Modal v-model="showModal" :title="isEdit ? '编辑工具' : '新建工具'" @confirm="handleSaveTool">
      <div class="modal-form">
        <div class="form-item">
          <label>工具名称</label>
          <input type="text" v-model="editingTool.name" placeholder="输入工具名称" />
        </div>

        <div class="form-item">
          <label>描述</label>
          <textarea
            v-model="editingTool.description"
            rows="3"
            placeholder="描述这个工具的功能和用途"
          ></textarea>
        </div>
        <div class="form-item">
          <label>连接类型</label>
          <Flex gap="12px" align="baseline">
            <Flex align="baseline" gap="4px">
              <input
                type="radio"
                id="studio"
                name="connectionType"
                value="studio"
                class="radio-input"
              />
              <label for="studio">Studio</label>
            </Flex>
            <Flex align="baseline" gap="4px">
              <input type="radio" id="sse" name="connectionType" value="sse" />
              <label for="sse">SSE</label>
            </Flex>
          </Flex>
        </div>

        <div class="form-item">
          <label>MCP配置</label>
          <textarea v-model="editingTool.connectionConfig" rows="6" placeholder="输入MCP配置内容"></textarea>
        </div>
        <div class="form-item">
          <Switch
            style="font-size: 12px"
            :enabled="editingTool.enabled"
            :label="editingTool.enabled ? '已启用' : '已禁用'"
          />
        </div>
      </div>

      <template #footer>
        <Flex justify="space-between" gap="8px" class="footer-buttons">
          <button v-if="isEdit" class="footer-btn delete-btn" @click="showDeleteConfirm = true">
            <Icon icon="carbon:trash-can" />
            删除
          </button>
          <Flex gap="8px">
            <button class="footer-btn cancel-btn" @click="showModal = false" :disabled="saving">取消</button>
            <button class="footer-btn save-btn" @click="handleSaveTool" :disabled="saving">
              <Icon v-if="saving" icon="carbon:renew" class="spinning" />
              <Icon v-else icon="carbon:save" />
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </Flex>
        </Flex>
        <!-- 编辑模式下显示删除按钮 -->
      </template>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteConfirm" title="删除确认">
      <div class="delete-confirm">
        <p>确定要删除 {{ editingTool.name }} 吗？此操作不可恢复。</p>
      </div>
      <template #footer>
        <Flex gap="8px" justify="flex-end">
          <button class="footer-btn cancel-btn" @click="showDeleteConfirm = false" :disabled="saving">取消</button>
          <button class="footer-btn delete-btn" @click="handleDeleteTool" :disabled="saving">
            <Icon v-if="saving" icon="carbon:renew" class="spinning" />
            <Icon v-else icon="carbon:trash-can" />
            {{ saving ? '删除中...' : '删除' }}
          </button>
        </Flex>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '@/components/modal/index.vue'
import Flex from '@/components/flex/index.vue'
import Switch from '@/components/switch/index.vue'
import { McpApiService, type McpServer, type McpServerRequest } from '@/api/mcp-api-service'

interface Tool extends McpServer {
  name: string
  description: string
  enabled: boolean
}

// 响应式数据
const tools = reactive<Tool[]>([])
const showModal = ref(false)
const showDeleteConfirm = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)

const defaultTool: Tool = {
  id: 0,
  name: '',
  description: '',
  connectionType: 'STUDIO',
  mcpServerName: '',
  connectionConfig: '',
  enabled: true,
}

const editingTool = reactive<Tool>({ ...defaultTool })

// 加载 MCP 服务器列表
const loadMcpServers = async () => {
  loading.value = true
  try {
    console.log('开始加载 MCP 服务器列表...')
    const mcpServers = await McpApiService.getAllMcpServers()
    
    // 转换为 Tool 格式
    const convertedTools: Tool[] = mcpServers.map(server => ({
      ...server,
      name: server.mcpServerName,
      description: `${server.connectionType} 连接类型的 MCP 服务器`,
      enabled: true, // 默认启用，可以根据实际需求调整
    }))
    
    tools.splice(0, tools.length, ...convertedTools)
    console.log('MCP 服务器列表加载完成:', tools.length, '个服务器')
  } catch (error) {
    console.error('加载 MCP 服务器列表失败:', error)
    showMessage('加载 MCP 服务器列表失败: ' + (error as Error).message, 'error')
  } finally {
    loading.value = false
  }
}

// 显示工具配置弹窗
const showToolModal = (tool?: Tool) => {
  isEdit.value = !!tool
  if (tool) {
    Object.assign(editingTool, tool)
  } else {
    Object.assign(editingTool, { ...defaultTool })
  }
  showModal.value = true
}

// 保存工具
const handleSaveTool = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      // 编辑模式暂不支持，因为遗留 API 只支持添加和删除
      showMessage('当前版本不支持编辑 MCP 服务器，请删除后重新添加', 'info')
      return
    } else {
      // 创建新 MCP 服务器
      const mcpConfig: McpServerRequest = {
        connectionType: editingTool.connectionType,
        configJson: editingTool.connectionConfig
      }
      
      const result = await McpApiService.addMcpServer(mcpConfig)
      
      if (result.success) {
        showMessage(result.message, 'success')
        // 重新加载列表
        await loadMcpServers()
      } else {
        showMessage(result.message, 'error')
      }
    }
    showModal.value = false
  } catch (error) {
    console.error('保存 MCP 服务器失败:', error)
    showMessage('保存失败: ' + (error as Error).message, 'error')
  } finally {
    saving.value = false
  }
}

// 删除工具
const handleDeleteTool = async () => {
  saving.value = true
  try {
    const result = await McpApiService.removeMcpServer(editingTool.id)
    
    if (result.success) {
      showMessage(result.message, 'success')
      // 重新加载列表
      await loadMcpServers()
    } else {
      showMessage(result.message, 'error')
    }
    
    showDeleteConfirm.value = false
    showModal.value = false
  } catch (error) {
    console.error('删除 MCP 服务器失败:', error)
    showMessage('删除失败: ' + (error as Error).message, 'error')
  } finally {
    saving.value = false
  }
}

// 显示编辑功能禁用消息
const showEditDisabledMessage = () => {
  showMessage('当前版本不支持编辑 MCP 服务器，请删除后重新添加', 'info')
}

// 开关切换（本地状态切换，不影响后端）
const handleSwitchChange = (tool: Tool, value: boolean) => {
  tool.enabled = value
  // 注意：这里只是本地 UI 状态，实际的启用/禁用需要后端支持
}

// 显示消息提示
const showMessage = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  // 创建一个临时的消息提示元素
  const messageDiv = document.createElement('div')
  messageDiv.className = `message-toast message-${type}`
  messageDiv.textContent = message
  
  // 设置样式
  Object.assign(messageDiv.style, {
    position: 'fixed',
    top: '20px',
    right: '20px',
    padding: '12px 20px',
    borderRadius: '6px',
    color: 'white',
    fontSize: '14px',
    zIndex: '9999',
    maxWidth: '400px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    animation: 'slideIn 0.3s ease-out',
    backgroundColor: type === 'success' ? '#52c41a' : 
                     type === 'error' ? '#ff4d4f' : '#1890ff'
  })
  
  document.body.appendChild(messageDiv)
  
  // 3秒后自动移除
  setTimeout(() => {
    if (messageDiv.parentNode) {
      messageDiv.style.animation = 'slideOut 0.3s ease-in'
      setTimeout(() => {
        if (messageDiv.parentNode) {
          document.body.removeChild(messageDiv)
        }
      }, 300)
    }
  }, 3000)
  
  // 同时在控制台输出
  console.log(`${type.toUpperCase()}: ${message}`)
}

// 格式化配置显示
const formatConfig = (config: string): string => {
  if (!config) return ''
  
  // 如果配置信息太长，截断显示
  if (config.length > 100) {
    return config.substring(0, 100) + '...'
  }
  
  return config
}

// 组件挂载时加载数据
onMounted(() => {
  loadMcpServers()
})
</script>

<style scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.loading-container,
.empty-container {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.tool-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tool-name {
  font-weight: 500;
  font-size: 16px;
}

.tool-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tool-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.tool-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.disabled-btn {
  opacity: 0.5;
  cursor: not-allowed !important;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05) !important;
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
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  margin-bottom: 16px;
  & label {
    display: block;
    margin-bottom: 8px;
    color: rgba(255, 255, 255, 0.8);
  }

  & input[type='text'],
  & textarea {
    width: 100%;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    transition: all 0.3s;
  }

  & input[type='text']:focus,
  & textarea:focus {
    border-color: #667eea;
    outline: none;
  }
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;
}

.delete-confirm p {
  color: rgba(255, 255, 255, 0.8);
}

.footer-buttons {
  width: 100%;
  .footer-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px;
    cursor: pointer;
    transition: all 0.3s;
    border-radius: 6px;
  }

  .cancel-btn {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    cursor: pointer;
  }

  .save-btn {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    cursor: pointer;
  }

  .delete-btn {
    background: rgba(234, 102, 102, 0.1);
    border: 1px solid rgba(234, 102, 102, 0.2);
    color: #ea6666;
    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}

/* 消息提示动画 */
@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(100%);
  }
}

/* 旋转动画 */
.spinning {
  animation: spin 1s linear infinite;
}

/* 禁用状态 */
button:disabled {
  opacity: 0.6;
  cursor: not-allowed !important;
}
</style>
