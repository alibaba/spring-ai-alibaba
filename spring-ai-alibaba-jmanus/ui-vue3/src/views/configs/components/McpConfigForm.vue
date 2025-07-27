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

    <!-- 使用说明 -->
    <div class="usage-instructions">
      <div class="instructions-header">
        <Icon icon="carbon:information" class="instructions-icon" />
        <h4>使用说明</h4>
      </div>
      <div class="instructions-content">
        <ol class="instructions-list">
          <li>
            <strong>获取MCP服务列表</strong>
            <ul>
              <li>可以在<code>mcp.higress.ai</code>上查找可用的MCP服务</li>
              <li>如果需要批量配置MCP服务，可以使用右上角的全部导入功能</li>
            </ul>
          </li>
          <li>
            <strong>配置MCP服务</strong>
            <ul>
              <li>填写服务名称:</li>
              <li>选择连接类型:本地选择 <code>STUDIO</code> 以及输入 Command、Args和Env。远程选择 <code>SSE</code> 或 <code>STREAMING</code> 以及输入 URL</li>
              <li>点击保存按钮完成MCP配置，MCP工具将自动注册到系统中</li>
            </ul>
          </li>
          <li>
            <strong>配置Agent使用</strong>
            <ul>
              <li>在Agent配置页面创建新的Agent，为Agent添加刚配置的MCP服务，这样可以减少工具冲突，提高Agent选择工具的准确性</li>
            </ul>
          </li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
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
  { id: 'STUDIO', name: 'STUDIO' },
  { id: 'SSE', name: 'SSE' },
  { id: 'STREAMING', name: 'STREAMING' }
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

/* 使用说明样式 */
.usage-instructions {
  margin-top: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(102, 126, 234, 0.5);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
}

.usage-instructions::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: rgba(102, 126, 234, 0.8);
  border-radius: 8px 8px 0 0;
}

.instructions-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.instructions-icon {
  font-size: 16px;
  color: #667eea;
}

.instructions-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.instructions-content {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
  font-size: 14px;
}

.instructions-list {
  margin: 0;
  padding-left: 16px;
}

.instructions-list > li {
  margin-bottom: 12px;
  position: relative;
  padding-left: 4px;
}

.instructions-list > li::marker {
  color: #667eea;
  font-weight: 600;
  font-size: 14px;
}

.instructions-list > li:last-child {
  margin-bottom: 0;
}

.instructions-list strong {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 600;
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
}

.instructions-list ul {
  margin: 6px 0 0 0;
  padding-left: 16px;
  list-style-type: disc;
}

.instructions-list ul li {
  margin-bottom: 3px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  line-height: 1.4;
}

.instructions-list ul li strong {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  display: inline;
  margin-bottom: 0;
}

.instructions-list code {
  background: rgba(102, 126, 234, 0.15);
  color: #a8b3ff;
  padding: 1px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .usage-instructions {
    margin-top: 20px;
    padding: 12px;
  }
  
  .instructions-header h4 {
    font-size: 15px;
  }
  
  .instructions-content {
    font-size: 13px;
  }
  
  .instructions-list {
    padding-left: 12px;
  }
  
  .instructions-list > li {
    margin-bottom: 10px;
  }
  
  .instructions-list ul li {
    font-size: 12px;
  }
}
</style> 
