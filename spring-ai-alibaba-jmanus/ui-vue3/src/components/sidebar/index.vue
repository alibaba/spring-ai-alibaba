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
  <div class="sidebar-wrapper" :class="{ 'sidebar-wrapper-collapsed': isCollapsed }">
    <div class="sidebar-content">
      <div class="sidebar-content-header">
        <div class="sidebar-content-title">计划模板</div>

        <div class="config-button" @click="handleConfig">
          <Icon icon="carbon:settings-adjust" width="20" />
        </div>
      </div>
      
      <!-- Tab Switcher -->
      <div class="tab-switcher">
        <button 
          class="tab-button" 
          :class="{ active: currentTab === 'list' }"
          @click="currentTab = 'list'"
        >
          <Icon icon="carbon:list" width="16" />
          模板列表
        </button>
        <button 
          class="tab-button" 
          :class="{ active: currentTab === 'config' }"
          @click="currentTab = 'config'"
          :disabled="!selectedTemplate"
        >
          <Icon icon="carbon:settings" width="16" />
          配置
        </button>
      </div>

      <!-- List Tab Content -->
      <div v-if="currentTab === 'list'" class="tab-content">
        <div class="new-task-section">
          <button class="new-task-btn" @click="handleNewTaskButtonClick">
            <Icon icon="carbon:add" width="16" />
            新建计划
            <span class="shortcut">⌘ K</span>
          </button>
        </div>

        <div class="sidebar-content-list">
          <!-- Loading state -->
          <div v-if="isLoading" class="loading-state">
            <Icon icon="carbon:circle-dash" width="20" class="spinning" />
            <span>加载中...</span>
          </div>
          
          <!-- Error state -->
          <div v-else-if="errorMessage" class="error-state">
            <Icon icon="carbon:warning" width="20" />
            <span>{{ errorMessage }}</span>
            <button @click="loadPlanTemplateList" class="retry-btn">重试</button>
          </div>
          
          <!-- Empty state -->
          <div v-else-if="planTemplateList.length === 0" class="empty-state">
            <Icon icon="carbon:document" width="32" />
            <span>没有可用的计划模板</span>
          </div>
          
          <!-- Plan template list -->
          <div 
            v-else
            v-for="template in sortedTemplates" 
            :key="template.id"
            class="sidebar-content-list-item"
            :class="{ 'sidebar-content-list-item-active': template.id === currentPlanTemplateId }"
            @click="handlePlanTemplateClick(template)"
          >
            <div class="task-icon">
              <Icon icon="carbon:document" width="20" />
            </div>
            <div class="task-details">
              <div class="task-title">{{ template.title || '未命名计划' }}</div>
              <div class="task-preview">{{ truncateText(template.description || '无描述', 40) }}</div>
            </div>
            <div class="task-time">{{ getRelativeTimeString(new Date(template.updateTime || template.createTime)) }}</div>
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
      <div v-else-if="currentTab === 'config'" class="tab-content config-tab">
        <div v-if="selectedTemplate" class="config-container">
          <!-- Template Info Header -->
          <div class="template-info-header">
            <div class="template-info">
              <h3>{{ selectedTemplate.title || '未命名计划' }}</h3>
              <span class="template-id">ID: {{ selectedTemplate.id }}</span>
            </div>
            <button class="back-to-list-btn" @click="currentTab = 'list'">
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
                  @click="handleRollback"
                  :disabled="!canRollback"
                  title="回滚"
                >
                  <Icon icon="carbon:undo" width="14" />
                </button>
                <button 
                  class="btn btn-sm"
                  @click="handleRestore"
                  :disabled="!canRestore"
                  title="恢复"
                >
                  <Icon icon="carbon:redo" width="14" />
                </button>
                <button 
                  class="btn btn-primary btn-sm"
                  @click="handleSaveTemplate"
                  :disabled="isGenerating || isExecuting"
                >
                  <Icon icon="carbon:save" width="14" />
                </button>
              </div>
            </div>
            <textarea
              v-model="jsonContent"
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
                v-model="generatorPrompt"
                class="prompt-input"
                placeholder="描述您想要生成的计划..."
                rows="3"
              ></textarea>
              <div class="generator-actions">
                <button 
                  class="btn btn-primary btn-sm"
                  @click="handleGeneratePlan"
                  :disabled="isGenerating || !generatorPrompt.trim()"
                >
                  <Icon 
                    :icon="isGenerating ? 'carbon:circle-dash' : 'carbon:generate'" 
                    width="14" 
                    :class="{ spinning: isGenerating }"
                  />
                  {{ isGenerating ? '生成中...' : '生成计划' }}
                </button>
                <button 
                  class="btn btn-secondary btn-sm"
                  @click="handleUpdatePlan"
                  :disabled="isGenerating || !generatorPrompt.trim() || !jsonContent.trim()"
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
                    v-model="executionParams"
                    class="params-input"
                    placeholder="输入执行参数..."
                  />
                  <button 
                    class="clear-params-btn"
                    @click="executionParams = ''"
                    title="清空参数"
                  >
                    <Icon icon="carbon:close" width="12" />
                  </button>
                </div>
              </div>
              <div class="api-url-display">
                <span class="api-url-label">API URL:</span>
                <code class="api-url">{{ computedApiUrl }}</code>
              </div>
              <button 
                class="btn btn-primary execute-btn"
                @click="handleExecutePlan"
                :disabled="isExecuting || isGenerating"
              >
                <Icon 
                  :icon="isExecuting ? 'carbon:circle-dash' : 'carbon:play'" 
                  width="16" 
                  :class="{ spinning: isExecuting }"
                />
                {{ isExecuting ? '执行中...' : '执行计划' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="sidebar-switch" @click="toggleSidebar">
      <div class="tb-line-wrapper" :class="{ 'tb-line-wrapper-expanded': !isCollapsed }">
        <div class="tb-line" :class="{ 'tb-line-expanded': !isCollapsed }"></div>
        <div class="bt-line" :class="{ 'bt-line-expanded': !isCollapsed }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Icon } from '@iconify/vue'

import router from '@/router'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate, PlanTemplateEvents } from '@/types/plan-template'

// Props and Emits
const emit = defineEmits<PlanTemplateEvents>()

// Reactive state - List Tab
const isCollapsed = ref(true)
const currentPlanTemplateId = ref<string | null>(null)
const planTemplateList = ref<PlanTemplate[]>([])
const isLoading = ref(false)
const errorMessage = ref<string>('')

// Reactive state - Tab Management
const currentTab = ref<'list' | 'config'>('list')
const selectedTemplate = ref<PlanTemplate | null>(null)

// Reactive state - Config Tab
const jsonContent = ref('')
const generatorPrompt = ref('')
const executionParams = ref('')
const isGenerating = ref(false)
const isExecuting = ref(false)

// Version control
const planVersions = ref<string[]>([])
const currentVersionIndex = ref(-1)

// Computed properties
const sortedTemplates = computed(() => {
  return [...planTemplateList.value].sort((a, b) => {
    const timeA = new Date(a.updateTime || a.createTime)
    const timeB = new Date(b.updateTime || b.createTime)
    return timeB.getTime() - timeA.getTime()
  })
})

const canRollback = computed(() => {
  return planVersions.value.length > 1 && currentVersionIndex.value > 0
})

const canRestore = computed(() => {
  return planVersions.value.length > 1 && currentVersionIndex.value < planVersions.value.length - 1
})

const computedApiUrl = computed(() => {
  if (!selectedTemplate.value) return ''
  const baseUrl = `/api/plan-template/executePlanByTemplateId/${selectedTemplate.value.id}`
  const params = executionParams.value.trim()
  return params ? `${baseUrl}?${encodeURIComponent(params)}` : baseUrl
})

// Methods
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleConfig = () => {
  router.push('/configs')
}

const handleNewTaskButtonClick = () => {
  // 清空当前选择
  currentPlanTemplateId.value = null
  
  // 发送事件
  emit('jsonContentClear')
  emit('planParamsChanged', { prompt: '', params: '' })
  emit('newTaskRequested')
}

const loadPlanTemplateList = async () => {
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    console.log('[PlanTemplateSidebar] 开始加载计划模板列表...')
    const response = await PlanActApiService.getAllPlanTemplates()
    
    // 处理 API 返回的数据结构: { count: number, templates: Array }
    if (response && response.templates && Array.isArray(response.templates)) {
      planTemplateList.value = response.templates
      console.log(`[PlanTemplateSidebar] 成功加载 ${response.templates.length} 个计划模板`)
    } else {
      planTemplateList.value = []
      console.warn('[PlanTemplateSidebar] API 返回的数据格式异常，使用空列表', response)
    }
  } catch (error: any) {
    console.error('[PlanTemplateSidebar] 加载计划模板列表失败:', error)
    planTemplateList.value = []
    errorMessage.value = `加载失败: ${error.message}`
  } finally {
    isLoading.value = false
  }
}

const handlePlanTemplateClick = async (template: PlanTemplate) => {
  // 更新本地状态
  currentPlanTemplateId.value = template.id
  selectedTemplate.value = template
  
  // 切换到配置标签页
  currentTab.value = 'config'
  
  // 加载模板数据
  await loadTemplateData(template)
  
  console.log(`[PlanTemplateSidebar] 选择了计划模板: ${template.id}`)
}

const handleDeletePlanTemplate = async (template: PlanTemplate) => {
  if (!template || !template.id) {
    console.warn("[PlanTemplateSidebar] handleDeletePlanTemplate: 无效的模板对象或ID")
    return
  }

  if (confirm(`确定要删除计划模板 "${template.title || '未命名计划'}" 吗？此操作不可恢复。`)) {
    try {
      await PlanActApiService.deletePlanTemplate(template.id)
      
      if (currentPlanTemplateId.value === template.id) {
        // 如果删除的是当前选中的模板，清空选择和相关内容
        currentPlanTemplateId.value = null
        emit('jsonContentClear')
        emit('planParamsChanged', {
          prompt: '',
          params: ''
        })
      }
      
      // 发送删除事件
      emit('planTemplateDeleted', { templateId: template.id })
      
      // 重新加载列表
      await loadPlanTemplateList()
      alert('计划模板已删除。')

    } catch (error: any) {
      console.error('删除计划模板失败:', error)
      alert('删除计划模板失败: ' + error.message)
      // 即使出错也刷新列表以确保一致性
      await loadPlanTemplateList()
    }
  }
}

// Config tab methods
const loadTemplateData = async (template: PlanTemplate) => {
  try {
    // Load versions
    const versionsResponse = await PlanActApiService.getPlanVersions(template.id)
    planVersions.value = versionsResponse.versions || []
    
    if (planVersions.value.length > 0) {
      const latestContent = planVersions.value[planVersions.value.length - 1]
      jsonContent.value = latestContent
      currentVersionIndex.value = planVersions.value.length - 1
      
      // Parse and set prompt from JSON if available
      try {
        const parsed = JSON.parse(latestContent)
        if (parsed.prompt) {
          generatorPrompt.value = parsed.prompt
        }
        if (parsed.params) {
          executionParams.value = parsed.params
        }
      } catch (e) {
        console.warn('无法解析JSON内容获取提示信息')
      }
    } else {
      jsonContent.value = ''
      generatorPrompt.value = ''
      executionParams.value = ''
    }
  } catch (error: any) {
    console.error('加载模板数据失败:', error)
    alert('加载模板数据失败: ' + error.message)
  }
}

const handleRollback = () => {
  if (canRollback.value) {
    currentVersionIndex.value--
    jsonContent.value = planVersions.value[currentVersionIndex.value]
  }
}

const handleRestore = () => {
  if (canRestore.value) {
    currentVersionIndex.value++
    jsonContent.value = planVersions.value[currentVersionIndex.value]
  }
}

const handleSaveTemplate = async () => {
  if (!selectedTemplate.value) return
  
  const content = jsonContent.value.trim()
  if (!content) {
    alert('内容不能为空。')
    return
  }

  try {
    // 尝试解析以验证格式
    JSON.parse(content)
  } catch (e: any) {
    alert('格式无效，请修正后再保存。\\n错误: ' + e.message)
    return
  }

  try {
    const saveResult = await PlanActApiService.savePlanTemplate(selectedTemplate.value.id, content)
    
    // 根据保存结果显示不同的消息
    if (saveResult.duplicate) {
      alert(`保存完成：${saveResult.message}\\n\\n当前版本数：${saveResult.versionCount}`)
    } else if (saveResult.saved) {
      // 保存到本地版本历史
      if (currentVersionIndex.value < planVersions.value.length - 1) {
        planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
      }
      planVersions.value.push(content)
      currentVersionIndex.value = planVersions.value.length - 1
      
      alert(`保存成功：${saveResult.message}\\n\\n当前版本数：${saveResult.versionCount}`)
    } else {
      alert(`保存状态：${saveResult.message}`)
    }
  } catch (error: any) {
    console.error('保存计划修改失败:', error)
    alert('保存计划修改失败: ' + error.message)
  }
}

const handleGeneratePlan = async () => {
  if (!generatorPrompt.value.trim()) return
  
  isGenerating.value = true
  
  try {
    const response = await PlanActApiService.generatePlan(generatorPrompt.value)
    jsonContent.value = response.planJson || ''
    
    // 保存到版本历史
    if (currentVersionIndex.value < planVersions.value.length - 1) {
      planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
    }
    planVersions.value.push(jsonContent.value)
    currentVersionIndex.value = planVersions.value.length - 1
    
    alert('计划生成成功！')
  } catch (error: any) {
    console.error('生成计划失败:', error)
    alert('生成计划失败: ' + error.message)
  } finally {
    isGenerating.value = false
  }
}

const handleUpdatePlan = async () => {
  if (!generatorPrompt.value.trim() || !jsonContent.value.trim()) return
  if (!selectedTemplate.value) return
  
  isGenerating.value = true
  
  try {
    const response = await PlanActApiService.updatePlanTemplate(
      selectedTemplate.value.id, 
      generatorPrompt.value, 
      jsonContent.value
    )
    jsonContent.value = response.planJson || ''
    
    // 保存到版本历史
    if (currentVersionIndex.value < planVersions.value.length - 1) {
      planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
    }
    planVersions.value.push(jsonContent.value)
    currentVersionIndex.value = planVersions.value.length - 1
    
    alert('计划更新成功！')
  } catch (error: any) {
    console.error('更新计划失败:', error)
    alert('更新计划失败: ' + error.message)
  } finally {
    isGenerating.value = false
  }
}

const handleExecutePlan = async () => {
  if (!selectedTemplate.value) return
  
  isExecuting.value = true
  
  try {
    const params = executionParams.value.trim()
    const response = params 
      ? await PlanActApiService.executePlan(selectedTemplate.value.id, params)
      : await PlanActApiService.executePlan(selectedTemplate.value.id)
    
    const query = `Executing plan template: ${selectedTemplate.value.id}`
    console.log('计划模板执行请求成功:', response)
    alert(`计划执行成功！计划ID: ${response.planId}`)
  } catch (error: any) {
    console.error('执行计划出错:', error)
    alert('执行计划失败: ' + error.message)
  } finally {
    isExecuting.value = false
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
  loadPlanTemplateList()
})

// 暴露方法供父组件调用
defineExpose({
  loadPlanTemplateList,
  currentPlanTemplateId: currentPlanTemplateId
})
</script>

<style scoped>
.sidebar-wrapper {
  position: relative;
  width: 280px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: width 0.3s ease-in-out;
  cursor: pointer;

  .sidebar-switch {
    position: absolute;
    top: 50%;
    right: -40px;
    transform: translateY(-50%);
    height: 80px;
    z-index: 10;

    .tb-line-wrapper {
      display: flex;
      flex-direction: column;
      gap: 6px;
      transition: all 0.3s ease-in-out;

      .tb-line,
      .bt-line {
        background-color: #667eea;
        width: 22px;
        height: 6px;
        border-radius: 10px;
        transform: rotate(-45deg);
        transition: all 0.3s ease-in-out;
      }
      .tb-line {
        background-color: #6646a2;
        transition: all 0.5s ease-in-out;
        transform: rotate(45deg);
      }

      .tb-line-expanded,
      .bt-line-expanded {
        width: 36px;
      }

      .tb-line-expanded {
        transform: rotate(45deg) translateY(9px);
      }

      .bt-line-expanded {
        transform: rotate(-45deg) translateY(-9px);
      }
    }

    .tb-line-wrapper-expanded {
      transform: rotate(180deg);
    }
  }
}
.sidebar-wrapper-collapsed {
  width: 1px;
}

.sidebar-content {
  height: 100%;
  width: 100%;
  padding: 12px 0 12px 12px;
  display: flex;
  flex-direction: column;

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

    .config-button {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      margin-right: 16px;
      cursor: pointer;
      border-radius: 4px;
      transition: background-color 0.2s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }
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
