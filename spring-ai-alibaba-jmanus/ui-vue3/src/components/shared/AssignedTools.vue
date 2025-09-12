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
  <div class="assigned-tools">
    <div class="section-header">
      <span>{{ title }} ({{ filteredSelectedToolIds.length }})</span>
      <button 
        class="action-btn small"
        @click="$emit('add-tools')"
        v-if="showAddButton"
      >
        <Icon icon="carbon:add" />
        {{ addButtonText }}
      </button>
    </div>

    <div class="tools-grid" :class="{ 'grid-layout': useGridLayout }">
      <div
        v-for="toolId in filteredSelectedToolIds" 
        :key="toolId"
        class="tool-item assigned"
      >
        <div class="tool-info">
          <span class="tool-name">{{ getToolDisplayName(toolId) }}</span>
          <span class="tool-desc">{{ getToolDescription(toolId) }}</span>
        </div>
      </div>

      <div v-if="filteredSelectedToolIds.length === 0" class="no-tools">
        <Icon icon="carbon:tool-box" />
        <span>{{ emptyText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, watch } from 'vue'

// Props
interface Props {
  title: string
  selectedToolIds: string[]
  availableTools: Array<{
    key: string
    name: string
    description: string
    enabled?: boolean
    serviceGroup?: string
  }>
  addButtonText?: string
  emptyText?: string
  showAddButton?: boolean
  useGridLayout?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  addButtonText: 'Add/Remove Tools',
  emptyText: 'No tools assigned',
  showAddButton: true,
  useGridLayout: false
})

// Emits
const emit = defineEmits<{
  'add-tools': []
  'tools-filtered': [filteredToolIds: string[]]
}>()

// Computed property to filter out tools that are not in availableTools
const filteredSelectedToolIds = computed(() => {
  return props.selectedToolIds.filter(toolId => 
    props.availableTools.some(tool => tool.key === toolId)
  )
})

// Watch for changes in filtered tools and emit event
watch(filteredSelectedToolIds, (newFilteredTools) => {
  // Only emit if there's a difference (some tools were filtered out)
  if (newFilteredTools.length !== props.selectedToolIds.length) {
    emit('tools-filtered', newFilteredTools)
  }
}, { immediate: true })

// Methods
const getToolDisplayName = (toolId: string): string => {
  const tool = props.availableTools.find(t => t.key === toolId)
  return tool ? tool.name : toolId
}

const getToolDescription = (toolId: string): string => {
  const tool = props.availableTools.find(t => t.key === toolId)
  return tool ? tool.description : ''
}
</script>

<style scoped>
.assigned-tools {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-header span {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
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
}

.action-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.small {
  padding: 6px 10px;
  font-size: 11px;
}

.tools-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tools-grid.grid-layout {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  max-height: 200px;
  overflow-y: auto;
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
}

.tools-grid.grid-layout .tool-item {
  padding: 12px;
  justify-content: flex-start;
}

.tool-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.tool-item.assigned {
  border-color: rgba(102, 126, 234, 0.3);
  background: rgba(102, 126, 234, 0.1);
}

.tool-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.tool-name {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.tool-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.3;
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

.tools-grid.grid-layout .no-tools {
  padding: 16px;
  font-size: 12px;
  grid-column: 1 / -1;
}

.no-tools svg {
  width: 20px;
  height: 20px;
  opacity: 0.5;
}
</style>
