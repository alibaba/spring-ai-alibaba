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
          <div v-if="message.type === 'user'" class="user-message">
            {{ message.content }}
          </div>
          <div v-else class="assistant-message">
            <div class="thinking" v-if="message.thinking">
              <Icon icon="carbon:thinking" class="thinking-icon" />
              <span>{{ message.thinking }}</span>
            </div>
            <div class="response" v-if="message.content">
              {{ message.content }}
            </div>
            <div class="steps-container" v-if="message.steps && message.steps.length > 0">
              <div 
                v-for="(step, index) in message.steps" 
                :key="index"
                class="ai-section"
                :class="{ 
                  current: index === message.currentStepIndex,
                  completed: index < (message.currentStepIndex || 0),
                  pending: index > (message.currentStepIndex || 0)
                }"
                @click="handleStepClick(message, index)"
              >
                <div class="section-header">
                  <span class="step-icon">
                    {{ index < (message.currentStepIndex || 0) ? '✓' : 
                       index === (message.currentStepIndex || 0) ? '▶' : '○' }}
                  </span>
                  <span class="step-title">{{ step.title || step.description || step || `步骤 ${index + 1}` }}</span>
                </div>
                
                <!-- 显示步骤执行动作信息（基于 chat-handler.js 逻辑） -->
                <div 
                  v-if="message.stepActions && message.stepActions[index]" 
                  class="action-info"
                >
                  <div class="action-description">
                    <span class="action-icon">
                      {{ message.stepActions[index]?.status === 'current' ? '🔄' : '✓' }}
                    </span>
                    {{ message.stepActions[index]?.actionDescription }}
                  </div>
                  <div v-if="message.stepActions[index]?.toolParameters" class="tool-params">
                    <span class="tool-icon">⚙️</span>
                    参数: {{ message.stepActions[index]?.toolParameters }}
                  </div>
                  <div 
                    v-if="message.stepActions[index]?.thinkOutput" 
                    class="think-details"
                  >
                    <div class="think-output">
                      <span class="think-label">思考输出:</span>
                      <span class="think-content">{{ message.stepActions[index]?.thinkOutput }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="progress" v-if="message.progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: message.progress + '%' }"></div>
              </div>
              <span class="progress-text">{{ message.progressText }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="isLoading" class="message assistant">
        <div class="message-content">
          <div class="assistant-message">
            <div class="thinking">
              <div class="thinking-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <span>Analyzing your request...</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <InputArea 
      ref="inputAreaRef"
      :disabled="isLoading"
      :placeholder="isLoading ? '等待任务完成...' : '向 JTaskPilot 发送消息'"
      @send="handleSendMessage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import InputArea from '@/components/input/index.vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import { CommonApiService } from '@/api/common-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { EVENTS } from '@/constants/events'
import { usePlanExecution } from '@/utils/use-plan-execution'

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
  (e: typeof EVENTS.PLAN_UPDATE, planData: any): void
  (e: typeof EVENTS.EXECUTION_STATE_CHANGED, executionData: any): void
  (e: typeof EVENTS.PLAN_COMPLETED, result: any): void
  (e: typeof EVENTS.USER_MESSAGE_SEND_REQUESTED, message: string): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'plan' // 使用计划模式，通过 plan-execution-manager 处理
})
const emit = defineEmits<Emits>()

// 使用计划执行管理器
const planExecution = usePlanExecution()

const messagesRef = ref<HTMLElement>()
const inputAreaRef = ref<InstanceType<typeof InputArea>>()
const isLoading = ref(false)
const messages = ref<Message[]>([])
const currentPlanId = ref<string>()
const currentExecutionId = ref<string>()
const pollingInterval = ref<number>()

const addMessage = (type: 'user' | 'assistant', content: string, options?: Partial<Message>) => {
  const message: Message = {
    id: Date.now().toString(),
    type,
    content,
    timestamp: new Date(),
    ...options,
  }
  messages.value.push(message)
  scrollToBottom()
  return message
}

const updateLastMessage = (updates: Partial<Message>) => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.type === 'assistant') {
    Object.assign(lastMessage, updates)
  }
}

const handlePlanMode = async (query: string) => {
  try {
    isLoading.value = true
    
    // 添加思考状态消息
    const assistantMessage = addMessage('assistant', '', {
      thinking: '正在分析您的需求并生成执行计划...'
    })

    // 生成计划
    const planResponse = await PlanActApiService.generatePlan(query)
    
    if (planResponse.planId) {
      currentPlanId.value = planResponse.planId
      assistantMessage.planId = planResponse.planId
      assistantMessage.thinking = undefined
      
      // 开始监听计划更新事件
      startListeningPlanUpdates(planResponse.planId)
      
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
      progressText: undefined
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
      thinking: '正在处理您的请求...'
    })

    // 直接执行
    const response = await DirectApiService.sendMessage(query)
    
    assistantMessage.thinking = undefined
    assistantMessage.content = response.result || response.message || '执行完成'
    
  } catch (error: any) {
    console.error('Direct mode error:', error)
    updateLastMessage({
      thinking: undefined,
      content: `执行出现错误：${error?.message || '未知错误'}`
    })
  } finally {
    isLoading.value = false
  }
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
            steps: details.steps
          })
          
          emit(EVENTS.PLAN_COMPLETED, details)
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
    currentStepIndex: currentStep
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
    case 'completed': return '已完成'
    case 'current': return '执行中'
    case 'pending': return '待执行'
    default: return '待执行'
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleSendMessage = (message: string) => {
  // 首先添加用户消息到UI
  addMessage('user', message)
  
  // 调用input组件的handleMessageSent方法
  inputAreaRef.value?.handleMessageSent(message)

  // 根据模式处理消息
  if (props.mode === 'plan') {
    // 在计划模式下，触发用户消息发送请求事件
    // 这将被 plan-execution-manager 捕获并处理
    const event = new CustomEvent(EVENTS.USER_MESSAGE_SEND_REQUESTED, {
      detail: { query: message }
    })
    window.dispatchEvent(event)
    emit(EVENTS.USER_MESSAGE_SEND_REQUESTED, message)
  } else {
    // 直接模式仍然直接处理
    handleDirectMode(message)
  }
}

// 处理步骤点击事件
const handleStepClick = (message: Message, stepIndex: number) => {
  if (!message.planId) return
  
  // 触发自定义事件，通知右侧面板显示步骤详情
  const event = new CustomEvent('ui:step:selected', {
    detail: {
      planId: message.planId,
      stepIndex: stepIndex
    }
  })
  window.dispatchEvent(event)
}

// 输入控制方法（类似 chat-input.js 的功能）
const clearInput = () => {
  inputAreaRef.value?.clearInput()
}

const updateInputState = (enabled: boolean, placeholder?: string) => {
  inputAreaRef.value?.updateState(enabled, placeholder)
}

const focusInput = () => {
  inputAreaRef.value?.focus()
}

const startListeningPlanUpdates = (planId: string) => {
  // 监听计划更新事件
  const handlePlanUpdateEvent = (event: any) => {
    const planDetails = event.detail
    if (planDetails && planDetails.planId === planId) {
      handlePlanUpdate(planDetails)
    }
  }
  
  window.addEventListener(EVENTS.PLAN_UPDATE, handlePlanUpdateEvent)
  
  // 存储事件监听器以便清理
  const cleanup = () => {
    window.removeEventListener(EVENTS.PLAN_UPDATE, handlePlanUpdateEvent)
  }
  
  // 在组件卸载时清理
  onUnmounted(cleanup)
}

// 处理计划更新（基于 chat-handler.js 的逻辑）
const handlePlanUpdate = (planDetails: any) => {
  if (!planDetails.steps || !planDetails.steps.length) return
  
  // 找到对应的消息并更新
  const messageIndex = messages.value.findIndex(m => m.planId === planDetails.planId)
  if (messageIndex === -1) return
  
  const message = messages.value[messageIndex]
  
  // 更新消息的步骤信息
  message.steps = planDetails.steps
  message.currentStepIndex = planDetails.currentStepIndex
  
  // 更新进度信息
  const progress = calculateProgress(planDetails)
  message.progress = progress.percentage
  message.progressText = progress.text
  
  // 处理执行序列和步骤动作
  if (planDetails.agentExecutionSequence?.length > 0) {
    updateStepActions(message, planDetails)
  }
  
  // 处理用户输入等待状态
  if (planDetails.userInputWaitState) {
    // TODO: 实现用户输入表单显示逻辑
    console.log('需要用户输入:', planDetails.userInputWaitState)
  }
  
  // 发送事件通知其他组件
  emit(EVENTS.PLAN_UPDATE, planDetails)
}

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
    const stepTitle = planDetails.steps[currentStep].title || 
                     planDetails.steps[currentStep].description || 
                     planDetails.steps[currentStep]
    text += `: ${stepTitle}`
  }
  
  return { percentage, text }
}

// 更新步骤执行动作（基于 chat-handler.js 逻辑）
const updateStepActions = (message: Message, planDetails: any) => {
  if (!message.steps) return
  
  // 初始化存储每个步骤的最后执行动作
  const lastStepActions = new Array(message.steps.length).fill(null)
  
  // 遍历所有执行序列，匹配步骤并更新动作
  if (planDetails.agentExecutionSequence?.length > 0) {
    let index = 0
    planDetails.agentExecutionSequence.forEach((execution: any) => {
      if (execution?.thinkActSteps?.length > 0) {
        const latestThinkAct = execution.thinkActSteps[execution.thinkActSteps.length - 1]
        
        if (latestThinkAct?.actionDescription && latestThinkAct?.toolParameters) {
          // 保存此步骤的最后执行动作
          lastStepActions[index] = {
            actionDescription: latestThinkAct.actionDescription,
            toolParameters: latestThinkAct.toolParameters,
            thinkInput: latestThinkAct.thinkInput || '',
            thinkOutput: latestThinkAct.thinkOutput || '',
            status: index < planDetails.currentStepIndex ? 'completed' : 
                   index === planDetails.currentStepIndex ? 'current' : 'pending'
          }
        } else if (latestThinkAct) {
          // 思考中状态
          lastStepActions[index] = {
            actionDescription: '思考中',
            toolParameters: '等待决策中',
            thinkInput: latestThinkAct.thinkInput || '',
            thinkOutput: latestThinkAct.thinkOutput || '',
            status: index === planDetails.currentStepIndex ? 'current' : 'pending'
          }
        } else {
          lastStepActions[index] = {
            actionDescription: '执行完成',
            toolParameters: '无工具',
            thinkInput: '',
            thinkOutput: '',
            status: 'completed'
          }
        }
      }
      index++
    })
  }
  
  // 将步骤动作信息附加到消息上
  message.stepActions = lastStepActions
}

// 全局事件监听器管理
let globalEventListeners: { event: string; handler: (event: any) => void }[] = []

// 设置全局事件监听器（基于 chat-handler.js 和 plan-execution-manager.js）
const setupGlobalEventListeners = () => {
  // 监听对话轮次开始事件
  const handleDialogRoundStart = (event: any) => {
    const { planId, query } = event.detail || {}
    if (planId && query) {
      // 添加用户消息（如果还没有的话）
      const hasUserMessage = messages.value.some(m => m.type === 'user' && m.content === query)
      if (!hasUserMessage) {
        addMessage('user', query)
      }
      
      // 添加助手消息准备显示步骤
      const assistantMessage = addMessage('assistant', '任务已提交，正在处理中...', {
        planId: planId,
        steps: [],
        currentStepIndex: 0,
        progress: 5,
        progressText: '准备执行...'
      })
    }
  }

  // 监听计划更新事件（来自 plan-execution-manager）
  const handlePlanUpdateFromManager = (event: any) => {
    const planDetails = event.detail
    if (planDetails && planDetails.planId) {
      handlePlanUpdate(planDetails)
    }
  }

  // 监听计划完成事件
  const handlePlanCompletedFromManager = (event: any) => {
    const details = event.detail
    if (details && details.planId) {
      // 找到对应的消息并更新为完成状态
      const messageIndex = messages.value.findIndex(m => m.planId === details.planId)
      if (messageIndex !== -1) {
        const message = messages.value[messageIndex]
        message.progress = 100
        message.progressText = '执行完成！'
        message.content = details.summary || '计划执行完成'
        
        emit(EVENTS.PLAN_COMPLETED, details)
      }
    }
  }

  // 注册事件监听器
  const eventListeners = [
    { event: EVENTS.DIALOG_ROUND_START, handler: handleDialogRoundStart },
    { event: EVENTS.PLAN_UPDATE, handler: handlePlanUpdateFromManager },
    { event: EVENTS.PLAN_COMPLETED, handler: handlePlanCompletedFromManager }
  ]

  eventListeners.forEach(({ event, handler }) => {
    window.addEventListener(event, handler)
    globalEventListeners.push({ event, handler })
  })

  console.log('[Chat] Global event listeners setup complete')
}

// 清理全局事件监听器
const cleanupGlobalEventListeners = () => {
  globalEventListeners.forEach(({ event, handler }) => {
    window.removeEventListener(event, handler)
  })
  globalEventListeners = []
  console.log('[Chat] Global event listeners cleaned up')
}

onMounted(() => {
  // 设置全局事件监听器
  setupGlobalEventListeners()
  
  // Initialize with initial prompt if provided
  if (props.initialPrompt) {
    addMessage('user', props.initialPrompt)
    if (props.mode === 'plan') {
      handlePlanMode(props.initialPrompt)
    } else {
      handleDirectMode(props.initialPrompt)
    }
  }
})

onUnmounted(() => {
  // 清理轮询
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }
  
  // 清理计划执行管理器资源
  planExecution.cleanup()
  
  // 清理全局事件监听器
  cleanupGlobalEventListeners()
})
</script>

<style lang="less" scoped>
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: scroll;
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
  .thinking {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #888888;
    font-size: 14px;
    margin-bottom: 12px;

    .thinking-icon {
      animation: pulse 2s infinite;
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

      .section-header {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        background: rgba(255, 255, 255, 0.02);
        
        .step-number {
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

      .section-content {
        padding: 12px 16px;
        color: #cccccc;
        font-size: 14px;
        line-height: 1.5;
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

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
