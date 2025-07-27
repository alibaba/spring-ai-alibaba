<template>
  <div class="mcp-config-form">
    <div class="form-item">
      <label>MCP名称 <span class="required">*</span></label>
      <input
        :value="formData.mcpServerName || ''"
        @input="handleInput('mcpServerName', $event)"
        type="text"
        class="config-input"
        :placeholder="t('config.mcpConfig.mcpServerNamePlaceholder')"
      />
    </div>

    <div class="form-item">
      <label>{{ t('config.mcpConfig.connectionType') }} <span class="required">*</span></label>
      <CustomSelect
        :model-value="formData.connectionType"
        @update:model-value="handleConnectionTypeUpdate"
        :options="connectionTypes"
        :placeholder="t('config.mcpConfig.connectionTypePlaceholder')"
        :dropdown-title="t('config.mcpConfig.connectionTypePlaceholder')"
        icon="carbon:connection"
      />
    </div>

    <!-- Command 字段 - 仅当选择STUDIO时显示 -->
    <div class="form-item" v-if="formData.connectionType === 'STUDIO'">
      <label>{{ t('config.mcpConfig.command') }} <span class="required">*</span></label>
      <input
        :value="formData.command || ''"
        @input="handleInput('command', $event)"
        type="text"
        class="config-input"
        :placeholder="t('config.mcpConfig.commandPlaceholder')"
      />
    </div>

    <!-- URL 字段 - 仅当选择SSE或STREAMING时显示 -->
    <div class="form-item" v-if="formData.connectionType === 'SSE' || formData.connectionType === 'STREAMING'">
      <label>{{ t('config.mcpConfig.url') }} <span class="required">*</span></label>
      <input
        :value="formData.url || ''"
        @input="handleInput('url', $event)"
        type="text"
        class="config-input"
        :placeholder="t('config.mcpConfig.urlPlaceholder')"
      />
    </div>

    <!-- Args 字段 - 仅当选择STUDIO时显示 -->
    <div class="form-item" v-if="formData.connectionType === 'STUDIO'">
      <label>{{ t('config.mcpConfig.args') }} <span class="required">*</span></label>
      <textarea
        :value="formData.args || ''"
        @input="handleInput('args', $event)"
        class="config-textarea"
        :placeholder="t('config.mcpConfig.argsPlaceholder')"
        rows="3"
      />
    </div>

    <!-- Env 字段 - 仅当选择STUDIO时显示 -->
    <div class="form-item" v-if="formData.connectionType === 'STUDIO'">
      <label>{{ t('config.mcpConfig.env') }}</label>
      <textarea
        :value="formData.env || ''"
        @input="handleInput('env', $event)"
        class="config-textarea"
        :placeholder="t('config.mcpConfig.envPlaceholder')"
        rows="3"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import CustomSelect from '@/components/select/index.vue'

const { t } = useI18n()

// Props
interface Props {
  formData: {
    mcpServerName?: string
    connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
    command?: string
    url?: string
    args?: string
    env?: string
    status?: 'ENABLE' | 'DISABLE'
  }
  isEditMode: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'connection-type-change': []
  'update:formData': [data: any]
}>()

// 连接类型选项
const connectionTypes = [
  { id: 'STUDIO', name: 'STUDIO', icon: 'carbon:plug' },
  { id: 'SSE', name: 'SSE', icon: 'carbon:plug' },
  { id: 'STREAMING', name: 'STREAMING', icon: 'carbon:plug' }
]

// 处理输入事件
const handleInput = (field: string, event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  if (props.isEditMode) {
    // 编辑模式：直接修改formData
    ;(props.formData as any)[field] = target.value
  } else {
    // 新增模式：通过emit通知父组件
    emit('update:formData', { ...props.formData, [field]: target.value })
  }
}

// 处理连接类型更新
const handleConnectionTypeUpdate = (value: string | null) => {
  if (!value) return
  
  if (props.isEditMode) {
    // 编辑模式：直接修改formData
    ;(props.formData as any).connectionType = value
  } else {
    // 新增模式：通过emit通知父组件
    emit('update:formData', { ...props.formData, connectionType: value })
  }
  
  // 触发连接类型变化事件
  emit('connection-type-change')
}
</script>

<style scoped>
.mcp-config-form {
  width: 100%;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #e5e7eb;
}

.required {
  color: #ef4444;
}

.config-input,
.config-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.config-input:focus,
.config-textarea:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.config-textarea {
  resize: vertical;
  min-height: 80px;
}

.config-input::placeholder,
.config-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}
</style> 
