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
      <button class="attach-btn" :title="$t('input.attachFile')" @click="handleFileUpload">
        <Icon icon="carbon:attachment" />
      </button>
      <input 
        ref="fileInputRef" 
        type="file" 
        multiple 
        style="display: none" 
        @change="handleFileChange"
        accept=".pdf,.txt,.md,.doc,.docx,.csv,.xlsx,.xls,.json,.xml,.html,.htm,.log,.java,.py,.js,.ts,.sql,.sh,.bat,.yaml,.yml,.properties,.conf,.ini"
      />
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
    
    <!-- Uploaded files display -->
    <div v-if="uploadedFiles.length > 0" class="uploaded-files">
      <div class="files-header">
        <Icon icon="carbon:document" />
        <span>{{ t('input.attachedFiles') }} ({{ uploadedFiles.length }})</span>
      </div>
      <div class="files-list">
        <div v-for="file in uploadedFiles" :key="file.name" class="file-item">
          <Icon icon="carbon:document" class="file-icon" />
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">({{ formatFileSize(file.size) }})</span>
          <button @click="removeFile(file)" class="remove-btn" :title="t('input.removeFile')">
            <Icon icon="carbon:close" />
          </button>
        </div>
      </div>
    </div>
    
    <!-- Upload progress indicator -->
    <div v-if="isUploading" class="upload-progress">
      <Icon icon="carbon:rotate--clockwise" class="loading-icon" />
      <span>{{ t('input.uploading') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import { memoryStore } from "@/stores/memory"
import type { InputMessage } from "@/stores/memory"
import { setUploadedFiles, clearUploadedFiles, type UploadedFile } from "@/stores/uploadedFiles"
import { FileUploadApiService } from "@/api/file-upload-api-service"

const { t } = useI18n()

interface Props {
  placeholder?: string
  disabled?: boolean
  initialValue?: string
}

interface Emits {
  (e: 'send', message: InputMessage): void
  (e: 'clear'): void
  (e: 'update-state', enabled: boolean, placeholder?: string): void
  (e: 'plan-mode-clicked'): void
  (e: 'files-uploaded', files: UploadedFile[]): void
}



const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  disabled: false,
  initialValue: '',
})

const emit = defineEmits<Emits>()

const inputRef = ref<HTMLTextAreaElement>()
const fileInputRef = ref<HTMLInputElement>()
const currentInput = ref('')
const defaultPlaceholder = computed(() => props.placeholder || t('input.placeholder'))
const currentPlaceholder = ref(defaultPlaceholder.value)
const uploadedFiles = ref<UploadedFile[]>([])
const isUploading = ref(false)
const sessionPlanId = ref<string | null>(null)

// Function to reset sessionPlanId when starting a new conversation session
const resetSession = () => {
  sessionPlanId.value = null
  uploadedFiles.value = []
  clearUploadedFiles()
}

// Auto-reset session when component is unmounted to prevent memory leaks
onUnmounted(() => {
  resetSession()
})

// Watch for specific conditions to auto-reset session
watch(() => uploadedFiles.value.length, (newCount, oldCount) => {
  // If files were removed and now there are no files, keep session alive for follow-up questions
  // The session will only be reset on component unmount or manual reset
  if (newCount === 0 && oldCount > 0) {
    // Keep sessionPlanId alive for follow-up conversations
    // Only reset on explicit user action or component cleanup
  }
})

// resetSession will be exposed in the main defineExpose call below

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

  let finalInput = currentInput.value.trim()
  
  // If files are uploaded, add file information to the query
  if (uploadedFiles.value.length > 0) {
    const fileInfo = uploadedFiles.value.map(f => `${f.name} (${f.relativePath})`).join(', ')
    finalInput += `\n\n[Uploaded files: ${fileInfo}]`
  }

  const query = {
    input: finalInput,
    memoryId: memoryStore.selectMemoryId,
    uploadedFiles: uploadedFiles.value,
    sessionPlanId: sessionPlanId.value 
  }

  // Use Vue's emit to send a message
  emit('send', query)

  // Clear the input and uploaded files
  clearInput()
  uploadedFiles.value = []
  // Don't clear sessionPlanId immediately - keep it for potential follow-up conversations
  // sessionPlanId.value = null
  clearUploadedFiles()
}

const handlePlanModeClick = () => {
  // Trigger the plan mode toggle event
  emit('plan-mode-clicked')
}

// File upload handlers
const handleFileUpload = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  
  if (!files || files.length === 0) return
  
  // Convert FileList to Array and add to pending files (for batch upload)
  const fileArray = Array.from(files)
  console.log('[FileUpload] Selected files for upload:', fileArray.map(f => f.name))
  
  // Immediately upload all selected files
  await uploadFiles(fileArray)
  
  // Reset file input
  if (target) {
    target.value = ''
  }
}

const uploadFiles = async (files: File[]) => {
  if (files.length === 0) return
  
  isUploading.value = true
  
  try {
    // Create FormData for file upload
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    
    // Upload files - use existing sessionPlanId or create new one
    let uploadUrl = '/api/file-upload/upload'
    if (sessionPlanId.value) {
      uploadUrl = `/api/file-upload/upload/${sessionPlanId.value}`
      console.log('[FileUpload] Using existing sessionPlanId:', sessionPlanId.value)
    } else {
      console.log('[FileUpload] Creating new temporary planId')
    }
    
    const response = await fetch(uploadUrl, {
      method: 'POST',
      body: formData
    })
    
    if (!response.ok) {
      throw new Error(`Upload failed: ${response.statusText}`)
    }
    
    const result = await response.json()
    
    if (result.uploadedFiles) {
      // Set sessionPlanId if not already set
      if (!sessionPlanId.value && result.planId) {
        sessionPlanId.value = result.planId
        console.log('[FileUpload] Set sessionPlanId:', sessionPlanId.value)
      } else if (sessionPlanId.value) {
        console.log('[FileUpload] SessionPlanId already exists:', sessionPlanId.value)
      }
      
      // Convert uploaded files to our format
      const newFiles: UploadedFile[] = result.uploadedFiles.map((file: any) => ({
        name: file.originalName,
        size: file.size,
        type: file.extension,
        planId: result.planId,
        relativePath: file.relativePath
      }))
      
      uploadedFiles.value = [...uploadedFiles.value, ...newFiles]
      emit('files-uploaded', newFiles)
      
      // Update global state
      setUploadedFiles(uploadedFiles.value)
      console.log('[Input] Updated global uploadedFiles state:', uploadedFiles.value)
      
      // Update placeholder to show files are attached
      if (uploadedFiles.value.length > 0) {
        currentPlaceholder.value = t('input.filesAttached', { count: uploadedFiles.value.length })
      }
    }
    
    // Show success message or update UI as needed
    console.log('Files uploaded successfully:', result)
    
  } catch (error) {
    console.error('File upload error:', error)
    // Show error message to user
  } finally {
    isUploading.value = false
  }
}

// File management functions
const removeFile = async (fileToRemove: UploadedFile) => {
  try {
    console.log('🗑️ Removing file:', fileToRemove.name, 'from plan:', fileToRemove.planId)
    
    // Call backend API to delete the file from server
    if (fileToRemove.planId) {
      await FileUploadApiService.deleteFile(fileToRemove.planId, fileToRemove.name)
      console.log('✅ File deleted from server successfully')
    }
    
    // Update frontend state
    uploadedFiles.value = uploadedFiles.value.filter(file => file.name !== fileToRemove.name)
    
    // Update placeholder, keep sessionPlanId for follow-up conversations
    if (uploadedFiles.value.length === 0) {
      currentPlaceholder.value = defaultPlaceholder.value
      // Keep sessionPlanId for follow-up conversations about uploaded files
      // Only clear sessionPlanId when user starts a completely new session
      // sessionPlanId.value = null
      clearUploadedFiles()
    } else {
      currentPlaceholder.value = t('input.filesAttached', { count: uploadedFiles.value.length })
      setUploadedFiles(uploadedFiles.value)
    }
    
    console.log('🎉 File removal completed, remaining files:', uploadedFiles.value.length)
    
  } catch (error) {
    console.error('❌ Error removing file:', error)
    alert(t('input.fileDeleteError') || 'Failed to delete file')
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
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

// Expose methods to the parent component
defineExpose({
  clearInput,
  updateState,
  setInputValue,
  getQuery,
  resetSession,
  focus: () => inputRef.value?.focus()
})

onMounted(() => {
  // Initialization logic after component mounting
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

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
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

/* File upload styles */
.uploaded-files {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.files-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.file-icon {
  font-size: 14px;
  color: #007acc;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #ffffff;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.remove-btn {
  background: none;
  border: none;
  padding: 2px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  transition: all 0.2s ease;
  border-radius: 3px;
  flex-shrink: 0;

  &:hover {
    color: #ff6b6b;
    background: rgba(255, 107, 107, 0.1);
  }
}

.upload-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(0, 122, 204, 0.1);
  border-radius: 6px;
  border: 1px solid rgba(0, 122, 204, 0.2);
  font-size: 12px;
  color: #007acc;
}

.loading-icon {
  font-size: 14px;
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

/* Enhance attach button */
.attach-btn {
  &:hover {
    background: rgba(255, 255, 255, 0.15);
    color: #007acc;
    transform: translateY(-1px);
  }
}
</style>
