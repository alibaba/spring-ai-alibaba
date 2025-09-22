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
  <Teleport to="body">
    <Transition name="toast" appear>
      <div v-if="visible" class="message-toast" :class="type">
        <div class="toast-content">
          <i :class="getIcon()"></i>
          <span class="toast-text">{{ message }}</span>
        </div>
        <button v-if="closable" class="toast-close" @click="close">
          <i class="bi bi-x"></i>
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<script>
import { ref, onMounted } from 'vue'

export default {
  name: 'MessageToast',
  props: {
    message: {
      type: String,
      required: true
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'error', 'warning', 'info'].includes(value)
    },
    duration: {
      type: Number,
      default: 3000
    },
    closable: {
      type: Boolean,
      default: true
    }
  },
  emits: ['close'],
  setup(props, { emit }) {
    const visible = ref(false)

    const getIcon = () => {
      const icons = {
        success: 'bi bi-check-circle-fill',
        error: 'bi bi-exclamation-circle-fill',
        warning: 'bi bi-exclamation-triangle-fill',
        info: 'bi bi-info-circle-fill'
      }
      return icons[props.type]
    }

    const close = () => {
      visible.value = false
      setTimeout(() => {
        emit('close')
      }, 300)
    }

    onMounted(() => {
      visible.value = true
      
      if (props.duration > 0) {
        setTimeout(() => {
          close()
        }, props.duration)
      }
    })

    return {
      visible,
      getIcon,
      close
    }
  }
}
</script>

<style scoped>
.message-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  min-width: 320px;
  max-width: 480px;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 500;
}

.message-toast.success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.message-toast.error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
}

.message-toast.warning {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  color: #faad14;
}

.message-toast.info {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  color: #1890ff;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.toast-content i {
  font-size: 16px;
}

.toast-text {
  line-height: 1.4;
}

.toast-close {
  background: none;
  border: none;
  color: inherit;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  margin-left: 12px;
  opacity: 0.7;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.toast-close:hover {
  opacity: 1;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease-out;
}

.toast-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
