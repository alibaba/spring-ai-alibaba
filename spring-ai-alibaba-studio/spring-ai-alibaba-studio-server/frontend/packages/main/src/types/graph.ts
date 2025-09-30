export interface IGraphData {
  id: string;
  name: string;
  description?: string;
  nodes?: GraphNode[];
  edges?: GraphEdge[];
  // Add other relevant fields as needed
  
  // 新增的 observability 字段
  title?: string;
  tags?: string[];
  mermaidGraph?: string;
  stateGraph?: any;
}

export interface GraphNode {
  id: string;
  type: string;
  data: Record<string, any>;
  position: {
    x: number;
    y: number;
  };
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;
  targetHandle?: string;
  label?: string;
}

export interface IGraphCard {
  id: string;
  name: string;
  description?: string;
  tags?: string[];
  gmt_modified: string;
  status: 'ACTIVE' | 'DRAFT' | 'DISABLED';
}

export interface GraphRunActionParam {
  graphName: string;
  input: Record<string, any>;
  stream?: boolean;
  // Add other relevant fields as needed
}

export interface GraphRunResult {
  id: string;
  status: string;
  result: any;
  telemetry?: any;
  // Add other relevant fields as needed
}

export interface GraphOptions {
  // Graph-specific configuration options
  maxExecutionTime?: number;
  enableParallelExecution?: boolean;
  debugMode?: boolean;
  // Add other configuration options as needed
}

export interface GraphStudioConfig {
  graphOptions: GraphOptions;
  // Add other studio-specific configurations
}

// 增强节点输出接口
export interface EnhancedNodeOutput {
  node_id: string;
  execution_status: 'EXECUTING' | 'SUCCESS' | 'FAILED' | 'SKIPPED';
  execution_order?: number;
  duration_ms?: number;
  data?: any;
  error_message?: string;
  timestamp?: string;
}
