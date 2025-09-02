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
  <div class="execution-details">
    <!-- Plan overview -->
    <div class="plan-overview" v-if="planExecution">
      <div class="plan-header">
        <h3 class="plan-title">{{ planExecution.title || $t('chat.planExecution') }}</h3>
        <div class="plan-status-badge" :class="getPlanStatusClass()">
          {{ getPlanStatusText() }}
        </div>
      </div>
      
      <!-- Parent tool call information for sub-plans -->
      <div v-if="planExecution.parentActToolCall" class="parent-tool-call">
        <div class="parent-tool-header">
          <Icon icon="carbon:flow" class="tool-icon" />
          <span class="tool-label">{{ $t('chat.triggeredByTool') }}:</span>
          <span class="tool-name">{{ planExecution.parentActToolCall.name }}</span>
        </div>
        <div v-if="planExecution.parentActToolCall.parameters" class="tool-parameters">
          <span class="param-label">{{ $t('common.parameters') }}:</span>
          <pre class="param-content">{{ formatToolParameters(planExecution.parentActToolCall.parameters) }}</pre>
        </div>
      </div>
    </div>

    <!-- Agent execution sequence -->
    <div class="agent-execution-container" v-if="(planExecution?.agentExecutionSequence?.length ?? 0) > 0">
      <h4 class="section-title">{{ $t('chat.agentExecutionSequence') }}</h4>
      
      <div
        v-for="(agentExecution, agentIndex) in planExecution?.agentExecutionSequence"
        :key="agentExecution.id || agentIndex"
        class="agent-execution-item"
        :class="getAgentStatusClass(agentExecution.status)"
      >
        <!-- Agent execution header -->
        <div class="agent-header" @click="handleAgentClick(agentExecution)">
          <div class="agent-info">
            <Icon :icon="getAgentStatusIcon(agentExecution.status)" class="agent-status-icon" />
            <div class="agent-details">
              <div class="agent-name">{{ agentExecution.agentName || $t('chat.unknownAgent') }}</div>
              <pre class="request-content">{{ agentExecution.agentRequest }}</pre>
            </div>
          </div>
          <div class="agent-controls">
            <div class="agent-status-badge" :class="getAgentStatusClass(agentExecution.status)">
              {{ getAgentStatusText(agentExecution.status) }}
            </div>
            <Icon icon="carbon:arrow-right" class="step-select-icon" />
          </div>
        </div>

        <!-- Agent execution info -->
        <div class="agent-execution-info">
          <!-- Agent result -->
          <div v-if="agentExecution.result" class="agent-result">
            <div class="result-header">
              <Icon icon="carbon:checkmark" class="result-icon" />
              <span class="result-label">{{ $t('chat.agentResult') }}:</span>
            </div>
            <pre class="result-content">{{ agentExecution.result }}</pre>
          </div>

          <!-- Error message -->
          <div v-if="agentExecution.errorMessage" class="agent-error">
            <div class="error-header">
              <Icon icon="carbon:warning" class="error-icon" />
              <span class="error-label">{{ $t('chat.errorMessage') }}:</span>
            </div>
            <pre class="error-content">{{ agentExecution.errorMessage }}</pre>
          </div>
        </div>

        <!-- Sub-plan executions -->
        <div v-if="agentExecution.subPlanExecutionRecords?.length" class="sub-plans-container">
          <div class="sub-plans-header">
            <Icon icon="carbon:tree-view" class="sub-plans-icon" />
            <span class="sub-plans-title">
              {{ $t('chat.subPlanExecutions') }} ({{ agentExecution.subPlanExecutionRecords.length }})
            </span>
          </div>
          
          <div class="sub-plans-list">
            <div
              v-for="(subPlan, subPlanIndex) in agentExecution.subPlanExecutionRecords"
              :key="subPlan.currentPlanId || subPlanIndex"
              class="sub-plan-item"
              :class="getSubPlanStatusClass(subPlan)"
              @click="handleSubPlanClick(agentIndex, subPlanIndex, subPlan)"
            >
              <!-- Sub-plan header -->
              <div class="sub-plan-header">
                <div class="sub-plan-info">
                  <Icon :icon="getSubPlanStatusIcon(subPlan)" class="sub-plan-status-icon" />
                  <div class="sub-plan-details">
                    <div class="sub-plan-title">
                      {{ subPlan.title || $t('chat.subPlan') }} #{{ subPlanIndex + 1 }}
                    </div>
                    <div class="sub-plan-id">{{ subPlan.currentPlanId }}</div>
                  </div>
                </div>
                <div class="sub-plan-meta">
                  <div class="sub-plan-status-badge" :class="getSubPlanStatusClass(subPlan)">
                    {{ getSubPlanStatusText(subPlan) }}
                  </div>
                  <div v-if="subPlan.parentActToolCall" class="trigger-tool">
                    <Icon icon="carbon:function" class="trigger-icon" />
                    <span class="trigger-text">{{ subPlan.parentActToolCall.name }}</span>
                  </div>
                </div>
              </div>

              <!-- Sub-plan progress -->
              <div v-if="subPlan.agentExecutionSequence?.length" class="sub-plan-progress">
                <div class="progress-info">
                  <span class="progress-text">
                    {{ $t('chat.progress') }}: {{ getSubPlanCompletedCount(subPlan) }} / {{ subPlan.agentExecutionSequence.length }}
                  </span>
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: getSubPlanProgress(subPlan) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <!-- Sub-plan agent execution preview -->
              <div v-if="subPlan.agentExecutionSequence?.length" class="sub-plan-agents-preview">
                <div class="agents-preview-header">
                  <span class="agents-label">{{ $t('chat.agentExecutions') }}:</span>
                </div>
                <div class="agents-list">
                  <div
                    v-for="(agent, agentIndex) in subPlan.agentExecutionSequence.slice(0, 3)"
                    :key="agent.id || agentIndex"
                    class="agent-preview-item"
                    :class="getAgentPreviewStatusClass(agent.status)"
                  >
                    <Icon :icon="getAgentPreviewStatusIcon(agent.status)" class="agent-icon" />
                    <span class="agent-text">{{ agent.agentName || $t('chat.unknownAgent') }}</span>
                  </div>
                  <div v-if="subPlan.agentExecutionSequence.length > 3" class="more-agents">
                    <span class="more-text">
                      {{ $t('chat.andMoreAgents', { count: subPlan.agentExecutionSequence.length - 3 }) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- User input form for the main plan -->
    <div
      v-if="planExecution?.userInputWaitState?.waiting"
      class="user-input-form-container"
    >
      <div class="user-input-header">
        <Icon icon="carbon:user" class="user-icon" />
        <h4 class="user-input-title">{{ $t('chat.userInputRequired') }}</h4>
      </div>
      
      <p class="user-input-message">
        {{ planExecution?.userInputWaitState?.message ?? $t('chat.userInput.message') }}
      </p>
      
      <p v-if="planExecution?.userInputWaitState?.formDescription" class="form-description">
        {{ planExecution?.userInputWaitState?.formDescription }}
      </p>

      <form @submit.prevent="handleUserInputSubmit" class="user-input-form">
        <template
          v-if="planExecution?.userInputWaitState?.formInputs && planExecution.userInputWaitState.formInputs.length > 0"
        >
          <div class="form-grid">
            <div
              v-for="(input, inputIndex) in planExecution?.userInputWaitState?.formInputs"
              :key="inputIndex"
              class="form-group"
            >
              <label :for="`form-input-${input.label.replace(/\W+/g, '_')}`">
                {{ input.label }}{{ isRequired(input.required) ? ' *' : '' }}:
              </label>

              <!-- Form input types (keeping the original logic) -->
              <input
                v-if="!input.type || input.type === 'text'"
                type="text"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input"
              />

              <input
                v-else-if="input.type === 'email'"
                type="email"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input"
              />

              <input
                v-else-if="input.type === 'number'"
                type="number"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input"
              />

              <input
                v-else-if="input.type === 'password'"
                type="password"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input"
              />

              <textarea
                v-else-if="input.type === 'textarea'"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input form-textarea"
                rows="3"
              ></textarea>

              <select
                v-else-if="input.type === 'select' && input.options"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input form-select"
              >
                <option value="">{{ $t('selectCommon.pleaseSelect') }}</option>
                <option
                  v-for="option in getOptionsArray(input.options)"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
              </select>

              <input
                v-else
                type="text"
                :id="`form-input-${input.label.replace(/\W+/g, '_')}`"
                :name="input.label"
                :placeholder="input.placeholder || ''"
                :required="isRequired(input.required)"
                v-model="formInputsStore[inputIndex]"
                class="form-input"
              />
            </div>
          </div>
        </template>

        <template v-else>
          <div class="form-group">
            <label for="form-input-genericInput">{{ $t('common.input') }}:</label>
            <input
              type="text"
              id="form-input-genericInput"
              name="genericInput"
              v-model="genericInput"
              class="form-input"
            />
          </div>
        </template>

        <button type="submit" class="submit-user-input-btn">
          {{ $t('chat.userInput.submit') }}
        </button>
      </form>
    </div>
  </div>
</template>
  
<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import type { PlanExecutionRecord, AgentExecutionRecord, ExecutionStatus } from '@/types/plan-execution-record'
import type { CompatiblePlanExecutionRecord } from './composables/useChatMessages'

interface Props {
  planExecution: CompatiblePlanExecutionRecord
  genericInput?: string
}

interface Emits {
  (e: 'agent-selected', agentIndex: number, agent: AgentExecutionRecord): void
  (e: 'sub-plan-selected', agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord): void
  (e: 'user-input-submitted', inputData: any): void
  (e: 'step-selected', stepId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Initialize i18n
const { t } = useI18n()

// Local state
const formInputsStore = reactive<Record<number, string>>({})
const genericInput = ref(props.genericInput || '')

// Computed properties
const agentExecutionSequence = computed(() => props.planExecution?.agentExecutionSequence ?? [])

// Agent click handler
const handleAgentClick = (agentExecution: AgentExecutionRecord) => {
  if (agentExecution.stepId) {
    emit('step-selected', agentExecution.stepId)
  } else {
    console.warn('[ExecutionDetails] Agent execution has no stepId:', agentExecution)
  }
}

// Plan status methods
const getPlanStatusClass = (): string => {
  if (!props.planExecution) return 'unknown'
  
  if (props.planExecution.completed) {
    return 'completed'
  }
  
  const hasRunningAgent = agentExecutionSequence.value.some(agent => agent.status === 'RUNNING')
  if (hasRunningAgent) {
    return 'running'
  }
  
  const hasFinishedAgent = agentExecutionSequence.value.some(agent => agent.status === 'FINISHED')
  if (hasFinishedAgent) {
    return 'in-progress'
  }
  
  return 'pending'
}

const getPlanStatusText = (): string => {
  const statusClass = getPlanStatusClass()
  switch (statusClass) {
    case 'completed':
      return t('chat.status.completed')
    case 'running':
      return t('chat.status.executing')
    case 'in-progress':
      return t('chat.status.inProgress')
    case 'pending':
      return t('chat.status.pending')
    default:
      return t('chat.status.unknown')
  }
}

// Agent status methods
const getAgentStatusClass = (status?: ExecutionStatus): string => {
  switch (status) {
    case 'RUNNING':
      return 'running'
    case 'FINISHED':
      return 'completed'
    case 'IDLE':
    default:
      return 'pending'
  }
}

const getAgentStatusText = (status?: ExecutionStatus): string => {
  switch (status) {
    case 'RUNNING':
      return t('chat.status.executing')
    case 'FINISHED':
      return t('chat.status.completed')
    case 'IDLE':
    default:
      return t('chat.status.pending')
  }
}

const getAgentStatusIcon = (status?: ExecutionStatus): string => {
  switch (status) {
    case 'RUNNING':
      return 'carbon:play'
    case 'FINISHED':
      return 'carbon:checkmark'
    case 'IDLE':
    default:
      return 'carbon:dot-mark'
  }
}

// Sub-plan status methods
const getSubPlanStatusClass = (subPlan: PlanExecutionRecord): string => {
  if (subPlan.completed) {
    return 'completed'
  }
  
  const hasRunningAgent = subPlan.agentExecutionSequence?.some(agent => agent.status === 'RUNNING')
  if (hasRunningAgent) {
    return 'running'
  }
  
  const hasFinishedAgent = subPlan.agentExecutionSequence?.some(agent => agent.status === 'FINISHED')
  if (hasFinishedAgent) {
    return 'in-progress'
  }
  
  return 'pending'
}

const getSubPlanStatusText = (subPlan: PlanExecutionRecord): string => {
  const statusClass = getSubPlanStatusClass(subPlan)
  switch (statusClass) {
    case 'completed':
      return t('chat.status.completed')
    case 'running':
      return t('chat.status.executing')
    case 'in-progress':
      return t('chat.status.inProgress')
    case 'pending':
      return t('chat.status.pending')
    default:
      return t('chat.status.unknown')
  }
}

const getSubPlanStatusIcon = (subPlan: PlanExecutionRecord): string => {
  const statusClass = getSubPlanStatusClass(subPlan)
  switch (statusClass) {
    case 'completed':
      return 'carbon:checkmark'
    case 'running':
      return 'carbon:play'
    case 'in-progress':
      return 'carbon:in-progress'
    case 'pending':
    default:
      return 'carbon:dot-mark'
  }
}

const getSubPlanProgress = (subPlan: PlanExecutionRecord): number => {
  if (!subPlan.agentExecutionSequence?.length) return 0
  if (subPlan.completed) return 100
  
  const completedCount = getSubPlanCompletedCount(subPlan)
  return Math.min(100, (completedCount / subPlan.agentExecutionSequence.length) * 100)
}

// Get completed agent count for sub-plan
const getSubPlanCompletedCount = (subPlan: PlanExecutionRecord): number => {
  if (!subPlan.agentExecutionSequence?.length) return 0
  return subPlan.agentExecutionSequence.filter(agent => agent.status === 'FINISHED').length
}

// Agent preview status methods for sub-plan agent preview
const getAgentPreviewStatusClass = (status?: ExecutionStatus): string => {
  switch (status) {
    case 'FINISHED':
      return 'completed'
    case 'RUNNING':
      return 'running'
    case 'IDLE':
    default:
      return 'pending'
  }
}

const getAgentPreviewStatusIcon = (status?: ExecutionStatus): string => {
  switch (status) {
    case 'FINISHED':
      return 'carbon:checkmark'
    case 'RUNNING':
      return 'carbon:play'
    case 'IDLE':
    default:
      return 'carbon:dot-mark'
  }
}

// Event handlers
const handleSubPlanClick = (agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord) => {
  emit('sub-plan-selected', agentIndex, subPlanIndex, subPlan)
}

const handleUserInputSubmit = async () => {
  try {
    const inputData: any = {}

    const formInputs = props.planExecution.userInputWaitState?.formInputs
    if (formInputs && formInputs.length > 0) {
      Object.entries(formInputsStore).forEach(([index, value]) => {
        const numIndex = parseInt(index, 10)
        const input = formInputs[numIndex]
        if (input) {
          const label = input.label || `input_${index}`
          inputData[label] = value
        }
      })
    } else {
      inputData.genericInput = genericInput.value
    }

    emit('user-input-submitted', inputData)
  } catch (error: any) {
    console.error('[ExecutionDetails] User input submission failed:', error)
  }
}

// Helper methods
const formatDateTime = (dateTime?: string): string => {
  if (!dateTime) return ''
  
  try {
    const date = new Date(dateTime)
    return date.toLocaleString()
  } catch (error) {
    return dateTime
  }
}

const formatToolParameters = (parameters?: string): string => {
  if (!parameters) return ''
  
  try {
    const parsed = JSON.parse(parameters)
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    return parameters
  }
}

const getOptionsArray = (options: string | string[] | undefined): string[] => {
  if (!options) return []
  if (Array.isArray(options)) return options
  if (typeof options === 'string') {
    return options.split(',').map(opt => opt.trim()).filter(opt => opt.length > 0)
  }
  return []
}

const isRequired = (required: boolean | string | undefined): boolean => {
  if (typeof required === 'boolean') return required
  if (typeof required === 'string') return required === 'true'
  return false
}


</script>

<style lang="less" scoped>.execution-details {
  // Plan overview
  .plan-overview {
    margin-bottom: 20px;
    
    .plan-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
      
      .plan-title {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #ffffff;
      }
      
      .plan-status-badge {
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
        
        &.completed {
          background: rgba(34, 197, 94, 0.2);
          color: #22c55e;
        }
        
        &.running {
          background: rgba(102, 126, 234, 0.2);
          color: #667eea;
        }
        
        &.in-progress {
          background: rgba(251, 191, 36, 0.2);
          color: #fbbf24;
        }
        
        &.pending {
          background: rgba(156, 163, 175, 0.2);
          color: #9ca3af;
        }
      }
    }
    
    .parent-tool-call {
      background: rgba(102, 126, 234, 0.1);
      border: 1px solid rgba(102, 126, 234, 0.2);
      border-radius: 8px;
      padding: 12px;
      
      .parent-tool-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
        
        .tool-icon {
          font-size: 16px;
          color: #667eea;
        }
        
        .tool-label {
          color: #aaaaaa;
          font-size: 13px;
        }
        
        .tool-name {
          color: #ffffff;
          font-weight: 600;
          font-size: 14px;
        }
      }
      
      .tool-parameters {
        .param-label {
          color: #aaaaaa;
          font-size: 12px;
          margin-bottom: 4px;
          display: block;
        }
        
        .param-content {
          margin: 0;
          padding: 8px;
          background: rgba(0, 0, 0, 0.2);
          border-radius: 4px;
          font-family: monospace;
          font-size: 11px;
          color: #cccccc;
          white-space: pre-wrap;
          max-height: 120px;
          overflow-y: auto;
        }
      }
    }
  }

  // Agent execution container
  .agent-execution-container {
    .section-title {
      margin: 0 0 16px 0;
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
      padding-bottom: 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    
    .agent-execution-item {
      margin-bottom: 16px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      overflow: hidden;
      transition: all 0.2s ease;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      &.running {
        border-color: rgba(102, 126, 234, 0.4);
        box-shadow: 0 0 8px rgba(102, 126, 234, 0.2);
      }
      
      &.completed {
        border-color: rgba(34, 197, 94, 0.3);
      }
      
      &.pending {
        opacity: 0.8;
      }
      
      .agent-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        background: rgba(255, 255, 255, 0.02);
        cursor: pointer;
        transition: background 0.2s ease;
        
        &:hover {
          background: rgba(255, 255, 255, 0.05);
        }
        
        .agent-info {
          display: flex;
          align-items: center;
          gap: 12px;
          flex: 1;
          
          .agent-status-icon {
            font-size: 18px;
            
            &.running {
              color: #667eea;
            }
            
            &.completed {
              color: #22c55e;
            }
            
            &.pending {
              color: #9ca3af;
            }
          }
          
          .agent-details {
            .agent-name {
              font-weight: 600;
              color: #ffffff;
              font-size: 14px;
              margin-bottom: 2px;
            }
            
            .agent-description {
              color: #aaaaaa;
              font-size: 12px;
              line-height: 1.3;
            }
            
            .request-content {
              margin: 4px 0 0 0;
              padding: 8px;
              background: rgba(0, 0, 0, 0.2);
              border-radius: 4px;
              font-family: monospace;
              font-size: 14px;
              color: #cccccc;
              white-space: pre-wrap;
              word-wrap: break-word;
              word-break: break-word;
              overflow-wrap: break-word;
              max-height: 120px;
              overflow-y: auto;
              line-height: 1.4;
            }
          }
        }
        
        .agent-controls {
          display: flex;
          align-items: center;
          gap: 12px;
          
          .agent-status-badge {
            padding: 3px 8px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 500;
            
            &.running {
              background: rgba(102, 126, 234, 0.2);
              color: #667eea;
            }
            
            &.completed {
              background: rgba(34, 197, 94, 0.2);
              color: #22c55e;
            }
            
            &.pending {
              background: rgba(156, 163, 175, 0.2);
              color: #9ca3af;
            }
          }
          
          .step-select-icon {
            font-size: 16px;
            color: #667eea;
            transition: transform 0.2s ease;
          }
        }
      }
      
      .agent-execution-info {
        padding: 16px;
        background: rgba(0, 0, 0, 0.1);
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        margin-bottom: 16px;
        
        .agent-result, .agent-error {
          margin-bottom: 12px;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          .result-header, .error-header {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 6px;
            
            .result-icon, .error-icon {
              font-size: 14px;
            }
            
            .result-icon {
              color: #22c55e;
            }
            
            .error-icon {
              color: #ef4444;
            }
            
            .result-label, .error-label {
              color: #ffffff;
              font-size: 13px;
              font-weight: 500;
            }
          }
          
          .result-content, .error-content {
            margin: 0;
            padding: 8px;
            background: rgba(0, 0, 0, 0.2);
            border-radius: 4px;
            font-family: monospace;
            font-size: 12px;
            white-space: pre-wrap;
            max-height: 150px;
            overflow-y: auto;
            color: #cccccc;
          }
          
          .error-content {
            color: #ff9999;
            border: 1px solid rgba(239, 68, 68, 0.2);
          }
        }
      }
      
      .sub-plans-container {
        padding: 16px;
        background: rgba(0, 0, 0, 0.1);
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        
        .sub-plans-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 12px;
          
          .sub-plans-icon {
            font-size: 16px;
            color: #667eea;
          }
          
          .sub-plans-title {
            color: #ffffff;
            font-weight: 600;
            font-size: 14px;
          }
        }
        
        .sub-plans-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
          
          .sub-plan-item {
            background: rgba(102, 126, 234, 0.05);
            border: 1px solid rgba(102, 126, 234, 0.1);
            border-radius: 6px;
            padding: 12px;
            cursor: pointer;
            transition: all 0.2s ease;
            
            &:hover {
              background: rgba(102, 126, 234, 0.1);
              border-color: rgba(102, 126, 234, 0.2);
            }
            
            &.running {
              border-color: rgba(102, 126, 234, 0.3);
              background: rgba(102, 126, 234, 0.08);
              box-shadow: 0 0 8px rgba(102, 126, 234, 0.15);
            }
            
            &.completed {
              border-color: rgba(34, 197, 94, 0.3);
              background: rgba(34, 197, 94, 0.05);
            }
            
            &.pending {
              opacity: 0.7;
            }
            
            .sub-plan-header {
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 8px;
              
              .sub-plan-info {
                display: flex;
                align-items: center;
                gap: 8px;
                flex: 1;
                
                .sub-plan-status-icon {
                  font-size: 16px;
                  
                  &.completed {
                    color: #22c55e;
                  }
                  
                  &.running {
                    color: #667eea;
                  }
                  
                  &.in-progress {
                    color: #fbbf24;
                  }
                  
                  &.pending {
                    color: #9ca3af;
                  }
                }
                
                .sub-plan-details {
                  .sub-plan-title {
                    font-weight: 600;
                    color: #ffffff;
                    font-size: 13px;
                    margin-bottom: 2px;
                  }
                  
                  .sub-plan-id {
                    color: #aaaaaa;
                    font-size: 11px;
                    font-family: monospace;
                  }
                }
              }
              
              .sub-plan-status-badge {
                padding: 2px 6px;
                border-radius: 8px;
                font-size: 10px;
                font-weight: 500;
                
                &.completed {
                  background: rgba(34, 197, 94, 0.2);
                  color: #22c55e;
                }
                
                &.running {
                  background: rgba(102, 126, 234, 0.2);
                  color: #667eea;
                }
                
                &.in-progress {
                  background: rgba(251, 191, 36, 0.2);
                  color: #fbbf24;
                }
                
                &.pending {
                  background: rgba(156, 163, 175, 0.2);
                  color: #9ca3af;
                }
              }
            }
            
            .sub-plan-progress {
              margin-bottom: 8px;
              
              .progress-bar-container {
                background: rgba(0, 0, 0, 0.2);
                border-radius: 4px;
                height: 4px;
                overflow: hidden;
                
                .progress-bar {
                  height: 100%;
                  background: linear-gradient(90deg, #667eea, #764ba2);
                  transition: width 0.3s ease;
                  border-radius: 4px;
                }
              }
              
              .progress-text {
                color: #aaaaaa;
                font-size: 10px;
                margin-top: 4px;
              }
            }
            
            .sub-plan-agents-preview {
              .agents-preview-header {
                color: #aaaaaa;
                font-size: 11px;
                margin-bottom: 6px;
                font-weight: 500;
              }
              
              .agents-list {
                display: flex;
                flex-wrap: wrap;
                gap: 4px;
                
                .agent-preview-item {
                  display: flex;
                  align-items: center;
                  gap: 4px;
                  padding: 2px 6px;
                  background: rgba(0, 0, 0, 0.2);
                  border-radius: 4px;
                  font-size: 10px;
                  
                  .agent-icon {
                    font-size: 10px;
                    
                    &.completed {
                      color: #22c55e;
                    }
                    
                    &.running {
                      color: #667eea;
                    }
                    
                    &.pending {
                      color: #9ca3af;
                    }
                  }
                  
                  .agent-text {
                    color: #cccccc;
                    max-width: 80px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // User input form
  .user-input-form-container {
    margin-top: 20px;
    padding: 16px;
    background: rgba(102, 126, 234, 0.1);
    border: 1px solid rgba(102, 126, 234, 0.2);
    border-radius: 8px;
    
    .user-input-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      
      .user-icon {
        font-size: 16px;
        color: #667eea;
      }
      
      .user-input-title {
        margin: 0;
        color: #ffffff;
        font-size: 14px;
        font-weight: 600;
      }
    }
    
    .user-input-message {
      margin-bottom: 12px;
      font-weight: 500;
      color: #ffffff;
      font-size: 14px;
    }
    
    .form-description {
      margin-bottom: 16px;
      color: #aaaaaa;
      font-size: 13px;
      line-height: 1.4;
    }
    
    .user-input-form {
      .form-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
        margin-bottom: 16px;
        
        @media (max-width: 768px) {
          grid-template-columns: 1fr;
          gap: 12px;
        }
      }
      
      .form-group {
        display: flex;
        flex-direction: column;
        gap: 4px;
        
        label {
          font-size: 13px;
          font-weight: 500;
          color: #ffffff;
        }
        
        .form-input {
          padding: 8px 12px;
          background: rgba(0, 0, 0, 0.3);
          border: 1px solid rgba(255, 255, 255, 0.2);
          border-radius: 6px;
          color: #ffffff;
          font-size: 14px;
          transition: border-color 0.2s ease;
          
          &:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
          }
          
          &::placeholder {
            color: #888888;
          }
        }
        
        .form-textarea {
          resize: vertical;
          min-height: 60px;
          font-family: inherit;
        }
        
        .form-select {
          cursor: pointer;
          
          option {
            background: #2d3748;
            color: #ffffff;
          }
        }
      }
      
      .submit-user-input-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #ffffff;
        border: none;
        padding: 10px 20px;
        border-radius: 6px;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
        }
        
        &:active {
          transform: translateY(0);
        }
      }
    }
  }
}
</style>



