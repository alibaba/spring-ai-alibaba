<!--
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by appl.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-textarea {law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <div class="config-section">
    <div class="section-header">
      <Icon icon="carbon:code" width="16" />
      <span>{{ $t('sidebar.jsonTemplate') }}</span>
    </div>
    <!-- Visual JSON Editor -->
    <div class="visual-editor">
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

      <!-- Steps Editor -->
      <div class="steps-section">
        <div class="steps-header">
          <label class="form-label">{{ $t('sidebar.tasks') }}</label>
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
                  <select v-model="step.agentName" class="form-select agent-select">
                    <option value="">{{ $t('sidebar.selectAgent') }}</option>
                    <option value="DEFAULT_AGENT">DEFAULT_AGENT</option>
                    <option value="BROWSER_AGENT">BROWSER_AGENT</option>
                    <option value="TEXT_FILE_AGENT">TEXT_FILE_AGENT</option>
                    <option value="JSX_GENERATOR_AGENT">JSX_GENERATOR_AGENT</option>
                    <option value="FILE_MANAGER_AGENT">FILE_MANAGER_AGENT</option>
                  </select>
                  
                  <!-- Add step button -->
                  <button 
                    @click="addStep" 
                    class="btn btn-sm btn-add-step"
                    :title="$t('sidebar.addStep')"
                  >
                    <Icon icon="carbon:add" width="14" />
                  </button>
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
            v-model="parsedData.planTemplateId" 
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
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useJsonEditor, type JsonEditorProps } from './json-editor-logic'

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

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.form-checkbox {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
}

.checkbox-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}

/* Steps Section */
.steps-section {
  margin-bottom: 20px;
}

/* Plan ID Section */
.plan-id-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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

.btn-add {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.btn-add:hover:not(:disabled) {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* Legacy styles for compatibility */
.json-editor {
  width: 100%;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: white;
  font-size: 11px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  padding: 8px;
  resize: vertical;
  min-height: 200px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: break-word;
  tab-size: 2;
  font-variant-ligatures: none;
}

.json-editor:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.json-editor::placeholder {
  color: rgba(255, 255, 255, 0.4);
}
</style>
