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
  <div class="file-tree-node">
    <div
      class="node-content"
      :class="{
        'is-directory': node.type === 'directory',
        'is-file': node.type === 'file',
        'is-expanded': isExpanded
      }"
      :style="{ paddingLeft: `${level * 16 + 12}px` }"
      @click="handleClick"
      @contextmenu.prevent="handleRightClick"
    >
      <!-- Expand/Collapse Icon for directories -->
      <div
        v-if="node.type === 'directory'"
        class="expand-icon"
        @click.stop="toggleExpanded"
      >
        <Icon
          :icon="isExpanded ? 'carbon:chevron-down' : 'carbon:chevron-right'"
          class="chevron-icon"
        />
      </div>

      <!-- File/Directory Icon -->
      <div class="node-icon">
        <Icon :icon="getNodeIcon()" />
      </div>

      <!-- Name -->
      <span class="node-name">{{ node.name }}</span>

      <!-- File size for files -->
      <span v-if="node.type === 'file'" class="file-size">
        {{ formatFileSize(node.size) }}
      </span>

      <!-- Actions -->
      <div class="node-actions" v-if="showActions">
        <button
          v-if="node.type === 'file'"
          @click.stop="$emit('download-file', node)"
          class="action-btn download-btn"
          title="Download"
        >
          <Icon icon="carbon:download" />
        </button>
      </div>
    </div>

    <!-- Children (for directories) -->
    <div v-if="node.type === 'directory' && isExpanded && node.children" class="children">
      <FileTreeNode
        v-for="child in node.children"
        :key="child.path"
        :node="child"
        :level="level + 1"
        @file-selected="$emit('file-selected', $event)"
        @download-file="$emit('download-file', $event)"
      />
    </div>

    <!-- Context Menu -->
    <div
      v-if="showContextMenu"
      class="context-menu"
      :style="contextMenuStyle"
      @click.stop
    >
      <div class="context-menu-item" @click="handleFileSelect">
        <Icon icon="carbon:view" />
        <span>Open</span>
      </div>
      <div
        v-if="node.type === 'file'"
        class="context-menu-item"
        @click="handleDownload"
      >
        <Icon icon="carbon:download" />
        <span>Download</span>
      </div>
      <div class="context-menu-divider"></div>
      <div class="context-menu-item" @click="copyPath">
        <Icon icon="carbon:copy" />
        <span>Copy Path</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import { FileBrowserApiService, type FileNode } from '@/api/file-browser-api-service'

interface Props {
  node: FileNode
  level: number
}

interface Emits {
  (e: 'file-selected', file: FileNode): void
  (e: 'download-file', file: FileNode): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
// const { t } = useI18n()

// State
const isExpanded = ref(props.level === 0) // Root directory expanded by default
const showActions = ref(false)
const showContextMenu = ref(false)
const contextMenuStyle = ref({})

// Computed
// const hasChildren = computed(() => {
//   return props.node.type === 'directory' && props.node.children && props.node.children.length > 0
// })

// Methods
const toggleExpanded = () => {
  if (props.node.type === 'directory') {
    isExpanded.value = !isExpanded.value
  }
}

const handleClick = () => {
  if (props.node.type === 'directory') {
    toggleExpanded()
  } else {
    handleFileSelect()
  }
}

const handleFileSelect = () => {
  if (props.node.type === 'file') {
    emit('file-selected', props.node)
  }
  hideContextMenu()
}

const handleDownload = () => {
  emit('download-file', props.node)
  hideContextMenu()
}

const handleRightClick = (event: MouseEvent) => {
  event.preventDefault()
  showContextMenu.value = true

  // Position context menu
  // const rect = (event.target as HTMLElement).getBoundingClientRect()
  contextMenuStyle.value = {
    position: 'fixed',
    top: `${event.clientY}px`,
    left: `${event.clientX}px`,
    zIndex: 1000
  }

  // Add click listener to hide context menu
  setTimeout(() => {
    document.addEventListener('click', hideContextMenu)
  }, 0)
}

const hideContextMenu = () => {
  showContextMenu.value = false
  document.removeEventListener('click', hideContextMenu)
}

const copyPath = async () => {
  try {
    await navigator.clipboard.writeText(props.node.path)
    // You could show a toast message here
  } catch (err) {
    console.error('Failed to copy path:', err)
  }
  hideContextMenu()
}

const getNodeIcon = () => {
  if (props.node.type === 'directory') {
    return isExpanded.value ? 'carbon:folder-open' : 'carbon:folder'
  }
  return FileBrowserApiService.getFileIcon(props.node)
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// Lifecycle
onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style scoped>
.file-tree-node {
  position: relative;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 6px;
  margin: 1px 8px;
  position: relative;
}

.node-content:hover {
  background: rgba(255, 255, 255, 0.08);
}

.node-content.is-directory {
  font-weight: 500;
}

.expand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: color 0.2s ease;
}

.expand-icon:hover {
  color: #ffffff;
}

.chevron-icon {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.node-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.8);
}

.node-icon .iconify {
  font-size: 16px;
}

.node-name {
  flex: 1;
  color: #ffffff;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  white-space: nowrap;
}

.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.node-content:hover .node-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.action-btn .iconify {
  font-size: 12px;
}

.children {
  overflow: hidden;
}

.context-menu {
  background: rgba(20, 20, 20, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(20px);
  padding: 6px;
  min-width: 160px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  transition: background-color 0.2s ease;
}

.context-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.context-menu-item .iconify {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.context-menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 4px 0;
}

/* Directory specific styles */
.node-content.is-directory .node-name {
  color: #ffffff;
  font-weight: 500;
}

.node-content.is-directory.is-expanded .node-icon {
  color: #677eea;
}

/* File specific styles */
.node-content.is-file .node-name {
  color: rgba(255, 255, 255, 0.9);
}

/* Animation for expanding/collapsing */
.children {
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
