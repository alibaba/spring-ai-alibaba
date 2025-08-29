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
      <!-- Step execution details -->
      <div class="steps-container" v-if="(planExecution?.steps?.length ?? 0) > 0">
        <h4 class="steps-title">{{ $t('chat.stepExecutionDetails') }}</h4>
  
        <!-- Iterate through all steps -->
        <div
            v-for="(step, index) in planExecution?.steps"
            :key="index"
            class="ai-section"
            :class="{
            running: getAgentExecutionStatus(index) === 'RUNNING',
            completed: getAgentExecutionStatus(index) === 'FINISHED',
            pending: getAgentExecutionStatus(index) === 'IDLE',
          }"
            @click.stop="handleStepClick(index)"
        >
          <div class="section-header">
            <span class="step-icon">
              {{
                getAgentExecutionStatus(index) === 'FINISHED'
                    ? '✓'
                    : getAgentExecutionStatus(index) === 'RUNNING'
                        ? '▶'
                        : '○'
              }}
            </span>
            <span class="step-title">
              {{ step || `${$t('chat.step')} ${index + 1}` }}
            </span>
            <span
                v-if="getAgentExecutionStatus(index) === 'RUNNING'"
                class="step-status current"
            >
              {{ $t('chat.status.executing') }}
            </span>
            <span
                v-else-if="getAgentExecutionStatus(index) === 'FINISHED'"
                class="step-status completed"
            >
              {{ $t('chat.status.completed') }}
            </span>
            <span v-else class="step-status pending">
              {{ $t('chat.status.pending') }}
            </span>
          </div>
  
          <!-- Display step execution action information -->
          <div
              v-if="stepActions && stepActions[index]"
              class="action-info"
          >
            <div class="action-description">
              <span class="action-icon">
                {{
                  stepActions[index]?.status === 'current'
                      ? '🔄'
                      : stepActions[index]?.status === 'completed'
                          ? '✓'
                          : '⏳'
                }}
              </span>
              <strong>{{ stepActions[index]?.actionDescription }}</strong>
            </div>
  
            <div v-if="stepActions[index]?.toolParameters" class="tool-params">
              <span class="tool-icon">⚙️</span>
              <span class="param-label">{{ $t('common.parameters') }}:</span>
              <pre class="param-content">{{
                  stepActions[index]?.toolParameters
                }}</pre>
            </div>
  
            <div v-if="stepActions[index]?.thinkOutput" class="think-details">
              <div class="think-header">
                <span class="think-icon">💭</span>
                <span class="think-label">{{ $t('chat.thinkingOutput') }}:</span>
              </div>
              <div class="think-output">
                <pre class="think-content">{{
                    stepActions[index]?.thinkOutput
                  }}</pre>
              </div>
            </div>
          </div>
  
          <!-- Sub-plan steps -->
          <div v-if="getSubPlanSteps(index)?.length > 0" class="sub-plan-steps">
            <div class="sub-plan-header">
              <Icon icon="carbon:tree-view" class="sub-plan-icon" />
              <span class="sub-plan-title">{{ $t('rightPanel.subPlan') }}</span>
            </div>
            <div class="sub-plan-step-list">
              <div
                  v-for="(subStep, subStepIndex) in getSubPlanSteps(index)"
                  :key="`sub-${index}-${subStepIndex}`"
                  class="sub-plan-step-item"
                  :class="{
                  completed:
                    getSubPlanStepStatus(index, subStepIndex) === 'completed',
                  current:
                    getSubPlanStepStatus(index, subStepIndex) === 'current',
                  pending:
                    getSubPlanStepStatus(index, subStepIndex) === 'pending',
                }"
                  @click.stop="handleSubPlanStepClick(index, subStepIndex)"
              >
                <div class="sub-step-indicator">
                  <span class="sub-step-icon">
                    {{
                      getSubPlanStepStatus(index, subStepIndex) === 'completed'
                          ? '✓'
                          : getSubPlanStepStatus(index, subStepIndex) === 'current'
                              ? '▶'
                              : '○'
                    }}
                  </span>
                  <span class="sub-step-number">{{ subStepIndex + 1 }}</span>
                </div>
                <div class="sub-step-content">
                  <span class="sub-step-title">{{ subStep }}</span>
                  <span class="sub-step-badge">{{ $t('rightPanel.subStep') }}</span>
                </div>
              </div>
            </div>
          </div>
  
          <!-- User input form -->
          <div
              v-if="
              planExecution?.userInputWaitState &&
              getAgentExecutionStatus(index) === 'RUNNING'
            "
              class="user-input-form-container"
          >
            <p class="user-input-message">
              {{
                planExecution?.userInputWaitState?.message ??
                $t('chat.userInput.message')
              }}
            </p>
            <p
                v-if="planExecution?.userInputWaitState?.formDescription"
                class="form-description"
            >
              {{ planExecution?.userInputWaitState?.formDescription }}
            </p>
  
            <form
                @submit.prevent="handleUserInputSubmit"
                class="user-input-form"
            >
              <template
                  v-if="
                  planExecution?.userInputWaitState?.formInputs &&
                  planExecution.userInputWaitState.formInputs.length > 0
                "
              >
                <div class="form-grid">
                  <div
                      v-for="(input, inputIndex) in planExecution?.userInputWaitState
                      ?.formInputs"
                      :key="inputIndex"
                      class="form-group"
                  >
                    <label :for="`form-input-${input.label.replace(/\W+/g, '_')}`">
                      {{ input.label }}{{ isRequired(input.required) ? ' *' : '' }}:
                    </label>
  
                    <!-- Text Input -->
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
  
                    <!-- Email Input -->
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
  
                    <!-- Number Input -->
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
  
                    <!-- Password Input -->
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
  
                    <!-- Textarea -->
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
  
                    <!-- Select -->
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
  
                    <!-- Fallback to text input -->
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
      </div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, reactive } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { Icon } from '@iconify/vue'
  import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'
  
interface Props {
  planExecution: PlanExecutionRecord
  stepActions?: Array<{
    actionDescription: string
    toolParameters: string
    thinkInput: string
    thinkOutput: string
    status: 'completed' | 'current' | 'pending'
  } | null>
  genericInput?: string
}
  
  interface Emits {
    (e: 'step-selected', stepIndex: number): void
    (e: 'sub-plan-step-selected', stepIndex: number, subStepIndex: number): void
    (e: 'user-input-submitted', inputData: any): void
  }
  
  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()
  
  // Initialize i18n
  const { t } = useI18n()
  
  // Local form inputs store
  const formInputsStore = reactive<Record<number, string>>({})
  const genericInput = ref(props.genericInput || '')
  
  // Get agent execution status based on index
  const getAgentExecutionStatus = (index: number): string => {
    const agentExecutionSequence = props.planExecution.agentExecutionSequence ?? []
    if (index < 0 || index >= agentExecutionSequence.length) {
      return 'IDLE'
    }
    const agentExecution = agentExecutionSequence[index]
    return agentExecution.status ?? 'IDLE'
  }
  
  // Handle step click events
  const handleStepClick = (stepIndex: number) => {
    emit('step-selected', stepIndex)
  }
  
  // Get sub-plan steps from agentExecutionSequence
  const getSubPlanSteps = (stepIndex: number): string[] => {
    try {
      const agentExecutionSequence = props.planExecution.agentExecutionSequence
      if (!agentExecutionSequence?.length) {
        return []
      }
  
      const agentExecution = agentExecutionSequence[stepIndex] as AgentExecutionRecord | undefined
      if (!agentExecution?.thinkActSteps) {
        return []
      }
  
      for (const thinkActStep of agentExecution.thinkActSteps) {
        if (thinkActStep.subPlanExecutionRecord) {
          const rawSteps = thinkActStep.subPlanExecutionRecord.steps ?? []
          return rawSteps.map((step: any) => {
            if (typeof step === 'string') {
              return step
            } else if (typeof step === 'object' && step !== null) {
              return step.title || step.description || t('rightPanel.subStep')
            }
            return t('rightPanel.subStep')
          })
        }
      }
  
      return []
    } catch (error) {
      console.warn('[ExecutionDetails] Error getting sub-plan steps:', error)
      return []
    }
  }
  
  // Get sub-plan step status
  const getSubPlanStepStatus = (stepIndex: number, subStepIndex: number): string => {
    try {
      const agentExecutionSequence = props.planExecution.agentExecutionSequence
      if (!agentExecutionSequence?.length) {
        return 'pending'
      }
  
      const agentExecution = agentExecutionSequence[stepIndex] as AgentExecutionRecord | undefined
      if (!agentExecution?.thinkActSteps) {
        return 'pending'
      }
  
      let subPlan = null
      for (const thinkActStep of agentExecution.thinkActSteps) {
        if (thinkActStep.subPlanExecutionRecord) {
          subPlan = thinkActStep.subPlanExecutionRecord
          break
        }
      }
  
      if (!subPlan) {
        return 'pending'
      }
  
      const currentStepIndex = subPlan.currentStepIndex
      if (subPlan.completed) {
        return 'completed'
      }
  
      if (currentStepIndex == null) {
        return subStepIndex === 0 ? 'current' : 'pending'
      }
  
      if (subStepIndex < currentStepIndex) {
        return 'completed'
      } else if (subStepIndex === currentStepIndex) {
        return 'current'
      } else {
        return 'pending'
      }
    } catch (error) {
      console.warn('[ExecutionDetails] Error getting sub-plan step status:', error)
      return 'pending'
    }
  }
  
  // Handle sub-plan step click
  const handleSubPlanStepClick = (stepIndex: number, subStepIndex: number) => {
    emit('sub-plan-step-selected', stepIndex, subStepIndex)
  }
  
  // Handle user input form submission
  const handleUserInputSubmit = async () => {
    try {
      const inputData: any = {}
  
      const formInputs = props.planExecution.userInputWaitState?.formInputs
      if (formInputs && formInputs.length > 0) {
        Object.entries(formInputsStore).forEach(([index, value]) => {
          const numIndex = parseInt(index, 10)
          const label = formInputs[numIndex]?.label || `input_${index}`
          inputData[label] = value
        })
      } else {
        inputData.genericInput = genericInput.value
      }
  
      emit('user-input-submitted', inputData)
    } catch (error: any) {
      console.error('[ExecutionDetails] User input submission failed:', error)
    }
  }
  
  // Helper function to safely get options array
  const getOptionsArray = (options: string | string[] | undefined): string[] => {
    if (!options) return []
    if (Array.isArray(options)) return options
    if (typeof options === 'string') {
      return options.split(',').map(opt => opt.trim()).filter(opt => opt.length > 0)
    }
    return []
  }
  
  // Helper function to check if field is required
  const isRequired = (required: boolean | string | undefined): boolean => {
    if (typeof required === 'boolean') return required
    if (typeof required === 'string') return required === 'true'
    return false
  }
  </script>
  
  <style lang="less" scoped>
  .execution-details {
    .steps-container {
      margin-top: 16px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      overflow: hidden;
  
      .steps-title {
        margin: 0;
        padding: 10px 16px;
        font-size: 14px;
        font-weight: 600;
        color: #ffffff;
        background: rgba(102, 126, 234, 0.15);
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      }
  
      .ai-section {
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        cursor: pointer;
        transition: all 0.2s ease;
  
        &:last-child {
          border-bottom: none;
        }
  
        &:hover {
          background: rgba(255, 255, 255, 0.05);
        }
  
        &.running {
          background: rgba(102, 126, 234, 0.1);
          border-left: 3px solid #667eea;
        }
  
        &.completed {
          border-left: 3px solid rgba(34, 197, 94, 0.6);
        }
  
        &.pending {
          opacity: 0.7;
        }
  
        .section-header {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 16px;
          background: rgba(255, 255, 255, 0.02);
  
          .step-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 24px;
            height: 24px;
            background: rgba(102, 126, 234, 0.2);
            border-radius: 50%;
            font-size: 12px;
            font-weight: bold;
            color: #667eea;
          }
  
          .step-title {
            flex: 1;
            font-weight: 500;
            color: #ffffff;
          }
  
          .step-status {
            font-size: 12px;
            padding: 4px 8px;
            border-radius: 12px;
  
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
  
        .action-info {
          padding: 12px 16px;
          background: rgba(0, 0, 0, 0.2);
          border-top: 1px solid rgba(255, 255, 255, 0.1);
  
          .action-description {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 8px;
  
            .action-icon {
              font-size: 16px;
            }
          }
  
          .tool-params {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            margin-bottom: 8px;
            font-size: 13px;
  
            .tool-icon {
              margin-top: 2px;
            }
  
            .param-label {
              color: #aaaaaa;
              margin-right: 4px;
            }
  
            .param-content {
              margin: 0;
              padding: 6px;
              background: rgba(0, 0, 0, 0.2);
              border-radius: 4px;
              font-family: monospace;
              font-size: 12px;
              white-space: pre-wrap;
              max-height: 100px;
              overflow-y: auto;
            }
          }
  
          .think-details {
            margin-top: 10px;
            padding-top: 8px;
            border-top: 1px solid rgba(255, 255, 255, 0.1);
  
            .think-header {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 6px;
  
              .think-icon {
                font-size: 14px;
              }
  
              .think-label {
                color: #aaaaaa;
                font-size: 13px;
              }
            }
  
            .think-output {
              .think-content {
                margin: 0;
                padding: 8px;
                background: rgba(0, 0, 0, 0.15);
                border-radius: 4px;
                font-family: monospace;
                font-size: 12px;
                white-space: pre-wrap;
                max-height: 120px;
                overflow-y: auto;
                color: #bbbbbb;
              }
            }
          }
        }
  
        .sub-plan-steps {
          margin-top: 8px;
          padding: 8px 16px;
          background: rgba(102, 126, 234, 0.05);
          border-top: 1px solid rgba(102, 126, 234, 0.2);
  
          .sub-plan-header {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 8px;
  
            .sub-plan-icon {
              font-size: 14px;
              color: #667eea;
            }
  
            .sub-plan-title {
              font-size: 13px;
              font-weight: 600;
              color: #667eea;
            }
          }
  
          .sub-plan-step-list {
            display: flex;
            flex-direction: column;
            gap: 4px;
          }
  
          .sub-plan-step-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 8px;
            background: rgba(255, 255, 255, 0.02);
            border: 1px solid rgba(255, 255, 255, 0.05);
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.2s ease;
            margin-left: 20px;
  
            &:hover {
              background: rgba(255, 255, 255, 0.05);
              border-color: rgba(102, 126, 234, 0.3);
            }
  
            &.completed {
              background: rgba(34, 197, 94, 0.05);
              border-color: rgba(34, 197, 94, 0.2);
            }
  
            &.running {
              background: rgba(102, 126, 234, 0.05);
              border-color: rgba(102, 126, 234, 0.3);
              box-shadow: 0 0 4px rgba(102, 126, 234, 0.2);
            }
  
            &.pending {
              opacity: 0.6;
            }
  
            .sub-step-indicator {
              display: flex;
              align-items: center;
              gap: 4px;
              flex-shrink: 0;
  
              .sub-step-icon {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 16px;
                height: 16px;
                background: rgba(102, 126, 234, 0.1);
                border-radius: 50%;
                font-size: 10px;
                font-weight: bold;
                color: #667eea;
              }
  
              .sub-step-number {
                font-size: 10px;
                color: #888888;
                font-weight: 500;
                min-width: 12px;
                text-align: center;
              }
            }
  
            .sub-step-content {
              flex: 1;
              display: flex;
              align-items: center;
              justify-content: space-between;
              min-width: 0;
  
              .sub-step-title {
                color: #cccccc;
                font-size: 12px;
                line-height: 1.3;
                word-break: break-word;
                flex: 1;
              }
  
              .sub-step-badge {
                background: rgba(102, 126, 234, 0.15);
                color: #667eea;
                font-size: 9px;
                padding: 1px 4px;
                border-radius: 8px;
                font-weight: 500;
                flex-shrink: 0;
                margin-left: 6px;
              }
            }
          }
        }
  
        .user-input-form-container {
          margin-top: 12px;
          padding: 16px;
          background: rgba(102, 126, 234, 0.1);
          border: 1px solid rgba(102, 126, 234, 0.2);
          border-radius: 8px;
  
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
              align-items: end;
  
              @media (max-width: 768px) {
                grid-template-columns: 1fr;
                gap: 12px;
                align-items: start;
              }
  
              @media (max-width: 480px) {
                gap: 8px;
              }
            }
  
            .form-group {
              margin-bottom: 0;
              display: grid;
              grid-template-rows: 1fr 40px;
              height: 68px;
              align-content: stretch;
              gap: 5px;
              
              @media (max-width: 768px) {
                grid-template-rows: 1fr 42px;
                height: auto;
                min-height: 70px;
                align-content: stretch;
                gap: 4px;
              }
  
              label {
                display: block;
                margin-bottom: 0;
                font-size: 13px;
                font-weight: 500;
                color: #ffffff;
                line-height: 1.3;
                word-wrap: break-word;
                overflow-wrap: break-word;
                hyphens: auto;
                grid-row: 1;
                align-self: end;
                justify-self: start;
                
                @media (max-width: 768px) {
                  font-size: 12px;
                  align-self: start;
                }
              }
  
              .form-input {
                width: 100%;
                padding: 8px 12px;
                background: rgba(0, 0, 0, 0.3);
                border: 1px solid rgba(255, 255, 255, 0.2);
                border-radius: 6px;
                color: #ffffff;
                font-size: 14px;
                line-height: 1.4;
                height: 40px;
                box-sizing: border-box;
                transition: border-color 0.2s ease;
                grid-row: 2;
                align-self: stretch;
  
                &:focus {
                  outline: none;
                  border-color: #667eea;
                  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
                }
  
                &::placeholder {
                  color: #888888;
                }
                
                @media (max-width: 768px) {
                  font-size: 14px;
                  height: 42px;
                }
              }
  
              .form-textarea {
                resize: vertical;
                min-height: 60px;
                height: 60px;
                font-family: inherit;
                line-height: 1.4;
                box-sizing: border-box;
                padding: 8px 12px;
                background: rgba(0, 0, 0, 0.3);
                border: 1px solid rgba(255, 255, 255, 0.2);
                border-radius: 6px;
                color: #ffffff;
                font-size: 14px;
                transition: border-color 0.2s ease;
                grid-row: 2;
                align-self: stretch;
                
                &:focus {
                  outline: none;
                  border-color: #667eea;
                  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
                }

                &::placeholder {
                  color: #888888;
                }
                
                @media (max-width: 768px) {
                  height: 65px;
                }
              }

              &.form-group-wide {
                grid-column: span 2;

                @media (max-width: 768px) {
                  grid-column: span 1;
                }
              }

              &.form-group-full {
                grid-column: span 2;

                @media (max-width: 768px) {
                  grid-column: span 1;
                }
              }

              &:has(.form-textarea) {
                grid-template-rows: 1fr 60px;
                height: 88px;
                
                @media (max-width: 768px) {
                  grid-template-rows: 1fr 65px;
                  height: auto;
                  min-height: 93px;
                }
              }

              &.form-group-textarea {
                grid-template-rows: 1fr 60px;
                height: 88px;
                
                @media (max-width: 768px) {
                  grid-template-rows: 1fr 65px;
                  height: auto;
                  min-height: 93px;
                }
              }

              .form-select {
                cursor: pointer;
                height: 40px;
                padding: 8px 12px;
                background: rgba(0, 0, 0, 0.3);
                border: 1px solid rgba(255, 255, 255, 0.2);
                border-radius: 6px;
                color: #ffffff;
                font-size: 14px;
                line-height: 1.4;
                box-sizing: border-box;
                transition: border-color 0.2s ease;
                grid-row: 2;
                align-self: stretch;

                &:focus {
                  outline: none;
                  border-color: #667eea;
                  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
                }

                option {
                  background: #2d3748;
                  color: #ffffff;
                }
                
                @media (max-width: 768px) {
                  height: 42px;
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
    }
  }
  </style>
