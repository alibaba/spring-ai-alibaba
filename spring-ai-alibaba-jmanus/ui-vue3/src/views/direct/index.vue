<!-- /*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the const messagesRef = const handlePlanExecutionRequested = async (payload: { title: string; planData: any; params?: string }) => {
  console.log('[Direct] handlePlanExecutionRequested called with payload:', payload)
  console.log('[Direct] Current isExecutingPlan state:', isExecutingPlan.value)
  
  // 防止重复执行
  if (isExecutingPlan.value) {
    console.log('[Direct] Plan execution already in progress, ignoring request')
    return
  }
  
  console.log('[Direct] Starting plan execution process')
  isExecutingPlan.value = trueement>()
const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')
const isLoading = ref(false)nse");
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
  <div class="direct-page">
    <Sidebar 
      ref="sidebarRef" 
      @planExecutionRequested="handlePlanExecutionRequested"
    />
    <div class="direct-chat">
      <!-- Left Panel - Chat -->
      <div class="left-panel">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>Direct Chat</h2>
          <button class="config-button" @click="handleConfig" title="配置">
            <Icon icon="carbon:settings-adjust" width="20" />
          </button>
        </div>

        <PlanExecutionComponent 
          ref="planExecutionRef"
          :initial-prompt="prompt" 
          mode="direct"
          placeholder="向 JTaskPilot 发送消息"
          @plan-update="handlePlanUpdate"
          @plan-completed="handlePlanCompleted"
          @dialog-round-start="handleDialogRoundStart"
          @step-selected="handleStepSelected"
          @message-sent="handleMessageSent"
          @plan-mode-clicked="handlePlanModeClicked"
        />
      </div>

      <!-- Right Panel - Preview -->
      <RightPanel ref="rightPanelRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/index.vue'
import RightPanel from '@/components/right-panel/index.vue'
import PlanExecutionComponent from '@/components/plan-execution/index.vue'
import { PlanActApiService } from '@/api/plan-act-api-service'

const route = useRoute()
const router = useRouter()

const prompt = ref<string>('')
const planExecutionRef = ref()
const rightPanelRef = ref()
const sidebarRef = ref()
const isExecutingPlan = ref(false)

onMounted(() => {
  // Initialize with prompt from conversation page
  prompt.value = (route.query.prompt as string) || ''
})

const handlePlanUpdate = (planData: any) => {
  console.log('[DirectView] Plan updated:', planData)
  // 将计划更新传递给右侧面板
  try {
    if (rightPanelRef.value && typeof rightPanelRef.value.handlePlanUpdate === 'function') {
      rightPanelRef.value.handlePlanUpdate(planData)
    }
  } catch (error) {
    console.error('[DirectView] Error calling rightPanelRef.handlePlanUpdate:', error)
  }
}

const handlePlanCompleted = (result: any) => {
  console.log('[DirectView] Plan completed:', result)
  // 处理计划完成事件
}

const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[DirectView] Step selected:', planId, stepIndex)
  // 将步骤选择事件传递给右侧面板
  try {
    if (rightPanelRef.value && typeof rightPanelRef.value.showStepDetails === 'function') {
      rightPanelRef.value.showStepDetails(planId, stepIndex)
    }
  } catch (error) {
    console.error('[DirectView] Error calling rightPanelRef.showStepDetails:', error)
  }
}

const handleDialogRoundStart = (planId: string, query: string) => {
  console.log('[DirectView] Dialog round started:', planId, query)
  // 处理对话轮次开始事件
}

const handleMessageSent = (message: string) => {
  console.log('[DirectView] Message sent:', message)
  // 处理消息发送事件
}

const goBack = () => {
  router.push('/home')
}

const handlePlanModeClicked = () => {
  console.log('[DirectView] Plan mode button clicked, toggling sidebar')
  if (sidebarRef.value && typeof sidebarRef.value.toggleSidebar === 'function') {
    sidebarRef.value.toggleSidebar()
  } else {
    console.warn('[DirectView] Sidebar ref not available or toggleSidebar method not found')
  }
}

const handleConfig = () => {
  router.push('/configs')
}

const handlePlanExecutionRequested = async (payload: { title: string; planData: any; params?: string }) => {
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
    
    console.log('[Direct] Executing plan with templateId:', planTemplateId, 'params:', payload.params)
    
    // 调用真实的 API 执行计划
    console.log('[Direct] About to call PlanActApiService.executePlan')
    let response
    if (payload.params && payload.params.trim()) {
      console.log('[Direct] Calling executePlan with params:', payload.params.trim())
      response = await PlanActApiService.executePlan(planTemplateId, payload.params.trim())
    } else {
      console.log('[Direct] Calling executePlan without params')
      response = await PlanActApiService.executePlan(planTemplateId)
    }
    
    console.log('[Direct] Plan execution API response:', response)
    
    // 使用返回的 planId，直接在聊天中显示执行开始消息，但不触发新的执行
    if (response.planId) {
      console.log('[Direct] Got planId from response:', response.planId, 'adding messages to chat')
      
      // 获取chat组件的引用直接添加消息
      const chatRef = planExecutionRef.value?.getChatRef()
      if (chatRef) {
        // 只添加用户消息，不通过sendMessage触发新的执行
        console.log('[Direct] Adding user message to chat:', payload.title)
        chatRef.addMessage('user', payload.title)
        
        // 添加助手消息表示正在执行
        console.log('[Direct] Adding assistant message to chat')
        chatRef.addMessage('assistant', '已收到执行计划请求，正在启动执行流程...', {
          thinking: '正在初始化计划执行环境...'
        })
        
        // 手动启动计划执行序列和轮询
        console.log('[Direct] Starting plan execution sequence with planId:', response.planId)
        const manager = planExecutionRef.value?.getPlanExecutionManager()
        if (manager) {
          // 设置活动计划ID
          manager.state.activePlanId = response.planId
          console.log('[Direct] Set activePlanId to:', response.planId)
          
          // 启动执行序列和轮询
          if (typeof manager.initiatePlanExecutionSequence === 'function') {
            manager.initiatePlanExecutionSequence(payload.title, response.planId)
          } else {
            console.error('[Direct] initiatePlanExecutionSequence method not available')
          }
        } else {
          console.error('[Direct] Plan execution manager not available')
        }
      } else {
        console.error('[Direct] Chat ref not available')
      }
    } else {
      console.error('[Direct] No planId in response:', response)
      throw new Error('执行计划失败：未返回有效的计划ID')
    }
    
  } catch (error: any) {
    console.error('[Direct] Plan execution failed:', error)
    console.error('[Direct] Error details:', { message: error.message, stack: error.stack })
    
    // 获取chat组件的引用来显示错误
    const chatRef = planExecutionRef.value?.getChatRef()
    if (chatRef) {
      console.log('[Direct] Adding error messages to chat')
      // 先添加用户消息
      chatRef.addMessage('user', payload.title)
      // 再添加错误消息
      chatRef.addMessage('assistant', `执行计划失败: ${error.message || '未知错误'}`, {
        thinking: undefined
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
  width: 50%;
  border-right: 1px solid #1a1a1a;
  display: flex;
  flex-direction: column;
  height: 100vh; /* 使用固定高度 */
  overflow: hidden; /* 防止面板本身溢出 */
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
</style>
