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
  <Modal v-model="showModal" title="发布MCP服务" @confirm="handlePublish">
    <div class="modal-form">
      <!-- Endpoint配置 -->
      <div class="form-section">
        <div class="form-item">
          <label>Endpoint <span class="required">*</span></label>
          <div class="endpoint-container">
            <select
              v-model="formData.endpoint"
              class="endpoint-select"
              @change="handleEndpointChange"
            >
              <option value="">请选择或输入Endpoint</option>
              <option
                v-for="endpoint in availableEndpoints"
                :key="endpoint"
                :value="endpoint"
              >
                {{ endpoint }}
              </option>
            </select>
            <input
              type="text"
              v-model="formData.endpoint"
              placeholder="请输入Endpoint（必须是英文）"
              class="endpoint-input"
              @input="handleEndpointInput"
            />
          </div>
        </div>
      </div>

      <!-- Tool Name -->
      <div class="form-section">
        <div class="form-item">
          <label>Tool Name <span class="required">*</span></label>
          <input
            type="text"
            v-model="formData.serviceName"
            placeholder="请输入Tool Name"
            required
          />
        </div>
      </div>

      <!-- Tool Description -->
      <div class="form-section">
        <div class="form-item">
          <label>Tool Description <span class="required">*</span></label>
          <textarea
            v-model="formData.userRequest"
            placeholder="请输入Tool Description"
            class="description-field"
            rows="3"
            required
          />
        </div>
      </div>

      <!-- 参数配置 -->
      <div class="form-section">
        <h4 class="section-title">参数配置</h4>
        <div class="parameters-table">
          <table>
            <thead>
              <tr>
                <th>字段名</th>
                <th>字段描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(param, index) in formData.parameters" :key="index">
                <td>
                  <input
                    type="text"
                    v-model="param.name"
                    placeholder="字段名"
                    class="param-input"
                  />
                </td>
                <td>
                  <input
                    type="text"
                    v-model="param.description"
                    placeholder="字段描述"
                    class="param-input"
                  />
                </td>
                <td>
                  <button
                    type="button"
                    class="remove-param-btn"
                    @click="removeParameter(index)"
                  >
                    <Icon icon="carbon:trash-can" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button type="button" class="add-param-btn" @click="addParameter">
          <Icon icon="carbon:add" />
          添加参数
        </button>
      </div>
    </div>

    <template #footer>
      <button class="cancel-btn" @click="handleCancel">
        {{ t('common.cancel') }}
      </button>
      <button class="save-btn" @click="handleSave" :disabled="saving">
        <Icon icon="carbon:loading" v-if="saving" class="loading-icon" />
        {{ saving ? '保存中...' : '保存' }}
      </button>
      <button class="publish-btn" @click="handlePublish" :disabled="publishing">
        <Icon icon="carbon:loading" v-if="publishing" class="loading-icon" />
        {{ publishing ? '发布中...' : '发布' }}
      </button>
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
  'published': [tool: CoordinatorToolVO]
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
const availableEndpoints = ref<string[]>([])

// 当前工具数据，用于判断是创建还是更新
const currentTool = ref<CoordinatorToolVO | null>(null)

// Form data
const formData = reactive({
  serviceName: '',
  userRequest: '',
  endpoint: '',
  parameters: [] as Array<{ name: string; description: string }>
})

// 初始化表单数据
const initializeFormData = () => {
  formData.serviceName = ''
  formData.userRequest = props.planDescription || ''
  formData.endpoint = ''
  formData.parameters = []
  currentTool.value = null
}

// 加载可用的endpoints
const loadEndpoints = async () => {
  try {
    availableEndpoints.value = await CoordinatorToolApiService.getAllEndpoints()
  } catch (err: any) {
    console.error('加载endpoints失败:', err)
    showMessage('加载endpoints失败: ' + err.message, 'error')
  }
}

// 添加参数
const addParameter = () => {
  formData.parameters.push({ name: '', description: '' })
}

// 删除参数
const removeParameter = (index: number) => {
  formData.parameters.splice(index, 1)
}

// 处理endpoint选择
const handleEndpointChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  formData.endpoint = target.value
}

// 处理endpoint输入
const handleEndpointInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  // 只允许英文字母、数字、下划线和斜杠
  const value = target.value.replace(/[^a-zA-Z0-9_/]/g, '')
  formData.endpoint = value
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
    showMessage('请输入Tool Name', 'error')
    return false
  }
  if (!formData.userRequest.trim()) {
    showMessage('请输入Tool Description', 'error')
    return false
  }
  if (!formData.endpoint.trim()) {
    showMessage('请输入Endpoint', 'error')
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
    showMessage('MCP服务保存成功', 'success')
    
  } catch (err: any) {
    console.error('[PublishModal] 保存MCP服务失败:', err)
    showMessage('保存MCP服务失败: ' + err.message, 'error')
  } finally {
    saving.value = false
  }
}

// 处理发布
const handlePublish = async () => {
  console.log('[PublishModal] 开始处理发布请求')
  console.log('[PublishModal] 表单数据:', formData)
  console.log('[PublishModal] planTemplateId:', props.planTemplateId)
  
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
    const publishResult = await CoordinatorToolApiService.publishCoordinatorTool(currentTool.value.id!)
    console.log('[PublishModal] 发布结果:', publishResult)
    
    if (publishResult.success) {
      console.log('[PublishModal] 发布成功')
      showMessage('MCP服务发布成功', 'success')
      emit('published', currentTool.value)
      showModal.value = false
    } else {
      throw new Error(publishResult.message)
    }
  } catch (err: any) {
    console.error('[PublishModal] 发布MCP服务失败:', err)
    showMessage('发布MCP服务失败: ' + err.message, 'error')
  } finally {
    publishing.value = false
  }
}

// 处理取消
const handleCancel = () => {
  showModal.value = false
  initializeFormData()
}

// 监听modal显示状态
const watchModal = async () => {
  if (showModal.value) {
    console.log('[PublishModal] 模态框打开，开始初始化数据')
    initializeFormData()
    await loadEndpoints()
    await loadCoordinatorToolData()
  }
}

// 加载协调器工具数据
const loadCoordinatorToolData = async () => {
  if (!props.planTemplateId) {
    console.log('[PublishModal] 没有planTemplateId，跳过加载协调器工具数据')
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
        console.warn('[PublishModal] 解析inputSchema失败:', e)
        formData.parameters = []
      }
      
      console.log('[PublishModal] 表单数据已填充:', formData)
    } else {
      console.log('[PublishModal] 获取协调器工具数据失败:', result.message)
    }
  } catch (err: any) {
    console.error('[PublishModal] 加载协调器工具数据失败:', err)
  }
}

// 监听props变化
watch(() => props.modelValue, watchModal)

// 组件挂载时初始化
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
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  gap: 8px;
}

.form-item label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
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
}

.form-item input:focus,
.form-item textarea:focus {
  border-color: #667eea;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
}

.form-item input::placeholder,
.form-item textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.description-field {
  resize: vertical;
  min-height: 80px;
  line-height: 1.5;
}

.parameters-table {
  margin-bottom: 16px;
}

.parameters-table table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.parameters-table th {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.parameters-table td {
  padding: 8px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.param-input {
  width: 100%;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s ease;
}

.param-input:focus {
  border-color: #667eea;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
}

.remove-param-btn {
  background: rgba(234, 102, 102, 0.2);
  border: 1px solid rgba(234, 102, 102, 0.3);
  color: #ea6666;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-param-btn:hover {
  background: rgba(234, 102, 102, 0.3);
  border-color: rgba(234, 102, 102, 0.5);
}

.add-param-btn {
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: #a8b3ff;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.add-param-btn:hover {
  background: rgba(102, 126, 234, 0.3);
  border-color: rgba(102, 126, 234, 0.5);
}

.endpoint-container {
  display: flex;
  gap: 8px;
}

.endpoint-select {
  flex: 1;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.endpoint-select:focus {
  border-color: #667eea;
  outline: none;
}

.endpoint-select option {
  background: #1a1a1a;
  color: #fff;
}

.endpoint-input {
  flex: 2;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
}

.endpoint-input:focus {
  border-color: #667eea;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
}

.cancel-btn,
.save-btn,
.publish-btn {
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.save-btn {
  background: rgba(255, 193, 7, 0.2);
  border: 1px solid rgba(255, 193, 7, 0.3);
  color: #ffc107;
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover:not(:disabled) {
  background: rgba(255, 193, 7, 0.3);
  border-color: rgba(255, 193, 7, 0.5);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.publish-btn {
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: #a8b3ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.publish-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.3);
  border-color: rgba(102, 126, 234, 0.5);
}

.publish-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

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