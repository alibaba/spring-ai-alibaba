// Authentication configuration interface
interface Auth {
  type: 'api_key' | 'none';
  authorization_type: 'basic' | 'bearer' | 'custom';
  authorization_position: 'header' | 'query';
  authorization_key: string;
  authorization_value: string;
}

// Plugin configuration interface
interface Config {
  schema_version?: 'v1';
  type?: 'openapi';
  server: string;
  auth: Auth;
  headers?: Record<string, string> | { name: string; value: string }[];
}

// Plugin interface
export interface Plugin {
  gmt_modified?: any;
  plugin_id?: string;
  name: string;
  description: string;
  config?: Config;
}

// Parameters for listing plugins
export interface ListPluginParams {
  name?: string;
  current?: number;
  size?: number;
  status?: number;
}

// Generic paginated list response
export interface PagingList<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}

// Plugin tool interface
export interface PluginTool {
  gmt_modified?: any;
  plugin_id?: string;
  tool_id?: string;
  name: string;
  description: string;
  enabled?: boolean;
  test_status?: string;
  config?: ToolConfig;
  status?: TEditStatus;
}

// Plugin edit status types
export type TEditStatus = 'draft' | 'published' | 'published_editing';

// Tool configuration interface
export interface ToolConfig {
  path: string;
  request_method: 'POST' | 'GET';
  content_type: 'application/json' | 'application/x-www-form-urlencoded';
  input_params?: {
    location: string;
    type: string;
    key: string;
    required: boolean;
    description: string;
    user_input: boolean; // Whether user input is required
  }[];
  output_params?: {
    type: string;
    key: string;
    description: string;
  }[];
  examples?: {
    query: string;
    parameters: Record<string, any>;
    path: string;
  }[];
}
