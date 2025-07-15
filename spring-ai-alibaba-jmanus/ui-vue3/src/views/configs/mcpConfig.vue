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
  <div class="mcp-config-panel">
    <div class="mcp-header">
      <div class="header-left">
        <h2>{{ t('config.mcpConfig.title') }}</h2>
        <div class="mcp-stats">
          <span class="stat-item">
            <span class="stat-label">{{ t('config.mcpConfig.mcpServers') }}:</span>
            <span class="stat-value">{{ mcpServers.length }}</span>
          </span>
        </div>
      </div>
      <div class="header-right">
        <div class="search-box">
          <input
              v-model="searchQuery"
              type="text"
              :placeholder="t('config.mcpSearch')"
              class="search-input"
          />
          <span class="search-icon">🔍</span>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>{{ t('config.loading') }}</p>
    </div>

    <!-- MCP Layout -->
    <div v-else class="mcp-layout">
      <!-- MCP Server List -->
      <div class="mcp-table-container">
        <h3 class="section-title">{{ t('config.mcpConfig.serverList') }}</h3>

        <!-- Empty State -->
        <div v-if="filteredMcpServers.length === 0" class="empty-state">
          <div class="empty-state-icon">📂</div>
          <div class="empty-state-text">
            {{ searchQuery ? t('config.notFound') : t('config.mcpConfig.noServers') }}
          </div>
        </div>

        <!-- MCP Server Table -->
        <div v-else class="mcp-table-wrapper">
          <table class="mcp-table">
            <thead>
            <tr>
              <th>ID</th>
              <th>{{ t('agent.name') }}</th>
              <th>{{ t('config.mcpConfig.connectionType') }}</th>
              <th>{{ t('config.mcpConfig.configJsonLabel') }}</th>
              <th>{{ t('common.actions') }}</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="server in filteredMcpServers" :key="server.id" class="mcp-row">
              <td class="mcp-id">{{ server.id }}</td>
              <td class="mcp-server-name">
                <div class="server-name-content">
                  <span class="server-icon">🔌</span>
                  {{ server.mcpServerName }}
                </div>
              </td>
              <td class="mcp-connection-type">
                  <span class="connection-type-badge" :class="server.connectionType.toLowerCase()">
                    {{ server.connectionType }}
                  </span>
              </td>
              <td class="mcp-config">
                <div class="config-preview" :title="server.connectionConfig">
                  {{ formatConfig(server.connectionConfig) }}
                </div>
              </td>
              <td class="mcp-actions">
                <button
                    @click="removeMcpServer(server.id)"
                    class="action-btn delete-btn"
                    :disabled="loading"
                >
                  {{ t('common.delete') }}
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Add MCP Server Form -->
      <div class="add-mcp-container">
        <div class="add-mcp-header">
          <h3 class="add-mcp-title">{{ t('config.mcpConfig.addMcpServer') }}</h3>
        </div>

        <div class="mcp-form">
          <!-- Connection Type Selection -->
          <div class="mcp-form-group">
            <label class="form-label">{{ t('config.mcpConfig.connectionType') }}：</label>
            <div class="connection-type-options">
              <div class="connection-type-option">
                <input
                    type="radio"
                    id="mcp-connection-type-studio"
                    v-model="newMcpServer.connectionType"
                    value="STUDIO"
                />
                <label for="mcp-connection-type-studio" class="radio-label">
                  <span class="radio-title">STUDIO</span>
                  <span class="connection-type-desc">{{ t('config.mcpConfig.instructionStep1LocalDesc') }}</span>
                </label>
              </div>
              <div class="connection-type-option">
                <input
                    type="radio"
                    id="mcp-connection-type-sse"
                    v-model="newMcpServer.connectionType"
                    value="SSE"
                />
                <label for="mcp-connection-type-sse" class="radio-label">
                  <span class="radio-title">SSE</span>
                  <span class="connection-type-desc">{{ t('config.mcpConfig.instructionStep1RemoteDesc') }}</span>
                </label>
              </div>
              <div class="connection-type-option">
                <input
                    type="radio"
                    id="mcp-connection-type-streaming"
                    v-model="newMcpServer.connectionType"
                    value="STREAMING"
                />
                <label for="mcp-connection-type-streaming" class="radio-label">
                  <span class="radio-title">Streamable HTTP</span>
                  <span class="connection-type-desc">{{ t('config.mcpConfig.instructionStep1RemoteDesc') }}</span>
                </label>
              </div>
            </div>
          </div>

          <!-- JSON Config Input -->
          <div class="mcp-form-group">
            <label class="form-label">{{ t('config.mcpConfig.configJsonLabel') }}</label>
            <textarea
                v-model="newMcpServer.configJson"
                :placeholder="t('config.mcpConfig.configJsonPlaceholder')"
                class="config-textarea"
                rows="6"
            ></textarea>
          </div>

          <!-- Action Buttons -->
          <div class="mcp-form-actions">
            <button @click="addMcpServer" class="action-btn add-btn" :disabled="loading">
              {{ t('common.add') }}
            </button>
            <button @click="resetForm" class="action-btn reset-btn" :disabled="loading">
              {{ t('common.reset') }}
            </button>

          </div>
        </div>

        <!-- Usage Instructions -->
        <div class="mcp-form-instructions">
          <h4>{{ t('config.mcpConfig.instructions') }}</h4>
          <ol>
            <li>{{ t('config.mcpConfig.instructionStep1') }}
              <ul class="indented-list">
                <li><strong>{{ t('config.mcpConfig.instructionStep1Local') }}</strong>：{{ t('config.mcpConfig.instructionStep1LocalDesc') }}</li>
                <li><strong>{{ t('config.mcpConfig.instructionStep1Remote') }}</strong>：{{ t('config.mcpConfig.instructionStep1RemoteDesc') }}</li>
              </ul>
            </li>
            <li>{{ t('config.mcpConfig.instructionStep2') }}</li>
            <li>{{ t('config.mcpConfig.instructionStep3') }}</li>
            <li>{{ t('config.mcpConfig.instructionStep4') }}</li>
          </ol>
        </div>
      </div>
    </div>

    <!-- Message Toast -->
    <transition name="message-fade">
      <div v-if="message.show" :class="['message-toast', message.type]">
        {{ message.text }}
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { McpApiService, type McpServer, type McpServerRequest } from '@/api/mcp-api-service'

// Internationalization
const { t } = useI18n()

// Reactive data
const loading = ref(false)
const mcpServers = ref<McpServer[]>([])
const searchQuery = ref('')

// Add MCP Server Form
const newMcpServer = reactive<McpServerRequest & { configJson: string }>({
  connectionType: 'STUDIO',
  configJson: ''
})

// Message Toast
const message = reactive({
  show: false,
  text: '',
  type: 'success' as 'success' | 'error'
})

// Computed property: Whether it can be submitted
const canSubmit = computed(() => {
  return newMcpServer.configJson.trim().length > 0
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

// Format configuration information
const formatConfig = (config: string): string => {
  if (!config) return ''

  // If the configuration information is too long, truncate it for display
  if (config.length > 50) {
    return config.substring(0, 50) + '...'
  }

  return config
}

// Show message toast
const showMessage = (text: string, type: 'success' | 'error' = 'success') => {
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

// Add MCP server
const addMcpServer = async () => {
  if (!canSubmit.value) {
    showMessage(t('config.mcpConfig.configRequired'), 'error')
    return
  }

  // Validate JSON format
  try {
    JSON.parse(newMcpServer.configJson)
  } catch {
    showMessage(t('config.mcpConfig.invalidJson'), 'error')
    return
  }

  try {
    loading.value = true

    const requestData: McpServerRequest = {
      connectionType: newMcpServer.connectionType,
      configJson: newMcpServer.configJson
    }

    const result = await McpApiService.addMcpServer(requestData)

    if (result.success) {
      showMessage(t('config.mcpConfig.addSuccess'))
      resetForm()
      await loadMcpServers() // Reload the list
    } else {
      showMessage(result.message || t('config.mcpConfig.addFailed'), 'error')
    }
  } catch (error) {
    console.error('添加MCP服务器失败:', error)
    showMessage(t('config.mcpConfig.addFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Delete MCP Server
const removeMcpServer = async (id: number) => {
  if (!confirm(t('config.mcpConfig.deleteConfirm'))) {
    return
  }

  try {
    loading.value = true

    const result = await McpApiService.removeMcpServer(id)

    if (result.success) {
      showMessage(t('config.mcpConfig.deleteSuccess'))
      await loadMcpServers() // Reload the list
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

// Reset form
const resetForm = () => {
  newMcpServer.connectionType = 'STUDIO'
  newMcpServer.configJson = ''
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
  flex-direction: column;
  gap: 24px;
}

.mcp-table-container {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
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

.empty-state-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state-text {
  font-size: 14px;
}

.mcp-table-wrapper {
  overflow-x: auto;
}

.mcp-table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
  overflow: hidden;
}

.mcp-table th {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  padding: 12px 16px;
  text-align: left;
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.mcp-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.mcp-row:hover {
  background: rgba(255, 255, 255, 0.03);
}

.mcp-id {
  font-family: monospace;
  color: rgba(255, 255, 255, 0.6);
  width: 60px;
}

.server-name-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.server-icon {
  font-size: 16px;
}

.connection-type-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
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

.config-preview {
  font-family: monospace;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.mcp-actions {
  width: 80px;
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
}

.action-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.5);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.add-mcp-container {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  height: fit-content;
}

.add-mcp-header {
  margin-bottom: 20px;
}

.add-mcp-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.mcp-form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.connection-type-options {
  display: flex;
  flex-direction: row;
  gap: 24px;
  flex-wrap: wrap;
}

.connection-type-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.connection-type-option input[type="radio"] {
  margin-top: 2px;
  accent-color: #667eea;
}

.radio-label {
  flex: 1;
  cursor: pointer;
}

.radio-title {
  display: block;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
}

.connection-type-desc {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
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

.mcp-form-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.submit-mcp-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: white;
  padding: 10px 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-mcp-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.submit-mcp-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.reset-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  padding: 10px 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.reset-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.12);
}

.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mcp-form-instructions {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px;
}

.mcp-form-instructions h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.mcp-form-instructions ol {
  margin: 0;
  padding-left: 18px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  line-height: 1.5;
}

.mcp-form-instructions li {
  margin-bottom: 8px;
}

.indented-list {
  margin: 8px 0;
  padding-left: 18px;
}

.indented-list li {
  margin-bottom: 4px;
}

.mcp-form-instructions a {
  color: #90caf9;
  text-decoration: none;
}

.mcp-form-instructions a:hover {
  text-decoration: underline;
}

.mcp-form-instructions code {
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #90caf9;
  word-break: break-all;
  display: inline-block;
  max-width: 100%;
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
    gap: 20px;
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
