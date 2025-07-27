// MCP相关类型定义

// 基础MCP服务器接口
export interface McpServer {
  id: number
  mcpServerName: string
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  connectionConfig: string
  status: 'ENABLE' | 'DISABLE'
}

// 扩展MCP服务器接口（包含UI字段）
export interface ExtendedMcpServer extends McpServer {
  args?: string // 前端显示为JSON字符串
  env?: string // 前端显示为JSON字符串
  url?: string
  command?: string
}

// MCP服务器字段请求接口
export interface McpServerFieldRequest {
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  mcpServerName: string
  status: 'ENABLE' | 'DISABLE'
  command?: string
  url?: string
  args?: string[]
  env?: Record<string, string>
}

// MCP服务器保存请求接口（合并新增和更新）
export interface McpServerSaveRequest extends McpServerFieldRequest {
  id?: number // 可选，有id则为更新，无id则为新增
}

// MCP服务器JSON导入请求接口
export interface McpServerRequest {
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  configJson: string
}

// API响应接口
export interface ApiResponse<T = any> {
  success: boolean
  message?: string
  data?: T
}

// 表单数据接口
export interface McpConfigFormData {
  mcpServerName: string
  connectionType: 'STUDIO' | 'SSE' | 'STREAMING'
  command: string
  url: string
  args: string // 前端输入为JSON字符串
  env: string // 前端输入为JSON字符串
  status: 'ENABLE' | 'DISABLE'
}

// 消息类型
export type MessageType = 'success' | 'error' | 'info'

// 消息接口
export interface Message {
  show: boolean
  text: string
  type: MessageType
}

// Tab配置接口
export interface TabConfig {
  name: string
  label: string
}

// JSON校验结果接口
export interface JsonValidationResult {
  isValid: boolean
  errors?: string[]
} 
