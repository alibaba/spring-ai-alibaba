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
  <div class="custom-select">
    <button class="select-btn" @click="toggleDropdown" :title="placeholder">
      <Icon :icon="props.icon || 'carbon:select-01'" width="18" />
      <span v-if="selectedOption" class="current-option">
        <span class="option-name">{{ selectedOption.name }}</span>
      </span>
      <span v-else class="current-option">
        {{ placeholder }}
      </span>
      <Icon
        :icon="showDropdown ? 'carbon:chevron-up' : 'carbon:chevron-down'"
        width="14"
        class="chevron"
      />
    </button>

    <transition name="slideDown">
      <div
        v-show="showDropdown"
        class="select-dropdown"
        :style="{ ...dropStyles, ...(props.direction === 'right' ? { right: 0 } : { left: 0 }) }"
        @click.stop
      >
        <div class="dropdown-header">
          <span>{{ dropdownTitle }}</span>
          <button class="close-btn" @click="showDropdown = false">
            <Icon icon="carbon:close" width="16" />
          </button>
        </div>
        <div class="select-options">
          <button
            v-for="option in options"
            :key="option.id"
            class="select-option"
            :class="{ active: isSelected(option) }"
            @click="selectOption(option)"
          >
            <span class="option-name">{{ option.name }}</span>
            <Icon v-if="isSelected(option)" icon="carbon:checkmark" width="16" class="check-icon" />
          </button>
        </div>
      </div>
    </transition>

    <!-- Backdrop -->
    <div v-if="showDropdown" class="backdrop" @click="showDropdown = false"></div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'

// Define props
const props = defineProps<{
  modelValue?: string | null
  options: Array<{ id: string; name: string }>
  placeholder: string
  dropdownTitle?: string
  icon?: string
  direction?: 'left' | 'right'
  dropStyles?: Record<string, string>
}>()

// Define emit
const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
}>()

// Control the display and hiding of the dropdown
const showDropdown = ref(false)

// Current selected option object
const selectedOption = computed(() => {
  return props.options.find(opt => opt.id === props.modelValue)
})

// Check if the option is selected
const isSelected = (option: { id: string }) => {
  return option.id === props.modelValue
}

// Toggle the dropdown
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

// Triggered when an option is selected
const selectOption = (option: { id: string }) => {
  emit('update:modelValue', option.id)
  showDropdown.value = false
}
</script>
<style scoped>
.custom-select {
  position: relative;
  display: inline-block;
}

.select-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: 1.5px solid #667eea;
  border-radius: 8px;
  color: #8da2fb;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 600;
  outline: none;
}

.select-btn:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: #7c9eff;
  color: #a3bffa;
  box-shadow: 0 0 15px rgba(102, 126, 234, 0.2);
}

.select-btn:focus {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.5);
}

.current-option {
  color: inherit;
  font-weight: 600;
  min-width: 40px;
  text-align: left;
  text-shadow: none;
}

.chevron {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.9;
  filter: none;
}

.select-dropdown {
  position: absolute;
  top: 100%;
  /* left: 0; */
  z-index: 9999;
  margin-top: 4px;
  background: linear-gradient(135deg, rgba(40, 40, 50, 0.95), rgba(30, 30, 40, 0.95));
  backdrop-filter: blur(16px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(102, 126, 234, 0.2);
  min-width: 300px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(102, 126, 234, 0.05));
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.select-options {
  padding: 8px 0;
}

.select-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.select-option:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
}

.select-option.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(102, 126, 234, 0.1));
  color: #7c9eff;
  border-left: 3px solid #667eea;
  padding-left: 13px;
}

.option-type {
  display: inline-block;
  min-width: 24px;
  font-size: 12px;
  font-weight: 600;
  opacity: 0.8;
}

.option-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  color: #667eea;
  opacity: 0.8;
}

.spacer {
  display: inline-block;
  width: 12px;
}

.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: transparent;
}

.slideDown-enter-active,
.slideDown-leave-active {
  transition: all 0.2s ease;
  transform-origin: top;
}

.slideDown-enter-from,
.slideDown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}
</style>
