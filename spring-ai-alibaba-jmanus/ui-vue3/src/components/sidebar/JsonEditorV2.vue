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
  <div class="config-section">
    <div class="section-header">
      <Icon icon="carbon:code" width="16" />
      <span>{{ $t('sidebar.dynamicAgentPlan') }}</span>
      <div class="section-actions">
        <button
          class="btn btn-sm"
          @click="handleRollback"
          :disabled="!(canRollback ?? false)"
          :title="$t('sidebar.rollback')"
        >
          <Icon icon="carbon:undo" width="14" />
        </button>
        <button
          class="btn btn-sm"
          @click="handleRestore"
          :disabled="!(canRestore ?? false)"
          :title="$t('sidebar.restore')"
        >
          <Icon icon="carbon:redo" width="14" />
        </button>
        <button
          class="btn btn-primary btn-sm"
          @click="handleSave"
          :disabled="isGenerating || isExecuting"
        >
          <Icon icon="carbon:save" width="14" />
        </button>
      </div>
    </div>
    <!-- Error Display -->
    <div v-if="planTypeError" class="error-section">
      <div class="error-message">
        <Icon icon="carbon:warning" width="16" />
        <div class="error-content">
          <div class="error-title">{{ $t('sidebar.planTypeError') }}</div>
          <div class="error-description">{{ planTypeError }}</div>
        </div>
      </div>
    </div>

    <!-- Visual JSON Editor -->
    <div v-else class="visual-editor">
      <!-- Plan Basic Info -->
      <div class="plan-basic-info">
        <div class="form-row">
          <label class="form-label">{{ $t('sidebar.title') }}</label>
          <input 
            v-model="parsedData.title" 
            type="text" 
            class="form-input"
            :placeholder="$t('sidebar.titlePlaceholder')"
          />
        </div>
      </div>

      <!-- Tool Selection Section for Dynamic Agent Plan -->
      <div class="tool-selection-section">
        <div class="form-row">
          <label class="form-label">{{ $t('sidebar.selectedTools') }}</label>
          <div class="tool-selection-container">
            <!-- Selected Tools Display -->
            <div class="selected-tools-display">
              <div 
                v-for="toolKey in selectedToolKeys" 
                :key="toolKey"
                class="selected-tool-item"
              >
                <span class="tool-name">{{ getToolName(toolKey) }}</span>
                <button 
                  @click="removeTool(toolKey)"
                  class="remove-tool-btn"
                  :title="$t('sidebar.removeTool')"
                >
                  <Icon icon="carbon:close" width="12" />
                </button>
              </div>
              <div v-if="selectedToolKeys.length === 0" class="no-tools-selected">
                {{ $t('sidebar.noToolsSelected') }}
              </div>
            </div>
            
            <!-- Tool Selection Controls -->
            <div class="tool-selection-controls">
              <button 
                @click="showToolSelectionModal = true"
                class="btn btn-sm btn-add-tool"
                :disabled="isLoadingTools"
              >
                <Icon icon="carbon:add" width="14" />
                {{ $t('sidebar.addTools') }}
              </button>
              <button 
                @click="clearAllTools"
                class="btn btn-sm btn-clear-tools"
                :disabled="selectedToolKeys.length === 0"
              >
                <Icon icon="carbon:trash-can" width="14" />
                {{ $t('sidebar.clearAllTools') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Steps Editor -->
      <div class="steps-section">
        <div class="steps-header">
          <label class="form-label">{{ $t('sidebar.tasks') }}</label>
          <div class="steps-actions">
            <button 
              @click="loadAvailableAgents" 
              class="btn btn-xs"
              :disabled="isLoadingAgents"
              :title="$t('sidebar.refreshAgents')"
            >
              <Icon icon="carbon:reset" width="12" />
            </button>
            <!-- Agent count badge -->
            <span class="agent-count-badge" v-if="hasLoadedAgents && availableAgents.length > 0">
              {{ availableAgents.length }} agents
            </span>
            <!-- Error indicator -->
            <span class="error-badge" v-if="shouldShowError">
              <Icon icon="carbon:warning" width="12" />
              Error
            </span>
          </div>
        </div>
        
        <div class="steps-container">
          <div 
            v-for="(step, index) in parsedData.steps" 
            :key="index"
            class="step-item"
          >
            <div class="step-header">
              <span class="step-number">{{ $t('sidebar.subtask') }} {{ index + 1 }}</span>
              <div class="step-actions">
                <button 
                  @click="moveStepUp(index)"
                  :disabled="index === 0"
                  class="btn btn-xs"
                  :title="$t('sidebar.moveUp')"
                >
                  <Icon icon="carbon:chevron-up" width="12" />
                </button>
                <button 
                  @click="moveStepDown(index)"
                  :disabled="index === parsedData.steps.length - 1"
                  class="btn btn-xs"
                  :title="$t('sidebar.moveDown')"
                >
                  <Icon icon="carbon:chevron-down" width="12" />
                </button>
                <button 
                  @click="removeStep(index)"
                  class="btn btn-xs btn-danger"
                  :title="$t('sidebar.removeStep')"
                >
                  <Icon icon="carbon:trash-can" width="12" />
                </button>
              </div>
            </div>
            
            <div class="step-content">
              <!-- Agent Selection -->
              <div class="form-row">
                <label class="form-label">{{ $t('sidebar.agent') }}</label>
                <div class="agent-selector">
                  <select v-model="step.agentType" class="form-select agent-select" :disabled="isLoadingAgents || shouldShowError">
                    <!-- Loading state -->
                    <option v-if="isLoadingAgents" disabled>{{ $t('sidebar.loading') }}</option>
                    
                    <!-- Error state -->
                    <option v-else-if="shouldShowError" disabled>{{ $t('sidebar.agentLoadError') }}</option>
                    
                    <!-- Normal state -->
                    <template v-else>
                      <option 
                        v-for="agent in agentOptions" 
                        :key="agent.id"
                        :value="agent.id"
                        :title="generateAgentTooltip(agent)"
                      >
                        {{ formatAgentDisplayText(agent) }}
                      </option>
                    </template>
                  </select>
                  
                  <!-- Error refresh button -->
                  <button 
                    v-if="shouldShowError"
                    @click="loadAvailableAgents" 
                    class="btn btn-sm btn-danger"
                    :title="$t('sidebar.retryLoadAgents')"
                  >
                    <Icon icon="carbon:warning" width="14" />
                    {{ $t('sidebar.retry') }}
                  </button>
                  
                  <!-- Normal add step button -->
                  <button 
                    v-else
                    @click="addStep" 
                    class="btn btn-sm btn-add-step"
                    :title="$t('sidebar.addStep')"
                  >
                    <Icon icon="carbon:add" width="14" />
                  </button>
                </div>
                
                <!-- Error message -->
                <div v-if="shouldShowError && agentsLoadError" class="error-message">
                  <Icon icon="carbon:warning" width="12" />
                  {{ agentsLoadError }}
                </div>
              </div>
              
              <!-- Step Requirement -->
              <div class="form-row">
                <label class="form-label">{{ $t('sidebar.stepRequirement') }}</label>
                <textarea 
                  v-model="step.stepContent"
                  class="form-textarea auto-resize"
                  :placeholder="$t('sidebar.stepRequirementPlaceholder')"
                  rows="4"
                  @input="autoResizeTextarea($event)"
                ></textarea>
              </div>
              
              <!-- Terminate Columns -->
              <div class="form-row">
                <label class="form-label">{{ $t('sidebar.terminateColumns') }}</label>
                <input 
                  v-model="step.terminateColumns"
                  type="text" 
                  class="form-input"
                  :placeholder="$t('sidebar.terminateColumnsPlaceholder')"
                />
              </div>
            </div>
          </div>
          
          <!-- Empty State -->
          <div v-if="parsedData.steps.length === 0" class="empty-steps">
            <Icon icon="carbon:add-alt" width="32" class="empty-icon" />
            <p>{{ $t('sidebar.noSteps') }}</p>
            <button @click="addStep" class="btn btn-primary">
              <Icon icon="carbon:add" width="14" />
              {{ $t('sidebar.addFirstStep') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Plan ID Section -->
      <div class="plan-id-section">
        <div class="form-row">
          <label class="form-label">{{ $t('sidebar.planId') }}</label>
          <input 
            v-model="parsedData.planId" 
            type="text" 
            class="form-input"
            placeholder="planTemplate-1756109892045"
          />
        </div>
      </div>

      <!-- JSON Preview (Optional) -->
      <div class="json-preview" v-if="showJsonPreview">
        <div class="preview-header">
          <label class="form-label">{{ $t('sidebar.jsonPreview') }}</label>
          <button 
            @click="closeJsonPreview"
            class="btn btn-xs"
          >
            <Icon icon="carbon:close" width="12" />
          </button>
        </div>
        <pre class="json-code">{{ formattedJsonOutput }}</pre>
      </div>
      
      <!-- Toggle JSON Preview -->
      <div class="editor-footer">
        <button 
          @click="toggleJsonPreview"
          class="btn btn-sm btn-secondary"
        >
          <Icon icon="carbon:code" width="14" />
          {{ showJsonPreview ? $t('sidebar.hideJson') : $t('sidebar.showJson') }}
        </button>
      </div>
    </div>

    <!-- Tool Selection Modal -->
    <Modal
      v-model="showToolSelectionModal"
      :title="$t('toolSelection.title')"
      @confirm="handleToolSelectionConfirm"
      @update:model-value="showToolSelectionModal = false"
    >
      <div class="tool-selection-content">
        <!-- Search and Sort -->
        <div class="tool-controls">
          <div class="search-container">
            <input
              v-model="toolSearchQuery"
              type="text"
              class="search-input"
              :placeholder="$t('toolSelection.searchPlaceholder')"
            />
          </div>
          <div class="sort-container">
            <select v-model="toolSortBy" class="sort-select">
              <option value="group">{{ $t('toolSelection.sortByGroup') }}</option>
              <option value="name">{{ $t('toolSelection.sortByName') }}</option>
              <option value="enabled">{{ $t('toolSelection.sortByStatus') }}</option>
            </select>
          </div>
        </div>

        <!-- Tool Statistics -->
        <div class="tool-summary">
          <span class="summary-text">
            {{ $t('toolSelection.summary', {
              groups: groupedTools.size,
              tools: totalTools,
              selected: tempSelectedTools.length
            }) }}
          </span>
        </div>

        <!-- Tool Group List -->
        <div class="tool-groups" v-if="groupedTools.size > 0">
          <div
            v-for="[groupName, tools] in groupedTools"
            :key="groupName"
            class="tool-group"
          >
            <!-- Group Header -->
            <div
              class="tool-group-header"
              :class="{ collapsed: collapsedGroups.has(groupName) }"
              @click="toggleGroupCollapse(groupName)"
            >
              <div class="group-title-area">
                <Icon
                  :icon="collapsedGroups.has(groupName) ? 'carbon:chevron-right' : 'carbon:chevron-down'"
                  class="collapse-icon"
                />
                <Icon icon="carbon:folder" class="group-icon" />
                <span class="group-name">{{ groupName }}</span>
                <span class="group-count">
                  ({{ getSelectedToolsInGroup(tools).length }}/{{ tools.length }})
                </span>
              </div>
              <div class="group-actions" @click.stop>
                <label class="group-enable-all">
                  <input
                    type="checkbox"
                    class="group-enable-checkbox"
                    :checked="isGroupFullySelected(tools)"
                    @change="toggleGroupSelection(tools, $event)"
                    :data-group="groupName"
                  />
                  <span class="enable-label">{{ $t('toolSelection.enableAll') }}</span>
                </label>
              </div>
            </div>

            <!-- Group Content -->
            <div
              class="tool-group-content"
              :class="{ collapsed: collapsedGroups.has(groupName) }"
            >
              <div
                v-for="tool in tools.filter(t => t && t.key)"
                :key="tool.key"
                class="tool-selection-item"
              >
                <div class="tool-info">
                  <div class="tool-selection-name">{{ tool.name }}</div>
                  <div v-if="tool.description" class="tool-selection-desc">
                    {{ tool.description }}
                  </div>
                </div>
                <div class="tool-actions">
                  <label class="tool-enable-switch" @click.stop>
                    <input
                      type="checkbox"
                      class="tool-enable-checkbox"
                      :checked="isToolSelected(tool.key)"
                      @change="toggleToolSelection(tool.key, $event)"
                    />
                    <span class="tool-enable-slider"></span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state">
          <Icon icon="carbon:tools" class="empty-icon" />
          <p>{{ $t('toolSelection.noToolsFound') }}</p>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useJsonEditor, type JsonEditorProps } from './json-editor-logic'
import Modal from '../modal/index.vue'
import type { Tool } from '@/api/agent-api-service'
import { AgentApiService } from '@/api/agent-api-service'
import { ref, computed, watch, onMounted } from 'vue'

// Props
const props = withDefaults(defineProps<JsonEditorProps>(), {
  hiddenFields: () => ['currentPlanId', 'userRequest', 'rootPlanId']
})

// Emits
const emit = defineEmits<{
  rollback: []
  restore: []
  save: []
  'update:jsonContent': [value: string]
}>()

const {
  showJsonPreview,
  parsedData,
  availableAgents,
  isLoadingAgents,
  agentsLoadError,
  hasLoadedAgents,
  formattedJsonOutput,
  agentOptions,
  shouldShowError,
  loadAvailableAgents,
  formatAgentDisplayText,
  generateAgentTooltip,
  addStep,
  removeStep,
  moveStepUp,
  moveStepDown,
  handleRollback,
  handleRestore,
  handleSave,
  toggleJsonPreview,
  closeJsonPreview
} = useJsonEditor(props, emit)

// Tool selection state
const showToolSelectionModal = ref(false)
const availableTools = ref<Tool[]>([])
const isLoadingTools = ref(false)
const toolSearchQuery = ref('')
const toolSortBy = ref<'group' | 'name' | 'enabled'>('group')
const collapsedGroups = ref(new Set<string>())
const tempSelectedTools = ref<string[]>([])

// Selected tools for dynamic agent plan
const selectedToolKeys = ref<string[]>([])

// Error state
const planTypeError = ref<string | null>(null)

// Initialize selectedToolKeys from parsedData if it exists
const initializeSelectedToolKeys = () => {
  try {
    // Validate that plan type is dynamic_agent
    if (parsedData.planType !== 'dynamic_agent') {
      const errorMessage = `JsonEditorV2 only supports 'dynamic_agent' plan type. Current plan type: ${parsedData.planType}`
      planTypeError.value = errorMessage
      console.error(errorMessage)
      return
    }
    
    // Clear any previous errors
    planTypeError.value = null
    
    // Try to get from parsedData if it exists (for backward compatibility)
    const data = JSON.parse(props.jsonContent || '{}')
    if (data.selectedToolKeys && Array.isArray(data.selectedToolKeys)) {
      selectedToolKeys.value = data.selectedToolKeys
    }
  } catch (error) {
    const errorMessage = `Failed to initialize JsonEditorV2: ${error instanceof Error ? error.message : 'Unknown error'}`
    planTypeError.value = errorMessage
    console.error(errorMessage, error)
  }
}

// Load available tools
const loadAvailableTools = async () => {
  if (isLoadingTools.value) return
  
  isLoadingTools.value = true
  try {
    const response = await AgentApiService.getAvailableTools()
    if (response && Array.isArray(response)) {
      availableTools.value = response
    }
  } catch (error) {
    console.error('Failed to load tools:', error)
  } finally {
    isLoadingTools.value = false
  }
}

// Tool selection logic
const isToolSelected = (toolKey: string) => {
  return tempSelectedTools.value.includes(toolKey)
}

const toggleToolSelection = (toolKey: string, event: Event) => {
  event.stopPropagation()
  const target = event.target as HTMLInputElement
  const isChecked = target.checked

  if (!toolKey) {
    console.error('toolKey is undefined, cannot proceed')
    return
  }

  if (isChecked) {
    if (!tempSelectedTools.value.includes(toolKey)) {
      tempSelectedTools.value = [...tempSelectedTools.value, toolKey]
    }
  } else {
    tempSelectedTools.value = tempSelectedTools.value.filter(id => id !== toolKey)
  }
}

// Group selection logic
const getSelectedToolsInGroup = (tools: Tool[]) => {
  return tools.filter(tool => tempSelectedTools.value.includes(tool.key))
}

const isGroupFullySelected = (tools: Tool[]) => {
  return tools.length > 0 && tools.every(tool => tempSelectedTools.value.includes(tool.key))
}


const toggleGroupSelection = (tools: Tool[], event: Event) => {
  event.stopPropagation()
  const target = event.target as HTMLInputElement
  const isChecked = target.checked
  const toolKeys = tools.map(tool => tool.key)

  if (isChecked) {
    const newSelected = [...tempSelectedTools.value]
    toolKeys.forEach(key => {
      if (!newSelected.includes(key)) {
        newSelected.push(key)
      }
    })
    tempSelectedTools.value = newSelected
  } else {
    tempSelectedTools.value = tempSelectedTools.value.filter(id => !toolKeys.includes(id))
  }
}

// Group collapse logic
const toggleGroupCollapse = (groupName: string) => {
  if (collapsedGroups.value.has(groupName)) {
    collapsedGroups.value.delete(groupName)
  } else {
    collapsedGroups.value.add(groupName)
  }
}

// Filtered and sorted tools
const filteredTools = computed(() => {
  let filtered = availableTools.value.filter(tool => tool.key)

  if (toolSearchQuery.value) {
    const query = toolSearchQuery.value.toLowerCase()
    filtered = filtered.filter(
      tool =>
        tool.name.toLowerCase().includes(query) ||
        tool.description.toLowerCase().includes(query) ||
        (tool.serviceGroup?.toLowerCase().includes(query) ?? false)
    )
  }

  switch (toolSortBy.value) {
    case 'name':
      filtered = [...filtered].sort((a, b) => a.name.localeCompare(b.name))
      break
    case 'enabled':
      filtered = [...filtered].sort((a, b) => {
        const aSelected = tempSelectedTools.value.includes(a.key)
        const bSelected = tempSelectedTools.value.includes(b.key)
        if (aSelected && !bSelected) return -1
        if (!aSelected && bSelected) return 1
        return a.name.localeCompare(b.name)
      })
      break
    case 'group':
    default:
      filtered = [...filtered].sort((a, b) => {
        const groupA = a.serviceGroup ?? 'Ungrouped'
        const groupB = b.serviceGroup ?? 'Ungrouped'
        if (groupA !== groupB) {
          return groupA.localeCompare(groupB)
        }
        return a.name.localeCompare(b.name)
      })
      break
  }

  return filtered
})

// Tools grouped by service group
const groupedTools = computed(() => {
  const groups = new Map<string, Tool[]>()

  filteredTools.value.forEach(tool => {
    const groupName = tool.serviceGroup ?? 'Ungrouped'
    if (!groups.has(groupName)) {
      groups.set(groupName, [])
    }
    groups.get(groupName)!.push(tool)
  })

  return new Map([...groups.entries()].sort())
})

// Total number of tools
const totalTools = computed(() => filteredTools.value.length)

// Tool management functions
const getToolName = (toolKey: string): string => {
  const tool = availableTools.value.find(t => t.key === toolKey)
  return tool ? tool.name : toolKey
}

const removeTool = (toolKey: string) => {
  selectedToolKeys.value = selectedToolKeys.value.filter((key: string) => key !== toolKey)
}

const clearAllTools = () => {
  selectedToolKeys.value = []
}

// Modal event handlers
const handleToolSelectionConfirm = () => {
  selectedToolKeys.value = [...tempSelectedTools.value]
  showToolSelectionModal.value = false
}

// Watch for modal opening
watch(showToolSelectionModal, (newVisible) => {
  if (newVisible) {
    tempSelectedTools.value = [...selectedToolKeys.value]
    collapsedGroups.value.clear()
    const groupNames = Array.from(groupedTools.value.keys())
    if (groupNames.length > 1) {
      groupNames.slice(1).forEach(name => {
        collapsedGroups.value.add(name)
      })
    }
  }
})

// Watch for selectedToolKeys changes and update JSON output
watch(selectedToolKeys, () => {
  // Update the JSON content to include selectedToolKeys
  const currentData = JSON.parse(props.jsonContent || '{}')
  currentData.selectedToolKeys = selectedToolKeys.value
  currentData.planType = 'dynamic_agent'
  emit('update:jsonContent', JSON.stringify(currentData, null, 2))
}, { deep: true })

// Load tools on mount
onMounted(() => {
  loadAvailableTools()
  initializeSelectedToolKeys()
})

const autoResizeTextarea = (event: Event) => {
  const textarea = event.target as HTMLTextAreaElement
  if (!textarea) return
  
  textarea.style.height = 'auto'
  
  const lineHeight = 20
  const lines = Math.ceil(textarea.scrollHeight / lineHeight)
  
  const minRows = 4
  const maxRows = 12
  const targetRows = Math.max(minRows, Math.min(maxRows, lines))
  
  const newHeight = targetRows * lineHeight
  textarea.style.height = `${newHeight}px`
  textarea.rows = targetRows
  
  if (lines > maxRows) {
    textarea.style.overflowY = 'auto'
  } else {
    textarea.style.overflowY = 'hidden'
  }
}
</script>

<style scoped>
.config-section {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
  gap: 8px;
}

.section-actions {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

/* Error Section Styles */
.error-section {
  margin-bottom: 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  padding: 16px;
}

.error-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  color: #ef4444;
}

.error-content {
  flex: 1;
}

.error-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.error-description {
  font-size: 12px;
  color: rgba(239, 68, 68, 0.8);
  line-height: 1.4;
}

/* Visual Editor Styles */
.visual-editor {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.plan-basic-info,
.tool-selection-section,
.plan-id-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.form-row {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 10px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.form-input,
.form-select,
.form-textarea {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.3);
  color: white;
  font-size: 11px;
  font-family: inherit;
  transition: all 0.2s ease;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  line-height: 1.4;
}

.form-textarea.auto-resize {
  resize: none;
  transition: height 0.2s ease;
  overflow-y: auto;
  max-height: 240px;
}

/* Tool Selection Styles */
.tool-selection-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-tools-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  align-items: flex-start;
}

.selected-tool-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 4px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.9);
}

.tool-name {
  font-weight: 500;
}

.remove-tool-btn {
  width: 16px;
  height: 16px;
  background: transparent;
  border: none;
  border-radius: 2px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.remove-tool-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.no-tools-selected {
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  font-style: italic;
}

.tool-selection-controls {
  display: flex;
  gap: 8px;
}

.btn-add-tool {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.btn-add-tool:hover:not(:disabled) {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.btn-clear-tools {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-clear-tools:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* Steps Section */
.steps-section {
  margin-bottom: 20px;
}

.steps-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.steps-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-count-badge {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.error-badge {
  font-size: 10px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  display: flex;
  align-items: center;
  gap: 2px;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #ef4444;
  margin-top: 4px;
  padding: 4px 8px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 4px;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.steps-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-item {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 16px;
  background: rgba(102, 126, 234, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.step-number {
  font-weight: 600;
  color: #667eea;
  font-size: 11px;
  min-width: 20px;
}

.step-actions {
  display: flex;
  gap: 4px;
}

.step-content {
  padding: 16px;
}

.agent-selector {
  display: flex;
  gap: 8px;
  align-items: center;
}

.agent-select {
  flex: 1;
}

.btn-add-step {
  padding: 6px 8px;
  min-width: auto;
}

/* Empty State */
.empty-steps {
  text-align: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.6);
}

.empty-icon {
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 12px;
}

/* JSON Preview */
.json-preview {
  margin-bottom: 16px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 6px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.json-code {
  padding: 12px;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  background: transparent;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.editor-footer {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

/* Button Styles */
.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transform: translateY(-1px);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 11px;
}

.btn-xs {
  padding: 2px 4px;
  font-size: 10px;
  min-width: auto;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* Tool Selection Modal Styles */
.tool-selection-content {
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
}

.tool-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-container {
  flex: 1;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.sort-container {
  min-width: 140px;
}

.sort-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
}

.tool-summary {
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.summary-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.tool-group {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}

.tool-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-group-header:hover {
  background: rgba(255, 255, 255, 0.08);
}

.tool-group-header.collapsed {
  border-bottom: none;
}

.group-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.collapse-icon {
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s;
}

.group-icon {
  color: rgba(255, 255, 255, 0.8);
}

.group-name {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.group-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.group-actions {
  display: flex;
  align-items: center;
}

.group-enable-all {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.group-enable-checkbox {
  cursor: pointer;
}

.tool-group-content {
  max-height: 200px;
  overflow-y: auto;
  transition: all 0.3s;
}

.tool-group-content.collapsed {
  max-height: 0;
  overflow: hidden;
}

.tool-selection-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: background-color 0.3s;
}

.tool-selection-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.tool-selection-item:last-child {
  border-bottom: none;
}

.tool-info {
  flex: 1;
}

.tool-selection-name {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
}

.tool-selection-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  line-height: 1.4;
}

.tool-actions {
  margin-left: 12px;
}

.tool-enable-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
}

.tool-enable-checkbox {
  opacity: 0;
  width: 0;
  height: 0;
}

.tool-enable-slider {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  transition: all 0.3s;
}

.tool-enable-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s;
}

.tool-enable-checkbox:checked + .tool-enable-slider {
  background: rgba(102, 126, 234, 0.8);
}

.tool-enable-checkbox:checked + .tool-enable-slider:before {
  transform: translateX(20px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}
</style>
