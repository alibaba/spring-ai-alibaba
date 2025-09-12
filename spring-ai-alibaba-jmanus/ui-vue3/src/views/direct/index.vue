<!--
  /*
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
 */
-->

<template>
  <div class="direct-page">
    <div class="direct-chat">
      <Sidebar ref="sidebarRef" @planExecutionRequested="handlePlanExecutionRequested" />
      <!-- Left Panel - Chat -->
      <div class="left-panel" :style="{ width: computedLeftPanelWidth + '%' }">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>{{ $t('conversation') }}</h2>
          <div class="header-actions">
            <LanguageSwitcher />
            <button class="config-button" @click="newChat" :title="$t('memory.newChat')">
              <Icon icon="carbon:add" width="20" />
            </button>
            <button class="config-button" @click="handleConfig" :title="$t('direct.configuration')">
              <Icon icon="carbon:settings-adjust" width="20" />
            </button>
            <button class="cron-task-btn" @click="showCronTaskModal = true" :title="$t('cronTask.title')">
              <Icon icon="carbon:alarm" width="20" />
            </button>
            <button class="cron-task-btn" @click="memoryStore.toggleSidebar()" :title="$t('memory.selectMemory')">
              <Icon icon="carbon:calendar" width="20" />
            </button>
          </div>
        </div>

        <!-- Chat Container -->
        <div class="chat-content">
          <ChatContainer
            ref="chatRef"
            mode="direct"
            :initial-prompt="prompt || ''"
            @step-selected="handleStepSelected"
            @sub-plan-step-selected="handleSubPlanStepSelected"
          />
        </div>

        <!-- Input Area -->
        <InputArea
          :key="$i18n.locale"
          ref="inputRef"
          :disabled="isLoading"
          :placeholder="isLoading ? t('input.waiting') : t('input.placeholder')"
          :initial-value="prompt"
          @send="handleSendMessage"
          @clear="handleInputClear"
          @focus="handleInputFocus"
          @update-state="handleInputUpdateState"
          @plan-mode-clicked="handlePlanModeClicked"
        />
      </div>

      <!-- Resizer -->
      <div
        class="panel-resizer"
        @mousedown="startResize"
        @dblclick="resetPanelSize"
        :title="$t('direct.panelResizeHint')"
      >
        <div class="resizer-line"></div>
      </div>

      <!-- Right Panel - Preview -->
      <RightPanel ref="rightPanelRef" :style="{ width: 100 - leftPanelWidth + '%' }" :current-root-plan-id="currentRootPlanId" />
    </div>

    <!-- Cron Task Modal -->
    <CronTaskModal v-model="showCronTaskModal" />

    <!-- Memory Modal -->
    <Memory
        @memory-selected="memorySelected"
    />

    <!-- Message toast component -->
    <div v-if="message.show" class="message-toast" :class="message.type">
      <div class="message-content">
        <span>{{ message.text }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/Sidebar.vue'
import Memory from '@/components/memory/Memory.vue'
import RightPanel from '@/components/right-panel/RightPanel.vue'
import ChatContainer from '@/components/chat/ChatContainer.vue'
import InputArea from '@/components/input/InputArea.vue'
import LanguageSwitcher from '@/components/language-switcher/LanguageSwitcher.vue'
import CronTaskModal from '@/components/cron-task-modal/CronTaskModal.vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import { useTaskStore } from '@/stores/task'
import { sidebarStore } from '@/stores/sidebar'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import { useMessage } from '@/composables/useMessage'
import { memoryStore } from "@/stores/memory";
import type { InputMessage } from "@/stores/memory";
import { getUploadedFiles, hasUploadedFiles } from '@/stores/uploadedFiles'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()
const { t } = useI18n()
const { message } = useMessage()

const prompt = ref<string>('')
const inputOnlyContent = ref<string>('')
const rightPanelRef = ref()
const chatRef = ref()
const inputRef = ref()
const sidebarRef = ref()
const isExecutingPlan = ref(false)
const isLoading = ref(false)
const currentRootPlanId = ref<string | null>(null)
const showCronTaskModal = ref(false)

// Related to panel width
const leftPanelWidth = ref(50) // Left panel width percentage
const isResizing = ref(false)
const startX = ref(0)
const startLeftWidth = ref(0)

// Computed left panel width that adjusts based on sidebar width
const computedLeftPanelWidth = computed(() => {
  if (sidebarStore.isCollapsed) {
    return leftPanelWidth.value
  }
  
  // When sidebar is expanded, calculate available width for left panel
  // Get sidebar width from the sidebar component if available
  let sidebarWidth = 26 // Default sidebar width
  
  // Try to get actual sidebar width from the sidebar component
  if (sidebarRef.value && sidebarRef.value.sidebarWidth !== undefined) {
    sidebarWidth = sidebarRef.value.sidebarWidth
  }
  
  // Calculate maximum available width for left panel
  // Total width is 100%, so left panel max = 100% - sidebar width
  const maxAvailableWidth = 100 - sidebarWidth
  
  // Ensure left panel width doesn't exceed available space
  // Also maintain minimum width of 20%
  return Math.max(20, Math.min(maxAvailableWidth, leftPanelWidth.value))
})

onMounted(() => {
  console.log('[Direct] onMounted called')
  console.log('[Direct] taskStore.currentTask:', taskStore.currentTask)
  console.log('[Direct] taskStore.hasUnprocessedTask():', taskStore.hasUnprocessedTask())

  // Register event callbacks to planExecutionManager
  planExecutionManager.setEventCallbacks({
    onPlanUpdate: (rootPlanId: string) => {
      console.log('[Direct] Plan update event received for rootPlanId:', rootPlanId)

      if (!shouldProcessEventForCurrentPlan(rootPlanId)) {
        return
      }

      console.log('[Direct] Processing plan update for current rootPlanId:', rootPlanId)

      // Call chat component's handlePlanUpdate method
      if (chatRef.value && typeof chatRef.value.handlePlanUpdate === 'function') {
        console.log('[Direct] Calling chatRef.handlePlanUpdate with rootPlanId:', rootPlanId)
        chatRef.value.handlePlanUpdate(rootPlanId)
      } else {
        console.warn('[Direct] chatRef.handlePlanUpdate method not available')
      }

      // Call right panel component's updateDisplayedPlanProgress method
      if (rightPanelRef.value && typeof rightPanelRef.value.updateDisplayedPlanProgress === 'function') {
        console.log('[Direct] Calling rightPanelRef.updateDisplayedPlanProgress with rootPlanId:', rootPlanId)
        rightPanelRef.value.updateDisplayedPlanProgress(rootPlanId)
      } else {
        console.warn('[Direct] rightPanelRef.updateDisplayedPlanProgress method not available')
      }
    },

    onPlanCompleted: (rootPlanId: string) => {
      console.log('[Direct] Plan completed event received for rootPlanId:', rootPlanId)

      if (!shouldProcessEventForCurrentPlan(rootPlanId)) {
        return
      }

      console.log('[Direct] Processing plan completion for current rootPlanId:', rootPlanId)

      // Call chat component's handlePlanCompleted method
      if (chatRef.value && typeof chatRef.value.handlePlanCompleted === 'function') {
        const planDetails = planExecutionManager.getCachedPlanRecord(rootPlanId)
        console.log('[Direct] Calling chatRef.handlePlanCompleted with details:', planDetails)
        chatRef.value.handlePlanCompleted(planDetails ?? { planId: rootPlanId })
      } else {
        console.warn('[Direct] chatRef.handlePlanCompleted method not available')
      }

      // Clear current root plan ID when plan is completed
      currentRootPlanId.value = null
      console.log('[Direct] Cleared currentRootPlanId after plan completion')
    },

    onDialogRoundStart: (rootPlanId: string) => {
      console.log('[Direct] Dialog round start event received for rootPlanId:', rootPlanId)

      // Set current root plan ID when dialog starts
      currentRootPlanId.value = rootPlanId
      console.log('[Direct] Set currentRootPlanId to:', rootPlanId)

      // Call chat component's handleDialogRoundStart method (without query parameter)
      if (chatRef.value && typeof chatRef.value.handleDialogRoundStart === 'function') {
        console.log('[Direct] Calling chatRef.handleDialogRoundStart with planId:', rootPlanId)
        chatRef.value.handleDialogRoundStart(rootPlanId)
      } else {
        console.warn('[Direct] chatRef.handleDialogRoundStart method not available')
      }
    },

    onChatInputClear: () => {
      console.log('[Direct] Chat input clear event received')
      handleInputClear()
    },

    onChatInputUpdateState: (rootPlanId: string) => {
      console.log('[Direct] Chat input update state event received for rootPlanId:', rootPlanId)

      if (!shouldProcessEventForCurrentPlan(rootPlanId, true)) {
        return
      }

      const uiState = planExecutionManager.getCachedUIState(rootPlanId)
      if (uiState) {
        handleInputUpdateState(uiState.enabled, uiState.placeholder)
      }
    },

    onPlanError: (message: string) => {
      // No right panel notification needed, uncomment if needed
      // showMessage(`${t('planTemplate.executionFailed')}: ${message}`, 'error')
      chatRef.value.handlePlanError(message)
    },
  })

  console.log('[Direct] Event callbacks registered to planExecutionManager')

  // Initialize sidebar data
  sidebarStore.loadPlanTemplateList()

  // Check if there is a task in the store
  if (taskStore.hasUnprocessedTask() && taskStore.currentTask) {
    const taskContent = taskStore.currentTask.prompt
    console.log('[Direct] Found unprocessed task from store:', taskContent)
    // Mark the task as processed to prevent duplicate responses
    taskStore.markTaskAsProcessed()

    // Execute task directly without showing content in input box
    nextTick(async () => {
      try {
        console.log('[Direct] Calling handleChatSendMessage with taskContent:', taskContent)
        await handleChatSendMessage({
          input: taskContent
        })
      } catch (error) {
        console.warn('[Direct] handleChatSendMessage failed, falling back to prompt:', error)
        prompt.value = taskContent
      }
    })
  } else {
    // Check if there is a task to input (for pre-filling input without executing)
    const taskToInput = taskStore.getAndClearTaskToInput()
    if (taskToInput) {
      inputOnlyContent.value = taskToInput
      console.log('[Direct] Setting inputOnlyContent for input only:', inputOnlyContent.value)
      // Don't set prompt.value since this is just for input pre-filling
    } else {
      // Degrade to URL parameters (backward compatibility)
      prompt.value = (route.query.prompt as string) || ''
      console.log('[Direct] Received task from URL:', prompt.value)
      console.log('[Direct] No unprocessed task in store')
    }
  }

  // Restore panel width from localStorage
  const savedWidth = localStorage.getItem('directPanelWidth')
  if (savedWidth) {
    leftPanelWidth.value = parseFloat(savedWidth)
  }

  console.log('[Direct] Final prompt value:', prompt.value)

  // Set input content if there's inputOnlyContent (after component is mounted)
  if (inputOnlyContent.value) {
    nextTick(() => {
      if (inputRef.value && typeof inputRef.value.setInputValue === 'function') {
        inputRef.value.setInputValue(inputOnlyContent.value)
        console.log('[Direct] Set input value:', inputOnlyContent.value)
        inputOnlyContent.value = '' // Clear after setting
      }
    })
  }

  // Listen for plan-execution-requested events on window
  window.addEventListener('plan-execution-requested', ((event: CustomEvent) => {
    console.log('[DirectView] Received plan-execution-requested event:', event.detail)
    handlePlanExecutionRequested(event.detail)
  }) as EventListener)

  // No longer need to check pending plans in sessionStorage
  // Because emitPlanExecutionRequested in task.ts already sends events directly
})

// Listen for changes in the store's task (only handle unprocessed tasks)
watch(
  () => taskStore.currentTask,
  newTask => {
    console.log('[Direct] Watch taskStore.currentTask triggered, newTask:', newTask)
    if (newTask && !newTask.processed) {
      const taskContent = newTask.prompt
      taskStore.markTaskAsProcessed()
      console.log('[Direct] Received new task from store:', taskContent)

      // Execute task directly without showing content in input box
      nextTick(async () => {
        try {
          console.log('[Direct] Directly executing new task via handleChatSendMessage:', taskContent)
          await handleChatSendMessage({ input: taskContent })
        } catch (error) {
          console.warn('[Direct] handleChatSendMessage failed for new task:', error)
        }
      })
    } else {
      console.log('[Direct] Task is null or already processed, ignoring')
    }
  },
  { immediate: false }
)

// Listen for changes in prompt value, only for logging purposes
watch(
  () => prompt.value,
  (newPrompt, oldPrompt) => {
    console.log('[Direct] prompt value changed from:', oldPrompt, 'to:', newPrompt)
    // Prompt is now only used for input field initial value, no automatic execution
  },
  { immediate: false }
)

// Listen for changes in taskToInput (for handling cron task template setting)
watch(
  () => taskStore.taskToInput,
  (newTaskToInput) => {
    console.log('[Direct] Watch taskStore.taskToInput triggered, newTaskToInput:', newTaskToInput)
    if (newTaskToInput?.trim()) {
      console.log('[Direct] Setting input value from taskToInput:', newTaskToInput)
      nextTick(() => {
        if (inputRef.value && typeof inputRef.value.setInputValue === 'function') {
          inputRef.value.setInputValue(newTaskToInput.trim())
          console.log('[Direct] Input value set from taskToInput watch:', newTaskToInput.trim())
          // Clear the taskToInput after setting
          taskStore.getAndClearTaskToInput()
        }
      })
    }
  },
  { immediate: false }
)

onUnmounted(() => {
  console.log('[Direct] onUnmounted called, cleaning up resources')

  // Clear current root plan ID
  currentRootPlanId.value = null

  // Clean up plan execution manager resources
  planExecutionManager.cleanup()

  // Remove event listeners
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  window.removeEventListener('plan-execution-requested', ((event: CustomEvent) => {
    handlePlanExecutionRequested(event.detail)
  }) as EventListener)
})

// Methods related to panel size adjustment
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  startX.value = e.clientX
  startLeftWidth.value = leftPanelWidth.value

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'

  e.preventDefault()
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isResizing.value) return

  const containerWidth = window.innerWidth
  const deltaX = e.clientX - startX.value
  const deltaPercent = (deltaX / containerWidth) * 100

  let newWidth = startLeftWidth.value + deltaPercent

  // Limit panel width between 20% and 80%
  newWidth = Math.max(20, Math.min(80, newWidth))

  leftPanelWidth.value = newWidth
}

const handleMouseUp = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''

  // Save to localStorage
  localStorage.setItem('directPanelWidth', leftPanelWidth.value.toString())
}

const resetPanelSize = () => {
  leftPanelWidth.value = 50
  localStorage.setItem('directPanelWidth', '50')
}

// Helper function to check if the event should be processed for the current plan
const shouldProcessEventForCurrentPlan = (rootPlanId: string, allowSpecialIds: boolean = false): boolean => {
  // If no current plan is set, allow all events (initial state)
  if (!currentRootPlanId.value) {
    return true
  }

  // Check if this event is for the current active plan
  if (rootPlanId === currentRootPlanId.value) {
    return true
  }

  // Allow special IDs for UI state updates (error handling, etc.)
  if (allowSpecialIds && (rootPlanId === 'ui-state' || rootPlanId === 'error')) {
    return true
  }

  // Otherwise, ignore the event
  console.log('[Direct] Ignoring event for non-current rootPlanId:', rootPlanId, 'current:', currentRootPlanId.value)
  return false
}

// Handle message sending from ChatContainer via event
const handleChatSendMessage = async (query: InputMessage) => {
  let assistantMessage: any = null
  
  try {
    console.log('[DirectView] Processing send-message event:', query)
    
    // Add user message to UI
    const userMessage = chatRef.value?.addMessage('user', query.input)
    if ((query as any).attachments && userMessage) {
      chatRef.value?.updateMessage(userMessage.id, { attachments: (query as any).attachments })
    }

    // Add assistant thinking message
    assistantMessage = chatRef.value?.addMessage('assistant', '', {
      thinking: t('chat.thinkingProcessing')
    })

    if (assistantMessage) {
      chatRef.value?.startStreaming(assistantMessage.id)
    }

    // Import and call DirectApiService to send message to backend
    const { DirectApiService } = await import('@/api/direct-api-service')
    
    console.log('[DirectView] Calling DirectApiService.sendMessage')
    const response = await DirectApiService.sendMessage(query)
    console.log('[DirectView] API response received:', response)

    // Handle the response
    if (response.planId && assistantMessage) {
      // Plan mode: Update message with plan execution info
      chatRef.value?.updateMessage(assistantMessage.id, {
        thinking: t('chat.planningExecution'),
        planExecution: { 
          currentPlanId: response.planId,
          rootPlanId: response.planId,
          status: 'running'
        }
      })
      
      // Set current root plan ID for the new plan execution
      currentRootPlanId.value = response.planId
      console.log('[DirectView] Set currentRootPlanId to:', response.planId)
      
      // Start polling for plan updates
      planExecutionManager.handlePlanExecutionRequested(response.planId, query.input)
      console.log('[DirectView] Started polling for plan execution updates')
    } else if (assistantMessage) {
      // Direct mode: Show the response
      chatRef.value?.updateMessage(assistantMessage.id, {
        content: response.message || response.result || 'No response received from backend'
      })
      chatRef.value?.stopStreaming(assistantMessage.id)
    }
    
  } catch (error: any) {
    console.error('[DirectView] Send message failed:', error)
    
    // Show error message
    chatRef.value?.addMessage('assistant', `Error: ${error?.message || 'Failed to send message'}`)
    if (assistantMessage) {
      chatRef.value?.stopStreaming(assistantMessage.id)
    }
  }
}

// Event handler for input area send button
const handleSendMessage = async (message: InputMessage) => {
  console.log('[DirectView] Send message from input:', JSON.stringify(message))

  // Directly handle the message sending
  await handleChatSendMessage(message)
}

const handleInputClear = () => {
  console.log('[DirectView] Input cleared')
  if (inputRef.value && typeof inputRef.value.clear === 'function') {
    inputRef.value.clear()
  }
}

const handleInputFocus = () => {
  console.log('[DirectView] Input focused')
}

const handleInputUpdateState = (enabled: boolean, placeholder?: string) => {
  console.log('[DirectView] Input state updated:', enabled, placeholder)
  isLoading.value = !enabled
}

const handleStepSelected = (stepId: string) => {
  console.log('[DirectView] Step selected:', stepId)

  // Forward step selection to right panel
  if (rightPanelRef.value && typeof rightPanelRef.value.handleStepSelected === 'function') {
    console.log('[DirectView] Forwarding step selection to right panel:', stepId)
    rightPanelRef.value.handleStepSelected(stepId)
  } else {
    console.warn('[DirectView] rightPanelRef.handleStepSelected method not available')
  }
}

const handleSubPlanStepSelected = (parentPlanId: string, subPlanId: string, stepIndex: number, subStepIndex: number) => {
  console.log('[DirectView] Sub plan step selected:', {
    parentPlanId,
    subPlanId,
    stepIndex,
    subStepIndex
  })

  // Forward sub plan step selection to right panel
  if (rightPanelRef.value && typeof rightPanelRef.value.handleSubPlanStepSelected === 'function') {
    console.log('[DirectView] Forwarding sub plan step selection to right panel:', {
      parentPlanId,
      subPlanId,
      stepIndex,
      subStepIndex
    })
    rightPanelRef.value.handleSubPlanStepSelected(parentPlanId, subPlanId, stepIndex, subStepIndex)
  } else {
    console.warn('[DirectView] rightPanelRef.handleSubPlanStepSelected method not available')
  }
}

const handlePlanModeClicked = () => {
  console.log('[DirectView] Plan mode button clicked')
  // Toggle sidebar display state
  sidebarStore.toggleSidebar()
  console.log('[DirectView] Sidebar toggled, isCollapsed:', sidebarStore.isCollapsed)
}

const goBack = () => {
  router.push('/home')
}

const handleConfig = () => {
  router.push('/configs')
}

const handlePlanExecutionRequested = async (payload: {
  title: string
  planData: any
  params?: string | undefined
  replacementParams?: Record<string, string> | undefined
}) => {
  console.log('[DirectView] Plan execution requested:', payload)

  // Prevent duplicate execution
  if (isExecutingPlan.value) {
    console.log('[DirectView] Plan execution already in progress, ignoring request')
    return
  }

  isExecutingPlan.value = true

  // Mark whether user message has been added
  let userMessageAdded = false;
  let assistantMessage: any = null;

  // Add user and assistant messages using the same pattern as handleChatSendMessage
  try {
    console.log('[DirectView] Adding messages for plan execution:', payload.title)
    
    // Add user message
    chatRef.value?.addMessage('user', payload.title)
    userMessageAdded = true;
    
    // Add assistant message to show system feedback
    assistantMessage = chatRef.value?.addMessage('assistant', '', {
      thinking: t('chat.planningExecution')
    })
    
    if (assistantMessage) {
      chatRef.value?.startStreaming(assistantMessage.id)
      console.log('[DirectView] Added assistant message for plan execution:', assistantMessage.id)
    }
  } catch (messageError) {
    console.warn('[DirectView] Failed to add messages:', messageError)
  }
  try {
    // Get the plan template ID
    const planTemplateId = payload.planData?.planTemplateId || payload.planData?.id || payload.planData?.planId

    if (!planTemplateId) {
      throw new Error(t('direct.planTemplateIdNotFound'))
    }

    console.log(
      '[Direct] Executing plan with templateId:',
      planTemplateId,
      'params:',
      payload.params
    )

    // Call real API to execute plan
    console.log('[Direct] About to call PlanActApiService.executePlan')
    
    // Get uploaded files from global state
    const uploadedFiles = hasUploadedFiles() ? getUploadedFiles() : undefined
    console.log('[Direct] Executing with uploaded files:', uploadedFiles?.length ?? 0)
    console.log('[Direct] Executing with replacement params:', payload.replacementParams)
    
    let response
    if (payload.params?.trim()) {
      console.log('[Direct] Calling executePlan with rawParam:', payload.params.trim())
      response = await PlanActApiService.executePlan(planTemplateId, payload.params.trim(), uploadedFiles, payload.replacementParams)
    } else {
      console.log('[Direct] Calling executePlan without rawParam')
      response = await PlanActApiService.executePlan(planTemplateId, undefined, uploadedFiles, payload.replacementParams)
    }

    console.log('[Direct] Plan execution API response:', response)

    // Use the returned planId to start the plan execution process
    if (response.planId && assistantMessage) {
      console.log('[Direct] Got planId from response:', response.planId, 'starting plan execution')

      // Update assistant message with plan execution info
      chatRef.value?.updateMessage(assistantMessage.id, {
        thinking: t('chat.planningExecution'),
        planExecution: { 
          currentPlanId: response.planId,
          rootPlanId: response.planId,
          status: 'running'
        }
      })

      // Set current root plan ID for the new plan execution
      currentRootPlanId.value = response.planId
      console.log('[Direct] Set currentRootPlanId to:', response.planId)

      // Use planExecutionManager to handle plan execution
      console.log('[Direct] Delegating plan execution to planExecutionManager')
      planExecutionManager.handlePlanExecutionRequested(response.planId, payload.title)
    } else {
      console.error('[Direct] No planId in response:', response)
      throw new Error(t('direct.executionFailedNoPlanId'))
    }
  } catch (error: any) {
    console.error('[Direct] Plan execution failed:', error)
    console.error('[Direct] Error details:', { message: error.message, stack: error.stack })

    // Clear current root plan ID on error
    currentRootPlanId.value = null

    // Handle error messages using consistent pattern
    try {
      console.log('[Direct] Adding error messages to chat')
      
      // Only add user message if it hasn't been added before
      if (!userMessageAdded) {
        chatRef.value?.addMessage('user', payload.title)
      }
      
      // Update assistant message with error or add new error message
      if (assistantMessage) {
        chatRef.value?.updateMessage(assistantMessage.id, {
          content: `${t('direct.executionFailed')}: ${error.message || t('common.unknownError')}`,
          thinking: undefined
        })
        chatRef.value?.stopStreaming(assistantMessage.id)
      } else {
        chatRef.value?.addMessage('assistant', `${t('direct.executionFailed')}: ${error.message || t('common.unknownError')}`)
      }
    } catch (errorHandlingError) {
      console.error('[Direct] Failed to add error messages:', errorHandlingError)
      // Note: This would need toast import if used in this context
      alert(`${t('direct.executionFailed')}: ${error.message || t('common.unknownError')}`)
    }
  } finally {
    console.log('[Direct] Plan execution finished, resetting isExecutingPlan flag')
    isExecutingPlan.value = false
  }
}

const memorySelected = () => {
  chatRef.value.showMemory()
}

const newChat = () => {
  memoryStore.clearMemoryId()
  chatRef.value.newChat()
}
</script>

<style lang="less" scoped>
.direct-page {
  width: 100%;
  display: flex;
  position: relative;
}

.direct-chat {
  height: 100vh;
  width: 100%;
  background: #0a0a0a;
  display: flex;
}

.left-panel {
  position: relative;
  border-right: none; /* Remove the original border, which will be provided by the resizer */
  display: flex;
  flex-direction: column;
  height: 100vh; /* Use fixed height */
  overflow: hidden; /* Prevent panel itself overflow */
  transition: width 0.1s ease; /* Smooth transition */
}

.panel-resizer {
  width: 6px;
  height: 100vh;
  background: #1a1a1a;
  cursor: col-resize;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  flex-shrink: 0;

  &:hover {
    background: #2a2a2a;

    .resizer-line {
      background: #4a90e2;
      width: 2px;
    }
  }

  &:active {
    background: #3a3a3a;
  }
}

.resizer-line {
  width: 1px;
  height: 40px;
  background: #3a3a3a;
  border-radius: 1px;
  transition: all 0.2s ease;
}

/* Adjust right panel styles */
:deep(.right-panel) {
  transition: width 0.1s ease;
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.02);
  flex-shrink: 0; /* Ensure the header will not be compressed */
  position: sticky; /* Fix the header at the top */
  top: 0;
  z-index: 100;

  h2 {
    flex: 1;
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
  }
}

.chat-content {
  flex: 1; /* Occupy remaining space */
  display: flex;
  flex-direction: column;
  min-height: 0; /* Allow shrink */
  overflow: hidden; /* Prevent overflow */
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.config-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.cron-task-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.loading-prompt {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888888;
  font-size: 16px;
  padding: 50px;
}

/* Message toast styles */
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
  animation: slideInRight 0.3s ease-out;
  font-size: 14px;
  font-weight: 500;
}

.message-toast.error {
  color: #fff2f0;
  background-color: #ff4d4f;
}

.message-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  word-break: break-all;
}

.message-content i {
  font-size: 16px;
}

</style>
