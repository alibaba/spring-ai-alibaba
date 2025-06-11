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
        <div v-if="selectedStep" class="step-info">
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

        <div v-else class="no-selection">
          <Icon icon="carbon:events" class="empty-icon" />
          <h3>未选择执行步骤</h3>
          <p>请在左侧聊天区域点击任意执行步骤查看详情</p>
        </div>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/editor/index.vue'

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

// 处理计划更新事件 - 改为直接方法调用
const handlePlanUpdate = (planData: any) => {
  if (!planData?.planId) return
  
  planDataMap.value.set(planData.planId, planData)
  
  // 如果是当前显示的计划，更新显示
  if (planData.planId === currentDisplayedPlanId.value) {
    updateDisplayedPlanProgress(planData)
  }
}

// 删除原来的 handleStepSelection 方法，直接使用 showStepDetails

// 更新显示的计划进度
const updateDisplayedPlanProgress = (planData: any) => {
  // 这里可以更新UI状态，比如进度条等
  if (planData.steps && planData.steps.length > 0) {
    const totalSteps = planData.steps.length
    const currentStep = (planData.currentStepIndex || 0) + 1
    console.log(`进度: ${currentStep} / ${totalSteps}`)
  }
}

// 显示步骤详情 - 参照 right-sidebar.js 的逻辑
const showStepDetails = (planId: string, stepIndex: number) => {
  const planData = planDataMap.value.get(planId)
  
  if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
    selectedStep.value = null
    console.log('[RightPanel] Invalid step data:', { planId, stepIndex, hasSteps: !!planData?.steps })
    return
  }
  
  currentDisplayedPlanId.value = planId
  const step = planData.steps[stepIndex]
  const agentExecution = planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex]
  
  // 构造步骤详情对象，类似 right-sidebar.js 的逻辑
  selectedStep.value = {
    index: stepIndex,
    title: typeof step === 'string' ? step : (step.title || step.description || step.name || `步骤 ${stepIndex + 1}`),
    description: typeof step === 'string' ? step : (step.description || step),
    agentExecution: agentExecution,
    completed: agentExecution?.isCompleted || false,
    current: planData.currentStepIndex === stepIndex,
  }
  
  console.log('[RightPanel] Step details updated:', {
    planId,
    stepIndex,
    stepTitle: selectedStep.value.title,
    hasAgentExecution: !!agentExecution,
    hasThinkActSteps: !!agentExecution?.thinkActSteps?.length,
    thinkActStepsData: agentExecution?.thinkActSteps // 添加详细的数据结构日志
  })
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
})

// 生命周期 - 卸载时的清理（移除全局事件监听相关）
onUnmounted(() => {
  console.log('右侧面板组件已卸载')
})

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
  overflow: hidden;
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
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.step-info {
  h3 {
    color: #ffffff;
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.agent-info {
  margin-bottom: 16px;
  
  .info-item {
    display: flex;
    margin-bottom: 8px;
    
    .label {
      font-weight: 600;
      color: #888888;
      min-width: 80px;
      margin-right: 12px;
    }
    
    .value {
      color: #cccccc;
      flex: 1;
      
      &.success {
        color: #27ae60;
      }
    }
  }
}

.think-act-steps {
  margin-top: 24px;
  
  h4 {
    color: #ffffff;
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
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

.no-steps-message {
  text-align: center;
  color: #666666;
  font-style: italic;
  margin-top: 16px;
  
  p {
    margin: 0;
  }
}

.execution-status {
  margin-top: 24px;
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
</style>
