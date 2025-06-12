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
      <button class="attach-btn" title="附加文件">
        <Icon icon="carbon:attachment" />
      </button>
      <textarea
        v-model="currentInput"
        ref="inputRef"
        class="chat-input"
        :placeholder="currentPlaceholder"
        :disabled="disabled"
        @keydown="handleKeydown"
        @input="adjustInputHeight"
      ></textarea>
      <button class="plan-mode-btn" title="进入计划模式" @click="handlePlanModeClick">
        <Icon icon="carbon:document" />
        计划模式
      </button>
      <button
        class="send-button"
        :disabled="!currentInput.trim() || disabled"
        @click="handleSend"
        title="发送"
      >
        <Icon icon="carbon:send-alt" />
        发送
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'

interface Props {
  placeholder?: string
  disabled?: boolean
}

interface Emits {
  (e: 'send', message: string): void
  (e: 'clear'): void
  (e: 'focus'): void
  (e: 'update-state', enabled: boolean, placeholder?: string): void
  (e: 'message-sent', message: string): void
  (e: 'plan-mode-clicked'): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '向 JTaskPilot 发送消息',
  disabled: false,
})

const emit = defineEmits<Emits>()

const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')
const currentPlaceholder = ref(props.placeholder)

// 监听全局事件来清空输入和更新状态
const eventBus = ref<any>()

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

  const query = currentInput.value.trim()
  
  // 使用 Vue 的 emit 发送消息
  emit('send', query)
  
  // 清空输入
  clearInput()
  
  // 发送消息已发送事件
  emit('message-sent', query)
}

const handlePlanModeClick = () => {
  // 触发计划模式切换事件
  emit('plan-mode-clicked')
}

/**
 * 清空输入框
 */
const clearInput = () => {
  currentInput.value = ''
  adjustInputHeight()
  emit('clear')
}

/**
 * 更新输入区域的状态（启用/禁用）
 * @param {boolean} enabled - 是否启用输入
 * @param {string} [placeholder] - 启用时的占位文本
 */
const updateState = (enabled: boolean, placeholder?: string) => {
  if (placeholder) {
    currentPlaceholder.value = enabled ? placeholder : '等待任务完成...'
  }
  emit('update-state', enabled, placeholder)
}

/**
 * 聚焦输入框
 */
const focus = () => {
  inputRef.value?.focus()
  emit('focus')
}

/**
 * 获取当前输入框的值
 * @returns {string} 当前输入框的文本值 (已去除首尾空格)
 */
const getQuery = () => {
  return currentInput.value.trim()
}

// 暴露方法给父组件使用
defineExpose({
  clearInput,
  updateState,
  getQuery,
  focus: () => inputRef.value?.focus()
})

onMounted(() => {
  // 组件挂载后的初始化逻辑
})

onUnmounted(() => {
  // 组件卸载前的清理逻辑
})
</script>

<style lang="less" scoped>
.input-area {
  min-height: 112px;
  padding: 20px 24px;
  border-top: 1px solid #1a1a1a;
  background: rgba(255, 255, 255, 0.02);
  /* 确保输入区域始终在底部 */
  flex-shrink: 0; /* 不会被压缩 */
  position: sticky; /* 固定在底部 */
  bottom: 0;
  z-index: 100;
  /* 添加轻微的阴影以区分消息区域 */
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(20px);
}

.input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 16px;

  &:focus-within {
    border-color: #667eea;
  }
}

.attach-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }
}

.chat-input {
  flex: 1;
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

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    
    &::placeholder {
      color: #444444;
    }
  }
}

.plan-mode-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: #667eea;
    transform: translateY(-1px);
  }
}

.send-button {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 12px;
  cursor: pointer;
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
