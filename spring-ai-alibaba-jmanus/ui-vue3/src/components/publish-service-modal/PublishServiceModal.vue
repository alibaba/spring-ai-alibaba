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
  <Modal 
    v-model="showModal" 
    :title="modalTitle" 
    @confirm="handlePublish" 
  >
    <div class="modal-form wide-modal">
      <!-- Tool Name -->
      <div class="form-section">
        <div class="form-item">
          <label>{{ t('mcpService.toolNameRequired') }}</label>
          <input
            type="text"
            v-model="formData.serviceName"
            :placeholder="t('mcpService.toolNamePlaceholder')"
            :class="{ 'error': !formData.serviceName || !formData.serviceName.trim() }"
            required
          />
          <div class="field-description">{{ t('mcpService.toolNameDescription') }}</div>
        </div>
      </div>

      <!-- Tool Description -->
      <div class="form-section">
        <div class="form-item">
          <label>{{ t('mcpService.toolDescriptionRequired') }}</label>
          <textarea
            v-model="formData.userRequest"
            :placeholder="t('mcpService.toolDescriptionPlaceholder')"
            :class="{ 'error': !formData.userRequest || !formData.userRequest.trim() }"
            class="description-field"
            rows="4"
            required
          />
          <div class="field-description">{{ t('mcpService.toolDescriptionDescription') }}</div>
        </div>
      </div>

      <!-- Service Group -->
      <div class="form-section">
        <div class="form-item">
          <label>{{ t('mcpService.serviceGroup') }}</label>
          <input
            type="text"
            v-model="formData.serviceGroup"
            :placeholder="t('mcpService.serviceGroupPlaceholder')"
            :class="{ 'error': !formData.serviceGroup || !formData.serviceGroup.trim() }"
            required
          />
          <div class="field-description">{{ t('mcpService.serviceGroupDescription') }}</div>
        </div>
      </div>

      <!-- Parameter Configuration -->
      <div class="form-section">
        <div class="section-title">{{ t('mcpService.parameterConfig') }}</div>
        
        <!-- Parameter Requirements Help Text -->
        <div v-if="parameterRequirements.hasParameters" class="params-help-text">
          {{ t('sidebar.parameterRequirementsHelp') }}
        </div>
        
        <div class="parameter-table">
          <table>
            <thead>
              <tr>
                <th>{{ t('mcpService.parameterName') }}</th>
                <th>{{ t('mcpService.parameterDescription') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(param, index) in formData.parameters" :key="index">
                <td>
                  <input
                    type="text"
                    v-model="param.name"
                    :placeholder="t('mcpService.parameterName')"
                    class="parameter-input"
                    :readonly="parameterRequirements.hasParameters"
                    :class="{ 'readonly-input': parameterRequirements.hasParameters }"
                    required
                  />
                </td>
                <td>
                  <input
                    type="text"
                    v-model="param.description"
                    :placeholder="t('mcpService.parameterDescription')"
                    class="parameter-input"
                    required
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Service Publishing Options -->
      <div class="form-section">
        <div class="service-publish-options">
          <!-- Internal Toolcall Publishing Option -->
          <div class="internal-toolcall-publish-option">
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="publishAsInternalToolcall"
                class="checkbox-input"
              />
              <span class="checkbox-text">{{ t('mcpService.publishAsInternalToolcall') }}</span>
            </label>
            <div class="checkbox-description">
              {{ t('mcpService.publishAsInternalToolcallDescription') }}
            </div>
          </div>
          
          <!-- HTTP POST Service Publishing Option -->
          <div class="http-publish-option">
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="publishAsHttpService"
                class="checkbox-input"
              />
              <span class="checkbox-text">{{ t('mcpService.publishAsHttpService') }}</span>
            </label>
            <div class="checkbox-description">
              {{ t('mcpService.publishAsHttpServiceDescription') }}
            </div>
            
          </div>
          
          <!-- MCP Service Publishing Option -->
          <div class="mcp-publish-option">
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="publishAsMcpService"
                class="checkbox-input"
              />
              <span class="checkbox-text">{{ t('mcpService.publishAsMcpService') }}</span>
            </label>
            <div class="checkbox-description">
              {{ t('mcpService.publishAsMcpServiceDescription') }}
            </div>
            
            <!-- Endpoint Configuration - Only shown when MCP service publishing option is selected -->
            <div v-if="publishAsMcpService" class="form-section">
              <div class="form-item">
                <label>{{ t('mcpService.endpointRequired') }}</label>
                <div class="endpoint-description">
                  {{ t('mcpService.endpointDescription') }}
                </div>
                <div class="endpoint-container">
                  <div class="endpoint-row">
                    <div class="custom-select">
                      <div class="select-input-container">
                        <div class="input-content">
                          <Icon icon="carbon:application" width="16" class="input-icon" />
                          <input
                            type="text"
                            v-model="formData.endpoint"
                            :placeholder="t('mcpService.endpointPlaceholder')"
                            class="select-input"
                            @focus="isDropdownOpen = true"
                            @input="handleEndpointInput"
                            @keydown.enter="handleEndpointEnter"
                            @blur="handleEndpointBlur"
                          />
                        </div>
                        <button class="select-arrow-btn" @click="toggleDropdown" title="Expand Options">
                          <Icon
                            :icon="isDropdownOpen ? 'carbon:chevron-up' : 'carbon:chevron-down'"
                            width="14"
                            class="chevron"
                          />
                        </button>
                      </div>

                      <!-- Dropdown Options - Using absolute positioning, not occupying document flow -->
                      <div v-if="isDropdownOpen" class="select-dropdown" :class="{ 'dropdown-top': dropdownPosition === 'top' }">
                        <div class="dropdown-header">
                          <span>{{ t('mcpService.selectEndpoint') }}</span>
                          <button class="close-btn" @click="isDropdownOpen = false">
                            <Icon icon="carbon:close" width="12" />
                          </button>
                        </div>
                        <div class="select-options">
                          <button
                            v-for="endpoint in availableEndpoints"
                            :key="endpoint"
                            class="select-option"
                            :class="{ active: formData.endpoint === endpoint }"
                            @click="selectEndpoint(endpoint)"
                          >
                            <span class="option-name">{{ endpoint }}</span>
                            <Icon v-if="formData.endpoint === endpoint" icon="carbon:checkmark" width="14" class="check-icon" />
                          </button>
                        </div>
                        <!-- Manual Input Area -->
                        <div class="manual-input-section">
                          <div class="manual-input-container">
                            <input
                              type="text"
                              v-model="manualEndpointInput"
                              :placeholder="t('mcpService.manualInput')"
                              class="manual-input"
                              @keydown.enter="addManualEndpoint"
                              @blur="handleManualInputBlur"
                            />
                            <button class="add-manual-btn" @click="addManualEndpoint">
                              <Icon icon="carbon:add" width="14" />
                            </button>
                          </div>
                        </div>
                      </div>
                      <div v-if="isDropdownOpen" class="backdrop" @click="isDropdownOpen = false"></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- MCP Streamable URL Configuration -->
              <div v-if="endpointUrl" class="form-item url-item">
                <label>{{ t('mcpService.mcpStreamableUrl') }}</label>
                <div class="url-container">
                  <div class="url-display" @dblclick="copyEndpointUrl" :title="t('mcpService.copyUrl') + ': ' + endpointUrl">
                    <span class="url-text">{{ endpointUrl || t('mcpService.mcpStreamableUrlPlaceholder') }}</span>
                    <Icon icon="carbon:copy" width="16" class="copy-icon" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <template #footer>
      <div class="button-container">
        <!-- Delete Button - Only shown when saved -->
        <button 
          v-if="isSaved && currentTool?.id" 
          class="action-btn danger" 
          @click="handleDelete" 
          :disabled="deleting"
        >
          <Icon icon="carbon:loading" v-if="deleting" class="loading-icon" />
          <Icon icon="carbon:trash-can" v-else />
          {{ deleting ? t('mcpService.deleting') : t('mcpService.delete') }}
        </button>
        
        <!-- Publish as Service Button - Always shown -->
        <button class="action-btn primary" @click="handlePublish" :disabled="publishing">
          <Icon icon="carbon:loading" v-if="publishing" class="loading-icon" />
          <Icon icon="carbon:cloud-upload" v-else />
          {{ publishing ? t('mcpService.publishing') : t('mcpService.publishAsService') }}
        </button>
        
      </div>
    </template>
  </Modal>

  <!-- Error Toast -->
  <div v-if="error" class="error-toast" @click="error = ''">
    <Icon icon="carbon:error" />
    {{ error }}
  </div>

  <!-- Success Toast -->
  <div v-if="success" class="success-toast" @click="success = ''">
    <Icon icon="carbon:checkmark" />
    {{ success }}
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal/index.vue'
import { CoordinatorToolApiService, type CoordinatorToolVO } from '@/api/coordinator-tool-api-service'
import { PlanParameterApiService, type ParameterRequirements } from '@/api/plan-parameter-api-service'

const { t } = useI18n()

// Props
interface Props {
  modelValue: boolean
  planTemplateId?: string
  planTitle?: string
  planDescription?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  planTemplateId: '',
  planTitle: '',
  planDescription: ''
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'published': [tool: CoordinatorToolVO | null]
}>()

// Reactive data
const showModal = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const error = ref('')
const success = ref('')
const publishing = ref(false)
const deleting = ref(false)
const availableEndpoints = ref<string[]>([])

// New reactive data
const endpointUrl = ref('')
const isDropdownOpen = ref(false)
const dropdownPosition = ref('bottom')
const manualEndpointInput = ref('')

// Current tool data, used to determine whether to create or update
const currentTool = ref<CoordinatorToolVO | null>(null)

// Publishing status
const isSaved = ref(false)

// Service publishing options
const publishAsMcpService = ref(false)
const publishAsHttpService = ref(false)
const publishAsInternalToolcall = ref(true) // Default to true

// Parameter requirements from plan template
const parameterRequirements = ref<ParameterRequirements>({
  parameters: [],
  hasParameters: false,
  requirements: ''
})
const isLoadingParameters = ref(false)





// Form data
const formData = reactive({
  serviceName: '',
  userRequest: '',
  endpoint: '',
  serviceGroup: '',
  parameters: [] as Array<{ name: string; description: string }>
})

// Calculate modal title
const modalTitle = computed(() => {
  const isUpdate = currentTool.value && currentTool.value.id
  return isUpdate ? t('mcpService.updateService') : t('mcpService.createService')
})

// Initialize form data
const initializeFormData = () => {
  formData.serviceName = '' // Show empty when no entity
  formData.userRequest = props.planDescription || ''
  formData.endpoint = ''
  formData.serviceGroup = ''
  // Only reset parameters when not loaded from plan template
  if (!parameterRequirements.value.hasParameters) {
    formData.parameters = []
  }
  currentTool.value = null
  endpointUrl.value = ''
  isSaved.value = false
}

// Load available endpoints
const loadEndpoints = async () => {
  try {
    availableEndpoints.value = await CoordinatorToolApiService.getAllEndpoints()
  } catch (err: any) {
    console.error('Failed to load endpoints:', err)
    showMessage(t('mcpService.loadEndpointsFailed') + ': ' + err.message, 'error')
  }
}

// Load parameter requirements from plan template
const loadParameterRequirements = async () => {
  if (!props.planTemplateId) {
    parameterRequirements.value = {
      parameters: [],
      hasParameters: false,
      requirements: ''
    }
    return
  }

  isLoadingParameters.value = true
  try {
    const requirements = await PlanParameterApiService.getParameterRequirements(props.planTemplateId)
    parameterRequirements.value = requirements
    
    // Initialize form parameters with extracted parameters
    if (requirements.hasParameters) {
      formData.parameters = requirements.parameters.map(param => ({
        name: param,
        description: ''
      }))
    }
  } catch (error) {
    console.error('[PublishModal] Failed to load parameter requirements:', error)
    // Don't show error for 404 - template might not be ready yet
    if (error instanceof Error && !error.message.includes('404')) {
      console.warn('[PublishModal] Parameter requirements not available yet, will retry later')
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

    // Dropdown related methods
const toggleDropdown = () => {
  if (!isDropdownOpen.value) {
    // Calculate position before showing dropdown
    calculateDropdownPosition()
  }
  isDropdownOpen.value = !isDropdownOpen.value
}

const calculateDropdownPosition = () => {
  const selectElement = document.querySelector('.custom-select') as HTMLElement
  if (!selectElement) return
  
  const rect = selectElement.getBoundingClientRect()
  const windowHeight = window.innerHeight
  const dropdownHeight = 200 // Estimated dropdown height
  
  // If there's not enough space below, show above
  if (rect.bottom + dropdownHeight > windowHeight) {
    dropdownPosition.value = 'top'
  } else {
    dropdownPosition.value = 'bottom'
  }
}

const selectEndpoint = (endpoint: string) => {
  formData.endpoint = endpoint
  isDropdownOpen.value = false
  manualEndpointInput.value = ''
}

const addManualEndpoint = () => {
  if (manualEndpointInput.value.trim()) {
    const newEndpoint = manualEndpointInput.value.trim().replace(/[^a-zA-Z0-9_/]/g, '')
    if (newEndpoint) {
      formData.endpoint = newEndpoint
      if (!availableEndpoints.value.includes(newEndpoint)) {
        availableEndpoints.value.push(newEndpoint)
      }
      isDropdownOpen.value = false
      manualEndpointInput.value = ''
    }
  }
}

const handleManualInputBlur = () => {
      // Delay processing to ensure click events can trigger normally
  setTimeout(() => {
    if (manualEndpointInput.value.trim()) {
      addManualEndpoint()
    }
  }, 200)
}

// Handle dropdown input
const handleEndpointInput = () => {
  // When user inputs in the input box, if it's an existing endpoint, select it directly
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}

// Handle dropdown enter key
const handleEndpointEnter = () => {
  // When user presses enter in input box, if it's an existing endpoint, select it directly
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}

// Handle dropdown blur
const handleEndpointBlur = () => {
  // When input box loses focus, if it's an existing endpoint, select it directly
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}



// Copy endpointUrl
const copyEndpointUrl = async () => {
  if (!endpointUrl.value) return
  
  try {
    await navigator.clipboard.writeText(endpointUrl.value)
    showMessage(t('common.copy') + ' ' + t('common.success'), 'success')
  } catch (err) {
    console.error('Copy failed:', err)
    showMessage(t('common.copy') + ' ' + t('common.error'), 'error')
  }
}

// Show message
const showMessage = (msg: string, type: 'success' | 'error' | 'info') => {
  if (type === 'success') {
    success.value = msg
    setTimeout(() => {
      success.value = ''
    }, 3000)
  } else if (type === 'error') {
    error.value = msg
    setTimeout(() => {
      error.value = ''
    }, 5000)
  }
}

// Validate form
const validateForm = (): boolean => {
  // Validate tool name
  if (!formData.serviceName || !formData.serviceName.trim()) {
    showMessage(t('mcpService.toolNameRequiredError'), 'error')
    return false
  }
  
  // Validate tool description
  if (!formData.userRequest || !formData.userRequest.trim()) {
    showMessage(t('mcpService.toolDescriptionRequiredError'), 'error')
    return false
  }
  
  // Validate service group
  if (!formData.serviceGroup || !formData.serviceGroup.trim()) {
    showMessage(t('mcpService.serviceGroupRequiredError'), 'error')
    return false
  }
  
  // Validate MCP service endpoint
  if (publishAsMcpService.value && (!formData.endpoint || !formData.endpoint.trim())) {
    showMessage(t('mcpService.endpointRequiredError'), 'error')
    return false
  }
  
  
  // Ensure at least one service is selected
  if (!publishAsInternalToolcall.value && !publishAsHttpService.value && !publishAsMcpService.value) {
    showMessage('Please select at least one service type', 'error')
    return false
  }
  
  // Validate parameter names and descriptions
  for (let i = 0; i < formData.parameters.length; i++) {
    const param = formData.parameters[i]
    if (param.name && !param.description.trim()) {
      showMessage(`Parameter "${param.name}" description cannot be empty`, 'error')
      return false
    }
    if (param.description && !param.name.trim()) {
      showMessage(`Parameter description "${param.description}" corresponding name cannot be empty`, 'error')
      return false
    }
  }
  
  return true
}


// Handle publishing
const handlePublish = async () => {
  console.log('[PublishModal] Starting to handle publish request')
  console.log('[PublishModal] Form data:', formData)
  console.log('[PublishModal] Current tool:', currentTool.value)
  console.log('[PublishModal] Publish as MCP service:', publishAsMcpService.value)
  console.log('[PublishModal] Publish as HTTP service:', publishAsHttpService.value)
  
  if (!validateForm()) {
    console.log('[PublishModal] Form validation failed')
    return
  }

  publishing.value = true
  try {
    // 1. If no current tool data, get or create first
    if (!currentTool.value) {
      console.log('[PublishModal] No current tool data, getting or creating first')
      currentTool.value = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
    }

    // 2. Update tool information
    console.log('[PublishModal] Updating tool information')
    currentTool.value.toolName = formData.serviceName.trim()
    currentTool.value.toolDescription = formData.userRequest.trim()
    currentTool.value.serviceGroup = formData.serviceGroup.trim()
    currentTool.value.planTemplateId = props.planTemplateId // Ensure planTemplateId is set

    // Set service enabled status and corresponding endpoint
    currentTool.value.enableInternalToolcall = publishAsInternalToolcall.value
    currentTool.value.enableHttpService = publishAsHttpService.value
    currentTool.value.enableMcpService = publishAsMcpService.value
    
    // Set corresponding endpoint - now supports multiple services enabled simultaneously
    if (publishAsMcpService.value) {
      currentTool.value.mcpEndpoint = formData.endpoint.trim()
    } else {
      currentTool.value.mcpEndpoint = undefined
    }
    

    // 3. Update inputSchema
    const inputSchema = formData.parameters
      .filter(param => param.name.trim() && param.description.trim())
      .map(param => ({
        name: param.name.trim(),
        description: param.description.trim(),
        type: 'string'
      }))
    
    currentTool.value.inputSchema = JSON.stringify(inputSchema)
    console.log('[PublishModal] Updated tool information:', currentTool.value)

    // 4. Save tool
    if (currentTool.value.id) {
      console.log('[PublishModal] Updating existing tool, ID:', currentTool.value.id)
      await CoordinatorToolApiService.updateCoordinatorTool(currentTool.value.id, currentTool.value)
    } else {
      console.log('[PublishModal] Creating new tool')
      const savedTool = await CoordinatorToolApiService.createCoordinatorTool(currentTool.value)
      currentTool.value = savedTool // Update current tool, including newly generated ID
    }

    // 5. Perform corresponding publishing operations based on publish type
    const enabledServices = []
    if (publishAsInternalToolcall.value) enabledServices.push('Internal Method Call')
    if (publishAsHttpService.value) enabledServices.push('HTTP Service')
    if (publishAsMcpService.value) enabledServices.push('MCP Service')
    
    if (enabledServices.length > 0) {
      console.log('[PublishModal] Step 5: Publishing service, ID:', currentTool.value.id, 'Enabled services:', enabledServices.join(', '))
      
      // Build service URL information
      const serviceUrls = []
      if (publishAsMcpService.value && currentTool.value.mcpEndpoint) {
        const baseUrl = window.location.origin
        serviceUrls.push(`MCP: ${baseUrl}/mcp${currentTool.value.mcpEndpoint}`)
      }
      if (publishAsInternalToolcall.value) {
        serviceUrls.push(`Internal Call: ${formData.serviceName}`)
      }
      
      endpointUrl.value = serviceUrls.join('\n')
      
      console.log('[PublishModal] Service published successfully, service URLs:', endpointUrl.value)
      showMessage(t('mcpService.publishSuccess'), 'success')
      emit('published', currentTool.value)
    } else {
      // Just save, don't publish as any service
      console.log('[PublishModal] Only saving tool, not publishing as any service')
      showMessage(t('mcpService.saveSuccess'), 'success')
      emit('published', currentTool.value)
    }
  } catch (err: any) {
    console.error('[PublishModal] Failed to publish service:', err)
    showMessage(t('mcpService.publishFailed') + ': ' + err.message, 'error')
  } finally {
    publishing.value = false
  }
}

// Handle delete
const handleDelete = async () => {
  if (deleting.value) return
  
  // Confirm deletion
  if (!confirm(t('mcpService.deleteConfirmMessage'))) {
    return
  }
  
  if (!currentTool.value || !currentTool.value.id) {
    showMessage(t('mcpService.deleteFailed') + ': ' + t('mcpService.selectPlanTemplateFirst'), 'error')
    return
  }
  
  deleting.value = true
  try {
    console.log('[PublishModal] Starting to delete MCP service, ID:', currentTool.value.id)
    
    // Call delete API
    const result = await CoordinatorToolApiService.deleteCoordinatorTool(currentTool.value.id)
    
    if (result.success) {
      console.log('[PublishModal] Deleted successfully')
      showMessage(t('mcpService.deleteSuccess'), 'success')
      
      // Close modal
      showModal.value = false
      
      // Notify parent component of successful deletion
      emit('published', null)
    } else {
      throw new Error(result.message)
    }
  } catch (error: any) {
    console.error('[PublishModal] Failed to delete MCP service:', error)
    showMessage(t('mcpService.deleteFailed') + ': ' + error.message, 'error')
  } finally {
    deleting.value = false
  }
}

// Watch modal display state
const watchModal = async () => {
  if (showModal.value) {
    console.log('[PublishModal] Modal opened, starting to initialize data')
    initializeFormData()
    await loadEndpoints()
    await loadCoordinatorToolData()
  }
}

// Load coordinator tool data
const loadCoordinatorToolData = async () => {
  if (!props.planTemplateId) {
    console.log('[PublishModal] ' + t('mcpService.noPlanTemplateId'))
    return
  }
  
  try {
    console.log('[PublishModal] Starting to load coordinator tool data, planTemplateId:', props.planTemplateId)
    const tool = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
    console.log('[PublishModal] Get coordinator tool data result:', tool)
    
    // Save current tool data
    currentTool.value = tool
    
    // Only existing tools (with ID) are set as saved
    isSaved.value = !!(tool.id)
    
    // Build service URL information
    const serviceUrls = []
    if (tool.enableMcpService && tool.mcpEndpoint) {
      const baseUrl = window.location.origin
      serviceUrls.push(`MCP: ${baseUrl}/mcp${tool.mcpEndpoint}`)
    }
    if (tool.enableInternalToolcall) {
      serviceUrls.push(`Internal Call: ${tool.toolName}`)
    }
    
    endpointUrl.value = serviceUrls.join('\n')
    
    console.log('[PublishModal] Load tool data - endpointUrl:', endpointUrl.value)
    // Fill form data
    formData.serviceName = tool.toolName || ''
    formData.userRequest = tool.toolDescription || props.planDescription || ''
    formData.serviceGroup = tool.serviceGroup || ''
    
    // Set form data based on service type
    publishAsMcpService.value = tool.enableMcpService || false
    publishAsHttpService.value = tool.enableHttpService || false
    publishAsInternalToolcall.value = tool.enableInternalToolcall || false
    
    if (tool.enableMcpService) {
      formData.endpoint = tool.mcpEndpoint || ''
    }
    
    // Parse inputSchema as parameters
    try {
      if (tool.inputSchema) {
        const parameters = JSON.parse(tool.inputSchema)
        if (Array.isArray(parameters) && parameters.length > 0) {
          // Only override when inputSchema has parameters, otherwise keep parameters loaded from plan template
          formData.parameters = parameters.map(param => ({
            name: param.name || '',
            description: param.description || ''
          }))
          console.log('[PublishModal] Load parameters from inputSchema:', formData.parameters)
        } else {
          console.log('[PublishModal] inputSchema is empty, keeping existing parameters:', formData.parameters)
        }
      }
    } catch (e) {
      console.warn('[PublishModal] ' + t('mcpService.parseInputSchemaFailed') + ':', e)
      // Don't clear parameters when parsing fails, keep existing parameters
      console.log('[PublishModal] Parsing failed, keeping existing parameters:', formData.parameters)
    }
    
    console.log('[PublishModal] Form data filled:', formData)
  } catch (err: any) {
    console.error('[PublishModal] ' + t('mcpService.loadToolDataFailed') + ':', err)
    showMessage(t('mcpService.loadToolDataFailed') + ': ' + err.message, 'error')
  }
}

// Watch props changes
watch(() => props.modelValue, watchModal)



// Watch for planTemplateId changes
watch(() => props.planTemplateId, (newId, oldId) => {
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

// Initialize when component mounts
onMounted(async () => {
  if (showModal.value) {
    console.log('[PublishModal] Initialize when component mounted')
    initializeFormData()
    await loadEndpoints()
    await loadCoordinatorToolData()
    await loadParameterRequirements()
  }
})

// Expose methods for parent component
defineExpose({
  loadParameterRequirements
})
</script>

<style scoped>
/* Wide modal styles */
:deep(.wide-modal .modal-container) {
  width: 90%;
  max-width: 900px !important; /* Adjust width to 900px */
}

/* Form layout optimization - reference new Model modal styles */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px; /* Adjust to 16px, consistent with new Model modal */
  width: 100%;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 8px; /* Adjust to 8px, optimize title and input spacing */
  width: 100%;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 8px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px; /* Adjust to 8px, optimize title and input spacing */
}

.form-item label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  margin: 0; /* Remove default margin */
}

.required {
  color: #ff6b6b;
}

/* Field description styles */
.field-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
  line-height: 1.4;
  font-style: italic;
}

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s ease;
  font-family: inherit;
  box-sizing: border-box;
  min-height: 48px; /* Ensure consistent minimum height */
}

.form-item input {
  height: 48px; /* Single line input fixed height */
}

.form-item input:focus,
.form-item textarea:focus {
  border-color: #667eea;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.form-item input::placeholder,
.form-item textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.description-field {
  resize: vertical;
  min-height: 80px;
  line-height: 1.5;
  width: 100%;
}

/* Parameter Requirements Help Text */
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

/* Parameter table responsive */
.parameter-table {
  margin-bottom: 16px;
  width: 100%;
  overflow-x: auto;
}

.parameter-table table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
  min-width: 600px;
}

.parameter-table th {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  padding: 12px;
  text-align: left;
  font-size: 14px;
  white-space: nowrap;
}

.parameter-table td {
  padding: 8px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.parameter-input {
  width: 100%;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s ease;
}

.parameter-input:focus {
  border-color: #667eea;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
}

.readonly-input {
  background: rgba(255, 255, 255, 0.02) !important;
  color: rgba(255, 255, 255, 0.6) !important;
  cursor: not-allowed;
  border-color: rgba(255, 255, 255, 0.05) !important;
}


/* Delete and add button styles removed */

/* Endpoint component responsive - supports manual input */
.custom-dropdown {
  position: relative;
  width: 100%;
}

.dropdown-input {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  height: 48px; /* Ensure consistent height with other inputs */
}

.dropdown-input input {
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 14px;
  width: 100%;
  cursor: text; /* Allow text input */
  height: 100%;
  padding: 0;
}

.dropdown-input input::placeholder {
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.dropdown-input.active {
  border-color: #667eea;
  outline: none;
  background-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
  transform: translateY(-1px);
}

.dropdown-input:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background-color: rgba(255, 255, 255, 0.07);
  transform: translateY(-1px);
}

.dropdown-arrow {
  transition: transform 0.3s ease;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  flex-shrink: 0;
  margin-left: 8px;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
  max-height: 200px;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  z-index: 10;
  margin-top: 4px;
}

.dropdown-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(102, 126, 234, 0.1));
  color: #a8b3ff;
  transform: translateX(4px);
}

.dropdown-item.selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(102, 126, 234, 0.2));
  color: #a8b3ff;
  font-weight: 500;
}

.dropdown-item.custom-input {
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
  padding-left: 16px; /* Align with other items */
}

.dropdown-item.custom-input .custom-label {
  color: rgba(255, 255, 255, 0.6);
  margin-right: 4px;
}

/* Button container - reference screenshot styles */
.button-container {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 0;
  align-items: center;
  padding: 16px 0;
  min-height: 52px; /* Ensure container has fixed height, prevent jitter */
  position: relative; /* Provide reference for absolutely positioned child elements */
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.action-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
  color: #a8b3ff;
}

.action-btn.primary:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.3);
}

.action-btn.danger {
  background: rgba(234, 102, 102, 0.2);
  border-color: rgba(234, 102, 102, 0.3);
  color: #ff6b6b;
}

.action-btn.danger:hover:not(:disabled) {
  background: rgba(234, 102, 102, 0.3);
}

.action-btn.danger:disabled {
  background: rgba(234, 102, 102, 0.1);
  border-color: rgba(234, 102, 102, 0.2);
  color: rgba(255, 107, 107, 0.5);
  cursor: not-allowed;
}

/* Read-only input styles */
.readonly-input {
  background: rgba(255, 255, 255, 0.03) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  cursor: not-allowed !important;
  opacity: 0.8;
}

.readonly-input::placeholder {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* Endpoint container layout */
.endpoint-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.endpoint-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

/* Custom dropdown styles - reference screenshot three */
.custom-select {
  position: relative;
  display: inline-block;
  width: 100%; /* Adjust to 100% to fit new layout */
  flex-shrink: 0;
}

.select-input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(20, 20, 25, 0.95);
  border: 2px solid rgba(102, 126, 234, 0.6);
  border-radius: 12px;
  color: #667eea;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 600;
  outline: none;
  width: 100%;
  justify-content: space-between;
  height: 44px;
  box-sizing: border-box;
  box-shadow: 0 0 0 1px rgba(102, 126, 234, 0.3);
  padding: 0 12px;
}

.select-input-container:hover {
  border-color: rgba(102, 126, 234, 0.8);
  background-color: rgba(20, 20, 25, 0.98);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.4);
}

.select-input-container:focus-within {
  border-color: rgba(102, 126, 234, 0.9);
  outline: none;
  background-color: rgba(20, 20, 25, 0.98);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3);
}

.input-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.input-icon {
  color: #667eea;
  flex-shrink: 0;
}

.select-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  text-shadow: none;
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  height: 100%;
}

.select-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.select-arrow-btn {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.select-arrow-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.chevron {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.9;
  filter: none;
  width: 16px;
  height: 16px;
}

.select-dropdown {
  position: absolute;
  top: 100%;
  z-index: 99999;
  margin-top: 6px;
  background: rgba(15, 15, 20, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  width: 280px; /* Keep consistent with select-btn width */
  max-height: 280px;
  overflow: hidden;
  /* Ensure not occupying document flow */
  pointer-events: auto;
}

.select-dropdown.dropdown-top {
  top: auto;
  bottom: 100%;
  margin-top: 0;
  margin-bottom: 4px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.select-options {
  padding: 12px 0;
  max-height: 200px;
  overflow-y: auto;
}

.select-option {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
  padding: 12px 18px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  font-size: 14px;
}

.select-option:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.select-option.active {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
  border-left: 3px solid #667eea;
  padding-left: 15px;
  font-weight: 500;
}

.select-option.custom-input {
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
}

.select-option.custom-input .custom-label {
  color: rgba(255, 255, 255, 0.6);
  margin-right: 4px;
}

.option-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  color: #667eea;
  opacity: 0.8;
}

/* Manual input area styles */
.manual-input-section {
  padding: 14px 18px;
  border-top: 1px solid rgba(102, 126, 234, 0.2);
  background: rgba(102, 126, 234, 0.03);
}

.manual-input-container {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.manual-input {
  flex: 1;
  padding: 8px 12px;
  background: rgba(20, 20, 25, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #667eea;
  font-size: 14px;
  transition: all 0.3s ease;
}

.manual-input:focus {
  border-color: rgba(102, 126, 234, 0.8);
  outline: none;
  background: rgba(20, 20, 25, 0.9);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.manual-input::placeholder {
  color: rgba(102, 126, 234, 0.5);
}

.add-manual-btn {
  padding: 8px 12px;
  background: rgba(102, 126, 234, 0.15);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-manual-btn:hover {
  background: rgba(102, 126, 234, 0.25);
  border-color: rgba(102, 126, 234, 0.5);
}

/* Service publishing option styles */
.service-publish-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mcp-publish-option,
.http-publish-option,
.internal-toolcall-publish-option {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

/* .http-publish-option, .mcp-publish-option, .internal-toolcall-publish-option now use the same styles */

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
  cursor: pointer;
}

.checkbox-text {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.checkbox-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
  margin-left: 24px;
}

/* HTTP service options now use the same styles as MCP service, no special handling needed */


.endpoint-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
  margin-bottom: 8px;
}

/* Two-column layout styles */
.endpoint-url-row {
  display: flex;
  gap: 16px;
  width: 100%;
}

.endpoint-url-row.single-item {
  gap: 0;
}

.endpoint-url-row.single-item .endpoint-item {
  flex: 0 0 50%; /* Maintain 50% width even when displayed alone */
}

.endpoint-item {
  flex: 0 0 50%; /* Fixed width 50%, not responsive */
  min-width: 0;
}

.url-item {
  flex: 1;
  min-width: 0;
}

/* URL display styles */
.url-container {
  width: 100%;
}

.url-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 48px;
}

.url-display:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.url-text {
  flex: 1;
  font-size: 14px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  word-break: break-all;
  color: rgba(255, 255, 255, 0.9);
}

.copy-icon {
  color: rgba(255, 255, 255, 0.6);
  margin-left: 8px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.url-display:hover .copy-icon {
  color: rgba(255, 255, 255, 0.8);
}


.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 99998;
  background: transparent;
}





.slideDown-enter-active,
.slideDown-leave-active {
  transition: all 0.2s ease;
  transform-origin: top;
}

.slideDown-enter-from,
.slideDown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}



/* Old save button styles removed */







.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Required field styles */
.required {
  color: #ff4d4f;
  margin-left: 4px;
}

/* Error state styles */
.form-item input.error,
.form-item textarea.error {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.1);
}

.form-item input.error:focus,
.form-item textarea.error:focus {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
}

/* Tooltip messages */
.error-toast,
.success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  z-index: 10000; /* Ensure above modal */
  animation: slideIn 0.3s ease;
}

.error-toast {
  background: rgba(234, 102, 102, 0.9);
  border: 1px solid rgba(234, 102, 102, 0.5);
}

.success-toast {
  background: rgba(102, 234, 102, 0.9);
  border: 1px solid rgba(102, 234, 102, 0.5);
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}


</style> 
