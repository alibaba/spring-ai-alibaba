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

    <div class="sidebar-switch" @click="toggleSidebar">
      <div class="tb-line-wrapper" :class="{ 'tb-line-wrapper-expanded': !isCollapsed }">
        <div class="tb-line" :class="{ 'tb-line-expanded': !isCollapsed }"></div>
        <div class="bt-line" :class="{ 'bt-line-expanded': !isCollapsed }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'

import router from '@/router'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate, PlanTemplateEvents } from '@/types/plan-template'

// Props and Emits
const emit = defineEmits<PlanTemplateEvents>()

// Reactive state
const isCollapsed = ref(true)
const currentPlanTemplateId = ref<string | null>(null)
const planTemplateList = ref<PlanTemplate[]>([])
const isLoading = ref(false)
const errorMessage = ref<string>('')

// Computed properties
const sortedTemplates = computed(() => {
  return [...planTemplateList.value].sort((a, b) => {
    const timeA = new Date(a.updateTime || a.createTime)
    const timeB = new Date(b.updateTime || b.createTime)
    return timeB.getTime() - timeA.getTime()
  })
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
  // 发送计划模板选择事件
  emit('planTemplateSelected', { templateId: template.id, template })
  console.log(`[PlanTemplateSidebar] 选择了计划模板: ${template.id}`)
  
  try {
    const versionsResponse = await PlanActApiService.getPlanVersions(template.id)
    const planVersionsList = versionsResponse.versions || []

    if (planVersionsList.length > 0) {
      const latestPlanJson = planVersionsList[planVersionsList.length - 1]
      
      // 发送版本历史
      emit('planVersionsLoaded', { 
        templateId: template.id, 
        versions: planVersionsList 
      })
      
      // 设置JSON内容
      emit('jsonContentSet', { 
        content: latestPlanJson 
      })

      try {
        const parsedPlan = JSON.parse(latestPlanJson)
        
        // 发送计划参数变化
        emit('planParamsChanged', {
          prompt: parsedPlan.prompt || '',
          params: parsedPlan.params || ''
        })
      } catch (parseError) { 
        console.warn('解析计划JSON时出错:', parseError)
        // 发送空的计划参数
        emit('planParamsChanged', {
          prompt: '',
          params: ''
        })
      }
    } else {
      // 清空JSON内容和参数
      emit('jsonContentClear')
      emit('planParamsChanged', {
        prompt: '',
        params: ''
      })
    }

    // 更新本地状态
    currentPlanTemplateId.value = template.id
  } catch (error: any) {
    console.error('加载计划模板详情失败:', error)
    alert('加载计划模板详情失败: ' + error.message)
    
    // 清空相关内容
    emit('jsonContentClear')
    emit('planParamsChanged', {
      prompt: '',
      params: ''
    })
  }
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
