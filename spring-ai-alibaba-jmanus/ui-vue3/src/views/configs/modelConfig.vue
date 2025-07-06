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
  <div class="config-panel">
    <div class="panel-header">
      <h2>{{ t('config.modelConfig.title') }}</h2>
      <div class="panel-actions">
        <button class="action-btn" @click="handleImport">
          <Icon icon="carbon:upload" />
          {{ t('config.modelConfig.import') }}
        </button>
        <button class="action-btn" @click="handleExport" :disabled="!selectedModel">
          <Icon icon="carbon:download" />
          {{ t('config.modelConfig.export') }}
        </button>
      </div>
    </div>

    <div class="model-layout">
      <!-- Model列表 -->
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
              <span class="model-name">{{ model.name }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p class="model-desc">{{ model.description }}</p>
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

      <!-- Model详情 -->
      <div class="model-detail" v-if="selectedModel">
        <div class="detail-header">
          <h3>{{ selectedModel.name }}</h3>
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
          <label>{{ t('config.modelConfig.modelName') }} <span class="required">*</span></label>
          <input 
            type="text" 
            v-model="selectedModel.name"
            :placeholder="t('config.modelConfig.modelNamePlaceholder')"
            required
          />
        </div>
        
        <div class="form-item">
          <label>{{ t('config.modelConfig.description') }} <span class="required">*</span></label>
          <textarea 
            v-model="selectedModel.description"
            rows="3"
            :placeholder="t('config.modelConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>

      </div>

      <!-- 空状态 -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.modelConfig.selectModelHint') }}</p>
      </div>
    </div>

    <!-- 新建Model弹窗 -->
    <Modal v-model="showModal" :title="t('config.modelConfig.newModel')" @confirm="handleAddModel">
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.modelConfig.modelName') }} <span class="required">*</span></label>
          <input 
            type="text" 
            v-model="newModel.name"
            :placeholder="t('config.modelConfig.modelNamePlaceholder')"
            required 
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.modelConfig.description') }} <span class="required">*</span></label>
          <textarea
            v-model="newModel.description"
            rows="3"
            :placeholder="t('config.modelConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteModal" title="删除确认">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>{{ t('config.modelConfig.deleteConfirmText') }} <strong>{{ selectedModel?.name }}</strong> {{ t('common.confirm') }}？</p>
        <p class="warning-text">{{ t('config.modelConfig.deleteWarning') }}</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">{{ t('common.cancel') }}</button>
        <button class="confirm-btn danger" @click="handleDelete">{{ t('common.delete') }}</button>
      </template>
    </Modal>

    <!-- 错误提示 -->
    <div v-if="error" class="error-toast" @click="error = ''">
      <Icon icon="carbon:error" />
      {{ error }}
    </div>

    <!-- 成功提示 -->
    <div v-if="success" class="success-toast" @click="success = ''">
      <Icon icon="carbon:checkmark" />
      {{ success }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal/index.vue'
import { ModelApiService, type Model } from '@/api/model-api-service'

// 国际化
const { t } = useI18n()

// 响应式数据
const loading = ref(false)
const error = ref('')
const success = ref('')
const models = reactive<Model[]>([])
const selectedModel = ref<Model | null>(null)
const showModal = ref(false)
const showDeleteModal = ref(false)

// 新建Model表单数据
const newModel = reactive<Omit<Model, 'id'>>({
  name: '',
  description: ''
})

// 消息提示
const showMessage = (msg: string, type: 'success' | 'error') => {
  if (type === 'success') {
    success.value = msg
    setTimeout(() => { success.value = '' }, 3000)
  } else {
    error.value = msg
    setTimeout(() => { error.value = '' }, 5000)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 并行加载Model列表和可用工具
    const [loadedModels] = await Promise.all([
      ModelApiService.getAllModels()
    ])

    const normalizedModels = loadedModels.map(model => ({
      ...model
    }))
    
    models.splice(0, models.length, ...normalizedModels)
    
    // 选中第一个Model
    if (normalizedModels.length > 0) {
      await selectModel(normalizedModels[0])
    }
  } catch (err: any) {
    console.error('加载数据失败:', err)
    showMessage(t('config.modelConfig.loadDataFailed') + ': ' + err.message, 'error')
    
    const demoModels = [
      {
        id: 'demo-1',
        name: '通用助手',
        description: '一个能够处理各种任务的智能助手',
      },
      {
        id: 'demo-2',
        name: '数据分析师',
        description: '专门用于数据分析和可视化的Model',
      }
    ]
    models.splice(0, models.length, ...demoModels)
    
    if (demoModels.length > 0) {
      selectedModel.value = demoModels[0]
    }
  } finally {
    loading.value = false
  }
}

// 选择Model
const selectModel = async (model: Model) => {
  if (!model) return
  
  try {
    // 加载详细信息
    const detailedModel = await ModelApiService.getModelById(model.id)
    selectedModel.value = {
      ...detailedModel
    }
  } catch (err: any) {
    console.error('加载Model详情失败:', err)
    showMessage(t('config.modelConfig.loadDetailsFailed') + ': ' + err.message, 'error')
    // 使用基本信息作为后备
    selectedModel.value = {
      ...model
    }
  }
}

// 显示新建Model弹窗
const showAddModelModal = () => {
  newModel.name = ''
  newModel.description = ''
  showModal.value = true
}

// 创建新Model
const handleAddModel = async () => {
  if (!newModel.name.trim() || !newModel.description.trim()) {
    showMessage(t('config.modelConfig.requiredFields'), 'error')
    return
  }

  try {
    const modelData: Omit<Model, 'id'> = {
      name: newModel.name.trim(),
      description: newModel.description.trim()
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

// 保存Model
const handleSave = async () => {
  if (!selectedModel.value) return

  if (!selectedModel.value.name.trim() || !selectedModel.value.description.trim()) {
    showMessage(t('config.modelConfig.requiredFields'), 'error')
    return
  }

  try {
    const savedModel = await ModelApiService.updateModel(selectedModel.value.id, selectedModel.value)
    
    // 更新本地列表中的数据
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

// 显示删除确认
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 删除Model
const handleDelete = async () => {
  if (!selectedModel.value) return

  try {
    await ModelApiService.deleteModel(selectedModel.value.id)
    
    // 从列表中移除
    const index = models.findIndex(a => a.id === selectedModel.value!.id)
    if (index !== -1) {
      models.splice(index, 1)
    }

    // 选择其他Model或清除选中状态
    selectedModel.value = models.length > 0 ? models[0] : null
    showDeleteModal.value = false
    showMessage(t('config.modelConfig.deleteSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.deleteFailed') + ': ' + err.message, 'error')
  }
}

// 导入Model
const handleImport = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = (event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = async (e) => {
        try {
          const modelData = JSON.parse(e.target?.result as string)
          // 基本验证
          if (!modelData.name || !modelData.description) {
            throw new Error(t('config.modelConfig.invalidFormat'))
          }
          
          // 移除id字段，让后端分配新的id
          const { id, ...importData } = modelData
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

// 导出Model
const handleExport = () => {
  if (!selectedModel.value) return

  try {
    const jsonString = JSON.stringify(selectedModel.value, null, 2)
    const dataBlob = new Blob([jsonString], { type: 'application/json' })
    const url = URL.createObjectURL(dataBlob)
    const link = document.createElement('a')
    link.href = url
    link.download = `model-${selectedModel.value.name}-${new Date().toISOString().split('T')[0]}.json`
    link.click()
    URL.revokeObjectURL(url)
    showMessage(t('config.modelConfig.exportSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.modelConfig.exportFailed') + ': ' + err.message, 'error')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.config-panel {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.panel-actions {
  display: flex;
  gap: 12px;
}

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
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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

.model-tools {
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

.tools-section {
  h4 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  span {
    font-weight: 500;
    color: rgba(255, 255, 255, 0.8);
  }
}

.tools-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  transition: all 0.3s ease;
  
  &.assigned {
    border-color: rgba(102, 126, 234, 0.3);
    background: rgba(102, 126, 234, 0.1);
  }
}

.tool-info {
  flex: 1;
  
  .tool-name {
    display: block;
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .tool-desc {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.3;
  }
}

.no-tools {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
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

.confirm-btn, .cancel-btn {
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

/* 提示消息 */
.error-toast, .success-toast {
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
