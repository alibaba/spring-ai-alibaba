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
  <div class="chat-message" :class="messageClasses">
    <!-- User message -->
    <UserMessage
      v-if="message.type === 'user'"
      :message="message"
    />
    
    <!-- Assistant message -->
    <AssistantMessage
      v-else-if="message.type === 'assistant'"
      :message="message"
      :is-streaming="isStreaming || false"
      @copy="handleCopy"
      @regenerate="handleRegenerate"
      @retry="handleRetry"
      @step-selected="handleStepSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import UserMessage from './UserMessage.vue'
import AssistantMessage from './AssistantMessage.vue'
import type { ChatMessage } from './composables/useChatMessages'
import { useMessageFormatting } from './composables/useMessageFormatting'

interface Props {
  message: ChatMessage
  isStreaming?: boolean
}

interface Emits {
  (e: 'copy', messageId: string): void
  (e: 'regenerate', messageId: string): void
  (e: 'retry', messageId: string): void
  (e: 'step-selected', stepId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { getMessageClasses } = useMessageFormatting()

// Computed properties
const messageClasses = computed(() => {
  return getMessageClasses(props.message)
})

// Event handlers
const handleCopy = (messageId: string) => {
  emit('copy', messageId)
}

const handleRegenerate = (messageId: string) => {
  emit('regenerate', messageId)
}

const handleRetry = (messageId: string) => {
  emit('retry', messageId)
}

const handleStepSelected = (stepId: string) => {
  emit('step-selected', stepId)
}
</script>

<style lang="less" scoped>
.chat-message {
  width: 100%;
  
  &.streaming {
    position: relative;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent, #4f46e5, transparent);
      animation: streaming-pulse 2s ease-in-out infinite;
    }
  }
}

@keyframes streaming-pulse {
  0% {
    transform: translateX(-100%);
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: translateX(100%);
    opacity: 0;
  }
}
</style>
