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
                message.progress !== undefined ||
                (message.steps && message.steps.length > 0)
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
                <div class="progress" v-if="message.progress !== undefined">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: message.progress + '%' }"></div>
                  </div>
                  <span class="progress-text">{{ message.progressText || '处理中...' }}</span>
                </div>

                <!-- 步骤执行详情 -->
                <div class="steps-container" v-if="message.steps && message.steps.length > 0">
                  <h4 class="steps-title">步骤执行详情</h4>

                  <!-- 遍历所有步骤 -->
                  <div
                    v-for="(step, index) in message.steps"
                    :key="index"
                    class="ai-section"
                    :class="{
                      current: index === message.currentStepIndex,
                      completed: index < (message.currentStepIndex || 0),
                      pending: index > (message.currentStepIndex || 0),
                    }"
                    @click.stop="handleStepClick(message, index)"
                  >
                    <div class="section-header">
                      <span class="step-icon">
                        {{
                          index < (message.currentStepIndex || 0)
                            ? '✓'
                            : index === (message.currentStepIndex || 0)
                              ? '▶'
                              : '○'
                        }}
                      </span>
                      <span class="step-title">
                        {{ step.title || step.description || step || `步骤 ${index + 1}` }}
                      </span>
                      <span v-if="index === message.currentStepIndex" class="step-status current">
                        执行中
                      </span>
                      <span
                        v-else-if="index < (message.currentStepIndex || 0)"
                        class="step-status completed"
                      >
                        已完成
                      </span>
                      <span v-else class="step-status pending"> 待执行 </span>
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
                        <span class="param-label">参数:</span>
                        <pre class="param-content">{{
                          message.stepActions[index]?.toolParameters
                        }}</pre>
                      </div>

                      <div v-if="message.stepActions[index]?.thinkOutput" class="think-details">
                        <div class="think-header">
                          <span class="think-icon">💭</span>
                          <span class="think-label">思考输出:</span>
                        </div>
                        <div class="think-output">
                          <pre class="think-content">{{
                            message.stepActions[index]?.thinkOutput
                          }}</pre>
                        </div>
                      </div>
                    </div>

                    <!-- 用户输入表单 -->
                    <div
                      v-if="message.userInputWaitState && index === message.currentStepIndex"
                      class="user-input-form-container"
                    >
                      <p class="user-input-message">
                        {{ message.userInputWaitState.message || '请输入所需信息:' }}
                      </p>
                      <p v-if="message.userInputWaitState.formDescription" class="form-description">
                        {{ message.userInputWaitState.formDescription }}
                      </p>

                      <form
                        @submit.prevent="handleUserInputSubmit(message, index)"
                        class="user-input-form"
                      >
                        <div
                          v-if="
                            message.userInputWaitState.formInputs &&
                            message.userInputWaitState.formInputs.length > 0
                          "
                          v-for="(input, inputIndex) in message.userInputWaitState.formInputs"
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
                            v-model="message.userInputWaitState.formInputs[inputIndex].value"
                            class="form-input"
                          />
                        </div>

                        <div v-else class="form-group">
                          <label for="form-input-genericInput">输入:</label>
                          <input
                            type="text"
                            id="form-input-genericInput"
                            name="genericInput"
                            v-model="message.genericInput"
                            class="form-input"
                          />
                        </div>

                        <button type="submit" class="submit-user-input-btn">提交</button>
                      </form>
                    </div>
                  </div>
                </div>

                <!-- 只在没有最终内容且正在处理时显示默认处理状态 -->
                <div
                  v-else-if="
                    !message.content &&
                    (message.thinking || (message.progress !== undefined && message.progress < 100))
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
import { PlanActApiService } from '@/api/plan-act-api-service'
import { CommonApiService } from '@/api/common-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { usePlanExecution } from '@/utils/use-plan-execution'
import { useRightPanelStore } from '@/stores/right-panel'

interface Message {
  id: string
  type: 'user' | 'assistant'
  content: string
  thinking?: string
  progress?: number
  progressText?: string
  timestamp: Date
  planId?: string
  executionId?: string
  steps?: any[]
  currentStepIndex?: number
  planCompleted?: boolean
  stepActions?: Array<{
    actionDescription: string
    toolParameters: string
    thinkInput: string
    thinkOutput: string
    status: 'completed' | 'current' | 'pending'
  } | null>
  userInputWaitState?: {
    message?: string
    formDescription?: string
    formInputs?: Array<{
      label: string
      value?: string
    }>
  }
  genericInput?: string
}

interface Props {
  initialPrompt?: string
  mode?: 'plan' | 'direct' // 计划模式或直接聊天模式
}

interface Emits {
  (e: 'plan-update', planData: any): void
  (e: 'execution-state-changed', executionData: any): void
  (e: 'plan-completed', result: any): void
  (e: 'user-message-send-requested', message: string): void
  (e: 'input-clear'): void
  (e: 'input-update-state', enabled: boolean, placeholder?: string): void
  (e: 'input-focus'): void
  (e: 'step-selected', planId: string, stepIndex: number): void
  (e: 'dialog-round-start', planId: string, query: string): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'plan', // 使用计划模式，通过 plan-execution-manager 处理
})
const emit = defineEmits<Emits>()

// 使用计划执行管理器
const planExecution = usePlanExecution()

// 使用right-panel store
const rightPanelStore = useRightPanelStore()

const messagesRef = ref<HTMLElement>()
const isLoading = ref(false)
const messages = ref<Message[]>([])
const currentPlanId = ref<string>()
const currentExecutionId = ref<string>()
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

const handlePlanMode = async (query: string) => {
  try {
    isLoading.value = true

    // 添加思考状态消息
    const assistantMessage = addMessage('assistant', '', {
      thinking: '正在分析您的需求并生成执行计划...',
    })

    // 生成计划
    const planResponse = await PlanActApiService.generatePlan(query)

    if (planResponse.planId) {
      currentPlanId.value = planResponse.planId
      assistantMessage.planId = planResponse.planId
      assistantMessage.thinking = undefined

      // 重要：使用 plan execution manager 来处理执行
      // 这会触发轮询和所有相关的事件处理逻辑
      planExecution.startExecution(query, planResponse.planId)

      assistantMessage.content = '已生成执行计划，正在开始执行...'
      assistantMessage.steps = planResponse.plan?.steps || []
      assistantMessage.currentStepIndex = 0
      assistantMessage.progress = 10
      assistantMessage.progressText = '准备执行计划...'
    } else {
      assistantMessage.thinking = undefined
      assistantMessage.content = '抱歉，计划生成失败，请重试。'
    }
  } catch (error: any) {
    console.error('Plan mode error:', error)
    updateLastMessage({
      thinking: undefined,
      content: `执行出现错误：${error?.message || '未知错误'}`,
      progress: undefined,
      progressText: undefined,
    })
  } finally {
    isLoading.value = false
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

    assistantMessage.thinking = undefined

    // 生成自然的人性化回复
    const finalResponse = generateDirectModeResponse(response, query)
    assistantMessage.content = finalResponse
  } catch (error: any) {
    console.error('Direct mode error:', error)
    updateLastMessage({
      thinking: undefined,
      content: generateErrorResponse(error),
    })
  } finally {
    isLoading.value = false
  }
}

// 生成直接模式的自然回复
const generateDirectModeResponse = (response: any, originalQuery: string): string => {
  const result = response.result || response.message || response.content || ''

  if (!result) {
    return `我理解了您的问题，但暂时没有获得具体的结果。能否请您提供更多详细信息，这样我可以更好地帮助您。`
  }

  // 如果回复已经是自然对话形式，稍作调整使其更完整
  if (result.includes('我') || result.includes('您') || result.includes('可以')) {
    // 确保回复有合适的结尾
    if (!result.includes('?') && !result.includes('。') && !result.includes('！')) {
      return `${result}。还有什么我可以帮您的吗？`
    }
    return result
  }

  // 根据查询类型和结果长度生成不同风格的回复
  const isQuestion =
    originalQuery.includes('?') ||
    originalQuery.includes('？') ||
    originalQuery.includes('什么') ||
    originalQuery.includes('如何') ||
    originalQuery.includes('怎么')

  if (isQuestion) {
    if (result.length > 200) {
      return `关于您的问题，我来详细回答一下：\n\n${result}\n\n希望这个回答对您有帮助。如果还有疑问，请随时告诉我。`
    } else {
      return `${result}。这样回答您的问题是否清楚？如果还需要更多信息，请告诉我。`
    }
  } else {
    // 任务或请求类型
    if (result.length > 150) {
      return `好的，我已经处理了您的请求：\n\n${result}\n\n任务完成！还有其他需要帮助的吗？`
    } else {
      return `完成了！${result}。还有什么我可以帮您处理的吗？`
    }
  }
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

const startExecutionPolling = (planId: string, executionId: string) => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }

  pollingInterval.value = window.setInterval(async () => {
    try {
      // 获取计划详情来检查执行状态
      const details = await CommonApiService.getDetails(planId)

      if (details) {
        updateExecutionProgress(details)

        // 检查是否完成
        if (details.completed || details.status === 'completed') {
          clearInterval(pollingInterval.value!)
          pollingInterval.value = undefined

          updateLastMessage({
            progress: 100,
            progressText: '执行完成！',
            content: details.summary || '计划执行完成',
            steps: details.steps,
          })

          emit('plan-completed', details)
        }
      }
    } catch (error: any) {
      console.error('Polling error:', error)
      // 继续轮询，不中断
    }
  }, 2000) // 每2秒轮询一次
}

const updateExecutionProgress = (details: any) => {
  if (!details.steps || !Array.isArray(details.steps)) return

  const totalSteps = details.steps.length
  const currentStep = details.currentStepIndex || 0
  const progress = Math.min(Math.round((currentStep / totalSteps) * 80) + 20, 95) // 20-95%

  let progressText = `执行步骤 ${currentStep + 1}/${totalSteps}`
  if (details.steps[currentStep]) {
    progressText += `: ${details.steps[currentStep].title || details.steps[currentStep].description || ''}`
  }

  updateLastMessage({
    progress,
    progressText,
    steps: details.steps,
    currentStepIndex: currentStep,
  })
}

const getStepStatus = (stepIndex: number, currentStepIndex?: number) => {
  if (currentStepIndex === undefined) return 'pending'
  if (stepIndex < currentStepIndex) return 'completed'
  if (stepIndex === currentStepIndex) return 'current'
  return 'pending'
}

const getStepStatusText = (stepIndex: number, currentStepIndex?: number) => {
  const status = getStepStatus(stepIndex, currentStepIndex)
  switch (status) {
    case 'completed':
      return '已完成'
    case 'current':
      return '执行中'
    case 'pending':
      return '待执行'
    default:
      return '待执行'
  }
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

// 处理步骤点击事件
const handleStepClick = (message: Message, stepIndex: number) => {
  if (!message.planId) {
    console.warn('[ChatComponent] Cannot handle step click: missing planId')
    return
  }

  console.log('[ChatComponent] Step clicked:', {
    planId: message.planId,
    stepIndex: stepIndex,
    stepTitle: message.steps?.[stepIndex]?.title || message.steps?.[stepIndex],
  })

  // 直接使用right-panel store显示步骤详情，替代emit事件
  rightPanelStore.showStepDetails(message.planId, stepIndex)
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
  if (!message.steps) return

  console.log(
    '[ChatComponent] 开始更新步骤动作, 步骤数:',
    message.steps.length,
    '执行序列:',
    planDetails.agentExecutionSequence?.length || 0
  )

  // 初始化存储每个步骤的最后执行动作
  const lastStepActions = new Array(message.steps.length).fill(null)

  // 遍历所有执行序列，匹配步骤并更新动作
  if (planDetails.agentExecutionSequence?.length > 0) {
    // 检查执行序列与步骤数是否匹配
    const sequenceLength = Math.min(planDetails.agentExecutionSequence.length, message.steps.length)

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
      m => m.planId === planId && m.type === 'assistant'
    )

    // 如果没有现有消息，添加助手消息准备显示步骤
    if (existingAssistantMsg === -1) {
      const assistantMessage = addMessage('assistant', '', {
        planId: planId,
        thinking: '正在分析任务需求...',
        steps: [],
        currentStepIndex: 0,
        progress: 5,
        progressText: '准备执行...',
      })

      console.log('[ChatComponent] Created new assistant message for planId:', planId)
    } else {
      console.log('[ChatComponent] Found existing assistant message for planId:', planId)
    }

    // 滚动到底部确保用户能看到最新进展
    scrollToBottom()
  }
}

// 处理计划更新
const handlePlanUpdate = (planDetails: any) => {
  console.log('[ChatComponent] Processing plan update:', planDetails)
  console.log('[ChatComponent] Plan steps:', planDetails?.steps)
  console.log('[ChatComponent] Plan completed:', planDetails?.completed)

  if (!planDetails || !planDetails.planId) {
    console.warn('[ChatComponent] Plan update missing planId')
    return
  }

  // 直接使用right-panel store处理计划更新，替代emit事件
  rightPanelStore.handlePlanUpdate(planDetails)

  // 找到对应的消息
  const messageIndex = messages.value.findIndex(
    m => m.planId === planDetails.planId && m.type === 'assistant'
  )
  let message

  if (messageIndex !== -1) {
    message = messages.value[messageIndex]
    console.log('[ChatComponent] Found existing assistant message for planId:', planDetails.planId)
  } else {
    console.warn(
      '[ChatComponent] No existing assistant message found for planId:',
      planDetails.planId
    )
    console.log(
      '[ChatComponent] Current messages:',
      messages.value.map(m => ({
        type: m.type,
        planId: m.planId,
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
      // 更新 planId 以确保后续更新能找到它
      message.planId = planDetails.planId
      console.log(
        '[ChatComponent] Using last assistant message and updating planId to:',
        planDetails.planId
      )
    } else {
      console.error('[ChatComponent] No assistant message found at all, this should not happen')
      return
    }
  }

  // 处理简单响应（没有步骤的情况）
  if (!planDetails.steps || planDetails.steps.length === 0) {
    console.log('[ChatComponent] Handling simple response without steps')

    if (planDetails.completed) {
      // 直接设置最终回复，清除所有处理状态
      message.thinking = undefined
      message.progress = undefined
      message.progressText = undefined

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
    emit('plan-update', planDetails)
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

  // 更新消息的步骤信息
  message.steps = formattedSteps
  message.currentStepIndex =
    planDetails.currentStepIndex !== undefined ? planDetails.currentStepIndex : 0

  // 标记计划是否已完成（用于步骤状态显示）
  message.planCompleted = planDetails.completed || planDetails.status === 'completed'

  // 更新进度信息
  const progress = calculateProgress(planDetails)
  message.progress = progress.percentage
  message.progressText = progress.text

  // 处理执行序列和步骤动作 - 参考chat-handler.js的逻辑
  if (planDetails.agentExecutionSequence?.length > 0) {
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
      if (currentExecution?.thinkActSteps?.length > 0) {
        const latestThinkAct =
          currentExecution.thinkActSteps[currentExecution.thinkActSteps.length - 1]
        if (latestThinkAct?.thinkOutput) {
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
    const currentStep = message.steps?.[message.currentStepIndex || 0]
    const stepTitle = currentStep?.title || currentStep?.description || '处理中'
    message.thinking = `正在执行: ${stepTitle}`
  }

  // 处理用户输入等待状态
  if (planDetails.userInputWaitState) {
    console.log('[ChatComponent] 需要用户输入:', planDetails.userInputWaitState)

    // 将用户输入等待状态附加到消息上
    message.userInputWaitState = {
      message: planDetails.userInputWaitState.message,
      formDescription: planDetails.userInputWaitState.formDescription,
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
    if (message.userInputWaitState) {
      message.userInputWaitState = undefined
    }
  }

  // 检查计划是否已完成
  if (planDetails.completed || planDetails.status === 'completed') {
    console.log('[ChatComponent] Plan is completed, updating final response')
    // 清除所有处理状态
    message.progress = undefined
    message.progressText = undefined
    message.thinking = undefined

    // 重要：设置 currentStepIndex 为总步骤数，这样所有步骤都会显示为"已完成"
    if (message.steps && message.steps.length > 0) {
      message.currentStepIndex = message.steps.length
      console.log(
        '[ChatComponent] Set currentStepIndex to',
        message.steps.length,
        'to mark all steps as completed'
      )
    }

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

  // 通知父组件
  emit('plan-update', planDetails)
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
    const messageIndex = messages.value.findIndex(m => m.planId === details.planId)
    if (messageIndex !== -1) {
      const message = messages.value[messageIndex]
      // 清除所有处理状态，显示为完成
      message.progress = undefined
      message.progressText = undefined
      message.thinking = undefined

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

      emit('plan-completed', details)
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
const handleUserInputSubmit = async (message: Message, stepIndex: number) => {
  if (!message.planId || !message.userInputWaitState) {
    console.error('[ChatComponent] 缺少planId或userInputWaitState')
    return
  }

  try {
    // 收集表单数据
    let inputData: any = {}

    if (message.userInputWaitState.formInputs && message.userInputWaitState.formInputs.length > 0) {
      // 多个字段的情况
      message.userInputWaitState.formInputs.forEach(input => {
        inputData[input.label] = input.value || ''
      })
    } else {
      // 单个通用输入的情况
      inputData.genericInput = message.genericInput || ''
    }

    console.log('[ChatComponent] 提交用户输入:', inputData)

    // 通过API提交用户输入
    const response = await CommonApiService.submitFormInput(message.planId, inputData)

    // 清除用户输入等待状态
    message.userInputWaitState = undefined
    message.genericInput = undefined

    // 继续轮询以获取计划更新（提交后应该会自动继续执行）
    planExecution.startPolling()

    console.log('[ChatComponent] 用户输入提交成功:', response)
  } catch (error: any) {
    console.error('[ChatComponent] 用户输入提交失败:', error)
    // 可以在UI中显示错误消息
    alert(`提交失败: ${error?.message || '未知错误'}`)
  }
}

// 定义事件处理器，以便可以正确地添加和移除事件监听器
const eventHandlers = {
  onPlanUpdate: (event: CustomEvent) => {
    console.log('[ChatComponent] Received PLAN_UPDATE event:', event.detail)

    // 检查是否有有效的计划详情
    if (event.detail && event.detail.planId) {
      // 添加调试信息
      if (event.detail.agentExecutionSequence) {
        console.log(
          `[ChatComponent] 计划包含 ${event.detail.agentExecutionSequence.length} 个执行序列`
        )

        // 输出每个执行序列的基本信息
        event.detail.agentExecutionSequence.forEach((execution: any, index: number) => {
          if (execution?.thinkActSteps?.length) {
            console.log(
              `[ChatComponent] 序列 ${index}: ${execution.thinkActSteps.length} 个思考步骤`
            )
          }
        })
      }

      // 处理计划更新
      handlePlanUpdate(event.detail)
    } else {
      console.warn('[ChatComponent] 收到无效的计划更新事件:', event.detail)
    }
  },

  onPlanCompleted: (event: CustomEvent) => {
    console.log('[ChatComponent] Received PLAN_COMPLETED event:', event.detail)
    handlePlanCompleted(event.detail)
  },

  onDialogRoundStart: (event: CustomEvent) => {
    console.log('[ChatComponent] Received DIALOG_ROUND_START event:', event.detail)
    if (event.detail && event.detail.planId && event.detail.query) {
      handleDialogRoundStart(event.detail.planId, event.detail.query)
    }
  },

  onMessageUpdate: (event: CustomEvent) => {
    console.log('[ChatComponent] Received MESSAGE_UPDATE event:', event.detail)
    const data = event.detail
    if (data && data.content) {
      // 查找或创建对应planId的消息
      let message
      if (data.planId) {
        const index = messages.value.findIndex(m => m.planId === data.planId)
        if (index >= 0) {
          message = messages.value[index]
        } else {
          // 如果找不到对应的消息，创建一个新的
          message = addMessage('assistant', '', { planId: data.planId })
        }

        // 根据消息类型更新不同的内容
        if (data.type === 'status') {
          message.thinking = data.content
        } else if (data.type === 'step') {
          message.progressText = data.content
        } else if (data.type === 'completion') {
          message.content = data.content
          message.thinking = undefined
          message.progress = 100
        } else if (data.type === 'error') {
          message.content = data.content
          message.thinking = undefined
        }
      }
    }
  },

  onChatInputUpdateState: (event: CustomEvent) => {
    console.log('[ChatComponent] Received CHAT_INPUT_UPDATE_STATE event:', event.detail)
    if (event.detail) {
      emit('input-update-state', event.detail?.enabled, event.detail.placeholder)
    }
  },

  onChatInputClear: () => {
    console.log('[ChatComponent] Received CHAT_INPUT_CLEAR event')
    emit('input-clear')
  },
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
