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

import { defineStore } from 'pinia'
import { ref, reactive, computed, nextTick } from 'vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'

export const useRightPanelStore = defineStore('rightPanel', () => {
  // Basic state
  const activeTab = ref('details')

  // Plan data mapping (similar to planDataMap in right-sidebar.js)
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
  const codeContent = ref(`// Generated Spring Boot REST API
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        User updatedUser = userService.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}`)

  const codeLanguage = ref('java')

  const chatBubbles = ref([
    {
      id: '1',
      type: 'thinking',
      icon: 'carbon:thinking',
      title: '分析需求',
      content:
        '将您的请求分解为可操作的步骤：1) 创建用户实体，2) 实现用户服务，3) 构建 REST 端点，4) 添加验证和错误处理。',
      timestamp: '2 分钟前',
    },
    {
      id: '2',
      type: 'progress',
      icon: 'carbon:in-progress',
      title: '生成代码',
      content:
        '创建具有用户管理 CRUD 操作的 Spring Boot REST API。包括正确的 HTTP 状态代码和错误处理。',
      timestamp: '1 分钟前',
    },
    {
      id: '3',
      type: 'success',
      icon: 'carbon:checkmark',
      title: '代码已生成',
      content:
        '成功生成具有所有 CRUD 操作的 UserController。代码包含正确的 REST 约定、错误处理，并遵循 Spring Boot 最佳实践。',
      timestamp: '刚刚',
    },
  ])

  // Preview tab configuration
  const previewTabs = [
    { id: 'details', name: '步骤执行详情', icon: 'carbon:events' },
    { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
    { id: 'code', name: 'Code', icon: 'carbon:code' },
  ]

  // DOM element reference (needs to be set in component)
  const scrollContainer = ref<HTMLElement>()

  // Computed properties
  const currentPlan = computed(() => {
    if (!currentDisplayedPlanId.value) return null
    return planDataMap.value.get(currentDisplayedPlanId.value)
  })

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
  const handlePlanUpdate = async (planData: any) => {
    console.log('[RightPanelStore] Received plan update event, planData:', planData)

    // Validate data validity
    if (!planData || !planData.planId) {
      console.warn('[RightPanelStore] Invalid plan data received:', planData)
      return
    }

    // Update plan data to local mapping
    planDataMap.value.set(planData.planId, planData)
    console.log('[RightPanelStore] Plan data updated to planDataMap:', planData.planId)

    // If currently selected step corresponds to this plan, reload step details
    if (selectedStep.value?.planId === planData.planId) {
      console.log('[RightPanelStore] Refresh details of currently selected step:', selectedStep.value.index)
      showStepDetails(planData.planId, selectedStep.value.index)
    }

    // After data update, auto-scroll to latest content if previously at bottom
    autoScrollToBottomIfNeeded()
  }

  const updateDisplayedPlanProgress = (planData: any) => {
    // Here you can update UI state, such as progress bars
    if (planData.steps && planData.steps.length > 0) {
      const totalSteps = planData.steps.length
      const currentStep = (planData.currentStepIndex || 0) + 1
      console.log(`[RightPanelStore] Progress: ${currentStep} / ${totalSteps}`)
    }
  }

  // Actions - Step details management
  const showStepDetails = (planId: string, stepIndex: number) => {
    console.log('[RightPanelStore] Show step details:', { planId, stepIndex })

    const planData = planDataMap.value.get(planId)

    if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
      selectedStep.value = null
      console.log('[RightPanelStore] Invalid step data:', {
        planId,
        stepIndex,
        hasSteps: !!planData?.steps,
      })
      stopAutoRefresh() // Stop auto refresh
      return
    }

    currentDisplayedPlanId.value = planId
    const step = planData.steps[stepIndex]
    const agentExecution =
      planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex]

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

    console.log('[RightPanelStore] Step details updated:', {
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

  const clearSelectedStep = () => {
    selectedStep.value = null
    currentDisplayedPlanId.value = undefined
    stopAutoRefresh()
  }

  // Actions - Auto refresh management
  const startAutoRefresh = (planId: string, stepIndex: number) => {
    console.log('[RightPanelStore] Start auto refresh:', { planId, stepIndex })

    // Stop previous refresh
    stopAutoRefresh()

    autoRefreshTimer.value = window.setInterval(() => {
      console.log('[RightPanelStore] Execute auto refresh - Step details')

      // Check if plan is still executing
      const planData = planDataMap.value.get(planId)
      if (!planData || planData.completed) {
        console.log('[RightPanelStore] Plan completed, stop auto refresh')
        stopAutoRefresh()
        return
      }

      // Check if step is still executing
      const agentExecution = planData.agentExecutionSequence?.[stepIndex]
      if (agentExecution?.isCompleted) {
        console.log('[RightPanelStore] Step completed, stop auto refresh')
        stopAutoRefresh()
        return
      }

      // Check if already moved to next step
      const currentStepIndex = planData.currentStepIndex ?? 0
      if (stepIndex < currentStepIndex) {
        console.log('[RightPanelStore] Already moved to next step, stop auto refresh')
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
      console.log('[RightPanelStore] Auto refresh stopped')
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

    console.log('[RightPanelStore] Scroll state check:', {
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
        console.log('[RightPanelStore] Auto scroll to bottom')
      }
    })
  }

  // Actions - Code operations
  const copyCode = () => {
    navigator.clipboard.writeText(codeContent.value)
  }

  const downloadCode = () => {
    const blob = new Blob([codeContent.value], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'UserController.java'
    a.click()
    URL.revokeObjectURL(url)
  }

  // Actions - Data cleanup
  const clearPlanData = () => {
    planDataMap.value.clear()
    selectedStep.value = null
    currentDisplayedPlanId.value = undefined
    stopAutoRefresh()
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

  return {
    // State
    activeTab,
    planDataMap,
    currentDisplayedPlanId,
    selectedStep,
    showScrollToBottomButton,
    isNearBottom,
    shouldAutoScrollToBottom,
    autoRefreshTimer,
    codeContent,
    codeLanguage,
    chatBubbles,
    previewTabs,
    scrollContainer,

    // Computed properties
    currentPlan,
    stepStatusText,

    // Actions
    switchTab,
    handlePlanUpdate,
    updateDisplayedPlanProgress,
    showStepDetails,
    clearSelectedStep,
    startAutoRefresh,
    stopAutoRefresh,
    setScrollContainer,
    checkScrollState,
    scrollToBottom,
    autoScrollToBottomIfNeeded,
    copyCode,
    downloadCode,
    clearPlanData,
    formatJson,
    cleanup,
  }
})
