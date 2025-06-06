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
      :placeholder="isLoading ? '等待任务完成...' : placeholder"
      @send="handleSendMessage"
      @clear="handleInputClear"
      @focus="handleInputFocus"
      @update-state="handleInputUpdateState"
      @message-sent="handleInputMessageSent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, defineProps, defineEmits } from 'vue'
import ChatContainer from '@/components/chat/index.vue'
import InputArea from '@/components/input/index.vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'

// 定义 props
interface Props {
  initialPrompt?: string
  mode?: 'direct' | 'plan'
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialPrompt: '',
  mode: 'direct',
  placeholder: '向 JTaskPilot 发送消息'
})

// 定义 emits
interface Emits {
  (e: 'plan-update', planData: any): void
  (e: 'plan-completed', result: any): void
  (e: 'dialog-round-start', planId: string, query: string): void
  (e: 'step-selected', planId: string, stepIndex: number): void
  (e: 'message-sent', message: string): void
}

const emit = defineEmits<Emits>()

// 组件状态
const isLoading = ref(false)
const chatRef = ref()
const inputRef = ref()

onMounted(() => {
  console.log('[PlanExecutionComponent] Initialized')
  
  // 设置 plan execution manager 的事件回调
  planExecutionManager.setEventCallbacks({
    onPlanUpdate: handlePlanManagerUpdate,
    onPlanCompleted: handlePlanManagerCompleted,
    onDialogRoundStart: handlePlanManagerDialogStart,
    onMessageUpdate: handlePlanManagerMessageUpdate,
    onChatInputUpdateState: handlePlanManagerInputUpdate,
    onChatInputClear: handlePlanManagerInputClear
  })

  // 如果有初始 prompt，自动发送
  if (props.initialPrompt) {
    handleUserMessageSendRequested(props.initialPrompt)
  }
})

onUnmounted(() => {
  cleanup()
})

/**
 * 处理来自 plan execution manager 的计划更新事件
 */
const handlePlanManagerUpdate = (planData: any) => {
  console.log('[PlanExecutionComponent] Received plan update from manager:', planData)
  
  // 将计划更新传递给 chat container
  if (chatRef.value && typeof chatRef.value.handlePlanUpdate === 'function') {
    console.log('[PlanExecutionComponent] Calling chatRef.handlePlanUpdate with:', planData)
    chatRef.value.handlePlanUpdate(planData)
  } else {
    console.warn('[PlanExecutionComponent] chatRef.value.handlePlanUpdate is not available:', chatRef.value)
  }
  
  // 更新加载状态
  isLoading.value = !planData.completed
  
  // 向父组件发射事件
  emit('plan-update', planData)
}

/**
 * 处理来自 plan execution manager 的计划完成事件
 */
const handlePlanManagerCompleted = (result: any) => {
  console.log('[PlanExecutionComponent] Received plan completed from manager:', result)
  
  // 将计划完成传递给 chat container
  if (chatRef.value && typeof chatRef.value.handlePlanCompleted === 'function') {
    chatRef.value.handlePlanCompleted(result)
  }
  
  // 更新加载状态
  isLoading.value = false
  
  // 向父组件发射事件
  emit('plan-completed', result)
}

/**
 * 处理来自 plan execution manager 的对话轮次开始事件
 */
const handlePlanManagerDialogStart = (dialogData: any) => {
  console.log('[PlanExecutionComponent] Received dialog round start from manager:', dialogData)
  
  // 将对话开始传递给 chat container
  if (chatRef.value && typeof chatRef.value.handleDialogRoundStart === 'function') {
    chatRef.value.handleDialogRoundStart(dialogData.planId, dialogData.query)
  }
  
  // 更新加载状态
  isLoading.value = true
  
  // 向父组件发射事件
  emit('dialog-round-start', dialogData.planId, dialogData.query)
}

/**
 * 处理来自 plan execution manager 的消息更新事件
 */
const handlePlanManagerMessageUpdate = (messageData: any) => {
  console.log('[PlanExecutionComponent] Received message update from manager:', messageData)
  
  // 将消息更新传递给 chat container
  if (chatRef.value && typeof chatRef.value.handleMessageUpdate === 'function') {
    chatRef.value.handleMessageUpdate(messageData)
  }
}

/**
 * 处理来自 plan execution manager 的输入状态更新事件
 */
const handlePlanManagerInputUpdate = (inputData: any) => {
  console.log('[PlanExecutionComponent] Received input update from manager:', inputData)
  
  // 更新输入框状态
  if (inputData.enabled !== undefined) {
    isLoading.value = !inputData.enabled
  }
}

/**
 * 处理来自 plan execution manager 的输入清空事件
 */
const handlePlanManagerInputClear = () => {
  console.log('[PlanExecutionComponent] Received input clear from manager')
  
  // 清空输入框
  if (inputRef.value && typeof inputRef.value.clear === 'function') {
    inputRef.value.clear()
  }
}

/**
 * 处理用户消息发送请求
 */
const handleUserMessageSendRequested = async (query: string): Promise<void> => {
  console.log('[PlanExecutionComponent] User message send requested:', query)
  
  // 委托给 plan execution manager
  await planExecutionManager.handleUserMessageSendRequested(query)
}

/**
 * 清理资源
 */
const cleanup = (): void => {
  planExecutionManager.cleanup()
}

// Input Area 事件处理
const handleSendMessage = (message: string) => {
  console.log('[PlanExecutionComponent] Send message:', message)
  handleUserMessageSendRequested(message)
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

const handleInputMessageSent = (message: string) => {
  console.log('[PlanExecutionComponent] Input message sent:', message)
  handleUserMessageSendRequested(message)
}

// Chat Container 事件处理
const handleMessageSent = (message: string) => {
  console.log('[PlanExecutionComponent] Message sent from chat container:', message)
  emit('message-sent', message)
}

const handlePlanUpdate = (planData: any) => {
  console.log('[PlanExecutionComponent] Plan updated:', planData)
  emit('plan-update', planData)
}

const handlePlanCompleted = (result: any) => {
  console.log('[PlanExecutionComponent] Plan completed:', result)
  emit('plan-completed', result)
}

const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[PlanExecutionComponent] Step selected:', planId, stepIndex)
  emit('step-selected', planId, stepIndex)
}

const handleDialogRoundStart = (planId: string, query: string) => {
  console.log('[PlanExecutionComponent] Dialog round started:', planId, query)
  emit('dialog-round-start', planId, query)
}

// 暴露给父组件的方法
defineExpose({
  getActivePlanId: () => planExecutionManager.getActivePlanId(),
  getState: () => planExecutionManager.getState(),
  cleanup,
  sendMessage: handleUserMessageSendRequested,
  getChatRef: () => chatRef.value,
  getInputRef: () => inputRef.value
})
</script>

<style lang="less" scoped>
.plan-execution-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}
</style>
