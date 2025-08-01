<!--
  Copyright 2025 the original author or authors.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<template>
  <div class="grouped-select">
    <button class="select-btn" @click="toggleDropdown" :title="placeholder || ''">
      <span v-if="selectedOption" class="selected-text">
        {{ selectedOption.name }}
        <span class="model-category">[{{ selectedOption.category }}]</span>
      </span>
      <span v-else class="placeholder-text">{{ placeholder }}</span>
      <Icon icon="carbon:chevron-down" class="chevron" :class="{ 'rotated': isOpen }" />
    </button>

    <div v-if="isOpen" class="dropdown-overlay" @click="closeDropdown"></div>
    <div v-if="isOpen" class="dropdown-content">
      <div class="dropdown-header">
        <h3>{{ dropdownTitle }}</h3>
        <button class="close-btn" @click="closeDropdown">
          <Icon icon="carbon:close" />
        </button>
      </div>

      <div class="search-container">
        <input
          v-model="searchText"
          type="text"
          :placeholder="t('config.modelConfig.searchModels')"
          class="search-input"
        />
        <Icon icon="carbon:search" class="search-icon" />
      </div>

      <div class="groups-container">
        <div v-for="group in filteredGroups" :key="group.category" class="model-group">
          <div class="group-header">
            <span class="group-title">{{ group.category }}</span>
            <span class="group-count">({{ group.models.length }})</span>
          </div>
          <div class="models-grid">
            <button
              v-for="model in group.models"
              :key="model.id"
              class="model-option"
              :class="{ 'selected': model.id === modelValue }"
              @click="selectModel(model)"
              :title="model.description"
            >
              <div class="model-info">
                <div class="model-name">{{ model.name }}</div>
                <div class="model-description">{{ model.description }}</div>
              </div>
              <div class="model-category-tag">[{{ model.category }}]</div>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'

interface ModelOption {
  id: string
  name: string
  description: string
  category: string
}

interface Props {
  modelValue?: string
  options: ModelOption[]
  placeholder?: string
  dropdownTitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Please select a model',
  dropdownTitle: 'Available Models'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const isOpen = ref(false)
const searchText = ref('')

// Group models by category
const groupedOptions = computed(() => {
  const groups: { [key: string]: ModelOption[] } = {}

  props.options.forEach(option => {
    if (!groups[option.category]) {
      groups[option.category] = []
    }
    groups[option.category].push(option)
  })

  // Sort groups by priority
  const priorityOrder = ['Turbo', 'Plus', 'Max', 'Coder', 'Math', 'Vision', 'TTS', 'Standard']
  return priorityOrder
    .filter(category => groups[category])
    .map(category => ({
      category,
      models: groups[category].sort((a, b) => a.name.localeCompare(b.name))
    }))
})

// Filter search results
const filteredGroups = computed(() => {
  if (!searchText.value) {
    return groupedOptions.value
  }

  return groupedOptions.value.map(group => ({
    ...group,
    models: group.models.filter(model =>
      model.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
      model.description.toLowerCase().includes(searchText.value.toLowerCase()) ||
      model.category.toLowerCase().includes(searchText.value.toLowerCase())
    )
  })).filter(group => group.models.length > 0)
})

// Currently selected option
const selectedOption = computed(() => {
  return props.options.find(option => option.id === props.modelValue)
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    searchText.value = ''
  }
}

const closeDropdown = () => {
  isOpen.value = false
  searchText.value = ''
}

const selectModel = (model: ModelOption) => {
  emit('update:modelValue', model.id)
  closeDropdown()
}

// Close dropdown when clicking outside
const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement
  if (!target.closest('.grouped-select')) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.grouped-select {
  position: relative;
  width: 100%;
}

.select-btn {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;
  font-size: 14px;
}

.select-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.selected-text {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.placeholder-text {
  color: rgba(255, 255, 255, 0.5);
  flex: 1;
}

.model-category {
  color: #a8b3ff;
  font-size: 12px;
  font-weight: 500;
}

.chevron {
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s ease;
}

.chevron.rotated {
  transform: rotate(180deg);
}

.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.dropdown-content {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #1a1a1a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  z-index: 1000;
  max-height: 500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.dropdown-header h3 {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.search-container {
  position: relative;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.search-input:focus {
  border-color: #a8b3ff;
  background: rgba(255, 255, 255, 0.1);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.groups-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.model-group {
  margin-bottom: 16px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 0 8px;
}

.group-title {
  color: #a8b3ff;
  font-weight: 600;
  font-size: 14px;
}

.group-count {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.models-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
  padding: 0 8px;
}

.model-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
  min-height: 60px;
}

.model-option:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.model-option.selected {
  background: rgba(168, 179, 255, 0.2);
  border-color: #a8b3ff;
}

.model-info {
  flex: 1;
  width: 100%;
}

.model-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.9);
}

.model-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.3;
}

.model-category-tag {
  align-self: flex-end;
  color: #a8b3ff;
  font-size: 11px;
  font-weight: 500;
  margin-top: 4px;
}

/* Responsive design */
@media (max-width: 768px) {
  .models-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}

@media (max-width: 480px) {
  .models-grid {
    grid-template-columns: 1fr;
  }
}
</style>
