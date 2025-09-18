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
              :class="{ 'error': parameterErrors[param] }"
              :placeholder="`Enter value for ${param}`"
              @input="updateParameterValue(param, ($event.target as HTMLInputElement).value)"
              required
            />
            <div v-if="parameterErrors[param]" class="parameter-error">
              {{ parameterErrors[param] }}
            </div>
          </div>
        </div>
        
        <!-- Validation status message -->
        <div v-if="parameterRequirements.hasParameters && !canExecute && !props.isExecuting && !props.isGenerating" class="validation-message">
          <Icon icon="carbon:warning" width="14" />
          {{ t('sidebar.fillAllRequiredParameters') }}
        </div>
        
      </div>
      <button
        class="btn btn-primary execute-btn"
        @click="handleExecutePlan"
        :disabled="!canExecute"
      >
        <Icon
          :icon="props.isExecuting ? 'carbon:circle-dash' : 'carbon:play'"
          width="16"
          :class="{ spinning: props.isExecuting }"
        />
        {{ props.isExecuting ? t('sidebar.executing') : t('sidebar.executePlan') }}
      </button>
      <button
        class="btn publish-mcp-btn"
        @click="handlePublishMcpService"
        :disabled="!currentPlanTemplateId"
        v-if="showPublishButton"
      >
        <Icon icon="carbon:application" width="16" />
        {{ buttonText }}
      </button>
      
      <!-- Internal Call wrapper - only show when enableInternalToolcall is true -->
      <div v-if="toolInfo?.enableInternalToolcall" class="call-example-wrapper">
        <div class="call-example-header">
          <h4 class="call-example-title">Internal Call</h4>
          <p class="call-example-description">You have published this plan-act as an internal method. You can find this tool's method in the agent configuration's add tools section and add and use it.</p>
        </div>
        <div class="internal-call-wrapper">
          <div class="call-info">
            <div class="call-method">Internal Method Call</div>
            <div class="call-endpoint">Tool Name: {{ toolInfo?.toolName || currentPlanTemplateId }}</div>
            <div v-if="toolInfo?.serviceGroup" class="call-endpoint">Service Group: {{ toolInfo.serviceGroup }}</div>
            <div class="call-description">After adding this tool in agent configuration, you can directly call this method in the agent</div>
            <div class="call-example">
              <strong>Usage:</strong>
              <pre class="example-code">In the agent configuration's "Add Tools" section, search and add this tool, then call it directly in the agent</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- HTTP API URLs wrapper with tabs - only show when enableHttpService is true -->
      <div v-if="toolInfo?.enableHttpService" class="call-example-wrapper">
        <div class="call-example-header">
          <h4 class="call-example-title">HTTP Call Example</h4>
          <p class="call-example-description">You have published this plan-act as an HTTP service. You can call it according to the example below.</p>
        </div>
        <div class="http-api-urls-wrapper">
          <div class="tab-container">
            <div class="tab-header">
              <button 
                v-for="tab in apiTabs" 
                :key="tab.id"
                :class="['tab-button', { active: activeTab === tab.id }]"
                @click="activeTab = tab.id"
              >
                {{ tab.label }}
              </button>
            </div>
            <div class="tab-content">
              <div v-for="tab in apiTabs" :key="tab.id" v-show="activeTab === tab.id" class="tab-panel">
                <div class="http-api-url-display">
                  <div class="api-method">{{ tab.method }}</div>
                  <div class="api-endpoint">{{ tab.endpoint }}</div>
                  <div class="api-description">{{ tab.description }}</div>
                  <div v-if="tab.example" class="api-example">
                    <strong>Example:</strong>
                    <pre class="example-code">{{ tab.example }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- MCP Call wrapper - only show when enableMcpService is true -->
      <div v-if="toolInfo?.enableMcpService" class="call-example-wrapper">
        <div class="call-example-header">
          <h4 class="call-example-title">MCP Call</h4>
          <p class="call-example-description">You have published this plan-act as an MCP service. You can use it through MCP streamable or SSE methods.</p>
        </div>
        <div class="mcp-call-wrapper">
          <div class="call-info">
            <div class="call-method">MCP Service Call</div>
            <div class="call-endpoint">MCP Endpoint: /mcp/execute</div>
            <div class="call-description">Call through MCP protocol using streaming or SSE methods</div>
            <div class="call-example">
              <strong>Usage:</strong>
              <pre class="example-code">Connect to this service through MCP client, using streamable or SSE methods for calling</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import { PlanParameterApiService, type ParameterRequirements } from '@/api/plan-parameter-api-service'
import type { CoordinatorToolVO } from '@/api/coordinator-tool-api-service'

const { t } = useI18n()


// Props
interface Props {
  currentPlanTemplateId?: string
  isExecuting?: boolean
  isGenerating?: boolean
  showPublishButton?: boolean
  toolInfo?: CoordinatorToolVO
}

const props = withDefaults(defineProps<Props>(), {
  currentPlanTemplateId: '',
  isExecuting: false,
  isGenerating: false,
  showPublishButton: true,
  toolInfo: () => ({
    toolName: '',
    toolDescription: '',
    planTemplateId: '',
    inputSchema: '[]',
    enableHttpService: false,
    enableMcpService: false,
    enableInternalToolcall: false,
    serviceGroup: ''
  })
})

// Emits
const emit = defineEmits<{
  executePlan: [replacementParams?: Record<string, string>]
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
const activeTab = ref('get-sync')
const parameterErrors = ref<Record<string, string>>({})
const isValidationError = ref(false)

// API tabs configuration
const apiTabs = ref([
  {
    id: 'get-sync',
    label: 'GET + Sync',
    method: 'GET',
    endpoint: '/api/executor/executeByToolNameSync/{toolName}',
    description: 'Synchronous GET request - returns execution result immediately',
    example: `GET /api/executor/executeByToolNameSync/my-tool?allParams={"rawParam":"test"}
Response: {
  "status": "completed",
  "result": "Execution result here"
}`
  },
  {
    id: 'post-sync',
    label: 'POST + Sync',
    method: 'POST',
    endpoint: '/api/executor/executeByToolNameSync',
    description: 'Synchronous POST request - returns execution result immediately',
    example: `POST /api/executor/executeByToolNameSync
Content-Type: application/json

{
  "toolName": "my-tool",
  "replacementParams": {
    "rawParam": "test"
  },
  "uploadedFiles": []
}

Response: {
  "status": "completed",
  "result": "Execution result here"
}`
  },
  {
    id: 'post-async',
    label: 'POST + Async',
    method: 'POST',
    endpoint: '/api/executor/executeByToolNameAsync',
    description: 'Asynchronous POST request - returns task ID, check status separately',
    example: `POST /api/executor/executeByToolNameAsync
Content-Type: application/json

{
  "toolName": "my-tool",
  "replacementParams": {
    "rawParam": "test"
  },
  "uploadedFiles": []
}

Response: {
  "planId": "plan-123",
  "status": "processing",
  "message": "Task submitted, processing",
  "memoryId": "ABC12345",
  "toolName": "my-tool",
  "planTemplateId": "template-456"
}

# Check execution status and get detailed results:
GET /api/executor/details/{planId}
Response: {
  "currentPlanId": "plan-123",
  "title": "Plan Title",
  "status": "completed",
  "summary": "Execution completed successfully",
  "agentExecutionSequence": [...],
  "userInputWaitState": null
}`
  }
])

// Computed properties
const isAnyServiceEnabled = computed(() => {
  return props.toolInfo?.enableInternalToolcall || 
         props.toolInfo?.enableHttpService || 
         props.toolInfo?.enableMcpService
})

const buttonText = computed(() => {
  return isAnyServiceEnabled.value ? t('sidebar.updateServiceStatus') : t('sidebar.publishMcpService')
})

const canExecute = computed(() => {
  if (props.isExecuting || props.isGenerating) {
    return false
  }
  
  if (parameterRequirements.value.hasParameters) {
    // Check if all required parameters are filled
    return parameterRequirements.value.parameters.every(param => 
      parameterValues.value[param]?.trim()
    )
  }
  
  return true
})

// Methods
const handleExecutePlan = () => {
  // Validate parameters before execution
  if (!validateParameters()) {
    console.log('[ExecutionController] Parameter validation failed:', parameterErrors.value)
    return
  }
  
  // Pass replacement parameters if available
  const replacementParams = parameterRequirements.value.hasParameters && Object.keys(parameterValues.value).length > 0 
    ? parameterValues.value 
    : undefined
  emit('executePlan', replacementParams)
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
    // Don't show error for 404 - template might not be ready yet
    if (error instanceof Error && !error.message.includes('404')) {
      console.warn('Parameter requirements not available yet, will retry later')
    }
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
  // Clear error for this parameter when user starts typing
  if (parameterErrors.value[paramName]) {
    delete parameterErrors.value[paramName]
  }
  updateExecutionParamsFromParameters()
}

// Validate all parameters
const validateParameters = (): boolean => {
  parameterErrors.value = {}
  isValidationError.value = false
  
  if (!parameterRequirements.value.hasParameters) {
    return true
  }
  
  let hasErrors = false
  
  parameterRequirements.value.parameters.forEach(param => {
    const value = parameterValues.value[param]?.trim()
    if (!value) {
      parameterErrors.value[param] = `${param} is required`
      hasErrors = true
    }
  })
  
  isValidationError.value = hasErrors
  return !hasErrors
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
watch(() => props.currentPlanTemplateId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // If this is a new template ID (not from initial load), retry loading parameters
    if (oldId && newId.startsWith('planTemplate-')) {
      // Retry loading parameters with a delay for new templates
      setTimeout(() => {
        loadParameterRequirements()
      }, 1000)
    } else {
      loadParameterRequirements()
    }
  }
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

      &.error {
        border-color: #ff6b6b;
        box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.2);
      }
    }

    .parameter-error {
      color: #ff6b6b;
      font-size: 11px;
      margin-top: 4px;
      display: block;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    }
  }

  .validation-message {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #ffa726;
    font-size: 14px;
    margin-top: 8px;
    padding: 8px 12px;
    background: rgba(255, 167, 38, 0.1);
    border: 1px solid rgba(255, 167, 38, 0.3);
    border-radius: 4px;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  }

}


.call-example-wrapper {
  margin-top: 12px;
}

.call-example-header {
  margin-bottom: 12px;
}

.call-example-title {
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 6px 0;
}

.call-example-description {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  line-height: 1.4;
  margin: 0;
  padding: 8px 12px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 6px;
}

.http-api-urls-wrapper {
  margin-top: 0;
}

.internal-call-wrapper,
.mcp-call-wrapper {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
}

.call-info {
  font-size: 11px;

  .call-method {
    display: inline-block;
    padding: 2px 6px;
    background: #667eea;
    color: white;
    border-radius: 3px;
    font-size: 10px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .call-endpoint {
    color: #64b5f6;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    margin-bottom: 8px;
    word-break: break-all;
    background: rgba(0, 0, 0, 0.3);
    padding: 6px 8px;
    border-radius: 4px;
    border: 1px solid rgba(100, 181, 246, 0.2);
  }

  .call-description {
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 8px;
    line-height: 1.4;
  }

  .call-example {
    margin-top: 8px;

    strong {
      color: rgba(255, 255, 255, 0.9);
      font-size: 10px;
      display: block;
      margin-bottom: 4px;
    }

    .example-code {
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 4px;
      padding: 8px;
      color: #e0e0e0;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 10px;
      line-height: 1.3;
      white-space: pre-wrap;
      word-break: break-all;
      overflow-x: auto;
    }
  }
}

.tab-container {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.tab-header {
  display: flex;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tab-button {
  flex: 1;
  padding: 8px 12px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border-right: 1px solid rgba(255, 255, 255, 0.1);

  &:last-child {
    border-right: none;
  }

  &:hover {
    background: rgba(102, 126, 234, 0.1);
    color: rgba(255, 255, 255, 0.8);
  }

  &.active {
    background: rgba(102, 126, 234, 0.2);
    color: #667eea;
    font-weight: 600;
  }
}

.tab-content {
  padding: 0;
}

.tab-panel {
  padding: 0;
}

.http-api-url-display {
  padding: 12px;
  font-size: 11px;

  .api-method {
    display: inline-block;
    padding: 2px 6px;
    background: #667eea;
    color: white;
    border-radius: 3px;
    font-size: 10px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .api-endpoint {
    color: #64b5f6;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    margin-bottom: 8px;
    word-break: break-all;
    background: rgba(0, 0, 0, 0.3);
    padding: 6px 8px;
    border-radius: 4px;
    border: 1px solid rgba(100, 181, 246, 0.2);
  }

  .api-description {
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 8px;
    line-height: 1.4;
  }

  .api-example {
    margin-top: 8px;

    strong {
      color: rgba(255, 255, 255, 0.9);
      font-size: 10px;
      display: block;
      margin-bottom: 4px;
    }

    .example-code {
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 4px;
      padding: 8px;
      color: #e0e0e0;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 10px;
      line-height: 1.3;
      white-space: pre-wrap;
      word-break: break-all;
      overflow-x: auto;
    }
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
