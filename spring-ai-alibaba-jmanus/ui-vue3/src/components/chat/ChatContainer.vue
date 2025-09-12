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
  <div class="chat-container">
    <!-- Messages container -->
    <div 
      class="messages" 
      ref="messagesRef" 
      @scroll="handleScroll"
      @click="handleMessageContainerClick"
    >
      <!-- Message list -->
      <ChatMessage
        v-for="message in compatibleMessages"
        :key="message.id"
        :message="message"
        :is-streaming="isMessageStreaming(message.id)"
        @copy="handleCopyMessage"
        @regenerate="handleRegenerateMessage"
        @retry="handleRetryMessage"
        @step-selected="handleStepSelected"
      />
      
      <!-- Loading indicator -->
      <div v-if="isLoading" class="loading-message">
        <div class="loading-content">
          <Icon icon="carbon:circle-dash" class="loading-icon" />
          <span>{{ $t('chat.processing') }}</span>
        </div>
      </div>
    </div>

    <!-- Scroll to bottom button -->
    <Transition name="scroll-button">
      <button
        v-if="showScrollToBottom"
        class="scroll-to-bottom"
        @click="() => scrollToBottom()"
        :title="$t('chat.scrollToBottom')"
      >
        <Icon icon="carbon:chevron-down" />
      </button>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'

// Import new modular components
import ChatMessage from './ChatMessage.vue'

// Import composables
import { useChatMessages, convertMessageToCompatible } from './composables/useChatMessages'
import { useScrollBehavior } from './composables/useScrollBehavior'

// Import plan execution manager
import { planExecutionManager } from '@/utils/plan-execution-manager'

interface Props {
  mode?: 'plan' | 'direct'
  initialPrompt?: string
}

interface Emits {
  (e: 'step-selected', stepId: string): void
}

// InputMessage interface removed - not needed in display component

withDefaults(defineProps<Props>(), {
  mode: 'plan',
  initialPrompt: ''
})

const emit = defineEmits<Emits>()

// Initialize composables
const { t } = useI18n()

// Chat messages state
const {
  messages,
  isLoading,
  streamingMessageId,
  addMessage,
  updateMessage,
  startStreaming,
  stopStreaming,
  findMessage
} = useChatMessages()

// Scroll behavior
const messagesRef = ref<HTMLElement | null>(null)
const {
  scrollToBottom,
  autoScrollToBottom,
  showScrollToBottom
} = useScrollBehavior(messagesRef)

// Local state
const pollingInterval = ref<number>()

// Computed properties
const isMessageStreaming = (messageId: string) => {
  return streamingMessageId.value === messageId
}

// Convert messages to compatible format for ChatMessage component
const compatibleMessages = computed(() => {
  return messages.value.map(convertMessageToCompatible)
})

// Event handlers
const handleScroll = () => {
  // Scroll behavior is handled by useScrollBehavior composable
}

const handleMessageContainerClick = (event: Event) => {
  // Handle markdown copy buttons and other click events
  const target = event.target as HTMLElement
  
  if (target.classList.contains('md-copy-btn')) {
    const rawText = target.getAttribute('data-raw')
    if (rawText) {
      const text = decodeURIComponent(rawText)
      navigator.clipboard.writeText(text).then(() => {
        // Show copy feedback
        target.textContent = t('chat.copied')
        setTimeout(() => {
          target.textContent = t('chat.copy')
        }, 1000)
      })
    }
  }
}

const handleCopyMessage = async (messageId: string) => {
  const message = findMessage(messageId)
  if (!message) return
  
  try {
    // Strip HTML tags for clipboard
    const plainText = message.content.replace(/<[^>]*>/g, '')
    await navigator.clipboard.writeText(plainText)
    // Could add toast notification here
  } catch (error) {
    console.error('Failed to copy message:', error)
  }
}

const handleRegenerateMessage = (messageId: string) => {
  // Implementation for regenerating assistant response
  const message = findMessage(messageId)
  if (message && message.type === 'assistant') {
    // Reset message content and restart generation
    updateMessage(messageId, {
      content: ''
    })
    startStreaming(messageId)
    // Trigger regeneration logic here
  }
}

const handleRetryMessage = (messageId: string) => {
  // Implementation for retrying failed message
  const message = findMessage(messageId)
  if (message) {
    updateMessage(messageId, {
      content: ''
    })
    startStreaming(messageId)
    // Trigger retry logic here
  }
}

const handleStepSelected = (stepId: string) => {
  console.log('[ChatContainer] Step selected:', stepId)
  emit('step-selected', stepId)
}

// Message handling methods removed - ChatContainer is now a pure display component

// Plan execution handlers
const handlePlanUpdate = (rootPlanId: string) => {
  console.log('[ChatContainer] Plan update received:', rootPlanId)
  
  // Get the PlanExecutionRecord from the cache
  const planDetails = planExecutionManager.getCachedPlanRecord(rootPlanId)
  
  if (!planDetails) {
    console.warn('[ChatContainer] No cached plan data found for rootPlanId:', rootPlanId)
    return
  }
  
  console.log('[ChatContainer] Retrieved plan details from cache:', planDetails)
  
  // Find the corresponding message
  const messageIndex = messages.value.findIndex(
    m => m.planExecution?.currentPlanId === planDetails.currentPlanId && m.type === 'assistant'
  )
  
  if (messageIndex !== -1) {
    const message = messages.value[messageIndex]
    
    // Update planExecution data using updateMessage
    const updates: any = {
      planExecution: JSON.parse(JSON.stringify(planDetails))
    }
    
    // Handle simple responses (cases without agent execution sequence)
    if (!planDetails.agentExecutionSequence || planDetails.agentExecutionSequence.length === 0) {
      console.log('[ChatContainer] Handling simple response without agent execution sequence')
      
      if (planDetails.completed) {
        // Clear thinking state and set final response
        updates.thinking = ''
        const finalResponse = planDetails.summary ?? planDetails.result ?? planDetails.message ?? 'Execution completed'
        updates.content = finalResponse
        console.log('[ChatContainer] Set simple response content:', finalResponse)
      }
    } else {
      console.log('[ChatContainer] Handling detailed plan with agent execution sequence')
      // This is a detailed plan with execution steps, keep the plan execution display
    }
    
    // Update the message
    updateMessage(message.id, updates)
  }
}

const handlePlanCompleted = (planDetails: any) => {
  console.log('[ChatContainer] Plan completed:', planDetails)
  
  if (planDetails.rootPlanId) {
    const messageIndex = messages.value.findIndex(
      m => m.planExecution?.currentPlanId === planDetails.rootPlanId
    )
    
    if (messageIndex !== -1) {
      const message = messages.value[messageIndex]
      
      const summary = planDetails.summary ?? planDetails.result ?? 'Execution completed'
      updateMessage(message.id, {
        thinking: '',
        content: summary
      })
      console.log('[ChatContainer] Updated completed message:', summary)
    }
  }
}

const handleDialogRoundStart = (planId: string) => {
  console.log('[ChatContainer] Dialog round start:', planId)
  // This method can be used to initialize plan execution state
}

const handlePlanError = (message: string) => {
  console.log('[ChatContainer] Plan error:', message)
  
  // Show error message
  addMessage('assistant', `Error: ${message}`)
  console.error('[ChatContainer] Plan execution error:', message)
}

// Scroll handlers (remove unused function)

// Lifecycle
onMounted(() => {
  // Initial prompt processing removed - handled by parent component

  // Auto-scroll to bottom when new messages are added
  watch(messages, () => {
    nextTick(() => {
      autoScrollToBottom()
    })
  }, { deep: true })

  // Register plan execution callbacks
  planExecutionManager.setEventCallbacks({
    onPlanUpdate: handlePlanUpdate,
    onPlanCompleted: handlePlanCompleted,
    onDialogRoundStart: handleDialogRoundStart,
    onPlanError: handlePlanError
  })
})

onUnmounted(() => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }
})

// Expose methods for parent components
defineExpose({
  scrollToBottom,
  handlePlanUpdate,
  handlePlanCompleted,
  handleDialogRoundStart,
  handlePlanError,
  addMessage,
  updateMessage,
  startStreaming,
  stopStreaming
})
</script>

<style lang="less" scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background: #1a1a1a;
  
  .messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    scroll-behavior: smooth;
    
    // Custom scrollbar
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 3px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.3);
      border-radius: 3px;
      
      &:hover {
        background: rgba(255, 255, 255, 0.5);
      }
    }
  }
  
  .loading-message {
    display: flex;
    justify-content: center;
    padding: 20px;
    
    .loading-content {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #aaaaaa;
      font-size: 14px;
      
      .loading-icon {
        font-size: 16px;
        animation: spin 1s linear infinite;
      }
    }
  }
  
  .scroll-to-bottom {
    position: absolute;
    bottom: 30px;
    right: 30px;
    width: 40px;
    height: 40px;
    background: rgba(79, 70, 229, 0.9);
    border: none;
    border-radius: 50%;
    color: #ffffff;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    transition: all 0.2s ease;
    z-index: 10;
    
    &:hover {
      background: rgba(79, 70, 229, 1);
      transform: scale(1.1);
    }
    
    svg {
      font-size: 20px;
    }
  }
}

// Transitions
.scroll-button-enter-active,
.scroll-button-leave-active {
  transition: all 0.3s ease;
}

.scroll-button-enter-from,
.scroll-button-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .chat-container {
    .messages {
      padding: 16px;
    }
    
    .scroll-to-bottom {
      bottom: 20px;
      right: 20px;
      width: 36px;
      height: 36px;
      
      svg {
        font-size: 18px;
      }
    }
  }
}
</style>
