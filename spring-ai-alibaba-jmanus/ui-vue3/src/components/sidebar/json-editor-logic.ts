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
// import { useI18n } from 'vue-i18n' // Currently unused

// Types - Based on backend ExecutionStep.java
export interface StepData {
  stepRequirement: string
  agentName: string
  modelName: string | null
  selectedToolKeys: string[]
  terminateColumns: string
  agentType?: string
  stepContent: string
}

// Types - Based on backend DynamicAgentExecutionPlan.java
export interface ParsedPlanData {
  title: string
  steps: StepData[]
  directResponse: boolean
  planTemplateId?: string
  planType?: string
}

export interface JsonEditorProps {
  jsonContent: string
  canRollback: boolean
  canRestore: boolean
  isGenerating: boolean
  isExecuting: boolean
  hiddenFields?: string[]
  currentPlanTemplateId: string
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
  // const { t } = useI18n() // Currently unused
  
  // State
  const showJsonPreview = ref(false)

  // Reactive parsed data - Based on DynamicAgentExecutionPlan structure
  const parsedData = reactive<ParsedPlanData>({
    title: '',
    steps: [],
    directResponse: false, // Always false for dynamic agent planning
    planTemplateId: props.currentPlanTemplateId || '',
    planType: 'dynamic_agent'
  })

  /**
   * Parse JSON content into visual data
   * Maps backend DynamicAgentExecutionPlan structure to frontend ParsedPlanData
   */
  const parseJsonToVisual = (jsonContent: string) => {
    try {
      if (!jsonContent) {
        // Reset to default - matches DynamicAgentExecutionPlan structure
        Object.assign(parsedData, {
          title: '',
          steps: [],
          directResponse: false,
          planTemplateId: props.currentPlanTemplateId || '',
          planType: 'dynamic_agent'
        })
        return
      }

      const parsed = JSON.parse(jsonContent)
      
      // Map basic fields from DynamicAgentExecutionPlan
      parsedData.title = parsed.title || ''
      parsedData.directResponse = false // Always false for dynamic agent planning
      parsedData.planTemplateId = parsed.planTemplateId || props.currentPlanTemplateId || ''
      parsedData.planType = parsed.planType || 'dynamic_agent'
      
      // Parse steps - maps ExecutionStep structure
      parsedData.steps = (parsed.steps || []).map((step: any) => ({
        stepRequirement: step.stepRequirement || '',
        agentName: step.agentName || '',
        modelName: step.modelName || null, // Default to null if not specified
        selectedToolKeys: step.selectedToolKeys || [],
        terminateColumns: step.terminateColumns || ''
      }))
    } catch (error) {
      console.warn('Failed to parse JSON content:', error)
      // Keep current data if parsing fails
    }
  }

  /**
   * Convert visual data back to JSON
   * Maps frontend ParsedPlanData to backend DynamicAgentExecutionPlan structure
   */
  const convertVisualToJson = (): string => {
    try {
      const result: any = {
        title: parsedData.title,
        steps: parsedData.steps.map(step => ({
          stepRequirement: step.stepRequirement,
          agentName: step.agentName,
          modelName: step.modelName || '', // Convert null to empty string for JSON
          selectedToolKeys: step.selectedToolKeys,
          terminateColumns: step.terminateColumns
        })),
        directResponse: parsedData.directResponse,
        planTemplateId: parsedData.planTemplateId,
        planType: parsedData.planType
      }
      
      return JSON.stringify(result, null, 2)
    } catch (error) {
      console.error('Failed to convert visual data to JSON:', error)
      return '{}'
    }
  }

  // Computed properties
  const formattedJsonOutput = computed(() => convertVisualToJson())

  // Watch for JSON content changes
  watch(() => props.jsonContent, (newContent) => {
    parseJsonToVisual(newContent)
  }, { immediate: true })

  // Watch for parsed data changes and emit updates
  watch(parsedData, () => {
    const jsonOutput = convertVisualToJson()
    emit('update:jsonContent', jsonOutput)
  }, { deep: true })

  // Watch for currentPlanTemplateId changes
  watch(() => props.currentPlanTemplateId, (newId) => {
    if (newId) {
      parsedData.planTemplateId = newId
    }
  })

  // Step management functions
  const addStep = () => {
    const newStep: StepData = {
      stepRequirement: '',
      agentName: 'ConfigurableDynaAgent', // Default agent name
      modelName: null, // Default to null (no model selected)
      selectedToolKeys: [],
      terminateColumns: '',
      agentType: '',
      stepContent: ''
    }
    parsedData.steps.push(newStep)
  }

  const removeStep = (index: number) => {
    if (index >= 0 && index < parsedData.steps.length) {
      parsedData.steps.splice(index, 1)
    }
  }

  const moveStepUp = (index: number) => {
    if (index > 0) {
      const step = parsedData.steps.splice(index, 1)[0]
      parsedData.steps.splice(index - 1, 0, step)
    }
  }

  const moveStepDown = (index: number) => {
    if (index < parsedData.steps.length - 1) {
      const step = parsedData.steps.splice(index, 1)[0]
      parsedData.steps.splice(index + 1, 0, step)
    }
  }

  // JSON preview functions
  const toggleJsonPreview = () => {
    showJsonPreview.value = !showJsonPreview.value
  }

  const closeJsonPreview = () => {
    showJsonPreview.value = false
  }

  // Action handlers
  const handleRollback = () => {
    emit('rollback')
  }

  const handleRestore = () => {
    emit('restore')
  }

  const handleSave = () => {
    emit('save')
  }

  return {
    // State
    showJsonPreview,
    parsedData,
    formattedJsonOutput,
    
    // Step management
    addStep,
    removeStep,
    moveStepUp,
    moveStepDown,
    
    // JSON preview
    toggleJsonPreview,
    closeJsonPreview,
    
    // Actions
    handleRollback,
    handleRestore,
    handleSave
  }
}
