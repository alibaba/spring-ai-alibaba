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
      <h2>{{ t('config.modelConfig.title') }}</h2>
    </template>

    <template #actions>
      <button class="action-btn" @click="handleImport">
        <Icon icon="carbon:upload" />
        {{ t('config.modelConfig.import') }}
      </button>
      <button class="action-btn" @click="handleExport" :disabled="!selectedModel">
        <Icon icon="carbon:download" />
        {{ t('config.modelConfig.export') }}
      </button>
    </template>

    <div class="model-layout">
      <!-- Model list -->
      <div class="model-list">
        <div class="list-header">
          <h3>{{ t('config.modelConfig.configuredModels') }}</h3>
          <span class="model-count">({{ models.length }})</span>
        </div>

        <div class="models-container" v-if="!loading">
          <div
            v-for="model in models"
            :key="model.id"
            class="model-card"
            :class="{ active: selectedModel?.id === model.id }"
            @click="selectModel(model)"
          >
            <div class="model-card-header">
              <span class="model-name">{{ model.modelName }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p class="model-desc">{{ model.modelDescription }}</p>
            <div class="model-type" v-if="model.type">
              <span class="model-tag">
                {{ model.type }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="loading-icon" />
          {{ t('common.loading') }}
        </div>

        <div v-if="!loading && models.length === 0" class="empty-state">
          <Icon icon="carbon:bot" class="empty-icon" />
          <p>{{ t('config.modelConfig.noModel') }}</p>
        </div>

        <button class="add-btn" @click="showAddModelModal">
          <Icon icon="carbon:add" />
          {{ t('config.modelConfig.createNew') }}
        </button>
      </div>

      <!-- Model details -->
      <div class="model-detail" v-if="selectedModel">
        <div class="detail-header">
          <h3>{{ selectedModel.modelName }}</h3>
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
          <label>{{ t('config.modelConfig.type') }} <span class="required">*</span></label>
          <CustomSelect
            v-model="selectedModel.type"
            :options="modelTypes.map(type => ({ id: type, name: type }))"
            :placeholder="t('config.modelConfig.typePlaceholder')"
            :dropdown-title="t('config.modelConfig.typePlaceholder')"
            icon="carbon:types"
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.modelConfig.baseUrl') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="selectedModel.baseUrl"
            :placeholder="t('config.modelConfig.baseUrlPlaceholder')"
            required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.modelConfig.headers') }} </label>
          <input
              type="text"
              v-model="selectedHeadersJson"
              :placeholder="t('config.modelConfig.headersPlaceholder')"
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.modelConfig.apiKey') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="selectedModel.apiKey"
            :placeholder="t('config.modelConfig.apiKeyPlaceholder')"
            required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.modelConfig.modelName') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="selectedModel.modelName"
            :placeholder="t('config.modelConfig.modelNamePlaceholder')"
            required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.modelConfig.description') }} <span class="required">*</span></label>
          <textarea
            v-model="selectedModel.modelDescription"
            rows="3"
            :placeholder="t('config.modelConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.modelConfig.selectModelHint') }}</p>
      </div>
    </div>

    <!-- New Model modal -->
    <Modal v-model="showModal" :title="t('config.modelConfig.newModel')" @confirm="handleAddModel">
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.modelConfig.type') }} <span class="required">*</span></label>
          <CustomSelect
            v-model="newModel.type"
            :options="modelTypes.map(type => ({ id: type, name: type }))"
            :placeholder="t('config.modelConfig.typePlaceholder')"
            :dropdown-title="t('config.modelConfig.typePlaceholder')"
            icon="carbon:types"
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.baseUrl') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newModel.baseUrl"
            :placeholder="t('config.modelConfig.baseUrlPlaceholder')"
            required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.headers') }} </label>
          <input
              type="text"
              v-model="newHeadersJson"
              :placeholder="t('config.modelConfig.headersPlaceholder')"
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.apiKey') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newModel.apiKey"
            :placeholder="t('config.modelConfig.apiKeyPlaceholder')"
            required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.modelName') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newModel.modelName"
            :placeholder="t('config.modelConfig.modelNamePlaceholder')"
            required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.description') }} <span class="required">*</span></label>
          <textarea
            v-model="newModel.modelDescription"
            rows="3"
            :placeholder="t('config.modelConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- Delete confirmation modal -->
    <Modal v-model="showDeleteModal" title="Delete confirmation">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>
          {{ t('config.modelConfig.deleteConfirmText') }}
          <strong>{{ selectedModel?.modelName }}</strong> {{ t('common.confirm') }}？
        </p>
        <p class="warning-text">{{ t('config.modelConfig.deleteWarning') }}</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">
          {{ t('common.cancel') }}
        </button>
        <button class="confirm-btn danger" @click="handleDelete">{{ t('common.delete') }}</button>
      </template>
    </Modal>

    <!-- Error toast -->
    <div v-if="error" class="error-toast" @click="error = ''">
      <Icon icon="carbon:error" />
      {{ error }}
    </div>

    <!-- Success toast -->
    <div v-if="success" class="success-toast" @click="success = ''">
      <Icon icon="carbon:checkmark" />
      {{ success }}
    </div>
  </ConfigPanel>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted,computed } from 'vue'
// 其余代码保持不变
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import ConfigPanel from './components/configPanel.vue'
import Modal from '@/components/modal/index.vue'
import CustomSelect from '@/components/select/index.vue'
import { ModelApiService, type Model } from '@/api/model-api-service'

// Internationalization
const { t } = useI18n()

// Reactive data
const loading = ref(false)
const error = ref('')
const success = ref('')
const models = reactive<Model[]>([])
const modelTypes = reactive<string[]>([])
const selectedModel = ref<Model | null>(null)
const showModal = ref(false)
const showDeleteModal = ref(false)

const selectedHeadersJson = computed({
  get() {
    if (!selectedModel.value?.headers) return ''
    return JSON.stringify(selectedModel.value.headers, null, 2)
  },
  set(val) {
      if (!selectedModel.value) return
      // 空值处理
      selectedModel.value.headers = val.trim() ? JSON.parse(val) : null
    }
})

const newHeadersJson = computed({
  get() {
    return newModel.headers ? JSON.stringify(newModel.headers, null, 2) : ''
  },
  set(val) {
      newModel.headers = val.trim() ? JSON.parse(val) : null
    }
})

// New Model form data
const newModel = reactive<Omit<Model, 'id'>>({
  baseUrl:  '',
  headers:  null,
  apiKey:  '',
  modelName:  '',
  modelDescription:  '',
  type:  '',
})

// Message toast
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
    // Load the Model list and available types in parallel
    const [loadedModels, loadedTypes] = await Promise.all([
      ModelApiService.getAllModels(),
      ModelApiService.getAllTypes(),
    ])
    const normalizedModels = loadedModels.map(model => ({
      ...model,
    }))

    models.splice(0, models.length, ...normalizedModels)
    modelTypes.splice(0, modelTypes.length, ...loadedTypes)

    // Select the first Model
    if (normalizedModels.length > 0) {
      await selectModel(normalizedModels[0])
    }
  } catch (err: any) {
    console.error('加载数据失败:', err)
    showMessage(t('config.modelConfig.loadDataFailed') + ': ' + err.message, 'error')
  } finally {
    loading.value = false
  }
}

// Select Model
const selectModel = async (model: Model) => {
  try {
    // Load the detailed information
    const detailedModel = await ModelApiService.getModelById(model.id)
    selectedModel.value = {
      ...detailedModel,
    }
  } catch (err: any) {
    console.error('加载Model详情失败:', err)
    showMessage(t('config.modelConfig.loadDetailsFailed') + ': ' + err.message, 'error')
    // Use basic information as a fallback
    selectedModel.value = {
      ...model,
    }
  }
}

// Show the new Model modal
const showAddModelModal = () => {
  newModel.baseUrl = ''
  newModel.headers = null
  newModel.apiKey = ''
  newModel.modelName = ''
  newModel.modelDescription = ''
  newModel.type = ''
  showModal.value = true
}

// Create new Model
const handleAddModel = async () => {
  if (!newModel.modelName.trim() || !newModel.modelDescription.trim()) {
    showMessage(t('config.modelConfig.requiredFields'), 'error')
    return
  }

  try {
    const modelData: Omit<Model, 'id'> = {
      baseUrl: newModel.baseUrl.trim(),
      headers: newModel.headers,
      apiKey: newModel.apiKey.trim(),
      modelName: newModel.modelName.trim(),
      modelDescription: newModel.modelDescription.trim(),
      type: newModel.type.trim(),
    }

    const createdModel = await ModelApiService.createModel(modelData)
    models.push(createdModel)
    selectedModel.value = createdModel
    showModal.value = false
    showMessage(t('config.modelConfig.createSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.createFailed') + ': ' + err.message, 'error')
  }
}

// Save Model
const handleSave = async () => {
  if (!selectedModel.value) return

  if (!selectedModel.value.modelName.trim() || !selectedModel.value.modelDescription.trim()) {
    showMessage(t('config.modelConfig.requiredFields'), 'error')
    return
  }

  try {
    const savedModel = await ModelApiService.updateModel(
      selectedModel.value.id,
      selectedModel.value
    )

    // Update the data in the local list
    const index = models.findIndex(a => a.id === savedModel.id)
    if (index !== -1) {
      models[index] = savedModel
    }
    selectedModel.value = savedModel
    showMessage(t('config.modelConfig.saveSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.saveFailed') + ': ' + err.message, 'error')
  }
}

// Show the delete confirmation modal
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// Delete Model
const handleDelete = async () => {
  if (!selectedModel.value) return

  try {
    await ModelApiService.deleteModel(selectedModel.value.id)

    // Remove from the local list
    const index = models.findIndex(a => a.id === selectedModel.value!.id)
    if (index !== -1) {
      models.splice(index, 1)
    }

    // Select the next Model or clear the selection
    selectedModel.value = models.length > 0 ? models[0] : null
    showDeleteModal.value = false
    showMessage(t('config.modelConfig.deleteSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.deleteFailed') + ': ' + err.message, 'error')
  }
}

// Import Model
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
          const modelData = JSON.parse(e.target?.result as string)
          // Basic verification
          if (!modelData.modelName || !modelData.modelDescription) {
            throw new Error(t('config.modelConfig.invalidFormat'))
          }

          // Remove the id field and let the backend assign a new id
          const { id: _id, ...importData } = modelData
          const savedModel = await ModelApiService.createModel(importData)
          models.push(savedModel)
          selectedModel.value = savedModel
          showMessage(t('config.modelConfig.importSuccess'), 'success')
        } catch (err: any) {
          showMessage(t('config.modelConfig.importFailed') + ': ' + err.message, 'error')
        }
      }
      reader.readAsText(file)
    }
  }
  input.click()
}

// Export Model
const handleExport = () => {
  if (!selectedModel.value) return

  try {
    const jsonString = JSON.stringify(selectedModel.value, null, 2)
    const dataBlob = new Blob([jsonString], { type: 'application/json' })
    const url = URL.createObjectURL(dataBlob)
    const link = document.createElement('a')
    link.href = url
    link.download = `model-${selectedModel.value.modelName}-${new Date().toISOString().split('T')[0]}.json`
    link.click()
    URL.revokeObjectURL(url)
    showMessage(t('config.modelConfig.exportSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.exportFailed') + ': ' + err.message, 'error')
  }
}

// Load data when the component is mounted
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.model-layout {
  display: flex;
  gap: 30px;
  flex: 1;
  min-height: 0;
}

.model-list {
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

.model-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.models-container {
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

.model-card {
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

.model-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.model-name {
  font-weight: 500;
  font-size: 16px;
}

.model-desc {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 12px;
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

.model-detail {
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

/* 弹窗样式 */
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

.model-type {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.model-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(181, 102, 234, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: #a8b3ff;
}

/* 提示消息 */
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
</style>
