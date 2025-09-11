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
      <h2>{{ t('config.agentConfig.title') }}</h2>
    </template>

    <template #actions>
      <button class="action-btn" @click="showMultiLanguageDialog">
        <Icon icon="carbon:language" />
        {{ t('agent.multiLanguage.title') }}
      </button>
      <button class="action-btn" @click="handleImport">
        <Icon icon="carbon:upload" />
        {{ t('config.agentConfig.import') }}
      </button>
      <button class="action-btn" @click="handleExport" :disabled="!selectedAgent">
        <Icon icon="carbon:download" />
        {{ t('config.agentConfig.export') }}
      </button>
    </template>

    <div class="agent-layout">
      <!-- Agent List -->
      <div class="agent-list">
        <div class="list-header">
          <h3>{{ t('config.agentConfig.configuredAgents') }}</h3>
          <span class="agent-count"
            >({{ agents.length }}{{ t('config.agentConfig.agentCount') }})</span
          >
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
              <div class="agent-name-section">
                <span class="agent-name">{{ agent.name }}</span>
                <span v-if="agent.builtIn" class="built-in-badge">Built-in</span>
              </div>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p class="agent-desc">{{ agent.description }}</p>
            <div class="agent-model" v-if="agent.model">
              <span class="model-tag">
                {{ agent.model.type }}
              </span>
              <span class="model-tag">
                {{ agent.model.modelName }}
              </span>
            </div>
            <div class="agent-tools" v-if="agent.availableTools?.length > 0">
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
          {{ t('common.loading') }}
        </div>

        <div v-if="!loading && agents.length === 0" class="empty-state">
          <Icon icon="carbon:bot" class="empty-icon" />
          <p>{{ t('config.agentConfig.noAgent') }}</p>
        </div>

        <button class="add-btn" @click="showAddAgentModal">
          <Icon icon="carbon:add" />
          {{ t('config.agentConfig.createNew') }}
        </button>
      </div>

      <!-- Agent Details -->
      <div class="agent-detail" v-if="selectedAgent">
        <div class="detail-header">
          <h3>{{ selectedAgent.name }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              {{ t('common.save') }}
            </button>
            <button 
              class="action-btn danger" 
              @click="showDeleteConfirm"
              :disabled="!!selectedAgent?.builtIn"
              :title="selectedAgent?.builtIn ? t('config.agentConfig.cannotDeleteBuiltIn') : ''"
            >
              <Icon icon="carbon:trash-can" />
              {{ t('common.delete') }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>{{ t('config.agentConfig.agentName') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="selectedAgent.name"
            :placeholder="t('config.agentConfig.agentNamePlaceholder')"
            required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.agentConfig.description') }} <span class="required">*</span></label>
          <textarea
            v-model="selectedAgent.description"
            rows="3"
            :placeholder="t('config.agentConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>

        <div class="form-item">
          <label>{{ t('config.agentConfig.nextStepPrompt') }}</label>
          <textarea
            :value="selectedAgent.nextStepPrompt || ''"
            @input="selectedAgent.nextStepPrompt = ($event.target as HTMLTextAreaElement).value"
            rows="8"
            :placeholder="t('config.agentConfig.nextStepPromptPlaceholder')"
          ></textarea>
        </div>

        <!-- Model Allocation Area -->
        <div class="model-section">
          <h4>{{ t('config.agentConfig.modelConfiguration') }}</h4>
          <div class="form-item">
            <div class="model-chooser">
              <button class="model-btn" @click="toggleDropdown" :title="$t('model.switch')">
                <Icon icon="carbon:build-run" width="18" />
                <span v-if="chooseModel" class="current-model">
                  <span class="model-type">{{ chooseModel.type }}</span>
                  <span class="spacer"></span>
                  <span class="model-name">{{ chooseModel.modelName }}</span>
                </span>
                <span v-else class="current-model">
                  <span class="current-model">{{
                    t('config.agentConfig.modelConfigurationLabel')
                  }}</span>
                </span>
                <Icon
                  :icon="showDropdown ? 'carbon:chevron-up' : 'carbon:chevron-down'"
                  width="14"
                  class="chevron"
                />
              </button>

              <div v-if="showDropdown" class="model-dropdown" @click.stop>
                <div class="dropdown-header">
                  <span>{{ t('config.agentConfig.modelConfigurationLabel') }}</span>
                  <button class="close-btn" @click="showDropdown = false">
                    <Icon icon="carbon:close" width="16" />
                  </button>
                </div>
                <div class="model-options">
                  <button
                    v-for="option in modelOptions"
                    :key="option.id"
                    class="model-option"
                    :class="{ active: chooseModel?.id === option.id }"
                    @click="selectModel(option)"
                  >
                    <span class="model-type">{{ option.type }}</span>
                    <span class="model-name">{{ option.modelName }}</span>
                    <Icon
                      v-if="chooseModel?.id === option.id"
                      icon="carbon:checkmark"
                      width="16"
                      class="check-icon"
                    />
                  </button>
                </div>
              </div>

              <!-- Backdrop -->
              <div v-if="showDropdown" class="backdrop" @click="showDropdown = false"></div>
            </div>
          </div>
        </div>

        <!-- Tool Allocation Area -->
        <div class="tools-section">
          <h4>{{ t('config.agentConfig.toolConfiguration') }}</h4>

          <!-- Assigned Tools -->
          <AssignedTools
            :title="t('config.agentConfig.assignedTools')"
            :selected-tool-ids="selectedAgent.availableTools || []"
            :available-tools="availableTools"
            :add-button-text="t('config.agentConfig.addRemoveTools')"
            :empty-text="t('config.agentConfig.noAssignedTools')"
            :show-add-button="availableTools.length > 0"
            @add-tools="showToolSelectionModal"
            @tools-filtered="handleToolsFiltered"
          />
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.agentConfig.selectAgentHint') }}</p>
      </div>
    </div>

    <!-- New Agent Popup -->
    <Modal v-model="showModal" :title="t('config.agentConfig.newAgent')" @confirm="handleAddAgent">
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.agentConfig.agentName') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newAgent.name"
            :placeholder="t('config.agentConfig.agentNamePlaceholder')"
            required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.agentConfig.description') }} <span class="required">*</span></label>
          <textarea
            v-model="newAgent.description"
            rows="3"
            :placeholder="t('config.agentConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>
        <div class="form-item">
          <label>{{ t('config.agentConfig.nextStepPrompt') }}</label>
          <textarea
            :value="newAgent.nextStepPrompt || ''"
            @input="newAgent.nextStepPrompt = ($event.target as HTMLTextAreaElement).value"
            rows="8"
            :placeholder="t('config.agentConfig.nextStepPromptPlaceholder')"
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- Tool Selection Popup -->
    <ToolSelectionModal
      v-model="showToolModal"
      :tools="availableTools"
      :selected-tool-ids="selectedAgent?.availableTools || []"
      @confirm="handleToolSelectionConfirm"
    />

    <!-- Delete Confirmation Popup -->
    <Modal v-model="showDeleteModal" :title="t('config.agentConfig.deleteConfirm')">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>
          {{ t('config.agentConfig.deleteConfirmText') }}
          <strong>{{ selectedAgent?.name }}</strong> {{ t('common.confirm') }}？
        </p>
        <p class="warning-text">{{ t('config.agentConfig.deleteWarning') }}</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">
          {{ t('common.cancel') }}
        </button>
        <button class="confirm-btn danger" @click="handleDelete">{{ t('common.delete') }}</button>
      </template>
    </Modal>

    <!-- Error Prompt -->
    <div v-if="error" class="error-toast" @click="error = ''">
      <Icon icon="carbon:error" />
      {{ error }}
    </div>

    <!-- Success Prompt -->
    <div v-if="success" class="success-toast" @click="success = ''">
      <Icon icon="carbon:checkmark" />
      {{ success }}
    </div>

    <!-- Multi-language Management Modal -->
    <Modal v-model="showMultiLanguageModal" :title="t('agent.multiLanguage.title')" @confirm="confirmResetAgents">
      <template #title>
        {{ t('agent.multiLanguage.title') }}
      </template>

      <div class="multi-language-content">
        <div class="stats-section">
          <div class="stat-item">
            <span class="stat-label">{{ t('agent.multiLanguage.currentLanguage') }}:</span>
            <span class="stat-value">{{ getLanguageLabel($i18n.locale) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">{{ t('common.total') }}:</span>
            <span class="stat-value">{{ agentStats.total }}</span>
          </div>
        </div>

        <div class="language-selection">
          <label class="selection-label">{{ t('agent.multiLanguage.selectLanguage') }}:</label>
          <select v-model="selectedLanguage" class="language-select">
            <option value="">{{ t('agent.multiLanguage.selectLanguage') }}</option>
            <option v-for="lang in supportedLanguages" :key="lang" :value="lang">
              {{ getLanguageLabel(lang) }}
            </option>
          </select>
        </div>

        <div class="warning-section">
          <div class="warning-box">
            <Icon icon="carbon:warning" class="warning-icon" />
            <div class="warning-text">
              <p>{{ t('agent.multiLanguage.resetAllWarning') }}</p>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </ConfigPanel>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted ,watch} from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import ConfigPanel from './components/configPanel.vue'
import Modal from '@/components/modal/index.vue'
import ToolSelectionModal from '@/components/tool-selection-modal/ToolSelectionModal.vue'
import AssignedTools from '@/components/shared/AssignedTools.vue'
import { AgentApiService, type Agent, type Tool } from '@/api/agent-api-service'
import { type Model, ModelApiService } from '@/api/model-api-service'
import { usenameSpaceStore } from '@/stores/namespace'
import { getSupportedLanguages, resetAllAgents, getAgentStats, type AgentStats } from '@/api/agent'

// Internationalization
const { t } = useI18n()

const namespaceStore = usenameSpaceStore()
const { namespace} = storeToRefs(namespaceStore)

// Reactive data
const loading = ref(false)
const error = ref('')
const success = ref('')
const agents = reactive<Agent[]>([])
const selectedAgent = ref<Agent | null>(null)
const availableTools = reactive<Tool[]>([])
const showModal = ref(false)
const showDeleteModal = ref(false)
const showToolModal = ref(false)
const showDropdown = ref(false)
const chooseModel = ref<Model | null>(null)
const modelOptions = reactive<Model[]>([])

// Multi-language management
const showMultiLanguageModal = ref(false)
const supportedLanguages = ref<string[]>([])
const selectedLanguage = ref<string>('')
const resetting = ref(false)
const agentStats = ref<AgentStats>({
  total: 0,
  namespace: '',
  supportedLanguages: []
})

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const selectModel = (option: Model) => {
  chooseModel.value = option
  showDropdown.value = false
}

// New Agent form data
const newAgent = reactive<Omit<Agent, 'id' | 'availableTools'>>({
  name: '',
  description: '',
  nextStepPrompt: '',
})

// Computed property - removed unused unassignedTools since it's not used in the template

// Helper function for agent card display
const getToolDisplayName = (toolId: string): string => {
  const tool = availableTools.find(t => t.key === toolId)
  return tool ? tool.name : toolId
}

// Message Prompt
const showMessage = (msg: string, type: 'success' | 'error') => {
  if (type === 'success') {
    success.value = msg
    setTimeout(() => {
      success.value = ''
    }, 3000)
  } else {
    error.value = msg
    setTimeout(() => {
      error.value = ''
    }, 5000)
  }
}

// Load data
const loadData = async () => {
  loading.value = true
  try {
    // Load the Agent list and available tools in parallel
    const [loadedAgents, loadedTools, loadedModels] = await Promise.all([
      AgentApiService.getAllAgents(namespace.value),
      AgentApiService.getAvailableTools(),
      ModelApiService.getAllModels(),
    ])

    // Ensure each agent has an availableTools array
    const normalizedAgents = loadedAgents.map(agent => ({
      ...agent,
      availableTools: agent.availableTools,
      ...loadedModels,
    }))

    // Sort agents: non-built-in agents first, then built-in agents
    const sortedAgents = normalizedAgents.sort((a, b) => {
      // If both are built-in or both are not built-in, maintain original order
      if (a.builtIn === b.builtIn) {
        return 0
      }
      // Put non-built-in agents first (a.builtIn = false comes before b.builtIn = true)
      return a.builtIn ? 1 : -1
    })

    agents.splice(0, agents.length, ...sortedAgents)
    availableTools.splice(0, availableTools.length, ...loadedTools)
    modelOptions.splice(0, modelOptions.length, ...loadedModels)

    // Select the first agent
    if (sortedAgents.length > 0) {
      await selectAgent(sortedAgents[0])
    }
  } catch (err: any) {
    console.error('Failed to load data:', err)
    showMessage(t('config.agentConfig.loadDataFailed') + ': ' + err.message, 'error')
  } finally {
    loading.value = false
  }
}

// Select Agent
const selectAgent = async (agent: Agent) => {
  try {
    // Load the detailed information
    const detailedAgent = await AgentApiService.getAgentById(agent.id)
    // Agent interface guarantees availableTools is an array
    selectedAgent.value = {
      ...detailedAgent,
      availableTools: detailedAgent.availableTools,
    }
    chooseModel.value = detailedAgent.model ?? null
  } catch (err: any) {
    console.error('Failed to load Agent details:', err)
    showMessage(t('config.agentConfig.loadDetailsFailed') + ': ' + err.message, 'error')
    // Use basic information as a fallback
    selectedAgent.value = {
      ...agent,
      availableTools: agent.availableTools,
    }
  }
}

// Show the new Agent modal
const showAddAgentModal = () => {
  newAgent.name = ''
  newAgent.description = ''
  newAgent.nextStepPrompt = ''
  showModal.value = true
}

// Create a new Agent
const handleAddAgent = async () => {
  if (!newAgent.name.trim() || !newAgent.description.trim()) {
    showMessage(t('config.agentConfig.requiredFields'), 'error')
    return
  }

  try {
    const agentData: Omit<Agent, 'id'> = {
      name: newAgent.name.trim(),
      description: newAgent.description.trim(),
      nextStepPrompt: newAgent.nextStepPrompt?.trim() ?? '',
      availableTools: [],
      namespace: namespace.value
    }

    const createdAgent = await AgentApiService.createAgent(agentData)
    agents.push(createdAgent)
    selectedAgent.value = createdAgent
    showModal.value = false
    showMessage(t('config.agentConfig.createSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.agentConfig.createFailed') + ': ' + err.message, 'error')
  }
}

// Show the tool selection popup
const showToolSelectionModal = () => {
  showToolModal.value = true
}

// Handle tool selection confirmation
const handleToolSelectionConfirm = (selectedToolIds: string[]) => {
  if (selectedAgent.value) {
    selectedAgent.value.availableTools = [...selectedToolIds]
  }
}

// Handle tools filtered event (remove tools that are no longer available)
const handleToolsFiltered = (filteredTools: string[]) => {
  if (selectedAgent.value) {
    selectedAgent.value.availableTools = [...filteredTools]
  }
}

// Removed unused addTool function since it's not used anywhere in the code

// Save Agent
const handleSave = async () => {
  if (!selectedAgent.value) return

  if (!selectedAgent.value.name.trim() || !selectedAgent.value.description.trim()) {
    showMessage(t('config.agentConfig.requiredFields'), 'error')
    return
  }

  try {
    selectedAgent.value.model = chooseModel.value
    const savedAgent = await AgentApiService.updateAgent(
      selectedAgent.value.id,
      selectedAgent.value
    )

    // Update the data in the local list
    const index = agents.findIndex(a => a.id === savedAgent.id)
    if (index !== -1) {
      agents[index] = savedAgent
    }

    selectedAgent.value = savedAgent
    selectedAgent.value.model = chooseModel.value
    showMessage(t('config.agentConfig.saveSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.agentConfig.saveFailed') + ': ' + err.message, 'error')
  }
}

// Show the delete confirmation popup
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// Delete Agent
const handleDelete = async () => {
  if (!selectedAgent.value) return

  try {
    await AgentApiService.deleteAgent(selectedAgent.value.id)

    // Remove from the list
    const index = agents.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.splice(index, 1)
    }

    // Select another Agent or clear the selection
    selectedAgent.value = agents.length > 0 ? agents[0] : null
    showDeleteModal.value = false
    showMessage(t('config.agentConfig.deleteSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.agentConfig.deleteFailed') + ': ' + err.message, 'error')
  }
}

// Import Agent
const handleImport = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = event => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = async e => {
        try {
          const agentData = JSON.parse(e.target?.result as string)
          // Basic validation
          if (!agentData.name || !agentData.description) {
            throw new Error(t('config.agentConfig.invalidFormat'))
          }

          // Remove the id field and let the backend assign a new id
          const { id: _id, ...importData } = agentData
          const savedAgent = await AgentApiService.createAgent(importData)
          agents.push(savedAgent)
          selectedAgent.value = savedAgent
          showMessage(t('config.agentConfig.importSuccess'), 'success')
        } catch (err: any) {
          showMessage(t('config.agentConfig.importFailed') + ': ' + err.message, 'error')
        }
      }
      reader.readAsText(file)
    }
  }
  input.click()
}

// Export Agent
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
    showMessage(t('config.agentConfig.exportSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.agentConfig.exportFailed') + ': ' + err.message, 'error')
  }
}

// Multi-language management methods
const getLanguageLabel = (lang: string): string => {
  const labels: Record<string, string> = {
    'zh': t('language.zh'),
    'en': 'English'
  }
  return labels[lang] || lang
}

const loadSupportedLanguages = async () => {
  try {
    const response = await getSupportedLanguages()
    supportedLanguages.value = response.languages
    if (!selectedLanguage.value && response.default) {
      selectedLanguage.value = response.default
    }
  } catch (error) {
    console.error('Failed to load supported languages:', error)
    showMessage(t('common.loadFailed'), 'error')
  }
}

const loadAgentStats = async () => {
  try {
    const response = await getAgentStats()
    agentStats.value = response
  } catch (error) {
    console.error('Failed to load agent stats:', error)
  }
}

const showMultiLanguageDialog = async () => {
  await Promise.all([
    loadSupportedLanguages(),
    loadAgentStats()
  ])
  showMultiLanguageModal.value = true
}

const confirmResetAgents = async () => {
  if (!selectedLanguage.value) {
    showMessage(t('agent.multiLanguage.selectLanguage'), 'error')
    return
  }

  resetting.value = true
  try {
    await resetAllAgents({ language: selectedLanguage.value })
    showMessage(t('agent.multiLanguage.resetSuccess'), 'success')
    showMultiLanguageModal.value = false

    // Reload agents and stats
    await Promise.all([
      loadData(),
      loadAgentStats()
    ])
  } catch (error: any) {
    console.error('Failed to reset agents:', error)
    showMessage(error.message || t('agent.multiLanguage.resetFailed'), 'error')
  } finally {
    resetting.value = false
  }
}

// Load data when the component is mounted
onMounted(() => {
  loadData()
})

watch(
  () => namespace.value,
  (newNamespace, oldNamespace) => {
    if (newNamespace !== oldNamespace) {
      agents.splice(0)
      selectedAgent.value = null
      loadData()
    }
  }
)
</script>

<style scoped>
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
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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
  align-items: flex-start;
  margin-bottom: 8px;
}

.agent-name-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
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

.agent-model {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.model-tag {
  display: inline-block;
  padding: 4px 8px;
  margin-bottom: 10px;
  background: rgba(181, 102, 234, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: #a8b3ff;
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

.model-section {
  h4 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
  }
}

.tools-section {
  h4 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
  }
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

/* Modal styles */
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

.confirm-btn,
.cancel-btn {
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

/* Toast messages */
.error-toast,
.success-toast {
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

.model-chooser {
  position: relative;
  display: inline-block;
}

.model-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: 1.5px solid #667eea;
  border-radius: 8px;
  color: #8da2fb;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 600;
  outline: none;
}

.model-btn:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: #7c9eff;
  color: #a3bffa;
  box-shadow: 0 0 15px rgba(102, 126, 234, 0.2);
}

.model-btn:focus {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.5);
}

.current-model {
  color: inherit;
  font-weight: 600;
  min-width: 40px;
  text-align: left;
  text-shadow: none;
}

.chevron {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.9;
  filter: none;
}

.model-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 9999;
  margin-top: 4px;
  background: linear-gradient(135deg, rgba(40, 40, 50, 0.95), rgba(30, 30, 40, 0.95));
  backdrop-filter: blur(16px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(102, 126, 234, 0.2);
  min-width: 300px;
  animation: slideDown 0.2s ease;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(102, 126, 234, 0.05));
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.model-options {
  padding: 8px 0;
}

.model-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.model-option:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
}

.model-option.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(102, 126, 234, 0.1));
  color: #7c9eff;
  border-left: 3px solid #667eea;
  padding-left: 13px;
}

.model-type {
  display: inline-block;
  min-width: 24px;
  font-size: 12px;
  font-weight: 600;
  opacity: 0.8;
}

.model-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  color: #667eea;
  opacity: 0.8;
}

.spacer {
  display: inline-block;
  width: 12px;
}

.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: transparent;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Multi-language management styles */
.multi-language-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 0;
}

.stats-section {
  display: flex;
  gap: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
}

.language-selection {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selection-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.language-select {
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.language-select:focus {
  border-color: #007acc;
  background: rgba(255, 255, 255, 0.15);
}

.language-select option {
  background: #2d2d2d;
  color: #ffffff;
}

.warning-section {
  margin: 10px 0;
}

.warning-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px;
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  border-radius: 8px;
}

.warning-icon {
  color: #ffc107;
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.warning-text {
  flex: 1;
}

.warning-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  line-height: 1.5;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Responsive design */
@media (max-width: 768px) {
  .model-dropdown {
    right: -8px;
    left: -8px;
    width: auto;
    min-width: auto;
  }

  .model-btn {
    padding: 6px 10px;
    font-size: 13px;
  }

  .current-model {
    min-width: 35px;
  }
}

/* Dark theme adjustments */
@media (prefers-color-scheme: light) {
  .model-dropdown {
    background: rgba(255, 255, 255, 0.95);
    border-color: rgba(0, 0, 0, 0.1);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  }

  .dropdown-header {
    color: rgba(0, 0, 0, 0.8);
    border-bottom-color: rgba(0, 0, 0, 0.1);
  }

  .close-btn {
    color: rgba(0, 0, 0, 0.6);
  }

  .close-btn:hover {
    background: rgba(0, 0, 0, 0.1);
    color: rgba(0, 0, 0, 0.8);
  }

  .model-option {
    color: rgba(0, 0, 0, 0.7);
  }

  .model-option:hover {
    background: rgba(0, 0, 0, 0.05);
    color: rgba(0, 0, 0, 0.9);
  }
}

.built-in-badge {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  align-self: flex-start;
  box-shadow: 0 1px 3px rgba(79, 70, 229, 0.3);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #ccc;
}

.action-btn.danger:disabled {
  background: #ccc;
  border-color: #ccc;
}
</style>
