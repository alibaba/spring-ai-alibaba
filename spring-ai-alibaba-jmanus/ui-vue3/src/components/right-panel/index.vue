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
  <div class="right-panel">
    <div class="preview-header">
      <div class="preview-tabs">
        <!-- 只显示 details 按钮，临时注释掉 chat 和 code 按钮 -->
        <button
          v-for="tab in previewTabs.filter(t => t.id === 'details')"
          :key="tab.id"
          class="tab-button"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          <Icon :icon="tab.icon" />
          {{ tab.name }}
        </button>
        <!-- 临时注释掉 chat 和 code 按钮，但保留所有实现 -->
        <!-- 
        <button
          v-for="tab in rightPanelStore.previewTabs.filter(t => t.id === 'chat' || t.id === 'code')"
          :key="tab.id"
          class="tab-button"
          :class="{ active: rightPanelStore.activeTab === tab.id }"
          @click="rightPanelStore.switchTab(tab.id)"
        >
          <Icon :icon="tab.icon" />
          {{ tab.name }}
        </button>
        -->
      </div>
      <div class="preview-actions">
        <button
          class="action-button"
          @click="copyCode"
          v-if="activeTab === 'code'"
        >
          <Icon icon="carbon:copy" />
        </button>
        <button
          class="action-button"
          @click="downloadCode"
          v-if="activeTab === 'code'"
        >
          <Icon icon="carbon:download" />
        </button>
      </div>
    </div>

    <div class="preview-content">
      <!-- Code Preview -->
      <div v-if="activeTab === 'code'" class="code-preview">
        <MonacoEditor
          :language="codeLanguage"
          :theme="'vs-dark'"
          :height="'100%'"
          :readonly="true"
          :editor-options="{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            wordWrap: 'on',
          }"
        />
      </div>

      <!-- Chat Preview -->
      <div v-else-if="activeTab === 'chat'" class="chat-preview">
        <div class="chat-bubbles">
          <!-- Chat bubbles content removed as chatBubbles is no longer available -->
        </div>
      </div>

      <!-- Step Execution Details -->
      <div v-else-if="activeTab === 'details'" class="step-details">
        <!-- Fixed top step basic information -->
        <div v-if="selectedStep" class="step-info-fixed">
          <h3>
            {{
              selectedStep.title ||
              selectedStep.description ||
              `步骤 ${selectedStep.index + 1}`
            }}
          </h3>

          <div class="agent-info" v-if="selectedStep.agentExecution">
            <div class="info-item">
              <span class="label">执行智能体:</span>
              <span class="value">{{ selectedStep.agentExecution.agentName }}</span>
            </div>
            <div class="info-item">
              <span class="label">描述:</span>
              <span class="value">{{
                selectedStep.agentExecution.agentDescription || ''
              }}</span>
            </div>
            <div class="info-item">
              <span class="label">请求:</span>
              <span class="value">{{
                selectedStep.agentExecution.agentRequest || ''
              }}</span>
            </div>
            <div class="info-item">
              <span class="label">执行结果:</span>
              <span
                class="value"
                :class="{ success: selectedStep.agentExecution.isCompleted }"
              >
                {{ selectedStep.agentExecution.result || '执行中...' }}
              </span>
            </div>
          </div>

          <div class="execution-status">
            <div class="status-item">
              <Icon
                icon="carbon:checkmark-filled"
                v-if="selectedStep.completed"
                class="status-icon success"
              />
              <Icon
                icon="carbon:in-progress"
                v-else-if="selectedStep.current"
                class="status-icon progress"
              />
              <Icon icon="carbon:time" v-else class="status-icon pending" />
              <span class="status-text">
                {{ stepStatusText }}
              </span>
            </div>
          </div>
        </div>

        <!-- Scrollable detailed content area -->
        <div
          ref="scrollContainer"
          class="step-details-scroll-container"
          @scroll="checkScrollState"
        >
          <div v-if="selectedStep">
            <!-- Think and action steps -->
            <div
              class="think-act-steps"
              v-if="selectedStep.agentExecution?.thinkActSteps?.length > 0"
            >
              <h4>思考与行动步骤</h4>
              <div class="steps-container">
                <div
                  v-for="(tas, index) in selectedStep.agentExecution.thinkActSteps"
                  :key="index"
                  class="think-act-step"
                >
                  <div class="step-header">
                    <span class="step-number">#{{ index + 1 }}</span>
                    <span class="step-status" :class="tas.status">{{
                      tas.status || '执行中'
                    }}</span>
                  </div>

                  <!-- Think section - strictly follow right-sidebar.js logic -->
                  <div class="think-section">
                    <h5><Icon icon="carbon:thinking" /> 思考</h5>
                    <div class="think-content">
                      <div class="input">
                        <span class="label">输入:</span>
                        <pre>{{ formatJson(tas.thinkInput) }}</pre>
                      </div>
                      <div class="output">
                        <span class="label">输出:</span>
                        <pre>{{ formatJson(tas.thinkOutput) }}</pre>
                      </div>
                    </div>
                  </div>

                  <!-- Action section - strictly follow right-sidebar.js logic -->
                  <div v-if="tas.actionNeeded" class="action-section">
                    <h5><Icon icon="carbon:play" /> 行动</h5>
                    <div class="action-content">
                      <div class="tool-info">
                        <span class="label">工具:</span>
                        <span class="value">{{ tas.toolName || '' }}</span>
                      </div>
                      <div class="input">
                        <span class="label">工具参数:</span>
                        <pre>{{ formatJson(tas.toolParameters) }}</pre>
                      </div>
                      <div class="output">
                        <span class="label">执行结果:</span>
                        <pre>{{ formatJson(tas.actionResult) }}</pre>
                      </div>
                    </div>
                  </div>

                  <!-- 子执行计划部分 - 新增功能 -->
                  <div v-if="tas.subPlanExecutionRecord" class="sub-plan-section">
                    <h5><Icon icon="carbon:tree-view" /> 子执行计划</h5>
                    <div class="sub-plan-content">
                      <div class="sub-plan-header">
                        <div class="sub-plan-info">
                          <span class="label">子计划ID:</span>
                          <span class="value">{{ tas.subPlanExecutionRecord.planId }}</span>
                        </div>
                        <div class="sub-plan-info" v-if="tas.subPlanExecutionRecord.title">
                          <span class="label">标题:</span>
                          <span class="value">{{ tas.subPlanExecutionRecord.title }}</span>
                        </div>
                        <div class="sub-plan-status">
                          <Icon
                            icon="carbon:checkmark-filled"
                            v-if="tas.subPlanExecutionRecord.completed"
                            class="status-icon success"
                          />
                          <Icon icon="carbon:in-progress" v-else class="status-icon progress" />
                          <span class="status-text">
                            {{ tas.subPlanExecutionRecord.completed ? '已完成' : '执行中' }}
                          </span>
                        </div>
                      </div>

                      <!-- 子计划的主要步骤 -->
                      <div
                        class="sub-plan-steps"
                        v-if="tas.subPlanExecutionRecord.steps && tas.subPlanExecutionRecord.steps.length > 0"
                      >
                        <h6>主要步骤</h6>
                        <div class="sub-plan-step-list">
                          <div
                            v-for="(subStep, subStepIndex) in tas.subPlanExecutionRecord.steps"
                            :key="subStepIndex"
                            class="sub-plan-step-item"
                            :class="{
                              completed: getSubStepStatus(tas.subPlanExecutionRecord, subStepIndex) === 'completed',
                              current: getSubStepStatus(tas.subPlanExecutionRecord, subStepIndex) === 'current',
                              pending: getSubStepStatus(tas.subPlanExecutionRecord, subStepIndex) === 'pending'
                            }"
                            @click="handleSubPlanStepClick(tas.subPlanExecutionRecord, subStepIndex)"
                          >
                            <div class="sub-step-indicator">
                              <Icon
                                icon="carbon:checkmark-filled"
                                v-if="getSubStepStatus(tas.subPlanExecutionRecord, subStepIndex) === 'completed'"
                                class="step-icon success"
                              />
                              <Icon
                                icon="carbon:in-progress"
                                v-else-if="getSubStepStatus(tas.subPlanExecutionRecord, subStepIndex) === 'current'"
                                class="step-icon current"
                              />
                              <Icon icon="carbon:circle" v-else class="step-icon pending" />
                              <span class="step-number">{{ subStepIndex + 1 }}</span>
                            </div>
                            <div class="sub-step-content">
                              <span class="sub-step-title">{{ subStep }}</span>
                              <span class="sub-step-badge">子步骤</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-else-if="
                selectedStep.agentExecution &&
                !selectedStep.agentExecution.thinkActSteps?.length
              "
              class="no-steps-message"
            >
              <p>暂无详细步骤信息</p>
            </div>

            <!-- Handle no agentExecution case -->
            <div
              v-else-if="!selectedStep.agentExecution"
              class="no-execution-message"
            >
              <Icon icon="carbon:information" class="info-icon" />
              <h4>步骤信息</h4>
              <div class="step-basic-info">
                <div class="info-item">
                  <span class="label">步骤名称:</span>
                  <span class="value">{{
                    selectedStep.title ||
                    selectedStep.description ||
                    `步骤 ${selectedStep.index + 1}`
                  }}</span>
                </div>
                <div class="info-item" v-if="selectedStep.description">
                  <span class="label">描述:</span>
                  <span class="value">{{ selectedStep.description }}</span>
                </div>
                <div class="info-item">
                  <span class="label">状态:</span>
                  <span class="value" :class="{
                    'status-completed': selectedStep.completed,
                    'status-current': selectedStep.current,
                    'status-pending': !selectedStep.completed && !selectedStep.current
                  }">
                    {{
                      selectedStep.completed ? '已完成' :
                      selectedStep.current ? '执行中' : '待执行'
                    }}
                  </span>
                </div>
              </div>
              <p class="no-execution-hint">该步骤暂无详细执行信息</p>
            </div>

            <!-- Dynamic effect during execution -->
            <div
              v-if="selectedStep.current && !selectedStep.completed"
              class="execution-indicator"
            >
              <div class="execution-waves">
                <div class="wave wave-1"></div>
                <div class="wave wave-2"></div>
                <div class="wave wave-3"></div>
              </div>
              <p class="execution-text">
                <Icon icon="carbon:in-progress" class="rotating-icon" />
                步骤正在执行中，请稍候...
              </p>
            </div>
          </div>

          <div v-else class="no-selection">
            <Icon icon="carbon:events" class="empty-icon" />
            <h3>{{ t('rightPanel.noStepSelected') }}</h3>
            <p>{{ t('rightPanel.selectStepHint') }}</p>
          </div>
        </div>

        <!-- Scroll to bottom button -->
        <Transition name="scroll-button">
          <button
            v-if="showScrollToBottomButton"
            @click="scrollToBottom"
            class="scroll-to-bottom-btn"
            title="滚动到底部"
          >
            <Icon icon="carbon:chevron-down" />
          </button>
        </Transition>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-preview">
        <Icon icon="carbon:document" class="empty-icon" />
        <h3>No preview available</h3>
        <p>Start a conversation to see the generated content here.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/editor/index.vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import type { PlanExecutionRecord } from '@/types/plan-execution-record'

const { t } = useI18n()

// DOM element reference
const scrollContainer = ref<HTMLElement>()

// Local state - replacing store state
const activeTab = ref('details')
const planDataMap = ref<Map<string, any>>(new Map())
const currentDisplayedPlanId = ref<string>()
const selectedStep = ref<any>()

// Scroll-related state
const showScrollToBottomButton = ref(false)
const isNearBottom = ref(true)
const shouldAutoScrollToBottom = ref(true)

// Auto-refresh related state
const autoRefreshTimer = ref<number | null>(null)
const AUTO_REFRESH_INTERVAL = 3000 // Refresh step details every 3 seconds

// Code and chat preview related state
const codeLanguage = ref('java')

// Preview tab configuration
const previewTabs = [
  { id: 'details', name: '步骤执行详情', icon: 'carbon:events' },
  { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
  { id: 'code', name: 'Code', icon: 'carbon:code' },
]


const stepStatusText = computed(() => {
  if (!selectedStep.value) return ''
  if (selectedStep.value.completed) return '已完成'
  if (selectedStep.value.current) return '执行中'
  return '等待执行'
})

// Actions - Tab management
const switchTab = (tabId: string) => {
  activeTab.value = tabId
}

// Actions - Plan data management
const handlePlanUpdate = async (planData: PlanExecutionRecord) => {
  console.log('[RightPanel] Received plan update event, planData:', planData)

  // Validate data validity
  if (!planData || !planData.currentPlanId) {
    console.warn('[RightPanel] Invalid plan data received:', planData)
    return
  }

  // Update plan data to local mapping
  planDataMap.value.set(planData.currentPlanId, planData)
  console.log('[RightPanel] Plan data updated to planDataMap:', planData.currentPlanId)

  // Process sub-plan data from agentExecutionSequence if exists
  if (planData.agentExecutionSequence) {
    planData.agentExecutionSequence.forEach((agentExecution: any, agentIndex: number) => {
      if (agentExecution.thinkActSteps) {
        agentExecution.thinkActSteps.forEach((thinkActStep: any, stepIndex: number) => {
          if (thinkActStep.subPlanExecutionRecord) {
            const subPlanId = thinkActStep.subPlanExecutionRecord.currentPlanId
            if (subPlanId) {
              console.log(`[RightPanel] Processing sub-plan from agent ${agentIndex}, step ${stepIndex}:`, subPlanId)
              planDataMap.value.set(subPlanId, thinkActStep.subPlanExecutionRecord)
            }
          }
        })
      }
    })
  }

  // If currently selected step corresponds to this plan, reload step details
  if (selectedStep.value?.planId === planData.currentPlanId) {
    console.log('[RightPanel] Refresh details of currently selected step:', selectedStep.value.index)
    showStepDetails(planData.currentPlanId, selectedStep.value.index)
  }

  // After data update, auto-scroll to latest content if previously at bottom
  autoScrollToBottomIfNeeded()
}

const updateDisplayedPlanProgress = (planData: any) => {
  // Here you can update UI state, such as progress bars
  if (planData.steps && planData.steps.length > 0) {
    const totalSteps = planData.steps.length
    const currentStep = (planData.currentStepIndex || 0) + 1
    console.log(`[RightPanel] Progress: ${currentStep} / ${totalSteps}`)
  }
}

// Actions - Step details management
const showStepDetails = (planId: string, stepIndex: number) => {
  console.log('[RightPanel] Show step details:', { planId, stepIndex })

  const planData = planDataMap.value.get(planId)

  if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
    selectedStep.value = null
    console.warn('[RightPanel] Invalid step data:', {
      planId,
      stepIndex,
      hasPlanData: !!planData,
      hasSteps: !!planData?.steps,
      stepsLength: planData?.steps?.length,
      planDataKeys: Array.from(planDataMap.value.keys()),
      availablePlansData: Array.from(planDataMap.value.entries()).map(([key, value]) => ({
        planId: key,
        stepsCount: value.steps?.length || 0,
        hasAgentExecution: !!value.agentExecutionSequence,
        agentExecutionLength: value.agentExecutionSequence?.length || 0
      }))
    })
    stopAutoRefresh() // Stop auto refresh
    return
  }

  currentDisplayedPlanId.value = planId
  const step = planData.steps[stepIndex]
  const agentExecution =
    planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex]

  console.log('[RightPanel] Step data details:', {
    planId,
    stepIndex,
    step,
    hasAgentExecutionSequence: !!planData.agentExecutionSequence,
    agentExecutionSequenceLength: planData.agentExecutionSequence?.length,
    agentExecution,
    hasThinkActSteps: !!agentExecution?.thinkActSteps,
    thinkActStepsLength: agentExecution?.thinkActSteps?.length
  })

  // Determine if step is completed - multiple condition checks
  const isStepCompleted =
    agentExecution?.isCompleted ||
    planData.completed ||
    (planData.currentStepIndex !== undefined && stepIndex < planData.currentStepIndex)

  const isCurrent =
    !isStepCompleted && stepIndex === planData.currentStepIndex && !planData.completed

  // Construct step details object, similar to right-sidebar.js logic
  selectedStep.value = {
    planId: planId, // Ensure planId is included
    index: stepIndex,
    title:
      typeof step === 'string'
        ? step
        : step.title || step.description || step.name || `步骤 ${stepIndex + 1}`,
    description: typeof step === 'string' ? step : step.description || step,
    agentExecution: agentExecution,
    completed: isStepCompleted,
    current: isCurrent,
  }

  console.log('[RightPanel] Step details updated:', {
    planId,
    stepIndex,
    stepTitle: selectedStep.value.title,
    hasAgentExecution: !!agentExecution,
    hasThinkActSteps: !!agentExecution?.thinkActSteps?.length,
    thinkActStepsData: agentExecution?.thinkActSteps,
    completed: isStepCompleted,
    current: isCurrent,
    planCurrentStep: planData.currentStepIndex,
    planCompleted: planData.completed,
  })

  // Process sub-plan data if exists
  if (agentExecution?.thinkActSteps) {
    agentExecution.thinkActSteps.forEach((thinkActStep: any, index: number) => {
      if (thinkActStep.subPlanExecutionRecord) {
        console.log(`[RightPanel] Found sub-plan in thinkActStep ${index}:`, thinkActStep.subPlanExecutionRecord)
        
        const subPlanId = thinkActStep.subPlanExecutionRecord.planId
        
        // Critical fix: Check for ID conflicts between main plan and sub-plan
        if (subPlanId === planId) {
          console.error(`[RightPanel] CRITICAL ERROR: Sub-plan ID "${subPlanId}" is identical to parent plan ID "${planId}". This will cause data corruption!`)
          console.error('[RightPanel] Sub-plan data:', thinkActStep.subPlanExecutionRecord)
          console.error('[RightPanel] Parent plan data:', planData)
          
          // Generate a unique sub-plan ID to prevent collision
          const uniqueSubPlanId = `${subPlanId}_sub_${stepIndex}_${index}_${Date.now()}`
          console.warn(`[RightPanel] Auto-correcting sub-plan ID from "${subPlanId}" to "${uniqueSubPlanId}"`)
          
          // Update the sub-plan record with the corrected ID
          const correctedSubPlan = { ...thinkActStep.subPlanExecutionRecord, planId: uniqueSubPlanId }
          planDataMap.value.set(uniqueSubPlanId, correctedSubPlan)
          
          // Also update the original record to prevent future issues
          thinkActStep.subPlanExecutionRecord.planId = uniqueSubPlanId
        } else if (subPlanId && !planDataMap.value.has(subPlanId)) {
          // Normal case: sub-plan has unique ID
          console.log(`[RightPanel] Storing sub-plan with unique ID: ${subPlanId}`)
          planDataMap.value.set(subPlanId, thinkActStep.subPlanExecutionRecord)
        } else if (subPlanId && planDataMap.value.has(subPlanId)) {
          // Sub-plan ID already exists, check if it's the same plan or different
          const existingPlan = planDataMap.value.get(subPlanId)
          if (existingPlan && existingPlan !== thinkActStep.subPlanExecutionRecord) {
            console.warn(`[RightPanel] Sub-plan ID "${subPlanId}" already exists with different data. Updating with latest data.`)
            planDataMap.value.set(subPlanId, thinkActStep.subPlanExecutionRecord)
          }
        }
      }
    })
  }

  // If step is not completed and plan is still executing, start auto refresh
  if (
    !isStepCompleted &&
    !planData.completed &&
    planExecutionManager.getActivePlanId() === planId
  ) {
    startAutoRefresh(planId, stepIndex)
  } else {
    stopAutoRefresh()
  }

  // Delay scroll state check to ensure DOM is updated
  setTimeout(() => {
    checkScrollState()
  }, 100)

  // After data update, auto-scroll to latest content if previously at bottom
  autoScrollToBottomIfNeeded()
}

// Actions - Step selection handling (callable methods for external components)
/**
 * Handle step selection - reuses handlePlanUpdate logic for data loading and display
 * This method can be called from parent components to display specific step details
 * @param planId - The plan ID to display
 * @param stepIndex - The step index to display
 */
const handleStepSelected = (planId: string, stepIndex: number) => {
  console.log('[RightPanel] Step selected:', { planId, stepIndex })
  
  // Use existing plan data if available, otherwise trigger plan update
  const existingPlanData = planDataMap.value.get(planId)
  if (existingPlanData) {
    // Direct display using existing data
    showStepDetails(planId, stepIndex)
  } else {
    // Load plan data from plan execution manager first
    const planData = planExecutionManager.getCachedPlanRecord(planId)
    if (planData) {
      handlePlanUpdate(planData).then(() => {
        showStepDetails(planId, stepIndex)
      }).catch((error) => {
        console.error('[RightPanel] Error loading plan data for step selection:', error)
      })
    } else {
      console.warn('[RightPanel] Plan data not found in manager:', planId)
    }
  }
}

/**
 * Handle sub-plan step selection - reuses handlePlanUpdate logic for nested plan display
 * This method can be called from parent components to display specific sub-plan step details
 * @param parentPlanId - The parent plan ID
 * @param subPlanId - The sub-plan ID to display
 * @param stepIndex - The parent step index
 * @param subStepIndex - The sub-step index to display
 */
const handleSubPlanStepSelected = (parentPlanId: string, subPlanId: string, stepIndex: number, subStepIndex: number) => {
  console.log('[RightPanel] Sub plan step selected:', {
    parentPlanId,
    subPlanId, 
    stepIndex,
    subStepIndex
  })
  
  // Check if sub-plan data is already available in planDataMap
  const subPlanData = planDataMap.value.get(subPlanId)
  if (subPlanData) {
    // Direct display using existing sub-plan data
    showStepDetails(subPlanId, subStepIndex)
  } else {
    // First ensure parent plan is loaded to access sub-plan data
    const parentPlanData = planDataMap.value.get(parentPlanId)
    if (parentPlanData) {
      // Parent plan exists, try to find and load sub-plan data
      const agentExecution = parentPlanData.agentExecutionSequence?.[stepIndex]
      const thinkActStep = agentExecution?.thinkActSteps?.find((step: any) => 
        step.subPlanExecutionRecord?.planId === subPlanId
      )
      
      if (thinkActStep?.subPlanExecutionRecord) {
        // Add sub-plan to planDataMap and display
        planDataMap.value.set(subPlanId, thinkActStep.subPlanExecutionRecord)
        showStepDetails(subPlanId, subStepIndex)
      } else {
        console.warn('[RightPanel] Sub-plan not found in parent plan data:', {
          parentPlanId,
          subPlanId,
          stepIndex
        })
      }
    } else {
      // Load parent plan first from plan execution manager
      const parentPlan = planExecutionManager.getCachedPlanRecord(parentPlanId)
      if (parentPlan) {
        handlePlanUpdate(parentPlan).then(() => {
          handleSubPlanStepSelected(parentPlanId, subPlanId, stepIndex, subStepIndex)
        }).catch((error) => {
          console.error('[RightPanel] Error loading parent plan data for sub-plan step selection:', error)
        })
      } else {
        console.warn('[RightPanel] Parent plan data not found in manager:', parentPlanId)
      }
    }
  }
}


// Actions - Auto refresh management
const startAutoRefresh = (planId: string, stepIndex: number) => {
  console.log('[RightPanel] Start auto refresh:', { planId, stepIndex })

  // Stop previous refresh
  stopAutoRefresh()

  autoRefreshTimer.value = window.setInterval(() => {
    console.log('[RightPanel] Execute auto refresh - Step details')

    // Check if plan is still executing
    const planData = planDataMap.value.get(planId)
    if (!planData || planData.completed) {
      console.log('[RightPanel] Plan completed, stop auto refresh')
      stopAutoRefresh()
      return
    }

    // Check if step is still executing
    const agentExecution = planData.agentExecutionSequence?.[stepIndex]
    if (agentExecution?.isCompleted) {
      console.log('[RightPanel] Step completed, stop auto refresh')
      stopAutoRefresh()
      return
    }

    // Check if already moved to next step
    const currentStepIndex = planData.currentStepIndex ?? 0
    if (stepIndex < currentStepIndex) {
      console.log('[RightPanel] Already moved to next step, stop auto refresh')
      stopAutoRefresh()
      return
    }

    // Refresh step details
    showStepDetails(planId, stepIndex)

    // After data update, auto-scroll to latest content if previously at bottom
    autoScrollToBottomIfNeeded()
  }, AUTO_REFRESH_INTERVAL)
}

const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
    console.log('[RightPanel] Auto refresh stopped')
  }
}

// Actions - Scroll management
const setScrollContainer = (element: HTMLElement | null) => {
  scrollContainer.value = element || undefined
}

const checkScrollState = () => {
  if (!scrollContainer.value) return

  const { scrollTop, scrollHeight, clientHeight } = scrollContainer.value
  const isAtBottom = scrollHeight - scrollTop - clientHeight < 50
  const hasScrollableContent = scrollHeight > clientHeight

  isNearBottom.value = isAtBottom
  showScrollToBottomButton.value = hasScrollableContent && !isAtBottom

  // Update auto-scroll flag: should auto-scroll if user scrolls to bottom
  // If user actively scrolls up away from bottom, stop auto-scrolling
  if (isAtBottom) {
    shouldAutoScrollToBottom.value = true
  } else if (scrollHeight - scrollTop - clientHeight > 100) {
    // If user clearly scrolled up (more than 100px from bottom), stop auto-scrolling
    shouldAutoScrollToBottom.value = false
  }

  console.log('[RightPanel] Scroll state check:', {
    scrollTop,
    scrollHeight,
    clientHeight,
    isAtBottom,
    hasScrollableContent,
    showButton: showScrollToBottomButton.value,
    shouldAutoScroll: shouldAutoScrollToBottom.value,
  })
}

const scrollToBottom = () => {
  if (!scrollContainer.value) return

  scrollContainer.value.scrollTo({
    top: scrollContainer.value.scrollHeight,
    behavior: 'smooth',
  })

  // Reset state after scrolling
  nextTick(() => {
    shouldAutoScrollToBottom.value = true
    checkScrollState()
  })
}

const autoScrollToBottomIfNeeded = () => {
  if (!shouldAutoScrollToBottom.value || !scrollContainer.value) return

  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
      console.log('[RightPanel] Auto scroll to bottom')
    }
  })
}

// Actions - Code operations
const copyCode = () => {
  console.warn('Copy code functionality is disabled as codeContent is removed.')
}

const downloadCode = () => {
  console.warn('Download code functionality is disabled as codeContent is removed.')
}


// Actions - Utility functions
const formatJson = (jsonData: any): string => {
  if (jsonData === null || typeof jsonData === 'undefined' || jsonData === '') {
    return 'N/A'
  }
  try {
    const jsonObj = typeof jsonData === 'object' ? jsonData : JSON.parse(jsonData)
    return JSON.stringify(jsonObj, null, 2)
  } catch (e) {
    // If parsing fails, return string format directly (similar to _escapeHtml in right-sidebar.js)
    return String(jsonData)
  }
}

// Actions - Resource cleanup
const cleanup = () => {
  stopAutoRefresh()
  planDataMap.value.clear()
  selectedStep.value = null
  currentDisplayedPlanId.value = undefined
  shouldAutoScrollToBottom.value = true

  if (scrollContainer.value) {
    scrollContainer.value.removeEventListener('scroll', checkScrollState)
  }
}

// Initialize scroll listener
const initScrollListener = () => {
  const setupScrollListener = () => {
    const element = scrollContainer.value
    if (!element) {
      console.log('[RightPanel] Scroll container not found, retrying...')
      return false
    }

    // Set scroll container
    setScrollContainer(element)

    element.addEventListener('scroll', checkScrollState)
    // Initial state check
    shouldAutoScrollToBottom.value = true // Reset to auto scroll state
    checkScrollState()
    console.log('[RightPanel] Scroll listener initialized successfully')
    return true
  }

  // Use nextTick to ensure DOM is updated
  nextTick(() => {
    if (!setupScrollListener()) {
      // If first attempt fails, try again
      setTimeout(() => {
        setupScrollListener()
      }, 100)
    }
  })
}

// Lifecycle - initialization on mount
onMounted(() => {
  console.log('Right panel component mounted')
  // Use nextTick to ensure DOM is rendered
  nextTick(() => {
    initScrollListener()
  })
})

// Lifecycle - cleanup on unmount
onUnmounted(() => {
  console.log('[RightPanel] Component unmounting, cleaning up...')
  cleanup()
})

// Get sub-step status
const getSubStepStatus = (subPlan: any, stepIndex: number) => {
  if (!subPlan) return 'pending'
  
  const currentStepIndex = subPlan.currentStepIndex
  if (subPlan.completed) {
    return 'completed'
  }
  
  if (currentStepIndex === undefined || currentStepIndex === null) {
    return stepIndex === 0 ? 'current' : 'pending'
  }
  
  if (stepIndex < currentStepIndex) {
    return 'completed'
  } else if (stepIndex === currentStepIndex) {
    return 'current'
  } else {
    return 'pending'
  }
}

// Handle sub-plan step click
const handleSubPlanStepClick = (subPlan: any, stepIndex: number) => {
  if (!subPlan || !subPlan.planId) {
    console.warn('[RightPanel] Invalid sub-plan data:', subPlan)
    return
  }

  console.log('[RightPanel] Sub-plan step clicked:', {
    subPlanId: subPlan.planId,
    stepIndex: stepIndex,
    stepTitle: subPlan.steps?.[stepIndex]
  })

  // Show sub-plan step details
  showStepDetails(subPlan.planId, stepIndex)
}

// Expose methods to parent component - only keep necessary interfaces
defineExpose({
  handlePlanUpdate,
  showStepDetails,
  updateDisplayedPlanProgress,
  handleStepSelected,
  handleSubPlanStepSelected,
})
</script>

<style lang="less" scoped>
.right-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
}

.preview-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.02);
}

.preview-tabs {
  display: flex;
  gap: 8px;
}

.tab-button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
    border-color: #667eea;
    color: #667eea;
  }
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 确保flex子项可以收缩 */
}

.code-preview {
  height: 100%;
}

.chat-preview {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.chat-bubbles {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-bubble {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;

  &.thinking {
    border-left: 4px solid #f39c12;
  }

  &.progress {
    border-left: 4px solid #3498db;
  }

  &.success {
    border-left: 4px solid #27ae60;
  }
}

.bubble-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;

  .timestamp {
    margin-left: auto;
    font-size: 12px;
    color: #666666;
    font-weight: 400;
  }
}

.bubble-content {
  color: #cccccc;
  line-height: 1.5;
  font-size: 14px;
}

.empty-preview {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666666;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #888888;
  }

  p {
    margin: 0;
    font-size: 14px;
    text-align: center;
  }
}

/* 步骤详情样式 */
.step-details {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 确保flex子项可以收缩 */
}

/* 固定在顶部的步骤基本信息 */
.step-info-fixed {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(41, 42, 45, 0.95); /* 半透明背景，保持一定透明度 */
  backdrop-filter: blur(10px); /* 背景模糊效果 */
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px;
  margin: 0 20px;
  border-radius: 8px 8px 0 0;

  h3 {
    color: #ffffff;
    margin: 0 0 16px 0;
    font-size: 18px;
    font-weight: 600;
    padding-bottom: 8px;
    border-bottom: 2px solid #667eea;
  }
}

.step-details-scroll-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 20px 20px; /* 移除顶部padding，因为固定头部已有padding */
  margin: 0 20px 20px;
  background: rgba(255, 255, 255, 0.01);
  border-radius: 0 0 8px 8px; /* 调整圆角，与固定头部配合 */

  /* 自定义滚动条样式 */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 3px;

    &:hover {
      background: rgba(255, 255, 255, 0.5);
    }
  }
}

/* 步骤信息样式 - 用于固定顶部 */
.agent-info {
  margin-bottom: 16px;

  .info-item {
    display: flex;
    margin-bottom: 8px;
    font-size: 14px;
    line-height: 1.4;

    .label {
      min-width: 100px;
      font-weight: 600;
      color: #888888;
      flex-shrink: 0;
    }

    .value {
      flex: 1;
      color: #cccccc;
      word-break: break-word;

      &.success {
        color: #27ae60;
      }
    }
  }
}

.execution-status {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .status-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-icon {
      font-size: 16px;

      &.success {
        color: #27ae60;
      }

      &.progress {
        color: #3498db;
      }

      &.pending {
        color: #f39c12;
      }
    }

    .status-text {
      color: #cccccc;
      font-weight: 500;
    }
  }
}

.no-steps-message {
  text-align: center;
  color: #666666;
  font-style: italic;
  margin-top: 16px;

  p {
    margin: 0;
  }
}

.no-execution-message {
  padding: 20px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin-top: 16px;

  .info-icon {
    color: #6c757d;
    font-size: 20px;
    margin-bottom: 8px;
  }

  h4 {
    margin: 0 0 16px 0;
    color: #495057;
    font-size: 16px;
    font-weight: 500;
  }

  .step-basic-info {
    .info-item {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;

      .label {
        font-weight: 500;
        color: #6c757d;
        min-width: 80px;
        margin-right: 8px;
      }

      .value {
        color: #333;
        flex: 1;

        &.status-completed {
          color: #28a745;
          font-weight: 500;
        }

        &.status-current {
          color: #007bff;
          font-weight: 500;
        }

        &.status-pending {
          color: #6c757d;
        }
      }
    }
  }

  .no-execution-hint {
    margin: 16px 0 0 0;
    color: #6c757d;
    font-style: italic;
    font-size: 13px;
    text-align: center;
  }
}

.execution-indicator {
  margin-top: 20px;
  padding: 20px;
  background: rgba(74, 144, 226, 0.1);
  border: 1px solid rgba(74, 144, 226, 0.3);
  border-radius: 8px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.execution-waves {
  position: relative;
  height: 4px;
  margin-bottom: 16px;
  background: rgba(74, 144, 226, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.wave {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(74, 144, 226, 0.6), transparent);
  border-radius: 2px;
}

.wave-1 {
  animation: wave-animation 2s ease-in-out infinite;
}

.wave-2 {
  animation: wave-animation 2s ease-in-out infinite 0.6s;
}

.wave-3 {
  animation: wave-animation 2s ease-in-out infinite 1.2s;
}

@keyframes wave-animation {
  0% {
    left: -100%;
  }
  50% {
    left: 100%;
  }
  100% {
    left: 100%;
  }
}

.execution-text {
  color: #4a90e2;
  font-size: 14px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rotating-icon {
  animation: rotate-animation 1s linear infinite;
}

@keyframes rotate-animation {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.step-info {
  h3 {
    color: #ffffff;
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.think-act-steps {
  margin-top: 20px; /* 增加顶部间距，因为现在没有固定头部的步骤信息 */

  h4 {
    color: #ffffff;
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  }
}

.steps-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.think-act-step {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 16px;

  .step-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .step-number {
      font-weight: 600;
      color: #667eea;
      font-size: 14px;
    }

    .step-status {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;

      &.completed {
        background: rgba(39, 174, 96, 0.2);
        color: #27ae60;
      }

      &.running {
        background: rgba(52, 152, 219, 0.2);
        color: #3498db;
      }

      &.pending {
        background: rgba(243, 156, 18, 0.2);
        color: #f39c12;
      }
    }
  }

  .think-section,
  .action-section,
  .sub-plan-section {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }

    h5 {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0 0 12px 0;
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
    }
  }

  .think-content,
  .action-content {
    .input,
    .output,
    .tool-info {
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        display: block;
        font-weight: 600;
        color: #888888;
        margin-bottom: 4px;
        font-size: 12px;
      }

      .value {
        color: #cccccc;
        font-size: 14px;
      }

      pre {
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 4px;
        padding: 12px;
        color: #cccccc;
        font-size: 12px;
        overflow-x: auto;
        white-space: pre-wrap;
        margin: 0;
        line-height: 1.4;
        max-height: 200px;
        overflow-y: auto;
      }
    }
  }

  /* 子计划样式 */
  .sub-plan-content {
    .sub-plan-header {
      background: rgba(102, 126, 234, 0.1);
      border: 1px solid rgba(102, 126, 234, 0.3);
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 12px;

      .sub-plan-info {
        display: flex;
        margin-bottom: 8px;
        font-size: 12px;

        &:last-child {
          margin-bottom: 0;
        }

        .label {
          min-width: 80px;
          font-weight: 600;
          color: #888888;
          flex-shrink: 0;
        }

        .value {
          flex: 1;
          color: #cccccc;
          word-break: break-word;
        }
      }

      .sub-plan-status {
        display: flex;
        align-items: center;
        gap: 6px;
        padding-top: 8px;
        border-top: 1px solid rgba(255, 255, 255, 0.1);

        .status-icon {
          font-size: 14px;

          &.success {
            color: #27ae60;
          }

          &.progress {
            color: #3498db;
          }
        }

        .status-text {
          color: #cccccc;
          font-size: 12px;
          font-weight: 500;
        }
      }
    }

    .sub-plan-steps {
      h6 {
        color: #ffffff;
        margin: 0 0 8px 0;
        font-size: 13px;
        font-weight: 600;
      }

      .sub-plan-step-list {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }

      .sub-plan-step-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 12px;
        background: rgba(255, 255, 255, 0.02);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s ease;
        position: relative;

        &:hover {
          background: rgba(255, 255, 255, 0.05);
          border-color: rgba(102, 126, 234, 0.5);
        }

        &.completed {
          background: rgba(39, 174, 96, 0.1);
          border-color: rgba(39, 174, 96, 0.3);
        }

        &.current {
          background: rgba(52, 152, 219, 0.1);
          border-color: rgba(52, 152, 219, 0.3);
          box-shadow: 0 0 8px rgba(52, 152, 219, 0.2);
        }

        &.pending {
          opacity: 0.7;
        }

        .sub-step-indicator {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-shrink: 0;

          .step-icon {
            font-size: 12px;

            &.success {
              color: #27ae60;
            }

            &.current {
              color: #3498db;
            }

            &.pending {
              color: #666666;
            }
          }

          .step-number {
            font-size: 11px;
            color: #888888;
            font-weight: 500;
            min-width: 16px;
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
            font-size: 13px;
            line-height: 1.4;
            word-break: break-word;
            flex: 1;
          }

          .sub-step-badge {
            background: rgba(102, 126, 234, 0.2);
            color: #667eea;
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 10px;
            font-weight: 500;
            flex-shrink: 0;
            margin-left: 8px;
          }
        }
      }
    }
  }
}

.no-selection {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666666;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    color: #444444;
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #888888;
  }

  p {
    margin: 0;
    font-size: 14px;
    text-align: center;
    max-width: 300px;
    line-height: 1.5;
  }
}

/* 滚动到底部按钮 */
.scroll-to-bottom-btn {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(74, 144, 226, 0.9);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 100;

  &:hover {
    background: rgba(74, 144, 226, 1);
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

/* 滚动按钮过渡动画 */
.scroll-button-enter-active,
.scroll-button-leave-active {
  transition: all 0.3s ease;
}

.scroll-button-enter-from,
.scroll-button-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}
</style>
