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
  <!-- Toast notification container -->
  <Teleport to="body">
    <transition name="slide">
      <!-- Toast message box -->
      <div
        v-if="visible"
        class="toast"
        :class="`toast--${type}`"
        @click="hide"
      >
        <Icon :icon="icon" class="toast-icon" />
        <span>{{ message }}</span>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'

// Interface for exposing toast methods
interface ToastInstance {
  show: (message: string, type?: 'success' | 'error', duration?: number) => void
}

// Toast visibility state
const visible = ref(false)
// Message content
const message = ref('')
// Toast type: success / error
const type = ref('success')
const icon = ref('carbon:checkmark')
// Duration before auto-hide
const duration = ref(3000)

/**
 * Show the toast message
 * @param msg - Message text to display
 * @param toastType - Type of toast: 'success' or 'error'
 * @param customDuration - Optional custom duration in ms
 */
const show = (msg:string, toastType = 'success', customDuration = 3000) => {
  message.value = msg
  type.value = toastType
  icon.value = toastType === 'success' ? 'carbon:checkmark' : 'carbon:error'
  duration.value = customDuration
  visible.value = true

  setTimeout(() => {
    visible.value = false
  }, duration.value)
}

// Hide the toast manually
const hide = () => {
  visible.value = false
}

// Expose public API
defineExpose<ToastInstance>({ show })
</script>

<style scoped>
/* Base toast styles */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #fff;
  z-index: 9999;
  cursor: pointer;
  animation: slideIn 0.3s ease;
}

/* Success style */
.toast--success {
  background: rgba(102, 234, 102, 0.9);
  border: 1px solid rgba(102, 234, 102, 0.5);
}

/* Error style */
.toast--error {
  background: rgba(234, 102, 102, 0.9);
  border: 1px solid rgba(234, 102, 102, 0.5);
}
</style>

