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


import { reactive } from 'vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate } from '@/types/plan-template'
import { i18n } from '@/base/i18n'

type TabType = 'list' | 'config'

export class SidebarStore {
  // Basic state
  isCollapsed = true
  currentTab: TabType = 'list'

  // Template list related state
  currentPlanTemplateId: string | null = null
  planTemplateList: PlanTemplate[] = []
  selectedTemplate: PlanTemplate | null = null
  isLoading = false
  errorMessage = ''

  // Configuration related state
  jsonContent = ''
  generatorPrompt = ''
  executionParams = ''
  isGenerating = false
  isExecuting = false

  // Version control
  planVersions: string[] = []
  currentVersionIndex = -1

  // Computed properties
  get sortedTemplates(): PlanTemplate[] {
    return [...this.planTemplateList].sort((a, b) => {
      const timeA = new Date(a.updateTime ?? a.createTime)
      const timeB = new Date(b.updateTime ?? b.createTime)
      return timeB.getTime() - timeA.getTime()
    })
  }

  get canRollback(): boolean {
    return this.planVersions.length > 1 && this.currentVersionIndex > 0
  }

  get canRestore(): boolean {
    return (
      this.planVersions.length > 1 && this.currentVersionIndex < this.planVersions.length - 1
    )
  }

  get computedApiUrl(): string {
  if (!this.selectedTemplate) return ''
  const baseUrl = `/api/plan-template/execute/${this.selectedTemplate.id}`
  const params = this.executionParams.trim()
  // GET 方式，参数名为 allParams
  return params ? `${baseUrl}?allParams=${encodeURIComponent(params)}` : baseUrl
  }

  // Actions
  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed
  }

  switchToTab(tab: TabType) {
    this.currentTab = tab
  }

  async loadPlanTemplateList() {
    this.isLoading = true
    this.errorMessage = ''
    try {
      console.log('[SidebarStore] Starting to load plan template list...')
      const response = await PlanActApiService.getAllPlanTemplates()
      if (response?.templates && Array.isArray(response.templates)) {
        this.planTemplateList = response.templates
        console.log(`[SidebarStore] Successfully loaded ${response.templates.length} plan templates`)
      } else {
        this.planTemplateList = []
        console.warn('[SidebarStore] API returned abnormal data format, using empty list', response)
      }
    } catch (error: any) {
      console.error('[SidebarStore] Failed to load plan template list:', error)
      this.planTemplateList = []
      this.errorMessage = `Load failed: ${error.message}`
    } finally {
      this.isLoading = false
    }
  }

  async selectTemplate(template: PlanTemplate) {
    this.currentPlanTemplateId = template.id
    this.selectedTemplate = template
    this.currentTab = 'config'
    await this.loadTemplateData(template)
    console.log(`[SidebarStore] Selected plan template: ${template.id}`)
  }

  async loadTemplateData(template: PlanTemplate) {
    try {
      const versionsResponse = await PlanActApiService.getPlanVersions(template.id)
      this.planVersions = versionsResponse.versions || []
      if (this.planVersions.length > 0) {
        const latestContent = this.planVersions[this.planVersions.length - 1]
        this.jsonContent = latestContent
        this.currentVersionIndex = this.planVersions.length - 1
        try {
          const parsed = JSON.parse(latestContent)
          if (parsed.prompt) {
            this.generatorPrompt = parsed.prompt
          }
          if (parsed.params) {
            this.executionParams = parsed.params
          }
        } catch {
          console.warn('Unable to parse JSON content to get prompt information')
        }
      } else {
        this.jsonContent = ''
        this.generatorPrompt = ''
        this.executionParams = ''
      }
    } catch (error: any) {
      console.error('Failed to load template data:', error)
      throw error
    }
  }

  createNewTemplate() {
    const emptyTemplate: PlanTemplate = {
      id: `new-${Date.now()}`,
      title: i18n.global.t('sidebar.newTemplateName'),
      description: i18n.global.t('sidebar.newTemplateDescription'),
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString(),
    }
    this.selectedTemplate = emptyTemplate
    this.currentPlanTemplateId = null
    this.jsonContent = ''
    this.generatorPrompt = ''
    this.executionParams = ''
    this.planVersions = []
    this.currentVersionIndex = -1
    this.currentTab = 'config'
    console.log('[SidebarStore] Created new empty plan template, switching to config tab')
  }

  async deleteTemplate(template: PlanTemplate) {
    if (!template.id) {
      console.warn('[SidebarStore] deleteTemplate: Invalid template object or ID')
      return
    }
    try {
      await PlanActApiService.deletePlanTemplate(template.id)
      if (this.currentPlanTemplateId === template.id) {
        this.clearSelection()
      }
      await this.loadPlanTemplateList()
      console.log(`[SidebarStore] Plan template ${template.id} has been deleted`)
    } catch (error: any) {
      console.error('Failed to delete plan template:', error)
      await this.loadPlanTemplateList()
      throw error
    }
  }

  clearSelection() {
    this.currentPlanTemplateId = null
    this.selectedTemplate = null
    this.jsonContent = ''
    this.generatorPrompt = ''
    this.executionParams = ''
    this.planVersions = []
    this.currentVersionIndex = -1
    this.currentTab = 'list'
  }

  clearExecutionParams() {
    this.executionParams = ''
  }

  rollbackVersion() {
    if (this.canRollback) {
      this.currentVersionIndex--
      this.jsonContent = this.planVersions[this.currentVersionIndex]
    }
  }

  restoreVersion() {
    if (this.canRestore) {
      this.currentVersionIndex++
      this.jsonContent = this.planVersions[this.currentVersionIndex]
    }
  }

  async saveTemplate() {
    if (!this.selectedTemplate) return
    const content = this.jsonContent.trim()
    if (!content) {
      throw new Error('Content cannot be empty')
    }
    try {
      JSON.parse(content)
    } catch (e: any) {
      throw new Error('Invalid format, please correct and save.\nError: ' + e.message)
    }
    try {
      const saveResult = await PlanActApiService.savePlanTemplate(
        this.selectedTemplate.id,
        content
      )
      if (this.currentVersionIndex < this.planVersions.length - 1) {
        this.planVersions = this.planVersions.slice(0, this.currentVersionIndex + 1)
      }
      this.planVersions.push(content)
      this.currentVersionIndex = this.planVersions.length - 1
      return saveResult
    } catch (error: any) {
      console.error('Failed to save plan template:', error)
      throw error
    }
  }

  async generatePlan() {
    if (!this.generatorPrompt.trim()) return
    this.isGenerating = true
    try {
      const response = await PlanActApiService.generatePlan(this.generatorPrompt)
      this.jsonContent = response.planJson || ''
      if (this.selectedTemplate && this.selectedTemplate.id.startsWith('new-')) {
        let title = 'New Plan Template'
        try {
          const planData = JSON.parse(response.planJson || '{}')
          title = planData.title || title
        } catch {
          console.warn('Unable to parse plan JSON to get title')
        }
        this.selectedTemplate = {
          id: response.planTemplateId,
          title: title,
          description: i18n.global.t('sidebar.generatedTemplateDescription'),
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          planJson: response.planJson,
        }
        this.currentPlanTemplateId = response.planTemplateId
        await this.loadPlanTemplateList()
      }
      if (this.currentVersionIndex < this.planVersions.length - 1) {
        this.planVersions = this.planVersions.slice(0, this.currentVersionIndex + 1)
      }
      this.planVersions.push(this.jsonContent)
      this.currentVersionIndex = this.planVersions.length - 1
      return response
    } catch (error: any) {
      console.error('Failed to generate plan:', error)
      throw error
    } finally {
      this.isGenerating = false
    }
  }

  async updatePlan() {
    if (!this.generatorPrompt.trim() || !this.jsonContent.trim()) return
    if (!this.selectedTemplate) return
    this.isGenerating = true
    try {
      const response = await PlanActApiService.updatePlanTemplate(
        this.selectedTemplate.id,
        this.generatorPrompt,
        this.jsonContent
      )
      this.jsonContent = response.planJson || ''
      if (this.currentVersionIndex < this.planVersions.length - 1) {
        this.planVersions = this.planVersions.slice(0, this.currentVersionIndex + 1)
      }
      this.planVersions.push(this.jsonContent)
      this.currentVersionIndex = this.planVersions.length - 1
      return response
    } catch (error: any) {
      console.error('Failed to update plan:', error)
      throw error
    } finally {
      this.isGenerating = false
    }
  }

  preparePlanExecution() {
    if (!this.selectedTemplate) return null
    this.isExecuting = true
    try {
      let planData
      try {
        planData = JSON.parse(this.jsonContent)
        planData.planTemplateId = this.selectedTemplate.id
      } catch {
        planData = {
          planTemplateId: this.selectedTemplate.id,
          planId: this.selectedTemplate.id,
          title: this.selectedTemplate.title ?? i18n.global.t('sidebar.defaultExecutionPlanTitle'),
          steps: [
            { stepRequirement: '[BROWSER_AGENT] 访问百度搜索阿里巴巴的最新股价' },
            { stepRequirement: '[DEFAULT_AGENT] 提取和整理搜索结果中的股价信息' },
            { stepRequirement: '[TEXT_FILE_AGENT] 创建一个文本文件记录查询结果' },
            { stepRequirement: '[DEFAULT_AGENT] 向用户报告查询结果' },
          ],
        }
      }
      const title = this.selectedTemplate.title ?? planData.title ?? 'Execution Plan'
      return {
        title,
        planData,
        params: this.executionParams.trim() || undefined,
      }
    } catch (error: any) {
      console.error('Failed to prepare plan execution:', error)
      this.isExecuting = false
      throw error
    }
  }

  finishPlanExecution() {
    this.isExecuting = false
  }
}

export const sidebarStore = reactive(new SidebarStore())

