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
  <div class="config-section">
    <div class="section-header">
      <Icon icon="carbon:play" width="16" />
      <span>{{ t('sidebar.executionController') }}</span>
    </div>
    <div class="execution-content">
      <!-- Parameter Requirements Display -->
      <div class="params-requirements-group">
        <label>{{ t('sidebar.parameterRequirements') }}</label>
        <div class="params-help-text">
          {{ t('sidebar.parameterRequirementsHelp') }}
        </div>
        
        <!-- Show parameter fields only if there are parameters -->
        <div v-if="parameterRequirements.hasParameters" class="parameter-fields">
          <div
            v-for="param in parameterRequirements.parameters"
            :key="param"
            class="parameter-field"
          >
            <label class="parameter-label">
              {{ param }}
              <span class="required">*</span>
            </label>
            <input
              v-model="parameterValues[param]"
              class="parameter-input"
              :placeholder="`Enter value for ${param}`"
              @input="updateParameterValue(param, ($event.target as HTMLInputElement).value)"
              required
            />
          </div>
        </div>
        
      </div>
      <div class="api-url-display">
        <span class="api-url-label">{{ t('sidebar.apiUrl') }}:</span>
        <code class="api-url">{{ computedApiUrl }}</code>
      </div>
      <div class="api-url-display">
        <span class="api-url-label">{{ t('sidebar.statusApiUrl') }}:</span>
        <code class="api-url">/api/executor/details/{planId}</code>
      </div>
      <button
        class="btn btn-primary execute-btn"
        @click="handleExecutePlan"
        :disabled="isExecuting || isGenerating"
      >
        <Icon
          :icon="isExecuting ? 'carbon:circle-dash' : 'carbon:play'"
          width="16"
          :class="{ spinning: isExecuting }"
        />
        {{ isExecuting ? t('sidebar.executing') : t('sidebar.executePlan') }}
      </button>
      <button
        class="btn publish-mcp-btn"
        @click="handlePublishMcpService"
        :disabled="!currentPlanTemplateId"
        v-if="showPublishButton"
      >
        <Icon icon="carbon:application" width="16" />
        {{ t('sidebar.publishMcpService') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import { PlanParameterApiService, type ParameterRequirements } from '@/api/plan-parameter-api-service'

const { t } = useI18n()


// Props
interface Props {
  currentPlanTemplateId?: string
  isExecuting?: boolean
  isGenerating?: boolean
  showPublishButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentPlanTemplateId: '',
  isExecuting: false,
  isGenerating: false,
  showPublishButton: true
})

// Emits
const emit = defineEmits<{
  executePlan: []
  publishMcpService: []
  clearParams: []
  updateExecutionParams: [params: string]
}>()

// Local state
const executionParams = ref('')
const parameterRequirements = ref<ParameterRequirements>({
  parameters: [],
  hasParameters: false,
  requirements: ''
})
const parameterValues = ref<Record<string, string>>({})
const isLoadingParameters = ref(false)

// Computed properties
const computedApiUrl = computed(() => {
  if (!props.currentPlanTemplateId) return ''
  return `/api/executor/execute/${props.currentPlanTemplateId}`
})

// Methods
const handleExecutePlan = () => {
  emit('executePlan')
}

const handlePublishMcpService = () => {
  emit('publishMcpService')
}

const clearExecutionParams = () => {
  executionParams.value = ''
  emit('clearParams')
}

// Load parameter requirements when plan template changes
const loadParameterRequirements = async () => {
  if (!props.currentPlanTemplateId) {
    parameterRequirements.value = {
      parameters: [],
      hasParameters: false,
      requirements: ''
    }
    parameterValues.value = {}
    return
  }

  isLoadingParameters.value = true
  try {
    const requirements = await PlanParameterApiService.getParameterRequirements(props.currentPlanTemplateId)
    parameterRequirements.value = requirements
    
    // Initialize parameter values
    const newValues: Record<string, string> = {}
    requirements.parameters.forEach(param => {
      newValues[param] = parameterValues.value[param] || ''
    })
    parameterValues.value = newValues
    
    // Update execution params with current parameter values
    updateExecutionParamsFromParameters()
  } catch (error) {
    console.error('Failed to load parameter requirements:', error)
    parameterRequirements.value = {
      parameters: [],
      hasParameters: false,
      requirements: ''
    }
  } finally {
    isLoadingParameters.value = false
  }
}

// Update parameter value and sync with execution params
const updateParameterValue = (paramName: string, value: string) => {
  parameterValues.value[paramName] = value
  updateExecutionParamsFromParameters()
}

// Update execution params from parameter values
const updateExecutionParamsFromParameters = () => {
  if (parameterRequirements.value.hasParameters) {
    // Convert parameter values to JSON string for execution
    executionParams.value = JSON.stringify(parameterValues.value, null, 2)
  } else {
    executionParams.value = ''
  }
  emit('updateExecutionParams', executionParams.value)
}


// Watch for changes in plan template ID
watch(() => props.currentPlanTemplateId, () => {
  loadParameterRequirements()
})

// Watch for changes in execution params (for backward compatibility)
watch(() => executionParams.value, (newValue) => {
  emit('updateExecutionParams', newValue)
})

// Load parameters on mount
onMounted(() => {
  loadParameterRequirements()
})

// Expose methods for parent component
defineExpose({
  executionParams,
  clearExecutionParams,
  loadParameterRequirements
})
</script>

<style scoped>
.config-section {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
  gap: 8px;
}

.execution-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.params-requirements-group {
  label {
    display: block;
    margin-bottom: 6px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.8);
    font-weight: 500;
  }

  .params-help-text {
    margin-bottom: 12px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.4;
    padding: 6px 8px;
    background: rgba(102, 126, 234, 0.1);
    border: 1px solid rgba(102, 126, 234, 0.2);
    border-radius: 4px;
  }

  .parameter-fields {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 12px;
  }

  .parameter-field {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .parameter-label {
      font-size: 11px;
      color: rgba(255, 255, 255, 0.8);
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;

      .required {
        color: #ff6b6b;
        font-weight: bold;
      }
    }

    .parameter-input {
      width: 100%;
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 6px;
      color: white;
      font-size: 12px;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      padding: 8px 12px;
      min-height: 36px;

      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
      }

      &::placeholder {
        color: rgba(255, 255, 255, 0.4);
      }
    }
  }

}


.api-url-display {
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 11px;

  .api-url-label {
    color: rgba(255, 255, 255, 0.7);
    margin-right: 8px;
  }

  .api-url {
    color: #64b5f6;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    word-break: break-all;
  }
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;

  &.btn-primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    }
  }

  &.publish-mcp-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
    border: none;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none !important;
    box-shadow: none !important;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }
}

.execute-btn,
.publish-mcp-btn {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  width: 100%;
  margin-bottom: 8px;
}

.publish-mcp-btn {
  margin-bottom: 0;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
