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
  <div class="input-area">
    <div class="input-container">
      <input 
        ref="fileInputRef"
        type="file" 
        style="display: none" 
        @change="handleFileSelect"
        multiple
        accept=".txt,.csv,.xlsx,.xls,.json,.xml,.md,.pdf,.docx,.doc,.pptx,.ppt,.zip"
      />
      <button 
        class="attach-btn" 
        :title="$t('input.attachFile')" 
        @click="triggerFileUpload"
      >
        <Icon icon="carbon:attachment" />
      </button>
      <textarea
        v-model="currentInput"
        ref="inputRef"
        class="chat-input"
        :placeholder="currentPlaceholder"
        :disabled="isDisabled"
        @keydown="handleKeydown"
        @input="adjustInputHeight"
      ></textarea>
      <button class="plan-mode-btn" :title="$t('input.planMode')" @click="handlePlanModeClick">
        <Icon icon="carbon:document" />
        {{ $t('input.planMode') }}
      </button>
      <button
        class="send-button"
        :disabled="!currentInput.trim() || isDisabled"
        @click="handleSend"
        :title="$t('input.send')"
      >
        <Icon icon="carbon:send-alt" />
        {{ $t('input.send') }}
      </button>
    </div>
    
    <!-- File Bar for Shared Files -->
    <div v-if="sharedFiles.length > 0" class="file-bar">
      <div class="file-bar-header">
        <Icon icon="carbon:document" />
        <span>{{ $t('input.sharedFiles') }} ({{ sharedFiles.length }})</span>
      </div>
      <div class="file-list">
        <div 
          v-for="file in sharedFiles" 
          :key="file.name" 
          class="file-item"
        >
          <div class="file-info">
            <Icon :icon="getFileIcon(file)" class="file-icon" />
            <span class="file-name">{{ file.originalName || file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
          </div>
          <button 
            class="file-delete-btn" 
            @click="deleteSharedFile(file.name)"
            :title="$t('input.deleteFile')"
          >
            <Icon icon="carbon:close" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import { memoryStore } from "@/stores/memory"
import type { InputMessage } from "@/stores/memory"

const { t } = useI18n()

interface Props {
  placeholder?: string
  disabled?: boolean
  initialValue?: string
  planId?: string | null
}

interface Emits {
  (e: 'send', message: InputMessage): void
  (e: 'clear'): void
  (e: 'update-state', enabled: boolean, placeholder?: string): void
  (e: 'plan-mode-clicked'): void
  (e: 'files-uploaded', files: File[]): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  disabled: false,
  initialValue: '',
  planId: null,
})

const emit = defineEmits<Emits>()

const inputRef = ref<HTMLTextAreaElement>()
const fileInputRef = ref<HTMLInputElement>()
const currentInput = ref('')
const defaultPlaceholder = computed(() => props.placeholder || t('input.placeholder'))
const currentPlaceholder = ref(defaultPlaceholder.value)
const uploadedFiles = ref<File[]>([])
const sharedFiles = ref<any[]>([])
const currentPlanId = ref<string>('')

// Computed property to ensure 'disabled' is a boolean type
const isDisabled = computed(() => Boolean(props.disabled))

const adjustInputHeight = () => {
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
      inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
    }
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!currentInput.value.trim() || isDisabled.value) return

  const query = {
    input: currentInput.value.trim(),
    memoryId: memoryStore.selectMemoryId
  }

  // Use Vue's emit to send a message
  emit('send', query)

  // Clear the input
  clearInput()
}

const handlePlanModeClick = () => {
  // Trigger the plan mode toggle event
  emit('plan-mode-clicked')
}

/**
 * Trigger file upload dialog
 */
const triggerFileUpload = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

/**
 * Handle file selection
 */
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  
  if (files.length === 0) return
  
  try {
    // Upload files to shared directory by default, or to specific plan if available
    for (const file of files) {
      if (currentPlanId.value) {
        await uploadFileToSandbox(file)
      } else {
        await uploadFileToShared(file)
      }
    }
    
    // Store uploaded files
    uploadedFiles.value.push(...files)
    
    // Emit files uploaded event
    emit('files-uploaded', files)
    
    // Show success message
    console.log(`Successfully uploaded ${files.length} file(s) to unified directory - visible in FileBrowser`)
    
    // Update input placeholder to indicate files are ready
    if (files.length > 0) {
      currentPlaceholder.value = t('input.filesUploaded', { count: uploadedFiles.value.length })
    }
    
  } catch (error) {
    console.error('Error uploading files:', error)
    // Could emit an error event here if needed
  } finally {
    // Clear the file input
    if (target) {
      target.value = ''
    }
  }
}

/**
 * Upload single file to shared directory
 */
const uploadFileToShared = async (file: File): Promise<void> => {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await fetch('/api/file-sandbox/upload-shared', {
    method: 'POST',
    body: formData
  })
  
  if (!response.ok) {
    const errorData = await response.json()
    throw new Error(errorData.message || 'Upload failed')
  }
  
  const result = await response.json()
  console.log('File uploaded to shared directory:', result)
  
  // Refresh shared files list
  await loadSharedFiles()
}

/**
 * Upload single file to sandbox
 * Files are now saved directly to the plan root directory for FileBrowser compatibility
 */
const uploadFileToSandbox = async (file: File): Promise<void> => {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await fetch(`/api/file-sandbox/upload/${currentPlanId.value}`, {
    method: 'POST',
    body: formData
  })
  
  if (!response.ok) {
    const errorData = await response.json()
    throw new Error(errorData.message || 'Upload failed')
  }
  
  const result = await response.json()
  console.log('File uploaded in:', currentPlanId.value)
  console.log('File uploaded to unified directory:', result)
}

/**
 * Load shared files list
 */
const loadSharedFiles = async () => {
  try {
    const response = await fetch('/api/file-sandbox/files-shared')
    if (response.ok) {
      const result = await response.json()
      if (result.success) {
        sharedFiles.value = result.files
      }
    }
  } catch (error) {
    console.error('Error loading shared files:', error)
  }
}

/**
 * Delete shared file
 */
const deleteSharedFile = async (fileName: string) => {
  try {
    const response = await fetch(`/api/file-sandbox/file-shared/${encodeURIComponent(fileName)}`, {
      method: 'DELETE'
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.success) {
        console.log('File deleted successfully:', fileName)
        // Refresh shared files list
        await loadSharedFiles()
      }
    }
  } catch (error) {
    console.error('Error deleting shared file:', error)
  }
}

/**
 * Clear the input box
 */
const clearInput = () => {
  currentInput.value = ''
  adjustInputHeight()
  emit('clear')
}

/**
 * Update the state of the input area (enable/disable)
 * @param {boolean} enabled - Whether to enable input
 * @param {string} [placeholder] - Placeholder text when enabled
 */
const updateState = (enabled: boolean, placeholder?: string) => {
  if (placeholder) {
    currentPlaceholder.value = enabled ? placeholder : t('input.waiting')
  }
  emit('update-state', enabled, placeholder)
}

/**
 * Set the input value without triggering send
 * @param {string} value - The value to set
 */
const setInputValue = (value: string) => {
  currentInput.value = value
  adjustInputHeight()
}

/**
 * Get the current value of the input box
 * @returns {string} The text value of the current input box (trimmed)
 */
const getQuery = () => {
  return currentInput.value.trim()
}

/**
 * Get file icon based on file type
 */
const getFileIcon = (file: any): string => {
  if (!file || !file.name) return 'carbon:document'
  
  const fileName = file.name.toLowerCase()
  
  // Programming languages
  if (fileName.endsWith('.js')) return 'vscode-icons:file-type-js'
  if (fileName.endsWith('.ts')) return 'vscode-icons:file-type-typescript'
  if (fileName.endsWith('.vue')) return 'vscode-icons:file-type-vue'
  if (fileName.endsWith('.java')) return 'vscode-icons:file-type-java'
  if (fileName.endsWith('.py')) return 'vscode-icons:file-type-python'
  if (fileName.endsWith('.json')) return 'vscode-icons:file-type-json'
  if (fileName.endsWith('.xml')) return 'vscode-icons:file-type-xml'
  if (fileName.endsWith('.html')) return 'vscode-icons:file-type-html'
  if (fileName.endsWith('.css')) return 'vscode-icons:file-type-css'
  if (fileName.endsWith('.md')) return 'vscode-icons:file-type-markdown'
  if (fileName.endsWith('.yml') || fileName.endsWith('.yaml')) return 'vscode-icons:file-type-yaml'
  
  // Documents
  if (fileName.endsWith('.pdf')) return 'vscode-icons:file-type-pdf2'
  if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 'vscode-icons:file-type-word'
  if (fileName.endsWith('.xls') || fileName.endsWith('.xlsx')) return 'vscode-icons:file-type-excel'
  if (fileName.endsWith('.ppt') || fileName.endsWith('.pptx')) return 'vscode-icons:file-type-powerpoint'
  
  // Archives
  if (fileName.match(/\.(zip|rar|7z|tar|gz)$/)) return 'carbon:archive'
  
  // Default
  return 'carbon:document'
}

/**
 * Format file size
 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// Watch for initialValue changes
watch(
  () => props.initialValue,
  (newValue) => {
    if (newValue && newValue.trim()) {
      currentInput.value = newValue
      adjustInputHeight()
    }
  },
  { immediate: true }
)

// Watch for planId changes to update currentPlanId
watch(
  () => props.planId,
  (newPlanId) => {
    if (newPlanId) {
      currentPlanId.value = newPlanId
      console.log('Plan ID updated in input component:', newPlanId)
    } 
  },
  { immediate: true }
)

// Expose methods to the parent component
defineExpose({
  clearInput,
  updateState,
  setInputValue,
  getQuery,
  focus: () => inputRef.value?.focus()
})

onMounted(() => {
  // Load shared files on component mount
  loadSharedFiles()
})

onUnmounted(() => {
  // Cleanup logic before component unmounting
})
</script>

<style lang="less" scoped>
.input-area {
  min-height: 112px;
  padding: 20px 24px;
  border-top: 1px solid #1a1a1a;
  background: rgba(255, 255, 255, 0.02);
  /* Ensure the input area is always at the bottom */
  flex-shrink: 0; /* Won't be compressed */
  position: sticky; /* Fixed at the bottom */
  bottom: 0;
  z-index: 100;
  /* Add a slight shadow to distinguish the message area */
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(20px);
}

.input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 16px;

  &:focus-within {
    border-color: #667eea;
  }
}

.attach-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.chat-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 20px;
  max-height: 120px;

  &::placeholder {
    color: #666666;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;

    &::placeholder {
      color: #444444;
    }
  }
}

.plan-mode-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: #667eea;
    transform: translateY(-1px);
  }
}

.send-button {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.clear-memory-btn{
  width: 1.5em;
  height: 1.5em;
}

/* File Bar Styles */
.file-bar {
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  overflow: hidden;
}

.file-bar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  font-weight: 500;
}

.file-list {
  max-height: 120px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: background-color 0.2s ease;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.file-name {
  color: #ffffff;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  margin-left: 8px;
  flex-shrink: 0;
}

.file-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: 8px;
}

.file-delete-btn:hover {
  background: rgba(220, 38, 38, 0.2);
  color: #ef4444;
}

/* Scrollbar for file list */
.file-list::-webkit-scrollbar {
  width: 4px;
}

.file-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.file-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.file-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
