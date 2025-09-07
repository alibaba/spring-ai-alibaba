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
        </div>
      </div>

      <!-- 参数配置 -->
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

      <!-- 服务发布选项 -->
      <div class="form-section">
        <div class="service-publish-options">
          <!-- Internal Toolcall发布选项 -->
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
          
          <!-- HTTP POST服务发布选项 -->
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
          
          <!-- MCP服务发布选项 -->
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
            
            <!-- Endpoint配置 - 仅在MCP服务发布选项选中时显示 -->
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
                        <button class="select-arrow-btn" @click="toggleDropdown" title="展开选项">
                          <Icon
                            :icon="isDropdownOpen ? 'carbon:chevron-up' : 'carbon:chevron-down'"
                            width="14"
                            class="chevron"
                          />
                        </button>
                      </div>

                      <!-- 下拉选项 - 使用绝对定位，不占用文档流 -->
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
                        <!-- 手工输入区域 -->
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

              <!-- MCP Streamable URL配置 - 仅在已发布时显示 -->
              <div v-if="publishStatus === 'PUBLISHED'" class="form-item url-item">
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
        <!-- 删除按钮 - 只在已保存时显示 -->
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
        
        <!-- 发布为服务按钮 - 始终显示 -->
        <button class="action-btn primary" @click="handlePublish" :disabled="publishing">
          <Icon icon="carbon:loading" v-if="publishing" class="loading-icon" />
          <Icon icon="carbon:cloud-upload" v-else />
          {{ publishing ? t('mcpService.publishing') : t('mcpService.publishAsService') }}
        </button>
        
      </div>
    </template>
  </Modal>

  <!-- 错误提示 -->
  <div v-if="error" class="error-toast" @click="error = ''">
    <Icon icon="carbon:error" />
    {{ error }}
  </div>

  <!-- 成功提示 -->
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

// 新增的响应式数据
const publishStatus = ref('')
const endpointUrl = ref('')
const isDropdownOpen = ref(false)
const dropdownPosition = ref('bottom')
const manualEndpointInput = ref('')

// 当前工具数据，用于判断是创建还是更新
const currentTool = ref<CoordinatorToolVO | null>(null)

// 发布状态
const isSaved = ref(false)

// 服务发布选项
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

// 计算模态框标题
const modalTitle = computed(() => {
  const isUpdate = currentTool.value && currentTool.value.id
  return isUpdate ? t('mcpService.updateService') : t('mcpService.createService')
})

// 初始化表单数据
const initializeFormData = () => {
  formData.serviceName = '' // 当没有实体时显示为空
  formData.userRequest = props.planDescription || ''
  formData.endpoint = ''
  formData.serviceGroup = ''
  // 只有在没有从计划模板加载参数时才重置参数
  if (!parameterRequirements.value.hasParameters) {
    formData.parameters = []
  }
  currentTool.value = null
  publishStatus.value = ''
  endpointUrl.value = ''
  isSaved.value = false
}

// 加载可用的endpoints
const loadEndpoints = async () => {
  try {
    availableEndpoints.value = await CoordinatorToolApiService.getAllEndpoints()
  } catch (err: any) {
    console.error('加载endpoints失败:', err)
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
    parameterRequirements.value = {
      parameters: [],
      hasParameters: false,
      requirements: ''
    }
  } finally {
    isLoadingParameters.value = false
  }
}

// 下拉框相关方法
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
  // 延迟处理，确保点击事件能够正常触发
  setTimeout(() => {
    if (manualEndpointInput.value.trim()) {
      addManualEndpoint()
    }
  }, 200)
}

// 处理下拉框输入
const handleEndpointInput = () => {
  // 当用户在输入框中输入时，如果输入的是一个已有的endpoint，则直接选中
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}

// 处理下拉框回车
const handleEndpointEnter = () => {
  // 当用户在输入框中按回车时，如果输入的是一个已有的endpoint，则直接选中
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}

// 处理下拉框失焦
const handleEndpointBlur = () => {
  // 当输入框失去焦点时，如果输入的是一个已有的endpoint，则直接选中
  if (availableEndpoints.value.includes(formData.endpoint)) {
    selectEndpoint(formData.endpoint)
  }
}



// 复制endpointUrl
const copyEndpointUrl = async () => {
  if (!endpointUrl.value) return
  
  try {
    await navigator.clipboard.writeText(endpointUrl.value)
    showMessage(t('common.copy') + ' ' + t('common.success'), 'success')
  } catch (err) {
    console.error('复制失败:', err)
    showMessage(t('common.copy') + ' ' + t('common.error'), 'error')
  }
}

// 显示消息
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

// 验证表单
const validateForm = (): boolean => {
  // 验证工具名称
  if (!formData.serviceName || !formData.serviceName.trim()) {
    showMessage(t('mcpService.toolNameRequiredError'), 'error')
    return false
  }
  
  // 验证工具描述
  if (!formData.userRequest || !formData.userRequest.trim()) {
    showMessage(t('mcpService.toolDescriptionRequiredError'), 'error')
    return false
  }
  
  // 验证服务分组
  if (!formData.serviceGroup || !formData.serviceGroup.trim()) {
    showMessage(t('mcpService.serviceGroupRequiredError'), 'error')
    return false
  }
  
  // 验证MCP服务端点
  if (publishAsMcpService.value && (!formData.endpoint || !formData.endpoint.trim())) {
    showMessage(t('mcpService.endpointRequiredError'), 'error')
    return false
  }
  
  
  // 确保至少选择一种服务
  if (!publishAsInternalToolcall.value && !publishAsHttpService.value && !publishAsMcpService.value) {
    showMessage('请至少选择一种服务类型', 'error')
    return false
  }
  
  // 验证参数名称和描述
  for (let i = 0; i < formData.parameters.length; i++) {
    const param = formData.parameters[i]
    if (param.name && !param.description.trim()) {
      showMessage(`参数"${param.name}"的描述不能为空`, 'error')
      return false
    }
    if (param.description && !param.name.trim()) {
      showMessage(`参数描述"${param.description}"对应的名称不能为空`, 'error')
      return false
    }
  }
  
  return true
}


// 处理发布
const handlePublish = async () => {
  console.log('[PublishModal] 开始处理发布请求')
  console.log('[PublishModal] 表单数据:', formData)
  console.log('[PublishModal] 当前工具:', currentTool.value)
  console.log('[PublishModal] 发布为MCP服务:', publishAsMcpService.value)
  console.log('[PublishModal] 发布为HTTP服务:', publishAsHttpService.value)
  
  if (!validateForm()) {
    console.log('[PublishModal] 表单验证失败')
    return
  }

  publishing.value = true
  try {
    // 1. 如果没有当前工具数据，先获取或创建
    if (!currentTool.value) {
      console.log('[PublishModal] 没有当前工具数据，先获取或创建')
      currentTool.value = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
    }

    // 2. 更新工具信息
    console.log('[PublishModal] 更新工具信息')
    currentTool.value.toolName = formData.serviceName.trim()
    currentTool.value.toolDescription = formData.userRequest.trim()
    currentTool.value.serviceGroup = formData.serviceGroup.trim()
    currentTool.value.planTemplateId = props.planTemplateId // 确保planTemplateId被设置

    // 设置服务启用状态和对应的endpoint
    currentTool.value.enableInternalToolcall = publishAsInternalToolcall.value
    currentTool.value.enableHttpService = publishAsHttpService.value
    currentTool.value.enableMcpService = publishAsMcpService.value
    
    // 设置对应的endpoint - 现在支持多种服务同时启用
    if (publishAsMcpService.value) {
      currentTool.value.mcpEndpoint = formData.endpoint.trim()
    } else {
      currentTool.value.mcpEndpoint = undefined
    }
    

    // 3. 更新inputSchema
    const inputSchema = formData.parameters
      .filter(param => param.name.trim() && param.description.trim())
      .map(param => ({
        name: param.name.trim(),
        description: param.description.trim(),
        type: 'string'
      }))
    
    currentTool.value.inputSchema = JSON.stringify(inputSchema)
    console.log('[PublishModal] 更新后的工具信息:', currentTool.value)

    // 4. 保存工具
    if (currentTool.value.id) {
      console.log('[PublishModal] 更新现有工具，ID:', currentTool.value.id)
      await CoordinatorToolApiService.updateCoordinatorTool(currentTool.value.id, currentTool.value)
    } else {
      console.log('[PublishModal] 创建新工具')
      const savedTool = await CoordinatorToolApiService.createCoordinatorTool(currentTool.value)
      currentTool.value = savedTool // 更新当前工具，包含新生成的ID
    }

    // 5. 根据发布类型进行相应的发布操作
    const enabledServices = []
    if (publishAsInternalToolcall.value) enabledServices.push('内部方法调用')
    if (publishAsHttpService.value) enabledServices.push('HTTP服务')
    if (publishAsMcpService.value) enabledServices.push('MCP服务')
    
    if (enabledServices.length > 0) {
      console.log('[PublishModal] 步骤5: 发布服务，ID:', currentTool.value.id, '启用服务:', enabledServices.join(', '))
      publishStatus.value = 'PUBLISHED'
      
      // 构建服务URL信息
      const serviceUrls = []
      if (publishAsMcpService.value && currentTool.value.mcpEndpoint) {
        const baseUrl = window.location.origin
        serviceUrls.push(`MCP: ${baseUrl}/mcp${currentTool.value.mcpEndpoint}`)
      }
      if (publishAsInternalToolcall.value) {
        serviceUrls.push(`内部调用: ${formData.serviceName}`)
      }
      
      endpointUrl.value = serviceUrls.join('\n')
      
      console.log('[PublishModal] 服务发布成功，服务URLs:', endpointUrl.value)
      showMessage(t('mcpService.publishSuccess'), 'success')
      emit('published', currentTool.value)
    } else {
      // 只是保存，不发布为任何服务
      console.log('[PublishModal] 仅保存工具，不发布为任何服务')
      showMessage(t('mcpService.saveSuccess'), 'success')
      emit('published', currentTool.value)
    }
  } catch (err: any) {
    console.error('[PublishModal] 发布服务失败:', err)
    showMessage(t('mcpService.publishFailed') + ': ' + err.message, 'error')
  } finally {
    publishing.value = false
  }
}

// Handle delete
const handleDelete = async () => {
  if (deleting.value) return
  
  // 确认删除
  if (!confirm(t('mcpService.deleteConfirmMessage'))) {
    return
  }
  
  if (!currentTool.value || !currentTool.value.id) {
    showMessage(t('mcpService.deleteFailed') + ': ' + t('mcpService.selectPlanTemplateFirst'), 'error')
    return
  }
  
  deleting.value = true
  try {
    console.log('[PublishModal] 开始删除MCP服务，ID:', currentTool.value.id)
    
    // 调用删除API
    const result = await CoordinatorToolApiService.deleteCoordinatorTool(currentTool.value.id)
    
    if (result.success) {
      console.log('[PublishModal] 删除成功')
      showMessage(t('mcpService.deleteSuccess'), 'success')
      
      // 关闭模态框
      showModal.value = false
      
      // 通知父组件删除成功
      emit('published', null)
    } else {
      throw new Error(result.message)
    }
  } catch (error: any) {
    console.error('[PublishModal] 删除MCP服务失败:', error)
    showMessage(t('mcpService.deleteFailed') + ': ' + error.message, 'error')
  } finally {
    deleting.value = false
  }
}

// Watch modal display state
const watchModal = async () => {
  if (showModal.value) {
    console.log('[PublishModal] 模态框打开，开始初始化数据')
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
    console.log('[PublishModal] 开始加载协调器工具数据，planTemplateId:', props.planTemplateId)
    const tool = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
    console.log('[PublishModal] 获取协调器工具数据结果:', tool)
    
    // 保存当前工具数据
    currentTool.value = tool
    
    // 设置发布状态和URL
    publishStatus.value = tool.publishStatus || ''
    // 只有已存在的工具（有ID）才设置为已保存
    isSaved.value = !!(tool.id)
    
    // 使用后端返回的endpointUrl，如果没有则构建
    if (tool.publishStatus === 'PUBLISHED') {
      // 检查是否有后端返回的endpointUrl
      if ((tool as any).endpointUrl) {
        endpointUrl.value = (tool as any).endpointUrl
      } else if (tool.mcpEndpoint) {
        // 如果没有后端返回的endpointUrl，则构建
        const baseUrl = window.location.origin
        endpointUrl.value = `${baseUrl}/mcp${tool.mcpEndpoint}`
      } else {
        endpointUrl.value = ''
      }
    } else {
      endpointUrl.value = ''
    }
    
    console.log('[PublishModal] 加载工具数据 - publishStatus:', publishStatus.value, 'endpointUrl:', endpointUrl.value)
    // 填充表单数据
    formData.serviceName = tool.toolName || ''
    formData.userRequest = tool.toolDescription || props.planDescription || ''
    formData.serviceGroup = tool.serviceGroup || ''
    
    // 根据服务类型设置表单数据
    publishAsMcpService.value = tool.enableMcpService || false
    publishAsHttpService.value = tool.enableHttpService || false
    publishAsInternalToolcall.value = tool.enableInternalToolcall || false
    
    if (tool.enableMcpService) {
      formData.endpoint = tool.mcpEndpoint || ''
    }
    
    // 解析inputSchema为参数
    try {
      if (tool.inputSchema) {
        const parameters = JSON.parse(tool.inputSchema)
        if (Array.isArray(parameters) && parameters.length > 0) {
          // 只有当inputSchema中有参数时才覆盖，否则保持从计划模板加载的参数
          formData.parameters = parameters.map(param => ({
            name: param.name || '',
            description: param.description || ''
          }))
          console.log('[PublishModal] 从inputSchema加载参数:', formData.parameters)
        } else {
          console.log('[PublishModal] inputSchema为空，保持现有参数:', formData.parameters)
        }
      }
    } catch (e) {
      console.warn('[PublishModal] ' + t('mcpService.parseInputSchemaFailed') + ':', e)
      // 解析失败时不清空参数，保持现有参数
      console.log('[PublishModal] 解析失败，保持现有参数:', formData.parameters)
    }
    
    console.log('[PublishModal] 表单数据已填充:', formData)
  } catch (err: any) {
    console.error('[PublishModal] ' + t('mcpService.loadToolDataFailed') + ':', err)
    showMessage(t('mcpService.loadToolDataFailed') + ': ' + err.message, 'error')
  }
}

// Watch props changes
watch(() => props.modelValue, watchModal)



// Watch for planTemplateId changes
watch(() => props.planTemplateId, () => {
  loadParameterRequirements()
})

// Initialize when component mounts
onMounted(async () => {
  if (showModal.value) {
    console.log('[PublishModal] 组件挂载时初始化')
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
/* 宽模态框样式 */
:deep(.wide-modal .modal-container) {
  width: 90%;
  max-width: 900px !important; /* 调整宽度为900px */
}

/* 表单布局优化 - 参考新建Model模态框的样式 */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px; /* 调整为16px，与新建Model模态框一致 */
  width: 100%;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 8px; /* 调整为8px，优化标题与输入框间距 */
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
  gap: 8px; /* 调整为8px，优化标题与输入框间距 */
}

.form-item label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  margin: 0; /* 移除默认margin */
}

.required {
  color: #ff6b6b;
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
  min-height: 48px; /* 确保最小高度一致 */
}

.form-item input {
  height: 48px; /* 单行输入框固定高度 */
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

/* 参数表格自适应 */
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


/* 删除按钮和添加按钮样式已移除 */

/* Endpoint组件自适应 - 支持手动输入 */
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
  height: 48px; /* 确保与其他输入框高度一致 */
}

.dropdown-input input {
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 14px;
  width: 100%;
  cursor: text; /* 允许文本输入 */
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

/* 按钮容器 - 参考截图的样式 */
.button-container {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 0;
  align-items: center;
  padding: 16px 0;
  min-height: 52px; /* 确保容器有固定高度，防止抖动 */
  position: relative; /* 为绝对定位的子元素提供参考 */
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

/* 只读输入框样式 */
.readonly-input {
  background: rgba(255, 255, 255, 0.03) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  cursor: not-allowed !important;
  opacity: 0.8;
}

.readonly-input::placeholder {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* Endpoint容器布局 */
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

/* 自定义下拉框样式 - 参考截图三 */
.custom-select {
  position: relative;
  display: inline-block;
  width: 100%; /* 调整为100%以适应新的布局 */
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
  width: 280px; /* 与select-btn宽度保持一致 */
  max-height: 280px;
  overflow: hidden;
  /* 确保不占用文档流 */
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

/* 手工输入区域样式 */
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

/* 服务发布选项样式 */
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

/* .http-publish-option, .mcp-publish-option, .internal-toolcall-publish-option 现在使用相同的样式 */

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

/* HTTP服务选项现在与MCP服务使用相同的样式，无需特殊处理 */


.endpoint-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
  margin-bottom: 8px;
}

/* 两列布局样式 */
.endpoint-url-row {
  display: flex;
  gap: 16px;
  width: 100%;
}

.endpoint-url-row.single-item {
  gap: 0;
}

.endpoint-url-row.single-item .endpoint-item {
  flex: 0 0 50%; /* 即使单独显示也保持50%宽度 */
}

.endpoint-item {
  flex: 0 0 50%; /* 固定宽度为50%，不自适应 */
  min-width: 0;
}

.url-item {
  flex: 1;
  min-width: 0;
}

/* URL显示样式 */
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



/* 旧的保存按钮样式已移除 */







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

/* 必填字段样式 */
.required {
  color: #ff4d4f;
  margin-left: 4px;
}

/* 错误状态样式 */
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

/* 提示消息 */
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
  z-index: 10000; /* 确保在模态框之上 */
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