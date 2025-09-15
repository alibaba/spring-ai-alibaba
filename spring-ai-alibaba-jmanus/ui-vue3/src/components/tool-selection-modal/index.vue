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
  <Modal
    v-model="visible"
    :title="$t('toolSelection.title')"
    @confirm="handleConfirm"
    @update:model-value="handleCancel"
  >
    <div class="tool-selection-content">
      <!-- Search and Sort -->
      <div class="tool-controls">
        <div class="search-container">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            :placeholder="$t('toolSelection.searchPlaceholder')"
          />
        </div>
        <div class="sort-container">
          <select v-model="sortBy" class="sort-select">
            <option value="group">{{ $t('toolSelection.sortByGroup') }}</option>
            <option value="name">{{ $t('toolSelection.sortByName') }}</option>
            <option value="enabled">{{ $t('toolSelection.sortByStatus') }}</option>
          </select>
        </div>
      </div>

      <!-- Tool Statistics -->
      <div class="tool-summary">
        <span class="summary-text">
          {{ $t('toolSelection.summary', {
            groups: groupedTools.size,
            tools: totalTools,
            selected: selectedTools.length
          }) }}
        </span>
      </div>

      <!-- Tool Group List -->
      <div class="tool-groups" v-if="groupedTools.size > 0">
        <div
          v-for="[groupName, tools] in groupedTools"
          :key="groupName"
          class="tool-group"
        >
          <!-- Group Header -->
          <div
            class="tool-group-header"
            :class="{ collapsed: collapsedGroups.has(groupName) }"
            @click="toggleGroupCollapse(groupName)"
          >
            <div class="group-title-area">
              <Icon
                :icon="collapsedGroups.has(groupName) ? 'carbon:chevron-right' : 'carbon:chevron-down'"
                class="collapse-icon"
              />
              <Icon icon="carbon:folder" class="group-icon" />
              <span class="group-name">{{ groupName }}</span>
              <span class="group-count">
                ({{ getSelectedToolsInGroup(tools).length }}/{{ tools.length }})
              </span>
            </div>
            <div class="group-actions" @click.stop>
              <label class="group-enable-all">
                <input
                  type="checkbox"
                  class="group-enable-checkbox"
                  :checked="isGroupFullySelected(tools)"
                  @change="toggleGroupSelection(tools, $event)"
                  :data-group="groupName"
                />
                <span class="enable-label">{{ $t('toolSelection.enableAll') }}</span>
              </label>
            </div>
          </div>

          <!-- Group Content -->
          <div
            class="tool-group-content"
            :class="{ collapsed: collapsedGroups.has(groupName) }"
          >
            <div
              v-for="tool in tools.filter(t => t && t.key)"
              :key="tool.key"
              class="tool-selection-item"
            >
              <div class="tool-info">
                <div class="tool-selection-name">{{ tool.name }}</div>
                <div v-if="tool.description" class="tool-selection-desc">
                  {{ tool.description }}
                </div>
              </div>
              <div class="tool-actions">
                <label class="tool-enable-switch" @click.stop>
                  <input
                    type="checkbox"
                    class="tool-enable-checkbox"
                    :checked="isToolSelected(tool.key)"
                    @change="toggleToolSelection(tool.key, $event)"
                  />
                  <span class="tool-enable-slider"></span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state">
        <Icon icon="carbon:tools" class="empty-icon" />
        <p>{{ $t('toolSelection.noToolsFound') }}</p>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '../modal/index.vue'
import type { Tool } from '@/api/agent-api-service'

interface Props {
  modelValue: boolean
  tools: Tool[]
  selectedToolIds: string[]
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', selectedToolIds: string[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Reactive state
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const searchQuery = ref('')
const sortBy = ref<'group' | 'name' | 'enabled'>('group')
const collapsedGroups = ref(new Set<string>())
const selectedTools = ref<string[]>([])

// Set group checkbox indeterminate state
const updateGroupCheckboxState = (groupName: string, tools: Tool[]) => {
  const checkbox = document.querySelector(`input[data-group="${groupName}"]`) as HTMLInputElement | null
  if (checkbox) {
    checkbox.indeterminate = isGroupPartiallySelected(tools)
  }
}

// Initialize the selected tools
watch(
  () => props.selectedToolIds,
  (newIds) => {
    selectedTools.value = [...newIds]
  },
  { immediate: true }
)

// Filtered and sorted tools
const filteredTools = computed(() => {
  let filtered = props.tools.filter(tool => tool.key) // Filter out invalid tools

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(
      tool =>
        tool.name.toLowerCase().includes(query) ||
        tool.description.toLowerCase().includes(query) ||
        (tool.serviceGroup?.toLowerCase().includes(query) ?? false)
    )
  }

  // Sorting
  switch (sortBy.value) {
    case 'name':
      filtered = [...filtered].sort((a, b) => a.name.localeCompare(b.name))
      break
    case 'enabled':
      filtered = [...filtered].sort((a, b) => {
        const aSelected = selectedTools.value.includes(a.key)
        const bSelected = selectedTools.value.includes(b.key)
        if (aSelected && !bSelected) return -1
        if (!aSelected && bSelected) return 1
        return a.name.localeCompare(b.name)
      })
      break
    case 'group':
    default:
      filtered = [...filtered].sort((a, b) => {
        const groupA = a.serviceGroup ?? 'Ungrouped'
        const groupB = b.serviceGroup ?? 'Ungrouped'
        if (groupA !== groupB) {
          return groupA.localeCompare(groupB)
        }
        return a.name.localeCompare(b.name)
      })
      break
  }

  return filtered
})

// Tools grouped by service group
const groupedTools = computed(() => {
  const groups = new Map<string, Tool[]>()

  filteredTools.value.forEach(tool => {
    const groupName = tool.serviceGroup ?? 'Ungrouped'
    if (!groups.has(groupName)) {
      groups.set(groupName, [])
    }
    groups.get(groupName)!.push(tool)
  })

  // Sort the groups
  return new Map([...groups.entries()].sort())
})

// Total number of tools
const totalTools = computed(() => filteredTools.value.length)

// Watch for changes in the selected state and update the indeterminate state of group checkboxes
watch(
  [selectedTools, groupedTools],
  () => {
    nextTick(() => {
      for (const [groupName, tools] of groupedTools.value) {
        updateGroupCheckboxState(groupName, tools)
      }
    })
  },
  { flush: 'post', deep: false }
)

// Tool selection logic
const isToolSelected = (toolKey: string) => {
  return selectedTools.value.includes(toolKey)
}

const toggleToolSelection = (toolKey: string, event: Event) => {
  event.stopPropagation()
  const target = event.target as HTMLInputElement
  const isChecked = target.checked

  // Prevent undefined toolKey
  if (!toolKey) {
    console.error('toolKey is undefined, cannot proceed')
    return
  }

  if (isChecked) {
    // Add the tool to the selected list
    if (!selectedTools.value.includes(toolKey)) {
      selectedTools.value = [...selectedTools.value, toolKey]
    }
  } else {
    // Remove the tool from the selected list
    selectedTools.value = selectedTools.value.filter(id => id !== toolKey)
  }
}

// Group selection logic
const getSelectedToolsInGroup = (tools: Tool[]) => {
  return tools.filter(tool => selectedTools.value.includes(tool.key))
}

const isGroupFullySelected = (tools: Tool[]) => {
  return tools.length > 0 && tools.every(tool => selectedTools.value.includes(tool.key))
}

const isGroupPartiallySelected = (tools: Tool[]) => {
  const selectedCount = getSelectedToolsInGroup(tools).length
  return selectedCount > 0 && selectedCount < tools.length
}

const toggleGroupSelection = (tools: Tool[], event: Event) => {
  event.stopPropagation()
  const target = event.target as HTMLInputElement
  const isChecked = target.checked
  const toolKeys = tools.map(tool => tool.key)

  if (isChecked) {
    // Select all tools in the group - Use a new array to avoid reference issues
    const newSelected = [...selectedTools.value]
    toolKeys.forEach(key => {
      if (!newSelected.includes(key)) {
        newSelected.push(key)
      }
    })
    selectedTools.value = newSelected
  } else {
    // Deselect all tools in the group - Create a new array
    selectedTools.value = selectedTools.value.filter(id => !toolKeys.includes(id))
  }
}

// Group collapse logic
const toggleGroupCollapse = (groupName: string) => {
  if (collapsedGroups.value.has(groupName)) {
    collapsedGroups.value.delete(groupName)
  } else {
    collapsedGroups.value.add(groupName)
  }
}

// Dialog event handling
const handleConfirm = () => {
  emit('confirm', [...selectedTools.value])
  emit('update:modelValue', false)
}

const handleCancel = () => {
// Translate to English and follow frontend terminology
  selectedTools.value = [...props.selectedToolIds]
  emit('update:modelValue', false)
}

// When the modal opens, expand the first group by default and collapse the others
watch(visible, (newVisible) => {
  if (newVisible) {
    collapsedGroups.value.clear()
    const groupNames = Array.from(groupedTools.value.keys())
    if (groupNames.length > 1) {
      // Collapse all groups except the first one by default
      groupNames.slice(1).forEach(name => {
        collapsedGroups.value.add(name)
      })
    }
  }
})
</script>

<style scoped>
.tool-selection-content {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
}

.tool-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-container {
  flex: 1;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.sort-container {
  min-width: 140px;
}

.sort-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
}

.tool-summary {
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.summary-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}


.tool-group {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}

.tool-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-group-header:hover {
  background: rgba(255, 255, 255, 0.08);
}

.tool-group-header.collapsed {
  border-bottom: none;
}

.group-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.collapse-icon {
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s;
}

.group-icon {
  color: rgba(255, 255, 255, 0.8);
}

.group-name {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.group-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.group-actions {
  display: flex;
  align-items: center;
}

.group-enable-all {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.group-enable-checkbox {
  cursor: pointer;
}

.tool-group-content {
  max-height: 300px;
  overflow-y: auto;
  transition: all 0.3s;
}

.tool-group-content.collapsed {
  max-height: 0;
  overflow: hidden;
}

.tool-selection-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: background-color 0.3s;
}

.tool-selection-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.tool-selection-item:last-child {
  border-bottom: none;
}

.tool-info {
  flex: 1;
}

.tool-selection-name {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
}

.tool-selection-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  line-height: 1.4;
}

.tool-actions {
  margin-left: 12px;
}

.tool-enable-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
}

.tool-enable-checkbox {
  opacity: 0;
  width: 0;
  height: 0;
}

.tool-enable-slider {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  transition: all 0.3s;
}

.tool-enable-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s;
}

.tool-enable-checkbox:checked + .tool-enable-slider {
  background: rgba(102, 126, 234, 0.8);
}

.tool-enable-checkbox:checked + .tool-enable-slider:before {
  transform: translateX(20px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}
</style>
