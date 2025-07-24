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
  <ConfigPanel>
    <template #title>
      <h2>{{ t('config.mcpConfig.title') }}</h2>
    </template>

    <template #actions>
      <button class="action-btn" @click="handleImport">
        <Icon icon="carbon:upload" />
        {{ t('config.mcpConfig.import') }}
      </button>
      <button class="action-btn" @click="handleExport" :disabled="!selectedServer">
        <Icon icon="carbon:download" />
        {{ t('config.mcpConfig.export') }}
      </button>
    </template>

    <div class="mcp-layout">
      <!-- MCP Server List -->
      <div class="server-list">
        <div class="list-header">
          <h3>{{ t('config.mcpConfig.serverList') }}</h3>
          <span class="server-count">({{ mcpServers.length }})</span>
        </div>

        <div class="search-box">
          <input
              v-model="searchQuery"
              type="text"
              :placeholder="t('config.mcpSearch')"
              class="search-input"
          />
          <Icon icon="carbon:search" class="search-icon" />
        </div>

        <div class="servers-container" v-if="!loading">
          <div
            v-for="server in filteredMcpServers"
            :key="server.id"
            class="server-card"
            :class="{ active: selectedServer?.id === server.id }"
            @click="selectServer(server)"
          >
            <div class="server-card-header">
              <span class="server-name">{{ server.mcpServerName }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <div class="server-connection-type">
              <span class="connection-type-badge" :class="server.connectionType.toLowerCase()">
                {{ server.connectionType }}
              </span>
            </div>
            <div class="server-config-preview">
              <pre class="config-preview" v-html="formatJsonForDisplay(server.connectionConfig)"></pre>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="loading-icon" />
          {{ t('common.loading') }}
        </div>

        <div v-if="!loading && filteredMcpServers.length === 0" class="empty-state">
          <Icon icon="carbon:bot" class="empty-icon" />
          <p>{{ searchQuery ? t('config.notFound') : t('config.mcpConfig.noServers') }}</p>
        </div>

        <button class="add-btn" @click="showAddServerModal">
          <Icon icon="carbon:add" />
          {{ t('config.mcpConfig.addMcpServer') }}
        </button>
      </div>

      <!-- Server details -->
      <div class="server-detail" v-if="selectedServer">
        <div class="detail-header">
          <h3>{{ selectedServer.mcpServerName }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              {{ t('common.save') }}
            </button>
            <button class="action-btn danger" @click="showDeleteConfirm">
              <Icon icon="carbon:trash-can" />
              {{ t('common.delete') }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>{{ t('config.mcpConfig.connectionType') }} <span class="required">*</span></label>
          <CustomSelect
            v-model="selectedServer.connectionType"
            :options="connectionTypes.map(type => ({ id: type, name: type }))"
            :placeholder="t('config.mcpConfig.connectionTypePlaceholder')"
            :dropdown-title="t('config.mcpConfig.connectionTypePlaceholder')"
            icon="carbon:connection"
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.mcpConfig.configJsonLabel') }} <span class="required">*</span></label>
          <div class="json-editor-container">
            <div class="json-editor-header">
              <span class="json-status" :class="{ 'valid': isJsonValid, 'invalid': !isJsonValid && selectedServer.connectionConfig.trim() }">
                {{ getJsonStatusText() }}
              </span>
              <button 
                v-if="selectedServer.connectionConfig.trim()" 
                @click="formatJson" 
                class="format-btn"
                :disabled="!isJsonValid"
              >
                <Icon icon="carbon:settings" />
                {{ t('config.mcpConfig.formatJson') }}
              </button>
            </div>
            <div class="json-editor-wrapper">
              <MonacoEditor
                  v-model="selectedServer.connectionConfig"
                  :placeholder="t('config.mcpConfig.configJsonPlaceholder')"
                  @change="validateJson"
                  class="json-editor"
                  language="json"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.mcpConfig.selectServerHint') }}</p>
      </div>
    </div>

    <!-- New Server modal -->
    <Modal v-model="showModal" :title="t('config.mcpConfig.newServer')" @confirm="handleAddServer">
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.mcpConfig.connectionType') }} <span class="required">*</span></label>
          <CustomSelect
            v-model="newServer.connectionType"
            :options="connectionTypes.map(type => ({ id: type, name: type }))"
            :placeholder="t('config.mcpConfig.connectionTypePlaceholder')"
            :dropdown-title="t('config.mcpConfig.connectionTypePlaceholder')"
            icon="carbon:connection"
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.mcpConfig.configJsonLabel') }} <span class="required">*</span></label>
          <div class="json-editor-container">
            <div class="json-editor-header">
              <span class="json-status" :class="{ 'valid': isJsonValid, 'invalid': !isJsonValid && newServer.configJson.trim() }">
                {{ getJsonStatusText() }}
              </span>
              <button 
                v-if="newServer.configJson.trim()" 
                @click="formatJson" 
                class="format-btn"
                :disabled="!isJsonValid"
              >
                <Icon icon="carbon:settings" />
                {{ t('config.mcpConfig.formatJson') }}
              </button>
            </div>
            <div class="json-editor-wrapper">
              <MonacoEditor
                  v-model="newServer.configJson"
                  :placeholder="t('config.mcpConfig.configJsonPlaceholder')"
                  @change="validateJson"
                  class="json-editor"
                  language="json"
              />
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <!-- Delete confirmation modal -->
    <Modal v-model="showDeleteModal" :title="t('config.mcpConfig.deleteConfirmTitle')" @confirm="handleDeleteServer">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>{{ t('config.mcpConfig.deleteConfirmMessage') }}</p>
        <p class="warning-text">{{ t('config.mcpConfig.deleteWarning') }}</p>
      </div>
    </Modal>

    <!-- Message Toast -->
    <transition name="message-fade">
      <div v-if="message.show" :class="['message-toast', message.type]">
        {{ message.text }}
      </div>
    </transition>
  </ConfigPanel>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { McpApiService, type McpServer, type McpServerRequest } from '@/api/mcp-api-service'
import MonacoEditor from '@/components/MonacoEditor.vue'
import ConfigPanel from './components/configPanel.vue'
import CustomSelect from '@/components/select/index.vue'
import Modal from '@/components/modal/index.vue'
import { Icon } from '@iconify/vue'

// Internationalization
const { t } = useI18n()

// Reactive data
const loading = ref(false)
const mcpServers = ref<McpServer[]>([])
const searchQuery = ref('')

// Selected MCP Server
const selectedServer = ref<McpServer | null>(null)

// Show modals
const showModal = ref(false)
const showDeleteModal = ref(false)

// Add MCP Server Form
const newServer = reactive<McpServerRequest & { configJson: string }>({
  connectionType: 'STUDIO',
  configJson: ''
})

// Connection types for select options
const connectionTypes = ['STUDIO', 'SSE', 'STREAMING']

// JSON validation and formatting
const isJsonValid = ref(true)
const validationErrors = ref<string[]>([])

// Message Toast
const message = reactive({
  show: false,
  text: '',
  type: 'success' as 'success' | 'error' | 'info'
})

// Computed property: Whether it can be submitted
const canSubmit = computed(() => {
  return newServer.configJson.trim().length > 0
})

// Computed property: Filtered MCP servers
const filteredMcpServers = computed(() => {
  if (!searchQuery.value.trim()) {
    return mcpServers.value
  }

  const query = searchQuery.value.toLowerCase()
  return mcpServers.value.filter(server =>
      server.mcpServerName.toLowerCase().includes(query) ||
      server.connectionType.toLowerCase().includes(query) ||
      server.connectionConfig.toLowerCase().includes(query)
  )
})

// Select a server
const selectServer = (server: McpServer) => {
  selectedServer.value = { ...server }
}

// Show add server modal
const showAddServerModal = () => {
  selectedServer.value = null // Clear selected server
  newServer.connectionType = 'STUDIO'
  newServer.configJson = ''
  isJsonValid.value = true
  validationErrors.value = []
  showModal.value = true
}

// Handle add server from modal
const handleAddServer = async () => {
  if (!canSubmit.value) {
    showMessage(t('config.mcpConfig.configRequired'), 'error')
    return
  }

  // Validate JSON format
  let parsedConfig
  try {
    parsedConfig = JSON.parse(newServer.configJson)
  } catch {
    showMessage(t('config.mcpConfig.invalidJson'), 'error')
    return
  }

  try {
    loading.value = true

    // 统一处理配置中的url字段
    const normalizedConfig = normalizeMcpConfig(parsedConfig)
    
    const requestData: McpServerRequest = {
      connectionType: newServer.connectionType,
      configJson: JSON.stringify(normalizedConfig)
    }

    const result = await McpApiService.addMcpServer(requestData)

    if (result.success) {
      showMessage(t('config.mcpConfig.addSuccess'))
      resetForm()
      showModal.value = false
      await loadMcpServers() // Reload the list
    } else {
      // 显示详细的错误信息
      showMessage(result.message, 'error')
    }
  } catch (error) {
    console.error('添加MCP服务器失败:', error)
    // 如果是网络错误或其他异常，显示错误信息
    const errorMessage = error instanceof Error ? error.message : t('config.mcpConfig.addFailed')
    showMessage(errorMessage, 'error')
  } finally {
    loading.value = false
  }
}

// Show delete confirmation modal
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// Handle delete server
const handleDeleteServer = async () => {
  if (!selectedServer.value?.id) {
    showMessage(t('config.mcpConfig.noServerSelected'), 'error')
    return
  }

  try {
    loading.value = true
    const result = await McpApiService.removeMcpServer(selectedServer.value.id)
    if (result.success) {
      showMessage(t('config.mcpConfig.deleteSuccess'))
      selectedServer.value = null
      showDeleteModal.value = false
      await loadMcpServers()
    } else {
      showMessage(result.message || t('config.mcpConfig.deleteFailed'), 'error')
    }
  } catch (error) {
    console.error('删除MCP服务器失败:', error)
    showMessage(t('config.mcpConfig.deleteFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Handle save server
const handleSave = async () => {
  if (!selectedServer.value?.id) {
    showMessage(t('config.mcpConfig.noServerSelected'), 'error')
    return
  }

  // Validate JSON format
  let parsedConfig
  try {
    parsedConfig = JSON.parse(selectedServer.value.connectionConfig)
  } catch {
    showMessage(t('config.mcpConfig.invalidJson'), 'error')
    return
  }

  try {
    loading.value = true

    // 统一处理配置中的url字段
    const normalizedConfig = normalizeMcpConfig(parsedConfig)
    
    const requestData: McpServerRequest = {
      connectionType: selectedServer.value.connectionType,
      configJson: JSON.stringify(normalizedConfig)
    }

    // 由于没有updateMcpServer方法，我们使用删除后重新添加的方式
    const deleteResult = await McpApiService.removeMcpServer(selectedServer.value.id)
    if (deleteResult.success) {
      const addResult = await McpApiService.addMcpServer(requestData)
      if (addResult.success) {
        showMessage(t('config.mcpConfig.updateSuccess'))
        await loadMcpServers() // Reload the list
      } else {
        showMessage(addResult.message || t('config.mcpConfig.updateFailed'), 'error')
      }
    } else {
      showMessage(deleteResult.message || t('config.mcpConfig.updateFailed'), 'error')
    }
  } catch (error) {
    console.error('更新MCP服务器失败:', error)
    showMessage(t('config.mcpConfig.updateFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Handle import
const handleImport = () => {
  // Implement import logic
  showMessage(t('config.mcpConfig.importFeatureComingSoon'), 'info')
}

// Handle export
const handleExport = () => {
  // Implement export logic
  showMessage(t('config.mcpConfig.exportFeatureComingSoon'), 'info')
}

// JSON validation
const validateJson = () => {
  const jsonText = selectedServer.value?.connectionConfig || newServer.configJson
  if (!jsonText) {
    isJsonValid.value = true
    validationErrors.value = []
    return
  }

  try {
    const parsed = JSON.parse(jsonText)
    const validationResult = validateMcpConfig(parsed)
    
    if (validationResult.isValid) {
      // 验证通过后，应用配置统一化并更新编辑器内容
      const normalizedConfig = normalizeMcpConfig(parsed)
      const normalizedJson = JSON.stringify(normalizedConfig, null, 2)
      
      // 只有当统一化后的JSON与原始JSON不同时才更新
      if (normalizedJson !== jsonText) {
        if (selectedServer.value) {
          selectedServer.value.connectionConfig = normalizedJson
        } else {
          newServer.configJson = normalizedJson
        }
      }
      
      isJsonValid.value = true
      validationErrors.value = []
    } else {
      isJsonValid.value = false
      validationErrors.value = validationResult.errors || []
      if (validationResult.errors && validationResult.errors.length > 0) {
        showMessage(validationResult.errors.join('\n'), 'error')
      }
    }
  } catch (error) {
    isJsonValid.value = false
    
    // 提供更具体的JSON语法错误信息
    let errorMessage = t('config.mcpConfig.invalidJson')
    if (error instanceof SyntaxError) {
      const message = error.message
      if (message.includes('Unexpected token')) {
        errorMessage = '❌ JSON语法错误 - 请检查括号、逗号、引号等符号是否正确'
      } else if (message.includes('Unexpected end')) {
        errorMessage = '❌ JSON不完整 - 请检查是否缺少结束括号或引号'
      } else if (message.includes('Unexpected number')) {
        errorMessage = '❌ JSON数字格式错误 - 请检查数字格式'
      } else if (message.includes('Unexpected string')) {
        errorMessage = '❌ JSON字符串格式错误 - 请检查引号是否配对'
      } else {
        errorMessage = `❌ JSON语法错误: ${message}`
      }
    }
    
    validationErrors.value = [errorMessage]
    showMessage(errorMessage, 'error')
  }
}

// Validate MCP configuration structure
const validateMcpConfig = (config: any): { isValid: boolean; errors?: string[] } => {
  const errors: string[] = []
  
  // Check if config has mcpServers property
  if (!config.mcpServers || typeof config.mcpServers !== 'object') {
    errors.push(t('config.mcpConfig.missingMcpServers'))
    errors.push('💡 正确格式示例: {"mcpServers": {"server-id": {"name": "服务器名称", "url": "服务器地址"}}}')
    return { isValid: false, errors }
  }

  const servers = config.mcpServers
  
  // Validate each server configuration
  for (const [serverId, serverConfig] of Object.entries(servers)) {
    if (typeof serverConfig !== 'object' || serverConfig === null) {
      errors.push(t('config.mcpConfig.invalidServerConfig', { serverId }))
      continue
    }

    const server = serverConfig as any
    
    // Check required fields - name field is optional, so we skip validation

    // Validate based on whether command exists
    if (server.command) {
      // If command exists, validate args and env
      if (!Array.isArray(server.args)) {
        errors.push(t('config.mcpConfig.invalidArgs', { serverId }))
      } else {
        // args should contain strings
        for (let i = 0; i < server.args.length; i++) {
          if (typeof server.args[i] !== 'string') {
            errors.push(t('config.mcpConfig.invalidArgsType', { serverId, index: i }))
          }
        }
      }
      
      // 1.1 增强env校验逻辑：可以没有env，有env的话可以允许env:[]为空
      if (server.env !== undefined) {
        if (server.env !== null && typeof server.env !== 'object') {
          errors.push(t('config.mcpConfig.invalidEnv', { serverId }))
        } else if (server.env !== null && Array.isArray(server.env)) {
          // env是数组的情况，允许空数组
          if (server.env.length > 0) {
            // 如果数组不为空，检查每个元素是否为字符串
            for (let i = 0; i < server.env.length; i++) {
              if (typeof server.env[i] !== 'string') {
                errors.push(t('config.mcpConfig.invalidEnvType', { serverId, index: i }))
              }
            }
          }
        } else if (server.env !== null && !Array.isArray(server.env)) {
          // env是对象的情况，检查每个值是否为字符串
          for (const [key, value] of Object.entries(server.env)) {
            if (typeof value !== 'string') {
              errors.push(t('config.mcpConfig.invalidEnvType', { serverId, key }))
            }
          }
        }
      }
      // 如果没有env字段，则跳过校验（允许没有env）
    } else {
      // If no command, validate url or baseUrl - 必须有一个
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'
      
      if (!hasUrl && !hasBaseUrl) {
        errors.push(`缺少url字段: ${serverId} - 没有command时必须有url或baseUrl`)
        errors.push('💡 需要提供 url 或 baseUrl 字段')
      } else {
        // 2. 校验url或baseUrl格式
        const urlToValidate = hasUrl ? server.url : server.baseUrl
        try {
          new URL(urlToValidate)
        } catch {
          errors.push(t('config.mcpConfig.invalidUrl', { serverId }))
        }
        
        // 3. 统一使用url字段：如果配置中使用的是baseUrl，转换为url
        if (hasBaseUrl && !hasUrl) {
          server.url = server.baseUrl
          delete server.baseUrl
        }
      }
    }
  }

  if (errors.length === 0) {
    return { isValid: true }
  } else {
    return { isValid: false, errors }
  }
}

// JSON formatting
const formatJson = () => {
  try {
    const jsonText = selectedServer.value?.connectionConfig || newServer.configJson
    const parsed = JSON.parse(jsonText)
    const formatted = JSON.stringify(parsed, null, 2)
    
    if (selectedServer.value) {
      selectedServer.value.connectionConfig = formatted
    } else {
      newServer.configJson = formatted
    }
    
    validateJson()
  } catch (error) {
    showMessage(t('config.mcpConfig.invalidJson'), 'error')
  }
}

// 统一处理MCP配置中的url字段
const normalizeMcpConfig = (config: any): any => {
  if (!config.mcpServers) {
    return config
  }

  const normalizedConfig = { ...config }
  normalizedConfig.mcpServers = { ...config.mcpServers }

  for (const [serverId, serverConfig] of Object.entries(config.mcpServers)) {
    const server = serverConfig as any
    const normalizedServer = { ...server }

    // 如果没有command，处理url/baseUrl统一化
    if (!server.command) {
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'
      
      if (hasBaseUrl && !hasUrl) {
        // 如果只有baseUrl，转换为url
        normalizedServer.url = server.baseUrl
        delete normalizedServer.baseUrl
      } else if (!hasUrl && !hasBaseUrl) {
        // 如果既没有url也没有baseUrl，保持原样（让校验函数处理错误）
        console.warn(`Server ${serverId} has no command but also no url or baseUrl`)
      }
    }

    normalizedConfig.mcpServers[serverId] = normalizedServer
  }

  return normalizedConfig
}

// Format JSON for display with syntax highlighting
const formatJsonForDisplay = (jsonString: string): string => {
  if (!jsonString) return ''
  try {
    const parsed = JSON.parse(jsonString)
    const formatted = JSON.stringify(parsed, null, 2)
    return highlightJson(formatted)
  } catch (e) {
    return highlightJson(jsonString) // 即使解析失败也尝试高亮显示
  }
}

// Simple JSON syntax highlighting
const highlightJson = (json: string): string => {
  // 先处理数字
  let highlighted = json.replace(/\b(\d+\.?\d*)\b/g, '<span class="json-number">$1</span>')
  
  // 处理布尔值和null
  highlighted = highlighted.replace(/\b(true|false)\b/g, '<span class="json-boolean">$1</span>')
  highlighted = highlighted.replace(/\bnull\b/g, '<span class="json-null">null</span>')
  
  // 处理字符串值（但不包括键名）
  highlighted = highlighted.replace(/"([^"]*)"/g, (match, content) => {
    // 如果这个字符串后面跟着冒号，说明是键名
    if (match.endsWith('":')) {
      return `<span class="json-key">"${content}"</span>:`
    }
    // 否则是字符串值
    return `<span class="json-string">"${content}"</span>`
  })
  
  return highlighted
}

// Get JSON status text
const getJsonStatusText = (): string => {
  const jsonText = selectedServer.value?.connectionConfig || newServer.configJson
  if (!jsonText) {
    return t('config.mcpConfig.jsonStatusEmpty')
  }
  
  if (isJsonValid.value) {
    return t('config.mcpConfig.jsonStatusValid')
  } else {
    // Show validation errors if available
    if (validationErrors.value.length > 0) {
      return validationErrors.value.join('\n')
    }
    return t('config.mcpConfig.jsonStatusInvalid')
  }
}

// Show message toast
const showMessage = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
  message.text = text
  message.type = type
  message.show = true

  setTimeout(() => {
    message.show = false
  }, 3000)
}

// Load MCP servers list
const loadMcpServers = async () => {
  try {
    loading.value = true
    mcpServers.value = await McpApiService.getAllMcpServers()
  } catch (error) {
    console.error('加载MCP服务器列表失败:', error)
    showMessage(t('config.basicConfig.loadConfigFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Reset form
const resetForm = () => {
  newServer.connectionType = 'STUDIO'
  newServer.configJson = ''
  isJsonValid.value = true
  validationErrors.value = []
}

// Load data when the component is mounted
onMounted(() => {
  loadMcpServers()
})
</script>

<style scoped>
.mcp-config-panel {
  position: relative;
}

.mcp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.mcp-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
}

.mcp-stats {
  display: flex;
  margin-left: 16px;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.stat-label {
  color: rgba(255, 255, 255, 0.6);
  margin-right: 4px;
}

.stat-value {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.search-box {
  position: relative;
}

.search-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 6px 12px 6px 32px;
  color: rgba(255, 255, 255, 0.9);
  width: 220px;
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
  width: 260px;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  opacity: 0.6;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.7);
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.mcp-layout {
  display: flex;
  gap: 24px;
}

.server-list {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.server-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.search-box {
  position: relative;
  margin-bottom: 20px;
}

.search-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 6px 12px 6px 32px;
  color: rgba(255, 255, 255, 0.9);
  width: 100%;
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
  width: 100%;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  opacity: 0.6;
}

.servers-container {
  flex-grow: 1;
  overflow-y: auto;
  padding-right: 10px; /* Add some padding for scrollbar */
  max-height: calc(100vh - 300px); /* 设置最大高度，确保可以滚动 */
}

.server-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.server-card:hover:not(.active) {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.1);
}

.server-card.active {
  border: 1px solid #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.server-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.server-name {
  flex-grow: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.server-connection-type {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.connection-type-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: rgba(158, 158, 158, 0.2);
  color: #bdbdbd;
}

.connection-type-badge.studio {
  background: rgba(33, 150, 243, 0.2);
  color: #90caf9;
}

.connection-type-badge.sse {
  background: rgba(76, 175, 80, 0.2);
  color: #a5d6a7;
}

.connection-type-badge.streaming {
  background: rgba(156, 39, 176, 0.2);
  color: #ce93d8;
}

.server-config-preview {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 8px 12px;
  max-height: 120px; /* 减少最大高度 */
  overflow-y: auto;
  overflow-x: auto;
}

.config-preview {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 10px; /* 减小字体大小 */
  line-height: 1.3;
  color: rgba(255, 255, 255, 0.9);
  white-space: pre-wrap;
  word-break: break-all;
}

/* JSON语法高亮 */
.config-preview .json-key {
  color: #90caf9;
}

.config-preview .json-string {
  color: #a5d6a7;
}

.config-preview .json-number {
  color: #f39c12;
}

.config-preview .json-boolean {
  color: #e74c3c;
}

.config-preview .json-null {
  color: #95a5a6;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.7);
}

.loading-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.5);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: white;
  padding: 10px 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: center;
}

.add-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.add-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.server-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.detail-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  background: rgba(244, 67, 54, 0.1);
  border: 1px solid rgba(244, 67, 54, 0.3);
  border-radius: 4px;
  color: #ef5350;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.5);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}

.action-btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef5350;
}

.action-btn.danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.form-item .required {
  color: #ef5350;
  margin-left: 4px;
}

.json-editor-container {
  position: relative;
}

.json-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.json-status {
  font-size: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  font-weight: 500;
  max-width: 400px;
  word-wrap: break-word;
  white-space: pre-line;
  line-height: 1.4;
  min-height: 20px;
}

.json-status.valid {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
}

.json-status.invalid {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.format-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 4px;
  color: #a8b3ff;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.format-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.5);
}

.format-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.format-icon {
  font-size: 12px;
}

.json-editor-wrapper {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
  min-height: 300px;
}

.json-editor {
  height: 100%;
  min-height: 280px;
}

.config-textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
  resize: vertical;
  transition: all 0.3s;
}

.config-textarea:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.config-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
  text-align: center;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.message-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.message-toast.success {
  background: #10b981;
}

.message-toast.error {
  background: #ef4444;
}

.message-toast.info {
  background: #667eea;
}

.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.3s ease;
}

.message-fade-enter-from,
.message-fade-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

@media (max-width: 1200px) {
  .mcp-layout {
    flex-direction: column;
  }

  .server-list,
  .server-detail {
    width: 100%;
  }

  .server-list {
    order: 2;
  }

  .server-detail {
    order: 1;
  }
}

@media (max-width: 768px) {
  .mcp-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .search-input:focus {
    width: 100%;
  }

  .mcp-table-wrapper {
    overflow-x: scroll;
  }

  .mcp-table {
    min-width: 600px;
  }

  .connection-type-options {
    flex-direction: column;
    gap: 12px;
  }

  .mcp-form-actions {
    flex-direction: column;
    gap: 12px;
  }

  .mcp-form-actions button {
    width: 100%;
  }
}
</style>
