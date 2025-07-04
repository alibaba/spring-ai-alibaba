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
  <div class="sidebar-wrapper" :class="{ 'sidebar-wrapper-collapsed': sidebarStore.isCollapsed }">
    <div class="sidebar-content">
      <div class="sidebar-content-header">
        <div class="sidebar-content-title">计划模板</div>
      </div>

      <!-- Tab Switcher -->
      <div class="tab-switcher">
        <button
          class="tab-button"
          :class="{ active: sidebarStore.currentTab === 'list' }"
          @click="sidebarStore.switchToTab('list')"
        >
          <Icon icon="carbon:list" width="16" />
          模板列表
        </button>
        <button
          class="tab-button"
          :class="{ active: sidebarStore.currentTab === 'config' }"
          @click="sidebarStore.switchToTab('config')"
          :disabled="!sidebarStore.selectedTemplate"
        >
          <Icon icon="carbon:settings" width="16" />
          配置
        </button>
      </div>

      <!-- List Tab Content -->
      <div v-if="sidebarStore.currentTab === 'list'" class="tab-content">
        <div class="new-task-section">
          <button class="new-task-btn" @click="handleNewTaskButtonClick">
            <Icon icon="carbon:add" width="16" />
            新建计划
            <span class="shortcut">⌘ K</span>
          </button>
        </div>

        <div class="sidebar-content-list">
          <!-- Loading state -->
          <div v-if="sidebarStore.isLoading" class="loading-state">
            <Icon icon="carbon:circle-dash" width="20" class="spinning" />
            <span>加载中...</span>
          </div>

          <!-- Error state -->
          <div v-else-if="sidebarStore.errorMessage" class="error-state">
            <Icon icon="carbon:warning" width="20" />
            <span>{{ sidebarStore.errorMessage }}</span>
            <button @click="sidebarStore.loadPlanTemplateList" class="retry-btn">重试</button>
          </div>

          <!-- Empty state -->
          <div v-else-if="sidebarStore.planTemplateList.length === 0" class="empty-state">
            <Icon icon="carbon:document" width="32" />
            <span>没有可用的计划模板</span>
          </div>

          <!-- Plan template list -->
          <div
            v-else
            v-for="template in sidebarStore.sortedTemplates"
            :key="template.id"
            class="sidebar-content-list-item"
            :class="{
              'sidebar-content-list-item-active':
                template.id === sidebarStore.currentPlanTemplateId,
            }"
            @click="handlePlanTemplateClick(template)"
          >
            <div class="task-icon">
              <Icon icon="carbon:document" width="20" />
            </div>
            <div class="task-details">
              <div class="task-title">{{ template.title || '未命名计划' }}</div>
              <div class="task-preview">
                {{ truncateText(template.description || '无描述', 40) }}
              </div>
            </div>
            <div class="task-time">
              {{ getRelativeTimeString(new Date(template.updateTime || template.createTime)) }}
            </div>
            <div class="task-actions">
              <button
                class="delete-task-btn"
                title="删除此计划模板"
                @click.stop="handleDeletePlanTemplate(template)"
              >
                <Icon icon="carbon:close" width="16" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Config Tab Content -->
      <div v-else-if="sidebarStore.currentTab === 'config'" class="tab-content config-tab">
        <div v-if="sidebarStore.selectedTemplate" class="config-container">
          <!-- Template Info Header -->
          <div class="template-info-header">
            <div class="template-info">
              <h3>{{ sidebarStore.selectedTemplate.title || '未命名计划' }}</h3>
              <span class="template-id">ID: {{ sidebarStore.selectedTemplate.id }}</span>
            </div>
            <button class="back-to-list-btn" @click="sidebarStore.switchToTab('list')">
              <Icon icon="carbon:arrow-left" width="16" />
            </button>
          </div>

          <!-- Section 1: JSON Editor -->
          <div class="config-section">
            <div class="section-header">
              <Icon icon="carbon:code" width="16" />
              <span>JSON 模板</span>
              <div class="section-actions">
                <button
                  class="btn btn-sm"
                  @click="sidebarStore.rollbackVersion"
                  :disabled="!sidebarStore.canRollback"
                  title="回滚"
                >
                  <Icon icon="carbon:undo" width="14" />
                </button>
                <button
                  class="btn btn-sm"
                  @click="sidebarStore.restoreVersion"
                  :disabled="!sidebarStore.canRestore"
                  title="恢复"
                >
                  <Icon icon="carbon:redo" width="14" />
                </button>
                <button
                  class="btn btn-primary btn-sm"
                  @click="handleSaveTemplate"
                  :disabled="sidebarStore.isGenerating || sidebarStore.isExecuting"
                >
                  <Icon icon="carbon:save" width="14" />
                </button>
              </div>
            </div>
            <textarea
              v-model="sidebarStore.jsonContent"
              class="json-editor"
              placeholder="输入 JSON 计划模板..."
              rows="8"
            ></textarea>
          </div>

          <!-- Section 2: Plan Generator -->
          <div class="config-section">
            <div class="section-header">
              <Icon icon="carbon:generate" width="16" />
              <span>计划生成器</span>
            </div>
            <div class="generator-content">
              <textarea
                v-model="sidebarStore.generatorPrompt"
                class="prompt-input"
                placeholder="描述您想要生成的计划..."
                rows="3"
              ></textarea>
              <div class="generator-actions">
                <button
                  class="btn btn-primary btn-sm"
                  @click="handleGeneratePlan"
                  :disabled="sidebarStore.isGenerating || !sidebarStore.generatorPrompt.trim()"
                >
                  <Icon
                    :icon="sidebarStore.isGenerating ? 'carbon:circle-dash' : 'carbon:generate'"
                    width="14"
                    :class="{ spinning: sidebarStore.isGenerating }"
                  />
                  {{ sidebarStore.isGenerating ? '生成中...' : '生成计划' }}
                </button>
                <button
                  class="btn btn-secondary btn-sm"
                  @click="handleUpdatePlan"
                  :disabled="
                    sidebarStore.isGenerating ||
                    !sidebarStore.generatorPrompt.trim() ||
                    !sidebarStore.jsonContent.trim()
                  "
                >
                  <Icon icon="carbon:edit" width="14" />
                  更新计划
                </button>
              </div>
            </div>
          </div>

          <!-- Section 3: Execution Controller -->
          <div class="config-section">
            <div class="section-header">
              <Icon icon="carbon:play" width="16" />
              <span>执行控制器</span>
            </div>
            <div class="execution-content">
              <div class="params-input-group">
                <label>执行参数</label>
                <div class="params-input-container">
                  <input
                    v-model="sidebarStore.executionParams"
                    class="params-input"
                    placeholder="输入执行参数..."
                  />
                  <button
                    class="clear-params-btn"
                    @click="sidebarStore.clearExecutionParams"
                    title="清空参数"
                  >
                    <Icon icon="carbon:close" width="12" />
                  </button>
                </div>
              </div>
              <div class="api-url-display">
                <span class="api-url-label">API URL:</span>
                <code class="api-url">{{ sidebarStore.computedApiUrl }}</code>
              </div>
              <button
                class="btn btn-primary execute-btn"
                @click="handleExecutePlan"
                :disabled="sidebarStore.isExecuting || sidebarStore.isGenerating"
              >
                <Icon
                  :icon="sidebarStore.isExecuting ? 'carbon:circle-dash' : 'carbon:play'"
                  width="16"
                  :class="{ spinning: sidebarStore.isExecuting }"
                />
                {{ sidebarStore.isExecuting ? '执行中...' : '执行计划' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useSidebarStore } from '@/stores/sidebar'
import type { PlanTemplate } from '@/types/plan-template'

// 使用pinia store
const sidebarStore = useSidebarStore()

// Emits - 保留部分事件用于与外部组件通信
const emit = defineEmits<{
  planExecutionRequested: [payload: { title: string; planData: any; params?: string }]
}>()

// Methods
const handleNewTaskButtonClick = () => {
  sidebarStore.createNewTemplate()
  console.log('[PlanTemplateSidebar] 创建新的空白计划模板，切换到配置标签页')
}

const handlePlanTemplateClick = async (template: PlanTemplate) => {
  try {
    await sidebarStore.selectTemplate(template)
    console.log(`[PlanTemplateSidebar] 选择了计划模板: ${template.id}`)
  } catch (error: any) {
    console.error('选择计划模板失败:', error)
    alert('选择计划模板失败: ' + error.message)
  }
}

const handleDeletePlanTemplate = async (template: PlanTemplate) => {
  if (confirm(`确定要删除计划模板 "${template.title || '未命名计划'}" 吗？此操作不可恢复。`)) {
    try {
      await sidebarStore.deleteTemplate(template)
      alert('计划模板已删除。')
    } catch (error: any) {
      console.error('删除计划模板失败:', error)
      alert('删除计划模板失败: ' + error.message)
    }
  }
}

const handleSaveTemplate = async () => {
  try {
    const saveResult = await sidebarStore.saveTemplate()

    if (saveResult?.duplicate) {
      alert(`保存完成：${saveResult.message}\n\n当前版本数：${saveResult.versionCount}`)
    } else if (saveResult?.saved) {
      alert(`保存成功：${saveResult.message}\n\n当前版本数：${saveResult.versionCount}`)
    } else if (saveResult?.message) {
      alert(`保存状态：${saveResult.message}`)
    }
  } catch (error: any) {
    console.error('保存计划修改失败:', error)
    alert(error.message || '保存计划修改失败')
  }
}

const handleGeneratePlan = async () => {
  try {
    const response = await sidebarStore.generatePlan()
    alert(`计划生成成功！模板ID: ${sidebarStore.selectedTemplate?.id || '未知'}`)
  } catch (error: any) {
    console.error('生成计划失败:', error)
    alert('生成计划失败: ' + error.message)
  }
}

const handleUpdatePlan = async () => {
  try {
    await sidebarStore.updatePlan()
    alert('计划更新成功！')
  } catch (error: any) {
    console.error('更新计划失败:', error)
    alert('更新计划失败: ' + error.message)
  }
}

const handleExecutePlan = async () => {
  console.log('[Sidebar] handleExecutePlan called')

  try {
    const planData = sidebarStore.preparePlanExecution()

    if (!planData) {
      console.log('[Sidebar] No plan data available, returning')
      return
    }

    console.log('[Sidebar] 触发计划执行请求:', planData)

    // 发送计划执行事件给聊天组件
    console.log('[Sidebar] Emitting planExecutionRequested event')
    emit('planExecutionRequested', planData)

    console.log('[Sidebar] Event emitted')
  } catch (error: any) {
    console.error('执行计划出错:', error)
    alert('执行计划失败: ' + error.message)
  } finally {
    sidebarStore.finishPlanExecution()
  }
}

// 工具函数
const getRelativeTimeString = (date: Date): string => {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 30) return `${diffDays}天前`

  return date.toLocaleDateString('zh-CN')
}

const truncateText = (text: string, maxLength: number): string => {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// Lifecycle
onMounted(() => {
  sidebarStore.loadPlanTemplateList()
})

// 暴露方法供父组件调用
defineExpose({
  loadPlanTemplateList: sidebarStore.loadPlanTemplateList,
  toggleSidebar: sidebarStore.toggleSidebar,
  currentPlanTemplateId: sidebarStore.currentPlanTemplateId,
})
</script>

<style scoped>
.sidebar-wrapper {
  position: relative;
  width: 500px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease-in-out;
  overflow: hidden;
}
.sidebar-wrapper-collapsed {
  border-right: none;
  width: 0;
  /* transform: translateX(-100%); */

  .sidebar-content {
    opacity: 0;
    pointer-events: none;
  }
}

.sidebar-content {
  height: 100%;
  width: 100%;
  padding: 12px 0 12px 12px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease-in-out;

  .sidebar-content-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    overflow: hidden;

    .sidebar-content-title {
      font-size: 20px;
      font-weight: 600;

      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .tab-switcher {
    display: flex;
    margin-bottom: 16px;
    padding-right: 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    padding: 4px;

    .tab-button {
      flex: 1;
      padding: 8px 12px;
      background: transparent;
      border: none;
      border-radius: 6px;
      color: rgba(255, 255, 255, 0.7);
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all 0.2s ease;

      &:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.9);
      }

      &.active {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }

  .tab-content {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
  }

  .config-tab {
    .config-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow-y: auto;
      padding-right: 12px;

      .template-info-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
        padding: 12px;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 8px;

        .template-info {
          flex: 1;
          min-width: 0;

          h3 {
            margin: 0 0 4px 0;
            font-size: 14px;
            font-weight: 600;
            color: white;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .template-id {
            font-size: 11px;
            color: rgba(255, 255, 255, 0.5);
          }
        }

        .back-to-list-btn {
          width: 28px;
          height: 28px;
          background: transparent;
          border: none;
          border-radius: 4px;
          color: rgba(255, 255, 255, 0.7);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.2s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.1);
            color: white;
          }
        }
      }

      .config-section {
        margin-bottom: 16px;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 8px;
        padding: 12px;

        .section-header {
          display: flex;
          align-items: center;
          margin-bottom: 12px;
          color: #667eea;
          font-size: 13px;
          font-weight: 600;
          gap: 8px;

          .section-actions {
            margin-left: auto;
            display: flex;
            gap: 6px;
          }
        }

        .json-editor,
        .prompt-input,
        .params-input {
          width: 100%;
          background: rgba(0, 0, 0, 0.3);
          border: 1px solid rgba(255, 255, 255, 0.2);
          border-radius: 6px;
          color: white;
          font-size: 12px;
          font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          padding: 8px;
          resize: vertical;
          min-height: 100px;

          &:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
          }

          &::placeholder {
            color: rgba(255, 255, 255, 0.4);
          }
        }

        .generator-content {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .generator-actions {
            display: flex;
            gap: 8px;
          }
        }

        .execution-content {
          display: flex;
          flex-direction: column;
          gap: 12px;

          .params-input-group {
            label {
              display: block;
              margin-bottom: 6px;
              font-size: 12px;
              color: rgba(255, 255, 255, 0.8);
              font-weight: 500;
            }

            .params-input-container {
              position: relative;
              display: flex;
              align-items: center;

              .params-input {
                min-height: auto;
                padding-right: 32px;
              }

              .clear-params-btn {
                position: absolute;
                right: 8px;
                width: 20px;
                height: 20px;
                background: transparent;
                border: none;
                border-radius: 4px;
                color: rgba(255, 255, 255, 0.5);
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: all 0.2s ease;

                &:hover {
                  background: rgba(255, 0, 0, 0.2);
                  color: #ff6b6b;
                }
              }
            }
          }

          .api-url-display {
            padding: 8px;
            background: rgba(0, 0, 0, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 6px;
            font-size: 11px;

            .api-url-label {
              color: rgba(255, 255, 255, 0.7);
              margin-right: 8px;
            }

            .api-url {
              color: #64b5f6;
              font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
              word-break: break-all;
            }
          }

          .execute-btn {
            padding: 10px 16px;
            font-size: 13px;
            font-weight: 500;
          }
        }
      }
    }
  }

  .btn {
    padding: 6px 12px;
    border: none;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.2s ease;

    &.btn-sm {
      padding: 4px 8px;
      font-size: 11px;
    }

    &.btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
      }
    }

    &.btn-secondary {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.2);

      &:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.2);
        color: white;
      }
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      transform: none !important;
      box-shadow: none !important;
    }

    .spinning {
      animation: spin 1s linear infinite;
    }
  }

  .new-task-section {
    margin-bottom: 16px;
    padding-right: 12px;

    .new-task-btn {
      width: 100%;
      padding: 12px 16px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 8px;
      color: white;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      .shortcut {
        font-size: 12px;
        opacity: 0.8;
        margin-left: auto;
      }
    }
  }

  .sidebar-content-list {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow-y: auto;
    padding-right: 12px;

    .loading-state,
    .error-state,
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 32px 16px;
      color: rgba(255, 255, 255, 0.6);
      font-size: 14px;
      text-align: center;
      gap: 12px;

      .spinning {
        animation: spin 1s linear infinite;
      }

      .retry-btn {
        padding: 8px 16px;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 4px;
        color: white;
        cursor: pointer;
        font-size: 12px;
        transition: background-color 0.2s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.2);
        }
      }
    }

    .sidebar-content-list-item {
      display: flex;
      align-items: flex-start;
      padding: 12px;
      margin-bottom: 8px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.2);
        transform: translateY(-1px);
      }

      &.sidebar-content-list-item-active {
        border: 2px solid #667eea;
        background: rgba(102, 126, 234, 0.1);
      }

      .task-icon {
        margin-right: 12px;
        color: #667eea;
        flex-shrink: 0;
        margin-top: 2px;
      }

      .task-details {
        flex: 1;
        min-width: 0;

        .task-title {
          font-size: 14px;
          font-weight: 600;
          color: white;
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .task-preview {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.7);
          line-height: 1.4;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .task-time {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.5);
        margin-left: 8px;
        flex-shrink: 0;
        position: absolute;
        top: 12px;
        right: 40px;
      }

      .task-actions {
        display: flex;
        align-items: center;
        margin-left: 8px;
        flex-shrink: 0;

        .delete-task-btn {
          width: 24px;
          height: 24px;
          background: transparent;
          border: none;
          border-radius: 4px;
          color: rgba(255, 255, 255, 0.5);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.2s ease;
          position: absolute;
          top: 12px;
          right: 12px;

          &:hover {
            background: rgba(255, 0, 0, 0.2);
            color: #ff6b6b;
          }
        }
      }
    }
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
