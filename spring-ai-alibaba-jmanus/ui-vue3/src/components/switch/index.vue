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
  <label class="switch">
    <input type="checkbox" :checked="enabled" @change="handleChange" />
    <span class="slider"></span>
    <span class="switch-label">{{ label }}</span>
  </label>
</template>

<script setup lang="ts">
defineProps<{
  enabled: boolean
  label: string
}>()

const emit = defineEmits<{
  (e: 'update:switchValue', value: boolean): void
}>()

const handleChange = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  emit('update:switchValue', checked)
}
</script>

<style scoped>
.switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;

  & input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  .slider {
    position: relative;
    display: inline-block;
    width: 40px;
    height: 20px;
    background: rgba(234, 102, 102, 0.1);
    border: 1px solid rgba(234, 102, 102, 0.2);
    border-radius: 20px;
    transition: all 0.3s;
    flex-shrink: 0;
    vertical-align: middle;
    &:before {
      position: absolute;
      content: '';
      height: 16px;
      width: 16px;
      left: 2px;
      bottom: 1px;
      background: #ea6666;
      border-radius: 50%;
      transition: all 0.3s;
    }
  }

  .switch-label {
    margin-left: 8px;
    font-size: 14px;
    line-height: 20px;
    height: 20px;
    color: rgba(255, 255, 255, 0.6);
    transition: color 0.3s;
  }

  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    & input[type='checkbox'] {
      width: 16px;
      height: 16px;
    }
  }

  input {
    &:checked + .slider {
      background: rgba(102, 126, 234, 0.1);
      border-color: rgba(102, 126, 234, 0.2);
    }

    &:checked + .slider:before {
      transform: translateX(20px);
      background: #667eea;
    }

    &:checked + .slider + .switch-label {
      color: #667eea;
    }

    &:not(:checked) + .slider + .switch-label {
      color: #ea6666;
    }
  }
}
</style>
