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
  <div class="plan-template-config">
    <!-- Header -->
    <div class="config-header">
      <div class="config-title">
        <Icon icon="carbon:settings" width="24" />
        <span>计划模板配置</span>
      </div>
      <div class="template-info" v-if="currentTemplate">
        <span class="template-name">{{ currentTemplate.title || '未命名计划' }}</span>
        <span class="template-id">ID: {{ currentTemplate.id }}</span>
      </div>
      <button class="close-btn" @click="handleClose" title="关闭配置">
        <Icon icon="carbon:close" width="20" />
      </button>
    </div>

    <!-- Main Content -->
    <div class="config-content">
      <!-- Section 1: JSON Plan Template Editor -->
      <div class="config-section json-editor-section">
        <div class="section-header">
          <Icon icon="carbon:code" width="20" />
          <span>JSON 计划模板</span>
          <div class="section-actions">
            <button 
              class="btn btn-secondary"
              @click="handleRollback"
              :disabled="!canRollback"
              title="回滚到上一个版本"
            >
              <Icon icon="carbon:undo" width="16" />
              回滚
            </button>
            <button 
              class="btn btn-secondary"
              @click="handleRestore"
              :disabled="!canRestore"
              title="恢复到下一个版本"
            >
              <Icon icon="carbon:redo" width="16" />
              恢复
            </button>
            <button 
              class="btn btn-primary"
              @click="handleSaveTemplate"
              :disabled="isGenerating || isExecuting || !currentTemplate"
            >
              <Icon icon="carbon:save" width="16" />
              保存模板
            </button>
          </div>
        </div>
        <div class="json-editor-container">
          <textarea
            ref="jsonEditor"
            v-model="jsonContent"
            class="json-editor"
            placeholder="在此输入 JSON 计划模板内容..."
            @input="onJsonContentChange"
          ></textarea>
        </div>
      </div>

      <!-- Section 2: Plan Generator -->
      <div class="config-section generator-section">
        <div class="section-header">
          <Icon icon="carbon:generate" width="20" />
          <span>计划生成器</span>
        </div>
        <div class="generator-container">
          <div class="input-group">
            <label for="prompt-input">生成提示</label>
            <textarea
              id="prompt-input"
              v-model="generatorPrompt"
              class="prompt-input"
              placeholder="描述您想要生成的计划..."
              rows="3"
            ></textarea>
          </div>
          <div class="generator-actions">
            <button 
              class="btn btn-primary btn-generate"
              @click="handleGeneratePlan"
              :disabled="isGenerating || !generatorPrompt.trim()"
            >
              <Icon 
                :icon="isGenerating ? 'carbon:circle-dash' : 'carbon:generate'" 
                width="16" 
                :class="{ spinning: isGenerating }"
              />
              {{ isGenerating ? '生成中...' : '生成计划' }}
            </button>
            <button 
              class="btn btn-secondary"
              @click="handleUpdatePlan"
              :disabled="isGenerating || !generatorPrompt.trim() || !currentTemplate"
            >
              <Icon icon="carbon:edit" width="16" />
              更新计划
            </button>
          </div>
        </div>
      </div>

      <!-- Section 3: Execution Controller -->
      <div class="config-section execution-section">
        <div class="section-header">
          <Icon icon="carbon:play" width="20" />
          <span>计划执行</span>
        </div>
        <div class="execution-container">
          <div class="input-group">
            <label for="params-input">执行参数</label>
            <textarea
              id="params-input"
              v-model="executionParams"
              class="params-input"
              placeholder="输入执行参数（可选）..."
              rows="2"
            ></textarea>
            <button 
              class="btn btn-link clear-params-btn"
              @click="clearExecutionParams"
              title="清空参数"
            >
              <Icon icon="carbon:close" width="14" />
            </button>
          </div>
          <div class="api-info">
            <label>API 调用地址</label>
            <div class="api-url">{{ apiUrl }}</div>
          </div>
          <div class="execution-actions">
            <button 
              class="btn btn-success btn-execute"
              @click="handleExecutePlan"
              :disabled="isExecuting || isGenerating || !currentTemplate"
            >
              <Icon 
                :icon="isExecuting ? 'carbon:circle-dash' : 'carbon:play'" 
                width="16" 
                :class="{ spinning: isExecuting }"
              />
              {{ isExecuting ? '执行中...' : '执行计划' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Icon } from '@iconify/vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import type { PlanTemplate } from '@/types/plan-template'

// Props
interface Props {
  template?: PlanTemplate | null
}

const props = withDefaults(defineProps<Props>(), {
  template: null
})

// Emits
const emit = defineEmits<{
  planExecuted: [{ planId: string, query: string }]
  templateSaved: [{ templateId: string, content: string }]
  jsonContentChanged: [{ content: string }]
  configClosed: []
}>()

// Reactive state
const currentTemplate = ref<PlanTemplate | null>(props.template)
const jsonContent = ref('')
const generatorPrompt = ref('')
const executionParams = ref('')
const isGenerating = ref(false)
const isExecuting = ref(false)

// Version control
const planVersions = ref<string[]>([])
const currentVersionIndex = ref(-1)

// Refs
const jsonEditor = ref<HTMLTextAreaElement>()

// Computed
const canRollback = computed(() => {
  return planVersions.value.length > 1 && currentVersionIndex.value > 0
})

const canRestore = computed(() => {
  return planVersions.value.length > 1 && currentVersionIndex.value < planVersions.value.length - 1
})

const apiUrl = computed(() => {
  if (!currentTemplate.value) return ''
  const baseUrl = `/api/plan-template/executePlanByTemplateId`
  const params = executionParams.value.trim()
  return params ? `${baseUrl}?${encodeURIComponent(params)}` : baseUrl
})

// Watch for template changes
watch(() => props.template, async (newTemplate) => {
  if (newTemplate) {
    currentTemplate.value = newTemplate
    await loadTemplateData(newTemplate)
  }
}, { immediate: true })

// Methods
const loadTemplateData = async (template: PlanTemplate) => {
  try {
    // Load versions
    const versionsResponse = await PlanActApiService.getPlanVersions(template.id)
    planVersions.value = versionsResponse.versions || []
    
    if (planVersions.value.length > 0) {
      const latestContent = planVersions.value[planVersions.value.length - 1]
      jsonContent.value = latestContent
      currentVersionIndex.value = planVersions.value.length - 1
      
      // Parse and set prompt from JSON if available
      try {
        const parsed = JSON.parse(latestContent)
        if (parsed.prompt) {
          generatorPrompt.value = parsed.prompt
        }
        if (parsed.params) {
          executionParams.value = parsed.params
        }
      } catch (e) {
        console.warn('无法解析JSON内容获取提示信息')
      }
    }
  } catch (error: any) {
    console.error('加载模板数据失败:', error)
  }
}

const onJsonContentChange = () => {
  emit('jsonContentChanged', { content: jsonContent.value })
}

const saveToVersionHistory = (content: string) => {
  // Remove future versions if we're not at the latest
  if (currentVersionIndex.value < planVersions.value.length - 1) {
    planVersions.value = planVersions.value.slice(0, currentVersionIndex.value + 1)
  }
  planVersions.value.push(content)
  currentVersionIndex.value = planVersions.value.length - 1
}

const handleRollback = () => {
  if (canRollback.value) {
    currentVersionIndex.value--
    jsonContent.value = planVersions.value[currentVersionIndex.value]
  }
}

const handleRestore = () => {
  if (canRestore.value) {
    currentVersionIndex.value++
    jsonContent.value = planVersions.value[currentVersionIndex.value]
  }
}

const handleSaveTemplate = async () => {
  if (!currentTemplate.value) return
  
  const content = jsonContent.value.trim()
  if (!content) {
    alert('内容不能为空')
    return
  }

  try {
    // Validate JSON
    JSON.parse(content)
  } catch (e: any) {
    alert('JSON 格式无效，请修正后再保存\n错误: ' + e.message)
    return
  }

  try {
    const result = await PlanActApiService.savePlanTemplate(currentTemplate.value.id, content)
    
    if (result.duplicate) {
      alert(`保存完成：${result.message}\n\n当前版本数：${result.versionCount}`)
    } else if (result.saved) {
      saveToVersionHistory(content)
      alert(`保存成功：${result.message}\n\n当前版本数：${result.versionCount}`)
    } else {
      alert(`保存状态：${result.message}`)
    }
    
    emit('templateSaved', { templateId: currentTemplate.value.id, content })
  } catch (error: any) {
    console.error('保存模板失败:', error)
    alert('保存模板失败: ' + error.message)
  }
}

const handleGeneratePlan = async () => {
  const prompt = generatorPrompt.value.trim()
  if (!prompt) return

  isGenerating.value = true
  try {
    const existingJson = jsonContent.value.trim() || undefined
    const response = await PlanActApiService.generatePlan(prompt, existingJson)
    
    if (response.planJson) {
      jsonContent.value = response.planJson
      saveToVersionHistory(response.planJson)
      
      // Update current template if this creates a new one
      if (response.planTemplateId && !currentTemplate.value) {
        currentTemplate.value = {
          id: response.planTemplateId,
          title: response.title || '新生成的计划',
          description: prompt,
          createTime: new Date().toISOString()
        }
      }
    }
  } catch (error: any) {
    console.error('生成计划失败:', error)
    alert('生成计划失败: ' + error.message)
  } finally {
    isGenerating.value = false
  }
}

const handleUpdatePlan = async () => {
  if (!currentTemplate.value) return
  
  const prompt = generatorPrompt.value.trim()
  if (!prompt) return

  isGenerating.value = true
  try {
    const existingJson = jsonContent.value.trim() || undefined
    const response = await PlanActApiService.updatePlanTemplate(
      currentTemplate.value.id, 
      prompt, 
      existingJson
    )
    
    if (response.planJson) {
      jsonContent.value = response.planJson
      saveToVersionHistory(response.planJson)
    }
  } catch (error: any) {
    console.error('更新计划失败:', error)
    alert('更新计划失败: ' + error.message)
  } finally {
    isGenerating.value = false
  }
}

const handleExecutePlan = async () => {
  if (!currentTemplate.value) return

  isExecuting.value = true
  try {
    const params = executionParams.value.trim() || undefined
    const response = await PlanActApiService.executePlan(currentTemplate.value.id, params)
    
    const query = `执行计划模板: ${currentTemplate.value.title || currentTemplate.value.id}`
    emit('planExecuted', { planId: response.planId, query })
    
    console.log('计划执行成功:', response)
  } catch (error: any) {
    console.error('执行计划失败:', error)
    alert('执行计划失败: ' + error.message)
  } finally {
    isExecuting.value = false
  }
}

const clearExecutionParams = () => {
  executionParams.value = ''
}

const handleClose = () => {
  emit('configClosed')
}

// Expose methods for parent component
defineExpose({
  loadTemplateData,
  getCurrentTemplate: () => currentTemplate.value,
  getJsonContent: () => jsonContent.value,
  setJsonContent: (content: string) => {
    jsonContent.value = content
    saveToVersionHistory(content)
  }
})
</script>

<style scoped>
.plan-template-config {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.02);
}

.config-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .config-title {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 20px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .template-info {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;

    .template-name {
      font-size: 16px;
      font-weight: 500;
      color: white;
    }

    .template-id {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.6);
    }
  }

  .close-btn {
    width: 32px;
    height: 32px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.7);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.3);
      color: white;
    }
  }
}

.config-content {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.config-section {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  overflow: hidden;

  .section-header {
    padding: 16px 20px;
    background: rgba(255, 255, 255, 0.03);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 16px;
    font-weight: 500;
    color: white;

    .section-actions {
      margin-left: auto;
      display: flex;
      gap: 8px;
    }
  }
}

.json-editor-section {
  .json-editor-container {
    padding: 20px;

    .json-editor {
      width: 100%;
      min-height: 300px;
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      color: white;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 14px;
      line-height: 1.5;
      padding: 16px;
      resize: vertical;

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

.generator-section {
  .generator-container {
    padding: 20px;

    .input-group {
      margin-bottom: 16px;

      label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.9);
      }

      .prompt-input {
        width: 100%;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 8px;
        color: white;
        font-size: 14px;
        padding: 12px;
        resize: vertical;

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

    .generator-actions {
      display: flex;
      gap: 12px;

      .btn-generate {
        flex: 1;
      }
    }
  }
}

.execution-section {
  .execution-container {
    padding: 20px;

    .input-group {
      margin-bottom: 16px;
      position: relative;

      .params-input {
        width: 100%;
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 8px;
        color: white;
        font-size: 14px;
        padding: 12px;
        padding-right: 40px;
        resize: vertical;

        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
        }

        &::placeholder {
          color: rgba(255, 255, 255, 0.4);
        }
      }

      .clear-params-btn {
        position: absolute;
        top: 32px;
        right: 8px;
        padding: 8px;
        color: rgba(255, 255, 255, 0.5);

        &:hover {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }

    .api-info {
      margin-bottom: 16px;

      label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.9);
      }

      .api-url {
        background: rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 8px;
        color: rgba(255, 255, 255, 0.8);
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 12px;
        padding: 12px;
        word-break: break-all;
      }
    }

    .execution-actions {
      .btn-execute {
        width: 100%;
      }
    }
  }
}

/* Button Styles */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.btn-primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
    }
  }

  &.btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: white;
    border: 1px solid rgba(255, 255, 255, 0.2);

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  &.btn-success {
    background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
    }
  }

  &.btn-link {
    background: transparent;
    color: rgba(255, 255, 255, 0.7);
    padding: 4px;

    &:hover:not(:disabled) {
      color: white;
    }
  }
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
