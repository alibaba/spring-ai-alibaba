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
      <!-- Global action buttons -->
      <div class="global-actions">
        <div class="json-actions">
          <button class="action-btn" @click="startJsonImport">
            <Icon icon="carbon:arrow-up" />
            {{ t('config.mcpConfig.importAll') }}
          </button>
          <button class="action-btn" @click="exportAllConfigs">
            <Icon icon="carbon:arrow-down" />
            {{ t('config.mcpConfig.exportAll') }}
          </button>
        </div>
      </div>
    </template>

    <div class="mcp-layout">
      <!-- MCP Server List -->
      <div class="server-list">
        <div class="list-header">
          <h3>{{ t('config.mcpConfig.serverList') }}</h3>
          <span class="server-count">({{ servers.length }})</span>
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
              <div class="server-status-toggle" @click.stop="toggleServerStatus(server)">
                <div
                  class="status-toggle"
                  :class="{ 'enabled': server.status === 'ENABLE' }"
                >
                  <div class="toggle-thumb"></div>
                  <span class="toggle-label">{{ server.status === 'ENABLE' ? t('config.mcpConfig.enabled') : t('config.mcpConfig.disabled') }}</span>
                </div>
              </div>
            </div>
            <div class="server-connection-type">
              <Icon :icon="getConnectionTypeIcon(server.connectionType)" class="connection-type-icon" />
              <span class="connection-type-badge" :class="server.connectionType.toLowerCase()">
                {{ server.connectionType }}
              </span>
            </div>
            <div class="server-config-summary">
              <div class="config-item" v-if="getServerConfigValue(server, 'command')">
                <span class="config-label">{{ t('config.mcpConfig.command') }}:</span>
                <span class="config-value">{{ getServerConfigValue(server, 'command') }}</span>
              </div>
              <div class="config-item" v-if="getServerConfigValue(server, 'url')">
                <span class="config-label">{{ t('config.mcpConfig.url') }}:</span>
                <span class="config-value">{{ getServerConfigValue(server, 'url') }}</span>
              </div>
              <div class="config-item" v-if="getServerConfigValue(server, 'args')">
                <span class="config-label">{{ t('config.mcpConfig.args') }}:</span>
                <span class="config-value">{{ getServerConfigValue(server, 'args') }}</span>
              </div>
              <div class="config-item" v-if="getServerConfigValue(server, 'env')">
                <span class="config-label">{{ t('config.mcpConfig.env') }}:</span>
                <span class="config-value">{{ getServerConfigValue(server, 'env') }}</span>
              </div>
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

        <!-- Add configuration button -->
        <div class="add-config-button-container">
          <button class="add-btn" @click="startAddConfig">
            <Icon icon="carbon:add" />
            {{ t('config.mcpConfig.newMcpConfig') }}
          </button>
        </div>
      </div>

      <!-- MCP Server Detail (Edit Mode) -->
      <div class="server-detail" v-if="selectedServer">
        <div class="detail-header">
          <h3>{{ selectedServer.mcpServerName }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave" :disabled="loading">
              <Icon icon="carbon:save" />
              {{ t('config.mcpConfig.save') }}
            </button>
            <button class="action-btn danger" @click="handleDelete" :disabled="loading">
              <Icon icon="carbon:trash-can" />
              {{ t('config.mcpConfig.delete') }}
            </button>
          </div>
        </div>
        <div class="detail-content">
          <McpConfigForm
            :form-data="configForm"
            :is-edit-mode="true"
            @connection-type-change="handleConnectionTypeChange"
          />
        </div>
      </div>

      <!-- Add configuration form panel -->
      <div v-else-if="showAddForm" class="server-detail">
        <div class="detail-header">
          <h3>{{ t('config.mcpConfig.newMcpConfig') }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave" :disabled="loading">
              <Icon icon="carbon:save" />
              {{ t('config.mcpConfig.save') }}
            </button>
            <button class="action-btn" @click="resetNewConfig">
              <Icon icon="carbon:reset" />
              {{ t('config.mcpConfig.reset') }}
            </button>
          </div>
        </div>
        <div class="detail-content">
          <McpConfigForm
            :form-data="configForm"
            :is-edit-mode="false"
            @update:form-data="(data: any) => Object.assign(configForm, data)"
            @connection-type-change="handleConnectionTypeChange"
          />
        </div>
      </div>

                  <!-- JSON import form panel -->
            <div v-else-if="showJsonImport" class="server-detail">
              <div class="detail-header">
                <h3>{{ t('config.mcpConfig.importAll') }}</h3>
                <div class="detail-actions">
                  <button class="action-btn primary" @click="handleJsonImport" :disabled="loading">
                    <Icon icon="carbon:save" />
                    {{ t('config.mcpConfig.import') }}
                  </button>
                  <button class="action-btn" @click="cancelJsonImport">
                    <Icon icon="carbon:close" />
                    {{ t('config.mcpConfig.cancel') }}
                  </button>
                </div>
              </div>
        <div class="detail-content">
          <JsonImportPanel
            v-model="jsonEditorContent"
            @validation-change="handleJsonValidationChange"
          />
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.mcpConfig.selectServerHint') }}</p>
      </div>
    </div>

    <!-- JSON Editor Modal -->
    <Modal v-model="showJsonModal" :title="t('config.mcpConfig.jsonEditor')" @confirm="handleJsonSave">
      <div class="json-editor-container">
        <div class="json-editor-header">
          <span class="json-status" :class="{ 'valid': isJsonValid, 'invalid': !isJsonValid && jsonEditorContent.trim() }">
            {{ getJsonStatusText() }}
          </span>
          <button
            v-if="jsonEditorContent.trim()"
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
              v-model="jsonEditorContent"
              :placeholder="t('config.mcpConfig.configJsonPlaceholder')"
              @change="validateJson"
              class="json-editor"
              language="json"
          />
        </div>
      </div>
    </Modal>

    <!-- Delete confirmation modal -->
    <Modal v-model="showDeleteModal" :title="t('config.mcpConfig.confirmDelete')" @confirm="handleDeleteServer">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>{{ t('config.mcpConfig.deleteConfirmMessage') }}</p>
        <p class="warning-text">{{ t('config.mcpConfig.deleteWarningText') }}</p>
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
import { ref, onMounted, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { McpApiService, type McpServer } from '@/api/mcp-api-service'
import MonacoEditor from '@/components/MonacoEditor.vue'
import ConfigPanel from './components/configPanel.vue'
import Modal from '@/components/modal/index.vue'
import { Icon } from '@iconify/vue'
import McpConfigForm from './components/McpConfigForm.vue' // Import new form component
import JsonImportPanel from './components/JsonImportPanel.vue'
import { useMcpConfigForm } from '@/composables/useMcpConfigForm'
import { useMessage } from '@/composables/useMessage'
import { useRequest } from '@/composables/useRequest'
import type { McpServerSaveRequest, JsonValidationResult } from '@/types/mcp'

// Extend McpServer interface to include UI fields
interface ExtendedMcpServer extends McpServer {
  args?: string // Frontend display as JSON string
  env?: string // Frontend display as JSON string
  url?: string
  command?: string
}

// Internationalization
const { t } = useI18n()

// Use composition functions
const { configForm, resetForm, populateFormFromServer, validateForm, handleConnectionTypeChange } = useMcpConfigForm()
const { message, showMessage } = useMessage()
const { loading } = useRequest()

// Reactive data
const servers = ref<McpServer[]>([])
const selectedServer = ref<McpServer | null>(null)
const showDeleteModal = ref(false)
const showAddForm = ref(false)
const showJsonImport = ref(false)
const showJsonModal = ref(false)
const jsonEditorContent = ref('')
const isJsonValid = ref(true)
const validationErrors = ref<string[]>([])
const searchQuery = ref('')

// JSON Editor Modal
const isJsonModalForEdit = ref(false) // true for edit, false for new



// Computed property: Filtered MCP servers
const filteredMcpServers = computed(() => {
  if (!searchQuery.value.trim()) {
    return servers.value
  }

  const query = searchQuery.value.toLowerCase()
  return servers.value.filter((server: McpServer) =>
      server.mcpServerName.toLowerCase().includes(query) ||
      server.connectionType.toLowerCase().includes(query) ||
      server.connectionConfig.toLowerCase().includes(query)
  )
})

// Helper function to get server configuration values
const getServerConfigValue = (server: McpServer, field: 'command' | 'url' | 'args' | 'env'): string => {
  try {
    const config = JSON.parse(server.connectionConfig)
    switch (field) {
      case 'command':
        return config.command || ''
      case 'url':
        return config.url || ''
      case 'args':
        if (config.args && Array.isArray(config.args)) {
          return config.args.join('\n')
        }
        return ''
      case 'env':
        if (config.env && typeof config.env === 'object' && !Array.isArray(config.env)) {
          return Object.entries(config.env)
            .map(([key, value]) => `${key}:${value}`)
            .join('\n')
        }
        return ''
      default:
        return ''
    }
  } catch (error) {
    // If parsing fails, try to use field data
    const extendedServer = server as ExtendedMcpServer
    switch (field) {
      case 'command':
        return extendedServer.command || ''
      case 'url':
        return extendedServer.url ?? ''
      case 'args':
        return extendedServer.args ?? ''
      case 'env':
        return extendedServer.env ?? ''
      default:
        return ''
    }
  }
}



// Select a server
const selectServer = (server: McpServer) => {
  selectedServer.value = { ...server }
  showAddForm.value = false // Hide add form
  showJsonImport.value = false // Hide JSON import form
}



// Handle JSON save
const handleJsonSave = () => {
  if (!jsonEditorContent.value.trim()) {
    showMessage(t('config.mcpConfig.jsonConfigEmpty'), 'error')
    return
  }

  try {
    const parsed = JSON.parse(jsonEditorContent.value)

    if (isJsonModalForEdit.value && selectedServer.value) {
              // Edit mode: update selected server
        selectedServer.value.connectionConfig = JSON.stringify(parsed, null, 2)
    } else {
      // Create mode: create new server
      handleAddServerFromJson(parsed)
    }

    showJsonModal.value = false
    showMessage(t('config.mcpConfig.jsonConfigSaved'), 'success')
  } catch {
    showMessage(t('config.mcpConfig.jsonFormatError'), 'error')
  }
}

// Create new server from JSON
const handleAddServerFromJson = async (serverData: any) => {
  try {
    loading.value = true
    const result = await McpApiService.importMcpServers(serverData)

    if (result.success) {
      showMessage(t('config.mcpConfig.addSuccess'))
      await loadMcpServers()
    } else {
      showMessage(result.message, 'error')
    }
  } catch (error) {
    console.error('Failed to add MCP server:', error)
    showMessage(t('config.mcpConfig.addFailed'), 'error')
  } finally {
    loading.value = false
  }
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
    console.error('Failed to delete MCP server:', error)
    showMessage(t('config.mcpConfig.deleteFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Handle save
const handleSave = async () => {
  // Use form validation
  const validation = validateForm()
  if (!validation.isValid) {
    showMessage(validation.errors[0], 'error')
    return
  }

  try {
    // Build request data
    const requestData: McpServerSaveRequest = {
      connectionType: configForm.connectionType,
      mcpServerName: configForm.mcpServerName,
      status: configForm.status
    }

    if (configForm.connectionType === 'STUDIO') {
      requestData.command = configForm.command
      if (configForm.args.trim()) {
        try {
          // Convert multi-line string to array
          const argsArray = configForm.args.split('\n').filter(arg => arg.trim())
          // Validate that each element is a string
          if (!argsArray.every(arg => typeof arg === 'string')) {
            showMessage(t('config.mcpConfig.argsStringError'), 'error')
            return
          }
          requestData.args = argsArray
        } catch {
          showMessage(t('config.mcpConfig.argsFormatError'), 'error')
          return
        }
      }
      if (configForm.env.trim()) {
        try {
          // Convert multi-line key:value to object
          const envLines = configForm.env.split('\n').filter(line => line.trim())
          const envObj: Record<string, string> = {}

          for (const line of envLines) {
            const colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
              const key = line.substring(0, colonIndex).trim()
              const value = line.substring(colonIndex + 1).trim()
              if (key && value) {
                envObj[key] = value
              }
            }
          }

          // Validate that each value is a string
          if (!Object.values(envObj).every(value => typeof value === 'string')) {
            showMessage(t('config.mcpConfig.envStringError'), 'error')
            return
          }

          requestData.env = envObj
        } catch {
          showMessage(t('config.mcpConfig.envFormatError'), 'error')
          return
        }
      }
    } else {
      requestData.url = configForm.url
    }

    // If edit mode, add ID
    if (selectedServer.value?.id) {
      requestData.id = selectedServer.value.id
    }

    // Use unified save method
    const result = await McpApiService.saveMcpServer(requestData)

    if (result.success) {
      showMessage(selectedServer.value?.id ? t('config.mcpConfig.updateSuccess') : t('config.mcpConfig.addSuccess'), 'success')
      await loadMcpServers()
      if (!selectedServer.value?.id) {
        // Reset form after successful addition
        resetForm()
        showAddForm.value = false
      }
    } else {
      showMessage(result.message || t('config.mcpConfig.operationFailed'), 'error')
    }
  } catch (error) {
    console.error('Save failed:', error)
    showMessage(t('config.mcpConfig.saveFailed'), 'error')
  }
}



// JSON validation
const validateJson = () => {
  const jsonText = jsonEditorContent.value
  if (!jsonText) {
    isJsonValid.value = true
    validationErrors.value = []
    return
  }

  try {
    const parsed = JSON.parse(jsonText)
    const validationResult = validateMcpConfig(parsed)

    if (validationResult.isValid) {
      // After validation passes, apply configuration normalization and update editor content
      const normalizedConfig = normalizeMcpConfig(parsed)
      const normalizedJson = JSON.stringify(normalizedConfig, null, 2)

      // Only update when normalized JSON differs from original JSON
      if (normalizedJson !== jsonText) {
        jsonEditorContent.value = normalizedJson
      }

      isJsonValid.value = true
      validationErrors.value = []
    } else {
      isJsonValid.value = false
      validationErrors.value = validationResult.errors ?? []
      if (validationResult.errors && validationResult.errors.length > 0) {
        showMessage(validationResult.errors.join('\n'), 'error')
      }
    }
  } catch (error) {
    isJsonValid.value = false

    // Provide more specific JSON syntax error information
    let errorMessage = t('config.mcpConfig.invalidJson')
    if (error instanceof SyntaxError) {
      const message = error.message
      if (message.includes('Unexpected token')) {
        errorMessage = t('config.mcpConfig.jsonSyntaxError')
      } else if (message.includes('Unexpected end')) {
        errorMessage = t('config.mcpConfig.jsonIncomplete')
      } else if (message.includes('Unexpected number')) {
        errorMessage = t('config.mcpConfig.jsonNumberError')
      } else if (message.includes('Unexpected string')) {
        errorMessage = t('config.mcpConfig.jsonStringError')
      } else {
        errorMessage = t('config.mcpConfig.jsonSyntaxErrorWithMessage', { message })
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
    errors.push(t('config.mcpConfig.correctFormatExample'))
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

      // 1.1 Enhanced env validation logic: can have no env, if env exists, allow empty env:[]
      if (server.env !== undefined) {
        if (server.env !== null && typeof server.env !== 'object') {
          errors.push(t('config.mcpConfig.invalidEnv', { serverId }))
        } else if (server.env !== null && Array.isArray(server.env)) {
          // env is array case, allow empty array
          if (server.env.length > 0) {
            // If array is not empty, check if each element is a string
            for (let i = 0; i < server.env.length; i++) {
              if (typeof server.env[i] !== 'string') {
                errors.push(t('config.mcpConfig.invalidEnvType', { serverId, index: i }))
              }
            }
          }
        } else if (server.env !== null && !Array.isArray(server.env)) {
          // env is object case, check if each value is a string
          for (const [key, value] of Object.entries(server.env)) {
            if (typeof value !== 'string') {
              errors.push(t('config.mcpConfig.invalidEnvType', { serverId, key }))
            }
          }
        }
      }
      // If no env field, skip validation (allow no env)
    } else {
      // If no command, validate url or baseUrl - must have one
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'

      if (!hasUrl && !hasBaseUrl) {
        errors.push(t('config.mcpConfig.missingUrlField', { serverId }))
        errors.push(t('config.mcpConfig.urlFieldTip'))
      } else {
        // 2. Validate url or baseUrl format
        const urlToValidate = hasUrl ? server.url : server.baseUrl
        try {
          new URL(urlToValidate)
        } catch {
          errors.push(t('config.mcpConfig.invalidUrl', { serverId }))
        }

        // 3. Unify url field usage: if baseUrl is used in config, convert to url
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
    const jsonText = jsonEditorContent.value
    const parsed = JSON.parse(jsonText)
    const formatted = JSON.stringify(parsed, null, 2)

    jsonEditorContent.value = formatted
    validateJson()
  } catch {
    showMessage(t('config.mcpConfig.invalidJson'), 'error')
  }
}

// Unify url field handling in MCP configuration
const normalizeMcpConfig = (config: any): any => {
  if (!config.mcpServers) {
    return config
  }

  const normalizedConfig = { ...config }
  normalizedConfig.mcpServers = { ...config.mcpServers }

  for (const [serverId, serverConfig] of Object.entries(config.mcpServers)) {
    const server = serverConfig as any
    const normalizedServer = { ...server }

    // If no command, handle url/baseUrl unification
    if (!server.command) {
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'

      if (hasBaseUrl && !hasUrl) {
        // If only baseUrl exists, convert to url
        normalizedServer.url = server.baseUrl
        delete normalizedServer.baseUrl
      } else if (!hasUrl && !hasBaseUrl) {
        // If neither url nor baseUrl exists, keep as is (let validation function handle error)
        console.warn(t('config.mcpConfig.serverConfigWarning', { serverId }))
      }
    }

    normalizedConfig.mcpServers[serverId] = normalizedServer
  }

  return normalizedConfig
}

// Get JSON status text
const getJsonStatusText = (): string => {
  const jsonText = jsonEditorContent.value
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



// Load MCP servers list
const loadMcpServers = async () => {
  try {
    loading.value = true
    servers.value = await McpApiService.getAllMcpServers()

    // If there are servers and no currently selected server, auto-select the first one
    if (servers.value.length > 0 && !selectedServer.value && !showAddForm.value && !showJsonImport.value) {
      selectServer(servers.value[0])
    }
  } catch (error) {
    console.error('Failed to load MCP server list:', error)
    showMessage(t('config.basicConfig.loadConfigFailed'), 'error')
  } finally {
    loading.value = false
  }
}





// Toggle server status
const toggleServerStatus = async (server: McpServer) => {
  try {
    loading.value = true
    let result: any

    // Determine operation to execute based on current status
    // If currently enabled, disable after click
    // If currently disabled, enable after click
    if (server.status === 'ENABLE') {
      // Currently enabled, disable after click
      result = await McpApiService.disableMcpServer(server.id)
    } else {
      // Currently disabled, enable after click
      result = await McpApiService.enableMcpServer(server.id)
    }

    if (result.success) {
      // Update local status
      server.status = server.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
      showMessage(result.message || t('config.mcpConfig.statusToggleSuccess'))
    } else {
      showMessage(result.message || t('config.mcpConfig.statusToggleFailed'), 'error')
    }
  } catch (error) {
    console.error('Status toggle failed:', error)
    showMessage(t('config.mcpConfig.statusToggleFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Start adding new configuration
const startAddConfig = () => {
  selectedServer.value = null
  showAddForm.value = true
  showJsonImport.value = false
  resetNewConfig()
}

// Reset new configuration
const resetNewConfig = () => {
  resetForm()
}

// Start JSON import
const startJsonImport = () => {
  showJsonImport.value = true
  selectedServer.value = null
  showAddForm.value = false
  jsonEditorContent.value = ''
  isJsonValid.value = true
  validationErrors.value = []
}

// Cancel JSON import
const cancelJsonImport = () => {
  showJsonImport.value = false
  selectedServer.value = null
  showAddForm.value = false
  jsonEditorContent.value = ''
  isJsonValid.value = true
  validationErrors.value = []
}

// Watch selectedServer changes, populate configForm
watch(selectedServer, (newServer) => {
  if (newServer) {
    populateFormFromServer(newServer)
  }
}, { immediate: true })

// Load data when the component is mounted
onMounted(() => {
  loadMcpServers()
})

// Handle delete server
const handleDelete = () => {
  if (!selectedServer.value) {
    showMessage(t('config.mcpConfig.noServerSelected'), 'error')
    return
  }
  showDeleteModal.value = true
}

// Export all configurations
const exportAllConfigs = async () => {
  try {
    loading.value = true
    const servers = await McpApiService.getAllMcpServers()

    // Build export data
    const exportData: { mcpServers: Record<string, any> } = {
      mcpServers: {}
    }

    servers.forEach(server => {
      try {
        const config = JSON.parse(server.connectionConfig)
        exportData.mcpServers[server.mcpServerName] = config
      } catch (error) {
        console.error(`Failed to parse server configuration: ${server.mcpServerName}`, error)
      }
    })

    // Create and download file
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'mcp_servers.json'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    showMessage(t('config.mcpConfig.exportSuccess'))
  } catch (error) {
    console.error('Failed to export MCP servers:', error)
    showMessage(t('config.mcpConfig.exportFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Handle JSON validation result changes
const handleJsonValidationChange = (result: JsonValidationResult) => {
  isJsonValid.value = result.isValid
  validationErrors.value = result.errors ?? []
}

// Handle JSON import
const handleJsonImport = async () => {
  if (!jsonEditorContent.value.trim()) {
    showMessage(t('config.mcpConfig.jsonConfigEmpty'), 'error')
    return
  }

  try {
    const parsed = JSON.parse(jsonEditorContent.value)
    const validationResult = validateMcpConfig(parsed)

    if (validationResult.isValid) {
      loading.value = true
      const result = await McpApiService.importMcpServers(parsed)
      if (result.success) {
        showMessage(t('config.mcpConfig.importSuccess'))
        await loadMcpServers()
      } else {
        showMessage(result.message || t('config.mcpConfig.importFailed'), 'error')
      }
    } else {
      showMessage(validationResult.errors?.join('\n') ?? t('config.mcpConfig.importInvalidJson'), 'error')
    }
  } catch (error) {
    showMessage(t('config.mcpConfig.importFailed'), 'error')
  } finally {
    loading.value = false
    showJsonImport.value = false
  }
}

// Get connection type icon
const getConnectionTypeIcon = (type: string) => {
  switch (type) {
    case 'STUDIO':
      return 'carbon:plug'
    case 'SSE':
      return 'carbon:plug'
    case 'STREAMING':
      return 'carbon:plug'
    default:
      return 'carbon:plug'
  }
}
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
  gap: 30px;
  flex: 1;
  min-height: 0;
}

.server-list {
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

.server-count {
  color: rgba(255, 255, 255, 0.6);
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
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.servers-container::-webkit-scrollbar {
  width: 6px;
}

.servers-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.servers-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.servers-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.server-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.server-card:hover:not(.active) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.2);
}

.server-card.active {
  border-color: #667eea;
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
  gap: 10px;
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
  display: flex;
  align-items: center;
  gap: 6px;
}

.connection-type-icon {
  font-size: 14px;
  opacity: 0.8;
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

.server-config-summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.config-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.config-label {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
  min-width: 50px;
  margin-right: 8px;
}

.config-value {
  color: rgba(255, 255, 255, 0.8);
  word-break: break-all;
  flex: 1;
}

.server-config-preview {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 8px 12px;
  max-height: 120px; /* Reduce max height */
  overflow-y: auto;
  overflow-x: auto;
}

.config-preview {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 10px; /* Reduce font size */
  line-height: 1.3;
  color: rgba(255, 255, 255, 0.9);
  white-space: pre-wrap;
  word-break: break-all;
}

/* JSON syntax highlighting */
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

.add-config-button-container {
  margin-top: 0;
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

.form-row {
  display: flex;
  gap: 20px;
}

.form-row .form-item {
  flex: 1;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}



.server-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  overflow-y: auto;
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

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-content::-webkit-scrollbar {
  width: 6px;
}

.detail-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
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
  border-radius: 4px;
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
  border-radius: 4px;
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

.config-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.config-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.config-input::placeholder {
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
  z-index: 9999; /* Increase z-index to ensure top layer */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  max-width: 400px; /* Limit max width */
  word-wrap: break-word; /* Allow text wrapping */
  white-space: pre-line; /* Preserve line breaks */
  line-height: 1.4;
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

/* Wide modal styles */
.wide-modal {
  width: 80vw !important;
  max-width: 1200px !important;
}

/* Form row layout */
.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-row .form-item {
  flex: 1;
}

/* Narrow input styles */
.narrow-input {
  width: 70%;
}

.narrow-input .config-input,
.narrow-input .config-textarea {
  width: 100%;
}

/* Status toggle component styles */
.status-toggle-container {
  display: flex;
  align-items: center;
}

.status-toggle {
  position: relative;
  width: 60px;
  height: 30px;
  background: #6b7280;
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
}

.status-toggle.enabled {
  background: #10b981;
}

.toggle-thumb {
  position: absolute;
  width: 24px;
  height: 24px;
  background: white;
  border-radius: 50%;
  top: 3px;
  left: 3px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.status-toggle.enabled .toggle-thumb {
  left: 33px;
}

.toggle-label {
  color: white;
  font-size: 12px;
  font-weight: 500;
  user-select: none;
}

/* Status toggle styles in server card */
.server-status-toggle {
  display: flex;
  align-items: center;
}

.server-card-header .status-toggle {
  width: 50px;
  height: 24px;
}

.server-card-header .toggle-thumb {
  width: 18px;
  height: 18px;
  top: 3px;
  left: 3px;
}

.server-card-header .status-toggle.enabled .toggle-thumb {
  left: 29px;
}

.server-card-header .toggle-label {
  font-size: 10px;
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

  .form-row {
    flex-direction: column;
    gap: 15px;
  }

  .narrow-input {
    width: 100%;
  }

  /* Add configuration form responsive styles */
  .server-detail {
    padding: 16px;
  }

  .detail-header {
    margin-bottom: 16px;
  }

  .detail-content {
    gap: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 16px;
  }

  .form-actions {
    flex-direction: column;
    gap: 8px;
  }

  .form-actions .action-btn {
    width: 100%;
  }
}

/* JSON import related styles */
.json-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.json-import-form {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.json-import-form .form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.json-import-form .form-item label {
  margin-bottom: 8px;
}

.json-import-form .monaco-editor {
  flex: 1;
  min-height: 600px;
  border: none;
  border-radius: 0;
}

.validation-errors {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 0, 0, 0.1);
  border: 1px solid rgba(255, 0, 0, 0.3);
  border-radius: 4px;
}

.error-item {
  color: #ff4444;
  font-size: 14px;
  margin-bottom: 4px;
}

.error-item:last-child {
  margin-bottom: 0;
}

/* TabPanel related styles */
.json-tab-panel {
  margin-top: 8px;
}

/* Configuration example related styles */
.example-json {
  margin: 0;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  overflow-x: auto;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
}

.example-json code {
  color: rgba(255, 255, 255, 0.9);
  background: none;
  padding: 0;
  border: none;
  border-radius: 0;
  font-family: inherit;
  font-size: inherit;
}

/* JSON syntax highlighting */
.example-json .string { color: #a78bfa; }
.example-json .number { color: #fbbf24; }
.example-json .boolean { color: #f87171; }
.example-json .null { color: rgba(255, 255, 255, 0.6); }
.example-json .key { color: #34d399; }
</style>
