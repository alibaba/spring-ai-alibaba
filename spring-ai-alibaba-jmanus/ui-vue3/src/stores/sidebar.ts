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
import { ref, computed } from 'vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate } from '@/types/plan-template'

export const useSidebarStore = defineStore('sidebar', () => {
  // 基本状态
  const isCollapsed = ref(true) // 默认隐藏侧边栏
  const currentTab = ref<'list' | 'config'>('list')

  // 模板列表相关状态
  const currentPlanTemplateId = ref<string | null>(null)
  const planTemplateList = ref<PlanTemplate[]>([])
  const selectedTemplate = ref<PlanTemplate | null>(null)
  const isLoading = ref(false)
  const errorMessage = ref<string>('')

  // 配置相关状态
  const jsonContent = ref('')
  const generatorPrompt = ref('')
  const executionParams = ref('')
  const isGenerating = ref(false)
  const isExecuting = ref(false)

  // 版本控制
  const planVersions = ref<string[]>([])
  const currentVersionIndex = ref(-1)

  // 计算属性
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
    return (
      planVersions.value.length > 1 && currentVersionIndex.value < planVersions.value.length - 1
    )
  })

  const computedApiUrl = computed(() => {
    if (!selectedTemplate.value) return ''
    const baseUrl = `/api/plan-template/executePlanByTemplateId/${selectedTemplate.value.id}`
    const params = executionParams.value.trim()
    return params ? `${baseUrl}?${encodeURIComponent(params)}` : baseUrl
  })

  // Actions
  const toggleSidebar = () => {
    isCollapsed.value = !isCollapsed.value
  }

  const showSidebar = () => {
    isCollapsed.value = false
  }

  const hideSidebar = () => {
    isCollapsed.value = true
  }

  const switchToTab = (tab: 'list' | 'config') => {
    currentTab.value = tab
  }

  const loadPlanTemplateList = async () => {
    isLoading.value = true
    errorMessage.value = ''

    try {
      console.log('[SidebarStore] 开始加载计划模板列表...')
      const response = await PlanActApiService.getAllPlanTemplates()

      if (response && response.templates && Array.isArray(response.templates)) {
        planTemplateList.value = response.templates
        console.log(`[SidebarStore] 成功加载 ${response.templates.length} 个计划模板`)
      } else {
        planTemplateList.value = []
        console.warn('[SidebarStore] API 返回的数据格式异常，使用空列表', response)
      }
    } catch (error: any) {
      console.error('[SidebarStore] 加载计划模板列表失败:', error)
      planTemplateList.value = []
      errorMessage.value = `加载失败: ${error.message}`
    } finally {
      isLoading.value = false
    }
  }

  const selectTemplate = async (template: PlanTemplate) => {
    currentPlanTemplateId.value = template.id
    selectedTemplate.value = template
    currentTab.value = 'config'

    // 加载模板数据
    await loadTemplateData(template)
    console.log(`[SidebarStore] 选择了计划模板: ${template.id}`)
  }

  const loadTemplateData = async (template: PlanTemplate) => {
    try {
      const versionsResponse = await PlanActApiService.getPlanVersions(template.id)
      planVersions.value = versionsResponse.versions || []

      if (planVersions.value.length > 0) {
        const latestContent = planVersions.value[planVersions.value.length - 1]
        jsonContent.value = latestContent
        currentVersionIndex.value = planVersions.value.length - 1

        // 解析并设置提示信息
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
      throw error
    }
  }

  const createNewTemplate = () => {
    const emptyTemplate: PlanTemplate = {
      id: `new-${Date.now()}`,
      title: '新建计划',
      description: '请使用计划生成器创建新的计划模板',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString(),
    }

    selectedTemplate.value = emptyTemplate
    currentPlanTemplateId.value = null
    jsonContent.value = ''
    generatorPrompt.value = ''
    executionParams.value = ''
    planVersions.value = []
    currentVersionIndex.value = -1
    currentTab.value = 'config'

    console.log('[SidebarStore] 创建新的空白计划模板，切换到配置标签页')
  }

  const deleteTemplate = async (template: PlanTemplate) => {
    if (!template || !template.id) {
      console.warn('[SidebarStore] deleteTemplate: 无效的模板对象或ID')
      return
    }

    try {
      await PlanActApiService.deletePlanTemplate(template.id)

      if (currentPlanTemplateId.value === template.id) {
        // 如果删除的是当前选中的模板，清空选择和相关内容
        clearSelection()
      }

      // 重新加载列表
      await loadPlanTemplateList()
      console.log(`[SidebarStore] 计划模板 ${template.id} 已删除`)
    } catch (error: any) {
      console.error('删除计划模板失败:', error)
      // 即使出错也刷新列表以确保一致性
      await loadPlanTemplateList()
      throw error
    }
  }

  const clearSelection = () => {
    currentPlanTemplateId.value = null
    selectedTemplate.value = null
    jsonContent.value = ''
    generatorPrompt.value = ''
    executionParams.value = ''
    planVersions.value = []
    currentVersionIndex.value = -1
    currentTab.value = 'list'
  }

  const clearExecutionParams = () => {
    executionParams.value = ''
  }

  const rollbackVersion = () => {
    if (canRollback.value) {
      currentVersionIndex.value--
      jsonContent.value = planVersions.value[currentVersionIndex.value]
    }
  }

  const restoreVersion = () => {
    if (canRestore.value) {
      currentVersionIndex.value++
      jsonContent.value = planVersions.value[currentVersionIndex.value]
    }
  }

  const saveTemplate = async () => {
    if (!selectedTemplate.value) return

    const content = jsonContent.value.trim()
    if (!content) {
      throw new Error('内容不能为空')
    }

    try {
      JSON.parse(content)
    } catch (e: any) {
      throw new Error('格式无效，请修正后再保存。\n错误: ' + e.message)
    }

    try {
      const saveResult = await PlanActApiService.savePlanTemplate(
        selectedTemplate.value.id,
        content
      )

      // 保存到本地版本历史
      if (currentVersionIndex.value < planVersions.value.length - 1) {
        planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
      }
      planVersions.value.push(content)
      currentVersionIndex.value = planVersions.value.length - 1

      return saveResult
    } catch (error: any) {
      console.error('保存计划修改失败:', error)
      throw error
    }
  }

  const generatePlan = async () => {
    if (!generatorPrompt.value.trim()) return

    isGenerating.value = true

    try {
      const response = await PlanActApiService.generatePlan(generatorPrompt.value)
      jsonContent.value = response.planJson || ''

      // 如果是新建的模板，更新模板信息
      if (selectedTemplate.value && selectedTemplate.value.id.startsWith('new-')) {
        let title = '新建计划模板'
        try {
          const planData = JSON.parse(response.planJson || '{}')
          title = planData.title || title
        } catch (e) {
          console.warn('无法解析计划JSON获取标题')
        }

        selectedTemplate.value = {
          id: response.planTemplateId,
          title: title,
          description: '通过生成器创建的计划模板',
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          planJson: response.planJson,
        }

        currentPlanTemplateId.value = response.planTemplateId
        await loadPlanTemplateList()
      }

      // 保存到版本历史
      if (currentVersionIndex.value < planVersions.value.length - 1) {
        planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
      }
      planVersions.value.push(jsonContent.value)
      currentVersionIndex.value = planVersions.value.length - 1

      return response
    } catch (error: any) {
      console.error('生成计划失败:', error)
      throw error
    } finally {
      isGenerating.value = false
    }
  }

  const updatePlan = async () => {
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

      return response
    } catch (error: any) {
      console.error('更新计划失败:', error)
      throw error
    } finally {
      isGenerating.value = false
    }
  }

  const preparePlanExecution = () => {
    if (!selectedTemplate.value) return null

    isExecuting.value = true

    try {
      let planData
      try {
        planData = JSON.parse(jsonContent.value)
        planData.planTemplateId = selectedTemplate.value.id
      } catch (e) {
        planData = {
          planTemplateId: selectedTemplate.value.id,
          planId: selectedTemplate.value.id,
          title: selectedTemplate.value.title || '执行计划',
          steps: [
            { stepRequirement: '[BROWSER_AGENT] 访问百度搜索阿里巴巴的最新股价' },
            { stepRequirement: '[DEFAULT_AGENT] 提取和整理搜索结果中的股价信息' },
            { stepRequirement: '[TEXT_FILE_AGENT] 创建一个文本文件记录查询结果' },
            { stepRequirement: '[DEFAULT_AGENT] 向用户报告查询结果' },
          ],
        }
      }

      const title = selectedTemplate.value.title || planData.title || '执行计划'

      return {
        title,
        planData,
        params: executionParams.value.trim() || undefined,
      }
    } catch (error: any) {
      console.error('准备计划执行失败:', error)
      isExecuting.value = false
      throw error
    }
  }

  const finishPlanExecution = () => {
    isExecuting.value = false
  }

  return {
    // 状态
    isCollapsed,
    currentTab,
    currentPlanTemplateId,
    planTemplateList,
    selectedTemplate,
    isLoading,
    errorMessage,
    jsonContent,
    generatorPrompt,
    executionParams,
    isGenerating,
    isExecuting,
    planVersions,
    currentVersionIndex,

    // 计算属性
    sortedTemplates,
    canRollback,
    canRestore,
    computedApiUrl,

    // Actions
    toggleSidebar,
    showSidebar,
    hideSidebar,
    switchToTab,
    loadPlanTemplateList,
    selectTemplate,
    createNewTemplate,
    deleteTemplate,
    clearSelection,
    clearExecutionParams,
    rollbackVersion,
    restoreVersion,
    saveTemplate,
    generatePlan,
    updatePlan,
    preparePlanExecution,
    finishPlanExecution,
  }
})
