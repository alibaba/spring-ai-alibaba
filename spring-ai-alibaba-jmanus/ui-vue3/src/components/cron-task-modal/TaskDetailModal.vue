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
            <button class="close-btn" @click="$emit('update:modelValue', false)">
              <Icon icon="carbon:close" />
            </button>
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
                ></textarea>
              </div>

              <!-- Task Status -->
              <div class="form-group">
                <label class="form-label">{{ $t('cronTask.taskStatus') }}</label>
                <div class="status-toggle">
                  <button
                    type="button"
                    :class="['status-btn', formData.status === 0 ? 'active' : '']"
                    @click="formData.status = 0"
                  >
                    <Icon icon="carbon:checkmark" />
                    {{ $t('cronTask.active') }}
                  </button>
                  <button
                    type="button"
                    :class="['status-btn', formData.status === 1 ? 'active' : '']"
                    @click="formData.status = 1"
                  >
                    <Icon icon="carbon:close" />
                    {{ $t('cronTask.inactive') }}
                  </button>
                </div>
              </div>

              <!-- Task Info (Read-only) -->
              <div v-if="task?.createTime" class="form-group">
                <label class="form-label">{{ $t('cronTask.createTime') }}</label>
                <div class="form-info">{{ formatTime(task.createTime) }}</div>
              </div>

              <div v-if="task?.updateTime" class="form-group">
                <label class="form-label">{{ $t('cronTask.updateTime') }}</label>
                <div class="form-info">{{ formatTime(task.updateTime) }}</div>
              </div>


            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="cancel-btn" @click="$emit('update:modelValue', false)">
              {{ $t('common.cancel') }}
            </button>
            <button type="button" class="save-btn" @click="handleSave" :disabled="saving">
              <Icon v-if="saving" icon="carbon:loading" class="loading-icon" />
              {{ $t('common.save') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Icon } from '@iconify/vue'
import type { CronConfig } from '@/types/cron-task'

const props = defineProps<{
  modelValue: boolean
  task: CronConfig | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'save': [task: CronConfig]
}>()

const saving = ref(false)
const formData = ref<CronConfig>({
  cronName: '',
  cronTime: '',
  planDesc: '',
  status: 1,
})

const handleOverlayClick = (e: MouseEvent) => {
  if (e.target === e.currentTarget) {
    emit('update:modelValue', false)
  }
}

const handleSave = async () => {
  if (!formData.value.cronName?.trim() || !formData.value.cronTime?.trim()) {
    return
  }

  saving.value = true
  try {
    const taskToSave: CronConfig = {
      ...formData.value,
      ...(props.task?.id !== undefined && { id: props.task.id }),
      cronName: formData.value.cronName.trim(),
      cronTime: formData.value.cronTime.trim(),
      planDesc: formData.value.planDesc?.trim() || '',
      status: formData.value.status
    }
    emit('save', taskToSave)
  } finally {
    saving.value = false
  }
}

const formatTime = (timeString: string) => {
  return new Date(timeString).toLocaleString()
}

// Watch for task changes to populate form
watch(
  () => props.task,
  (newTask) => {
    if (newTask) {
      formData.value = {
        cronName: newTask.cronName || '',
        cronTime: newTask.cronTime || '',
        planDesc: newTask.planDesc || '',
        status: newTask.status ?? 1,
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
.form-textarea {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus,
.form-textarea:focus {
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

.form-info {
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.status-toggle {
  display: flex;
  gap: 8px;
}

.status-btn {
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

.status-btn.active {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
  color: #667eea;
}

.status-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.status-btn.active:hover {
  background: rgba(102, 126, 234, 0.3);
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


</style>

