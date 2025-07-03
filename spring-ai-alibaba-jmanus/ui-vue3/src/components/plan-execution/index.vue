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
      @input-clear="handleInputClear"
      @input-update-state="handleInputUpdateState"
      @input-focus="handleInputFocus"
      @step-selected="handleStepSelected"
      @sub-plan-step-selected="handleSubPlanStepSelected"
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
      @plan-mode-clicked="handlePlanModeClicked"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, defineProps, defineEmits, watch } from 'vue'
import ChatContainer from '@/components/chat/index.vue'
import InputArea from '@/components/input/index.vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import { useSidebarStore } from '@/stores/sidebar'
import { useRightPanelStore } from '@/stores/right-panel'

// 使用pinia stores
const sidebarStore = useSidebarStore()
const rightPanelStore = useRightPanelStore()

// 定义 props
interface Props {
  initialPrompt?: string
  mode?: 'direct' | 'plan'
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialPrompt: '',
  mode: 'direct',
  placeholder: '向 JTaskPilot 发送消息',
})

// 定义 emits
interface Emits {
  (e: 'message-sent', message: string): void
}

const emit = defineEmits<Emits>()

// 组件状态
const isLoading = ref(false)
const chatRef = ref()
const inputRef = ref()
const hasProcessedInitialPrompt = ref(false) // 标记是否已经处理过初始 prompt

onMounted(() => {
  console.log('[PlanExecutionComponent] Initialized')
  console.log('[PlanExecutionComponent] props.initialPrompt:', props.initialPrompt)

  // 设置 plan execution manager 的事件回调
  planExecutionManager.setEventCallbacks({
    onPlanUpdate: handlePlanManagerUpdate,
    onPlanCompleted: handlePlanManagerCompleted,
    onDialogRoundStart: handlePlanManagerDialogStart,
    onMessageUpdate: handlePlanManagerMessageUpdate,
    onChatInputUpdateState: handlePlanManagerInputUpdate,
    onChatInputClear: handlePlanManagerInputClear,
  })

  // 如果有初始 prompt，自动发送（只发送一次）
  if (props.initialPrompt && !hasProcessedInitialPrompt.value) {
    console.log('[PlanExecutionComponent] Auto-sending initial prompt:', props.initialPrompt)
    hasProcessedInitialPrompt.value = true
    handleUserMessageSendRequested(props.initialPrompt)
  } else {
    console.log('[PlanExecutionComponent] No initial prompt to send or already processed')
  }
})

// 监听 initialPrompt 的变化
watch(
  () => props.initialPrompt,
  (newPrompt: string, oldPrompt: string) => {
    console.log('[PlanExecutionComponent] initialPrompt changed from:', oldPrompt, 'to:', newPrompt)
    // 只有在新的 prompt 不为空，且不同于旧的 prompt，且还没有处理过初始 prompt 时才发送
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
 * Handle plan update event from plan execution manager (now with rootPlanId-based approach)
 */
const handlePlanManagerUpdate = (rootPlanId: string) => {
  console.log('[PlanExecutionComponent] Received plan update event - rootPlanId:', rootPlanId)

  // Get plan data from cache
  const planData = planExecutionManager.getCachedPlanRecord(rootPlanId)
  
  if (!planData) {
    console.warn('[PlanExecutionComponent] No cached plan data found for rootPlanId:', rootPlanId)
    return
  }

  console.log('[PlanExecutionComponent] Retrieved plan data from cache:', planData)

  // Pass plan update to chat container
  if (chatRef.value && typeof chatRef.value.handlePlanUpdate === 'function') {
    console.log('[PlanExecutionComponent] Calling chatRef.handlePlanUpdate with rootPlanId')
    chatRef.value.handlePlanUpdate(rootPlanId)
  } else {
    console.warn(
      '[PlanExecutionComponent] chatRef.value.handlePlanUpdate is not available:',
      chatRef.value
    )
  }

  // Update loading state
  isLoading.value = !planData.completed

  // Use right-panel store to handle plan update with cached data
  rightPanelStore.handlePlanUpdate(planData)
}

/**
 * Handle plan completion event from plan execution manager (now with rootPlanId-based approach)
 */
const handlePlanManagerCompleted = (rootPlanId: string) => {
  console.log('[PlanExecutionComponent] Received plan completed event - rootPlanId:', rootPlanId)

  // Get plan data from cache
  const result = planExecutionManager.getCachedPlanRecord(rootPlanId)
  
  if (!result) {
    console.warn('[PlanExecutionComponent] No cached plan data found for completed rootPlanId:', rootPlanId)
    return
  }

  console.log('[PlanExecutionComponent] Retrieved completed plan data from cache:', result)

  // Pass plan completion to chat container
  if (chatRef.value && typeof chatRef.value.handlePlanCompleted === 'function') {
    chatRef.value.handlePlanCompleted(result)
  }

  // Update loading state
  isLoading.value = false
}

/**
 * 处理来自 plan execution manager 的对话轮次开始事件
 */
const handlePlanManagerDialogStart = (rootPlanId: string) => {
  console.log('[PlanExecutionComponent] Received dialog round start from manager - rootPlanId:', rootPlanId)

  // 将对话开始传递给 chat container
  if (chatRef.value && typeof chatRef.value.handleDialogRoundStart === 'function') {
    // 需要获取planId和query，这里可能需要从缓存中获取或使用默认值
    const planData = planExecutionManager.getCachedPlanRecord(rootPlanId)
    const planId = planData?.currentPlanId || rootPlanId
    const query = planData?.userRequest || '执行计划'
    
    chatRef.value.handleDialogRoundStart(planId, query)
  }

  // 更新加载状态
  isLoading.value = true

  // 记录对话轮次状态
  const planData = planExecutionManager.getCachedPlanRecord(rootPlanId)
  console.log('[PlanExecutionComponent] Dialog round started with plan data:', planData)
}

/**
 * 处理来自 plan execution manager 的消息更新事件
 */
const handlePlanManagerMessageUpdate = (rootPlanId: string) => {
  console.log('[PlanExecutionComponent] Received message update from manager - rootPlanId:', rootPlanId)

  // 从缓存获取消息数据
  const messageData = planExecutionManager.getCachedMessage(rootPlanId)
  
  if (!messageData) {
    console.warn('[PlanExecutionComponent] No cached message data found for rootPlanId:', rootPlanId)
    return
  }

  // 将消息更新传递给 chat container
  if (chatRef.value && typeof chatRef.value.handleMessageUpdate === 'function') {
    chatRef.value.handleMessageUpdate(messageData)
  }
}

/**
 * 处理来自 plan execution manager 的输入状态更新事件
 */
const handlePlanManagerInputUpdate = (rootPlanId: string) => {
  console.log('[PlanExecutionComponent] Received input update from manager - rootPlanId:', rootPlanId)

  // 从缓存获取UI状态数据
  const inputData = planExecutionManager.getCachedUIState(rootPlanId)
  
  if (!inputData) {
    console.warn('[PlanExecutionComponent] No cached UI state data found for rootPlanId:', rootPlanId)
    return
  }

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
  console.log('[PlanExecutionComponent] handleUserMessageSendRequested called with query:', query)
  console.log('[PlanExecutionComponent] Current isLoading state:', isLoading.value)

  // 委托给 plan execution manager
  await planExecutionManager.handleUserMessageSendRequested(query)
  console.log(
    '[PlanExecutionComponent] planExecutionManager.handleUserMessageSendRequested completed'
  )
}

/**
 * 清理资源
 */
const cleanup = (): void => {
  planExecutionManager.cleanup()
}

// Input Area 事件处理
const handleSendMessage = (message: string) => {
  console.log('[PlanExecutionComponent] Send message from input:', message)
  // 不直接调用 handleUserMessageSendRequested，而是通过 ChatContainer 来处理
  // 这样可以避免重复调用
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

// Chat Container 事件处理
const handleMessageSent = (message: string) => {
  console.log('[PlanExecutionComponent] Message sent from chat container:', message)
  emit('message-sent', message)
  // 处理来自ChatContainer的用户消息发送请求
  handleUserMessageSendRequested(message)
}

const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[PlanExecutionComponent] Step selected:', planId, stepIndex)

  // 立即触发进度刷新
  if (planExecutionManager.getActivePlanId() === planId) {
    console.log('[PlanExecutionComponent] Triggering immediate progress refresh for selected step')
    planExecutionManager.pollPlanStatusImmediately().catch(error => {
      console.warn('[PlanExecutionComponent] Failed to refresh progress immediately:', error)
    })
  }
}

const handleSubPlanStepSelected = (parentPlanId: string, subPlanId: string, stepIndex: number, subStepIndex: number) => {
  console.log('[PlanExecutionComponent] Sub plan step selected:', {
    parentPlanId,
    subPlanId,
    stepIndex,
    subStepIndex
  })

  // 可以在这里添加子计划步骤选择的处理逻辑
  if (planExecutionManager.getActivePlanId() === parentPlanId) {
    planExecutionManager.pollPlanStatusImmediately().catch(error => {
      console.warn('[PlanExecutionComponent] Failed to refresh progress immediately:', error)
    })
  }
}

// 暴露给父组件的方法
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
  flex: 1; /* 占据剩余空间 */
  height: 100%;
  min-height: 0; /* 确保可以收缩 */
  overflow: hidden; /* 防止溢出 */
}
</style>
