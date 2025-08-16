import $i18n from '@/i18n';

// MCP server status enum
export enum McpStatus {
  DISABLED = 0,
  ENABLED = 1,
  DELETED = 3,
}

// MCP status mapping with display text and colors
export const McpStatusMap = {
  [McpStatus.DISABLED]: {
    color: 'error',
    text: $i18n.get({ id: 'main.types.mcp.stopped', dm: '已停用' }),
  },
  [McpStatus.ENABLED]: {
    color: 'success',
    text: $i18n.get({ id: 'main.types.mcp.started', dm: '已启动' }),
  },
  [McpStatus.DELETED]: {
    color: 'default',
    text: $i18n.get({ id: 'main.types.mcp.deleted', dm: '已删除' }),
  },
};

// Maximum limit for MCP servers
export const MCP_MAX_LIMIT = 5;

// Interface for MCP server basic information
export interface IMcpServer {
  server_code: string;
  name: string;
  deploy_config: string;
  detail_config: string;
  status: McpStatus;
  type: string; // CUSTOMER/OFFICIAL
  description: string;
  install_type: string;
  source?: string;
  need_tools?: boolean;
  tools?: IMCPTool[];
  gmt_modified?: string;
}

// Parameters for creating MCP server
export interface ICreateMcpParams {
  name: string;
  deploy_config: string;
  detail_config?: string;
  type: string;
  description: string;
  install_type?: string;
}

// Parameters for updating MCP server
export interface IUpdateMcpParams {
  server_code: string;
  name: string;
  deploy_config: string;
  detail_config?: string;
  status?: McpStatus;
  type: string;
  description: string;
  install_type?: string;
  source?: string;
}

// Parameters for getting MCP server details
export interface IGetMcpServerParams {
  server_code: string;
  need_tools: boolean;
}

// Parameters for listing MCP servers
export interface IListMcpServersParams {
  current?: number;
  size?: number;
  total?: number;
  status?: McpStatus;
  need_tools?: boolean;
  name?: string;
}

// Parameters for listing MCP servers by codes
export interface IListMcpServersByCodesParams {
  server_codes: string[];
  need_tools: boolean;
}

// Generic paginated list response
export interface IPagingList<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}

// Request parameters for MCP tool call
export interface IMcpServerCallToolRequest {
  server_code: string;
  tool_name: string;
  tool_params?: Record<string, any>;
}

// Response for MCP tool call
export interface IMcpServerCallToolResponse {
  tool_output: string;
  status: string;
  [key: string]: any; // Additional extensible parameters
}

// Interface for MCP tool definition
export interface IMCPTool {
  name: string;
  description: string;
  input_schema: {
    type: string;
    properties: Record<string, IToolProperty>;
    required: string[];
    additionalProperties: boolean;
  };
}

// Interface for tool property definition
export interface IToolProperty {
  type: string;
  description?: string;
}
