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
  <div class="plan-execution-container">
    <ChatContainer
      ref="chatRef"
      :initial-prompt="initialPrompt"
      @user-message-send-requested="handleMessageSent"
      @plan-update="handlePlanUpdate"
      @plan-completed="handlePlanCompleted"
      @step-selected="handleStepSelected"
      @dialog-round-start="handleDialogRoundStart"
    />

    <!-- Input Area -->
    <InputArea
      ref="inputRef"
      :disabled="isLoading"
      :placeholder="inputPlaceholder"
      @send="handleSendMessage"
      @clear="handleInputClear"
      @focus="handleInputFocus"
      @update-state="handleInputUpdateState"
      @plan-mode-clicked="handlePlanModeClicked"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, defineProps, defineEmits, watch, computed } from 'vue'
import ChatContainer from '@/components/chat/index.vue'
import InputArea from '@/components/input/index.vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import { useSidebarStore } from '@/stores/sidebar'
import { useI18n } from 'vue-i18n'

// Use i18n
const { t } = useI18n()

// Use pinia stores
const sidebarStore = useSidebarStore()

// Define props interface
interface Props {
  initialPrompt?: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialPrompt: '',
  placeholder: '',
})

// Define emits - Remove plan-update and step-selected as we're using store directly
interface Emits {
  (e: 'plan-completed', result: any): void
  (e: 'dialog-round-start', planId: string, query: string): void
  (e: 'message-sent', message: string): void
}

const emit = defineEmits<Emits>()

// Component state
const isLoading = ref(false)
const chatRef = ref()
const inputRef = ref()
const hasProcessedInitialPrompt = ref(false) // Mark whether the initial prompt has been processed

// Computed property to ensure placeholder is a string type
const inputPlaceholder = computed(() => {
  if (isLoading.value) {
    return t('input.waiting')
  }
  return props.placeholder || ''
})

onMounted(() => {
  console.log('[PlanExecutionComponent] Initialized')
  console.log('[PlanExecutionComponent] props.initialPrompt:', props.initialPrompt)

  // Set the event callbacks for the plan execution manager
  planExecutionManager.setEventCallbacks({
    onPlanUpdate: handlePlanManagerUpdate,
    onPlanCompleted: handlePlanManagerCompleted,
    onDialogRoundStart: handlePlanManagerDialogStart,
    onChatInputUpdateState: handlePlanManagerInputUpdate,
    onChatInputClear: handlePlanManagerInputClear,
  })

  // If there is an initial prompt, automatically send it (only once).
  if (props.initialPrompt && !hasProcessedInitialPrompt.value) {
    console.log('[PlanExecutionComponent] Auto-sending initial prompt:', props.initialPrompt)
    hasProcessedInitialPrompt.value = true
    handleUserMessageSendRequested(props.initialPrompt)
  } else {
    console.log('[PlanExecutionComponent] No initial prompt to send or already processed')
  }
})

// Watch for changes in initialPrompt
watch(
  () => props.initialPrompt,
  (newPrompt: string, oldPrompt: string) => {
    console.log('[PlanExecutionComponent] initialPrompt changed from:', oldPrompt, 'to:', newPrompt)
    // Send the request only when the new prompt is not empty, differs from the old prompt, and the initial prompt has not yet been processed.
    if (newPrompt && newPrompt !== oldPrompt && !hasProcessedInitialPrompt.value) {
      console.log('[PlanExecutionComponent] Auto-sending new initial prompt:', newPrompt)
      hasProcessedInitialPrompt.value = true
      handleUserMessageSendRequested(newPrompt)
    } else {
      console.log(
        '[PlanExecutionComponent] Not sending prompt - already processed or invalid conditions'
      )
    }
  },
  { immediate: false }
)

onUnmounted(() => {
  cleanup()
})

/**
 * Handle plan update events from the plan execution manager
 */
const handlePlanManagerUpdate = (planData: any) => {
  console.log('[PlanExecutionComponent] Received plan update from manager:', planData)

  // Pass the plan update to the chat container
  if (chatRef.value && typeof chatRef.value.handlePlanUpdate === 'function') {
    console.log('[PlanExecutionComponent] Calling chatRef.handlePlanUpdate with:', planData)
    chatRef.value.handlePlanUpdate(planData)
  } else {
    console.warn(
      '[PlanExecutionComponent] chatRef.value.handlePlanUpdate is not available:',
      chatRef.value
    )
  }

  // Update the loading state
  isLoading.value = !planData.completed
}

/**
 * Handle plan completion events from the plan execution manager
 */
const handlePlanManagerCompleted = (result: any) => {
  console.log('[PlanExecutionComponent] Received plan completed from manager:', result)

  // Pass the plan completion to the chat container
  if (chatRef.value && typeof chatRef.value.handlePlanCompleted === 'function') {
    chatRef.value.handlePlanCompleted(result)
  }

  // Update loading status
  isLoading.value = false

  // Emit an event to the parent component
  emit('plan-completed', result)
}

/**
 * Handle dialog round start events from the plan execution manager
 */
const handlePlanManagerDialogStart = (dialogData: any) => {
  console.log('[PlanExecutionComponent] Received dialog round start from manager:', dialogData)

  // Pass the dialog round start to the chat container
  if (chatRef.value && typeof chatRef.value.handleDialogRoundStart === 'function') {
    chatRef.value.handleDialogRoundStart(dialogData.planId, dialogData.query)
  }

  // Update loading status
  isLoading.value = true

  // Emit an event to the parent component
  emit('dialog-round-start', dialogData.planId, dialogData.query)
}

/**
 * Handle input state update events from the plan execution manager
 */
const handlePlanManagerInputUpdate = (inputData: any) => {
  console.log('[PlanExecutionComponent] Received input update from manager:', inputData)

  // Update the input area state
  if (inputData.enabled !== undefined) {
    isLoading.value = !inputData.enabled
  }
}

/**
 * Handle input clear events from the plan execution manager
 */
const handlePlanManagerInputClear = () => {
  console.log('[PlanExecutionComponent] Received input clear from manager')

  // Clear the input area
  if (inputRef.value && typeof inputRef.value.clear === 'function') {
    inputRef.value.clear()
  }
}

/**
 * Handle user message send requests
 */
const handleUserMessageSendRequested = async (query: string): Promise<void> => {
  console.log('[PlanExecutionComponent] handleUserMessageSendRequested called with query:', query)
  console.log('[PlanExecutionComponent] Current isLoading state:', isLoading.value)

  // Delegate to the plan execution manager
  await planExecutionManager.handleUserMessageSendRequested(query)
  console.log(
    '[PlanExecutionComponent] planExecutionManager.handleUserMessageSendRequested completed'
  )
}

/**
 * Clean up resources
 */
const cleanup = (): void => {
  planExecutionManager.cleanup()
}

// Input Area event handling
const handleSendMessage = (message: string) => {
  console.log('[PlanExecutionComponent] Send message from input:', message)
  // Instead of calling handleUserMessageSendRequested directly, handle it through ChatContainer
  // This can avoid repeated calls
  if (chatRef.value && typeof chatRef.value.handleSendMessage === 'function') {
    chatRef.value.handleSendMessage(message)
  }
}

const handleInputClear = () => {
  console.log('[PlanExecutionComponent] Input cleared')
  if (inputRef.value && typeof inputRef.value.clear === 'function') {
    inputRef.value.clear()
  }
}

const handleInputFocus = () => {
  console.log('[PlanExecutionComponent] Input focused')
}

const handleInputUpdateState = (enabled: boolean, placeholder?: string) => {
  console.log('[PlanExecutionComponent] Input state updated:', enabled, placeholder)
  isLoading.value = !enabled
}

const handlePlanModeClicked = () => {
  console.log('[PlanExecutionComponent] Plan mode button clicked, toggling sidebar')
  sidebarStore.toggleSidebar()
}

// Chat Container event handling
const handleMessageSent = (message: string) => {
  console.log('[PlanExecutionComponent] Message sent from chat container:', message)
  emit('message-sent', message)
  // Handle the user message send request from ChatContainer
  handleUserMessageSendRequested(message)
}

const handlePlanUpdate = (planData: any) => {
  console.log('[PlanExecutionComponent] Plan updated:', planData)
}

const handlePlanCompleted = (result: any) => {
  console.log('[PlanExecutionComponent] Plan completed:', result)
  emit('plan-completed', result)
}

const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[PlanExecutionComponent] Step selected:', planId, stepIndex)

  // Trigger an immediate progress refresh
  if (planExecutionManager.getActivePlanId() === planId) {
    console.log('[PlanExecutionComponent] Triggering immediate progress refresh for selected step')
    planExecutionManager.pollPlanStatusImmediately().catch(error => {
      console.warn('[PlanExecutionComponent] Failed to refresh progress immediately:', error)
    })
  }
}

const handleDialogRoundStart = (planId: string, query: string) => {
  console.log('[PlanExecutionComponent] Dialog round started:', planId, query)
  emit('dialog-round-start', planId, query)
}

// Methods exposed to parent components
defineExpose({
  getActivePlanId: () => planExecutionManager.getActivePlanId(),
  getState: () => planExecutionManager.getState(),
  cleanup,
  sendMessage: handleUserMessageSendRequested,
  getChatRef: () => chatRef.value,
  getInputRef: () => inputRef.value,
  getPlanExecutionManager: () => planExecutionManager,
})
</script>

<style lang="less" scoped>
.plan-execution-container {
  display: flex;
  flex-direction: column;
  flex: 1; /* Occupy the remaining space */
  height: 100%;
  min-height: 0; /* Ensure it can shrink */
  overflow: hidden; /* Prevent overflow */
}
</style>
