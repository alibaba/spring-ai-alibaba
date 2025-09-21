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
  <div class="recursive-sub-plan" :class="getNestingClass()">
    <!-- Sub-plan header -->
    <div class="sub-plan-header" @click="handleSubPlanClick">
      <div class="sub-plan-info">
        <Icon :icon="getSubPlanStatusIcon()" class="sub-plan-status-icon" />
        <div class="sub-plan-details">
          <div class="sub-plan-title">
            {{ subPlan.title || $t('chat.subPlan') }} #{{ subPlanIndex + 1 }}
            <span v-if="(nestingLevel ?? 0) > 0" class="nesting-level">(L{{ (nestingLevel ?? 0) + 1 }})</span>
          </div>
          <div class="sub-plan-id">{{ subPlan.currentPlanId }}</div>
        </div>
      </div>
      <div class="sub-plan-meta">
        <div class="sub-plan-status-badge" :class="getSubPlanStatusClass()">
          {{ getSubPlanStatusText() }}
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
          {{ $t('chat.progress') }}: {{ getSubPlanCompletedCount() }} / {{ subPlan.agentExecutionSequence.length }}
        </span>
        <div class="progress-bar">
          <div 
            class="progress-fill" 
            :style="{ width: getSubPlanProgress() + '%' }"
          ></div>
        </div>
      </div>
    </div>

    <!-- Sub-plan agent execution steps -->
    <div v-if="subPlan.agentExecutionSequence?.length" class="sub-plan-agents-steps">
      <div class="agents-steps-header">
        <span class="agents-label">{{ $t('chat.agentExecutions') }}:</span>
      </div>
      <div class="agents-steps-list">
        <div
          v-for="(agent, agentIndex) in subPlan.agentExecutionSequence"
          :key="agent.id || agentIndex"
          class="agent-step-item"
          :class="getAgentPreviewStatusClass(agent.status)"
          @click="handleSubPlanAgentClick(agentIndex, agent)"
        >
          <div class="agent-step-header">
            <Icon :icon="getAgentPreviewStatusIcon(agent.status)" class="agent-icon" />
            <span class="agent-name">{{ agent.agentName || $t('chat.unknownAgent') }}</span>
            <div class="agent-status-badge" :class="getAgentPreviewStatusClass(agent.status)">
              {{ getAgentStatusText(agent.status) }}
            </div>
          </div>
          
          <!-- Agent execution info for sub-plan agents -->
          <div class="sub-agent-execution-info">
            <!-- Agent result -->
            <div v-if="agent.result" class="agent-result">
              <div class="result-header">
                <Icon icon="carbon:checkmark" class="result-icon" />
                <span class="result-label">{{ $t('chat.agentResult') }}:</span>
              </div>
              <pre class="result-content">{{ agent.result }}</pre>
            </div>

            <!-- Error message -->
            <div v-if="agent.errorMessage" class="agent-error">
              <div class="error-header">
                <Icon icon="carbon:warning" class="error-icon" />
                <span class="error-label">{{ $t('chat.errorMessage') }}:</span>
              </div>
              <pre class="error-content">{{ agent.errorMessage }}</pre>
            </div>

            <!-- Think-act steps with nested sub-plans -->
            <div v-if="agent.thinkActSteps?.length" class="think-act-preview">
              <div class="think-act-header">
                <Icon icon="carbon:thinking" class="think-act-icon" />
                <span class="think-act-label">{{ $t('chat.thinkActSteps') }} ({{ agent.thinkActSteps.length }})</span>
              </div>
              <div class="think-act-steps-preview">
                <div
                  v-for="(step, stepIndex) in agent.thinkActSteps.slice(0, maxVisibleSteps ?? 2)"
                  :key="step.id || stepIndex"
                  class="think-act-step-preview"
                  @click.stop="handleThinkActStepClick(agentIndex, stepIndex, agent)"
                >
                  <span class="step-number">#{{ stepIndex + 1 }}</span>
                  <span class="step-description">{{ step.actionDescription || $t('chat.thinking') }}</span>
                  <Icon icon="carbon:arrow-right" class="step-arrow" />
                </div>
                <div v-if="agent.thinkActSteps.length > (maxVisibleSteps ?? 2)" class="more-steps">
                  <span class="more-steps-text">
                    {{ $t('chat.andMoreSteps', { count: agent.thinkActSteps.length - (maxVisibleSteps ?? 2) }) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Nested sub-plans from think-act steps -->
            <div v-if="hasNestedSubPlans(agent)" class="nested-sub-plans">
              <div class="nested-sub-plans-header">
                <Icon icon="carbon:tree-view-alt" class="nested-icon" />
                <span class="nested-label">{{ $t('chat.nestedSubPlans') }}</span>
              </div>
              <div class="nested-sub-plans-list">
                <RecursiveSubPlan
                  v-for="(nestedStep, nestedStepIndex) in getNestedSubPlans(agent)"
                  :key="nestedStep.id || nestedStepIndex"
                  :sub-plan="nestedStep.subPlanExecutionRecord!"
                  :sub-plan-index="nestedStepIndex"
                  :nesting-level="(nestingLevel ?? 0) + 1"
                  :max-nesting-depth="maxNestingDepth ?? 3"
                  :max-visible-steps="maxVisibleSteps ?? 2"
                  @sub-plan-selected="handleNestedSubPlanSelected"
                  @step-selected="handleNestedStepSelected"
                />
              </div>
            </div>

            <!-- Direct sub-plans from agent -->
            <div v-if="agent.subPlanExecutionRecords?.length" class="direct-sub-plans">
              <div class="direct-sub-plans-header">
                <Icon icon="carbon:tree-view" class="direct-icon" />
                <span class="direct-label">{{ $t('chat.directSubPlans') }}</span>
              </div>
              <div class="direct-sub-plans-list">
                <RecursiveSubPlan
                  v-for="(directSubPlan, directIndex) in agent.subPlanExecutionRecords"
                  :key="directSubPlan.currentPlanId || directIndex"
                  :sub-plan="directSubPlan"
                  :sub-plan-index="directIndex"
                  :nesting-level="(nestingLevel ?? 0) + 1"
                  :max-nesting-depth="maxNestingDepth ?? 3"
                  :max-visible-steps="maxVisibleSteps ?? 2"
                  @sub-plan-selected="handleNestedSubPlanSelected"
                  @step-selected="handleNestedStepSelected"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import type { PlanExecutionRecord, AgentExecutionRecord, ExecutionStatus, ThinkActRecord } from '@/types/plan-execution-record'

interface Props {
  subPlan: PlanExecutionRecord
  subPlanIndex: number
  nestingLevel?: number
  maxNestingDepth?: number
  maxVisibleSteps?: number
}

interface Emits {
  (e: 'sub-plan-selected', agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord): void
  (e: 'step-selected', stepId: string): void
}

const props = withDefaults(defineProps<Props>(), {
  nestingLevel: 0,
  maxNestingDepth: 3,
  maxVisibleSteps: 2
})

const emit = defineEmits<Emits>()

// Initialize i18n
const { t } = useI18n()

// Helper functions
const getNestingClass = (): string => {
  return `nesting-level-${props.nestingLevel}`
}

// Sub-plan status methods
const getSubPlanStatusClass = (): string => {
  if (props.subPlan.completed) {
    return 'completed'
  }
  
  const hasRunningAgent = props.subPlan.agentExecutionSequence?.some(agent => agent.status === 'RUNNING')
  if (hasRunningAgent) {
    return 'running'
  }
  
  const hasFinishedAgent = props.subPlan.agentExecutionSequence?.some(agent => agent.status === 'FINISHED')
  if (hasFinishedAgent) {
    return 'in-progress'
  }
  
  return 'pending'
}

const getSubPlanStatusText = (): string => {
  const statusClass = getSubPlanStatusClass()
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

const getSubPlanStatusIcon = (): string => {
  const statusClass = getSubPlanStatusClass()
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

const getSubPlanProgress = (): number => {
  if (!props.subPlan.agentExecutionSequence?.length) return 0
  if (props.subPlan.completed) return 100
  
  const completedCount = getSubPlanCompletedCount()
  return Math.min(100, (completedCount / props.subPlan.agentExecutionSequence.length) * 100)
}

const getSubPlanCompletedCount = (): number => {
  if (!props.subPlan.agentExecutionSequence?.length) return 0
  return props.subPlan.agentExecutionSequence.filter(agent => agent.status === 'FINISHED').length
}

// Agent preview status methods
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

// Nested sub-plans detection
const hasNestedSubPlans = (agent: AgentExecutionRecord): boolean => {
  return agent.thinkActSteps?.some(step => step.subPlanExecutionRecord) ?? false
}

const getNestedSubPlans = (agent: AgentExecutionRecord): ThinkActRecord[] => {
  return agent.thinkActSteps?.filter(step => step.subPlanExecutionRecord) ?? []
}

// Event handlers
const handleSubPlanClick = () => {
  emit('sub-plan-selected', -1, props.subPlanIndex, props.subPlan)
}

const handleSubPlanAgentClick = (agentIndex: number, agent: AgentExecutionRecord) => {
  const stepId = agent.stepId || `subplan-${props.subPlanIndex}-agent-${agentIndex}`
  emit('step-selected', stepId)
}

const handleThinkActStepClick = (agentIndex: number, _stepIndex: number, agent: AgentExecutionRecord) => {
  const stepId = agent.stepId || `subplan-${props.subPlanIndex}-agent-${agentIndex}`
  emit('step-selected', stepId)
}

const handleNestedSubPlanSelected = (agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord) => {
  emit('sub-plan-selected', agentIndex, subPlanIndex, subPlan)
}

const handleNestedStepSelected = (stepId: string) => {
  emit('step-selected', stepId)
}
</script>

<style lang="less" scoped>
.recursive-sub-plan {
  background: rgba(102, 126, 234, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.1);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
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

  // Nesting level styles
  &.nesting-level-1 {
    margin-left: 16px;
    border-left: 3px solid rgba(102, 126, 234, 0.3);
  }

  &.nesting-level-2 {
    margin-left: 32px;
    border-left: 3px solid rgba(251, 191, 36, 0.3);
  }

  &.nesting-level-3 {
    margin-left: 48px;
    border-left: 3px solid rgba(34, 197, 94, 0.3);
  }

  .sub-plan-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    cursor: pointer;

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
          display: flex;
          align-items: center;
          gap: 6px;

          .nesting-level {
            color: #888888;
            font-size: 10px;
            font-weight: 400;
            background: rgba(136, 136, 136, 0.2);
            padding: 1px 4px;
            border-radius: 3px;
          }
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

          .nested-sub-plans, .direct-sub-plans {
            margin-top: 12px;

            .nested-sub-plans-header, .direct-sub-plans-header {
              display: flex;
              align-items: center;
              gap: 6px;
              margin-bottom: 8px;

              .nested-icon, .direct-icon {
                font-size: 12px;
                color: #fbbf24;
              }

              .nested-label, .direct-label {
                color: #aaaaaa;
                font-size: 11px;
                font-weight: 500;
              }
            }

            .nested-sub-plans-list, .direct-sub-plans-list {
              display: flex;
              flex-direction: column;
              gap: 6px;
            }
          }
        }
      }
    }
  }
}
</style>
