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

        <!-- Sub-plan executions with nested support -->
        <div v-if="agentExecution.subPlanExecutionRecords?.length" class="sub-plans-container">
          <div class="sub-plans-header">
            <Icon icon="carbon:tree-view" class="sub-plans-icon" />
            <span class="sub-plans-title">
              {{ $t('chat.subPlanExecutions') }} ({{ agentExecution.subPlanExecutionRecords.length }})
            </span>
          </div>
          
          <div class="sub-plans-list">
            <RecursiveSubPlan
              v-for="(subPlan, subPlanIndex) in agentExecution.subPlanExecutionRecords"
              :key="subPlan.currentPlanId || subPlanIndex"
              :sub-plan="subPlan"
              :sub-plan-index="subPlanIndex"
              :nesting-level="0"
              :max-nesting-depth="3"
              :max-visible-steps="2"
              @sub-plan-selected="handleSubPlanClick"
              @step-selected="handleStepSelected"
            />
          </div>
        </div>
      </div>
    </div>

  </div>
</template>
  
<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import type { PlanExecutionRecord, AgentExecutionRecord, ExecutionStatus } from '@/types/plan-execution-record'
import type { CompatiblePlanExecutionRecord } from './composables/useChatMessages'
import RecursiveSubPlan from './RecursiveSubPlan.vue'

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

// Note: Sub-plan status methods are now handled by RecursiveSubPlan component

// Note: Agent preview status methods are now handled by RecursiveSubPlan component

// Event handlers
const handleSubPlanClick = (agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord) => {
  emit('sub-plan-selected', agentIndex, subPlanIndex, subPlan)
}

const handleStepSelected = (stepId: string) => {
  emit('step-selected', stepId)
}

// Note: Sub-plan agent and think-act step handling is now done by RecursiveSubPlan component


// Helper methods

const formatToolParameters = (parameters?: string): string => {
  if (!parameters) return ''
  
  try {
    const parsed = JSON.parse(parameters)
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    return parameters
  }
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
              
              .sub-plan-meta {
                display: flex;
                align-items: center;
                gap: 8px;
                
                .trigger-tool {
                  display: flex;
                  align-items: center;
                  gap: 4px;
                  padding: 2px 6px;
                  background: rgba(102, 126, 234, 0.1);
                  border-radius: 4px;
                  font-size: 10px;
                  
                  .trigger-icon {
                    font-size: 10px;
                    color: #667eea;
                  }
                  
                  .trigger-text {
                    color: #cccccc;
                    font-weight: 500;
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
              
              .progress-info {
                .progress-text {
                  color: #aaaaaa;
                  font-size: 10px;
                  margin-bottom: 4px;
                }
                
                .progress-bar {
                  background: rgba(0, 0, 0, 0.2);
                  border-radius: 4px;
                  height: 4px;
                  overflow: hidden;
                  
                  .progress-fill {
                    height: 100%;
                    background: linear-gradient(90deg, #667eea, #764ba2);
                    transition: width 0.3s ease;
                    border-radius: 4px;
                  }
                }
              }
            }
            
            .sub-plan-agents-steps {
              .agents-steps-header {
                color: #aaaaaa;
                font-size: 11px;
                margin-bottom: 6px;
                font-weight: 500;
              }
              
              .agents-steps-list {
                display: flex;
                flex-direction: column;
                gap: 8px;
                
                .agent-step-item {
                  border: 1px solid rgba(255, 255, 255, 0.1);
                  border-radius: 6px;
                  padding: 8px;
                  background: rgba(0, 0, 0, 0.05);
                  cursor: pointer;
                  transition: all 0.2s;
                  
                  &:hover {
                    background: rgba(0, 0, 0, 0.1);
                    border-color: rgba(255, 255, 255, 0.2);
                  }
                  
                  &.completed {
                    border-color: rgba(34, 197, 94, 0.3);
                    background: rgba(34, 197, 94, 0.05);
                  }
                  
                  &.running {
                    border-color: rgba(102, 126, 234, 0.3);
                    background: rgba(102, 126, 234, 0.08);
                  }
                  
                  &.pending {
                    opacity: 0.7;
                  }
                  
                  .agent-step-header {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    margin-bottom: 8px;
                    
                    .agent-icon {
                      font-size: 14px;
                      
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
                    
                    .agent-name {
                      color: #ffffff;
                      font-size: 13px;
                      font-weight: 500;
                      flex: 1;
                    }
                    
                    .agent-status-badge {
                      padding: 2px 6px;
                      border-radius: 3px;
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
                      
                      &.pending {
                        background: rgba(156, 163, 175, 0.2);
                        color: #9ca3af;
                      }
                    }
                  }
                  
                  .sub-agent-execution-info {
                    margin-left: 22px;
                    
                    .agent-result, .agent-error {
                      margin-bottom: 8px;
                      
                      &:last-child {
                        margin-bottom: 0;
                      }
                      
                      .result-header, .error-header {
                        display: flex;
                        align-items: center;
                        gap: 4px;
                        margin-bottom: 4px;
                        
                        .result-icon, .error-icon {
                          font-size: 12px;
                        }
                        
                        .result-icon {
                          color: #22c55e;
                        }
                        
                        .error-icon {
                          color: #ef4444;
                        }
                        
                        .result-label, .error-label {
                          color: #ffffff;
                          font-size: 11px;
                          font-weight: 500;
                        }
                      }
                      
                      .result-content, .error-content {
                        margin: 0;
                        padding: 6px;
                        background: rgba(0, 0, 0, 0.2);
                        border-radius: 3px;
                        font-family: monospace;
                        font-size: 10px;
                        white-space: pre-wrap;
                        max-height: 80px;
                        overflow-y: auto;
                        color: #cccccc;
                        line-height: 1.3;
                      }
                    }
                    
                    .think-act-preview {
                      margin-top: 8px;
                      
                      .think-act-header {
                        display: flex;
                        align-items: center;
                        gap: 4px;
                        margin-bottom: 6px;
                        
                        .think-act-icon {
                          font-size: 12px;
                          color: #667eea;
                        }
                        
                        .think-act-label {
                          color: #aaaaaa;
                          font-size: 11px;
                          font-weight: 500;
                        }
                      }
                      
                      .think-act-steps-preview {
                        display: flex;
                        flex-direction: column;
                        gap: 3px;
                        
                        .think-act-step-preview {
                          display: flex;
                          align-items: center;
                          gap: 6px;
                          padding: 4px 6px;
                          background: rgba(0, 0, 0, 0.1);
                          border-radius: 3px;
                          cursor: pointer;
                          transition: all 0.2s;
                          font-size: 10px;
                          
                          &:hover {
                            background: rgba(0, 0, 0, 0.2);
                          }
                          
                          .step-number {
                            color: #667eea;
                            font-weight: 500;
                            min-width: 20px;
                          }
                          
                          .step-description {
                            color: #cccccc;
                            flex: 1;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                          }
                          
                          .step-arrow {
                            font-size: 10px;
                            color: #888888;
                          }
                        }
                        
                        .more-steps {
                          padding: 2px 6px;
                          color: #888888;
                          font-size: 9px;
                          font-style: italic;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}
</style>



