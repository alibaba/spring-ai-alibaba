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
 * JsonEditor组件的业务逻辑
 */
export function useJsonEditor(props: JsonEditorProps, emit: JsonEditorEmits) {
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
   * 解析JSON内容为可视化数据
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
   * 将可视化数据转换回JSON
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
   * 计算属性：格式化的JSON输出
   */
  const formattedJsonOutput = computed(() => {
    return convertVisualToJson()
  })

  /**
   * 计算属性：可用的agent选项
   */
  const agentOptions = computed(() => {
    return availableAgents.value
  })

  /**
   * 计算属性：是否应该显示错误状态
   */
  const shouldShowError = computed(() => {
    return hasLoadedAgents.value && !isLoadingAgents.value && availableAgents.value.length === 0
  })

  /**
   * 加载可用的agents列表
   */
  const loadAvailableAgents = async () => {
    if (isLoadingAgents.value) return

    try {
      isLoadingAgents.value = true
      agentsLoadError.value = ''
      
      // 尝试从两个API获取agents数据
      const [configAgents, managementAgents] = await Promise.allSettled([
        AgentApiService.getAllAgents(),
        getAllAgents()
      ])

      const agents: AvailableAgent[] = []
      let hasSuccessfulCall = false

      // 处理AgentApiService的结果
      if (configAgents.status === 'fulfilled') {
        hasSuccessfulCall = true
        const configAgentList = configAgents.value.map((agent: Agent) => ({
          id: agent.name.toUpperCase().replace(/\s+/g, '_'),
          name: agent.name.toUpperCase().replace(/\s+/g, '_'),
          description: agent.description,
          agentType: agent.name
        }))
        agents.push(...configAgentList)
      }

      // 处理agent management API的结果  
      if (managementAgents.status === 'fulfilled') {
        hasSuccessfulCall = true
        const managementAgentList = managementAgents.value.map((agent: AgentEntity) => ({
          id: agent.agentName.toUpperCase().replace(/\s+/g, '_'),
          name: agent.agentName.toUpperCase().replace(/\s+/g, '_'),
          description: agent.agentDescription,
          agentType: agent.agentName
        }))
        agents.push(...managementAgentList)
      }

      // 如果两个API都失败了
      if (!hasSuccessfulCall) {
        throw new Error('Failed to load agents from both APIs')
      }

      // 去重（基于id）
      const uniqueAgents = agents.filter((agent, index, self) => 
        index === self.findIndex(a => a.id === agent.id)
      )

      availableAgents.value = uniqueAgents
      hasLoadedAgents.value = true

    } catch (error) {
      console.error('Failed to load agents:', error)
      agentsLoadError.value = error instanceof Error ? error.message : 'Failed to load agents'
      availableAgents.value = []
      hasLoadedAgents.value = true
    } finally {
      isLoadingAgents.value = false
    }
  }

  /**
   * 发送JSON更新事件
   */
  const emitJsonUpdate = () => {
    const jsonResult = convertVisualToJson()
    emit('update:jsonContent', jsonResult)
  }

  // Step management methods
  /**
   * 添加新步骤
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
   * 删除步骤
   */
  const removeStep = (index: number) => {
    parsedData.steps.splice(index, 1)
    emitJsonUpdate()
  }

  /**
   * 上移步骤
   */
  const moveStepUp = (index: number) => {
    if (index > 0) {
      const step = parsedData.steps.splice(index, 1)[0]
      parsedData.steps.splice(index - 1, 0, step)
      emitJsonUpdate()
    }
  }

  /**
   * 下移步骤
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
   * 处理回滚操作
   */
  const handleRollback = () => {
    emit('rollback')
  }

  /**
   * 处理恢复操作
   */
  const handleRestore = () => {
    emit('restore')
  }

  /**
   * 处理保存操作
   */
  const handleSave = () => {
    emit('save')
  }

  /**
   * 切换JSON预览显示
   */
  const toggleJsonPreview = () => {
    showJsonPreview.value = !showJsonPreview.value
  }

  /**
   * 关闭JSON预览
   */
  const closeJsonPreview = () => {
    showJsonPreview.value = false
  }

  // Watchers
  /**
   * 监听外部JSON内容变化
   */
  watch(() => props.jsonContent, (newContent) => {
    parseJsonToVisual(newContent)
  }, { immediate: true })

  /**
   * 监听可视化数据变化
   */
  watch(parsedData, () => {
    emitJsonUpdate()
  }, { deep: true })

  // 组件挂载后尝试加载agents一次
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
