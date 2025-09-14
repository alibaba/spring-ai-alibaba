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
    <transition name="modal">
      <!-- Toast message box -->
      <div
        v-if="visible"
        class="toast-overlay"
        @click="hide"
      >
        <div
          class="toast-modal"
          :class="`toast--${type}`"
          @click.stop
        >
          <div class="toast-header">
            <Icon :icon="icon" class="toast-icon" />
            <span class="toast-title">{{ type === 'success' ? 'Success' : 'Error' }}</span>
          </div>
          <div class="toast-content">
            <span>{{ message }}</span>
          </div>
          <div class="toast-actions">
            <button class="toast-btn toast-btn--primary" @click="hide">
              Confirm
            </button>
          </div>
        </div>
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
 * @param customDuration - Optional custom duration in ms (ignored for modal style)
 */
const show = (msg:string, toastType = 'success', customDuration = 3000) => {
  message.value = msg
  type.value = toastType
  icon.value = toastType === 'success' ? 'carbon:checkmark' : 'carbon:error'
  duration.value = customDuration
  visible.value = true


  // Modal style toast doesn't auto-hide, user must click confirm
  // setTimeout(() => {
  //   visible.value = false
  // }, duration.value)
}

// Hide the toast manually
const hide = () => {
  visible.value = false
}

// Expose public API
defineExpose<ToastInstance>({ show })
</script>

<style scoped>
/* Toast overlay - full screen backdrop */
.toast-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999999 !important; /* Highest possible z-index */
  backdrop-filter: blur(4px);
}

/* Toast modal container */
.toast-modal {
  background: rgba(20, 20, 25, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 24px;
  min-width: 320px;
  max-width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px);
}

/* Toast header */
.toast-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toast-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.toast-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

/* Toast content */
.toast-content {
  margin-bottom: 20px;
  line-height: 1.5;
}

.toast-content span {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* Toast actions */
.toast-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.toast-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.toast-btn--primary {
  background: rgba(102, 126, 234, 0.8);
  color: #fff;
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.toast-btn--primary:hover {
  background: rgba(102, 126, 234, 1);
  border-color: rgba(102, 126, 234, 0.5);
  transform: translateY(-1px);
}

/* Success style */
.toast--success .toast-icon {
  color: #10b981;
}

.toast--success .toast-title {
  color: #10b981;
}

/* Error style */
.toast--error .toast-icon {
  color: #ef4444;
}

.toast--error .toast-title {
  color: #ef4444;
}

/* Modal transition animations */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .toast-modal,
.modal-leave-to .toast-modal {
  transform: scale(0.9) translateY(-20px);
}

.modal-enter-to .toast-modal,
.modal-leave-from .toast-modal {
  transform: scale(1) translateY(0);
}
</style>

