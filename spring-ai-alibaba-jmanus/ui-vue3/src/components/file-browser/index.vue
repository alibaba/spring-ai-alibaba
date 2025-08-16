<!--
  /*
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
 */
-->

<template>
  <div class="file-browser">
    <div class="file-browser-header">
      <h3>{{ $t('fileBrowser.title') }}</h3>
      <div class="header-actions">
                <button
          class="refresh-btn"
          @click="refreshFileTree"
          :disabled="loading"
          :title="$t('fileBrowser.refresh')"
        >
          <Icon
            icon="carbon:refresh"
            :class="{ 'rotating': loading }"
            :style="{ color: '#ffffff', fontSize: '18px', width: '18px', height: '18px' }"
          />
        </button>
      </div>
    </div>

    <div class="file-browser-content">
      <!-- File Tree -->
      <div class="file-tree-panel">
        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="rotating" />
          <span>{{ $t('fileBrowser.loading') }}</span>
        </div>

        <div v-else-if="error" class="error-state">
          <div v-if="isPlanDirectoryNotFound" class="waiting-for-files">
            <Icon icon="carbon:time" class="rotating" />
            <div class="message-content">
              <h3>{{ $t('fileBrowser.waitingForGeneration') }}</h3>
              <p>{{ $t('fileBrowser.planExecuting') }}</p>
              <div class="tips">
                <Icon icon="carbon:information" />
                <span>{{ $t('fileBrowser.filesTip') }}</span>
              </div>
            </div>
            <button @click="refreshFileTree" class="retry-btn" :disabled="loading">
              <Icon icon="carbon:refresh" :class="{ 'rotating': loading }" />
              {{ loading ? $t('fileBrowser.checking') : $t('fileBrowser.checkNow') }}
            </button>
          </div>
          <div v-else class="actual-error">
            <Icon icon="carbon:warning" />
            <span>{{ error }}</span>
            <button @click="refreshFileTree" class="retry-btn">
              {{ $t('fileBrowser.retry') }}
            </button>
          </div>
        </div>

        <div v-else-if="!fileTree" class="empty-state">
          <Icon icon="carbon:folder-off" />
          <span>{{ $t('fileBrowser.noFiles') }}</span>
        </div>

        <div v-else class="file-tree">
          <FileTreeNode
            :node="fileTree"
            :level="0"
            @file-selected="handleFileSelected"
            @download-file="handleDownloadFile"
          />
        </div>
      </div>

      <!-- File Content Viewer -->
      <div class="file-content-panel" v-if="selectedFile">
        <div class="file-content-header">
          <div class="file-info">
            <Icon :icon="getFileIcon(selectedFile)" />
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
          </div>
          <div class="file-actions">
            <button
              @click="handleDownloadFile(selectedFile)"
              class="download-btn"
              :title="$t('fileBrowser.download')"
            >
              <Icon icon="carbon:download" />
            </button>
            <button
              @click="closeFileViewer"
              class="close-btn"
              :title="$t('common.close')"
            >
              <Icon icon="carbon:close" />
            </button>
          </div>
        </div>

        <div class="file-content-body">
          <div v-if="loadingContent" class="loading-content">
            <Icon icon="carbon:loading" class="rotating" />
            <span>{{ $t('fileBrowser.loadingContent') }}</span>
          </div>

          <div v-else-if="contentError" class="content-error">
            <Icon icon="carbon:warning" />
            <span>{{ contentError }}</span>
          </div>

          <div v-else-if="fileContent" class="file-content">
            <div v-if="isTextFile" class="text-content">
              <pre><code>{{ fileContent.content }}</code></pre>
            </div>
            <div v-else class="binary-content">
              <Icon icon="carbon:document-unknown" />
              <p>{{ $t('fileBrowser.binaryFile') }}</p>
              <button @click="handleDownloadFile(selectedFile)" class="download-btn-large">
                <Icon icon="carbon:download" />
                {{ $t('fileBrowser.downloadToView') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import FileTreeNode from './FileTreeNode.vue'
import { FileBrowserApiService, type FileNode, type FileContent } from '@/api/file-browser-api-service'

interface Props {
  planId: string
}

const props = defineProps<Props>()
const { t } = useI18n()

// State
const loading = ref(false)
const error = ref<string | null>(null)
const fileTree = ref<FileNode | null>(null)
const selectedFile = ref<FileNode | null>(null)
const fileContent = ref<FileContent | null>(null)
const loadingContent = ref(false)
const contentError = ref<string | null>(null)
const autoRefreshTimer = ref<number | null>(null)

// Computed
const isTextFile = computed(() => {
  if (!fileContent.value || !selectedFile.value) return false
  return FileBrowserApiService.isTextFile(fileContent.value.mimeType, selectedFile.value.name)
})

const isPlanDirectoryNotFound = computed(() => {
  return error.value && (
    error.value.includes('Plan directory not found') ||
    error.value.includes('not found')
  )
})

// Methods
const clearAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearTimeout(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

const startAutoRefresh = () => {
  clearAutoRefresh()
  // Auto refresh every 5 seconds when plan directory is not found
  autoRefreshTimer.value = window.setTimeout(() => {
    if (isPlanDirectoryNotFound.value) {
      refreshFileTree()
    }
  }, 5000)
}

const refreshFileTree = async () => {
  if (!props.planId) return

  loading.value = true
  error.value = null
  clearAutoRefresh()

  try {
    fileTree.value = await FileBrowserApiService.getFileTree(props.planId)
    // Success - no need to auto refresh
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('fileBrowser.loadError')
    console.error('Failed to load file tree:', err)

    // Start auto refresh if it's a "directory not found" error
    const errorMessage = err instanceof Error ? err.message : ''
    if (errorMessage.includes('Plan directory not found') ||
        errorMessage.includes('not found')) {
      startAutoRefresh()
    }
  } finally {
    loading.value = false
  }
}

const handleFileSelected = async (file: FileNode) => {
  if (file.type === 'directory') return

  selectedFile.value = file
  fileContent.value = null
  loadingContent.value = true
  contentError.value = null

  try {
    fileContent.value = await FileBrowserApiService.getFileContent(props.planId, file.path)
  } catch (err) {
    contentError.value = err instanceof Error ? err.message : t('fileBrowser.contentLoadError')
    console.error('Failed to load file content:', err)
  } finally {
    loadingContent.value = false
  }
}

const handleDownloadFile = async (file: FileNode) => {
  try {
    await FileBrowserApiService.downloadFile(props.planId, file.path, file.name)
  } catch (err) {
    console.error('Failed to download file:', err)
    // You could show a toast message here
  }
}

const closeFileViewer = () => {
  selectedFile.value = null
  fileContent.value = null
  contentError.value = null
}

const getFileIcon = (file: FileNode) => {
  return FileBrowserApiService.getFileIcon(file)
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// Watch for plan ID changes
watch(() => props.planId, (newPlanId) => {
  if (newPlanId) {
    selectedFile.value = null
    fileContent.value = null
    refreshFileTree()
  }
}, { immediate: true })

// Lifecycle
onMounted(() => {
  if (props.planId) {
    refreshFileTree()
  }
})

onUnmounted(() => {
  clearAutoRefresh()
})
</script>

<style scoped>
.file-browser {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.file-browser-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.file-browser-header h3 {
  margin: 0;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.refresh-btn .iconify {
  color: #ffffff !important;
  font-size: 18px !important;
  width: 18px !important;
  height: 18px !important;
}

.refresh-btn svg {
  fill: #ffffff !important;
  color: #ffffff !important;
  width: 18px !important;
  height: 18px !important;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.refresh-btn:hover:not(:disabled) .iconify,
.refresh-btn:hover:not(:disabled) svg {
  color: #ffffff !important;
  fill: #ffffff !important;
}

.refresh-btn:active:not(:disabled) {
  transform: translateY(0);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
}

.refresh-btn:disabled .iconify,
.refresh-btn:disabled svg {
  color: rgba(255, 255, 255, 0.4) !important;
  fill: rgba(255, 255, 255, 0.4) !important;
}

.file-browser-content {
  display: flex;
  flex: 1;
  min-height: 0;
}

.file-tree-panel {
  flex: 0 0 300px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  overflow-y: auto;
}

.file-content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.7);
  gap: 12px;
}

.loading-state .iconify,
.error-state .iconify,
.empty-state .iconify {
  font-size: 32px;
}

.retry-btn {
  padding: 8px 16px;
  background: rgba(103, 126, 234, 0.2);
  border: 1px solid rgba(103, 126, 234, 0.3);
  border-radius: 6px;
  color: #677eea;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: rgba(103, 126, 234, 0.3);
}

.retry-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.waiting-for-files {
  color: #74c0fc;
}

.waiting-for-files .iconify {
  font-size: 48px;
  margin-bottom: 20px;
  opacity: 0.8;
}

.waiting-for-files .message-content {
  margin-bottom: 24px;
  text-align: center;
}

.waiting-for-files h3 {
  margin: 0 0 8px 0;
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
}

.waiting-for-files p {
  margin: 0 0 16px 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.5;
}

.waiting-for-files .tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(116, 192, 252, 0.1);
  border: 1px solid rgba(116, 192, 252, 0.2);
  border-radius: 8px;
  color: #74c0fc;
  font-size: 13px;
}

.waiting-for-files .tips .iconify {
  font-size: 16px;
  margin: 0;
}

.actual-error {
  color: #ff6b6b;
}

.file-tree {
  padding: 8px 0;
}

.file-content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ffffff;
}

.file-info .iconify {
  font-size: 18px;
}

.file-name {
  font-weight: 600;
}

.file-size {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.download-btn,
.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.download-btn:hover,
.close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-1px);
}

.file-content-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.loading-content,
.content-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: rgba(255, 255, 255, 0.7);
  gap: 12px;
}

.file-content {
  flex: 1;
  overflow: hidden;
}

.text-content {
  height: 100%;
  overflow: auto;
  padding: 20px;
}

.text-content pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #ffffff;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.binary-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: rgba(255, 255, 255, 0.7);
  gap: 16px;
}

.binary-content .iconify {
  font-size: 48px;
}

.download-btn-large {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(103, 126, 234, 0.2);
  border: 1px solid rgba(103, 126, 234, 0.3);
  border-radius: 8px;
  color: #677eea;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.download-btn-large:hover {
  background: rgba(103, 126, 234, 0.3);
  transform: translateY(-1px);
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Scrollbar styling */
.file-tree-panel::-webkit-scrollbar,
.text-content::-webkit-scrollbar {
  width: 6px;
}

.file-tree-panel::-webkit-scrollbar-track,
.text-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.file-tree-panel::-webkit-scrollbar-thumb,
.text-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.file-tree-panel::-webkit-scrollbar-thumb:hover,
.text-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
