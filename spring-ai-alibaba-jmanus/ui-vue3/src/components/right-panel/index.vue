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
          v-for="tab in rightPanelStore.previewTabs.filter(t => t.id === 'details')"
          :key="tab.id"
          class="tab-button"
          :class="{ active: rightPanelStore.activeTab === tab.id }"
          @click="rightPanelStore.switchTab(tab.id)"
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
          @click="rightPanelStore.copyCode"
          v-if="rightPanelStore.activeTab === 'code'"
        >
          <Icon icon="carbon:copy" />
        </button>
        <button
          class="action-button"
          @click="rightPanelStore.downloadCode"
          v-if="rightPanelStore.activeTab === 'code'"
        >
          <Icon icon="carbon:download" />
        </button>
      </div>
    </div>

    <div class="preview-content">
      <!-- Code Preview -->
      <div v-if="rightPanelStore.activeTab === 'code'" class="code-preview">
        <MonacoEditor
          v-model="rightPanelStore.codeContent"
          :language="rightPanelStore.codeLanguage"
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
      <div v-else-if="rightPanelStore.activeTab === 'chat'" class="chat-preview">
        <div class="chat-bubbles">
          <div
            v-for="bubble in rightPanelStore.chatBubbles"
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
      <div v-else-if="rightPanelStore.activeTab === 'details'" class="step-details">
        <!-- 固定顶部的步骤基本信息 -->
        <div v-if="rightPanelStore.selectedStep" class="step-info-fixed">
          <h3>
            {{
              rightPanelStore.selectedStep.title ||
              rightPanelStore.selectedStep.description ||
              `步骤 ${rightPanelStore.selectedStep.index + 1}`
            }}
          </h3>

          <div class="agent-info" v-if="rightPanelStore.selectedStep.agentExecution">
            <div class="info-item">
              <span class="label">执行智能体:</span>
              <span class="value">{{ rightPanelStore.selectedStep.agentExecution.agentName }}</span>
            </div>
            <div class="info-item">
              <span class="label">描述:</span>
              <span class="value">{{
                rightPanelStore.selectedStep.agentExecution.agentDescription || ''
              }}</span>
            </div>
            <div class="info-item">
              <span class="label">请求:</span>
              <span class="value">{{
                rightPanelStore.selectedStep.agentExecution.agentRequest || ''
              }}</span>
            </div>
            <div class="info-item">
              <span class="label">执行结果:</span>
              <span
                class="value"
                :class="{ success: rightPanelStore.selectedStep.agentExecution.isCompleted }"
              >
                {{ rightPanelStore.selectedStep.agentExecution.result || '执行中...' }}
              </span>
            </div>
          </div>

          <div class="execution-status">
            <div class="status-item">
              <Icon
                icon="carbon:checkmark-filled"
                v-if="rightPanelStore.selectedStep.completed"
                class="status-icon success"
              />
              <Icon
                icon="carbon:in-progress"
                v-else-if="rightPanelStore.selectedStep.current"
                class="status-icon progress"
              />
              <Icon icon="carbon:time" v-else class="status-icon pending" />
              <span class="status-text">
                {{ rightPanelStore.stepStatusText }}
              </span>
            </div>
          </div>
        </div>

        <!-- 可滚动的详细内容区域 -->
        <div
          ref="scrollContainer"
          class="step-details-scroll-container"
          @scroll="rightPanelStore.checkScrollState"
        >
          <div v-if="rightPanelStore.selectedStep">
            <!-- 思考与行动步骤 -->
            <div
              class="think-act-steps"
              v-if="rightPanelStore.selectedStep.agentExecution?.thinkActSteps?.length > 0"
            >
              <h4>思考与行动步骤</h4>
              <div class="steps-container">
                <div
                  v-for="(tas, index) in rightPanelStore.selectedStep.agentExecution.thinkActSteps"
                  :key="index"
                  class="think-act-step"
                >
                  <div class="step-header">
                    <span class="step-number">#{{ index + 1 }}</span>
                    <span class="step-status" :class="tas.status">{{
                      tas.status || '执行中'
                    }}</span>
                  </div>

                  <!-- 思考部分 - 严格按照 right-sidebar.js 的逻辑 -->
                  <div class="think-section">
                    <h5><Icon icon="carbon:thinking" /> 思考</h5>
                    <div class="think-content">
                      <div class="input">
                        <span class="label">输入:</span>
                        <pre>{{ rightPanelStore.formatJson(tas.thinkInput) }}</pre>
                      </div>
                      <div class="output">
                        <span class="label">输出:</span>
                        <pre>{{ rightPanelStore.formatJson(tas.thinkOutput) }}</pre>
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
                        <pre>{{ rightPanelStore.formatJson(tas.toolParameters) }}</pre>
                      </div>
                      <div class="output">
                        <span class="label">执行结果:</span>
                        <pre>{{ rightPanelStore.formatJson(tas.actionResult) }}</pre>
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
                rightPanelStore.selectedStep.agentExecution &&
                !rightPanelStore.selectedStep.agentExecution.thinkActSteps?.length
              "
              class="no-steps-message"
            >
              <p>暂无详细步骤信息</p>
            </div>

            <!-- 处理没有agentExecution的情况 -->
            <div
              v-else-if="!rightPanelStore.selectedStep.agentExecution"
              class="no-execution-message"
            >
              <Icon icon="carbon:information" class="info-icon" />
              <h4>步骤信息</h4>
              <div class="step-basic-info">
                <div class="info-item">
                  <span class="label">步骤名称:</span>
                  <span class="value">{{
                    rightPanelStore.selectedStep.title ||
                    rightPanelStore.selectedStep.description ||
                    `步骤 ${rightPanelStore.selectedStep.index + 1}`
                  }}</span>
                </div>
                <div class="info-item" v-if="rightPanelStore.selectedStep.description">
                  <span class="label">描述:</span>
                  <span class="value">{{ rightPanelStore.selectedStep.description }}</span>
                </div>
                <div class="info-item">
                  <span class="label">状态:</span>
                  <span class="value" :class="{
                    'status-completed': rightPanelStore.selectedStep.completed,
                    'status-current': rightPanelStore.selectedStep.current,
                    'status-pending': !rightPanelStore.selectedStep.completed && !rightPanelStore.selectedStep.current
                  }">
                    {{
                      rightPanelStore.selectedStep.completed ? '已完成' :
                      rightPanelStore.selectedStep.current ? '执行中' : '待执行'
                    }}
                  </span>
                </div>
              </div>
              <p class="no-execution-hint">该步骤暂无详细执行信息</p>
            </div>

            <!-- 执行中的动态效果 -->
            <div
              v-if="rightPanelStore.selectedStep.current && !rightPanelStore.selectedStep.completed"
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
            <h3>未选择执行步骤</h3>
            <p>请在左侧聊天区域点击任意执行步骤查看详情</p>
          </div>
        </div>

        <!-- 滚动到底部按钮 -->
        <Transition name="scroll-button">
          <button
            v-if="rightPanelStore.showScrollToBottomButton"
            @click="rightPanelStore.scrollToBottom"
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
import { useRightPanelStore } from '@/stores/right-panel'

// 使用 Pinia store
const rightPanelStore = useRightPanelStore()

// DOM 元素引用
const scrollContainer = ref<HTMLElement>()

// 初始化滚动监听器
const initScrollListener = () => {
  const setupScrollListener = () => {
    const element = scrollContainer.value
    if (!element) {
      console.log('[RightPanel] Scroll container not found, retrying...')
      return false
    }

    // 设置滚动容器到 store
    rightPanelStore.setScrollContainer(element)

    element.addEventListener('scroll', rightPanelStore.checkScrollState)
    // 初始状态检查
    rightPanelStore.shouldAutoScrollToBottom = true // 重置为自动滚动状态
    rightPanelStore.checkScrollState()
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

// 生命周期 - 挂载时的初始化
onMounted(() => {
  console.log('右侧面板组件已挂载')
  // 使用nextTick确保DOM已渲染
  nextTick(() => {
    initScrollListener()
  })
})

// 生命周期 - 卸载时的清理
onUnmounted(() => {
  console.log('[RightPanel] Component unmounting, cleaning up...')
  rightPanelStore.cleanup()
})

// 获取子步骤状态
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

// 处理子计划步骤点击
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

  // 使用 rightPanelStore 显示子计划的步骤详情
  rightPanelStore.showStepDetails(subPlan.planId, stepIndex)
}

// 暴露给父组件的方法 - 仅保留必要的接口
defineExpose({
  handlePlanUpdate: rightPanelStore.handlePlanUpdate,
  showStepDetails: rightPanelStore.showStepDetails,
  updateDisplayedPlanProgress: rightPanelStore.updateDisplayedPlanProgress,
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
