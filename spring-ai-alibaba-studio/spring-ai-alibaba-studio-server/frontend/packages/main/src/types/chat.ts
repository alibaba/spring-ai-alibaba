// Interface for tool call function
export interface IToolCallFunction {
  name: string; // Tool name
  arguments?: string; // Input parameters
  output?: string; // Output parameters
}

// Interface for file search call function
export interface IFileSearchCallFunction {
  query: string;
  search_options: {
    enable_citation: boolean;
    enable_rerank: boolean;
    enable_search: boolean;
    hybrid_weight: number;
    kb_ids: string[];
    search_type: string;
    similarity_threshold: number;
    top_k: number;
  };
}

// Interface for file search result
export interface IFileSearchResult {
  chunk_id: string;
  doc_id: string;
  doc_name: string;
  enabled: boolean;
  page_number: number;
  score: number;
  text: string;
  title: string;
}

// Interface for file search result function
export interface IFileSearchResultFunction {
  output: string; // Contains IFileSearchResult[] when parsed
}

// Interface for tool call
export interface IToolCall {
  id: string;
  function:
    | IToolCallFunction
    | IFileSearchCallFunction
    | IFileSearchResultFunction[];
  type:
    | 'tool_call' // Remote function call
    | 'tool_result' // Remote function call result
    | 'file_search_call' // Document search call
    | 'file_search_result' // File search result
    | 'mcp_tool_call' // MCP tool call
    | 'mcp_tool_result' // MCP tool result
    | 'component_tool_call' // Component call
    | 'component_tool_result'; // Component call result
}

// Interface for message content
export interface IMessageContent {
  role: 'assistant' | 'plugin' | 'function';
  name?: string;
  content: string;
  tool_calls?: IToolCall[];
  reasoning_content?: string; // Deep reasoning content
}

// Interface for token usage
export interface IUsage {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
}

// Interface for error
export interface IError {
  code: string;
  message: string;
}

// Interface for received message
export interface IReceiveMessage {
  finish_reason?: 'stop' | 'length' | 'tool_calls';
  message?: IMessageContent;
  usage?: IUsage;
  error?: IError;
  request_id: string;
}
