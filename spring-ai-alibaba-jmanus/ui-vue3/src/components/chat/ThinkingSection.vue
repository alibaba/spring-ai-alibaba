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
  <div class="thinking-section" v-if="shouldShowThinking">
    <div class="thinking-header">
      <div class="thinking-avatar">
        <Icon icon="carbon:thinking" class="thinking-icon" />
      </div>
      <div class="thinking-label">{{ $t('chat.thinkingLabel') }}</div>
    </div>

    <div class="thinking-content">
      <!-- Basic thinking state -->
      <div v-if="thinking" class="thinking">
        <Icon icon="carbon:thinking" class="thinking-icon" />
        <span>{{ thinking }}</span>
      </div>

      <!-- Execution Details Component -->
      <ExecutionDetails
        v-if="thinkingDetails || planExecution"
        :plan-execution="(thinkingDetails || planExecution)!"
        :step-actions="stepActions || []"
        :generic-input="genericInput || ''"
        @agent-selected="handleAgentSelected"
        @sub-plan-selected="handleSubPlanSelected"
        @user-input-submitted="handleUserInputSubmit"
        @step-selected="handleStepSelected"
      />

      <!-- Display the default processing state only when there is no final content -->
      <div
        v-else-if="!hasContent && thinking"
        class="default-processing"
      >
        <div class="processing-indicator">
          <div class="thinking-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span>{{ thinking || $t('chat.thinkingProcessing') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'
import ExecutionDetails from './ExecutionDetails.vue'
import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'
import type { CompatiblePlanExecutionRecord } from './composables/useChatMessages'

interface Props {
  thinking?: string
  thinkingDetails?: CompatiblePlanExecutionRecord
  planExecution?: CompatiblePlanExecutionRecord
  stepActions?: any[]
  genericInput?: string
  hasContent?: boolean
}

interface Emits {
  (e: 'agent-selected', agentIndex: number, agent: AgentExecutionRecord): void
  (e: 'sub-plan-selected', agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord): void
  (e: 'user-input-submitted', inputData: any): void
  (e: 'step-selected', stepId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Computed properties
const shouldShowThinking = computed(() => {
  return props.thinking || 
         (props.thinkingDetails?.agentExecutionSequence?.length ?? 0) > 0 ||
         (props.planExecution?.agentExecutionSequence?.length ?? 0) > 0
})

// Event handlers
const handleAgentSelected = (agentIndex: number, agent: AgentExecutionRecord) => {
  emit('agent-selected', agentIndex, agent)
}

const handleSubPlanSelected = (agentIndex: number, subPlanIndex: number, subPlan: PlanExecutionRecord) => {
  emit('sub-plan-selected', agentIndex, subPlanIndex, subPlan)
}

const handleUserInputSubmit = (inputData: any) => {
  emit('user-input-submitted', inputData)
}

const handleStepSelected = (stepId: string) => {
  emit('step-selected', stepId)
}
</script>

<style lang="less" scoped>
.thinking-section {
  margin-bottom: 16px;
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  background: rgba(102, 126, 234, 0.05);
  overflow: hidden;
  
  .thinking-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: rgba(102, 126, 234, 0.1);
    border-bottom: 1px solid rgba(102, 126, 234, 0.15);
    
    .thinking-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      
      .thinking-icon {
        font-size: 16px;
        color: #ffffff;
      }
    }
    
    .thinking-label {
      font-weight: 600;
      color: #ffffff;
      font-size: 14px;
    }
  }
  
  .thinking-content {
    padding: 16px;
    
    .thinking {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      padding: 8px 12px;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 8px;
      font-size: 13px;
      color: #ffffff;
      
      .thinking-icon {
        font-size: 14px;
        color: #667eea;
        flex-shrink: 0;
      }
      
      span {
        line-height: 1.4;
      }
    }
    
    .default-processing {
      .processing-indicator {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        color: #aaaaaa;
        font-size: 14px;
        
        .thinking-dots {
          display: flex;
          gap: 4px;
          
          span {
            width: 6px;
            height: 6px;
            background: #667eea;
            border-radius: 50%;
            animation: thinking-pulse 1.5s ease-in-out infinite;
            
            &:nth-child(1) {
              animation-delay: 0s;
            }
            
            &:nth-child(2) {
              animation-delay: 0.2s;
            }
            
            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
      }
    }
  }
}

@keyframes thinking-pulse {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@media (max-width: 768px) {
  .thinking-section {
    .thinking-header {
      padding: 10px 14px;
      
      .thinking-avatar {
        width: 28px;
        height: 28px;
        
        .thinking-icon {
          font-size: 14px;
        }
      }
      
      .thinking-label {
        font-size: 13px;
      }
    }
    
    .thinking-content {
      padding: 14px;
      
      .thinking {
        padding: 6px 10px;
        font-size: 12px;
      }
    }
  }
}
</style>
