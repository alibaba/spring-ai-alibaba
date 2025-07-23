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
   Dynamic Prompt Configuration

   Features:
    - Add, edit and delete prompts
    - View prompt details
    - Validate promptName uniqueness

   TODO:
    - add namespace for dynamic prompts
    - Allow editing of promptName during edit mode (originally was disabled)
-->
<template>
  <ConfigPanel>
    <template #title>
      <h2>{{ t('config.promptConfig.title') }}</h2>
    </template>

    <div class="prompt-layout">
      <!--left prompt list-->
      <div class="prompt-list">
        <div class="list-header">
          <h3>{{ t('config.promptConfig.configuredprompts') }}</h3>
          <span class="prompt-count"
            >({{ prompts.length }}{{ t('config.promptConfig.promptCount') }})</span
          >
        </div>

        <div class="prompts-container" v-if="!loading">
          <div
            v-for="prompt in prompts"
            :key="prompt.id"
            class="prompt-card"
            :class="{ active: selectedPrompt?.id === prompt.id }"
            @click="selectPrompt(prompt)"
          >
            <div class="prompt-card-header">
              <span class="prompt-name">{{ prompt.promptName }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p v-if="prompt.promptDescription?.trim()" class="prompt-desc">
              {{ prompt.promptDescription }}
            </p>
            <div class="tags">
              <span class="tag">
                {{
                  prompt.builtIn
                    ? t('config.promptConfig.builtIn')
                    : t('config.promptConfig.custom')
                }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="loading-icon" />
          {{ t('common.loading') }}
        </div>

        <div v-if="!loading && prompts.length === 0" class="empty-state">
          <Icon icon="carbon:bot" class="empty-icon" />
          <p>{{ t('config.promptConfig.noPrompts') }}</p>
        </div>

        <button class="add-btn" @click="showAddPromptModal">
          <Icon icon="carbon:add" />
          {{ t('config.promptConfig.createNew') }}
        </button>
      </div>

      <!-- prompt详情 -->
      <div class="prompt-detail" v-if="selectedPrompt">
        <div class="detail-header">
          <h3>{{ selectedPrompt.promptName }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              {{ t('common.save') }}
            </button>
            <button
              v-if="!selectedPrompt.builtIn"
              class="action-btn danger"
              @click="showDeleteConfirm"
            >
              <Icon icon="carbon:trash-can" />
              {{ t('common.delete') }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>{{ t('config.promptConfig.promptName') }} <span class="required">*</span></label>
          <input
            :disabled="true"
            type="text"
            v-model="selectedPrompt.promptName"
            :placeholder="t('config.promptConfig.placeholder')"
            required
          />
        </div>

        <div class="form-item">
          <label>
            {{ t('config.promptConfig.messageType') }}
            <span class="required">*</span>
          </label>
          <CustomSelect
            v-if="!selectedPrompt.builtIn"
            v-model="selectedPrompt.messageType"
            :options="messageTypeEnum"
            :disabled="selectedPrompt.builtIn"
            :placeholder="t('config.promptConfig.placeholder')"
          />
          <input v-else type="text" v-model="selectedPrompt.messageType" :disabled="true" />
        </div>

        <div class="form-item">
          <label>
            {{ t('config.promptConfig.type') }}
            <span class="required">*</span>
          </label>
          <CustomSelect
            v-if="!selectedPrompt.builtIn"
            v-model="selectedPrompt.type"
            :options="typeEnum"
            :disabled="selectedPrompt.builtIn"
            :placeholder="t('config.promptConfig.placeholder')"
          />
          <input v-else type="text" v-model="selectedPrompt.type" :disabled="true" required />
        </div>

        <div class="form-item">
          <label>
            {{ t('config.promptConfig.description') }}
          </label>
          <textarea
            v-model="selectedPrompt.promptDescription"
            rows="2"
            :placeholder="t('config.promptConfig.descriptionPlaceholder')"
            required
          ></textarea>
        </div>

        <div class="form-item">
          <label>
            {{ t('config.promptConfig.promptContent') }}
            <span class="required">*</span>
          </label>
          <textarea
            v-model="selectedPrompt.promptContent"
            rows="8"
            :placeholder="t('config.promptConfig.promptContentPlaceholder')"
            required
          ></textarea>
        </div>
      </div>

      <!-- empty selection -->
      <div v-else class="no-selection">
        <Icon icon="carbon:bot" class="placeholder-icon" />
        <p>{{ t('config.promptConfig.selectPromptHint') }}</p>
      </div>
    </div>

    <!-- new prompt dialog -->
    <Modal
      v-model="showModal"
      :title="t('config.promptConfig.newPrompt')"
      @confirm="handleAddPrompt"
    >
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.promptConfig.promptName') }} <span class="required">*</span></label>
          <input
            type="text"
            v-model="newPrompt.promptName"
            :placeholder="t('config.promptConfig.placeholder')"
            required
          />
        </div>
        <div class="form-item">
          <label>
            {{ t('config.promptConfig.messageType') }}
            <span class="required">*</span>
          </label>
          <CustomSelect
            v-model="newPrompt.messageType"
            :options="messageTypeEnum"
            :placeholder="t('config.promptConfig.placeholder')"
          />
        </div>
        <div class="form-item">
          <label>
            {{ t('config.promptConfig.type') }}
            <span class="required">*</span>
          </label>
          <CustomSelect
            v-model="newPrompt.type"
            :options="typeEnum"
            :placeholder="t('config.promptConfig.placeholder')"
          />
        </div>
        <div class="form-item">
          <label>
            {{ t('config.promptConfig.description') }}
          </label>
          <textarea
            v-model="newPrompt.promptDescription"
            rows="3"
            :placeholder="t('config.promptConfig.descriptionPlaceholder')"
          ></textarea>
        </div>

        <div class="form-item">
          <label>
            {{ t('config.promptConfig.promptContent') }}
            <span class="required">*</span>
          </label>
          <textarea
            v-model="newPrompt.promptContent"
            rows="8"
            :placeholder="t('config.promptConfig.promptContentPlaceholder')"
            required
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- Delete confirmation dialog -->
    <Modal v-model="showDeleteModal" :title="t('config.promptConfig.deleteConfirm')">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>
          {{ t('config.promptConfig.deleteConfirmText') }}
          <strong>{{ selectedPrompt?.promptName }}</strong> {{ t('common.confirm') }}？
        </p>
        <p class="warning-text">{{ t('config.promptConfig.deleteWarning') }}</p>
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
import { PromptApiService, type Prompt } from '@/api/prompt-api-service'
import { useToast } from '@/plugins/useToast'
import CustomSelect from './components/select.vue'
import ConfigPanel from './components/configPanel.vue'
import { usenameSpaceStore } from '@/stores/namespace'


type PromptField = string | null | undefined

const { t } = useI18n()
const { success, error } = useToast()

const namespaceStore = usenameSpaceStore()

// Reactive data properties
const loading = ref(false)
const prompts = reactive<Prompt[]>([])
const selectedPrompt = ref<Prompt | null>(null)
const showModal = ref(false)
const showDeleteModal = ref(false)
const messageTypeEnum: Array<{
  id: string
  name: string
}> = [
  { id: 'USER', name: 'USER' },
  { id: 'ASSISTANT', name: 'ASSISTANT' },
  { id: 'SYSTEM', name: 'SYSTEM' },
  { id: 'TOOL', name: 'TOOL' },
]

const typeEnum: Array<{
  id: string
  name: string
}> = [
  { id: 'LLM', name: 'LLM' },
  { id: 'PLANNING', name: 'PLANNING' },
  { id: 'AGENT', name: 'AGENT' },
]

// default values for creat Prompt fields
const defaultPromptValues = {
  promptName: '',
  // namespace: '', //todo
  messageType: '',
  type: '',
  promptDescription: '',
}

const newPrompt = reactive<Omit<Prompt, 'id'>>({ ...defaultPromptValues } as Omit<Prompt, 'id'>)
// Load prompt list data from API
const loadData = async () => {
  loading.value = true
  try {
    const loadedPrompts = (await PromptApiService.getAllPrompts(namespaceStore.namespace)) as Prompt[]

    if (loadedPrompts.length > 0) {
      await selectPrompt(loadedPrompts[0])
    }
    prompts.splice(0, prompts.length, ...loadedPrompts)
  } catch (err: any) {
    console.error('加载数据失败:', err)
    error(t('config.promptConfig.loadDataFailed') + ': ' + err.message)
  } finally {
    loading.value = false
  }
}
// select prompt
const selectPrompt = async (prompt: Prompt) => {
  try {
    const detailedPrompt = await PromptApiService.getPromptById(prompt.id)
    selectedPrompt.value = {
      ...detailedPrompt,
    }
  } catch (err: any) {
    console.error('加载Prompt详情失败:', err)
    error(t('config.promptConfig.loadDetailsFailed') + ': ' + err.message)
    // base pormpt
    selectedPrompt.value = {
      ...prompt,
    }
  }
}
//
const handleAddPrompt = async () => {
  if (!validatePrompt(newPrompt)) return
  try {
    const promptData: Omit<Prompt, 'id'> = {
      ...newPrompt,
      builtIn: false,
    }
    const createdPrompt = await PromptApiService.createPrompt(promptData)
    prompts.push(createdPrompt)
    selectedPrompt.value = createdPrompt
    showModal.value = false
    success(t('config.promptConfig.createSuccess'))
  } catch (err: any) {
    error(t('config.promptConfig.createFailed') + ': ' + err.message)
  }
}

// edit save prompt
const handleSave = async () => {
  if (!selectedPrompt.value) return

  if (!validatePrompt(selectedPrompt.value)) return

  try {
    const savedPrompt = await PromptApiService.updatePrompt(
      selectedPrompt.value.id,
      selectedPrompt.value
    )

    const index = prompts.findIndex(a => a.id === savedPrompt.id)
    if (index !== -1) {
      prompts[index] = savedPrompt
    }
    selectedPrompt.value = {
      ...savedPrompt,
    }
    success(t('config.promptConfig.saveSuccess'))
  } catch (err: any) {
    error(t('config.promptConfig.saveFailed') + ': ' + err.message)
  }
}

//delete promopt
const handleDelete = async () => {
  if (!selectedPrompt.value) return

  try {
    await PromptApiService.deletePrompt(selectedPrompt.value.id)

    const index = prompts.findIndex(a => a.id === selectedPrompt.value!.id)
    if (index !== -1) {
      prompts.splice(index, 1)
    }

    selectedPrompt.value = prompts.length > 0 ? prompts[0] : null
    showDeleteModal.value = false
    success(t('config.promptConfig.deleteSuccess'))
  } catch (err: any) {
    error(t('config.promptConfig.deleteFailed') + ': ' + err.message)
  }
}

function validatePrompt(prompt: Omit<Prompt, 'id'> | Prompt): prompt is Prompt {
  const requiredFields = ['promptName', 'messageType', 'type', 'promptContent'] as const

  for (const field of requiredFields) {
    const value = prompt[field] as PromptField
    if (
      value === null ||
      value === undefined ||
      (typeof value === 'string' && value.trim() === '')
    ) {
      error(`${field} is required and cannot be empty`)
      return false
    }
  }

  const uniqueCheck = validatePromptNameUnique(prompts, prompt)
  if (!uniqueCheck) {
    return false
  }

  return true
}

function validatePromptNameUnique(
  prompts: Prompt[],
  prompt: Omit<Prompt, 'id'> | Prompt
): prompt is Prompt {
  const existing = prompts.find(p => p.promptName === prompt.promptName)

  // If a prompt with the same name exists and it's not the current one (edit mode), return error
  if (existing && (prompt as Prompt).id && existing.id !== (prompt as Prompt).id) {
    error(`Name "${prompt.promptName}" already exists. Please use a different name.`)
    return false
  }

  // If in create mode and a prompt with the same name already exists, return error
  if (existing && !(prompt as Prompt).id) {
    error(`Name "${prompt.promptName}" already exists. Please use a different name.`)
    return false
  }

  return true
}

const showAddPromptModal = () => {
  newPrompt.promptName = ''
  newPrompt.promptDescription = ''
  newPrompt.promptContent = ''
  // 初始化 newPrompt 数据
  Object.assign(newPrompt, defaultPromptValues)
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

.prompt-layout {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.prompt-list {
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

.prompt-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.prompts-container {
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

.prompt-card {
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

.prompt-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.prompt-name {
  font-weight: 500;
  font-size: 16px;
}

.prompt-desc {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 0px;
  margin-top: 8px;
}

.prompt-tools {
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

.prompt-detail {
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
