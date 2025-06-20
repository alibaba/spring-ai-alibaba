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
  // 基本状态
  const activeTab = ref('details')

  // 计划数据映射 (类似 right-sidebar.js 的 planDataMap)
  const planDataMap = ref<Map<string, any>>(new Map())
  const currentDisplayedPlanId = ref<string>()
  const selectedStep = ref<any>()

  // 滚动相关状态
  const showScrollToBottomButton = ref(false)
  const isNearBottom = ref(true)
  const shouldAutoScrollToBottom = ref(true)

  // 自动刷新相关状态
  const autoRefreshTimer = ref<number | null>(null)
  const AUTO_REFRESH_INTERVAL = 3000 // 3秒刷新一次步骤详情

  // 代码和聊天预览相关状态
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

  // 预览标签页配置
  const previewTabs = [
    { id: 'details', name: '步骤执行详情', icon: 'carbon:events' },
    { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
    { id: 'code', name: 'Code', icon: 'carbon:code' },
  ]

  // DOM 元素引用（需要在组件中设置）
  const scrollContainer = ref<HTMLElement>()

  // 计算属性
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

  // Actions - 标签页管理
  const switchTab = (tabId: string) => {
    activeTab.value = tabId
  }

  // Actions - 计划数据管理
  const handlePlanUpdate = async (planData: any) => {
    console.log('[RightPanelStore] 收到计划更新事件, planData:', planData)

    // 验证数据有效性
    if (!planData || !planData.planId) {
      console.warn('[RightPanelStore] Invalid plan data received:', planData)
      return
    }

    // 更新计划数据到本地映射
    planDataMap.value.set(planData.planId, planData)
    console.log('[RightPanelStore] 计划数据已更新到 planDataMap:', planData.planId)

    // 如果当前选中的步骤对应这个计划，重新加载步骤详情
    if (selectedStep.value?.planId === planData.planId) {
      console.log('[RightPanelStore] 刷新当前选中步骤的详情:', selectedStep.value.index)
      showStepDetails(planData.planId, selectedStep.value.index)
    }

    // 数据更新后，如果之前在底部则自动滚动到最新内容
    autoScrollToBottomIfNeeded()
  }

  const updateDisplayedPlanProgress = (planData: any) => {
    // 这里可以更新UI状态，比如进度条等
    if (planData.steps && planData.steps.length > 0) {
      const totalSteps = planData.steps.length
      const currentStep = (planData.currentStepIndex || 0) + 1
      console.log(`[RightPanelStore] 进度: ${currentStep} / ${totalSteps}`)
    }
  }

  // Actions - 步骤详情管理
  const showStepDetails = (planId: string, stepIndex: number) => {
    console.log('[RightPanelStore] 显示步骤详情:', { planId, stepIndex })

    const planData = planDataMap.value.get(planId)

    if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
      selectedStep.value = null
      console.log('[RightPanelStore] Invalid step data:', {
        planId,
        stepIndex,
        hasSteps: !!planData?.steps,
      })
      stopAutoRefresh() // 停止自动刷新
      return
    }

    currentDisplayedPlanId.value = planId
    const step = planData.steps[stepIndex]
    const agentExecution =
      planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex]

    // 判断步骤是否完成 - 多重条件判断
    const isStepCompleted =
      agentExecution?.isCompleted ||
      planData.completed ||
      (planData.currentStepIndex !== undefined && stepIndex < planData.currentStepIndex)

    const isCurrent =
      !isStepCompleted && stepIndex === planData.currentStepIndex && !planData.completed

    // 构造步骤详情对象，类似 right-sidebar.js 的逻辑
    selectedStep.value = {
      planId: planId, // 确保包含planId
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

    // 如果步骤未完成且计划还在执行中，启动自动刷新
    if (
      !isStepCompleted &&
      !planData.completed &&
      planExecutionManager.getActivePlanId() === planId
    ) {
      startAutoRefresh(planId, stepIndex)
    } else {
      stopAutoRefresh()
    }

    // 延迟检查滚动状态，确保DOM已更新
    setTimeout(() => {
      checkScrollState()
    }, 100)

    // 数据更新后，如果之前在底部则自动滚动到最新内容
    autoScrollToBottomIfNeeded()
  }

  const clearSelectedStep = () => {
    selectedStep.value = null
    currentDisplayedPlanId.value = undefined
    stopAutoRefresh()
  }

  // Actions - 自动刷新管理
  const startAutoRefresh = (planId: string, stepIndex: number) => {
    console.log('[RightPanelStore] 启动自动刷新:', { planId, stepIndex })

    // 停止之前的刷新
    stopAutoRefresh()

    autoRefreshTimer.value = window.setInterval(() => {
      console.log('[RightPanelStore] 执行自动刷新 - Step details')

      // 检查计划是否还在执行
      const planData = planDataMap.value.get(planId)
      if (!planData || planData.completed) {
        console.log('[RightPanelStore] 计划已完成，停止自动刷新')
        stopAutoRefresh()
        return
      }

      // 检查步骤是否还在执行
      const agentExecution = planData.agentExecutionSequence?.[stepIndex]
      if (agentExecution?.isCompleted) {
        console.log('[RightPanelStore] 步骤已完成，停止自动刷新')
        stopAutoRefresh()
        return
      }

      // 检查是否已经进入下一步
      const currentStepIndex = planData.currentStepIndex ?? 0
      if (stepIndex < currentStepIndex) {
        console.log('[RightPanelStore] 已进入下一步，停止自动刷新')
        stopAutoRefresh()
        return
      }

      // 刷新步骤详情
      showStepDetails(planId, stepIndex)

      // 数据更新后，如果之前在底部则自动滚动到最新内容
      autoScrollToBottomIfNeeded()
    }, AUTO_REFRESH_INTERVAL)
  }

  const stopAutoRefresh = () => {
    if (autoRefreshTimer.value) {
      clearInterval(autoRefreshTimer.value)
      autoRefreshTimer.value = null
      console.log('[RightPanelStore] 自动刷新已停止')
    }
  }

  // Actions - 滚动管理
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

    // 更新自动滚动标记：如果用户滚动到底部，应该自动滚动
    // 如果用户主动向上滚动离开底部，则停止自动滚动
    if (isAtBottom) {
      shouldAutoScrollToBottom.value = true
    } else if (scrollHeight - scrollTop - clientHeight > 100) {
      // 如果用户明显向上滚动了（距离底部超过100px），则停止自动滚动
      shouldAutoScrollToBottom.value = false
    }

    console.log('[RightPanelStore] 滚动状态检查:', {
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

    // 滚动后重置状态
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
        console.log('[RightPanelStore] 自动滚动到底部')
      }
    })
  }

  // Actions - 代码操作
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

  // Actions - 数据清理
  const clearPlanData = () => {
    planDataMap.value.clear()
    selectedStep.value = null
    currentDisplayedPlanId.value = undefined
    stopAutoRefresh()
  }

  // Actions - 工具函数
  const formatJson = (jsonData: any): string => {
    if (jsonData === null || typeof jsonData === 'undefined' || jsonData === '') {
      return 'N/A'
    }
    try {
      const jsonObj = typeof jsonData === 'object' ? jsonData : JSON.parse(jsonData)
      return JSON.stringify(jsonObj, null, 2)
    } catch (e) {
      // 如果解析失败，直接返回字符串形式（类似 right-sidebar.js 的 _escapeHtml）
      return String(jsonData)
    }
  }

  // Actions - 清理资源
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
    // 状态
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

    // 计算属性
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
