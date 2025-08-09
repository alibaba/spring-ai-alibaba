<template>
  <div class="mcp-config-form">
    <div class="form-item">
      <label>{{ $t('config.mcpConfig.mcpName') }} <span class="required">*</span></label>
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

    <!-- Command field - only show when STUDIO is selected -->
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

    <!-- URL field - only show when SSE or STREAMING is selected -->
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

    <!-- Args field - only show when STUDIO is selected -->
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

    <!-- Env field - only show when STUDIO is selected -->
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

    <!-- Usage instructions -->
    <div class="usage-instructions">
      <div class="instructions-header">
        <Icon icon="carbon:information" class="instructions-icon" />
        <h4>{{ $t('config.mcpConfig.usageInstructions') }}</h4>
      </div>
      <div class="instructions-content">
        <ol class="instructions-list">
          <li>
            <strong>{{ $t('config.mcpConfig.getMcpServiceList') }}</strong>
            <ul>
              <li>{{ $t('config.mcpConfig.findMcpServices') }}</li>
              <li>{{ $t('config.mcpConfig.batchImportTip') }}</li>
            </ul>
          </li>
          <li>
            <strong>{{ $t('config.mcpConfig.configureMcpService') }}</strong>
            <ul>
              <li>{{ $t('config.mcpConfig.fillServiceName') }}</li>
              <li>{{ $t('config.mcpConfig.selectConnectionType') }}</li>
              <li>{{ $t('config.mcpConfig.clickSaveToComplete') }}</li>
            </ul>
          </li>
          <li>
            <strong>{{ $t('config.mcpConfig.configureAgentUsage') }}</strong>
            <ul>
              <li>{{ $t('config.mcpConfig.createAgentTip') }}</li>
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

// Connection type options
const connectionTypes = [
  { id: 'STUDIO', name: 'STUDIO' },
  { id: 'SSE', name: 'SSE' },
  { id: 'STREAMING', name: 'STREAMING' }
]

// Handle input events
const handleInput = (field: string, event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  if (props.isEditMode) {
    // Edit mode: directly modify formData
    ;(props.formData as any)[field] = target.value
  } else {
    // Add mode: notify parent component via emit
    emit('update:formData', { ...props.formData, [field]: target.value })
  }
}

// Handle connection type update
const handleConnectionTypeUpdate = (value: string | null) => {
  if (!value) return

  if (props.isEditMode) {
    // Edit mode: directly modify formData
    ;(props.formData as any).connectionType = value
  } else {
    // Add mode: notify parent component via emit
    emit('update:formData', { ...props.formData, connectionType: value })
  }

  // Trigger connection type change event
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

/* Usage instructions styles */
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

/* Responsive design */
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
