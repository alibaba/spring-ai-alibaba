<template>
  <div class="json-import-panel">
    <div class="form-item">
      <TabPanel
        :tabs="tabs"
        v-model="activeTabIndex"
        class="json-tab-panel"
      >
        <template #json-config>
          <div class="json-config-container">
            <MonacoEditor
              v-model="jsonContent"
              language="json"
              :height="400"
              @change="validateJson"
            />

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
                    </ul>
                  </li>
                  <li>
                    <strong>{{ $t('config.mcpConfig.configureMcpService') }}</strong>
                    <ul>
                      <li>{{ $t('config.mcpConfig.copyJsonConfig') }}</li>
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

        <template #config-example>
          <pre class="example-json"><code>{
  "mcpServers": {
    "mcp-ip-query": {
      "url": "https://mcp.higress.ai/mcp-ip-query/cmb6h8vpr00e08a01dx15ck8o"
    },
    "excel-server": {
      "command": "uvx",
      "args": [
        "excel-server"
      ],
      "env": {
        "EXCEL_PATH": "/path/to/excel/files"
      }
    }
  }
}</code></pre>
        </template>
      </TabPanel>
    </div>

    <div v-if="!isJsonValid" class="validation-errors">
      <div v-for="error in validationErrors" :key="error" class="error-item">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import TabPanel from '@/components/TabPanel.vue'
import type { TabConfig, JsonValidationResult } from '@/types/mcp'

// Props
interface Props {
  modelValue: string
  onValidationChange?: (result: JsonValidationResult) => void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  onValidationChange: () => {}
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'validationChange': [result: JsonValidationResult]
}>()

// Internationalization
const { t } = useI18n()

// Reactive data
const jsonContent = ref(props.modelValue)
const isJsonValid = ref(true)
const validationErrors = ref<string[]>([])
const activeTabIndex = ref(0)

// Tab configuration
const tabs = computed<TabConfig[]>(() => [
  {
    name: 'json-config',
    label: 'JSON Configuration'
  },
  {
    name: 'config-example',
    label: 'Configuration Example'
  }
])

// Watch modelValue changes
watch(() => props.modelValue, (newValue) => {
  jsonContent.value = newValue
})

// Watch jsonContent changes
watch(jsonContent, (newValue) => {
  emit('update:modelValue', newValue)
})

// JSON validation
const validateJson = () => {
  const jsonText = jsonContent.value
  if (!jsonText) {
    isJsonValid.value = true
    validationErrors.value = []
    emitValidationResult()
    return
  }

  try {
    const parsed = JSON.parse(jsonText)
    const validationResult = validateMcpConfig(parsed)

    if (validationResult.isValid) {
      // After validation passes, apply configuration normalization and update editor content
      const normalizedConfig = normalizeMcpConfig(parsed)
      const normalizedJson = JSON.stringify(normalizedConfig, null, 2)

      // Only update when normalized JSON differs from original JSON
      if (normalizedJson !== jsonText) {
        jsonContent.value = normalizedJson
      }

      isJsonValid.value = true
      validationErrors.value = []
    } else {
      isJsonValid.value = false
      validationErrors.value = validationResult.errors || []
    }

    emitValidationResult()
  } catch (error) {
    isJsonValid.value = false

    // Provide more specific JSON syntax error information
    let errorMessage = t('config.mcpConfig.invalidJson')
    if (error instanceof SyntaxError) {
      const message = error.message
      if (message.includes('Unexpected token')) {
        errorMessage = '❌ JSON syntax error - Please check if brackets, commas, quotes and other symbols are correct'
      } else if (message.includes('Unexpected end')) {
        errorMessage = '❌ JSON incomplete - Please check if closing brackets or quotes are missing'
      } else if (message.includes('Unexpected number')) {
        errorMessage = '❌ JSON number format error - Please check number format'
      } else if (message.includes('Unexpected string')) {
        errorMessage = '❌ JSON string format error - Please check if quotes are paired'
      } else {
        errorMessage = `❌ JSON syntax error: ${message}`
      }
    }

    validationErrors.value = [errorMessage]
    emitValidationResult()
  }
}

// Send validation result
const emitValidationResult = () => {
  const result: JsonValidationResult = {
    isValid: isJsonValid.value,
    errors: validationErrors.value
  }
  emit('validationChange', result)
  props.onValidationChange?.(result)
}

// Validate MCP configuration structure
const validateMcpConfig = (config: any): JsonValidationResult => {
  const errors: string[] = []

  // Check if config has mcpServers property
  if (!config.mcpServers || typeof config.mcpServers !== 'object') {
    errors.push(t('config.mcpConfig.missingMcpServers'))
    errors.push('💡 Correct format example: {"mcpServers": {"server-id": {"name": "Server Name", "url": "Server URL"}}}')
    return { isValid: false, errors }
  }

  const servers = config.mcpServers

  // Validate each server configuration
  for (const [serverId, serverConfig] of Object.entries(servers)) {
    if (typeof serverConfig !== 'object' || serverConfig === null) {
      errors.push(t('config.mcpConfig.invalidServerConfig', { serverId }))
      continue
    }

    const server = serverConfig as any

    // Validate based on whether command exists
    if (server.command) {
      // If command exists, validate args and env
      if (!Array.isArray(server.args)) {
        errors.push(t('config.mcpConfig.invalidArgs', { serverId }))
      } else {
        // args should contain strings
        for (let i = 0; i < server.args.length; i++) {
          if (typeof server.args[i] !== 'string') {
            errors.push(t('config.mcpConfig.invalidArgsType', { serverId, index: i }))
          }
        }
      }

      // Enhanced env validation logic: can have no env, if env exists, allow empty env:[]
      if (server.env !== undefined) {
        if (server.env !== null && typeof server.env !== 'object') {
          errors.push(t('config.mcpConfig.invalidEnv', { serverId }))
        } else if (server.env !== null && Array.isArray(server.env)) {
          // env is array case, allow empty array
          if (server.env.length > 0) {
            // If array is not empty, check if each element is a string
            for (let i = 0; i < server.env.length; i++) {
              if (typeof server.env[i] !== 'string') {
                errors.push(t('config.mcpConfig.invalidEnvType', { serverId, index: i }))
              }
            }
          }
        } else if (server.env !== null && !Array.isArray(server.env)) {
          // env is object case, check if each value is a string
          for (const [key, value] of Object.entries(server.env)) {
            if (typeof value !== 'string') {
              errors.push(t('config.mcpConfig.invalidEnvType', { serverId, key }))
            }
          }
        }
      }
    } else {
      // If no command, validate url or baseUrl - must have one
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'

      if (!hasUrl && !hasBaseUrl) {
        errors.push(`Missing url field: ${serverId} - Must have url or baseUrl when no command`)
        errors.push('💡 Need to provide url or baseUrl field')
      } else {
        // Validate url or baseUrl format
        const urlToValidate = hasUrl ? server.url : server.baseUrl
        try {
          new URL(urlToValidate)
        } catch {
          errors.push(t('config.mcpConfig.invalidUrl', { serverId }))
        }

        // Unify url field usage: if baseUrl is used in config, convert to url
        if (hasBaseUrl && !hasUrl) {
          server.url = server.baseUrl
          delete server.baseUrl
        }
      }
    }
  }

  if (errors.length === 0) {
    return { isValid: true }
  } else {
    return { isValid: false, errors }
  }
}

// Unify url field handling in MCP configuration
const normalizeMcpConfig = (config: any): any => {
  if (!config.mcpServers) {
    return config
  }

  const normalizedConfig = { ...config }
  normalizedConfig.mcpServers = { ...config.mcpServers }

  for (const [serverId, serverConfig] of Object.entries(config.mcpServers)) {
    const server = serverConfig as any
    const normalizedServer = { ...server }

    // If no command, handle url/baseUrl unification
    if (!server.command) {
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'

      if (hasBaseUrl && !hasUrl) {
        // If only baseUrl exists, convert to url
        normalizedServer.url = server.baseUrl
        delete normalizedServer.baseUrl
      } else if (!hasUrl && !hasBaseUrl) {
        // If neither url nor baseUrl exists, keep as is (let validation function handle error)
        console.warn(`Server ${serverId} has no command but also no url or baseUrl`)
      }
    }

    normalizedConfig.mcpServers[serverId] = normalizedServer
  }

  return normalizedConfig
}

// Expose methods to parent component
defineExpose({
  validateJson,
  isJsonValid: computed(() => isJsonValid.value),
  validationErrors: computed(() => validationErrors.value)
})
</script>

<style scoped>
.json-import-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.json-tab-panel {
  margin-top: 8px;
}

.validation-errors {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 0, 0, 0.1);
  border: 1px solid rgba(255, 0, 0, 0.3);
  border-radius: 4px;
}

.error-item {
  color: #ff4444;
  font-size: 14px;
  margin-bottom: 4px;
}

.error-item:last-child {
  margin-bottom: 0;
}

/* Configuration example related styles */
.example-json {
  margin: 0;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  overflow-x: auto;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
}

.example-json code {
  color: rgba(255, 255, 255, 0.9);
  background: none;
  padding: 0;
  border: none;
  border-radius: 0;
  font-family: inherit;
  font-size: inherit;
}

/* JSON syntax highlighting */
.example-json .string { color: #a78bfa; }
.example-json .number { color: #fbbf24; }
.example-json .boolean { color: #f87171; }
.example-json .null { color: rgba(255, 255, 255, 0.6); }
.example-json .key { color: #34d399; }

/* JSON configuration container */
.json-config-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Usage instructions styles */
.usage-instructions {
  margin-top: 16px;
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
</style>
