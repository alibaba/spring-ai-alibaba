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
      <Sidebar @planExecutionRequested="handlePlanExecutionRequested" />
      <!-- Left Panel - Chat -->
      <div class="left-panel" :style="{ width: leftPanelWidth + '%' }">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>{{ $t('conversation') }}</h2>
          <div class="header-actions">
            <LanguageSwitcher />
            <button class="config-button" @click="handleConfig" :title="$t('direct.configuration')">
              <Icon icon="carbon:settings-adjust" width="20" />
            </button>
          </div>
        </div>

        <!-- Chat Container -->
        <div class="chat-content">
          <ChatContainer
            ref="chatRef"
            :initial-prompt="prompt || ''"
            @user-message-send-requested="handleMessageSent"
            @input-clear="handleInputClear"
            @input-update-state="handleInputUpdateState"
            @input-focus="handleInputFocus"
            @step-selected="handleStepSelected"
            @sub-plan-step-selected="handleSubPlanStepSelected"
          />
        </div>

        <!-- Input Area -->
        <InputArea
          ref="inputRef"
          :disabled="isLoading"
          :placeholder="isLoading ? '等待任务完成...' : t('input.placeholder')"
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
      <RightPanel ref="rightPanelRef" :style="{ width: 100 - leftPanelWidth + '%' }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/index.vue'
import RightPanel from '@/components/right-panel/index.vue'
import ChatContainer from '@/components/chat/index.vue'
import InputArea from '@/components/input/index.vue'
import LanguageSwitcher from '@/components/language-switcher/index.vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import { useTaskStore } from '@/stores/task'
import { useSidebarStore } from '@/stores/sidebar'
import { planExecutionManager } from '@/utils/plan-execution-manager'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()
const sidebarStore = useSidebarStore()
const { t } = useI18n()

const prompt = ref<string>('')
const rightPanelRef = ref()
const chatRef = ref()
const inputRef = ref()
const isExecutingPlan = ref(false)
const isLoading = ref(false)
const currentRootPlanId = ref<string | null>(null)

// 面板宽度相关
const leftPanelWidth = ref(50) // 左面板宽度百分比
const isResizing = ref(false)
const startX = ref(0)
const startLeftWidth = ref(0)

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
      
      // Call chat component's handleDialogRoundStart method
      if (chatRef.value && typeof chatRef.value.handleDialogRoundStart === 'function') {
        const planData = planExecutionManager.getCachedPlanRecord(rootPlanId)
        const query = planData?.title ?? '执行计划'
        console.log('[Direct] Calling chatRef.handleDialogRoundStart with planId and query:', rootPlanId, query)
        chatRef.value.handleDialogRoundStart(rootPlanId, query)
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
    }
  })
  
  console.log('[Direct] Event callbacks registered to planExecutionManager')

  // 初始化侧边栏数据
  sidebarStore.loadPlanTemplateList()

  // 检查 store 中是否有任务
  if (taskStore.hasUnprocessedTask() && taskStore.currentTask) {
    prompt.value = taskStore.currentTask.prompt
    console.log('[Direct] Setting prompt from store:', prompt.value)
    // 标记任务为已处理，防止重复响应
    taskStore.markTaskAsProcessed()
    console.log('[Direct] Received task from store:', prompt.value)
  } else {
    // 降级到 URL 参数（向后兼容）
    prompt.value = (route.query.prompt as string) || ''
    console.log('[Direct] Received task from URL:', prompt.value)
    console.log('[Direct] No unprocessed task in store')
  }

  // 从 localStorage 恢复面板宽度
  const savedWidth = localStorage.getItem('directPanelWidth')
  if (savedWidth) {
    leftPanelWidth.value = parseFloat(savedWidth)
  }

  console.log('[Direct] Final prompt value:', prompt.value)
})

// 监听 store 中的任务变化（仅处理未处理的任务）
watch(
  () => taskStore.currentTask,
  newTask => {
    console.log('[Direct] Watch taskStore.currentTask triggered, newTask:', newTask)
    if (newTask && !newTask.processed) {
      prompt.value = newTask.prompt
      taskStore.markTaskAsProcessed()
      console.log('[Direct] Received new task from store:', prompt.value)
    } else {
      console.log('[Direct] Task is null or already processed, ignoring')
    }
  },
  { immediate: false }
)

// 监听 prompt 值的变化，仅用于日志记录
watch(
  () => prompt.value,
  (newPrompt, oldPrompt) => {
    console.log('[Direct] prompt value changed from:', oldPrompt, 'to:', newPrompt)
    // 不再手动调用 sendMessage，让 PlanExecutionComponent 通过 initialPrompt prop 自己处理
  },
  { immediate: false }
)

onUnmounted(() => {
  console.log('[Direct] onUnmounted called, cleaning up resources')
  
  // Clear current root plan ID
  currentRootPlanId.value = null
  
  // Clean up plan execution manager resources
  planExecutionManager.cleanup()
  
  // 移除事件监听器
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// 面板大小调整相关方法
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

  // 限制面板宽度在 20% 到 80% 之间
  newWidth = Math.max(20, Math.min(80, newWidth))

  leftPanelWidth.value = newWidth
}

const handleMouseUp = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''

  // 保存到 localStorage
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

const handleMessageSent = (message: string) => {
  console.log('[DirectView] Message sent from chat:', message)
  
  // Use planExecutionManager to handle user message send request
  console.log('[DirectView] Delegating chat message to planExecutionManager:', message)
  planExecutionManager.handleUserMessageSendRequested(message)
}

// New event handler function
const handleSendMessage = (message: string) => {
  console.log('[DirectView] Send message from input:', message)
  
  // Use planExecutionManager to handle user message send request
  console.log('[DirectView] Delegating message to planExecutionManager:', message)
  planExecutionManager.handleUserMessageSendRequested(message)
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

const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[DirectView] Step selected:', planId, stepIndex)
  
  // Forward step selection to right panel
  if (rightPanelRef.value && typeof rightPanelRef.value.handleStepSelected === 'function') {
    console.log('[DirectView] Forwarding step selection to right panel:', planId, stepIndex)
    rightPanelRef.value.handleStepSelected(planId, stepIndex)
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
  // 切换侧边栏显示状态
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
}) => {
  console.log('[DirectView] Plan execution requested:', payload)

  // 防止重复执行
  if (isExecutingPlan.value) {
    console.log('[DirectView] Plan execution already in progress, ignoring request')
    return
  }

  isExecutingPlan.value = true

  try {
    // 获取计划模板ID
    const planTemplateId = payload.planData.planTemplateId || payload.planData.planId

    if (!planTemplateId) {
      throw new Error('没有找到计划模板ID')
    }

    console.log(
      '[Direct] Executing plan with templateId:',
      planTemplateId,
      'params:',
      payload.params
    )

    // Call real API to execute plan
    console.log('[Direct] About to call PlanActApiService.executePlan')
    let response
    if (payload.params?.trim()) {
      console.log('[Direct] Calling executePlan with params:', payload.params.trim())
      response = await PlanActApiService.executePlan(planTemplateId, payload.params.trim())
    } else {
      console.log('[Direct] Calling executePlan without params')
      response = await PlanActApiService.executePlan(planTemplateId)
    }

    console.log('[Direct] Plan execution API response:', response)

    // 使用返回的 planId，启动计划执行流程，让管理器负责所有消息处理
    if (response.planId) {
      console.log('[Direct] Got planId from response:', response.planId, 'starting plan execution')

      // Set current root plan ID for the new plan execution
      currentRootPlanId.value = response.planId
      console.log('[Direct] Set currentRootPlanId to:', response.planId)

      // Use planExecutionManager to handle plan execution
      console.log('[Direct] Delegating plan execution to planExecutionManager')
      planExecutionManager.handlePlanExecutionRequested(response.planId, payload.title)
    } else {
      console.error('[Direct] No planId in response:', response)
      throw new Error('执行计划失败：未返回有效的计划ID')
    }
  } catch (error: any) {
    console.error('[Direct] Plan execution failed:', error)
    console.error('[Direct] Error details:', { message: error.message, stack: error.stack })

    // Clear current root plan ID on error
    currentRootPlanId.value = null
    
    // 获取chat组件的引用来显示错误
    if (chatRef.value && typeof chatRef.value.addMessage === 'function') {
      console.log('[Direct] Adding error messages to chat')
      // 先添加用户消息
      chatRef.value.addMessage('user', payload.title)
      // 再添加错误消息
      chatRef.value.addMessage('assistant', `执行计划失败: ${error.message || '未知错误'}`, {
        thinking: undefined,
      })
    } else {
      console.error('[Direct] Chat ref not available, showing alert')
      alert(`执行计划失败: ${error.message || '未知错误'}`)
    }
  } finally {
    console.log('[Direct] Plan execution finished, resetting isExecutingPlan flag')
    isExecutingPlan.value = false
  }
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
  border-right: none; /* 移除原来的边框，由分隔条提供 */
  display: flex;
  flex-direction: column;
  height: 100vh; /* 使用固定高度 */
  overflow: hidden; /* 防止面板本身溢出 */
  transition: width 0.1s ease; /* 平滑过渡 */
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

/* 调整右面板样式 */
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
  flex-shrink: 0; /* 确保头部不会被压缩 */
  position: sticky; /* 固定在顶部 */
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
  flex: 1; /* 占据剩余空间 */
  display: flex;
  flex-direction: column;
  min-height: 0; /* 允许收缩 */
  overflow: hidden; /* 防止溢出 */
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

.loading-prompt {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888888;
  font-size: 16px;
  padding: 50px;
}
</style>
