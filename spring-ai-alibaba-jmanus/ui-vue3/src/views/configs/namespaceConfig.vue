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
<!-- 
   No API interface available for now 

   Features:
    - Add, edit and delete namespaces
    - View namespace details
    - Validate name uniqueness
-->
<template>
  <ConfigPanel>
    <template #title>
      <h2>{{ t('config.namespaceConfig.title') }}</h2>
    </template>

    <div class="namespace-layout">
      <!--left namespace list-->
      <div class="namespace-list">
        <div class="list-header">
          <h3>{{ t('config.namespaceConfig.configured') }}</h3>
        </div>

        <div class="namespaces-container" v-if="!loading">
          <div
            v-for="namespace in namespaces"
            :key="namespace.id"
            class="namespace-card"
            :class="{ active: selectedNamespace?.id === namespace.id }"
            @click="selectNamespace(namespace)"
          >
            <div class="namespace-card-header">
              <span class="namespace-name">{{ namespace.name }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <Icon icon="carbon:loading" class="loading-icon" />
        {{ t('common.loading') }}
      </div>

      <div v-if="!loading && namespaces.length === 0" class="empty-state">
        <Icon icon="carbon:bot" class="empty-icon" />
        <p>{{ t('config.namespaceConfig.noPrompts') }}</p>
      </div>

      <button class="add-btn" @click="showAddNamespaceModal">
        <Icon icon="carbon:add" />
        {{ t('config.namespaceConfig.createNew') }}
      </button>
    </div>

    <!-- prompt详情 -->
    <div class="namespace-detail" v-if="selectedNamespace">
      <div class="detail-header">
        <h3>{{ selectedNamespace.name }}</h3>
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
        <label>{{ t('config.namespaceConfig.name') }} <span class="required">*</span></label>
        <input
          :disabled="true"
          type="text"
          v-model="selectedNamespace.name"
          :placeholder="t('config.namespaceConfig.placeholder')"
          required
        />
      </div>
    </div>

    <!-- empty selection -->
    <div v-else class="no-selection">
      <Icon icon="carbon:bot" class="placeholder-icon" />
      <p>{{ t('config.namespaceConfig.selectPromptHint') }}</p>
    </div>

    <!-- new namespace dialog -->
    <Modal
      v-model="showModal"
      :title="t('config.namespaceConfig.newPrompt')"
      @confirm="handleAddNamespace"
    >
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.namespaceConfig.name') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newNamespace.name"
            :placeholder="t('config.namespaceConfig.placeholder')"
            required
          />
        </div>
      </div>
    </Modal>

    <!-- Delete confirmation dialog -->
    <Modal v-model="showDeleteModal" :title="t('config.namespaceConfig.deleteConfirm')">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>
          {{ t('config.namespaceConfig.deleteConfirmText') }}
          <strong>{{ selectedNamespace?.name }}</strong> {{ t('common.confirm') }}？
        </p>
        <p class="warning-text">{{ t('config.namespaceConfig.deleteWarning') }}</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">
          {{ t('common.cancel') }}
        </button>
        <button class="confirm-btn danger" @click="handleDelete">{{ t('common.delete') }}</button>
      </template>
    </Modal>
  </ConfigPanel>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal/index.vue'
import { NamespaceApiService, type Namespace } from '@/api/namespace-api-service'
import { useToast } from '@/plugins/useToast'
import ConfigPanel from './components/configPanel.vue'

type PromptField = string | null | undefined

const { t } = useI18n()
const { success, error } = useToast()

// Reactive data properties
const loading = ref(false)
const namespaces = reactive<Namespace[]>([])
const selectedNamespace = ref<Namespace | null>(null)
const showModal = ref(false)
const showDeleteModal = ref(false)

// default values for creating Namespace fields
const defaultNamespaceValues = {
  name: '',
}

const newNamespace = reactive<Omit<Namespace, 'id'>>({ ...defaultNamespaceValues } as Omit<
  Namespace,
  'id'
>)
// Load namespace list data from API
const loadData = async () => {
  loading.value = true
  try {
    const loadedNamespaces = (await NamespaceApiService.getAllNamespaces()) as Namespace[]

    if (loadedNamespaces.length > 0) {
      await selectNamespace(loadedNamespaces[0])
    }
    namespaces.splice(0, namespaces.length, ...loadedNamespaces)
  } catch (err: any) {
    console.error('加载数据失败:', err)
    error(t('config.namespaceConfig.loadDataFailed') + ': ' + err.message)
  } finally {
    loading.value = false
  }
}
// select namespace
const selectNamespace = async (namespace: Namespace) => {
  try {
    const detailedNamespace = await NamespaceApiService.getNamespaceById(namespace.id)
    selectedNamespace.value = {
      ...detailedNamespace,
    }
  } catch (err: any) {
    console.error('加载Prompt详情失败:', err)
    error(t('config.namespaceConfig.loadDetailsFailed') + ': ' + err.message)
    // base pormpt
    selectedNamespace.value = {
      ...namespace,
    }
  }
}
//
const handleAddNamespace = async () => {
  if (!validateNamespace(newNamespace)) return
  try {
    const namespaceData: Omit<Namespace, 'id'> = {
      ...newNamespace,
    }
    const createdNamespace = await NamespaceApiService.createNamespace(namespaceData)
    namespaces.push(createdNamespace)
    selectedNamespace.value = createdNamespace
    showModal.value = false
    success(t('config.namespaceConfig.createSuccess'))
  } catch (err: any) {
    error(t('config.namespaceConfig.createFailed') + ': ' + err.message)
  }
}

// edit save namespace
const handleSave = async () => {
  if (!selectedNamespace.value) return

  if (!validateNamespace(selectedNamespace.value)) return

  try {
    const savedNamespace = await NamespaceApiService.updateNamespace(
      selectedNamespace.value.id,
      selectedNamespace.value
    )

    const index = namespaces.findIndex(a => a.id === savedNamespace.id)
    if (index !== -1) {
      namespaces[index] = savedNamespace
    }
    selectedNamespace.value = {
      ...savedNamespace,
    }
    success(t('config.namespaceConfig.saveSuccess'))
  } catch (err: any) {
    error(t('config.namespaceConfig.saveFailed') + ': ' + err.message)
  }
}

//delete namespace
const handleDelete = async () => {
  if (!selectedNamespace.value) return

  try {
    await NamespaceApiService.deleteNamespace(selectedNamespace.value.id)

    const index = namespaces.findIndex(a => a.id === selectedNamespace.value!.id)
    if (index !== -1) {
      namespaces.splice(index, 1)
    }

    selectedNamespace.value = namespaces.length > 0 ? namespaces[0] : null
    showDeleteModal.value = false
    success(t('config.namespaceConfig.deleteSuccess'))
  } catch (err: any) {
    error(t('config.namespaceConfig.deleteFailed') + ': ' + err.message)
  }
}

function validateNamespace(namespace: Omit<Namespace, 'id'> | Namespace): namespace is Namespace {
  const requiredFields = ['name'] as const

  for (const field of requiredFields) {
    const value = namespace[field] as PromptField
    if (
      value === null ||
      value === undefined ||
      (typeof value === 'string' && value.trim() === '')
    ) {
      error(`${field} is required and cannot be empty`)
      return false
    }
  }

  const uniqueCheck = validatePromptNameUnique(namespaces, namespace)
  if (!uniqueCheck) {
    return false
  }

  return true
}

function validatePromptNameUnique(
  namespaces: Namespace[],
  namespace: Omit<Namespace, 'id'> | Namespace
): namespace is Namespace {
  const existing = namespaces.find(p => p.name === namespace.name)

  // If a namespace with the same name exists and it's not the current one (edit mode), return error
  if (existing && (namespace as Namespace).id && existing.id !== (namespace as Namespace).id) {
    error(`Name "${namespace.name}" already exists. Please use a different name.`)
    return false
  }

  // If in create mode and a namespace with the same name already exists, return error
  if (existing && !(namespace as Namespace).id) {
    error(`Name "${namespace.name}" already exists. Please use a different name.`)
    return false
  }

  return true
}

const showAddNamespaceModal = () => {
  newNamespace.name = ''
  Object.assign(newNamespace, defaultNamespaceValues)
  showModal.value = true
}

const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.namespace-layout {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.namespace-list {
  width: 336px;
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

.namespace-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.namespaces-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
  padding-right: 6px;
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

.namespace-card {
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

.namespace-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.namespace-name {
  font-weight: 500;
  font-size: 16px;
}

.namespace-desc {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 0px;
  margin-top: 8px;
}

.namespace-tools {
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

.namespace-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 12px 24px;
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
  margin-bottom: 24px;
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

    &:disabled {
      cursor: not-allowed;
      background: rgba(255, 255, 255, 0.03);
      color: rgba(255, 255, 255);
      border-color: rgba(255, 255, 255, 0.05);
      opacity: 0.6;
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

.tags {
  margin-top: 8px;
}

.tags .tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: #a8b3ff;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
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
