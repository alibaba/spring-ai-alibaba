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
                :class="{ current: index === message.currentStepIndex }"
                @click="handleStepClick(message, index)"
              >
                <div class="section-header">
                  <span class="step-number">{{ index + 1 }}</span>
                  <span class="step-title">{{ step.title || step.description || `步骤 ${index + 1}` }}</span>
                  <span class="step-status" :class="getStepStatus(index, message.currentStepIndex)">
                    {{ getStepStatusText(index, message.currentStepIndex) }}
                  </span>
                </div>
                <div v-if="step.detail" class="section-content">
                  {{ step.detail }}
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
  mode: 'plan'
})
const emit = defineEmits<Emits>()

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
      assistantMessage.content = '已生成执行计划，正在开始执行...'
      assistantMessage.progress = 10
      assistantMessage.progressText = '准备执行计划...'
      
      emit(EVENTS.PLAN_UPDATE, planResponse)
      
      // 执行计划
      const executionResponse = await PlanActApiService.executePlan(planResponse.planId)
      
      if (executionResponse.executionId) {
        currentExecutionId.value = executionResponse.executionId
        assistantMessage.executionId = executionResponse.executionId
        assistantMessage.progress = 20
        assistantMessage.progressText = '开始执行步骤...'
        
        emit(EVENTS.EXECUTION_STATE_CHANGED, executionResponse)
        
        // 开始轮询执行状态
        startExecutionPolling(planResponse.planId, executionResponse.executionId)
      }
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
  addMessage('user', message)
  emit(EVENTS.USER_MESSAGE_SEND_REQUESTED, message)

  // 调用input组件的handleMessageSent方法
  inputAreaRef.value?.handleMessageSent(message)

  // 根据模式处理消息
  if (props.mode === 'plan') {
    handlePlanMode(message)
  } else {
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

onMounted(() => {
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
