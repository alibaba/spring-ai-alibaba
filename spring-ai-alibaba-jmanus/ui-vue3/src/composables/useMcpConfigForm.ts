import { reactive } from 'vue'
import type { McpConfigFormData, McpServer } from '@/types/mcp'

export function useMcpConfigForm() {
  // 统一的配置表单数据
  const configForm = reactive<McpConfigFormData>({
    mcpServerName: '',
    connectionType: 'STUDIO',
    command: '',
    url: '',
    args: '', // 前端输入为JSON字符串
    env: '', // 前端输入为JSON字符串
    status: 'ENABLE'
  })

  // 解析环境变量字符串为对象
  const parseEnvString = (envString: string): Record<string, string> => {
    const env: Record<string, string> = {}
    const lines = envString.split('\n').filter(line => line.trim())
    
    for (const line of lines) {
      const colonIndex = line.indexOf(':')
      if (colonIndex > 0) {
        const key = line.substring(0, colonIndex).trim()
        const value = line.substring(colonIndex + 1).trim()
        if (key && value) {
          env[key] = value
        }
      }
    }
    
    return env
  }

  // 重置表单
  const resetForm = () => {
    Object.assign(configForm, {
      mcpServerName: '',
      connectionType: 'STUDIO',
      command: '',
      url: '',
      args: '',
      env: '',
      status: 'ENABLE'
    })
  }

  // 从服务器数据填充表单
  const populateFormFromServer = (server: McpServer) => {
    try {
      const config = JSON.parse(server.connectionConfig)
      
      // 填充configForm
      configForm.mcpServerName = server.mcpServerName || ''
      configForm.connectionType = server.connectionType
      configForm.command = config.command || ''
      configForm.url = config.url || ''
      
      // args和env都使用多行格式显示
      if (config.args && Array.isArray(config.args)) {
        configForm.args = config.args.join('\n')
      } else {
        configForm.args = ''
      }
      
      if (config.env && typeof config.env === 'object' && !Array.isArray(config.env)) {
        configForm.env = Object.entries(config.env)
          .map(([key, value]) => `${key}:${value}`)
          .join('\n')
      } else {
        configForm.env = ''
      }
      
      configForm.status = server.status || 'ENABLE'
    } catch (error) {
      console.error('解析服务器配置失败:', error)
      // 如果解析失败，使用默认值
      configForm.mcpServerName = server.mcpServerName || ''
      configForm.connectionType = server.connectionType
      configForm.command = ''
      configForm.url = ''
      configForm.args = ''
      configForm.env = ''
      configForm.status = server.status || 'ENABLE'
    }
  }

  // 表单验证
  const validateForm = (): { isValid: boolean; errors: string[] } => {
    const errors: string[] = []
    
    if (!configForm.mcpServerName?.trim()) {
      errors.push('请输入MCP服务器名称')
    }

    if (configForm.connectionType === 'STUDIO') {
      if (!configForm.command?.trim()) {
        errors.push('请输入Command')
      }
    } else if (configForm.connectionType === 'SSE' || configForm.connectionType === 'STREAMING') {
      if (!configForm.url?.trim()) {
        errors.push('请输入URL')
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  // 处理连接类型变化
  const handleConnectionTypeChange = () => {
    // 当连接类型改变时，清空相关字段
    configForm.command = ''
    configForm.args = ''
    configForm.env = ''
    configForm.url = ''
  }

  return {
    configForm,
    parseEnvString,
    resetForm,
    populateFormFromServer,
    validateForm,
    handleConnectionTypeChange
  }
} 
