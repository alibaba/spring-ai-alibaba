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
          <!-- 用户消息部分 -->
          <div v-if="message.type === 'user'" class="user-message">
            {{ message.content }}
          </div>

          <!-- 助手消息的三部分结构 -->
          <div v-else class="assistant-message">
            <!-- 1. TaskPilot 思考/处理部分 - 只在有处理内容时显示 -->
            <div
              class="thinking-section"
              v-if="
                message.thinking ||
                message.planExecution?.progress !== undefined ||
                (message.planExecution?.steps && message.planExecution.steps.length > 0)
              "
            >
              <div class="thinking-header">
                <div class="thinking-avatar">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                </div>
                <div class="thinking-label">TaskPilot 思考/处理</div>
              </div>

              <div class="thinking-content">
                <!-- 基础思考状态 -->
                <div class="thinking" v-if="message.thinking">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                  <span>{{ message.thinking }}</span>
                </div>

                <!-- 进度条 -->
                <div class="progress" v-if="message.planExecution?.progress !== undefined">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: message.planExecution.progress + '%' }"></div>
                  </div>
                  <span class="progress-text">{{ message.planExecution.progressText || '处理中...' }}</span>
                </div>

                <!-- 步骤执行详情 -->
                <div class="steps-container" v-if="message.planExecution?.steps && message.planExecution.steps.length > 0">
                  <h4 class="steps-title">{{ $t('chat.stepExecutionDetails') }}</h4>

                  <!-- 遍历所有步骤 -->
                  <div
                    v-for="(step, index) in message.planExecution.steps"
                    :key="index"
                    class="ai-section"
                    :class="{
                      current: index === message.planExecution?.currentStepIndex,
                      completed: index < (message.planExecution?.currentStepIndex || 0),
                      pending: index > (message.planExecution?.currentStepIndex || 0),
                    }"
                    @click.stop="handleStepClick(message, index)"
                  >
                    <div class="section-header">
                      <span class="step-icon">
                        {{
                          index < (message.planExecution?.currentStepIndex || 0)
                            ? '✓'
                            : index === (message.planExecution?.currentStepIndex || 0)
                              ? '▶'
                              : '○'
                        }}
                      </span>
                      <span class="step-title">
                        {{ step || `步骤 ${index + 1}` }}
                      </span>
                      <span v-if="index === message.planExecution?.currentStepIndex" class="step-status current">
                        {{ $t('chat.status.executing') }}
                      </span>
                      <span
                        v-else-if="index < (message.planExecution?.currentStepIndex || 0)"
                        class="step-status completed"
                      >
                        {{ $t('chat.status.completed') }}
                      </span>
                      <span v-else class="step-status pending"> {{ $t('chat.status.pending') }} </span>
                    </div>

                    <!-- 显示步骤执行动作信息 -->
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

                    <!-- 子计划步骤 - 新增功能 -->
                    <div
                      v-if="getSubPlanSteps(message, index)?.length > 0"
                      class="sub-plan-steps"
                    >
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
                            completed: getSubPlanStepStatus(message, index, subStepIndex) === 'completed',
                            current: getSubPlanStepStatus(message, index, subStepIndex) === 'current',
                            pending: getSubPlanStepStatus(message, index, subStepIndex) === 'pending'
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

                    <!-- 用户输入表单 -->
                    <div
                      v-if="message.planExecution?.userInputWaitState && index === message.planExecution?.currentStepIndex"
                      class="user-input-form-container"
                    >
                      <p class="user-input-message">
                        {{ message.planExecution.userInputWaitState.message || $t('chat.userInput.message') }}
                      </p>
                      <p v-if="message.planExecution.userInputWaitState.formDescription" class="form-description">
                        {{ message.planExecution.userInputWaitState.formDescription }}
                      </p>

                      <form
                        @submit.prevent="handleUserInputSubmit(message)"
                        class="user-input-form"
                      >
                        <div
                          v-if="
                            message.planExecution?.userInputWaitState?.formInputs &&
                            message.planExecution.userInputWaitState.formInputs.length > 0
                          "
                          v-for="(input, inputIndex) in message.planExecution.userInputWaitState.formInputs"
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
                            v-model="message.planExecution.userInputWaitState.formInputs[inputIndex].value"
                            class="form-input"
                          />
                        </div>

                        <div v-else class="form-group">
                          <label for="form-input-genericInput">{{ $t('common.input') }}:</label>
                          <input
                            type="text"
                            id="form-input-genericInput"
                            name="genericInput"
                            v-model="message.genericInput"
                            class="form-input"
                          />
                        </div>

                        <button type="submit" class="submit-user-input-btn">{{ $t('chat.userInput.submit') }}</button>
                      </form>
                    </div>
                  </div>
                </div>

                <!-- 只在没有最终内容且正在处理时显示默认处理状态 -->
                <div
                  v-else-if="
                    !message.content &&
                    (message.thinking || (message.planExecution?.progress !== undefined && message.planExecution.progress < 100))
                  "
                  class="default-processing"
                >
                  <div class="processing-indicator">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span>{{ message.thinking || '正在处理您的请求...' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. TaskPilot 最终回复部分 - 独立的人性化对话单元 -->
            <div class="response-section">
              <div class="response-header">
                <div class="response-avatar">
                  <Icon icon="carbon:bot" class="bot-icon" />
                </div>
                <div class="response-name">TaskPilot:</div>
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
                    <span class="typing-text">正在组织语言回复您...</span>
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
            <!-- 加载状态的思考部分 -->
            <div class="thinking-section">
              <div class="thinking-header">
                <div class="thinking-avatar">
                  <Icon icon="carbon:thinking" class="thinking-icon" />
                </div>
                <div class="thinking-label">TaskPilot 思考/处理</div>
              </div>
              <div class="thinking-content">
                <div class="default-processing">
                  <div class="processing-indicator">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span>正在思考如何最好地帮助您...</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 加载状态的回复部分 -->
            <div class="response-section">
              <div class="response-header">
                <div class="response-avatar">
                  <Icon icon="carbon:bot" class="bot-icon" />
                </div>
                <div class="response-name">TaskPilot:</div>
              </div>
              <div class="response-content">
                <div class="response-placeholder">
                  <div class="typing-indicator">
                    <div class="typing-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span class="typing-text">正在为您整理最合适的回答...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 滚动到底部按钮 -->
    <div
      v-if="showScrollToBottom"
      class="scroll-to-bottom-btn"
      @click="forceScrollToBottom"
      title="滚动到底部"
    >
      <Icon icon="carbon:chevron-down" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'

import { CommonApiService } from '@/api/common-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { usePlanExecution } from '@/utils/use-plan-execution'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import type { PlanExecutionRecord } from '@/types/plan-execution-record'

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
  initialPrompt?: string
  mode?: 'plan' | 'direct' // 计划模式或直接聊天模式
}

interface Emits {
  (e: 'user-message-send-requested', message: string): void
  (e: 'input-clear'): void
  (e: 'input-update-state', enabled: boolean, placeholder?: string): void
  (e: 'input-focus'): void
  (e: 'step-selected', planId: string, stepIndex: number): void
  (e: 'sub-plan-step-selected', parentPlanId: string, subPlanId: string, stepIndex: number, subStepIndex: number): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'plan', // 使用计划模式，通过 plan-execution-manager 处理
})
const emit = defineEmits<Emits>()

// 使用计划执行管理器
const planExecution = usePlanExecution()


const messagesRef = ref<HTMLElement>()
const isLoading = ref(false)
const messages = ref<Message[]>([])
const pollingInterval = ref<number>()
const showScrollToBottom = ref(false)

const addMessage = (type: 'user' | 'assistant', content: string, options?: Partial<Message>) => {
  const message: Message = {
    id: Date.now().toString(),
    type,
    content,
    timestamp: new Date(),
    ...options,
  }

  // 如果是助手消息，确保有基本的思考状态，即使没有内容
  if (type === 'assistant') {
    if (!message.thinking && !message.content) {
      message.thinking = '正在思考...'
    }
  }

  messages.value.push(message)
  // 新消息时强制滚动到底部
  forceScrollToBottom()
  return message
}

const updateLastMessage = (updates: Partial<Message>) => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.type === 'assistant') {
    Object.assign(lastMessage, updates)
    // 内容更新时也要滚动，确保用户能看到最新内容
    scrollToBottom()
  }
}


const handleDirectMode = async (query: string) => {
  try {
    isLoading.value = true

    // 添加思考状态消息
    const assistantMessage = addMessage('assistant', '', {
      thinking: '正在理解您的请求并准备回复...',
    })

    // 直接执行
    const response = await DirectApiService.sendMessage(query)

    // 清除思考状态 - 使用delete而不是赋值undefined
    delete assistantMessage.thinking

    // 生成自然的人性化回复
    const finalResponse = generateDirectModeResponse(response, query)
    assistantMessage.content = finalResponse
  } catch (error: any) {
    console.error('Direct mode error:', error)
    updateLastMessage({
      content: generateErrorResponse(error),
    })
  } finally {
    isLoading.value = false
  }
}

// 从response中获取响应内容
const generateDirectModeResponse = (response: any, _originalQuery: string): string => {
  return response.result || response.message || response.content || ''
}

// 生成错误响应
const generateErrorResponse = (error: any): string => {
  const errorMsg = error?.message || error?.toString() || '未知错误'

  // 常见错误类型的友好提示
  if (errorMsg.includes('网络') || errorMsg.includes('network') || errorMsg.includes('timeout')) {
    return `抱歉，似乎网络连接有些问题。请检查您的网络连接后再试一次，或者稍等几分钟再重新提问。`
  }

  if (errorMsg.includes('认证') || errorMsg.includes('权限') || errorMsg.includes('auth')) {
    return `抱歉，访问权限出现了问题。这可能是系统配置的问题，请联系管理员或稍后再试。`
  }

  if (errorMsg.includes('格式') || errorMsg.includes('参数') || errorMsg.includes('invalid')) {
    return `抱歉，您的请求格式可能有些问题。能否请您重新表述一下您的需求？我会尽力理解并帮助您。`
  }

  // 通用错误回复
  return `抱歉，处理您的请求时遇到了一些问题（${errorMsg}）。请稍后再试，或者换个方式表达您的需求，我会尽力帮助您的。`
}


const scrollToBottom = (force = false) => {
  nextTick(() => {
    if (messagesRef.value) {
      const container = messagesRef.value

      // 检查是否需要滚动到底部
      const isNearBottom =
        force || container.scrollHeight - container.scrollTop - container.clientHeight < 150

      if (isNearBottom) {
        // 使用 smooth 滚动，除非强制滚动
        container.scrollTo({
          top: container.scrollHeight,
          behavior: force ? 'auto' : 'smooth',
        })
      }
    }
  })
}

// 强制滚动到底部的辅助函数
const forceScrollToBottom = () => {
  scrollToBottom(true)
  // 滚动后隐藏滚动按钮
  showScrollToBottom.value = false
}

// 检查是否需要显示滚动到底部按钮
const checkScrollPosition = () => {
  if (messagesRef.value) {
    const container = messagesRef.value
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 150
    showScrollToBottom.value = !isNearBottom && messages.value.length > 0
  }
}

// 添加滚动监听器
const addScrollListener = () => {
  if (messagesRef.value) {
    messagesRef.value.addEventListener('scroll', checkScrollPosition)
  }
}

// 移除滚动监听器
const removeScrollListener = () => {
  if (messagesRef.value) {
    messagesRef.value.removeEventListener('scroll', checkScrollPosition)
  }
}

const handleSendMessage = (message: string) => {
  // 首先添加用户消息到UI
  addMessage('user', message)

  // 通过 emit 通知父组件清空输入
  emit('input-clear')

  // 根据模式处理消息
  if (props.mode === 'plan') {
    // 在计划模式下，触发用户消息发送请求事件
    emit('user-message-send-requested', message)
  } else {
    // 直接模式仍然直接处理
    handleDirectMode(message)
  }
}

// 处理步骤点击事件 - 只暴露事件，不处理具体逻辑
const handleStepClick = (message: Message, stepIndex: number) => {
  if (!message.planExecution?.currentPlanId) {
    console.warn('[ChatComponent] Cannot handle step click: missing currentPlanId')
    return
  }

  console.log('[ChatComponent] Step clicked:', {
    planId: message.planExecution.currentPlanId,
    stepIndex: stepIndex,
    stepTitle: message.planExecution.steps?.[stepIndex]
  })

  // 向父组件发射步骤选择事件
  emit('step-selected', message.planExecution.currentPlanId, stepIndex)
}

// 获取子计划步骤 - 新增功能
const getSubPlanSteps = (message: Message, stepIndex: number): string[] => {
  try {
    // 从planExecution.agentExecutionSequence中查找子计划
    const agentExecutionSequence = message.planExecution?.agentExecutionSequence
    if (!agentExecutionSequence || !Array.isArray(agentExecutionSequence)) {
      console.log('[ChatComponent] No agentExecutionSequence found')
      return []
    }

    // 获取对应步骤的agentExecution
    const agentExecution = agentExecutionSequence[stepIndex]
    if (!agentExecution || !agentExecution.thinkActSteps) {
      console.log(`[ChatComponent] No agentExecution or thinkActSteps found for step ${stepIndex}`)
      return []
    }

    // 查找thinkActSteps中是否有子计划
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep && thinkActStep.subPlanExecutionRecord) {
        console.log(`[ChatComponent] Found sub-plan for step ${stepIndex}:`, thinkActStep.subPlanExecutionRecord)
        return thinkActStep.subPlanExecutionRecord.steps || []
      }
    }

    return []
  } catch (error) {
    console.warn('[ChatComponent] Error getting sub-plan steps:', error)
    return []
  }
}

// 获取子计划步骤状态 - 新增功能
const getSubPlanStepStatus = (message: Message, stepIndex: number, subStepIndex: number): string => {
  try {
    const agentExecutionSequence = message.planExecution?.agentExecutionSequence
    if (!agentExecutionSequence || !Array.isArray(agentExecutionSequence)) {
      return 'pending'
    }

    const agentExecution = agentExecutionSequence[stepIndex]
    if (!agentExecution || !agentExecution.thinkActSteps) {
      return 'pending'
    }

    // 查找thinkActSteps中的子计划
    let subPlan = null
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep && thinkActStep.subPlanExecutionRecord) {
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

    if (currentStepIndex === undefined || currentStepIndex === null) {
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
    if (!agentExecutionSequence || !Array.isArray(agentExecutionSequence)) {
      console.warn('[ChatComponent] No agentExecutionSequence data for sub-plan step click')
      return
    }

    const agentExecution = agentExecutionSequence[stepIndex]
    if (!agentExecution || !agentExecution.thinkActSteps) {
      console.warn('[ChatComponent] No agentExecution or thinkActSteps for step click')
      return
    }

    // Find sub-plan in thinkActSteps
    let subPlan = null
    for (const thinkActStep of agentExecution.thinkActSteps) {
      if (thinkActStep && thinkActStep.subPlanExecutionRecord) {
        subPlan = thinkActStep.subPlanExecutionRecord
        break
      }
    }

    if (!subPlan || !subPlan.currentPlanId) {
      console.warn('[ChatComponent] No sub-plan data for step click')
      return
    }

    // Emit event with necessary identifiers for parent component to handle
    emit('sub-plan-step-selected', message.planExecution?.currentPlanId || '', subPlan.currentPlanId, stepIndex, subStepIndex)
  } catch (error) {
    console.error('[ChatComponent] Error handling sub-plan step click:', error)
  }
}

// 旧的handlePlanUpdate函数已移除，保留计算进度和更新步骤动作的函数

// 计算执行进度（基于 chat-handler.js 逻辑）
const calculateProgress = (planDetails: any) => {
  const totalSteps = planDetails.steps?.length || 0
  const currentStep = planDetails.currentStepIndex ?? 0

  if (totalSteps === 0) {
    return { percentage: 0, text: '准备中...' }
  }

  const percentage = Math.min(Math.round((currentStep / totalSteps) * 80) + 20, 95)
  let text = `执行步骤 ${currentStep + 1}/${totalSteps}`

  if (planDetails.steps[currentStep]) {
    const stepTitle =
      planDetails.steps[currentStep].title ||
      planDetails.steps[currentStep].description ||
      planDetails.steps[currentStep]
    text += `: ${stepTitle}`
  }

  return { percentage, text }
}

// 更新步骤执行动作（基于 chat-handler.js 逻辑）
const updateStepActions = (message: Message, planDetails: any) => {
  if (!message.planExecution?.steps) return

  console.log(
    '[ChatComponent] 开始更新步骤动作, 步骤数:',
    message.planExecution.steps.length,
    '执行序列:',
    planDetails.agentExecutionSequence?.length || 0
  )

  // 初始化存储每个步骤的最后执行动作
  const lastStepActions = new Array(message.planExecution.steps.length).fill(null)

  // 遍历所有执行序列，匹配步骤并更新动作
  if (planDetails.agentExecutionSequence?.length > 0) {
    // 检查执行序列与步骤数是否匹配
    const sequenceLength = Math.min(planDetails.agentExecutionSequence.length, message.planExecution.steps.length)

    for (let index = 0; index < sequenceLength; index++) {
      const execution = planDetails.agentExecutionSequence[index]

      if (execution?.thinkActSteps?.length > 0) {
        const latestThinkAct = execution.thinkActSteps[execution.thinkActSteps.length - 1]

        if (latestThinkAct?.actionDescription && latestThinkAct?.toolParameters) {
          // 保存此步骤的最后执行动作
          lastStepActions[index] = {
            actionDescription: latestThinkAct.actionDescription,
            toolParameters:
              typeof latestThinkAct.toolParameters === 'string'
                ? latestThinkAct.toolParameters
                : JSON.stringify(latestThinkAct.toolParameters, null, 2),
            thinkInput: latestThinkAct.thinkInput || '',
            thinkOutput: latestThinkAct.thinkOutput || '',
            status:
              index < planDetails.currentStepIndex
                ? 'completed'
                : index === planDetails.currentStepIndex
                  ? 'current'
                  : 'pending',
          }

          console.log(
            `[ChatComponent] 步骤 ${index} 已设置动作: ${lastStepActions[index].actionDescription}`
          )
        } else if (latestThinkAct) {
          // 思考中状态
          lastStepActions[index] = {
            actionDescription: '思考中',
            toolParameters: '等待决策中',
            thinkInput: latestThinkAct.thinkInput || '',
            thinkOutput: latestThinkAct.thinkOutput || '',
            status: index === planDetails.currentStepIndex ? 'current' : 'pending',
          }

          console.log(`[ChatComponent] 步骤 ${index} 正在思考中`)
        } else {
          lastStepActions[index] = {
            actionDescription: '执行完成',
            toolParameters: '无工具',
            thinkInput: '',
            thinkOutput: '',
            status: 'completed',
          }

          console.log(`[ChatComponent] 步骤 ${index} 执行完成`)
        }
      } else {
        // 没有thinkActSteps的情况
        lastStepActions[index] = {
          actionDescription: index < planDetails.currentStepIndex ? '已完成' : '待执行',
          toolParameters: '无工具参数',
          thinkInput: '',
          thinkOutput: '',
          status: index < planDetails.currentStepIndex ? 'completed' : 'pending',
        }

        console.log(
          `[ChatComponent] 步骤 ${index} 无执行细节, 状态设为: ${lastStepActions[index].status}`
        )
      }
    }
  } else {
    console.log('[ChatComponent] 没有执行序列数据')
  }

  // 将步骤动作信息附加到消息上
  message.stepActions = lastStepActions

  console.log(
    '[ChatComponent] 步骤动作更新完成:',
    JSON.stringify(lastStepActions.map(a => a?.actionDescription))
  )
}

// 处理对话轮次开始
const handleDialogRoundStart = (planId: string, query: string) => {
  console.log('[ChatComponent] Starting dialog round with planId:', planId, 'query:', query)

  if (planId && query) {
    // 添加用户消息（如果还没有的话）
    const hasUserMessage = messages.value.findIndex(m => m.type === 'user' && m.content === query)

    if (hasUserMessage === -1) {
      addMessage('user', query)
      console.log('[ChatComponent] Added user message:', query)
    } else {
      console.log('[ChatComponent] User message already exists:', query)
    }

    // 检查是否已经有针对此计划的助手消息，使用与 handlePlanUpdate 相同的查找逻辑
    const existingAssistantMsg = messages.value.findIndex(
      m => m.planExecution?.currentPlanId === planId && m.type === 'assistant'
    )

    // 如果没有现有消息，添加助手消息准备显示步骤
    if (existingAssistantMsg === -1) {
      addMessage('assistant', '', {
        planExecution: { currentPlanId: planId } as PlanExecutionRecord,
        thinking: '正在准备执行计划...'
      })
      console.log('[ChatComponent] Created new assistant message for planId:', planId)
    } else {
      console.log('[ChatComponent] Found existing assistant message for planId:', planId)
    }

    // 滚动到底部确保用户能看到最新进展
    scrollToBottom()
  }
}

// 处理计划更新 - 使用基于 rootPlanId 的新方案
const handlePlanUpdate = (rootPlanId: string) => {
  console.log('[ChatComponent] Processing plan update with rootPlanId:', rootPlanId)
  
  // 从缓存中获取 PlanExecutionRecord
  const planDetails = planExecutionManager.getCachedPlanRecord(rootPlanId)
  
  if (!planDetails) {
    console.warn('[ChatComponent] No cached plan data found for rootPlanId:', rootPlanId)
    return
  }
  
  console.log('[ChatComponent] Retrieved plan details from cache:', planDetails)
  console.log('[ChatComponent] Plan steps:', planDetails?.steps)
  console.log('[ChatComponent] Plan completed:', planDetails?.completed)

  if (!planDetails.currentPlanId) {
    console.warn('[ChatComponent] Plan update missing currentPlanId')
    return
  }

  // 找到对应的消息 - 使用 currentPlanId 字段
  const messageIndex = messages.value.findIndex(
    m => m.planExecution?.currentPlanId === planDetails.currentPlanId && m.type === 'assistant'
  )
  let message

  if (messageIndex !== -1) {
    message = messages.value[messageIndex]
    console.log('[ChatComponent] Found existing assistant message for currentPlanId:', planDetails.currentPlanId)
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
        content: m.content?.substring(0, 50),
      }))
    )

    // 如果找不到对应消息，应该已经由 handleDialogRoundStart 创建了，这里不再创建新消息
    // 而是尝试找到最近的助手消息来更新
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

  // 确保 planExecution 存在
  if (!message.planExecution) {
    message.planExecution = {} as PlanExecutionRecord
  }

  // 更新 planExecution 数据
  message.planExecution = { ...planDetails }

  // 处理简单响应（没有步骤的情况）
  if (!planDetails.steps || planDetails.steps.length === 0) {
    console.log('[ChatComponent] Handling simple response without steps')

    if (planDetails.completed) {
      // 直接设置最终回复，清除所有处理状态
      delete message.thinking

      let finalResponse =
        planDetails.summary || planDetails.result || planDetails.message || '处理完成'
      // 确保回复自然
      message.content = generateNaturalResponse(finalResponse)

      console.log('[ChatComponent] Set simple response content:', message.content)
    } else {
      // 如果有标题或状态信息，更新思考状态
      if (planDetails.title) {
        message.thinking = `正在执行: ${planDetails.title}`
      }
    }

    scrollToBottom()
    return
  }

  // 处理有步骤的计划...
  // 处理步骤信息 - 确保格式一致
  const formattedSteps = planDetails.steps.map((step: any) => {
    // 如果步骤是字符串，转换为对象格式
    if (typeof step === 'string') {
      return { title: step, description: step }
    }
    // 如果是对象但缺少title，使用description
    else if (typeof step === 'object') {
      return {
        title: step.title || step.description || `步骤`,
        description: step.description || step.title || '',
        ...step,
      }
    }
    return step
  })

  // 更新步骤信息到 planExecution 中
  message.planExecution.steps = formattedSteps

  // 处理执行序列和步骤动作 - 参考chat-handler.js的逻辑
  if (planDetails.agentExecutionSequence && planDetails.agentExecutionSequence.length > 0) {
    console.log(
      '[ChatComponent] 发现执行序列数据，数量:',
      planDetails.agentExecutionSequence.length
    )

    // 调用updateStepActions更新步骤动作信息
    updateStepActions(message, planDetails)

    // 从当前正在执行的步骤更新思考状态
    const currentStepIndex = planDetails.currentStepIndex || 0
    if (currentStepIndex >= 0 && currentStepIndex < planDetails.agentExecutionSequence.length) {
      const currentExecution = planDetails.agentExecutionSequence[currentStepIndex]
      if (currentExecution && currentExecution.thinkActSteps && currentExecution.thinkActSteps.length > 0) {
        const latestThinkAct =
          currentExecution.thinkActSteps[currentExecution.thinkActSteps.length - 1]
        if (latestThinkAct && latestThinkAct.thinkOutput) {
          // 如果思考输出太长，截断它
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
    // 如果没有执行序列，使用基本思考状态
    const currentStep = message.planExecution?.steps?.[message.planExecution?.currentStepIndex || 0]
    const stepTitle = (typeof currentStep === 'string') 
      ? currentStep 
      : "";
    message.thinking = `正在执行: ${stepTitle}`
  }

  // 处理用户输入等待状态
  if (planDetails.userInputWaitState) {
    console.log('[ChatComponent] 需要用户输入:', planDetails.userInputWaitState)

    // 将用户输入等待状态附加到 planExecution 上
    if (!message.planExecution.userInputWaitState) {
      message.planExecution.userInputWaitState = {}
    }
    message.planExecution.userInputWaitState = {
      message: planDetails.userInputWaitState.message || '',
      formDescription: planDetails.userInputWaitState.formDescription || '',
      formInputs:
        planDetails.userInputWaitState.formInputs?.map((input: any) => ({
          label: input.label,
          value: input.value || '',
        })) || [],
    }

    // 清除思考状态，显示等待用户输入的消息
    message.thinking = '等待用户输入...'
  } else {
    // 如果没有用户输入等待状态，清除之前的状态
    if (message.planExecution.userInputWaitState) {
      delete message.planExecution.userInputWaitState
    }
  }

  // 检查计划是否已完成
  if (planDetails.completed || planDetails.status === 'completed') {
    console.log('[ChatComponent] Plan is completed, updating final response')
    // 清除所有处理状态
    delete message.thinking

    // 设置最终响应内容 - 模拟人类对话回复
    let finalResponse = ''
    if (planDetails.summary) {
      finalResponse = planDetails.summary
    } else if (planDetails.result) {
      finalResponse = planDetails.result
    } else {
      finalResponse = '任务已完成'
    }

    // 生成自然的人性化回复
    message.content = generateCompletedPlanResponse(finalResponse, planDetails)

    console.log('[ChatComponent] Updated completed message:', message.content)
  }

  // 滚动到底部确保用户能看到最新进展
  scrollToBottom()

}

// 生成自然回复的辅助函数
const generateNaturalResponse = (text: string): string => {
  if (!text) return '我明白了，还有什么我可以帮您的吗？'

  // 如果已经是自然对话形式，直接返回
  if (
    text.includes('我') ||
    text.includes('您') ||
    text.includes('您好') ||
    text.includes('可以')
  ) {
    return text
  }

  // 根据文本内容生成更自然的回复
  if (text.length < 10) {
    return `${text}！还有什么需要我帮助的吗？`
  } else if (text.length < 50) {
    return `好的，${text}。如果您还有其他问题，请随时告诉我。`
  } else {
    return `${text}\n\n希望这个回答对您有帮助！还有什么我可以为您做的吗？`
  }
}

// 生成完成计划的自然回复
const generateCompletedPlanResponse = (text: string, planDetails: any): string => {
  if (!text) return '任务已完成！还有什么我可以帮您的吗？'

  // 如果已经是自然对话形式，确保有合适的结尾
  if (text.includes('我') || text.includes('您')) {
    if (
      !text.includes('?') &&
      !text.includes('？') &&
      !text.includes('。') &&
      !text.includes('！')
    ) {
      return `${text}。还有其他需要帮助的地方吗？`
    }
    return text
  }

  // 根据计划类型生成回复
  const hasSteps = planDetails?.steps?.length > 0
  if (hasSteps) {
    return `很好！我已经完成了您的任务：${text}\n\n所有步骤都已成功执行。还有什么我可以帮您处理的吗？`
  } else {
    return `${text}\n\n希望这个回答对您有帮助！如果还有其他问题，请随时告诉我。`
  }
}

// 处理计划完成事件
const handlePlanCompleted = (details: any) => {
  console.log('[ChatComponent] Plan completed:', details)

  if (details && details.planId) {
    // 找到对应的消息并更新为完成状态
    const messageIndex = messages.value.findIndex(m => m.planExecution?.currentPlanId === details.planId)
    if (messageIndex !== -1) {
      const message = messages.value[messageIndex]
      // 清除所有处理状态，显示为完成
      delete message.thinking

      // 生成人性化的最终回复
      const summary = details.summary || details.result || '任务已完成'

      // 确保回复听起来更像人类对话
      let finalResponse = summary
      if (!finalResponse.includes('我') && !finalResponse.includes('您')) {
        if (finalResponse.includes('成功') || finalResponse.includes('完成')) {
          finalResponse = `很好！${finalResponse}。如果您还有其他需要帮助的地方，请随时告诉我。`
        } else {
          finalResponse = `我已经完成了您的请求：${finalResponse}`
        }
      }

      message.content = finalResponse

      console.log('[ChatComponent] Updated completed message:', message.content)

    } else {
      console.warn('[ChatComponent] No message found for completed planId:', details.planId)
    }
  }
}

// 格式化回复文本，让其更像自然对话
const formatResponseText = (text: string): string => {
  if (!text) return ''

  // 将换行符转换为HTML换行
  let formatted = text.replace(/\n\n/g, '<br><br>').replace(/\n/g, '<br>')

  // 添加适当的段落间距和格式
  formatted = formatted.replace(/(<br><br>)/g, '</p><p>')

  // 如果有多个段落，用p标签包装
  if (formatted.includes('</p><p>')) {
    formatted = `<p>${formatted}</p>`
  }

  return formatted
}

// 处理用户输入表单提交
const handleUserInputSubmit = async (message: Message) => {
  if (!message.planExecution?.currentPlanId || !message.planExecution?.userInputWaitState) {
    console.error('[ChatComponent] 缺少planExecution.currentPlanId或userInputWaitState')
    return
  }

  try {
    // 收集表单数据
    let inputData: any = {}

    if (message.planExecution.userInputWaitState.formInputs && message.planExecution.userInputWaitState.formInputs.length > 0) {
      // 多个字段的情况
      message.planExecution.userInputWaitState.formInputs.forEach((input: any) => {
        inputData[input.label] = input.value || ''
      })
    } else {
      // 单个通用输入的情况
      inputData.genericInput = message.genericInput || ''
    }

    console.log('[ChatComponent] 提交用户输入:', inputData)

    // 通过API提交用户输入
    const response = await CommonApiService.submitFormInput(message.planExecution.currentPlanId, inputData)

    // 清除用户输入等待状态
    delete message.planExecution.userInputWaitState
    delete message.genericInput

    // 继续轮询以获取计划更新（提交后应该会自动继续执行）
    planExecution.startPolling()

    console.log('[ChatComponent] 用户输入提交成功:', response)
  } catch (error: any) {
    console.error('[ChatComponent] 用户输入提交失败:', error)
    // 可以在UI中显示错误消息
    alert(`提交失败: ${error?.message || '未知错误'}`)
  }
}

onMounted(() => {
  console.log('[ChatComponent] Mounted, setting up event listeners')

  // 等待 DOM 更新后添加滚动监听器
  nextTick(() => {
    addScrollListener()
  })

  // 移除自动发送初始提示的逻辑，让 PlanExecutionComponent 统一处理
  // 这样可以避免重复发送消息
})

onUnmounted(() => {
  console.log('[ChatComponent] Unmounting, cleaning up resources')

  // 移除滚动监听器
  removeScrollListener()

  // 清理轮询
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  // 清理计划执行管理器资源
  planExecution.cleanup()
})

// 暴露方法给父组件使用
defineExpose({
  handleSendMessage,
  handlePlanUpdate,
  handlePlanCompleted,
  handleDialogRoundStart,
  addMessage,
})
</script>

<style lang="less" scoped>
.chat-container {
  flex: 1; /* 占据剩余空间 */
  display: flex;
  flex-direction: column;
  min-height: 0; /* 确保可以收缩 */
  overflow: hidden; /* 防止容器溢出 */
}

.messages {
  padding: 24px;
  flex: 1; /* 使用 flex: 1 而不是 height: 100% */
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto; /* 使用 auto 而不是 scroll */
  min-height: 0; /* 确保可以收缩 */
  /* 添加平滑滚动 */
  scroll-behavior: smooth;
  /* 改善滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;

  /* Webkit 滚动条样式 */
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
  /* 1. TaskPilot 思考/处理部分样式 */
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

  /* 2. TaskPilot 最终回复部分样式 - 模拟人类对话单元 */
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

          /* 让文本看起来更像自然对话 */
          p {
            margin: 0 0 12px 0;

            &:last-child {
              margin-bottom: 0;
            }
          }

          /* 增强可读性 */
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

      &.current {
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

          &.current {
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

      /* 子计划步骤样式 - 新增功能 */
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

          &.current {
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

/* 滚动到底部按钮 */
.scroll-to-bottom-btn {
  position: absolute;
  bottom: 120px; /* 在输入框上方 */
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

  /* 添加脉冲动画 */
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
