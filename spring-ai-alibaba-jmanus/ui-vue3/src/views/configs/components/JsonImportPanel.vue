<template>
  <div class="json-import-panel">
    <div class="form-item">
      <TabPanel 
        :tabs="tabs" 
        v-model="activeTabIndex"
        class="json-tab-panel"
      >
        <template #json-config>
          <MonacoEditor
            v-model="jsonContent"
            language="json"
            :height="600"
            @change="validateJson"
          />
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

// 响应式数据
const jsonContent = ref(props.modelValue)
const isJsonValid = ref(true)
const validationErrors = ref<string[]>([])
const activeTabIndex = ref(0)

// Tab配置
const tabs = computed<TabConfig[]>(() => [
  {
    name: 'json-config',
    label: 'JSON配置'
  },
  {
    name: 'config-example',
    label: '配置示例'
  }
])

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  jsonContent.value = newValue
})

// 监听jsonContent变化
watch(jsonContent, (newValue) => {
  emit('update:modelValue', newValue)
})

// JSON校验
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
      // 验证通过后，应用配置统一化并更新编辑器内容
      const normalizedConfig = normalizeMcpConfig(parsed)
      const normalizedJson = JSON.stringify(normalizedConfig, null, 2)
      
      // 只有当统一化后的JSON与原始JSON不同时才更新
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
    
    // 提供更具体的JSON语法错误信息
    let errorMessage = t('config.mcpConfig.invalidJson')
    if (error instanceof SyntaxError) {
      const message = error.message
      if (message.includes('Unexpected token')) {
        errorMessage = '❌ JSON语法错误 - 请检查括号、逗号、引号等符号是否正确'
      } else if (message.includes('Unexpected end')) {
        errorMessage = '❌ JSON不完整 - 请检查是否缺少结束括号或引号'
      } else if (message.includes('Unexpected number')) {
        errorMessage = '❌ JSON数字格式错误 - 请检查数字格式'
      } else if (message.includes('Unexpected string')) {
        errorMessage = '❌ JSON字符串格式错误 - 请检查引号是否配对'
      } else {
        errorMessage = `❌ JSON语法错误: ${message}`
      }
    }
    
    validationErrors.value = [errorMessage]
    emitValidationResult()
  }
}

// 发送校验结果
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
    errors.push('💡 正确格式示例: {"mcpServers": {"server-id": {"name": "服务器名称", "url": "服务器地址"}}}')
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
      
      // 增强env校验逻辑：可以没有env，有env的话可以允许env:[]为空
      if (server.env !== undefined) {
        if (server.env !== null && typeof server.env !== 'object') {
          errors.push(t('config.mcpConfig.invalidEnv', { serverId }))
        } else if (server.env !== null && Array.isArray(server.env)) {
          // env是数组的情况，允许空数组
          if (server.env.length > 0) {
            // 如果数组不为空，检查每个元素是否为字符串
            for (let i = 0; i < server.env.length; i++) {
              if (typeof server.env[i] !== 'string') {
                errors.push(t('config.mcpConfig.invalidEnvType', { serverId, index: i }))
              }
            }
          }
        } else if (server.env !== null && !Array.isArray(server.env)) {
          // env是对象的情况，检查每个值是否为字符串
          for (const [key, value] of Object.entries(server.env)) {
            if (typeof value !== 'string') {
              errors.push(t('config.mcpConfig.invalidEnvType', { serverId, key }))
            }
          }
        }
      }
    } else {
      // If no command, validate url or baseUrl - 必须有一个
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'
      
      if (!hasUrl && !hasBaseUrl) {
        errors.push(`缺少url字段: ${serverId} - 没有command时必须有url或baseUrl`)
        errors.push('💡 需要提供 url 或 baseUrl 字段')
      } else {
        // 校验url或baseUrl格式
        const urlToValidate = hasUrl ? server.url : server.baseUrl
        try {
          new URL(urlToValidate)
        } catch {
          errors.push(t('config.mcpConfig.invalidUrl', { serverId }))
        }
        
        // 统一使用url字段：如果配置中使用的是baseUrl，转换为url
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

// 统一处理MCP配置中的url字段
const normalizeMcpConfig = (config: any): any => {
  if (!config.mcpServers) {
    return config
  }

  const normalizedConfig = { ...config }
  normalizedConfig.mcpServers = { ...config.mcpServers }

  for (const [serverId, serverConfig] of Object.entries(config.mcpServers)) {
    const server = serverConfig as any
    const normalizedServer = { ...server }

    // 如果没有command，处理url/baseUrl统一化
    if (!server.command) {
      const hasUrl = server.url && typeof server.url === 'string'
      const hasBaseUrl = server.baseUrl && typeof server.baseUrl === 'string'
      
      if (hasBaseUrl && !hasUrl) {
        // 如果只有baseUrl，转换为url
        normalizedServer.url = server.baseUrl
        delete normalizedServer.baseUrl
      } else if (!hasUrl && !hasBaseUrl) {
        // 如果既没有url也没有baseUrl，保持原样（让校验函数处理错误）
        console.warn(`Server ${serverId} has no command but also no url or baseUrl`)
      }
    }

    normalizedConfig.mcpServers[serverId] = normalizedServer
  }

  return normalizedConfig
}

// 暴露方法给父组件
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

/* 配置示例相关样式 */
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

/* JSON语法高亮 */
.example-json .string { color: #a78bfa; }
.example-json .number { color: #fbbf24; }
.example-json .boolean { color: #f87171; }
.example-json .null { color: rgba(255, 255, 255, 0.6); }
.example-json .key { color: #34d399; }
</style> 
