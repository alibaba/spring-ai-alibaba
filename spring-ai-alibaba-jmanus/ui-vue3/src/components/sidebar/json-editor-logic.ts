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

import { reactive, computed, watch, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AgentApiService, type Agent } from '@/api/agent-api-service'
import { getAllAgents, type AgentEntity } from '@/api/agent'

// Types
export interface StepData {
  agentType: string
  stepContent: string
  terminateColumns: string
}

export interface AvailableAgent {
  id: string
  name: string
  description: string
  agentType?: string
  tools?: string[]
}

export interface ParsedPlanData {
  planType: string
  title: string
  directResponse: boolean
  steps: StepData[]
  planId: string
}

export interface JsonEditorProps {
  jsonContent: string
  canRollback: boolean
  canRestore: boolean
  isGenerating: boolean
  isExecuting: boolean
  hiddenFields?: string[]
}

export interface JsonEditorEmits {
  (e: 'rollback'): void
  (e: 'restore'): void
  (e: 'save'): void
  (e: 'update:jsonContent', value: string): void
}

/**
 * Business logic for JsonEditor component
 */
export function useJsonEditor(props: JsonEditorProps, emit: JsonEditorEmits) {
  // I18n
  const { t } = useI18n()
  
  // State
  const showJsonPreview = ref(false)
  const availableAgents = ref<AvailableAgent[]>([])
  const isLoadingAgents = ref(false)
  const agentsLoadError = ref<string>('')
  const hasLoadedAgents = ref(false)

  // Reactive parsed data
  const parsedData = reactive<ParsedPlanData>({
    planType: 'simple',
    title: '',
    directResponse: false,
    steps: [],
    planId: ''
  })

  /**
   * Parse JSON content into visual data
   */
  const parseJsonToVisual = (jsonContent: string) => {
    try {
      if (!jsonContent) {
        // Reset to default
        Object.assign(parsedData, {
          planType: 'simple',
          title: '',
          directResponse: false,
          steps: [],
          planId: ''
        })
        return
      }

      const parsed = JSON.parse(jsonContent)
      
      parsedData.planType = 'simple' // Always use simple as default
      parsedData.title = parsed.title || ''
      parsedData.directResponse = false // Always use false as default
      parsedData.planId = parsed.planId || ''
      
      // Parse steps
      parsedData.steps = (parsed.steps || []).map((step: any) => {
        // Extract agent type from stepRequirement
        let agentType = 'SWEAGENT'
        let stepContent = step.stepRequirement || ''
        
        const agentMatch = stepContent.match(/^\[([^\]]+)\]/)
        if (agentMatch) {
          agentType = agentMatch[1]
          stepContent = stepContent.replace(/^\[[^\]]+\]\s*/, '')
        }
        
        return {
          agentType,
          stepContent,
          terminateColumns: step.terminateColumns || ''
        }
      })
    } catch (error) {
      console.warn('Failed to parse JSON content:', error)
      // Keep current data if parsing fails
    }
  }

  /**
   * Convert visual data back to JSON
   */
  const convertVisualToJson = (): string => {
    try {
      const result: any = {
        planType: parsedData.planType,
        title: parsedData.title,
        directResponse: parsedData.directResponse,
        planId: parsedData.planId,
        steps: parsedData.steps.map(step => ({
          stepRequirement: `[${step.agentType}] ${step.stepContent}`,
          terminateColumns: step.terminateColumns
        }))
      }
      
      return JSON.stringify(result, null, 2)
    } catch (error) {
      console.warn('Failed to convert visual data to JSON:', error)
      return props.jsonContent
    }
  }

  /**
   * Computed property: Formatted JSON output
   */
  const formattedJsonOutput = computed(() => {
    return convertVisualToJson()
  })

  /**
   * Computed property: Available agent options
   */
  const agentOptions = computed(() => {
    return availableAgents.value
  })

  /**
   * Computed property: Whether error status should be displayed
   */
  const shouldShowError = computed(() => {
    return hasLoadedAgents.value && !isLoadingAgents.value && availableAgents.value.length === 0
  })

  /**
   * Format agent display text (with truncated description)
   */
  const formatAgentDisplayText = (agent: AvailableAgent, maxDescLength = 20): string => {
    const description = agent.description || ''
    const truncatedDesc = description.length > maxDescLength 
      ? description.substring(0, maxDescLength) + '...' 
      : description
    
    return truncatedDesc ? `[${agent.name}] ${truncatedDesc}` : `[${agent.name}]`
  }

  /**
   * Generate full tooltip text for agent
   */
  const generateAgentTooltip = (agent: AvailableAgent): string => {
    let tooltip = agent.description || agent.name
    
    if (agent.tools && agent.tools.length > 0) {
      tooltip += '\n\n' + t('sidebar.availableTools') + ':\n'
      tooltip += agent.tools.map(tool => `• ${tool}`).join('\n')
    }
    
    return tooltip
  }

  /**
   * Load list of available agents
   */
  const loadAvailableAgents = async () => {
    if (isLoadingAgents.value) return

    try {
      isLoadingAgents.value = true
      agentsLoadError.value = ''
      
      // Try to get agents data from two APIs
      const [configAgents, managementAgents] = await Promise.allSettled([
        AgentApiService.getAllAgents(),
        getAllAgents()
      ])

      const agents: AvailableAgent[] = []
      let hasSuccessfulCall = false

      // Process AgentApiService results
      if (configAgents.status === 'fulfilled') {
        hasSuccessfulCall = true
        const configAgentList = configAgents.value.map((agent: Agent) => ({
          id: agent.name.toUpperCase().replace(/\s+/g, '_'),
          name: agent.name.toUpperCase().replace(/\s+/g, '_'),
          description: agent.description,
          agentType: agent.name,
          tools: agent.availableTools || []
        }))
        agents.push(...configAgentList)
      }

      // Process agent management API results  
      if (managementAgents.status === 'fulfilled') {
        hasSuccessfulCall = true
        const managementAgentList = managementAgents.value.map((agent: AgentEntity) => ({
          id: agent.agentName.toUpperCase().replace(/\s+/g, '_'),
          name: agent.agentName.toUpperCase().replace(/\s+/g, '_'),
          description: agent.agentDescription,
          agentType: agent.agentName,
          tools: agent.availableToolKeys || []
        }))
        agents.push(...managementAgentList)
      }

      // If both APIs failed
      if (!hasSuccessfulCall) {
        throw new Error(t('sidebar.agentLoadError'))
      }

      // Deduplicate (based on id)
      const uniqueAgents = agents.filter((agent, index, self) => 
        index === self.findIndex(a => a.id === agent.id)
      )

      availableAgents.value = uniqueAgents
      hasLoadedAgents.value = true

    } catch (error) {
      console.error('Failed to load agents:', error)
      agentsLoadError.value = error instanceof Error ? error.message : t('sidebar.agentLoadError')
      availableAgents.value = []
      hasLoadedAgents.value = true
    } finally {
      isLoadingAgents.value = false
    }
  }

  /**
   * Emit JSON update event
   */
  const emitJsonUpdate = () => {
    const jsonResult = convertVisualToJson()
    emit('update:jsonContent', jsonResult)
  }

  // Step management methods
  /**
   * Add new step
   */
  const addStep = () => {
    parsedData.steps.push({
      agentType: 'SWEAGENT',
      stepContent: '',
      terminateColumns: ''
    })
    emitJsonUpdate()
  }

  /**
   * Remove step
   */
  const removeStep = (index: number) => {
    parsedData.steps.splice(index, 1)
    emitJsonUpdate()
  }

  /**
   * Move step up
   */
  const moveStepUp = (index: number) => {
    if (index > 0) {
      const step = parsedData.steps.splice(index, 1)[0]
      parsedData.steps.splice(index - 1, 0, step)
      emitJsonUpdate()
    }
  }

  /**
   * Move step down
   */
  const moveStepDown = (index: number) => {
    if (index < parsedData.steps.length - 1) {
      const step = parsedData.steps.splice(index, 1)[0]
      parsedData.steps.splice(index + 1, 0, step)
      emitJsonUpdate()
    }
  }

  // Event handlers
  /**
   * Handle rollback operation
   */
  const handleRollback = () => {
    emit('rollback')
  }

  /**
   * Handle restore operation
   */
  const handleRestore = () => {
    emit('restore')
  }

  /**
   * Handle save operation
   */
  const handleSave = () => {
    emit('save')
  }

  /**
   * Toggle JSON preview display
   */
  const toggleJsonPreview = () => {
    showJsonPreview.value = !showJsonPreview.value
  }

  /**
   * Close JSON preview
   */
  const closeJsonPreview = () => {
    showJsonPreview.value = false
  }

  // Watchers
  /**
   * Watch external JSON content changes
   */
  watch(() => props.jsonContent, (newContent) => {
    parseJsonToVisual(newContent)
  }, { immediate: true })

  /**
   * Watch visual data changes
   */
  watch(parsedData, () => {
    emitJsonUpdate()
  }, { deep: true })

  // Try to load agents once after component mounts
  loadAvailableAgents()

  return {
    // State
    showJsonPreview,
    parsedData,
    availableAgents,
    isLoadingAgents,
    agentsLoadError,
    hasLoadedAgents,
    
    // Computed
    formattedJsonOutput,
    agentOptions,
    shouldShowError,
    
    // Methods
    parseJsonToVisual,
    convertVisualToJson,
    emitJsonUpdate,
    loadAvailableAgents,
    formatAgentDisplayText,
    generateAgentTooltip,
    addStep,
    removeStep,
    moveStepUp,
    moveStepDown,
    handleRollback,
    handleRestore,
    handleSave,
    toggleJsonPreview,
    closeJsonPreview
  }
}
