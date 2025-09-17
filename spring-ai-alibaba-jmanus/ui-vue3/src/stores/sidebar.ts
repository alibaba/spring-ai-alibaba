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
  planType = 'dynamic_agent'
  generatorPrompt = ''
  executionParams = ''
  isGenerating = false
  isExecuting = false

  // Version control
  planVersions: string[] = []
  currentVersionIndex = -1

  // Available tools state
  availableTools: Array<{
    key: string
    name: string
    description: string
    enabled: boolean
    serviceGroup?: string
  }> = []
  isLoadingTools = false
  toolsLoadError = ''

  constructor() {
    // Ensure properties are properly initialized
    this.planVersions = []
    this.currentVersionIndex = -1
  }

  // Helper function to parse date from different formats
  parseDateTime(dateValue: any): Date {
    if (!dateValue) {
      return new Date()
    }

    // If array format [year, month, day, hour, minute, second, nanosecond]
    if (Array.isArray(dateValue) && dateValue.length >= 6) {
      // JavaScript Date constructor months start from 0, so subtract 1
      return new Date(dateValue[0], dateValue[1] - 1, dateValue[2], dateValue[3], dateValue[4], dateValue[5], Math.floor(dateValue[6] / 1000000))
    }

    // If string format, parse directly
    if (typeof dateValue === 'string') {
      return new Date(dateValue)
    }

    // Return current time for other cases
    return new Date()
  }

  // Computed properties
  get sortedTemplates(): PlanTemplate[] {
    return [...this.planTemplateList].sort((a, b) => {
      const timeA = this.parseDateTime(a.updateTime ?? a.createTime)
      const timeB = this.parseDateTime(b.updateTime ?? b.createTime)
      return timeB.getTime() - timeA.getTime()
    })
  }

  get canRollback(): boolean {
    return this.planVersions && this.planVersions.length > 1 && this.currentVersionIndex > 0
  }

  get canRestore(): boolean {
    return (
      this.planVersions && this.planVersions.length > 1 && this.currentVersionIndex < this.planVersions.length - 1
    )
  }

  get computedApiUrl(): string {
  if (!this.selectedTemplate) return ''
  const baseUrl = `/api/plan-template/execute/${this.selectedTemplate.id}`
  const params = this.executionParams.trim()
  // GET method, parameter name is allParams
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
          // Update planType based on the loaded template's JSON content
          if (parsed.planType) {
            this.planType = parsed.planType
            console.log(`[SidebarStore] Updated planType to: ${this.planType}`)
          }
        } catch {
          console.warn('Unable to parse JSON content to get prompt information')
        }
      } else {
        this.jsonContent = ''
        this.generatorPrompt = ''
        this.executionParams = ''
        this.planType = 'dynamic_agent'
      }
    } catch (error: any) {
      console.error('Failed to load template data:', error)
      throw error
    }
  }

  createNewTemplate(planType: string) {
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
    // Reset to default planType for new templates
    this.planType = planType
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
    if (this.canRollback && this.planVersions && this.currentVersionIndex > 0) {
      this.currentVersionIndex--
      this.jsonContent = this.planVersions[this.currentVersionIndex] || ''
    }
  }

  restoreVersion() {
    if (this.canRestore && this.planVersions && this.currentVersionIndex < this.planVersions.length - 1) {
      this.currentVersionIndex++
      this.jsonContent = this.planVersions[this.currentVersionIndex] || ''
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
      
      // Update the selected template ID with the real planId returned from backend
      if (saveResult?.planId && this.selectedTemplate.id.startsWith('new-')) {
        console.log('[SidebarStore] Updating template ID from', this.selectedTemplate.id, 'to', saveResult.planId)
        this.selectedTemplate.id = saveResult.planId
        this.currentPlanTemplateId = saveResult.planId
      }
      
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
      const response = await PlanActApiService.generatePlan(this.generatorPrompt, undefined, this.planType)
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
        this.jsonContent,
        this.planType
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
          title: this.selectedTemplate.title ?? i18n.global.t('sidebar.defaultExecutionPlanTitle'),
          steps: [
            { stepRequirement: '[BROWSER_AGENT] Visit Baidu to search for Alibaba\'s latest stock price' },
            { stepRequirement: '[DEFAULT_AGENT] Extract and organize stock price information from search results' },
            { stepRequirement: '[TEXT_FILE_AGENT] Create a text file to record query results' },
            { stepRequirement: '[DEFAULT_AGENT] Report query results to user' },
          ],
        }
      }
      const title = this.selectedTemplate.title ?? planData.title ?? 'Execution Plan'
      return {
        title,
        planData,
        params: this.executionParams.trim() || undefined,
        replacementParams: undefined as Record<string, string> | undefined
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

  // Load available tools from backend
  async loadAvailableTools() {
    if (this.isLoadingTools) {
      return // Avoid duplicate requests
    }

    this.isLoadingTools = true
    this.toolsLoadError = ''

    try {
      console.log('[SidebarStore] Loading available tools...')
      const response = await fetch('/api/agents/tools')
      
      if (response.ok) {
        const tools = await response.json()
        console.log('[SidebarStore] Loaded available tools:', tools)
        this.availableTools = tools
      } else {
        console.error('[SidebarStore] Failed to load tools:', response.statusText)
        this.toolsLoadError = `Failed to load tools: ${response.statusText}`
        this.availableTools = []
      }
    } catch (error) {
      console.error('[SidebarStore] Error loading tools:', error)
      this.toolsLoadError = error instanceof Error ? error.message : 'Unknown error'
      this.availableTools = []
    } finally {
      this.isLoadingTools = false
    }
  }
}

export const sidebarStore = reactive(new SidebarStore())

