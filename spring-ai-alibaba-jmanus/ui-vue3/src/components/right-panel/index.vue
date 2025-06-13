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
          @click="activeTab = tab.id"
        >
          <Icon :icon="tab.icon" />
          {{ tab.name }}
        </button>
        <!-- 临时注释掉 chat 和 code 按钮，但保留所有实现 -->
        <!-- 
        <button
          v-for="tab in previewTabs.filter(t => t.id === 'chat' || t.id === 'code')"
          :key="tab.id"
          class="tab-button"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <Icon :icon="tab.icon" />
          {{ tab.name }}
        </button>
        -->
      </div>
      <div class="preview-actions">
        <button class="action-button" @click="copyCode" v-if="activeTab === 'code'">
          <Icon icon="carbon:copy" />
        </button>
        <button class="action-button" @click="downloadCode" v-if="activeTab === 'code'">
          <Icon icon="carbon:download" />
        </button>
      </div>
    </div>

    <div class="preview-content">
      <!-- Code Preview -->
      <div v-if="activeTab === 'code'" class="code-preview">
        <MonacoEditor
          v-model="codeContent"
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
          <div
            v-for="bubble in chatBubbles"
            :key="bubble.id"
            class="chat-bubble"
            :class="bubble.type"
          >
            <div class="bubble-header">
              <Icon :icon="bubble.icon" />
              <span>{{ bubble.title }}</span>
              <span class="timestamp">{{ bubble.timestamp }}</span>
            </div>
            <div class="bubble-content">
              {{ bubble.content }}
            </div>
          </div>
        </div>
      </div>

      <!-- Step Execution Details -->
      <div v-else-if="activeTab === 'details'" class="step-details">
        <!-- 固定顶部的步骤基本信息 -->
        <div v-if="selectedStep" class="step-info-fixed">
          <h3>{{ selectedStep.title || selectedStep.description || `步骤 ${selectedStep.index + 1}` }}</h3>
          
          <div class="agent-info" v-if="selectedStep.agentExecution">
            <div class="info-item">
              <span class="label">执行智能体:</span>
              <span class="value">{{ selectedStep.agentExecution.agentName }}</span>
            </div>
            <div class="info-item">
              <span class="label">描述:</span>
              <span class="value">{{ selectedStep.agentExecution.agentDescription || '' }}</span>
            </div>
            <div class="info-item">
              <span class="label">请求:</span>
              <span class="value">{{ selectedStep.agentExecution.agentRequest || '' }}</span>
            </div>
            <div class="info-item">
              <span class="label">执行结果:</span>
              <span class="value" :class="{ success: selectedStep.agentExecution.isCompleted }">
                {{ selectedStep.agentExecution.result || '执行中...' }}
              </span>
            </div>
          </div>

          <div class="execution-status">
            <div class="status-item">
              <Icon icon="carbon:checkmark-filled" v-if="selectedStep.completed" class="status-icon success" />
              <Icon icon="carbon:in-progress" v-else-if="selectedStep.current" class="status-icon progress" />
              <Icon icon="carbon:time" v-else class="status-icon pending" />
              <span class="status-text">
                {{ getStepStatusText(selectedStep) }}
              </span>
            </div>
          </div>
        </div>
        
        <!-- 可滚动的详细内容区域 -->
        <div 
          ref="scrollContainer" 
          class="step-details-scroll-container"
          @scroll="handleScroll"
        >
          <div v-if="selectedStep">
            <!-- 思考与行动步骤 -->
            <div class="think-act-steps" v-if="selectedStep.agentExecution?.thinkActSteps?.length > 0">
              <h4>思考与行动步骤</h4>
              <div class="steps-container">
                <div 
                  v-for="(tas, index) in selectedStep.agentExecution.thinkActSteps"
                  :key="index"
                  class="think-act-step"
                >
                  <div class="step-header">
                    <span class="step-number">#{{ index + 1 }}</span>
                    <span class="step-status" :class="tas.status">{{ tas.status || '执行中' }}</span>
                  </div>
                  
                  <!-- 思考部分 - 严格按照 right-sidebar.js 的逻辑 -->
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
                  
                  <!-- 行动部分 - 严格按照 right-sidebar.js 的逻辑 -->
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
                </div>
              </div>
            </div>
            
            <div v-else-if="selectedStep.agentExecution && !selectedStep.agentExecution.thinkActSteps?.length" class="no-steps-message">
              <p>暂无详细步骤信息</p>
            </div>
            
            <!-- 执行中的动态效果 -->
            <div v-if="selectedStep.current && !selectedStep.completed" class="execution-indicator">
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
            <h3>未选择执行步骤</h3>
            <p>请在左侧聊天区域点击任意执行步骤查看详情</p>
          </div>
        </div>
        
        <!-- 滚动到底部按钮 -->
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/editor/index.vue'
import { planExecutionManager } from '@/utils/plan-execution-manager'

const activeTab = ref('details')

const previewTabs = [
  { id: 'details', name: '步骤执行详情', icon: 'carbon:events' },
  { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
  { id: 'code', name: 'Code', icon: 'carbon:code' },
]

// 计划数据映射 (类似 right-sidebar.js 的 planDataMap)
const planDataMap = ref<Map<string, any>>(new Map())
const currentDisplayedPlanId = ref<string>()
const selectedStep = ref<any>()

// 滚动相关状态
const scrollContainer = ref<HTMLElement>()
const showScrollToBottomButton = ref(false)
const isNearBottom = ref(true)
const shouldAutoScrollToBottom = ref(true) // 是否应该自动滚动到底部

// 自动刷新相关状态
const autoRefreshTimer = ref<number | null>(null)
const AUTO_REFRESH_INTERVAL = 3000 // 3秒刷新一次步骤详情

// 处理计划更新事件 - 改为直接方法调用
const handlePlanUpdate = async (planData: any) => {
  console.log('[RightPanel] 收到计划更新事件, planData:', planData)
  
  // 验证数据有效性
  if (!planData || !planData.planId) {
    console.warn('[RightPanel] Invalid plan data received:', planData)
    return
  }
  
  // 更新计划数据到本地映射
  planDataMap.value.set(planData.planId, planData)
  console.log('[RightPanel] 计划数据已更新到 planDataMap:', planData.planId)
  
  // 如果当前选中的步骤对应这个计划，重新加载步骤详情
  if (selectedStep.value?.planId === planData.planId) {
    console.log('[RightPanel] 刷新当前选中步骤的详情:', selectedStep.value.index)
    showStepDetails(planData.planId, selectedStep.value.index)
  }
  
  // 数据更新后，如果之前在底部则自动滚动到最新内容
  autoScrollToBottomIfNeeded()
}

// 更新显示的计划进度
const updateDisplayedPlanProgress = (planData: any) => {
  // 这里可以更新UI状态，比如进度条等
  if (planData.steps && planData.steps.length > 0) {
    const totalSteps = planData.steps.length
    const currentStep = (planData.currentStepIndex || 0) + 1
    console.log(`进度: ${currentStep} / ${totalSteps}`)
  }
}

// 显示步骤详情
const showStepDetails = (planId: string, stepIndex: number) => {
  console.log('[RightPanel] 显示步骤详情:', { planId, stepIndex })
  
  const planData = planDataMap.value.get(planId)
  
  if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
    selectedStep.value = null
    console.log('[RightPanel] Invalid step data:', { planId, stepIndex, hasSteps: !!planData?.steps })
    stopAutoRefresh() // 停止自动刷新
    return
  }
  
  currentDisplayedPlanId.value = planId
  const step = planData.steps[stepIndex]
  const agentExecution = planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex]
  
  // 判断步骤是否完成 - 多重条件判断
  const isStepCompleted = agentExecution?.isCompleted || 
                         planData.completed || 
                         (planData.currentStepIndex !== undefined && stepIndex < planData.currentStepIndex)
  
  const isCurrent = !isStepCompleted && stepIndex === planData.currentStepIndex && !planData.completed
  
  // 构造步骤详情对象，类似 right-sidebar.js 的逻辑
  selectedStep.value = {
    planId: planId, // 确保包含planId
    index: stepIndex,
    title: typeof step === 'string' ? step : (step.title || step.description || step.name || `步骤 ${stepIndex + 1}`),
    description: typeof step === 'string' ? step : (step.description || step),
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
    thinkActStepsData: agentExecution?.thinkActSteps, // 添加详细的数据结构日志
    completed: isStepCompleted,
    current: isCurrent,
    planCurrentStep: planData.currentStepIndex,
    planCompleted: planData.completed
  })
  
  // 如果步骤未完成且计划还在执行中，启动自动刷新
  if (!isStepCompleted && !planData.completed && planExecutionManager.getActivePlanId() === planId) {
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

// 自动刷新步骤详情
const startAutoRefresh = (planId: string, stepIndex: number) => {
  console.log('[RightPanel] 启动自动刷新:', { planId, stepIndex })
  
  // 停止之前的刷新
  stopAutoRefresh()
  
  autoRefreshTimer.value = window.setInterval(() => {
    console.log('[RightPanel] 执行自动刷新 - Step details')
    
    // 检查计划是否还在执行
    const planData = planDataMap.value.get(planId)
    if (!planData || planData.completed) {
      console.log('[RightPanel] 计划已完成，停止自动刷新')
      stopAutoRefresh()
      return
    }
    
    // 检查步骤是否还在执行
    const agentExecution = planData.agentExecutionSequence?.[stepIndex]
    if (agentExecution?.isCompleted) {
      console.log('[RightPanel] 步骤已完成，停止自动刷新')
      stopAutoRefresh()
      return
    }
    
    // 检查是否已经进入下一步
    const currentStepIndex = planData.currentStepIndex ?? 0
    if (stepIndex < currentStepIndex) {
      console.log('[RightPanel] 已进入下一步，停止自动刷新')
      stopAutoRefresh()
      return
    }
    
    // 刷新步骤详情
    showStepDetails(planId, stepIndex)
    
    // 数据更新后，如果之前在底部则自动滚动到最新内容
    autoScrollToBottomIfNeeded()
  }, AUTO_REFRESH_INTERVAL)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
    console.log('[RightPanel] 自动刷新已停止')
  }
}

// 格式化JSON显示 - 按照 right-sidebar.js 的逻辑
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

// 获取步骤状态文本
const getStepStatusText = (step: any): string => {
  if (step.completed) return '已完成'
  if (step.current) return '执行中'
  return '等待执行'
}

// 生命周期 - 挂载时的初始化（移除全局事件监听）
onMounted(() => {
  console.log('右侧面板组件已挂载')
  // 使用nextTick确保DOM已渲染
  nextTick(() => {
    initScrollListener()
  })
})

// 生命周期 - 卸载时的清理（移除全局事件监听相关）
onUnmounted(() => {
  console.log('[RightPanel] Component unmounting, cleaning up...')
  
  // 停止自动刷新
  stopAutoRefresh()
  
  // 移除滚动监听器
  if (scrollContainer.value) {
    scrollContainer.value.removeEventListener('scroll', checkScrollState)
  }
  
  // 清理计划数据
  planDataMap.value.clear()
})

// 初始化滚动监听器
const initScrollListener = () => {
  const setupScrollListener = () => {
    const element = scrollContainer.value
    if (!element) {
      console.log('[RightPanel] Scroll container not found, retrying...')
      return false
    }

    element.addEventListener('scroll', checkScrollState)
    // 初始状态检查
    shouldAutoScrollToBottom.value = true // 重置为自动滚动状态
    checkScrollState()
    console.log('[RightPanel] Scroll listener initialized successfully')
    return true
  }

  // 使用 nextTick 确保 DOM 已更新
  nextTick(() => {
    if (!setupScrollListener()) {
      // 如果第一次失败，再尝试一次
      setTimeout(() => {
        setupScrollListener()
      }, 100)
    }
  })
}

// 移除滚动监听
const removeScrollListener = () => {
  if (scrollContainer.value) {
    scrollContainer.value.removeEventListener('scroll', handleScroll)
    console.log('[RightPanel] Scroll listener removed')
  }
}

// 处理滚动事件
const handleScroll = () => {
  checkScrollState()
}

// 检查滚动状态
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
  
  console.log('滚动状态检查:', {
    scrollTop,
    scrollHeight,
    clientHeight,
    isAtBottom,
    hasScrollableContent,
    showButton: showScrollToBottomButton.value,
    shouldAutoScroll: shouldAutoScrollToBottom.value
  })
}

// 滚动到底部
const scrollToBottom = () => {
  if (!scrollContainer.value) return
  
  scrollContainer.value.scrollTo({
    top: scrollContainer.value.scrollHeight,
    behavior: 'smooth'
  })
  
  // 滚动后重置状态
  nextTick(() => {
    shouldAutoScrollToBottom.value = true
    checkScrollState()
  })
}

// 自动滚动到底部（仅在应该自动滚动时）
const autoScrollToBottomIfNeeded = () => {
  if (!shouldAutoScrollToBottom.value || !scrollContainer.value) return
  
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
      console.log('自动滚动到底部')
    }
  })
}

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

// 暴露给父组件的方法
defineExpose({
  handlePlanUpdate,
  showStepDetails,
  updateDisplayedPlanProgress
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
  background: linear-gradient(90deg, 
    transparent, 
    rgba(74, 144, 226, 0.6), 
    transparent
  );
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
  
  .think-section, .action-section {
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
  
  .think-content, .action-content {
    .input, .output, .tool-info {
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
