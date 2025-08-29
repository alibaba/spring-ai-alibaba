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
    <div class="messages" ref="messagesRef" @click="handleMessageContainerClick">
      <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="{ user: message.type === 'user', assistant: message.type === 'assistant' }"
      >
        <div class="message-content">
          <!-- User message section -->
          <div v-if="message.type === 'user'" class="user-message">
            {{ message.content }}
          </div>

          <!-- Three-part structure of assistant message -->
          <div v-else class="assistant-message">
            <!-- 1. JManus Thinking/Processing Section - Only displayed when there is processing content -->
            <div
                class="thinking-section"
                v-if="
                message.thinking ||
                (message.planExecution?.steps?.length ?? 0) > 0
              "
            >
              <div class="thinking-header">
                <div class="thinking-avatar">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                </div>
                <div class="thinking-label">{{ $t('chat.thinkingLabel') }}</div>
              </div>

              <div class="thinking-content">
                <!-- Basic thinking state -->
                <div class="thinking" v-if="message.thinking">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                  <span>{{ message.thinking }}</span>
                </div>

                <!-- Execution Details Component -->
                <ExecutionDetails
                    v-if="message.planExecution"
                    :plan-execution="message.planExecution"
                    :step-actions="message.stepActions || []"
                    :generic-input="message.genericInput || ''"
                    @step-selected="(stepIndex: number) => handleStepClick(message, stepIndex)"
                    @sub-plan-step-selected="(stepIndex: number, subStepIndex: number) => handleSubPlanStepClick(message, stepIndex, subStepIndex)"
                    @user-input-submitted="(inputData: any) => handleUserInputSubmit(message, inputData)"
                />



                <!-- Display the default processing state only when there is no final content -->
                <div
                    v-else-if="
                    !message.content && message.thinking
                  "
                    class="default-processing"
                >
                  <div class="processing-indicator">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span>{{ message.thinking ?? $t('chat.thinkingProcessing') }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. JManus Final Response Section - Independent humanized dialogue unit -->
            <div class="response-section">
              <div class="response-header">
                <div class="response-avatar">
                  <Icon icon="carbon:bot" class="bot-icon" />
                </div>
                <div class="response-name">{{ $t('chat.botName') }}</div>
              </div>
              <div class="response-content">
                <div v-if="message.content" class="final-response">
                  <div class="response-text" v-html="formatResponseText(message.content)"></div>
                </div>
                <div v-else class="response-placeholder">
                  <div class="typing-indicator">
                    <div class="typing-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span class="typing-text">{{ $t('chat.thinkingResponse') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="isLoading" class="message assistant">
        <div class="message-content">
          <div class="assistant-message">
            <!-- Thinking section in loading state -->
            <div class="thinking-section">
              <div class="thinking-header">
                <div class="thinking-avatar">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                </div>
                <div class="thinking-label">{{ $t('chat.thinkingLabel') }}</div>
              </div>
              <div class="thinking-content">
                <div class="default-processing">
                  <div class="processing-indicator">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span>{{ $t('chat.thinking') }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Response section in loading state -->
            <div class="response-section">
              <div class="response-header">
                <div class="response-avatar">
                  <Icon icon="carbon:bot" class="bot-icon" />
                </div>
                <div class="response-name">{{ $t('chat.botName') }}</div>
              </div>
              <div class="response-content">
                <div class="response-placeholder">
                  <div class="typing-indicator">
                    <div class="typing-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span class="typing-text">{{ $t('chat.thinkingResponse') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Scroll to bottom button -->
    <div
        v-if="showScrollToBottom"
        class="scroll-to-bottom-btn"
        @click="forceScrollToBottom"
        :title="$t('chat.scrollToBottom')"
    >
      <Icon icon="carbon:chevron-down" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import ExecutionDetails from './ExecutionDetails.vue'

import { CommonApiService } from '@/api/common-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { usePlanExecution } from '@/utils/use-plan-execution'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'
import type { InputMessage } from "@/stores/memory"
import {memoryStore} from "@/stores/memory";
import {MemoryApiService} from "@/api/memory-api-service";
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

/**
 * Chat message interface that includes PlanExecutionRecord for plan-based messages
 * Removes all duplicate fields that exist in PlanExecutionRecord
 */
interface Message {
  /** Unique message identifier for Vue rendering */
  id: string

  /** Message type: user input or assistant response */
  type: 'user' | 'assistant'

  /** Main content for display */
  content: string

  /** Message timestamp */
  timestamp: Date

  /** AI thinking process text (for loading states) */
  thinking?: string

  /** Generic user input field for simple interactions */
  genericInput?: string

  /** Plan execution data - contains all plan-related information */
  planExecution?: PlanExecutionRecord

  /** Legacy step actions for UI display (computed from planExecution) */
  stepActions?: Array<{
    actionDescription: string
    toolParameters: string
    thinkInput: string
    thinkOutput: string
    status: 'completed' | 'current' | 'pending'
  } | null>
}

interface Props {
  mode?: 'plan' | 'direct' // Plan mode or direct chat mode
  initialPrompt?: string // Initial prompt to process
}

interface Emits {
  (e: 'step-selected', planId: string, stepIndex: number): void
  (
      e: 'sub-plan-step-selected',
      parentPlanId: string,
      subPlanId: string,
      stepIndex: number,
      subStepIndex: number
  ): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'plan', // Use plan mode, handled by plan-execution-manager
  initialPrompt: '', // Default value for initialPrompt
})
const emit = defineEmits<Emits>()

// Initialize i18n
const { t } = useI18n()

// Use the plan execution manager
const planExecution = usePlanExecution()

// Configure marked once with GFM and line breaks
marked.setOptions({ gfm: true, breaks: true })

// Custom renderer: highlight code blocks, add copy button (markdown fenced code treated same as code)
const mdRenderer = new marked.Renderer()
mdRenderer.code = ({ text, lang }: { text: string; lang?: string; escaped?: boolean }): string => {
  const langRaw = (lang || '').trim()
  const langLower = langRaw.toLowerCase()

  let highlighted = ''
  try {
    if (langLower && hljs.getLanguage(langLower)) {
      highlighted = hljs.highlight(text, { language: langLower }).value
    } else {
      highlighted = hljs.highlightAuto(text).value
    }
  } catch (e) {
    highlighted = text
  }

  const rawEncoded = encodeURIComponent(text)
  const label = langLower || 'text'
  return `
<div class="md-code-block" data-lang="${label}">
  <div class="md-code-header">
    <span class="md-lang">${label}</span>
    <button class="md-copy-btn" data-raw="${rawEncoded}" title="copy">copy</button>
  </div>
  <pre><code class="hljs language-${label}">${highlighted}</code></pre>
</div>`
}

const messagesRef = ref<HTMLElement>()
const isLoading = ref(false)
const messages = ref<Message[]>([])
const pollingInterval = ref<number>()
const showScrollToBottom = ref(false)
const formInputsStore = reactive<Record<string, Record<number, string>>>({})
// Remove forceUpdateKey as it causes DOM to be recreated and scroll position to reset

const addMessage = (type: 'user' | 'assistant', content: string, options?: Partial<Message>) => {
  const message: Message = {
    id: Date.now().toString(),
    type,
    content,
    timestamp: new Date(),
    ...options,
  }

  // If it's an assistant message, ensure there's a basic thinking state even if there's no content
  if (type === 'assistant') {
    if (!message.thinking && !message.content) {
      message.thinking = t('chat.thinking')
    }
  }

  messages.value.push(message)
  // Remove forced scroll to bottom for new messages
  // Users can manually scroll if needed
  return message
}

const updateLastMessage = (updates: Partial<Message>) => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage.type === 'assistant') {
    Object.assign(lastMessage, updates)
    // Remove automatic scroll when content updates
    // Let users control their viewing position
  }
}

const handleDirectMode = async (query: InputMessage) => {
  try {
    isLoading.value = true

    // Add a thinking state message
    const assistantMessage = addMessage('assistant', '', {
      thinking: t('chat.thinkingProcessing'),
    })

    // Execute directly
    const response = await DirectApiService.sendMessage(query)

    if (response.planId) {
      console.log('[ChatComponent] Received planId from direct execution:', response.planId)

      if (response.memoryId) {
        memoryStore.setMemory(response.memoryId)
      }

      if (!assistantMessage.planExecution) {
        assistantMessage.planExecution = {} as any
      }
      assistantMessage.planExecution!.currentPlanId = response.planId

      planExecutionManager.handlePlanExecutionRequested(response.planId, query.input)

      delete assistantMessage.thinking

      console.log('[ChatComponent] Started polling for plan execution updates')
    } else {
      delete assistantMessage.thinking

      // Generate a natural and human-like response
      const finalResponse = generateDirectModeResponse(response, query.input)
      assistantMessage.content = finalResponse
    }
  } catch (error: any) {
    console.error('Direct mode error:', error)
    updateLastMessage({
      content: generateErrorResponse(error),
    })
  } finally {
    isLoading.value = false
  }
}

// Get the response content from the response
const generateDirectModeResponse = (response: any, _originalQuery: string): string => {
  return response.result ?? response.message ?? response.content ?? ''
}

// Generate an error response
const generateErrorResponse = (error: any): string => {
  const errorMsg = error?.message ?? error?.toString() ?? t('chat.unknownError')

  // Common error types with friendly prompts
  if (errorMsg.includes('network') || errorMsg.includes('timeout')) {
    return t('chat.networkError')
  }

  if (errorMsg.includes('auth') || errorMsg.includes('unauthorized')) {
    return t('chat.authError')
  }

  if (errorMsg.includes('invalid') || errorMsg.includes('format') || errorMsg.includes('parameter')) {
    return t('chat.formatError')
  }

  // Generic error response
  return `${t('chat.unknownError')} (${errorMsg})`
}

const scrollToBottom = (force = false) => {
  nextTick(() => {
    if (messagesRef.value) {
      const container = messagesRef.value

      // Check if scrolling to the bottom is required
      const isNearBottom =
          force || container.scrollHeight - container.scrollTop - container.clientHeight < 150

      if (isNearBottom) {
        // Use smooth scrolling unless forced scrolling is specified.
        container.scrollTo({
          top: container.scrollHeight,
          behavior: force ? 'auto' : 'smooth',
        })
      }
    }
  })
}

// Helper function to force scroll to the bottom
const forceScrollToBottom = () => {
  scrollToBottom(true)
  // Hide scroll button after scrolling
  showScrollToBottom.value = false
}

// Check if the scroll-to-bottom button needs to be displayed
const checkScrollPosition = () => {
  if (messagesRef.value) {
    const container = messagesRef.value
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 150
    showScrollToBottom.value = !isNearBottom && messages.value.length > 0
  }
}

// Add a scroll listener
const addScrollListener = () => {
  if (messagesRef.value) {
    messagesRef.value.addEventListener('scroll', checkScrollPosition)
  }
}

// Remove a scroll listener
const removeScrollListener = () => {
  if (messagesRef.value) {
    messagesRef.value.removeEventListener('scroll', checkScrollPosition)
  }
}

const handleSendMessage = (message: InputMessage) => {
  // First, add the user message to the UI.
  addMessage('user', message.input)

  // Handle messages according to the mode
  if (props.mode === 'plan') {
    // In plan mode, only add UI message, parent component handles the API call
    // This prevents double API calls
    console.log('[ChatComponent] Plan mode message sent, parent should handle:', message.input)
    // Don't call any API here, just add to UI
  } else {
    // Direct mode is still handled directly
    handleDirectMode(message)
  }
}


// Handle step click events - Only expose events without handling specific logic
const handleStepClick = (message: Message, stepIndex: number) => {
  if (!message.planExecution?.currentPlanId) {
    console.warn('[ChatComponent] Cannot handle step click: missing currentPlanId')
    return
  }

  console.log('[ChatComponent] Step clicked:', {
    planId: message.planExecution.currentPlanId,
    stepIndex: stepIndex,
    stepTitle: message.planExecution.steps?.[stepIndex],
  })

  // Emit a step selection event to the parent component
  emit('step-selected', message.planExecution.currentPlanId, stepIndex)
}


// Handle sub-plan step click - simplified to only emit events
const handleSubPlanStepClick = (message: Message, stepIndex: number, subStepIndex: number) => {
  try {
    const agentExecutionSequence = message.planExecution?.agentExecutionSequence
    if (!agentExecutionSequence?.length) {
      console.warn('[ChatComponent] No agentExecutionSequence data for sub-plan step click')
      return
    }

    const agentExecution = agentExecutionSequence[stepIndex] as AgentExecutionRecord | undefined
    if (!agentExecution) {
      console.warn('[ChatComponent] No agentExecution found for step', stepIndex)
      return
    }

    if (!agentExecution.thinkActSteps) {
      console.warn('[ChatComponent] No thinkActSteps found for step', stepIndex)
      return
    }

    // Find sub-plan in thinkActSteps
    let subPlan = null
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep.subPlanExecutionRecord) {
        subPlan = thinkActStep.subPlanExecutionRecord
        break
      }
    }

    if (!subPlan?.currentPlanId) {
      console.warn('[ChatComponent] No sub-plan data for step click')
      return
    }

    // Emit event with necessary identifiers for parent component to handle
    emit(
        'sub-plan-step-selected',
        message.planExecution?.currentPlanId ?? '',
        subPlan.currentPlanId,
        stepIndex,
        subStepIndex
    )
  } catch (error) {
    console.error('[ChatComponent] Error handling sub-plan step click:', error)
  }
}

// Update step execution actions (based on chat-handler.js logic)
const updateStepActions = (message: Message, planDetails: PlanExecutionRecord) => {
  if (!message.planExecution?.steps) return

  console.log(
      '[ChatComponent] Starting to update step actions, steps count:',
      message.planExecution.steps.length,
      'execution sequence:',
      planDetails.agentExecutionSequence?.length ?? 0
  )

  const lastStepActions = new Array(message.planExecution.steps.length).fill(null)

  if (planDetails.agentExecutionSequence?.length) {
    const sequenceLength = Math.min(
        planDetails.agentExecutionSequence.length,
        message.planExecution.steps.length
    )

    for (let index = 0; index < sequenceLength; index++) {
      const execution = planDetails.agentExecutionSequence[index]

      if (execution.thinkActSteps?.length) {
        const latestThinkAct = execution.thinkActSteps[execution.thinkActSteps.length - 1]

        if (latestThinkAct.actionDescription && latestThinkAct.toolParameters) {
          lastStepActions[index] = {
            actionDescription: latestThinkAct.actionDescription,
            toolParameters:
                typeof latestThinkAct.toolParameters === 'string'
                    ? latestThinkAct.toolParameters
                    : JSON.stringify(latestThinkAct.toolParameters, null, 2),
            thinkInput: latestThinkAct.thinkInput ?? '',
            thinkOutput: latestThinkAct.thinkOutput ?? '',
            status:
                planDetails.currentStepIndex !== undefined && index < planDetails.currentStepIndex
                    ? 'completed'
                    : planDetails.currentStepIndex !== undefined && index === planDetails.currentStepIndex
                        ? 'current'
                        : 'pending',
          }

          console.log(
              `[ChatComponent] Step ${index} action set: ${lastStepActions[index].actionDescription}`
          )
        } else {
          lastStepActions[index] = {
            actionDescription: t('chat.thinking'),
            toolParameters: t('chat.waitingDecision'),
            thinkInput: latestThinkAct.thinkInput ?? '',
            thinkOutput: latestThinkAct.thinkOutput ?? '',
            status: planDetails.currentStepIndex !== undefined && index === planDetails.currentStepIndex ? 'current' : 'pending',
          }

          console.log(`[ChatComponent] Step ${index} is thinking`)
        }
      } else {
        lastStepActions[index] = {
          actionDescription: planDetails.currentStepIndex !== undefined && index < planDetails.currentStepIndex ? t('chat.status.completed') : t('chat.status.pending'),
          toolParameters: t('chat.noToolParameters'),
          thinkInput: '',
          thinkOutput: '',
          status: planDetails.currentStepIndex !== undefined && index < planDetails.currentStepIndex ? 'completed' : 'pending',
        }

        console.log(
            `[ChatComponent] Step ${index} has no execution details, status set to: ${lastStepActions[index].status}`
        )
      }
    }
  } else {
    console.log('[ChatComponent] No execution sequence data')
  }

  message.stepActions = [...lastStepActions]

  console.log(
      '[ChatComponent] Step actions update completed:',
      JSON.stringify(lastStepActions.map(a => a?.actionDescription))
  )

  nextTick(() => {
    console.log('[ChatComponent] UI update completed via reactivity')
  })
}

// Handle the start of a dialog round
const handleDialogRoundStart = (planId: string) => {
  console.log('[ChatComponent] Starting dialog round with planId:', planId)

  if (planId) {
    // Check if there is already an assistant message for this plan
    const existingAssistantMsg = messages.value.findIndex(
        m => m.planExecution?.currentPlanId === planId && m.type === 'assistant'
    )

    // If there is no existing message, add an assistant message to prepare to display steps
    if (existingAssistantMsg === -1) {
      addMessage('assistant', '', {
        planExecution: { currentPlanId: planId } as PlanExecutionRecord,
        thinking: t('chat.preparingExecution'),
      })
      console.log('[ChatComponent] Created new assistant message for planId:', planId)
    } else {
      console.log('[ChatComponent] Found existing assistant message for planId:', planId)
    }

    // Remove automatic scroll for dialog round start
    // Keep user at their current viewing position
  }
}

// Handle plan updates - Use a new scheme based on rootPlanId
const handlePlanUpdate = (rootPlanId: string) => {
  console.log('[ChatComponent] Processing plan update with rootPlanId:', rootPlanId)

  // Get the PlanExecutionRecord from the cache
  const planDetails = planExecutionManager.getCachedPlanRecord(rootPlanId)

  if (!planDetails) {
    console.warn('[ChatComponent] No cached plan data found for rootPlanId:', rootPlanId)
    return
  }

  console.log('[ChatComponent] Retrieved plan details from cache:', planDetails)
  console.log('[ChatComponent] Plan steps:', planDetails.steps)
  console.log('[ChatComponent] Plan completed:', planDetails.completed)

  if (!planDetails.currentPlanId) {
    console.warn('[ChatComponent] Plan update missing currentPlanId')
    return
  }

  // Find the corresponding message - Use the currentPlanId field
  const messageIndex = messages.value.findIndex(
      m => m.planExecution?.currentPlanId === planDetails.currentPlanId && m.type === 'assistant'
  )
  let message

  if (messageIndex !== -1) {
    message = messages.value[messageIndex]
    console.log(
        '[ChatComponent] Found existing assistant message for currentPlanId:',
        planDetails.currentPlanId
    )
  } else {
    console.warn(
        '[ChatComponent] No existing assistant message found for currentPlanId:',
        planDetails.currentPlanId
    )
    console.log(
        '[ChatComponent] Current messages:',
        messages.value.map(m => ({
          type: m.type,
          planId: m.planExecution?.currentPlanId,
          content: m.content.substring(0, 50),
        }))
    )

    // If no corresponding message is found, it should have been created by handleDialogRoundStart. Do not create a new message here
    // Instead, try to find the latest assistant message to update
    let lastAssistantIndex = -1
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].type === 'assistant') {
        lastAssistantIndex = i
        break
      }
    }

    if (lastAssistantIndex !== -1) {
      message = messages.value[lastAssistantIndex]
      // Update planExecution to ensure subsequent updates can find it
      if (!message.planExecution) {
        message.planExecution = {} as PlanExecutionRecord
      }
      message.planExecution.currentPlanId = planDetails.currentPlanId
      console.log(
          '[ChatComponent] Using last assistant message and updating planExecution.currentPlanId to:',
          planDetails.currentPlanId
      )
    } else {
      console.error('[ChatComponent] No assistant message found at all, this should not happen')
      return
    }
  }

  // Ensure planExecution exists
  if (!message.planExecution) {
    message.planExecution = {} as PlanExecutionRecord
  }

  // Update planExecution data - Use deep copy to ensure reactivity
  message.planExecution = JSON.parse(JSON.stringify(planDetails))

  // Handle simple responses (cases without steps)
  if (!planDetails.steps || planDetails.steps.length === 0) {
    console.log('[ChatComponent] Handling simple response without steps')

    if (planDetails.completed) {
      // Directly set the final response, clear all processing states
      delete message.thinking

      const finalResponse =
          planDetails.summary ?? planDetails.result ?? planDetails.message ?? t('chat.executionCompleted')
      // Ensure the response is natural
      message.content = generateNaturalResponse(finalResponse)

      console.log('[ChatComponent] Set simple response content:', message.content)
    } else {
      // If there is a title or status information, update the thinking state
      if (planDetails.title) {
        message.thinking = `${t('chat.thinkingExecuting', { title: planDetails.title })}`
      }
    }

    // Remove automatic scroll in simple response
    // Users can manually scroll if they want to see the latest content
    return
  }

  // Handle plans with steps...

  // Clear the initial thinking state to display the plan execution information
  delete message.thinking

  // Process step information - Ensure consistent format and maintain a user-friendly display
  const formattedSteps = planDetails.steps.map((step: any) => {
    // If the step is a string, return it directly
    if (typeof step === 'string') {
      return step
    }
    // If it's an object, extract the title for display
    else if (typeof step === 'object' && step !== null) {
      return step.title || step.description || t('chat.step')
    }
    return t('chat.step')
  })

  // Update the step information in planExecution
  if (message.planExecution) {
    message.planExecution.steps = formattedSteps
  }

  // Process the execution sequence and step actions - Refer to the logic in chat-handler.js
  if (planDetails.agentExecutionSequence && planDetails.agentExecutionSequence.length > 0) {
    console.log(
        '[ChatComponent] Found execution sequence data, count:',
        planDetails.agentExecutionSequence.length
    )

    // Call updateStepActions to update the step action information
    updateStepActions(message, planDetails)

    // Update the thinking state from the currently executing step
    const currentStepIndex = planDetails.currentStepIndex ?? 0
    if (currentStepIndex >= 0 && currentStepIndex < planDetails.agentExecutionSequence.length) {
      const currentExecution = planDetails.agentExecutionSequence[currentStepIndex]
      const thinkActSteps = currentExecution.thinkActSteps
      if (thinkActSteps && thinkActSteps.length > 0) {
        const latestThinkAct = thinkActSteps[thinkActSteps.length - 1]
        if (latestThinkAct.thinkOutput) {
          // If the think output is too long, truncate it
          const maxLength = 150
          const displayOutput =
              latestThinkAct.thinkOutput.length > maxLength
                  ? latestThinkAct.thinkOutput.substring(0, maxLength) + '...'
                  : latestThinkAct.thinkOutput

          message.thinking = `${t('chat.thinking')}: ${displayOutput}`
        }
      }
    }
  } else {
    // If there is no execution sequence, use the basic thinking state
    if (message.planExecution) {
      const currentStepIndex = message.planExecution.currentStepIndex ?? 0
      const currentStep = message.planExecution.steps?.[currentStepIndex]
      const stepTitle = typeof currentStep === 'string' ? currentStep : ''
      message.thinking = `${t('chat.thinkingExecuting', { title: stepTitle })}`
    }
  }

  // Handle the user input waiting state
  if (planDetails.userInputWaitState && message.planExecution) {
    console.log('[ChatComponent] User input required:', planDetails.userInputWaitState)

    // Attach the user input waiting state to planExecution
    if (!message.planExecution.userInputWaitState) {
      message.planExecution.userInputWaitState = {}
    }
    message.planExecution.userInputWaitState = {
      message: planDetails.userInputWaitState.message ?? '',
      formDescription: planDetails.userInputWaitState.formDescription ?? '',
      formInputs:
          planDetails.userInputWaitState.formInputs?.map((input: any) => ({
            label: input.label,
            value: input.value || '',
            type: input.type || 'text',
            required: input.required === 'true' || input.required === true,
            placeholder: input.placeholder || '',
            name: input.name || input.label,
            options: input.options || undefined,
          })) ?? [],
    }

    formInputsStore[message.id] ??= {}
    // Clear the thinking state and display the message waiting for user input
    message.thinking = t('input.waiting')
  } else {
    // If there is no user input waiting state, clear the previous state
    if (message.planExecution?.userInputWaitState) {
      delete message.planExecution.userInputWaitState
    }
  }

  // Check if the plan is completed
  if (planDetails.completed ?? planDetails.status === 'completed') {
    console.log('[ChatComponent] Plan is completed, updating final response')
    // Clear all processing states
    delete message.thinking

    // Set the final response content - Simulate a human conversation response
    let finalResponse = ''
    if (planDetails.summary) {
      finalResponse = planDetails.summary
    } else if (planDetails.result) {
      finalResponse = planDetails.result
    } else {
      finalResponse = t('chat.executionCompleted')
    }

    // Generate natural, human-like responses
    message.content = generateCompletedPlanResponse(finalResponse)

    console.log('[ChatComponent] Updated completed message:', message.content)
  }

  // Remove automatic scroll to bottom for plan updates
  // Let users stay at their current viewing position

  // Use Vue's reactivity system instead of force update
  // The UI will automatically update when planExecution data changes
  nextTick(() => {
    console.log('[ChatComponent] Plan update UI refresh completed via reactivity')
  })
}

// Helper function to generate natural responses
const generateNaturalResponse = (text: string): string => {
  if (!text) return t('chat.defaultResponse')

  // If it's already in a natural conversation format, return it directly
  if (
      text.includes('I ') ||
      text.includes('you') ||
      text.includes('hello') ||
      text.includes('can') ||
      text.includes('I') ||
      text.includes('you') ||
      text.includes('can')
  ) {
    return text
  }

  // Generate a more natural response based on the text content
  if (text.length < 10) {
    return `${text}! ${t('chat.anythingElse')}`
  } else if (text.length < 50) {
    return `${t('chat.okayDone', { text })}. ${t('chat.ifOtherQuestions')}`
  } else {
    return `${text}\n\n${t('chat.hopeHelpful')} ${t('chat.anythingElse')}`
  }
}

// Generate a natural response for a completed plan
const generateCompletedPlanResponse = (text: string): string => {
  if (!text) return `${t('chat.executionCompleted')}! ${t('chat.anythingElse')}`
  else{
    return `${text}`;
  }
}

// Handle the plan completion event
const handlePlanCompleted = (rootPlanId: string) => {
  console.log('[ChatComponent] Plan completed with rootPlanId:', rootPlanId);

  const details = planExecutionManager.getCachedPlanRecord(rootPlanId);
  if (!details) {
    console.warn('[ChatComponent] No cached plan data found for rootPlanId:', rootPlanId);
    return;
  }

  console.log('[ChatComponent] Plan details:', details);

  if (details.rootPlanId) {
    const messageIndex = messages.value.findIndex(
        m => m.planExecution?.currentPlanId === details.rootPlanId
    );
    if (messageIndex !== -1) {
      const message = messages.value[messageIndex];
      delete message.thinking;

      const summary = details.summary ?? details.result ?? t('chat.executionCompleted');
      let finalResponse = summary;
      if (!finalResponse.includes('I') && !finalResponse.includes('you')) {
        if (finalResponse.includes('success') || finalResponse.includes('complete') || finalResponse.includes('finished')) {
          finalResponse = `${t('chat.great')}${finalResponse}. ${t('chat.ifOtherHelp')}`;
        } else {
          finalResponse = `${t('chat.completedRequest', { result: finalResponse })}`;
        }
      }

      message.content = finalResponse;
      console.log('[ChatComponent] Updated completed message:', message.content);
    } else {
      console.warn('[ChatComponent] No message found for completed rootPlanId:', details.rootPlanId);
    }
  }
};

// Handle the plan error event
const handlePlanError = (message: string) => {
  isLoading.value = false
  messages.value[messages.value.length -1] = {
    id: Date.now().toString(),
    type: 'assistant',
    content: message,
    timestamp: new Date(),
  }
}

// Format the response text to make it more like a natural conversation
const formatResponseText = (text: string): string => {
  if (!text) return ''

  try {
    const rawHtml = marked.parse(text, { renderer: mdRenderer })
    // Sanitize to avoid XSS
    return DOMPurify.sanitize(rawHtml as string)
  } catch (e) {
    console.error('Markdown render error:', e)
    // Fallback: preserve original simple formatting
    let fallback = text.replace(/\n\n/g, '<br><br>').replace(/\n/g, '<br>')
    fallback = fallback.replace(/(<br><br>)/g, '</p><p>')
    if (fallback.includes('</p><p>')) fallback = `<p>${fallback}</p>`
    return fallback
  }
}

// Copy button handler (event delegation)
const handleMessageContainerClick = (event: Event) => {
  const target = event.target as HTMLElement
  if (!target) return
  const btn = target.closest('.md-copy-btn') as HTMLElement | null
  if (!btn) return

  const raw = btn.getAttribute('data-raw') || ''
  let textToCopy = ''
  try {
    textToCopy = decodeURIComponent(raw)
  } catch {
    textToCopy = raw
  }

  const doCopy = async () => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(textToCopy)
      } else {
        const ta = document.createElement('textarea')
        ta.value = textToCopy
        ta.style.position = 'fixed'
        ta.style.left = '-9999px'
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
      }
      btn.textContent = 'copy'
      setTimeout(() => (btn.textContent = 'copy'), 1500)
    } catch (err) {
      console.error('Copy failed:', err)
      btn.textContent = 'copy failed'
      setTimeout(() => (btn.textContent = 'copy'), 1500)
    }
  }

  doCopy()
}

// Handle user input form submission
const handleUserInputSubmit = async (message: Message, inputData?: any) => {
  if (!message.planExecution?.currentPlanId || !message.planExecution.userInputWaitState) {
    console.error('[ChatComponent] Missing planExecution.currentPlanId or userInputWaitState')
    return
  }

  try {
    // Use provided inputData or collect from message
    let formData: any = inputData

    if (!formData) {
      // Fallback to collecting form data from message (legacy support)
      formData = {}

    const formInputs = message.planExecution.userInputWaitState.formInputs
    if (formInputs && formInputs.length > 0) {
      // Multiple fields case
      Object.entries(formInputsStore[message.id]).forEach(([index, value]) => {
        const numIndex = parseInt(index, 10)
        const label = formInputs[numIndex]?.label || `input_${index}`
          formData[label] = value
      })
    } else {
      // Single generic input case
        formData.genericInput = message.genericInput ?? ''
      }
    }

    console.log('[ChatComponent] Submitting user input:', formData)

    // Submit user input via API
    const response = await CommonApiService.submitFormInput(
        message.planExecution.currentPlanId,
        formData
    )

    // Clear the user input waiting state
    delete message.planExecution.userInputWaitState
    delete message.genericInput
    delete formInputsStore[message.id]

    // Continue polling for plan updates (should resume automatically after submission)
    planExecution.startPolling()

    console.log('[ChatComponent] User input submitted successfully:', response)
  } catch (error: any) {
    console.error('[ChatComponent] User input submission failed:', error)
    // Can display error message in UI
    alert(`${t('common.submitFailed')}: ${error?.message || t('common.unknownError')}`)
  }
}

watch(
  () => props.initialPrompt,
  (newPrompt, oldPrompt) => {
    console.log('[ChatComponent] initialPrompt changed from:', oldPrompt, 'to:', newPrompt)
    if (newPrompt && typeof newPrompt === 'string' && newPrompt.trim() && newPrompt !== oldPrompt) {
      console.log('[ChatComponent] Processing changed initial prompt:', newPrompt)
      nextTick(() => {
        handleSendMessage({
          input: newPrompt
        })
      })
    }
  },
  { immediate: false }
)

onMounted(() => {
  console.log('[ChatComponent] Mounted, setting up event listeners')

  planExecutionManager.setEventCallbacks({
    onPlanUpdate: handlePlanUpdate,
    onPlanCompleted: handlePlanCompleted,
    onDialogRoundStart: handleDialogRoundStart,
    onChatInputUpdateState: (rootPlanId: string) => {
      console.log('[ChatComponent] Chat input state update for rootPlanId:', rootPlanId)
    },
    onChatInputClear: () => {
      console.log('[ChatComponent] Chat input clear requested')
    },
    onPlanError: handlePlanError
  })

  nextTick(() => {
    addScrollListener()
  })

  if (props.initialPrompt && typeof props.initialPrompt === 'string' && props.initialPrompt.trim()) {
    console.log('[ChatComponent] Processing initial prompt:', props.initialPrompt)
    nextTick(() => {
      handleSendMessage({
        input: props.initialPrompt!
      })
    })
  }
})

onUnmounted(() => {
  console.log('[ChatComponent] Unmounting, cleaning up resources')

  // Remove the scroll listener
  removeScrollListener()

  // Clean up polling
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  // Clean up plan execution manager resources
  planExecution.cleanup()

  // Clear form inputs
  Object.keys(formInputsStore).forEach(key => delete formInputsStore[key])
})


const showMemory = async () => {
  if(memoryStore.selectMemoryId) {
    const memory = await MemoryApiService.getMemory(memoryStore.selectMemoryId);
    messages.value = []
    memory.messages.map(message => {
      if(message.messageType.toLowerCase() === 'user') {
        addMessage('user',message.text)
      }
      if(message.messageType.toLowerCase() === 'assistant') {
        addMessage('assistant',message.text)
      }
    });
    forceScrollToBottom()
  }
}

const newChat = () => {
  messages.value = []
}

// Expose methods to parent components for usage
defineExpose({
  handleSendMessage,
  handlePlanUpdate,
  handlePlanCompleted,
  handleDialogRoundStart,
  addMessage,
  handlePlanError,
  showMemory,
  newChat
})
</script>

<style lang="less" scoped>
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.messages {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  min-height: 0;

  scroll-behavior: smooth;

  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;


  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.5);
  }
}

.message {
  display: flex;

  &.user {
    justify-content: flex-end;

    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #ffffff;
      max-width: 80%;
    }
  }

  &.assistant {
    justify-content: flex-start;

    .message-content {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #ffffff;
      max-width: 85%;
    }
  }
}

.message-content {
  padding: 16px 20px;
  border-radius: 16px;
  backdrop-filter: blur(20px);
}

.user-message {
  line-height: 1.5;
}

.assistant-message {

  .thinking-section {
    margin-bottom: 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.02);
    overflow: hidden;

    .thinking-header {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      background: rgba(102, 126, 234, 0.1);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .thinking-avatar {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        border-radius: 50%;

        .thinking-icon {
          font-size: 16px;
          color: #ffffff;
          animation: pulse 2s infinite;
        }
      }

      .thinking-label {
        font-weight: 600;
        font-size: 14px;
        color: #f59e0b;
        letter-spacing: 0.5px;
      }
    }

    .thinking-content {
      padding: 16px;
    }

    .thinking {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #cccccc;
      font-size: 14px;
      margin-bottom: 12px;
      padding: 12px;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 8px;
      border-left: 3px solid #f59e0b;

      .thinking-icon {
        animation: pulse 2s infinite;
      }
    }

    .default-processing {
      padding: 16px;
      text-align: center;

      .processing-indicator {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        color: #cccccc;
        font-size: 14px;
      }
    }
  }


  .response-section {
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 18px;
    background: linear-gradient(
        135deg,
        rgba(255, 255, 255, 0.12) 0%,
        rgba(255, 255, 255, 0.06) 100%
    );
    overflow: hidden;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
    backdrop-filter: blur(12px);
    margin-top: 16px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
    }

    .response-header {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 18px 24px 14px 24px;
      background: linear-gradient(
          135deg,
          rgba(102, 126, 234, 0.18) 0%,
          rgba(118, 75, 162, 0.12) 100%
      );
      border-bottom: 1px solid rgba(255, 255, 255, 0.15);

      .response-avatar {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 50%;
        box-shadow: 0 3px 12px rgba(102, 126, 234, 0.4);
        transition: transform 0.2s ease;

        &:hover {
          transform: scale(1.05);
        }

        .bot-icon {
          font-size: 20px;
          color: #ffffff;
        }
      }

      .response-name {
        font-weight: 700;
        font-size: 17px;
        color: #667eea;
        letter-spacing: 0.8px;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        font-family:
            -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei',
            sans-serif;
      }
    }

    .response-content {
      padding: 24px;

      .final-response {
        .response-text {
          word-break: break-all;
          line-height: 1.8;
          color: #ffffff;
          font-size: 15px;
          font-weight: 400;
          text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          letter-spacing: 0.4px;
          word-spacing: 1.2px;
          text-align: left;
          font-family:
              -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei',
              sans-serif;


          p {
            margin: 0 0 12px 0;

            &:last-child {
              margin-bottom: 0;
            }
          }


          strong {
            color: #f8fafc;
            font-weight: 600;
          }

          em {
            color: #e2e8f0;
            font-style: italic;
          }

            /* Headings */
            h1, h2, h3, h4, h5, h6 {
              margin: 12px 0 8px;
              font-weight: 700;
              line-height: 1.4;
            }
            h1 { font-size: 22px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 6px; }
            h2 { font-size: 20px; margin-top: 16px; }
            h3 { font-size: 18px; }

            /* Lists */
            ul, ol {
              margin: 6px 0 12px 22px;
              padding-left: 18px;
            }
            li { margin: 4px 0; }

            /* Blockquote */
            blockquote {
              margin: 10px 0;
              padding: 8px 12px;
              border-left: 3px solid #667eea;
              background: rgba(102, 126, 234, 0.08);
              color: #e5e7eb;
            }

            /* Inline code */
            code {
              background: rgba(0,0,0,0.35);
              padding: 2px 6px;
              border-radius: 4px;
              font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
                'Courier New', monospace;
              font-size: 13px;
            }

            /* Code blocks */
            pre {
              background: rgba(0,0,0,0.5);
              border: 1px solid rgba(255, 255, 255, 0.08);
              border-radius: 8px;
              padding: 12px 14px;
              overflow: auto;
              margin: 10px 0 14px;
            }
            pre code {
              background: transparent;
              padding: 0;
              font-size: 13px;
              line-height: 1.6;
              color: #e5e7eb;
              white-space: pre;
            }

            /* Enhanced code block container with toolbar */
            :deep(.md-code-block) {
              position: relative;
              margin: 12px 0 16px;
              border: 1px solid #30363d; /* GitHub dark border */
              border-radius: 8px;
              background: #0d1117; /* GitHub dark bg */
              box-shadow: inset 0 1px 0 rgba(255,255,255,0.03);
            }
            :deep(.md-code-block .md-code-header) {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 8px 10px;
              border-bottom: 1px solid #30363d;
              background: #161b22; /* GitHub dark header */
              border-top-left-radius: 8px;
              border-top-right-radius: 8px;
            }
            :deep(.md-code-block .md-code-header .md-lang) {
              margin-right: auto;
            }
            :deep(.md-code-block .md-code-header .md-copy-btn) {
              margin-left: auto; /* ensure right aligned */
            }
            :deep(.md-code-block .md-lang) {
              font-size: 12px;
              color: #8b949e;
              text-transform: lowercase;
            }
            :deep(.md-code-block .md-copy-btn) {
              height: 22px;
              padding: 0 8px;
              background: #21262d; /* GitHub dark button bg */
              color: #c9d1d9; /* GitHub dark text */
              border: 1px solid #30363d;
              border-radius: 6px;
              font-size: 12px;
              cursor: pointer;
              transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, transform 0.1s ease;
            }
            :deep(.md-code-block .md-copy-btn:hover) {
              background: #30363d; /* GitHub dark hover bg */
              color: #f0f6fc;
              border-color: #8b949e;
              transform: translateY(-1px);
            }
            :deep(.md-code-block pre) {
              margin: 0;
              border: none;
              border-bottom-left-radius: 8px;
              border-bottom-right-radius: 8px;
              background: #0d1117; /* match container */
            }
        }
      }

      .response-placeholder {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 90px;

        .typing-indicator {
          display: flex;
          align-items: center;
          gap: 14px;

          .typing-text {
            color: #cbd5e0;
            font-style: italic;
            font-size: 14px;
            opacity: 0.9;
            letter-spacing: 0.3px;
          }
        }
      }
    }
  }

  .assistant-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .assistant-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;

      .bot-icon {
        font-size: 18px;
        color: #ffffff;
      }
    }

    .assistant-name {
      font-weight: 600;
      font-size: 14px;
      color: #667eea;
      letter-spacing: 0.5px;
    }
  }

  .response {
    line-height: 1.5;
    white-space: pre-line;
  }



  .steps-container {
    margin-top: 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    overflow: hidden;

    .steps-title {
      margin: 0;
      padding: 10px 16px;
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
      background: rgba(102, 126, 234, 0.15);
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    }

    .ai-section {
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      cursor: pointer;
      transition: all 0.2s ease;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }

      &.running {
        background: rgba(102, 126, 234, 0.1);
        border-left: 3px solid #667eea;
      }

      &.completed {
        border-left: 3px solid rgba(34, 197, 94, 0.6);
      }

      &.pending {
        opacity: 0.7;
      }

      .section-header {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        background: rgba(255, 255, 255, 0.02);

        .step-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 24px;
          height: 24px;
          background: rgba(102, 126, 234, 0.2);
          border-radius: 50%;
          font-size: 12px;
          font-weight: bold;
          color: #667eea;
        }

        .step-title {
          flex: 1;
          font-weight: 500;
          color: #ffffff;
        }

        .step-status {
          font-size: 12px;
          padding: 4px 8px;
          border-radius: 12px;

          &.completed {
            background: rgba(34, 197, 94, 0.2);
            color: #22c55e;
          }

          &.running {
            background: rgba(102, 126, 234, 0.2);
            color: #667eea;
          }

          &.pending {
            background: rgba(156, 163, 175, 0.2);
            color: #9ca3af;
          }
        }
      }

      .action-info {
        padding: 12px 16px;
        background: rgba(0, 0, 0, 0.2);
        border-top: 1px dashed rgba(255, 255, 255, 0.1);

        .action-description {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;

          .action-icon {
            font-size: 16px;
          }
        }

        .tool-params {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          margin-bottom: 8px;
          font-size: 13px;

          .tool-icon {
            margin-top: 2px;
          }

          .param-label {
            color: #aaaaaa;
            margin-right: 4px;
          }

          .param-content {
            margin: 0;
            padding: 6px;
            background: rgba(0, 0, 0, 0.2);
            border-radius: 4px;
            font-family: monospace;
            font-size: 12px;
            white-space: pre-wrap;
            max-height: 100px;
            overflow-y: auto;
          }
        }

        .think-details {
          margin-top: 10px;
          padding-top: 8px;
          border-top: 1px dashed rgba(255, 255, 255, 0.1);

          .think-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            .think-icon {
              font-size: 14px;
            }

            .think-label {
              color: #aaaaaa;
              font-size: 13px;
            }
          }

          .think-output {
            .think-content {
              margin: 0;
              padding: 8px;
              background: rgba(0, 0, 0, 0.15);
              border-radius: 4px;
              font-family: monospace;
              font-size: 12px;
              white-space: pre-wrap;
              max-height: 120px;
              overflow-y: auto;
              color: #bbbbbb;
            }
          }
        }
      }


      .sub-plan-steps {
        margin-top: 8px;
        padding: 8px 16px;
        background: rgba(102, 126, 234, 0.05);
        border-top: 1px solid rgba(102, 126, 234, 0.2);

        .sub-plan-header {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 8px;

          .sub-plan-icon {
            font-size: 14px;
            color: #667eea;
          }

          .sub-plan-title {
            font-size: 13px;
            font-weight: 600;
            color: #667eea;
          }
        }

        .sub-plan-step-list {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .sub-plan-step-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 8px;
          background: rgba(255, 255, 255, 0.02);
          border: 1px solid rgba(255, 255, 255, 0.05);
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s ease;
          margin-left: 20px;

          &:hover {
            background: rgba(255, 255, 255, 0.05);
            border-color: rgba(102, 126, 234, 0.3);
          }

          &.completed {
            background: rgba(34, 197, 94, 0.05);
            border-color: rgba(34, 197, 94, 0.2);
          }

          &.running {
            background: rgba(102, 126, 234, 0.05);
            border-color: rgba(102, 126, 234, 0.3);
            box-shadow: 0 0 4px rgba(102, 126, 234, 0.2);
          }

          &.pending {
            opacity: 0.6;
          }

          .sub-step-indicator {
            display: flex;
            align-items: center;
            gap: 4px;
            flex-shrink: 0;

            .sub-step-icon {
              display: flex;
              align-items: center;
              justify-content: center;
              width: 16px;
              height: 16px;
              background: rgba(102, 126, 234, 0.1);
              border-radius: 50%;
              font-size: 10px;
              font-weight: bold;
              color: #667eea;
            }

            .sub-step-number {
              font-size: 10px;
              color: #888888;
              font-weight: 500;
              min-width: 12px;
              text-align: center;
            }
          }

          .sub-step-content {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: space-between;
            min-width: 0;

            .sub-step-title {
              color: #cccccc;
              font-size: 12px;
              line-height: 1.3;
              word-break: break-word;
              flex: 1;
            }

            .sub-step-badge {
              background: rgba(102, 126, 234, 0.15);
              color: #667eea;
              font-size: 9px;
              padding: 1px 4px;
              border-radius: 8px;
              font-weight: 500;
              flex-shrink: 0;
              margin-left: 6px;
            }
          }
        }
      }
    }
  }
}

.thinking-dots {
  display: flex;
  gap: 4px;

  span {
    width: 4px;
    height: 4px;
    background: #667eea;
    border-radius: 50%;
    animation: thinking 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }
    &:nth-child(2) {
      animation-delay: -0.16s;
    }
    &:nth-child(3) {
      animation-delay: 0s;
    }
  }
}

.typing-dots {
  display: flex;
  gap: 3px;

  span {
    width: 6px;
    height: 6px;
    background: #667eea;
    border-radius: 50%;
    animation: typing 1.2s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: 0s;
    }
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes thinking {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.user-input-form-container {
  margin-top: 12px;
  padding: 16px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 8px;

  .user-input-message {
    margin-bottom: 12px;
    font-weight: 500;
    color: #ffffff;
    font-size: 14px;
  }

  .form-description {
    margin-bottom: 16px;
    color: #aaaaaa;
    font-size: 13px;
    line-height: 1.4;
  }

  .user-input-form {
    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
      margin-bottom: 16px;
      align-items: end;

      @media (max-width: 768px) {
        grid-template-columns: 1fr;
        gap: 12px;
        align-items: start;
      }

      @media (max-width: 480px) {
        gap: 8px;
      }
    }

    .form-group {
      margin-bottom: 0;
      display: grid;
      grid-template-rows: 1fr 40px;
      height: 68px;
      align-content: stretch;
      gap: 5px;
      
      @media (max-width: 768px) {
        grid-template-rows: 1fr 42px;
        height: auto;
        min-height: 70px;
        align-content: stretch;
        gap: 4px;
      }

      label {
        display: block;
        margin-bottom: 0;
        font-size: 13px;
        font-weight: 500;
        color: #ffffff;
        line-height: 1.3;
        word-wrap: break-word;
        overflow-wrap: break-word;
        hyphens: auto;
        grid-row: 1;
        align-self: end;
        justify-self: start;
        
        @media (max-width: 768px) {
          font-size: 12px;
          align-self: start;
        }
      }

      .form-input {
        width: 100%;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 6px;
        color: #ffffff;
        font-size: 14px;
        line-height: 1.4;
        height: 40px;
        box-sizing: border-box;
        transition: border-color 0.2s ease;
        grid-row: 2;
        align-self: stretch;

        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
        }

        &::placeholder {
          color: #888888;
        }
        
        @media (max-width: 768px) {
          font-size: 14px;
          height: 42px;
        }
      }

      .form-textarea {
        resize: vertical;
        min-height: 60px;
        height: 60px;
        font-family: inherit;
        line-height: 1.4;
        box-sizing: border-box;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 6px;
        color: #ffffff;
        font-size: 14px;
        transition: border-color 0.2s ease;
        grid-row: 2;
        align-self: stretch;
        
        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
        }

        &::placeholder {
          color: #888888;
        }
        
        @media (max-width: 768px) {
          height: 65px;
        }
      }


      &.form-group-wide {
        grid-column: span 2;

        @media (max-width: 768px) {
          grid-column: span 1;
        }
      }

      &.form-group-full {
        grid-column: span 2;

        @media (max-width: 768px) {
          grid-column: span 1;
        }
      }

      &:has(.form-textarea) {
        grid-template-rows: 1fr 60px;
        height: 88px;
        
        @media (max-width: 768px) {
          grid-template-rows: 1fr 65px;
          height: auto;
          min-height: 93px;
        }
      }

      &.form-group-textarea {
        grid-template-rows: 1fr 60px;
        height: 88px;
        
        @media (max-width: 768px) {
          grid-template-rows: 1fr 65px;
          height: auto;
          min-height: 93px;
        }
      }

      .form-select {
        cursor: pointer;
        height: 40px;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 6px;
        color: #ffffff;
        font-size: 14px;
        line-height: 1.4;
        box-sizing: border-box;
        transition: border-color 0.2s ease;
        grid-row: 2;
        align-self: stretch;

        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
        }

        option {
          background: #2d3748;
          color: #ffffff;
        }
        
        @media (max-width: 768px) {
          height: 42px;
        }
      }
    }

    .submit-user-input-btn {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #ffffff;
      border: none;
      padding: 10px 20px;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }
}


.scroll-to-bottom-btn {
  position: absolute;
  bottom: 120px;
  right: 24px;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 15;
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.5);
  }

  &:active {
    transform: translateY(0);
  }

  svg {
    font-size: 20px;
    color: #ffffff;
  }


  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0%,
  100% {
    box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  }
  50% {
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
  }
}
</style>
