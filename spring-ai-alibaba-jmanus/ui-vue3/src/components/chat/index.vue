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
    <div class="messages" ref="messagesRef">
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
            <!-- 1. TaskPilot Thinking/Processing Section - Only displayed when there is processing content -->
            <div
              class="thinking-section"
              v-if="
                message.thinking ||
                message.planExecution?.progress !== undefined ||
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

                <!-- Progress bar -->
                <div class="progress" v-if="message.planExecution?.progress !== undefined">
                  <div class="progress-bar">
                    <div
                      class="progress-fill"
                      :style="{ width: message.planExecution.progress + '%' }"
                    ></div>
                  </div>
                  <span class="progress-text">{{
                    message.planExecution.progressText ?? $t('chat.processing') + '...'
                  }}</span>
                </div>

                <!-- Step execution details -->
                <div class="steps-container" v-if="(message.planExecution?.steps?.length ?? 0) > 0">
                  <h4 class="steps-title">{{ $t('chat.stepExecutionDetails') }}</h4>

                  <!-- 遍历所有步骤 -->
                  <div
                    v-for="(step, index) in message.planExecution?.steps"
                    :key="index"
                    class="ai-section"
                    :class="{
                      running: getAgentExecutionStatus(message, index) === 'RUNNING',
                      completed: getAgentExecutionStatus(message, index) === 'FINISHED',
                      pending: getAgentExecutionStatus(message, index) === 'IDLE',
                    }"
                    @click.stop="handleStepClick(message, index)"
                  >
                    <div class="section-header">
                      <span class="step-icon">
                        {{
                          getAgentExecutionStatus(message, index) === 'FINISHED'
                            ? '✓'
                            : getAgentExecutionStatus(message, index) === 'RUNNING'
                              ? '▶'
                              : '○'
                        }}
                      </span>
                      <span class="step-title">
                        {{ step || `${$t('chat.step')} ${index + 1}` }}
                      </span>
                      <span
                        v-if="getAgentExecutionStatus(message, index) === 'RUNNING'"
                        class="step-status current"
                      >
                        {{ $t('chat.status.executing') }}
                      </span>
                      <span
                        v-else-if="getAgentExecutionStatus(message, index) === 'FINISHED'"
                        class="step-status completed"
                      >
                        {{ $t('chat.status.completed') }}
                      </span>
                      <span v-else class="step-status pending">
                        {{ $t('chat.status.pending') }}
                      </span>
                    </div>

                    <!-- Display step execution action information -->
                    <div
                      v-if="message.stepActions && message.stepActions[index]"
                      class="action-info"
                    >
                      <div class="action-description">
                        <span class="action-icon">
                          {{
                            message.stepActions[index]?.status === 'current'
                              ? '🔄'
                              : message.stepActions[index]?.status === 'completed'
                                ? '✓'
                                : '⏳'
                          }}
                        </span>
                        <strong>{{ message.stepActions[index]?.actionDescription }}</strong>
                      </div>

                      <div v-if="message.stepActions[index]?.toolParameters" class="tool-params">
                        <span class="tool-icon">⚙️</span>
                        <span class="param-label">{{ $t('common.parameters') }}:</span>
                        <pre class="param-content">{{
                          message.stepActions[index]?.toolParameters
                        }}</pre>
                      </div>

                      <div v-if="message.stepActions[index]?.thinkOutput" class="think-details">
                        <div class="think-header">
                          <span class="think-icon">💭</span>
                          <span class="think-label">{{ $t('chat.thinkingOutput') }}:</span>
                        </div>
                        <div class="think-output">
                          <pre class="think-content">{{
                            message.stepActions[index]?.thinkOutput
                          }}</pre>
                        </div>
                      </div>
                    </div>

                    <!-- Sub-plan steps - New feature -->
                    <div v-if="getSubPlanSteps(message, index)?.length > 0" class="sub-plan-steps">
                      <div class="sub-plan-header">
                        <Icon icon="carbon:tree-view" class="sub-plan-icon" />
                        <span class="sub-plan-title">子执行计划</span>
                      </div>
                      <div class="sub-plan-step-list">
                        <div
                          v-for="(subStep, subStepIndex) in getSubPlanSteps(message, index)"
                          :key="`sub-${index}-${subStepIndex}`"
                          class="sub-plan-step-item"
                          :class="{
                            completed:
                              getSubPlanStepStatus(message, index, subStepIndex) === 'completed',
                            current:
                              getSubPlanStepStatus(message, index, subStepIndex) === 'current',
                            pending:
                              getSubPlanStepStatus(message, index, subStepIndex) === 'pending',
                          }"
                          @click.stop="handleSubPlanStepClick(message, index, subStepIndex)"
                        >
                          <div class="sub-step-indicator">
                            <span class="sub-step-icon">
                              {{
                                getSubPlanStepStatus(message, index, subStepIndex) === 'completed'
                                  ? '✓'
                                  : getSubPlanStepStatus(message, index, subStepIndex) === 'current'
                                    ? '▶'
                                    : '○'
                              }}
                            </span>
                            <span class="sub-step-number">{{ subStepIndex + 1 }}</span>
                          </div>
                          <div class="sub-step-content">
                            <span class="sub-step-title">{{ subStep }}</span>
                            <span class="sub-step-badge">子步骤</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- User input form -->
                    <div
                      v-if="
                        message.planExecution?.userInputWaitState &&
                        getAgentExecutionStatus(message, index) === 'RUNNING'
                      "
                      class="user-input-form-container"
                    >
                      <p class="user-input-message">
                        {{
                          message.planExecution?.userInputWaitState?.message ??
                          $t('chat.userInput.message')
                        }}
                      </p>
                      <p
                        v-if="message.planExecution?.userInputWaitState?.formDescription"
                        class="form-description"
                      >
                        {{ message.planExecution?.userInputWaitState?.formDescription }}
                      </p>

                      <form
                        @submit.prevent="handleUserInputSubmit(message)"
                        class="user-input-form"
                      >
                        <template
                          v-if="
                            message.planExecution?.userInputWaitState?.formInputs &&
                            message.planExecution.userInputWaitState.formInputs.length > 0
                          "
                        >
                          <div
                            v-for="(input, inputIndex) in message.planExecution?.userInputWaitState
                              ?.formInputs"
                            :key="inputIndex"
                            class="form-group"
                          >
                            <label :for="`form-input-${input.label.replace(/\W+/g, '_')}`">
                              {{ input.label }}:
                            </label>
                            <input
                              type="text"
                              :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                              :name="input.label"
                              v-model="formInputsStore[message.id][inputIndex]"
                              class="form-input"
                            />
                          </div>
                        </template>

                        <template v-else>
                          <div class="form-group">
                            <label for="form-input-genericInput">{{ $t('common.input') }}:</label>
                            <input
                              type="text"
                              id="form-input-genericInput"
                              name="genericInput"
                              v-model="message.genericInput"
                              class="form-input"
                            />
                          </div>
                        </template>

                        <button type="submit" class="submit-user-input-btn">
                          {{ $t('chat.userInput.submit') }}
                        </button>
                      </form>
                    </div>
                  </div>
                </div>

                <!-- Display the default processing state only when there is no final content and processing is in progress -->
                <div
                  v-else-if="
                    !message.content &&
                    (message.thinking ||
                      (message.planExecution?.progress !== undefined &&
                        (message.planExecution?.progress ?? 0) < 100))
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

            <!-- 2. TaskPilot Final Response Section - Independent humanized dialogue unit -->
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

import { CommonApiService } from '@/api/common-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { usePlanExecution } from '@/utils/use-plan-execution'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'

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

const handleDirectMode = async (query: string) => {
  try {
    isLoading.value = true

    // Add a thinking state message
    const assistantMessage = addMessage('assistant', '', {
      thinking: '正在理解您的请求并准备回复...',
    })

    // Execute directly
    const response = await DirectApiService.sendMessage(query)

    if (response.planId) {
      console.log('[ChatComponent] Received planId from direct execution:', response.planId)

      if (!assistantMessage.planExecution) {
        assistantMessage.planExecution = {} as any
      }
      assistantMessage.planExecution!.currentPlanId = response.planId

      planExecutionManager.handlePlanExecutionRequested(response.planId, query)

      delete assistantMessage.thinking

      console.log('[ChatComponent] Started polling for plan execution updates')
    } else {
      delete assistantMessage.thinking

      // Generate a natural and human-like response
      const finalResponse = generateDirectModeResponse(response, query)
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
  const errorMsg = error?.message ?? error?.toString() ?? '未知错误'

  // Common error types with friendly prompts
  if (errorMsg.includes('网络') || errorMsg.includes('network') || errorMsg.includes('timeout')) {
    return `抱歉，似乎网络连接有些问题。请检查您的网络连接后再试一次，或者稍等几分钟再重新提问。`
  }

  if (errorMsg.includes('认证') || errorMsg.includes('权限') || errorMsg.includes('auth')) {
    return `抱歉，访问权限出现了问题。这可能是系统配置的问题，请联系管理员或稍后再试。`
  }

  if (errorMsg.includes('格式') || errorMsg.includes('参数') || errorMsg.includes('invalid')) {
    return `抱歉，您的请求格式可能有些问题。能否请您重新表述一下您的需求？我会尽力理解并帮助您。`
  }

  // Generic error response
  return `抱歉，处理您的请求时遇到了一些问题（${errorMsg}）。请稍后再试，或者换个方式表达您的需求，我会尽力帮助您的。`
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

const handleSendMessage = (message: string) => {
  // First, add the user message to the UI.
  addMessage('user', message)

  // Handle messages according to the mode
  if (props.mode === 'plan') {
    // In plan mode, only add UI message, parent component handles the API call
    // This prevents double API calls
    console.log('[ChatComponent] Plan mode message sent, parent should handle:', message)
    // Don't call any API here, just add to UI
  } else {
    // Direct mode is still handled directly
    handleDirectMode(message)
  }
}

// Get agent execution status based on index
const getAgentExecutionStatus = (message: Message, index: number): string => {
  const agentExecutionSequence = message.planExecution?.agentExecutionSequence ?? []
  // 使用安全的索引检查来避免越界访问
  if (index < 0 || index >= agentExecutionSequence.length) {
    return 'IDLE'
  }
  const agentExecution = agentExecutionSequence[index]
  return agentExecution.status ?? 'IDLE'
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

// Get sub-plan steps from agentExecutionSequence
const getSubPlanSteps = (message: Message, stepIndex: number): string[] => {
  try {
    // Find sub-plan from planExecution.agentExecutionSequence
    const agentExecutionSequence = message.planExecution?.agentExecutionSequence
    if (!agentExecutionSequence?.length) {
      console.log('[ChatComponent] No agentExecutionSequence found')
      return []
    }

    // Get corresponding step's agentExecution
    const agentExecution = agentExecutionSequence[stepIndex] as AgentExecutionRecord | undefined
    if (!agentExecution) {
      console.log(`[ChatComponent] No agentExecution found for step ${stepIndex}`)
      return []
    }

    if (!agentExecution.thinkActSteps) {
      console.log(`[ChatComponent] No thinkActSteps found for step ${stepIndex}`)
      return []
    }

    // Find sub-plan in thinkActSteps
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep.subPlanExecutionRecord) {
        console.log(
          `[ChatComponent] Found sub-plan for step ${stepIndex}:`,
          thinkActStep.subPlanExecutionRecord
        )
        const rawSteps = thinkActStep.subPlanExecutionRecord.steps ?? []
        // Apply the same formatting logic as main steps
        return rawSteps.map((step: any) => {
          if (typeof step === 'string') {
            return step
          } else if (typeof step === 'object' && step !== null) {
            return step.title || step.description || `子步骤`
          }
          return `子步骤`
        })
      }
    }

    return []
  } catch (error) {
    console.warn('[ChatComponent] Error getting sub-plan steps:', error)
    return []
  }
}

// Get sub-plan step status - new feature
const getSubPlanStepStatus = (
  message: Message,
  stepIndex: number,
  subStepIndex: number
): string => {
  try {
    const agentExecutionSequence = message.planExecution?.agentExecutionSequence
    if (!agentExecutionSequence?.length) {
      return 'pending'
    }

    const agentExecution = agentExecutionSequence[stepIndex] as AgentExecutionRecord | undefined
    if (!agentExecution) {
      return 'pending'
    }

    if (!agentExecution.thinkActSteps) {
      return 'pending'
    }

    // Find sub-plan in thinkActSteps
    let subPlan = null
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep.subPlanExecutionRecord) {
        subPlan = thinkActStep.subPlanExecutionRecord
        break
      }
    }

    if (!subPlan) {
      return 'pending'
    }

    const currentStepIndex = subPlan.currentStepIndex
    if (subPlan.completed) {
      return 'completed'
    }

    if (currentStepIndex == null) {
      return subStepIndex === 0 ? 'current' : 'pending'
    }

    if (subStepIndex < currentStepIndex) {
      return 'completed'
    } else if (subStepIndex === currentStepIndex) {
      return 'current'
    } else {
      return 'pending'
    }
  } catch (error) {
    console.warn('[ChatComponent] Error getting sub-plan step status:', error)
    return 'pending'
  }
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
            actionDescription: '思考中',
            toolParameters: '等待决策',
            thinkInput: latestThinkAct.thinkInput ?? '',
            thinkOutput: latestThinkAct.thinkOutput ?? '',
            status: planDetails.currentStepIndex !== undefined && index === planDetails.currentStepIndex ? 'current' : 'pending',
          }

          console.log(`[ChatComponent] Step ${index} is thinking`)
        }
      } else {
        lastStepActions[index] = {
          actionDescription: planDetails.currentStepIndex !== undefined && index < planDetails.currentStepIndex ? '已完成' : '等待中',
          toolParameters: '无工具参数',
          thinkInput: '',
          thinkOutput: '',
          status: planDetails.currentStepIndex !== undefined && index < planDetails.currentStepIndex ? 'completed' : 'pending',
        }

        console.log(
          `[ChatComponent] 步骤 ${index} 无执行细节, 状态设为: ${lastStepActions[index].status}`
        )
      }
    }
  } else {
    console.log('[ChatComponent] 没有执行序列数据')
  }

  message.stepActions = [...lastStepActions]

  console.log(
    '[ChatComponent] 步骤动作更新完成:',
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
        thinking: '正在准备执行计划...',
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
      // 更新 planExecution 以确保后续更新能找到它
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
      // 直接设置最终回复，清除所有处理状态
      delete message.thinking

      const finalResponse =
        planDetails.summary ?? planDetails.result ?? planDetails.message ?? '处理完成'
      // Ensure the response is natural
      message.content = generateNaturalResponse(finalResponse)

      console.log('[ChatComponent] Set simple response content:', message.content)
    } else {
      // If there is a title or status information, update the thinking state
      if (planDetails.title) {
        message.thinking = `正在执行: ${planDetails.title}`
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
      return step.title || step.description || `步骤`
    }
    return `步骤`
  })

  // Update the step information in planExecution
  if (message.planExecution) {
    message.planExecution.steps = formattedSteps
  }

  // Process the execution sequence and step actions - Refer to the logic in chat-handler.js
  if (planDetails.agentExecutionSequence && planDetails.agentExecutionSequence.length > 0) {
    console.log(
      '[ChatComponent] 发现执行序列数据，数量:',
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

          message.thinking = `正在思考: ${displayOutput}`
        }
      }
    }
  } else {
    // If there is no execution sequence, use the basic thinking state
    if (message.planExecution) {
      const currentStepIndex = message.planExecution.currentStepIndex ?? 0
      const currentStep = message.planExecution.steps?.[currentStepIndex]
      const stepTitle = typeof currentStep === 'string' ? currentStep : ''
      message.thinking = `正在执行: ${stepTitle}`
    }
  }

  // Handle the user input waiting state
  if (planDetails.userInputWaitState && message.planExecution) {
    console.log('[ChatComponent] 需要用户输入:', planDetails.userInputWaitState)

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
        })) ?? [],
    }

    formInputsStore[message.id] ??= {}
    // Clear the thinking state and display the message waiting for user input
    message.thinking = '等待用户输入...'
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
      finalResponse = '任务已完成'
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
  if (!text) return '我明白了，还有什么我可以帮您的吗？'

  // If it's already in a natural conversation format, return it directly
  if (
    text.includes('我') ||
    text.includes('您') ||
    text.includes('您好') ||
    text.includes('可以')
  ) {
    return text
  }

  // Generate a more natural response based on the text content
  if (text.length < 10) {
    return `${text}！还有什么需要我帮助的吗？`
  } else if (text.length < 50) {
    return `好的，${text}。如果您还有其他问题，请随时告诉我。`
  } else {
    return `${text}\n\n希望这个回答对您有帮助！还有什么我可以为您做的吗？`
  }
}

// Generate a natural response for a completed plan
const generateCompletedPlanResponse = (text: string): string => {
  if (!text) return '任务已完成！还有什么我可以帮您的吗？'
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

      const summary = details.summary ?? details.result ?? '任务已完成';
      let finalResponse = summary;
      if (!finalResponse.includes('我') && !finalResponse.includes('您')) {
        if (finalResponse.includes('成功') || finalResponse.includes('完成')) {
          finalResponse = `很好！${finalResponse}。如果您还有其他需要帮助的地方，请随时告诉我。`;
        } else {
          finalResponse = `我已经完成了您的请求：${finalResponse}`;
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
    type: 'error',
    content: message,
    timestamp: new Date(),
  }
}

// Format the response text to make it more like a natural conversation
const formatResponseText = (text: string): string => {
  if (!text) return ''

  // Convert line breaks to HTML line breaks
  let formatted = text.replace(/\n\n/g, '<br><br>').replace(/\n/g, '<br>')

  // Add appropriate paragraph spacing and formatting
  formatted = formatted.replace(/(<br><br>)/g, '</p><p>')

  // Wrap with p tags if there are multiple paragraphs
  if (formatted.includes('</p><p>')) {
    formatted = `<p>${formatted}</p>`
  }

  return formatted
}

// Handle user input form submission
const handleUserInputSubmit = async (message: Message) => {
  if (!message.planExecution?.currentPlanId || !message.planExecution.userInputWaitState) {
    console.error('[ChatComponent] 缺少planExecution.currentPlanId或userInputWaitState')
    return
  }

  try {
    // Collect form data
    const inputData: any = {}

    const formInputs = message.planExecution.userInputWaitState.formInputs
    if (formInputs && formInputs.length > 0) {
      // Multiple fields case
      Object.entries(formInputsStore[message.id]).forEach(([index, value]) => {
        const numIndex = parseInt(index, 10)
        const label = formInputs[numIndex]?.label || `input_${index}`
        inputData[label] = value
      })
    } else {
      // Single generic input case
      inputData.genericInput = message.genericInput ?? ''
    }

    console.log('[ChatComponent] 提交用户输入:', inputData)

    // Submit user input via API
    const response = await CommonApiService.submitFormInput(
      message.planExecution.currentPlanId,
      inputData
    )

    // Clear the user input waiting state
    delete message.planExecution.userInputWaitState
    delete message.genericInput
    delete formInputsStore[message.id]

    // Continue polling for plan updates (should resume automatically after submission)
    planExecution.startPolling()

    console.log('[ChatComponent] 用户输入提交成功:', response)
  } catch (error: any) {
    console.error('[ChatComponent] 用户输入提交失败:', error)
    // 可以在UI中显示错误消息
    alert(`提交失败: ${error?.message || '未知错误'}`)
  }
}

watch(
  () => props.initialPrompt,
  (newPrompt, oldPrompt) => {
    console.log('[ChatComponent] initialPrompt changed from:', oldPrompt, 'to:', newPrompt)
    if (newPrompt && typeof newPrompt === 'string' && newPrompt.trim() && newPrompt !== oldPrompt) {
      console.log('[ChatComponent] Processing changed initial prompt:', newPrompt)
      nextTick(() => {
        handleSendMessage(newPrompt)
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
      handleSendMessage(props.initialPrompt!)
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

// Expose methods to parent components for usage
defineExpose({
  handleSendMessage,
  handlePlanUpdate,
  handlePlanCompleted,
  handleDialogRoundStart,
  addMessage,
  handlePlanError
})
</script>

<style lang="less" scoped>
.chat-container {
  flex: 1; /* Occupy the remaining space of the parent container */
  display: flex;
  flex-direction: column;
  height: 100%; /* Fill the height of the parent container */
  min-height: 0; /* Allow shrinking */
  overflow: hidden; /* Prevent container overflow */
}

.messages {
  padding: 24px;
  flex: 1; /* Use flex: 1 instead of height: 100% */
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto; /* Use auto instead of scroll */
  min-height: 0; /* Ensure it can shrink */
  /* Add smooth scrolling */
  scroll-behavior: smooth;
  /* Improve scrollbar style */
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;

  /* WebKit scrollbar styling */
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
  /* 1. TaskPilot Thinking/Processing Section Style */
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

  /* 2. TaskPilot Final Response Section Style - Simulate Human Conversation Unit */
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

          /* Make the text look more like a natural conversation */
          p {
            margin: 0 0 12px 0;

            &:last-child {
              margin-bottom: 0;
            }
          }

          /* Enhance readability */
          strong {
            color: #f8fafc;
            font-weight: 600;
          }

          em {
            color: #e2e8f0;
            font-style: italic;
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

  .progress {
    margin-top: 12px;

    .progress-bar {
      width: 100%;
      height: 4px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 2px;
      overflow: hidden;
      margin-bottom: 8px;

      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        transition: width 0.3s ease;
      }
    }

    .progress-text {
      font-size: 12px;
      color: #888888;
    }
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

      /* Sub-plan Step Style - New Feature */
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
          margin-left: 20px; /* 缩进显示父子关系 */

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
    .form-group {
      margin-bottom: 16px;

      label {
        display: block;
        margin-bottom: 6px;
        font-size: 13px;
        font-weight: 500;
        color: #ffffff;
      }

      .form-input {
        width: 100%;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 6px;
        color: #ffffff;
        font-size: 14px;
        transition: border-color 0.2s ease;

        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
        }

        &::placeholder {
          color: #888888;
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

/* Scroll to Bottom Button */
.scroll-to-bottom-btn {
  position: absolute;
  bottom: 120px; /* Above the input field */
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

  /* Add pulse animation */
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
