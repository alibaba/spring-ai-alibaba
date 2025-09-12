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
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click="handleOverlayClick">
        <div class="modal-container" @click.stop>
          <div class="modal-header">
            <h3>{{ $t('cronTask.taskDetail') }}</h3>
            <div class="header-actions">
              <div class="status-switch">
                <span class="status-label">{{ $t('cronTask.taskStatus') }}</span>
                <label class="toggle-switch">
                  <input type="checkbox" :checked="formData.status === 0" @change="formData.status = formData.status === 0 ? 1 : 0">
                  <span class="toggle-slider"></span>
                </label>
              </div>
              <button class="close-btn" @click="$emit('update:modelValue', false)">
                <Icon icon="carbon:close" />
              </button>
            </div>
          </div>
          <div class="modal-content">
            <form @submit.prevent="handleSave" class="task-form">
              <!-- Task Name -->
              <div class="form-group">
                <label class="form-label">{{ $t('cronTask.taskName') }}</label>
                <input
                  v-model="formData.cronName"
                  type="text"
                  class="form-input"
                  :placeholder="$t('cronTask.taskNamePlaceholder')"
                  required
                />
              </div>

              <!-- Cron Expression -->
              <div class="form-group">
                <label class="form-label">{{ $t('cronTask.cronExpression') }}</label>
                <input
                  v-model="formData.cronTime"
                  type="text"
                  class="form-input"
                  :placeholder="$t('cronTask.cronExpressionPlaceholder')"
                  required
                />
                <div class="form-help">
                  {{ $t('cronTask.cronExpressionHelp') }}
                </div>
              </div>

              <!-- Task Description -->
              <div class="form-group">
                <label class="form-label">{{ $t('cronTask.taskDescription') }}</label>
                <textarea
                  v-model="formData.planDesc"
                  class="form-textarea"
                  :placeholder="$t('cronTask.taskDescriptionPlaceholder')"
                  rows="4"
                  required
                ></textarea>
              </div>

              <!-- Plan Template Association -->
              <div class="form-group">
                <label class="form-label">{{ $t('cronTask.planTemplate') }}</label>
                <div class="template-toggle">
                  <button
                    type="button"
                    :class="['template-btn', formData.linkTemplate ? 'active' : '']"
                    @click="formData.linkTemplate = true"
                  >
                    <Icon icon="carbon:checkmark" />
                    {{ $t('cronTask.linkTemplate') }}
                  </button>
                  <button
                    type="button"
                    :class="['template-btn', !formData.linkTemplate ? 'active' : '']"
                    @click="handleDisableLinkTemplate"
                  >
                    <Icon icon="carbon:close" />
                    {{ $t('cronTask.noTemplate') }}
                  </button>
                </div>
                <div v-if="formData.linkTemplate" class="template-selector">
                  <select
                    v-model="formData.templateId"
                    class="form-select"
                  >
                    <option value="">{{ $t('cronTask.selectTemplate') }}</option>
                    <option v-for="template in templates" :key="template.id" :value="template.id">
                      {{ template.name }}
                    </option>
                  </select>
                  <div class="form-help">
                    {{ $t('cronTask.templateHelpText') }}
                  </div>
                </div>
              </div>

              <!-- Task Information (Read-only) -->
              <div v-if="task?.createTime" class="form-group">
                <div class="time-info">
                  <span class="time-label">{{ $t('cronTask.createTime') }}:</span>
                  <span class="time-value">{{ formatTime(task.createTime) }}</span>
                </div>
              </div>

              <div v-if="task?.updateTime" class="form-group">
                <div class="time-info">
                  <span class="time-label">{{ $t('cronTask.updateTime') }}:</span>
                  <span class="time-value">{{ formatTime(task.updateTime) }}</span>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="cancel-btn" @click="$emit('update:modelValue', false)">
              {{ $t('common.cancel') }}
            </button>
            <button type="button" class="save-btn" @click="handleSave" :disabled="saving">
              <Icon v-if="saving" icon="carbon:loading" class="loading-icon" />
              {{ props.task?.id ? $t('common.save') : $t('common.create') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import type { CronConfig } from '@/types/cron-task'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate } from '@/types/plan-template'
import { CronTaskUtils } from '@/utils/cron-task-utils'

const props = defineProps<{
  modelValue: boolean
  task: CronConfig | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'save': [task: CronConfig]
}>()

// State variables
const saving = ref(false)
const templates = ref<Array<{id: string, name: string}>>([])

// Form data
const formData = ref<CronConfig>({
  cronName: '',
  cronTime: '',
  planDesc: '',
  status: 1,
  linkTemplate: false,
  templateId: '',
  planTemplateId: ''
})

/**
 * Get template list from API
 */
const fetchTemplates = async () => {
  try {
    const response = await PlanActApiService.getAllPlanTemplates()
    if (response && response.templates) {
      templates.value = response.templates.map((template: PlanTemplate) => ({
        id: template.id,
        name: template.title || 'Unnamed Template'
      }))
    }
  } catch (error) {
    console.error('Failed to get template list:', error)
  }
}

// Handle ESC key to close modal
const handleEscKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && props.modelValue) {
    emit('update:modelValue', false)
  }
}

onMounted(() => {
  fetchTemplates()
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
})

/**
 * Handle click on modal overlay area
 */
const handleOverlayClick = (e: MouseEvent) => {
  if (e.target === e.currentTarget) {
    emit('update:modelValue', false)
  }
}

/**
 * Disable template linking
 */
const handleDisableLinkTemplate = () => {
  formData.value.linkTemplate = false
  formData.value.templateId = ''
  formData.value.planTemplateId = ''
}

/**
 * Validate form data
 */
const validateForm = (): boolean => {
  // Required field validation
  if (!formData.value.cronName.trim()) {
    alert('Task name cannot be empty')
    return false
  }

  if (!formData.value.cronTime.trim()) {
    alert('Cron expression cannot be empty')
    return false
  }

  // Validate Cron expression format
  if (!CronTaskUtils.validateCronExpression(formData.value.cronTime)) {
    alert('Invalid Cron expression format, should be 5-6 parts separated by spaces')
    return false
  }

  if (!formData.value.planDesc.trim()) {
    alert('Task description cannot be empty')
    return false
  }

  // If template linking is selected, template ID cannot be empty
  if (formData.value.linkTemplate && !formData.value.templateId) {
    alert('Please select a plan template')
    return false
  }

  return true
}

/**
 * Format time
 */
const formatTime = (timeString: string) => {
  return CronTaskUtils.formatTime(timeString)
}

/**
 * Save task
 */
const handleSave = async () => {
  if (!validateForm()) return

  saving.value = true
  try {
    // Prepare data to save
    const taskToSave: CronConfig = {
      ...formData.value,
      ...(props.task?.id !== undefined && { id: props.task.id }),
      cronName: formData.value.cronName.trim(),
      cronTime: formData.value.cronTime.trim(),
      planDesc: formData.value.planDesc.trim(),
      status: formData.value.status,
      planTemplateId: formData.value.linkTemplate ? formData.value.templateId || '' : ''
    }

    // Trigger save event
    emit('save', taskToSave)
  } finally {
    saving.value = false
  }
}

// Watch for task changes, update form data
watch(
  () => props.task,
  (newTask) => {
    if (newTask) {
      // Unified handling of template ID field
      const templateId = newTask.templateId || newTask.planTemplateId || ''

      formData.value = {
        cronName: newTask.cronName || '',
        cronTime: newTask.cronTime || '',
        planDesc: newTask.planDesc || '',
        status: newTask.status ?? 1,
        linkTemplate: !!templateId,
        templateId: templateId,
        planTemplateId: templateId
      }
    } else {
      // Reset form
      formData.value = {
        cronName: '',
        cronTime: '',
        planDesc: '',
        status: 1,
        linkTemplate: false,
        templateId: '',
        planTemplateId: ''
      }
    }
  },
  { immediate: true }
)

// Reset form when modal closes
watch(
  () => props.modelValue,
  (newValue) => {
    if (!newValue) {
      formData.value = {
        cronName: '',
        cronTime: '',
        planDesc: '',
        status: 1,
        linkTemplate: false,
        templateId: '',
        planTemplateId: ''
      }
    }
  }
)
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.modal-container {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  color: rgba(255, 255, 255, 0.9);
}

.modal-content {
  padding: 24px;
}

.task-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.form-input,
.form-textarea,
.form-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-help {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.time-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}

.time-value {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.template-toggle {
  display: flex;
  gap: 8px;
}

.template-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

.template-btn.active {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
  color: #667eea;
}

.template-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.template-btn.active:hover {
  background: rgba(102, 126, 234, 0.3);
}

.template-selector {
  margin-top: 8px;
}

.form-select {
  width: 100%;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='rgba(255, 255, 255, 0.5)' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.save-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.save-btn {
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: #667eea;
}

.save-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.3);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Transition animations */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

/* Toggle Switch Styles */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.2);
  transition: .4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: rgba(102, 126, 234, 0.6);
}

input:focus + .toggle-slider {
  box-shadow: 0 0 1px rgba(102, 126, 234, 0.6);
}

input:checked + .toggle-slider:before {
  transform: translateX(26px);
}
</style>

