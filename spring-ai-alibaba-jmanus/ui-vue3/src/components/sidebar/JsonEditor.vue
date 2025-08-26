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
  <div class="config-section">
    <div class="section-header">
      <Icon icon="carbon:code" width="16" />
      <span>{{ $t('sidebar.jsonTemplate') }}</span>
      <div class="section-actions">
        <button
          class="btn btn-sm"
          @click="handleRollback"
          :disabled="!canRollback"
          :title="$t('sidebar.rollback')"
        >
          <Icon icon="carbon:undo" width="14" />
        </button>
        <button
          class="btn btn-sm"
          @click="handleRestore"
          :disabled="!canRestore"
          :title="$t('sidebar.restore')"
        >
          <Icon icon="carbon:redo" width="14" />
        </button>
        <button
          class="btn btn-primary btn-sm"
          @click="handleSave"
          :disabled="isGenerating || isExecuting"
        >
          <Icon icon="carbon:save" width="14" />
        </button>
      </div>
    </div>
    <textarea
      v-model="formattedJsonContent"
      class="json-editor"
      :placeholder="$t('sidebar.jsonPlaceholder')"
      rows="12"
    ></textarea>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'



// Props
interface Props {
  jsonContent: string
  canRollback: boolean
  canRestore: boolean
  isGenerating: boolean
  isExecuting: boolean
  hiddenFields?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  hiddenFields: () => ['currentPlanId', 'userRequest', 'rootPlanId']
})

// Emits
const emit = defineEmits<{
  rollback: []
  restore: []
  save: []
  'update:jsonContent': [value: string]
}>()

// Computed property for formatted JSON content
const formattedJsonContent = computed({
  get() {
    try {
      if (!props.jsonContent) return ''

      const parsed = JSON.parse(props.jsonContent)

      // Remove hidden fields for display
      const filtered = { ...parsed }
      props.hiddenFields.forEach(field => {
        delete filtered[field]
      })

      // Return formatted JSON
      return JSON.stringify(filtered, null, 2)
    } catch {
      // If parsing fails, return original content
      return props.jsonContent
    }
  },
  set(value: string) {
    try {
      if (!value.trim()) {
        emit('update:jsonContent', '')
        return
      }

      const parsed = JSON.parse(value)

      // Get original data to preserve hidden fields
      let originalData: any = {}
      try {
        originalData = JSON.parse(props.jsonContent || '{}')
      } catch {
        // If original is not valid JSON, start fresh
      }

      // Merge user input with preserved hidden fields
      const merged: any = { ...parsed }
      props.hiddenFields.forEach(field => {
        if (originalData[field] !== undefined) {
          merged[field] = originalData[field]
        }
      })

      emit('update:jsonContent', JSON.stringify(merged))
    } catch {
      // If parsing fails, store as-is
      emit('update:jsonContent', value)
    }
  }
})

// Methods
const handleRollback = () => {
  emit('rollback')
}

const handleRestore = () => {
  emit('restore')
}

const handleSave = () => {
  emit('save')
}
</script>

<style scoped>
.config-section {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
  gap: 8px;
}

.section-actions {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

.json-editor {
  width: 100%;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: white;
  font-size: 11px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  padding: 8px;
  resize: vertical;
  min-height: 200px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: break-word;
  tab-size: 2;
  font-variant-ligatures: none;
}

.json-editor:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.json-editor::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 11px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
</style>
