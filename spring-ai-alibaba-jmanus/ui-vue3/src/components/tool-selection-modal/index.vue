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
    title="选择工具"
    @confirm="handleConfirm"
    @update:model-value="handleCancel"
  >
    <div class="tool-selection-content">
      <!-- 搜索和排序 -->
      <div class="tool-controls">
        <div class="search-container">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="搜索工具..."
          />
        </div>
        <div class="sort-container">
          <select v-model="sortBy" class="sort-select">
            <option value="group">按服务组排序</option>
            <option value="name">按名称排序</option>
            <option value="enabled">按启用状态排序</option>
          </select>
        </div>
      </div>

      <!-- 工具统计 -->
      <div class="tool-summary">
        <span class="summary-text">
          共 {{ groupedTools.size }} 个服务组，{{ totalTools }} 个工具
          (已选择 {{ selectedTools.length }} 个)
        </span>
      </div>

      <!-- 工具组列表 -->
      <div class="tool-groups" v-if="groupedTools.size > 0">
        <div
          v-for="[groupName, tools] in groupedTools"
          :key="groupName"
          class="tool-group"
        >
          <!-- 组标题 -->
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
                <span class="enable-label">启用全部</span>
              </label>
            </div>
          </div>

          <!-- 组内容 -->
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

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <Icon icon="carbon:tools" class="empty-icon" />
        <p>没有找到工具</p>
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

// 响应式状态
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const searchQuery = ref('')
const sortBy = ref<'group' | 'name' | 'enabled'>('group')
const collapsedGroups = ref(new Set<string>())
const selectedTools = ref<string[]>([])

// 设置组复选框的 indeterminate 状态
const updateGroupCheckboxState = (groupName: string, tools: Tool[]) => {
  const checkbox = document.querySelector(`input[data-group="${groupName}"]`) as HTMLInputElement
  if (checkbox) {
    checkbox.indeterminate = isGroupPartiallySelected(tools)
  }
}

// 初始化选中的工具
watch(
  () => props.selectedToolIds,
  (newIds) => {
    selectedTools.value = [...newIds]
  },
  { immediate: true }
)

// 过滤和排序的工具
const filteredTools = computed(() => {
  let filtered = props.tools.filter(tool => tool && tool.key) // 过滤掉无效工具

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(
      tool =>
        tool.name.toLowerCase().includes(query) ||
        tool.description.toLowerCase().includes(query) ||
        (tool.serviceGroup && tool.serviceGroup.toLowerCase().includes(query))
    )
  }

  // 排序
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
        const groupA = a.serviceGroup || '未分组'
        const groupB = b.serviceGroup || '未分组'
        if (groupA !== groupB) {
          return groupA.localeCompare(groupB)
        }
        return a.name.localeCompare(b.name)
      })
      break
  }

  return filtered
})

// 按组分组的工具
const groupedTools = computed(() => {
  const groups = new Map<string, Tool[]>()
  
  filteredTools.value.forEach(tool => {
    const groupName = tool.serviceGroup || '未分组'
    if (!groups.has(groupName)) {
      groups.set(groupName, [])
    }
    groups.get(groupName)!.push(tool)
  })

  // 对组进行排序
  return new Map([...groups.entries()].sort())
})

// 总工具数
const totalTools = computed(() => filteredTools.value.length)

// 监听选中状态变化，更新组复选框的 indeterminate 状态
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

// 工具选择逻辑
const isToolSelected = (toolKey: string) => {
  return selectedTools.value.includes(toolKey)
}

const toggleToolSelection = (toolKey: string, event: Event) => {
  event.stopPropagation()
  const target = event.target as HTMLInputElement
  const isChecked = target.checked
  
  // 防止 undefined 的 toolKey
  if (!toolKey) {
    console.error('toolKey is undefined, cannot proceed')
    return
  }
  
  if (isChecked) {
    // 添加工具到选中列表
    if (!selectedTools.value.includes(toolKey)) {
      selectedTools.value = [...selectedTools.value, toolKey]
    }
  } else {
    // 从选中列表中移除工具
    selectedTools.value = selectedTools.value.filter(id => id !== toolKey)
  }
}

// 组选择逻辑
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
    // 选择组中所有工具 - 使用新数组避免引用问题
    const newSelected = [...selectedTools.value]
    toolKeys.forEach(key => {
      if (!newSelected.includes(key)) {
        newSelected.push(key)
      }
    })
    selectedTools.value = newSelected
  } else {
    // 取消选择组中所有工具 - 创建新数组
    selectedTools.value = selectedTools.value.filter(id => !toolKeys.includes(id))
  }
}

// 组折叠逻辑
const toggleGroupCollapse = (groupName: string) => {
  if (collapsedGroups.value.has(groupName)) {
    collapsedGroups.value.delete(groupName)
  } else {
    collapsedGroups.value.add(groupName)
  }
}

// 对话框事件处理
const handleConfirm = () => {
  emit('confirm', [...selectedTools.value])
  emit('update:modelValue', false)
}

const handleCancel = () => {
  // 重置为初始状态
  selectedTools.value = [...props.selectedToolIds]
  emit('update:modelValue', false)
}

// 当模态框打开时，默认展开第一个组，折叠其他组
watch(visible, (newVisible) => {
  if (newVisible) {
    collapsedGroups.value.clear()
    const groupNames = Array.from(groupedTools.value.keys())
    if (groupNames.length > 1) {
      // 除第一个组外，其他组默认折叠
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
