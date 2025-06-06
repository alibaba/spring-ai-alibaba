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
  <div class="input-area">
    <div class="input-container">
      <textarea
        v-model="currentInput"
        ref="inputRef"
        class="chat-input"
        :placeholder="placeholder"
        @keydown="handleKeydown"
        @input="adjustInputHeight"
      ></textarea>
      <button
        class="send-button"
        :disabled="!currentInput.trim() || disabled"
        @click="handleSend"
      >
        <Icon icon="carbon:send-alt" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { Icon } from '@iconify/vue'

interface Props {
  placeholder?: string
  disabled?: boolean
}

interface Emits {
  (e: 'send', message: string): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Ask a follow-up question or provide more details...',
  disabled: false,
})

const emit = defineEmits<Emits>()

const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')

const adjustInputHeight = () => {
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
      inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
    }
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!currentInput.value.trim() || props.disabled) return

  const message = currentInput.value
  currentInput.value = ''
  adjustInputHeight()
  
  emit('send', message)
}
</script>

<style lang="less" scoped>
.input-area {
  min-height: 112px;
  padding: 20px 24px;
  border-top: 1px solid #1a1a1a;
  background: rgba(255, 255, 255, 0.02);
}

.input-container {
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 50px 12px 16px;

  &:focus-within {
    border-color: #667eea;
  }
}

.chat-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 20px;
  max-height: 120px;

  &::placeholder {
    color: #666666;
  }
}

.send-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
