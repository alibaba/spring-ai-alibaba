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
  <div class="assistant-message">
    <!-- Thinking section (when available) -->
    <ThinkingSection
      v-if="message.thinkingDetails"
      :thinking-details="message.thinkingDetails"
    />
    
    <!-- Response section -->
    <ResponseSection
      v-if="message.content || message.error || isStreaming"
      :content="message.content || ''"
      :is-streaming="isStreaming || false"
      v-bind="message.error ? { error: message.error } : {}"
      :timestamp="message.timestamp"
      @copy="handleCopy"
      @regenerate="handleRegenerate"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import ThinkingSection from './ThinkingSection.vue'
import ResponseSection from './ResponseSection.vue'
import type { ChatMessage } from './composables/useChatMessages'

interface Props {
  message: ChatMessage
  isStreaming?: boolean
}

interface Emits {
  (e: 'copy', messageId: string): void
  (e: 'regenerate', messageId: string): void
  (e: 'retry', messageId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Event handlers
const handleCopy = () => {
  emit('copy', props.message.id)
}

const handleRegenerate = () => {
  emit('regenerate', props.message.id)
}

const handleRetry = () => {
  emit('retry', props.message.id)
}
</script>

<style lang="less" scoped>
.assistant-message {
  margin-bottom: 24px;
  
  // Add spacing between thinking and response sections
  > * + * {
    margin-top: 16px;
  }
}

@media (max-width: 768px) {
  .assistant-message {
    margin-bottom: 20px;
    
    > * + * {
      margin-top: 12px;
    }
  }
}
</style>
