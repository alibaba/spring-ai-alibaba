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
        <!-- Step Execution Details tab -->
        <div
            class="tab-item"
            :class="{ active: activeTab === 'details' }"
            @click="activeTab = 'details'"
        >
          <Icon icon="carbon:events"/>
          <span>{{ t('rightPanel.stepExecutionDetails') }}</span>
        </div>
        <!-- File Browser tab -->
        <div
            class="tab-item"
            :class="{ active: activeTab === 'files' }"
            @click="activeTab = 'files'"
        >
          <Icon icon="carbon:folder"/>
          <span>{{ t('fileBrowser.title') }}</span>
        </div>
      </div>
    </div>

    <div class="preview-content">
      <!-- Step Execution Details -->
      <div v-if="activeTab === 'details'" class="step-details">
        <!-- Fixed top step basic information -->
        <div v-if="selectedStep" class="step-info-fixed">
          <h3>
            {{
              selectedStep.title ||
              selectedStep.description ||
              t('rightPanel.defaultStepTitle', { number: selectedStep.index + 1 })
            }}
          </h3>

          <div class="agent-info" v-if="selectedStep.agentExecution">
            <div class="info-item">
              <span class="label">{{ t('rightPanel.executingAgent') }}:</span>
              <span class="value">{{ selectedStep.agentExecution.agentName }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('rightPanel.description') }}:</span>
              <span class="value">{{
                  selectedStep.agentExecution.agentDescription || ''
                }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('rightPanel.callingModel') }}:</span>
              <span class="value">{{ selectedStep.agentExecution.modelName }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('rightPanel.request') }}:</span>
              <span class="value">{{
                  selectedStep.agentExecution.agentRequest || ''
                }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('rightPanel.executionResult') }}:</span>
              <span
                  class="value"
                  :class="{ success: selectedStep.agentExecution.status === 'FINISHED' }"
              >
                {{ selectedStep.agentExecution.status || t('rightPanel.executing') }}
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
              <Icon icon="carbon:time" v-else class="status-icon pending"/>
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
                v-if="selectedStep.agentExecution?.thinkActSteps && selectedStep.agentExecution.thinkActSteps.length > 0"
            >
              <h4>{{ t('rightPanel.thinkAndActionSteps') }}</h4>
              <div class="steps-container">
                <div
                    v-for="(tas, index) in selectedStep.agentExecution.thinkActSteps"
                    :key="index"
                    class="think-act-step"
                >
                  <div class="step-header">
                    <span class="step-number">#{{ index + 1 }}</span>
                    <span class="step-status" :class="tas.status">{{
                        tas.status || t('rightPanel.executing')
                      }}</span>
                  </div>

                  <!-- Think section - strictly follow right-sidebar.js logic -->
                  <div class="think-section">
                    <h5>
                      <Icon icon="carbon:thinking"/>
                      {{ t('rightPanel.thinking') }}
                    </h5>
                    <div class="think-content">
                      <div class="input">
                        <span class="label">{{ t('rightPanel.input') }}:</span>
                        <pre>{{ formatJson(tas.thinkInput) }}</pre>
                      </div>
                      <div class="output">
                        <span class="label">{{ t('rightPanel.output') }}:</span>
                        <pre>{{ formatJson(tas.thinkOutput) }}</pre>
                      </div>
                    </div>
                  </div>

                  <!-- Action section - strictly follow right-sidebar.js logic -->
                  <div v-if="tas.actionNeeded" class="action-section">
                    <h5>
                      <Icon icon="carbon:play"/>
                      {{ t('rightPanel.action') }}
                    </h5>
                    <div class="action-content">
                      <div v-for="(actToolInfo, index) in tas.actToolInfoList" :key="index">
                        <div class="tool-info">
                          <span class="label">{{ t('rightPanel.tool') }}:</span>
                          <span class="value">{{ actToolInfo.name || '' }}</span>
                        </div>
                        <div class="input">
                          <span class="label">{{ t('rightPanel.toolParameters') }}:</span>
                          <pre>{{ formatJson(actToolInfo.parameters) }}</pre>
                        </div>
                        <div class="output">
                          <span class="label">{{ t('rightPanel.executionResult') }}:</span>
                          <pre>{{ formatJson(actToolInfo.result) }}</pre>
                        </div>
                      </div>
                    </div>

                    <!-- Sub execution plan section - new feature -->
                    <div v-if="tas.subPlanExecutionRecord" class="sub-plan-section">
                      <h5>
                        <Icon icon="carbon:tree-view"/>
                        {{ t('rightPanel.subPlan') }}
                      </h5>
                      <div class="sub-plan-content">
                        <div class="sub-plan-header">
                          <div class="sub-plan-info">
                            <span class="label">{{ $t('rightPanel.subPlanId') }}:</span>
                            <span class="value">{{ tas.subPlanExecutionRecord.currentPlanId }}</span>
                          </div>
                          <div class="sub-plan-info" v-if="tas.subPlanExecutionRecord.title">
                            <span class="label">{{ $t('rightPanel.title') }}:</span>
                            <span class="value">{{ tas.subPlanExecutionRecord.title }}</span>
                          </div>
                          <div class="sub-plan-status">
                            <Icon
                                icon="carbon:checkmark-filled"
                                v-if="tas.subPlanExecutionRecord.completed"
                                class="status-icon success"
                            />
                            <Icon icon="carbon:in-progress" v-else class="status-icon progress"/>
                            <span class="status-text">
                            {{ tas.subPlanExecutionRecord.completed ? $t('rightPanel.status.completed') : $t('rightPanel.status.executing') }}
                          </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div
                  v-if="
                selectedStep.agentExecution &&
                !selectedStep.agentExecution.thinkActSteps?.length
              "
                  class="no-steps-message"
              >
                <p>{{ t('rightPanel.noStepDetails') }}</p>
              </div>

              <!-- Handle no agentExecution case -->
              <div
                  v-else-if="!selectedStep.agentExecution"
                  class="no-execution-message"
              >
                <Icon icon="carbon:information" class="info-icon"/>
                <h4>{{ t('rightPanel.stepInfo') }}</h4>
                <div class="step-basic-info">
                  <div class="info-item">
                    <span class="label">{{ t('rightPanel.stepName') }}:</span>
                    <span class="value">{{
                        selectedStep.title ||
                        selectedStep.description ||
                        $t('rightPanel.stepNumber', { number: selectedStep.index + 1 })
                      }}</span>
                  </div>
                  <div class="info-item" v-if="selectedStep.description">
                    <span class="label">{{ $t('rightPanel.description') }}:</span>
                    <span class="value">{{ selectedStep.description }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">{{ $t('rightPanel.status.label') }}:</span>
                    <span class="value" :class="{
                    'status-completed': selectedStep.completed,
                    'status-current': selectedStep.current,
                    'status-pending': !selectedStep.completed && !selectedStep.current
                  }">
                    {{
                        selectedStep.completed ? $t('rightPanel.status.completed') :
                            selectedStep.current ? $t('rightPanel.status.executing') : $t('rightPanel.status.pending')
                      }}
                  </span>
                  </div>
                </div>
                <p class="no-execution-hint">{{ t('rightPanel.noExecutionInfo') }}</p>
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
                  <Icon icon="carbon:in-progress" class="rotating-icon"/>
                  {{ t('rightPanel.stepExecuting') }}
                </p>
              </div>
            </div>

            <div v-else class="no-selection">
              <Icon icon="carbon:events" class="empty-icon"/>
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
                :title="t('rightPanel.scrollToBottom')"
            >
              <Icon icon="carbon:chevron-down"/>
            </button>
          </Transition>
        </div>
      </div>

      <!-- File Browser -->
      <div v-if="activeTab === 'files'" class="file-browser-container">
        <FileBrowser 
          v-if="fileBrowserPlanId" 
          :plan-id="fileBrowserPlanId"
        />
        <div v-else-if="shouldShowNoTaskMessage" class="no-plan-message">
          <Icon icon="carbon:folder-off" />
          <div class="message-content">
            <h3>{{ t('fileBrowser.noFilesYet') }}</h3>
            <p>{{ t('fileBrowser.noPlanExecuting') }}</p>
            <div class="tips">
              <Icon icon="carbon:information" />
              <span>{{ t('fileBrowser.startTaskTip') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'
import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'
import FileBrowser from '@/components/file-browser/index.vue'

// Define props interface
interface Props {
  currentRootPlanId?: string | null
}

const props = defineProps<Props>()

// Define step selection context interface
interface StepSelectionContext {
  planId: string
  stepIndex: number
  rootPlanId?: string    // For sub-plan steps, reference to root plan
  subPlanId?: string     // For sub-plan steps
  subStepIndex?: number  // For sub-plan steps
  isSubPlan: boolean     // Flag to indicate if this is a sub-plan step
}

// Define selected step interface
interface SelectedStep {
  planId: string
  index: number
  title: string
  description: string
  agentExecution?: AgentExecutionRecord
  completed: boolean
  current: boolean
}

const { t } = useI18n()

// DOM element reference
const scrollContainer = ref<HTMLElement>()

// Local state - replacing store state
const currentDisplayedPlanId = ref<string>()
const selectedStep = ref<SelectedStep | null>()
const activeTab = ref<'details' | 'files'>('details')

// Keep track of the last executed plan for file browser
const lastExecutedPlanId = ref<string | null>(localStorage.getItem('jmanus-last-plan-id'))
const hasExecutedAnyPlan = ref(localStorage.getItem('jmanus-has-executed-plan') === 'true')

// Current step selection context for auto-refresh
const currentStepContext = ref<StepSelectionContext | null>(null)

// Scroll-related state
const showScrollToBottomButton = ref(false)
const isNearBottom = ref(true)
const shouldAutoScrollToBottom = ref(true)


const stepStatusText = computed(() => {
  if (!selectedStep.value) return ''
  if (selectedStep.value.completed) return t('rightPanel.status.completed')
  if (selectedStep.value.current) return t('rightPanel.status.executing')
  return t('rightPanel.status.waiting')
})

// Computed property to determine which planId to show in file browser
const fileBrowserPlanId = computed(() => {
  // If there's a current plan, use it
  if (props.currentRootPlanId) {
    return props.currentRootPlanId
  }
  // Otherwise, use the last executed plan if any
  return lastExecutedPlanId.value
})

// Computed property to determine if we should show the "no task" message
const shouldShowNoTaskMessage = computed(() => {
  return !fileBrowserPlanId.value && !hasExecutedAnyPlan.value
})

// Actions - Plan data management and refresh control
/**
 * Primary method for external refresh control of the right panel component.
 *
 * USAGE:
 * This method should be called by external components (typically Direct component)
 * to trigger a refresh of the plan progress display and step details. It serves as
 * the main entry point for updating the component's state based on plan execution changes.
 *
 * REFRESH LOGIC:
 * 1. Validates if the provided rootPlanId matches the currently displayed plan
 * 2. Updates plan progress information (current step vs total steps)
 * 3. If a step is currently selected and belongs to the provided rootPlanId:
 *    - Re-fetches the latest plan execution data
 *    - Refreshes the step details display with updated information
 *    - Auto-scrolls to show latest content if user was previously at bottom
 *
 * KEY DESIGN PRINCIPLE:
 * This component does NOT maintain internal auto-refresh timers. Instead, it relies
 * entirely on external calls to this method for updates. This allows parent components
 * to have full control over when and how frequently the refresh occurs.
 *
 * CONSISTENCY CHECK:
 * Only updates are allowed for the currently displayed plan to prevent conflicting updates.
 *
 * ================================================================================
 *
 * Main method for external refresh control of the right panel component.
 *
 * Usage:
 * This method should be called by external components (usually the Direct component) to trigger
 * refresh of plan progress display and step details.
 * It serves as the main entry point for updating component state based on plan execution changes.
 *
 * Refresh logic:
 * 1. Verify that the provided rootPlanId matches the currently displayed plan
 * 2. Update plan progress information (current step vs total steps)
 * 3. If there is a currently selected step belonging to the provided rootPlanId:
 *    - Re-fetch the latest plan execution data
 *    - Refresh step details display and update information
 *    - Auto-scroll to show latest content if user was previously at bottom
 *
 * Key design principle:
 * This component does not maintain internal auto-refresh timers. Instead, it relies entirely
 * on external calls to this method for updates.
 * This allows parent components to have complete control over when and how frequently to refresh.
 *
 * Consistency check:
 * Only allows updates to the currently displayed plan to prevent conflicting update operations.
 *
 * @param rootPlanId - The ID of the root plan execution to refresh
 */
const updateDisplayedPlanProgress = (rootPlanId: string) => {
  console.log(`[RightPanel] updateDisplayedPlanProgress called with rootPlanId: ${rootPlanId}`)

  // Check if there's a currently selected step and validate plan consistency
  if (selectedStep.value && currentStepContext.value) {
    // Determine the current root plan ID based on step context
    const currentRootPlanId = currentStepContext.value.rootPlanId ?? currentDisplayedPlanId.value

    // Only update if the provided rootPlanId matches the currently displayed plan
    if (currentRootPlanId && currentRootPlanId !== rootPlanId) {
      console.log(`[RightPanel] Plan ID mismatch - skipping update. Current: ${currentRootPlanId}, Requested: ${rootPlanId}`)
      return
    }
  }

  console.log(`[RightPanel] Plan ID validation passed - proceeding with update for rootPlanId: ${rootPlanId}`)

  // Get plan data from plan execution manager
  const planData = planExecutionManager.getCachedPlanRecord(rootPlanId)
  if (!planData) {
    console.warn(`[RightPanel] Plan data not found for rootPlanId: ${rootPlanId}`)
    return
  }

  // Update UI state, such as progress bars
  if (planData.steps && planData.steps.length > 0) {
    const totalSteps = planData.steps.length
    const currentStep = (planData.currentStepIndex ?? 0) + 1
    console.log(`[RightPanel] Progress: ${currentStep} / ${totalSteps}`)
  }

  // If there's a selected step and it belongs to the current plan, refresh its details
  if (selectedStep.value && currentDisplayedPlanId.value) {
    // Check if the selected step belongs to the current root plan
    const isCurrentPlanStep = currentDisplayedPlanId.value === rootPlanId ||
                             (currentStepContext.value?.rootPlanId === rootPlanId)

    if (isCurrentPlanStep) {
      console.log(`[RightPanel] Refreshing selected step details for plan: ${rootPlanId}`)

      // Get the current step context
      if (currentStepContext.value) {
        const ctx = currentStepContext.value

        // Re-fetch and update the step details
        const planRecord = findPlanExecutionRecord(ctx.planId, ctx.rootPlanId, ctx.subPlanId)
        if (planRecord) {
          displayStepDetails(planRecord, ctx.stepIndex, ctx.planId, ctx.isSubPlan)
          // Auto-scroll to latest content if previously at bottom
          autoScrollToBottomIfNeeded()
        } else {
          console.warn(`[RightPanel] Could not find plan record for refresh:`, ctx)
        }
      }
    }
  }
}

// Actions - Step selection handling (callable methods for external components)
/**
 * Unified step selection handler - handles both regular steps and sub-plan steps
 * @param planId - The plan ID to display (can be main plan or sub-plan)
 * @param stepIndex - The step index to display
 * @param rootPlanId - Optional root plan ID for sub-plan steps
 * @param subPlanId - Optional sub-plan ID for sub-plan steps
 * @param subStepIndex - Optional sub-step index for sub-plan steps
 */
const handleStepSelected = (
  planId: string,
  stepIndex: number,
  rootPlanId?: string,
  subPlanId?: string,
  subStepIndex?: number
) => {
  console.log('[RightPanel] Step selected:', {
    planId,
    stepIndex,
    rootPlanId,
    subPlanId,
    subStepIndex
  })

  const isSubPlan = !!(rootPlanId && subPlanId && subStepIndex !== undefined)

  // Set current step context for auto-refresh
  currentStepContext.value = {
    planId,
    stepIndex,
    isSubPlan,
    ...(isSubPlan && { rootPlanId, subPlanId, subStepIndex })
  }

  // Find the PlanExecutionRecord based on parameters
  const planRecord = findPlanExecutionRecord(planId, rootPlanId, subPlanId)

  if (!planRecord) {
    console.warn('[RightPanel] Plan data not found:', { planId, rootPlanId, subPlanId })
    selectedStep.value = null
    currentStepContext.value = null
    return
  }

  // Display step details using unified logic
  displayStepDetails(planRecord, stepIndex, planId, isSubPlan)
}

/**
 * Find PlanExecutionRecord based on parameters
 * For regular steps: directly get from planExecutionManager
 * For sub-plan steps: traverse root plan to find the sub-plan record
 */
const findPlanExecutionRecord = (
  planId: string,
  rootPlanId?: string,
  subPlanId?: string
): PlanExecutionRecord | null => {
  // For regular steps, directly get from manager
  if (!rootPlanId || !subPlanId) {
    return planExecutionManager.getCachedPlanRecord(planId) ?? null
  }

  // For sub-plan steps, first try direct access
  const directRecord = planExecutionManager.getCachedPlanRecord(planId)
  if (directRecord) {
    return directRecord
  }

  // If direct access fails, traverse root plan to find sub-plan
  const rootPlanData = planExecutionManager.getCachedPlanRecord(rootPlanId)
  if (!rootPlanData?.agentExecutionSequence) {
    return null
  }

  // Traverse all agent execution records to find the sub-plan
  for (const agentExecution of rootPlanData.agentExecutionSequence) {
    if (agentExecution.thinkActSteps) {
      for (const thinkActStep of agentExecution.thinkActSteps) {
        if (thinkActStep.subPlanExecutionRecord?.currentPlanId === subPlanId) {
          return thinkActStep.subPlanExecutionRecord
        }
      }
    }
  }

  return null
}

/**
 * Unified display logic for step details
 */
const displayStepDetails = (
  planRecord: PlanExecutionRecord,
  stepIndex: number,
  planId: string,
  isSubPlan: boolean
) => {
  // Validate step index
  if (!planRecord.steps || stepIndex >= planRecord.steps.length) {
    selectedStep.value = null
    currentStepContext.value = null
    console.warn('[RightPanel] Invalid step data:', {
      planId,
      stepIndex,
      hasSteps: !!planRecord.steps,
      stepsLength: planRecord.steps?.length,
      message: 'Invalid step index'
    })
    return
  }

  currentDisplayedPlanId.value = planId
  const step = planRecord.steps[stepIndex]
  const agentExecution = planRecord.agentExecutionSequence?.[stepIndex]

  console.log('[RightPanel] Step data details:', {
    planId,
    stepIndex,
    step,
    hasAgentExecutionSequence: !!planRecord.agentExecutionSequence,
    agentExecutionSequenceLength: planRecord.agentExecutionSequence?.length,
    agentExecution,
    hasThinkActSteps: !!agentExecution?.thinkActSteps,
    thinkActStepsLength: agentExecution?.thinkActSteps?.length,
    isSubPlan
  })

  // Determine if step is completed
  const isStepCompleted =
    agentExecution?.status === 'FINISHED' ;

  const isCurrent =
    !isStepCompleted && stepIndex === planRecord.currentStepIndex && !planRecord.completed

  // Construct step details object
  const stepData: SelectedStep = {
    planId: planId,
    index: stepIndex,
    title: typeof step === 'string'
      ? step
      : (step as any).title || (step as any).description || (step as any).name ||
        `${isSubPlan ? 'Sub ' : ''}Step ${stepIndex + 1}`,
    description: typeof step === 'string' ? step : (step as any).description || step,
    completed: isStepCompleted,
    current: isCurrent,
  }

  if (agentExecution) {
    stepData.agentExecution = agentExecution
  }

  selectedStep.value = stepData

  console.log('[RightPanel] Step details updated:', {
    planId,
    stepIndex,
    stepTitle: selectedStep.value.title,
    hasAgentExecution: !!agentExecution,
    hasThinkActSteps: (agentExecution?.thinkActSteps?.length ?? 0) > 0,
    completed: isStepCompleted,
    current: isCurrent,
    planCurrentStep: planRecord.currentStepIndex,
    planCompleted: planRecord.completed,
    isSubPlan
  })

  // Process sub-plan data if exists - just log for debugging
  if (agentExecution?.thinkActSteps) {
    agentExecution.thinkActSteps.forEach((thinkActStep: any, index: number) => {
      if (thinkActStep.subPlanExecutionRecord) {
        console.log(`[RightPanel] Found sub-plan in thinkActStep ${index}:`, thinkActStep.subPlanExecutionRecord)
      }
    })
  }

  // Note: Auto-refresh is now controlled externally via updateDisplayedPlanProgress
  // No internal auto-refresh logic needed

  // Delay scroll state check to ensure DOM is updated
  setTimeout(() => {
    checkScrollState()
  }, 100)

  // After data update, auto-scroll to latest content if previously at bottom
  autoScrollToBottomIfNeeded()
}

/**
 * Handle sub-plan step selection - wrapper for backward compatibility
 * @param rootPlanId - The root plan ID
 * @param subPlanId - The sub-plan ID to display
 * @param stepIndex - The parent step index (not used in new logic but kept for compatibility)
 * @param subStepIndex - The sub-step index to display
 */
const handleSubPlanStepSelected = (rootPlanId: string, subPlanId: string, stepIndex: number, subStepIndex: number) => {
  console.log('[RightPanel] Sub plan step selected (delegating to unified handler):', {
    rootPlanId,
    subPlanId,
    stepIndex,
    subStepIndex
  })

  // Delegate to unified handler
  handleStepSelected(subPlanId, subStepIndex, rootPlanId, subPlanId, subStepIndex)
}

// Actions - Scroll management
const setScrollContainer = (element: HTMLElement | null) => {
  scrollContainer.value = element ?? undefined
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


// Actions - Utility functions
const formatJson = (jsonData: any): string => {
  if (jsonData === null || typeof jsonData === 'undefined' || jsonData === '') {
    return 'N/A'
  }
  try {
    const jsonObj = typeof jsonData === 'object' ? jsonData : JSON.parse(jsonData)
    return JSON.stringify(jsonObj, null, 2)
  } catch {
    // If parsing fails, return string format directly (similar to _escapeHtml in right-sidebar.js)
    return String(jsonData)
  }
}

// Actions - Resource cleanup
const cleanup = () => {
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

// Watch for currentRootPlanId changes to track execution history
watch(() => props.currentRootPlanId, (newPlanId, oldPlanId) => {
  if (newPlanId && newPlanId !== oldPlanId) {
    // A new plan has started executing
    lastExecutedPlanId.value = newPlanId
    hasExecutedAnyPlan.value = true
    
    // Persist to localStorage
    localStorage.setItem('jmanus-last-plan-id', newPlanId)
    localStorage.setItem('jmanus-has-executed-plan', 'true')
    
    console.log('[RightPanel] New plan started:', newPlanId)
  } else if (!newPlanId && oldPlanId) {
    // Plan execution finished, but keep the lastExecutedPlanId for file browser
    console.log('[RightPanel] Plan execution finished, keeping last plan:', lastExecutedPlanId.value)
  }
}, { immediate: true })

// Lifecycle - initialization on mount
onMounted(() => {
  console.log('[RightPanel] Component mounted')
  // Use nextTick to ensure DOM is rendered
  nextTick(() => {
    initScrollListener()
  })
})

// Lifecycle - cleanup on unmount
onUnmounted(() => {
  console.log('[RightPanel] Component unmounting, cleaning up...')
  currentStepContext.value = null // Clear step context
  cleanup()
})

// Expose methods to parent component - only keep necessary interfaces
defineExpose({
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
  background: rgba(255, 255, 255, 0.02);

  .tab-button {
    padding: 8px 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
    border-color: #667eea;
    color: #667eea;
    cursor: default;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
  }
}


.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* Ensure flex items can shrink */
}

/* Step details styles */
.step-details {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0; /* Ensure flex items can shrink */
}

/* Fixed step basic information at top */
.step-info-fixed {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(41, 42, 45, 0.95); /* Semi-transparent background with some transparency */
  backdrop-filter: blur(10px); /* Background blur effect */
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
  padding: 0 20px 20px; /* Remove top padding since fixed header already has padding */
  margin: 0 20px 20px;
  background: rgba(255, 255, 255, 0.01);
  border-radius: 0 0 8px 8px; /* Adjust border radius to match fixed header */

  /* Custom scrollbar styles */
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

/* Step information styles - for fixed top */
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
  margin-top: 20px; /* Increase top spacing since there's no fixed header step info now */

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

  /* Sub plan styles */
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

/* Scroll to bottom button */
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

/* Scroll button transition animation */
.scroll-button-enter-active,
.scroll-button-leave-active {
  transition: all 0.3s ease;
}

.scroll-button-enter-from,
.scroll-button-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

/* File Browser Container */
.file-browser-container {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.no-plan-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.6);
  gap: 24px;
  padding: 40px 20px;
  text-align: center;
}

.no-plan-message > .iconify {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.3);
}

.message-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: 300px;
}

.message-content h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.message-content p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.6);
}

.tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(103, 126, 234, 0.1);
  border: 1px solid rgba(103, 126, 234, 0.2);
  border-radius: 8px;
  font-size: 12px;
  color: rgba(103, 126, 234, 0.9);
}

.tips .iconify {
  font-size: 14px;
  flex-shrink: 0;
}

/* Tab styles */
.preview-tabs {
  display: flex;
  gap: 0;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 4px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 500;
  min-width: 0;
  flex: 1;
  justify-content: center;
  position: relative;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.9);
}

.tab-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.tab-item .iconify {
  font-size: 16px;
  flex-shrink: 0;
}

.tab-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
