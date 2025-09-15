import { reactive } from 'vue'
import type { McpConfigFormData, McpServer } from '@/types/mcp'

export function useMcpConfigForm() {
  // Unified configuration form data
  const configForm = reactive<McpConfigFormData>({
    mcpServerName: '',
    connectionType: 'STUDIO',
    command: '',
    url: '',
    args: '', // Frontend input as JSON string
    env: '', // Frontend input as JSON string
    status: 'ENABLE'
  })

  // Parse environment variable string to object
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

  // Reset form
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

  // Populate form from server data
  const populateFormFromServer = (server: McpServer) => {
    try {
      const config = JSON.parse(server.connectionConfig)

      // Populate configForm
      configForm.mcpServerName = server.mcpServerName || ''
      configForm.connectionType = server.connectionType
      configForm.command = config.command || ''
      configForm.url = config.url || ''

      // Both args and env use multi-line format for display
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
      console.error('Failed to parse server configuration:', error)
      // If parsing fails, use default values
      configForm.mcpServerName = server.mcpServerName || ''
      configForm.connectionType = server.connectionType
      configForm.command = ''
      configForm.url = ''
      configForm.args = ''
      configForm.env = ''
      configForm.status = server.status || 'ENABLE'
    }
  }

  // Form validation
  const validateForm = (): { isValid: boolean; errors: string[] } => {
    const errors: string[] = []

    if (!configForm.mcpServerName?.trim()) {
      errors.push('Please enter MCP server name')
    }

    if (configForm.connectionType === 'STUDIO') {
      if (!configForm.command?.trim()) {
        errors.push('Please enter Command')
      }
    } else if (configForm.connectionType === 'SSE' || configForm.connectionType === 'STREAMING') {
      if (!configForm.url?.trim()) {
        errors.push('Please enter URL')
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  // Handle connection type change
  const handleConnectionTypeChange = () => {
    // When connection type changes, clear related fields
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
