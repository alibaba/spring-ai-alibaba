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
            :class="{ 'error': titleError }"
            :placeholder="$t('sidebar.titlePlaceholder')"
          />
          <!-- Inline validation message for title -->
          <div v-if="titleError" class="field-error-message">
            <Icon icon="carbon:warning" width="12" />
            {{ titleError }}
          </div>
        </div>
        
        <!-- Plan Template ID (Read-only) -->
        <div class="form-row">
          <label class="form-label">{{ $t('sidebar.planTemplateId') }}</label>
          <input 
            :value="currentPlanTemplateId" 
            type="text" 
            class="form-input readonly-input"
            readonly
            :placeholder="$t('sidebar.planTemplateIdPlaceholder')"
          />
        </div>
      </div>


      <!-- Steps Editor -->
      <div class="steps-section">
        <div class="steps-header">
          <label class="form-label">{{ $t('sidebar.tasks') }}</label>
          <div class="steps-actions">
            <button 
              @click="addStep" 
              class="btn btn-xs"
              :title="$t('sidebar.addStep')"
            >
              <Icon icon="carbon:add" width="12" />
            </button>
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
              
              <!-- Step Requirement -->
              <div class="form-row">
                <label class="form-label">{{ $t('sidebar.stepRequirement') }}</label>
                <textarea 
                  v-model="step.stepRequirement"
                  class="form-textarea auto-resize"
                  :placeholder="$t('sidebar.stepRequirementPlaceholder')"
                  rows="4"
                  @input="autoResizeTextarea($event)"
                ></textarea>
              </div>
              
               <!-- Terminate Columns -->
               <div class="form-row">
                 <label class="form-label">{{ $t('sidebar.terminateColumns') }}</label>
                 <textarea 
                   v-model="step.terminateColumns"
                   class="form-textarea auto-resize"
                   :placeholder="$t('sidebar.terminateColumnsPlaceholder')"
                   rows="4"
                   @input="autoResizeTextarea($event)"
                 ></textarea>
               </div>


              <!-- Model Name -->
              <div class="form-row">
                <label class="form-label">{{ $t('sidebar.modelName') }}</label>
                <div class="model-selector">
                  <select 
                    v-model="step.modelName" 
                    class="form-select model-select"
                    :disabled="isLoadingModels"
                  >
                    <!-- Loading state -->
                    <option v-if="isLoadingModels" disabled value="">{{ $t('sidebar.loading') }}</option>
                    
                    <!-- Error state -->
                    <option v-else-if="modelsLoadError" disabled value="">{{ $t('sidebar.modelLoadError') }}</option>
                    
                    <!-- Placeholder option -->
                    <option value="" disabled>{{ $t('sidebar.modelNameDescription') }}</option>
                    
                    <!-- Default empty option -->
                    <option value="">{{ $t('sidebar.noModelSelected') }}</option>
                    
                    <!-- Model options -->
                    <option 
                      v-for="model in availableModels" 
                      :key="model.value"
                      :value="model.value"
                      :title="model.label"
                    >
                      {{ model.label }}
                    </option>
                  </select>
                  
                  <!-- Error refresh button -->
                  <button 
                    v-if="modelsLoadError"
                    @click="loadAvailableModels" 
                    class="btn btn-sm btn-danger"
                    :title="$t('sidebar.retryLoadModels')"
                  >
                    <Icon icon="carbon:warning" width="14" />
                    {{ $t('sidebar.retry') }}
                  </button>
                </div>
                
                <!-- Error message -->
                <div v-if="modelsLoadError" class="error-message">
                  <Icon icon="carbon:warning" width="12" />
                  {{ modelsLoadError }}
                </div>
              </div>
              
              <!-- Tool Selection -->
              <div class="form-row">
                <AssignedTools
                  :title="$t('sidebar.selectedTools')"
                  :selected-tool-ids="step.selectedToolKeys"
                  :available-tools="sidebarStore.availableTools"
                  :add-button-text="$t('sidebar.addRemoveTools')"
                  :empty-text="$t('sidebar.noTools')"
                  :use-grid-layout="true"
                  @add-tools="showToolSelectionModal(index)"
                  @tools-filtered="(filteredTools) => handleToolsFiltered(index, filteredTools)"
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
            class="btn btn-primary"
            @click="handleSave"
            :disabled="isGenerating || isExecuting"
          >
            <Icon icon="carbon:save" width="14" />
                Save
          </button>
        </div>
      </div>
    </div>

    <!-- Tool Selection Modal -->
    <ToolSelectionModal
      v-model="showToolModal"
      :tools="sidebarStore.availableTools"
      :selected-tool-ids="currentStepIndex >= 0 ? parsedData.steps[currentStepIndex]?.selectedToolKeys || [] : []"
      @confirm="handleToolSelectionConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useJsonEditor, type JsonEditorProps } from './json-editor-logic'
import { ref, watch, onMounted } from 'vue'
import ToolSelectionModal from '@/components/tool-selection-modal/ToolSelectionModal.vue'
import AssignedTools from '@/components/shared/AssignedTools.vue'
import { ConfigApiService, type ModelOption } from '@/api/config-api-service'
import { sidebarStore } from '@/stores/sidebar'

// Props
const props = withDefaults(defineProps<JsonEditorProps>(), {
  hiddenFields: () => [],
  currentPlanTemplateId: ''
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
  formattedJsonOutput,
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

// Error state
const planTypeError = ref<string | null>(null)
const titleError = ref<string>('')

// Model selection state
const availableModels = ref<ModelOption[]>([])
const isLoadingModels = ref(false)
const modelsLoadError = ref<string>('')

// Tool selection state - use sidebar store's availableTools
const showToolModal = ref(false)
const currentStepIndex = ref<number>(-1)


// Load available models
const loadAvailableModels = async () => {
  if (isLoadingModels.value) return
  
  isLoadingModels.value = true
  modelsLoadError.value = ''
  
  try {
    const response = await ConfigApiService.getAvailableModels()
    if (response && response.options) {
      availableModels.value = response.options
    } else {
      availableModels.value = []
    }
  } catch (error) {
    console.error('Failed to load models:', error)
    modelsLoadError.value = error instanceof Error ? error.message : 'Failed to load models'
    availableModels.value = []
  } finally {
    isLoadingModels.value = false
  }
}

// Available tools are now loaded from sidebar store

// Tool selection functions
const showToolSelectionModal = (stepIndex: number) => {
  currentStepIndex.value = stepIndex
  showToolModal.value = true
  console.log('[JsonEditorV2] Available tools from store:', sidebarStore.availableTools)
}

const handleToolSelectionConfirm = (selectedToolIds: string[]) => {
  if (currentStepIndex.value >= 0 && currentStepIndex.value < parsedData.steps.length) {
    // Update the specific step's selected tool keys
    parsedData.steps[currentStepIndex.value].selectedToolKeys = [...selectedToolIds]
  }
  showToolModal.value = false
  currentStepIndex.value = -1
}

const handleToolsFiltered = (stepIndex: number, filteredTools: string[]) => {
  if (stepIndex >= 0 && stepIndex < parsedData.steps.length) {
    // Update the step's selected tool keys with filtered tools
    parsedData.steps[stepIndex].selectedToolKeys = [...filteredTools]
  }
}



// Initialize parsedData with default structure
const initializeParsedData = () => {
  try {
    // Clear any previous errors
    planTypeError.value = null
    
    // Initialize with default structure if not exists
    if (!parsedData.title) {
      parsedData.title = ''
    }
    if (!parsedData.steps) {
      parsedData.steps = []
    }
    parsedData.directResponse = false // Always false for dynamic agent planning

  } catch (error) {
    const errorMessage = `Failed to initialize JsonEditorV2: ${error instanceof Error ? error.message : 'Unknown error'}`
    planTypeError.value = errorMessage
    console.error(errorMessage, error)
  }
}

// Watch for parsedData changes to validate structure
watch(() => parsedData, (newData) => {
  try {
    // Soft validation for title - show warning but don't block the form
    if (!newData.title || !newData.title.trim()) {
      titleError.value = 'Title is required field'
    } else {
      titleError.value = ''
    }
    
    // Clear any structural errors
    planTypeError.value = null
  } catch (error) {
    planTypeError.value = `Invalid data structure: ${error instanceof Error ? error.message : 'Unknown error'}`
    titleError.value = ''
  }
}, { immediate: true, deep: true })



// Initialize on mount
onMounted(() => {
  initializeParsedData()
  loadAvailableModels()
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

.plan-basic-info {
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

/* Error state for form inputs */
.form-input.error,
.form-select.error,
.form-textarea.error {
  border-color: #ef4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

/* Field error message */
.field-error-message {
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

.readonly-input {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
  cursor: not-allowed;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.readonly-input:focus {
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: none;
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

/* Model Selector Styles */
.model-selector {
  display: flex;
  gap: 8px;
  align-items: center;
}

.model-select {
  flex: 1;
}



.tool-keys-display {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 32px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
}

.tool-key-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-key-input {
  flex: 1;
  font-size: 10px;
}

.remove-tool-key-btn {
  width: 20px;
  height: 20px;
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

.remove-tool-key-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.no-tool-keys {
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  font-style: italic;
  text-align: center;
  padding: 8px;
}

.btn-add-tool-key {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  align-self: flex-start;
}

.btn-add-tool-key:hover:not(:disabled) {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

</style>
