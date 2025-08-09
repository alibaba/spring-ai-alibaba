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
    :endpoint-url="endpointUrl"
    @confirm="handlePublish" 
    class="wide-modal"
  >
    <div class="modal-form">
      <!-- Endpoint和MCP Streamable URL配置 -->
      <div class="form-section">
        <div class="endpoint-url-row" :class="{ 'single-item': !isPublished }">
          <!-- Endpoint配置 -->
          <div class="form-item endpoint-item">
            <label>{{ t('mcpService.endpointRequired') }}</label>
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
          <div v-if="isPublished" class="form-item url-item">
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



      <!-- Tool Name -->
      <div class="form-section">
        <div class="form-item">
          <label>{{ t('mcpService.toolNameRequired') }}</label>
          <input
            type="text"
            v-model="formData.serviceName"
            :placeholder="t('mcpService.toolNamePlaceholder')"
            required
            readonly
            class="readonly-input"
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
            class="description-field"
            rows="3"
            required
          />
        </div>
      </div>

      <!-- 参数配置 -->
      <div class="form-section">
        <div class="section-title">{{ t('mcpService.parameterConfig') }}</div>
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
                  />
                </td>
                <td>
                  <input
                    type="text"
                    v-model="param.description"
                    :placeholder="t('mcpService.parameterDescription')"
                    class="parameter-input"
                  />
                </td>
              </tr>
            </tbody>
          </table>
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
        
        <!-- 保存按钮 - 始终显示 -->
        <button class="action-btn primary" @click="handleSave" :disabled="saving">
          <Icon icon="carbon:loading" v-if="saving" class="loading-icon" />
          <Icon icon="carbon:save" v-else />
          {{ saving ? t('mcpService.saving') : t('mcpService.save') }}
        </button>
        
        <!-- 发布开关组件 - 只在已保存时显示 -->
        <div 
          v-if="isSaved && currentTool?.id" 
          class="publish-toggle-container"
        >
          <div class="publish-toggle" @click="handlePublishToggle">
            <div class="toggle-track" :class="{ 'toggle-on': isPublished, 'toggle-off': !isPublished }">
              <div class="toggle-thumb" :class="{ 'thumb-on': isPublished, 'thumb-off': !isPublished }"></div>
              <span class="toggle-label" :class="{ 'label-on': isPublished, 'label-off': !isPublished }">
                {{ isPublished ? t('mcpService.published') : t('mcpService.unpublished') }}
              </span>
            </div>
          </div>
        </div>
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
const saving = ref(false)
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

// 发布开关相关状态
const isSaved = ref(false)
const isPublished = ref(false)

// 计算完整的URL功能已移除

// Form data
const formData = reactive({
  serviceName: '',
  userRequest: '',
  endpoint: '',
  parameters: [] as Array<{ name: string; description: string }>
})

// 计算模态框标题
const modalTitle = computed(() => {
  const isUpdate = currentTool.value && currentTool.value.id
  return isUpdate ? t('mcpService.updateService') : t('mcpService.createService')
})

// 状态图标相关计算属性已移除

// 初始化表单数据
const initializeFormData = () => {
  formData.serviceName = ''
  formData.userRequest = props.planDescription || ''
  formData.endpoint = ''
  formData.parameters = []
  currentTool.value = null
  publishStatus.value = ''
  endpointUrl.value = ''
  isSaved.value = false
  isPublished.value = false
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

// 参数操作方法已移除

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

// 复制到剪贴板功能已移除

// 复制完整URL功能已移除

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
  if (!formData.serviceName.trim()) {
    showMessage(t('mcpService.toolNameRequiredError'), 'error')
    return false
  }
  if (!formData.userRequest.trim()) {
    showMessage(t('mcpService.toolDescriptionRequiredError'), 'error')
    return false
  }
  if (!formData.endpoint.trim()) {
    showMessage(t('mcpService.endpointRequiredError'), 'error')
    return false
  }
  return true
}

// 处理保存
const handleSave = async () => {
  console.log('[PublishModal] 开始处理保存请求')
  console.log('[PublishModal] 表单数据:', formData)
  console.log('[PublishModal] 当前工具:', currentTool.value)
  
  if (!validateForm()) {
    console.log('[PublishModal] 表单验证失败')
    return
  }

  saving.value = true
  try {
    // 1. 如果没有当前工具数据，先获取或创建
    if (!currentTool.value) {
      console.log('[PublishModal] 没有当前工具数据，先获取或创建')
      const result = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
      
      if (!result.success) {
        throw new Error(result.message)
      }

      if (Array.isArray(result.data)) {
        currentTool.value = result.data[0]
      } else {
        currentTool.value = result.data as CoordinatorToolVO
      }
    }

    // 2. 更新工具信息
    console.log('[PublishModal] 更新工具信息')
    currentTool.value.toolName = formData.serviceName.trim()
    currentTool.value.toolDescription = formData.userRequest.trim()
    currentTool.value.endpoint = formData.endpoint.trim()
    currentTool.value.planTemplateId = props.planTemplateId // 确保planTemplateId被设置

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
    let savedTool: CoordinatorToolVO
    if (currentTool.value.id) {
      console.log('[PublishModal] 更新现有工具，ID:', currentTool.value.id)
      savedTool = await CoordinatorToolApiService.updateCoordinatorTool(currentTool.value.id, currentTool.value)
    } else {
      console.log('[PublishModal] 创建新工具')
      savedTool = await CoordinatorToolApiService.createCoordinatorTool(currentTool.value)
      currentTool.value = savedTool // 更新当前工具，包含新生成的ID
    }

    console.log('[PublishModal] 保存成功:', savedTool)
    showMessage(t('mcpService.saveSuccess'), 'success')
    isSaved.value = true
  } catch (err: any) {
    console.error('[PublishModal] 保存MCP服务失败:', err)
    showMessage(t('mcpService.saveFailed') + ': ' + err.message, 'error')
  } finally {
    saving.value = false
  }
}

// 处理发布
const handlePublish = async () => {
  console.log('[PublishModal] 开始处理发布请求')
  console.log('[PublishModal] 表单数据:', formData)
  console.log('[PublishModal] 当前工具:', currentTool.value)
  
  if (!validateForm()) {
    console.log('[PublishModal] 表单验证失败')
    return
  }

  publishing.value = true
  try {
    // 1. 如果没有当前工具数据，先获取或创建
    if (!currentTool.value) {
      console.log('[PublishModal] 没有当前工具数据，先获取或创建')
      const result = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
      
      if (!result.success) {
        throw new Error(result.message)
      }

      if (Array.isArray(result.data)) {
        currentTool.value = result.data[0]
      } else {
        currentTool.value = result.data as CoordinatorToolVO
      }
    }

    // 2. 更新工具信息
    console.log('[PublishModal] 更新工具信息')
    currentTool.value.toolName = formData.serviceName.trim()
    currentTool.value.toolDescription = formData.userRequest.trim()
    currentTool.value.endpoint = formData.endpoint.trim()
    currentTool.value.planTemplateId = props.planTemplateId // 确保planTemplateId被设置

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

    // 5. 发布工具
    console.log('[PublishModal] 步骤5: 发布工具，ID:', currentTool.value.id)
    const publishResult = await CoordinatorToolApiService.publishCoordinatorTool(currentTool.value.id!) as any
    console.log('[PublishModal] 发布结果:', publishResult)
    
    if (publishResult.success) {
      console.log('[PublishModal] 发布成功')
      // 设置发布状态和URL - 从响应中获取正确的endpointUrl
      publishStatus.value = publishResult.publishStatus || 'PUBLISHED'
      
      // 优先使用响应中的endpointUrl，如果没有则构建完整URL
      if (publishResult.endpointUrl) {
        endpointUrl.value = publishResult.endpointUrl
      } else if (currentTool.value.endpoint) {
        // 构建完整的URL
        const baseUrl = window.location.origin
        endpointUrl.value = `${baseUrl}/mcp${currentTool.value.endpoint}`
      } else {
        endpointUrl.value = ''
      }
      
      console.log('[PublishModal] 设置状态 - publishStatus:', publishStatus.value, 'endpointUrl:', endpointUrl.value)
      showMessage(t('mcpService.publishSuccess'), 'success')
      emit('published', currentTool.value)
      // 不立即关闭模态框，让用户可以看到URL
      // showModal.value = false
    } else {
      throw new Error(publishResult.message)
    }
  } catch (err: any) {
    console.error('[PublishModal] 发布MCP服务失败:', err)
    showMessage(t('mcpService.publishFailed') + ': ' + err.message, 'error')
  } finally {
    publishing.value = false
  }
}

// Handle cancel functionality removed

// Handle publish toggle switch
const handlePublishToggle = async () => {
  if (publishing.value) return
  
  publishing.value = true
  try {
    if (isPublished.value) {
      // 取消发布 - 暂时使用更新状态的方式
      console.log('[PublishModal] 取消发布MCP服务')
      if (currentTool.value) {
        currentTool.value.publishStatus = 'UNPUBLISHED'
        await CoordinatorToolApiService.updateCoordinatorTool(currentTool.value.id!, currentTool.value)
        isPublished.value = false
        endpointUrl.value = '' // 清空endpointUrl
        showMessage(t('mcpService.unpublishSuccess'), 'success')
      }
    } else {
      // 发布
      console.log('[PublishModal] 发布MCP服务')
      await handlePublish()
      isPublished.value = true
      // handlePublish已经处理了endpointUrl的设置
    }
  } catch (error: any) {
    console.error('[PublishModal] 发布开关操作失败:', error)
    showMessage(t('mcpService.unpublishFailed') + ': ' + error.message, 'error')
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
    const result = await CoordinatorToolApiService.getOrNewCoordinatorToolsByTemplate(props.planTemplateId)
    console.log('[PublishModal] 获取协调器工具数据结果:', result)
    
    if (result.success && result.data) {
      let tool: CoordinatorToolVO
      
      if (Array.isArray(result.data)) {
        // 如果已存在，使用第一个
        tool = result.data[0]
        console.log('[PublishModal] 使用已存在的工具:', tool)
      } else {
        // 如果不存在，创建新的
        tool = result.data as CoordinatorToolVO
        console.log('[PublishModal] 使用新创建的工具:', tool)
      }
      
      // 保存当前工具数据
      currentTool.value = tool
      
      // 设置发布状态和URL
      publishStatus.value = tool.publishStatus || ''
      isPublished.value = tool.publishStatus === 'PUBLISHED'
      // 只有已存在的工具（有ID）才设置为已保存
      isSaved.value = !!(tool.id)
      
      // 使用后端返回的endpointUrl，如果没有则构建
      if (tool.publishStatus === 'PUBLISHED') {
        // 检查是否有后端返回的endpointUrl
        if ((result as any).endpointUrl) {
          endpointUrl.value = (result as any).endpointUrl
        } else if (tool.endpoint) {
          // 如果没有后端返回的endpointUrl，则构建
          const baseUrl = window.location.origin
          endpointUrl.value = `${baseUrl}/mcp${tool.endpoint}`
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
      formData.endpoint = tool.endpoint || ''
      
      // 解析inputSchema为参数
      try {
        if (tool.inputSchema) {
          const parameters = JSON.parse(tool.inputSchema)
          if (Array.isArray(parameters)) {
            formData.parameters = parameters.map(param => ({
              name: param.name || '',
              description: param.description || ''
            }))
          }
        }
      } catch (e) {
        console.warn('[PublishModal] ' + t('mcpService.parseInputSchemaFailed') + ':', e)
        formData.parameters = []
      }
      
      console.log('[PublishModal] 表单数据已填充:', formData)
    } else {
      console.log('[PublishModal] 获取协调器工具数据失败:', result.message)
    }
  } catch (err: any) {
    console.error('[PublishModal] ' + t('mcpService.loadToolDataFailed') + ':', err)
  }
}

// Watch props changes
watch(() => props.modelValue, watchModal)

// Initialize when component mounts
onMounted(async () => {
  if (showModal.value) {
    console.log('[PublishModal] 组件挂载时初始化')
    initializeFormData()
    await loadEndpoints()
    await loadCoordinatorToolData()
  }
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

/* 发布开关样式 */
.publish-toggle-container {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  min-width: 156px; /* 保持最小宽度，防止布局抖动 */
}

.publish-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  width: 156px; /* 固定宽度，防止抖动 */
  flex-shrink: 0; /* 防止收缩 */
}

.toggle-track {
  position: relative;
  width: 104px; /* 固定宽度，防止抖动 */
  height: 36px; /* 与保存按钮一致 */
  border-radius: 18px;
  transition: all 0.3s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between; /* 改为两端对齐 */
  padding: 0 12px; /* 添加左右内边距 */
  overflow: hidden; /* 防止文字溢出 */
}

.toggle-track.toggle-on {
  background: #4caf50;
  box-shadow: 0 0 0 1px rgba(76, 175, 80, 0.3);
}

.toggle-track.toggle-off {
  background: rgba(120, 120, 120, 0.6);
  box-shadow: 0 0 0 1px rgba(120, 120, 120, 0.3);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  width: 32px; /* 调整大小以适应36px高度 */
  height: 32px; /* 调整大小以适应36px高度 */
  border-radius: 16px; /* 与轨道圆角保持一致 */
  background: #fff;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 2; /* 确保在文字之上 */
}

.toggle-thumb.thumb-on {
  right: 2px; /* 开启状态：滑块在右侧 */
}

.toggle-thumb.thumb-off {
  left: 2px; /* 关闭状态：滑块在左侧 */
}

.toggle-label {
  font-size: 12px; /* 调整字体大小以适应较小的高度 */
  font-weight: 500;
  transition: all 0.3s ease;
  color: #fff;
  z-index: 1;
  white-space: nowrap;
  flex: 1; /* 自适应剩余空间 */
  text-align: center; /* 文字居中 */
  padding: 0 8px; /* 左右内边距，避免与滑块重叠 */
  line-height: 32px; /* 垂直居中 */
}

.toggle-label.label-on {
  color: #fff;
  text-align: left; /* 开启状态：文字左对齐 */
  padding-left: 8px; /* 左边距 */
  padding-right: 40px; /* 右边距，为滑块留出空间 */
}

.toggle-label.label-off {
  color: #fff;
  text-align: right; /* 关闭状态：文字右对齐 */
  padding-left: 40px; /* 左边距，为滑块留出空间 */
  padding-right: 8px; /* 右边距 */
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
  z-index: 1000;
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